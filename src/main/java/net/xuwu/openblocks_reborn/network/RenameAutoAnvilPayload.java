package net.xuwu.openblocks_reborn.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xuwu.openblocks_reborn.OpenBlocksReborn;
import net.xuwu.openblocks_reborn.menu.MachineMenu;

public record RenameAutoAnvilPayload(int containerId, String itemName) implements CustomPacketPayload {
    public static final Type<RenameAutoAnvilPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(OpenBlocksReborn.MOD_ID, "rename_auto_anvil"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RenameAutoAnvilPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, RenameAutoAnvilPayload::containerId,
                    ByteBufCodecs.stringUtf8(50), RenameAutoAnvilPayload::itemName,
                    RenameAutoAnvilPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RenameAutoAnvilPayload payload, IPayloadContext context) {
        if (context.player().containerMenu instanceof MachineMenu menu) {
            menu.handleTextUpdate(payload.containerId(), payload.itemName());
        }
    }
}
