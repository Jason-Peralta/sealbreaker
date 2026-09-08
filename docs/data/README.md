# Registry schema docs

One file per datapack registry we add. Each file: the JSON shape with every field, defaults, an example, and which module reads it. Written with the registry, in the same change. Item data components we add get a file too.

| Registry | Doc | Declared | Entries in |
|---|---|---|---|
| `sb:weapon_archetype` | [weapon_archetype.md](weapon_archetype.md) | `sb_core` | `sb_combat`, `sb_gear` |
| `sb:damage_class` | [damage_class.md](damage_class.md) | `sb_core` | `sb_core` (all five) |
| `sb:rarity` | [rarity.md](rarity.md) | `sb_core` | `sb_core` (all six) |
| `sb:seal` | [seal.md](seal.md) | `sb_core` | `sb_core` (all six) |
| `sb:tier` | [tier.md](tier.md) | `sb_core` | `sb_core` |
| `sb:reforge_modifier`, `sb:reforge_pool` | [reforge_modifier.md](reforge_modifier.md) | `sb_core` | `sb_core`, `sb_gear` |
| `sb_core:reforge_prefix` (component) | [reforge_prefix.md](reforge_prefix.md) | `sb_core` | written by the reforge service and loot |

Planned: `sb:accessory_combine`, `sb:armor_set`, `sb:realm`.

Numbers that are not content live in [attributes_and_config.md](attributes_and_config.md): our attributes (class damage, crit) and the one config file (difficulty scalars, death rules, Seal mode).

Core's own entries are written by datagen (`./gradlew :sb_core:runData`, `SbCoreEntries`) into `sb_core/src/generated/resources`, which is committed. Editing a number means editing datagen and rerunning it, never editing the generated JSON by hand.
