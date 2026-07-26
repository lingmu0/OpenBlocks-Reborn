package net.xuwu.openblocks_reborn.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;

import java.util.function.BooleanSupplier;

public final class HandlerContainer implements Container {
    private final IItemHandlerModifiable handler;
    private final int paddedSize;
    private final BooleanSupplier valid;

    public HandlerContainer(IItemHandlerModifiable handler, int rows, BooleanSupplier valid) {
        this.handler = handler;
        this.paddedSize = rows * 9;
        this.valid = valid;
    }

    @Override
    public int getContainerSize() {
        return paddedSize;
    }

    @Override
    public boolean isEmpty() {
        for (int slot = 0; slot < handler.getSlots(); slot++) if (!handler.getStackInSlot(slot).isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot < handler.getSlots() ? handler.getStackInSlot(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return slot < handler.getSlots() ? handler.extractItem(slot, amount, false) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return slot < handler.getSlots() ? handler.extractItem(slot, Integer.MAX_VALUE, false) : ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < handler.getSlots()) handler.setStackInSlot(slot, stack);
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(Player player) {
        return valid.getAsBoolean();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot < handler.getSlots() && handler.isItemValid(slot, stack);
    }

    @Override
    public void clearContent() {
        for (int slot = 0; slot < handler.getSlots(); slot++) handler.setStackInSlot(slot, ItemStack.EMPTY);
    }
}
