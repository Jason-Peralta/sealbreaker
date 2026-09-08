package dev.sealbreaker.combat.kit;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.sealbreaker.combat.SbCombat;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/** Validate the authored JSON table and emit stable runtime data. Tuning never moves into Java. */
@EventBusSubscriber(modid = SbCombat.MOD_ID)
public final class DashDataProvider implements DataProvider {
    private final PackOutput output;

    public DashDataProvider(PackOutput output) {
        this.output = output;
    }

    @SubscribeEvent
    static void gather(GatherDataEvent.Client event) {
        event.createProvider(DashDataProvider::new);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        Path source = Path.of(System.getProperty("sb.combat.dataInput"), "dash.json");
        try (Reader reader = Files.newBufferedReader(source)) {
            DashSettings value = DashSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader)).getOrThrow();
            Path target = output.createPathProvider(PackOutput.Target.DATA_PACK, "sb/kit").json(DashData.ID);
            return DataProvider.saveStable(cache, DashSettings.CODEC, value, target);
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public String getName() {
        return "Sealbreaker dash data";
    }
}
