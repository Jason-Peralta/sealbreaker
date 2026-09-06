package dev.sealbreaker.bosses.gametest;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/** The test-only mod that carries {@code sb_bosses}'s game tests; see {@code SbCoreTests} for the layout. */
@Mod(SbBossesTests.MOD_ID)
public final class SbBossesTests {
    public static final String MOD_ID = "sb_bosses_tests";

    public SbBossesTests(IEventBus modEventBus) {
        SbBossesTestFunctions.register(modEventBus);
    }
}
