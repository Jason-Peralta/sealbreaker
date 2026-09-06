package dev.sealbreaker.core.api.reforge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * One attribute change a reforge modifier makes. Operations use vanilla's names ({@code add_value},
 * {@code add_multiplied_base}, {@code add_multiplied_total}) so the JSON reads like an {@code attribute_modifiers}
 * component entry.
 *
 * @param attribute the attribute, vanilla or ours; a key, resolved when the modifier is applied
 * @param amount    the modifier amount
 * @param operation how the amount combines with the base value
 */
public record StatModifier(ResourceKey<Attribute> attribute, double amount, AttributeModifier.Operation operation) {
    public static final Codec<StatModifier> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceKey.codec(Registries.ATTRIBUTE).fieldOf("attribute").forGetter(StatModifier::attribute),
            Codec.DOUBLE.fieldOf("amount").forGetter(StatModifier::amount),
            AttributeModifier.Operation.CODEC.fieldOf("operation").forGetter(StatModifier::operation)
    ).apply(i, StatModifier::new));

    /** The vanilla modifier this stat becomes on an item, under the namespaced id that identifies its reforge. */
    public AttributeModifier toAttributeModifier(Identifier id) {
        return new AttributeModifier(id, this.amount, this.operation);
    }
}
