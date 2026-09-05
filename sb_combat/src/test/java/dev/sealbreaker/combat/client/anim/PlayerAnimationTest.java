package dev.sealbreaker.combat.client.anim;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Keyframe parsing, sampling and blending, independent of the game. */
class PlayerAnimationTest {
    private static final String JSON = """
            {
              "animation_length": 0.5,
              "bones": {
                "rightArm": {
                  "rotation": {
                    "0.0": [0, 0, 0],
                    "0.2": {"post": [-90, 40, 0], "easing": "linear"},
                    "0.4": {"post": [-90, -40, 0], "easing": "easeInQuad"},
                    "0.5": [0, 0, 0]
                  },
                  "position": [1, 2, 3]
                },
                "right_item": {
                  "rotation": {"0.1": [90, 0, 0]}
                }
              }
            }
            """;

    private static PlayerAnimation animation() {
        JsonObject json = JsonParser.parseString(JSON).getAsJsonObject();
        return PlayerAnimation.parse(json);
    }

    @Test
    void parsesLengthAndBonesUnderJavaNames() {
        PlayerAnimation animation = animation();
        assertEquals(0.5f, animation.length(), 1e-6f);
        assertTrue(animation.hasBone("right_arm"), "Blockbench camelCase bone names are normalised");
        assertTrue(animation.hasBone("right_item"));
        assertFalse(animation.hasBone("left_arm"));
    }

    @Test
    void samplesKeyframesWithEasingAndClamping() {
        PlayerAnimation animation = animation();
        Vector3f out = new Vector3f();
        animation.sample("right_arm", PlayerAnimation.ChannelType.ROTATION, 0.1f, out);
        assertEquals(-45.0f, out.x, 1e-4f, "linear midway between the first two keys");
        assertEquals(20.0f, out.y, 1e-4f);
        animation.sample("right_arm", PlayerAnimation.ChannelType.ROTATION, 0.3f, out);
        assertEquals(-90.0f, out.x, 1e-4f);
        assertEquals(20.0f, out.y, 1e-4f, "easeInQuad at t=0.5 is 0.25 of the way: 40 - 80 * 0.25");
        animation.sample("right_arm", PlayerAnimation.ChannelType.ROTATION, 9.0f, out);
        assertEquals(0.0f, out.x, 1e-6f, "past the last key holds the last value");
        animation.sample("right_arm", PlayerAnimation.ChannelType.POSITION, 0.25f, out);
        assertEquals(new Vector3f(1.0f, 2.0f, 3.0f), out, "a bare array is a constant channel");
        animation.sample("right_item", PlayerAnimation.ChannelType.ROTATION, 0.0f, out);
        assertEquals(90.0f, out.x, 1e-6f, "before the first key holds the first value");
        animation.sample("left_leg", PlayerAnimation.ChannelType.ROTATION, 0.2f, out);
        assertEquals(new Vector3f(), out, "absent bones sample as zero");
    }

    @Test
    void blendFadesTheInterruptedPoseIntoTheNewOne() {
        PlayerAnimation animation = animation();
        // Interrupted at 0.2 s (arm at -90, 40); the new move at its start (rest), a third of the way blended in.
        PoseSource pose = new BlendedPose(new PoseSource.AnimationAt(animation, 0.2f), new PoseSource.AnimationAt(animation, 0.0f), 1.0f / 3.0f);
        Vector3f out = pose.sample("right_arm", PlayerAnimation.ChannelType.ROTATION, new Vector3f());
        assertEquals(-60.0f, out.x, 1e-4f);
        assertEquals(40.0f * 2.0f / 3.0f, out.y, 1e-4f);
        PoseSource done = new BlendedPose(new PoseSource.AnimationAt(animation, 0.2f), new PoseSource.AnimationAt(animation, 0.0f), 1.0f);
        assertEquals(0.0f, done.sample("right_arm", PlayerAnimation.ChannelType.ROTATION, new Vector3f()).x, 1e-6f,
                "at full weight only the new animation counts");
        // A frozen blend can itself be blended out of, which is how a move fades in over a fading idle.
        PoseSource nested = new BlendedPose(pose, new PoseSource.AnimationAt(animation, 0.2f), 0.5f);
        assertEquals(-75.0f, nested.sample("right_arm", PlayerAnimation.ChannelType.ROTATION, new Vector3f()).x, 1e-4f);
        assertTrue(BlendedPose.of(animation, 0.0f).hasBone("right_item"));
    }
}
