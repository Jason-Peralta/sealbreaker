"""Generates sb_combat's sword animations from compact key tables (run from the repository root).

Until the team authors in Blockbench, edit the tables here and re-run. Conventions are documented in
docs/data/weapon_archetype.md and were verified with the animation debugger (runClientAnim): body x+ leans
forward, body y+ twists to face the character's right; right_arm x- raises forward (-90 level, -180 up; vanilla
adds about -18 for a held item), y+ swings a raised arm to the character's right; right_item x 90 lays the blade
along the forearm, y turns the flat (0 = flat sideways, edge up and down; 90 = flat level, edge leading a
horizontal cut), z cocks the blade sideways (positive toward the character's right). Positions are pixels,
z negative = forward.

The three moves are one chained sequence, as a real combo is: the backhand finishes low on the right, from where
the forehand rises over the right shoulder; the forehand finishes low on the left, from where the thrust
withdraws; the thrust recovers to the idle stance. Every move therefore starts at the pose the previous one ends on, and the client blends whatever is
on screen into a move's first frame, so a combo flows and a dropped combo settles back to the stance.
"""
import json
from collections import OrderedDict

OUT = 'sb_combat/src/main/resources/assets/sb_combat/sb_animations/player/sword.json'

OUT_Q, IN_Q, IO_Q, LIN = "easeOutQuad", "easeInQuad", "easeInOutQuad", "linear"
BONES = ("body", "right_arm", "right_item", "left_arm", "right_leg", "left_leg", "camera")


def pose(body=(0, 0, 0), arm=(0, 0, 0), item=(0, 0, 0), left_arm=(0, 0, 0), right_leg=(0, 0, 0), left_leg=(0, 0, 0),
         body_pos=(0, 0, 0), arm_pos=(0, 0, 0), camera=(0, 0, 0)):
    """camera: pitch, yaw, roll in degrees added to the local player's view (the head's sway; see CameraFeel)."""
    return {"body": (body, body_pos), "right_arm": (arm, arm_pos), "right_item": (item, None),
            "left_arm": (left_arm, None), "right_leg": (right_leg, None), "left_leg": (left_leg, None),
            "camera": (camera, None)}


def with_camera(p, camera):
    q = dict(p)
    q["camera"] = (camera, None)
    return q


def tweak(p, **changes):
    """A copy of a pose with some bones replaced (rotation only)."""
    q = dict(p)
    for bone, rot in changes.items():
        q[bone] = (rot, q[bone][1])
    return q


def animation(length, keys):
    """keys: list of (time, pose, easing). Builds one channel per bone from the poses."""
    bones = OrderedDict()
    for bone in BONES:
        rotation = OrderedDict()
        position = OrderedDict()
        any_position = False
        for t, p, e in keys:
            rot, pos = p[bone]
            key = f"{t:.2f}"
            rotation[key] = list(rot) if e is None else {"post": list(rot), "easing": e}
            if pos is not None:
                position[key] = list(pos) if e is None else {"post": list(pos), "easing": e}
                any_position = any_position or tuple(pos) != (0, 0, 0)
        entry = OrderedDict([("rotation", rotation)])
        if any_position:
            entry["position"] = position
        bones[bone] = entry
    return OrderedDict([("animation_length", length), ("bones", bones)])


# ---------------------------------------------------------------------------------------------------------------
# Stances. Angles in degrees; the vanilla held-item pose (arm raised 18) is underneath all of them.
#
# The cuts are diagonal, as most real one-handed cuts are and as combat mods draw them: the sword is raised over
# one shoulder with the blade back (the way vanilla's own grip holds it), whips over the top and across the front
# at chest height with the wrist opening so the blade runs out along the forearm, and finishes low on the other
# side. A flat horizontal sweep reads as "holding the sword out sideways" on this model; a diagonal reads as a
# cut from every angle, and its arc shows the player exactly where the hit window is.
CAM = 0.65  # the user asked for about a third less sway


def cam(pitch, yaw, roll):
    return (pitch * CAM, yaw * CAM, roll * CAM)


IDLE = pose(arm=(-4, -3, 0), item=(0, 0, 0))  # vanilla's own hold, all but untouched

# Backhand: raised over the LEFT shoulder, cut down across the front to the lower right.
RAISE_L = pose(body=(-5, -35, 4), arm=(-140, -45, 0), item=(20, 30, 0), left_arm=(15, -8, 0),
               right_leg=(10, 0, 0), left_leg=(-10, 0, 0), arm_pos=(0, 0, 1))
