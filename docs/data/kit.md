# Movement kit: capability flags and the Tier 1 dash

Issue #20; decision 0007 and PRD 3.6. Players begin with vanilla movement. An accessory equips a capability; the combat module enforces it on the server. No dash is granted by default, and M1 has no invulnerability frames.

## Gear contract

`dev.sealbreaker.combat.api.kit.KitCapabilities` exposes `grant(Player, Identifier)`, `revoke(Player, Identifier)` and `has(Player, Identifier)`. The dash flag is `sb_core:dash` (`KitCapabilities.DASH`). Call mutations on the server thread; clients may only read the synced set. Grant/revoke are idempotent set operations, not reference counts: the equipment integration in #25 must retain a flag while any equipped source supplies it and revoke after the last source leaves.

Flags are transient derived equipment state. Reconcile them on login, respawn and equipment changes. They are not saved progression and do not add a save format. Revoking a capability stops an active dash on the next tick. Regranting it during cooldown does not make another dash available early.

## Data and datagen

Author `sb_combat/src/data/dash.json`, then run `./gradlew :sb_combat:runData`. The provider validates and emits `sb_combat/src/generated/resources/data/sb_combat/sb/kit/dash.json`; commit both input and generated output. Java contains no dash tuning defaults. The Gradle data run supplies the authoring directory through `sb.combat.dataInput`.

This is a server reload resource, not a new shared registry. A datapack can override `data/sb_combat/sb/kit/dash.json`; `/reload` validates it and publishes new settings. An active dash and its cooldown retain their original settings. Missing/invalid dash data fails the reload rather than silently creating free movement.

| Field | Type | First-pass M1 value | Meaning |
|---|---|---|---|
| `distance` | positive double, at most 64 | 4 blocks | Total unobstructed horizontal travel |
| `duration_ticks` | int, 1–200 | 4 | Movement spread over this many server ticks |
| `cooldown_ticks` | positive int, at least duration | 30 | Time from accepted request to next permitted request |
| `lean_degrees` | float, 0–45 | 12 | Peak third-person lean; a sine envelope starts and ends at rest |
| `invulnerable` | boolean | false | Must be false in M1; true is rejected |

Bounds are validation limits. First-pass values are tuning proposals for the slice and require balance review; they are not a playtest result.

## Input, movement and multiplayer

Controls lists **Dash / Dodge**, default **R**, under **Sealbreaker**. The mapping is rebindable and active in-game. Its empty `sb_combat:dash_request` payload contains no direction, position, distance or timestamp. The server checks capability, cooldown, ground contact, life/spectator state, mounted/flying/sleeping state and open menus. Only the initial request requires ground contact; the accepted ground dash finishes its short horizontal motion over an edge without granting another airborne dash.

Movement follows the server's look yaw and passes through vanilla block collision each tick. An obstruction ends the dash. The server confirms each resulting position to the owner; the normal tracking path replicates movement to observers. The synced `DashState` supplies a short third-person body lean through the existing pose hook. No Mixin, external dependency, invulnerability or new progression gate is added.

## Verification

JUnit round-trips/validates settings and checks the animation envelope. GameTests use a mock server connection marked client-loaded (vanilla otherwise protects a loading player from damage). They verify denial without a flag, idempotent grants, actual displacement and distance, duplicate requests, cooldown expiry, equip-cycle abuse, revocation during movement, wall collision, airborne denial and damage while dashing.

`./gradlew :sb_combat:runClientDash` opens Controls, scrolls to the last category, writes `sb_combat/run/screenshots/dash_controls.png` and quits. This dev-only run does not change bindings or create a world. Actual dedicated-server feel, the lean in motion and two-client position agreement still require an in-world acceptance run; the Controls capture does not verify those.
