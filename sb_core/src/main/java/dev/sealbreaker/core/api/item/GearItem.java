package dev.sealbreaker.core.api.item;

import dev.sealbreaker.core.api.component.ReforgePrefix;
import dev.sealbreaker.core.api.component.SbDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Base class of every item that can carry a reforge prefix: weapons, armor pieces and accessories. In 26.2 those
 * are plain {@link Item}s configured through {@link Item.Properties} (sword, humanoidArmor, ...), so this class
 * adds only what components cannot express on their own: the prefix in the name and the glint that marks a
 * reforged item. Everything else about a gear item stays data.
 */
public class GearItem extends Item {
    public GearItem(Properties properties) {
        super(properties);
    }

    /** "Lucky Spike Sword": the prefix is composed at display time, so lang changes and modifier renames never go stale in saves. */
    @Override
    public Component getName(ItemStack stack) {
        ReforgePrefix prefix = stack.get(SbDataComponents.REFORGE_PREFIX.get());
        Component base = super.getName(stack);
        return prefix == null ? base : prefix.decorateName(base);
    }

    /** A reforged item glints; the vanilla {@code enchantment_glint_override} component still wins when set. */
    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.has(SbDataComponents.REFORGE_PREFIX.get()) || super.isFoil(stack);
    }
}
