package net.xuwu.openblocks_reborn.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.xuwu.openblocks_reborn.item.ImaginaryItem;
import net.xuwu.openblocks_reborn.registry.ModItems;
import net.xuwu.openblocks_reborn.registry.ModRecipes;

/** Paper plus a magic pencil creates pencil glasses and consumes one use. */
public class PencilGlassesRecipe extends CustomRecipe {
    public PencilGlassesRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int paper = 0;
        int pencils = 0;
        for (ItemStack stack : input.items()) {
            if (stack.isEmpty()) continue;
            if (stack.is(Items.PAPER)) paper++;
            else if (stack.getItem() instanceof ImaginaryItem
                    && !ImaginaryItem.isCrayon(stack)
                    && ImaginaryItem.getUses(stack) >= 1.0F) pencils++;
            else return false;
        }
        return paper == 1 && pencils == 1;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return matchesIgnoringLevel(input)
                ? new ItemStack(ModItems.PENCIL_GLASSES.get()) : ItemStack.EMPTY;
    }

    private static boolean matchesIgnoringLevel(CraftingInput input) {
        int paper = 0;
        int pencils = 0;
        for (ItemStack stack : input.items()) {
            if (stack.isEmpty()) continue;
            if (stack.is(Items.PAPER)) paper++;
            else if (stack.getItem() instanceof ImaginaryItem
                    && !ImaginaryItem.isCrayon(stack)
                    && ImaginaryItem.getUses(stack) >= 1.0F) pencils++;
            else return false;
        }
        return paper == 1 && pencils == 1;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> result = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (!(stack.getItem() instanceof ImaginaryItem)) continue;
            float uses = ImaginaryItem.getUses(stack) - 1.0F;
            if (uses > 0.0F) {
                result.set(slot, ImaginaryItem.createConfigured(null,
                        ImaginaryItem.getModeIndex(stack), uses));
            }
        }
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.PENCIL_GLASSES.get();
    }
}
