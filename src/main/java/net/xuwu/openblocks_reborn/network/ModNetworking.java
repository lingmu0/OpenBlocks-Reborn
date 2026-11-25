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
                        GoldenEyeSelectPayload::handle)
                .playToServer(VillageHighlighterRequestPayload.TYPE, VillageHighlighterRequestPayload.STREAM_CODEC,
                        VillageHighlighterRequestPayload::handle)
                .playToClient(VillageHighlighterOpenPayload.TYPE, VillageHighlighterOpenPayload.STREAM_CODEC,
                        VillageHighlighterOpenPayload::handle)
                .playToClient(VillageHighlighterSnapshotPayload.TYPE, VillageHighlighterSnapshotPayload.STREAM_CODEC,
                        VillageHighlighterSnapshotPayload::handle);
    }

    private ModNetworking() {
    }
}
