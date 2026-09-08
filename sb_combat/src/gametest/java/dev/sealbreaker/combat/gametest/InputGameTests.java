package dev.sealbreaker.combat.gametest;

import dev.sealbreaker.combat.item.SbCombatItems;
import dev.sealbreaker.combat.network.SwingRequestPayload;
import dev.sealbreaker.combat.swing.SbCombatAttachments;
import dev.sealbreaker.combat.swing.SwingService;
import dev.sealbreaker.combat.swing.SwingState;
import dev.sealbreaker.core.api.combat.AttackContext;
import dev.sealbreaker.core.api.combat.WeaponArchetype;
import dev.sealbreaker.core.api.component.SbDataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/** Exercise the real request/tick/hit path with server-owned mock players and a test-only data entry. */
final class InputGameTests {
    private static final Identifier FIXTURE = Identifier.fromNamespaceAndPath(SbCombatTests.MOD_ID, "input_fixture");

    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        player.connection = new ServerGamePacketListenerImpl(helper.getLevel().getServer(), connection, player,
                CommonListenerCookie.createInitial(player.getGameProfile(), false));
        player.setPos(helper.absoluteVec(new Vec3(7.5, 1, 4.5)));
        player.setYRot(0);
        player.setXRot(0);
        player.setOnGround(true);
        ItemStack weapon = new ItemStack(SbCombatItems.SPIKE_SWORD.get());
        weapon.set(SbDataComponents.ARCHETYPE.get(), FIXTURE);
        player.setItemInHand(InteractionHand.MAIN_HAND, weapon);
        return player;
    }

    private static SwingState state(ServerPlayer player) {
        return player.getExistingDataOrNull(SbCombatAttachments.SWING.get());
    }

    private static void edge(ServerPlayer player, AttackContext context, boolean release) {
        SwingService.onSwingRequest(player, new SwingRequestPayload(context, release));
    }

    private static void tap(ServerPlayer player) {
        edge(player, AttackContext.TAP, false);
        edge(player, AttackContext.TAP, true);
    }

    static void contexts(GameTestHelper helper) {
        ServerPlayer tap = player(helper);
        edge(tap, AttackContext.TAP, true);
        helper.assertTrue(state(tap) == null, "release without a press must not attack");
        edge(tap, AttackContext.TAP, false);
        helper.assertTrue(state(tap) == null, "ground press waits for release or charge threshold");
        edge(tap, AttackContext.TAP, true);
        helper.assertTrue(state(tap).context() == AttackContext.TAP, "short release selects tap");

        ServerPlayer air = player(helper);
        edge(air, AttackContext.AIR, false);
        helper.assertTrue(state(air) == null, "forged airborne input on ground is rejected");
        air.setOnGround(false);
        air.setDeltaMovement(0, 0.1, 0);
        edge(air, AttackContext.AIR, false);
        helper.assertTrue(state(air) == null, "rising is not a falling aerial attack");
        air.setDeltaMovement(0, -0.1, 0);
        edge(air, AttackContext.AIR, false);
        helper.assertTrue(state(air).context() == AttackContext.AIR, "falling press selects air");
        air.setOnGround(true);
        SwingService.tick(air);
        helper.assertTrue(state(air) == null, "landing ends an aerial move before another hit tick");

        ServerPlayer sprint = player(helper);
        edge(sprint, AttackContext.SPRINT, false);
        helper.assertTrue(state(sprint) == null, "forged sprint input is rejected");
        sprint.setSprinting(true);
        edge(sprint, AttackContext.SPRINT, false);
        helper.assertTrue(state(sprint).context() == AttackContext.SPRINT, "ground sprint selects sprint");
        WeaponArchetype data = SwingService.lookup(helper.getLevel(), FIXTURE).orElseThrow();
        helper.assertTrue(data.move(state(sprint).context(), 0).reach() > data.move(0).reach(), "sprint selects its own data move");
        helper.succeed();
    }

    static void charge(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        Pig target = helper.spawn(EntityTypes.PIG, new Vec3(7.5, 1, 6.5));
        target.setNoAi(true);
        WeaponArchetype data = SwingService.lookup(helper.getLevel(), FIXTURE).orElseThrow();
        long pressedAt = helper.getLevel().getGameTime();
        ServerPlayer partial = player(helper);
        edge(partial, AttackContext.TAP, false);
        helper.runAfterDelay(data.chargeTicks() / 2, () -> {
            edge(partial, AttackContext.HOLD, true);
            float expected = data.chargeCurve().scale(helper.getLevel().getGameTime() - pressedAt, data.chargeTicks());
            helper.assertTrue(state(partial).damageScale() == expected && expected < data.chargeCurve().max(),
                    "partial release uses the actual elapsed server ticks");
        });
        edge(player, AttackContext.TAP, false);
        helper.runAfterDelay(data.holdThresholdTicks() + 1, () -> {
            SwingService.tick(player);
            helper.assertTrue(state(player) != null && state(player).charging(), "crossing threshold syncs the charge pose");
            helper.assertTrue(target.getHealth() == target.getMaxHealth(), "charging never hits");
        });
        helper.runAfterDelay(data.chargeTicks() + 2, () -> {
            // Even a malicious TAP release cannot turn a long press into a cheap combo attack.
            edge(player, AttackContext.TAP, true);
            SwingState released = state(player);
            helper.assertTrue(released.context() == AttackContext.HOLD && !released.charging(), "server derives hold from elapsed ticks");
            float scale = data.chargeCurve().scale(helper.getLevel().getGameTime() - pressedAt, data.chargeTicks());
            helper.assertTrue(released.damageScale() == scale && scale == data.chargeCurve().max(), "overcharge clamps to data cap");
            edge(player, AttackContext.HOLD, true);
            helper.assertTrue(state(player).equals(released), "duplicate release cannot restart the attack");
            {
                player.getRandom().setSeed(0); // first roll > 4%: deterministic non-crit damage
                float before = target.getHealth();
                float expected = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE)
                        * data.move(AttackContext.HOLD, 0).damageMultiplier() * scale;
                SwingService.tick(player);
                helper.assertTrue(Math.abs(before - target.getHealth() - expected) < 0.001,
                        "actual hit uses move multiplier times server charge curve");
                float after = target.getHealth();
                SwingService.tick(player);
                helper.assertTrue(target.getHealth() == after, "target is hit at most once per charged move");
                helper.succeed();
            }
        });
    }

    static void buffering(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        WeaponArchetype data = SwingService.lookup(helper.getLevel(), FIXTURE).orElseThrow();
        tap(player);
        SwingState initial = state(player);
        tap(player);
        tap(player);
        helper.assertTrue(state(player).equals(initial), "repeated taps never interrupt the current move");
        helper.runAfterDelay(data.move(0).totalTicks() - 1, () -> {
            SwingService.tick(player);
            helper.assertTrue(state(player).step() == 0, "recovery plays to its last tick");
        });
        helper.runAfterDelay(data.move(0).totalTicks(), () -> {
            SwingService.tick(player);
            helper.assertTrue(state(player).context() == AttackContext.TAP && state(player).step() == 1,
                    "one buffered tap starts the next move exactly when the first ends");
            helper.runAfterDelay(data.move(1).totalTicks(), () -> {
                SwingService.tick(player);
                helper.assertTrue(state(player) == null, "multiple taps buffer only one move");
                helper.runAfterDelay(data.comboWindowTicks() + 1, () -> {
                    tap(player);
                    helper.assertTrue(state(player).step() == 0, "expired combo restarts at zero");
                    helper.succeed();
                });
            });
        });
    }

    static void cancellation(GameTestHelper helper) {
        ServerPlayer swapped = player(helper);
        tap(swapped);
        tap(swapped);
        swapped.setItemInHand(InteractionHand.MAIN_HAND, swapped.getMainHandItem().copy());
        SwingService.tick(swapped);
        helper.assertTrue(state(swapped) == null, "replacing even the same archetype cancels old move and buffer");
        tap(swapped);
        helper.assertTrue(state(swapped).step() == 0, "new stack starts at combo zero");
        ServerPlayer charging = player(helper);
        edge(charging, AttackContext.HOLD, false);
        edge(charging, AttackContext.HOLD, true);
        helper.assertTrue(state(charging) == null, "fabricated hold start and release cannot create a charge");
        edge(charging, AttackContext.TAP, false);
        helper.runAfterDelay(7, () -> {
            SwingService.tick(charging);
            helper.assertTrue(state(charging).charging(), "charge started");
            SwingService.onSwingRequest(charging, SwingRequestPayload.CANCEL);
            helper.assertTrue(state(charging) == null, "UI cancellation removes charge pose");
            edge(charging, AttackContext.HOLD, true);
            helper.assertTrue(state(charging) == null, "cancelled charge cannot be released later");
            ServerPlayer spectator = (ServerPlayer) helper.makeMockServerPlayer(GameType.SPECTATOR);
            spectator.setItemInHand(InteractionHand.MAIN_HAND, charging.getMainHandItem().copy());
            tap(spectator);
            helper.assertTrue(state(spectator) == null, "spectator cannot attack");
            helper.succeed();
        });
    }

    private InputGameTests() {
    }
}
