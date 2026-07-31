package net.xuwu.openblocks_reborn.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.xuwu.openblocks_reborn.item.GoldenEyeItem;
import net.xuwu.openblocks_reborn.menu.GoldenEyeMenu;

import java.util.function.Supplier;

public record GoldenEyeSelectPayload(int containerId, String structureId) {
    public static void encode(GoldenEyeSelectPayload payload, FriendlyByteBuf buffer) {
        buffer.writeVarInt(payload.containerId);
        buffer.writeUtf(payload.structureId, 256);
    }

    public static GoldenEyeSelectPayload decode(FriendlyByteBuf buffer) {
        return new GoldenEyeSelectPayload(buffer.readVarInt(), buffer.readUtf(256));
    }

    public static void handle(GoldenEyeSelectPayload payload, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null
                && player.containerMenu instanceof GoldenEyeMenu menu
                && menu.containerId == payload.containerId()) {
            ResourceLocation structureId = ResourceLocation.tryParse(payload.structureId());
            if (structureId != null
                    && menu.structures().contains(structureId)
                    && GoldenEyeItem.beginBindingStructure(player, menu.hand(), structureId)) {
                player.closeContainer();
            }
        }
        context.setPacketHandled(true);
    }
}
