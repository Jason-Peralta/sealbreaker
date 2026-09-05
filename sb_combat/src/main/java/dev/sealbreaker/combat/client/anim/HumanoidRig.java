package dev.sealbreaker.combat.client.anim;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The numbers the vanilla humanoid model is built from, and the one calculation both views share: where a
 * pose puts the main hand. Model space, pixels unless stated: y down, the front toward -z, the character's
 * left toward +x.
 */
public final class HumanoidRig {
    /** Hips of the humanoid model: the body rotates about them and the arms and head ride on it. */
    public static final Vector3f HIP = new Vector3f(0.0f, 12.0f, 0.0f);
    /** Shoulder pivot of the right arm; the left arm mirrors x. */
    public static final Vector3f RIGHT_ARM_PIVOT = new Vector3f(-5.0f, 2.0f, 0.0f);
    /** Centre of the fist in arm space (the arm spans -2..10 along its length, one pixel outward of the pivot). */
    public static final Vector3f RIGHT_FIST = new Vector3f(-1.0f, 8.0f, 0.0f);
    /** Vanilla raises the arm holding an item by this much (HumanoidModel#poseRightArm), radians. */
    public static final float ITEM_ARM_RAISE = (float) (-Math.PI / 10.0);

    /**
     * The main fist's frame in model space, blocks: the fist centre, oriented like the arm (the wrist is applied
     * on top by {@link HeldItemPose#applyAtFist}), for a pose sampled with the animation's right-handed
     * conventions mirrored for a left-handed character. Mirrors {@code AnimationPoser}: the body rotates about
     * the hips and the arm rides on it.
     */
    public static Matrix4f fistFrame(PoseSource pose, HumanoidArm arm, Matrix4f out) {
        float side = arm == HumanoidArm.LEFT ? -1.0f : 1.0f;
        Vector3f rot = new Vector3f();
        Vector3f pos = new Vector3f();
        out.identity();
        sample(pose, "body", side, rot, pos);
        out.translate(pos.x / 16.0f, pos.y / 16.0f, pos.z / 16.0f);
        out.translate(HIP.x / 16.0f, HIP.y / 16.0f, HIP.z / 16.0f);
        out.rotateZYX(rot.z, rot.y, rot.x);
        out.translate(-HIP.x / 16.0f, -HIP.y / 16.0f, -HIP.z / 16.0f);
        sample(pose, "right_arm", side, rot, pos);
        out.translate((RIGHT_ARM_PIVOT.x * side + pos.x) / 16.0f, (RIGHT_ARM_PIVOT.y + pos.y) / 16.0f, (RIGHT_ARM_PIVOT.z + pos.z) / 16.0f);
        out.rotateZYX(rot.z, rot.y, rot.x + ITEM_ARM_RAISE);
        out.translate(RIGHT_FIST.x * side / 16.0f, RIGHT_FIST.y / 16.0f, RIGHT_FIST.z / 16.0f);
        return out;
    }

    /** Length of a one-handed blade from the grip to the tip, blocks (the sword sprite at vanilla's third-person scale). */
    public static final float BLADE_LENGTH = 0.8f;
    /** Where a handheld item's blade points in the fist frame with no wrist rotation: the vanilla fist grip. */
    private static final Vector3f BLADE_AT_REST = new Vector3f(0.0f, 0.174f, -0.985f);

    /**
     * The blade's grip and tip in world space for an avatar at {@code x, y, z} with body yaw {@code bodyYaw}
     * (degrees), from the pose: the fist frame, the wrist, then vanilla's entity transform.
     */
    public static Vec3[] bladeInWorld(PoseSource pose, HumanoidArm arm, double x, double y, double z, float bodyYaw) {
        float side = arm == HumanoidArm.LEFT ? -1.0f : 1.0f;
        Matrix4f fist = fistFrame(pose, arm, new Matrix4f());
        Vector3f rot = new Vector3f();
        Vector3f pos = new Vector3f();
        sample(pose, HeldItemPose.ITEM_BONE, side, rot, pos);
        Matrix4f blade = new Matrix4f(fist).translate(pos.x / 16.0f, pos.y / 16.0f, pos.z / 16.0f).rotateZYX(rot.z, rot.y, rot.x);
        Vector3f grip = blade.transformPosition(new Vector3f());
        Vector3f tip = blade.transformPosition(new Vector3f(BLADE_AT_REST).mul(BLADE_LENGTH));
        Matrix4f entity = new Matrix4f().rotateY((180.0f - bodyYaw) * Mth.DEG_TO_RAD).scale(-1.0f, -1.0f, 1.0f).translate(0.0f, -1.501f, 0.0f);
        entity.transformPosition(grip);
        entity.transformPosition(tip);
        return new Vec3[]{new Vec3(x + grip.x, y + grip.y, z + grip.z), new Vec3(x + tip.x, y + tip.y, z + tip.z)};
    }

    /** The blade's direction in the fist frame (arm space at the fist) for the pose's wrist. */
    public static Vector3f bladeDirection(PoseSource pose, HumanoidArm arm, Vector3f out) {
        float side = arm == HumanoidArm.LEFT ? -1.0f : 1.0f;
        Vector3f rot = new Vector3f();
        Vector3f pos = new Vector3f();
        sample(pose, HeldItemPose.ITEM_BONE, side, rot, pos);
        return new Matrix4f().rotateZYX(rot.z, rot.y, rot.x).transformDirection(out.set(BLADE_AT_REST));
    }

    /** The normal of the blade's flat in the fist frame for the pose's wrist (the sprite faces sideways with no wrist). */
    public static Vector3f flatNormal(PoseSource pose, HumanoidArm arm, Vector3f out) {
        float side = arm == HumanoidArm.LEFT ? -1.0f : 1.0f;
        Vector3f rot = new Vector3f();
        Vector3f pos = new Vector3f();
        sample(pose, HeldItemPose.ITEM_BONE, side, rot, pos);
        return new Matrix4f().rotateZYX(rot.z, rot.y, rot.x).transformDirection(out.set(1.0f, 0.0f, 0.0f));
    }

    /** Samples a bone into radians and pixels, mirrored for a left-handed character. */
    static void sample(PoseSource pose, String bone, float side, Vector3f rot, Vector3f pos) {
        if (!pose.hasBone(bone)) {
            rot.set(0.0f);
            pos.set(0.0f);
            return;
        }
        pose.sample(bone, PlayerAnimation.ChannelType.ROTATION, rot);
        pose.sample(bone, PlayerAnimation.ChannelType.POSITION, pos);
        rot.set(rot.x * Mth.DEG_TO_RAD, rot.y * Mth.DEG_TO_RAD * side, rot.z * Mth.DEG_TO_RAD * side);
        pos.x *= side;
    }

    private HumanoidRig() {
    }
}
