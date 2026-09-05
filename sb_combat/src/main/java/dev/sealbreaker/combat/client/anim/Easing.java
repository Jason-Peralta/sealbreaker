package dev.sealbreaker.combat.client.anim;

import java.util.Locale;

/**
 * Easing curves for keyframe segments, named as on easings.net and in Blockbench. A keyframe's easing
 * describes the transition *into* that keyframe from the previous one.
 */
public enum Easing {
    LINEAR,
    EASE_IN_SINE, EASE_OUT_SINE, EASE_IN_OUT_SINE,
    EASE_IN_QUAD, EASE_OUT_QUAD, EASE_IN_OUT_QUAD,
    EASE_IN_CUBIC, EASE_OUT_CUBIC, EASE_IN_OUT_CUBIC,
    EASE_IN_QUART, EASE_OUT_QUART, EASE_IN_OUT_QUART,
    EASE_IN_BACK, EASE_OUT_BACK, EASE_IN_OUT_BACK,
    /** Smooth spline through neighbouring keys (Blockbench "catmullrom" lerp mode). */
    CATMULLROM;

    public float apply(float t) {
        t = Math.max(0.0f, Math.min(1.0f, t));
        return switch (this) {
            case LINEAR, CATMULLROM -> t;
            case EASE_IN_SINE -> 1.0f - (float) Math.cos(t * Math.PI * 0.5);
            case EASE_OUT_SINE -> (float) Math.sin(t * Math.PI * 0.5);
            case EASE_IN_OUT_SINE -> -(float) (Math.cos(Math.PI * t) - 1.0) * 0.5f;
            case EASE_IN_QUAD -> t * t;
            case EASE_OUT_QUAD -> 1.0f - (1.0f - t) * (1.0f - t);
            case EASE_IN_OUT_QUAD -> t < 0.5f ? 2.0f * t * t : 1.0f - (float) Math.pow(-2.0 * t + 2.0, 2) * 0.5f;
            case EASE_IN_CUBIC -> t * t * t;
            case EASE_OUT_CUBIC -> 1.0f - (float) Math.pow(1.0 - t, 3);
            case EASE_IN_OUT_CUBIC -> t < 0.5f ? 4.0f * t * t * t : 1.0f - (float) Math.pow(-2.0 * t + 2.0, 3) * 0.5f;
            case EASE_IN_QUART -> t * t * t * t;
            case EASE_OUT_QUART -> 1.0f - (float) Math.pow(1.0 - t, 4);
            case EASE_IN_OUT_QUART -> t < 0.5f ? 8.0f * t * t * t * t : 1.0f - (float) Math.pow(-2.0 * t + 2.0, 4) * 0.5f;
            case EASE_IN_BACK -> {
                float c1 = 1.70158f;
                yield (c1 + 1.0f) * t * t * t - c1 * t * t;
            }
            case EASE_OUT_BACK -> {
                float c1 = 1.70158f;
                float u = t - 1.0f;
                yield 1.0f + (c1 + 1.0f) * u * u * u + c1 * u * u;
            }
            case EASE_IN_OUT_BACK -> {
                float c2 = 1.70158f * 1.525f;
                yield t < 0.5f
                        ? (float) (Math.pow(2.0 * t, 2) * ((c2 + 1.0) * 2.0 * t - c2)) * 0.5f
                        : (float) (Math.pow(2.0 * t - 2.0, 2) * ((c2 + 1.0) * (t * 2.0 - 2.0) + c2) + 2.0) * 0.5f;
            }
        };
    }

    /** Parses "easeOutQuad", "ease_out_quad", "linear" or "catmullrom"; unknown names fall back to linear. */
    public static Easing parse(String name) {
        if (name == null) {
            return LINEAR;
        }
        String normalized = name.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase(Locale.ROOT);
        for (Easing easing : values()) {
            if (easing.name().equals(normalized)) {
                return easing;
            }
        }
        return LINEAR;
    }
}
