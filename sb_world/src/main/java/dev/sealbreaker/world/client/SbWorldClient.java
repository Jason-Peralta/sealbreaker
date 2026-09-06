package dev.sealbreaker.world.client;

import dev.sealbreaker.world.SbWorld;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Client hooks of the world module. Milestone 0: only the capture harnesses. */
@EventBusSubscriber(modid = SbWorld.MOD_ID, value = Dist.CLIENT)
public final class SbWorldClient {
    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        if (WorldDebug.enabled()) {
            WorldDebug.tick();
        }
        if (PortalDebug.enabled()) {
            PortalDebug.tick();
        }
    }

    @SubscribeEvent
    static void onChat(ClientChatReceivedEvent event) {
        if (PortalDebug.enabled()) {
            PortalDebug.onChat(event);
        }
    }

    private SbWorldClient() {
    }
}
