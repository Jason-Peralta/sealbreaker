package dev.sealbreaker.world.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * A two-tall door that opens by hand only after its key item has been used on it, and never by redstone. The key
 * id lives in the lower half's block entity so it can come from a structure template. Vanilla's {@link DoorBlock}
 * supplies the shape, the halves, the hinge and the open animation; the iron set type makes vanilla treat the
 * door as one that hands cannot open, so only our interaction path opens it.
 */
public final class LockedDoorBlock extends DoorBlock implements EntityBlock {
    public static final MapCodec<LockedDoorBlock> CODEC = simpleCodec(LockedDoorBlock::new);

    public LockedDoorBlock(Properties properties) {
        super(BlockSetType.IRON, properties);
    }

    @Override
    public MapCodec<LockedDoorBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new LockedDoorBlockEntity(pos, state) : null;
    }

    /** The lower half's block entity, whichever half was clicked. */
    public static @Nullable LockedDoorBlockEntity lock(Level level, BlockPos pos, BlockState state) {
        BlockPos lower = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
        return level.getBlockEntity(lower) instanceof LockedDoorBlockEntity lock ? lock : null;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        LockedDoorBlockEntity lock = lock(level, pos, state);
        if (lock == null || lock.isUnlocked() || !lock.opens(stack)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide()) {
            lock.unlock();
            this.setOpen(player, level, state, pos, true);
            level.playSound(null, pos, SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 1.0f, 0.8f);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        LockedDoorBlockEntity lock = lock(level, pos, state);
        if (lock == null || !lock.isUnlocked()) {
            if (!level.isClientSide()) {
                level.playSound(null, pos, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
            return InteractionResult.FAIL;
        }
        this.setOpen(player, level, state, pos, !this.isOpen(state));
        return InteractionResult.SUCCESS;
    }

    /** Redstone never moves a locked door. */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
    }
}
