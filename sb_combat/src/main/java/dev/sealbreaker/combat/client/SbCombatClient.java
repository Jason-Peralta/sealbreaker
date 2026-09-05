package dev.sealbreaker.combat.client;

import dev.sealbreaker.combat.SbCombat;
import dev.sealbreaker.combat.client.anim.AnimDebug;
import dev.sealbreaker.combat.client.anim.AnimationPoser;
import dev.sealbreaker.combat.client.anim.CameraFeel;
import dev.sealbreaker.combat.client.anim.HumanoidRig;
import dev.sealbreaker.combat.client.anim.SwingTrails;
import dev.sealbreaker.combat.client.anim.PoseSource;
import dev.sealbreaker.combat.client.anim.SwingBlendTracker;
import dev.sealbreaker.combat.client.anim.FirstPersonWeaponRenderer;
import dev.sealbreaker.combat.client.anim.PlayerAnimation;
import dev.sealbreaker.combat.client.anim.PlayerAnimations;
import dev.sealbreaker.combat.client.anim.SwingItemLayer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import dev.sealbreaker.combat.item.SbCombatItems;
import dev.sealbreaker.combat.network.SwingRequestPayload;
import dev.sealbreaker.combat.swing.SbCombatAttachments;
import dev.sealbreaker.combat.swing.SwingState;
import dev.sealbreaker.core.api.combat.SwingMove;
import dev.sealbreaker.core.api.combat.WeaponArchetype;
import dev.sealbreaker.core.api.component.SbDataComponents;
import dev.sealbreaker.core.api.registry.SbRegistries;
import dev.sealbreaker.core.client.api.HumanoidPoseHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Client side of the combat core: turn the attack key into a swing request for archetype weapons (never
 * into block breaking), attach the synced swing to render states, pose the third-person model with keyframe
 * animations through the core pose hook, and draw the first-person arm and item with the same animations.
 */
