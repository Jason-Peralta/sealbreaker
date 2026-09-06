package dev.sealbreaker.world;

import com.mojang.logging.LogUtils;
import dev.sealbreaker.world.block.SbWorldBlockEntities;
import dev.sealbreaker.world.block.SbWorldBlocks;
import dev.sealbreaker.world.item.SbWorldItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * World: structures and dungeons, the blocks inside them, portals and dimensions (PRD 3.10, 3.12).
 * Milestone 0 spike S5: a three-piece jigsaw structure with a processor list and a locked door.
 */
@Mod(SbWorld.MOD_ID)
public final class SbWorld {
    public static final String MOD_ID = "sb_world";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SbWorld(IEventBus modEventBus, ModContainer modContainer) {
        SbWorldBlocks.register(modEventBus);
        SbWorldBlockEntities.register(modEventBus);
        SbWorldItems.register(modEventBus);
        LOGGER.info("Sealbreaker world {} constructed", modContainer.getModInfo().getVersion());
    }
}
