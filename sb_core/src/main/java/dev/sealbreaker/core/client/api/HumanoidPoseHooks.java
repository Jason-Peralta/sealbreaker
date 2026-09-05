package dev.sealbreaker.core.client.api;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

import java.util.ArrayList;
import java.util.List;

/**
 * Client hook that runs after vanilla has posed a humanoid model for a frame, so a module can layer its own
 * pose on top (a weapon swing, a cast, a dodge). Backed by the one Mixin in {@code sb_core}
 * ({@code HumanoidModelMixin}); content modules register a poser and never touch the Mixin.
 *
 * <p>Posers run in registration order and may read render data attached to the state by a
 * render-state modifier. They must be cheap: this runs for every humanoid on screen every frame.
 */
public final class HumanoidPoseHooks {
    @FunctionalInterface
    public interface Poser {
        void pose(HumanoidModel<?> model, HumanoidRenderState state);
    }

    private static final List<Poser> POSERS = new ArrayList<>();

    public static void register(Poser poser) {
        POSERS.add(poser);
    }

    /** Called by the Mixin at the tail of {@code HumanoidModel#setupAnim}. */
    public static void apply(HumanoidModel<?> model, HumanoidRenderState state) {
        for (Poser poser : POSERS) {
            poser.pose(model, state);
        }
    }

    private HumanoidPoseHooks() {
    }
}
