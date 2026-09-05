package dev.sealbreaker.combat.client.anim;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.sealbreaker.combat.SbCombat;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.HashMap;
import java.util.Map;

/**
 * Client registry of player animations, loaded from {@code assets/<namespace>/sb_animations/player/*.json}
 * and reloaded with F3+T. A file may hold several animations under {@code "animations"}; each is registered as
 * {@code <namespace>:<animation name>}, so a file name is free to group them (for example {@code sword.json}).
 */
public final class PlayerAnimations extends SimpleJsonResourceReloadListener<JsonElement> {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(SbCombat.MOD_ID, "player_animations");
    private static Map<Identifier, PlayerAnimation> animations = Map.of();

    public PlayerAnimations() {
        super(ExtraCodecs.JSON, FileToIdConverter.json("sb_animations/player"));
    }

    public static PlayerAnimation get(Identifier id) {
        return animations.get(id);
    }

    public static Map<Identifier, PlayerAnimation> all() {
        return animations;
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<Identifier, PlayerAnimation> loaded = new HashMap<>();
        for (Map.Entry<Identifier, JsonElement> file : files.entrySet()) {
            try {
                JsonObject root = file.getValue().getAsJsonObject();
                JsonObject list = root.getAsJsonObject("animations");
                if (list == null) {
                    SbCombat.LOGGER.warn("Player animation file {} has no \"animations\" object", file.getKey());
                    continue;
                }
                for (Map.Entry<String, JsonElement> entry : list.entrySet()) {
                    Identifier id = Identifier.fromNamespaceAndPath(file.getKey().getNamespace(), entry.getKey());
                    loaded.put(id, PlayerAnimation.parse(entry.getValue().getAsJsonObject()));
                }
            } catch (RuntimeException e) {
                SbCombat.LOGGER.error("Failed to parse player animation file {}", file.getKey(), e);
            }
        }
        animations = Map.copyOf(loaded);
        FirstPersonWeaponRenderer.reset();
        SbCombat.LOGGER.info("Loaded {} player animations", animations.size());
    }
}
