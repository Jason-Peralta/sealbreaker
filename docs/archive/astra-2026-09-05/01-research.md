# Phase 1 — Technical research and recommendations

**Project:** Original high-fantasy Minecraft: Java Edition modpack  
**Research date:** 2026-09-04, UTC  
**Status:** Proposed baseline; awaiting team review before Phase 2  
**Scope:** Research only. No mod code, project scaffolding, PRD, or implementation commitments.

## 1. Team context and recommendation

The supplied screenshot answers all six starting-context questions.

| Input | Confirmed context |
|---|---|
| Team | Two developers |
| Java | Both describe their experience as strong |
| Minecraft modding | One has some experience; the other has none |
| Play target | Small private server; possibly a hosting service later |
| Dependencies | Library/API and utility mods allowed when proportionate |
| Horizon | Hobby project with no deadline |
| Ownership | All gameplay/content mods, original code, names, lore, and assets authored by the team |

Paid hosting does not imply a public server. Concurrent player count, weekly availability, and art/audio capacity remain open.

**Recommendation: start the first playable build on Minecraft 26.1.2, NeoForge, Java 25, and the official ModDevGradle template. Use one loader and Minecraft's official unobfuscated names. Freeze the Minecraft target through the vertical slice.**

This is a recommendation for this team's new codebase, not a claim that 26.1.2 has the largest mod catalog or guaranteed long-term support. Minecraft 1.21.1 is the stronger established modern content-mod ecosystem; that advantage matters less when the team supplies all gameplay content. The evidence and qualifications follow.

### Decision register

Confidence means confidence in the recommendation for this team: **High** = strong evidence and limited dependence on unknown preferences; **Medium** = credible choice with meaningful uncertainty; **Low** = insufficient evidence to settle.

| Decision | Single recommendation | Confidence | Why not the alternatives? |
|---|---|---|---|
| Established ecosystem benchmark | Treat 1.21.1 as the leading established modern content ecosystem; do not assert a raw-count winner | Medium | 1.20.1 has substantial legacy breadth, but available catalogs do not establish a clean cross-platform ranking |
| New project target | Minecraft 26.1.2 | Medium | 1.21.1 starts before the unobfuscation transition; 26.2 adds newer churn; 26.3 is still a prerelease |
| Loader | NeoForge only | High | Fabric is capable but offers less advantage for this content-heavy, single-loader project; Forge and Quilt offer no decisive benefit here |
| Build | Official 26.1.2 ModDevGradle MDK and its Gradle wrapper | High | A custom build or alternative plugin adds setup work without advancing the gameplay experiment |
| Java and IDE | Microsoft Build of OpenJDK 25 and IntelliJ IDEA | High | Other compatible JDKs/IDEs can work, but this follows NeoForge's documented path |
| Names/mappings | Official unobfuscated names; no mapping overlay initially | High | Yarn is no longer maintained for new unobfuscated releases; Parchment is unnecessary for basic name recovery |
| Multiple loaders | Defer; keep gameplay logic separable without adding a portability framework | High | Architectury or a multi-loader template still creates additional adapters, builds, and tests |
| Configuration | NeoForge's built-in configuration system | High | An extra configuration library duplicates functionality before a concrete need exists |
| Authoring | Java for behavior; data/assets for definitions and variation; generate repetitive files | High | All-Java slows content iteration; all-datapack implementation constrains novel mechanics |
| First dungeon | One hand-built structure; bounded modular generation later | High | A general procedural dungeon engine creates substantial risk before the combat loop is proven |
| First dimension | Defer a full new dimension until after the vertical slice | High | More terrain and biomes do not validate whether boss rewards and reforging are enjoyable |
| Verification | JUnit + GameTests + real dedicated-server playtests | High | No single test layer covers pure logic, world behavior, networking, and visual feedback |
| CI | GitHub Actions, if creating a new repository | High | Another CI provider is reasonable if already used; a new self-managed runner is unnecessary initially |
| Packaging and release | Versioned private builds first; packwiz when packaging starts; Modrinth first for public distribution | Medium | CurseForge can follow as a mirror; a custom launcher and hosting stack add maintenance |

These are proposed choices, not approvals. Gameplay policy questions are collected in Section 8.

## 2. Target version and ecosystem evidence

### What changed recently?

| Date | Verified change | Implication |
|---|---|---|
| 2024-09-08 | Mojang announced more frequent game drops instead of relying on one annual major update | Expect more potential porting boundaries; do not follow every drop automatically |
| 2025-10-29 | Mojang announced removal of Java Edition obfuscation | Readable game names simplify development, but do not make Minecraft open source |
| 2025-12-02 | Mojang announced year-based version numbering beginning in 2026 | The old expectation of a future “1.22” is obsolete; 26 denotes 2026 |
| 2026-03-24 | NeoForge published its 26.1 release announcement, covering unobfuscated development and Java 25 | This is a real tooling transition, not merely a version-label change |
| 2026-04-09 | Minecraft 26.1.2 released | A concrete released target with an official NeoForge MDK |
| 2026-06-16 | Minecraft 26.2 released | Newer than the recommendation, but “newest” is not the selection criterion |
| 2026-09-01 | Minecraft 26.3 Pre-Release 1 published | Do not build the initial baseline against this prerelease |

