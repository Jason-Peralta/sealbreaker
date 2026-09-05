package dev.sealbreaker.combat.client.anim;

import dev.sealbreaker.combat.swing.SwingState;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Turns the synced swing state of an avatar into the pose on screen, with the transitions a combo needs:
 * a move fades in over whatever was showing when it started (the idle stance, or the previous move's pose
 * the tick it ended), a move that is not continued fades from its last frame into the idle stance, and a
 * hit freezes the pose for the move's impact frames. Client only; entries are dropped once a fade has
 * finished and with the level.
 *
 * <p>Impact frames arrive as the server shifting the move's start tick by the move's hitstop while the step
 * stays the same: the pose holds where it was when the shift was seen, for that many ticks, then continues
 * in step with the shifted timeline.
 */
public final class SwingBlendTracker {
    /** Ticks a move takes to fade in over the pose it started from. */
    private static final float BLEND_IN_TICKS = 3.0f;
    /** Ticks a finished move takes to settle into the idle stance. */
    private static final float BLEND_OUT_TICKS = 6.0f;
    /** A start-tick shift of at most this many ticks with the same step is a hit, not a new move. */
    private static final int MAX_HITSTOP_TICKS = 6;

    /**
     * What the pose code needs this frame.
     *
     * @param pose       the pose to draw
     * @param moveWeight 0 idle, 1 mid-move, in between while fading: how much the torso follows the look
     * @param elapsed    where in the move the pose is, ticks (fractional, hitstop removed); -1 when idle
     * @param impact     true on the first frame a hit's impact frames were seen (camera kick, once)
     */
    public record Result(PoseSource pose, float moveWeight, float elapsed, boolean impact) {
    }

    private static final class Entry {
        final SwingState state;
        final PlayerAnimation animation;
        final int totalTicks;
        @Nullable
        final PoseSource from;
        long startTick;
        float freezeAt = -1.0f;
        int freezeTicks;

        Entry(SwingState state, PlayerAnimation animation, int totalTicks, @Nullable PoseSource from) {
            this.state = state;
            this.animation = animation;
            this.totalTicks = totalTicks;
            this.from = from;
            this.startTick = state.startTick();
        }

        /** Ticks into the animation for a game time, holding still through the impact frames. */
        float elapsed(float atTicks) {
            float e = atTicks - startTick;
            if (freezeAt < 0.0f || e < freezeAt) {
                return e;
            }
            return e < freezeAt + freezeTicks ? freezeAt : e - freezeTicks;
        }

        boolean sameMove(SwingState other) {
            return state.archetype().equals(other.archetype()) && state.step() == other.step();
        }
    }

    private static final Map<Integer, Entry> ENTRIES = new HashMap<>();
    private static Level level;

    /**
     * @param state         the avatar's synced swing, or null when it has none
     * @param moveAnimation the animation of that swing's move (null: no keyframes, nothing to track)
     * @param totalTicks    the move's length
     * @param idle          the stance between moves
     * @param nowTicks      game time including the partial tick
     */
    public static Result pose(Level currentLevel, int entityId, @Nullable SwingState state, @Nullable PlayerAnimation moveAnimation,
                              int totalTicks, PlayerAnimation idle, float nowTicks) {
        if (level != currentLevel) {
            level = currentLevel;
            ENTRIES.clear();
        }
        Entry entry = ENTRIES.get(entityId);
        boolean impact = false;
        if (state != null && moveAnimation != null) {
            if (entry != null && entry.animation == moveAnimation && entry.sameMove(state) && entry.freezeAt < 0.0f
                    && state.startTick() > entry.startTick && state.startTick() - entry.startTick <= MAX_HITSTOP_TICKS
                    && nowTicks - entry.startTick < entry.totalTicks) {
                entry.freezeTicks = (int) (state.startTick() - entry.startTick);
                entry.freezeAt = nowTicks - entry.startTick;
                impact = true;
            } else if (entry == null || entry.animation != moveAnimation || !entry.sameMove(state) || state.startTick() != entry.startTick) {
                // A move started: remember what was on screen at that moment and fade the move in over it.
                PoseSource from = displayed(entry, idle, state.startTick()).pose();
                entry = new Entry(state, moveAnimation, totalTicks, from);
                ENTRIES.put(entityId, entry);
            }
        }
        Result result = displayed(entry, idle, nowTicks);
        if (entry != null && entry.elapsed(nowTicks) >= entry.totalTicks + BLEND_OUT_TICKS) {
            ENTRIES.remove(entityId);
        }
        return impact ? new Result(result.pose(), result.moveWeight(), result.elapsed(), true) : result;
    }

    private static Result displayed(@Nullable Entry entry, PlayerAnimation idle, float atTicks) {
        PoseSource rest = new PoseSource.AnimationAt(idle, 0.0f);
        if (entry == null) {
            return new Result(rest, 0.0f, -1.0f, false);
        }
        float elapsed = entry.elapsed(atTicks);
        if (elapsed < entry.totalTicks) {
            PoseSource playing = new PoseSource.AnimationAt(entry.animation, elapsed / 20.0f);
            float weight = Math.min(1.0f, Math.max(0.0f, elapsed / BLEND_IN_TICKS));
            return new Result(entry.from == null ? playing : new BlendedPose(entry.from, playing, weight), weight, elapsed, false);
        }
        PoseSource end = new PoseSource.AnimationAt(entry.animation, entry.animation.length());
        float weight = Math.min(1.0f, (elapsed - entry.totalTicks) / BLEND_OUT_TICKS);
        return new Result(new BlendedPose(end, rest, weight), 1.0f - weight, -1.0f, false);
    }

    private SwingBlendTracker() {
    }
}
