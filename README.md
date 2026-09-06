# Sealbreaker (working title)

An original, Terraria-inspired high-fantasy Minecraft Java modpack in which every gameplay mod is written by us: boss-gated progression, swings-and-hitboxes combat with believable movesets, an earned movement kit, reforging and accessory combining, dungeons and realms. Built for a private four-player server.

- Minecraft **26.2** on **NeoForge**, Java 25, ModDevGradle. See `docs/01-research.md` for why.
- Design: `docs/02-prd.md`. Plan: `docs/03-roadmap.md`. Decisions: `docs/decisions/`. Spikes: `docs/spikes/`.
- Working conventions for humans and Claude Code sessions: `CLAUDE.md`. Tickets: the GitHub issues, grouped by milestone and track.

## Build and run

Requires a JDK 17+ on the PATH to run Gradle (the Java 25 toolchain is downloaded automatically through the foojay resolver) and an internet connection for the first build. The first build on a machine downloads NeoForge 26.2.0.76 and decompiles the client: allow 10 to 20 minutes and a few gigabytes under `~/.gradle`; every later clone reuses that cache (a fresh clone built in 17 seconds on a warm machine).

```bash
./gradlew build                        # compile, unit-test and package every module
./gradlew gameTests                    # headless game tests of every module (exit code = failed tests)
./gradlew :sb_combat:runClient         # client with core and combat; each module has client, client2, server, gameTestServer and data runs
./gradlew :sb_gear:runClient           # client with core, combat and gear; :sb_world and :sb_bosses likewise load what they depend on
./gradlew :sb_core:runServer           # dedicated server (accept the EULA in sb_core/run/eula.txt by hand first)
```

Layout and conventions every module shares live in `buildSrc/src/main/groovy/sealbreaker.mod.gradle`; a module's `build.gradle` is the plugin line plus `sealbreakerMod.dependsOnModules ':sb_core', ...`. Game tests live in each module's `src/gametest` as a test-only mod that only the game test server loads; the shipped jars carry none of them.

Things a fresh machine may trip on:

- **Windows shells.** Gradle output in this README is from Git Bash with `--console=plain`; PowerShell works the same. Paths inside the game (`sb_world/run/...`) are relative to each module's `run/` directory, which is created on first launch and ignored by git.
- **Run directories start empty.** A first client launch shows the accessibility onboarding screen before the title screen; the capture runs wait it out, a person clicks through it once. The animation capture run (`:sb_combat:runClientAnim`) expects a save named `Anim Capture` under `sb_combat/run/saves`; create any creative flat world with that name.
- **GeckoLib** (`sb_bosses`) resolves from the Modrinth Maven by version id (`sb_bosses/gradle.properties`), not from GeckoLib's own Maven, which has no 26.2 artifact (decision 0020).
- **Parallel game test servers.** `./gradlew gameTests` runs every module's `runGameTestServer` concurrently (Gradle parallel mode); each has its own `run/gametestserver` folder, so this is fine, but expect five game windows' worth of CPU for half a minute.
- **The IDE does not need the Minecraft Development plugin** to build; it helps with mixin navigation only.

## IDE and the edit loop

IntelliJ IDEA 2026.x: open the root folder as a Gradle project and let it sync; ModDevGradle generates an Application run configuration per run (`sb_combat - Client`, `sb_combat - ClientAnim`, ...; each loads its module and the modules it depends on, so the sword is in `sb_combat - Client`, not `sb_core - Client`). Settings that matter:

- **Project SDK:** any JDK 17+ for Gradle itself; the modules compile with the Java 25 toolchain Gradle downloads.
- **Run JRE for the run configurations:** the bundled **JetBrains Runtime 25** (IntelliJ 2026.1 ships 25.0.2 under `C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.1\jbr`): add it as an SDK and make it the project SDK, and every generated configuration runs on it. The run configurations already carry `-XX:+IgnoreUnrecognizedVMOptions -XX:+AllowEnhancedClassRedefinition` (through the `@build/moddev/<run>VmArgs.txt` argument file); on JBR those flags enable hot-swapping added and removed methods and fields while the game runs, on any other JDK they are ignored.
- **Code loop** (confirmed 6 Sep 2026): start the generated `sb_combat - Client` configuration with **Debug** (hot swap needs the debugger), open the Java file in the editor, edit, press Ctrl+Shift+F9 (Recompile) with the editor focused, answer Reload. A new private method plus a new log line in `SwingService.hit` appeared on the next swing without a restart. Changing a class's shape beyond added methods and fields still needs a restart.
- **Resource loop:** edit a JSON under `src/main/resources`, run `Build > Build Project` (that is `processResources`, which copies it to `build/resources/main`, where the running game reads resources from), then press F3+T in game. Verified without a person for the player animations: `./gradlew :sb_combat:runClientAnim -Psb.animdebug=reload` edits the sword animation's copy under `build/resources/main`, presses F3+T through `reloadResourcePacks` and logs `sword_sweep_ltr length before 0.55 after 0.95 (the reload picked up the edit)`. Data pack registries (`data/<ns>/sb/...`) reload with `/reload` instead.

## Capture runs (dev only)

Each spike left a run configuration that drives the client without a person and screenshots into the module's `run/screenshots`; they are the fastest way to see a change: `:sb_combat:runClientAnim` (animation contact sheets and the live combo), `:sb_gear:runClientGear` (item name, tooltip, glint), `:sb_world:runClientWorld -Psb.worlddebug=<seed>` (finds the spike structure), `:sb_world:runClientPortal` + `:sb_world:runClientPortalGuest` and `:sb_bosses:runClientBoss` + `:sb_bosses:runClientBossGuest` (two clients on one machine: the host opens its world to LAN with authentication off, start it first, then the guest). The shared plumbing is `sb_core`'s `DevHarness`.

## Layout

One Gradle subproject per mod (`sb_core`, `sb_combat`, `sb_gear`, `sb_world`, `sb_bosses` exist; `sb_realms`, `sb_hub`, `sb_blocks` follow), the shared build plugin in `buildSrc/`, the packwiz modpack in `pack/`, generators for reproducible assets in `tools/`, documentation in `docs/`.

## Status

Milestone 0 (environment and spikes): all five spikes answered (`docs/spikes/`), the build and the game tests pass from a fresh clone; the packwiz pack installs from a served URL and the IDE hot-swap loop is confirmed; CI runs the build and every game test on each push and gates `main`; the packwiz pack installs a throwaway server and a Prism client that join each other. The only open Milestone 0 item is the second developer's machine (`docs/03-roadmap.md`).

## Licence

Code: MIT. Assets (textures, models, sounds, structures, lore): all rights reserved. Public repository since 6 Sep 2026 (decision 0021).
