package net.xuwu.openblocks_reborn.item;

import net.xuwu.openblocks_reborn.util.LegacyItemData;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.xuwu.openblocks_reborn.registry.ModItems;

public class HeightMapItem extends Item {
    private static final String MAP_X = "MapX";
    private static final String MAP_Z = "MapZ";
    private static final String HEIGHTS = "Heights";

    public HeightMapItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack emptyMap = player.getItemInHand(otherHand);
        if (emptyMap.is(ModItems.EMPTY_MAP.get())) {
            if (!level.isClientSide) {
                ItemStack copy = player.getItemInHand(hand).copyWithCount(1);
                if (!player.getAbilities().instabuild) emptyMap.shrink(1);
                if (!player.addItem(copy)) player.drop(copy, false);
                player.displayClientMessage(Component.translatable("message.openblocks_reborn.height_map_copied"), true);
            }
            return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
        }
        if (!level.isClientSide) {
            var data = LegacyItemData.copyTag(player.getItemInHand(hand));
            int x = data.contains(MAP_X) ? data.getInt(MAP_X) : player.getBlockX();
            int z = data.contains(MAP_Z) ? data.getInt(MAP_Z) : player.getBlockZ();
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
            BlockPos pos = new BlockPos(x, Math.max(level.getMinBuildHeight(), y - 1), z);
            String biome = level.getBiome(pos).unwrapKey().map(key -> key.location().toString()).orElse("unknown");
            player.displayClientMessage(Component.translatable("message.openblocks_reborn.height_map", x, z, y, biome), false);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    public static ItemStack create(Level level, BlockPos center) {
        ItemStack result = new ItemStack(ModItems.HEIGHT_MAP.get());
        int[] heights = new int[81];
        for (int z = -4; z <= 4; z++) {
            for (int x = -4; x <= 4; x++) {
                heights[(z + 4) * 9 + x + 4] = level.getHeight(Heightmap.Types.WORLD_SURFACE,
                        center.getX() + x, center.getZ() + z);
            }
        }
        LegacyItemData.update(result, tag -> {
            tag.putInt(MAP_X, center.getX());
            tag.putInt(MAP_Z, center.getZ());
            tag.putIntArray(HEIGHTS, heights);
        });
        return result;
    }

    public static int[] getHeights(ItemStack stack) {
        int[] heights = LegacyItemData.copyTag(stack).getIntArray(HEIGHTS);
        return heights.length == 81 ? heights : new int[0];
    }
}
