package dev.sealbreaker.core.api.combat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Data-driven multiplier: min + (max - min) * pow(clamped held ticks / charge ticks, exponent). */
public record ChargeCurve(float min, float max, float exponent) {
    /** Missing charge data must not introduce a balance bonus. */
    public static final ChargeCurve IDENTITY = new ChargeCurve(1, 1, 1);
    public static final Codec<ChargeCurve> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.floatRange(0, Float.MAX_VALUE).fieldOf("min").forGetter(ChargeCurve::min),
            Codec.floatRange(0, Float.MAX_VALUE).fieldOf("max").forGetter(ChargeCurve::max),
            Codec.floatRange(Float.MIN_NORMAL, Float.MAX_VALUE).fieldOf("exponent").forGetter(ChargeCurve::exponent)
    ).apply(i, ChargeCurve::new));

    public float scale(long heldTicks, int chargeTicks) {
        double progress = Math.clamp((double) heldTicks / chargeTicks, 0, 1);
        return (float) (min + (max - min) * Math.pow(progress, exponent));
    }
}
