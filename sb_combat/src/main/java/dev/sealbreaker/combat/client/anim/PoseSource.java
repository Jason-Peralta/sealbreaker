package dev.sealbreaker.combat.client.anim;

import org.joml.Vector3f;

/**
 * Something the pose code can sample per bone: an animation at a time, or a blend of two sources. All
 * implementations are immutable, so a source can be kept as a frozen snapshot of what was on screen when a
 * move was interrupted and blended out of later.
 */
public interface PoseSource {
    boolean hasBone(String bone);

    /** Samples a channel of a bone, degrees or pixels; zero for a bone the source does not touch. */
    Vector3f sample(String bone, PlayerAnimation.ChannelType channel, Vector3f out);

    /** An animation frozen at, or playing through, a moment in time. */
    record AnimationAt(PlayerAnimation animation, float seconds) implements PoseSource {
        @Override
        public boolean hasBone(String bone) {
            return animation.hasBone(bone);
        }

        @Override
        public Vector3f sample(String bone, PlayerAnimation.ChannelType channel, Vector3f out) {
            return animation.sample(bone, channel, seconds, out);
        }
    }
}
