package dev.sealbreaker.combat.data;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.sealbreaker.combat.SbCombat;
import dev.sealbreaker.core.api.combat.WeaponArchetype;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/** Validate and emit the authored sword table; animation keyframes remain in the existing generator. */
@EventBusSubscriber(modid = SbCombat.MOD_ID)
public final class SwordDataProvider implements DataProvider {
    private final PackOutput output;

    public SwordDataProvider(PackOutput output) {
        this.output = output;
    }

    @SubscribeEvent
    static void gather(GatherDataEvent.Client event) {
        event.createProvider(SwordDataProvider::new);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        Path source = Path.of(System.getProperty("sb.combat.dataInput"), "sword.json");
        try (Reader reader = Files.newBufferedReader(source)) {
            WeaponArchetype value = WeaponArchetype.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader)).getOrThrow();
            Path target = output.createPathProvider(PackOutput.Target.DATA_PACK, "sb/weapon_archetype")
                    .json(Identifier.fromNamespaceAndPath(SbCombat.MOD_ID, "sword"));
            return DataProvider.saveStable(cache, WeaponArchetype.CODEC, value, target);
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public String getName() {
        return "Sealbreaker sword archetype";
    }
}
