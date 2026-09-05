package dev.sealbreaker.combat.swing;

import dev.sealbreaker.combat.SbCombat;
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

    /** Server-only bookkeeping per attacker: targets hit this move, buffered input, and the last combo position. */
    private static final class Runtime {
        final Set<UUID> hitThisMove = new HashSet<>();
        boolean buffered;
        int lastStep = -1;
        long lastEndTick = Long.MIN_VALUE;
    }

    private static final Map<UUID, Runtime> RUNTIME = new ConcurrentHashMap<>();

    /** Handles a swing request from a client. */
    public static void onSwingRequest(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        Identifier archetypeId = held.get(SbDataComponents.ARCHETYPE.get());
        if (archetypeId == null) {
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        Optional<WeaponArchetype> archetype = lookup(level, archetypeId);
        if (archetype.isEmpty()) {
            SbCombat.LOGGER.warn("Item {} references unknown weapon archetype {}", held, archetypeId);
            return;
        }
        long now = level.getGameTime();
        Runtime runtime = RUNTIME.computeIfAbsent(player.getUUID(), k -> new Runtime());
        SwingState current = player.getExistingDataOrNull(SbCombatAttachments.SWING.get());
        if (current != null && current.archetype().equals(archetypeId)) {
            SwingMove move = archetype.get().move(current.step());
            if (!move.isFinishedAt(current.ticksSince(now))) {
                runtime.buffered = true;
                return;
            }
        }
        int step = 0;
        if (runtime.lastStep >= 0 && now - runtime.lastEndTick <= archetype.get().comboWindowTicks()) {
            step = archetype.get().nextStep(runtime.lastStep);
        }
        startMove(player, level, archetype.get(), archetypeId, step, runtime);
    }

    private static void startMove(ServerPlayer player, ServerLevel level, WeaponArchetype archetype, Identifier archetypeId, int step, Runtime runtime) {
        player.setData(SbCombatAttachments.SWING.get(), new SwingState(archetypeId, step, level.getGameTime()));
        runtime.hitThisMove.clear();
        runtime.buffered = false;
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
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        SwingState swing = player.getExistingDataOrNull(SbCombatAttachments.SWING.get());
        if (swing == null) {
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        Optional<WeaponArchetype> archetype = lookup(level, swing.archetype());
        if (archetype.isEmpty()) {
            player.removeData(SbCombatAttachments.SWING.get());
            return;
        }
        Runtime runtime = RUNTIME.computeIfAbsent(player.getUUID(), k -> new Runtime());
        SwingMove move = archetype.get().move(swing.step());
        long elapsed = swing.ticksSince(level.getGameTime());
        if (move.isFinishedAt(elapsed)) {
            if (runtime.buffered && player.getMainHandItem().has(SbDataComponents.ARCHETYPE.get())) {
                startMove(player, level, archetype.get(), swing.archetype(), archetype.get().nextStep(swing.step()), runtime);
                return;
            }
            player.removeData(SbCombatAttachments.SWING.get());
            runtime.buffered = false;
            runtime.lastStep = swing.step();
            runtime.lastEndTick = level.getGameTime();
            return;
        }
        int activeIndex = move.activeIndexAt(elapsed);
        if (activeIndex >= 0) {
            if (activeIndex == 0 && runtime.hitThisMove.isEmpty()) {
                playSwing(player, level, move, swing.step());
            }
            applyActiveTick(player, level, move, activeIndex, runtime);
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
                    player.setData(SbCombatAttachments.SWING.get(), new SwingState(swing.archetype(), swing.step(), swing.startTick() + move.hitstopTicks()));
                }
            }
            hit(player, level, target, move);
        }
    }

    /** Applies one hit: damage through the vanilla pipeline, crit roll, knockback, feedback. */
    public static void hit(ServerPlayer player, ServerLevel level, LivingEntity target, SwingMove move) {
        float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * move.damageMultiplier();
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
