package dev.sealbreaker.combat.item;

import dev.sealbreaker.combat.SbCombat;
import dev.sealbreaker.core.api.component.SbDataComponents;
import dev.sealbreaker.core.api.item.GearItem;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ToolMaterial;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Items of the combat module. Milestone 0: one spike weapon to exercise the swing system. */
@EventBusSubscriber(modid = SbCombat.MOD_ID)
public final class SbCombatItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SbCombat.MOD_ID);

    /** Iron-sword stats, our swing archetype. Spike only; replaced by the Tier 1 weapon table in Milestone 1. */
    public static final DeferredItem<GearItem> SPIKE_SWORD = ITEMS.registerItem("spike_sword", GearItem::new, props -> props
            .sword(ToolMaterial.IRON, 3.0f, -2.4f)
            .component(SbDataComponents.ARCHETYPE.get(), Identifier.fromNamespaceAndPath(SbCombat.MOD_ID, "sword")));

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }

    @SubscribeEvent
    static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(SPIKE_SWORD);
        }
    }

    private SbCombatItems() {
    }
}
