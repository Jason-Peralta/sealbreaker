package dev.sealbreaker.world.client;

import dev.sealbreaker.core.client.dev.DevHarness;
import dev.sealbreaker.world.SbWorld;
import dev.sealbreaker.world.block.SpikePortalBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.clock.ServerClockManager;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;

import java.util.Locale;

/**
 * Two-client harness for the portal spike (S3), driven by the {@code sb.portaldebug} system property.
 *
 * <p>{@code host:<seed>} (the {@code clientPortal} run): creates a fresh world, opens it to LAN on {@link #PORT}
 * without authentication so a second offline dev client can join, and once it has: places a spike portal, poses
 * both players to look at it, screenshots, walks into the portal, screenshots the realm (a violet sky), steps out
 * of and back into the portal laid at the arrival point to come back, screenshots again, and quits. Every capture point is announced with {@code /say sb:capture <name>} so the guest captures the same
 * moments from the overworld; {@code sb:quit} ends the guest.
 *
 * <p>{@code guest} (the {@code clientPortalGuest} run, which quick-plays to the LAN address): screenshots on
 * each announcement and quits on request.
 */
public final class PortalDebug {
    public static final int PORT = 25585;
    private static final Identifier REALM_CLOCK = Identifier.fromNamespaceAndPath(SbWorld.MOD_ID, "spike_realm");
    private static final Identifier OVERWORLD_CLOCK = Identifier.withDefaultNamespace("overworld");

    private enum Step { CREATE, PUBLISH, WAIT_GUEST, SETUP, BEFORE, ENTER, IN_REALM, RETURNING, AFTER, QUIT, DONE }

    private static Step step = Step.CREATE;
    private static int ticksInStep;
    private static BlockPos portal;
    private static BlockPos arrival;
    private static volatile long[] clocksBefore;
    private static volatile long[] clocksAfter;

    public static boolean enabled() {
        return System.getProperty("sb.portaldebug") != null;
    }

    private static boolean isHost() {
        return System.getProperty("sb.portaldebug").startsWith("host");
    }

    private static long seed() {
        String[] spec = System.getProperty("sb.portaldebug").split(":");
        return spec.length > 1 ? Long.parseLong(spec[1]) : 3L;
    }

