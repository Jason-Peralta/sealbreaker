package dev.sealbreaker.world.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The lower half of a locked door remembers which item opens it and whether it has been unlocked. The key id is
 * data: structure templates carry it as block NBT ({@code {"key": "sb_world:spike_key"}}), so one block serves
 * every dungeon with a different key each.
 */
public final class LockedDoorBlockEntity extends BlockEntity {
    public static final String KEY_TAG = "key";
    public static final String UNLOCKED_TAG = "unlocked";

    private @Nullable Identifier key;
    private boolean unlocked;

    public LockedDoorBlockEntity(BlockPos pos, BlockState state) {
        super(SbWorldBlockEntities.LOCKED_DOOR.get(), pos, state);
    }

    /** The item that opens this door, or null for a door that never opens by hand. */
    public @Nullable Identifier key() {
        return this.key;
    }

    public boolean isUnlocked() {
        return this.unlocked;
    }

    public boolean opens(ItemStack stack) {
        return this.key != null && !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(this.key);
    }

    public void setKey(@Nullable Identifier key) {
        this.key = key;
        this.setChanged();
    }

    public void unlock() {
        this.unlocked = true;
        this.setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (this.key != null) {
            output.putString(KEY_TAG, this.key.toString());
        }
        output.putBoolean(UNLOCKED_TAG, this.unlocked);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.key = input.getString(KEY_TAG).map(Identifier::tryParse).orElse(null);
        this.unlocked = input.getBooleanOr(UNLOCKED_TAG, false);
    }
}
