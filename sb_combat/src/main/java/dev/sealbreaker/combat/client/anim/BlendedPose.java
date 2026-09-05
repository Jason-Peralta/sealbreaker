package dev.sealbreaker.combat.client.anim;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * A pose faded out of another: {@code from} (whatever was on screen when this one started, frozen) into
 * {@code to} (the source playing now). A combo starts its next move the tick the previous one ends and a
 * move that is not continued settles into the idle stance; both go through here so nothing snaps.
 *
 * @param from   the pose being left behind, or null for none
 * @param to     the pose being entered
 * @param weight 0 = entirely {@code from}, 1 = entirely {@code to}
 */
public record BlendedPose(@Nullable PoseSource from, PoseSource to, float weight) implements PoseSource {
    public static PoseSource of(PlayerAnimation animation, float seconds) {
        return new PoseSource.AnimationAt(animation, seconds);
    }

    @Override
    public boolean hasBone(String bone) {
        return to.hasBone(bone) || (from != null && weight < 1.0f && from.hasBone(bone));
    }

    @Override
    public Vector3f sample(String bone, PlayerAnimation.ChannelType channel, Vector3f out) {
        to.sample(bone, channel, out);
        if (from == null || weight >= 1.0f) {
            return out;
        }
        Vector3f previous = from.sample(bone, channel, new Vector3f());
        return out.set(previous.lerp(out, weight));
    }
}
