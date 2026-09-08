package dev.sealbreaker.combat.client;

import dev.sealbreaker.combat.SbCombat;
import dev.sealbreaker.core.client.dev.DevHarness;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Dev-only, reproducible Controls capture. Does not create worlds or change key bindings. */
@EventBusSubscriber(modid = SbCombat.MOD_ID, value = Dist.CLIENT)
public final class DashDebug {
    private static final boolean ENABLED = Boolean.getBoolean("sb.dashdebug");
    private static int stage;
    private static int ticks;

    @SubscribeEvent
    static void tick(ClientTickEvent.Post event) {
        if (!ENABLED) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (stage == 0 && DevHarness.readyToCreate(minecraft)) {
            minecraft.setScreenAndShow(new KeyBindsScreen(minecraft.gui.screen(), minecraft.options));
            stage = 1;
        } else if (stage == 1 && ++ticks >= 10) {
            for (var child : minecraft.gui.screen().children()) {
                if (child instanceof AbstractScrollArea list) {
                    list.setScrollAmount(list.maxScrollAmount());
                }
            }
            stage = 2;
            ticks = 0;
        } else if (stage == 2 && ++ticks >= 10) {
            Screenshot.grab(minecraft.gameDirectory, "dash_controls.png", minecraft.gameRenderer.mainRenderTarget(), 1,
                    message -> SbCombat.LOGGER.info("Dash Controls capture: {}", message.getString()));
            stage = 3;
            ticks = 0;
        } else if (stage == 3 && ++ticks >= 10) {
            minecraft.stop();
        }
    }

    private DashDebug() {
    }
}
