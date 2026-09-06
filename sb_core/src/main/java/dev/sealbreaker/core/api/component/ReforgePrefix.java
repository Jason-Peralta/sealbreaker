package dev.sealbreaker.core.api.component;

import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

import java.util.function.Consumer;

/**
 * The value of {@link SbDataComponents#REFORGE_PREFIX}: which reforge modifier an item carries. The modifier id
 * will point into the {@code sb:reforge_modifier} registry once it exists (PRD 3.5); until then only its display
 * name is read, through the lang key {@code reforge.<namespace>.<path>}.
 *
 * <p>Presentation rules, all derived from this one component so it stays the single source of truth for loot
 * tables, the reforge transaction and saves: the name shows the prefix first ({@link #decorateName}), the
 * tooltip gets one line ({@link #addToTooltip}, wired as a NeoForge component tooltip appender by core), and
 * {@link dev.sealbreaker.core.api.item.GearItem} glints while the component is present.
 */
public record ReforgePrefix(Identifier modifier) implements TooltipProvider {
    public static final Codec<ReforgePrefix> CODEC = Identifier.CODEC.xmap(ReforgePrefix::new, ReforgePrefix::modifier);
    public static final StreamCodec<RegistryFriendlyByteBuf, ReforgePrefix> STREAM_CODEC =
            Identifier.STREAM_CODEC.map(ReforgePrefix::new, ReforgePrefix::modifier).cast();

    /** Lang key of the combined name: {@code "%s %s"} in English, prefix first; other languages may reorder. */
    public static final String NAMED_KEY = "reforge.sb_core.named";
    /** Lang key of the tooltip line; its one argument is the prefix name. */
    public static final String TOOLTIP_KEY = "reforge.sb_core.tooltip";

    /** The prefix's own name, e.g. "Lucky". */
    public MutableComponent displayName() {
        return Component.translatable(Util.makeDescriptionId("reforge", this.modifier));
    }

    /** The item's name with the prefix in front of it. */
    public MutableComponent decorateName(Component itemName) {
        return Component.translatable(NAMED_KEY, this.displayName(), itemName);
    }

    @Override
    public void addToTooltip(Item.TooltipContext context, Consumer<Component> consumer, TooltipFlag flag, DataComponentGetter components) {
        consumer.accept(Component.translatable(TOOLTIP_KEY, this.displayName().withStyle(ChatFormatting.GOLD)).withStyle(ChatFormatting.GRAY));
    }
}
