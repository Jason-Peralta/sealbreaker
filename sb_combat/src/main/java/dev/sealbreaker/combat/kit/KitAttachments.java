package dev.sealbreaker.combat.kit;

import com.mojang.serialization.Codec;
import dev.sealbreaker.combat.SbCombat;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;
import java.util.Set;

public final class KitAttachments {
    private static final DeferredRegister<AttachmentType<?>> TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, SbCombat.MOD_ID);
    private static final Codec<Set<Identifier>> FLAGS_CODEC = Identifier.CODEC.listOf().xmap(Set::copyOf, List::copyOf);
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Set<Identifier>>> FLAGS =
            TYPES.register("kit_flags", () -> AttachmentType.<Set<Identifier>>builder(() -> Set.of())
                    .sync(ByteBufCodecs.fromCodec(FLAGS_CODEC)).build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<DashState>> DASH =
            TYPES.register("dash", () -> AttachmentType.builder(() -> new DashState(0, 1, 0))
                    .sync(ByteBufCodecs.fromCodec(DashState.CODEC)).build());

    public static void register(IEventBus bus) {
        TYPES.register(bus);
    }

    private KitAttachments() {
    }
}
