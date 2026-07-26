package net.xuwu.openblocks_reborn.blockentity;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.function.IntPredicate;

/**
 * Capability view that exposes only the insertion and extraction slots enabled
 * for one configured machine face. The GUI itself keeps using the full handler.
 */
final class ConfiguredItemHandler implements IItemHandler {
    private final ItemStackHandler delegate;
    private final IntPredicate canInsert;
    private final IntPredicate canExtract;

    ConfiguredItemHandler(ItemStackHandler delegate, IntPredicate canInsert, IntPredicate canExtract) {
        this.delegate = delegate;
        this.canInsert = canInsert;
        this.canExtract = canExtract;
    }

    @Override
    public int getSlots() {
        return delegate.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return delegate.getStackInSlot(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return canInsert.test(slot) ? delegate.insertItem(slot, stack, simulate) : stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return canExtract.test(slot) ? delegate.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return delegate.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return canInsert.test(slot) && delegate.isItemValid(slot, stack);
    }
}
