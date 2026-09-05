package dev.sealbreaker.core.api.component;

import dev.sealbreaker.core.SbCore;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Item data components shared across modules. */
public final class SbDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, SbCore.MOD_ID);

    /**
     * Marks an item as a weapon with movesets: the id of its {@code sb:weapon_archetype} entry. An item with
     * this component never breaks blocks and always swings (decision 0006).
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Identifier>> ARCHETYPE =
            COMPONENTS.registerComponentType("archetype", b -> b
                    .persistent(Identifier.CODEC)
                    .networkSynchronized(Identifier.STREAM_CODEC));

    public static void register(IEventBus modEventBus) {
        COMPONENTS.register(modEventBus);
    }

    private SbDataComponents() {
    }
}
