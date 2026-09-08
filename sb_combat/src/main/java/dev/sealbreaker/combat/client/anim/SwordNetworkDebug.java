package dev.sealbreaker.combat.client.anim;

import dev.sealbreaker.combat.SbCombat;
import dev.sealbreaker.combat.network.SwingRequestPayload;
import dev.sealbreaker.combat.swing.SbCombatAttachments;
import dev.sealbreaker.combat.swing.SwingState;
import dev.sealbreaker.core.api.combat.AttackContext;
import dev.sealbreaker.core.api.registry.SbRegistries;
import dev.sealbreaker.core.client.dev.DevHarness;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.HashSet;
import java.util.Set;

/** Dev-only two-client capture of the real request and attachment-sync path, using a fresh test world. */
@EventBusSubscriber(modid = SbCombat.MOD_ID, value = Dist.CLIENT)
public final class SwordNetworkDebug {
    public static final int PORT = 25576;
    private static final String MODE = System.getProperty("sb.sworddebug", "");
    private static final AttackContext[] CONTEXTS = {AttackContext.TAP, AttackContext.SPRINT, AttackContext.HOLD, AttackContext.AIR};
    private static final Set<String> SEEN = new HashSet<>();
    private enum Step { CREATE, PUBLISH, WAIT_GUEST, SETUP, ATTACK, QUIT, DONE }
    private static Step step = Step.CREATE;
    private static int ticks;
    private static int contextIndex;
    private static BlockPos stage;
    private static boolean guestConnecting;

    private static void advance(Step next) {
        step = next;
        ticks = 0;
    }

    @SubscribeEvent
    static void chat(ClientChatReceivedEvent event) {
        if (MODE.equals("guest")) {
            DevHarness.onGuestChat(event, "sword_guest_announced");
        }
    }

