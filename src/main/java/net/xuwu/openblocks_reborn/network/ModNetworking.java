package net.xuwu.openblocks_reborn.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.xuwu.openblocks_reborn.OpenBlocksReborn;

public final class ModNetworking {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(OpenBlocksReborn.MOD_ID, "main"))
            .networkProtocolVersion(() -> PROTOCOL)
            .clientAcceptedVersions(PROTOCOL::equals)
            .serverAcceptedVersions(PROTOCOL::equals)
            .simpleChannel();

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(RenameAutoAnvilPayload.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RenameAutoAnvilPayload::encode)
                .decoder(RenameAutoAnvilPayload::decode)
                .consumerMainThread(RenameAutoAnvilPayload::handle)
                .add();
        CHANNEL.messageBuilder(GoldenEyeSelectPayload.class, id, NetworkDirection.PLAY_TO_SERVER)
                .encoder(GoldenEyeSelectPayload::encode)
                .decoder(GoldenEyeSelectPayload::decode)
                .consumerMainThread(GoldenEyeSelectPayload::handle)
                .add();
    }

    public static void sendToServer(Object message) {
        CHANNEL.sendToServer(message);
    }

    private ModNetworking() {
    }
}
