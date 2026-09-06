package dev.sealbreaker.world.item;

import dev.sealbreaker.world.SbWorld;
import dev.sealbreaker.world.block.SbWorldBlocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Items of the world module. Milestone 0: the locked door and the key that opens the spike structure's room. */
@EventBusSubscriber(modid = SbWorld.MOD_ID)
public final class SbWorldItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SbWorld.MOD_ID);

    public static final DeferredItem<BlockItem> LOCKED_DOOR = ITEMS.registerSimpleBlockItem(SbWorldBlocks.LOCKED_DOOR);
    public static final DeferredItem<BlockItem> SPIKE_PORTAL = ITEMS.registerSimpleBlockItem(SbWorldBlocks.SPIKE_PORTAL);
    /** Spike only: the key the S5 room's door asks for. Real keys come with the dungeons (#29). */
    public static final DeferredItem<Item> SPIKE_KEY = ITEMS.registerSimpleItem("spike_key", props -> props.stacksTo(1));

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }

    @SubscribeEvent
    static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(LOCKED_DOOR);
            event.accept(SPIKE_PORTAL);
        } else if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(SPIKE_KEY);
        }
    }

    private SbWorldItems() {
    }
}
