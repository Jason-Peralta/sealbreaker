package dev.sealbreaker.combat.swing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.sealbreaker.core.api.combat.AttackContext;
import dev.sealbreaker.core.api.combat.SwingMove;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/** Transient server-authoritative move, synced to the owner and tracking clients; never saved. */
public record SwingState(Identifier archetype, int step, long startTick, AttackContext context,
                         boolean charging, float damageScale, long landingTick) {
    public static final MapCodec<SwingState> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Identifier.CODEC.fieldOf("archetype").forGetter(SwingState::archetype),
            Codec.INT.fieldOf("step").forGetter(SwingState::step),
            Codec.LONG.fieldOf("start_tick").forGetter(SwingState::startTick),
            AttackContext.CODEC.fieldOf("context").forGetter(SwingState::context),
            Codec.BOOL.fieldOf("charging").forGetter(SwingState::charging),
            Codec.FLOAT.fieldOf("damage_scale").forGetter(SwingState::damageScale),
            Codec.LONG.fieldOf("landing_tick").forGetter(SwingState::landingTick)
    ).apply(i, SwingState::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, SwingState> STREAM_CODEC =
            ByteBufCodecs.fromCodec(CODEC.codec()).cast();

    public SwingState(Identifier archetype, int step, long startTick, AttackContext context, boolean charging, float damageScale) {
        this(archetype, step, startTick, context, charging, damageScale, -1);
    }

    public SwingState landed(long tick) {
        return new SwingState(archetype, step, startTick, context, charging, damageScale, tick);
    }

    /** Integer server clock, including worlds older than float tick precision. */
    public long moveTicks(SwingMove move, long now) {
        if (move.shape() != SwingMove.Shape.PLUNGE) {
            return now - startTick;
        }
        if (landingTick >= 0) {
            return move.windupTicks() + move.activeTicks() + Math.max(0, now - landingTick - move.hitstopTicks());
        }
        return Math.min(now - startTick, (long) move.windupTicks() + move.activeTicks() - 1);
    }

    /** Plunges hold the last active pose until landing, then pause at impact before recovering. */
    public float animationTicks(SwingMove move, long now, float partial) {
        if (move.shape() != SwingMove.Shape.PLUNGE) {
            return now - startTick + partial;
        }
        if (landingTick >= 0) {
            return move.windupTicks() + move.activeTicks() + Math.max(0, now - landingTick - move.hitstopTicks() + partial);
        }
        return Math.min(now - startTick + partial, move.windupTicks() + move.activeTicks() - 1);
    }

    public SwingState(Identifier archetype, int step, long startTick) {
        this(archetype, step, startTick, AttackContext.TAP, false, 1);
    }

    public SwingState withStartTick(long tick) {
        return new SwingState(archetype, step, tick, context, charging, damageScale, landingTick);
    }

    public long ticksSince(long gameTime) {
        return gameTime - startTick;
    }
}
