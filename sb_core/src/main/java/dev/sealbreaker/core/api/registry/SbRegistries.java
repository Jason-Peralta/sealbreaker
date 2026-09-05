package dev.sealbreaker.core.api.registry;

import dev.sealbreaker.core.api.combat.WeaponArchetype;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/**
 * Keys of the datapack registries this project adds. All live under the {@code sb} namespace, so an
 * entry authored by the combat module sits at {@code data/sb_combat/sb/weapon_archetype/<name>.json}.
 * Every registry is declared once, in {@link dev.sealbreaker.core.SbCore}, and documented in
 * {@code docs/data/}.
 */
public final class SbRegistries {
    public static final String NAMESPACE = "sb";

    public static final ResourceKey<Registry<WeaponArchetype>> WEAPON_ARCHETYPE = key("weapon_archetype");

    private static <T> ResourceKey<Registry<T>> key(String path) {
        return ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(NAMESPACE, path));
    }

    private SbRegistries() {
    }
}
