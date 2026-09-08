package dev.sealbreaker.combat.kit;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** All dash tuning comes from data; invulnerability is deliberately unavailable in M1. */
public record DashSettings(double distance, int durationTicks, int cooldownTicks, float leanDegrees, boolean invulnerable) {
    public static final Codec<DashSettings> CODEC = RecordCodecBuilder.<DashSettings>create(i -> i.group(
            Codec.doubleRange(Double.MIN_NORMAL, 64).fieldOf("distance").forGetter(DashSettings::distance),
            Codec.intRange(1, 200).fieldOf("duration_ticks").forGetter(DashSettings::durationTicks),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("cooldown_ticks").forGetter(DashSettings::cooldownTicks),
            Codec.floatRange(0, 45).fieldOf("lean_degrees").forGetter(DashSettings::leanDegrees),
            Codec.BOOL.fieldOf("invulnerable").forGetter(DashSettings::invulnerable)
    ).apply(i, DashSettings::new)).validate(value -> value.invulnerable
            ? DataResult.error(() -> "M1 dash has no invulnerability frames")
            : value.cooldownTicks < value.durationTicks
            ? DataResult.error(() -> "cooldown_ticks must cover duration_ticks") : DataResult.success(value));
}
