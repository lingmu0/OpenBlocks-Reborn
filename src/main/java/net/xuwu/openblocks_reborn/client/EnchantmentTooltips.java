package net.xuwu.openblocks_reborn.client;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.xuwu.openblocks_reborn.OpenBlocksReborn;
import net.xuwu.openblocks_reborn.config.EnchantmentConfig;

@EventBusSubscriber(modid = OpenBlocksReborn.MOD_ID, value = Dist.CLIENT)
public final class EnchantmentTooltips {
    @SubscribeEvent public static void describe(ItemTooltipEvent event) {
        for (var entry : EnchantmentHelper.getEnchantmentsForCrafting(event.getItemStack()).entrySet()) {
            var key = entry.getKey().unwrapKey().orElse(null);
            if (key == null || !key.location().getNamespace().equals(OpenBlocksReborn.MOD_ID)) continue;
            String id = key.location().getPath();
            if (!java.util.Set.of("last_stand", "unstable", "flim_flam").contains(id)) continue;
            // Enchantment Descriptions reads the same .desc keys. Avoid duplicate descriptions.
            if (!ModList.get().isLoaded("enchdesc")) event.getToolTip().add(Component.translatable(
                    "enchantment.openblocks_reborn." + id + ".desc").withStyle(ChatFormatting.GRAY));
            if (event.getEntity() == null) continue;
            boolean enabled = switch (id) {
                case "last_stand" -> EnchantmentConfig.LAST_STAND_ENABLED.get();
                case "unstable" -> EnchantmentConfig.UNSTABLE_ENABLED.get();
                default -> EnchantmentConfig.FLIM_FLAM_ENABLED.get() && EnchantmentConfig.anyPrankEnabled();
            };
            if (!enabled) event.getToolTip().add(Component.translatable("tooltip.openblocks_reborn.enchantment_disabled")
                    .withStyle(ChatFormatting.RED));
        }
    }
    private EnchantmentTooltips() {}
}
