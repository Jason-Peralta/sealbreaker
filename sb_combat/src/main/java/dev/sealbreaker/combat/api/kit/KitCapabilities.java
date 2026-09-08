package dev.sealbreaker.combat.api.kit;

import dev.sealbreaker.combat.kit.KitAttachments;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Set;

/**
 * Accessory equip/unequip contract (#20/#25). Server-thread mutations only; clients read the synced set.
 * Flags are derived equipment state, not permanent progression: regrant them on login/respawn/equip.
 * Grants are idempotent, not reference counted. An equipment reconciler must retain a flag while any
 * equipped source grants it and revoke only after the last source leaves.
 */
public final class KitCapabilities {
    public static final Identifier DASH = Identifier.fromNamespaceAndPath("sb_core", "dash");

    public static boolean has(Player player, Identifier capability) {
        Set<Identifier> flags = player.getExistingDataOrNull(KitAttachments.FLAGS.get());
        return flags != null && flags.contains(capability);
    }

    public static void grant(Player player, Identifier capability) {
        change(player, capability, true);
    }

    public static void revoke(Player player, Identifier capability) {
        change(player, capability, false);
    }

    private static void change(Player player, Identifier capability, boolean add) {
        if (!(player instanceof ServerPlayer server) || !server.level().getServer().isSameThread()) {
            throw new IllegalStateException("Kit capability changes require the server thread");
        }
        Set<Identifier> old = player.getExistingDataOrNull(KitAttachments.FLAGS.get());
        Set<Identifier> updated = new HashSet<>(old == null ? Set.of() : old);
        boolean changed = add ? updated.add(capability) : updated.remove(capability);
        if (changed) {
            player.setData(KitAttachments.FLAGS.get(), Set.copyOf(updated));
        }
    }

    private KitCapabilities() {
    }
}
