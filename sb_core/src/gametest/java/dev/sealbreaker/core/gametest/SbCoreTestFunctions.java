package dev.sealbreaker.core.gametest;

import dev.sealbreaker.core.SbCore;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.HashSet;
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
        Rarity common = rarities.getOrThrow(SbCoreEntries.COMMON).value();
        Rarity mythic = rarities.getOrThrow(SbCoreEntries.MYTHIC).value();
        helper.assertTrue(common.order() < mythic.order() && common.reforgeCostMultiplier() < mythic.reforgeCostMultiplier(),
                "common sits below mythic in order and reforge cost");
        helper.assertTrue(common.tooltipKey(SbCoreEntries.COMMON).equals("rarity.sb_core.common"), "the tooltip key derives from the id");

        helper.assertTrue(seals.listElements().count() == 6, "six Seals");
        Seal sealII = seals.getOrThrow(SbCoreEntries.SEAL_II).value();
        helper.assertTrue(sealII.order() == 2 && sealII.unlocks().contains(SbCoreEntries.NETHER_IGNITION), "Seal II unlocks Nether ignition");

        Tier tier1 = tiers.getOrThrow(SbCoreEntries.TIER_1).value();
        helper.assertTrue(tier1.order() == 1 && tier1.requiresSeal().isEmpty(), "the starting tier requires no Seal");
        helper.assertTrue(tier1.healthBudget() > 0 && tier1.defenseBudget() > 0 && tier1.reforgeBaseCost() > 0, "the tier budgets are set");

        ReforgeModifier lucky = modifiers.getOrThrow(SbCoreEntries.LUCKY).value();
        helper.assertTrue(rarities.get(lucky.rarity()).isPresent(), "Lucky's rarity is a registered rarity");
        helper.assertTrue(lucky.stats().size() == 1 && lucky.stats().getFirst().attribute().equals(SbCoreEntries.CRIT_CHANCE), "Lucky adds crit chance");
        helper.assertTrue(lucky.prefixKey(SbCoreEntries.LUCKY).equals("reforge.sb_core.lucky"), "the prefix key derives from the id");

        ReforgePool meleePool = pools.getOrThrow(SbCoreEntries.MELEE_WEAPONS).value();
        helper.assertTrue(meleePool.totalWeight() == 10 && modifiers.get(meleePool.entries().getFirst().modifier()).isPresent(),
                "the melee pool lists a registered modifier with its weight");
        helper.succeed();
    }

    public static void register(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    private SbCoreTestFunctions() {
    }
}
