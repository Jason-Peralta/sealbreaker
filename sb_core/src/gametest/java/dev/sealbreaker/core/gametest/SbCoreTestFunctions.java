package dev.sealbreaker.core.gametest;

import dev.sealbreaker.core.SbCore;
import dev.sealbreaker.core.api.attribute.SbAttributes;
import dev.sealbreaker.core.api.config.SbConfig;
import dev.sealbreaker.core.api.damage.DamageClass;
import dev.sealbreaker.core.api.progression.Seal;
import dev.sealbreaker.core.api.progression.Tier;
import dev.sealbreaker.core.api.rarity.Rarity;
import dev.sealbreaker.core.api.reforge.ReforgeModifier;
import dev.sealbreaker.core.api.reforge.ReforgePool;
import dev.sealbreaker.core.api.registry.SbRegistries;
import dev.sealbreaker.core.datagen.SbCoreEntries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    /** The six {@code sb:*} registries load the core entries from the generated data, cross-references resolve, and the ladders are consistent. */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> REGISTRIES_LOADED =
            TEST_FUNCTIONS.register("registries_loaded", () -> SbCoreTestFunctions::registriesLoaded);

    private static void registriesLoaded(GameTestHelper helper) {
        HolderLookup.Provider registries = helper.getLevel().registryAccess();
        HolderLookup.RegistryLookup<DamageClass> damageClasses = registries.lookupOrThrow(SbRegistries.DAMAGE_CLASS);
        HolderLookup.RegistryLookup<Rarity> rarities = registries.lookupOrThrow(SbRegistries.RARITY);
        HolderLookup.RegistryLookup<Seal> seals = registries.lookupOrThrow(SbRegistries.SEAL);
        HolderLookup.RegistryLookup<Tier> tiers = registries.lookupOrThrow(SbRegistries.TIER);
        HolderLookup.RegistryLookup<ReforgeModifier> modifiers = registries.lookupOrThrow(SbRegistries.REFORGE_MODIFIER);
        HolderLookup.RegistryLookup<ReforgePool> pools = registries.lookupOrThrow(SbRegistries.REFORGE_POOL);

        helper.assertTrue(damageClasses.listElements().count() == 5, "five damage classes, one per class");
        DamageClass melee = damageClasses.getOrThrow(SbCoreEntries.MELEE).value();
        helper.assertTrue(melee.name().equals("melee") && melee.attribute().equals(SbCoreEntries.MELEE_DAMAGE), "melee scales by sb_core:melee_damage");

        helper.assertTrue(rarities.listElements().count() == 6, "six rarities");
        Set<Integer> orders = new HashSet<>();
        rarities.listElements().forEach(r -> orders.add(r.value().order()));
        helper.assertTrue(orders.size() == 6, "rarity orders are unique");
        Rarity bad = rarities.getOrThrow(SbCoreEntries.BAD).value();
        Rarity common = rarities.getOrThrow(SbCoreEntries.COMMON).value();
        Rarity mythic = rarities.getOrThrow(SbCoreEntries.MYTHIC).value();
        helper.assertTrue(bad.order() < common.order() && bad.reforgeCostMultiplier() < common.reforgeCostMultiplier(),
                "bad sits below common, and a bad item is cheaper to reforge away");
        helper.assertTrue(common.order() < mythic.order() && common.reforgeCostMultiplier() < mythic.reforgeCostMultiplier(),
                "common sits below mythic in order and reforge cost");
        helper.assertTrue(mythic.rainbow() && !common.rainbow(), "only the top rarity is the rainbow one");
        helper.assertTrue(common.tooltipKey(SbCoreEntries.COMMON).equals("rarity.sb_core.common"), "the tooltip key derives from the id");

        helper.assertTrue(seals.listElements().count() == 6, "six Seals");
        Seal sealII = seals.getOrThrow(SbCoreEntries.SEAL_II).value();
        helper.assertTrue(sealII.order() == 2 && sealII.unlocks().contains(SbCoreEntries.NETHER_IGNITION), "Seal II unlocks Nether ignition");

        Tier tier1 = tiers.getOrThrow(SbCoreEntries.TIER_1).value();
        helper.assertTrue(tier1.order() == 1 && tier1.requiresSeal().isEmpty(), "the starting tier requires no Seal");
        helper.assertTrue(tier1.healthBudget() > 0 && tier1.defenseBudget() > 0 && tier1.reforgeBaseCost() > 0, "the tier budgets are set");

        ReforgeModifier lucky = modifiers.getOrThrow(SbCoreEntries.LUCKY).value();
        helper.assertTrue(rarities.get(lucky.rarity()).isPresent(), "Lucky rarity is a registered rarity");
        helper.assertTrue(lucky.stats().size() == 1 && lucky.stats().getFirst().attribute().equals(SbCoreEntries.CRIT_CHANCE), "Lucky adds crit chance");
        helper.assertTrue(lucky.prefixKey(SbCoreEntries.LUCKY).equals("reforge.sb_core.lucky"), "the prefix key derives from the id");
        ReforgeModifier broken = modifiers.getOrThrow(SbCoreEntries.BROKEN).value();
        helper.assertTrue(broken.rarity().equals(SbCoreEntries.BAD) && broken.stats().getFirst().amount() < 0.0,
                "a bad prefix is a penalty, not a prize");

        // Four pools (decision 0022), each over its own tag, each listing registered modifiers.
        helper.assertTrue(pools.listElements().count() == 4, "four reforge pools: tools, melee, guns, magic");
        for (ResourceKey<ReforgePool> key : List.of(SbCoreEntries.TOOL_POOL, SbCoreEntries.MELEE_POOL, SbCoreEntries.GUN_POOL, SbCoreEntries.MAGIC_POOL)) {
            ReforgePool pool = pools.getOrThrow(key).value();
            helper.assertTrue(pool.totalWeight() > 0, key.identifier() + " has weighted entries");
            pool.entries().forEach(entry -> helper.assertTrue(modifiers.get(entry.modifier()).isPresent(),
                    key.identifier() + " lists the registered modifier " + entry.modifier().identifier()));
            helper.assertTrue(pool.entries().stream().anyMatch(entry -> entry.modifier().equals(SbCoreEntries.BROKEN)),
                    key.identifier() + " can roll the bad prefix");
        }
        // The tags are core, empty by default, and merged from content modules; the pools load either way.
        helper.assertTrue(pools.getOrThrow(SbCoreEntries.MELEE_POOL).value().entries().size() == 4, "the melee pool has its four first-pass entries");
        helper.succeed();
    }

    /** Our attributes are on players with the documented defaults, and the config's defaults are what the docs claim. */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ATTRIBUTES_AND_CONFIG =
            TEST_FUNCTIONS.register("attributes_and_config", () -> SbCoreTestFunctions::attributesAndConfig);

    private static void attributesAndConfig(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        SbAttributes.ALL.forEach(attribute ->
                helper.assertTrue(player.getAttributes().hasAttribute(attribute), "players carry " + attribute.getId()));
        helper.assertTrue(player.getAttributeValue(SbAttributes.MELEE_DAMAGE) == 1.0, "class multipliers start at 1.0");
        helper.assertTrue(player.getAttributeValue(SbAttributes.CRIT_CHANCE) == 0.04, "base crit chance is 4% (decision 0016)");
        helper.assertTrue(player.getAttributeValue(SbAttributes.CRIT_MULTIPLIER) == 2.0, "a crit deals 2x");

        // Mobs carry the same attributes so they can use our weapons, at a fraction of a player (decision 0023).
        Pig pig = helper.spawn(EntityTypes.PIG, new BlockPos(2, 1, 2));
        SbAttributes.ALL.forEach(attribute ->
                helper.assertTrue(pig.getAttributes().hasAttribute(attribute), "mobs carry " + attribute.getId()));
        helper.assertTrue(pig.getAttributeValue(SbAttributes.MELEE_DAMAGE) == 1.0, "a mob class multiplier starts at 1.0 like a player");
        helper.assertTrue(pig.getAttributeValue(SbAttributes.WEAPON_PROFICIENCY) == SbAttributes.MOB_PROFICIENCY,
                "a mob starts at the mob share of a weapon built for a player");
        helper.assertTrue(SbAttributes.proficiencyOf(player) == 1.0, "a player gets everything out of their weapon");
        helper.assertTrue(SbAttributes.proficiencyOf(pig) == SbAttributes.MOB_PROFICIENCY,
                "a mob gets the configured share, so a stolen weapon keeps its moveset but hits softer");

        helper.assertTrue(SbConfig.enemyHealthScalar() == 1.0 && SbConfig.enemyDamageScalar() == 1.0 && SbConfig.critDamageScalar() == 1.0,
                "difficulty scalars default to the tuned experience");
        helper.assertTrue(SbConfig.mobWeaponProficiencyScalar() == 1.0, "the mob proficiency scalar defaults to the tuned experience");
        helper.assertTrue(SbConfig.dropCoinsOnDeath() && SbConfig.keepGearOnDeath(), "death drops coins and keeps gear (decision 0013)");
        helper.assertTrue(SbConfig.sealMode() == SbConfig.SealMode.WORLD, "Seals are world-wide (decision 0016)");
        helper.succeed();
    }

    public static void register(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    private SbCoreTestFunctions() {
    }
}