    /** {@code host:<seed>:solo} runs the trip without waiting for a guest. */
    private static boolean solo() {
        return System.getProperty("sb.portaldebug").endsWith(":solo");
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!isHost() || step == Step.DONE) {
            return;
        }
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            if (step == Step.CREATE && DevHarness.readyToCreate(minecraft)) {
                DevHarness.createFresh(minecraft, "Spike S3 seed " + seed(), seed());
                advance(Step.PUBLISH);
            }
            return;
        }
        IntegratedServer server = minecraft.getSingleplayerServer();
        if (server == null) {
            return;
        }
        ticksInStep++;
        switch (step) {
            case PUBLISH -> {
                if (ticksInStep < 40) {
                    return;
                }
                DevHarness.publishLan(server, PORT);
                advance(Step.WAIT_GUEST);
            }
            case WAIT_GUEST -> {
                if (solo() || server.getPlayerList().getPlayerCount() >= 2) {
                    SbWorld.LOGGER.info("Portal debug: guest joined after {} ticks", ticksInStep);
                    advance(Step.SETUP);
                }
            }
            case SETUP -> {
                // Everything on the ground (not on a canopy): the portal three blocks east of the player, the guest six south of it.
                BlockPos feet = DevHarness.ground(minecraft, player.blockPosition());
                portal = DevHarness.ground(minecraft, feet.east(3));
                BlockPos guest = DevHarness.ground(minecraft, portal.south(6));
                player.connection.sendCommand("time set noon");
                player.connection.sendCommand("weather clear");
                for (BlockPos column : new BlockPos[]{feet, portal, guest}) {
                    player.connection.sendCommand(String.format(Locale.ROOT, "fill %d %d %d %d %d %d minecraft:air", column.getX(), column.getY(), column.getZ(), column.getX(), column.getY() + 2, column.getZ()));
                }
                player.connection.sendCommand(String.format(Locale.ROOT, "setblock %d %d %d sb_world:spike_portal", portal.getX(), portal.getY(), portal.getZ()));
                player.connection.sendCommand("summon minecraft:pig " + DevHarness.centre(portal.south()) + " {NoAI:1b}");
                player.connection.sendCommand("tp @s " + DevHarness.centre(feet) + " facing " + DevHarness.centreLook(portal));
                player.connection.sendCommand("tp Dev2 " + DevHarness.centre(guest) + " facing " + DevHarness.centreLook(portal));
                advance(Step.BEFORE);
            }
            case BEFORE -> {
                if (ticksInStep == 60) {
                    DevHarness.capture(minecraft, player, "s3_host", "before");
                } else if (ticksInStep == 70) {
                    server.execute(() -> clocksBefore = readClocks(server));
                    player.connection.sendCommand("tp @s " + DevHarness.centre(portal));
                    advance(Step.ENTER);
                }
            }
            case ENTER -> {
                if (minecraft.level.dimension() == SpikePortalBlock.REALM) {
                    arrival = player.blockPosition();
                    SbWorld.LOGGER.info("Portal debug: arrived in {} after {} ticks in the portal, at {}", minecraft.level.dimension().identifier(), ticksInStep, arrival);
                    advance(Step.IN_REALM);
                } else if (ticksInStep > 400) {
                    SbWorld.LOGGER.error("Portal debug: the portal never fired");
                    advance(Step.QUIT);
                }
            }
            case IN_REALM -> {
                if (ticksInStep == 40) {
                    // Vanilla keeps refreshing the cooldown while an entity stands in a portal, so step out first.
                    player.connection.sendCommand("tp @s " + DevHarness.centre(arrival.west(3)) + " facing " + DevHarness.centreLook(arrival));
                } else if (ticksInStep == 100) {
                    DevHarness.capture(minecraft, player, "s3_host", "realm");
                } else if (ticksInStep == 110) {
                    server.execute(() -> clocksAfter = readClocks(server));
                    player.connection.sendCommand("tp @s " + DevHarness.centre(arrival));
                    advance(Step.RETURNING);
                }
            }
            case RETURNING -> {
                if (minecraft.level.dimension() == Level.OVERWORLD) {
                    SbWorld.LOGGER.info("Portal debug: back in the overworld after {} ticks, at {}", ticksInStep, player.blockPosition());
                    advance(Step.AFTER);
                } else if (ticksInStep > 400) {
                    SbWorld.LOGGER.error("Portal debug: the return trip never happened");
                    advance(Step.QUIT);
                }
            }
            case AFTER -> {
                if (ticksInStep == 60) {
                    player.connection.sendCommand("tp @s " + DevHarness.centre(portal.west(3)) + " facing " + DevHarness.centreLook(portal));
                } else if (ticksInStep == 100) {
                    DevHarness.capture(minecraft, player, "s3_host", "after");
                    long[] before = clocksBefore;
                    long[] after = clocksAfter;
                    if (before != null && after != null) {
                        SbWorld.LOGGER.info("Portal debug: clocks advanced overworld {} ticks, realm {} ticks between the two readings", after[0] - before[0], after[1] - before[1]);
                    }
                } else if (ticksInStep == 120) {
                    advance(Step.QUIT);
                }
            }
            case QUIT -> {
                if (ticksInStep == 1) {
                    player.connection.sendCommand("say " + DevHarness.QUIT);
                } else if (ticksInStep == 40) {
                    SbWorld.LOGGER.info("Portal debug: done, quitting");
                    advance(Step.DONE);
                    minecraft.stop();
                }
            }
            default -> {
            }
        }
    }

    private static void advance(Step next) {
        step = next;
        ticksInStep = 0;
    }

    /** Server thread: the overworld's and the realm's clock readings, to show they run apart. */
    private static long[] readClocks(MinecraftServer server) {
        ServerClockManager clocks = server.overworld().clockManager();
        Holder<WorldClock> overworld = server.registryAccess().lookupOrThrow(Registries.WORLD_CLOCK).getOrThrow(ResourceKey.create(Registries.WORLD_CLOCK, OVERWORLD_CLOCK));
        Holder<WorldClock> realm = server.registryAccess().lookupOrThrow(Registries.WORLD_CLOCK).getOrThrow(ResourceKey.create(Registries.WORLD_CLOCK, REALM_CLOCK));
        // The realm's clock runs three times faster from here on, so a 12000-tick day passes in 200 seconds.
        clocks.setRate(realm, 3.0f);
        return new long[]{clocks.getTotalTicks(overworld), clocks.getTotalTicks(realm)};
    }

    /** Guest side: the host announces capture points in chat. */
    public static void onChat(ClientChatReceivedEvent event) {
        if (!isHost()) {
            DevHarness.onGuestChat(event, "s3_guest");
        }
    }

    private PortalDebug() {
    }
}
