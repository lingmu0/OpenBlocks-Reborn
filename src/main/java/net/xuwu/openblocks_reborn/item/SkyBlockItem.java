package net.xuwu.openblocks_reborn.item;

import net.xuwu.openblocks_reborn.util.LegacyItemData;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.xuwu.openblocks_reborn.block.SkyBlock;

import java.util.List;

/** Preserves the legacy normal/inverted Sky Block item subtype. */
public class SkyBlockItem extends BlockItem {
    private static final String INVERTED = "SkyInverted";

    public SkyBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    public static boolean isInverted(ItemStack stack) {
        return LegacyItemData.copyTag(stack).getBoolean(INVERTED);
    }

    public static ItemStack create(boolean inverted) {
        ItemStack result = new ItemStack(net.xuwu.openblocks_reborn.registry.ModBlocks.SKY.get().asItem());
        if (inverted) {
            LegacyItemData.update(result,
                    tag -> tag.putBoolean(INVERTED, true));
        }
        return result;
    }

    @Override
    protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        return state == null ? null : state.setValue(SkyBlock.INVERTED,
                isInverted(context.getItemInHand()));
    }

    @Override
    public void appendHoverText(ItemStack stack, @javax.annotation.Nullable net.minecraft.world.level.Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        if (isInverted(stack)) {
            tooltip.add(Component.translatable("message.openblocks_reborn.inverted"));
        }
    }
}
