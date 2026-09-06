package dev.sealbreaker.combat.gametest;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/** The test-only mod that carries {@code sb_combat}'s game tests; see {@code SbCoreTests} for the layout. */
@Mod(SbCombatTests.MOD_ID)
public final class SbCombatTests {
    public static final String MOD_ID = "sb_combat_tests";

    public SbCombatTests(IEventBus modEventBus) {
        SbCombatTestFunctions.register(modEventBus);
    }
}
