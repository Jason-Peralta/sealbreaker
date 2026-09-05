package dev.sealbreaker.combat.client.anim;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.sealbreaker.core.api.combat.SwingMove;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * First person: the weapon in a virtual fist placed where vanilla draws a held item, oriented and moved by the
 * same rig that poses the third-person model, then framed for a game: the crosshair is the centre of attention.
 *
 * <p>Framing rules, in order:
 * <ul>
 * <li><b>Through the crosshair.</b> For every move, the pose at its contact moment is placed once and the
 * blade's reference point (the tip of a thrust, the middle of a cut) is measured; the whole move is then
 * shifted so that point sits on the crosshair at contact. A cut therefore crosses the crosshair on its way
 * and a thrust lands on it, whatever the third-person choreography does with the arm.</li>
 * <li><b>Aim.</b> Around a thrust's hit window the blade is turned to point straight down the view line.</li>
 * <li><b>Guards.</b> Motion toward the viewer is damped and the hand stays at arm's length; a blade that would
 * point behind the camera plane is turned to lie along the screen; the flat is twisted to face a point above
 * the viewer so a sprite is never seen edge-on as a black slab.</li>
 * </ul>
 * No arm is drawn: at this distance a real arm fills the screen, and vanilla shows none with an item either.
 */
public final class FirstPersonWeaponRenderer {
    /** What the framing needs to know about the move playing, beyond the pose. */
    public record Framing(@Nullable PlayerAnimation animation, @Nullable SwingMove move, float moveWeight, float aimWeight,
                          float centringWeight) {
        public static final Framing IDLE = new Framing(null, null, 0.0f, 0.0f, 0.0f);
    }

    /** Vanilla's first-person hand point for a held item (ItemInHandRenderer#applyItemArmTransform). */
    private static final float HAND_RIGHT = 0.56f;
    private static final float HAND_DOWN = -0.52f;
    private static final float HAND_FORWARD = -0.72f;
    /** Roll of the weapon at rest, so the blade leans toward the middle of the screen the way vanilla's does. */
    private static final float REST_ROLL_DEGREES = -18.0f;
    /**
     * Fist travel in the pose is scaled for the near, wide framing of first person: forward travel exaggerated
     * so a thrust visibly recedes toward the crosshair, motion back toward the viewer damped.
     */
    private static final float TRAVEL_SCALE_ACROSS = 1.0f;
    private static final float TRAVEL_SCALE_UP = 0.5f;
    private static final float TRAVEL_SCALE_FORWARD = 1.4f;
    private static final float TRAVEL_SCALE_BACK = 0.3f;
    /** The hand never comes nearer than this (camera z, negative is away). */
    private static final float HAND_FORWARD_LIMIT = -0.6f;
    /** The blade always points at least this much away from the viewer (cosine of the angle off the screen plane). */
    private static final float BLADE_MIN_AWAY = 0.2f;
    /** The flat of the blade always faces the viewer at least this much (cosine of the angle to the view). */
    private static final float FLAT_MIN_FACING = 0.45f;
    /** Where the crosshair is aimed at for framing, blocks ahead. */
    private static final float AIM_DISTANCE = 3.0f;
    /** The reference point is put this far below the exact centre (per block of depth) so the blade passes through the crosshair. */
    private static final float CENTRE_DROP = 0.03f;

    /** Per-move shift that puts the blade on the crosshair at contact; keyed by the animation, one slot per arm. */
    private static final Map<PlayerAnimation, Vector3f[]> CENTRING = new HashMap<>();

    /** A placed weapon in camera space, before the item's own grip chain. */
    private record Placement(Vector3f hand, Matrix4f orientation, Vector3f blade, Vector3f flatNormal) {
    }

