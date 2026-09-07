package dev.sealbreaker.core.api.attribute;

import dev.sealbreaker.core.SbCore;
import dev.sealbreaker.core.api.config.SbConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/**
 * Our attributes: the class damage multipliers of the five damage classes, the crit numbers, and how well a
 * wielder handles one of our weapons. Every living entity carries them (added through
 * {@link EntityAttributeModificationEvent}), they are syncable so a client can show the value, and data refers to
 * them by id, so a {@code sb:damage_class} or {@code sb:reforge_modifier} entry names one without this class
 * being loaded first.
 *
 * <p>The class multipliers default to 1.0 and multiply the damage a weapon of that class deals; gear, set
 * bonuses, accessories and reforge prefixes raise them. Crit chance defaults to the 4% every class starts with
 * and crit multiplier to the 2x of decision 0016; both are attributes rather than constants, so gear, set
 * bonuses, accessories and prefixes are the only things that move them.
 *
 * <p>Mobs carry the same attributes with lower base values (decision 0023): a zombie that picks up one of our
 * swords gets its moveset, but swings it at a fraction of a player's output. {@link #WEAPON_PROFICIENCY} is that
 * fraction, and a designer raises it per entity type to make an elite or a boss handle gear properly.
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
    /**
     * How much of one of our weapons a wielder gets out of it (decision 0023): 1.0 for players, {@link #MOB_PROFICIENCY}
     * for everything else. The moveset, reach and effects are the weapon's; the numbers are scaled by this.
     */
    public static final DeferredHolder<Attribute, Attribute> WEAPON_PROFICIENCY = ATTRIBUTES.register("weapon_proficiency",
            () -> ranged("weapon_proficiency", 1.0, 0.0, 4.0));

    /** What a mob gets out of a weapon built for a player, before its own modifiers (decision 0023). */
    public static final double MOB_PROFICIENCY = 0.5;

    /** Every attribute we add; all of them are put on every living entity. */
    public static final List<DeferredHolder<Attribute, Attribute>> ALL = List.of(
            MELEE_DAMAGE, RANGED_DAMAGE, MAGIC_DAMAGE, SUMMON_DAMAGE, HEALING_POWER, CRIT_CHANCE, CRIT_MULTIPLIER, WEAPON_PROFICIENCY);

    private static DeferredHolder<Attribute, Attribute> classMultiplier(String name) {
        return ATTRIBUTES.register(name, () -> ranged(name, 1.0, 0.0, 1024.0));
    }

    private static Attribute ranged(String name, double defaultValue, double min, double max) {
        return new RangedAttribute("attribute.name." + SbCore.MOD_ID + "." + name, defaultValue, min, max).setSyncable(true);
    }

    /**
     * Every living entity carries our attributes, so a mob that picks up one of our weapons can use it
     * (decision 0023). Non-players start at {@link #MOB_PROFICIENCY} weapon proficiency; a boss or an elite gets
     * a higher base value from its own attribute supplier, or a modifier from its gear.
     */
    @SubscribeEvent
    static void addToLivingEntities(EntityAttributeModificationEvent event) {
        for (EntityType<? extends LivingEntity> type : event.getTypes()) {
            boolean player = type == EntityTypes.PLAYER;
            for (DeferredHolder<Attribute, Attribute> attribute : ALL) {
                if (attribute == WEAPON_PROFICIENCY && !player) {
                    event.add(type, attribute, MOB_PROFICIENCY);
                } else {
                    event.add(type, attribute);
                }
            }
        }
    }

    /**
     * The share of a weapon's player-tuned output this wielder gets: its {@link #WEAPON_PROFICIENCY}, scaled by
     * the config's mob scalar for non-players. Damage, healing and knockback multiply by this; the moveset, the
     * reach and the effects do not, so a zombie with a special sword still performs the special attack.
     */
    public static double proficiencyOf(LivingEntity wielder) {
        double proficiency = valueOf(wielder, WEAPON_PROFICIENCY);
        return wielder instanceof Player ? proficiency : proficiency * SbConfig.mobWeaponProficiencyScalar();
    }

    /** Convenience for damage code: the wielder's value, or the attribute's default when the entity lacks it. */
    public static double valueOf(LivingEntity entity, Holder<Attribute> attribute) {
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
