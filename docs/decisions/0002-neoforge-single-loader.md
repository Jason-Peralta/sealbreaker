# 0002 — NeoForge, single loader
Date: 2026-09-04
Status: accepted
Context: Fabric is fastest to each drop and has more mods on 26.x, but a thinner API means more Mixins for a content-heavy pack and its accessory libraries have not reached 26.x; Forge is small; Quilt's standard library stopped in 2024; multi-loader doubles build and test cost for a private pack.
Decision: NeoForge only, no Architectury or multi-loader template.
Consequences: use NeoForge's registries, data attachments, events, payloads, biome/loot modifiers and data maps directly; Curios is the accessory library; Fabric remains a fallback port target, kept viable by confining Mixins to sb_core.
