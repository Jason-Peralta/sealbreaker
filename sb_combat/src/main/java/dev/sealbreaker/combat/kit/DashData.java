package dev.sealbreaker.combat.kit;

import dev.sealbreaker.combat.SbCombat;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.Map;
import java.util.Objects;

/** Reloadable server data, rather than a new cross-mod registry contract for one capability. */
@EventBusSubscriber(modid = SbCombat.MOD_ID)
public final class DashData extends SimpleJsonResourceReloadListener<DashSettings> {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(SbCombat.MOD_ID, "dash");
    private static volatile DashSettings current;

    private DashData() {
        super(DashSettings.CODEC, FileToIdConverter.json("sb/kit"));
    }

    @SubscribeEvent
    static void register(AddServerReloadListenersEvent event) {
        event.addListener(ID, new DashData());
    }

    @Override
    protected void apply(Map<Identifier, DashSettings> data, ResourceManager manager, ProfilerFiller profiler) {
        current = Objects.requireNonNull(data.get(ID), "Missing or invalid data/sb_combat/sb/kit/dash.json");
    }

    public static DashSettings get() {
        return Objects.requireNonNull(current, "Dash data is not loaded");
    }

    @SubscribeEvent
    static void stopped(ServerStoppedEvent event) {
        current = null;
    }
}
