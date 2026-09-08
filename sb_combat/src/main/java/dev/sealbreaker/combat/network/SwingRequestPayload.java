package dev.sealbreaker.combat.network;

import dev.sealbreaker.combat.SbCombat;
import dev.sealbreaker.core.api.combat.AttackContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Input edges only. No client damage, charge duration, position or target is trusted. */
public record SwingRequestPayload(AttackContext context, boolean release, boolean cancel) implements CustomPacketPayload {
    public static final SwingRequestPayload CANCEL = new SwingRequestPayload(AttackContext.TAP, false, true);
    public static final Type<SwingRequestPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(SbCombat.MOD_ID, "swing_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SwingRequestPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.fromCodec(AttackContext.CODEC), SwingRequestPayload::context,
            ByteBufCodecs.BOOL, SwingRequestPayload::release,
            ByteBufCodecs.BOOL, SwingRequestPayload::cancel,
            SwingRequestPayload::new);

    public SwingRequestPayload(AttackContext context, boolean release) {
        this(context, release, false);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
