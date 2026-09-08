package dev.sealbreaker.core.api.combat;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/** The four existing attack inputs. The server validates movement before choosing a move. */
public enum AttackContext implements StringRepresentable {
    TAP("tap"), HOLD("hold"), AIR("air"), SPRINT("sprint");

    public static final Codec<AttackContext> CODEC = StringRepresentable.fromEnum(AttackContext::values);
    private final String name;

    AttackContext(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
