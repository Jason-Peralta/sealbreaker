package dev.sealbreaker.combat.client.anim;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/** Small lit platform, only called in the fresh worlds created by sword capture runs. Server thread only. */
final class SwordCaptureStage {
    static void prepare(ServerLevel level, BlockPos centre) {
        for (int x = -8; x <= 8; x++) {
            for (int z = -8; z <= 8; z++) {
                BlockPos floor = centre.offset(x, -1, z);
                level.getChunkAt(floor);
                level.setBlockAndUpdate(floor, Blocks.SMOOTH_STONE.defaultBlockState());
                for (int y = 0; y <= 8; y++) {
                    level.setBlockAndUpdate(centre.offset(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    private SwordCaptureStage() {
    }
}