@EventBusSubscriber(modid = SbCombat.MOD_ID, value = Dist.CLIENT)
public final class SbCombatClient {
    private static long lastRequestTick = Long.MIN_VALUE;
    /** Fraction of the head-body yaw difference closed each tick during a move (vanilla uses 0.3 for attacks). */
    private static final float BODY_TURN_RATE = 0.5f;

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        HumanoidPoseHooks.register(SbCombatClient::poseThirdPerson);
    }

    @SubscribeEvent
    static void onAddReloadListeners(AddClientReloadListenersEvent event) {
        event.addListener(PlayerAnimations.ID, new PlayerAnimations());
    }

    /** Attack key with a weapon in hand: request a swing instead of the vanilla attack or block breaking. */
    @SubscribeEvent
    static void onAttackKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !player.getMainHandItem().has(SbDataComponents.ARCHETYPE.get())) {
            return;
        }
        event.setCanceled(true);
        event.setSwingHand(false);
        long now = player.level().getGameTime();
        if (now != lastRequestTick) {
            lastRequestTick = now;
            ClientPacketDistributor.sendToServer(SwingRequestPayload.INSTANCE);
        }
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        AnimDebug.tick(Minecraft.getInstance());
    }

    /**
     * Vanilla turns a player's body toward their look direction while the attack animation plays; ours never
     * runs it, so do the same for every avatar mid-move (each client computes body yaw locally, so this runs
     * for other players too). The cut then follows the torso, and the torso follows the crosshair.
     */
    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide()) {
            return;
        }
        SwingState swing = player.getExistingDataOrNull(SbCombatAttachments.SWING.get());
        if (swing == null) {
            return;
        }
        float toLook = net.minecraft.util.Mth.wrapDegrees(player.yHeadRot - player.yBodyRot);
        player.yBodyRot += toLook * BODY_TURN_RATE;
    }

    /** Attach the current move and its progress to the avatar's render state; suppress the vanilla attack pose. */
    @SubscribeEvent
    static void registerRenderStateModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerAvatarEntityModifier(new AvatarRenderStateModifier() {
            @Override
            public <T extends Avatar & ClientAvatarEntity> void accept(T avatar, AvatarRenderState state) {
                SwingRenderData data = renderData(avatar);
                if (data == null) {
                    return;
                }
                if (data.pose() != null) {
                    // Our item layer draws the main-hand item with the wrist bone; mute vanilla's for this frame.
                    ItemStack held = AnimDebug.enabled() && avatar == Minecraft.getInstance().player
                            ? new ItemStack(SbCombatItems.SPIKE_SWORD.get()) : avatar.getMainHandItem();
                    ItemStackRenderState itemState = new ItemStackRenderState();
                    boolean left = state.mainArm == HumanoidArm.LEFT;
                    Minecraft.getInstance().getItemModelResolver().updateForTopItem(itemState, held,
                            left ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                            avatar.level(), avatar, avatar.getId());
                    data = data.withItemState(itemState);
                    (left ? state.leftHandItemState : state.rightHandItemState).clear();
                }
                state.setRenderData(SwingRenderData.KEY, data);
                state.attackTime = 0.0f;
                state.attackArm = state.mainArm;
            }
        });
    }

    /** Our item layer on both player models (wide and slim). */
    @SubscribeEvent
    static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerModelType type : event.getSkins()) {
            var renderer = event.getPlayerRenderer(type);
            if (renderer != null) {
                renderer.addLayer(new SwingItemLayer(renderer));
            }
        }
    }

    /** Third person: keyframe animation when the move has one, procedural fallback otherwise. */
    private static void poseThirdPerson(net.minecraft.client.model.HumanoidModel<?> model, HumanoidRenderState state) {
        SwingRenderData data = state.getRenderData(SwingRenderData.KEY);
        if (data == null) {
            return;
        }
        if (data.pose() != null) {
            AnimationPoser.apply(model, data.pose(), state.mainArm, state.xRot, data.moveWeight());
        } else if (data.move() != null) {
            SwingAnimations.poseThirdPerson(model, state);
        }
    }

    /** First person: replace the vanilla hand render with the animated arm and item while a move plays. */
    @SubscribeEvent
    static void onRenderHand(RenderHandEvent event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }
        SwingRenderData data = renderData(player);
        boolean debugging = AnimDebug.overrideFirstPerson();
        if (data == null || data.pose() == null || (!debugging && !event.getItemStack().has(SbDataComponents.ARCHETYPE.get()))) {
            return;
        }
        ItemStack stack = debugging ? new ItemStack(SbCombatItems.SPIKE_SWORD.get()) : event.getItemStack();
        event.setCanceled(true);
        float now = player.level().getGameTime() + minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        FirstPersonWeaponRenderer.render(event.getPoseStack(), event.getSubmitNodeCollector(), event.getPackedLight(), player,
                stack, player.getMainArm(), event.getEquipProgress(), data.pose(), new PoseSource.AnimationAt(data.idle(), 0.0f),
                framing(data), data.striking() ? now : Float.NaN);
    }

    /** How first person frames the move: which move, how much of it is showing, and whether the blade is aiming. */
    private static FirstPersonWeaponRenderer.Framing framing(SwingRenderData data) {
        SwingMove move = data.move();
        if (move == null) {
            return FirstPersonWeaponRenderer.Framing.IDLE;
        }
        PlayerAnimation animation = move.animation().map(PlayerAnimations::get).orElse(null);
        float aim = 0.0f;
        if (move.shape() == SwingMove.Shape.THRUST) {
            // Full aim through the hit window, ramping in over the drive and out over the start of the withdrawal.
            float in = (data.elapsedTicks() - (move.windupTicks() - 5)) / 4.0f;
            float out = (move.windupTicks() + move.activeTicks() + 3 - data.elapsedTicks()) / 3.0f;
            aim = Mth.clamp(Math.min(in, out), 0.0f, 1.0f) * data.moveWeight();
        }
        // The shift that centres the strike ramps in over the wind-up, so the wind-up itself starts where the
        // hand really is and the blade arrives on the crosshair at contact.
        float contact = move.windupTicks() + move.activeTicks() * 0.5f;
        float ramp = Mth.clamp(data.elapsedTicks() / Math.max(1.0f, contact), 0.0f, 1.0f);
        float centring = ramp * ramp * (3.0f - 2.0f * ramp) * data.moveWeight();
        return new FirstPersonWeaponRenderer.Framing(animation, move, data.moveWeight(), aim, centring);
    }

    /** What to draw for an avatar this frame: its move, its idle stance, or the debugger's frozen frame; null for vanilla. */
    private static <T extends Avatar> SwingRenderData renderData(T avatar) {
        Minecraft minecraft = Minecraft.getInstance();
        if (avatar == minecraft.player && AnimDebug.overrideAnimation() != null) {
            PlayerAnimation animation = PlayerAnimations.get(AnimDebug.overrideAnimation());
            if (animation == null) {
                return null;
            }
            PlayerAnimation idle = PlayerAnimations.get(AnimDebug.DEBUG_IDLE);
            return new SwingRenderData(null, AnimDebug.overrideSeconds() * 20.0f,
                    new PoseSource.AnimationAt(animation, AnimDebug.overrideSeconds()), idle == null ? PlayerAnimation.EMPTY : idle, 1.0f);
        }
        Identifier archetypeId = avatar.getMainHandItem().get(SbDataComponents.ARCHETYPE.get());
        if (archetypeId == null) {
            if (avatar == minecraft.player) {
                CameraFeel.setPose(null);
            }
            return null;
        }
        Optional<WeaponArchetype> archetype = avatar.level().registryAccess()
                .lookupOrThrow(SbRegistries.WEAPON_ARCHETYPE).getOptional(archetypeId);
        if (archetype.isEmpty()) {
            return null;
        }
        PlayerAnimation idle = archetype.get().idle().map(PlayerAnimations::get).orElse(PlayerAnimation.EMPTY);
        float partial = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        float now = avatar.level().getGameTime() + partial;

        SwingState swing = avatar.getExistingDataOrNull(SbCombatAttachments.SWING.get());
        SwingMove move = null;
        PlayerAnimation animation = null;
        float elapsed = 0.0f;
        if (swing != null && swing.archetype().equals(archetypeId)) {
            SwingMove candidate = archetype.get().move(swing.step());
            elapsed = now - swing.startTick();
            if (elapsed >= 0.0f && elapsed < candidate.totalTicks()) {
                move = candidate;
            }
            animation = candidate.animation().map(PlayerAnimations::get).orElse(null);
        }
        if (avatar == minecraft.player && (swing != null && animation == null)) {
            CameraFeel.setPose(null);
        }
        if (swing != null && animation == null) {
            // A move without keyframes: the procedural fallback poses it, nothing to blend.
            return move == null ? new SwingRenderData(null, 0.0f, new PoseSource.AnimationAt(idle, 0.0f), idle, 0.0f)
                    : new SwingRenderData(move, elapsed, null, idle, 1.0f);
        }
        int total = swing == null ? 0 : archetype.get().move(swing.step()).totalTicks();
        SwingBlendTracker.Result result = SwingBlendTracker.pose(avatar.level(), avatar.getId(), swing, animation, total, idle, now);
        boolean local = avatar == minecraft.player;
        if (local) {
            CameraFeel.setPose(result.pose());
            if (result.impact() && move != null) {
                CameraFeel.kick(now, move.shape() == SwingMove.Shape.SWEEP ? 1.0f : 1.5f);
            }
        }
        boolean striking = move != null && result.elapsed() >= move.windupTicks() - 2
                && result.elapsed() <= move.windupTicks() + move.activeTicks() + 3;
        // The arc follows the blade the viewer sees: the first-person renderer samples its own weapon.
        if (striking && !(local && minecraft.options.getCameraType().isFirstPerson())) {
            Vec3 at = avatar.getPosition(partial);
            float bodyYaw = Mth.rotLerp(partial, avatar.yBodyRotO, avatar.yBodyRot);
            Vec3[] blade = HumanoidRig.bladeInWorld(result.pose(), avatar.getMainArm(), at.x, at.y, at.z, bodyYaw);
            SwingTrails.sample(avatar.getId(), blade[0], blade[1], now);
        }
        return new SwingRenderData(move, elapsed, result.pose(), idle, result.moveWeight(), striking);
    }

    /** The blade arcs of everyone mid-strike, in world space, after the entities. */
    @SubscribeEvent
    static void onSubmitCustomGeometry(SubmitCustomGeometryEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        float now = minecraft.level.getGameTime() + minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        SwingTrails.render(event.getPoseStack(), event.getSubmitNodeCollector(), event.getLevelRenderState().cameraRenderState.pos, now);
    }

    /** The local player's camera: the animation's camera bone and the kick of a landed hit. */
    @SubscribeEvent
    static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        float now = minecraft.level.getGameTime() + (float) event.getPartialTick();
        Vector3f offsets = CameraFeel.offsets(now, new Vector3f());
        if (offsets.x != 0.0f || offsets.y != 0.0f || offsets.z != 0.0f) {
            event.setPitch(event.getPitch() + offsets.x);
            event.setYaw(event.getYaw() + offsets.y);
            event.setRoll(event.getRoll() + offsets.z);
        }
    }

    private SbCombatClient() {
    }
}
