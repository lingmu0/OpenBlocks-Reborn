package net.xuwu.openblocks_reborn.recipe;

import net.minecraft.resources.ResourceLocation;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.xuwu.openblocks_reborn.item.ImaginaryItem;
import net.xuwu.openblocks_reborn.registry.ModRecipes;

/** Combines matching magic pencils or crayons and preserves all remaining uses. */
public class CrayonMergeRecipe extends CustomRecipe {
    public CrayonMergeRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer input, Level level) {
        ItemStack first = ItemStack.EMPTY;
        int count = 0;
        for (ItemStack stack : RecipeInputs.items(input)) {
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
    public ItemStack assemble(CraftingContainer input, RegistryAccess registries) {
        ItemStack first = ItemStack.EMPTY;
        float uses = 0.0F;
        for (ItemStack stack : RecipeInputs.items(input)) {
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
