# 0010 — Library dependencies: GeckoLib, Curios, JEI only
Date: 2026-09-04
Status: accepted; refined by 0015 (5 Sep 2026: external mods are utilities only, list may grow by decision-log entry)
Context: library mods are allowed "within reason"; Accessories and EMI have no 26.x builds; PlayerAnimator is on 1.21.7.
Decision: GeckoLib (MIT) for boss and enemy animation; Curios (LGPL-3, NeoForge) for accessory slots, re-evaluated at MVP against writing our own; JEI for playtesting only. Sodium and Spark are optional client/dev tools. Any other dependency needs a new decision.
Consequences: narrow interfaces around Curios and GeckoLib so either can be replaced; PlayerAnimator is checked in the Milestone 0 combat spike.
