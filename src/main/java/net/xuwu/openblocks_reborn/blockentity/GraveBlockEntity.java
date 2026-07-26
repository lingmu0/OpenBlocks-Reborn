package net.xuwu.openblocks_reborn.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.xuwu.openblocks_reborn.registry.ModBlockEntities;

import java.util.Collection;
import java.util.UUID;

public class GraveBlockEntity extends BlockEntity {
    private final NonNullList<ItemStack> items = NonNullList.withSize(54, ItemStack.EMPTY);
    private UUID owner;
    private String ownerName = "";

    public GraveBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GRAVE.get(), pos, state);
    }

    public void setOwner(UUID owner, String ownerName) {
        this.owner = owner;
        this.ownerName = ownerName;
        setChanged();
    }

    public boolean canOpen(UUID player) {
        return owner == null || owner.equals(player);
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void store(Collection<ItemStack> drops) {
        for (ItemStack input : drops) insert(input.copy());
        setChanged();
    }

    private void insert(ItemStack stack) {
        for (int slot = 0; slot < items.size() && !stack.isEmpty(); slot++) {
            ItemStack existing = items.get(slot);
            if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, stack)) {
                int moved = Math.min(stack.getCount(), existing.getMaxStackSize() - existing.getCount());
                if (moved > 0) {
                    existing.grow(moved);
                    stack.shrink(moved);
                }
            }
        }
        for (int slot = 0; slot < items.size() && !stack.isEmpty(); slot++) {
            if (items.get(slot).isEmpty()) items.set(slot, stack.copyAndClear());
        }
    }

    public NonNullList<ItemStack> removeAll() {
        NonNullList<ItemStack> result = NonNullList.create();
        for (int slot = 0; slot < items.size(); slot++) {
            ItemStack stack = items.set(slot, ItemStack.EMPTY);
            if (!stack.isEmpty()) result.add(stack);
        }
        setChanged();
        return result;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, items, registries);
        if (tag.hasUUID("Owner")) owner = tag.getUUID("Owner");
        ownerName = tag.getString("OwnerName");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        if (owner != null) tag.putUUID("Owner", owner);
        tag.putString("OwnerName", ownerName);
    }
}
