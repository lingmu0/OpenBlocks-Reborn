package net.xuwu.openblocks_reborn.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.xuwu.openblocks_reborn.menu.MachineMenu;

import java.util.function.Supplier;

public record RenameAutoAnvilPayload(int containerId, String itemName) {
    public static void encode(RenameAutoAnvilPayload payload, FriendlyByteBuf buffer) {
        buffer.writeVarInt(payload.containerId);
        buffer.writeUtf(payload.itemName, 50);
    }

    public static RenameAutoAnvilPayload decode(FriendlyByteBuf buffer) {
        return new RenameAutoAnvilPayload(buffer.readVarInt(), buffer.readUtf(50));
    }

    public static void handle(RenameAutoAnvilPayload payload, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getSender() != null && context.getSender().containerMenu instanceof MachineMenu menu) {
            menu.handleTextUpdate(payload.containerId(), payload.itemName());
        }
        context.setPacketHandled(true);
    }
}
