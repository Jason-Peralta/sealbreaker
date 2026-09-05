package dev.sealbreaker.core.api.combat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Optional;

/**
 * A weapon archetype: the moves of a weapon and how they chain, authored as JSON in the
 * {@code sb:weapon_archetype} datapack registry ({@code data/<mod>/sb/weapon_archetype/<name>.json}).
 *
 * <p>Current scope: the tap combo. Hold, airborne and sprint moves from the PRD are added as further
 * optional fields without moving the data.
 *
 * @param tap              the tap combo, in order; a tap during a move's recovery, or within
 *                         {@code comboWindowTicks} after it ends, continues with the next move
 * @param comboWindowTicks how long after a move ends the next tap still continues the combo
 * @param idle             keyframe animation held while the weapon is in hand and no move plays (a ready
 *                         stance); absent: the vanilla held-item pose
 */
public record WeaponArchetype(List<SwingMove> tap, int comboWindowTicks, Optional<Identifier> idle) {
    public static final Codec<WeaponArchetype> CODEC = RecordCodecBuilder.create(i -> i.group(
            SwingMove.CODEC.listOf(1, 16).fieldOf("tap").forGetter(WeaponArchetype::tap),
            Codec.INT.optionalFieldOf("combo_window_ticks", 10).forGetter(WeaponArchetype::comboWindowTicks),
            Identifier.CODEC.optionalFieldOf("idle").forGetter(WeaponArchetype::idle)
    ).apply(i, WeaponArchetype::new));

    public WeaponArchetype(List<SwingMove> tap, int comboWindowTicks) {
        this(tap, comboWindowTicks, Optional.empty());
    }

    public SwingMove move(int step) {
        return tap.get(Math.floorMod(step, tap.size()));
    }

    /** The step that follows {@code step}; the combo wraps to its first move after the last. */
    public int nextStep(int step) {
        return (step + 1) % tap.size();
    }
}
