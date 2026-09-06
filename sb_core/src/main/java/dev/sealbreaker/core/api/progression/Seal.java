package dev.sealbreaker.core.api.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;

import java.util.List;

/**
 * A Seal (PRD 3.1): one of the world-level gates a boss guards; breaking it is a world-wide event that unlocks
 * content for everyone. Authored as JSON in the {@code sb:seal} datapack registry
 * ({@code data/<mod>/sb/seal/<name>.json}). Boss and unlock names stay placeholders until the Tier 1 design session.
 *
 * @param order   position in the Seal chain, 1 first
 * @param boss    the boss entity whose defeat breaks the Seal; a key, so the entry may name a boss another module
 *                registers later
 * @param unlocks ids of the unlock flags this Seal grants (for example {@code sb_core:nether_ignition}); systems
 *                ask the Seal state "is this flag unlocked" rather than "is Seal N broken"
 */
public record Seal(int order, ResourceKey<EntityType<?>> boss, List<Identifier> unlocks) {
    public static final Codec<Seal> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("order").forGetter(Seal::order),
            ResourceKey.codec(Registries.ENTITY_TYPE).fieldOf("boss").forGetter(Seal::boss),
            Identifier.CODEC.listOf().optionalFieldOf("unlocks", List.of()).forGetter(Seal::unlocks)
    ).apply(i, Seal::new));
}
