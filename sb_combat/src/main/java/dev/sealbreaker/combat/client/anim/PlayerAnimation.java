package dev.sealbreaker.combat.client.anim;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A keyframe animation for a humanoid: per bone, rotation (degrees, Java model conventions) and position
 * (pixels) channels, sampled by time in seconds. Parsed from the Bedrock/Blockbench animation JSON shape:
 *
 * <pre>{@code
 * "bones": { "right_arm": { "rotation": { "0.0": [0, 0, 0], "0.2": {"post": [-105, -55, -10], "easing": "easeOutQuad"} } } }
 * }</pre>
 *
 * Bones: {@code head, body, right_arm, left_arm, right_leg, left_leg} for third person, {@code right_arm}
 * (plus {@code camera}, reserved) for first person. Values are applied on top of the vanilla pose.
 */
public final class PlayerAnimation {
    public enum ChannelType { ROTATION, POSITION }

    public record Keyframe(float time, Vector3f value, Easing easing) {
    }

    public static final class Channel {
        private final List<Keyframe> keys;

        Channel(List<Keyframe> keys) {
            List<Keyframe> sorted = new ArrayList<>(keys);
            sorted.sort(Comparator.comparingDouble(Keyframe::time));
            this.keys = List.copyOf(sorted);
        }

        public Vector3f sample(float time, Vector3f out) {
            if (keys.isEmpty()) {
                return out.set(0.0f);
            }
            if (time <= keys.getFirst().time()) {
                return out.set(keys.getFirst().value());
            }
            if (time >= keys.getLast().time()) {
                return out.set(keys.getLast().value());
            }
            int next = 1;
            while (keys.get(next).time() < time) {
                next++;
            }
            Keyframe a = keys.get(next - 1);
            Keyframe b = keys.get(next);
            float span = b.time() - a.time();
            float t = span <= 0.0f ? 1.0f : (time - a.time()) / span;
            if (b.easing() == Easing.CATMULLROM && keys.size() >= 3) {
                Vector3f p0 = keys.get(Math.max(0, next - 2)).value();
                Vector3f p3 = keys.get(Math.min(keys.size() - 1, next + 1)).value();
                return catmullRom(p0, a.value(), b.value(), p3, t, out);
            }
            float e = b.easing().apply(t);
            return out.set(a.value()).lerp(b.value(), e);
        }

        private static Vector3f catmullRom(Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3, float t, Vector3f out) {
            float t2 = t * t;
            float t3 = t2 * t;
            return out.set(
                    0.5f * (2.0f * p1.x + (-p0.x + p2.x) * t + (2.0f * p0.x - 5.0f * p1.x + 4.0f * p2.x - p3.x) * t2 + (-p0.x + 3.0f * p1.x - 3.0f * p2.x + p3.x) * t3),
                    0.5f * (2.0f * p1.y + (-p0.y + p2.y) * t + (2.0f * p0.y - 5.0f * p1.y + 4.0f * p2.y - p3.y) * t2 + (-p0.y + 3.0f * p1.y - 3.0f * p2.y + p3.y) * t3),
                    0.5f * (2.0f * p1.z + (-p0.z + p2.z) * t + (2.0f * p0.z - 5.0f * p1.z + 4.0f * p2.z - p3.z) * t2 + (-p0.z + 3.0f * p1.z - 3.0f * p2.z + p3.z) * t3));
        }
    }

    public record BoneTrack(Channel rotation, Channel position) {
    }

    /** No bones: the vanilla pose, used while a weapon is held but no move plays. */
    public static final PlayerAnimation EMPTY = new PlayerAnimation(0.0f, Map.of());

    private final float length;
    private final Map<String, BoneTrack> bones;

    private PlayerAnimation(float length, Map<String, BoneTrack> bones) {
        this.length = length;
        this.bones = Map.copyOf(bones);
    }

    public float length() {
        return length;
    }

    public boolean hasBone(String bone) {
        return bones.containsKey(bone);
    }

    /** Samples a bone channel; returns zeros for bones or channels the animation does not touch. */
    public Vector3f sample(String bone, ChannelType channel, float time, Vector3f out) {
        BoneTrack track = bones.get(bone);
        if (track == null) {
            return out.set(0.0f);
        }
        Channel c = channel == ChannelType.ROTATION ? track.rotation() : track.position();
        return c == null ? out.set(0.0f) : c.sample(time, out);
    }

    /** Parses one animation object (the value under {@code "animations": { "<name>": ... }}). */
    public static PlayerAnimation parse(JsonObject animation) {
        float length = animation.has("animation_length") ? animation.get("animation_length").getAsFloat() : 0.0f;
        Map<String, BoneTrack> bones = new HashMap<>();
        if (animation.has("bones")) {
            for (Map.Entry<String, JsonElement> bone : animation.getAsJsonObject("bones").entrySet()) {
                JsonObject boneJson = bone.getValue().getAsJsonObject();
                Channel rotation = boneJson.has("rotation") ? parseChannel(boneJson.get("rotation")) : null;
                Channel position = boneJson.has("position") ? parseChannel(boneJson.get("position")) : null;
                bones.put(normalizeBone(bone.getKey()), new BoneTrack(rotation, position));
                for (Keyframe k : rotation == null ? List.<Keyframe>of() : rotation.keys) {
                    length = Math.max(length, k.time());
                }
            }
        }
        return new PlayerAnimation(length, bones);
    }

    private static Channel parseChannel(JsonElement element) {
        List<Keyframe> keys = new ArrayList<>();
        if (element.isJsonArray()) {
            keys.add(new Keyframe(0.0f, vec(element.getAsJsonArray()), Easing.LINEAR));
        } else {
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                float time = Float.parseFloat(entry.getKey());
                JsonElement value = entry.getValue();
                if (value.isJsonArray()) {
                    keys.add(new Keyframe(time, vec(value.getAsJsonArray()), Easing.LINEAR));
                } else {
                    JsonObject key = value.getAsJsonObject();
                    JsonElement post = key.has("post") ? key.get("post") : key.get("pre");
                    Easing easing = Easing.LINEAR;
                    if (key.has("easing")) {
                        easing = Easing.parse(key.get("easing").getAsString());
                    } else if (key.has("lerp_mode")) {
                        easing = Easing.parse(key.get("lerp_mode").getAsString());
                    }
                    keys.add(new Keyframe(time, vec(post.getAsJsonArray()), easing));
                }
            }
        }
        return new Channel(keys);
    }

    private static Vector3f vec(JsonArray array) {
        return new Vector3f(array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat());
    }

    /** Accepts Blockbench's names too: rightArm, torso, RightArm. */
    static String normalizeBone(String name) {
        String snake = name.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase(java.util.Locale.ROOT);
        return switch (snake) {
            case "torso" -> "body";
            case "rightarm" -> "right_arm";
            case "leftarm" -> "left_arm";
            case "rightleg" -> "right_leg";
            case "leftleg" -> "left_leg";
            default -> snake;
        };
    }
}
