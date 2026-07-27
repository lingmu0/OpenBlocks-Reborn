package net.xuwu.openblocks_reborn.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class OpenBlocksConfig {
    public static final ModConfigSpec SERVER_SPEC;
    public static final ModConfigSpec.BooleanValue GENERATE_GRAVES;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Grave settings").push("grave");
        GENERATE_GRAVES = builder
                .comment("Whether a grave should capture player drops when a player dies.")
                .define("generateOnPlayerDeath", true);
        builder.pop();
        SERVER_SPEC = builder.build();
    }

    private OpenBlocksConfig() {
    }
}
