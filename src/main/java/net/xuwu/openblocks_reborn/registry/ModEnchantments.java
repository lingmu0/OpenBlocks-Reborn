package net.xuwu.openblocks_reborn.registry;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.xuwu.openblocks_reborn.OpenBlocksReborn;

/** Code-registered enchantments for Minecraft 1.20.1. */
public final class ModEnchantments {
    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, OpenBlocksReborn.MOD_ID);

    public static final RegistryObject<Enchantment> EXPLOSIVE = ENCHANTMENTS.register(
            "explosive", () -> new OpenBlocksEnchantment(Enchantment.Rarity.RARE,
                    EnchantmentCategory.ARMOR, EquipmentSlot.values(), 3));
    public static final RegistryObject<Enchantment> LAST_STAND = ENCHANTMENTS.register(
            "last_stand", () -> new OpenBlocksEnchantment(Enchantment.Rarity.VERY_RARE,
                    EnchantmentCategory.ARMOR, EquipmentSlot.values(), 3));
    public static final RegistryObject<Enchantment> FLIM_FLAM = ENCHANTMENTS.register(
            "flim_flam", () -> new OpenBlocksEnchantment(Enchantment.Rarity.RARE,
                    EnchantmentCategory.BREAKABLE, EquipmentSlot.values(), 4));

    public static int level(LivingEntity entity, ItemStack stack,
                            RegistryObject<Enchantment> enchantment) {
        return stack.isEmpty() ? 0
                : EnchantmentHelper.getItemEnchantmentLevel(enchantment.get(), stack);
    }

    private static final class OpenBlocksEnchantment extends Enchantment {
        private final int maximumLevel;

        private OpenBlocksEnchantment(Rarity rarity, EnchantmentCategory category,
                                      EquipmentSlot[] slots, int maximumLevel) {
            super(rarity, category, slots);
            this.maximumLevel = maximumLevel;
        }

        @Override
        public int getMaxLevel() {
            return maximumLevel;
        }
    }

    private ModEnchantments() {
    }
}
