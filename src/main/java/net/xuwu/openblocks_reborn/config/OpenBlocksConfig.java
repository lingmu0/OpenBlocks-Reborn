package net.xuwu.openblocks_reborn.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class OpenBlocksConfig {
    public static final ForgeConfigSpec SERVER_SPEC;
    public static final ForgeConfigSpec.BooleanValue GENERATE_GRAVES;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
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