    @SubscribeEvent
    static void tick(ClientTickEvent.Post event) {
        if (MODE.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.options.pauseOnLostFocus = false;
        if (MODE.equals("guest")) {
            if (minecraft.player == null && !guestConnecting && DevHarness.readyToCreate(minecraft)) {
                guestConnecting = true;
                String address = "127.0.0.1:" + PORT;
                net.minecraft.client.gui.screens.ConnectScreen.startConnecting(new net.minecraft.client.gui.screens.TitleScreen(), minecraft,
                        net.minecraft.client.multiplayer.resolver.ServerAddress.parseString(address),
                        new net.minecraft.client.multiplayer.ServerData("Sword capture", address, net.minecraft.client.multiplayer.ServerData.Type.LAN), false, null);
            }
            if (!minecraft.gui.hud.isHidden()) {
                minecraft.gui.hud.toggle();
            }
            if (minecraft.player != null && minecraft.level != null) {
                for (var entity : minecraft.level.getEntities(minecraft.player, minecraft.player.getBoundingBox().inflate(32), e -> e instanceof Avatar)) {
                    captureState(minecraft, (Avatar) entity, "guest");
                }
            }
            return;
        }
        if (step == Step.DONE) {
            return;
        }
        if (minecraft.player == null || minecraft.level == null) {
            if (step == Step.CREATE && DevHarness.readyToCreate(minecraft)) {
                DevHarness.createFresh(minecraft, "Sword network " + System.currentTimeMillis(), 4);
                advance(Step.PUBLISH);
            }
            return;
        }
        var server = minecraft.getSingleplayerServer();
        var player = minecraft.player;
        if (server == null) {
            return;
        }
        ticks++;
        switch (step) {
            case PUBLISH -> {
                if (ticks >= 40) {
                    DevHarness.publishLan(server, PORT);
                    advance(Step.WAIT_GUEST);
                }
            }
            case WAIT_GUEST -> {
                if (server.getPlayerList().getPlayerCount() >= 2) {
                    advance(Step.SETUP);
                }
            }
            case SETUP -> {
                if (ticks == 1) {
                    player.connection.sendCommand("time set noon");
                    player.connection.sendCommand("weather clear");
                    stage = player.blockPosition().above(4);
                    server.execute(() -> SwordCaptureStage.prepare(server.overworld(), stage));
                    player.connection.sendCommand("tp @s " + DevHarness.centre(stage) + " 0 0");
                    player.connection.sendCommand("tp Dev2 " + DevHarness.centre(stage.south(5)) + " 180 0");
                    if (!minecraft.gui.hud.isHidden()) {
                        minecraft.gui.hud.toggle();
                    }
                    player.connection.sendCommand("give @s sb_combat:spike_sword");
                    minecraft.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                } else if (ticks >= 60) {
                    advance(Step.ATTACK);
                }
            }
            case ATTACK -> {
                AttackContext context = CONTEXTS[contextIndex];
                if (ticks == 1) {
                    server.execute(() -> {
                        var actor = server.getPlayerList().getPlayer(player.getUUID());
                        if (actor != null) {
                            actor.connection.teleport(stage.getX() + 0.5, stage.getY() + (context == AttackContext.AIR ? 4 : 0), stage.getZ() + 0.5, 0, 0);
                            actor.setOnGround(context != AttackContext.AIR);
                            actor.setSprinting(context == AttackContext.SPRINT);
                            actor.setDeltaMovement(context == AttackContext.AIR ? new Vec3(0, -0.1, 0) : Vec3.ZERO);
                            actor.connection.send(new ClientboundSetEntityMotionPacket(actor));
                        }
                    });
                }
                if (ticks == 4) {
                    AttackContext press = context == AttackContext.HOLD ? AttackContext.TAP : context;
                    ClientPacketDistributor.sendToServer(new SwingRequestPayload(press, false));
                    if (context != AttackContext.HOLD) {
                        ClientPacketDistributor.sendToServer(new SwingRequestPayload(press, true));
                    }
                }
                if (context == AttackContext.HOLD && ticks == 34) {
                    ClientPacketDistributor.sendToServer(new SwingRequestPayload(AttackContext.HOLD, true));
                }
                captureState(minecraft, player, "host");
                if (ticks >= 80) {
                    String expected = context.getSerializedName() + "_active";
                    if (!SEEN.contains(expected)) {
                        throw new IllegalStateException("Sword network capture never observed " + expected);
                    }
                    contextIndex++;
                    advance(contextIndex == CONTEXTS.length ? Step.QUIT : Step.ATTACK);
                }
            }
            case QUIT -> {
                if (ticks == 1) {
                    player.connection.sendCommand("say " + DevHarness.QUIT);
                    SbCombat.LOGGER.info("Sword network host observed {}", SEEN);
                } else if (ticks >= 40) {
                    advance(Step.DONE);
                    minecraft.stop();
                }
            }
            default -> { }
        }
    }

    private static void captureState(Minecraft minecraft, Avatar actor, String side) {
        SwingState swing = actor.getExistingDataOrNull(SbCombatAttachments.SWING.get());
        if (swing == null) {
            return;
        }
        var archetype = actor.level().registryAccess().lookupOrThrow(SbRegistries.WEAPON_ARCHETYPE)
                .getOptional(Identifier.parse("sb_combat:sword")).orElseThrow();
        var move = archetype.move(swing.context(), swing.step());
        long now = actor.level().getGameTime();
        if (!swing.charging() && move.activeIndexAt(swing.moveTicks(move, now)) < 0) {
            return;
        }
        String name = swing.context().getSerializedName() + (swing.charging() ? "_charging" : "_active");
        if (SEEN.add(name)) {
            SbCombat.LOGGER.info("Sword network {} sees {} start={} at={} scale={}", side, name, swing.startTick(), now, swing.damageScale());
            Screenshot.grab(minecraft.gameDirectory, "sword_" + side + "_" + name + ".png",
                    minecraft.gameRenderer.mainRenderTarget(), 1, message -> { });
        }
    }

    private SwordNetworkDebug() {
    }
}
