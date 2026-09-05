# 0014 — The Nether is a tier setting behind Seal II; vanilla bosses may gate side content
Date: 2026-09-04
Status: accepted
Context: vanilla dimensions and bosses should stay relevant (pillar 1) without replacing tier bosses.
Decision: Nether portal ignition is gated behind Seal II so the Nether can be the Tier 3 setting. Working assumption, not designed until after the MVP: the Wither gates a Tier 3–4 side material and a realm shortcut; the Ender Dragon gates a Tier 5 side Seal (an End dungeon and an accessory) that feeds the final tier. Neither replaces a tier boss.
Consequences: a portal-ignition hook in sb_world reads the Seals service; side-Seals reuse the Seal registry.
