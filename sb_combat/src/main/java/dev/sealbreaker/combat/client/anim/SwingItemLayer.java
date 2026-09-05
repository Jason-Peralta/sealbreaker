package dev.sealbreaker.combat.client.anim;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.sealbreaker.combat.client.SwingRenderData;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Third person: renders the main-hand item during a move with the animation's wrist bone applied at the hand
 * (see {@link HeldItemPose}). The vanilla item layer is muted for the frame by the render-state modifier,
 * which also prepares the item render state this layer submits.
 */
public final class SwingItemLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    public SwingItemLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
        SwingRenderData data = state.getRenderData(SwingRenderData.KEY);
        if (data == null || data.itemState() == null || data.pose() == null) {
            return;
        }
        ItemStackRenderState item = data.itemState();
        if (item.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        getParentModel().translateToHand(state, state.mainArm, poseStack);
        HeldItemPose.apply(poseStack, data.pose(), state.mainArm);
        item.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
    }
}