CONTACT_LTR = pose(body=(10, 10, 0), arm=(-80, 10, 0), item=(70, 40, 15), left_arm=(-8, 4, 0),
                   right_leg=(2, 0, 0), left_leg=(-2, 0, 0), arm_pos=(0, 0, -2))
LOW_R = pose(body=(12, 35, -3), arm=(-35, 55, 0), item=(80, 40, 20), left_arm=(-24, 10, 0),
             right_leg=(-8, 0, 0), left_leg=(6, 0, 0))

# Forehand: from the lower right up over the RIGHT shoulder, cut down across the front to the lower left.
RAISE_R = pose(body=(-5, 38, -4), arm=(-140, 40, 0), item=(20, 30, 0), left_arm=(-10, 6, 0),
               right_leg=(8, 0, 0), left_leg=(-8, 0, 0), arm_pos=(0, 0, 1))
CONTACT_RTL = pose(body=(10, -10, 0), arm=(-80, -10, 0), item=(70, 40, -15), left_arm=(8, -4, 0),
                   right_leg=(-2, 0, 0), left_leg=(2, 0, 0), arm_pos=(0, 0, -2))
LOW_L = pose(body=(12, -35, 3), arm=(-35, -55, 0), item=(80, 40, -20), left_arm=(14, -8, 0),
             right_leg=(-8, 0, 0), left_leg=(6, 0, 0))

# Thrust: withdraw to the hip with the point on line (the blade turns forward as the hand comes back), coil the
# shoulder, drive the arm out level, hold, withdraw to the stance.
WITHDRAW = pose(body=(0, -6, 0), arm=(-20, -30, 0), item=(50, 20, -20), left_arm=(-10, 0, 0),
                right_leg=(2, 0, 0), left_leg=(-2, 0, 0))
CHAMBER_T = pose(body=(-3, 18, 0), arm=(25, 8, 0), item=(-10, 0, 0), left_arm=(-25, 5, 0),
                 right_leg=(10, 0, 0), left_leg=(-8, 0, 0), body_pos=(0, 0, 0.5), arm_pos=(0, 0, 1))
DRIVE = pose(body=(4, 0, 0), arm=(-25, 8, 0), item=(40, 0, 0), left_arm=(0, 8, 0),
             right_leg=(15, 0, 0), left_leg=(-16, 0, 0), body_pos=(0, 0, -0.5), arm_pos=(0, 0, -1))
# At full extension the shoulder turns in 12 and the arm angles 8 out, so the blade points down the look line.
EXTEND = pose(body=(12, -12, 0), arm=(-54, 8, 0), item=(70, 0, 0), left_arm=(28, 12, 0),
              right_leg=(20, 0, 0), left_leg=(-26, 0, 0), body_pos=(0, 0, -1.5), arm_pos=(0, 0, -2))
EXTEND_HELD = pose(body=(12, -11, 0), arm=(-52, 8, 0), item=(70, 0, 0), left_arm=(26, 12, 0),
                   right_leg=(20, 0, 0), left_leg=(-26, 0, 0), body_pos=(0, 0, -1.5), arm_pos=(0, 0, -2))
RECOVER = pose(body=(6, -6, 0), arm=(-40, 0, 0), item=(40, 0, 0), left_arm=(10, 6, 0),
               right_leg=(10, 0, 0), left_leg=(-12, 0, 0), body_pos=(0, 0, -0.5), arm_pos=(0, 0, -1))

