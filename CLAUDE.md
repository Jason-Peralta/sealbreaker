# CLAUDE.md — working conventions for this repository

This is *Sealbreaker* (working title): an original, Terraria-inspired high-fantasy Minecraft Java modpack in which every gameplay mod is written by this two-person team. Read this file first in every session, then `docs/README.md`.

## Fixed decisions (do not re-litigate; change them only through a new entry in `docs/decisions/`)

| Topic | Decision |
|---|---|
| Minecraft version | **26.2** (year-based versioning; unobfuscated jars; Java 25). Freeze during a milestone; bump only at milestone boundaries when NeoForge, GeckoLib, Curios and JEI have releases. |
| Loader | **NeoForge** (26.2.0.7x), single loader, no multi-loader abstraction. |
| Build | **ModDevGradle 2.0.x**, Gradle wrapper 9.2.1, Java 25 via the foojay toolchain resolver, Mojang names (no Parchment, no remapping). One Git repo, one Gradle multi-project build, one version for all mods. |
| IDE | IntelliJ IDEA 2026.x + Minecraft Development plugin; JetBrains Runtime 25 as the run JDK with `-XX:+AllowEnhancedClassRedefinition` for hot-swap. |
| Authoring | **Data-first, Java-thin.** Every tunable number lives in JSON (our own datapack registries where vanilla has none) and is emitted by datagen; Java is for entities, menus with slots, projectiles, networking and novel mechanics. |
| External mods | **Utilities only** (decision 0015): never a third-party gameplay or content mod; utility/library mods that save reinventing a wheel are fine with one decision-log line each. Current: JEI, Curios, GeckoLib. Dev-only tools (Spark) never ship. |
| Players | Tune for 1–4; v1.0 capacity ceiling 10 concurrent (decision 0016). |
| Licence | Our code MIT, our assets all-rights-reserved; private repo until decided otherwise. |
| Milestones | M0 environment + spikes → M1 vertical slice (Tier 1 loop, melee + bow) → **M2 MVP = Tier 1 complete with all five classes and one boss** → M3 Tiers 2–3 → M4 v1.0. See `docs/03-roadmap.md`. |

## Design pillars (from `docs/02-prd.md`)
1. Vanilla, expanded. 2. Seals, not grind. 3. Every fight has rules. 4. Your build is your class, and your kit is earned. 5. Every realm earns its visit.

## Repository map

```
sb_core/  sb_combat/  sb_gear/  sb_bosses/  sb_world/  sb_realms/  sb_hub/  sb_blocks/   one Gradle subproject per mod
pack/                                   packwiz modpack (what players install)
docs/01-research.md                     why 26.2 / NeoForge / the toolchain; reference mods and licences
docs/02-prd.md                          vision, pillars, tiers, every core system, architecture, decisions
docs/03-roadmap.md                      milestones, estimates, exit criteria, repo layout, branching
docs/04-reconciliation.md               how the parallel "Astra" research was merged; open conflicts C1–C8
docs/archive/                           frozen inputs from other sessions (never edit; never treat as current)
docs/decisions/                         decision log (one numbered file per decision, never edited after acceptance)
docs/design/                            per-system design passes (required before implementing a system)
docs/spikes/                            Milestone 0 spike write-ups
docs/data/                              JSON schema docs for our registries
```

Package root `dev.sealbreaker.<module>`; `api` packages are public across modules, `impl` packages are private. Mod ids and resource namespaces equal the subproject name (`sb_core`, …). Dependencies only point toward `sb_core`; nothing depends on `sb_hub`, `sb_bosses` or `sb_realms`. Mixins live only in `sb_core`, each with a comment stating why no event or API could do the job.

## Commands

```bash
./gradlew build                      # compile, test, package every module
./gradlew :sb_core:runClient         # client with all modules loaded
./gradlew :sb_core:runServer         # dedicated server (--nogui)
./gradlew :sb_core:runGameTestServer # headless game tests; exit code = failed tests
./gradlew :sb_core:runData           # datagen → src/generated/resources (commit the output)
```

## How to work in a session
- **One task per session**, taken from the current milestone's deliverable list in `docs/03-roadmap.md`. If the task drifts into a new system, stop and write a design note or a decision instead of code.
- **Design before code for systems.** Bosses, classes, dungeons, realms, magic, guns: a `docs/design/<name>.md` exists before implementation. Combat and reforging are specified in the PRD.
- **Placeholders stay placeholders.** Settings, boss names, unlocks, realm themes, NPC names, currency and material names are `[…]` in the PRD until a design session fills them. Do not invent lore or later-tier content in passing; `docs/02-prd.md` Appendix A is scratch only.
- **Numbers live in JSON, never in Java literals.** A hard-coded damage value is a bug.
- **Tests are part of done.** Gameplay features ship with a GameTest (`src/gametest`), pure logic with JUnit (`src/test`). CI runs both.
- **Verification is real.** "It compiles" is not done. State what was run (client, server, two clients, game tests) at the end of the session.
- **Docs move with the code**: registry schema docs, roadmap checkboxes, the decision log, in the same change.
- **Commits**: imperative, one topic, prefixed with the module (`combat: add spear thrust hit shape`). Branches `feat/<mod>-<thing>`, `fix/<thing>`, `spike/<name>`, `design/<system>`; squash-merge to `main`; `main` always builds.
- **Ask before**: adding a dependency, touching an `api` package or a save format, adding a Mixin, bumping the Minecraft or NeoForge version, deleting a world.

## Decision log format (`docs/decisions/NNNN-<slug>.md`)

```
# NNNN — <title>
Date: YYYY-MM-DD
Status: accepted | superseded by NNNN
Context: <the situation and the alternatives, two or three sentences>
Decision: <what we chose>
Consequences: <what this makes easier, harder, or forbidden>
```

## Parallel research
`docs/archive/astra-2026-09-05/` holds a parallel research and PRD pass. Its answers that contradicted this project (per-player Seals, gear-use gates, vanilla material gates, 20 players, two mods, 26.1.2) were all settled in this project's favour on 5 Sep 2026 (decision 0016). Treat the archive as provenance only; never as current design.

## Things a future session must not assume
- That Mojang mappings/Parchment/Yarn matter: 26.x is unobfuscated; the vanilla source in the IDE is the reference.
- That the vanilla `Portal` interface, GeckoLib on 26.2 or PlayerAnimator on 26.x behave as the research doc expects: each is a Milestone 0 spike with a write-up in `docs/spikes/`.
- That third-party content mods are available: they are reference material only (licences in `docs/01-research.md` section 5).
