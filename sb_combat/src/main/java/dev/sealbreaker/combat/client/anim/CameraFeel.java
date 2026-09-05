package dev.sealbreaker.combat.client.anim;

import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * What the local player's camera does during a move, on top of the look direction: the animation's
 * {@code camera} bone (pitch, yaw, roll in degrees, blended like every other bone: the sway a swing gives
 * the head, the dip of a lunge) and a short decaying kick when a hit lands. Both are presentation only;
 * the player's look direction and the hit test never see them.
 */
public final class CameraFeel {
    public static final String CAMERA_BONE = "camera";
    /** Degrees of pitch the first impact frame kicks; roll gets a little over half of it. */
    private static final float KICK_PITCH = 1.6f;
    private static final float KICK_ROLL = 0.9f;
    private static final float KICK_TICKS = 5.0f;

    @Nullable
    private static PoseSource pose;
    private static float kickStartTicks = Float.NEGATIVE_INFINITY;
    private static float kickScale = 1.0f;

    /** The pose the local player is showing this frame, or null when no weapon is in hand. */
    public static void setPose(@Nullable PoseSource current) {
        pose = current;
    }

    /** A hit landed: kick the camera. Heavier moves pass a larger scale. */
    public static void kick(float nowTicks, float scale) {
        kickStartTicks = nowTicks;
        kickScale = scale;
    }

    /** Pitch, yaw, roll offsets in degrees for the given game time (with the partial tick). */
    public static Vector3f offsets(float nowTicks, Vector3f out) {
        out.set(0.0f);
        if (pose != null && pose.hasBone(CAMERA_BONE)) {
            pose.sample(CAMERA_BONE, PlayerAnimation.ChannelType.ROTATION, out);
        }
        float age = nowTicks - kickStartTicks;
        if (age >= 0.0f && age < KICK_TICKS) {
            float decay = 1.0f - age / KICK_TICKS;
            float wave = Mth.sin(age * Mth.PI * 0.9f);
            out.x += KICK_PITCH * kickScale * decay * decay * wave;
            out.z += KICK_ROLL * kickScale * decay * Mth.cos(age * Mth.PI * 0.6f);
        }
        return out;
    }

    private CameraFeel() {
    }
}
