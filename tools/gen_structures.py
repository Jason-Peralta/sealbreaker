"""Generates the structure templates (.nbt) that are simpler to describe in code than to build with structure
blocks: the game-test arena and the S5 spike's structure pieces. Run from the repository root.

A template is a box of block states; positions not listed keep whatever the world had (the game test framework
clears its area first, so an arena only needs its floor). DataVersion is the 26.2 world data version.
"""
import os

import nbt

DATA_VERSION = 4903  # Minecraft 26.2 (read from a level.dat; bump when the game version bumps)


def template(size, blocks, palette, entities=()):
    """blocks: list of (x, y, z, palette_index[, nbt_compound]); palette: list of (name, properties dict)."""
    states = nbt.List([{"Name": name, **({"Properties": props} if props else {})} for name, props in palette], nbt.TAG_COMPOUND)
    placed = nbt.List(element_tag=nbt.TAG_COMPOUND)
    for entry in blocks:
        x, y, z, index = entry[:4]
        block = {"pos": nbt.List([nbt.Int(x), nbt.Int(y), nbt.Int(z)], nbt.TAG_INT), "state": nbt.Int(index)}
        if len(entry) > 4 and entry[4]:
            block["nbt"] = entry[4]
        placed.append(block)
    return {
        "size": nbt.List([nbt.Int(size[0]), nbt.Int(size[1]), nbt.Int(size[2])], nbt.TAG_INT),
        "palette": states,
        "blocks": placed,
        "entities": nbt.List(list(entities), nbt.TAG_COMPOUND),
        "DataVersion": nbt.Int(DATA_VERSION),
    }


def box(x0, y0, z0, x1, y1, z1, index):
    return [(x, y, z, index) for x in range(x0, x1 + 1) for y in range(y0, y1 + 1) for z in range(z0, z1 + 1)]


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    nbt.write(path, data)
    print("wrote", path)


def arena():
    """A 16x9x16 game-test arena: a stone floor with 8 blocks of air above it."""
    palette = [("minecraft:stone", None)]
    write("sb_core/src/gametest/resources/data/sb_core_tests/structure/arena.nbt",
          template((16, 9, 16), box(0, 0, 0, 15, 0, 15, 0), palette))


def spike_structure():
    """S5: three jigsaw pieces. Stone brick shells with jigsaw blocks joining them and a locked door in the room.

    Pieces (x is width, y height, z depth; the entrance faces -z):
      entrance  7x6x5  an open front, a jigsaw on the back wall leading to the corridor
      corridor  5x5x7  a passage with jigsaws at both ends
      room      9x6x9  a closed room whose only way in is the locked door on its front wall
    """
    stone = ("minecraft:stone_bricks", None)
    cracked = ("minecraft:cracked_stone_bricks", None)
    air = ("minecraft:air", None)
    jigsaw = ("minecraft:jigsaw", {"orientation": "north_up"})
    jigsaw_south = ("minecraft:jigsaw", {"orientation": "south_up"})
    door_lower = ("sb_world:locked_door", {"half": "lower", "facing": "north", "open": "false"})
    door_upper = ("sb_world:locked_door", {"half": "upper", "facing": "north", "open": "false"})

    def shell(w, h, d, palette_index):
        blocks = []
        for x in range(w):
            for y in range(h):
                for z in range(d):
                    edge = x in (0, w - 1) or y in (0, h - 1) or z in (0, d - 1)
                    blocks.append((x, y, z, palette_index if edge else 1))
        return blocks

    def jigsaw_nbt(name, target, pool, final_state, joint="rollable"):
        return {"name": name, "target": target, "pool": pool, "final_state": final_state, "joint": joint}

    # entrance: open on the -z face (remove that wall), jigsaw on the +z wall pointing south into the corridor
    palette = [stone, air, cracked, jigsaw_south, jigsaw]
    blocks = [b for b in shell(7, 6, 5, 0) if not (b[2] == 0 and 0 < b[0] < 6 and 0 < b[1] < 5)]
    blocks = [b for b in blocks if not (b[0] == 3 and b[1] in (1, 2) and b[2] == 4)]
    blocks.append((3, 1, 4, 3, jigsaw_nbt("sb_world:entrance_back", "sb_world:corridor_front", "sb_world:spike/corridor", "minecraft:air")))
    write("sb_world/src/main/resources/data/sb_world/structure/spike/entrance.nbt", template((7, 6, 5), blocks, palette))

    # corridor: jigsaws on both ends, the far one accepts either the room or another corridor
    blocks = shell(5, 5, 7, 0)
    blocks = [b for b in blocks if not (b[0] == 2 and b[1] in (1, 2) and b[2] in (0, 6))]
    blocks.append((2, 1, 0, 4, jigsaw_nbt("sb_world:corridor_front", "sb_world:entrance_back", "minecraft:empty", "minecraft:air")))
    blocks.append((2, 1, 6, 3, jigsaw_nbt("sb_world:corridor_back", "sb_world:room_front", "sb_world:spike/room", "minecraft:air")))
    write("sb_world/src/main/resources/data/sb_world/structure/spike/corridor.nbt", template((5, 5, 7), blocks, palette))

    # room: closed, a locked door in the middle of the front wall, a jigsaw beside it to join the corridor
    palette = [stone, air, cracked, jigsaw_south, jigsaw, door_lower, door_upper]
    blocks = [b for b in shell(9, 6, 9, 0) if not (b[0] == 4 and b[1] in (1, 2) and b[2] == 0)]
    blocks.append((4, 1, 0, 5, {"key": "sb_world:spike_key"}))
    blocks.append((4, 2, 0, 6))
    blocks = [b for b in blocks if not (b[0] == 3 and b[1] == 1 and b[2] == 0)]
    blocks.append((3, 1, 0, 4, jigsaw_nbt("sb_world:room_front", "sb_world:corridor_back", "minecraft:empty", "minecraft:stone_bricks")))
    write("sb_world/src/main/resources/data/sb_world/structure/spike/room.nbt", template((9, 6, 9), blocks, palette))


if __name__ == "__main__":
    import sys
    which = sys.argv[1] if len(sys.argv) > 1 else "all"
    if which in ("all", "arena"):
        arena()
    if which in ("all", "spike"):
        spike_structure()
