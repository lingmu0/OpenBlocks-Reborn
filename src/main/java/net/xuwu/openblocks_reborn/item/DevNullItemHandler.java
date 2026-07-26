package net.xuwu.openblocks_reborn.item;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

public final class DevNullItemHandler implements IItemHandlerModifiable {
    private final ItemStack container;

    public DevNullItemHandler(ItemStack container) {
        this.container = container;
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        if (slot == 0) DevNullItem.setStored(container, stack);
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return slot == 0 ? DevNullItem.getStored(container) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (slot != 0 || stack.isEmpty() || stack.getItem() instanceof DevNullItem) return stack;
        ItemStack stored = getStackInSlot(0);
        if (!stored.isEmpty() && !ItemStack.isSameItemSameComponents(stored, stack)) return stack;
        int space = stack.getMaxStackSize() - stored.getCount();
        int moved = Math.min(space, stack.getCount());
        if (!simulate && moved > 0) {
            ItemStack updated = stored.isEmpty() ? stack.copyWithCount(moved) : stored.copy();
            if (!stored.isEmpty()) updated.grow(moved);
            setStackInSlot(0, updated);
        }
        return stack.copyWithCount(stack.getCount() - moved);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        ItemStack stored = getStackInSlot(slot);
        if (stored.isEmpty()) return ItemStack.EMPTY;
        int moved = Math.min(amount, stored.getCount());
        ItemStack result = stored.copyWithCount(moved);
        if (!simulate) {
            stored.shrink(moved);
            setStackInSlot(slot, stored);
        }
        return result;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 64;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return slot == 0 && !(stack.getItem() instanceof DevNullItem);
    }
}
