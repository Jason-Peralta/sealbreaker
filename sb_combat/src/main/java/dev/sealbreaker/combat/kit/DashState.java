package dev.sealbreaker.combat.kit;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Only the animation timing is synced; no saved state or client-authoritative movement. */
public record DashState(long startTick, int durationTicks, float leanDegrees) {
    public static final Codec<DashState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.fieldOf("start_tick").forGetter(DashState::startTick),
            Codec.INT.fieldOf("duration_ticks").forGetter(DashState::durationTicks),
            Codec.FLOAT.fieldOf("lean_degrees").forGetter(DashState::leanDegrees)
    ).apply(i, DashState::new));

    public float leanAt(float gameTime) {
        float progress = (gameTime - startTick) / durationTicks;
        return progress < 0 || progress >= 1 ? 0 : (float) Math.sin(progress * Math.PI) * leanDegrees;
    }
}
