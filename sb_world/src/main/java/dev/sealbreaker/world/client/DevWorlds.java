package dev.sealbreaker.world.client;

import dev.sealbreaker.world.SbWorld;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

/** Shared bits of the capture harnesses: creating a fresh default world from the menus without a person. */
final class DevWorlds {
    private static int ticksOnMenus;

    /**
     * True once the client sits on the title screen, or has sat on any menu (first-launch screens included) long
     * enough that the title screen is not coming; the harness may then create its world.
     */
    static boolean readyToCreate(Minecraft minecraft) {
        ticksOnMenus++;
        return minecraft.gui.screen() instanceof TitleScreen || ticksOnMenus > 100;
    }

    /** Creates and joins a new creative, commands-enabled default world with the given seed. */
    static void createFresh(Minecraft minecraft, String name, long seed) {
        LevelSettings settings = new LevelSettings(name, GameType.CREATIVE, LevelSettings.DifficultySettings.DEFAULT, true, WorldDataConfiguration.DEFAULT);
        WorldOptions options = new WorldOptions(seed, true, false);
        SbWorld.LOGGER.info("Dev worlds: creating world '{}' with seed {}", name, seed);
        minecraft.createWorldOpenFlows().createFreshLevel(name, settings, options, WorldPresets::createNormalWorldDimensions, minecraft.gui.screen());
    }

    private DevWorlds() {
    }
}
