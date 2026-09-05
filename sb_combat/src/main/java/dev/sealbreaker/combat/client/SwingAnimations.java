package dev.sealbreaker.combat.client;

import dev.sealbreaker.core.api.combat.SwingMove;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

/**
 * Procedural swing poses that follow the hit region exactly: a left-to-right sweep cocks the arm to the
 * left during wind-up, travels across the arc during the active window, and settles during recovery; an
 * overhead raises the arm during wind-up and chops down during the active window. Milestone 1 replaces
 * the maths with Blockbench keyframes; the phase structure stays.
 *
 * <p>Model conventions (from the vanilla humanoid model): forward is -Z, up is -Y, the right hand is at
 * -X. A negative {@code xRot} raises an arm forward; a positive {@code yRot} moves a forward-pointing arm
 * toward the character's right; a positive body {@code yRot} twists the torso to the right.
 */
public final class SwingAnimations {
    // Third-person pose targets, radians.
    private static final float SWEEP_ARM_X = -1.35f;      // arm horizontal, pointing forward
    private static final float SWEEP_ARM_YAW = 0.95f;     // how far to each side the sweep travels
    private static final float SWEEP_BODY_YAW = 0.35f;
    private static final float OVERHEAD_ARM_UP_X = -3.0f;  // raised above the head
    private static final float OVERHEAD_ARM_DOWN_X = -0.55f;
    private static final float OVERHEAD_BODY_BACK_X = -0.15f;
    private static final float OVERHEAD_BODY_FORWARD_X = 0.3f;
    private static final float THRUST_ARM_BACK_X = 0.3f;     // hand drawn back beside the hip
    private static final float THRUST_ARM_OUT_X = -1.6f;     // arm driven out level
    private static final float THRUST_BODY_COIL_YAW = 0.35f;

    /** Third person: layered on top of the vanilla pose at the tail of {@code HumanoidModel#setupAnim}. */
    public static void poseThirdPerson(HumanoidModel<?> model, HumanoidRenderState state) {
        SwingRenderData data = state.getRenderData(SwingRenderData.KEY);
        if (data == null) {
            return;
        }
        boolean right = state.mainArm != HumanoidArm.LEFT;
        ModelPart arm = right ? model.rightArm : model.leftArm;
        float side = right ? 1.0f : -1.0f;
        SwingMove move = data.move();
        float windup = data.windupProgress();
        float active = data.activeProgress();
        float recovery = data.recoveryProgress();

        if (move.shape() == SwingMove.Shape.SWEEP) {
            // Sign of the sweep: left-to-right starts on the character's left (negative yaw) and ends on the right.
            float startYaw = (move.direction() == SwingMove.Direction.RIGHT_TO_LEFT ? SWEEP_ARM_YAW : -SWEEP_ARM_YAW) * side;
            float endYaw = -startYaw;
            float startBody = startYaw / SWEEP_ARM_YAW * SWEEP_BODY_YAW;
            float endBody = -startBody;
            if (windup >= 0.0f) {
                float t = easeOut(windup);
                arm.xRot = Mth.lerp(t, arm.xRot, SWEEP_ARM_X);
                arm.yRot = Mth.lerp(t, arm.yRot, startYaw);
                model.body.yRot = Mth.lerp(t, model.body.yRot, startBody);
                model.head.yRot = Mth.lerp(t, model.head.yRot, -startBody * 0.5f);
            } else if (active >= 0.0f) {
                float t = easeInOut(active);
                arm.xRot = SWEEP_ARM_X + 0.15f * Mth.sin(t * Mth.PI);
                arm.yRot = Mth.lerp(t, startYaw, endYaw);
                model.body.yRot = Mth.lerp(t, startBody, endBody);
                model.head.yRot = Mth.lerp(t, -startBody * 0.5f, -endBody * 0.5f);
            } else if (recovery >= 0.0f) {
                float t = 1.0f - easeOut(recovery);
                arm.xRot = Mth.lerp(t, arm.xRot, SWEEP_ARM_X);
                arm.yRot = Mth.lerp(t, arm.yRot, endYaw);
                model.body.yRot = Mth.lerp(t, model.body.yRot, endBody);
            }
        } else if (move.shape() == SwingMove.Shape.THRUST) {
            // Draw the hand back to the hip, then drive it out level; the blade stays forward the whole way.
            if (windup >= 0.0f) {
                float t = easeOut(windup);
                arm.xRot = Mth.lerp(t, arm.xRot, THRUST_ARM_BACK_X);
                arm.yRot = Mth.lerp(t, arm.yRot, 0.0f);
                model.body.yRot = Mth.lerp(t, model.body.yRot, THRUST_BODY_COIL_YAW * side);
            } else if (active >= 0.0f) {
                float t = easeOut(active);
                arm.xRot = Mth.lerp(t, THRUST_ARM_BACK_X, THRUST_ARM_OUT_X);
                arm.yRot = 0.0f;
                model.body.yRot = Mth.lerp(t, THRUST_BODY_COIL_YAW * side, -THRUST_BODY_COIL_YAW * side);
            } else if (recovery >= 0.0f) {
                float t = 1.0f - easeOut(recovery);
                arm.xRot = Mth.lerp(t, arm.xRot, THRUST_ARM_OUT_X);
                model.body.yRot = Mth.lerp(t, model.body.yRot, -THRUST_BODY_COIL_YAW * side);
            }
        } else {
            if (windup >= 0.0f) {
                float t = easeOut(windup);
                arm.xRot = Mth.lerp(t, arm.xRot, OVERHEAD_ARM_UP_X);
                arm.yRot = Mth.lerp(t, arm.yRot, 0.0f);
                model.body.xRot = Mth.lerp(t, model.body.xRot, OVERHEAD_BODY_BACK_X);
                model.head.xRot = Mth.lerp(t, model.head.xRot, -0.2f);
            } else if (active >= 0.0f) {
                float t = easeIn(active);
                arm.xRot = Mth.lerp(t, OVERHEAD_ARM_UP_X, OVERHEAD_ARM_DOWN_X);
                arm.yRot = 0.0f;
                model.body.xRot = Mth.lerp(t, OVERHEAD_BODY_BACK_X, OVERHEAD_BODY_FORWARD_X);
                model.head.xRot = Mth.lerp(t, -0.2f, 0.25f);
            } else if (recovery >= 0.0f) {
                float t = 1.0f - easeOut(recovery);
                arm.xRot = Mth.lerp(t, arm.xRot, OVERHEAD_ARM_DOWN_X);
                model.body.xRot = Mth.lerp(t, model.body.xRot, OVERHEAD_BODY_FORWARD_X);
                model.head.xRot = Mth.lerp(t, model.head.xRot, 0.25f);
            }
        }
        // The torso pivot drives the jacket/sleeve copies in the player model; keep the other arm honest.
        if (move.shape() == SwingMove.Shape.SWEEP) {
            ModelPart other = right ? model.leftArm : model.rightArm;
            other.yRot += model.body.yRot;
        }
    }


    static float easeOut(float t) {
        return 1.0f - (1.0f - t) * (1.0f - t);
    }

    static float easeIn(float t) {
        return t * t;
    }

    static float easeInOut(float t) {
        return t < 0.5f ? 2.0f * t * t : 1.0f - 2.0f * (1.0f - t) * (1.0f - t);
    }

    private SwingAnimations() {
    }
}
