package dev.sealbreaker.combat.swing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/**
 * An in-progress move, attached to the attacker and synced to every client that can see it. Server
 * authoritative: the client only ever renders it. Transient by design: not saved, gone on relog.
 *
 * @param archetype the {@code sb:weapon_archetype} entry that defines the moves
 * @param step      index of the move within the archetype's tap combo
 * @param startTick the level's game time on the move's first tick
 */
public record SwingState(Identifier archetype, int step, long startTick) {
    public static final MapCodec<SwingState> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Identifier.CODEC.fieldOf("archetype").forGetter(SwingState::archetype),
            Codec.INT.fieldOf("step").forGetter(SwingState::step),
            Codec.LONG.fieldOf("start_tick").forGetter(SwingState::startTick)
    ).apply(i, SwingState::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SwingState> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, SwingState::archetype,
            ByteBufCodecs.VAR_INT, SwingState::step,
            ByteBufCodecs.VAR_LONG, SwingState::startTick,
            SwingState::new
    );

    public long ticksSince(long gameTime) {
        return gameTime - startTick;
    }
}
