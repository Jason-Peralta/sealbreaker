# 01 — Research: the Minecraft modding landscape (September 2026)

| | |
|---|---|
| **Status** | Phase 1 complete, awaiting review |
| **Written** | 4 September 2026 |
| **Purpose** | Ground every technical decision for an original, Terraria-inspired high-fantasy modpack in which all gameplay mods are written by us. Feeds `02-prd.md` and `03-roadmap.md`. |
| **Team profile** | 2 people; strong Java; some Minecraft modding experience; small private server; library/API mods allowed within reason; hobby, no deadline. |
| **Method** | Web research on 4 Sep 2026 against primary sources (Mojang/minecraft.wiki, loader blogs and Maven repositories, GitHub, the Modrinth public API). Every table cites its source and date. Nothing here relies on memory for a version number; items that could not be verified are marked and collected in the Milestone 0 checklist. |

## Decision summary

| # | Decision | Recommendation | Confidence | Why not the alternatives (one line) |
|---|---|---|---|---|
| 1 | Target version | **The 26.x line. Pin 26.2 for Milestone 0; bump to 26.3 before the vertical slice; freeze until MVP.** | High (26.x over 1.21.1); medium (26.2 vs 26.3 timing) | 1.21.1 has the most mods but is obfuscated, Java 21 and two years old; chasing every quarterly drop costs 2–6 weeks each. |
| 2 | Mod loader | **NeoForge, single-loader.** | Medium-high | Fabric is fastest and a fine second choice but needs more Mixins and its accessory libraries are not on 26.x; Forge is small; Quilt's library is dead; multi-loader is pure overhead for a private pack. |
| 3 | Toolchain | **ModDevGradle 2.0.x, Java 25, Mojang names (no Parchment), IntelliJ 2026.x + Minecraft Development plugin, JBR 25 hot-swap, GameTest + JUnit, GitHub Actions, packwiz + Prism for distribution.** | High (medium on hot-swap) | NeoGradle is slower and no longer the default; Yarn/Parchment have no 26.x data; the Modrinth App cannot auto-update a private pack. |
| 4 | Content authoring | **Data-first, Java-thin: JSON schemas (our own registries where vanilla has none) with datagen; Java only for entities, menus with slots, projectiles, accessories and networking.** | High | Java-first iterates slower and hides content from non-programmers; data-only cannot do bosses or slot UIs. |
| 5 | Reference points | **Twilight Forest for world-enforced gating, Blue Skies for keys and NPC-tier progression, Apotheosis (MIT) for data-driven affix pools, Curios for slots, GeckoLib (MIT) for boss animation.** | High | Most other relevant mods are ARR, GPL or unmaintained on 26.x; we take mechanics, never code or assets. |
| 6 | Hard parts | **Bosses, the custom combat layer (swings, hitboxes, classes) and custom rendering are the real risks; guns and magic are full subsystems deferred past the vertical slice; dimensions, dungeons, reforging and gating are medium; blocks, loot and potions are volume, not risk.** | High | See the ranked table in section 6. |

## Scope reality check (to be expanded in the PRD)

A Terraria-scale roster (dozens of bosses, hundreds of weapons, several dimensions) is a multi-year commitment for two hobbyist developers, and the binding constraint is **art and animation throughput**, not code: every boss needs a model, textures, animations and sound; every block family needs 10–15 textures and models. The research supports building this incrementally: a vertical slice (one boss, one gate, one reforge, one small dungeon) first, then an MVP of roughly 3 bosses / 1 dimension / 2 dungeons / ~25 weapons, then a v1.0 in the range of 5–6 bosses / 2 dimensions / 4 dungeons. The PRD will make those cuts explicit.

Decision recorded 4 Sep 2026: combat is "vanilla expanded" (real swings with timing and weapon-shaped hitboxes, not click-to-hit) with Terraria-style classes that play differently (melee, ranged, mage, summoner, healer, and more). That puts a basic combat core *inside* the vertical slice, because it defines how the game feels. Guns and magic are both wanted, both built in-house with existing mods as reference only, and both deferred past the vertical slice; the PRD must reserve their design space (damage classes, a mana resource, ammo, projectile base classes) so the core never needs a rewrite.

---

## 1. Target version

### What changed at Mojang (and why it matters to us)

