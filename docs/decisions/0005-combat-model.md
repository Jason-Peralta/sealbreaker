# 0005 — Combat: vanilla expanded, four-input movesets, crit chance, no stamina
Date: 2026-09-04
Status: accepted
Context: the user wants swings and hitboxes instead of click-to-hit, believable weapon movesets, and "not a souls-like, it's Minecraft". Timing-window damage bonuses were proposed and rejected in favour of a simple crit.
Decision: every melee archetype has at most four moves on existing inputs: tap combo, hold charge, attack while airborne, attack while sprinting. No stamina, no lock-on, no fifth move. Damage lands if the attack connects, with a per-class crit chance: base 4% for every class, 2x damage, extra crit only from crit gear, the Lucky prefix and accessories. Enemies obey the same swing rules with telegraphed, hold-still wind-ups.
Consequences: weapon archetypes are JSON with a moves map; server-authoritative SwingState; one polished first-person animation set (sword) in the slice, reused elsewhere until MVP; hit-stop and trails are client-only cosmetics.
