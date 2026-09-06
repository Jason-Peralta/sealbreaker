package dev.sealbreaker.core.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import dev.sealbreaker.core.api.damage.DamageClass;
import dev.sealbreaker.core.api.progression.Seal;
import dev.sealbreaker.core.api.progression.Tier;
import dev.sealbreaker.core.api.rarity.Rarity;
import dev.sealbreaker.core.api.reforge.ReforgeModifier;
import dev.sealbreaker.core.api.reforge.ReforgePool;
import dev.sealbreaker.core.api.reforge.StatModifier;
import dev.sealbreaker.core.api.registry.SbRegistries;
import dev.sealbreaker.core.datagen.SbCoreEntries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every {@code sb:*} registry codec round-trips through JSON, with the field names the docs promise. The reforge
 * pool's item set needs a registry-aware ops, so its entry codec is tested here and the whole pool by the
 * {@code registries_loaded} game test.
 */
class RegistryCodecsTest {
    private static <T> T roundTrip(Codec<T> codec, T value) {
        JsonElement json = codec.encodeStart(JsonOps.INSTANCE, value).getOrThrow();
        DataResult<T> back = codec.parse(JsonOps.INSTANCE, json);
        return back.getOrThrow();
    }

    private static <T> JsonObject json(Codec<T> codec, T value) {
        return codec.encodeStart(JsonOps.INSTANCE, value).getOrThrow().getAsJsonObject();
    }

    @Test
    void damageClassRoundTrips() {
        DamageClass melee = new DamageClass("melee", SbCoreEntries.MELEE_DAMAGE);
        assertEquals(melee, roundTrip(DamageClass.CODEC, melee));
        JsonObject json = json(DamageClass.CODEC, melee);
        assertEquals("sb_core:melee_damage", json.get("attribute").getAsString());
        assertEquals("damage_class.melee", melee.translationKey());
    }

    @Test
    void rarityRoundTripsWithHexColour() {
        Rarity rare = new Rarity(0x55FFFF, 2, Optional.of("rarity.custom"), 1.5f, Optional.of(Identifier.fromNamespaceAndPath("sb_core", "rare")));
        assertEquals(rare, roundTrip(Rarity.CODEC, rare));
        JsonObject json = json(Rarity.CODEC, rare);
        assertEquals("#55ffff", json.get("color").getAsString(), "colours are written as #RRGGBB");
        assertTrue(json.has("reforge_cost_multiplier"));
        Rarity derived = new Rarity(0xFFFFFF, 0, Optional.empty(), 1.0f, Optional.empty());
        assertEquals("rarity.sb_core.common", derived.tooltipKey(SbCoreEntries.COMMON));
        assertEquals("rarity.custom", rare.tooltipKey(SbCoreEntries.RARE));
    }

    @Test
    void sealAndTierRoundTrip() {
        Seal seal = new Seal(2, ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("sb_bosses", "boss_ii")), List.of(SbCoreEntries.NETHER_IGNITION));
        assertEquals(seal, roundTrip(Seal.CODEC, seal));
        Seal bare = Seal.CODEC.parse(JsonOps.INSTANCE, json(Seal.CODEC, new Seal(1, seal.boss(), List.of()))).getOrThrow();
        assertTrue(bare.unlocks().isEmpty(), "unlocks default to none");

        Tier tier2 = new Tier(2, 40, 12, Optional.of(SbCoreEntries.SEAL_I), 20);
        assertEquals(tier2, roundTrip(Tier.CODEC, tier2));
        assertEquals("sb_core:seal_i", json(Tier.CODEC, tier2).get("requires_seal").getAsString());
        Tier tier1 = new Tier(1, 30, 8, Optional.empty(), 10);
        assertEquals(tier1, roundTrip(Tier.CODEC, tier1));
    }

    @Test
    void reforgeModifierRoundTripsWithVanillaOperationNames() {
        StatModifier stat = new StatModifier(SbCoreEntries.CRIT_CHANCE, 0.04, AttributeModifier.Operation.ADD_VALUE);
        ReforgeModifier lucky = new ReforgeModifier(Optional.empty(), SbCoreEntries.UNCOMMON, List.of(stat));
        assertEquals(lucky, roundTrip(ReforgeModifier.CODEC, lucky));
        JsonObject json = json(ReforgeModifier.CODEC, lucky);
        assertEquals("add_value", json.getAsJsonArray("stats").get(0).getAsJsonObject().get("operation").getAsString());
        assertEquals("reforge.sb_core.lucky", lucky.prefixKey(SbCoreEntries.LUCKY));
        assertEquals("reforge.custom", new ReforgeModifier(Optional.of("reforge.custom"), SbCoreEntries.UNCOMMON, List.of()).prefixKey(SbCoreEntries.LUCKY));

        AttributeModifier applied = stat.toAttributeModifier(Identifier.fromNamespaceAndPath("sb_core", "reforge/lucky"));
        assertEquals(0.04, applied.amount());
        assertEquals(AttributeModifier.Operation.ADD_VALUE, applied.operation());
    }

    @Test
    void reforgePoolEntriesRoundTripAndWeightsSum() {
        ReforgePool.Entry entry = new ReforgePool.Entry(SbCoreEntries.LUCKY, 10);
        assertEquals(entry, roundTrip(ReforgePool.Entry.CODEC, entry));
        assertTrue(ReforgePool.Entry.CODEC.parse(JsonOps.INSTANCE, json(ReforgePool.Entry.CODEC, new ReforgePool.Entry(SbCoreEntries.LUCKY, 1))).result().isPresent());
        JsonObject zero = json(ReforgePool.Entry.CODEC, entry);
        zero.addProperty("weight", 0);
        assertTrue(ReforgePool.Entry.CODEC.parse(JsonOps.INSTANCE, zero).error().isPresent(), "a weight of zero is rejected");
        assertEquals(SbRegistries.REFORGE_MODIFIER, entry.modifier().registryKey());
    }
}
