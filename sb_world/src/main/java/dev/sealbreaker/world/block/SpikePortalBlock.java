package dev.sealbreaker.world.block;

import com.mojang.serialization.MapCodec;
import dev.sealbreaker.world.SbWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Spike S3: a walk-in block that moves whatever enters it between the overworld and {@code sb_world:spike_realm}
 * through the vanilla {@link Portal} contract. The destination keeps the entity's x and z (coordinate scale 1),
 * lands on the surface, and lays a portal block there so the way back exists. Vanilla owns the rest: the stand-in
 * time ({@link #getPortalTransitionTime}), the cooldown after arrival, chunk tickets at the destination, and the
 * cross-dimension transfer of players, mobs and their passengers.
 *
 * <p>Real gates (PRD 3.12) will read their target realm, transition time and Seal gate from the {@code sb:realm}
 * registry; this block hard-wires the spike realm and is not content.
 */
public final class SpikePortalBlock extends Block implements Portal {
    public static final MapCodec<SpikePortalBlock> CODEC = simpleCodec(SpikePortalBlock::new);
    public static final ResourceKey<Level> REALM = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
            Identifier.fromNamespaceAndPath(SbWorld.MOD_ID, "spike_realm"));
    /** Ticks an entity stands in the block before it goes; a realm's JSON will own this number. */
    private static final int TRANSITION_TICKS = 20;

    public SpikePortalBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<SpikePortalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (entity.canUsePortal(false)) {
            entity.setAsInsidePortal(this, pos);
        }
    }

    @Override
    public int getPortalTransitionTime(ServerLevel level, Entity entity) {
        return TRANSITION_TICKS;
    }

    @Override
    public @Nullable TeleportTransition getPortalDestination(ServerLevel currentLevel, Entity entity, BlockPos portalEntryPos) {
        ResourceKey<Level> targetKey = currentLevel.dimension() == REALM ? Level.OVERWORLD : REALM;
        ServerLevel target = currentLevel.getServer().getLevel(targetKey);
        if (target == null) {
            SbWorld.LOGGER.warn("Spike portal: dimension {} is not loaded, staying put", targetKey.identifier());
            return null;
        }
        int x = portalEntryPos.getX();
        int z = portalEntryPos.getZ();
        target.getChunk(x >> 4, z >> 4);
        int surface = target.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        BlockPos landing = new BlockPos(x, surface, z);
        if (target.getBlockState(landing).canBeReplaced()) {
            target.setBlockAndUpdate(landing, this.defaultBlockState());
        }
        return new TeleportTransition(target, Vec3.atBottomCenterOf(landing), Vec3.ZERO, entity.getYRot(), entity.getXRot(),
                TeleportTransition.PLAY_PORTAL_SOUND.then(TeleportTransition.PLACE_PORTAL_TICKET));
    }
}
