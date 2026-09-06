package dev.sealbreaker.core.gametest;

import dev.sealbreaker.core.SbCore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Consumer;

/**
 * Game test functions. Game tests are data-driven: each test is a JSON file in
 * {@code data/sb_core_tests/test_instance/} that names one of these functions, a test environment and a
 * structure template (the shared {@code sb_core_tests:arena}, a 16x9x16 stone floor with air above). Run them
 * with {@code ./gradlew :sb_core:runGameTestServer} (headless, exit code = number of failed required tests).
 */
public final class SbCoreTestFunctions {
    public static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, SbCoreTests.MOD_ID);

    /** Milestone 0 smoke test: the mod is loaded and the helper can place and see a block. */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> HELLO_WORLD =
            TEST_FUNCTIONS.register("hello_world", () -> SbCoreTestFunctions::helloWorld);

    private static void helloWorld(GameTestHelper helper) {
        helper.assertTrue(ModList.get().isLoaded(SbCore.MOD_ID), "sb_core is not loaded");
        BlockPos pos = new BlockPos(0, 1, 0);
        helper.setBlock(pos, Blocks.STONE);
        helper.assertBlockPresent(Blocks.STONE, pos);
        helper.succeed();
    }

    public static void register(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    private SbCoreTestFunctions() {
    }
}
