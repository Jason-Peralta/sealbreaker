package dev.sealbreaker.core;

import com.mojang.logging.LogUtils;
import dev.sealbreaker.core.api.combat.WeaponArchetype;
import dev.sealbreaker.core.api.component.SbDataComponents;
import dev.sealbreaker.core.api.damage.DamageClass;
import dev.sealbreaker.core.api.progression.Seal;
import dev.sealbreaker.core.api.progression.Tier;
import dev.sealbreaker.core.api.rarity.Rarity;
import dev.sealbreaker.core.api.reforge.ReforgeModifier;
import dev.sealbreaker.core.api.reforge.ReforgePool;
import dev.sealbreaker.core.api.registry.SbRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.tooltip.TooltipAppender;
import net.neoforged.neoforge.event.RegisterTooltipAppendersEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import org.slf4j.Logger;

/**
 * Entry point of the core module: shared registries, components and services; no content. The datapack
 * registries ({@code sb:*}, see {@link SbRegistries}) are declared here and documented in {@code docs/data/}.
 */
@Mod(SbCore.MOD_ID)
public final class SbCore {
    public static final String MOD_ID = "sb_core";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SbCore(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerDatapackRegistries);
        modEventBus.addListener(this::registerTooltipAppenders);
        SbDataComponents.register(modEventBus);
        NeoForge.EVENT_BUS.register(this);
        LOGGER.info("Sealbreaker core {} constructed", modContainer.getModInfo().getVersion());
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Sealbreaker core: common setup complete");
    }

    /**
     * Declares our datapack registries; the network codec makes entries available on clients too. The records
     * reference other registries by key, so one codec serves both the disk and the wire.
     */
    private void registerDatapackRegistries(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(SbRegistries.WEAPON_ARCHETYPE, WeaponArchetype.CODEC, WeaponArchetype.CODEC);
        event.dataPackRegistry(SbRegistries.DAMAGE_CLASS, DamageClass.CODEC, DamageClass.CODEC);
        event.dataPackRegistry(SbRegistries.REFORGE_MODIFIER, ReforgeModifier.CODEC, ReforgeModifier.CODEC);
        event.dataPackRegistry(SbRegistries.REFORGE_POOL, ReforgePool.CODEC, ReforgePool.CODEC);
        event.dataPackRegistry(SbRegistries.RARITY, Rarity.CODEC, Rarity.CODEC);
        event.dataPackRegistry(SbRegistries.SEAL, Seal.CODEC, Seal.CODEC);
        event.dataPackRegistry(SbRegistries.TIER, Tier.CODEC, Tier.CODEC);
    }

    /**
     * Our components describe their own tooltip lines ({@code TooltipProvider}); this places them. The reforge
     * prefix line goes before every vanilla component line, right under the item's own text.
     */
    private void registerTooltipAppenders(RegisterTooltipAppendersEvent event) {
        event.registerComponentAppenderBeforeAll(SbDataComponents.REFORGE_PREFIX,
                TooltipAppender.createComponentAppender(SbDataComponents.REFORGE_PREFIX.get()));
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Sealbreaker core: server starting");
    }
}
