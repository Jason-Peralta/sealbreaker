package dev.sealbreaker.combat.swing;

import dev.sealbreaker.combat.SbCombat;
import dev.sealbreaker.combat.network.SwingRequestPayload;
import dev.sealbreaker.core.api.combat.AttackContext;
import dev.sealbreaker.core.api.combat.SwingMove;
import dev.sealbreaker.core.api.combat.WeaponArchetype;
import dev.sealbreaker.core.api.component.SbDataComponents;
import dev.sealbreaker.core.api.registry.SbRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-authoritative moves and combos. A request is validated here, the current move is attached to
 * the player (and synced), and every server tick during the move's active window the hit region is
 * tested and each target is hit at most once. Numbers come from the archetype JSON; nothing is tuned here.
 *
 * <p>Combo rules: a move always plays out in full. A request during a move is buffered and the next move
 * starts the tick this one ends (the follow-through of one cut is the wind-up of the next, so the
 * animations chain); a request within the archetype's combo window after a move ends continues the combo;
 * any later request starts again from the first move.
 */
@EventBusSubscriber(modid = SbCombat.MOD_ID)
public final class SwingService {
    /** Base crit chance (decision 0016: 4% for every class); gear and prefixes add to it in Milestone 1. */
    private static final float BASE_CRIT_CHANCE = 0.04f;
    private static final float CRIT_MULTIPLIER = 2.0f;

    /** Transient bookkeeping is bound to the exact player, level and held stack, never just an archetype. */
    private static final class Runtime {
        final ServerPlayer owner;
        final ServerLevel level;
        final ItemStack weapon;
        final ItemStack original;
        final Set<UUID> hitThisMove = new HashSet<>();
        boolean buffered;
        int lastStep = -1;
        long lastEndTick = Long.MIN_VALUE;
        long pressedAt;
        AttackContext pressContext;
        boolean canCharge;

        Runtime(ServerPlayer player) {
            owner = player;
            level = (ServerLevel) player.level();
            weapon = player.getMainHandItem();
            original = weapon.copy();
        }

        boolean matches(ServerPlayer player) {
            return owner == player && level == player.level() && weapon == player.getMainHandItem()
                    && ItemStack.isSameItemSameComponents(original, weapon);
        }
    }

    private static final Map<UUID, Runtime> RUNTIME = new ConcurrentHashMap<>();

