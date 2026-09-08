# `sb:weapon_archetype`

Datapack registry declared by `sb_core`; read by `sb_combat` (server hit tests, client animation). Files live at `data/<namespace>/sb/weapon_archetype/<name>.json`. An item becomes a weapon by carrying the `sb_core:archetype` data component whose value is the entry's id (for example `sb_combat:sword`). Loaded with the world; changes need a world reload, not `/reload`.

## Shape

```json
{
  "combo_window_ticks": 12,
  "idle": "sb_combat:sword_idle",
  "tap": [
    {
      "shape": "sweep",
      "direction": "left_to_right",
      "reach": 3.0,
      "arc_degrees": 150.0,
      "vertical_reach": 2.0,
      "windup_ticks": 2,
      "active_ticks": 3,
      "recovery_ticks": 3,
      "damage_multiplier": 0.7,
      "knockback": 0.3,
      "animation": "sb_combat:sword_sweep_ltr"
    }
  ]
}
```

| Field | Type | Default | Meaning |
|---|---|---|---|
| `combo_window_ticks` | int | 10 | After a move ends, a tap within this many ticks continues the combo; later taps restart at the first move. A tap during a move is buffered and the next move starts the tick the current one ends: moves always play out in full. |
| `idle` | animation id | none | Stance held while the weapon is in hand and no move plays; moves blend out of it and back into it over 3 ticks. Absent: the vanilla held-item pose. |
| `tap` | list of moves (1–16) | required | The tap combo, in order. It wraps to the first move after the last. |
| `tap[].shape` | `sweep`, `overhead`, `thrust` or `plunge` | required | `sweep`: the live hit window travels across the arc during the active ticks. `overhead`: a narrow cone, live for the whole active window (a chop). `thrust`: the same cone for a stab; give it a small arc and a longer reach. `plunge`: downward cone from the eyes, independent of look, held active until landing. |
| `tap[].direction` | `left_to_right`, `right_to_left`, `none` | `none` | Travel direction of a sweep, from the attacker's point of view. |
| `tap[].reach` | double | required | Horizontal reach in blocks from the eyes; a target's half-width counts toward it. |
| `tap[].arc_degrees` | float | required | Total horizontal arc. |
| `tap[].vertical_reach` | double | 2.0 | How far above or below the look line a target's centre may be at its distance, so aiming down at a low target or up at a high one reaches it. |
| `tap[].windup_ticks` | int | required | Ticks before the move can hit. |
| `tap[].active_ticks` | int | required | Ticks the hit region is live; a sweep divides its arc into this many windows (plus a 12° margin each side). |
| `tap[].recovery_ticks` | int | required | Ticks after the active window before the move ends. Nothing cancels it; a buffered tap starts the next move when it ends. |
| `tap[].damage_multiplier` | float | 1.0 | Multiplier on the attacker's attack-damage attribute. |
| `tap[].knockback` | float | 0.4 | Knockback strength per hit. |
| `tap[].animation` | animation id | none | Player animation to play for the move (see below), in both third and first person. Without one the client falls back to a procedural pose. |
| `tap[].hitstop_ticks` | int | 2 | Impact frames: on the move's first hit its timeline shifts by this many ticks, the attacker's pose freezes over the gap and the attacker's camera kicks. 0 disables. |
| `forward_impulse` | double (0–4) | 0 | One server-authored horizontal velocity impulse along facing yaw when the move starts. |
| `landing_particles` | int (0–256) | 0 | Block particles at the feet when a plunge lands. |
| `landing_spread` | float (0–4) | 0 | Horizontal spread of the landing particles. |

Move fields apply in every context. For `plunge`, `reach` is downward depth and `arc_degrees` the cone angle; `vertical_reach` only expands the broad-phase query. The target half-width and half-height count toward overlap. Landing closes damage immediately, freezes the impact pose for `hitstop_ticks`, then plays recovery. The synced transient `landing_tick` makes tracking clients use the same landing boundary.

## Input contexts (#15)

