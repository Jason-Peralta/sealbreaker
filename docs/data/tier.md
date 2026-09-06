# `sb:tier`

Datapack registry declared by `sb_core`; read by boss and enemy tuning, loot tables and the reforge NPC. Files live at `data/<namespace>/sb/tier/<name>.json`. Loaded with the world.

A tier is the band of content between two Seals. Its *budgets* are the health and defence a player is expected to have there; every boss and enemy of the tier is tuned against them (PRD section 2 and 3.2), so a tuning pass edits one entry rather than every mob.

## Shape

```json
{
  "order": 1,
  "health_budget": 30,
  "defense_budget": 8,
  "reforge_base_cost": 10,
  "requires_seal": "sb_core:seal_i"
}
```

| Field | Type | Default | Meaning |
|---|---|---|---|
| `order` | int | required | 1 for the starting tier. |
| `health_budget` | int | required | Expected player health in half-hearts (30 = 15 hearts: 10 base plus early heart shards). |
| `defense_budget` | int | required | Expected armor points. |
| `reforge_base_cost` | int | required | Coins for one reforge of this tier's gear, before the rarity multiplier (PRD 3.5). |
| `requires_seal` | seal id | none | The Seal that must be broken to enter. Absent for the starting tier. |

## Entries

`sb_core` ships `tier_1` only: nothing beyond Tier 1 is designed until the MVP is fun (decision 0009). Its numbers are first-pass slice values.

## Read by

`sb_bosses` and `sb_world` (tuning), `sb_gear` (loot and reforge cost), `sb_hub` (the reforge NPC).