    /**
     * @param trailAt game time (with the partial tick) at which to record the blade for its arc, or NaN when the
     *                move is not in its striking part
     */
    public static void render(PoseStack poseStack, SubmitNodeCollector collector, int light, AbstractClientPlayer player,
                              ItemStack stack, HumanoidArm arm, float inverseArmHeight, PoseSource pose, PoseSource idle,
                              Framing framing, float trailAt) {
        boolean right = arm != HumanoidArm.LEFT;
        float side = right ? 1.0f : -1.0f;

        Placement placed = place(pose, idle, arm, inverseArmHeight);
        Vector3f hand = new Vector3f(placed.hand());
        if (framing.animation() != null && framing.move() != null && framing.centringWeight() > 0.0f) {
            Vector3f shift = centring(framing.animation(), framing.move(), idle, arm);
            hand.add(shift.x * framing.centringWeight(), shift.y * framing.centringWeight(), 0.0f);
        }
        hand.z = Math.min(hand.z, HAND_FORWARD_LIMIT);

        // Aim: turn the blade toward the point the crosshair is on.
        Quaternionf aim = new Quaternionf();
        Vector3f blade = new Vector3f(placed.blade());
        if (framing.aimWeight() > 0.0f) {
            Vector3f toCrosshair = new Vector3f(0.0f, 0.0f, -AIM_DISTANCE).sub(hand).normalize();
            Quaternionf full = new Quaternionf().rotationTo(blade, toCrosshair);
            aim = new Quaternionf().slerp(full, framing.aimWeight());
            aim.transform(blade);
        }
        // Rest roll: the vanilla lean, fading out as a move takes over.
        Quaternionf roll = new Quaternionf().rotationZ(REST_ROLL_DEGREES * side * (1.0f - framing.moveWeight()) * Mth.DEG_TO_RAD);
        roll.transform(blade);

        // Guards.
        float yawFix = 0.0f;
        float flat = (float) Math.sqrt(blade.x * blade.x + blade.z * blade.z);
        if (flat > 1.0e-4f && blade.z > -BLADE_MIN_AWAY * flat) {
            // Angle of the blade from forward, positive to the right; a positive turn about +y moves it left.
            float current = (float) Math.atan2(blade.x, -blade.z);
            float limit = (float) Math.acos(BLADE_MIN_AWAY);
            yawFix = current - (current > 0.0f ? limit : -limit);
        }
        Quaternionf yawFixRotation = new Quaternionf().rotationY(yawFix);
        Vector3f bladeAxis = yawFixRotation.transform(new Vector3f(blade)).normalize();
        Vector3f flatNormal = yawFixRotation.transform(roll.transform(aim.transform(new Vector3f(placed.flatNormal())))).normalize();
        Vector3f facing = new Vector3f(hand).negate().add(0.0f, 0.6f, 0.0f).normalize();
        float twist = twistToFaceViewer(flatNormal, bladeAxis, facing);

        poseStack.pushPose();
        poseStack.translate(hand.x, hand.y, hand.z);
        if (twist != 0.0f) {
            poseStack.mulPose(new Quaternionf().rotationAxis(twist, bladeAxis));
        }
        poseStack.mulPose(yawFixRotation);
        poseStack.mulPose(roll);
        poseStack.mulPose(aim);
        poseStack.mulPose(placed.orientation());
        if (!Float.isNaN(trailAt)) {
            // The hand stack already holds the inverse view rotation, so its transform of a point is the
            // camera-relative world offset: add the camera position and the arc lands on the drawn blade.
            Matrix4f toWorld = poseStack.last().pose();
            Vector3f bladeLocal = HumanoidRig.bladeDirection(pose, arm, new Vector3f());
            Vector3f grip = toWorld.transformPosition(new Vector3f());
            Vector3f tip = toWorld.transformPosition(new Vector3f(bladeLocal).mul(HumanoidRig.BLADE_LENGTH));
            Vec3 camera = Minecraft.getInstance().gameRenderer.mainCamera().position();
            SwingTrails.sample(player.getId(), camera.add(grip.x, grip.y, grip.z), camera.add(tip.x, tip.y, tip.z), trailAt);
        }
        HeldItemPose.applyAtFist(poseStack, pose, arm);
        Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer().renderItem(player, stack,
                right ? ItemDisplayContext.THIRD_PERSON_RIGHT_HAND : ItemDisplayContext.THIRD_PERSON_LEFT_HAND,
                poseStack, collector, light);
        poseStack.popPose();
    }

