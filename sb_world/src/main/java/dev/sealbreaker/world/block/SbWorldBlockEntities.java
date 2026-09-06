package dev.sealbreaker.world.block;

import dev.sealbreaker.world.SbWorld;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Block entity types of the world module. */
public final class SbWorldBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, SbWorld.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LockedDoorBlockEntity>> LOCKED_DOOR =
            BLOCK_ENTITIES.register("locked_door", () -> new BlockEntityType<>(LockedDoorBlockEntity::new, SbWorldBlocks.LOCKED_DOOR.get()));

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }

    private SbWorldBlockEntities() {
    }
}
