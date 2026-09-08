package dev.sealbreaker.combat.kit;

import dev.sealbreaker.combat.SbCombat;
import dev.sealbreaker.combat.api.kit.KitCapabilities;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** A ground dash resolved through normal block collision, with server position confirmation each step. */
@EventBusSubscriber(modid = SbCombat.MOD_ID)
public final class DashService {
    private static final class Runtime {
        final ServerPlayer player;
        final Object level;
        long cooldownUntil;
        long lastMovedTick = Long.MIN_VALUE;
        Vec3 step = Vec3.ZERO;

        Runtime(ServerPlayer player) {
            this.player = player;
            this.level = player.level();
        }
    }

    private static final Map<UUID, Runtime> RUNTIME = new HashMap<>();

    private static boolean eligible(ServerPlayer player) {
        return player.isAlive() && !player.isSpectator() && !player.isPassenger() && !player.isSleeping()
                && !player.getAbilities().flying && player.containerMenu == player.inventoryMenu
                && KitCapabilities.has(player, KitCapabilities.DASH);
    }

    /** No client-supplied direction, distance or time; holding the key cannot bypass cooldown. */
    public static boolean request(ServerPlayer player) {
        if (!eligible(player) || !player.onGround()) {
            return false;
        }
        Runtime runtime = RUNTIME.get(player.getUUID());
        if (runtime == null || runtime.player != player || runtime.level != player.level()) {
            runtime = new Runtime(player);
            RUNTIME.put(player.getUUID(), runtime);
        }
        long now = player.level().getGameTime();
        if (now < runtime.cooldownUntil) {
            return false;
        }
        DashSettings settings = DashData.get();
        double yaw = Math.toRadians(player.getYRot());
        runtime.step = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw)).scale(settings.distance() / settings.durationTicks());
        runtime.cooldownUntil = now + settings.cooldownTicks();
        runtime.lastMovedTick = Long.MIN_VALUE;
        player.setData(KitAttachments.DASH.get(), new DashState(now, settings.durationTicks(), settings.leanDegrees()));
        tick(player);
        return true;
    }

    /** Public for the same authoritative tick path in GameTests. */
    public static void tick(ServerPlayer player) {
        Runtime runtime = RUNTIME.get(player.getUUID());
        if (runtime == null) {
            return;
        }
        if (runtime.player != player || runtime.level != player.level() || !player.isAlive()) {
            RUNTIME.remove(player.getUUID());
            player.removeData(KitAttachments.DASH.get());
            return;
        }
        DashState state = player.getExistingDataOrNull(KitAttachments.DASH.get());
        long now = player.level().getGameTime();
        if (state == null) {
            return;
        }
        if (!eligible(player) || now - state.startTick() >= state.durationTicks()) {
            player.removeData(KitAttachments.DASH.get());
            return;
        }
        if (runtime.lastMovedTick == now) {
            return;
        }
        runtime.lastMovedTick = now;
        player.move(MoverType.SELF, runtime.step);
        boolean blocked = player.horizontalCollision;
        Vec3 after = player.position();
        player.connection.teleport(after.x, after.y, after.z, player.getYRot(), player.getXRot());
        // Stop at an obstruction rather than spending the rest of the dash pressed into it.
        if (blocked) {
            player.removeData(KitAttachments.DASH.get());
        }
    }

    @SubscribeEvent
    static void playerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            tick(player);
        }
    }

    @SubscribeEvent
    static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        RUNTIME.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    static void stopped(ServerStoppedEvent event) {
        RUNTIME.clear();
    }

    private DashService() {
    }
}