| Fact | Detail | Source |
|---|---|---|
| Quarterly "game drops" | Mojang announced the move away from one annual update on 8 Sep 2024 and now ships roughly one themed drop per business quarter. 2025 had four (1.21.5, 1.21.6, 1.21.9, 1.21.11); 2026 so far: 26.1 "Tiny Takeover" (24 Mar 2026), 26.2 "Chaos Cubed" (16 Jun 2026); 26.3 is in pre-release for September 2026. | [Mojang: the future of Minecraft's development (8 Sep 2024)](https://www.minecraft.net/en-us/article/the-future-of-minecrafts-development), [Game drop – Minecraft Wiki](https://minecraft.wiki/w/Game_drop), [Third Drop 2026](https://minecraft.wiki/w/Third_Drop_2026) (accessed 4 Sep 2026) |
| Year-based versioning | Versions are now `YY.D[.hotfix]`. 1.21.11 (Dec 2025) was the last `1.x` release; 26.1 replaced what would have been 1.22. Snapshots are named after the release they test (e.g. "26.3 Pre-Release 2"). | [Version formats – Minecraft Wiki](https://minecraft.wiki/w/Version_formats), [Mojang announcement, 2 Dec 2025](https://www.minecraft.net/en-us/article/minecraft-new-version-numbering-system) |
| Obfuscation removed | From 26.1 the shipped game uses Mojang's real class, method, field and parameter names. Announced 29 Oct 2025; experimental unobfuscated 1.21.11 snapshots, then 26.1 fully unobfuscated. Yarn/Intermediary are discontinued for new versions; Fabric moved to Mojang names. | [Obfuscation map – Minecraft Wiki](https://minecraft.wiki/w/Obfuscation_map), [Fabric: Removing Obfuscation (31 Oct 2025)](https://fabricmc.net/2025/10/31/obfuscation.html), [Mojang article](https://www.minecraft.net/en-us/article/removing-obfuscation-in-java-edition) |
| Java 25 | 26.1 and 26.2 require Java SE 25 (LTS); NeoForge's 26.1 post confirms the JDK 25 / Gradle 9.1+ move. | [Java Edition 26.1 – Minecraft Wiki](https://minecraft.wiki/w/Java_Edition_26.1), [NeoForge for 26.1 (24 Mar 2026)](https://neoforged.net/news/26.1release/) |
| New engine work | 26.1 rewrote the lightmap and chunk-geometry GPU memory model; 26.2 added an experimental Vulkan renderer with OpenGL fallback. Mods must render through the Blaze3D abstraction, not raw OpenGL. | [Java Edition 26.2 – Minecraft Wiki](https://minecraft.wiki/w/Java_Edition_26.2), [Fabric for 26.2 (15 Jun 2026)](https://fabricmc.net/2026/06/15/262.html) |
| New data-driven systems in 26.x | 26.1: villager trades (`villager_trade`, `trade_set` registries), world clocks / time markers (per-dimension time), save-folder reorganisation. 26.2: physics attributes (bounciness, friction, air drag), `interval_select` density function, restructured entity predicates. | [26.1 Snapshot 1 notes](https://feedback.minecraft.net/hc/en-us/articles/42011663817357-Minecraft-Java-Edition-26-1-Snapshot-1), [26.1 Snapshot 3 notes](https://feedback.minecraft.net/hc/en-us/articles/42636283994637-Minecraft-Java-Edition-26-1-Snapshot-3), [Java Edition 26.2 – Minecraft Wiki](https://minecraft.wiki/w/Java_Edition_26.2) |

**Implication.** The game is a moving target on a fixed three-month clock, but each move is now far cheaper to follow because the code is readable and there is no mapping churn. A multi-year project should therefore *pin* a version and bump deliberately at milestone boundaries, not chase every drop.

### Where the mods are (Modrinth, mod projects, queried 4 Sep 2026)

| Minecraft version | Fabric | NeoForge | Forge | Notes |
|---|---:|---:|---:|---|
| 1.20.1 | 17,317 | n/a | 24,551 | The last great Forge modpack version (mid-2023). |
| 1.21.1 | 18,843 | 20,198 | 5,170 | The 2024–25 "settled" modpack version; 4,822 modpacks target it. |
| 1.21.11 | 17,044 | 7,618 | – | Last obfuscated release (Dec 2025). |
| 26.1 | 9,932 | 5,692 | – | First unobfuscated release; 446 modpacks so far. |
| 26.2 | 10,863 | 5,598 | 2,908 | Current release (Jun 2026). |
| All versions | 45,311 | 28,220 | 34,210 | Quilt: 10,147 (mostly Fabric-compatible mods). |

Source: Modrinth public search API (`/v2/search` with loader + version facets), queried 4 Sep 2026. Counts are projects listing that version, so a mod counts once per version it supports.

Reading the table: **the version with the most existing mods is 1.20.1 (Forge) / 1.21.1 (NeoForge + Fabric)**. The 26.x line has not "settled" yet, but 26.1 and 26.2 are growing at similar rates, and the NeoForge team's own expectation is that 26.1 (or its successor) becomes the next long-lived modpack target, replacing 1.21.1 ([NeoForge for 26.1](https://neoforged.net/news/26.1release/), stated as the author's educated guess, not a policy).

### Why 1.21.1 has the most mods, and what that means for us

1.21.1 (8 Aug 2024) is a *settled* version in the way 1.12.2, 1.16.5, 1.18.2 and 1.20.1 were before it: a release that stayed still long enough for hundreds of mods to become simultaneously stable. Three things made it stick:

1. **Every later drop broke mods.** 1.21.2/3 (Oct 2024), 1.21.4 (item model definitions, Dec 2024), 1.21.5 (GameTest rework, Mar 2025), 1.21.6 (dialogs and GUI changes, Jun 2025), 1.21.9 (Sep 2025) and 1.21.11 (environment attributes, weapon components, Dec 2025) each changed Java APIs or JSON formats. NeoForge's own advice for porting 1.21.1 → 26.1 is to read seven intermediate release notes first. Modders port to "latest" when convenient but keep 1.21.1 as the version their mod is *known good* on, and packs follow the mods.
2. **NeoForge 21.1 was the first NeoForge line that big packs adopted** after the 2023 fork, and NeoForge still publishes 21.1.x builds (21.1.249 is among its most recent releases), so 1.21.1 is a de-facto long-term-support version even though nobody calls it one. NeoForge's 2025 retrospective notes 1.21 surpassed Forge 1.20.1's adoption rate.
3. **Accumulation takes time.** 1.21.1 has had 25 months to collect mods; 26.2 has had 11 weeks. Neither 26.1 nor 26.2 has "won" yet.

What that looks like for mods a small private server might want *alongside* ours (Modrinth API, 4 Sep 2026):

| Mod | Type | Newest version | On 26.2? |
|---|---|---|---|
| Sodium, Iris, Distant Horizons | Performance / shaders | 26.2 | Yes |
| Jade, JourneyMap, Xaero's Minimap, Waystones | QoL / maps | 26.2 | Yes |
| Better Combat | Combat overhaul | 26.2 | Yes |
| JEI | Recipe viewer | 26.2 (beta) | Yes |
| AppleSkin | QoL | 1.21.11 | No |
| Create | Large content | 1.21.1 | No |
| Farmer's Delight | Content | 1.21.1 | No |
| EMI, Accessories, PlayerAnimator | Viewer / libraries | 1.21.1 / 1.21.10 / 1.21.7 | No |

The pattern is the usual one: client-side and utility mods port within weeks; large content mods sit on the settled version for a year or more.

**So is 26.2 optimal?** For *what we build ourselves*, yes. Every vanilla system and loader API we need exists on 26.2, and several we want (data-driven weapon components, merchant trades, world clocks, dialogs, data-driven game tests) do not exist on 1.21.1 at all; the libraries we would actually depend on (GeckoLib, Curios, JEI) are on 26.2; and the code is unobfuscated. For *using existing content mods*, 1.21.1 wins today and will keep winning for at least the next year. Because this pack's rule is "every gameplay mod is ours" and existing mods are reference material we read on GitHub regardless of version, that catalog gap costs us little. The honest trade-off: if you want Create-class third-party content running alongside our mods during development, that is a 1.21.1 decision, and it has to be made now rather than later, because it also means Java 21, obfuscated code, Parchment, and a large port at the end.

### Loader and library readiness on 26.x (checked 4 Sep 2026)

| Project | 26.1 | 26.2 | Source |
|---|---|---|---|
| NeoForge | Stable builds (latest 26.1.2.103, published 4 Sep 2026) | Stable since 26.2.0.57; latest 26.2.0.76 | [NeoForge Maven metadata](https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml) |
| Fabric (Loader 0.19.5, Loom 1.17.x, Fabric API 0.158.0+26.2 today; 0.19.3 / 0.152.0 at release) | Yes | Yes (support post the day before release) | [Fabric for 26.2](https://fabricmc.net/2026/06/15/262.html), [Fabric meta](https://meta.fabricmc.net/v2/versions/loader) |
| Forge (LexManos) | Yes | Yes (65.1.3 latest, 26 Aug 2026; 65.1.0 recommended) | [files.minecraftforge.net](https://files.minecraftforge.net/net/minecraftforge/forge/) |
| Architectury API | 20.0.12 (26 Jul 2026) | 21.0.7 (2 Aug 2026), Fabric + NeoForge | [CurseForge files](https://www.curseforge.com/minecraft/mc-mods/architectury-api/files/all) |
| GeckoLib (animation) | 5.5.2 | 5.5.4 (30 Aug 2026), Fabric/Forge/NeoForge | Modrinth API `/v2/project/geckolib/version` |
| Curios (accessory slots) | 15.0.0 (NeoForge only) | 16.0.0 (21 Jul 2026, NeoForge only) | Modrinth API `/v2/project/curios/version` |
| Accessories (accessory slots) | **No** – newest is 1.21.10, last updated 6 Feb 2026 | **No** | Modrinth API `/v2/project/accessories` |
| JEI (recipe viewer) | Beta builds (3 Sep 2026), Fabric + NeoForge | Beta builds (3 Sep 2026) | Modrinth API `/v2/project/jei/version` |
| EMI (recipe viewer) | **No** – newest is 1.21.1 | **No** | Modrinth API `/v2/project/emi` |
| Sodium (performance) | 0.9.2 beta (2 Sep 2026) | 0.9.2 beta (2 Sep 2026), Fabric + NeoForge | Modrinth API `/v2/project/sodium/version` |

### Recommendation

**Target the 26.x line. Pin 26.2 for Milestone 0 today; expect the first bump to 26.3 before the vertical slice starts, then freeze until MVP.** Confidence: **high** that 26.x beats 1.21.1 for a from-scratch multi-year project; **medium** on the exact 26.2-vs-26.3 timing because 26.3 ships this month and NeoForge/GeckoLib need a few weeks to stabilise on it.

Why:
1. **Unobfuscated code is a step change for a two-person team.** Readable stack traces, greppable vanilla source, no mapping decisions, no remap step, and cheap ports between drops. Every earlier version makes you pay the mapping tax for the life of the project.
2. **Starting on 1.21.1 buys you a giant port later.** NeoForge's own migration advice for 1.21.1 → 26.1 is to read the release notes for 21.2 through 21.11 first: data components, item model definitions, equipment assets, the transfer rework, the GameTest rework, the GUI rework, and deobfuscation all land in that gap. A pack that reaches v1.0 in 2028 on 1.21.1 will be four years behind.
3. **The 26.x systems match our feature list.** Data-driven villager trades (merchant NPCs), world clocks (a dimension with its own day/night), dialogs (server-driven UI), item model definitions, data-driven enchantments and game tests all reduce Java.
4. **Everything we depend on is already there.** NeoForge, Fabric, Forge, Architectury, GeckoLib, Curios, JEI and Sodium have 26.2 builds. Accessories and EMI have not moved yet; we do not need them.

Why not the alternatives:
- **1.21.1**: the most mods and tutorials, but obfuscated, Java 21, two years old, and a painful port ahead. Only sensible if we wanted to lean on third-party content mods, which the project explicitly excludes.
- **1.20.1**: Forge-era peak; no reason for new code.
- **Always-latest (26.3 the day it ships)**: tempting because all mods are ours, but NeoForge betas and library lag cost 2–6 weeks per drop; we should bump only at milestone boundaries.

**Version-bump policy (proposed):** freeze the version for the duration of a milestone; at each milestone boundary, bump only if NeoForge has non-beta builds, GeckoLib/Curios/JEI have releases, and the port is estimated under two working days. Skip drops that add nothing we need.

**Note (5 Sep 2026).** A parallel research pass recommended 26.1.2 instead, on the grounds that 26.2 adds newer churn (notably the experimental Vulkan backend) without a demonstrated benefit for the first slice. We stayed on 26.2 because the readiness table above shows NeoForge, GeckoLib, Curios, JEI and Sodium all stable there, the rendering concern is covered by the "no custom rendering, Blaze3D only" rule, and 26.2's data additions (feature types, physics attributes) cost nothing. Both passes agree on the substance: the 26.x line, one loader, unobfuscated names, freeze through the slice. See `04-reconciliation.md`.

---

## 2. Mod loader

### Comparison (verified 4 Sep 2026)

| | NeoForge | Fabric | Forge (legacy) | Quilt |
|---|---|---|---|---|
| Status | Active; stable 26.1/26.2; snapshot builds published; team says it is "more short-staffed than ever" | Active; supports each drop before release day; small core team | Active again under LexManos; 26.2 builds (Aug 2026) | Loader active (repo pushed 4 Sep 2026); **QSL, QFAPI, QKL and Quilt Mappings formally retired for 26.1+ by the 3 Feb 2026 announcement** ([Quilt: non-obfuscated updates](https://quiltmc.org/en/blog/2026-02-03-non-obfuscated-updates/)); QSL's last release was Dec 2024 for 1.21 |
| Mods on 26.2 (Modrinth) | 5,598 | 10,863 | 2,908 | (not listed separately) |
| Mods on 1.21.1 | 20,198 | 18,843 | 5,170 | – |
| API surface | Large, batteries-included: event bus, `DeferredRegister`, data attachments, capabilities, networking payloads, menus/screens helpers, entity attribute + spawn events, fluid/transfer API, config system, **plus JSON-only surfaces Fabric lacks: biome modifiers, global loot modifiers, data maps** | Lean core; Fabric API provides events, registry helpers, networking, data attachments, transfer API, but deeper hooks need Mixins and biome/loot injection is Java | Similar shape to NeoForge (common ancestry), smaller community and slower feature work | Fabric-compatible loader; its own standard library is effectively unmaintained |
| Update speed | Beta on release day; stable within weeks (26.2 stable by 26.2.0.57) | Fastest; 26.2 support post 15 Jun 2026, release was 16 Jun | Weeks after | Follows Fabric, no QSL |
| Docs | docs.neoforged.net (versioned per MC version, 26.1 current) + Mod Generator site | docs.fabricmc.net (modern, well-maintained) + template generator | docs.minecraftforge.net | wiki.quiltmc.org |
| Gradle plugin | ModDevGradle 2.0.146 (also NeoGradle 7.1.38) | Loom 1.17.x (`net.fabricmc.fabric-loom`, no remapping) | ForgeGradle | Loom |
| License | LGPL-2.1 | Apache-2.0 (loader and API) | LGPL-2.1 with extra terms ("Other" on GitHub) | Apache-2.0 |
| Multi-loader | Architectury 21.0.7 for 26.2 (Fabric + NeoForge); MultiLoader-template pattern | same | Architectury still lists Forge | – |

Sources: [NeoForge Maven](https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml), [NeoForge for 26.1](https://neoforged.net/news/26.1release/), [NeoForge 2025 retrospection (1 Jan 2026)](https://neoforged.net/news/2025-retrospection/), [Fabric for 26.2](https://fabricmc.net/2026/06/15/262.html), [Forge downloads](https://files.minecraftforge.net/net/minecraftforge/forge/), [GitHub API: FabricMC/fabric](https://api.github.com/repos/FabricMC/fabric), [neoforged/NeoForge](https://api.github.com/repos/neoforged/NeoForge), [MinecraftForge](https://api.github.com/repos/MinecraftForge/MinecraftForge), [QuiltMC/quilt-loader](https://api.github.com/repos/QuiltMC/quilt-loader), [QSL on Modrinth](https://api.modrinth.com/v2/project/qsl), [Architectury files](https://www.curseforge.com/minecraft/mc-mods/architectury-api/files/all), Modrinth search API counts (4 Sep 2026).

### Recommendation

**NeoForge, single-loader, no multi-loader abstraction.** Confidence: **medium-high**.

Why:
1. **We are writing a content-heavy pack, and NeoForge's API is built for that.** Registries, data attachments for per-player and per-chunk state (progression flags), capabilities, menus, networking, global loot modifiers, and a large event bus mean fewer Mixins into vanilla for the systems we want (boss gating, reforging, accessories, dungeon keys). Fewer Mixins means fewer things break on each version bump.
2. **The accessory library we would actually use is NeoForge-only on 26.x.** Curios has 26.2 builds; Accessories (the multi-loader alternative) is still on 1.21.10.
3. **It is where the Forge-lineage community moved.** On 1.21.1 NeoForge has four times the mods of legacy Forge; on 26.2 nearly twice. If we ever open the pack to friends who want QoL mods, NeoForge is the well-trodden path for large packs.
4. **Deobfuscation shrank Fabric's traditional edge.** Fabric's speed advantage mattered most when ports were mapping-heavy; on 26.x, NeoForge reached stable builds for both drops within weeks.

Why not the alternatives (one line each):
- **Fabric**: an excellent second choice and the fastest to each drop, but a thinner API means more Mixins for our systems, and its accessory libraries have not reached 26.x.
- **Forge**: alive, but the smallest community of the three and most of the original team is on NeoForge.
- **Quilt**: loader is maintained, but its standard library stopped at 1.21 in Dec 2024.
- **Multi-loader (Architectury / MultiLoader template)**: doubles build complexity and testing for zero benefit on a private pack where every mod is ours; revisit only if we publish publicly. Deobfuscation makes a later port cheaper than it used to be.

**Risk to track:** NeoForge's team is small by its own admission. Mitigation: our code stays on documented APIs, we keep Mixins minimal and isolated in one module, and Fabric remains a viable fallback port target.

---

## 3. Toolchain

All version numbers below were read from the loaders' Maven repositories, GitHub, or official docs on 4 Sep 2026. Fabric's meta server already lists 26.3-pre-2, so 26.3 is at pre-release.

### 3.1 Build system

| | NeoForge | Fabric |
|---|---|---|
| Gradle plugin | **ModDevGradle (MDG) 2.0.146** (`net.neoforged.moddev`); NeoGradle 7.1.38 remains an option | **Loom 1.17.x** (`net.fabricmc.fabric-loom`, the *non-remapping* plugin id introduced in Loom 1.14 for 26.1+; `fabric-loom-remap` is for ≤1.21.11) |
| Gradle | 9.2.1 wrapper in the MDK (min 9.1) | 9.5.1 (min 9.4) |
| Loader / API | NeoForge 26.2.0.76 (non-beta since 26.2.0.57) | Loader 0.19.5, Fabric API 0.158.0+26.2 |
| Java | JDK 25 (Microsoft OpenJDK recommended, any 64-bit JDK 25 works; foojay toolchain resolver in the MDK) | JDK 25 |
| Template | [neoforged.net/mod-generator](https://neoforged.net/mod-generator/) (defaults to MDG) or the pre-generated [MDK-26.2-ModDevGradle](https://github.com/NeoForgeMDKs/MDK-26.2-ModDevGradle) repo (regenerated 2 Sep 2026): Java 25 toolchain, configuration cache on, a `gameTestServer` run | [fabricmc.net/develop/template](https://fabricmc.net/develop/template/) |
| Release artifact | plain `jar` (no reobfuscation) | plain `jar` (no `remapJar`) |
| CI niceties | with `CI=true` MDG skips decompile/recompile (since 2.0.136) | `preferGradleTask = true` workaround for IntelliJ configuration-cache issues |

MDG's "vanilla mode" (`neoFormVersion` instead of `version`) is how a loader-neutral common subproject would be built if we ever went multi-loader; MDG's "legacy" plugin targets Forge/vanilla 1.17–1.20.1 and is irrelevant here.

**Recommendation:** ModDevGradle on NeoForge. Confidence **high**. Why not NeoGradle: slower setup, no longer the generator default, less active (last push 9 Aug 2026 vs MDG pushed 4 Sep 2026).

### 3.2 Mappings

26.1 ships official class, method, field **and parameter** names, so there is nothing to map. Fabric stopped Yarn and Intermediary after 1.21.11 and renamed Fabric API to official names. Parchment's last data set is for 1.21.11 (Dec 2025); `parchment-26.1`/`26.2` artifacts do not exist on its Maven and the 26.2 MDK has no Parchment block. No formal Parchment end-of-life notice was found (flagged), but the artifacts are simply absent. No remap step at publish time; Mixin refmaps are unnecessary.

**Recommendation:** Mojang names only, no Parchment, no remapping. Confidence **high**. Why not the alternatives: Yarn is frozen at 1.21.11; Parchment has no 26.x data.

### 3.3 IDE

- **IntelliJ IDEA 2025.3 or newer** is required for Mixin support on 26.1+ (Fabric 26.1 post); use 2026.x.
- **Minecraft Development plugin 1.8.22** (31 Aug 2026): builds for 2025.3/2026.1/2026.2; recent releases fixed MDG sync on 2026.2, hid the Yarn selector for 26.1+ projects, and added MixinExtras expression debugging.
- **Ravel** is not a Fabric project: it is a third-party PSI/Mapping-IO source remapper (0.6.4, 24 Mar 2026) that Fabric recommends for Yarn → Mojmap migration of *existing* mods, especially Kotlin. Irrelevant for a from-scratch project.
- VS Code has a Fabric setup track but lacks Mixin inspections; Eclipse is officially supported by NeoForge but MDG notes it cannot hold multiple runtime classpaths per project.

**Recommendation:** IntelliJ IDEA 2026.x + Minecraft Development plugin. Confidence **high**.

### 3.4 Hot reload and iteration speed

| Layer | What reloads | How |
|---|---|---|
| Data packs (recipes, loot, tags, advancements, functions, data-driven game tests) | Everything in `data/` | `/reload` in-game |
| Resource packs (models, textures, lang, sounds) | Everything in `assets/` | F3+T |
| Java method bodies | Existing methods only | IDE debug HotSwap |
| Java structure (add/remove fields and methods, lambdas) | Most non-hierarchy changes | **JetBrains Runtime with DCEVM**: run the game on JBR 25 (25.0.4.1, 24 Aug 2026) with `-XX:+AllowEnhancedClassRedefinition`; MDG's README uses exactly this flag as its `jvmArgument` example. Mixin hotswap additionally needs `-javaagent:<mixin jar>`. |
| Registrations (new blocks, items, entities, packets), class-hierarchy changes, Mixin target changes | Nothing | Restart the client/server. |

No new official or loader-provided hot-reload facility appeared in 26.x (flagged as checked, not found). Practical implication: the more of our gameplay is data-driven, the more of our iteration loop is `/reload` instead of restarts.

**Recommendation:** JBR 25 as the run JDK with enhanced class redefinition and the Mixin agent; keep gameplay data-driven. Confidence **medium** (JBR 25 works in principle; no modder report specifically on 26.x + JBR 25 was found).

### 3.5 Testing

- **GameTest is data-driven since 1.21.5** (`test_instance` + `test_environment` registries; block-based or function tests; `/test run|runall|runfailed`). 26.x changes: `game_rules` uses a single `rules` key, `time_of_day` → `clock_time` (26.1), `timeline_attributes` (26.1), `difficulty` environment (26.2). Headless run: `java -DbundlerMainClass=net.minecraft.gametest.Main -jar server.jar --tests <glob> --report <junit.xml> --packs <dir> --verify`.
- **NeoForge:** register test functions with `DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, modid)` plus `test_instance`/`test_environment` JSON (or datagen, or `RegisterGameTestsEvent`); `gradlew runGameTestServer` exits with the number of failed required tests. The MDK gates tests by `neoforge.enabledGameTestNamespaces` while the docs page mentions only `neoforge.enableGameTest` (inconsistency flagged). **JUnit:** MDG `neoForge { unitTest { enable(); testedMod = mods.x } }` boots FML for unit tests; `net.neoforged:testframework` adds `@ExtendWith(EphemeralTestServerProvider.class)` to inject a live `MinecraftServer`.
- **Fabric:** `fabricApi { configureTests { … } }` creates a `src/gametest` source set; server tests via Fabric's `GameTest` annotation run under `check`; client tests via `FabricClientGameTest` (screenshots, world builder), headless with xvfb in CI. Unit tests use `fabric-loader-junit` and bootstrap registries manually.

**Recommendation:** JUnit for pure logic (modifier rolling, progression rules), GameTest server runs for gameplay (a boss spawns, a door unlocks, a reforge applies), both on every CI build. Confidence **high**.

### 3.6 CI

- Baseline: the official Fabric example workflow (`ubuntu-24.04`, `actions/checkout@v6`, `gradle/actions/wrapper-validation@v6`, `actions/setup-java@v5` with Java 25, `./gradlew build`, `upload-artifact@v7`) works unchanged for NeoForge.
- `gradle/actions/setup-gradle@v6` (6.3.0, 2 Aug 2026): its enhanced caching moved to a proprietary library that is free for public repos and requires accepting terms; an MIT `cache-provider: basic` exists since 6.1.0. **For a private repo use `cache-provider: basic`** (whether enhanced caching is free for private repos could not be confirmed).
- NeoForge: add `./gradlew runGameTestServer` as a job step. MDG's `CI=true` path avoids decompiling.
- Publishing, only if we ever go public: `Kira-NT/mc-publish@v3` (3.3.1, 14 Jul 2026) or Modrinth's Minotaur 2.9.0 / Darkhax's CurseForgeGradle 1.3.33; matthewprenger's CurseGradle is archived.

**Recommendation:** GitHub Actions with setup-java + setup-gradle (basic cache) + build + game tests; no publishing plugin for now. Confidence **high** (medium on the caching licence question).

### 3.7 Distribution for a small private server

| Option | Client auto-update | Server install | Works with private mods |
|---|---|---|---|
| **packwiz + packwiz-installer** | Yes, every launch | `packwiz-installer-bootstrap -g -s server <pack.toml>` | Yes: TOML pack in git, hosted on any static host (GitHub Pages) |
| `.mrpack` via Modrinth App | Only for Modrinth-hosted packs | mrpack-install / mrpack4server | Jars must come from cdn.modrinth.com, github.com, raw.githubusercontent.com or gitlab.com, or ship in `overrides` |
| `.mrpack` via Prism Launcher | Manual re-import on every change | same | same |
| CurseForge | Yes via the CurseForge app | manual | Needs a public listing |

packwiz is active (commit 2 Sep 2026); it exports `.mrpack` and CurseForge formats too, and `packwiz serve` tests locally. packwiz-installer's last release is 0.5.14 (Apr 2024): dormant but functional (flagged). Prism Launcher 11.1.0 (3 Sep 2026) runs the installer as a pre-launch command. NeoForge server install: `java -jar neoforge-<ver>-installer.jar --install-server` (docs also show `--installServer`; both appear), then `run.sh`; ServerStarterJar (Aug 2026) exists for hosts that demand a `server.jar`. The Modrinth App does not auto-update locally imported packs (issue #362 closed 29 Jul 2026; whether it was implemented is unverified).

**Recommendation:** a packwiz pack in a GitHub repo served from GitHub Pages, our mod jars attached to GitHub Releases, packwiz-installer on every Prism client and in the server start script. Confidence **high**. Why not the alternatives: the Modrinth App cannot auto-update a private pack, CurseForge needs a public listing, a raw `.mrpack` means re-importing on every change.

### 3.8 Unverified items carried forward
- A formal Parchment end-of-life statement (artifacts simply do not exist for 26.x).
- Free enhanced Gradle caching on private GitHub repos.
- Modrinth App local-pack updating after issue #362.
- A modder report of JBR 25 DCEVM with 26.x.
- NeoForge installer flag spelling and the `enableGameTest` vs `enabledGameTestNamespaces` property mismatch.

Sources: [Fabric for 26.1 (14 Mar 2026)](https://fabricmc.net/2026/03/14/261.html), [Fabric for 26.2 (15 Jun 2026)](https://fabricmc.net/2026/06/15/262.html), [Loom releases](https://github.com/FabricMC/fabric-loom/releases), [Loom docs](https://docs.fabricmc.net/develop/loom/), [Fabric Maven metadata](https://maven.fabricmc.net/net/fabricmc/fabric-loom/maven-metadata.xml), [Fabric meta](https://meta.fabricmc.net/v2/versions/loader), [fabric-example-mod](https://github.com/FabricMC/fabric-example-mod), [Fabric docs: mappings](https://docs.fabricmc.net/develop/porting/mappings/), [automatic testing](https://docs.fabricmc.net/develop/automatic-testing), [IntelliJ setup](https://docs.fabricmc.net/develop/getting-started/intellij-idea/setting-up), [launching the game](https://docs.fabricmc.net/develop/getting-started/intellij-idea/launching-the-game), [Fabric wiki: hotswapping](https://wiki.fabricmc.net/tutorial:hotswapping), [mixin hotswaps (24 Mar 2026)](https://wiki.fabricmc.net/tutorial:mixin_hotswaps), [NeoForge for 26.1 (24 Mar 2026)](https://neoforged.net/news/26.1release/), [NeoForged Maven latest-version API](https://maven.neoforged.net/api/maven/latest/version/releases/net/neoforged/moddev-gradle), [ModDevGradle](https://github.com/neoforged/ModDevGradle), [MDK-26.2-ModDevGradle](https://github.com/NeoForgeMDKs/MDK-26.2-ModDevGradle), [mod-generator source](https://github.com/neoforged/mod-generator), [NeoForge docs: getting started](https://docs.neoforged.net/docs/gettingstarted/), [game tests](https://docs.neoforged.net/docs/misc/gametest/), [server install](https://docs.neoforged.net/user/docs/server/), [NeoForge JUnit sources (26.2.x)](https://github.com/neoforged/NeoForge/tree/26.2.x), [ServerStarterJar](https://github.com/neoforged/ServerStarterJar), [Parchment getting started](https://parchmentmc.org/docs/getting-started), [Parchment Maven](https://maven.parchmentmc.org/org/parchmentmc/data/), [GameTest – Minecraft Wiki](https://minecraft.wiki/w/GameTest), [Minecraft Development plugin](https://plugins.jetbrains.com/plugin/8327-minecraft-development), [MinecraftDev releases](https://github.com/minecraft-dev/MinecraftDev/releases), [Ravel](https://github.com/badasintended/ravel), [JetBrains Runtime](https://github.com/JetBrains/JetBrainsRuntime), [Mixin issue #592](https://github.com/SpongePowered/Mixin/issues/592), [gradle/actions v6 blog (24 Mar 2026)](https://blog.gradle.org/github-actions-for-gradle-v6), [gradle/actions releases](https://github.com/gradle/actions/releases), [mc-publish](https://github.com/Kira-NT/mc-publish), [Minotaur](https://github.com/modrinth/minotaur/releases), [.mrpack format (15 Oct 2025)](https://support.modrinth.com/en/articles/8802351-modrinth-modpack-format-mrpack), [Modrinth App issue #362](https://github.com/modrinth/code/issues/362), [packwiz](https://github.com/packwiz/packwiz), [packwiz-installer docs](https://packwiz.infra.link/tutorials/installing/packwiz-installer/), [Prism Launcher 11.1.0](https://github.com/PrismLauncher/PrismLauncher/releases/tag/11.1.0), [mrpack4server](https://github.com/Patbox/mrpack4server).

---

## 4. Content authoring model: JSON vs Java

Scope: Java Edition 26.2 (data pack format 107.1, resource pack format 88.0). Verified 4 Sep 2026 against minecraft.wiki, docs.fabricmc.net and docs.neoforged.net; items marked *[unverified]* could not be confirmed on a fetched page and go on the Milestone 0 checklist.

### 4.1 What recent drops handed to data packs

| Version | Date | Data-driven additions that matter to us |
|---|---|---|
| 1.21.9 | 30 Sep 2025 | `pack.mcmeta` `min_format`/`max_format`; Mannequin entity (posable humanoid, useful for NPC stand-ins). |
| 1.21.11 | 9 Dec 2025 | `timeline/` registry; **environment attributes** (`visual/*`, `gameplay/*`, `audio/*`) replace hard-coded dimension-type flags; **weapon item components** `attack_range`, `damage_type`, `kinetic_weapon`, `piercing_weapon`, `minimum_attack_charge`, `swing_animation`, `use_effects`; loot function `discard`. |
| 26.1 | 24 Mar 2026 | Unobfuscated jars, Java 25; `villager_trade/`, `trade_set/`, `world_clock/`; components `dye`, `additional_trade_cost`; number providers `environment_attribute`, `sum`; predicate `environment_attribute_check`; `dimension_type` gains `default_clock`, `has_ender_dragon_fight`; world save data is namespaced (`data/<ns>/…`). |
| 26.2 | 16 Jun 2026 | New feature types `sequence`, `template`, `weighted_random_selector`; density function `interval_select`; block predicate `matching_biomes`; entity attributes `bounciness`, `air_drag_modifier`, `friction_modifier`; **entity predicates restructured to a component map** (a breaking JSON change: `type` → `minecraft:entity_type`, etc.); test environment `difficulty`; experimental Vulkan backend. |

Sources: [Java Edition 1.21.9](https://minecraft.wiki/w/Java_Edition_1.21.9), [1.21.11](https://minecraft.wiki/w/Java_Edition_1.21.11), [26.1](https://minecraft.wiki/w/Java_Edition_26.1), [26.2](https://minecraft.wiki/w/Java_Edition_26.2) on minecraft.wiki.

### 4.2 Data-driven inventory (data pack side)

**Reloadable in-game with `/reload`:** advancements, functions (with macros), item modifiers, loot tables, predicates, recipes, tags (every registry, including `villager_trade` and `timeline`), structure templates (`structure/*.nbt`).

**Dynamic registries (loaded at world start; a change needs a world reload):**

| Folder | Since | Controls |
|---|---|---|
| `dimension`, `dimension_type` | 1.16 | Generator, biome source, height, light, environment attributes, `default_clock`, timelines. |
| `worldgen/*` (biome, carvers, configured/placed features, density functions, noise, noise settings, structures, structure sets, template pools, processor lists, world presets, multi-noise parameter lists) | 1.16–1.19 | All terrain and structure generation, including jigsaw dungeons. |
| `enchantment`, `enchantment_provider` | 1.21 | Enchantments as effect components (damage, knockback, projectile behaviour, attributes, `post_attack`, `hit_block`, `tick`, `location_changed`…) with entity effects such as `apply_mob_effect`, `damage_entity`, `explode`, `ignite`, `spawn_particles`, `summon_entity`, `replace_block`, and **`run_function`**. |
| `damage_type`, `chat_type` | 1.19–1.19.4 | Death messages, exhaustion, immunity tags. |
| `trim_material`, `trim_pattern`, `banner_pattern`, `painting_variant`, `jukebox_song`, `instrument` | 1.20–1.21.2 | Fully JSON. |
| Mob variants (`wolf_variant`, `cat_variant`, `pig_variant`, `cow_variant`, `chicken_variant`, `frog_variant`, …) | 1.20.5–1.21.11 | Texture, model choice and spawn conditions per variant; cannot add variants to mobs without a variant registry or change geometry. |
| `dialog` | 1.21.6 | Server-driven modal UI: `notice`, `confirmation`, `multi_action`, `server_links`, `dialog_list`; bodies `plain_message`, `item`; inputs `text`, `boolean`, `single_option`, `number_range`; actions run commands (incl. macros), open dialogs, custom events. Opened by `/dialog`, chat click events, pause-menu and quick-action tags. **Limits:** fixed layouts, **no item slots or inventories**, no NBT/score/selector in text, player frozen while open. |
| `villager_trade`, `trade_set` | 26.1 | Merchant offers (`wants`, `gives`, `max_uses`, `xp`, `merchant_predicate`) and pools. |
| `world_clock`, `timeline` | 26.1 / 1.21.11 | Per-dimension clocks, time markers, periodic environment-attribute modifiers. |
| `trial_spawner` | 1.21.2 | Spawner configurations. |
| `test_instance`, `test_environment` | 1.21.5 | Data-driven game tests (`block_based` or `function`), with environments for game rules, clock time, weather, difficulty. |

**Item data components usable from JSON and commands (no Java):** `weapon`, `blocks_attacks`, `consumable`, `food`, `tool`, `equippable` (any slot), `glider`, `use_cooldown`, `use_remainder`, `death_protection`, `damage_resistant`, `attack_range`, `kinetic_weapon`, `piercing_weapon`, `swing_animation`, `use_effects`, `damage_type`, `enchantable`, `repairable`, `attribute_modifiers`, `custom_data`, `item_model`, `tooltip_style`, `dye`. There is **no component that makes an arbitrary item fire a projectile**; bows, crossbows and wands stay Java.

### 4.3 Resource pack side

- **Block and item models**: `blockstates/`, `models/`; since 26.1 any block model may force translucent textures; 26.2 moved beds and signs to block models.
- **Item model definitions** (`items/`, 1.21.4+): `model`, `composite`, `condition`, `select`, `range_dispatch`, `special`, with conditions (`using_item`, `broken`, `damaged`, `has_component`, `component`, `keybind_down`), selectors (`main_hand`, `trim_material`, `block_state`, `display_context`, `local_time`, `context_dimension`, `context_entity_type`, `component`) and ranges (`damage`, `count`, `cooldown`, `time`, `use_duration`, `use_cycle`). This covers "the sword glows when reforged" without Java.
- **Equipment models** (`equipment/`, 1.21.2+): armor layers (`humanoid`, `humanoid_leggings`, `wings`, saddles, animal bodies) and dyeable defaults.
- **Textures with `.mcmeta` animation**, atlases, sounds + `sounds.json`, fonts, `particles/<id>.json` (texture lists only), `lang/`, post effects.
- **Shaders**: core shaders are "not officially supported"; 26.1 rewrote the lightmap shader, 26.2 collapsed text shaders, and shader packs do not run on the Vulkan backend. Fabric notes OpenGL is planned for removal once Vulkan is stable. **Treat custom shaders as out of scope.**

### 4.4 What requires Java (Fabric API vs NeoForge)

| Need | Fabric (Loader 0.19.3, API 0.152, Loom 1.17) | NeoForge (26.2.0.7x) |
|---|---|---|
| Register blocks, items, entities, block entities, menus, particles, effects, attributes, component types | `Registry.register(BuiltInRegistries.X, …)` | `DeferredRegister` families, `RegisterEvent` |
| Block behaviour, block entities, block entity renderers | Vanilla classes | Vanilla classes |
| Entities, AI goals, attributes, models/renderers, keyframe animation | `EntityType.Builder`, `PathfinderMob#registerGoals`, `FabricDefaultAttributeRegistry`, `EntityRenderers`, `AnimationDefinition` | Same vanilla classes; `EntityAttributeCreationEvent`, `EntityRenderersEvent` *[names from memory]* |
| GUIs with item slots | `MenuType` + `AbstractContainerMenu` + `MenuScreens` | `IMenuTypeExtension`, `openMenu` with extra data, `RegisterMenuScreensEvent` |
| Networking | `CustomPacketPayload` + `StreamCodec`, `PayloadTypeRegistry`, `ServerPlayNetworking` | `RegisterPayloadHandlersEvent` → versioned `PayloadRegistrar`, `PacketDistributor`; 1 MiB to client / 32 KiB to server limits |
| Custom item data components | `DataComponentType.builder().persistent(codec)` | `DeferredRegister.DataComponents` |
| Custom enchantment effect types, custom JSON registries | Registry entries + `MapCodec`; `DynamicRegistries.register` *[unverified]* | Same; `DataPackRegistryEvent.NewRegistry` |
| Per-player / per-chunk / per-level data | Fabric API Data Attachments (`AttachmentRegistry`, persistent, `copyOnDeath`, synced) | NeoForge Data Attachments (`AttachmentType.builder()`, `serialize`, `copyOnDeath`, `sync`) |
| World saved data | Vanilla `SavedData` / `SavedDataType`; 26.1 namespaced file layout | Same |
| Portal / teleport logic | Vanilla `Portal` block interface + teleport transition (1.21+) *[verify in M0]* | Same; documented in the [NeoForge 1.21 primer](https://docs.neoforged.net/primer/docs/1.21/) |
| Boss bars | `/bossbar` for simple cases; `ServerBossEvent` for entity-bound bars | Same |
| HUD / custom rendering | `HudElementRegistry` (replaced `HudRenderCallback` in 26.1) | `GuiGraphicsExtractor` (26.1 rename), `MutableQuad` |
| Inject features/spawns into vanilla biomes | Java `BiomeModifications` | **JSON** biome modifiers (`neoforge/biome_modifier/`) |
| Inject loot without overriding tables | `LootTableEvents` *[unverified on 26.x]* | **JSON** global loot modifiers (`neoforge:add_table`) |
| Registry-object metadata (e.g. "tier" of an item) | — | **JSON** data maps (`data_maps/<registry>/`) |
| Novel mechanics | Mixins, events | Events, Mixins, access transformers |

Two of the three "extra JSON surfaces" (biome modifiers, global loot modifiers, data maps) exist only on NeoForge, which is one more point for the loader recommendation in section 2.

### 4.5 Wish-list mapping

| Feature | Verdict | Notes |
|---|---|---|
| Custom dimensions | **JSON only** (+ thin Java for a real portal block) | Dimension, dimension type, environment attributes, timelines, world clock. |
| Custom biomes | **JSON only** | Biome + placed features; adding to vanilla dimensions is JSON on NeoForge. |
| Dungeon structures (jigsaw) | **JSON only** | Structure + template pools + processor lists + structure sets; 26.2 `template` feature for scatter pieces. |
| Loot with rarity tiers | **JSON only** | Loot tables, `random_sequence`, `set_attributes`, `set_enchantments`, `set_components`, `custom_data`. |
| Boss with phases | **Substantial Java** | Custom entity, goals, renderer, animation, network sync. |
| Boss-gated world flags | **Thin Java** | `SavedData` / attachments if Java systems must read them; advancements as triggers. |
| Weapon reforging with modifier pools | **JSON + thin Java** | Pools and costs as JSON (our own dynamic registry); the menu (item slot) is Java because dialogs cannot show item slots. |
| Accessory slots | **Substantial Java or library** | Curios 16.0.0+26.2 (NeoForge) or our own attachment-backed container. |
| Decorative block sets | **Thin Java + datagen JSON** | Vanilla stair/slab/wall/door classes; all assets generated. |
| Functional crafting stations | **Substantial Java** | Menu + screen + block entity; recipe *types* in Java, recipes in JSON. |
| NPC hub / town | **JSON-heavy** | Data-driven trades, dialogs, structures; custom NPC entities in Java. |
| Potions / buffs | **JSON** for potions of existing effects; **Java** for new effects | |
| Invasions / events | **JSON + functions** for timers and bars; **substantial Java** for raid-like waves | |
| Custom weapons | **JSON** for stats, reach, piercing, kinetic, use effects, enchant hooks; **Java** for projectiles, magic, new swing logic | |
| Mounts | **Substantial Java** | Saddle and body armour visuals are JSON via `equippable` + `equipment/`. |

### 4.6 Data generation and tools

- **Datagen is mandatory at our scale.** NeoForge: `GatherDataEvent.Client`/`.Server` with providers for models/blockstates/client items, language, equipment assets, particles, sounds, advancements, loot tables, recipes, tags, data maps, global loot modifiers and dynamic registry entries (worldgen, damage types, enchantments) via `DatapackBuiltinEntriesProvider`; output to `src/generated/resources`. Fabric: `DataGeneratorEntrypoint` + `runDatagen` with the equivalent providers.
- **Misode's generators** (misode.github.io) support 26.1/26.2/26.3 for worldgen, dimensions, enchantments, dialogs, predicates and loot; use them to prototype JSON, then port to datagen.
- **Blockbench** exports vanilla block/item model JSON and "Modded Entity" Java models; animations reach the game through GeckoLib/AzureLib or vanilla keyframe code.
- **Structure templates**: structure blocks save to `generated/<ns>/structure/` (singular since 26.1); Axiom 4.3.3 can export structure NBT directly.

### Recommendation

**Data-first, Java-thin.** Every system gets a JSON schema (our own dynamic registries where vanilla has none: reforge pools, progression tiers, dungeon keys), and Java only where the table above says so. Confidence: **high**. Why not "Java-first": it is slower to iterate, invisible to non-programmer collaborators, and 26.x has moved most of what we need into data; why not "data-only": bosses, menus with slots, projectiles and accessories genuinely need code.

Sources: [Data pack](https://minecraft.wiki/w/Data_pack), [Resource pack](https://minecraft.wiki/w/Resource_pack), [Dialog](https://minecraft.wiki/w/Dialog), [Items model definition](https://minecraft.wiki/w/Items_model_definition), [Equipment](https://minecraft.wiki/w/Equipment), [Enchantment definition](https://minecraft.wiki/w/Enchantment_definition), [Mob variant definitions](https://minecraft.wiki/w/Mob_variant_definitions), [Test instance definition](https://minecraft.wiki/w/Test_instance_definition), [Data component format](https://minecraft.wiki/w/Data_component_format) (minecraft.wiki, accessed 4 Sep 2026); [Fabric docs](https://docs.fabricmc.net/develop/) (data attachments, data generation, entities, container menus, networking, particles, custom data components, custom enchantment effects, porting); [NeoForge docs](https://docs.neoforged.net/docs/concepts/registries) (registries, attachments, saved data, payloads, resources, particles, data maps, global loot modifiers, biome modifiers), [NeoForge 26.1 primer](https://docs.neoforged.net/primer/docs/26.1/); [Misode generators](https://misode.github.io/generators/); [Blockbench formats](https://blockbench.net/wiki/blockbench/formats/).

---

## 5. Reference points (design ideas only)

Research date 4 Sep 2026. "MR-API" means the version/date/license was read from the Modrinth public API that day; otherwise it comes from the linked page. We study these for *mechanics*; we do not copy code, assets, names or structure files (see licensing notes at the end).

### 5.1 Boss-gated progression

| Mod | Loader / latest MC | Mechanism (player view) | Implementation notes | License | Sources |
|---|---|---|---|---|---|
| Twilight Forest | Forge/NeoForge/Fabric; 1.21.1 (CurseForge, 27 Aug 2026) | Linear chain: Naga → Lich → Swamp / Dark Forest / Snowy bosses. Locked biomes inflict effects (blindness/damage); locked structures are "shielded" (blocks regenerate, mobs invulnerable) until the prior boss's trophy is picked up. | Gamerule `tfEnforcedProgression`; unlocks tracked as **advancements** (since 3.0); structures wrapped in a custom `progression` structure type in datapack JSON; conquest tracked per structure start (maintainer issue #2459, 28 Jul 2025). | Code **LGPL-2.1**; assets **CC BY-NC-SA 4.0**; sounds and structure NBTs **ARR** | [LICENSE](https://github.com/TeamTwilight/twilightforest/blob/1.21.x/LICENSE), [ASSET_LICENSE](https://github.com/TeamTwilight/twilightforest/blob/1.21.x/ASSET_LICENSE), [CurseForge](https://www.curseforge.com/minecraft/mc-mods/the-twilight-forest), [issue #2459](https://github.com/TeamTwilight/twilightforest/issues/2459), [FTB wiki](https://ftb.fandom.com/wiki/Twilight_Forest_Progression) |
| Blue Skies | Forge/NeoForge; 1.20.4 (last update 5 Feb 2024, MR-API) | Two dimensions, two dungeons each. Collect 4 keys (one per floor) → insert into keystone → teleported to boss. Beating the boss unlocks new Gatekeeper trades and an item to respawn the boss harder. Conquered dungeons let you *buy* keys. | Key items + keystone block; NPC trade tiers double as progression state. | **ARR** | [Modrinth](https://modrinth.com/mod/blue-skies) |
| L_Ender's Cataclysm | Forge/NeoForge; 1.21.1 (CurseForge 22 Aug 2026), 1.21.5 | Post-dragon bosses each sit in a bespoke dungeon (citadel, arena, sunken city). Some are awakened by items (a Nether Star, an altar fed with ashes). Community "recommended order", not enforced. | Pre-placed boss entities in structures; item-activated altars. | **CC BY-NC-ND 4.0** (Modrinth) / "Custom" (CurseForge). A third-party GitHub mirror claiming MIT is not the author's; disregard. | [Modrinth](https://modrinth.com/mod/l_enders-cataclysm), [CurseForge](https://www.curseforge.com/minecraft/mc-mods/lendercataclysm), [wiki](https://lendercataclysm.wiki.gg/) |
| Bosses of Mass Destruction | Fabric only; 1.21.1 (13 Oct 2024, MR-API) | Four arena bosses located by "guide" items; no enforced order. | Structure-placed bosses; GeckoLib + Cardinal Components. | **LGPL-3.0-only** | [Modrinth](https://modrinth.com/mod/bosses-of-mass-destruction) |
| Mowzie's Mobs | Forge/NeoForge; 1.21.1 (CurseForge, Mar 2026) | Bosses spawn once as worldgen structures and drop ability items. | Structure-bound entities, GeckoLib; source public. | **Custom "Mowzie's Mobs License"** (treat as ARR) | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/mowzies-mobs), [Modrinth](https://modrinth.com/mod/mowzies-mobs) |
| DawnCraft (pack) | Forge 1.18.2 (CurseForge 20 Nov 2025) | Workstations locked behind a knowledge/reputation system; main-quest boss kills unlock anvil/enchanting; 12 custom Ender Eyes collected from bosses/traders to reach the End. | Custom companion mod + FTB Quests; Epic Fight combat. (Wiki pages returned HTTP 402; details from search snippets only.) | **ARR** | [CurseForge](https://www.curseforge.com/minecraft/modpacks/dawn-craft) |
| Prominence II: Hasturian Era (pack) | Fabric 1.20.1 (MR-API 4 Sep 2026) | "Stage leveling": player level cap 10 until a first campaign boss dies; caps 50 then 65 unlock per campaign stage; bosses show level. | Pack scripting over a skills mod + BOMD (scripts not documented). | **ARR** | [Modrinth changelog](https://modrinth.com/modpack/prominence-2-fabric/changelog) |
| Terramity / Prodigium Reforged (Terraria-likes) | Forge 1.20.1 (Terramity Jan 2025; Prodigium 9 Jun 2026) | Terramity: boss-driven progression, guidebook, 250+ Curios accessories, guns. Prodigium: "defeat bosses to unlock new ores, monsters, NPCs". | Mechanisms undocumented publicly; could not verify. | Both **ARR** | [Terramity](https://modrinth.com/mod/terramity), [Prodigium](https://www.curseforge.com/minecraft/modpacks/prodigium-reforged) |

**Takeaways for us.** Twilight Forest is the proof that hard gating works in Minecraft when the *world itself* enforces it (shielded structures, hostile biomes) rather than a quest book. Blue Skies shows keys + a keystone teleport as a clean "dungeon floor → boss" loop and reuses NPC trade tiers as progression state, which maps directly onto 26.1's data-driven trades. Advancements as the unlock trigger surface is the common, well-supported pattern.

### 5.2 Dimension travel and portals

| Mod | Loader / latest MC | Mechanism | Implementation notes | License | Sources |
|---|---|---|---|---|---|
| The Aether | Fabric/Forge/NeoForge; 1.21.1 (3 Oct 2025). Aether II: NeoForge 26.1.2 alpha (24 Jul 2026) | Glowstone frame + water bucket. Three dungeons with bosses; finishing unlocks day control. | Nether-portal-style frame validation with a custom fluid; dungeon reward chests. | Code **LGPL-3.0**, assets **ARR** | [Modrinth](https://modrinth.com/mod/aether), [GitHub](https://github.com/The-Aether-Team/The-Aether), [Aether II](https://www.curseforge.com/minecraft/mc-mods/aether-ii) |
| Twilight Forest | see 5.1 | Pool of water ringed by natural blocks with plants on the ring, any shape/size. | Shape-agnostic frame check via block tags. | see 5.1 | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/the-twilight-forest) |
| The Undergarden | Forge/NeoForge; 26.1.2 (MR-API 24 Aug 2026) | Nether-shaped frame of stone/deepslate bricks, activated by a crafted Catalyst item; ~10 advancements guide play. | Custom portal block + activation item. | **ARR** | [Modrinth](https://modrinth.com/mod/the-undergarden) |
| Deeper and Darker | Fabric/Forge/NeoForge/Quilt; 1.21.1 (11 Jul 2026) | Kill the Warden → heart item → activate a Reinforced Deepslate frame (only found in Ancient Cities) → the Otherside. | Uses a pre-existing vanilla structure as the frame, so the portal's *location* is itself gated by exploration. | **AGPL-3.0** | [Modrinth](https://modrinth.com/mod/deeperdarker), [GitHub](https://github.com/KyaniteMods/DeeperAndDarker) |
| Blue Skies | see 5.1 | Find the Gatekeeper NPC, buy a lighter, light the frame in his house. | NPC-gated ignition item. | **ARR** | [Modrinth](https://modrinth.com/mod/blue-skies) |

**Takeaways.** The strongest gating is "the portal frame is a place you must earn or find" (Deeper and Darker, Blue Skies), not "the recipe is expensive". Activation items are the standard way to tie a dimension to a boss kill.

### 5.3 Dungeon and structure generation

| Mod | Loader / latest MC | Mechanism | Implementation notes | License | Sources |
|---|---|---|---|---|---|
| YUNG's Better Dungeons | Fabric/Forge/NeoForge; 26.1.2 (24 Jun 2026) | Catacombs, undead fortresses, spider caves; sprawling multi-room layouts. | Hand-built NBT pieces assembled by YUNG's API's reimplemented jigsaw manager (extra pool-element types, better performance). | **LGPL-3.0-only** | [Modrinth](https://modrinth.com/mod/yungs-better-dungeons), [YUNG's API](https://modrinth.com/mod/yungs-api) |
| When Dungeons Arise | Fabric/Forge/NeoForge; 1.21.1 (26 Oct 2025) | 30+ large themed dungeons, ships, castles, mob-filled. | Uses **vanilla jigsaw**; no keys/locks documented. | **ARR** | [Modrinth](https://modrinth.com/mod/when-dungeons-arise) |
| Dungeon Crawl | Forge/NeoForge; 26.2 (10 Aug 2026) | Multi-level roguelike dungeons; random layout, size, loot. | **Runtime procedural** layout using NBT "model" rooms; JSON themes swap block palettes per biome; loot/spawner JSON overrides. | **GPL-3.0** | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/dungeon-crawl), [theming wiki](https://github.com/XYROC/DungeonCrawl/wiki/2.3.x-Theming) |
| Dungeons and Taverns | Datapack (+loader jars); 26.2 (31 Aug 2026) | Vanilla-style structures; cartographer trade tiers as soft gating. | Pure datapack: vanilla structure/jigsaw JSON. | **ARR** | [Modrinth](https://modrinth.com/mod/dungeons-and-taverns) |
| Integrated Dungeons and Structures | Forge/NeoForge; 1.21.1 (11 Jun 2026) | 50+ hand-built, heavily detailed dungeons. | Hand-built; hard dependencies on other content mods. | **ARR** | [Modrinth](https://modrinth.com/mod/idas) |
| Repurposed Structures | Fabric/Quilt (+NeoForge); 26.2 (20 Aug 2026) | Biome variants of vanilla structures. | Since 1.18.2 entirely datapack JSON (template pools, processors). | **LGPL-3.0-only** | [Modrinth](https://modrinth.com/mod/repurposed-structures-fabric) |

**Lock-and-key patterns.** Blue Skies: 4 keys → keystone teleport. Twilight Forest: consumable Tower Keys open locked vanishing blocks and are kept on death; the Lich Tower has no door and its blocks regenerate until the Naga dies ([FTB wiki: Tower Key](https://ftb.fandom.com/wiki/Tower_Key)).

**Takeaways.** Hand-built rooms + vanilla jigsaw is the proven path on 26.x (Dungeons and Taverns and Repurposed Structures ship as pure data). Runtime-procedural layouts (Dungeon Crawl) are a separate engineering project; not for v1.

### 5.4 Weapon modifiers, reforging, affixes

| Mod | Loader / latest MC | Mechanism | Implementation notes | License | Sources |
|---|---|---|---|---|---|
| Apotheosis (Adventure module) | Forge/NeoForge; 26.1.2 (2 Aug 2026) | Gear rolls affixes by rarity (Common → Mythic/Ancient, several world tiers). A Reforging Table rerolls affixes for rarity materials + gem dust + XP; a simpler table caps at Rare. Salvaging breaks gear into materials; gems socket via sigils. | Affixes, gems, rarities and tier augments are all **datapack JSON**; depends on Placebo (MIT). | **MIT** | [Modrinth](https://modrinth.com/mod/apotheosis), [GitHub (26.1 branch)](https://github.com/Shadows-of-Fire/Apotheosis), [issue #704](https://github.com/Shadows-of-Fire/Apotheosis/issues/704) |
| Tinkers' Construct | Forge/NeoForge; 1.20.1 (12 Jan 2026) | Tools built from material parts; modifiers consume slot types at a station. | Modifiers are JSON (1.18.2+), composable modules in 1.20; slot types data-defined. | **MIT** (code + textures) | [Modrinth](https://modrinth.com/mod/tinkers-construct), [docs](https://slimeknights.github.io/docs/json/slot-types/) |
| Iron's Spells 'n Spellbooks | Forge/NeoForge; 1.21.1 (18 Aug 2026) | An arcane anvil applies upgrade orbs (spell power, mana, cooldown) and imbues swords with spells. | Attribute-style bonuses; source-available. | **ARR** (custom README) | [Modrinth](https://modrinth.com/mod/irons-spells-n-spellbooks), [GitHub](https://github.com/iron431/irons-spells-n-spellbooks) |
| Simply Swords | Fabric/Forge/NeoForge; 1.21.1 (27 Aug 2026) | 14 weapon types; loot-only "unique" weapons with active abilities. | Architectury; pairs with Better Combat. | **Timefall Development License** (custom; no porting/bundling outside CF/Modrinth) | [Modrinth](https://modrinth.com/mod/simply-swords), [LICENSE](https://github.com/Sweenus/SimplySwords/blob/Architectury/LICENSE) |
| Better Combat | Fabric/Forge/NeoForge; 26.2 (22 Jul 2026) | Weapon-specific swing arcs, combos, dual wield, hold-to-attack. | Per-item JSON weapon attributes; auto-presets for unknown weapons. | **ARR** | [Modrinth](https://modrinth.com/mod/better-combat) |
| Epic Fight | Forge/NeoForge; 1.21.1 (31 May 2026) | Souls-like stances, combos, stamina, skills. | Weapon movesets as datapack JSON. | **GPL-3.0-or-later** | [Modrinth](https://modrinth.com/mod/epic-fight), [docs](https://epicfight-docs.readthedocs.io/Guides/Weapons/page1/) |
| Ars Nouveau | Forge/NeoForge; 1.21.1 (24 Aug 2026) | Enchanting apparatus: reagent + pedestal items + mana → upgraded item. | Custom recipe type in JSON. | **GPL-3.0-only** | [Modrinth](https://modrinth.com/mod/ars-nouveau) |
| Enigmatic Legacy | Forge; 1.20.1 (5 Aug 2024) | Curios relics/trinkets, mostly utility. | — | **Custom viral license** | [LICENSE](https://github.com/Aizistral-Studios/Enigmatic-Legacy/blob/1.20.X/LICENSE.md) |

**Takeaways.** Apotheosis is the closest existing analogue to Terraria reforging and is MIT: its data-driven affix pools, rarity tiers and cost model are the reference design. The Terraria model (one prefix per item, rerolled for coin, always at an NPC) is simpler than Apotheosis and easier to balance; we should start there and treat sockets/gems as a later tier.

### 5.5 Accessory and trinket APIs

| Mod | Loaders / 26.x status (MR-API 4 Sep 2026) | Slot model | License | Sources |
|---|---|---|---|---|
| Curios API | 26.x builds are **NeoForge only**: 15.0.0+26.1.2 (19 Jul 2026), 16.0.0+26.2 (21 Jul 2026) | Slot types, entity bindings and item eligibility all defined in datapack JSON and tags. | **LGPL-3.0-or-later** | [Modrinth](https://modrinth.com/mod/curios), [docs](https://docs.illusivesoulworks.com/curios/slots/slot-register) |
| Trinkets | Fabric/Quilt; last release for 1.21.1 (15 Jul 2024); **dormant** | Slot groups with data-driven slots. | **MIT** | [Modrinth](https://modrinth.com/mod/trinkets) |
| Accessories | Fabric/Forge/NeoForge; latest 1.21.10 (10 Dec 2025) and 1.21.1 (6 Feb 2026); **no 26.x build** | Data-driven slots and groups; compat layer emulating Curios and Trinkets. | **MIT** | [Modrinth](https://modrinth.com/mod/accessories), [docs](https://docs.wispforest.io/accessories/general/slot_types) |

### 5.6 Animation and model libraries

| Library | 26.x status (MR-API 4 Sep 2026) | Workflow | License | Sources |
|---|---|---|---|---|
| GeckoLib | 5.5.4 for 26.2 on Fabric, Forge, NeoForge (30 Aug 2026) | Blockbench plugin → geo model + animation JSON; easings, sound/particle keyframes. | **MIT** | [Modrinth](https://modrinth.com/mod/geckolib), [wiki](https://wiki.geckolib.com/) |
| AzureLib | 4.0.0 for 26.2 on Fabric + NeoForge (30 Aug 2026) | Fork of GeckoLib 4; same Blockbench pipeline. | **MIT** | [Modrinth](https://modrinth.com/mod/azurelib) |
| Vanilla keyframe system (`AnimationDefinition`, 1.19+) | Part of the game; used by Warden, Camel | Blockbench exports it natively; no Molang, fewer easing options. | Exported code is yours | [Blockbench plugins list](https://github.com/JannisX11/blockbench-plugins/blob/master/plugins.json) |

### 5.7 Combat, magic and gun references (added after the 4 Sep 2026 combat decision)

Reference designs for the "vanilla expanded" combat layer, the magic system and the gun system. All three are to be built in-house; these are for mechanics study only. Statuses from the Modrinth API, 4 Sep 2026.

| Mod | Area | Latest MC | Mechanism worth studying | License | Source |
|---|---|---|---|---|---|
| Better Combat | Melee | 26.2 (22 Jul 2026) | Per-weapon JSON attack definitions: swing arcs, reach, combo chains, hold-to-attack, dual wield; first- and third-person animations via a player animation library. | **ARR** (look only) | [Modrinth](https://modrinth.com/mod/better-combat) |
| Epic Fight | Melee | 1.21.1 (31 May 2026) | Stance-based movesets, stamina, hit-stop, weapon movesets as datapack JSON. | **GPL-3.0-or-later** (read, never paste) | [Modrinth](https://modrinth.com/mod/epic-fight) |
| PlayerAnimator | Player animation library | 1.21.7 (28 Dec 2025); **no 26.x build** | Keyframe animation layers on the vanilla player model; the library Better Combat historically relied on. | **MIT** | [Modrinth](https://modrinth.com/mod/playeranimator) |
| Iron's Spells 'n Spellbooks | Magic | 1.21.1 (18 Aug 2026) | Mana pool, schools, spell levels, cast times, cooldown bars, spellbook slots, gear-scaled spell power. | **ARR** | [Modrinth](https://modrinth.com/mod/irons-spells-n-spellbooks) |
| Spell Engine + Wizards | Magic | 1.21.1 (1 Sep 2026) | Data-driven spell definitions (JSON) with a separate content mod; spell schools, casting animations, particles. | Engine **GPL-3.0-only**; Wizards **ARR** | [Spell Engine](https://modrinth.com/mod/spell-engine), [Wizards](https://modrinth.com/mod/wizards) |
| Ars Nouveau | Magic | 1.21.1 (24 Aug 2026) | Glyph-composed spells, mana as a resource, mana-per-glyph costs. | **GPL-3.0-only** | [Modrinth](https://modrinth.com/mod/ars-nouveau) |
| Timeless and Classics Guns | Guns | 1.18.2 (Apr 2024) | Reload/aim states, recoil, attachments, ammo types, first-person animation. | **GPL-3.0-or-later** | [Modrinth](https://modrinth.com/mod/timeless-and-classics-guns) |
| Just Enough Guns / Scorched Guns 2 | Guns | 1.20.1 | Simpler gun items: ammo consumption, spread, projectile entities, gun crafting. | **GPL-2.0-or-later** / **GPL-3.0-or-later** | [JEG](https://modrinth.com/mod/just-enough-guns), [Scorched Guns 2](https://modrinth.com/mod/scorched-guns-2) |
| Gamingbarn's Guns | Guns | 26.2 (3 Jul 2026) | The one gun mod already on 26.2; useful to see what a 26.x projectile/reload implementation touches. | **CC BY-NC-ND 4.0** (look only) | [Modrinth](https://modrinth.com/mod/gamingbarns-guns) |
| Linggango (modpack) | Parry, plunge, telegraphs | Forge 1.20.1, 450+ mods | The user's reference for parry feel: a tight parry window with a tighter *perfect* frame inside it; a parry gives hit-stop, screen shake, a small heal and a shove; perfect parries reflect projectiles; perfect hits turn a plunging attack into 1.5× damage and 4× knockback; mobs wind up and hold still while they do, ranged mobs hold their shot. The pack does not credit which mods implement this. | Modpack, treat as **ARR** (mechanics reference only) | [CurseForge](https://www.curseforge.com/minecraft/modpacks/linggango), [wiki](https://linggango.wiki.gg/) (accessed 4 Sep 2026) |
| Terraria (the game) | Classes, earned mobility | — | Damage classes (melee, ranged, magic, summon), class armor set bonuses, mana as a regenerating pool with potion sickness, minion slots, ammo consumed per shot with a chance to save; movement is earned through accessories (jump bottles, boots, dashes, dodges) rather than given at spawn. Thorium (a Terraria mod) adds healer and bard classes with ally-targeting. | Design reference only | — |

Not verified: licenses of MrCrayfish's Gun Mod and Timeless and Classics Zero (both distributed mainly on CurseForge); treat as ARR until read.

### 5.8 Licensing considerations

- **Permissive (MIT): study, adapt, even copy with attribution.** Apotheosis, Placebo, Tinkers' Construct, Trinkets, Accessories, GeckoLib, AzureLib, PlayerAnimator. Keep the copyright notice if code is copied.
- **Weak copyleft (LGPL 2.1/3): study freely; *copying* code into our mod makes that code LGPL** (modifications to it must be published), but depending on the mod as a library is fine. Twilight Forest code, Aether code, BOMD, YUNG's, Repurposed Structures, Curios, and NeoForge itself. Twilight Forest and Aether *assets* are separately licensed (CC BY-NC-SA / ARR).
- **Strong copyleft (GPL-2/3, AGPL-3): copying any code would oblige the whole mod to be GPL/AGPL.** Dungeon Crawl, Epic Fight, Ars Nouveau, Spell Engine, Deeper and Darker, Timeless and Classics Guns, Just Enough Guns, Scorched Guns 2. Read for design; never paste.
- **Custom, non-commercial, no-derivatives, or All Rights Reserved: look only.** Cataclysm (CC BY-NC-ND), Gamingbarn's Guns (CC BY-NC-ND), Mowzie's Mobs, Blue Skies, Undergarden, IDAS, When Dungeons Arise, Dungeons and Taverns, Better Combat, Iron's Spells, Wizards, Simply Swords, Enigmatic Legacy, Terramity, and all modpacks. Treat "Custom License" on CurseForge as ARR until the text has been read.
- **General rule.** Game *mechanics* (a boss gate, a key count, a reforge cost curve) are not protected by copyright; *expression* (code, textures, models, sounds, structure NBT files, names, lore) is. Do clean-room design: write each mechanic down in our own words from play or wiki observation, then implement without ARR/GPL source open side-by-side. This project's own mods should pick a license early (MIT or LGPL are the community norms) so the question never blocks a release.
- **Unverified.** DawnCraft and Blue Skies wiki pages were blocked (HTTP 402); Terramity/Prodigium internals are undocumented; Curios Fabric 26.x builds could not be found.

---

## 6. Hard parts, ranked

Difficulty here means "engineering risk for a two-person team with strong Java and some modding experience", not lines of code. Volume work (hundreds of blocks) is rated by *risk*, then flagged separately as *volume*.

| Rank | Feature | Difficulty | Why | What makes it tractable |
|---|---|---|---|---|
| 1 | **Multiplayer-safe boss fights** (phases, arena rules, telegraphed attacks, animation) | **Hard** | Everything must be server-authoritative and synced: phase state, attack telegraphs, summoned adds, arena bounds, boss bar, and animation triggers all cross the network. Desync and "boss stuck" bugs are the classic failure mode. Balancing for 1–4 players is design work on top. | Vanilla gives `ServerBossEvent` (boss bars), `SynchedEntityData` (state sync), goal-based AI, and keyframe animations exported from Blockbench (`AnimationDefinition`, since 1.19.3). GeckoLib 5.5.4 (26.2, all loaders) handles animated models/particles with far less boilerplate. Start with *one* boss and a reusable "phase machine" base class. |
| 2 | **Custom combat layer** (weapon swings with timing windows, weapon-shaped hitboxes, Terraria-style classes with distinct roles) | **Hard** | Replaces the vanilla click-to-hit loop. Needs player animations in first *and* third person, server-side hit detection over an arc or volume during active frames, hit-stop and knockback rules, combo state, and network sync of swing state; it must compose with reforge modifiers (attack speed changes timing) and with every weapon class. First-person arm animation is the notoriously fiddly part. The reference mods are ARR (Better Combat) or GPL (Epic Fight), so this is a clean-room build. Decision of 4 Sep 2026: in scope, basic version inside the vertical slice. | Vanilla already provides sweep attacks, attack cooldowns, keyframe animation for models, and the 1.21.11 `attack_range` / `kinetic_weapon` / `piercing_weapon` / `swing_animation` item components. Plan: a combat-core mod with weapon archetypes as data (reach, arc, active frames, combo length, damage class), server-authoritative hit detection, and a player animation layer (our own, or PlayerAnimator (MIT) if it reaches 26.x). Classes are data too: damage class per weapon, class bonuses on armor sets, class-specific resources. |
| 3 | **Custom rendering** (weapon effects, animated blocks, dimension skies, HUD) | **Hard** | 26.1 rewrote the lightmap and chunk GPU memory model; 26.2 added an experimental Vulkan backend. Anything touching raw OpenGL or custom core shaders is now fragile across drops. Rendering code is also the least documented area of every loader. | Most item visuals are data-driven now: item model definitions (1.21.4+) with selectors/conditions, `equipment` assets for armor, mcmeta animated textures. Reserve Java renderers for a few "special" items and block entities, and go through Blaze3D's render-pipeline abstraction only. **No custom shaders in v1.** |
| 4 | **Guns and magic** (decided: both in-house, both deferred past the vertical slice) | **Hard** (as full subsystems) | Each is a whole subsystem: a resource (ammo, mana), projectiles with travel time and spread, reload or cast timing, first-person feedback, and above all balance. Magic in Minecraft mods is usually either dominant or useless, and the user has flagged both systems as major design efforts. | Reserve the design space now so nothing is rewritten later: damage classes and a mana attribute in the combat core, ammo as items/components, a projectile base class shared by bows, guns and spells, and class resources shown on the HUD. Reference designs in section 5.7 are read-only (ARR/GPL). |
| 5 | **Summoner minions** | **Medium** | Owned entities that follow the player, attack what the owner hits, respect a minion cap, despawn on unsummon and scale with gear. The AI is standard goal code; the risks are target selection and entity counts in multiplayer. | Vanilla tameable/owner patterns (wolves, allays) are readable reference code; the minion cap is an attribute; summons are entities with a data-driven stat block. |
| 6 | **Weapon reforging + modifier pools** | **Medium** | The mechanic is simple; the *system* is not: modifier tables per weapon class, rarity weighting, cost curves, tooltip rendering, and re-rolling UX all interact, and every weapon we add multiplies the balancing surface. | Item data components (1.20.5+) are the right home: a reforge writes a custom `reforge` component plus the vanilla `attribute_modifiers` component, so speed/damage/knockback changes need no Java at use-time. The NPC's shop can be data-driven trades (26.1 `villager_trade` / `trade_set`); only the reforge menu (`AbstractContainerMenu` + `Screen`) and the roll logic are Java. |
| 7 | **Dungeons** (hand-built jigsaw rooms, keys, boss rooms) | **Medium** | Jigsaw is powerful but fiddly: pool weights, terrain adaptation, `size` (max depth 20) and `max_distance_from_center` (max 128, or 116 with terrain adaptation) cap a single structure at roughly 256×256 blocks; loot and mob placement need processors or Java block entities. Runtime-procedural dungeons (rooms carved at generation by code) are **Hard** and not needed. | Templates come from structure blocks/Axiom; JSON template pools + processor lists + structure sets do placement; NeoForge structure modifiers and loot modifiers add hooks. Locked doors, key items and "boss room sealed until cleared" blocks are small Java classes. |
| 8 | **Custom dimensions** | **Medium-Low** | The definition is JSON (dimension type, noise settings, density functions, biomes, features). The Java parts are the portal/entry logic, dimension sky/fog effects, and any "return path" rules. Getting a *good-looking* terrain profile takes tuning time, not code. | Misode's generators produce dimension/noise JSON; 26.1 world clocks let a dimension keep its own time (eternal night, fast days); 1.21.11 environment attributes replace the old hard-coded dimension flags. Vanilla 1.21+ has a `Portal` block interface with teleport transitions (documented in the [NeoForge 1.21 primer](https://docs.neoforged.net/primer/docs/1.21/); verify against the unobfuscated 26.2 source in Milestone 0). |
| 9 | **Boss-gated progression flags** | **Medium-Low** | Needs a world-level state store, unlock triggers, and multiplayer rules (server-wide vs per-player unlocks, late joiners). The design decision is harder than the code. | `SavedData` for world flags, NeoForge data attachments for per-player state, advancements as the trigger surface (kill boss → grant advancement → flip flag), custom recipe/loot/spawn conditions read the flag. Sync to client for UI with a small payload. |
| 10 | **Accessories / trinket slots** | **Medium-Low** | Slot UI, equip/unequip effects, attribute application, and sync are known patterns but easy to get subtly wrong (duplication bugs). | Use Curios (NeoForge, 26.2 build available, LGPL) or write our own slot container on top of data attachments. Deferring this decision to the PRD. |
| 11 | **Enemy design and scaling** | **Medium-Low** | Vanilla goal AI covers melee/ranged/fleeing; difficulty comes from making enemies *feel* distinct and scaling per tier without HP sponges. | Data-driven spawn conditions per biome/tier (biome modifiers, spawn placements), attribute modifiers per tier, reuse of the boss "phase machine" for mini-bosses. |
| 12 | **Healer / support role** | **Medium-Low** | Healing and buffing allies needs friendly targeting (look target, area, or nearest ally), heal-over-time and shield effects, visible feedback, and a reason to exist in solo play. | `MobEffect`-based heals and buffs, area/ray targeting, party or team data in attachments; the class exists mainly for the multiplayer server this pack targets. |
| 13 | **Events / invasions** (optional) | **Medium** | Wave management, world-state triggers, and cleanup on server restart. | Same building blocks as bosses + world flags; timers via world clocks / time markers (26.1). |
| 14 | **Mounts** (optional) | **Medium** | Rideable entities need client input handling, saddle/inventory, and smooth movement sync. | Vanilla horse/camel/happy ghast code (readable now) is the reference; 26.2 physics attributes help. |
| 15 | **Potions / buffs** | **Low** | `MobEffect` subclasses + data-driven potion recipes/brewing hooks. | Straightforward Java; tooltips and icons are assets. |
| 16 | **Loot, rarity, economy** | **Low** | JSON loot tables, predicates, item modifiers; rarity is a vanilla item component (4 tiers) and can be extended with our own component for named colors. | Datagen writes the tables; JEI shows recipes to testers. |
| 17 | **Custom blocks and building sets** | **Low (high volume)** | Registration is trivial; the cost is *volume*: every wood/stone family means ~10–15 blocks × models × blockstates × loot × recipes × tags. | Datagen providers emit the JSON; a "block family" helper generates the whole set from one call. Budget artist time, not engineer time. |
| 18 | **NPC hub / town** | **Low-Medium** | Custom villager-like entities with data-driven trades (26.1) and simple schedules. Housing/town-growth rules are design scope, not risk. | Start with a static hub structure that NPCs teleport into when unlocked (the Terraria "NPC moves in" beat) rather than simulated towns. |
| 19 | **Fishing** (optional) | **Low** | Loot tables + a custom hook entity for special waters. | Vanilla fishing code is small and readable. |
| — | **Performance** (cross-cutting) | **Medium** | Server tick budget is 50 ms; custom density functions, structure density, particle-heavy bosses and entity counts are the usual culprits. | Profile early with Spark; cap concurrent boss/adds; keep custom worldgen close to vanilla noise; use `structure_set` spacing generously; test with 2–4 players on the real server from Milestone 1. |

### Straightforward on 26.x (do not over-plan these)
- Registering blocks, items, block entities, entities, effects, sounds, creative tabs, tags.
- Recipes, loot tables, advancements, tags, worldgen features, biomes, structure placement: all JSON, all generated by datagen.
- **Weapon *stats* without code**: since 1.21.11 the `attack_range`, `kinetic_weapon`, `piercing_weapon`, `swing_animation`, `use_effects` and `damage_type` item components cover reach, charge attacks and pierce as data. With the custom combat layer decided, these become the *inputs* to our swing/hitbox system rather than the whole system, but they still mean each new weapon is mostly a JSON file.
- Boss bars, custom particles (JSON particle definitions + small Java factory), custom damage types (JSON), enchantments (JSON, 1.21+, including a `run_function` hook for on-hit effects).
- Server-driven simple UI via dialogs (1.21.6+) for confirmations, quests, and NPC menus that do not need item slots.
- NPC merchants: data-driven trades (26.1) on a vanilla-style merchant entity.

Each hard part above has a matching experiment in the Milestone 0 verification checklist (appendix).

---

## 7. Open questions before Phase 2 (PRD)

Answers change the PRD materially; defaults are what the PRD will assume if a question is skipped. Status column updated 4 Sep 2026 after the first review.

| # | Question | Default if unanswered | Status |
|---|---|---|---|
| 1 | **Version pin.** Start Milestone 0 on 26.2 now, or wait for 26.3 (pre-release now, expected this month) plus a stable NeoForge build? | 26.2 now; first bump to 26.3 at the start of Milestone 1. | Discussed (see "Why 1.21.1 has the most mods"); awaiting confirmation. |
| 2 | **Loader.** NeoForge as recommended, or a strong preference for Fabric (for example, existing Fabric experience or friends' client mods)? | NeoForge. | Open, default applies. |
| 3 | **Library policy.** Accept GeckoLib (MIT, animation), Curios (LGPL-3, accessory slots) and JEI (recipe viewer for testers) as runtime dependencies? Or write our own accessory system from the start? | GeckoLib yes; Curios for the vertical slice with a decision point at MVP; JEI for playtesting only. | Open, default applies. |
| 4 | **Our licence and repo visibility.** MIT, LGPL, or all-rights-reserved for our mods; public or private GitHub? (Affects CI caching, how freely we read LGPL code, and whether Modrinth hosting is ever an option.) | Private repo now; MIT code, ARR assets, decided before any public release. | Open, default applies. |
| 5 | **Combat feel.** Vanilla combat model (cooldowns, sweep) extended with the 1.21.11 data-driven weapon components, or a custom combat layer (swing arcs, combos, stamina)? This is the single biggest scope lever after boss count. | Vanilla model + data-driven components for v1. | **Answered: custom "vanilla expanded" combat.** Real swings with timing and weapon-shaped hitboxes; large weapon variety; Terraria-style classes (melee, ranged, mage, summoner, healer, and more) that each play differently and have a distinct purpose. |
| 6 | **Art pipeline.** Who makes models, textures, animations and sounds? Are both of you comfortable in Blockbench? Is a vanilla-faithful 16px style acceptable to keep art cost down? | Vanilla-faithful 16px; Blockbench + GeckoLib for entities; art time budgeted per milestone. | **Answered: vanilla-faithful art is fine.** Who does the art is still open. |
| 7 | **Server and players.** Where does the private server run (own machine vs paid host), how many concurrent players (2–4?), and will non-developer friends play? (Affects difficulty scaling, late-joiner rules and hosting choices.) | Self-hosted, 2–4 players, friends join at MVP. | Open, default applies. |
| 8 | **Prior modding experience.** Which loader and version does "some experience" refer to? (Sets the pace of Milestone 0.) | Assume no NeoForge-specific experience. | **Answered:** modding experience is from other games; Java is the daily language. Milestone 0 assumes no Minecraft-loader-specific experience but no Java ramp-up. |
| 9 | **Machines.** Are both developers on hardware that comfortably runs Java 25, IntelliJ, a client and a dev server at once (16 GB RAM is the practical floor)? | Yes. | Open, default applies. |
| 10 | **Guns and magic** (new). Both wanted, both built in-house with existing mods as reference only, both flagged as major design efforts because magic in Minecraft mods tends to be either overpowered or useless. | — | **Answered: not in the proof of concept.** The PRD reserves their design space (damage classes, mana, ammo, shared projectile base) and carries a standing footnote that both systems need a dedicated design pass before MVP. |

## Appendix: verification checklist for Milestone 0

Cheap experiments that close the unverified items above and de-risk the hard parts before any design commits to them:

1. Generate the NeoForge MDK for the pinned version; confirm Java 25, MDG 2.0.x, configuration cache and the `gameTestServer` run all work on both machines.
2. Confirm the vanilla `Portal` block interface and teleport transition API in the unobfuscated source; teleport a player into a JSON-defined dimension with its own world clock.
3. GeckoLib 5.5.x: animate one Blockbench model, trigger an animation from the server, confirm it plays for two connected clients.
4. Item model definitions + a custom `reforge` data component: prefix in the item name, tooltip line, glow when reforged.
5. Jigsaw structure with a processor list and a locked-door block spawning at a configured rarity inside the custom dimension.
6. One data-driven `test_instance` running headless under `runGameTestServer` in GitHub Actions (with `cache-provider: basic`).
7. JBR 25 with `-XX:+AllowEnhancedClassRedefinition` hot-swapping an added method without a restart.
8. packwiz pack served from GitHub Pages, installed by Prism on a client and by the start script on the server.
9. **Combat spike:** one keyframed swing on the vanilla player model in both first and third person, driven by a server-side swing state, plus an arc hit test that damages every mob inside the weapon's arc during active frames. Check whether PlayerAnimator has reached 26.x; if not, the spike decides whether we write our own animation layer.
10. A projectile base entity that a bow, a gun and a spell could all extend (travel time, spread, damage class), to confirm the shared design before guns and magic are designed.
