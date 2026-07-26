package net.xuwu.openblocks_reborn.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.xuwu.openblocks_reborn.registry.ModBlockEntities;

public class BigButtonBlockEntity extends BlockEntity {
    private final ItemStackHandler inventory = new ItemStackHandler(8) {
        @Override
        protected void onContentsChanged(int slot) {
            BigButtonBlockEntity.this.setChanged();
        }
    };

    public BigButtonBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BIG_BUTTON.get(), pos, state);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public int getPressTicks() {
        int ticks = 0;
        for (int slot = 0; slot < inventory.getSlots(); slot++) ticks += inventory.getStackInSlot(slot).getCount();
        return Math.clamp(ticks, 1, 512);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", inventory.serializeNBT(registries));
    }
}
