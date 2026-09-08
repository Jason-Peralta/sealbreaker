package dev.sealbreaker.combat.gametest;

import dev.sealbreaker.combat.SbCombat;
import dev.sealbreaker.combat.swing.ArcHitTest;
import dev.sealbreaker.combat.swing.SwingService;
import dev.sealbreaker.core.api.combat.SwingMove;
import dev.sealbreaker.core.api.combat.WeaponArchetype;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.Consumer;

/** Game tests for the combat module; instances live in {@code data/sb_combat_tests/test_instance/}, on the shared arena. */
public final class SbCombatTestFunctions {
    public static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, SbCombatTests.MOD_ID);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> INPUT_CONTEXTS =
            TEST_FUNCTIONS.register("input_contexts", () -> InputGameTests::contexts);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> INPUT_CHARGE =
            TEST_FUNCTIONS.register("input_charge", () -> InputGameTests::charge);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> INPUT_BUFFERING =
            TEST_FUNCTIONS.register("input_buffering", () -> InputGameTests::buffering);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> INPUT_CANCELLATION =
            TEST_FUNCTIONS.register("input_cancellation", () -> InputGameTests::cancellation);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SWORD_CONTEXTS =
            TEST_FUNCTIONS.register("sword_contexts", () -> SwordMoveGameTests::contexts);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SWORD_PLUNGE =
            TEST_FUNCTIONS.register("sword_plunge", () -> SwordMoveGameTests::plunge);

    /** The sword's JSON loads, its sweeps travel left-to-right then right-to-left, and only the front pig is hit. */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ARC_TARGETING =
            TEST_FUNCTIONS.register("arc_targeting", () -> SbCombatTestFunctions::arcTargeting);

    private static void arcTargeting(GameTestHelper helper) {
        // The archetype must come from the datapack registry: proves the JSON in data/sb_combat/sb/weapon_archetype loaded.
        WeaponArchetype sword = SwingService.lookup(helper.getLevel(), Identifier.fromNamespaceAndPath(SbCombat.MOD_ID, "sword"))
                .orElse(null);
        helper.assertTrue(sword != null, "sb_combat:sword must be loaded from data/sb_combat/sb/weapon_archetype/sword.json");
        helper.assertTrue(sword.tap().size() == 3, "the sword must have a three-move tap combo");
        SwingMove sweepLtr = sword.move(0);
        SwingMove sweepRtl = sword.move(1);
        SwingMove thrust = sword.move(2);
        helper.assertTrue(sweepLtr.direction() == SwingMove.Direction.LEFT_TO_RIGHT
                && sweepRtl.direction() == SwingMove.Direction.RIGHT_TO_LEFT
                && thrust.shape() == SwingMove.Shape.THRUST, "move order must be sweep L-R, sweep R-L, thrust");
        helper.assertTrue(thrust.damageMultiplier() > sweepLtr.damageMultiplier(), "the thrust must hit harder than the sweeps");
        helper.assertTrue(thrust.reach() > sweepLtr.reach() && thrust.arcDegrees() < sweepLtr.arcDegrees(), "the thrust reaches further in a narrower lane");

        Player attacker = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 origin = helper.absoluteVec(new Vec3(7.5, 1.0, 3.5));
        attacker.setPos(origin);
        Vec3 eyes = origin.add(0.0, attacker.getEyeHeight(), 0.0);
        Vec3 lookPlusZ = new Vec3(0.0, 0.0, 1.0); // facing south: the attacker's left is +X

        Pig front = helper.spawn(EntityTypes.PIG, new Vec3(7.5, 1.0, 5.5));
        Pig back = helper.spawn(EntityTypes.PIG, new Vec3(7.5, 1.0, 1.5));
        Pig left = helper.spawn(EntityTypes.PIG, new Vec3(9.3, 1.0, 4.6));  // about 60 degrees to the left
        Pig right = helper.spawn(EntityTypes.PIG, new Vec3(5.7, 1.0, 4.6)); // about 60 degrees to the right
        for (Pig pig : List.of(front, back, left, right)) {
            pig.setNoAi(true);
        }

        helper.assertTrue(ArcHitTest.isInsideArc(eyes, lookPlusZ, front, sweepLtr), "the pig in front must be inside the arc");
        helper.assertFalse(ArcHitTest.isInsideArc(eyes, lookPlusZ, back, sweepLtr), "the pig behind must be outside the arc");
        helper.assertTrue(ArcHitTest.signedAngleDegrees(eyes, lookPlusZ, left) > 0.0, "a pig at +X is on the left when facing +Z");
        // A left-to-right sweep reaches the left pig on its first active tick and the right pig only on its last.
        helper.assertTrue(ArcHitTest.isInsideMoveAt(eyes, lookPlusZ, left, sweepLtr, 0), "left pig is hit at the start of a left-to-right sweep");
        helper.assertFalse(ArcHitTest.isInsideMoveAt(eyes, lookPlusZ, right, sweepLtr, 0), "right pig is not hit at the start of a left-to-right sweep");
        helper.assertTrue(ArcHitTest.isInsideMoveAt(eyes, lookPlusZ, right, sweepLtr, sweepLtr.activeTicks() - 1), "right pig is hit at the end of a left-to-right sweep");
        helper.assertTrue(ArcHitTest.isInsideMoveAt(eyes, lookPlusZ, right, sweepRtl, 0), "right pig is hit at the start of a right-to-left sweep");
        helper.assertFalse(ArcHitTest.isInsideMoveAt(eyes, lookPlusZ, left, thrust, 0), "a pig 60 degrees off is outside the thrust's lane");
        helper.assertTrue(ArcHitTest.isInsideMoveAt(eyes, lookPlusZ, front, thrust, 0), "the pig in front is inside the thrust's lane");
        // The hit region follows the look pitch: a pig four blocks up is out of a level thrust and inside one aimed at it.
        Pig high = helper.spawn(EntityTypes.PIG, new Vec3(7.5, 5.0, 5.5));
        high.setNoAi(true);
        Vec3 lookUp = new Vec3(0.0, 1.0, 1.0).normalize();
        helper.assertFalse(ArcHitTest.isInsideMoveAt(eyes, lookPlusZ, high, thrust, 0), "a pig four blocks up is outside a level thrust");
        helper.assertTrue(ArcHitTest.isInsideMoveAt(eyes, lookUp, high, thrust, 0), "the same pig is inside a thrust aimed up at it");

        // Now that the arena keeps every entity inside loaded chunks, the real query must agree with the geometry:
        // the first active tick of a left-to-right sweep reaches the front and left pigs, the last the front and right.
        List<LivingEntity> first = ArcHitTest.findTargets(helper.getLevel(), attacker, eyes, lookPlusZ, sweepLtr, 0);
        helper.assertTrue(first.contains(front) && first.contains(left) && !first.contains(right) && !first.contains(back) && !first.contains(high),
                "first active tick of the sweep must find exactly the front and left pigs, found " + first.size());
        List<LivingEntity> last = ArcHitTest.findTargets(helper.getLevel(), attacker, eyes, lookPlusZ, sweepLtr, sweepLtr.activeTicks() - 1);
        helper.assertTrue(last.contains(front) && last.contains(right) && !last.contains(left),
                "last active tick of the sweep must find the front and right pigs");

        float before = front.getHealth();
        boolean hurt = front.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(attacker), 2.0f);
        helper.assertTrue(hurt, "the front pig must accept damage from the attacker");
        helper.assertTrue(front.getHealth() < before, "the front pig must have lost health");
        helper.assertTrue(back.getHealth() == back.getMaxHealth(), "the pig behind must be untouched");
        helper.succeed();
    }

    public static void register(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    private SbCombatTestFunctions() {
    }
}
