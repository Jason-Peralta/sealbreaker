package dev.sealbreaker.combat;

import com.mojang.logging.LogUtils;
import dev.sealbreaker.combat.item.SbCombatItems;
import dev.sealbreaker.combat.swing.SbCombatAttachments;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Combat core. Milestone 0 spike S1: server-authoritative swings with an arc hit test, driven from a
 * data-driven archetype, rendered through the vanilla attack animation with our timing.
 */
@Mod(SbCombat.MOD_ID)
public final class SbCombat {
    public static final String MOD_ID = "sb_combat";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SbCombat(IEventBus modEventBus, ModContainer modContainer) {
        SbCombatAttachments.register(modEventBus);
        dev.sealbreaker.combat.kit.KitAttachments.register(modEventBus);
        SbCombatItems.register(modEventBus);
        LOGGER.info("Sealbreaker combat {} constructed", modContainer.getModInfo().getVersion());
    }
}
