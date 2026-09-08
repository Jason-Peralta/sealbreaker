package dev.sealbreaker.combat.kit;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DashSettingsTest {
    @Test
    void dataRoundTripsAndRejectsInvulnerabilityOrCooldownShorterThanMove() {
        var json = JsonParser.parseString("""
                {"distance":4,"duration_ticks":4,"cooldown_ticks":30,"lean_degrees":12,"invulnerable":false}
                """).getAsJsonObject();
        DashSettings value = DashSettings.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        assertEquals(value, DashSettings.CODEC.parse(JsonOps.INSTANCE,
                DashSettings.CODEC.encodeStart(JsonOps.INSTANCE, value).getOrThrow()).getOrThrow());
        json.addProperty("invulnerable", true);
        assertTrue(DashSettings.CODEC.parse(JsonOps.INSTANCE, json).error().isPresent());
        json.addProperty("invulnerable", false);
        json.addProperty("cooldown_ticks", 3);
        assertTrue(DashSettings.CODEC.parse(JsonOps.INSTANCE, json).error().isPresent());
        json.addProperty("cooldown_ticks", 30);
        json.addProperty("distance", 0);
        assertTrue(DashSettings.CODEC.parse(JsonOps.INSTANCE, json).error().isPresent());
    }

    @Test
    void syncedLeanStartsAndEndsAtRestAndReachesItsDataAmplitude() {
        DashState state = new DashState(100, 4, 12);
        assertEquals(0, state.leanAt(99));
        assertEquals(0, state.leanAt(100));
        assertEquals(12, state.leanAt(102));
        assertEquals(0, state.leanAt(104));
        assertEquals(0, state.leanAt(200));
    }
}
