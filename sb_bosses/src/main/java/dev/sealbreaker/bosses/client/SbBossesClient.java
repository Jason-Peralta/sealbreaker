package dev.sealbreaker.bosses.client;

import com.geckolib.renderer.GeoEntityRenderer;
import dev.sealbreaker.bosses.SbBosses;
import dev.sealbreaker.bosses.entity.SbBossesEntities;
import dev.sealbreaker.bosses.entity.SpikeDummy;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client hooks of the boss module: GeckoLib renderers and, in Milestone 0, the animation capture harness. */
@EventBusSubscriber(modid = SbBosses.MOD_ID, value = Dist.CLIENT)
public final class SbBossesClient {
    /**
     * GeckoLib's renderer reads the model, animations and texture from the entity type's id:
     * {@code geckolib/models/entity/spike_dummy.geo.json}, {@code geckolib/animations/entity/spike_dummy.animation.json},
     * {@code textures/entity/spike_dummy.png}.
     */
    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(SbBossesEntities.SPIKE_DUMMY.get(),
                context -> new GeoEntityRenderer<SpikeDummy, LivingEntityRenderState>(context, SbBossesEntities.SPIKE_DUMMY.get()));
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        if (BossDebug.enabled()) {
            BossDebug.tick();
        }
    }

    @SubscribeEvent
    static void onChat(ClientChatReceivedEvent event) {
        if (BossDebug.enabled()) {
            BossDebug.onChat(event);
        }
    }

    private SbBossesClient() {
    }
}
