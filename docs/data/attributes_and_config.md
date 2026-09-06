# Attributes and the config file

Not registries, but the other two places a number can live. The rule (CLAUDE.md) is unchanged: content numbers are datapack registries, these two are for what content cannot express.

## Attributes (`sb_core`)

Registered by `SbAttributes` into the vanilla attribute registry, so they work with vanilla's `attribute_modifiers` component, our reforge modifiers, Curios accessories and `/attribute`. Every one is syncable and is put on players by `EntityAttributeModificationEvent`; mobs only get what they are given.

| Attribute | Default | Range | Meaning |
|---|---|---|---|
| `sb_core:melee_damage` | 1.0 | 0 to 1024 | Multiplies damage a melee-class weapon deals. |
| `sb_core:ranged_damage` | 1.0 | 0 to 1024 | The same for ranged. |
| `sb_core:magic_damage` | 1.0 | 0 to 1024 | The same for magic. |
| `sb_core:summon_damage` | 1.0 | 0 to 1024 | The same for minion damage. |
| `sb_core:healing_power` | 1.0 | 0 to 1024 | Multiplies healing a healer-class item does. |
| `sb_core:crit_chance` | 0.04 | 0 to 1 | Chance a swing crits. 4% for every class (decision 0016); only gear, the `Lucky` prefix and accessories add to it. |
| `sb_core:crit_multiplier` | 2.0 | 1 to 16 | Damage multiplier of a crit (decision 0016), scaled by `difficulty.crit_damage_scalar`. |

A `sb:damage_class` entry names the multiplier attribute of its class, so damage code asks the class rather than hard-coding the attribute. Use `SbAttributes.valueOf(entity, attribute)` to read one: entities that do not carry the attribute (any mob) answer with its default instead of throwing.

## `config/sb_core-common.toml`

One common config file for the whole pack. Server-owner knobs only; balance lives in the registries. Read through `SbConfig`'s static getters, which are refreshed on load and on reload, so editing the file takes effect without a restart.

| Key | Default | Range | Meaning |
|---|---|---|---|
| `difficulty.enemy_health_scalar` | 1.0 | 0.1 to 10 | Multiplies the health of our enemies and bosses on top of their tier budget. |
| `difficulty.enemy_damage_scalar` | 1.0 | 0.1 to 10 | Multiplies the damage they deal. |
| `difficulty.crit_damage_scalar` | 1.0 | 0.1 to 4 | Multiplies the crit damage attribute. |
| `death.drop_coins` | true | | Drop all carried coins where you fell (PRD 3.12, decision 0013). |
| `death.keep_gear` | true | | Keep gear on death. |
| `seals.mode` | `WORLD` | `WORLD`, `PER_PLAYER` | How Seal progress is shared. `PER_PLAYER` is reserved and not implemented (decision 0016 fixes world-wide). |

Out-of-range values are clamped to the nearest bound rather than reset, so a typo softens the game instead of silently reverting a deliberate change; an unknown enum value is rejected and corrected to the default.

## Read by

`sb_combat` (crit chance and multiplier per swing), `sb_gear` (class damage, reforge stats), `sb_bosses` and `sb_world` (difficulty scalars), `sb_core` (Seal mode, death rules).
