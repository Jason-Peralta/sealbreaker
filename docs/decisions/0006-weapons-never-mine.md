# 0006 — Weapons never mine, tools never use movesets
Date: 2026-09-04
Status: accepted
Context: attacking and block breaking share the left mouse button; a crosshair priority rule near walls was the alternative.
Decision: items with a weapon archetype never start block breaking; tools keep vanilla behaviour and never use movesets.
Consequences: no priority logic to tune; vanilla swords lose cobweb/bamboo mining; the client-side mining path is skipped for archetype items (a Mixin in sb_core if no event covers it).
