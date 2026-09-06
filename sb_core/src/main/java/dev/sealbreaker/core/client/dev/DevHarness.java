package dev.sealbreaker.core.client.dev;

import dev.sealbreaker.core.SbCore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;

import java.util.Locale;

/**
 * Shared plumbing for the dev capture harnesses (dev only, enabled by a module's own system property): creating a
 * fresh world from the menus, opening it to LAN for a second offline dev client, a chat protocol that tells the
 * guest when to screenshot and when to quit, and command-text helpers.
 *
 * <p>The multiplayer verification path until a person accepts a dedicated server's EULA: the integrated server
 * published to LAN runs the same player list, levels and packets as a dedicated one.
 */
public final class DevHarness {
    public static final String CAPTURE = "sb:capture ";
    public static final String QUIT = "sb:quit";
    private static int ticksOnMenus;

    /**
     * True once the client sits on the title screen, or has sat on any menu (first-launch screens included) long
     * enough that the title screen is not coming; the harness may then create its world.
     */
    public static boolean readyToCreate(Minecraft minecraft) {
        ticksOnMenus++;
        return minecraft.gui.screen() instanceof TitleScreen || ticksOnMenus > 100;
    }

    /** Creates and joins a new creative, commands-enabled default world with the given seed. */
    public static void createFresh(Minecraft minecraft, String name, long seed) {
        LevelSettings settings = new LevelSettings(name, GameType.CREATIVE, LevelSettings.DifficultySettings.DEFAULT, true, WorldDataConfiguration.DEFAULT);
        WorldOptions options = new WorldOptions(seed, true, false);
        SbCore.LOGGER.info("Dev harness: creating world '{}' with seed {}", name, seed);
        minecraft.createWorldOpenFlows().createFreshLevel(name, settings, options, WorldPresets::createNormalWorldDimensions, minecraft.gui.screen());
    }

    /** Opens the integrated server to LAN on the port, without authentication so an offline dev client can join. */
    public static boolean publishLan(IntegratedServer server, int port) {
        boolean published = server.publishServer(MinecraftServer.MultiplayerScope.LAN, GameType.CREATIVE, true, port);
        server.setUsesAuthentication(false);
        SbCore.LOGGER.info("Dev harness: LAN {} on port {}, authentication off", published ? "published" : "NOT published", port);
        return published;
    }

    /** Screenshots as {@code <prefix>_<name>.png} and tells the guest to do the same. */
    public static void capture(Minecraft minecraft, LocalPlayer player, String prefix, String name) {
        Screenshot.grab(minecraft.gameDirectory, prefix + "_" + name + ".png", minecraft.gameRenderer.mainRenderTarget(), 1, c -> {
        });
        player.connection.sendCommand("say " + CAPTURE + name);
    }

    /** Guest side: screenshots on the host's capture announcements, quits on request. */
    public static void onGuestChat(ClientChatReceivedEvent event, String prefix) {
        String text = event.getMessage().getString();
        Minecraft minecraft = Minecraft.getInstance();
        int at = text.indexOf(CAPTURE);
        if (at >= 0) {
            String name = text.substring(at + CAPTURE.length()).trim();
            SbCore.LOGGER.info("Dev harness guest: capturing '{}'", name);
            Screenshot.grab(minecraft.gameDirectory, prefix + "_" + name + ".png", minecraft.gameRenderer.mainRenderTarget(), 1, c -> {
            });
        } else if (text.contains(QUIT)) {
            SbCore.LOGGER.info("Dev harness guest: done, quitting");
            minecraft.stop();
        }
    }

    /** Block-centre coordinates as command text; an integer plus ".5" is wrong for negative coordinates (block -2 is centred on -1.5). */
    public static String centre(BlockPos pos) {
        return String.format(Locale.ROOT, "%.1f %d %.1f", pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
    }

    public static String centreLook(BlockPos pos) {
        return String.format(Locale.ROOT, "%.1f %.1f %.1f", pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    /** The first air block above solid ground (leaves do not count) in the column of {@code pos}. */
    public static BlockPos ground(Minecraft minecraft, BlockPos pos) {
        return new BlockPos(pos.getX(), minecraft.level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ()), pos.getZ());
    }

    private DevHarness() {
    }
}
