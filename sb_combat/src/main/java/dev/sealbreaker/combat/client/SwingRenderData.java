package dev.sealbreaker.combat.client;

import dev.sealbreaker.combat.SbCombat;
import dev.sealbreaker.combat.client.anim.PlayerAnimation;
import dev.sealbreaker.combat.client.anim.PoseSource;
import dev.sealbreaker.core.api.combat.SwingMove;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import org.jetbrains.annotations.Nullable;

/**
 * Per-frame weapon information attached to a humanoid's render state by the render-state modifier and read
 * by the pose hook, the item layer and the first-person renderer.
 *
 * @param move         the move being performed, or null between moves (idle) and under the animation debugger
 * @param elapsedTicks how far into the move the entity is, fractional; 0 when idle
 * @param pose         the pose to draw (moves and idle already blended), or null for the procedural fallback
 * @param idle         the weapon's idle stance, the reference the first-person view moves relative to
 * @param moveWeight   0 idle, 1 mid-move, in between while fading: how much the torso follows the look
 * @param striking     true while the blade is in the part of the move that leaves an arc (around the hit window)
 * @param itemState    the prepared main-hand item for the swing item layer (null: vanilla draws the item)
 */
public record SwingRenderData(@Nullable SwingMove move, float elapsedTicks, @Nullable PoseSource pose, PlayerAnimation idle,
                              float moveWeight, boolean striking, @Nullable ItemStackRenderState itemState) {
    public static final ContextKey<SwingRenderData> KEY = new ContextKey<>(Identifier.fromNamespaceAndPath(SbCombat.MOD_ID, "swing"));

    public SwingRenderData(@Nullable SwingMove move, float elapsedTicks, @Nullable PoseSource pose, PlayerAnimation idle, float moveWeight) {
        this(move, elapsedTicks, pose, idle, moveWeight, false, null);
    }

    public SwingRenderData(@Nullable SwingMove move, float elapsedTicks, @Nullable PoseSource pose, PlayerAnimation idle, float moveWeight, boolean striking) {
        this(move, elapsedTicks, pose, idle, moveWeight, striking, null);
    }

    public SwingRenderData withItemState(ItemStackRenderState state) {
        return new SwingRenderData(move, elapsedTicks, pose, idle, moveWeight, striking, state);
    }

    /** 0..1 inside the wind-up, or -1 outside it (procedural fallback only). */
    public float windupProgress() {
        return move == null ? -1.0f : phase(0, move.windupTicks());
    }

    public float activeProgress() {
        return move == null ? -1.0f : phase(move.windupTicks(), move.activeTicks());
    }

    public float recoveryProgress() {
        return move == null ? -1.0f : phase(move.windupTicks() + move.activeTicks(), move.recoveryTicks());
    }

    private float phase(int start, int length) {
        if (length <= 0) {
            return -1.0f;
        }
        float t = (elapsedTicks - start) / length;
        return t >= 0.0f && t < 1.0f ? t : -1.0f;
    }
}
