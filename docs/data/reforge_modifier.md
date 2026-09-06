# `sb:reforge_modifier` and `sb:reforge_pool`

Two datapack registries declared by `sb_core`, read by the reforge service (`sb_gear`) and the reforge NPC (`sb_hub`). Files live at `data/<namespace>/sb/reforge_modifier/<name>.json` and `data/<namespace>/sb/reforge_pool/<name>.json`. Loaded with the world.

A *modifier* is the prefix an item can carry: a name, a rarity and the attribute changes it brings. A *pool* says which items can roll which modifiers, and how likely each is. An item carries at most one prefix, as the `sb_core:reforge_prefix` component holding the modifier's id (decision 0018, `reforge_prefix.md`).

## `sb:reforge_modifier`

```json
{
  "rarity": "sb_core:uncommon",
  "prefix_key": "reforge.sb_core.lucky",
  "stats": [
    { "attribute": "sb_core:crit_chance", "amount": 0.04, "operation": "add_value" }
  ]
}
```

| Field | Type | Default | Meaning |
|---|---|---|---|
| `rarity` | rarity id | required | The rarity shown for the prefix and used to weight it in rolls. |
| `prefix_key` | string | `reforge.<namespace>.<path>` | Lang key of the prefix name ("Lucky"). The default is the convention the item presentation uses, so a modifier usually omits it. |
| `stats` | list | `[]` | The attribute modifiers applied while the prefix is on the item. |
| `stats[].attribute` | attribute id | required | Vanilla (`minecraft:attack_damage`) or ours (`sb_core:crit_chance`). Named by key, so an entry may name an attribute another module registers. |
| `stats[].amount` | double | required | The amount. |
| `stats[].operation` | `add_value`, `add_multiplied_base`, `add_multiplied_total` | required | Vanilla's operation names, so the JSON reads like an `attribute_modifiers` component entry. |

Applying a modifier writes the component and rewrites the item's `attribute_modifiers` under a namespaced id (`sb_core:reforge/<name>`), so the next roll replaces it cleanly.

## `sb:reforge_pool`

```json
{
  "applies_to": "#sb_core:melee_weapons",
  "entries": [
    { "modifier": "sb_core:lucky", "weight": 10 }
  ]
}
```

| Field | Type | Default | Meaning |
|---|---|---|---|
| `applies_to` | item tag or list of item ids | required | The items this pool serves. A tag (`"#sb_core:melee_weapons"`) is the normal form. **The tag must exist**: an unknown tag fails the whole datapack load, so `sb_core` declares its tags empty and content modules append to them from their own jars. |
| `entries` | list, at least one | required | The modifiers this pool can roll. |
| `entries[].modifier` | modifier id | required | Named by key, so a pool may list modifiers from another module's data. |
| `entries[].weight` | int ≥ 1 | required | Relative chance. An item rolls from every pool that contains it; weights are summed across those pools. |

Restrictions from the PRD ("a spear cannot roll *Broad*") are expressed by which pools contain which items, not by a rule inside a modifier.

## Entries

`sb_core` ships `lucky` (crit chance +0.04, uncommon; the only reforge source of crit per decision 0016) and the `melee_weapons` pool over the `#sb_core:melee_weapons` tag. The 12 Tier 1 modifiers arrive with the reforging ticket.

## Read by

`sb_gear` (the reforge transaction and loot pre-rolls), `sb_hub` (the reforge menu), `sb_core` (presentation).
