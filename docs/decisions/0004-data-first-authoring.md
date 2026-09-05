# 0004 — Data-first, Java-thin authoring
Date: 2026-09-04
Status: accepted
Context: 26.x moved weapon stats, trades, world clocks, dialogs, enchantment effects and game tests into data packs; Java-first iterates slower and hides content from non-programmers.
Decision: every system gets a JSON schema (our own datapack registries where vanilla has none: weapon archetypes, damage classes, reforge modifiers and pools, accessory combine recipes, armor sets, rarity, seals, tiers); datagen emits all generated JSON; Java only for entities, menus with slots, projectiles, networking and novel mechanics. Tunable numbers never live in Java literals.
Consequences: `/reload` covers most iteration; datapacks can override our content; docs/data/ documents every registry schema.
