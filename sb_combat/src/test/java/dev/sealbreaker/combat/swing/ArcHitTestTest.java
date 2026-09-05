package dev.sealbreaker.combat.swing;

import dev.sealbreaker.core.api.combat.SwingMove;
import dev.sealbreaker.core.api.combat.WeaponArchetype;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pure-geometry and timing checks for moves, sweeps and combos. */
class ArcHitTestTest {
    private static final SwingMove SWEEP_LTR = new SwingMove(SwingMove.Shape.SWEEP, SwingMove.Direction.LEFT_TO_RIGHT,
            3.0, 150.0f, 2.0, 2, 3, 3, 0.7f, 0.3f);
    private static final SwingMove SWEEP_RTL = new SwingMove(SwingMove.Shape.SWEEP, SwingMove.Direction.RIGHT_TO_LEFT,
            3.0, 150.0f, 2.0, 2, 3, 3, 0.7f, 0.3f);
    private static final SwingMove OVERHEAD = new SwingMove(SwingMove.Shape.OVERHEAD, SwingMove.Direction.NONE,
            3.2, 50.0f, 2.5, 3, 2, 6, 1.4f, 0.7f);
    private static final SwingMove THRUST = new SwingMove(SwingMove.Shape.THRUST, SwingMove.Direction.NONE,
            3.4, 30.0f, 1.5, 8, 3, 7, 1.4f, 0.7f);
    private static final WeaponArchetype SWORD = new WeaponArchetype(List.of(SWEEP_LTR, SWEEP_RTL, THRUST), 12);

    @Test
    void timingWindows() {
        assertFalse(SWEEP_LTR.isActiveAt(1), "last windup tick is not active");
        assertTrue(SWEEP_LTR.isActiveAt(2), "first active tick");
        assertEquals(0, SWEEP_LTR.activeIndexAt(2));
        assertEquals(2, SWEEP_LTR.activeIndexAt(4));
        assertEquals(-1, SWEEP_LTR.activeIndexAt(5), "recovery has no active index");
        assertTrue(SWEEP_LTR.isInRecoveryAt(5));
        assertFalse(SWEEP_LTR.isFinishedAt(7));
        assertTrue(SWEEP_LTR.isFinishedAt(8), "finished after windup + active + recovery");
        assertEquals(8, SWEEP_LTR.totalTicks());
        assertEquals(11, OVERHEAD.totalTicks());
        assertEquals(18, THRUST.totalTicks());
    }

    @Test
    void comboStepsWrapAndWeightTheFinisher() {
        assertEquals(1, SWORD.nextStep(0));
        assertEquals(2, SWORD.nextStep(1));
        assertEquals(0, SWORD.nextStep(2), "the combo restarts after the thrust");
        assertTrue(SWORD.move(2).damageMultiplier() > SWORD.move(0).damageMultiplier(), "the finisher hits harder");
        assertEquals(SWORD.move(0).damageMultiplier(), SWORD.move(1).damageMultiplier(), "the two sweeps are equal");
    }

    @Test
    void leftToRightSweepStartsOnTheLeft() {
        double[] first = ArcHitTest.sweepWindow(SWEEP_LTR, 0);
        double[] last = ArcHitTest.sweepWindow(SWEEP_LTR, 2);
        assertTrue(first[1] > 60.0 && first[0] > 0.0, "first window covers the left edge (positive angles)");
        assertTrue(last[0] < -60.0 && last[1] < 0.0, "last window covers the right edge (negative angles)");
        double[] rtlFirst = ArcHitTest.sweepWindow(SWEEP_RTL, 0);
        assertTrue(rtlFirst[0] < -60.0, "a right-to-left sweep starts on the right");
    }

    @Test
    void sweepWindowsCoverTheWholeArcWithoutGaps() {
        double half = SWEEP_LTR.arcDegrees() * 0.5;
        for (double angle = -half; angle <= half; angle += 1.0) {
            boolean covered = false;
            for (int k = 0; k < SWEEP_LTR.activeTicks(); k++) {
                double[] w = ArcHitTest.sweepWindow(SWEEP_LTR, k);
                if (angle >= w[0] && angle <= w[1]) {
                    covered = true;
                    break;
                }
            }
            assertTrue(covered, "angle " + angle + " must be hit by some active tick");
        }
    }

    @Test
    void horizontalIgnoresPitch() {
        Vec3 flat = ArcHitTest.horizontal(new Vec3(0.0, -0.9, 0.5));
        assertEquals(0.0, flat.x, 1e-9);
        assertEquals(0.0, flat.y, 1e-9);
        assertEquals(1.0, flat.z, 1e-9);
        assertNull(ArcHitTest.horizontal(new Vec3(0.0, 1.0, 0.0)), "looking straight up has no horizontal direction");
    }

    @Test
    void yawConventionMatchesMinecraft() {
        Vec3 south = ArcHitTest.horizontalFromYaw(0.0f);
        assertEquals(1.0, south.z, 1e-6, "yaw 0 looks toward +Z (south)");
        Vec3 west = ArcHitTest.horizontalFromYaw(90.0f);
        assertEquals(-1.0, west.x, 1e-6, "yaw 90 looks toward -X (west)");
    }
}
