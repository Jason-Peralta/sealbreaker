package dev.sealbreaker.combat.swing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.sealbreaker.core.api.combat.AttackContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/** Transient server-authoritative move, synced to the owner and tracking clients; never saved. */
public record SwingState(Identifier archetype, int step, long startTick, AttackContext context,
                         boolean charging, float damageScale) {
    public static final MapCodec<SwingState> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Identifier.CODEC.fieldOf("archetype").forGetter(SwingState::archetype),
            Codec.INT.fieldOf("step").forGetter(SwingState::step),
            Codec.LONG.fieldOf("start_tick").forGetter(SwingState::startTick),
            AttackContext.CODEC.fieldOf("context").forGetter(SwingState::context),
            Codec.BOOL.fieldOf("charging").forGetter(SwingState::charging),
            Codec.FLOAT.fieldOf("damage_scale").forGetter(SwingState::damageScale)
    ).apply(i, SwingState::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, SwingState> STREAM_CODEC =
            ByteBufCodecs.fromCodec(CODEC.codec()).cast();

    public SwingState(Identifier archetype, int step, long startTick) {
        this(archetype, step, startTick, AttackContext.TAP, false, 1);
    }

    public SwingState withStartTick(long tick) {
        return new SwingState(archetype, step, tick, context, charging, damageScale);
    }

    public long ticksSince(long gameTime) {
        return gameTime - startTick;
    }
}