# ---------------------------------------------------------------------------------------------------------------
# Timelines. Sweeps: 11 ticks (0.55 s), hit window 0.25-0.35 s. Thrust: 18 ticks (0.90 s), hit window 0.40-0.55 s.
# Each cut: raise (ease out), a beat of anticipation, snap over the top and through contact in three ticks,
# decelerate to the low finish, overshoot a hair, settle. The finish of one is the start of the next.
SWEEP_LTR = [
    (0.00, IDLE, None),
    (0.10, with_camera(pose(body=(-3, -20, 2), arm=(-95, -30, 0), item=(10, 20, 0), left_arm=(8, -4, 0),
                            right_leg=(5, 0, 0), left_leg=(-5, 0, 0), arm_pos=(0, 0, 0.5)), cam(-0.6, -0.8, -1.0)), OUT_Q),
    (0.19, with_camera(RAISE_L, cam(-1.2, -1.5, -1.8)), OUT_Q),
    (0.24, with_camera(tweak(RAISE_L, body=(-6, -38, 4), right_arm=(-146, -48, 0), right_item=(14, 30, 0)), cam(-1.4, -1.8, -2.0)), IO_Q),
    (0.28, with_camera(pose(body=(4, -12, 2), arm=(-112, -18, 0), item=(45, 36, 5), left_arm=(2, -1, 0),
                            right_leg=(6, 0, 0), left_leg=(-6, 0, 0), arm_pos=(0, 0, -1)), cam(0.2, -0.6, -0.6)), IN_Q),
    (0.31, with_camera(CONTACT_LTR, cam(1.0, 0.8, 1.2)), IN_Q),
    (0.36, with_camera(pose(body=(12, 30, -2), arm=(-48, 45, 0), item=(78, 40, 20), left_arm=(-22, 9, 0),
                            right_leg=(-8, 0, 0), left_leg=(6, 0, 0), arm_pos=(0, 0, -1)), cam(1.2, 1.6, 2.0)), OUT_Q),
    (0.43, with_camera(tweak(LOW_R, body=(13, 39, -4), right_arm=(-30, 60, 0)), cam(0.9, 1.8, 2.2)), OUT_Q),
    (0.50, with_camera(LOW_R, cam(0.5, 1.0, 1.2)), IO_Q),
    (0.55, with_camera(LOW_R, cam(0.4, 0.8, 1.0)), LIN),
]
SWEEP_RTL = [
    (0.00, with_camera(LOW_R, cam(0.4, 0.8, 1.0)), None),
    (0.13, with_camera(RAISE_R, cam(-1.2, 1.5, 1.8)), OUT_Q),
    (0.22, with_camera(tweak(RAISE_R, body=(-6, 41, -4), right_arm=(-146, 43, 0), right_item=(14, 30, 0)), cam(-1.4, 1.8, 2.0)), IO_Q),
    (0.27, with_camera(pose(body=(4, 12, -2), arm=(-112, 18, 0), item=(45, 36, -5), left_arm=(-2, 1, 0),
                            right_leg=(6, 0, 0), left_leg=(-6, 0, 0), arm_pos=(0, 0, -1)), cam(0.2, 0.6, 0.6)), IN_Q),
    (0.30, with_camera(CONTACT_RTL, cam(1.0, -0.8, -1.2)), IN_Q),
    (0.34, with_camera(pose(body=(12, -30, 2), arm=(-48, -45, 0), item=(78, 40, -20), left_arm=(14, -9, 0),
                            right_leg=(-8, 0, 0), left_leg=(6, 0, 0), arm_pos=(0, 0, -1)), cam(1.2, -1.6, -2.0)), OUT_Q),
    (0.42, with_camera(tweak(LOW_L, body=(13, -39, 4), right_arm=(-30, -60, 0)), cam(0.9, -1.8, -2.2)), OUT_Q),
    (0.50, with_camera(LOW_L, cam(0.5, -1.0, -1.2)), IO_Q),
    (0.55, with_camera(LOW_L, cam(0.4, -0.8, -1.0)), LIN),
]
THRUST = [
    (0.00, with_camera(LOW_L, cam(0.4, -0.8, -1.0)), None),
    (0.12, with_camera(WITHDRAW, cam(-0.4, -0.3, -0.4)), OUT_Q),
    # Chamber and hold: the camera rises a touch as the weight goes back.
    (0.22, with_camera(CHAMBER_T, cam(-1.0, 0, 0)), IO_Q),
    (0.30, with_camera(DRIVE, cam(-0.4, 0, 0)), IN_Q),
    # The lunge: accelerate to full extension, overshoot a hair, hard stop, hold through the hit window.
    (0.40, with_camera(EXTEND, cam(1.6, 0, 0)), IN_Q),
    (0.43, with_camera(tweak(EXTEND, body=(14, -13, 0), right_arm=(-57, 8, 0)), cam(2.0, 0, 0)), OUT_Q),
    (0.55, with_camera(EXTEND_HELD, cam(1.4, 0, 0)), OUT_Q),
    (0.72, with_camera(RECOVER, cam(0.4, 0, 0)), IO_Q),
    (0.90, IDLE, IO_Q),
]

# Context moves enter and recover to the same idle. Active windows match src/data/sword.json.
PLUNGE_RAISED = pose(body=(-6, 0, 0), arm=(-155, 8, 0), item=(20, 25, 0),
                    left_arm=(-105, -15, 0), right_leg=(-25, 0, 0), left_leg=(20, 0, 0))
