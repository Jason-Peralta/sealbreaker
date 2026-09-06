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
    """Game-test arenas: a stone floor with air above it. 16x9x16 for most tests, 48x12x48 for structure assembly."""
    palette = [("minecraft:stone", None)]
    write("sb_core/src/gametest/resources/data/sb_core_tests/structure/arena.nbt",
          template((16, 9, 16), box(0, 0, 0, 15, 0, 15, 0), palette))
    write("sb_core/src/gametest/resources/data/sb_core_tests/structure/arena_large.nbt",
          template((48, 12, 48), box(0, 0, 0, 47, 0, 47, 0), palette))


def spike_structure():
    """S5: three jigsaw pieces. Stone brick shells joined by jigsaw blocks set into the floor, a locked door on the room.

    Pieces (x is width, y height, z depth; every piece is entered from -z and continues toward +z):
      entrance  7x6x5  open on its front, a doorway in its back wall
      corridor  5x5x7  a passage with a doorway at both ends
      room      9x6x9  a closed room whose only way in is the locked door on its front wall

    Jigsaw blocks sit in the floor under each doorway (final_state puts the floor back), so the connected
    pieces' doorways line up: a jigsaw facing +z (south_up) meets the next piece's jigsaw facing -z (north_up).
    The door's block entity data (its key) rides in the template as block NBT, which is what the spike checks.
    """
    stone = ("minecraft:stone_bricks", None)
    air = ("minecraft:air", None)
    jigsaw_south = ("minecraft:jigsaw", {"orientation": "south_up"})
    jigsaw_north = ("minecraft:jigsaw", {"orientation": "north_up"})
    door_lower = ("sb_world:locked_door", {"facing": "north", "half": "lower", "hinge": "left", "open": "false", "powered": "false"})
    door_upper = ("sb_world:locked_door", {"facing": "north", "half": "upper", "hinge": "left", "open": "false", "powered": "false"})
    STONE, AIR, JIGSAW_SOUTH, JIGSAW_NORTH, DOOR_LOWER, DOOR_UPPER = range(6)
    palette = [stone, air, jigsaw_south, jigsaw_north, door_lower, door_upper]

    def shell(w, h, d):
        blocks = []
        for x in range(w):
            for y in range(h):
                for z in range(d):
                    edge = x in (0, w - 1) or y in (0, h - 1) or z in (0, d - 1)
                    blocks.append((x, y, z, STONE if edge else AIR))
        return blocks

    def without(blocks, pred):
        return [b for b in blocks if not pred(*b[:3])]

    def jigsaw(x, y, z, index, name, target, pool, final_state="minecraft:stone_bricks"):
        return (x, y, z, index, {"name": name, "target": target, "pool": pool, "final_state": final_state, "joint": "rollable"})

    # entrance: no front wall (an open porch), a doorway in the back wall, the jigsaw under it pointing +z
    blocks = shell(7, 6, 5)
    blocks = without(blocks, lambda x, y, z: z == 0 and 0 < x < 6 and 0 < y < 5)
    blocks = without(blocks, lambda x, y, z: x == 3 and y in (1, 2) and z == 4)
    blocks = without(blocks, lambda x, y, z: (x, y, z) == (3, 0, 4))
    blocks.append(jigsaw(3, 0, 4, JIGSAW_SOUTH, "sb_world:spike/entrance_back", "sb_world:spike/corridor_front", "sb_world:spike/corridor"))
    write("sb_world/src/main/resources/data/sb_world/structure/spike/entrance.nbt", template((7, 6, 5), blocks, palette))

    # corridor: doorways at both ends; the far jigsaw asks the room pool for the next piece
    blocks = shell(5, 5, 7)
    blocks = without(blocks, lambda x, y, z: x == 2 and y in (1, 2) and z in (0, 6))
    blocks = without(blocks, lambda x, y, z: x == 2 and y == 0 and z in (0, 6))
    blocks.append(jigsaw(2, 0, 0, JIGSAW_NORTH, "sb_world:spike/corridor_front", "sb_world:spike/entrance_back", "minecraft:empty"))
    blocks.append(jigsaw(2, 0, 6, JIGSAW_SOUTH, "sb_world:spike/corridor_back", "sb_world:spike/room_front", "sb_world:spike/room"))
    write("sb_world/src/main/resources/data/sb_world/structure/spike/corridor.nbt", template((5, 5, 7), blocks, palette))

    # room: closed, the locked door in the middle of the front wall with the jigsaw in the floor under it
    blocks = shell(9, 6, 9)
    blocks = without(blocks, lambda x, y, z: x == 4 and y in (0, 1, 2) and z == 0)
    blocks.append(jigsaw(4, 0, 0, JIGSAW_NORTH, "sb_world:spike/room_front", "sb_world:spike/corridor_back", "minecraft:empty"))
    blocks.append((4, 1, 0, DOOR_LOWER, {"key": "sb_world:spike_key"}))
    blocks.append((4, 2, 0, DOOR_UPPER))
    write("sb_world/src/main/resources/data/sb_world/structure/spike/room.nbt", template((9, 6, 9), blocks, palette))


if __name__ == "__main__":
    import sys
    which = sys.argv[1] if len(sys.argv) > 1 else "all"
    if which in ("all", "arena"):
        arena()
    if which in ("all", "spike"):
        spike_structure()
