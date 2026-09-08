# `sb:seal`

Datapack registry declared by `sb_core`; read by the Seal state service (world-level progression) and by every gate that asks "is this unlocked". Files live at `data/<namespace>/sb/seal/<name>.json`. Loaded with the world.

A Seal is world-wide (PRD 3.1, decision 0016): breaking it changes the world for everyone, and per-player rewards ride on boss loot bags instead. Systems ask the Seal state for an *unlock flag*, not for a Seal number, so content can move between tiers without touching code.

## Shape

```json
{
  "order": 2,
  "boss": "sb_bosses:boss_ii",
  "unlocks": ["sb_core:nether_ignition"]
}
```

| Field | Type | Default | Meaning |
|---|---|---|---|
| `order` | int | required | Position in the chain, 1 first. The Seal count that gates spawns, trades and recipes is the highest broken `order`. |
| `boss` | entity type id | required | The boss whose defeat breaks the Seal. Named by key, so the entry loads before `sb_bosses` registers the entity. |
| `unlocks` | list of ids | `[]` | Flags this Seal grants. A flag is an arbitrary id our systems agree on (`sb_core:nether_ignition` gates Nether portal ignition, PRD section 2). |

## Entries

`sb_core` ships the six Seals of the PRD (`seal_i` to `seal_vi`). Bosses are placeholders (`sb_bosses:boss_i`, ...) and only Seal II carries an unlock, until the tier design sessions fill them in (decision 0012: placeholders stay placeholders).

## Read by

`sb_core` (Seal state, `sb:tier` entries), `sb_world` (spawns, structures), `sb_hub` (NPC trades), `sb_realms` (realm entry).
