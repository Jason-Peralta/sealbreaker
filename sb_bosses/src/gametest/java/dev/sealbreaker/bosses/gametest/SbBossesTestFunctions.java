package dev.sealbreaker.bosses.gametest;

import com.geckolib.animatable.GeoEntity;
import dev.sealbreaker.bosses.entity.SbBossesEntities;
import dev.sealbreaker.bosses.entity.SpikeDummy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Consumer;

/** Game tests for the boss module; instances live in {@code data/sb_bosses_tests/test_instance/}. */
public final class SbBossesTestFunctions {
    public static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, SbBossesTests.MOD_ID);

    /** A right-click raises the synced attack flag on the server for one animation, then it clears on its own. */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DUMMY_ATTACK =
            TEST_FUNCTIONS.register("dummy_attack", () -> SbBossesTestFunctions::dummyAttack);

    private static void dummyAttack(GameTestHelper helper) {
        SpikeDummy dummy = helper.spawn(SbBossesEntities.SPIKE_DUMMY.get(), new BlockPos(7, 1, 7));
        helper.assertTrue(dummy instanceof GeoEntity, "the dummy animates through GeckoLib");
        helper.assertTrue(!dummy.isAttacking(), "a fresh dummy is idle");
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        InteractionResult result = dummy.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
        helper.assertTrue(result.consumesAction(), "a right-click is consumed");
        helper.assertTrue(dummy.isAttacking(), "the right-click must raise the synced attack flag on the server");
        helper.runAfterDelay(SpikeDummy.ATTACK_TICKS / 2, () -> helper.assertTrue(dummy.isAttacking(), "the flag stays up through the animation"));
        helper.runAfterDelay(SpikeDummy.ATTACK_TICKS + 2, () -> {
            helper.assertTrue(!dummy.isAttacking(), "the flag clears after the animation's length");
            helper.succeed();
        });
    }

    public static void register(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    private SbBossesTestFunctions() {
    }
}