| Field | Type | Default | Meaning |
|---|---|---|---|
| `hold`, `air`, `sprint` | list of moves (0–16) | empty | Optional contexts; the first move is used. An absent/empty context falls back to tap. Only tap advances a combo. |
| `hold_threshold_ticks` | nonnegative int | 6 | A ground/ordinary press released at or before this boundary is a tap; longer holds charge. |
| `charge_ticks` | positive int | 20 | Server ticks from initial press to full charge; must exceed the threshold when hold moves exist. |
| `charge_curve` | `{min, max, exponent}` | identity (1, 1, 1) | Multiplies hold damage by `min + (max - min) * pow(clamp(held_ticks / charge_ticks, 0, 1), exponent)`. Nonnegative bounds, `max >= min`, positive exponent. |

`AttackInput` samples attack-key edges every client tick, independent of whether the crosshair sees air, an entity or a block. The existing interaction event only suppresses vanilla attacks and mining for weapons; tools keep vanilla input. Falling (`!onGround` and negative vertical velocity) selects air on press, ground sprinting selects sprint on press, and ordinary input selects tap/hold on release. Rising input remains ordinary input. An ordinary aerial move ends on landing or when its authored duration expires. A plunge keeps its final active tick until landing, then plays impact and recovery.

The `swing_request` payload carries a context, release flag and cancellation flag. Combat protocol version 3 includes the landing timestamp in synced swing state. A normal press starts a server timer. The server derives charge from that timer, validates movement claims, ignores repeated presses/releases, and never accepts a client-supplied duration, target or damage value. Charge poses sync to tracking clients, hold before the hit window and cannot hit while the key is down. Release starts the active window (the hold already supplied the anticipation) with the capped charge multiplier.

Tap requests during any released move buffer at most one subsequent tap. Wind-up, active frames and recovery always finish before the buffered move starts. Context moves do not advance the tap combo. A switch of held stack/components, death, spectator mode, dimension/player replacement or opening a server menu clears stale state. Client menus, lost focus and weapon switches cancel a pending press and require a new press; cancelling a released move's input does not interrupt its recovery. Transient runtime state is cleared on logout/server shutdown; this adds no saved player data.

