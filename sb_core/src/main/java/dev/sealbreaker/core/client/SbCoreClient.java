package dev.sealbreaker.core.client;

import dev.sealbreaker.core.SbCore;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/** Client-only entry point; never loaded on a dedicated server. */
@Mod(value = SbCore.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = SbCore.MOD_ID, value = Dist.CLIENT)
public final class SbCoreClient {
    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        SbCore.LOGGER.info("Sealbreaker core: client setup complete");
    }
}
