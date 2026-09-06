package dev.sealbreaker.gear;

import com.mojang.logging.LogUtils;
import dev.sealbreaker.gear.command.SbGearCommands;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * Gear: weapons, armor, accessories, reforging and combining, rarity and coins (PRD 3.4 to 3.6, 3.12).
 * Milestone 0 spike S4: the reforge prefix's presentation (name, tooltip, glint) and a command that applies one.
 */
@Mod(SbGear.MOD_ID)
public final class SbGear {
    public static final String MOD_ID = "sb_gear";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SbGear(IEventBus modEventBus, ModContainer modContainer) {
        NeoForge.EVENT_BUS.addListener(SbGearCommands::register);
        LOGGER.info("Sealbreaker gear {} constructed", modContainer.getModInfo().getVersion());
    }
}
