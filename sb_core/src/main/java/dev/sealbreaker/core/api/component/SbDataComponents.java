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

    /**
     * The reforge prefix on a weapon, armor piece or accessory (PRD 3.5): one per item, shown first in the name,
     * one tooltip line, and a glint on {@link dev.sealbreaker.core.api.item.GearItem}s. Written by the reforge
     * transaction and by loot tables ({@code set_components}); read by presentation and, later, by the modifier
     * registry lookup that applies its attribute modifiers.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ReforgePrefix>> REFORGE_PREFIX =
            COMPONENTS.registerComponentType("reforge_prefix", b -> b
                    .persistent(ReforgePrefix.CODEC)
                    .networkSynchronized(ReforgePrefix.STREAM_CODEC));

    public static void register(IEventBus modEventBus) {
        COMPONENTS.register(modEventBus);
    }

    private SbDataComponents() {
    }
}
