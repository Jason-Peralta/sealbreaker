# 0001 — Target the Minecraft 26.x line, pinned at 26.2
Date: 2026-09-04
Status: accepted
Context: 1.21.1 has the most existing mods but is obfuscated, on Java 21 and two years old; 26.1+ ships unobfuscated with official names and Java 25, and Mojang now releases quarterly drops. All gameplay mods are ours, so the third-party catalog gap matters little.
Decision: build on 26.2 now. Freeze the version for the duration of a milestone; at a milestone boundary bump only if NeoForge has non-beta builds, GeckoLib, Curios and JEI have releases, and the port is estimated under two working days.
Consequences: readable vanilla source and stack traces; no mapping toolchain; big content mods (Create-class) are unavailable as playtest companions; 26.3 is expected to be the first bump, before Milestone 1 if its ecosystem is ready.
