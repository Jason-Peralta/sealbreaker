package dev.sealbreaker.combat.network;

import dev.sealbreaker.combat.SbCombat;
import dev.sealbreaker.combat.swing.SwingService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Payload registration. Handlers run on the server main thread. */
@EventBusSubscriber(modid = SbCombat.MOD_ID)
public final class SbCombatNetwork {
    @SubscribeEvent
    static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(DashRequestPayload.TYPE, DashRequestPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                dev.sealbreaker.combat.kit.DashService.request(player);
            }
        });
        registrar.playToServer(SwingRequestPayload.TYPE, SwingRequestPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                SwingService.onSwingRequest(player);
            }
        });
    }

    private SbCombatNetwork() {
    }
}
