package dev.sealbreaker.core.datagen;

import dev.sealbreaker.core.SbCore;
import dev.sealbreaker.core.api.damage.DamageClass;
import dev.sealbreaker.core.api.progression.Seal;
import dev.sealbreaker.core.api.progression.Tier;
import dev.sealbreaker.core.api.rarity.Rarity;
import dev.sealbreaker.core.api.reforge.ReforgeModifier;
import dev.sealbreaker.core.api.reforge.ReforgePool;
import dev.sealbreaker.core.api.reforge.StatModifier;
import dev.sealbreaker.core.api.registry.SbRegistries;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.Optional;

/**
 * The core module's own entries of the {@code sb:*} registries, written by datagen to
 * {@code src/generated/resources/data/sb_core/sb/<registry>/<name>.json} and committed. Content modules add
 * their entries in their own namespaces the same way. Everything here is either fixed by the PRD (the five damage
 * classes, the six rarities, the six Seals) or a first-pass slice value marked as such in {@code docs/data/}.
 */
public final class SbCoreEntries {
    // damage classes (PRD 3.2): one per class, each scaled by its own attribute (registered by the attributes ticket)
    public static final ResourceKey<DamageClass> MELEE = damageClass("melee");
    public static final ResourceKey<DamageClass> RANGED = damageClass("ranged");
    public static final ResourceKey<DamageClass> MAGIC = damageClass("magic");
    public static final ResourceKey<DamageClass> SUMMON = damageClass("summon");
    public static final ResourceKey<DamageClass> HEALING = damageClass("healing");

    // rarities (decision 0022), lowest first; BAD sits below COMMON because a reforge prefix can be a penalty
    public static final ResourceKey<Rarity> BAD = rarity("bad");
    public static final ResourceKey<Rarity> COMMON = rarity("common");
    public static final ResourceKey<Rarity> RARE = rarity("rare");
    public static final ResourceKey<Rarity> EPIC = rarity("epic");
    public static final ResourceKey<Rarity> LEGENDARY = rarity("legendary");
    public static final ResourceKey<Rarity> MYTHIC = rarity("mythic");

    // the six Seals (PRD 3.1); bosses and most unlocks are placeholders until their design sessions
    public static final ResourceKey<Seal> SEAL_I = seal("seal_i");
    public static final ResourceKey<Seal> SEAL_II = seal("seal_ii");
    public static final ResourceKey<Seal> SEAL_III = seal("seal_iii");
    public static final ResourceKey<Seal> SEAL_IV = seal("seal_iv");
    public static final ResourceKey<Seal> SEAL_V = seal("seal_v");
    public static final ResourceKey<Seal> SEAL_VI = seal("seal_vi");
    /** Nether portal ignition opens with Seal II (PRD section 2, decided). */
    public static final Identifier NETHER_IGNITION = Identifier.fromNamespaceAndPath(SbCore.MOD_ID, "nether_ignition");

    /** The starting tier, the only one designed so far (decision 0009). */
    public static final ResourceKey<Tier> TIER_1 = tier("tier_1");

    // Reforge modifiers: a first-pass set that exercises every pool and both directions (a prefix can be a
    // penalty). The 12 Tier 1 modifiers arrive with the reforging ticket.
    /** Lucky, the crit prefix: crit rides on gear, set bonuses, accessories and prefixes (decision 0022). */
    public static final ResourceKey<ReforgeModifier> LUCKY = modifier("lucky");
    /** The one bad prefix of the first pass, in every pool, so the grey rarity is real from the start. */
    public static final ResourceKey<ReforgeModifier> BROKEN = modifier("broken");
    public static final ResourceKey<ReforgeModifier> QUICK = modifier("quick");
    public static final ResourceKey<ReforgeModifier> STURDY = modifier("sturdy");
    public static final ResourceKey<ReforgeModifier> HEAVY = modifier("heavy");
    public static final ResourceKey<ReforgeModifier> DEADLY = modifier("deadly");
    public static final ResourceKey<ReforgeModifier> ARCANE = modifier("arcane");

