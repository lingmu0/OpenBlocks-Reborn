package net.xuwu.openblocks_reborn.recipe;

import net.minecraft.resources.ResourceLocation;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.xuwu.openblocks_reborn.item.ImaginaryItem;
import net.xuwu.openblocks_reborn.registry.ModRecipes;

import java.util.HashMap;
import java.util.Map;

/**
 * Averages two or more differently coloured crayons. One use is consumed from
 * each ingredient and the new crayon receives the legacy 90% mixing yield.
 */
public class CrayonMixingRecipe extends CustomRecipe {
    private static final float CRAFTING_COST = 1.0F;

    public CrayonMixingRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer input, Level level) {
        int count = 0;
        Integer firstColor = null;
        boolean differentColor = false;
        for (ItemStack stack : RecipeInputs.items(input)) {
            if (stack.isEmpty()) continue;
            if (!(stack.getItem() instanceof ImaginaryItem) || !ImaginaryItem.isCrayon(stack)
                    || ImaginaryItem.getUses(stack) < CRAFTING_COST) return false;
            int color = ImaginaryItem.getColor(stack);
            if (firstColor == null) firstColor = color;
            else if (firstColor != color) differentColor = true;
            count++;
        }
        // Matching colours are handled by CrayonMergeRecipe without losing uses.
        return count >= 2 && differentColor;
    }

    @Override
    public ItemStack assemble(CraftingContainer input, RegistryAccess registries) {
        int count = 0;
        int red = 0;
        int green = 0;
        int blue = 0;
        Map<Integer, Integer> modes = new HashMap<>();
        for (ItemStack stack : RecipeInputs.items(input)) {
            if (stack.isEmpty()) continue;
            if (!(stack.getItem() instanceof ImaginaryItem) || !ImaginaryItem.isCrayon(stack)
                    || ImaginaryItem.getUses(stack) < CRAFTING_COST) return ItemStack.EMPTY;
            int color = ImaginaryItem.getColor(stack);
            red += color >> 16 & 0xFF;
            green += color >> 8 & 0xFF;
            blue += color & 0xFF;
            modes.merge(ImaginaryItem.getModeIndex(stack), 1, Integer::sum);
            count++;
        }
        if (count < 2) return ItemStack.EMPTY;
        int color = (red / count) << 16 | (green / count) << 8 | blue / count;
        int mode = modes.entrySet().stream().max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse(0);
        return ImaginaryItem.createConfigured(color, mode, count * 0.9F);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.getContainerSize(), ItemStack.EMPTY);
        for (int slot = 0; slot < input.getContainerSize(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof ImaginaryItem)) continue;
            float uses = ImaginaryItem.getUses(stack) - CRAFTING_COST;
            if (uses > 0.0F) {
                remaining.set(slot, ImaginaryItem.createConfigured(ImaginaryItem.isCrayon(stack)
                        ? ImaginaryItem.getColor(stack) : null, ImaginaryItem.getModeIndex(stack), uses));
            }
        }
        return remaining;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.CRAYON_MIXING.get();
    }
}
