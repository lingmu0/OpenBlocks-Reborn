package net.xuwu.openblocks_reborn.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class VillageHighlighterConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ALL, POIS, LINKS, GOLEMS, VILLAGE, THROUGH_WALLS;
    public static final ModConfigSpec.IntValue RADIUS;
    static {
        var builder = new ModConfigSpec.Builder();
        builder.push("villageHighlighter");
        ALL = builder.comment("False: inspect one linked villager live. True: manually request a static nearby snapshot; never auto-refreshes.").define("allMode", false);
        RADIUS = builder.comment("Scan radius around the activated highlighter, limited to loaded terrain.")
                .defineInRange("radius", 32, 8, 32);
        POIS = builder.define("showPois", true);
        LINKS = builder.define("showLinks", true);
        GOLEMS = builder.define("showGolemSearchBoxes", true);
        VILLAGE = builder.define("showVillageSectionBoundary", false);
        THROUGH_WALLS = builder.define("throughWalls", true);
        builder.pop();
        SPEC = builder.build();
    }
    private VillageHighlighterConfig() {}
}
