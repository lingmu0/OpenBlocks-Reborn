package net.xuwu.openblocks_reborn.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xuwu.openblocks_reborn.OpenBlocksReborn;
import net.xuwu.openblocks_reborn.item.GoldenEyeItem;
import net.xuwu.openblocks_reborn.menu.GoldenEyeMenu;

public record GoldenEyeSelectPayload(int containerId, String structureId)
        implements CustomPacketPayload {
    public static final Type<GoldenEyeSelectPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(OpenBlocksReborn.MOD_ID, "golden_eye_select"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GoldenEyeSelectPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, GoldenEyeSelectPayload::containerId,
                    ByteBufCodecs.stringUtf8(256), GoldenEyeSelectPayload::structureId,
                    GoldenEyeSelectPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(GoldenEyeSelectPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || !(player.containerMenu instanceof GoldenEyeMenu menu)
                || menu.containerId != payload.containerId()) return;
        ResourceLocation structureId = ResourceLocation.tryParse(payload.structureId());
        if (structureId == null || !menu.structures().contains(structureId)) return;
        if (GoldenEyeItem.beginBindingStructure(player, menu.hand(), structureId)) player.closeContainer();
    }
}
