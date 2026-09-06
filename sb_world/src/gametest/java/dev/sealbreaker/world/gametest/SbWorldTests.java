package dev.sealbreaker.world.gametest;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/** The test-only mod that carries {@code sb_world}'s game tests; see {@code SbCoreTests} for the layout. */
@Mod(SbWorldTests.MOD_ID)
public final class SbWorldTests {
    public static final String MOD_ID = "sb_world_tests";

    public SbWorldTests(IEventBus modEventBus) {
        SbWorldTestFunctions.register(modEventBus);
    }
}
