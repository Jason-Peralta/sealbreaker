# 04 — Reconciliation with the parallel "Astra" research (5 September 2026)

A second Phase 1 research document and a Phase 2 PRD draft were produced in parallel (author label "Astra", dated 4–5 Sep 2026) and handed over on 5 Sep 2026. This file records what was taken from them, what was adapted, and what conflicts with decisions made in this project's own review on 4 Sep 2026. The originals are archived unchanged in `archive/astra-2026-09-05/`. Our own documents remain the source of truth; where a conflict is listed, the decision stands until the user says otherwise.

## 1. Adopted as-is (now in our docs)

| Item | Where it landed |
|---|---|
| Gates are enforced server-side at the point of action (craft, use, equip, station, transition); recipe hiding, advancements and data-load conditions are presentation, not enforcement | PRD 3.1 |
| Entitlement (a claim per UUID and encounter id in SavedData) is separate from inventory delivery; delivery on next login; no duplicate or lost claims across death, reload or crash | PRD 3.1.1 |
| Reforging is a validated server transaction; reject stale/duplicate requests; test disconnect and restart windows; never trust client stat values | PRD 3.5 |
| First reforge is a guaranteed affordable improvement | PRD 3.5 |
| Opt-in roster at the altar; roster recorded at start; late joiners assist but qualify next attempt; death keeps eligibility; no damage quota; 60 s disconnect grace; offline persistence; scaling locked at start; wipe resets; restart cancels safely | PRD 3.7 (replaces our "dealt damage or 20 s in arena" rule) |
| Each boss teaches one decision; solo-viable; no unavoidable damage, long invulnerability or health-inflation difficulty | PRD 3.7 |
| Summon items craftable from pre-gate resources; creative-only recovery is never the only route | PRD 3.7 |
| Discovery guarantee: seed-suite validation, maps never point at missing structures, safe fallback; new structures do not appear in explored chunks | PRD 3.8 |
| Preserve building outside encounters; protect only arena mechanisms; never restore over player builds; altar recovery route | PRD 3.8 |
| Transition check on the travelling player incl. mounts/passengers; denied players stay safe; return always allowed; a lost flag never strands | PRD 3.9 |
| Service NPCs can never lock progress; respawn rule | PRD 3.10 |
| p95/p99 tick targets over a 30-minute session; separate exploration scenario; record hardware/seed/distances before sign-off; memory-growth checks; cleanup on reset and unload | PRD 6 |
| Dedicated server from the first feature (never only the integrated server) | PRD 6 |
| Data reload contract (validated snapshot, last-good on failure, rolled values never change on rebalance, id removal is a migration) | PRD 6 |
| Diagnostics with encounter ids; documented recovery routes; operator commands as explicit exceptions | PRD 6 |
| Server config owns shared rules; client config is presentation only | PRD 6 |
| Fresh-world policy; no support for importing arbitrary existing worlds | PRD 6 |
| Acceptance criteria as testable outcomes per release | PRD 6.1 (adapted to our systems) |
| Quilt's February 2026 announcement retiring QSL, QFAPI, QKL and Quilt Mappings for 26.1+ | Research doc, section 2 (source added) |
| Mojang's 8 Sep 2024 "future of development" post on more frequent drops | Research doc, section 1 (source added) |
| Copper equipment (1.21.9) exists as an ungated pre-iron option | PRD Appendix A note; Tier 0 design session |
| Team facts: one developer has no Minecraft modding experience; about six hours each per week; hosting provider undecided | Roadmap assumptions; CLAUDE.md |

## 2. Adapted (taken in a modified form)

| Item | Their form | Our form | Why |
|---|---|---|---|
| Keep-or-accept reforge flow | Pay to generate one alternative, keep or accept, offer persists | Recorded as an MVP design option next to the Terraria-style blind reroll; chosen at the Tier 1 design session | Both fit; the choice is feel, not architecture |
| 20-player capacity | Design and verify for 20 concurrent players | Tune for 1–4; test 20 as a capacity ceiling at v1.0 if confirmed | Balance for 20 is not something two people can playtest; capacity can be load-tested |
| Boss "encounter identity" table | Four specific bosses with lessons | The lesson-per-boss rule; their bosses go to Appendix A as candidates | Bosses are placeholders by decision 0012 |
| Reforge grades tied to seals | Tempered/Honed/Exalted unlocked by material seals | Our prefixes already scale cost and pools by tier; grade names go to Appendix A | Same idea, different vocabulary |
| Netherite and End edge cases | Netherite needs diamond + Nether seals; End unchanged | Netherite is Tier 3 alongside our Tier 3 material (already so); End entry and elytra parked with a recommendation (behind Seal IV) | Elytra would pre-empt the earned air kit; needs a decision after the MVP |
| Two-mod architecture | Core + content | Eight subprojects kept, with an end-of-M1 checkpoint to collapse to core + combat + content if boundaries cost more than they give | Their overhead argument is fair; the checkpoint makes it testable |
| "Do not estimate calendar delivery" | Effort ranges only after slice | Estimates kept, but restated at their 12 combined hours per week with an honest total | The user asked for estimates; assumptions are explicit |

## 3. Conflicts (their recorded answers versus this session's decisions) — settled 5 Sep 2026, decision 0016

| # | Topic | Our decision (4 Sep 2026) | Their recorded answer | Outcome |
|---|---|---|---|---|
| C1 | Progression scope | World-wide Seals (Terraria), per-player bags and upgrades | Per-player Seals, nothing shared | Ours stands |
| C2 | Gifted gear | Free to use by anyone | Use requires the recipient's own unlock | Ours stands |
| C3 | What bosses gate | Our own material tiers, realms, NPCs; vanilla iron/gold/diamond ungated | Bosses gate vanilla iron, gold, diamond, Nether | Ours stands |
| C4 | Player count | 4 | Up to 20 | Tune for 1–4; v1.0 capacity ceiling **10** |
| C5 | Module split | Eight subprojects | Two mods | As many modules as needed; eight is the starting plan |
| C6 | Minecraft version | 26.2 (built, loads) | 26.1.2 | 26.2 stays |
| C7 | Hardening after Seal III | Scaling, elites, blooms | No global transformations | Ours stands |
| C8 | Vertical-slice content | Melee + bow, one boss, reforge, one small structure, ~8 weapons, one block family | One boss, one unlock, one reforge, 4 weapons, 10 blocks | Ours stands as the budget |

Also settled the same day (decision 0015): external mods are utilities only. Their "no third-party gameplay pack" stance and ours now match exactly; JEI, Curios and GeckoLib remain as utilities.

## 4. Rejected (with reason)

| Item | Reason |
|---|---|
| Gating vanilla iron behind the first boss | Slows the familiar first hours (their own risk 8) and contradicts pillar 1 |
| Rarity of only Common/Uncommon/Rare | Our six rarities are already scoped and icon-labelled; fewer tiers do not reduce work meaningfully |
| No accessories, no mana system in v1.0 | Contradicts the decided five-class design and the earned kit |
| Advancement-only progression record | Both documents agree it is insufficient; ours already uses SavedData |
