package dev.sealbreaker.combat.swing;

import dev.sealbreaker.combat.SbCombat;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Data attachments owned by the combat module. */
public final class SbCombatAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, SbCombat.MOD_ID);

    /** The attacker's current move; absent when idle. Synced to the owner and to everyone tracking them. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<SwingState>> SWING =
            ATTACHMENTS.register("swing", () -> AttachmentType
                    .builder(() -> new SwingState(Identifier.fromNamespaceAndPath(SbCombat.MOD_ID, "none"), 0, Long.MIN_VALUE))
                    .sync(SwingState.STREAM_CODEC)
                    .build());

    public static void register(IEventBus modEventBus) {
        ATTACHMENTS.register(modEventBus);
    }

    private SbCombatAttachments() {
    }
}