Sources: [Mojang's development cadence][cadence], [numbering announcement][numbering], [unobfuscation announcement][unobfuscation], [NeoForge 26.1 announcement][neo261], and Mojang's [26.1.2][mc2612], [26.2][mc262], and [26.3 prerelease][mc263pre] release notes.

Year/drop/patch numbering is not a promise of mod binary compatibility. Unobfuscation removes a naming/remapping obstacle; Minecraft APIs, rendering, serialization, and loader hooks can still change. Old mods do not automatically become compatible. Fabric's 26.1 migration notes describe the corresponding build changes. [Fabric, 2026-03-14][fabric261]

### “Most existing mods” and “best new-project base” are different questions

| Candidate | Catalog/community evidence | Tooling and names | Assessment for this project |
|---|---|---|---|
| 1.20.1 | Large, long-established catalog, including legacy Forge content | Mature older tooling; predates unobfuscation | Attractive when existing mods dictate the pack; weak reason to choose it for an entirely new content suite |
| 1.21.1 | NeoForge reported over 16,000 mods and called it its most popular version in its January 2026 retrospective; major content projects still publish for it | Established documentation, examples, and Mojang-mapped development | Best established modern ecosystem benchmark; credible conservative alternative |
| 26.1.2 | Official NeoForge MDK and relevant library files verified; smaller accumulated history than 1.21.1 | Java 25, unobfuscated names, current NeoForge documentation line | Recommended balance for this new project |
| 26.2 | Released; current Fabric documentation and active Forge/NeoForge tooling exist | Newer changes, including work around rendering backends | Viable, but offers no identified requirement that justifies adopting the newer boundary immediately |
| 26.3 prerelease | Prerelease as of this research date | Moving target | Exclude from initial setup |

The 1.21.1 assessment is an **inference from several indicators**, not a verified universal leaderboard. NeoForge's retrospective is primary but self-reported: its mod total is dated, not independently deduplicated across hosts. Twilight Forest and Aether provide concrete examples of substantial content still supporting 1.21.1. [NeoForge, 2026-01-01][neo2025]; [Twilight Forest][twilight]; [Aether][aether]

**Mod-count limitation:** the inspected CurseForge version-filtered listings for 1.20.1, 1.21.1, and 26.1.2 all displayed a capped “10,000+” result count. Project cards can also show a project's latest file independently of the selected version. Modrinth's corresponding catalog/API data could not be retrieved in this session. Consequently, an exact count ranking would be unsupported. Historical versions such as 1.12.2 also have substantial catalogs; this research does not establish an all-time winner. [CurseForge: 1.20.1][cf1201], [1.21.1][cf1211], [26.1.2][cf2612], accessed 2026-09-04.

For loader activity and community, NeoForge reported 633 merged pull requests in 2025 and more than 240,000 Discord messages over a 120-day interval. These indicate activity, not unique developers or superiority over other communities. Fabric's dated 26.1/26.2 migration guides, Forge's 26.2 downloads, and Quilt's 2026 direction statement establish ongoing work elsewhere. There is no comparable, verified active-user dataset here. [NeoForge retrospective][neo2025]; [Fabric 26.2][fabric262]; [Forge downloads][forge-downloads]; [Quilt direction][quilt2026]

### Why recommend 26.1.2?

1. The team is starting from zero and is not waiting for a large third-party gameplay catalog.
2. Starting after unobfuscation avoids building the first systems on the earlier naming/tooling model.
3. An official 26.1.2 template, current NeoForge documentation, and relevant animation/accessory library releases already exist.
4. The newer 26.2 release introduces additional changes without a demonstrated benefit to the proposed first slice.

A NeoForge maintainer suggested that 26.1 might become the next stable modding hub, but explicitly described that as a personal educated guess. It is supporting context, **not an official support commitment**. [NeoForge, 2026-03-24][neo261]

**Version policy recommendation — Medium confidence:** hold Minecraft 26.1.2 through the vertical slice; reassess at the MVP planning boundary. Apply necessary compatible fixes deliberately. Require a separate migration branch, copied-save testing, and a full client/server validation before changing the Minecraft target. Recheck maintained builds before actual environment setup; do not treat today's observed pins as permanently current.

## 3. Loader comparison

All four can participate in modded Java Edition development. The choice is about maintenance and API fit, not whether Fabric can support “large” mods or whether a loader name determines performance.

| Loader | API surface and fit | Update status/speed evidence | Documentation | Principal loader/API license |
|---|---|---|---|---|
| **NeoForge** | Broad events, registries, networking, saved data, configuration, data generation, and content hooks; strong fit for interconnected gameplay systems | Active 26.x development; retrospective reports rapid initial ports, which must not be confused with immediate production stability | Comprehensive, version-selectable official guides; current main docs inspected identify 26.1 | LGPL-2.1 |
| **Fabric** | Small loader plus modular Fabric API; entirely capable of bosses, dimensions, and a full content suite; unsupported hooks may require mixins | Official 26.1 and 26.2 migration guidance shows active adaptation to releases | Substantial modern official guides, including entities, networking, persistence, rendering, and testing; current default inspected is 26.2 | Loader and Fabric API: Apache-2.0 |
| **Forge** | Broad traditional content APIs, events, registries, and ForgeGradle ecosystem | Still active: official 26.2 downloads and repository branch exist; “legacy Forge” should not be read as “abandoned” | Extensive official docs and historical tutorials; examples must be checked against the exact version | LGPL-2.1-only, per current license header |
| **Quilt** | Quilt Loader continues, but its independent API strategy has narrowed | February 2026 announcement retires QSL, QFAPI, Quilt Kotlin Libraries, and Quilt Mappings for 26.1 onward; older branches can remain maintained | Existing materials span older API generations; verify each guide against the post-transition direction | Quilt Loader: Apache-2.0 |

Sources: [NeoForge documentation][neo-start], [repository/license][neo-repo]; [Fabric documentation][fabric-docs], [loader][fabric-loader], [API][fabric-api], [26.2 update notes][fabric262]; [Forge documentation][forge-docs], [downloads][forge-downloads], [license header][forge-license]; [Quilt announcement][quilt2026], [loader/license][quilt-loader]. Living pages accessed 2026-09-04.

**Choose NeoForge — High confidence.** The team controls the full content suite, so one comprehensive API and one dedicated-server target are more valuable than portability. Prefer public loader hooks; use mixins only for a demonstrated gap and document the vanilla behavior being changed.

**Why not Fabric?** It is a sound second choice, especially for a team already fluent in it, but neither developer reports that specialization and rapid ports are not the primary goal. **Why not Forge?** No inherited dependency requires its ecosystem. **Why not Quilt?** Its retired independent API layers offer no compelling benefit over choosing Fabric directly for a new 26.x project.

### Multi-loader options

| Option | What it actually provides | Remaining cost |
|---|---|---|
| Architectury API and associated tooling | A runtime abstraction for selected cross-loader functionality, plus build tooling | Platform-specific code still exists; separate loader artifacts, dependencies, and validation remain |
| Jared's MultiLoader-Template | Common, Fabric, and NeoForge projects without requiring a third-party runtime abstraction | Common code cannot freely use loader-specific APIs; adapters and multiple test targets remain |
| Single-loader modular code | Pure Java rules separated from Minecraft/NeoForge integration | A future port still requires work, but no portability tax is paid before demand exists |

Architectury currently lists 26.1/26.2 support; it is not simply obsolete. The inspected MultiLoader-Template has a 26.2 branch. These are available alternatives, not requirements. [Architectury][architectury]; [MultiLoader-Template][multiloader]

**Recommendation — High confidence:** single-loader now. Reconsider only if a concrete external audience needs another loader. Splitting the team's own content into several mods is a separate architecture decision for Phase 2.

Licenses above describe the upstream projects, not blanket permission to copy code into this project. Dependency notices and obligations must be checked for the exact artifacts distributed; code and assets may carry different terms. See Section 6.

## 4. Toolchain and iteration

### Verified baseline

| Component | Recommendation or observed value | Evidence/status |
|---|---|---|
| Minecraft | 26.1.2 | Released 2026-04-09 |
| NeoForge | 26.1.2.100 | Value observed in the official 26.1.2 MDK's gradle.properties |
| Build plugin | net.neoforged.moddev, version 2.0.146 | Value observed in that MDK's build.gradle |
| Gradle | Use the MDK's committed wrapper | NeoForge's 26.1 announcement requires at least 9.1.0; exact current wrapper distribution was not independently retrieved |
| Java | 64-bit Microsoft Build of OpenJDK 25 | Official NeoForge getting-started recommendation |
| IDE | IntelliJ IDEA, with Java 25 support | Configure both project SDK and Gradle JVM to 25 |
| Names | Official unobfuscated Minecraft names | No initial Yarn/Parchment layer |
| Modeling | Blockbench; retain editable source models and textures | Official model/texture/animation authoring tool |

Sources: [official MDK][mdk], [MDK properties][mdk-properties], [MDK build script][mdk-build], [NeoForge setup][neo-start], [26.1 release requirements][neo261], [Blockbench][blockbench]. MDK values observed 2026-09-04.

**Verification boundary:** these are repository observations, not a tested build. No template was scaffolded, compiled, or launched in this session. Milestone 0 must capture the actual wrapper/JDK/loader versions and prove that the same pinned project loads on both machines and a dedicated server. Do not replace the wrapper with whichever system Gradle happens to be installed.

NeoForge also offers NeoGradle; use ModDevGradle because it is the documented, official template path selected here. For comparison, Fabric's unobfuscated-generation plugin is **net.fabricmc.fabric-loom**; older remapping plugin instructions are not interchangeable. Fabric's June 2026 guide listed Loom 1.17, Gradle 9.5.1, and Fabric Loader 0.19.3 at publication. Those numbers explain the current tooling generation, not a verified latest-patch recommendation today. [Fabric 26.1][fabric261]; [Fabric 26.2][fabric262]

### Mapping terminology

| Term | Earlier-version role | Relevance to this baseline |
|---|---|---|
| Mojmap | Mojang's official mappings for obfuscated releases | Use the official names directly on unobfuscated 26.1.2 |
| Yarn | Community names commonly used with Fabric on earlier versions | Fabric states that official Yarn support does not continue for new unobfuscated versions |
| Parchment | Additional parameter names and documentation layered onto official mappings | Name recovery is no longer needed; supplementary documentation may still be useful if a compatible artifact exists |

**Recommendation — High confidence:** do not add a mappings dependency to recover names already present. Consult version-matched sources when translating older tutorials. Removing obfuscation does not remove Minecraft's license or the need to port changed APIs. [Mojang][unobfuscation]; [Fabric migration][fabric261]; [NeoForge migration][neo261]

### Daily feedback loop

| Change | Fastest appropriate feedback | Limit |
|---|---|---|
| Textures, ordinary model JSON, language entries | Resource reload, normally F3+T | Custom renderer classes still require code reload/restart |
| Recipes, loot, tags, reloadable balance definitions | Server data reload with /reload | Not all registries or worldgen data are reloadable during play |
| Repetitive assets and data | Run the MDK's data-generation configuration, then reload or restart as appropriate | Inspect generated diffs; do not hand-edit generated output |
| Compatible Java method-body edits | IDE debugging and HotSwap | Standard HotSwap does not generally add fields/methods or change class structure |
| Registration, mixins, class shape, startup logic | Restart client/server | Do not promise instantaneous hot reload for structural changes |
| Dimension/worldgen definitions | Reload the world/server as required; test new chunks or a fresh disposable world | Existing generated terrain does not rebuild itself |

Sources: [NeoForge resource model][resources], [registries][registries], [data maps][datamaps], and [JetBrains HotSwap guidance, 2024-11-12][hotswap].

The inspected MDK names its data run **data** while configuring it with **clientData()**. Use its generated run configuration or inspect Gradle tasks instead of assuming that a command copied from a different template has the same name. [MDK build script][mdk-build]

**Recommendation — High confidence:** start with standard debugging, data/resource reloads, and normal restarts. Defer enhanced JVM hot-reload tools until restart time is a measured bottleneck.

### Testing and CI

| Layer | What to verify | What it cannot establish |
|---|---|---|
| JUnit Jupiter | Modifier eligibility/weights, cost calculations, progression rules, serialization round trips, boundary cases | Real Minecraft lifecycle, rendering, and network behavior |
| NeoForge GameTests | Block/inventory interactions, boss unlock triggers, reward behavior, structures, world-facing invariants | Actual multi-client latency, perceived fairness, or frame time |
| Dedicated server with at least two clients | Simultaneous reforging, duplicate requests, reconnects, late joins, death, boss completion, chunk unloading, save/restart | Large-server capacity without an agreed workload |
| Manual playtests and profiling | Telegraph readability, progression pace, dungeon navigation, frame/tick cost during combat and exploration | Long-term balance from a single successful session |

ModDevGradle supports a unit-test configuration. NeoForge's current GameTest documentation uses **/test**, not older tutorials' **/gametest** commands. Its **runGameTestServer** Gradle task can run tests headlessly and report required failures through its exit status. [ModDevGradle][mdg]; [NeoForge GameTests][gametest]

**CI recommendation — High confidence:** GitHub Actions should run the committed Gradle wrapper with JDK 25, compile/build, meaningful unit tests, data-generation consistency checks, and headless GameTests. Preserve failed-test logs and build artifacts. Validate a release JAR on a dedicated server before giving it to friends. Pin action revisions and dependency versions during implementation, not from an old tutorial. [GitHub's Gradle CI guide][github-ci]

Pure unit tests should not require booting Minecraft. GameTests should target gameplay risks rather than merely proving that a block was registered. Visual and multiplayer playtests remain release gates.

### Keep dependencies proportionate

| Dependency | Recommendation | Verified 26.1.2 evidence | License/qualification |
|---|---|---|---|
| NeoForge configuration | Use immediately when configuration is needed | Built-in server/client/common configuration support | Server config is synced; common config is not automatically synced |
| GeckoLib | Add when the first boss needs skeletal animation beyond a simple vanilla model | NeoForge 5.5.2 release, 2026-06-27 | MIT; team's models, animations, and gameplay remain original |
| Curios | Adopt when accessory slots enter scope | 15.0.0+26.1.2 release, 2026-07-19 | LGPL-3.0; includes two creative-only example items, so review the literal content boundary before shipping |
| JEI | Optional development utility; decide player inclusion later | NeoForge 29.35.0.94, 2026-09-03, marked beta | MIT; do not make the first gameplay loop depend on this beta utility |
| Architectury/config UI libraries | Do not add initially | Available, but no current requirement | Extra dependency surface without a demonstrated benefit |

Sources: [NeoForge configuration][config], [GeckoLib release listing][geckolib], [Curios release listing and description][curios], [JEI release listing][jei]. Compatibility means a matching file is published, not that this proposed combination has been integration-tested.

**Recommendation — High confidence:** begin with NeoForge itself; add each library at the milestone where it eliminates concrete work. No third-party gameplay pack, scripting content framework, or borrowed assets are required.

### Distribution and later server hosting

| Channel/tool | Suitable use | Constraints |
|---|---|---|
| Private versioned release | First friend-group playtests: owned mod JARs, configuration, dependency list, and installation notes | Client/server versions must match; retain backups and the previous release |
| packwiz | Track a pack manifest in Git and produce repeatable client/server distributions when packaging begins | Packaging tool, not a runtime gameplay mod or a custom launcher |
| Modrinth | First public mod and pack channel; .mrpack carries an index, file hashes, downloads, environment information, and overrides | Review distribution rights and actual client/server dependency requirements |
| CurseForge | Later public mirror to reach its audience | Uses its own manifest/overrides workflow; externally bundled mods must satisfy its approval rules |
| Self-hosted HTTPS | Controlled private distribution when needed | The team owns availability, access, hashes, and update instructions |

**Recommendation — Medium confidence:** private builds first, then packwiz-managed packaging and Modrinth as the primary public channel. Add CurseForge after the release process is stable. Publishing the team's individual mods on CurseForge before mirroring its pack avoids treating arbitrary bundled JARs as automatically approved. [packwiz][packwiz], [packwiz CurseForge export][packwiz-cf], [Modrinth format, 2025-10-15][mrpack], [CurseForge submission, 2026-02-03][cf-submit]

Distribution hosting and game-server hosting are different decisions. When needed, select a host that allows the chosen NeoForge server, Java 25, custom JARs, backups, and log access. Player count, budget, and measured CPU/RAM demand are missing, so choosing a provider or memory allocation now would be premature. Distribute the team's mods and permitted dependency artifacts, not a repackaged Minecraft game binary. [Minecraft EULA][eula]

## 5. Content authoring: data versus Java

“Data-driven” means choosing behavior from an existing implementation. JSON does not remove the need to implement a new behavior type, its validation, or its network synchronization.

| Content/system | Data/assets can supply | Java is needed for | Practical iteration |
|---|---|---|---|
| Recipes | Ingredients, results, categories, tags, parameters for registered recipe types | A new recipe type/serializer, novel station behavior, runtime progression checks | Reload recipes; restart for new registered code |
| Loot | Tables, pools, weights, conditions/functions already supported by the engine | New loot behavior and custom conditions/functions | Reload definitions; test distributions and progression guarantees |
| Tags and advancements | Membership, advancement criteria using existing triggers, rewards, presentation | New criterion types; authoritative unlock rules beyond existing behavior | Fast data iteration; an advancement is not a complete world-state system |
| Ordinary blocks/items | Models, blockstates, textures, item appearance, names, recipes, loot | Registration of new types and their behavior | Register a stable family once; generate repetitive assets/data |
| Enchantments and effects | Enchantments composed from supported effect types; balance values | Novel effect types or mechanics | Reuse supported effects before inventing an effect framework |
| World generation | Biomes, configured/placed features, noise settings, dimension types, dimensions, biome modifiers | New generator/feature algorithms or behavior absent from existing codecs | Test at world load and in fresh chunks; keep deterministic seeds |
| Dungeons/structures | Hand-built structure **NBT**, JSON template pools, processors, placements and structure definitions | Bespoke generation algorithms, encounter logic, keys, arena rules, custom placement constraints | Start with one fixed structure; introduce bounded modular assembly later |
| NPCs | Models/textures, supported dialogue/trade data; vanilla trades are data-driven in 26.1 | New entity/AI, schedules, services, custom interaction and reforging UI | Separate cheap offer changes from expensive behavior changes |
| Reforging | Eligible-item tags, modifier pools, rarity weights, costs, localization after a schema exists | Roll execution, persistence, attribute effects, server transaction, validation, UI/payloads | Reload a validated definition snapshot; persist the selected result on the item |
| Boss fights | Loot and parameters exposed by the team's code | Entity type, server AI, damage rules, phase transitions, synchronization, custom rendering | Tune parameters quickly; code changes still need normal development |
| Sounds and visuals | Original sound files, sound definitions, textures, ordinary models | Novel renderers, particle behavior, animation integration | Resource reload helps assets; visual code is a separate layer |

Sources: NeoForge [resources][resources], [registries][registries], [recipes][recipes], [tags][tags], [enchantments][enchantments], [biome modifiers][biomemodifiers], [data maps][datamaps], [item data components][components], and Fabric's [26.1 notes on data-driven villager trades][fabric261]. The distinction between NBT structures and JSON worldgen configuration is also documented in Mojang's [1.16.2 technical release notes][jigsaw-history]; that historical source is not a current schema reference.

### Boundaries that affect this design

**Static registrations versus reloadable definitions.** New block, item, and entity types require registration during startup. Data-pack registries such as worldgen registries load with the world; do not assume that /reload can add or replace everything safely during play. NeoForge data maps are explicitly reloadable and can associate configuration with registered entries. A custom data registry still needs a Java codec/loader before designers can author entries. [Registries][registries]; [data maps][datamaps]

**Progression must be enforced where an action occurs.** Hiding a recipe, granting an advancement, or using a data-load condition does not by itself make a dynamic boss gate authoritative. A player may obtain an item from another player or an alternate path. The selected gate policy must be checked by the relevant server-side crafting, use, equipment, or portal action. Requiring a boss-earned ingredient can make some gates simpler, provided sharing that ingredient matches the design. Data-load conditions are not live per-player progression checks. [Load conditions][conditions]; [recipes][recipes]

**Persist world facts separately from player presentation.** NeoForge SavedData can store persistent world data. A proposed implementation is a server-owned, namespaced progression record anchored to the Overworld, with an explicit schema version and dirty/save handling. Individual participation or catch-up information can be stored separately. Whether the unlock itself belongs to the world, a party, or a player is a Phase 2 decision, not settled by the storage API. [SavedData][saveddata]

**Reforging is a server transaction.** Store the selected modifier and its necessary values in typed item-stack data components. Treat the client as requesting an operation: the server validates the current menu, item, eligibility, interaction range, and cost, then performs the debit and item change together. Reject stale/duplicate requests. Distinguish an in-memory atomic change from crash-safe persistence and test save/restart behavior explicitly. These are proposed engineering safeguards, informed by NeoForge's [components][components] and [payload][payloads] APIs.

**Existing saves constrain worldgen changes.** Adding a structure to generation does not place it retroactively in explored chunks. A boss gate that depends on a newly added dungeon therefore needs an explicit access plan: unexplored territory, a deliberate placement mechanism, or a documented new-world requirement. Do not casually run a whole-world retrofit.

**Recommendation — High confidence:** use data generation for repetitive vanilla-format content, hand-authored data for carefully tuned content, and a small number of validated custom schemas for progression/loot modifiers. Keep one source of truth for each generated file. Publish which definitions support /reload and which require a restart or new world. Do not build a generic scripting engine for the first slice.

Vanilla datapack functions can prototype rules using existing entities and commands. They are useful experiments, but the proposed novel entities, interfaces, and durable multiplayer mechanics justify Java as the primary gameplay implementation.

## 6. Reference points: study behavior, create original content

These projects are design references only. None is proposed as a gameplay dependency. Observed behavior comes from project-owned pages; the transfer ideas are original recommendations with **Medium confidence** until playtested.

| Reference | Relevant observed design | Transfer to this project | Avoid importing |
|---|---|---|---|
| Twilight Forest | A themed dimension with distinct dungeons, bosses, rewards, and ordered progression | Make each boss unlock a concrete new activity; communicate why an area is currently dangerous or inaccessible | Its boss roster, lore, layouts, assets, exact sequence, or blanket terrain protection as an unexamined solution |
| The Aether | A dimension with its own resources, dungeon tiers, equipment, and a recognizable entry/return relationship; falling can return players to the Overworld | Design arrival safety, recovery, and the return path alongside the dimension's rewards | Its portal construction, visual identity, dungeon keys, and item identities |
| YUNG's Better Dungeons | Distinct dungeon families with different spatial identities; configurable/data-adjustable content | Build a small vocabulary of readable room roles and biome-specific encounters | A claim that we need its internal generator; this survey does not establish its implementation |
| Apotheosis | Affix rarities, salvage materials, reforging, sockets, and world-tier-related loot progression | Give unwanted loot a useful destination and make upgrading an understandable investment | All interacting layers at once; start with one modifier system and bounded outcomes |
| Curios | Accessory-slot infrastructure separated from the actual accessory effects supplied by content mods | Separate slot persistence/equipment rules from our original item behavior | A new inventory framework before the slot library has been evaluated |
| SevTech: Ages | Historical progression pack using advancements and staged visibility/unlocks; player progression can be synchronized through a team system | Decide world-versus-player progression and late-join rules before writing gates | Its progression tree, content dependencies, or old technical stack |

Sources: [Twilight Forest project][twilight] and [official progression wiki][twilight-progression]; [Aether project][aether]; [YUNG's Better Dungeons][yung]; [Apotheosis][apotheosis]; [Curios][curios]; [SevTech: Ages][sevtech]. SevTech is a historical 1.12.2 reference, with its listed release dating to 2021; it is not evidence for a current loader choice. The Twilight progression wiki was available in search indexing but its full page did not load; only the broad ordered-progression concept is used here.

### Minecraft-native lessons

- **Gate rewards and meaningful actions, not every route through terrain.** Mining, towering, flight, and item sharing make a two-dimensional game's barriers unsuitable as a direct template. Decide which bypasses are creative play and which invalidate progression.
- **Make the boss change the player's choices.** A new crafting capability, exploration tool, or hazard counter is more meaningful than a small numerical damage increase.
- **Make gear improvement legible.** Show eligibility, cost, possible outcomes, and the resulting change. Do not assume that repeated opaque rerolls create lasting depth.
- **Treat a dimension as a complete expedition.** Entry, navigation, rewards, death recovery, and a return route are part of the same feature.
- **Separate enemy challenge from resource denial.** Multiplayer participation and repeat summons need deliberate rules so late players can experience the encounter.

These are recommendations to explore in the PRD, not a copied content checklist or finalized player journey.

### Licensing and originality

**Recommendation — High confidence:** study observable systems and documented behavior, then write original implementations and assets. Keep a short source/provenance note when a design reference informs a decision. Do not copy even permissively licensed gameplay code, because this project's ownership rule is stricter than what a license might permit.

Public source availability does not grant unrestricted reuse. GitHub explains that an unlicensed repository retains default copyright restrictions. Check licenses per file and asset type: Twilight Forest has separate code and asset terms, with restrictions covering sounds and structure assets; the inspected Apotheosis distribution page labels its license All Rights Reserved despite linking public source. [GitHub licensing guidance][github-license]; [Twilight Forest repository/licenses][twilight-repo]; [Apotheosis listing][apotheosis]

The U.S. Copyright Office distinguishes game ideas and methods from protectable literary and pictorial expression. That is not a blanket clearance for names, branding, audiovisual presentation, or material governed by other rights. Use original names, lore, models, textures, sounds, animations, and dungeon builds. Unobfuscated Minecraft remains governed by its own terms. [U.S. Copyright Office][copyright-games]; [Minecraft EULA][eula]

For allowed library dependencies, record the exact artifact, version, source URL, license, and required notices. Do not infer that a project's code license also covers its textures, music, logos, or structure NBT files.

## 7. Hard parts and scope reality

The ranking below is an **engineering assessment**, not a measured benchmark. Ratings are relative to two strong Java developers with limited modding experience. Difficulty includes integration and failure cases, not merely getting a demo to render.

| Rank | Feature | Difficulty | Why it is difficult | Lower-risk first step |
|---|---|---|---|---|
| 1 | Performance across exploration and multiplayer combat | 5/5, cross-cutting | Chunk generation, AI, block/entity ticking, packet volume, allocation, and client rendering can compound | Profile the first dungeon/boss on a dedicated server; bound active entities, scans, particles, and encounter work |
| 2 | Novel custom dimension generation | 5/5 for a new algorithm; about 3/5 using existing generators | Terrain coherence, biome transitions, structures, generation cost, and save compatibility interact | Defer the full dimension; later use existing noise/biome systems before inventing a generator |
| 3 | Multiplayer-safe boss fights | 5/5 | Phase state, target changes, latency, deaths, disconnects, chunk unloading, restarts, duplicate rewards, and animation timing | One server-authoritative state machine; clients present telegraphs/animation; test two clients early |
| 4 | Procedural dungeon generation | 4–5/5 | Terrain fit, connected rooms, key ordering, chunk boundaries, reproducibility, and encounter pacing | One hand-built NBT dungeon; later a bounded set of modular rooms with seed-based validation |
| 5 | Custom weapon/block rendering and animation | 4/5 for specialized rendering; 1–2/5 for ordinary models | Renderer APIs change; animation, hitboxes, particles, and visual readability must agree | Ordinary JSON/Blockbench models; add an animation library for a demonstrated need |
| 6 | Reforging and equipment economy | 3–4/5 | The arithmetic is easy; inventory transactions, save data, UI feedback, rarity balance, and exploits are harder | One item class, a small modifier pool, and one clear cost source |
| 7 | Persistent progression and multiplayer access | 3/5 | Flags are easy; consistent gate enforcement, late joins, replay, and migrations are not | One world event and one enforced unlock; test restart and late join |
| 8 | Decorative blocks, basic weapons, recipes, tags, and loot | 1–2/5 technically | Mostly established APIs/data, but large quantities still require art, balance, localization, and QA | A small coordinated material set with data generation |

**Confidence:** High in identifying the risk areas; Medium in their relative order, which will change with the boss design and terrain ambition.

Performance is first because it constrains the other systems, not because it should become a separate optimization project before gameplay. Use actual server tick and client frame measurements; do not select a loader based on unsupported “always faster” claims. A dedicated server must work from the first gameplay feature: client-only classes and integrated-server assumptions can otherwise conceal failures. [NeoForge sides][sides]; [debug profiling][profiler]

Rendering deserves particular restraint. Fabric's 26.2 notes describe an experimental Vulkan backend alongside OpenGL and other rendering changes. Building directly against low-level graphics assumptions would increase future porting work. **Recommendation — High confidence:** stay within Minecraft/loader rendering abstractions for the slice. [Fabric, 2026-06-15][fabric262]

### What is straightforward?

Once setup and registries work, ordinary blocks/items, material variants, recipes, tags, loot tables, localized names, basic advancements, and effects built from existing behavior are relatively routine. A JSON-defined dimension using an existing generator is far easier than a novel terrain engine. Repeating content is still production work: fifty blocks require coherent assets and verification even if registration is generated.

### What should be cut or phased?

**The complete wishlist is unrealistic as a first release for two developers learning the modding ecosystem.** Strong Java experience reduces programming ramp-up; it does not supply finished art, combat design, dungeon building, balancing, or multiplayer QA. With no deadline, the full vision can remain a long-term direction, but it is not yet a defensible delivery commitment.

**Recommendation — High confidence:** prove one small dungeon, one boss, one enforced unlock, one reforging interaction, and a handful of original blocks/weapons before expanding. Use the Overworld for that loop. Defer multiple dimensions, procedural dungeon generalization, town simulation, invasions, mounts, elaborate sockets, and a large accessory catalog. Consider one complete dimension after the loop is fun and reliable; more dimensions should earn their place through distinct gameplay.

Do not estimate calendar delivery yet. Weekly hours, art coverage, concurrent players, and quality expectations are missing. Phase 3 should use effort ranges with explicit assumptions and revise them after the first slice.

## 8. Questions and Phase 1 review gate

The technical recommendations are concrete enough to review. These remaining choices materially affect the PRD; they have not been silently assumed.

| Question | Proposed direction for discussion | Why the answer matters |
|---|---|---|
| Approve Minecraft 26.1.2 + NeoForge + Java 25 + ModDevGradle? | Approve this baseline, subject to an environment-setup smoke test | Fixes the API and tooling vocabulary for subsequent design |
| What is the maximum expected concurrent player count, and is play cooperative PvE only? | A small cooperative private server; choose an actual cap | Determines encounter participation, scaling, test workloads, and economy abuse cases |
| Are boss unlocks shared by the world, by parties, or earned individually? | Shared world unlocks with personal participation records and an explicit late-join route | Changes storage, access checks, reward rules, and whether friends can progress separately |
| Should vanilla Nether/End/netherite/elytra progression remain available alongside the new path or become part of its gates? | Preserve vanilla progression during the slice; explicitly revisit bypasses in the PRD | Vanilla gear and travel can undermine a progression curve if ignored |
| Roughly how many hours per week can each person contribute, and who handles models, textures, animation, audio, and builds? | Keep initial art simple and original; choose owners before estimating | Required for credible scope and milestone effort |
| Are creative-only demonstration items in an otherwise useful API dependency acceptable? | Defer Curios until accessories are in scope, then inspect the shipped artifact | Clarifies the strict “all content is ours” boundary without adding a dependency now |

**Pause here.** Phase 2 begins only after the team gives its go-ahead and resolves the preferences that would change the design. Do not scaffold mod projects before the PRD has been reviewed.

## 9. Sources, dates, and verification notes

All sources were accessed on **2026-09-04**. Dates below are publication/release dates when verified. “Living” means no stable publication date is asserted. A repository's main branch or a project's release list can change; the observed values in Section 4 are a research snapshot, not a lockfile.

### Minecraft and ecosystem status

- [Mojang — The future of Minecraft's development][cadence] — 2024-09-08.
- [Mojang — New version numbering system][numbering] — 2025-12-02.
- [Mojang — Removing obfuscation in Java Edition][unobfuscation] — 2025-10-29.
- [Mojang — Java Edition 26.1.2][mc2612] — 2026-04-09.
- [Mojang — Java Edition 26.2][mc262] — 2026-06-16.
- [Mojang — 26.3 Pre-Release 1][mc263pre] — 2026-09-01.
- [NeoForge — 2025 retrospective][neo2025] — 2026-01-01; self-reported ecosystem/activity figures.
- [NeoForge — 26.1 release][neo261] — 2026-03-24; version requirements and explicitly qualified hub prediction.
- CurseForge filtered listings: [1.20.1][cf1201], [1.21.1][cf1211], [26.1.2][cf2612] — living; capped counts, not a reliable exact ranking.

### Loaders, builds, and development tools

- [Fabric — Modding for 26.1][fabric261] — 2026-03-14.
- [Fabric — Modding for 26.2][fabric262] — 2026-06-15.
- [Quilt — Non-obfuscated updates][quilt2026] — 2026-02-03.
- [NeoForge getting started][neo-start], [NeoForge repository/license][neo-repo] — living; documentation line inspected: 26.1.
- [Fabric developer documentation][fabric-docs], [Fabric Loader][fabric-loader], [Fabric API][fabric-api] — living; documentation default inspected: 26.2.
- [Forge downloads for 26.2][forge-downloads], [Forge documentation][forge-docs], [Forge 26.2 license header][forge-license] — living.
- [Quilt Loader repository/license][quilt-loader] — living.
- [Architectury API listing][architectury], [MultiLoader-Template][multiloader] — living; published/current branches include 26.x.
- [NeoForge 26.1.2 MDK][mdk], [gradle.properties][mdk-properties], [build.gradle][mdk-build] — living; baseline values transcribed on the access date.
- [ModDevGradle documentation][mdg] — living.
- [JetBrains — Altering execution / HotSwap][hotswap] — 2024-11-12.
- [Blockbench][blockbench] — living.
- [GitHub Actions — Java with Gradle][github-ci] — living.

### Minecraft/NeoForge implementation capabilities

- [Resources][resources], [registries][registries], [recipes][recipes], [tags][tags], [enchantments][enchantments], [load conditions][conditions] — living NeoForge 26.1 documentation.
- [Data maps][datamaps], [biome modifiers][biomemodifiers], [SavedData][saveddata], [item data components][components] — living NeoForge 26.1 documentation.
- [Networking payloads][payloads], [physical/logical sides][sides], [configuration][config], [GameTests][gametest], [debug profiler][profiler] — living NeoForge 26.1 documentation.
- [Mojang — Java Edition 1.16.2 technical notes][jigsaw-history] — 2020-08-11; historical confirmation of structure/template-pool concepts, not current JSON schemas.

### Libraries and design references

- [GeckoLib][geckolib] — living project/file list; cited 26.1.2 release: 2026-06-27.
- [Curios][curios] — living project/file list; cited 26.1.2 release: 2026-07-19.
- [JEI][jei] — living project/file list; cited 26.1.2 beta: 2026-09-03.
- [Twilight Forest][twilight], [official progression wiki][twilight-progression], [source/licenses][twilight-repo] — living; wiki full-page retrieval failed, indexed broad progression description only.
- [The Aether][aether], [YUNG's Better Dungeons][yung], [Apotheosis][apotheosis] — living project-owned distribution descriptions.
- [SevTech: Ages][sevtech] — historical pack description; listed release 2021-12-17.

### Distribution and rights

- [packwiz documentation][packwiz], [CurseForge export workflow][packwiz-cf] — living.
- [Modrinth modpack format][mrpack] — 2025-10-15.
- [CurseForge modpack export and submission][cf-submit] — updated 2026-02-03.
- [Minecraft EULA][eula], [GitHub repository licensing][github-license], [U.S. Copyright Office — Games][copyright-games] — living.

**Unresolved evidence:** exact cross-platform mod counts; comparable active-community sizes; exact MDK Gradle wrapper distribution; hands-on compatibility/performance of the proposed stack; future loader support duration. These limits are explicit and should not be converted into confident facts by a later session.

[cadence]: https://www.minecraft.net/en-us/article/the-future-of-minecrafts-development
[numbering]: https://www.minecraft.net/en-us/article/minecraft-new-version-numbering-system
[unobfuscation]: https://www.minecraft.net/en-us/article/removing-obfuscation-in-java-edition
[mc2612]: https://www.minecraft.net/en-us/article/minecraft-java-edition-26-1-2
[mc262]: https://www.minecraft.net/en-us/article/minecraft-java-edition-26-2
[mc263pre]: https://www.minecraft.net/en-us/article/minecraft-26-3-pre-release-1
[neo2025]: https://neoforged.net/news/2025-retrospection/
[neo261]: https://neoforged.net/news/26.1release/
[cf1201]: https://www.curseforge.com/minecraft/search?class=mc-mods&page=1&pageSize=20&sortBy=relevancy&version=1.20.1
[cf1211]: https://www.curseforge.com/minecraft/search?class=mc-mods&page=1&pageSize=20&sortBy=relevancy&version=1.21.1
[cf2612]: https://www.curseforge.com/minecraft/search?class=mc-mods&page=1&pageSize=20&sortBy=relevancy&version=26.1.2
[fabric261]: https://fabricmc.net/2026/03/14/261.html
[fabric262]: https://fabricmc.net/2026/06/15/262.html
[quilt2026]: https://quiltmc.org/en/blog/2026-02-03-non-obfuscated-updates/
[neo-start]: https://docs.neoforged.net/docs/gettingstarted/
[neo-repo]: https://github.com/neoforged/neoforge
[fabric-docs]: https://docs.fabricmc.net/develop/
[fabric-loader]: https://github.com/FabricMC/fabric-loader
[fabric-api]: https://github.com/FabricMC/fabric-api
[forge-downloads]: https://files.minecraftforge.net/net/minecraftforge/forge/index_26.2.html
[forge-docs]: https://docs.minecraftforge.net/en/latest/
[forge-license]: https://github.com/MinecraftForge/MinecraftForge/blob/26.2/LICENSE-header.txt
[quilt-loader]: https://github.com/QuiltMC/quilt-loader
[architectury]: https://www.curseforge.com/minecraft/mc-mods/architectury-api
[multiloader]: https://github.com/jaredlll08/MultiLoader-Template
[mdk]: https://github.com/NeoForgeMDKs/MDK-26.1.2-ModDevGradle
[mdk-properties]: https://github.com/NeoForgeMDKs/MDK-26.1.2-ModDevGradle/blob/main/gradle.properties
[mdk-build]: https://github.com/NeoForgeMDKs/MDK-26.1.2-ModDevGradle/blob/main/build.gradle
[mdg]: https://github.com/neoforged/ModDevGradle
[hotswap]: https://www.jetbrains.com/help/idea/altering-the-program-s-execution-flow.html
[blockbench]: https://blockbench.net/
[github-ci]: https://docs.github.com/en/actions/tutorials/build-and-test-code/java-with-gradle
[resources]: https://docs.neoforged.net/docs/resources/
[registries]: https://docs.neoforged.net/docs/concepts/registries/
[recipes]: https://docs.neoforged.net/docs/resources/server/recipes/
[tags]: https://docs.neoforged.net/docs/resources/server/tags/
[enchantments]: https://docs.neoforged.net/docs/resources/server/enchantments/
[conditions]: https://docs.neoforged.net/docs/resources/server/conditions/
[datamaps]: https://docs.neoforged.net/docs/resources/server/datamaps/
[biomemodifiers]: https://docs.neoforged.net/docs/worldgen/biomemodifier/
[saveddata]: https://docs.neoforged.net/docs/datastorage/saveddata/
[components]: https://docs.neoforged.net/docs/items/datacomponents/
[payloads]: https://docs.neoforged.net/docs/networking/payload/
[sides]: https://docs.neoforged.net/docs/concepts/sides/
[config]: https://docs.neoforged.net/docs/misc/config/
[gametest]: https://docs.neoforged.net/docs/misc/gametest/
[profiler]: https://docs.neoforged.net/docs/misc/debugprofiler/
[jigsaw-history]: https://www.minecraft.net/fr-fr/article/minecraft-java-edition-1-16-2
[geckolib]: https://www.curseforge.com/minecraft/mc-mods/geckolib
[curios]: https://www.curseforge.com/minecraft/mc-mods/curios
[jei]: https://www.curseforge.com/minecraft/mc-mods/jei
[twilight]: https://www.curseforge.com/minecraft/mc-mods/the-twilight-forest
[twilight-progression]: https://benimatic.com/tfwiki/index.php/Progression
[twilight-repo]: https://github.com/TeamTwilight/twilightforest
[aether]: https://www.curseforge.com/minecraft/mc-mods/aether
[yung]: https://www.curseforge.com/minecraft/mc-mods/yungs-better-dungeons
[apotheosis]: https://www.curseforge.com/minecraft/mc-mods/apotheosis
[sevtech]: https://www.curseforge.com/minecraft/modpacks/sevtech-ages
[packwiz]: https://packwiz.infra.link/
[packwiz-cf]: https://packwiz.infra.link/tutorials/hosting/curseforge/
[mrpack]: https://support.modrinth.com/en/articles/8802351-modrinth-modpack-format-mrpack
[cf-submit]: https://support.curseforge.com/support/solutions/articles/9000197908-exporting-a-modpack-for-curseforge-project-submission
[eula]: https://www.minecraft.net/en-us/eula
[github-license]: https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/licensing-a-repository
[copyright-games]: https://www.copyright.gov/register/tx-games.html
