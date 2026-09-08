package dev.sealbreaker.combat.input;

import dev.sealbreaker.combat.network.SwingRequestPayload;
import dev.sealbreaker.core.api.combat.AttackContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AttackInputTest {
    private final AttackInput input = new AttackInput();
    private final List<SwingRequestPayload> sent = new ArrayList<>();

    private void tick(boolean down, long tick) {
        input.tick(down, true, "weapon", tick, 6, true, false, false, sent::add);
    }

    @Test
    void oneTapAtTheBoundaryWithoutBlockOrEntityCallbacks() {
        for (int tick = 0; tick <= 6; tick++) {
            tick(tick < 6, tick);
        }
        assertEquals(List.of(new SwingRequestPayload(AttackContext.TAP, false),
                new SwingRequestPayload(AttackContext.TAP, true)), sent);
        tick(false, 7);
        assertEquals(2, sent.size());
    }

    @Test
    void aLongPressSendsOnlyEdgesAndReleasesAsHold() {
        for (int tick = 0; tick <= 30; tick++) {
            tick(tick < 30, tick);
        }
        assertEquals(List.of(new SwingRequestPayload(AttackContext.TAP, false),
                new SwingRequestPayload(AttackContext.HOLD, true)), sent);
    }

    @Test
    void movementIsSampledOnPressNotRelease() {
        input.tick(true, true, "weapon", 0, 6, false, true, true, sent::add);
        tick(false, 1);
        assertEquals(List.of(new SwingRequestPayload(AttackContext.AIR, false),
                new SwingRequestPayload(AttackContext.AIR, true)), sent);
        sent.clear();
        input.tick(true, true, "weapon", 2, 6, true, false, true, sent::add);
        tick(false, 3);
        assertEquals(AttackContext.SPRINT, sent.getFirst().context());
    }

    @Test
    void risingIsTapAndDoesNotBecomeAirOnRelease() {
        input.tick(true, true, "weapon", 0, 6, false, false, false, sent::add);
        input.tick(false, true, "weapon", 1, 6, false, true, false, sent::add);
        assertTrue(sent.stream().allMatch(p -> p.context() == AttackContext.TAP));
    }

    @Test
    void screenOrWeaponChangeCancelsAndRequiresANewPress() {
        tick(true, 0);
        input.tick(true, false, "weapon", 1, 6, true, false, false, sent::add);
        tick(true, 2);
        tick(false, 3);
        assertEquals(List.of(new SwingRequestPayload(AttackContext.TAP, false), SwingRequestPayload.CANCEL), sent);
        tick(true, 4);
        input.tick(true, true, "another weapon", 5, 6, true, false, false, sent::add);
        assertEquals(SwingRequestPayload.CANCEL, sent.getLast());
        tick(false, 6);
        assertEquals(4, sent.size());
    }
}
