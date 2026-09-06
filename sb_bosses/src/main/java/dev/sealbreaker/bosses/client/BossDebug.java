package dev.sealbreaker.bosses.client;

import dev.sealbreaker.bosses.SbBosses;
import dev.sealbreaker.bosses.entity.SpikeDummy;
import dev.sealbreaker.core.client.dev.DevHarness;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.util.Locale;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;

/**
 * Two-client harness for the boss animation spike (S2), driven by the {@code sb.bossdebug} system property:
 * {@code host:<seed>[:solo]} creates a world, opens it to LAN on {@link #PORT}, waits for the guest, summons the
 * spike dummy between the two players, screenshots it idle, right-clicks it (the real interaction packet, so the
 * server raises the synced attack flag) and screenshots twice during the attack and once after; the guest
 * ({@code guest}) screenshots on each announcement. Both logs record the client tick the attack flag flipped.
 */
public final class BossDebug {
    public static final int PORT = 25586;
    private static final String HOST_PREFIX = "s2_host";
    private static final String GUEST_PREFIX = "s2_guest";

    private enum Step { CREATE, PUBLISH, WAIT_GUEST, SETUP, IDLE, ATTACK, QUIT, DONE }

    private static Step step = Step.CREATE;
    private static int ticksInStep;
    private static BlockPos stage;
    private static boolean guestSawAttack;

    public static boolean enabled() {
        return System.getProperty("sb.bossdebug") != null;
    }

    private static boolean isHost() {
        return System.getProperty("sb.bossdebug").startsWith("host");
    }

    private static long seed() {
        String[] spec = System.getProperty("sb.bossdebug").split(":");
        return spec.length > 1 ? Long.parseLong(spec[1]) : 4L;
    }

    private static boolean solo() {
        return System.getProperty("sb.bossdebug").endsWith(":solo");
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (!isHost()) {
            guestTick(minecraft, player);
            return;
        }
        if (step == Step.DONE) {
            return;
        }
        if (player == null || minecraft.level == null) {
            if (step == Step.CREATE && DevHarness.readyToCreate(minecraft)) {
                DevHarness.createFresh(minecraft, "Spike S2 seed " + seed(), seed());
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
                if (ticksInStep >= 40) {
                    DevHarness.publishLan(server, PORT);
                    advance(Step.WAIT_GUEST);
                }
            }
            case WAIT_GUEST -> {
                if (solo() || server.getPlayerList().getPlayerCount() >= 2) {
                    SbBosses.LOGGER.info("Boss debug: guest joined after {} ticks", ticksInStep);
                    advance(Step.SETUP);
                }
            }
            case SETUP -> {
                // A flat stone stage at the player's ground level with clear air above, so any seed frames the same way.
                BlockPos feet = DevHarness.ground(minecraft, player.blockPosition());
                stage = feet.east(4);
                player.connection.sendCommand("time set noon");
                player.connection.sendCommand("weather clear");
                player.connection.sendCommand(String.format(Locale.ROOT, "fill %d %d %d %d %d %d minecraft:smooth_stone",
                        stage.getX() - 6, stage.getY() - 1, stage.getZ() - 4, stage.getX() + 6, stage.getY() - 1, stage.getZ() + 4));
                player.connection.sendCommand(String.format(Locale.ROOT, "fill %d %d %d %d %d %d minecraft:air",
                        stage.getX() - 6, stage.getY(), stage.getZ() - 4, stage.getX() + 6, stage.getY() + 5, stage.getZ() + 4));
                player.connection.sendCommand("summon sb_bosses:spike_dummy " + DevHarness.centre(stage) + " {NoAI:1b}");
                String look = String.format(Locale.ROOT, "%.1f %.1f %.1f", stage.getX() + 0.5, stage.getY() + 0.8, stage.getZ() + 0.5);
                player.connection.sendCommand("tp @s " + DevHarness.centre(stage.west(4)) + " facing " + look);
                player.connection.sendCommand("tp Dev2 " + DevHarness.centre(stage.east(4)) + " facing " + look);
                advance(Step.IDLE);
            }
            case IDLE -> {
                if (ticksInStep == 60) {
                    DevHarness.capture(minecraft, player, HOST_PREFIX, "idle");
                } else if (ticksInStep == 70) {
                    SpikeDummy dummy = findDummy(minecraft);
                    if (dummy == null) {
                        SbBosses.LOGGER.error("Boss debug: no spike dummy near the stage");
                        advance(Step.QUIT);
                        return;
                    }
                    // A real right-click: the interaction packet reaches the server, which raises the synced flag.
                    minecraft.gameMode.interact(player, dummy, new net.minecraft.world.phys.EntityHitResult(dummy), InteractionHand.MAIN_HAND);
                    SbBosses.LOGGER.info("Boss debug: right-clicked the dummy at client tick {}", minecraft.level.getGameTime());
                    advance(Step.ATTACK);
                }
            }
            case ATTACK -> {
                SpikeDummy dummy = findDummy(minecraft);
                if (ticksInStep == 1 || ticksInStep == 2 || ticksInStep == 3) {
                    SbBosses.LOGGER.info("Boss debug: host sees attacking={} at client tick {}", dummy != null && dummy.isAttacking(), minecraft.level.getGameTime());
                }
                if (ticksInStep == 6) {
                    DevHarness.capture(minecraft, player, HOST_PREFIX, "attack_early");
                } else if (ticksInStep == 14) {
                    DevHarness.capture(minecraft, player, HOST_PREFIX, "attack_late");
                } else if (ticksInStep == 60) {
                    DevHarness.capture(minecraft, player, HOST_PREFIX, "after");
                } else if (ticksInStep == 80) {
                    advance(Step.QUIT);
                }
            }
            case QUIT -> {
                if (ticksInStep == 1) {
                    player.connection.sendCommand("say " + DevHarness.QUIT);
                } else if (ticksInStep == 40) {
                    SbBosses.LOGGER.info("Boss debug: done, quitting");
                    advance(Step.DONE);
                    minecraft.stop();
                }
            }
            default -> {
            }
        }
    }

    /** Guest side: logs the client tick at which the synced attack flag arrives, to compare with the host's. */
    private static void guestTick(Minecraft minecraft, LocalPlayer player) {
        if (player == null || minecraft.level == null || guestSawAttack) {
            return;
        }
        for (Entity entity : minecraft.level.getEntities(player, player.getBoundingBox().inflate(16.0), e -> e instanceof SpikeDummy)) {
            if (((SpikeDummy) entity).isAttacking()) {
                guestSawAttack = true;
                SbBosses.LOGGER.info("Boss debug: guest sees attacking=true at client tick {}", minecraft.level.getGameTime());
            }
        }
    }

    private static SpikeDummy findDummy(Minecraft minecraft) {
        AABB around = new AABB(stage).inflate(8.0);
        for (Entity entity : minecraft.level.getEntities(minecraft.player, around, e -> e instanceof SpikeDummy)) {
            return (SpikeDummy) entity;
        }
        return null;
    }

    private static void advance(Step next) {
        step = next;
        ticksInStep = 0;
    }

    public static void onChat(ClientChatReceivedEvent event) {
        if (!isHost()) {
            DevHarness.onGuestChat(event, GUEST_PREFIX);
        }
    }

    private BossDebug() {
    }
}
