package dev.sealbreaker.combat.network;

import dev.sealbreaker.combat.SbCombat;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client asks the server to start a swing with the main-hand weapon. Carries no data: the server decides everything. */
public record SwingRequestPayload() implements CustomPacketPayload {
    public static final SwingRequestPayload INSTANCE = new SwingRequestPayload();
    public static final Type<SwingRequestPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(SbCombat.MOD_ID, "swing_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SwingRequestPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
