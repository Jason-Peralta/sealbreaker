package dev.sealbreaker.combat.network;

import dev.sealbreaker.combat.SbCombat;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** The server supplies everything about the dash; the packet is only an intent. */
public record DashRequestPayload() implements CustomPacketPayload {
    public static final DashRequestPayload INSTANCE = new DashRequestPayload();
    public static final Type<DashRequestPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(SbCombat.MOD_ID, "dash_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DashRequestPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
