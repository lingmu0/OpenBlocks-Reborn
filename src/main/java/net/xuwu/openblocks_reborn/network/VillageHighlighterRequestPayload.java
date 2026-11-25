package net.xuwu.openblocks_reborn.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xuwu.openblocks_reborn.registry.ModBlocks;
import net.xuwu.openblocks_reborn.village.VillageHighlighterData;
import net.xuwu.openblocks_reborn.village.HighlighterQueryLimiter;

public record VillageHighlighterRequestPayload(BlockPos anchor, BlockPos selected, boolean all,
                                               int radius, boolean villageLayer) implements CustomPacketPayload {
    public static final Type<VillageHighlighterRequestPayload> TYPE = new Type<>(ResourceLocation.parse("openblocks_reborn:highlighter_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, VillageHighlighterRequestPayload> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> { buf.writeBlockPos(value.anchor); buf.writeBlockPos(value.selected); buf.writeBoolean(value.all);
                buf.writeVarInt(value.radius); buf.writeBoolean(value.villageLayer); },
            buf -> new VillageHighlighterRequestPayload(buf.readBlockPos(), buf.readBlockPos(), buf.readBoolean(), buf.readVarInt(), buf.readBoolean()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    private static final java.util.Map<ServerPlayer, HighlighterQueryLimiter> LIMITERS = new java.util.WeakHashMap<>();

    public static boolean validAnchor(ServerPlayer player, BlockPos anchor) {
        return player.isAlive() && player.serverLevel().hasChunkAt(anchor)
                && player.distanceToSqr(anchor.getCenter()) <= 64 * 64
                && player.serverLevel().getBlockState(anchor).is(ModBlocks.VILLAGE_HIGHLIGHTER.get());
    }

    public static void handle(VillageHighlighterRequestPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !validAnchor(player, payload.anchor)) return;
        if (!LIMITERS.computeIfAbsent(player, ignored -> new HighlighterQueryLimiter())
                .acquire(player.getServer().getTickCount(), payload.all)) return;
        var snapshot = VillageHighlighterData.collect(player.serverLevel(), payload.anchor,
                payload.selected, payload.all, payload.radius, payload.villageLayer);
        PacketDistributor.sendToPlayer(player, new VillageHighlighterSnapshotPayload(
                payload.anchor, payload.selected, payload.all, snapshot));
    }
}
