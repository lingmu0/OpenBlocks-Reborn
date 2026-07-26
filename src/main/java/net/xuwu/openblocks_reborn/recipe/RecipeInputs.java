package net.xuwu.openblocks_reborn.recipe;

import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;

import java.util.Iterator;
import java.util.NoSuchElementException;

/** Small 1.20.1 compatibility view for recipes originally written against CraftingInput. */
final class RecipeInputs {
    private RecipeInputs() {
    }

    static Iterable<ItemStack> items(CraftingContainer input) {
        return () -> new Iterator<>() {
            private int slot;

            @Override
            public boolean hasNext() {
                return slot < input.getContainerSize();
            }

            @Override
            public ItemStack next() {
                if (!hasNext()) throw new NoSuchElementException();
                return input.getItem(slot++);
            }
        };
    }
}
