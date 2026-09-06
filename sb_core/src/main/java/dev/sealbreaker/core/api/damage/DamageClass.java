package dev.sealbreaker.core.api.damage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.attributes.Attribute;

/**
 * A damage class (PRD 3.2): the class a weapon's damage belongs to, and the attribute that scales it. Authored
 * as JSON in the {@code sb:damage_class} datapack registry ({@code data/<mod>/sb/damage_class/<name>.json}).
 *
 * @param name      short name used in lang keys ({@code damage_class.<name>}) and tooltips
 * @param attribute the multiplier attribute of the class, for example {@code sb_core:melee_damage}; resolved
 *                  against the attribute registry when damage is dealt, so an entry may name an attribute that
 *                  another module registers
 */
public record DamageClass(String name, ResourceKey<Attribute> attribute) {
    public static final Codec<DamageClass> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(DamageClass::name),
            ResourceKey.codec(Registries.ATTRIBUTE).fieldOf("attribute").forGetter(DamageClass::attribute)
    ).apply(i, DamageClass::new));

    /** Lang key of the class name shown in tooltips. */
    public String translationKey() {
        return "damage_class." + this.name;
    }
}