PLUNGE_DOWN = pose(body=(12, 0, 0), arm=(-35, 5, 0), item=(125, 20, -25),
                  left_arm=(-35, -15, 0), right_leg=(-15, 0, 0), left_leg=(15, 0, 0),
                  arm_pos=(0, 0, -2), camera=cam(1.3, 0, 0))
PLUNGE_LAND = pose(body=(18, 0, 0), arm=(-25, 5, 0), item=(115, 20, -25),
                  left_arm=(-25, -10, 0), right_leg=(-25, 0, 0), left_leg=(25, 0, 0),
                  body_pos=(0, 1, 0), camera=cam(2, 0, 0))
PLUNGE = [(0.00, IDLE, None), (0.10, PLUNGE_RAISED, OUT_Q), (0.20, PLUNGE_DOWN, IN_Q),
          (0.30, PLUNGE_DOWN, LIN), (0.35, PLUNGE_LAND, OUT_Q), (0.65, IDLE, IO_Q)]
LUNGE_CHAMBER = tweak(RAISE_R, body=(-4, 25, 0), right_leg=(-20, 0, 0), left_leg=(25, 0, 0))
LUNGE_CONTACT = with_camera(tweak(CONTACT_RTL, body=(18, -15, 0), right_leg=(-30, 0, 0), left_leg=(30, 0, 0)), cam(1.5, 0, 0))
LUNGE = [(0.00, IDLE, None), (0.12, LUNGE_CHAMBER, OUT_Q), (0.20, LUNGE_CONTACT, IN_Q),
         (0.35, LOW_L, OUT_Q), (0.65, IDLE, IO_Q)]
HEAVY_RAISED = pose(body=(-12, -8, 0), arm=(-160, 8, 0), item=(20, 30, 0),
                    left_arm=(-90, -20, 0), right_leg=(12, 0, 0), left_leg=(-12, 0, 0),
                    camera=cam(-1, 0, 0))
HEAVY_CONTACT = pose(body=(20, 0, 0), arm=(-75, 5, 0), item=(70, 30, -25),
                    left_arm=(-30, -10, 0), right_leg=(-15, 0, 0), left_leg=(15, 0, 0),
                    arm_pos=(0, 0, -2), camera=cam(2.5, 0, 0))
HEAVY_LOW = pose(body=(24, 0, 0), arm=(-15, 5, 0), item=(110, 30, -25),
                left_arm=(10, -10, 0), right_leg=(-15, 0, 0), left_leg=(15, 0, 0), camera=cam(2, 0, 0))
HEAVY = [(0.00, IDLE, None), (0.20, HEAVY_RAISED, OUT_Q), (0.35, HEAVY_RAISED, LIN),
         (0.40, HEAVY_CONTACT, IN_Q), (0.55, HEAVY_LOW, OUT_Q), (0.80, RECOVER, IO_Q), (1.00, IDLE, IO_Q)]

doc = OrderedDict([
    ("format_version", "1.8.0"),
    ("_notes", [
        "Sword moveset animations for sb_combat, generated by tools/gen_sword_anim.py (edit that, not this).",
        "Bedrock/Blockbench keyframe shape; rotations in degrees applied X then Y then Z; positions in pixels. All values are added on top of the vanilla pose.",
        "The moves chain: sword_sweep_ltr ends where sword_sweep_rtl starts, which ends where sword_thrust starts, which ends on sword_idle. The client blends into a move's first frame from whatever is on screen.",
        "Timing must match data/sb_combat/sb/weapon_archetype/sword.json: sweeps hit during 0.25-0.35 s, the thrust during 0.40-0.55 s.",
        "The camera bone (pitch, yaw, roll degrees) is added to the local player's view: the head sways with the cut and dips with the lunge.",
    ]),
    ("animations", OrderedDict([
        ("sword_idle", animation(0.0, [(0.0, IDLE, None)])),
        ("sword_sweep_ltr", animation(0.55, SWEEP_LTR)),
        ("sword_sweep_rtl", animation(0.55, SWEEP_RTL)),
        ("sword_thrust", animation(0.90, THRUST)),
        ("sword_plunge", animation(0.65, PLUNGE)),
        ("sword_lunge", animation(0.65, LUNGE)),
        ("sword_heavy", animation(1.00, HEAVY)),
    ])),
])

with open(OUT, 'w', encoding='utf-8') as f:
    json.dump(doc, f, indent=2)
    f.write('\n')
print('wrote', OUT)
