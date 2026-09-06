package dev.sealbreaker.bosses;

import com.mojang.logging.LogUtils;
import dev.sealbreaker.bosses.entity.SbBossesEntities;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Bosses: the Seal guardians, their arenas, summons and loot bags (PRD 3.2, 3.8).
 * Milestone 0 spike S2: a GeckoLib-animated placeholder entity whose attack is triggered by the server.
 */
@Mod(SbBosses.MOD_ID)
public final class SbBosses {
    public static final String MOD_ID = "sb_bosses";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SbBosses(IEventBus modEventBus, ModContainer modContainer) {
        SbBossesEntities.register(modEventBus);
        LOGGER.info("Sealbreaker bosses {} constructed", modContainer.getModInfo().getVersion());
    }
}
