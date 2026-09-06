package dev.sealbreaker.gear.client;

import dev.sealbreaker.gear.SbGear;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

/** Client hooks of the gear module. Milestone 0: only the presentation capture harness. */
@EventBusSubscriber(modid = SbGear.MOD_ID, value = Dist.CLIENT)
public final class SbGearClient {
    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        if (GearDebug.enabled()) {
            GearDebug.tick();
        }
    }

    @SubscribeEvent
    static void onScreenRender(ScreenEvent.Render.Pre event) {
        if (GearDebug.enabled()) {
            GearDebug.drawTooltip(event);
        }
    }

    private SbGearClient() {
    }
}
