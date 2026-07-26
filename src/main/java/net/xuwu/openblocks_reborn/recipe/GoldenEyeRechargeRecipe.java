package net.xuwu.openblocks_reborn.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.xuwu.openblocks_reborn.registry.ModItems;
import net.xuwu.openblocks_reborn.registry.ModRecipes;

/**
 * Restores the legacy Golden Eye recharge recipe. Each pearl repairs ten uses
 * and the copied result keeps the structure target stored on the eye.
 */
public class GoldenEyeRechargeRecipe extends CustomRecipe {
    private static final int REPAIR_PER_PEARL = 10;

    public GoldenEyeRechargeRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        ItemStack eye = ItemStack.EMPTY;
        int pearls = 0;
        for (ItemStack stack : input.items()) {
            if (stack.isEmpty()) continue;
            if (stack.is(ModItems.GOLDEN_EYE.get()) && eye.isEmpty()) eye = stack;
            else if (stack.is(Items.ENDER_PEARL)) pearls++;
            else return false;
        }
        return !eye.isEmpty() && pearls > 0 && eye.getDamageValue() >= pearls * REPAIR_PER_PEARL;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack eye = ItemStack.EMPTY;
        int pearls = 0;
        for (ItemStack stack : input.items()) {
            if (stack.is(ModItems.GOLDEN_EYE.get())) eye = stack;
            else if (stack.is(Items.ENDER_PEARL)) pearls++;
        }
        if (eye.isEmpty() || pearls == 0) return ItemStack.EMPTY;
        ItemStack result = eye.copyWithCount(1);
        result.setDamageValue(Math.max(0, eye.getDamageValue() - pearls * REPAIR_PER_PEARL));
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.GOLDEN_EYE_RECHARGE.get();
    }
}
