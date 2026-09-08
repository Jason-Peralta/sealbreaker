package dev.sealbreaker.core.datagen;

import dev.sealbreaker.core.SbCore;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.Set;

/**
 * Datagen entry point of the core module ({@code ./gradlew :sb_core:runData}). The run is a client data run, so
 * the client event carries both assets and data; it writes the {@code sb:*} registry entries of
 * {@link SbCoreEntries} under {@code src/generated/resources}, which is committed and shipped.
 */
@EventBusSubscriber(modid = SbCore.MOD_ID)
public final class SbCoreDatagen {
    @SubscribeEvent
    static void gatherData(GatherDataEvent.Client event) {
        event.createDatapackRegistryObjects(SbCoreEntries.builder(), Set.of(SbCore.MOD_ID));
    }

    private SbCoreDatagen() {
    }
}
