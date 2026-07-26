package net.xuwu.openblocks_reborn.recipe;

import net.minecraft.resources.ResourceLocation;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.xuwu.openblocks_reborn.item.SkyBlockItem;
import net.xuwu.openblocks_reborn.registry.ModBlocks;
import net.xuwu.openblocks_reborn.registry.ModRecipes;

/** A redstone torch toggles the normal and inverted Sky Block item variants. */
public class SkyBlockInversionRecipe extends CustomRecipe {
    public SkyBlockInversionRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer input, Level level) {
        int sky = 0;
        int torches = 0;
        for (ItemStack stack : RecipeInputs.items(input)) {
            if (stack.isEmpty()) continue;
            if (stack.is(ModBlocks.SKY.get().asItem())) sky++;
            else if (stack.is(Items.REDSTONE_TORCH)) torches++;
            else return false;
        }
        return sky == 1 && torches == 1;
    }

    @Override
    public ItemStack assemble(CraftingContainer input, RegistryAccess registries) {
        for (ItemStack stack : RecipeInputs.items(input)) {
            if (stack.is(ModBlocks.SKY.get().asItem())) {
                return SkyBlockItem.create(!SkyBlockItem.isInverted(stack));
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.SKY_INVERSION.get();
    }
}
