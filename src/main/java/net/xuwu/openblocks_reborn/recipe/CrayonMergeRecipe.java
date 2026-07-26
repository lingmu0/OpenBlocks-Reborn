package net.xuwu.openblocks_reborn.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.xuwu.openblocks_reborn.item.ImaginaryItem;
import net.xuwu.openblocks_reborn.registry.ModRecipes;

/** Combines matching magic pencils or crayons and preserves all remaining uses. */
public class CrayonMergeRecipe extends CustomRecipe {
    public CrayonMergeRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        ItemStack first = ItemStack.EMPTY;
        int count = 0;
        for (ItemStack stack : input.items()) {
            if (stack.isEmpty()) continue;
            if (!(stack.getItem() instanceof ImaginaryItem)) return false;
            if (first.isEmpty()) first = stack;
            else if (ImaginaryItem.isCrayon(first) != ImaginaryItem.isCrayon(stack)
                    || (ImaginaryItem.isCrayon(first)
                    && ImaginaryItem.getColor(first) != ImaginaryItem.getColor(stack))) {
                return false;
            }
            count++;
        }
        return count >= 2;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack first = ItemStack.EMPTY;
        float uses = 0.0F;
        for (ItemStack stack : input.items()) {
            if (stack.isEmpty()) continue;
            if (!(stack.getItem() instanceof ImaginaryItem)) return ItemStack.EMPTY;
            if (first.isEmpty()) first = stack;
            uses += ImaginaryItem.getUses(stack);
        }
        if (first.isEmpty() || uses <= 0.0F) return ItemStack.EMPTY;
        return ImaginaryItem.createConfigured(ImaginaryItem.isCrayon(first)
                ? ImaginaryItem.getColor(first) : null, 0, uses);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.CRAYON_MERGE.get();
    }
}
