// Derived from ElevatorMod 1.21.1-1.11.4 by VsnGamer (MIT); see LICENSE-ElevatorMod.
package net.xuwu.openblocks_reborn.elevator.neoforge.network;

import net.xuwu.openblocks_reborn.elevator.neoforge.init.Registry;
import net.xuwu.openblocks_reborn.elevator.network.client.RemoveCamoPacket;
import net.xuwu.openblocks_reborn.elevator.network.client.SetArrowPacket;
import net.xuwu.openblocks_reborn.elevator.network.client.SetDirectionalPacket;
import net.xuwu.openblocks_reborn.elevator.network.client.SetFacingPacket;
import net.xuwu.openblocks_reborn.elevator.network.TeleportPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;


@EventBusSubscriber(modid = net.xuwu.openblocks_reborn.OpenBlocksReborn.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class NetworkHandler {
    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1.0.0");

        registrar.playToServer(
                TeleportPacket.TYPE,
                TeleportPacket.STREAM_CODEC,
                (pkt, ctx) -> TeleportPacket.handle(pkt, (ServerPlayer) ctx.player(), Registry.TELEPORT_SOUND.get())
        );

        registrar.playToServer(SetDirectionalPacket.TYPE, SetDirectionalPacket.STREAM_CODEC, (pkt, ctx) -> SetDirectionalPacket.handle(pkt, (ServerPlayer) ctx.player()));
        registrar.playToServer(SetArrowPacket.TYPE, SetArrowPacket.STREAM_CODEC, (pkt, ctx) -> SetArrowPacket.handle(pkt, (ServerPlayer) ctx.player()));
        registrar.playToServer(RemoveCamoPacket.TYPE, RemoveCamoPacket.STREAM_CODEC, (pkt, ctx) -> RemoveCamoPacket.handle(pkt, (ServerPlayer) ctx.player()));
        registrar.playToServer(SetFacingPacket.TYPE, SetFacingPacket.STREAM_CODEC, (pkt, ctx) -> SetFacingPacket.handle(pkt, (ServerPlayer) ctx.player()));
    }
}
