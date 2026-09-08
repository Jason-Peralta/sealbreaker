package dev.sealbreaker.combat.gametest;

import dev.sealbreaker.combat.item.SbCombatItems;
import dev.sealbreaker.combat.network.SwingRequestPayload;
import dev.sealbreaker.combat.swing.ArcHitTest;
import dev.sealbreaker.combat.swing.SbCombatAttachments;
import dev.sealbreaker.combat.swing.SwingService;
import dev.sealbreaker.combat.swing.SwingState;
import dev.sealbreaker.core.api.combat.AttackContext;
import dev.sealbreaker.core.api.combat.SwingMove;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

final class SwordMoveGameTests {
    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        player.connection = new ServerGamePacketListenerImpl(helper.getLevel().getServer(), connection, player,
                CommonListenerCookie.createInitial(player.getGameProfile(), false));
        player.connection.markClientLoaded();
        player.setPos(helper.absoluteVec(new Vec3(7.5, 4, 7.5)));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(SbCombatItems.SPIKE_SWORD.get()));
        return player;
    }

    private static SwingState state(ServerPlayer player) {
        return player.getExistingDataOrNull(SbCombatAttachments.SWING.get());
    }

    static void contexts(GameTestHelper helper) {
        var sword = SwingService.lookup(helper.getLevel(), Identifier.parse("sb_combat:sword")).orElseThrow();
        helper.assertTrue(sword.tap().size() == 3 && sword.hold().size() == 1 && sword.air().size() == 1 && sword.sprint().size() == 1,
                "datagen sword has the tap combo and exactly three contextual moves");
        helper.assertTrue(sword.air().getFirst().shape() == SwingMove.Shape.PLUNGE, "sword air move uses downward cone");
        helper.assertTrue(sword.sprint().getFirst().reach() == sword.tap().getFirst().reach() + 0.5, "sprint reaches half a block further");
        for (AttackContext context : AttackContext.values()) {
            helper.assertTrue(sword.move(context, 0).animation().isPresent(), "each context names an animation");
        }
        ServerPlayer sprint = player(helper);
        sprint.setOnGround(true);
        sprint.setSprinting(true);
        sprint.setYRot(0);
        SwingService.onSwingRequest(sprint, new SwingRequestPayload(AttackContext.SPRINT, false));
        helper.assertTrue(state(sprint).context() == AttackContext.SPRINT, "sprint selects its authored move");
        double impulse = sprint.getDeltaMovement().z;
        helper.assertTrue(Math.abs(impulse - sword.sprint().getFirst().forwardImpulse()) < 0.0001, "sprint applies the data impulse");
        SwingService.onSwingRequest(sprint, new SwingRequestPayload(AttackContext.SPRINT, false));
        helper.assertTrue(sprint.getDeltaMovement().z == impulse, "duplicate sprint press cannot stack impulses");
        ServerPlayer hold = player(helper);
        hold.setOnGround(true);
        SwingService.onSwingRequest(hold, new SwingRequestPayload(AttackContext.TAP, false));
        helper.runAfterDelay(sword.chargeTicks(), () -> {
            SwingService.tick(hold);
            helper.assertTrue(state(hold).charging(), "authored sword charge syncs anticipation");
            SwingService.onSwingRequest(hold, new SwingRequestPayload(AttackContext.HOLD, true));
            helper.assertTrue(state(hold).damageScale() == sword.chargeCurve().max(), "full sword charge uses its cap");
            helper.assertTrue(state(hold).moveTicks(sword.hold().getFirst(), helper.getLevel().getGameTime()) == sword.hold().getFirst().windupTicks(),
                    "release starts the heavy cut's hit window");
            helper.succeed();
        });
    }

    static void plunge(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        var sword = SwingService.lookup(helper.getLevel(), Identifier.parse("sb_combat:sword")).orElseThrow();
        SwingMove move = sword.air().getFirst();
        var below = helper.spawn(EntityTypes.PIG, new Vec3(7.5, 2.5, 7.5));
        var side = helper.spawn(EntityTypes.PIG, new Vec3(11.5, 2.5, 7.5));
        var above = helper.spawn(EntityTypes.PIG, new Vec3(7.5, 7.5, 7.5));
        for (var pig : java.util.List.of(below, side, above)) {
            pig.setNoAi(true);
            pig.setNoGravity(true);
        }
        var targets = ArcHitTest.findTargets(helper.getLevel(), player, player.getEyePosition(), new Vec3(0, -1, 0), move, 0);
        helper.assertTrue(targets.contains(below) && !targets.contains(side) && !targets.contains(above),
                "downward cone hits below, not above or outside its radius, even looking straight down");
        player.setOnGround(false);
        player.setDeltaMovement(0, -0.1, 0);
        SwingService.onSwingRequest(player, new SwingRequestPayload(AttackContext.AIR, false));
        helper.runAfterDelay(move.windupTicks(), () -> {
            SwingService.tick(player);
            helper.assertTrue(below.getHealth() < below.getMaxHealth(), "air move applies a real hit at its active window");
        });
        helper.runAfterDelay(move.totalTicks() + 1, () -> {
            SwingService.tick(player);
            helper.assertTrue(state(player) != null && state(player).landingTick() < 0, "plunge stays active past authored duration until landing");
            player.setPos(helper.absoluteVec(new Vec3(7.5, 1, 7.5)));
            player.setOnGround(true);
            SwingService.tick(player);
            long landed = helper.getLevel().getGameTime();
            helper.assertTrue(state(player).landingTick() == landed, "landing is recorded and synced");
            helper.assertTrue(move.activeIndexAt(state(player).moveTicks(move, landed)) < 0, "landing ends damage window");
            helper.runAfterDelay(move.hitstopTicks() + move.recoveryTicks(), () -> {
                SwingService.tick(player);
                helper.assertTrue(state(player) == null, "impact pause and recovery finish at data duration");
                helper.succeed();
            });
        });
    }

    private SwordMoveGameTests() {
    }
}
