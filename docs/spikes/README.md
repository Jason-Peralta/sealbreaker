# Milestone 0 spikes

Each spike is a branch `spike/<name>`, a write-up here, and a decision-log entry. Write-ups are short: question, what was built, what happened, yes/no answer, what it means for the roadmap.

| Spike | Question | Status |
|---|---|---|
| S1 combat feel | Can we drive a keyframed sword swing (first and third person) from a server-side swing state and hit an arc? Own animation layer or PlayerAnimator? | **A: yes. B: yes (Mixin, decision 0017). C: yes for the item, own arm mesh pending.** See [s1-combat-feel.md](s1-combat-feel.md) (5 Sep 2026) |
| S2 boss animation | Does GeckoLib 5.5.x on NeoForge 26.2 play a server-triggered animation on two clients? | not started |
| S3 portal and dimension | Does the vanilla Portal interface work for a custom gate into a JSON dimension with its own world clock? | **yes on all three parts: the realm's clock, day length and sky are data; the Portal block moves players and mobs both ways with a second client watching (decision 0019).** See [s3-portal-dimension.md](s3-portal-dimension.md) (5 Sep 2026) |
| S4 item presentation | Do item model definitions plus a custom data component render a reforge prefix, tooltip line and glint? | **yes on all three, no Mixins; presentation derives from one component (decision 0018).** See [s4-item-presentation.md](s4-item-presentation.md) (5 Sep 2026) |
| S5 structure | Does a jigsaw structure with a processor list and a locked-door block spawn at a configured rarity? | **yes on all four items: pools assemble, the processor list runs, the door keeps its key through placement, five seeds found it.** See [s5-structure.md](s5-structure.md) (5 Sep 2026) |
