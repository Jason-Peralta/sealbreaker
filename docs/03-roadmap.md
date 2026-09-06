# 03 — Roadmap and working agreements

| | |
|---|---|
| **Status** | Phase 3, v0.1 (4 September 2026) |
| **Inputs** | `01-research.md` (26.2 on NeoForge, ModDevGradle, data-first), `02-prd.md` v0.4 (MVP = Tier 1 complete, one boss, all five classes) |
| **Team** | 2 developers, strong Java (one with some Minecraft modding experience, one with none), hobby cadence, no deadline. Capacity recorded on 5 Sep 2026: **about six hours each per week, twelve combined**, roughly 600 developer-hours a year after interruptions. Estimates below are calendar ranges at that cadence; halve them for a week of full-time push. |
| **Rule of the road** | A milestone is done when its exit criterion is met *in play on the real server with both of you*, not when its tickets are closed. |

## 1. Milestone overview

| # | Name | What exists at the end | Calendar estimate | Exit criterion |
|---|---|---|---|---|
| M0 | Environment and spikes | Repo, build, CI, hello-world mod loading on client and server, five technical spikes answered | 3–4 weeks | Both machines build and run the pack from a clean clone; every spike has a written result |
| M1 | Vertical slice (proof of concept) | Tier 1 loop with melee + bow: one boss, one Seal, the reforge NPC, one small structure, the first dash, a handful of blocks and weapons | 4–6 months | A blind tester (a friend who has not seen the pack) beats Boss I and says the sword feels better than vanilla's; acceptance criteria AC-01 to AC-08 pass |
| M2 | Tier 1 MVP | The same boss and tier with all five classes, boss loot bags, heart shards, combining, five NPCs, potions, scalable difficulty; the magic + healer design pass done first | 6–8 months | Four players each clear Tier 1 as a different class in one session and want to keep playing; AC-09 to AC-11 pass |
| M3 | Tiers 2–3: the Unsealing | Two more bosses, the first two major dungeons, the Nether as a tier setting, the Unsealing, Realm A opened | 9–12 months | A four-player group reaches Realm A from a fresh world in 15–22 hours; AC-12 to AC-15 pass |
| M4 | v1.0 | Tiers 4–6, Realm B, guns, the super accessory, trial mode, save-compat versioning, the 10-player capacity run | 12–18 months | A fresh world can be played to the last Seal; a world from M3 loads without loss; AC-16 passes (10-player ceiling) |
| — | Later | Parry, events, fishing, mounts, housing depth, extra classes | — | — |

Total to v1.0 at twelve combined hours a week: roughly **three to three and a half years** (about 1,600–2,300 developer-hours). That is the honest number for the PRD's scope with two people at this cadence; the milestones are ordered so that the pack is *playable and fun* from M1 onward, and stopping after M2 or M3 still leaves a complete game. Acceptance criteria are in PRD section 6.1.

## 2. Milestones in detail

### M0 — Environment and spikes (2–3 weeks)

**Goals.** Nothing designed in the PRD depends on a guess about the toolchain. Every unverified item from the research doc is closed by an experiment, and both developers can build, run, test and hot-swap.

**Deliverables.**
1. Repo initialised with the layout in section 3, `CLAUDE.md`, docs index, decision log, `.gitignore`, `.editorconfig`.
2. Gradle multi-project on ModDevGradle 2.0.x, NeoForge 26.2.0.7x, Java 25 via the foojay toolchain resolver; `sb_core` as a hello-world mod that logs on load; client, server and `gameTestServer` run configurations.
3. GitHub Actions: build + game tests on every push (`cache-provider: basic`).
4. One data-driven `test_instance` passing headlessly in CI.
5. JetBrains Runtime 25 configured as the run JDK with enhanced class redefinition; a method added while the game runs shows up without a restart.
6. packwiz pack skeleton in `pack/`, served locally with `packwiz serve`, installed by Prism Launcher on one client and by the start script on a throwaway server.
7. **Spikes** (each is a branch, a short write-up in `docs/spikes/`, and a decision-log entry):
   - **S1 Combat feel:** one keyframed sword swing on the vanilla player model in first and third person, driven by a server-side swing state, plus an arc hit test that damages every mob in the arc during active frames. Decides whether we write our own player animation layer or wait for PlayerAnimator on 26.x.
   - **S2 Boss animation:** a Blockbench model animated through GeckoLib 5.5.x, a server-triggered animation playing on two connected clients.
   - **S3 Portal and dimension:** a JSON dimension with its own world clock, entered through a block using the vanilla `Portal` interface.
   - **S4 Item presentation:** item model definitions plus a custom data component rendering a reforge prefix in the name, a tooltip line and a glint.
   - **S5 Structure:** a jigsaw structure with a processor list and a locked-door block spawning at a configured rarity.

