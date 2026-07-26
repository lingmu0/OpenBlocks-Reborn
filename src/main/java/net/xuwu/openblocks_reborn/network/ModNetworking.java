package net.xuwu.openblocks_reborn.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class ModNetworking {
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(
                RenameAutoAnvilPayload.TYPE,
                RenameAutoAnvilPayload.STREAM_CODEC,
                RenameAutoAnvilPayload::handle)
                .playToServer(
                        GoldenEyeSelectPayload.TYPE,
                        GoldenEyeSelectPayload.STREAM_CODEC,
                        GoldenEyeSelectPayload::handle);
    }

    private ModNetworking() {
    }
}
