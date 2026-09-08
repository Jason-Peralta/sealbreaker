package dev.sealbreaker.core.api.registry;

import dev.sealbreaker.core.api.combat.WeaponArchetype;
import dev.sealbreaker.core.api.damage.DamageClass;
import dev.sealbreaker.core.api.progression.Seal;
import dev.sealbreaker.core.api.progression.Tier;
import dev.sealbreaker.core.api.rarity.Rarity;
import dev.sealbreaker.core.api.reforge.ReforgeModifier;
import dev.sealbreaker.core.api.reforge.ReforgePool;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/**
 * Keys of the datapack registries this project adds. All live under the {@code sb} namespace, so an
 * entry authored by the combat module sits at {@code data/sb_combat/sb/weapon_archetype/<name>.json}.
 * Every registry is declared once, in {@link dev.sealbreaker.core.SbCore}, synced to clients, and documented in
 * {@code docs/data/}. Entries reference each other and vanilla registries by key, never by holder, so data from
 * one module may name what another module adds and the order of loading does not matter.
 */
public final class SbRegistries {
    public static final String NAMESPACE = "sb";

    public static final ResourceKey<Registry<WeaponArchetype>> WEAPON_ARCHETYPE = key("weapon_archetype");
    public static final ResourceKey<Registry<DamageClass>> DAMAGE_CLASS = key("damage_class");
    public static final ResourceKey<Registry<ReforgeModifier>> REFORGE_MODIFIER = key("reforge_modifier");
    public static final ResourceKey<Registry<ReforgePool>> REFORGE_POOL = key("reforge_pool");
    public static final ResourceKey<Registry<Rarity>> RARITY = key("rarity");
    public static final ResourceKey<Registry<Seal>> SEAL = key("seal");
    public static final ResourceKey<Registry<Tier>> TIER = key("tier");

    private static <T> ResourceKey<Registry<T>> key(String path) {
        return ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(NAMESPACE, path));
    }

    private SbRegistries() {
    }
}
