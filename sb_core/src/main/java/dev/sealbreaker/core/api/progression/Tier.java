package dev.sealbreaker.core.api.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.sealbreaker.core.api.registry.SbRegistries;
import net.minecraft.resources.ResourceKey;

import java.util.Optional;

/**
 * A tier (PRD section 2): the band of content between two Seals, with the health and defence a player is expected
 * to have there, which every boss and enemy of the tier is tuned against, and the base reforge cost of its gear.
 * Authored as JSON in the {@code sb:tier} datapack registry ({@code data/<mod>/sb/tier/<name>.json}).
 *
 * @param order           1 for the starting tier
 * @param healthBudget    the health (in half-hearts) a player is expected to have at this tier
 * @param defenseBudget   the armor points a player is expected to have at this tier
 * @param requiresSeal    the Seal that must be broken to enter the tier; absent for the starting tier
 * @param reforgeBaseCost coins for one reforge of this tier's gear, before the rarity multiplier (PRD 3.5)
 */
public record Tier(int order, int healthBudget, int defenseBudget, Optional<ResourceKey<Seal>> requiresSeal, int reforgeBaseCost) {
    public static final Codec<Tier> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("order").forGetter(Tier::order),
            Codec.INT.fieldOf("health_budget").forGetter(Tier::healthBudget),
            Codec.INT.fieldOf("defense_budget").forGetter(Tier::defenseBudget),
            ResourceKey.codec(SbRegistries.SEAL).optionalFieldOf("requires_seal").forGetter(Tier::requiresSeal),
            Codec.INT.fieldOf("reforge_base_cost").forGetter(Tier::reforgeBaseCost)
    ).apply(i, Tier::new));
}
