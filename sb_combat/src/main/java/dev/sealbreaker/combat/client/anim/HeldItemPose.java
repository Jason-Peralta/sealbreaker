package dev.sealbreaker.combat.client.anim;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Places the main-hand item in the hand of an animated arm, in the arm's own space (after
 * {@code translateToHand}): the animation's {@code right_item} bone is a wrist that pivots at the middle of the
 * fist, then vanilla's grip orientation follows.
 *
 * <p>The grip. Vanilla attaches a handheld item at the very end of the arm, so the handle's middle sits on the
 * fist's bottom edge and a blade laid along the forearm looks glued to the arm. Here the item is raised two
 * pixels so the handle's middle sits in the centre of the fist (measured from the sword sprite: the handle is
 * 5.4 scaled pixels below the sprite's centre along the blade, and vanilla puts that centre 4 pixels beyond the
 * grip point), the pommel pokes a pixel out of the back of the fist and the guard clears its front. Rotating
 * about that point keeps the handle inside the fist whatever the wrist does. Animations should keep the blade
 * at least 25-30 degrees off the forearm, as a real grip does: the blade leaves the fist beside the thumb, not
 * out of the end of the arm.
 *
 * <p>Wrist conventions, verified with the animation debugger (degrees; applied X first, then Y, then Z like
 * every other bone): with no rotation the blade of a handheld item sticks up perpendicular to the forearm,
 * the vanilla fist grip. {@code x = 90} lays the blade along the forearm, {@code x = 180} folds it back down
 * the other side. {@code y} then twists the flat of the blade about its length (a flat sprite is invisible
 * edge-on, so this is how the blade is angled toward a camera). {@code z} then cocks the blade sideways in the
 * plane of the forearm and the character's front, positive toward the character's right: the lag and whip of
 * a horizontal cut.
 */
public final class HeldItemPose {
    public static final String ITEM_BONE = "right_item";
    /** Vanilla's grip point sits this far in front of the arm's axis; with the fist centre as origin it puts the handle's middle in the fist. */
    private static final float GRIP_FORWARD = 1.0f;

    /** From the arm's pivot: moves to the fist centre, then {@link #applyAtFist}. */
    public static void apply(PoseStack poseStack, PoseSource pose, HumanoidArm arm) {
        float side = arm == HumanoidArm.LEFT ? -1.0f : 1.0f;
        poseStack.translate(HumanoidRig.RIGHT_FIST.x * side / 16.0f, HumanoidRig.RIGHT_FIST.y / 16.0f, HumanoidRig.RIGHT_FIST.z / 16.0f);
        applyAtFist(poseStack, pose, arm);
    }

    /** From the fist centre, oriented like the arm: the wrist, then vanilla's grip orientation. */
    public static void applyAtFist(PoseStack poseStack, PoseSource pose, HumanoidArm arm) {
        float side = arm == HumanoidArm.LEFT ? -1.0f : 1.0f;
        Vector3f rot = pose.sample(ITEM_BONE, PlayerAnimation.ChannelType.ROTATION, new Vector3f());
        Vector3f pos = pose.sample(ITEM_BONE, PlayerAnimation.ChannelType.POSITION, new Vector3f());
        poseStack.translate(pos.x * side / 16.0f, pos.y / 16.0f, pos.z / 16.0f);
        if (rot.x != 0.0f || rot.y != 0.0f || rot.z != 0.0f) {
            poseStack.mulPose(new Quaternionf().rotationZYX(rot.z * side * Mth.DEG_TO_RAD, rot.y * side * Mth.DEG_TO_RAD, rot.x * Mth.DEG_TO_RAD));
        }
        poseStack.translate(0.0f, 0.0f, -GRIP_FORWARD / 16.0f);
        // Vanilla's grip orientation (ItemInHandLayer#submitArmWithItem), minus the hand offset applied above.
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0f));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));
    }

    private HeldItemPose() {
    }
}
