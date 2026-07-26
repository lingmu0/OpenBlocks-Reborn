package net.xuwu.openblocks_reborn.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.xuwu.openblocks_reborn.OpenBlocksReborn;
import net.xuwu.openblocks_reborn.recipe.CrayonMergeRecipe;
import net.xuwu.openblocks_reborn.recipe.CrayonMixingRecipe;
import net.xuwu.openblocks_reborn.recipe.CrayonGlassesRecipe;
import net.xuwu.openblocks_reborn.recipe.GoldenEyeRechargeRecipe;
import net.xuwu.openblocks_reborn.recipe.PencilGlassesRecipe;
import net.xuwu.openblocks_reborn.recipe.SkyBlockInversionRecipe;

public final class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, OpenBlocksReborn.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<GoldenEyeRechargeRecipe>>
            GOLDEN_EYE_RECHARGE = RECIPE_SERIALIZERS.register("golden_eye_recharge",
            () -> new SimpleCraftingRecipeSerializer<>(GoldenEyeRechargeRecipe::new));
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<CrayonMergeRecipe>>
            CRAYON_MERGE = RECIPE_SERIALIZERS.register("crayon_merge",
            () -> new SimpleCraftingRecipeSerializer<>(CrayonMergeRecipe::new));
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<CrayonMixingRecipe>>
            CRAYON_MIXING = RECIPE_SERIALIZERS.register("crayon_mixing",
            () -> new SimpleCraftingRecipeSerializer<>(CrayonMixingRecipe::new));
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<CrayonGlassesRecipe>>
            CRAYON_GLASSES = RECIPE_SERIALIZERS.register("crayon_glasses",
            () -> new SimpleCraftingRecipeSerializer<>(CrayonGlassesRecipe::new));
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<PencilGlassesRecipe>>
            PENCIL_GLASSES = RECIPE_SERIALIZERS.register("pencil_glasses",
            () -> new SimpleCraftingRecipeSerializer<>(PencilGlassesRecipe::new));
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<SkyBlockInversionRecipe>>
            SKY_INVERSION = RECIPE_SERIALIZERS.register("sky_inversion",
            () -> new SimpleCraftingRecipeSerializer<>(SkyBlockInversionRecipe::new));

    private ModRecipes() {
    }
}
