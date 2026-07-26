package net.xuwu.openblocks_reborn.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

/**
 * Item metadata compatibility for Minecraft 1.20.1, before item data components
 * replaced the stack's root NBT tag.
 */
public final class LegacyItemData {
    private LegacyItemData() {
    }

    public static CompoundTag copyTag(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? new CompoundTag() : tag.copy();
    }

    public static void update(ItemStack stack, Consumer<CompoundTag> updater) {
        updater.accept(stack.getOrCreateTag());
    }
}
