package dev.sealbreaker.combat.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.sealbreaker.combat.SbCombat;
import dev.sealbreaker.combat.kit.DashState;
import dev.sealbreaker.combat.kit.KitAttachments;
import dev.sealbreaker.combat.network.DashRequestPayload;
import dev.sealbreaker.core.client.api.HumanoidPoseHooks;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Avatar;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/** One rebindable Dash/Dodge key and a short lean driven by server-synced timing. */
@EventBusSubscriber(modid = SbCombat.MOD_ID, value = Dist.CLIENT)
public final class DashClient {
    private static final ContextKey<Float> LEAN = new ContextKey<>(Identifier.fromNamespaceAndPath(SbCombat.MOD_ID, "dash_lean"));
    private static KeyMapping dash;

    @SubscribeEvent
    static void keys(RegisterKeyMappingsEvent event) {
        KeyMapping.Category category = new KeyMapping.Category(Identifier.fromNamespaceAndPath("sb_core", "sealbreaker"));
        event.registerCategory(category);
        dash = new KeyMapping("key.sb_combat.dash", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, category);
        event.register(dash);
    }

    @SubscribeEvent
    static void tick(ClientTickEvent.Post event) {
        if (dash == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        while (dash.consumeClick()) {
            if (minecraft.player != null && minecraft.gui.screen() == null && !minecraft.isPaused() && minecraft.isWindowActive()) {
                ClientPacketDistributor.sendToServer(DashRequestPayload.INSTANCE);
            }
        }
    }

    @SubscribeEvent
    static void setup(FMLClientSetupEvent event) {
        HumanoidPoseHooks.register((model, state) -> {
            Float lean = state.getRenderData(LEAN);
            if (lean != null) {
                model.body.xRot += lean * Mth.DEG_TO_RAD;
            }
        });
    }

    @SubscribeEvent
    static void renderState(RegisterRenderStateModifiersEvent event) {
        event.registerAvatarEntityModifier(new AvatarRenderStateModifier() {
            @Override
            public <T extends Avatar & ClientAvatarEntity> void accept(T avatar, AvatarRenderState state) {
                DashState dashState = avatar.getExistingDataOrNull(KitAttachments.DASH.get());
                float now = avatar.level().getGameTime() + Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
                state.setRenderData(LEAN, dashState == null ? 0 : dashState.leanAt(now));
            }
        });
    }

    private DashClient() {
    }
}
