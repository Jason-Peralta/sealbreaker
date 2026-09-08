package dev.sealbreaker.combat.swing;

import dev.sealbreaker.core.api.combat.SwingMove;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure geometry of a move: which living entities are inside its hit region on a given active tick.
 * No side effects, no networking, so it is unit-testable and shared by players, enemies and traps.
 *
 * <p>Angles are signed from the attacker's forward direction: positive is to the attacker's left,
 * negative to the right. A sweep's live window travels across the arc over the active ticks, so the hit
 * region follows the blade rather than covering the whole arc at once.
 */
public final class ArcHitTest {
    /** Extra width of a sweep's live window on each side, so fast blades leave no gaps between ticks. */
    private static final double SWEEP_MARGIN_DEGREES = 12.0;

    /** Every living, alive entity other than the attacker inside the move's hit region on this active tick. */
    public static List<LivingEntity> findTargets(Level level, Entity attacker, Vec3 origin, Vec3 look, SwingMove move, int activeIndex) {
        if (move.shape() != SwingMove.Shape.PLUNGE && horizontal(look) == null) {
            return List.of();
        }
        double reach = move.reach();
        AABB search = new AABB(origin, origin).inflate(reach + 1.0, reach + move.verticalReach() + 1.0, reach + 1.0);
        List<LivingEntity> hits = new ArrayList<>();
        for (Entity candidate : level.getEntities(attacker, search, e -> e instanceof LivingEntity living && living.isAlive() && e != attacker)) {
            LivingEntity living = (LivingEntity) candidate;
            if (isInsideMoveAt(origin, look, living, move, activeIndex)) {
                hits.add(living);
            }
        }
        return hits;
    }

    /**
     * Whether a target is inside the move's live region on the given active tick. {@code look} is the
     * attacker's look direction: the arc is measured around its horizontal part, and the region's height
     * follows its pitch, so a thrust aimed down at a target under the eye line reaches it.
     */
    public static boolean isInsideMoveAt(Vec3 origin, Vec3 look, LivingEntity target, SwingMove move, int activeIndex) {
        if (move.shape() == SwingMove.Shape.PLUNGE) {
            return isInsidePlunge(origin, target, move);
        }
        Vec3 forward = horizontal(look);
        if (forward == null || !isWithinReach(origin, look, target, move)) {
            return false;
        }
        double angle = signedAngleDegrees(origin, forward, target);
        double half = move.arcDegrees() * 0.5;
        if (Math.abs(angle) > half + halfWidthAllowance(target, origin)) {
            return false;
        }
        if (move.shape() != SwingMove.Shape.SWEEP || move.direction() == SwingMove.Direction.NONE) {
            return true;
        }
        double[] window = sweepWindow(move, activeIndex);
        double allowance = halfWidthAllowance(target, origin);
        return angle >= window[0] - allowance && angle <= window[1] + allowance;
    }

    /** Whether a target is anywhere inside the move's arc, ignoring sweep timing. */
    public static boolean isInsideArc(Vec3 origin, Vec3 look, LivingEntity target, SwingMove move) {
        if (move.shape() == SwingMove.Shape.PLUNGE) {
            return isInsidePlunge(origin, target, move);
        }
        Vec3 forward = horizontal(look);
        if (forward == null || !isWithinReach(origin, look, target, move)) {
            return false;
        }
        double angle = signedAngleDegrees(origin, forward, target);
        return Math.abs(angle) <= move.arcDegrees() * 0.5 + halfWidthAllowance(target, origin);
    }

    /** A cone directed downward, independent of look yaw/pitch, with allowance for target size. */
    private static boolean isInsidePlunge(Vec3 origin, LivingEntity target, SwingMove move) {
        Vec3 centre = target.position().add(0, target.getBbHeight() * 0.5, 0);
        double depth = origin.y - centre.y;
        if (depth < 0 || depth > move.reach() + target.getBbHeight() * 0.5) {
            return false;
        }
        double radius = depth * Math.tan(Math.toRadians(move.arcDegrees() * 0.5));
        return Math.hypot(centre.x - origin.x, centre.z - origin.z) <= radius + target.getBbWidth() * 0.5;
    }

    /**
     * The live angular window {min, max} (degrees, positive = left) of a sweep on the given active tick.
     * A left-to-right sweep starts at the left edge of the arc and ends at the right edge.
     */
    public static double[] sweepWindow(SwingMove move, int activeIndex) {
        int n = Math.max(1, move.activeTicks());
        int k = Mth.clamp(activeIndex, 0, n - 1);
        double half = move.arcDegrees() * 0.5;
        double slice = move.arcDegrees() / n;
        double from;
        double to;
        if (move.direction() == SwingMove.Direction.LEFT_TO_RIGHT) {
            from = half - (k + 1) * slice;
            to = half - k * slice;
        } else {
            from = -half + k * slice;
            to = -half + (k + 1) * slice;
        }
        return new double[]{from - SWEEP_MARGIN_DEGREES, to + SWEEP_MARGIN_DEGREES};
    }

    /** Signed horizontal angle from the look direction to the target's centre, in degrees; positive is the attacker's left. */
    public static double signedAngleDegrees(Vec3 origin, Vec3 look, LivingEntity target) {
        Vec3 forward = horizontal(look);
        if (forward == null) {
            return 0.0;
        }
        Vec3 centre = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
        double dx = centre.x - origin.x;
        double dz = centre.z - origin.z;
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance < 1.0e-4) {
            return 0.0;
        }
        double cos = Mth.clamp((dx * forward.x + dz * forward.z) / distance, -1.0, 1.0);
        double angle = Math.toDegrees(Math.acos(cos));
        // Cross product (forward x toTarget).y: facing +Z, a target at +X is on the attacker's left.
        double crossY = forward.z * dx - forward.x * dz;
        return crossY >= 0.0 ? angle : -angle;
    }

    /** Reach and height, the height window centred on the look line at the target's distance. */
    private static boolean isWithinReach(Vec3 origin, Vec3 look, LivingEntity target, SwingMove move) {
        Vec3 centre = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
        double dx = centre.x - origin.x;
        double dz = centre.z - origin.z;
        double distance = Math.sqrt(dx * dx + dz * dz);
        double lookFlat = Math.sqrt(look.x * look.x + look.z * look.z);
        double lineHeight = origin.y + (lookFlat < 1.0e-6 ? 0.0 : distance * look.y / lookFlat);
        if (Math.abs(centre.y - lineHeight) > move.verticalReach() + target.getBbHeight() * 0.5) {
            return false;
        }
        return distance - target.getBbWidth() * 0.5 <= move.reach();
    }

    /** Angular allowance for the target's own width, so a wide mob is hit when its edge is inside the window. */
    private static double halfWidthAllowance(LivingEntity target, Vec3 origin) {
        Vec3 centre = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
        double distance = Math.max(0.5, Math.hypot(centre.x - origin.x, centre.z - origin.z));
        return Math.toDegrees(Math.atan((target.getBbWidth() * 0.5) / distance));
    }

    /** Horizontal unit vector of a look direction, or null when looking straight up or down. */
    public static Vec3 horizontal(Vec3 look) {
        double length = Math.sqrt(look.x * look.x + look.z * look.z);
        if (length < 1.0e-6) {
            return null;
        }
        return new Vec3(look.x / length, 0.0, look.z / length);
    }

    /** Horizontal look vector for a yaw in degrees, using Minecraft's convention (yaw 0 = +Z). */
    public static Vec3 horizontalFromYaw(float yawDegrees) {
        float rad = yawDegrees * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(rad), 0.0, Mth.cos(rad));
    }

    private ArcHitTest() {
    }
}
