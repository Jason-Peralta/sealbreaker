package dev.sealbreaker.core.api.reforge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.sealbreaker.core.api.registry.SbRegistries;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

import java.util.List;

/**
 * A reforge pool (PRD 3.5): which modifiers a group of items can roll, and how likely each is. Authored as JSON
 * in the {@code sb:reforge_pool} datapack registry ({@code data/<mod>/sb/reforge_pool/<name>.json}). An item
 * rolls from every pool whose {@code applies_to} contains it; a spear cannot roll a sweep prefix because no pool
 * that contains spears lists it.
 *
 * @param appliesTo the items the pool serves: an item tag ({@code "#sb_gear:melee_weapons"}) or a list of ids
 * @param entries   the modifiers and their weights
 */
public record ReforgePool(HolderSet<Item> appliesTo, List<Entry> entries) {
    public static final Codec<ReforgePool> CODEC = RecordCodecBuilder.create(i -> i.group(
            RegistryCodecs.homogeneousList(Registries.ITEM).fieldOf("applies_to").forGetter(ReforgePool::appliesTo),
            Entry.CODEC.listOf(1, Integer.MAX_VALUE).fieldOf("entries").forGetter(ReforgePool::entries)
    ).apply(i, ReforgePool::new));

    /** Sum of the entry weights, the denominator of every roll. */
    public int totalWeight() {
        int total = 0;
        for (Entry entry : this.entries) {
            total += entry.weight();
        }
        return total;
    }

    /**
     * @param modifier the modifier; a key, so a pool may list modifiers another module's data adds
     * @param weight   relative chance, a positive integer
     */
    public record Entry(ResourceKey<ReforgeModifier> modifier, int weight) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceKey.codec(SbRegistries.REFORGE_MODIFIER).fieldOf("modifier").forGetter(Entry::modifier),
                Codec.intRange(1, Integer.MAX_VALUE).fieldOf("weight").forGetter(Entry::weight)
        ).apply(i, Entry::new));
    }
}