**Effort split.** The developer with modding experience takes S1 and S2 first (the two riskiest); the developer new to modding takes build, CI, pack and JBR (items 1–6), which is also the fastest way to learn the toolchain; then both on S3–S5. Every spike is pair-reviewed so the modding knowledge does not sit with one person.

**M0 status (4 Sep 2026, first machine):**
- [x] 1. Repo initialised: layout, `CLAUDE.md`, docs index, decision log 0001–0020, `.gitignore`, `.editorconfig`, `.gitattributes`, `LICENSE`; the private GitHub repo `Jason-Peralta/sealbreaker` holds it with 51 tickets in two tracks (5 Sep 2026).
- [x] 2. Gradle multi-project on ModDevGradle 2.0.146, NeoForge 26.2.0.76, Java 25 (Adoptium 25.0.4.1 fetched by the toolchain resolver). `./gradlew build` succeeds and produces `sb_core-0.0.1.jar`; `runClient` loads the mod (constructed, common setup, client setup logged) and reaches the title screen. Verified on one machine; the second machine and `runServer` (needs the EULA accepted by a person) are still to do. Fresh-clone check on the first machine (5 Sep 2026): clone, `./gradlew build` (17 s with a warm Gradle cache) and `./gradlew gameTests` (five module runs, 18 test executions, all green) with no untracked files needed; the second machine is still to do (#1), and `runServer` still needs a person for the EULA.
- [x] 3. CI: `.github/workflows/build.yml` (build + `gameTests` on ubuntu-24.04, jars uploaded) merged 6 Sep 2026 after the `workflow` token scope was granted; the first run failed on a non-executable `gradlew` (fixed with `git update-index --chmod=+x`), the second passed. `main` is protected: pull requests, code-owner review on the CODEOWNERS paths, the `build` check required and up to date, no force pushes; squash merges only. The repository is public (decision 0021) (#2).
- [x] 4. First data-driven test instance: `data/sb_core/test_instance/hello_world.json` → `sb_core:hello_world` test function; `./gradlew :sb_core:runGameTestServer` reports "All 2 required tests passed", exit code 0 (5 Sep 2026). The test function API was read from the unobfuscated 26.2 source (`TestData`, `FunctionGameTestInstance`, `BuiltInRegistries.TEST_FUNCTION`). JUnit is wired through ModDevGradle's `unitTest` support (`src/test`, one smoke test passing). Test functions and test instances now live in each module's `src/gametest` source set as a test-only mod (`sb_core_tests`, `sb_combat_tests`) that only `runGameTestServer` loads, so shipped jars carry no tests; every test instance stands on the shared `sb_core_tests:arena` template (16x9x16 stone floor, generated by `tools/gen_structures.py arena`), and `sb_combat_tests:arc_targeting` asserts the real sweep and thrust hit sets on pigs placed around the attacker (ticket #9, 5 Sep 2026).
- [x] 5. JBR hot-swap confirmed end to end 6 Sep 2026: the generated `sb_combat - Client` configuration on JBR 25.0.2 under the debugger, a new private method and log line added to `SwingService.hit`, Ctrl+Shift+F9, Reload, and the line printed on the next swing (`HOT-SWAP CHECK: Dev hit Pig ... with SWEEP for 4.2 damage`). The resource half (`-Psb.animdebug=reload`) was verified the day before. README IDE section has the settings (#4).
- [x] 6. packwiz pack (6 Sep 2026): NeoForge 26.2.0.76; GeckoLib 5.5.5, Curios 16.0.0, JEI 30.31 from Modrinth; our five jars from the `v0.0.1` pre-release. Verified end to end on the first machine: `packwiz serve`, then `pack/server/start.ps1` (throwaway server, EULA accepted by hand, all eight mods loaded), then a Prism Launcher instance whose packwiz pre-launch sync installed the same eight mods, then a join to `localhost`. `pack/README.md` has every step and the Prism quoting gotcha (#3).
- [x] 7. Spikes: S1 combat feel answered (A yes; B yes via the one core Mixin, decision 0017; C yes: the first-person view draws the real arm from the same keyframes) and its code is the seed of `sb_combat`: three-move data-driven combo with a blade-following hitbox, a keyframe animation pipeline (Blockbench-shaped JSON, hip-pivoted torso with the arms riding on it, a wrist bone for the blade, blending between combo moves) and an offline contact-sheet debugger; the sword's swipe, swipe, thrust are authored as one chained sequence with the edge leading and the handle in the fist, moves play out in full, swings follow the look, the cuts are diagonal (raised over a shoulder, whipped over the top and across the front, finished low), first person uses vanilla's framing driven by the rig, with hitstop, camera kick and sway, blade arcs that follow the drawn weapon, and strike-timed sound; reviewed from five camera angles and by playing the real combo in the debug world; the user judged the arc, sound and impact frames right and the cuts are the current iteration. Next: jump, dash and charge attacks. S4 item presentation answered yes on all three (name, tooltip line, glint) from the one `sb_core:reforge_prefix` component through a `GearItem` base class and NeoForge's tooltip appenders, no Mixins (decision 0018, `docs/spikes/s4-item-presentation.md`, 5 Sep 2026); `sb_gear` exists with `/sb reforge`, a capture run and a game test, and the module build layout moved into the `sealbreaker.mod` convention plugin under `buildSrc/`. S5 structure answered yes on all four items: three code-generated jigsaw pieces assemble from data, the processor list cracks a fifth of the bricks, the `sb_world:locked_door` block keeps its key through template and jigsaw placement (game-tested on a new 48-wide arena) and five seeds spawned it on land (`docs/spikes/s5-structure.md`, 5 Sep 2026); `sb_world` exists with the door, its key, a world-creating capture run and two game tests. S3 portal and dimension answered yes on all three parts: the realm is five data files on its own 26.2 world clock (a 12000-tick day, its own rate), the `sb_world:spike_portal` block implements the vanilla `Portal` interface and carried the host to the realm and back while a second client on a LAN-published integrated server watched (decision 0019, `docs/spikes/s3-portal-dimension.md`, 5 Sep 2026); the two-client harness (`clientPortal` + `clientPortalGuest`) is the multiplayer verification path until a person accepts a dedicated server's EULA. S2 boss animation answered yes: GeckoLib 5.5.5 for NeoForge 26.2 from the Modrinth Maven (decision 0020), a two-cube `sb_bosses:spike_dummy` whose server-set synced attack flag played the attack on both clients at the same world tick, GeckoLib's render-state hook recorded for the boss ticket (`docs/spikes/s2-boss-animation.md`, 5 Sep 2026); the two-client harness moved into core (`DevHarness`). All five spikes answered.

**Exit criterion.** A clean clone builds on both machines in one command; both can join the throwaway server through the packwiz-installed client; all five spike write-ups exist with a yes/no answer.

### M1 — Vertical slice (3–4 months)

Status: #10 (the six `sb:*` datapack registries in `sb_core`, with datagen entries, docs and tests) landed 6 Sep 2026; the rest of M1 is open.

**Goals.** Prove the core loop is fun with the smallest possible content: swing, dash, beat a boss, break a Seal, reforge, go again.

**Deliverables** (PRD "Slice" column throughout):
- `sb_core`: Seals service + `SealBrokenEvent`, `CharacterProgress` attachment, JSON registries (`damage_class`, `weapon_archetype`, `reforge_modifier`, `reforge_pool`, `rarity`, `seal`, `tier`), custom attributes, config, datagen base.
- `sb_combat`: swing state machine with the four input contexts; the **sword** with its full four-move set; spear, greatsword and bow with partial sets reusing sword animations; crit at 4% base; enemy swing goals with telegraphs and hold-still wind-ups; dash capability; hit-stop, trails, sounds; the Dash/Dodge keybind.
- `sb_gear`: 8 weapons (Tier 1 melee spread + bow + 2 uniques), 1 armor set, 5 accessories (sure-footing, dash, 3 stat), 12 reforge modifiers, 1 combine recipe, rarity tooltips, coins.
- `sb_bosses`: boss framework (phase machine with 2 phases, arena, participants, loot bags) and `[Boss I]` with a GeckoLib model; summon item and summon altar.
- `sb_world`: the Tier 1 small structure (where the reforge NPC is held) with a prototype locked door; summon altar placement; Tier 1 ore.
- `sb_hub`: hub structure, NPC entity with the guide and reforge roles, data-driven trades, reforge menu, rescue cell.
- `sb_blocks`: one decorative family via the `BlockFamily` generator.
- 3 enemies with distinct roles; Tier 1 settings, boss and unlocks *filled in* from the placeholders before art starts (a one-evening design session).
- Tests: JUnit for rolling and Seal rules; GameTests for summon/despawn/bag, reforge application, moveset input contexts, the locked door.

**Effort split.** Developer A: combat core and player animation (the long pole), then boss framework. Developer B: core registries, gear, hub and NPC menu, structure, then the boss model and animations in Blockbench. Art budget: ~25% of both developers' time.

**Exit criterion.** Blind playtest passes: a friend reaches and beats Boss I with sword and dash within two hours of a fresh world, reforges something on purpose, and prefers our sword to vanilla's when asked. Server holds two clients through the whole session with no desync visible in the boss fight. Acceptance criteria AC-01 to AC-08 (PRD 6.1) pass on the dedicated server, including the two-client death, disconnect, restart and duplicate-reforge cases.

**Cut list if late** (in order): second unique weapon, the block family (use vanilla blocks), the greatsword partial set, trails and camera nudge.

**Review at the end of M1:** module boundaries are "as many as needed" (decision 0016). Look at which of the eight planned modules actually have an owner and a clear boundary; merge any that do not, split any that grew a second purpose. Not a gate, an ordinary review item.

### M2 — Tier 1 MVP (4–6 months)

**Goals.** The same tier, made whole: every class has a reason to exist and a way to be built, and the systems that make the pack *Terraria-like* (bags, hearts, combining, potions) are all present at Tier 1 depth.

**Deliverables** (PRD "MVP" column throughout):
1. **Design pass first (2–3 weeks, both developers):** `docs/design/magic.md` and `docs/design/healer.md` with the constraints from PRD 3.15, a spell list of 6–8 across two schools, four healer abilities, mana and healing-power numbers against the Tier 1 health budget. No magic code before this exists.
2. Classes: summoner (one minion, minion slots, a minion rod archetype), magic v1 (staff archetype, mana pool, HUD, 6–8 spells, mana crystals, the magic NPC), healer v1 (healer focus archetype, heal arc, charged group heal, four abilities, shared heal cooldown); class damage attributes and set bonuses (one armor set per class).
3. Character growth: boss loot bags with the Boss I slot upgrade, heart shards with the Tier 1 cap, the compact health display, tier budget tuning for Boss I against all five classes.
4. Combining: the Combine tab of the reforge menu, `accessory_combine` registry, 4 recipes (safe-dash charm, class emblem, two utility).
5. Content: ~25 weapons, 12 accessories, 20 modifiers (incl. *Lucky*), 6 enemies incl. 2 elite affixes, 6 potions and the potions NPC, the merchant with buy-back and the pouch, ~50 blocks, the Tier 1 structure grown into a small dungeon with a real key and heart shards.
6. Full four-move sets for spear and greatsword; first-person sword set reused elsewhere.
7. Scalable difficulty: config scalars live-reloadable; death coin drop rule.
8. Save policy from here on: no more world resets without a migration.

**Effort split.** Developer A: magic and healer implementation after the joint design pass, then combining. Developer B: summoner, character growth, potions, merchant, dungeon growth, then balance. Art: ~30% of time (five armor sets, three new enemies, spell effects).

**Exit criterion.** A four-player session on the real server where each player clears Tier 1 as a different class, no class feels useless or dominant by the group's own vote, and the group asks what Tier 2 is.

**Cut list if late:** healer's fourth ability, two of the four combine recipes, the second elite affix, the compact health display (rows of hearts are acceptable for a while).

### M3 — Tiers 2–3: the Unsealing (6–9 months)

**Goals.** Turn one tier into a progression. Only now are Tier 2–3 settings, bosses, unlocks and kit designed (a two-evening design session at the start, using Appendix A of the PRD as scratch material).

**Deliverables** (PRD "Tiers 2–3" column): Bosses II and III with bags; the first two major jigsaw dungeons with keys, sealed boss rooms and traps; the Nether as the Tier 3 setting with ignition gated behind Seal II; the Unsealing (enemy scaling, elites, vein blooms in new chunks, throttled tremors); Realm A defined in JSON and opened (terrain, resources, enemies; no boss yet); dodge roll (Tier 2) and double jump (Tier 3) with their combine recipes; full four-move sets for hammer, axe and dagger; ~50 weapons, 9 armor sets, 24 accessories, 12 enemies, 10 potions, ~120 blocks; the Wither's side gate; save-data versioning with a fixture-world test.

**Effort split.** Split `sb_realms` out; one developer owns Realm A and worldgen, the other owns dungeons and Bosses II–III; both share content weeks.

**Exit criterion.** A four-player group reaches Realm A from a fresh world in 15–22 hours; a world saved at the end of M2 loads and continues.

### M4 — v1.0 (9–12 months)

**Deliverables** (PRD "v1.0" column): Tiers 4–6, Realm B and its air kit (glide, wall-jump, air dash), Bosses IV–VI and the capstone perk, the guns design pass and 6–8 guns, the housing and guns NPCs, the super accessory and the full combine trees, ~100 weapons, trial mode, the Ender Dragon's side Seal, accessibility pass, `en_us` string audit, performance pass against the NFR budgets.

**Exit criterion.** A fresh world can be played to the last Seal by four players; every NFR in PRD section 6 is measured and met; a world from M3 loads without loss.

### Version-bump points

Per the research doc's policy: freeze during a milestone, bump at boundaries. Expected: 26.2 through M0 and M1 unless 26.3's NeoForge and GeckoLib builds are stable before M1's first week (then start M1 on 26.3); reassess at the start of M2, M3 and M4. Never bump mid-milestone.

## 3. Repository layout

One repository, one Gradle build, one version.

```
Minecraft Redux/                 (rename with the pack once the working title is chosen)
├── CLAUDE.md                    working conventions for Claude Code sessions
├── README.md                    what this is, how to build, where the docs are
├── settings.gradle              includes every sb_* subproject
├── build.gradle                 root: shared plugins/versions only
├── gradle.properties            versions in one place (minecraft, neoforge, mdg, java, mod_version)
├── gradle/                      wrapper
├── buildSrc/                    the `sealbreaker.mod` convention plugin: every module's shared layout, runs and tests
├── sb_core/                     ┐
├── sb_combat/                   │ one Gradle subproject per mod, identical layout:
├── sb_gear/                     │   build.gradle
├── sb_bosses/                   │   src/main/java/dev/sealbreaker/<mod>/{api,impl,...}
├── sb_world/                    │   src/main/resources/{META-INF/neoforge.mods.toml, assets/, data/}
├── sb_realms/                   │   src/generated/resources/   (datagen output, committed)
├── sb_hub/                      │   src/gametest/java/          (game tests)
├── sb_blocks/                   ┘   src/test/java/              (JUnit)
├── pack/                        packwiz modpack: pack.toml, index.toml, mods/, config/, server/
├── docs/
│   ├── README.md                index of every document
│   ├── 01-research.md
│   ├── 02-prd.md
│   ├── 03-roadmap.md
│   ├── decisions/               decision log, one file per decision (0001-…md)
│   ├── design/                  system design passes (magic.md, healer.md, guns.md, boss-I.md, …)
│   ├── spikes/                  M0 spike write-ups
│   └── data/                    JSON schema docs for our registries
└── .github/workflows/build.yml  CI
```

Mod ids are `sb_core`, `sb_combat`, … (the `sb_` prefix follows the working title; a rename is a single search-and-replace before M1 content starts). Java package root: `dev.sealbreaker`. Resource namespaces equal mod ids.

## 4. Branching and releases

- `main` is always buildable and always the version the server runs. Protected: CI must pass.
- Short-lived branches per task: `feat/<mod>-<thing>`, `fix/<thing>`, `spike/<name>`, `design/<system>`; squash-merge into `main` with a message that states *what* and *why*.
- Two developers, so pull requests are for review of anything touching `sb_core` or `sb_combat` `api` packages, Mixins, save formats or balance data; everything else can merge on green CI.
- Tags at milestone ends: `m0`, `m1`, `m2`, …; the packwiz pack points at the tagged jars on GitHub Releases. Between tags the server runs `main` builds published by CI.
- Version string: `0.<milestone>.<build>` until v1.0 (`0.1.x` during M1, `0.2.x` during M2, …), then semver.
- Commit messages: imperative, one topic each; prefix with the mod (`combat: add spear thrust hit shape`).

## 5. How we use Claude Code across sessions

- **Read first.** Every session starts by reading `CLAUDE.md`, then `docs/README.md`, then the doc for the milestone in progress. The decision log is the memory: if a choice is not in it, it is not decided.
- **One task per session, scoped by the milestone deliverable list.** A session that drifts into a new system stops and writes a decision or a design note instead.
- **Design before code for systems.** Bosses, classes, dungeons and realms get a `docs/design/<name>.md` before implementation; combat and reforging already have their spec in the PRD.
- **Docs are updated in the same change** as the code they describe: schema docs for a new registry, the roadmap's deliverable checkboxes, the decision log.
- **Numbers live in JSON, never in Java literals.** A session that finds itself hard-coding a damage value moves it to data.
- **Tests are part of done.** A gameplay feature ships with a GameTest; a pure-logic feature with a JUnit test.
- **Verification is real.** "It compiles" is not done; "it ran on the server with two clients" is. Sessions end with a short note of what was verified and how.
- **No new dependencies without a decision.** Libraries are limited to GeckoLib, Curios and JEI unless the decision log says otherwise.
- **Placeholders stay placeholders** until the design session fills them; sessions must not invent lore, names or later-tier content in passing.

## 6. Decision log format

One file per decision in `docs/decisions/`, numbered, never edited after acceptance (add a new decision that supersedes it instead):

```
# 0007 — Weapons never mine, tools never use movesets
Date: 2026-09-04
Status: accepted (supersedes: none)
Context: attacking and block-breaking share the left mouse button; a priority rule near walls was the alternative.
Decision: items with a weapon archetype never start block breaking; tools keep vanilla behaviour and never use movesets.
Consequences: no crosshair priority logic; vanilla swords lose cobweb/bamboo mining; a Mixin may be needed in sb_core.
```

The first entries (0001–0012) capture the decisions already made in the research doc and the PRD, so the log is complete from day one.
