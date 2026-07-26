package net.xuwu.openblocks_reborn.registry;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.xuwu.openblocks_reborn.OpenBlocksReborn;

/** Resource keys for the data-driven 1.21 enchantments. */
public final class ModEnchantments {
    public static final ResourceKey<Enchantment> EXPLOSIVE = key("explosive");
    public static final ResourceKey<Enchantment> LAST_STAND = key("last_stand");
    public static final ResourceKey<Enchantment> FLIM_FLAM = key("flim_flam");

    private static ResourceKey<Enchantment> key(String id) {
        return ResourceKey.create(Registries.ENCHANTMENT,
                ResourceLocation.fromNamespaceAndPath(OpenBlocksReborn.MOD_ID, id));
    }

    public static Holder<Enchantment> holder(LivingEntity entity, ResourceKey<Enchantment> key) {
        return entity.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }

    public static int level(LivingEntity entity, ItemStack stack, ResourceKey<Enchantment> key) {
        return stack.isEmpty() ? 0 : EnchantmentHelper.getItemEnchantmentLevel(holder(entity, key), stack);
    }

    private ModEnchantments() {
    }
}
