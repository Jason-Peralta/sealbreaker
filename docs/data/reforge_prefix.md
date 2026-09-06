# `sb_core:reforge_prefix` (item data component)

The reforge prefix an item carries (PRD 3.5): at most one, written by the reforge transaction, by `/sb reforge`, and by loot tables that pre-roll a prefix. Read by presentation (name, tooltip line, glint, item model conditions) and, once `sb:reforge_modifier` exists, by the lookup that applies the modifier's attribute modifiers. Decision 0018.

## Shape

The value is an identifier: the modifier's id.

```json
"sb_core:reforge_prefix": "sb_gear:lucky"
```

In a loot table:

```json
{ "function": "minecraft:set_components", "components": { "sb_core:reforge_prefix": "sb_gear:lucky" } }
```

## Presentation (all derived from the component)

| Effect | Rule | Lang keys |
|---|---|---|
| Name | `GearItem.getName`: `reforge.sb_core.named` with the prefix name first, then the item's own name; rarity colour and the anvil italic still apply | `reforge.sb_core.named` = `"%s %s"`, `reforge.<ns>.<path>` = the prefix name (e.g. `reforge.sb_gear.lucky` = "Lucky") |
| Tooltip | one line right under the item's own text, before vanilla's component lines | `reforge.sb_core.tooltip` = `"Reforge: %s"` |
| Glint | `GearItem.isFoil` is true while the component is present; `minecraft:enchantment_glint_override` still wins when set | — |
| Model | item model definitions may branch on it: `"property": "minecraft:has_component", "component": "sb_core:reforge_prefix"` | — |

Only `GearItem`s (`dev.sealbreaker.core.api.item.GearItem`) show the name and glint; the component itself can sit on any item.

## Read by

`sb_core` (presentation), `sb_gear` (reforging, #23), `sb_combat` (modifiers that change swing shape, later).
