package net.xuwu.openblocks_reborn.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.xuwu.openblocks_reborn.registry.ModMenus;
import net.xuwu.openblocks_reborn.item.CursorItem;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntPredicate;

public final class MachineMenu extends AbstractContainerMenu {
    public static final int DATA_COUNT = 10;

    private final MachineLayout layout;
    private final BooleanSupplier valid;
    private final ContainerData data;
    private final IntPredicate buttonHandler;
    private final Consumer<String> textHandler;
    private final IItemHandlerModifiable machineHandler;

    public MachineMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, MachineLayout.fromNetwork(extraData.readVarInt()));
    }

    private MachineMenu(int containerId, Inventory playerInventory, MachineLayout layout) {
        this(containerId, playerInventory, new ItemStackHandler(layout.machineSlotCount()), layout,
                () -> true, new SimpleContainerData(DATA_COUNT), ignored -> true, ignored -> { });
    }

    public MachineMenu(int containerId, Inventory playerInventory, IItemHandlerModifiable handler,
                       MachineLayout layout, BooleanSupplier valid, ContainerData data) {
        this(containerId, playerInventory, handler, layout, valid, data, ignored -> false);
    }

    public MachineMenu(int containerId, Inventory playerInventory, IItemHandlerModifiable handler,
                       MachineLayout layout, BooleanSupplier valid, ContainerData data,
                       IntPredicate buttonHandler) {
        this(containerId, playerInventory, handler, layout, valid, data, buttonHandler, ignored -> { });
    }

    public MachineMenu(int containerId, Inventory playerInventory, IItemHandlerModifiable handler,
                       MachineLayout layout, BooleanSupplier valid, ContainerData data,
                       IntPredicate buttonHandler, Consumer<String> textHandler) {
        super(ModMenus.MACHINE.get(), containerId);
        if (handler.getSlots() != layout.machineSlotCount()) {
            throw new IllegalArgumentException("Machine layout " + layout + " expects "
                    + layout.machineSlotCount() + " slots, got " + handler.getSlots());
        }
        this.layout = layout;
        this.valid = valid;
        this.data = data;
        this.buttonHandler = buttonHandler;
        this.textHandler = textHandler;
        this.machineHandler = handler;
        checkContainerDataCount(data, DATA_COUNT);

        for (int slot = 0; slot < handler.getSlots(); slot++) {
            addSlot(new SlotItemHandler(handler, slot, layout.machineSlotX(slot), layout.machineSlotY(slot)));
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9,
                        layout.playerX() + column * 18, layout.playerY() + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, layout.playerX() + column * 18, layout.playerY() + 58));
        }
        addDataSlots(data);
    }

    public MachineLayout layout() {
        return layout;
    }

    public int data(int index) {
        return data.get(index);
    }

    public void handleTextUpdate(int requestedContainerId, String value) {
        if (requestedContainerId == containerId && layout == MachineLayout.AUTO_ANVIL) {
            textHandler.accept(value);
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        return buttonHandler.test(id);
    }

    @Override
    public boolean stillValid(Player player) {
        return valid.getAsBoolean() || CursorItem.isRemoteMenuValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        int machineSlots = layout.machineSlotCount();
        if (layout == MachineLayout.DRAWING_TABLE && index == 1) {
            ItemStack available = machineHandler.extractItem(1, slot.getItem().getCount(), true);
            if (available.isEmpty()) return ItemStack.EMPTY;
            ItemStack moving = available.copy();
            if (!moveItemStackTo(moving, machineSlots, slots.size(), true)) return ItemStack.EMPTY;
            int moved = available.getCount() - moving.getCount();
            return moved > 0 ? machineHandler.extractItem(1, moved, false) : ItemStack.EMPTY;
        }
        ItemStack source = slot.getItem();
        ItemStack copy = source.copy();
        if (index < machineSlots) {
            if (!moveItemStackTo(source, machineSlots, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(source, 0, machineSlots, false)) {
            return ItemStack.EMPTY;
        }
        if (source.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }
}
