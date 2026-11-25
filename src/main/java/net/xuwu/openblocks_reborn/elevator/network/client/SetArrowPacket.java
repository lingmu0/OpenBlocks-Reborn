// Derived from ElevatorMod 1.21.1-1.11.4 by VsnGamer (MIT); see LICENSE-ElevatorMod.
package net.xuwu.openblocks_reborn.elevator.network.client;

import net.xuwu.openblocks_reborn.elevator.ElevatorBlockBase;
import net.xuwu.openblocks_reborn.elevator.ElevatorMod;
import net.xuwu.openblocks_reborn.elevator.network.TeleportPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;


public record SetArrowPacket(boolean value, BlockPos pos) implements CustomPacketPayload {
    public static final Type<SetArrowPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ElevatorMod.ID, "set_arrow"));

    public static final StreamCodec<ByteBuf, SetArrowPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            SetArrowPacket::value,
            BlockPos.STREAM_CODEC,
            SetArrowPacket::pos,
            SetArrowPacket::new
    );

    @Override
    public Type<SetArrowPacket> type() {
        return TYPE;
    }

    public static void handle(SetArrowPacket msg, ServerPlayer player) {
            if (TeleportPacket.isBadClientPacket(player, msg.pos)) {
                return;
            }

            Level world = player.level();
            BlockState state = world.getBlockState(msg.pos);
            if (state.getBlock() instanceof ElevatorBlockBase) {
                world.setBlockAndUpdate(msg.pos, state.setValue(ElevatorBlockBase.SHOW_ARROW, msg.value));
            }
    }
}
