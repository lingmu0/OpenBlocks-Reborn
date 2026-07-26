package net.xuwu.openblocks_reborn.menu;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ContainerData;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.network.NetworkHooks;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.IntPredicate;

public final class MenuHelper {
    public static void open(ServerPlayer player, Component title, IItemHandlerModifiable handler,
                            MachineLayout layout, BooleanSupplier valid) {
        open(player, title, handler, layout, valid, data(() -> 0, () -> 0, () -> 0, () -> 0));
    }

    public static void open(ServerPlayer player, Component title, IItemHandlerModifiable handler,
                            MachineLayout layout, BooleanSupplier valid, ContainerData data) {
        open(player, title, handler, layout, valid, data, ignored -> false);
    }

    public static void open(ServerPlayer player, Component title, IItemHandlerModifiable handler,
                            MachineLayout layout, BooleanSupplier valid, ContainerData data,
                            IntPredicate buttonHandler) {
        open(player, title, handler, layout, valid, data, buttonHandler, ignored -> { });
    }

    public static void open(ServerPlayer player, Component title, IItemHandlerModifiable handler,
                            MachineLayout layout, BooleanSupplier valid, ContainerData data,
                            IntPredicate buttonHandler, Consumer<String> textHandler) {
        NetworkHooks.openScreen(player, new SimpleMenuProvider((containerId, inventory, ignored) ->
                new MachineMenu(containerId, inventory, handler, layout, valid, data,
                        buttonHandler, textHandler), title),
                buffer -> buffer.writeVarInt(layout.ordinal()));
    }

    public static ContainerData data(IntSupplier... suppliedValues) {
        IntSupplier[] values = new IntSupplier[MachineMenu.DATA_COUNT];
        for (int index = 0; index < values.length; index++) values[index] = () -> 0;
        System.arraycopy(suppliedValues, 0, values, 0,
                Math.min(suppliedValues.length, values.length));
        return new ContainerData() {
            @Override
            public int get(int index) {
                return values[index].getAsInt();
            }

            @Override
            public void set(int index, int value) {
                // The server owns machine state; the menu only synchronizes it to the screen.
            }

            @Override
            public int getCount() {
                return values.length;
            }
        };
    }

    private MenuHelper() {
    }
}
