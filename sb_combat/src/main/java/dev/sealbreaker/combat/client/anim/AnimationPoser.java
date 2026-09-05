package dev.sealbreaker.combat.client.anim;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Matrix3f;
import org.joml.Vector3f;

/**
 * Applies a pose on top of the vanilla third-person pose. Rotations are added in radians, positions in
 * model pixels, so a pose that is zero blends in and out of whatever vanilla is doing (walking, sneaking).
 *
 * <p>The vanilla humanoid is a flat list of parts, so a torso twist would leave the shoulders behind. Here the
 * {@code body} bone rotates about the hips and the arms and the head ride on it: their pivots move with the
 * torso and the arm rotations are composed with the body rotation, the way a rigged model behaves. The legs
 * hang from the hips and are unaffected. Animations are authored for a right-handed character; for a
 * left-handed one the arms are swapped and yaw/roll mirrored.
 *
 * <p>While a move plays the torso also leans with the player's look pitch ({@code lookPitchDegrees} scaled
 * by {@code lookFollow}), so a thrust aimed down at a target goes down, in third person as it does in
 * first person; the head keeps its own pitch, so it does not double up.
 */
public final class AnimationPoser {
    /** How much of the look pitch the torso takes on during a move. */
    private static final float PITCH_FOLLOW = 0.7f;
    private static final String[] LIMBS = {"right_leg", "left_leg"};

    public static void apply(HumanoidModel<?> model, PoseSource pose, HumanoidArm mainArm, float lookPitchDegrees, float lookFollow) {
        boolean mirror = mainArm == HumanoidArm.LEFT;
        float side = mirror ? -1.0f : 1.0f;
        Vector3f rot = new Vector3f();
        Vector3f pos = new Vector3f();

        HumanoidRig.sample(pose, "body", side, rot, pos);
        rot.x += Mth.clamp(lookPitchDegrees, -60.0f, 60.0f) * PITCH_FOLLOW * lookFollow * Mth.DEG_TO_RAD;
        Matrix3f bodyRot = null;
        if (rot.x != 0.0f || rot.y != 0.0f || rot.z != 0.0f) {
            bodyRot = new Matrix3f().rotationZYX(rot.z, rot.y, rot.x);
        }
        model.body.xRot += rot.x;
        model.body.yRot += rot.y;
        model.body.zRot += rot.z;
        // Rotating about the hips instead of the part's own pivot (the neck) means shifting the part.
        Vector3f shift = bodyRot == null ? new Vector3f() : pivotShift(bodyRot, new Vector3f());
        model.body.x += shift.x + pos.x;
        model.body.y += shift.y + pos.y;
        model.body.z += shift.z + pos.z;

        // The head keeps its own rotation (it faces where the player looks) but sits on the moving neck.
        HumanoidRig.sample(pose, "head", side, rot, pos);
        model.head.xRot += rot.x;
        model.head.yRot += rot.y;
        model.head.zRot += rot.z;
        if (bodyRot != null) {
            follow(model.head, bodyRot);
        }
        model.head.x += pos.x;
        model.head.y += pos.y;
        model.head.z += pos.z;

        arm(model.rightArm, pose, mirror ? "left_arm" : "right_arm", side, bodyRot, rot, pos);
        arm(model.leftArm, pose, mirror ? "right_arm" : "left_arm", side, bodyRot, rot, pos);

        for (String bone : LIMBS) {
            ModelPart part = part(model, mirror ? mirrorBone(bone) : bone);
            if (part != null && pose.hasBone(bone)) {
                HumanoidRig.sample(pose, bone, side, rot, pos);
                part.xRot += rot.x;
                part.yRot += rot.y;
                part.zRot += rot.z;
                part.x += pos.x;
                part.y += pos.y;
                part.z += pos.z;
            }
        }
    }

    private static void arm(ModelPart part, PoseSource pose, String bone, float side, Matrix3f bodyRot, Vector3f rot, Vector3f pos) {
        HumanoidRig.sample(pose, bone, side, rot, pos);
        part.xRot += rot.x;
        part.yRot += rot.y;
        part.zRot += rot.z;
        if (bodyRot != null) {
            follow(part, bodyRot);
            Matrix3f composed = new Matrix3f(bodyRot).rotateZYX(part.zRot, part.yRot, part.xRot);
            Vector3f euler = composed.getEulerAnglesZYX(new Vector3f());
            part.setRotation(euler.x, euler.y, euler.z);
        }
        part.x += pos.x;
        part.y += pos.y;
        part.z += pos.z;
    }

    /** Moves a part's pivot as if it were attached to a body rotated about the hips. */
    private static void follow(ModelPart part, Matrix3f bodyRot) {
        Vector3f p = new Vector3f(part.x, part.y, part.z).sub(HumanoidRig.HIP);
        bodyRot.transform(p).add(HumanoidRig.HIP);
        part.setPos(p.x, p.y, p.z);
    }

    /** hip - R * hip: the translation that turns a rotation about the origin into one about the hips. */
    private static Vector3f pivotShift(Matrix3f bodyRot, Vector3f out) {
        Vector3f rotated = bodyRot.transform(new Vector3f(HumanoidRig.HIP));
        return out.set(HumanoidRig.HIP).sub(rotated);
    }

    private static ModelPart part(HumanoidModel<?> model, String bone) {
        return switch (bone) {
            case "right_leg" -> model.rightLeg;
            case "left_leg" -> model.leftLeg;
            default -> null;
        };
    }

    private static String mirrorBone(String bone) {
        return switch (bone) {
            case "right_leg" -> "left_leg";
            case "left_leg" -> "right_leg";
            default -> bone;
        };
    }

    private AnimationPoser() {
    }
}
