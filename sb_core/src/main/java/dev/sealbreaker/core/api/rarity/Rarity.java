package dev.sealbreaker.core.api.rarity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ARGB;
import net.minecraft.util.ExtraCodecs;

import java.util.Optional;

/**
 * A rarity (PRD 3.12): a colour, an icon and a place in the order, plus what it does to reforge cost. Authored as
 * JSON in the {@code sb:rarity} datapack registry ({@code data/<mod>/sb/rarity/<name>.json}). Rarity affects
 * reforge cost and drop weight, never raw power.
 *
 * @param color                  the name colour, {@code "#RRGGBB"} in JSON, RGB without alpha in Java
 * @param order                  position in the rarity ladder, lowest first; unique across entries. Order 0 is
 *                               <em>below</em> common: bad prefixes exist (decision 0022), so a rarity may be a
 *                               penalty rather than a prize
 * @param tooltipKey             lang key of the rarity line; absent: {@code rarity.<namespace>.<path>}
 * @param reforgeCostMultiplier  multiplies the tier's base reforge cost (PRD 3.5)
 * @param rainbow                the name cycles through the spectrum instead of sitting at {@code color}; reserved
 *                               for the one-of-a-kind top rarity (decision 0022). {@code color} stays the static
 *                               fallback for chat, logs and anywhere that cannot animate
 * @param icon                   sprite shown next to the name so colour is never the only signal; absent until art exists
 */
public record Rarity(int color, int order, Optional<String> tooltipKey, float reforgeCostMultiplier, boolean rainbow, Optional<Identifier> icon) {
    /** {@code #RRGGBB} in JSON, a plain RGB int (no alpha) in Java, the form {@code Style.withColor(int)} takes. */
    public static final Codec<Integer> RGB_CODEC = ExtraCodecs.STRING_RGB_COLOR.xmap(ARGB::transparent, ARGB::opaque);
    public static final Codec<Rarity> CODEC = RecordCodecBuilder.create(i -> i.group(
            RGB_CODEC.fieldOf("color").forGetter(Rarity::color),
            Codec.INT.fieldOf("order").forGetter(Rarity::order),
            Codec.STRING.optionalFieldOf("tooltip_key").forGetter(Rarity::tooltipKey),
            Codec.FLOAT.fieldOf("reforge_cost_multiplier").forGetter(Rarity::reforgeCostMultiplier),
            Codec.BOOL.optionalFieldOf("rainbow", false).forGetter(Rarity::rainbow),
            Identifier.CODEC.optionalFieldOf("icon").forGetter(Rarity::icon)
    ).apply(i, Rarity::new));

    /** The tooltip line's lang key: the explicit one, else derived from the entry's id. */
    public String tooltipKey(ResourceKey<Rarity> self) {
        return this.tooltipKey.orElseGet(() -> "rarity." + self.identifier().getNamespace() + "." + self.identifier().getPath());
    }
}
