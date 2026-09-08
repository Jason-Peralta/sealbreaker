package dev.sealbreaker.combat.client.anim;

import dev.sealbreaker.combat.SbCombat;
import dev.sealbreaker.combat.item.SbCombatItems;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Animation contact sheets without a person at the keyboard. Enabled by the system property
 * {@code sb.animdebug=<animation id>[,<animation id>...]} together with a quick-play world: once in the
 * world it puts the spike sword in hand (creative only), freezes each animation at a series of times,
 * screenshots every frame from behind, from behind at three-quarters, from the character's left side, from the front and in first person,
 * then quits. Output: {@code run/screenshots/anim_<name>_<view>_<index>.png}.
 */
public final class AnimDebug {
    /** The idle stance the first-person view moves relative to while debugging. */
    public static final Identifier DEBUG_IDLE = Identifier.fromNamespaceAndPath(SbCombat.MOD_ID, "sword_idle");
    private static final float STEP_SECONDS = 0.05f;
    private static final int SETTLE_TICKS = 60;
    private static final int TICKS_PER_FRAME = 2;

    private record View(String name, CameraType camera, float lookYaw) {
    }

    private static final View[] VIEWS = {
            new View("back", CameraType.THIRD_PERSON_BACK, 0.0f),
            new View("quarter", CameraType.THIRD_PERSON_BACK, 45.0f),
            new View("side", CameraType.THIRD_PERSON_BACK, 90.0f),
            new View("front", CameraType.THIRD_PERSON_FRONT, 0.0f),
            new View("fp", CameraType.FIRST_PERSON, 0.0f),
    };

    private record Frame(Identifier animation, float seconds, View view, String fileName) {
        boolean firstPerson() {
            return view.camera() == CameraType.FIRST_PERSON;
        }
    }

    private static final List<Frame> QUEUE = new ArrayList<>();
    /**
     * {@code sb.animdebug=combo[:fp|back|side]}: instead of frozen frames, play the real combo through the
     * server (attack requests held down for two seconds) and screenshot every other tick for three seconds,
     * as {@code combo_<view>_<NN>.png}. Exercises the whole pipeline: buffering, chaining, blending, trails,
     * the camera bone.
     */
    private static final int COMBO_REQUEST_TICKS = 40;
    private static final int COMBO_CAPTURE_TICKS = 70;
    private static boolean combo;
    private static View comboView = VIEWS[3];
    private static int comboTick = -1;
    private static boolean initialised;
    private static int settle;
    private static boolean reload;
    private static int reloadTicks;
    private static float lengthBefore;
    private static String originalJson;
    private static java.nio.file.Path reloadedFile;
    /** Both sweeps are 0.55 s long; the reload check stretches them and reads one back. */
    private static final String LENGTH_BEFORE_TEXT = "\"animation_length\": 0.55";
    private static final String LENGTH_AFTER_TEXT = "\"animation_length\": 0.95";
    private static int cursor = -1;
    private static int ticksOnFrame;
    /** The frame currently on screen; read by the render code. */
    private static Frame current;

    public static boolean enabled() {
        return System.getProperty("sb.animdebug") != null;
    }

    /** Animation and time to force on the local player this frame, or null. */
    public static Identifier overrideAnimation() {
        return current == null ? null : current.animation();
    }

    public static float overrideSeconds() {
        return current == null ? 0.0f : current.seconds();
    }

    public static boolean overrideFirstPerson() {
        return current != null && current.firstPerson();
    }

