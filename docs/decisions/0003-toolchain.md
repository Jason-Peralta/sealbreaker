# 0003 — ModDevGradle, Java 25, Mojang names, IntelliJ, JBR hot-swap
Date: 2026-09-04
Status: accepted
Context: NeoGradle is no longer the generator default and less active; Parchment has no 26.x data; 26.x needs Java 25 and IntelliJ 2025.3+ for Mixin support.
Decision: ModDevGradle 2.0.x with Gradle 9.2.1, Java 25 via the foojay toolchain resolver, Mojang names with no Parchment and no remap step; IntelliJ IDEA 2026.x + Minecraft Development plugin; JetBrains Runtime 25 with `-XX:+AllowEnhancedClassRedefinition` as the run JDK; GitHub Actions with setup-gradle `cache-provider: basic`; packwiz + Prism Launcher for distribution.
Consequences: one Gradle multi-project, one version for all modules; plain jar artifacts; game tests run headlessly in CI; the modpack lives in pack/ as a packwiz project served from GitHub Pages.
