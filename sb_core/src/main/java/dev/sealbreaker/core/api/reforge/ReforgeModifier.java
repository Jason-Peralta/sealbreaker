package dev.sealbreaker.core.api.reforge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.sealbreaker.core.api.rarity.Rarity;
import dev.sealbreaker.core.api.registry.SbRegistries;
import net.minecraft.resources.ResourceKey;

import java.util.List;
import java.util.Optional;

/**
 * A reforge modifier (PRD 3.5): the prefix an item can carry, its rarity and the attribute changes it brings.
 * Authored as JSON in the {@code sb:reforge_modifier} datapack registry
 * ({@code data/<mod>/sb/reforge_modifier/<name>.json}). An item carries at most one, as the
 * {@code sb_core:reforge_prefix} component holding the entry's id (decision 0018).
 *
 * @param prefixKey lang key of the prefix name; absent: {@code reforge.<namespace>.<path>}, which is what the
 *                  presentation derives from the component without a registry lookup
 * @param rarity    the rarity the prefix shows and is weighted by
 * @param stats     the attribute modifiers applied while the prefix is on the item
 */
public record ReforgeModifier(Optional<String> prefixKey, ResourceKey<Rarity> rarity, List<StatModifier> stats) {
    public static final Codec<ReforgeModifier> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("prefix_key").forGetter(ReforgeModifier::prefixKey),
            ResourceKey.codec(SbRegistries.RARITY).fieldOf("rarity").forGetter(ReforgeModifier::rarity),
            StatModifier.CODEC.listOf().optionalFieldOf("stats", List.of()).forGetter(ReforgeModifier::stats)
    ).apply(i, ReforgeModifier::new));

    /** The prefix name's lang key: the explicit one, else the convention the component presentation uses. */
    public String prefixKey(ResourceKey<ReforgeModifier> self) {
        return this.prefixKey.orElseGet(() -> "reforge." + self.identifier().getNamespace() + "." + self.identifier().getPath());
    }
}
