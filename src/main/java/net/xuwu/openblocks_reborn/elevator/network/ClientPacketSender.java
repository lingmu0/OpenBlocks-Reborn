// Derived from ElevatorMod 1.21.1-1.11.4 by VsnGamer (MIT); see LICENSE-ElevatorMod.
package net.xuwu.openblocks_reborn.elevator.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public interface ClientPacketSender {
    void sendToServer(CustomPacketPayload packet);
}
