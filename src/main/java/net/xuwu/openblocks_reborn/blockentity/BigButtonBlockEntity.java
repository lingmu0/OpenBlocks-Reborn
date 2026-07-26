package net.xuwu.openblocks_reborn.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;
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

    @Override
    public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
            net.minecraftforge.common.capabilities.Capability<T> capability,
            @javax.annotation.Nullable net.minecraft.core.Direction side) {
        if (capability == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER) {
            return net.minecraftforge.common.util.LazyOptional
                    .<net.minecraftforge.items.IItemHandler>of(() -> inventory).cast();
        }
        return super.getCapability(capability, side);
    }

    public int getPressTicks() {
        int ticks = 0;
        for (int slot = 0; slot < inventory.getSlots(); slot++) ticks += inventory.getStackInSlot(slot).getCount();
        return net.minecraft.util.Mth.clamp(ticks, 1, 512);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inventory.deserializeNBT(tag.getCompound("Inventory"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", inventory.serializeNBT());
    }
}
