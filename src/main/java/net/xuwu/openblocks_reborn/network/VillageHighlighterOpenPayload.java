package net.xuwu.openblocks_reborn.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record VillageHighlighterOpenPayload(BlockPos anchor) implements CustomPacketPayload {
    public static final Type<VillageHighlighterOpenPayload> TYPE = new Type<>(ResourceLocation.parse("openblocks_reborn:highlighter_open"));
    public static final StreamCodec<RegistryFriendlyByteBuf, VillageHighlighterOpenPayload> STREAM_CODEC =
            StreamCodec.composite(BlockPos.STREAM_CODEC, VillageHighlighterOpenPayload::anchor, VillageHighlighterOpenPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(VillageHighlighterOpenPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> net.xuwu.openblocks_reborn.client.VillageHighlighterClient.open(payload.anchor()));
    }
}
