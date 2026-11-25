// Derived from ElevatorMod 1.21.1-1.11.4 by VsnGamer (MIT); see LICENSE-ElevatorMod.
package net.xuwu.openblocks_reborn.elevator.network.client;

import net.xuwu.openblocks_reborn.elevator.ElevatorMod;
import net.xuwu.openblocks_reborn.elevator.level.ElevatorBlockEntityBase;
import net.xuwu.openblocks_reborn.elevator.network.TeleportPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;


public record RemoveCamoPacket(BlockPos pos) implements CustomPacketPayload {
    public static final Type<RemoveCamoPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ElevatorMod.ID, "remove_camo"));

    public static final StreamCodec<ByteBuf, RemoveCamoPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            RemoveCamoPacket::pos,
            RemoveCamoPacket::new
    );

    @Override
    public Type<RemoveCamoPacket> type() {
        return TYPE;
    }

    public static void handle(RemoveCamoPacket msg, ServerPlayer player) {
        if (TeleportPacket.isBadClientPacket(player, msg.pos)) {
            return;
        }

        if (player.level().getBlockEntity(msg.pos) instanceof ElevatorBlockEntityBase tile) {
            tile.setCamoAndUpdate(null);
        }
    }
}
