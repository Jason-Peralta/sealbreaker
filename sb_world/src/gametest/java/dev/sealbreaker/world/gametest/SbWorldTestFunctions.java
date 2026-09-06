package dev.sealbreaker.world.gametest;

import dev.sealbreaker.world.SbWorld;
import dev.sealbreaker.world.block.LockedDoorBlock;
import dev.sealbreaker.world.block.LockedDoorBlockEntity;
import dev.sealbreaker.world.block.SbWorldBlocks;
import dev.sealbreaker.world.block.SpikePortalBlock;
import dev.sealbreaker.world.item.SbWorldItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.timeline.Timeline;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Game tests for the world module; instances live in {@code data/sb_world_tests/test_instance/}. */
public final class SbWorldTestFunctions {
    public static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, SbWorldTests.MOD_ID);

    private static final Identifier SPIKE_KEY = Identifier.fromNamespaceAndPath(SbWorld.MOD_ID, "spike_key");

    /** The room template places its locked door with the key from the template's block NBT, and only that key opens it. */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> LOCKED_DOOR_TEMPLATE =
            TEST_FUNCTIONS.register("locked_door_template", () -> SbWorldTestFunctions::lockedDoorTemplate);

    /** The real jigsaw pipeline assembles entrance, corridor and room, runs the processor list, and keeps the door's key. */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> JIGSAW_ASSEMBLY =
            TEST_FUNCTIONS.register("jigsaw_assembly", () -> SbWorldTestFunctions::jigsawAssembly);

    /**
     * The realm's data loads: its dimension type points at its own clock and timeline set, the timeline has a 12000-tick
     * day. The realm itself cannot exist here: the game test server bakes no datapack dimensions, so the portal
     * must report the missing level instead of teleporting or crashing.
     */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> REALM_DATA =
            TEST_FUNCTIONS.register("realm_data", () -> SbWorldTestFunctions::realmData);

    private static void realmData(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Identifier realm = Identifier.fromNamespaceAndPath(SbWorld.MOD_ID, "spike_realm");
        Holder<DimensionType> type = level.registryAccess().lookupOrThrow(Registries.DIMENSION_TYPE)
                .get(ResourceKey.create(Registries.DIMENSION_TYPE, realm)).orElse(null);
        helper.assertTrue(type != null, "the sb_world:spike_realm dimension type must load from data");
        Holder<WorldClock> clock = level.registryAccess().lookupOrThrow(Registries.WORLD_CLOCK)
                .get(ResourceKey.create(Registries.WORLD_CLOCK, realm)).orElse(null);
        helper.assertTrue(clock != null, "the sb_world:spike_realm world clock must load from data");
        helper.assertTrue(type.value().defaultClock().map(clock::equals).orElse(false), "the realm must run on its own clock");
        Holder<Timeline> day = level.registryAccess().lookupOrThrow(Registries.TIMELINE)
                .get(ResourceKey.create(Registries.TIMELINE, Identifier.fromNamespaceAndPath(SbWorld.MOD_ID, "spike_realm_day"))).orElse(null);
        helper.assertTrue(day != null, "the sb_world:spike_realm_day timeline must load from data");
        helper.assertTrue(day.value().clock().equals(clock) && day.value().periodTicks().orElse(0) == 12000,
                "the realm's day runs on the realm's clock with a 12000-tick period");
        helper.assertTrue(type.value().timelines().contains(day), "the dimension type must list the realm's day timeline");
        helper.assertTrue(level.getServer().getLevel(SpikePortalBlock.REALM) == null,
                "the game test server bakes no datapack dimensions; if this ever changes, extend this test to a real round trip");

        helper.setBlock(new BlockPos(7, 1, 7), SbWorldBlocks.SPIKE_PORTAL.get());
        Pig pig = helper.spawn(EntityTypes.PIG, new BlockPos(7, 1, 7));
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(pig.isAlive() && pig.level() == level, "without the realm the portal must leave the pig where it is");
            helper.succeed();
        });
    }

    private static void lockedDoorTemplate(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        StructureTemplate room = level.getStructureManager().get(Identifier.fromNamespaceAndPath(SbWorld.MOD_ID, "spike/room")).orElse(null);
        helper.assertTrue(room != null, "sb_world:spike/room must load from data/sb_world/structure/spike/room.nbt");
        BlockPos origin = helper.absolutePos(new BlockPos(3, 1, 3));
        helper.assertTrue(room.placeInWorld(level, origin, origin, new StructurePlaceSettings(), level.getRandom(), Block.UPDATE_ALL),
                "the room template must place");

        BlockPos door = new BlockPos(3 + 4, 1 + 1, 3);
        LockedDoorBlockEntity lock = helper.getBlockEntity(door, LockedDoorBlockEntity.class);
        helper.assertTrue(SPIKE_KEY.equals(lock.key()), "the door's key must survive template placement, got " + lock.key());
        helper.assertTrue(!lock.isUnlocked(), "a freshly placed door is locked");
        BlockState lower = helper.getBlockState(door);
        helper.assertTrue(lower.getBlock() == SbWorldBlocks.LOCKED_DOOR.get() && lower.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER
                && helper.getBlockState(door.above()).getBlock() == SbWorldBlocks.LOCKED_DOOR.get(), "both halves of the door must be placed");

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.useBlock(door, player);
        helper.assertTrue(!helper.getBlockState(door).getValue(DoorBlock.OPEN), "an empty hand must not open a locked door");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Blocks.STONE));
        helper.useBlock(door, player);
        helper.assertTrue(!helper.getBlockState(door).getValue(DoorBlock.OPEN), "the wrong item must not open a locked door");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(SbWorldItems.SPIKE_KEY.get()));
        helper.useBlock(door.above(), player);
        helper.assertTrue(helper.getBlockState(door).getValue(DoorBlock.OPEN) && helper.getBlockState(door.above()).getValue(DoorBlock.OPEN),
                "the key must open both halves, from either half");
        helper.assertTrue(lock.isUnlocked(), "using the key unlocks the door for good");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.useBlock(door, player);
        helper.assertTrue(!helper.getBlockState(door).getValue(DoorBlock.OPEN), "an unlocked door toggles by hand");
        helper.succeed();
    }

    private static void jigsawAssembly(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Holder<StructureTemplatePool> entrance = level.registryAccess().lookupOrThrow(Registries.TEMPLATE_POOL)
                .get(ResourceKey.create(Registries.TEMPLATE_POOL, Identifier.fromNamespaceAndPath(SbWorld.MOD_ID, "spike/entrance")))
                .orElse(null);
        helper.assertTrue(entrance != null, "the sb_world:spike/entrance template pool must be registered from data");
        // Anchor the entrance's back jigsaw at the arena's centre; the structure grows 21 blocks in a random direction.
        BlockPos anchor = helper.absolutePos(new BlockPos(24, 1, 24));
        boolean placed = JigsawPlacement.generateJigsaw(level, entrance, Identifier.fromNamespaceAndPath(SbWorld.MOD_ID, "spike/entrance_back"), 4, anchor, false);
        helper.assertTrue(placed, "the jigsaw pipeline must assemble the spike structure from its pools");

        List<BlockPos> doors = new ArrayList<>();
        int[] cracked = {0};
        int[] bricks = {0};
        helper.forEveryBlockInStructure(pos -> {
            BlockState state = helper.getBlockState(pos);
            if (state.getBlock() == SbWorldBlocks.LOCKED_DOOR.get() && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER) {
                doors.add(pos.immutable());
            } else if (state.is(Blocks.CRACKED_STONE_BRICKS)) {
                cracked[0]++;
            } else if (state.is(Blocks.STONE_BRICKS)) {
                bricks[0]++;
            } else if (state.is(Blocks.JIGSAW)) {
                helper.fail(Component.literal("jigsaw blocks must be replaced by their final state"), pos);
            }
        });
        helper.assertTrue(doors.size() == 1, "exactly one locked door must be placed (the room's), found " + doors.size());
        BlockPos door = doors.getFirst();
        LockedDoorBlockEntity lock = helper.getBlockEntity(door, LockedDoorBlockEntity.class);
        helper.assertTrue(SPIKE_KEY.equals(lock.key()), "the door's key must survive jigsaw placement, got " + lock.key());
        helper.assertTrue(cracked[0] > 0 && bricks[0] > cracked[0], "the processor list must crack some, not all, stone bricks: "
                + cracked[0] + " cracked of " + (cracked[0] + bricks[0]));
        // The room is 8 blocks past the corridor's far doorway, so 8 blocks in front of the door there is the entrance's doorway (air).
        BlockState state = helper.getBlockState(door);
        BlockPos entranceDoorway = door.relative(state.getValue(DoorBlock.FACING), 8);
        helper.assertBlock(entranceDoorway, block -> block == Blocks.AIR, block -> Component.literal(
                "the corridor must join the entrance's doorway 8 blocks in front of the door, found " + block));
        helper.assertTrue(LockedDoorBlock.lock(level, helper.absolutePos(door.above()), helper.getBlockState(door.above())) == lock,
                "the upper half must find the lower half's lock");
        helper.succeed();
    }

    public static void register(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    private SbWorldTestFunctions() {
    }
}
