package dev.sealbreaker.gear.gametest;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/** The test-only mod that carries {@code sb_gear}'s game tests; see {@code SbCoreTests} for the layout. */
@Mod(SbGearTests.MOD_ID)
public final class SbGearTests {
    public static final String MOD_ID = "sb_gear_tests";

    public SbGearTests(IEventBus modEventBus) {
        SbGearTestFunctions.register(modEventBus);
    }
}
