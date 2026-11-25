package net.xuwu.openblocks_reborn.elevator.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xuwu.openblocks_reborn.OpenBlocksReborn;
import net.xuwu.openblocks_reborn.elevator.ElevatorHandler;

/** Keeps ElevatorMod's input implementation entirely on the client distribution. */
@EventBusSubscriber(modid = OpenBlocksReborn.MOD_ID, value = Dist.CLIENT)
public final class ElevatorInputEvents {
    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        ElevatorHandler.handleInput(PacketDistributor::sendToServer);
    }
}
