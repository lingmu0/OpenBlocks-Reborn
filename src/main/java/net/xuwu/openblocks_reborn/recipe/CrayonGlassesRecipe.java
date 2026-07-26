package net.xuwu.openblocks_reborn.recipe;

import net.minecraft.resources.ResourceLocation;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.xuwu.openblocks_reborn.item.GlassesItem;
import net.xuwu.openblocks_reborn.item.ImaginaryItem;
import net.xuwu.openblocks_reborn.registry.ModRecipes;

/** Paper plus a magic crayon creates glasses tuned to that exact RGB colour. */
public class CrayonGlassesRecipe extends CustomRecipe {
    public CrayonGlassesRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer input, Level level) {
        int paper = 0;
        int crayons = 0;
        for (ItemStack stack : RecipeInputs.items(input)) {
            if (stack.isEmpty()) continue;
            if (stack.is(Items.PAPER)) paper++;
            else if (stack.getItem() instanceof ImaginaryItem
                    && ImaginaryItem.isCrayon(stack)
                    && ImaginaryItem.getUses(stack) >= 1.0F) crayons++;
            else return false;
        }
        return paper == 1 && crayons == 1;
    }

    @Override
    public ItemStack assemble(CraftingContainer input, RegistryAccess registries) {
        for (ItemStack stack : RecipeInputs.items(input)) {
            if (stack.getItem() instanceof ImaginaryItem && ImaginaryItem.isCrayon(stack)) {
                return GlassesItem.createCrayonGlasses(ImaginaryItem.getColor(stack));
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer input) {
        NonNullList<ItemStack> result = NonNullList.withSize(input.getContainerSize(), ItemStack.EMPTY);
        for (int slot = 0; slot < input.getContainerSize(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (!(stack.getItem() instanceof ImaginaryItem)) continue;
            float uses = ImaginaryItem.getUses(stack) - 1.0F;
            if (uses > 0.0F) {
                result.set(slot, ImaginaryItem.createConfigured(ImaginaryItem.getColor(stack),
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
        return ModRecipes.CRAYON_GLASSES.get();
    }
}
