package dev.sealbreaker.core.mixin.client;

import dev.sealbreaker.core.client.api.HumanoidPoseHooks;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Why a Mixin (decision 0017): vanilla resets every model part at the start of {@code setupAnim} and there is
 * no event after it, so a custom swing pose can only be applied here. NeoForge's render-state modifiers run
 * before {@code setupAnim} and cannot pose parts; render layers run after the body has been submitted.
 */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("TAIL"))
    private void sealbreaker$applyPoseHooks(HumanoidRenderState state, CallbackInfo ci) {
        HumanoidPoseHooks.apply((HumanoidModel<?>) (Object) this, state);
    }
}
