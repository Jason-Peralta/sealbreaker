package dev.sealbreaker.world.client;

import com.mojang.datafixers.util.Pair;
import dev.sealbreaker.world.SbWorld;
import dev.sealbreaker.world.block.SbWorldBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.Locale;

/**
 * Capture harness for the structure spike (S5), enabled by the {@code sb.worlddebug} system property holding a
 * world seed (the {@code clientWorld} run). From the title screen it creates a fresh default world for that seed,
 * asks the integrated server for the nearest {@code sb_world:spike} start, teleports the player to look at it,
 * screenshots it once the chunks are in, logs where it was, and quits. Run it once per seed for a seed list.
 */
public final class WorldDebug {
    private static final Identifier STRUCTURE = Identifier.fromNamespaceAndPath(SbWorld.MOD_ID, "spike");
    private static final int SEARCH_RADIUS_CHUNKS = 64;
    private static final int SETTLE_TICKS = 100;

    private static int ticksOnMenus;
    private static boolean creating;
    private static boolean searching;
    private static volatile BlockPos found;
    private static volatile BoundingBox bounds;
    private static volatile boolean notFound;
    private static volatile BlockPos door;
    private static volatile Direction doorFacing;
    private static int ticksSinceTeleport = -1;
    private static boolean done;

    public static boolean enabled() {
        return System.getProperty("sb.worlddebug") != null;
    }

    private static long seed() {
        return Long.parseLong(System.getProperty("sb.worlddebug"));
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (done) {
            return;
        }
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            // The title screen, or whatever first-launch screen sits in front of it on a fresh run directory.
            ticksOnMenus++;
            if (!creating && (minecraft.gui.screen() instanceof TitleScreen || ticksOnMenus > 100)) {
                creating = true;
                createWorld(minecraft, seed());
            }
            return;
        }
        IntegratedServer server = minecraft.getSingleplayerServer();
        if (server == null) {
            return;
        }
        if (!searching) {
            searching = true;
            server.execute(() -> locate(server.overworld(), player.blockPosition()));
            return;
        }
        if (notFound) {
            SbWorld.LOGGER.info("World debug: seed {}: no sb_world:spike within {} chunks of spawn, quitting", seed(), SEARCH_RADIUS_CHUNKS);
            finish(minecraft);
            return;
        }
        if (found == null) {
            return;
        }
        if (ticksSinceTeleport < 0) {
            ticksSinceTeleport = 0;
            BoundingBox box = bounds;
            BlockPos look = box != null ? box.getCenter() : found;
            int size = box != null ? Math.max(box.getXSpan(), box.getZSpan()) : 24;
            int x = look.getX() + size * 2 / 3, y = look.getY() + size / 2 + 4, z = look.getZ() + size * 2 / 3;
            player.connection.sendCommand("gamemode spectator");
            player.connection.sendCommand(String.format(Locale.ROOT, "tp @s %d %d %d facing %d %d %d", x, y, z, look.getX(), look.getY(), look.getZ()));
            player.connection.sendCommand("time set 6000");
            player.connection.sendCommand("weather clear");
            SbWorld.LOGGER.info("World debug: seed {}: sb_world:spike at {} (bounds {}), door at {} facing {}, viewing from {} {} {}", seed(), found, box, door, doorFacing, x, y, z);
            return;
        }
        ticksSinceTeleport++;
        if (ticksSinceTeleport == SETTLE_TICKS) {
            Screenshot.grab(minecraft.gameDirectory, String.format(Locale.ROOT, "s5_seed%d.png", seed()), minecraft.gameRenderer.mainRenderTarget(), 1, c -> {
            });
        } else if (ticksSinceTeleport == SETTLE_TICKS + 5) {
            BlockPos at = door;
            if (at == null) {
                finish(minecraft);
                return;
            }
            // Stand in the corridor, four blocks in front of the door, looking straight at it; the corridor is unlit.
            BlockPos from = at.relative(doorFacing, 4);
            player.connection.sendCommand("effect give @s minecraft:night_vision 1000 0 true");
            player.connection.sendCommand(String.format(Locale.ROOT, "tp @s %d.5 %d.0 %d.5 %.1f 0.0",
                    from.getX(), from.getY(), from.getZ(), doorFacing.getOpposite().toYRot()));
        } else if (ticksSinceTeleport == SETTLE_TICKS * 2) {
            Screenshot.grab(minecraft.gameDirectory, String.format(Locale.ROOT, "s5_seed%d_door.png", seed()), minecraft.gameRenderer.mainRenderTarget(), 1, c -> {
            });
        } else if (ticksSinceTeleport == SETTLE_TICKS * 2 + 5) {
            finish(minecraft);
        }
    }

    private static void createWorld(Minecraft minecraft, long seed) {
        String name = "Spike S5 seed " + seed;
        LevelSettings settings = new LevelSettings(name, GameType.CREATIVE, LevelSettings.DifficultySettings.DEFAULT, true, WorldDataConfiguration.DEFAULT);
        WorldOptions options = new WorldOptions(seed, true, false);
        SbWorld.LOGGER.info("World debug: creating world '{}'", name);
        minecraft.createWorldOpenFlows().createFreshLevel(name, settings, options, WorldPresets::createNormalWorldDimensions, minecraft.gui.screen());
    }

    /** Server thread: the nearest start of our structure, and its bounding box once its chunk is loaded. */
    private static void locate(ServerLevel level, BlockPos from) {
        Holder<Structure> structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE)
                .getOrThrow(ResourceKey.create(Registries.STRUCTURE, STRUCTURE));
        Pair<BlockPos, Holder<Structure>> nearest = level.getChunkSource().getGenerator()
                .findNearestMapStructure(level, HolderSet.direct(structure), from, SEARCH_RADIUS_CHUNKS, false);
        if (nearest == null) {
            notFound = true;
            return;
        }
        BlockPos pos = nearest.getFirst();
        level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
        // The locate position is the start chunk's corner, not necessarily inside the pieces: read the start itself.
        bounds = level.structureManager().startsForStructure(SectionPos.of(pos), structure.value()).stream()
                .filter(StructureStart::isValid).map(StructureStart::getBoundingBox).findFirst().orElse(null);
        int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ());
        BoundingBox box = bounds;
        if (box != null) {
            // Find the room's door so the second capture can stand in front of it; loads the pieces' chunks.
            for (BlockPos p : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
                BlockState state = level.getBlockState(p);
                if (state.getBlock() == SbWorldBlocks.LOCKED_DOOR.get() && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER) {
                    door = p.immutable();
                    doorFacing = state.getValue(DoorBlock.FACING);
                    break;
                }
            }
        }
        found = new BlockPos(pos.getX(), surface, pos.getZ());
    }

    private static void finish(Minecraft minecraft) {
        done = true;
        SbWorld.LOGGER.info("World debug: done, quitting");
        minecraft.stop();
    }

    private WorldDebug() {
    }
}
