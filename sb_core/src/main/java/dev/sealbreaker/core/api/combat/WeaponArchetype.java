package dev.sealbreaker.core.api.combat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Optional;

/**
 * Data-driven moves and input timing. Optional contexts fall back to tap when absent, preserving
 * existing partial archetypes. Charge is measured in server ticks from the initial ground press.
 */
public record WeaponArchetype(List<SwingMove> tap, int comboWindowTicks, Optional<Identifier> idle,
                              List<SwingMove> hold, List<SwingMove> air, List<SwingMove> sprint,
                              int holdThresholdTicks, int chargeTicks, ChargeCurve chargeCurve) {
    public static final Codec<WeaponArchetype> CODEC = RecordCodecBuilder.<WeaponArchetype>create(i -> i.group(
            SwingMove.CODEC.listOf(1, 16).fieldOf("tap").forGetter(WeaponArchetype::tap),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("combo_window_ticks", 10).forGetter(WeaponArchetype::comboWindowTicks),
            Identifier.CODEC.optionalFieldOf("idle").forGetter(WeaponArchetype::idle),
            SwingMove.CODEC.listOf(0, 16).optionalFieldOf("hold", List.of()).forGetter(WeaponArchetype::hold),
            SwingMove.CODEC.listOf(0, 16).optionalFieldOf("air", List.of()).forGetter(WeaponArchetype::air),
            SwingMove.CODEC.listOf(0, 16).optionalFieldOf("sprint", List.of()).forGetter(WeaponArchetype::sprint),
            Codec.intRange(0, Integer.MAX_VALUE - 1).optionalFieldOf("hold_threshold_ticks", 6).forGetter(WeaponArchetype::holdThresholdTicks),
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("charge_ticks", 20).forGetter(WeaponArchetype::chargeTicks),
            ChargeCurve.CODEC.optionalFieldOf("charge_curve", ChargeCurve.IDENTITY).forGetter(WeaponArchetype::chargeCurve)
    ).apply(i, WeaponArchetype::new)).validate(value -> {
        if (!value.hold.isEmpty() && value.chargeTicks <= value.holdThresholdTicks) {
            return DataResult.error(() -> "charge_ticks must exceed hold_threshold_ticks when hold moves exist");
        }
        if (value.chargeCurve.max() < value.chargeCurve.min()) {
            return DataResult.error(() -> "charge_curve.max must be at least min");
        }
        return DataResult.success(value);
    });

    public WeaponArchetype {
        tap = List.copyOf(tap);
        hold = List.copyOf(hold);
        air = List.copyOf(air);
        sprint = List.copyOf(sprint);
    }

    public WeaponArchetype(List<SwingMove> tap, int comboWindowTicks, Optional<Identifier> idle) {
        this(tap, comboWindowTicks, idle, List.of(), List.of(), List.of(), 6, 20, ChargeCurve.IDENTITY);
    }

    public WeaponArchetype(List<SwingMove> tap, int comboWindowTicks) {
        this(tap, comboWindowTicks, Optional.empty());
    }

    public List<SwingMove> moves(AttackContext context) {
        return switch (context) {
            case TAP -> tap;
            case HOLD -> hold;
            case AIR -> air;
            case SPRINT -> sprint;
        };
    }

    public AttackContext resolve(AttackContext context) {
        return moves(context).isEmpty() ? AttackContext.TAP : context;
    }

    public SwingMove move(AttackContext context, int step) {
        List<SwingMove> moves = moves(resolve(context));
        return moves.get(Math.floorMod(step, moves.size()));
    }

    public SwingMove move(int step) {
        return move(AttackContext.TAP, step);
    }

    /** Only taps advance the combo; contextual moves start their own list at zero. */
    public int nextStep(int step) {
        return (step + 1) % tap.size();
    }
}