    /**
     * The four reforge pools (decision 0022): tools, melee weapons, guns (bows included) and magic (magic,
     * healer and summoner items). Every tag is declared empty by core
     * ({@code data/sb_core/tags/item/<name>.json}) so the pools load in any world; content modules append their
     * items from their own jars, because tags merge.
     */
    public static final ResourceKey<ReforgePool> TOOL_POOL = pool("tools");
    public static final ResourceKey<ReforgePool> MELEE_POOL = pool("melee_weapons");
    public static final ResourceKey<ReforgePool> GUN_POOL = pool("guns");
    public static final ResourceKey<ReforgePool> MAGIC_POOL = pool("magic");
    public static final TagKey<Item> TOOLS_TAG = itemTag("tools");
    public static final TagKey<Item> MELEE_WEAPONS_TAG = itemTag("melee_weapons");
    public static final TagKey<Item> GUNS_TAG = itemTag("guns");
    public static final TagKey<Item> MAGIC_TAG = itemTag("magic");

    /** Attributes the attributes ticket registers; named by key here so the data loads before the Java exists. */
    public static final ResourceKey<Attribute> MELEE_DAMAGE = attribute("melee_damage");
    public static final ResourceKey<Attribute> RANGED_DAMAGE = attribute("ranged_damage");
    public static final ResourceKey<Attribute> MAGIC_DAMAGE = attribute("magic_damage");
    public static final ResourceKey<Attribute> SUMMON_DAMAGE = attribute("summon_damage");
    public static final ResourceKey<Attribute> HEALING_POWER = attribute("healing_power");
    public static final ResourceKey<Attribute> CRIT_CHANCE = attribute("crit_chance");

    /** Every registry with the core entries, for datagen and for tests that want the same objects. */
    public static RegistrySetBuilder builder() {
        return new RegistrySetBuilder()
                .add(SbRegistries.DAMAGE_CLASS, SbCoreEntries::damageClasses)
                .add(SbRegistries.RARITY, SbCoreEntries::rarities)
                .add(SbRegistries.SEAL, SbCoreEntries::seals)
                .add(SbRegistries.TIER, SbCoreEntries::tiers)
                .add(SbRegistries.REFORGE_MODIFIER, SbCoreEntries::modifiers)
                .add(SbRegistries.REFORGE_POOL, SbCoreEntries::pools);
    }

    static void damageClasses(BootstrapContext<DamageClass> context) {
        context.register(MELEE, new DamageClass("melee", MELEE_DAMAGE));
        context.register(RANGED, new DamageClass("ranged", RANGED_DAMAGE));
        context.register(MAGIC, new DamageClass("magic", MAGIC_DAMAGE));
        context.register(SUMMON, new DamageClass("summon", SUMMON_DAMAGE));
        context.register(HEALING, new DamageClass("healing", HEALING_POWER));
    }

    /** The ladder of decision 0022; cost multipliers are first-pass slice values (docs/data/rarity.md). */
    static void rarities(BootstrapContext<Rarity> context) {
        context.register(BAD, new Rarity(0x808080, 0, Optional.empty(), 0.75f, false, Optional.empty()));
        context.register(COMMON, new Rarity(0xFFFFFF, 1, Optional.empty(), 1.0f, false, Optional.empty()));
        context.register(RARE, new Rarity(0x5555FF, 2, Optional.empty(), 1.5f, false, Optional.empty()));
        context.register(EPIC, new Rarity(0xAA55FF, 3, Optional.empty(), 2.0f, false, Optional.empty()));
        context.register(LEGENDARY, new Rarity(0xFFAA00, 4, Optional.empty(), 3.0f, false, Optional.empty()));
        // The top rarity cycles through the spectrum; the static colour is the fallback for chat and logs.
        context.register(MYTHIC, new Rarity(0xFF55FF, 5, Optional.empty(), 4.0f, true, Optional.empty()));
    }

    static void seals(BootstrapContext<Seal> context) {
        context.register(SEAL_I, new Seal(1, boss("boss_i"), List.of()));
        context.register(SEAL_II, new Seal(2, boss("boss_ii"), List.of(NETHER_IGNITION)));
        context.register(SEAL_III, new Seal(3, boss("boss_iii"), List.of()));
        context.register(SEAL_IV, new Seal(4, boss("boss_iv"), List.of()));
        context.register(SEAL_V, new Seal(5, boss("boss_v"), List.of()));
        context.register(SEAL_VI, new Seal(6, boss("boss_vi"), List.of()));
    }

    /** Budgets and cost are first-pass slice values (docs/data/tier.md). */
    static void tiers(BootstrapContext<Tier> context) {
        context.register(TIER_1, new Tier(1, 30, 8, Optional.empty(), 10));
    }

