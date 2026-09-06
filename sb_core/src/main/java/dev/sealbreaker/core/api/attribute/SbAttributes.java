package dev.sealbreaker.core.api.attribute;

import dev.sealbreaker.core.SbCore;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/**
 * Our attributes: the class damage multipliers of the five damage classes, and the crit numbers. Every one is on
 * players (added through {@link EntityAttributeModificationEvent}), syncable so a client can show the value, and
 * referenced from data by id, so a {@code sb:damage_class} or {@code sb:reforge_modifier} entry names one without
 * this class being loaded first.
 *
 * <p>The class multipliers default to 1.0 and multiply the damage a weapon of that class deals; gear, accessories
 * and reforge prefixes raise them. Crit chance defaults to the 4% every class starts with and crit multiplier to
 * the 2x of decision 0016; both are attributes rather than constants so gear, the {@code Lucky} prefix and
 * accessories are the only things that change them.
 */
@EventBusSubscriber(modid = SbCore.MOD_ID)
public final class SbAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(Registries.ATTRIBUTE, SbCore.MOD_ID);

    public static final DeferredHolder<Attribute, Attribute> MELEE_DAMAGE = classMultiplier("melee_damage");
    public static final DeferredHolder<Attribute, Attribute> RANGED_DAMAGE = classMultiplier("ranged_damage");
    public static final DeferredHolder<Attribute, Attribute> MAGIC_DAMAGE = classMultiplier("magic_damage");
    public static final DeferredHolder<Attribute, Attribute> SUMMON_DAMAGE = classMultiplier("summon_damage");
    public static final DeferredHolder<Attribute, Attribute> HEALING_POWER = classMultiplier("healing_power");

    /** Chance for a swing to crit, 0 to 1. Base 4% for every class; only gear, Lucky and accessories add to it. */
    public static final DeferredHolder<Attribute, Attribute> CRIT_CHANCE = ATTRIBUTES.register("crit_chance",
            () -> ranged("crit_chance", 0.04, 0.0, 1.0));
    /** Damage multiplier of a crit. 2x (decision 0016); a config scalar, not a tuning dial for gear. */
    public static final DeferredHolder<Attribute, Attribute> CRIT_MULTIPLIER = ATTRIBUTES.register("crit_multiplier",
            () -> ranged("crit_multiplier", 2.0, 1.0, 16.0));

    /** Every attribute we add; all of them are put on players. */
    public static final List<DeferredHolder<Attribute, Attribute>> ALL = List.of(
            MELEE_DAMAGE, RANGED_DAMAGE, MAGIC_DAMAGE, SUMMON_DAMAGE, HEALING_POWER, CRIT_CHANCE, CRIT_MULTIPLIER);

    private static DeferredHolder<Attribute, Attribute> classMultiplier(String name) {
        return ATTRIBUTES.register(name, () -> ranged(name, 1.0, 0.0, 1024.0));
    }

    private static Attribute ranged(String name, double defaultValue, double min, double max) {
        return new RangedAttribute("attribute.name." + SbCore.MOD_ID + "." + name, defaultValue, min, max).setSyncable(true);
    }

    /** Every player carries our attributes; mobs get only what they are given (a boss's class damage, later). */
    @SubscribeEvent
    static void addToPlayers(EntityAttributeModificationEvent event) {
        for (DeferredHolder<Attribute, Attribute> attribute : ALL) {
            event.add(net.minecraft.world.entity.EntityTypes.PLAYER, attribute);
        }
    }

    /** Convenience for damage code: the attacker's value, or the attribute's default when the entity lacks it. */
    public static double valueOf(net.minecraft.world.entity.LivingEntity entity, Holder<Attribute> attribute) {
        return entity.getAttributes().hasAttribute(attribute) ? entity.getAttributeValue(attribute) : attribute.value().getDefaultValue();
    }

    /** Vanilla's attack damage, for code that needs both ours and vanilla's in one place. */
    public static Holder<Attribute> attackDamage() {
        return Attributes.ATTACK_DAMAGE;
    }

    public static void register(IEventBus modEventBus) {
        ATTRIBUTES.register(modEventBus);
    }

    private SbAttributes() {
    }
}
