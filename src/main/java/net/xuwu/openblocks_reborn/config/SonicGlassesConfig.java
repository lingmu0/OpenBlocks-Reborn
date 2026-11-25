package net.xuwu.openblocks_reborn.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class SonicGlassesConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.BooleanValue LABELS;
    public static final ModConfigSpec.DoubleValue RANGE;
    public static final ModConfigSpec.IntValue LIFETIME;
    public static final ModConfigSpec.IntValue MAX_MARKERS;
    static {
        var builder = new ModConfigSpec.Builder();
        builder.push("sonicGlasses");
        ENABLED = builder.comment("Visualize positional sounds while wearing Sonic Glasses.")
                .define("enabled", true);
        LABELS = builder.comment("Show translated subtitles and distance below sound icons.")
                .define("showLabels", true);
        RANGE = builder.comment("Maximum marker distance, also capped by the sound's attenuation range.")
                .defineInRange("range", 48.0, 4.0, 128.0);
        LIFETIME = builder.comment("Ticks before a sound marker fades; looping sounds refresh while active.")
                .defineInRange("lifetimeTicks", 40, 5, 200);
        MAX_MARKERS = builder.comment("Maximum simultaneous markers to bound rendering cost.")
                .defineInRange("maxMarkers", 64, 8, 256);
        builder.pop();
        SPEC = builder.build();
    }
    private SonicGlassesConfig() {}
}
