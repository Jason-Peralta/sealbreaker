package dev.sealbreaker.core.api.combat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;

import java.util.Optional;

/**
 * One move of a weapon archetype: its hit shape, timing and damage. Authored as JSON inside an
 * {@code sb:weapon_archetype} entry.
 *
 * @param shape            how the hit region is formed
 * @param direction        for {@link Shape#SWEEP}: which way the blade travels across the arc
 * @param reach            reach in blocks from the eyes; downward depth for a plunge, horizontal otherwise
 * @param arcDegrees       total horizontal arc; for a sweep the live window travels across it
 * @param verticalReach    how far above or below eye level a target's centre may be
 * @param windupTicks      ticks before the move can hit
 * @param activeTicks      ticks during which the hit region is live; each target is hit at most once per move
 * @param recoveryTicks    ticks after the active window before the next move may start on its own
 * @param damageMultiplier multiplier on the attacker's attack-damage attribute
 * @param knockback        knockback strength applied to each hit target
 * @param animation        id of the keyframe animation for this move ({@code assets/<ns>/sb_animations/player/}),
 *                         used for both third and first person. Absent: a procedural fallback.
 * @param hitstopTicks     ticks the move pauses on its first hit (the impact frames of a fighting game); the
 *                         move's timeline shifts by this much, the attacker's animation freezes and the camera
 *                         kicks. 0 disables it.
 * @param forwardImpulse   one horizontal velocity impulse along facing yaw at move start; 0 disables it
 * @param landingParticles block particle count on plunge landing; 0 disables it
 * @param landingSpread    horizontal spread of the landing particles
 */
public record SwingMove(
        Shape shape,
        Direction direction,
        double reach,
        float arcDegrees,
        double verticalReach,
        int windupTicks,
        int activeTicks,
        int recoveryTicks,
        float damageMultiplier,
        float knockback,
        Optional<Identifier> animation,
        int hitstopTicks,
        double forwardImpulse,
        int landingParticles,
        float landingSpread
) {
    public static final Codec<SwingMove> CODEC = RecordCodecBuilder.create(i -> i.group(
            Shape.CODEC.fieldOf("shape").forGetter(SwingMove::shape),
            Direction.CODEC.optionalFieldOf("direction", Direction.NONE).forGetter(SwingMove::direction),
            Codec.DOUBLE.fieldOf("reach").forGetter(SwingMove::reach),
            Codec.FLOAT.fieldOf("arc_degrees").forGetter(SwingMove::arcDegrees),
            Codec.DOUBLE.optionalFieldOf("vertical_reach", 2.0).forGetter(SwingMove::verticalReach),
            Codec.INT.fieldOf("windup_ticks").forGetter(SwingMove::windupTicks),
            Codec.INT.fieldOf("active_ticks").forGetter(SwingMove::activeTicks),
            Codec.INT.fieldOf("recovery_ticks").forGetter(SwingMove::recoveryTicks),
            Codec.FLOAT.optionalFieldOf("damage_multiplier", 1.0f).forGetter(SwingMove::damageMultiplier),
            Codec.FLOAT.optionalFieldOf("knockback", 0.4f).forGetter(SwingMove::knockback),
            Identifier.CODEC.optionalFieldOf("animation").forGetter(SwingMove::animation),
            Codec.INT.optionalFieldOf("hitstop_ticks", 2).forGetter(SwingMove::hitstopTicks),
            Codec.doubleRange(0, 4).optionalFieldOf("forward_impulse", 0.0).forGetter(SwingMove::forwardImpulse),
            Codec.intRange(0, 256).optionalFieldOf("landing_particles", 0).forGetter(SwingMove::landingParticles),
            Codec.floatRange(0, 4).optionalFieldOf("landing_spread", 0.0f).forGetter(SwingMove::landingSpread)
    ).apply(i, SwingMove::new));

    public SwingMove(Shape shape, Direction direction, double reach, float arcDegrees, double verticalReach,
                     int windupTicks, int activeTicks, int recoveryTicks, float damageMultiplier, float knockback,
                     Optional<Identifier> animation, int hitstopTicks) {
        this(shape, direction, reach, arcDegrees, verticalReach, windupTicks, activeTicks, recoveryTicks,
                damageMultiplier, knockback, animation, hitstopTicks, 0, 0, 0);
    }

    public SwingMove(Shape shape, Direction direction, double reach, float arcDegrees, double verticalReach,
                     int windupTicks, int activeTicks, int recoveryTicks, float damageMultiplier, float knockback) {
        this(shape, direction, reach, arcDegrees, verticalReach, windupTicks, activeTicks, recoveryTicks, damageMultiplier, knockback, Optional.empty(), 2);
    }

    public int totalTicks() {
        return windupTicks + activeTicks + recoveryTicks;
    }

    public boolean isActiveAt(long ticksSinceStart) {
        return ticksSinceStart >= windupTicks && ticksSinceStart < windupTicks + activeTicks;
    }

    public boolean isInRecoveryAt(long ticksSinceStart) {
        return ticksSinceStart >= windupTicks + activeTicks && ticksSinceStart < totalTicks();
    }

    public boolean isFinishedAt(long ticksSinceStart) {
        return ticksSinceStart >= totalTicks();
    }

    /** Index within the active window (0-based), or -1 outside it. */
    public int activeIndexAt(long ticksSinceStart) {
        return isActiveAt(ticksSinceStart) ? (int) (ticksSinceStart - windupTicks) : -1;
    }

    /** How the hit region of a move is formed. */
    public enum Shape implements StringRepresentable {
        /** A horizontal arc that the blade travels across during the active window; the live window follows the blade. */
        SWEEP("sweep"),
        /** A narrow cone in front of the attacker, live for the whole active window; a downward chop. */
        OVERHEAD("overhead"),
        /** A narrow cone straight ahead, live for the whole active window; a stab, so use a small arc and a long reach. */
        THRUST("thrust"),
        /** Downward cone held active while falling; landing starts recovery and its impact pause. */
        PLUNGE("plunge");

        public static final Codec<Shape> CODEC = StringRepresentable.fromEnum(Shape::values);
        private final String name;

        Shape(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    /** Travel direction of a sweep, from the attacker's point of view. */
    public enum Direction implements StringRepresentable {
        NONE("none"),
        LEFT_TO_RIGHT("left_to_right"),
        RIGHT_TO_LEFT("right_to_left");

        public static final Codec<Direction> CODEC = StringRepresentable.fromEnum(Direction::values);
        private final String name;

        Direction(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