    static void modifiers(BootstrapContext<ReforgeModifier> context) {
        context.register(LUCKY, new ReforgeModifier(Optional.empty(), RARE,
                List.of(stat(CRIT_CHANCE, 0.04, AttributeModifier.Operation.ADD_VALUE))));
        context.register(BROKEN, new ReforgeModifier(Optional.empty(), BAD,
                List.of(stat(Attributes.ATTACK_DAMAGE, -0.10, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL))));
        context.register(QUICK, new ReforgeModifier(Optional.empty(), COMMON,
                List.of(stat(Attributes.ATTACK_SPEED, 0.10, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL))));
        context.register(STURDY, new ReforgeModifier(Optional.empty(), COMMON,
                List.of(stat(Attributes.BLOCK_BREAK_SPEED, 0.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL))));
        context.register(HEAVY, new ReforgeModifier(Optional.empty(), COMMON, List.of(
                stat(Attributes.ATTACK_DAMAGE, 0.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                stat(Attributes.ATTACK_SPEED, -0.10, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL))));
        context.register(DEADLY, new ReforgeModifier(Optional.empty(), COMMON,
                List.of(stat(RANGED_DAMAGE, 0.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL))));
        context.register(ARCANE, new ReforgeModifier(Optional.empty(), COMMON,
                List.of(stat(MAGIC_DAMAGE, 0.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL))));
    }

    /**
     * One pool per item family (decision 0022). Which pool an item belongs to is its tag membership, so a spear
     * cannot roll a sweep prefix simply because no pool that contains spears lists one.
     */
    static void pools(BootstrapContext<ReforgePool> context) {
        HolderGetter<Item> items = context.lookup(Registries.ITEM);
        context.register(TOOL_POOL, new ReforgePool(items.getOrThrow(TOOLS_TAG), List.of(
                entry(BROKEN, 5), entry(QUICK, 10), entry(STURDY, 10))));
        context.register(MELEE_POOL, new ReforgePool(items.getOrThrow(MELEE_WEAPONS_TAG), List.of(
                entry(BROKEN, 5), entry(QUICK, 10), entry(HEAVY, 10), entry(LUCKY, 10))));
        context.register(GUN_POOL, new ReforgePool(items.getOrThrow(GUNS_TAG), List.of(
                entry(BROKEN, 5), entry(DEADLY, 10), entry(LUCKY, 10))));
        context.register(MAGIC_POOL, new ReforgePool(items.getOrThrow(MAGIC_TAG), List.of(
                entry(BROKEN, 5), entry(ARCANE, 10), entry(LUCKY, 10))));
    }

    private static StatModifier stat(ResourceKey<Attribute> attribute, double amount, AttributeModifier.Operation operation) {
        return new StatModifier(attribute, amount, operation);
    }

    private static StatModifier stat(Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation) {
        return new StatModifier(attribute.getKey(), amount, operation);
    }

    private static ReforgePool.Entry entry(ResourceKey<ReforgeModifier> modifier, int weight) {
        return new ReforgePool.Entry(modifier, weight);
    }

    private static TagKey<Item> itemTag(String name) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(SbCore.MOD_ID, name));
    }

    private static ResourceKey<DamageClass> damageClass(String name) {
        return ResourceKey.create(SbRegistries.DAMAGE_CLASS, Identifier.fromNamespaceAndPath(SbCore.MOD_ID, name));
    }

    private static ResourceKey<Rarity> rarity(String name) {
        return ResourceKey.create(SbRegistries.RARITY, Identifier.fromNamespaceAndPath(SbCore.MOD_ID, name));
    }

    private static ResourceKey<Seal> seal(String name) {
        return ResourceKey.create(SbRegistries.SEAL, Identifier.fromNamespaceAndPath(SbCore.MOD_ID, name));
    }

    private static ResourceKey<Tier> tier(String name) {
        return ResourceKey.create(SbRegistries.TIER, Identifier.fromNamespaceAndPath(SbCore.MOD_ID, name));
    }

    private static ResourceKey<ReforgeModifier> modifier(String name) {
        return ResourceKey.create(SbRegistries.REFORGE_MODIFIER, Identifier.fromNamespaceAndPath(SbCore.MOD_ID, name));
    }

    private static ResourceKey<ReforgePool> pool(String name) {
        return ResourceKey.create(SbRegistries.REFORGE_POOL, Identifier.fromNamespaceAndPath(SbCore.MOD_ID, name));
    }

    private static ResourceKey<Attribute> attribute(String name) {
        return ResourceKey.create(Registries.ATTRIBUTE, Identifier.fromNamespaceAndPath(SbCore.MOD_ID, name));
    }

    private static ResourceKey<EntityType<?>> boss(String placeholder) {
        return ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("sb_bosses", placeholder));
    }

    private SbCoreEntries() {
    }
}
