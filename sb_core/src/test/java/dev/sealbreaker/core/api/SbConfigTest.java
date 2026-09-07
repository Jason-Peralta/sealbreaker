package dev.sealbreaker.core.api;

import com.electronwill.nightconfig.core.CommentedConfig;
import dev.sealbreaker.core.api.config.SbConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The config spec's defaults are the values the docs and the decision log promise, the ranges reject nonsense,
 * and the getters answer with the defaults before any file is loaded (which is what a dedicated server does for
 * the first tick or two, and what the unit-test JVM always sees).
 */
class SbConfigTest {
    @Test
    void gettersAnswerWithDefaultsBeforeAnyFileIsLoaded() {
        assertEquals(1.0, SbConfig.enemyHealthScalar());
        assertEquals(1.0, SbConfig.enemyDamageScalar());
        assertEquals(1.0, SbConfig.critDamageScalar());
        assertEquals(1.0, SbConfig.mobWeaponProficiencyScalar());
        assertTrue(SbConfig.dropCoinsOnDeath(), "coins drop on death (decision 0013)");
        assertTrue(SbConfig.keepGearOnDeath(), "gear is kept on death (decision 0013)");
        assertEquals(SbConfig.SealMode.WORLD, SbConfig.sealMode(), "Seals are world-wide (decision 0016)");
    }

    @Test
    void specCorrectsOutOfRangeAndUnknownValues() {
        ModConfigSpec spec = SbConfig.SPEC;
        CommentedConfig config = CommentedConfig.inMemory();
        spec.correct(config);
        assertTrue(spec.isCorrect(config), "a corrected empty config is valid");
        assertEquals(1.0, config.<Double>get("difficulty.enemy_health_scalar"));
        assertEquals("WORLD", config.get("seals.mode").toString());

        config.set("difficulty.enemy_health_scalar", 99.0);
        assertFalse(spec.isCorrect(config), "a scalar above the range is rejected");
        spec.correct(config);
        assertEquals(10.0, config.<Double>get("difficulty.enemy_health_scalar"),
                "correction clamps to the nearest bound, so a typo softens the difficulty rather than resetting it");

        config.set("seals.mode", "SOMETHING_ELSE");
        assertFalse(spec.isCorrect(config), "an unknown Seal mode is rejected");
    }
}
