package dev.sealbreaker.combat.input;

import dev.sealbreaker.combat.network.SwingRequestPayload;
import dev.sealbreaker.core.api.combat.AttackContext;

import java.util.function.Consumer;

/** Edge detector independent of the crosshair and Minecraft client classes, also usable by capture runs. */
public final class AttackInput {
    private boolean down;
    private boolean blockedUntilRelease;
    private long pressedAt;
    private AttackContext context;
    private Object weapon;

    /** weapon identifies this held stack and level; replacing it cancels an unfinished press. */
    public void tick(boolean pressed, boolean enabled, Object weapon, long tick, int threshold,
                     boolean onGround, boolean falling, boolean sprinting, Consumer<SwingRequestPayload> send) {
        if (!enabled || (down && !java.util.Objects.equals(this.weapon, weapon))) {
            if (down) {
                send.accept(SwingRequestPayload.CANCEL);
            }
            down = false;
            blockedUntilRelease = pressed;
            this.weapon = null;
            return;
        }
        if (blockedUntilRelease) {
            blockedUntilRelease = pressed;
            return;
        }
        if (pressed && !down) {
            down = true;
            this.weapon = weapon;
            pressedAt = tick;
            context = !onGround && falling ? AttackContext.AIR
                    : onGround && sprinting ? AttackContext.SPRINT : AttackContext.TAP;
            send.accept(new SwingRequestPayload(context, false));
        } else if (!pressed && down) {
            down = false;
            AttackContext released = context == AttackContext.TAP && tick - pressedAt > threshold
                    ? AttackContext.HOLD : context;
            send.accept(new SwingRequestPayload(released, true));
        }
    }
}
