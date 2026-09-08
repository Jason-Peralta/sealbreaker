package dev.sealbreaker.combat.swing;

import dev.sealbreaker.core.api.combat.AttackContext;
import dev.sealbreaker.core.api.combat.SwingMove;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlungeTimingTest {
    @Test
    void activePoseWaitsForLandingAndKeepsExactServerTicksInOldWorlds() {
        SwingMove move = new SwingMove(SwingMove.Shape.PLUNGE, SwingMove.Direction.NONE, 3, 50, 2,
                4, 3, 6, 1, 0);
        long start = 1L << 30;
        SwingState state = new SwingState(Identifier.parse("sb_combat:sword"), 0, start, AttackContext.AIR, false, 1);
        assertEquals(3, state.moveTicks(move, start + 3));
        assertEquals(3.5f, state.animationTicks(move, start + 3, 0.5f));
        assertEquals(6, state.moveTicks(move, start + 100));
        state = state.landed(start + 100);
        assertEquals(7, state.moveTicks(move, start + 100));
        assertEquals(7, state.moveTicks(move, start + 101));
        assertEquals(7.5f, state.animationTicks(move, start + 102, 0.5f));
        assertEquals(13, state.moveTicks(move, start + 108));
        assertTrue(move.isFinishedAt(state.moveTicks(move, start + 108)));
    }
}
