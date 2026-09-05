package dev.sealbreaker.combat.client.anim;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * The arc a blade leaves behind while it is cutting: a translucent, additive ribbon between where the blade
 * was over the last few ticks and where it is now, drawn in world space for every avatar mid-strike (the
 * local player's included, so the arc shows in first person along the path the hit test walks). Sampled
 * once per frame from the rig's fist frame; rendered with the level's custom geometry pass.
 */
public final class SwingTrails {
    /** How long a point of the arc stays visible, ticks. */
    private static final float LIFETIME_TICKS = 6.0f;
    private static final int MAX_SAMPLES = 48;
    private static final float RED = 0.86f;
    private static final float GREEN = 0.92f;
    private static final float BLUE = 1.0f;
    private static final float TIP_ALPHA = 0.9f;
    private static final float BASE_ALPHA = 0.15f;

    private record Sample(Vec3 base, Vec3 tip, float atTicks) {
    }

    private static final Map<Integer, ArrayDeque<Sample>> TRAILS = new HashMap<>();

    /** Records where the blade is this frame, world space. */
    public static void sample(int entityId, Vec3 base, Vec3 tip, float nowTicks) {
        ArrayDeque<Sample> trail = TRAILS.computeIfAbsent(entityId, k -> new ArrayDeque<>());
        Sample last = trail.peekLast();
        if (last != null && last.atTicks() >= nowTicks) {
            return;
        }
        trail.addLast(new Sample(base, tip, nowTicks));
        while (trail.size() > MAX_SAMPLES) {
            trail.pollFirst();
        }
    }

    /** Draws every live arc; {@code poseStack} is at the camera-relative world origin. */
    public static void render(PoseStack poseStack, SubmitNodeCollector collector, Vec3 cameraPos, float nowTicks) {
        Iterator<Map.Entry<Integer, ArrayDeque<Sample>>> entries = TRAILS.entrySet().iterator();
        while (entries.hasNext()) {
            ArrayDeque<Sample> trail = entries.next().getValue();
            while (!trail.isEmpty() && nowTicks - trail.peekFirst().atTicks() > LIFETIME_TICKS) {
                trail.pollFirst();
            }
            if (trail.size() < 2) {
                if (trail.isEmpty()) {
                    entries.remove();
                }
                continue;
            }
            Sample[] samples = trail.toArray(new Sample[0]);
            collector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, buffer) -> {
                for (int i = 0; i + 1 < samples.length; i++) {
                    float fadeA = fade(nowTicks - samples[i].atTicks());
                    float fadeB = fade(nowTicks - samples[i + 1].atTicks());
                    quad(buffer, pose, cameraPos, samples[i], samples[i + 1], fadeA, fadeB);
                }
            });
        }
    }

    private static float fade(float age) {
        float t = 1.0f - Math.min(1.0f, Math.max(0.0f, age / LIFETIME_TICKS));
        return t * t;
    }

    /** One slice of the ribbon, both windings so it shows from either side of the cut. */
    private static void quad(VertexConsumer buffer, PoseStack.Pose pose, Vec3 camera, Sample a, Sample b, float fadeA, float fadeB) {
        vertex(buffer, pose, camera, a.base(), BASE_ALPHA * fadeA);
        vertex(buffer, pose, camera, a.tip(), TIP_ALPHA * fadeA);
        vertex(buffer, pose, camera, b.tip(), TIP_ALPHA * fadeB);
        vertex(buffer, pose, camera, b.base(), BASE_ALPHA * fadeB);
        vertex(buffer, pose, camera, b.base(), BASE_ALPHA * fadeB);
        vertex(buffer, pose, camera, b.tip(), TIP_ALPHA * fadeB);
        vertex(buffer, pose, camera, a.tip(), TIP_ALPHA * fadeA);
        vertex(buffer, pose, camera, a.base(), BASE_ALPHA * fadeA);
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, Vec3 camera, Vec3 world, float alpha) {
        buffer.addVertex(pose, (float) (world.x - camera.x), (float) (world.y - camera.y), (float) (world.z - camera.z))
                .setColor(RED, GREEN, BLUE, alpha);
    }

    public static void clear() {
        TRAILS.clear();
    }

    private SwingTrails() {
    }
}
