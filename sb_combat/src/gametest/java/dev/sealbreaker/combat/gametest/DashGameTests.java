package dev.sealbreaker.combat.gametest;

import dev.sealbreaker.combat.api.kit.KitCapabilities;
import dev.sealbreaker.combat.kit.DashData;
import dev.sealbreaker.combat.kit.DashService;
import dev.sealbreaker.combat.kit.DashSettings;
import dev.sealbreaker.combat.kit.KitAttachments;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Real server movement and block collision, with no equipment or client trust shortcuts. */
final class DashGameTests {
    private static ServerPlayer player(GameTestHelper helper, double x) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        player.connection = new ServerGamePacketListenerImpl(helper.getLevel().getServer(), connection, player,
                CommonListenerCookie.createInitial(player.getGameProfile(), false));
        player.connection.markClientLoaded();
        player.setPos(helper.absoluteVec(new Vec3(x, 1, 3.5)));
        player.setYRot(0);
        player.setOnGround(true);
        return player;
    }

    static void capabilityAndCooldown(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5);
        DashSettings data = DashData.get();
        Vec3 start = player.position();
        helper.assertFalse(KitCapabilities.has(player, KitCapabilities.DASH), "fresh player has no earned dash");
        helper.assertFalse(DashService.request(player), "request without capability is rejected");
        helper.assertTrue(player.position().equals(start), "rejected request does not move");
        KitCapabilities.grant(player, KitCapabilities.DASH);
        KitCapabilities.grant(player, KitCapabilities.DASH);
        helper.assertTrue(KitCapabilities.has(player, KitCapabilities.DASH), "grant is idempotent");
        helper.assertTrue(DashService.request(player), "granted dash starts");
        helper.assertTrue(player.getZ() > start.z, "accepted request moves in the server look direction");
        helper.assertFalse(player.isInvulnerable(), "M1 dash grants no invulnerability");
        float health = player.getHealth();
        player.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 1);
        helper.assertTrue(player.getHealth() < health, "an active dash still takes damage");
        Vec3 first = player.position();
        helper.assertFalse(DashService.request(player), "duplicate request cannot bypass cooldown");
        DashService.tick(player);
        helper.assertTrue(player.position().equals(first), "multiple calls in one server tick move only once");
        for (int tick = 1; tick < data.durationTicks(); tick++) {
            helper.runAfterDelay(tick, () -> DashService.tick(player));
        }
        helper.runAfterDelay(data.durationTicks(), () -> {
            DashService.tick(player);
            helper.assertTrue(Math.abs(player.getZ() - start.z - data.distance()) < 0.0001, "unobstructed dash travels the data distance");
            helper.assertTrue(player.getExistingDataOrNull(KitAttachments.DASH.get()) == null, "finished dash removes synced animation");
            KitCapabilities.revoke(player, KitCapabilities.DASH);
            KitCapabilities.grant(player, KitCapabilities.DASH);
            helper.assertFalse(DashService.request(player), "unequip/re-equip cannot reset cooldown");
        });
        helper.runAfterDelay(data.cooldownTicks(), () -> {
            player.setOnGround(true);
            helper.assertTrue(DashService.request(player), "dash becomes available exactly at cooldown end");
            KitCapabilities.revoke(player, KitCapabilities.DASH);
            Vec3 before = player.position();
            helper.runAfterDelay(1, () -> {
                DashService.tick(player);
                helper.assertTrue(player.position().equals(before), "revoking capability stops remaining movement");
                helper.assertFalse(DashService.request(player), "revoked capability rejects requests");
                helper.succeed();
            });
        });
    }

    static void collisionAndGroundGate(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5);
        KitCapabilities.grant(player, KitCapabilities.DASH);
        player.setOnGround(false);
        helper.assertFalse(DashService.request(player), "Tier 1 dash cannot start in the air");
        player.setOnGround(true);
        for (int x = 5; x <= 9; x++) {
            for (int y = 1; y <= 4; y++) {
                helper.setBlock(x, y, 5, Blocks.STONE);
            }
        }
        helper.assertTrue(DashService.request(player), "ground dash may approach an obstruction");
        for (int tick = 1; tick <= DashData.get().durationTicks(); tick++) {
            helper.runAfterDelay(tick, () -> DashService.tick(player));
        }
        helper.runAfterDelay(DashData.get().durationTicks() + 1, () -> {
            double wall = helper.absoluteVec(new Vec3(7.5, 1, 5)).z;
            helper.assertTrue(player.getBoundingBox().maxZ <= wall + 0.0001, "dash never tunnels through the wall");
            helper.assertTrue(player.getExistingDataOrNull(KitAttachments.DASH.get()) == null, "collision ends animation and movement");
            helper.assertTrue(player.getHealth() == player.getMaxHealth(), "dash itself does not change health");
            helper.succeed();
        });
    }

    private DashGameTests() {
    }
}
