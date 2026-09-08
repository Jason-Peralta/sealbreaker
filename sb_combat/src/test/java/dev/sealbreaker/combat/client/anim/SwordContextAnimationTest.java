package dev.sealbreaker.combat.client.anim;

import com.google.gson.JsonParser;
import net.minecraft.world.entity.HumanoidArm;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class SwordContextAnimationTest {
    @Test
    void plungeBladePointsDownDuringTheHeldActivePoseForBothHands() throws Exception {
        try (var stream = getClass().getResourceAsStream("/assets/sb_combat/sb_animations/player/sword.json")) {
            assertNotNull(stream);
            var json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            var animation = PlayerAnimation.parse(json.getAsJsonObject("animations").getAsJsonObject("sword_plunge"));
            for (HumanoidArm arm : HumanoidArm.values()) {
                for (float time : new float[]{0.20f, 0.25f, 0.30f}) {
                    var blade = HumanoidRig.bladeInWorld(new PoseSource.AnimationAt(animation, time), arm, 0, 0, 0, 0);
                    var direction = blade[1].subtract(blade[0]).normalize();
                    assertTrue(direction.y < -0.7, "plunge blade must point down, got " + direction + " for " + arm);
                }
            }
        }
    }
}
