# `sb:rarity`

Datapack registry declared by `sb_core`; read by `sb_gear` (item tooltips and colours), the reforge NPC (cost) and loot tables (drop weight). Files live at `data/<namespace>/sb/rarity/<name>.json`. Loaded with the world. Rarity affects reforge cost and drop weight, never raw power (PRD 3.12): a Rare weapon is a good weapon of its tier, a Mythic one has a unique move.

## Shape

```json
{
  "color": "#55ff55",
  "order": 1,
  "reforge_cost_multiplier": 1.25,
  "tooltip_key": "rarity.sb_core.uncommon",
  "icon": "sb_core:rare"
}
```

| Field | Type | Default | Meaning |
|---|---|---|---|
| `color` | `"#RRGGBB"` | required | Name colour. Written and read as a hex string; a plain RGB integer without alpha in Java. |
| `order` | int | required | Position in the ladder, lowest first, unique across entries. Comparisons ("at least Rare") use this, never the id. |
| `reforge_cost_multiplier` | float | required | Multiplies the tier's `reforge_base_cost` when reforging an item of this rarity. |
| `tooltip_key` | string | `rarity.<namespace>.<path>` | Lang key of the rarity line in the tooltip. |
| `icon` | sprite id | none | Sprite shown beside the name, so colour is never the only signal (accessibility). Absent until the art exists. |

## Entries

`sb_core` ships the six of PRD 3.12, orders 0 to 5: `common` (white, ×1.0), `uncommon` (green, ×1.25), `rare` (cyan, ×1.5), `epic` (magenta, ×2.0), `legendary` (orange, ×3.0), `mythic` (red, ×4.0). The colours and multipliers are first-pass slice values; tune them in datagen (`SbCoreEntries.rarities`), never in Java elsewhere.

## Read by

`sb_gear` (tooltips, loot), `sb_hub` (reforge cost), `sb_core` (`sb:reforge_modifier` entries name a rarity).
