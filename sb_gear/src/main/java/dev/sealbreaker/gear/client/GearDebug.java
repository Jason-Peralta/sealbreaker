package dev.sealbreaker.gear.client;

import dev.sealbreaker.combat.item.SbCombatItems;
import dev.sealbreaker.gear.SbGear;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.Locale;

/**
 * Screenshot harness for the item presentation spike (S4), enabled by the {@code sb.geardebug} system property
 * (the {@code clientGear} run sets it to a modifier id). After joining a world it gives the spike sword, reforges
 * it through the real command, screenshots the held item and then the inventory with the sword's tooltip drawn
 * over its hotbar slot, and quits. The tooltip is queued explicitly because a programmatic cursor move does not
 * reach the mouse handler; its content is exactly what hovering shows.
 */
public final class GearDebug {
    private static final int SETTLE_TICKS = 40;
    private static int ticks = -1;
    private static boolean done;

    public static boolean enabled() {
        return System.getProperty("sb.geardebug") != null;
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || done) {
            return;
        }
        ticks++;
        String prefix = System.getProperty("sb.geardebug");
        if (ticks == 0) {
            player.connection.sendCommand("time set 1000");
            player.connection.sendCommand("weather clear");
            ItemStack sword = new ItemStack(SbCombatItems.SPIKE_SWORD.get());
            player.getInventory().setSelectedSlot(0);
            minecraft.gameMode.handleCreativeModeItemAdd(sword, 36);
            player.getInventory().setItem(0, sword);
            ItemStack plain = new ItemStack(SbCombatItems.SPIKE_SWORD.get());
            minecraft.gameMode.handleCreativeModeItemAdd(plain, 37);
            player.getInventory().setItem(1, plain);
            SbGear.LOGGER.info("Gear debug: capturing the {} prefix", prefix);
        } else if (ticks == SETTLE_TICKS) {
            player.connection.sendCommand("sb reforge " + prefix);
            // The survival inventory is the screen players see; creative mode would open the item picker instead.
            player.connection.sendCommand("gamemode survival");
        } else if (ticks == SETTLE_TICKS * 2) {
            Screenshot.grab(minecraft.gameDirectory, "s4_held.png", minecraft.gameRenderer.mainRenderTarget(), 1, c -> {
            });
        } else if (ticks == SETTLE_TICKS * 2 + 5) {
            minecraft.gui.setScreen(new InventoryScreen(player));
        } else if (ticks == SETTLE_TICKS * 3) {
            Screenshot.grab(minecraft.gameDirectory, "s4_tooltip.png", minecraft.gameRenderer.mainRenderTarget(), 1, c -> {
            });
        } else if (ticks == SETTLE_TICKS * 3 + 5) {
            player.connection.sendCommand("gamemode creative");
            SbGear.LOGGER.info("Gear debug: done, quitting");
            done = true;
            minecraft.stop();
        }
    }

    /**
     * Draws the held sword's tooltip as if the cursor hovered its hotbar slot in the inventory screen. Queued
     * before the screen draws: deferred tooltips are flushed at the end of the screen's own pass, before the
     * post-render event, and the first one queued wins.
     */
    public static void drawTooltip(ScreenEvent.Render.Pre event) {
        if (!(event.getScreen() instanceof InventoryScreen screen)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        for (Slot slot : screen.getMenu().slots) {
            if (slot.container == minecraft.player.getInventory() && slot.getContainerSlot() == 0 && slot.hasItem()) {
                int x = screen.getLeftPos() + slot.x + 8;
                int y = screen.getTopPos() + slot.y + 8;
                event.getGuiGraphics().setTooltipForNextFrame(minecraft.font, slot.getItem(), x, y);
                return;
            }
        }
    }

    private GearDebug() {
    }
}
