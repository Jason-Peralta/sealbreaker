package dev.sealbreaker.core.api.combat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WeaponArchetypeTest {
    private static JsonObject fixture() {
        return JsonParser.parseString("""
                {"tap":[{"shape":"thrust","reach":3,"arc_degrees":30,
                 "windup_ticks":2,"active_ticks":2,"recovery_ticks":3}]}
                """).getAsJsonObject();
    }

    @Test
    void oldTapOnlyDataKeepsDefaultsAndFallsBack() {
        WeaponArchetype value = WeaponArchetype.CODEC.parse(JsonOps.INSTANCE, fixture()).getOrThrow();
        assertEquals(6, value.holdThresholdTicks());
        for (AttackContext context : AttackContext.values()) {
            assertEquals(value.move(0), value.move(context, 0));
            assertEquals(AttackContext.TAP, value.resolve(context));
        }
        assertEquals(1, value.chargeCurve().scale(100, value.chargeTicks()));
    }

    @Test
    void allContextsRoundTripAndListsCannotMutate() {
        JsonObject json = fixture();
        for (String field : new String[]{"hold", "air", "sprint"}) {
            json.add(field, json.get("tap").deepCopy());
        }
        json.addProperty("charge_ticks", 24);
        json.add("charge_curve", JsonParser.parseString("{\"min\":0.5,\"max\":3,\"exponent\":2}"));
        WeaponArchetype value = WeaponArchetype.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        assertEquals(value, WeaponArchetype.CODEC.parse(JsonOps.INSTANCE,
                WeaponArchetype.CODEC.encodeStart(JsonOps.INSTANCE, value).getOrThrow()).getOrThrow());
        for (AttackContext context : AttackContext.values()) {
            assertEquals(context, value.resolve(context));
            assertThrows(UnsupportedOperationException.class, () -> value.moves(context).clear());
        }
    }

    @Test
    void curveUsesHeldTicksAndClampsBeforeAndAfterItsRange() {
        ChargeCurve curve = new ChargeCurve(1, 3, 2);
        assertEquals(1, curve.scale(-1, 20));
        assertEquals(1.5f, curve.scale(10, 20));
        assertEquals(3, curve.scale(20, 20));
        assertEquals(3, curve.scale(Long.MAX_VALUE, 20));
    }

    @Test
    void rejectsUnreachableChargeAndInvalidCurve() {
        JsonObject json = fixture();
        json.add("hold", json.get("tap").deepCopy());
        json.addProperty("charge_ticks", 6);
        assertTrue(WeaponArchetype.CODEC.parse(JsonOps.INSTANCE, json).error().isPresent());
        json.addProperty("charge_ticks", 20);
        for (String curve : new String[]{"{\"min\":3,\"max\":1,\"exponent\":1}",
                "{\"min\":1,\"max\":3,\"exponent\":0}", "{\"min\":-1,\"max\":3,\"exponent\":1}"}) {
            json.add("charge_curve", JsonParser.parseString(curve));
            assertTrue(WeaponArchetype.CODEC.parse(JsonOps.INSTANCE, json).error().isPresent());
        }
    }
}
