"""Generates the S2 spike dummy's GeckoLib assets: a two-cube geometry, two animations and a flat-colour texture.
Run from the repository root. The real bosses are authored in Blockbench; this keeps the spike reproducible."""
import io
import json
import os
import struct
import zlib

A = "sb_bosses/src/main/resources/assets/sb_bosses/"


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    io.open(path, "w", encoding="utf-8", newline="\n").write(json.dumps(obj, indent=2) + "\n")
    print("wrote", path)


def write_png(path, width, height, pixels):
    """pixels: list of rows of (r, g, b, a)."""
    raw = b"".join(b"\x00" + b"".join(struct.pack("BBBB", *p) for p in row) for row in pixels)

    def chunk(kind, data):
        body = kind + data
        return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(png)
    print("wrote", path)


def geometry():
    write_json(A + "geckolib/models/entity/spike_dummy.geo.json", {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {"identifier": "geometry.spike_dummy", "texture_width": 64, "texture_height": 64,
                            "visible_bounds_width": 3, "visible_bounds_height": 3, "visible_bounds_offset": [0, 1, 0]},
            "bones": [
                {"name": "body", "pivot": [0, 0, 0], "cubes": [{"origin": [-8, 0, -8], "size": [16, 16, 16], "uv": [0, 0]}]},
                {"name": "head", "parent": "body", "pivot": [0, 16, 0], "cubes": [{"origin": [-4, 16, -4], "size": [8, 8, 8], "uv": [0, 32]}]},
            ],
        }],
    })


def animations():
    write_json(A + "geckolib/animations/entity/spike_dummy.animation.json", {
        "format_version": "1.8.0",
        "animations": {
            "idle": {"loop": True, "animation_length": 2.0, "bones": {
                "body": {"position": {"0.0": [0, 0, 0], "1.0": [0, 1, 0], "2.0": [0, 0, 0]}},
                "head": {"rotation": {"0.0": [0, -12, 0], "1.0": [0, 12, 0], "2.0": [0, -12, 0]}},
            }},
            # one second: rear back, lunge forward with the head snapping down, settle
            "attack": {"loop": False, "animation_length": 1.0, "bones": {
                "body": {"rotation": {"0.0": [0, 0, 0], "0.25": [-25, 0, 0], "0.5": [35, 0, 0], "1.0": [0, 0, 0]},
                         "position": {"0.0": [0, 0, 0], "0.25": [0, 2, 4], "0.5": [0, 0, -8], "1.0": [0, 0, 0]}},
                "head": {"rotation": {"0.0": [0, 0, 0], "0.3": [-45, 0, 0], "0.6": [40, 0, 0], "1.0": [0, 0, 0]}},
            }},
        },
    })


def texture():
    teal, dark_teal = (42, 157, 143, 255), (30, 110, 100, 255)
    orange, dark_orange = (231, 111, 81, 255), (170, 70, 50, 255)
    eye = (20, 20, 30, 255)
    clear = (0, 0, 0, 0)
    px = [[clear for _ in range(64)] for _ in range(64)]
    # body box uv at (0,0): 64x32; head box uv at (0,32): 32x16
    for y in range(0, 32):
        for x in range(0, 64):
            px[y][x] = dark_teal if (x % 16 in (0, 15) or y % 16 in (0, 15)) else teal
    for y in range(32, 48):
        for x in range(0, 32):
            px[y][x] = dark_orange if (x % 8 in (0, 7) or y % 8 in (0, 7)) else orange
    # eyes on the head's front face (x 8..16, y 40..48 in box layout)
    for (x, y) in ((10, 43), (13, 43), (10, 44), (13, 44)):
        px[y][x] = eye
    write_png(A + "textures/entity/spike_dummy.png", 64, 64, px)


if __name__ == "__main__":
    geometry()
    animations()
    texture()
