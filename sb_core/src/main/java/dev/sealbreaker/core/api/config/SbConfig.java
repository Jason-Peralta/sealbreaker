package dev.sealbreaker.core.api.config;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The pack's one config file, {@code config/sb_core-common.toml}: the difficulty scalars, the death rules and the
 * Seal mode. Common, because both sides read some of it and the server's copy is what counts in multiplayer.
 *
 * <p>Values are read through the static getters, never by holding a {@code ConfigValue}: the getters return the
 * cached primitives that {@link #onLoad} refreshes, so a config reload (the file edited, or a
 * {@code /reload}-triggered reload) takes effect without a restart and without every caller re-reading TOML.
 * Balance numbers that belong to content (damage, budgets, costs) live in the datapack registries instead; this
 * file is only for the knobs a server owner turns.
 */
public final class SbConfig {
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.DoubleValue ENEMY_HEALTH_SCALAR;
    private static final ModConfigSpec.DoubleValue ENEMY_DAMAGE_SCALAR;
    private static final ModConfigSpec.DoubleValue CRIT_DAMAGE_SCALAR;
    private static final ModConfigSpec.DoubleValue MOB_WEAPON_PROFICIENCY_SCALAR;
    private static final ModConfigSpec.BooleanValue DROP_COINS;
    private static final ModConfigSpec.BooleanValue KEEP_GEAR;
    private static final ModConfigSpec.EnumValue<SealMode> SEAL_MODE;

    // Cached, so gameplay code never touches the TOML; refreshed on load and on reload.
    private static double enemyHealthScalar = 1.0;
    private static double enemyDamageScalar = 1.0;
    private static double critDamageScalar = 1.0;
    private static double mobWeaponProficiencyScalar = 1.0;
    private static boolean dropCoinsOnDeath = true;
    private static boolean keepGearOnDeath = true;
    private static SealMode sealMode = SealMode.WORLD;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Difficulty scalars applied to our enemies and bosses on top of their tier budget (PRD 3.16).",
                        "1.0 is the tuned experience; a group of four may raise them, a solo player may lower them.")
                .push("difficulty");
        ENEMY_HEALTH_SCALAR = builder.comment("Multiplies the health of our enemies and bosses.")
                .defineInRange("enemy_health_scalar", 1.0, 0.1, 10.0);
        ENEMY_DAMAGE_SCALAR = builder.comment("Multiplies the damage our enemies and bosses deal.")
                .defineInRange("enemy_damage_scalar", 1.0, 0.1, 10.0);
        CRIT_DAMAGE_SCALAR = builder.comment("Multiplies the crit damage multiplier attribute (2x by default, decision 0016).")
                .defineInRange("crit_damage_scalar", 1.0, 0.1, 4.0);
        MOB_WEAPON_PROFICIENCY_SCALAR = builder.comment(
                        "Scales what a mob gets out of one of our weapons it picked up (decision 0023).",
                        "The mob keeps the moveset and the effects; its damage, healing and knockback scale by its",
                        "weapon proficiency attribute times this. 0 makes a stolen weapon harmless, 1 makes a zombie",
                        "as dangerous with it as the mob type is meant to be.")
                .defineInRange("mob_weapon_proficiency_scalar", 1.0, 0.0, 4.0);
        builder.pop();

        builder.comment("What death costs (PRD 3.12, decision 0013).").push("death");
        DROP_COINS = builder.comment("Drop all carried coins where you fell, as an ordinary pickup anyone can collect.")
                .define("drop_coins", true);
        KEEP_GEAR = builder.comment("Keep gear on death; only coins drop.")
                .define("keep_gear", true);
        builder.pop();

        builder.comment("How Seals are shared (PRD 3.1; decision 0016 fixes the default at world-wide).").push("seals");
        SEAL_MODE = builder.comment("WORLD: breaking a Seal opens it for everyone. PER_PLAYER: each player breaks their own (not supported yet; reserved).")
                .defineEnum("mode", SealMode.WORLD);
        builder.pop();

        SPEC = builder.build();
    }

    /** How Seal progress is shared between players. */
    public enum SealMode {
        WORLD,
        PER_PLAYER
    }

    /** Called by {@code SbCore}: registers the file and keeps the cached values fresh. */
    public static void register(ModContainer container, net.neoforged.bus.api.IEventBus modEventBus) {
        container.registerConfig(ModConfig.Type.COMMON, SPEC);
        modEventBus.addListener(ModConfigEvent.Loading.class, event -> onLoad(event.getConfig()));
        modEventBus.addListener(ModConfigEvent.Reloading.class, event -> onLoad(event.getConfig()));
    }

    private static void onLoad(ModConfig config) {
        if (config.getSpec() != SPEC) {
            return;
        }
        enemyHealthScalar = ENEMY_HEALTH_SCALAR.get();
        enemyDamageScalar = ENEMY_DAMAGE_SCALAR.get();
        critDamageScalar = CRIT_DAMAGE_SCALAR.get();
        mobWeaponProficiencyScalar = MOB_WEAPON_PROFICIENCY_SCALAR.get();
        dropCoinsOnDeath = DROP_COINS.get();
        keepGearOnDeath = KEEP_GEAR.get();
        sealMode = SEAL_MODE.get();
    }

    public static double enemyHealthScalar() {
        return enemyHealthScalar;
    }

    public static double enemyDamageScalar() {
        return enemyDamageScalar;
    }

    public static double critDamageScalar() {
        return critDamageScalar;
    }

    /** Scales a non-player wielder's weapon proficiency (decision 0023). */
    public static double mobWeaponProficiencyScalar() {
        return mobWeaponProficiencyScalar;
    }

    public static boolean dropCoinsOnDeath() {
        return dropCoinsOnDeath;
    }

    public static boolean keepGearOnDeath() {
        return keepGearOnDeath;
    }

    public static SealMode sealMode() {
        return sealMode;
    }

    private SbConfig() {
    }
}