    /** Called every client tick. */
    public static void tick(Minecraft minecraft) {
        if (!enabled()) {
            return;
        }
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return;
        }
        if (!initialised) {
            initialised = true;
            String spec = System.getProperty("sb.animdebug");
            if (spec.equals("reload")) {
                reload = true;
                return;
            }
            if (spec.startsWith("combo")) {
                combo = true;
                String view = spec.contains(":") ? spec.substring(spec.indexOf(':') + 1) : "fp";
                for (View v : VIEWS) {
                    if (v.name().equals(view)) {
                        comboView = v;
                    }
                }
            } else {
                buildQueue();
            }
            if (player.isCreative()) {
                player.connection.sendCommand("time set 1000");
                player.connection.sendCommand("weather clear");
                ItemStack sword = new ItemStack(SbCombatItems.SPIKE_SWORD.get());
                player.getInventory().setSelectedSlot(0);
                minecraft.gameMode.handleCreativeModeItemAdd(sword, 36);
                player.getInventory().setItem(0, sword);
            }
            SbCombat.LOGGER.info("Animation debug: {} frames queued", QUEUE.size());
            return;
        }
        if (reload) {
            tickReload(minecraft);
            return;
        }
        if (settle < SETTLE_TICKS) {
            settle++;
            return;
        }
        if (combo) {
            tickCombo(minecraft, player);
            return;
        }
        if (ticksOnFrame < TICKS_PER_FRAME && cursor >= 0) {
            ticksOnFrame++;
            if (ticksOnFrame == TICKS_PER_FRAME) {
                Frame done = QUEUE.get(cursor);
                Screenshot.grab(minecraft.gameDirectory, done.fileName(), minecraft.gameRenderer.mainRenderTarget(), 1, c -> {
                });
            }
            return;
        }
        cursor++;
        ticksOnFrame = 0;
        if (cursor >= QUEUE.size()) {
            SbCombat.LOGGER.info("Animation debug: done, quitting");
            current = null;
            minecraft.stop();
            return;
        }
        current = QUEUE.get(cursor);
        minecraft.options.setCameraType(current.view().camera());
        // The body faces the same way for every view; the third-person camera follows the look direction.
        float look = current.view().lookYaw();
        player.setYRot(look);
        player.yRotO = look;
        player.setXRot(0.0f);
        player.xRotO = 0.0f;
        player.yBodyRot = 0.0f;
        player.yBodyRotO = 0.0f;
        player.yHeadRot = look;
        player.yHeadRotO = look;
    }

    private static void tickCombo(Minecraft minecraft, LocalPlayer player) {
        comboTick++;
        if (comboTick == 0) {
            minecraft.options.setCameraType(comboView.camera());
            float look = comboView.lookYaw();
            player.setYRot(look);
            player.yRotO = look;
            player.setXRot(0.0f);
            player.xRotO = 0.0f;
            player.yBodyRot = 0.0f;
            player.yBodyRotO = 0.0f;
            player.yHeadRot = look;
            player.yHeadRotO = look;
        }
        if (comboTick < COMBO_REQUEST_TICKS) {
            net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new dev.sealbreaker.combat.network.SwingRequestPayload(dev.sealbreaker.core.api.combat.AttackContext.TAP, false));
            net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new dev.sealbreaker.combat.network.SwingRequestPayload(dev.sealbreaker.core.api.combat.AttackContext.TAP, true));
        }
        if (comboTick % 2 == 0) {
            Screenshot.grab(minecraft.gameDirectory, String.format(Locale.ROOT, "combo_%s_%02d.png", comboView.name(), comboTick / 2),
                    minecraft.gameRenderer.mainRenderTarget(), 1, c -> {
                    });
        }
        if (comboTick >= COMBO_CAPTURE_TICKS) {
            SbCombat.LOGGER.info("Animation debug: combo done, quitting");
            minecraft.stop();
        }
    }

    private static void buildQueue() {
        String[] ids = System.getProperty("sb.animdebug").split(",");
        for (String raw : ids) {
            String trimmed = raw.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            Identifier id = Identifier.parse(trimmed);
            PlayerAnimation animation = PlayerAnimations.get(id);
            if (animation == null) {
                SbCombat.LOGGER.warn("Animation debug: unknown animation {}", id);
                continue;
            }
            int frames = Math.max(1, Math.round(animation.length() / STEP_SECONDS)) + 1;
            String base = id.getPath().toLowerCase(Locale.ROOT);
            for (View view : VIEWS) {
                for (int i = 0; i < frames; i++) {
                    QUEUE.add(new Frame(id, Math.min(animation.length(), i * STEP_SECONDS), view,
                            String.format(Locale.ROOT, "anim_%s_%s_%02d.png", base, view.name(), i)));
                }
            }
        }
    }

    private AnimDebug() {
    }

    /**
     * {@code -Psb.animdebug=reload}: the resource-reload loop of ticket #4 without a person. Reads a move's length,
     * edits the animation JSON where the run reads it from (the processResources output, which IntelliJ's Build
     * refreshes from src), presses F3+T through {@code reloadResourcePacks}, reads the length again, restores the
     * file and quits. The log line "reload: ... before X after Y" is the verdict.
     */
    private static void tickReload(Minecraft minecraft) {
        Identifier id = Identifier.fromNamespaceAndPath(SbCombat.MOD_ID, "sword_sweep_ltr");
        reloadTicks++;
        try {
            if (reloadTicks == 20) {
                lengthBefore = PlayerAnimations.get(id).length();
                reloadedFile = minecraft.gameDirectory.toPath().resolve("../build/resources/main/assets/sb_combat/sb_animations/player/sword.json").normalize();
                originalJson = java.nio.file.Files.readString(reloadedFile);
                String edited = originalJson.replace(LENGTH_BEFORE_TEXT, LENGTH_AFTER_TEXT);
                if (edited.equals(originalJson)) {
                    throw new IllegalStateException("the sweep's animation_length was not found where expected in " + reloadedFile);
                }
                java.nio.file.Files.writeString(reloadedFile, edited);
                SbCombat.LOGGER.info("Animation debug: reload: edited {} and pressing F3+T", reloadedFile);
                minecraft.reloadResourcePacks();
            } else if (reloadTicks == 200) {
                float after = PlayerAnimations.get(id).length();
                java.nio.file.Files.writeString(reloadedFile, originalJson);
                SbCombat.LOGGER.info("Animation debug: reload: sword_sweep_ltr length before {} after {} ({})", lengthBefore, after,
                        after != lengthBefore ? "the reload picked up the edit" : "the reload did NOT pick up the edit");
                minecraft.stop();
            }
        } catch (java.io.IOException | RuntimeException e) {
            SbCombat.LOGGER.error("Animation debug: reload failed", e);
            if (originalJson != null && reloadedFile != null) {
                try {
                    java.nio.file.Files.writeString(reloadedFile, originalJson);
                } catch (java.io.IOException ignored) {
                }
            }
            minecraft.stop();
        }
    }
}