The shipped sword has all four contexts (#16). Its authored balance table is `sb_combat/src/data/sword.json`; `:sb_combat:runData` validates it through the registry codec and emits the committed `src/generated/resources/data/sb_combat/sb/weapon_archetype/sword.json`. Edit the source and regenerate. `sb_combat_tests:input_fixture` exercises all four lists in test resources only. GameTests cover context rejection/selection, landing, full and partial charge, actual damage, duplicate requests, buffering and cancellation. JUnit covers input boundaries, the curve and codec validation/backward-compatible defaults. AC-08 still requires the dedicated-server input/mining/tool check in play; automated tests are not a substitute for that acceptance run.

## Player animations

Client resource, hot-reloadable with F3+T: `assets/<namespace>/sb_animations/player/<file>.json`. Each file holds any number of animations under `"animations"`; an animation's id is `<namespace>:<key>` (the file name does not matter). The shape is the Bedrock/Blockbench animation export, so animations can be authored in Blockbench on a player rig and pasted in:

```json
{
  "animations": {
    "sword_sweep_ltr": {
      "animation_length": 0.55,
      "bones": {
        "body":       {"rotation": {"0.0": [0, 0, 0], "0.18": {"post": [4, -38, 3], "easing": "easeOutQuad"}, "0.55": [0, 0, 0]}},
        "right_arm":  {"rotation": {"0.0": [0, 0, 0], "0.18": {"post": [-78, -62, 0], "easing": "easeOutQuad"}}, "position": {"0.3": [0, 0, -1]}},
        "right_item": {"rotation": {"0.18": [90, 30, -45]}}
      }
    }
  }
}
```

- **Bones**: `head`, `body`, `right_arm`, `left_arm`, `right_leg`, `left_leg` (the vanilla parts; Blockbench's `rightArm`, `torso` spellings are accepted), `right_item`, the wrist of the main hand, and `camera`: pitch, yaw and roll in degrees added to the local player's view while the pose plays (the head swaying with a cut, dipping into a lunge). For a left-handed player the arms are swapped and yaw and roll mirrored automatically.
- **Channels**: `rotation` in degrees, `position` in pixels. Keys are `"<seconds>": [x, y, z]` or `{"post": [x, y, z], "easing": "<name>"}` (`linear`, `easeIn/Out/InOutSine|Quad|Cubic|Quart`, `easeOutBack`) or `{"post": [...], "lerp_mode": "catmullrom"}`. A bare array is a constant. Before the first key the first value holds; after the last, the last.
- **Everything is additive** on top of the vanilla pose (walking, sneaking, the raised item arm), so animations start and end at zero. Rotation order is X, then Y, then Z, like the vanilla model parts.
- **Rig**: the body rotates about the hips and the arms and head ride on it, so a torso twist carries the shoulders (vanilla's flat part list does not). The first-person view draws the player's real arm from a shoulder placed relative to the eyes with the same `body`, `right_arm` and `right_item` tracks, so one animation serves both views and what the player sees is what the hit test sweeps.
- **Conventions** (verified with the animation debugger): `body` x+ leans forward, y+ twists to face the character's right. `right_arm` x− raises the arm forward (−90 level, −180 straight up; vanilla already adds about −18 for a held item), y+ swings a raised arm toward the character's right. `right_item` x 90 lays the blade along the forearm (0 is vanilla's fist grip with the blade perpendicular), y twists the flat of the blade (a sprite is invisible edge-on), z cocks the blade sideways, positive toward the character's right.
- **Timing** is the move's business: keyframes must put the blade across the front of the character inside the move's active ticks.
- **Chaining.** Moves play out in full and the next starts the tick the previous ends, so author them as one sequence: each move's first frame is the previous move's last frame (the backhand's follow-through is the forehand's chamber, the forehand's follow-through is where the thrust withdraws from, the thrust ends on the idle). The client fades whatever is on screen into a move's first frame over 3 ticks and a finished move into the idle over 6, so a dropped combo settles instead of snapping.
- **Following the look.** While a move plays, the character's body turns to the look yaw (as vanilla does during its attack animation) and the torso takes on 70% of the look pitch, so third-person swings go where the player aims; first person is camera-relative already.
- **First person** draws only the weapon, where vanilla draws a held item, oriented by the same rig (a level blade in third person is a level blade here) and moved by the fist's travel from the idle stance. No arm: at that distance a real arm fills the screen. Then it is framed for a game, with the crosshair as the centre of attention: for every move the pose at its contact moment is placed once and the blade's reference point measured (the tip for a thrust, the middle for a cut), and the move is shifted, ramping in over the wind-up, so that point sits on the crosshair at contact. A cut therefore crosses the crosshair on its way and a thrust lands on it, whatever the third-person arm does. A thrust is additionally turned to point straight down the view line around its hit window. Three guards keep it readable: the hand never comes nearer than arm's length, a blade that would point behind the camera plane is turned to lie along the screen, and the flat is twisted to face a point above the viewer so a sprite is never seen edge-on as a black slab. At rest the weapon leans 18° toward the middle of the screen, the way vanilla's does.
- **Feel.** The whoosh plays when the hit window opens, not at the wind-up. A landed hit freezes the attacker's pose for the move's `hitstop_ticks` (the server shifts the timeline; every client holds the pose over the gap) and kicks the attacker's camera. During the strike the blade leaves an additive arc (a ribbon between where the blade was over the last 6 ticks and where it is): in world space from the rig for every other avatar, and from the drawn weapon itself for the local player in first person, so the arc always sits on the blade the viewer sees.
- **Cuts are diagonal.** A one-handed cut is raised over a shoulder with the blade back (the way vanilla's own grip holds a sword), whips over the top and across the front at chest height with the wrist opening so the blade runs out along the forearm (`right_item` x from about 20 to 80), and finishes low on the other side. A flat horizontal sweep reads as "holding the sword out sideways" on this model; a diagonal reads as a cut from every angle and its arc shows the hit window. Sprites are invisible edge-on, so the flat carries some twist (`y` 30–40) and first person twists it further toward the viewer.
- **Held, not stuck on.** A weapon in hand is always drawn through our grip, idle included: the handle's middle sits in the centre of the fist, the pommel a pixel out of its back, and the wrist pivots there. Keep the blade at least 25–30° off the forearm (`x` up to about 65, or `x` 90 with `|z|` of 35 or more): a real fist cannot hold a blade in line with the forearm, and on the model it reads as a sword glued to the end of the arm.

Contact sheets without a person at the keyboard: `./gradlew :sb_combat:runClientAnim "-Psb.animdebug=sb_combat:sword_sweep_ltr,sb_combat:sword_thrust"` joins the `Anim Capture` world (a copy of `New World` under `sb_combat/run/saves`; do not open it in a client while capturing, a locked world aborts the run), sets the time to day, freezes each animation every 0.05 s and screenshots it from behind, at three-quarters, from the side, from the front and in first person into `sb_combat/run/screenshots/anim_<name>_<view>_<NN>.png`; `java tools/ContactSheet.java` and `java tools/Crop.java` tile them.

## Current entries

| Id | Combo | Notes |
|---|---|---|
| `sb_combat:sword` | sweep left-to-right (0.7×), sweep right-to-left (0.7×), thrust (1.4×, 30° lane, reach 3.4, knockback 0.7) | 5/2/4, 5/2/4 and 8/3/7 ticks (wind-up/active/recovery: 0.55 + 0.55 + 0.90 s); hitstop 2/2/3; combo window 12; idle `sword_idle` (vanilla's hold, all but untouched) and the chained animations `sword_sweep_ltr` (a backhand raised over the left shoulder, cut down to the right), `sword_sweep_rtl` (a forehand raised over the right shoulder, cut down to the left), `sword_thrust` in `assets/sb_combat/sb_animations/player/sword.json` (generated from `tools/gen_sword_anim.py`) |

## Sword context reference (#16)

| Context | Move | Wind-up / active / recovery | Behavior |
|---|---|---|---|
| Air | `sword_plunge` | 4 / 3 (extended until landing) / 6 | Downward cone, reach 3.2, arc 50°, damage 1.2×; landing has 12 block particles and 2 impact ticks. |
| Sprint | `sword_lunge` | 4 / 3 / 6 | Cut with reach 3.5 (tap +0.5), 130° arc, damage 1.1×, forward impulse 0.45. |
| Hold | `sword_heavy` | 8 / 3 / 9 | Raised overhead anticipation; release starts the chop. Base damage 1.3×, charge curve 1–2× with exponent 1.5, full at 24 ticks. Camera bone dips into contact. |

The existing animation generator emits these clips alongside the unchanged tap combo. `PlungeTimingTest` checks landing/recovery timing at large world clocks; `SwordContextAnimationTest` checks the actual rig points the blade down during active frames for either main hand. `sword_contexts` and `sword_plunge` GameTests read the shipped data and check charge scaling, impulse, real downward hits, extended air time and landing recovery.

Reproducible capture runs create uniquely named **fresh** worlds and a platform at loaded spawn; existing saves are never changed or deleted:

```bash
./gradlew :sb_combat:runClientSwordFrames
./gradlew :sb_combat:runClientSwordFrames -Psb.animdebug=combo:fp
./gradlew :sb_combat:runClientSwordHost
# Once the host logs LAN publication on 25576, in another terminal:
./gradlew :sb_combat:runClientSwordGuest
```

Frames cover all five existing views. First person retains the HUD because hiding it also hides the held weapon. Host and guest runs exercise real client payloads and tracking attachment sync; logs and `sword_<host|guest>_<context>_<phase>.png` record observed contexts. The host stages movement to make cases repeatable; this is a network/animation check, not a keyboard usability or dedicated-server acceptance run. Run frames and host sequentially because they share the main dev game directory; the guest uses `run/sword-guest`.