    /** Input edges from the client. Movement and charge duration are always checked on the server. */
    public static void onSwingRequest(ServerPlayer player, SwingRequestPayload request) {
        if (!player.isAlive() || player.isSpectator()) {
            clear(player);
            return;
        }
        Runtime runtime = RUNTIME.get(player.getUUID());
        if (runtime != null && !runtime.matches(player)) {
            clear(player);
            runtime = null;
        }
        if (request.cancel()) {
            if (runtime != null) {
                runtime.pressContext = null;
                SwingState swing = player.getExistingDataOrNull(SbCombatAttachments.SWING.get());
                if (swing != null && swing.charging()) {
                    player.removeData(SbCombatAttachments.SWING.get());
                }
            }
            return;
        }
        Identifier archetypeId = player.getMainHandItem().get(SbDataComponents.ARCHETYPE.get());
        if (archetypeId == null) {
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        WeaponArchetype archetype = lookup(level, archetypeId).orElse(null);
        if (archetype == null) {
            return;
        }
        if (runtime == null) {
            runtime = new Runtime(player);
            RUNTIME.put(player.getUUID(), runtime);
        }
        long now = level.getGameTime();
        if (!request.release()) {
            // HOLD is derived from a real TAP press, not a separately trusted client claim.
            if (runtime.pressContext != null || request.context() == AttackContext.HOLD
                    || !validContext(player, request.context())) {
                return;
            }
            runtime.pressContext = request.context();
            runtime.pressedAt = now;
            runtime.canCharge = player.getExistingDataOrNull(SbCombatAttachments.SWING.get()) == null;
            if (request.context() != AttackContext.TAP) {
                requestMove(player, archetype, archetypeId, request.context(), 1, runtime);
            }
            return;
        }
        AttackContext pressed = runtime.pressContext;
        runtime.pressContext = null; // a duplicate release cannot attack or charge twice
        if (pressed != AttackContext.TAP) {
            return; // movement contexts already fired on press; unmatched releases do nothing
        }
        if (request.context() != AttackContext.TAP && request.context() != AttackContext.HOLD) {
            cancelCharge(player);
            return;
        }
        long heldTicks = now - runtime.pressedAt;
        AttackContext context = heldTicks > archetype.holdThresholdTicks()
                ? archetype.resolve(AttackContext.HOLD) : AttackContext.TAP;
        if (context == AttackContext.HOLD && !runtime.canCharge) {
            return;
        }
        cancelCharge(player);
        float scale = context == AttackContext.HOLD ? archetype.chargeCurve().scale(heldTicks, archetype.chargeTicks()) : 1;
        requestMove(player, archetype, archetypeId, context, scale, runtime);
    }

    private static boolean validContext(ServerPlayer player, AttackContext context) {
        return switch (context) {
            case AIR -> !player.onGround() && player.getDeltaMovement().y < 0;
            case SPRINT -> player.onGround() && player.isSprinting();
            case TAP, HOLD -> true;
        };
    }

    private static void requestMove(ServerPlayer player, WeaponArchetype archetype, Identifier id,
                                    AttackContext requested, float scale, Runtime runtime) {
        AttackContext context = archetype.resolve(requested);
        SwingState current = player.getExistingDataOrNull(SbCombatAttachments.SWING.get());
        if (current != null) {
            if (context == AttackContext.TAP && !current.charging()) {
                runtime.buffered = true;
            }
            return;
        }
        int step = context == AttackContext.TAP && runtime.lastStep >= 0
                && runtime.level.getGameTime() - runtime.lastEndTick <= archetype.comboWindowTicks()
                ? archetype.nextStep(runtime.lastStep) : 0;
        startMove(player, archetype, id, context, step, scale, runtime);
    }

    private static void startMove(ServerPlayer player, WeaponArchetype archetype, Identifier id, AttackContext context, int step, float scale, Runtime runtime) {
        // Holding has already supplied the anticipation; release enters the move's active window.
        long start = runtime.level.getGameTime() - (context == AttackContext.HOLD ? archetype.move(context, step).windupTicks() : 0);
        player.setData(SbCombatAttachments.SWING.get(), new SwingState(id, step, start, context, false, scale));
        runtime.hitThisMove.clear();
        runtime.buffered = false;
        runtime.lastStep = -1;
    }

    private static void cancelCharge(ServerPlayer player) {
        SwingState swing = player.getExistingDataOrNull(SbCombatAttachments.SWING.get());
        if (swing != null && swing.charging()) {
            player.removeData(SbCombatAttachments.SWING.get());
        }
    }

    private static void clear(ServerPlayer player) {
        RUNTIME.remove(player.getUUID());
        player.removeData(SbCombatAttachments.SWING.get());
    }

    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clear(player);
        }
    }

    @SubscribeEvent
    static void onServerStopped(ServerStoppedEvent event) {
        RUNTIME.clear();
    }

    /** The whoosh belongs to the strike, not the wind-up: played the tick the hit window opens. */
    private static void playSwing(ServerPlayer player, ServerLevel level, SwingMove move, int step) {
        float pitch = move.shape() == SwingMove.Shape.SWEEP ? 0.95f + step * 0.1f : 0.7f;
        float volume = move.shape() == SwingMove.Shape.SWEEP ? 0.7f : 0.9f;
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, volume, pitch);
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            tick(player);
        }
    }

    /** Shared by the server event and GameTests using mock server players. */
    public static void tick(ServerPlayer player) {
        Runtime runtime = RUNTIME.get(player.getUUID());
        if (runtime == null) {
            return;
        }
        if (!runtime.matches(player) || !player.isAlive() || player.isSpectator()
                || player.containerMenu != player.inventoryMenu) {
            clear(player);
            return;
        }
        Identifier id = player.getMainHandItem().get(SbDataComponents.ARCHETYPE.get());
        WeaponArchetype archetype = id == null ? null : lookup(runtime.level, id).orElse(null);
        if (archetype == null) {
            clear(player);
            return;
        }
        SwingState swing = player.getExistingDataOrNull(SbCombatAttachments.SWING.get());
        long now = runtime.level.getGameTime();
        if (swing == null && runtime.pressContext == AttackContext.TAP && runtime.canCharge
                && !archetype.hold().isEmpty() && now - runtime.pressedAt > archetype.holdThresholdTicks()) {
            player.setData(SbCombatAttachments.SWING.get(),
                    new SwingState(id, 0, runtime.pressedAt, AttackContext.HOLD, true, 1));
            return;
        }
        if (swing == null || swing.charging()) {
            return;
        }
        SwingMove move = archetype.move(swing.context(), swing.step());
        long elapsed = swing.ticksSince(now);
        if (move.isFinishedAt(elapsed) || (swing.context() == AttackContext.AIR && player.onGround())) {
            player.removeData(SbCombatAttachments.SWING.get());
            if (runtime.buffered) {
                int next = swing.context() == AttackContext.TAP ? archetype.nextStep(swing.step()) : 0;
                startMove(player, archetype, id, AttackContext.TAP, next, 1, runtime);
                return;
            }
            runtime.lastStep = swing.context() == AttackContext.TAP ? swing.step() : -1;
            runtime.lastEndTick = now;
            return;
        }
        int activeIndex = move.activeIndexAt(elapsed);
        if (activeIndex >= 0) {
            if (activeIndex == 0 && runtime.hitThisMove.isEmpty()) {
                playSwing(player, runtime.level, move, swing.step());
            }
            applyActiveTick(player, runtime.level, move, activeIndex, runtime);
        }
    }

    private static void applyActiveTick(ServerPlayer player, ServerLevel level, SwingMove move, int activeIndex, Runtime runtime) {
        List<LivingEntity> targets = ArcHitTest.findTargets(level, player, player.getEyePosition(), player.getLookAngle(), move, activeIndex);
        for (LivingEntity target : targets) {
            if (!runtime.hitThisMove.add(target.getUUID())) {
                continue;
            }
            if (runtime.hitThisMove.size() == 1 && move.hitstopTicks() > 0) {
                // Impact frames: shift the move's timeline; the clients freeze the pose over the gap.
                SwingState swing = player.getExistingDataOrNull(SbCombatAttachments.SWING.get());
                if (swing != null) {
                    player.setData(SbCombatAttachments.SWING.get(), swing.withStartTick(swing.startTick() + move.hitstopTicks()));
                }
            }
            hit(player, level, target, move);
        }
    }

    /** Applies one hit: damage through the vanilla pipeline, crit roll, knockback, feedback. */
    public static void hit(ServerPlayer player, ServerLevel level, LivingEntity target, SwingMove move) {
        SwingState swing = player.getExistingDataOrNull(SbCombatAttachments.SWING.get());
        float scale = swing == null ? 1 : swing.damageScale();
        float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * move.damageMultiplier() * scale;
        boolean crit = player.getRandom().nextFloat() < BASE_CRIT_CHANCE;
        if (crit) {
            damage *= CRIT_MULTIPLIER;
        }
        DamageSource source = level.damageSources().playerAttack(player);
        if (target.hurtServer(level, source, damage)) {
            float yaw = player.getYRot() * Mth.DEG_TO_RAD;
            target.knockback(move.knockback(), Mth.sin(yaw), -Mth.cos(yaw), source, damage);
            Vec3 c = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
            if (crit) {
                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 1.0f);
                level.sendParticles(ParticleTypes.CRIT, c.x, c.y, c.z, 12, 0.3, 0.3, 0.3, 0.2);
            } else {
                float pitch = move.shape() == SwingMove.Shape.SWEEP ? 1.0f : 0.8f;
                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.8f, pitch);
                level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, c.x, c.y, c.z, 4, 0.2, 0.2, 0.2, 0.1);
            }
        }
    }

    public static Optional<WeaponArchetype> lookup(ServerLevel level, Identifier id) {
        return level.registryAccess().lookupOrThrow(SbRegistries.WEAPON_ARCHETYPE).getOptional(id);
    }

    private SwingService() {
    }
}