    /** The rig's fist mapped into first-person camera space, with the travel scaling. */
    private static Placement place(PoseSource pose, PoseSource idle, HumanoidArm arm, float inverseArmHeight) {
        float side = arm == HumanoidArm.LEFT ? -1.0f : 1.0f;
        Matrix4f fist = HumanoidRig.fistFrame(pose, arm, new Matrix4f());
        Matrix4f rest = HumanoidRig.fistFrame(idle, arm, new Matrix4f());
        Vector3f travel = fist.getTranslation(new Vector3f()).sub(rest.getTranslation(new Vector3f()));
        Vector3f travelCamera = new Vector3f(-travel.x * TRAVEL_SCALE_ACROSS, -travel.y * TRAVEL_SCALE_UP, travel.z * TRAVEL_SCALE_FORWARD);
        if (travelCamera.z > 0.0f) {
            travelCamera.z *= TRAVEL_SCALE_BACK / TRAVEL_SCALE_FORWARD;
        }
        Vector3f hand = new Vector3f(side * HAND_RIGHT + travelCamera.x, HAND_DOWN + inverseArmHeight * -0.6f + travelCamera.y, HAND_FORWARD + travelCamera.z);
        Matrix4f orientation = new Matrix4f().rotationZ((float) Math.PI).mul(new Matrix4f(fist).setTranslation(0.0f, 0.0f, 0.0f));
        Vector3f blade = orientation.transformDirection(HumanoidRig.bladeDirection(pose, arm, new Vector3f())).normalize();
        Vector3f flatNormal = orientation.transformDirection(HumanoidRig.flatNormal(pose, arm, new Vector3f())).normalize();
        return new Placement(hand, orientation, blade, flatNormal);
    }

    /**
     * The shift (x, y) that puts the move's blade reference point on the crosshair at its contact moment: the
     * tip for a thrust, the middle of the blade for a cut. Computed once per animation and arm.
     */
    private static Vector3f centring(PlayerAnimation animation, SwingMove move, PoseSource idle, HumanoidArm arm) {
        Vector3f[] cached = CENTRING.computeIfAbsent(animation, k -> new Vector3f[2]);
        int slot = arm == HumanoidArm.LEFT ? 1 : 0;
        if (cached[slot] == null) {
            float contactSeconds = (move.windupTicks() + move.activeTicks() * 0.5f) / 20.0f;
            Placement contact = place(new PoseSource.AnimationAt(animation, contactSeconds), idle, arm, 0.0f);
            float along = move.shape() == SwingMove.Shape.THRUST ? HumanoidRig.BLADE_LENGTH : HumanoidRig.BLADE_LENGTH * 0.5f;
            Vector3f reference = new Vector3f(contact.hand()).add(new Vector3f(contact.blade()).mul(along));
            float depth = Math.max(0.3f, -Math.min(reference.z, HAND_FORWARD_LIMIT));
            // The crosshair at that depth is the view axis; drop a hair so the blade passes through it.
            cached[slot] = new Vector3f(-reference.x, -CENTRE_DROP * depth - reference.y, 0.0f);
        }
        return cached[slot];
    }

    /** Forget cached framing (animations reloaded). */
    public static void reset() {
        CENTRING.clear();
    }

    /**
     * The smallest rotation about {@code axis} that brings the flat's normal to at least {@link #FLAT_MIN_FACING}
     * of the view direction: the sprite is double-sided, so either sign of the dot product will do.
     */
    private static float twistToFaceViewer(Vector3f normal, Vector3f axis, Vector3f view) {
        float a = normal.dot(view);
        if (Math.abs(a) >= FLAT_MIN_FACING) {
            return 0.0f;
        }
        Vector3f across = new Vector3f(axis).cross(normal);
        float b = across.dot(view);
        float reach = (float) Math.sqrt(a * a + b * b);
        float phase = (float) Math.atan2(b, a);
        if (reach <= FLAT_MIN_FACING) {
            return wrap(phase);
        }
        float spread = (float) Math.acos(FLAT_MIN_FACING / reach);
        float best = 0.0f;
        float bestSize = Float.MAX_VALUE;
        for (float candidate : new float[]{phase - spread, phase + spread, phase + Mth.PI - spread, phase + Mth.PI + spread}) {
            float wrapped = wrap(candidate);
            if (Math.abs(wrapped) < bestSize) {
                bestSize = Math.abs(wrapped);
                best = wrapped;
            }
        }
        return best;
    }

    private static float wrap(float radians) {
        float r = radians % (2.0f * Mth.PI);
        if (r > Mth.PI) {
            r -= 2.0f * Mth.PI;
        } else if (r < -Mth.PI) {
            r += 2.0f * Mth.PI;
        }
        return r;
    }

    private FirstPersonWeaponRenderer() {
    }
}
