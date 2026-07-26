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
import net.xuwu.openblocks_reborn.item.ImaginaryItem;
import net.xuwu.openblocks_reborn.registry.ModItems;
import net.xuwu.openblocks_reborn.registry.ModRecipes;

/** Paper plus a magic pencil creates pencil glasses and consumes one use. */
public class PencilGlassesRecipe extends CustomRecipe {
    public PencilGlassesRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer input, Level level) {
        int paper = 0;
        int pencils = 0;
        for (ItemStack stack : RecipeInputs.items(input)) {
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
    public ItemStack assemble(CraftingContainer input, RegistryAccess registries) {
        return matchesIgnoringLevel(input)
                ? new ItemStack(ModItems.PENCIL_GLASSES.get()) : ItemStack.EMPTY;
    }

    private static boolean matchesIgnoringLevel(CraftingContainer input) {
        int paper = 0;
        int pencils = 0;
        for (ItemStack stack : RecipeInputs.items(input)) {
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
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer input) {
        NonNullList<ItemStack> result = NonNullList.withSize(input.getContainerSize(), ItemStack.EMPTY);
        for (int slot = 0; slot < input.getContainerSize(); slot++) {
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
