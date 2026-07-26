package net.xuwu.openblocks_reborn.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
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
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getBoolean(INVERTED);
    }

    public static ItemStack create(boolean inverted) {
        ItemStack result = new ItemStack(net.xuwu.openblocks_reborn.registry.ModBlocks.SKY.asItem());
        if (inverted) {
            CustomData.update(DataComponents.CUSTOM_DATA, result,
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
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        if (isInverted(stack)) {
            tooltip.add(Component.translatable("message.openblocks_reborn.inverted"));
        }
    }
}
