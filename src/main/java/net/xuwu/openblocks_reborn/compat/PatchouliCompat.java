package net.xuwu.openblocks_reborn.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.xuwu.openblocks_reborn.OpenBlocksReborn;
import vazkii.patchouli.api.PatchouliAPI;

/**
 * The required Patchouli manual, retaining the existing guide item ID.
 */
public final class PatchouliCompat {
    public static final ResourceLocation OPENBLOCKS_GUIDE = ResourceLocation.fromNamespaceAndPath(
            OpenBlocksReborn.MOD_ID, "openblocks_guide");

    public static void openGuide(ServerPlayer player) {
        PatchouliAPI.get().openBookGUI(player, OPENBLOCKS_GUIDE);
    }

    private PatchouliCompat() {
    }
}
