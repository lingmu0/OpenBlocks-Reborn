package net.xuwu.openblocks_reborn.network;

import io.netty.handler.codec.DecoderException;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xuwu.openblocks_reborn.village.VillageHighlighterData;

import java.util.ArrayList;

public record VillageHighlighterSnapshotPayload(BlockPos anchor, BlockPos selected, boolean all,
                                               VillageHighlighterData.Snapshot snapshot) implements CustomPacketPayload {
    public static final Type<VillageHighlighterSnapshotPayload> TYPE = new Type<>(ResourceLocation.parse("openblocks_reborn:highlighter_snapshot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, VillageHighlighterSnapshotPayload> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> {
                buf.writeBlockPos(value.anchor); buf.writeBlockPos(value.selected); buf.writeBoolean(value.all);
                buf.writeBoolean(value.snapshot.truncated());
                buf.writeVarInt(value.snapshot.pois().size());
                for (var poi : value.snapshot.pois()) { buf.writeBlockPos(poi.pos()); buf.writeByte(poi.kind()); buf.writeBoolean(poi.occupied()); }
                buf.writeVarInt(value.snapshot.links().size());
                for (var link : value.snapshot.links()) { buf.writeVarInt(link.villagerId()); buf.writeBlockPos(link.villagerPos());
                    buf.writeBlockPos(link.poi()); buf.writeByte(link.kind()); }
                buf.writeVarInt(value.snapshot.villageSections().size());
                for (long section : value.snapshot.villageSections()) buf.writeLong(section);
            }, buf -> {
                BlockPos anchor = buf.readBlockPos(), selected = buf.readBlockPos();
                boolean all = buf.readBoolean(), truncated = buf.readBoolean();
                var pois = new ArrayList<VillageHighlighterData.Poi>();
                for (int i = count(buf, VillageHighlighterData.MAX_POIS); i > 0; i--)
                    pois.add(new VillageHighlighterData.Poi(buf.readBlockPos(), kind(buf), buf.readBoolean()));
                var links = new ArrayList<VillageHighlighterData.Link>();
                for (int i = count(buf, VillageHighlighterData.MAX_LINKS); i > 0; i--)
                    links.add(new VillageHighlighterData.Link(buf.readVarInt(), buf.readBlockPos(), buf.readBlockPos(), kind(buf)));
                var sections = new ArrayList<Long>();
                for (int i = count(buf, VillageHighlighterData.MAX_SECTIONS); i > 0; i--) sections.add(buf.readLong());
                return new VillageHighlighterSnapshotPayload(anchor, selected, all,
                        new VillageHighlighterData.Snapshot(java.util.List.copyOf(pois), java.util.List.copyOf(links), java.util.List.copyOf(sections), truncated));
            });
    private static int count(RegistryFriendlyByteBuf buf, int max) {
        int count = buf.readVarInt();
        if (count < 0 || count > max) throw new DecoderException("Oversized village highlighter snapshot");
        return count;
    }
    private static int kind(RegistryFriendlyByteBuf buf) {
        int kind = buf.readUnsignedByte();
        if (kind > VillageHighlighterData.MEETING) throw new DecoderException("Unknown village POI kind");
        return kind;
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(VillageHighlighterSnapshotPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> net.xuwu.openblocks_reborn.client.VillageHighlighterClient.accept(payload));
    }
}
