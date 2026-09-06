package dev.sealbreaker.world.block;

import dev.sealbreaker.world.SbWorld;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Blocks of the world module. */
public final class SbWorldBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(SbWorld.MOD_ID);

    /** A door that opens only for its key item (PRD 3.10: dungeon inner doors). Unbreakable; iron-door looks for now. */
    public static final DeferredBlock<LockedDoorBlock> LOCKED_DOOR = BLOCKS.registerBlock("locked_door", LockedDoorBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_DOOR).strength(-1.0f, 3600000.0f));

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }

    private SbWorldBlocks() {
    }
}
