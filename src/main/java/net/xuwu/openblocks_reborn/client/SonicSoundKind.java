package net.xuwu.openblocks_reborn.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.xuwu.openblocks_reborn.OpenBlocksReborn;

/** Legacy icon vocabulary extended for modern sound sources; unknown mod sounds still work. */
public enum SonicSoundKind {
    UNKNOWN("unknown", 0xFFFFFF), BLOCK("click", 0xEEEEEE), PLAYER("player", 0xFFFFFF),
    FRIENDLY("mob_friendly", 0x9DE68B), HOSTILE("mob_hostile", 0xFF7777),
    HURT("mob_hurt", 0xFF9966), DEATH("mob_death", 0xBBBBBB),
    LIQUID("liquid", 0x73BFFF), WEATHER("rain", 0xA9CFFF), NOTE("note", 0xFFE180),
    PORTAL("portal", 0xD59BFF), SCULK("generic", 0x4DE0D0),
    COPPER("click", 0xD4946A), AMETHYST("note", 0xC59BFF), WIND("generic", 0xB7F4ED);

    private final String icon;
    public final int color;

    SonicSoundKind(String icon, int color) { this.icon = icon; this.color = color; }

    public ResourceLocation texture() {
        return ResourceLocation.fromNamespaceAndPath(OpenBlocksReborn.MOD_ID,
                "textures/gui/sonic/sound_" + icon + ".png");
    }

    public static SonicSoundKind classify(ResourceLocation id, SoundSource source) {
        String path = id.getPath();
        if (path.contains("sculk") || path.contains("warden")) return SCULK;
        if (path.contains("breeze") || path.contains("wind_charge")) return WIND;
        if (path.contains("amethyst")) return AMETHYST;
        if (path.contains("copper")) return COPPER;
        if (path.contains("portal") || path.contains("end_gateway")) return PORTAL;
        if (path.contains("note_block") || path.contains("allay")) return NOTE;
        if (path.endsWith(".death")) return DEATH;
        if (path.contains(".hurt")) return HURT;
        if (path.contains("water") || path.contains("lava") || path.contains("swim")
                || path.contains("splash") || path.contains("bubble")) return LIQUID;
        return switch (source) {
            case HOSTILE -> HOSTILE;
            case NEUTRAL -> FRIENDLY;
            case PLAYERS -> PLAYER;
            case BLOCKS -> BLOCK;
            case WEATHER -> WEATHER;
            default -> UNKNOWN;
        };
    }
}
