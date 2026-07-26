package net.xuwu.openblocks_reborn.item;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.xuwu.openblocks_reborn.block.ColorableBlock;
import net.xuwu.openblocks_reborn.blockentity.CanvasBlockEntity;

public class SqueegeeItem extends Item {
    private final boolean pixelMode;

    public SqueegeeItem(Properties properties) {
        this(properties, false);
    }

    public SqueegeeItem(Properties properties, boolean pixelMode) {
        super(properties);
        this.pixelMode = pixelMode;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().getBlockEntity(context.getClickedPos()) instanceof CanvasBlockEntity canvas) {
            if (!context.getLevel().isClientSide) {
                if (pixelMode) {
                    int[] pixel = PaintBrushItem.canvasPixel(context);
                    canvas.setPixel(context.getClickedFace(), pixel[0], pixel[1], -1);
                } else if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
                    canvas.setAll(-1);
                } else {
                    canvas.setColor(context.getClickedFace(), -1);
                }
                if (context.getPlayer() != null) context.getItemInHand().hurtAndBreak(1, context.getPlayer(), p -> p.broadcastBreakEvent(context.getHand()));
            }
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }
        BlockState state = context.getLevel().getBlockState(context.getClickedPos());
        if (!state.hasProperty(ColorableBlock.COLOR)) return InteractionResult.PASS;
        if (!context.getLevel().isClientSide) {
            context.getLevel().setBlock(context.getClickedPos(), state.setValue(ColorableBlock.COLOR, DyeColor.WHITE), Block.UPDATE_ALL);
            if (context.getPlayer() != null) context.getItemInHand().hurtAndBreak(1, context.getPlayer(), p -> p.broadcastBreakEvent(context.getHand()));
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }
}
