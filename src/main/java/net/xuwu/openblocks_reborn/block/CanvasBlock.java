package net.xuwu.openblocks_reborn.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.xuwu.openblocks_reborn.blockentity.CanvasBlockEntity;
import net.xuwu.openblocks_reborn.item.PaintBrushItem;
import net.xuwu.openblocks_reborn.item.SqueegeeItem;
import net.xuwu.openblocks_reborn.item.StencilItem;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import java.util.List;

public class CanvasBlock extends Block implements EntityBlock {
    public CanvasBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CanvasBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof DyeItem dye && level.getBlockEntity(pos) instanceof CanvasBlockEntity canvas) {
            if (!level.isClientSide) {
                int color = 0xFF000000 | dye.getDyeColor().getTextureDiffuseColor();
                if (player.isShiftKeyDown()) canvas.setAll(color); else canvas.setColor(hit.getDirection(), color);
                if (!player.getAbilities().instabuild) stack.shrink(1);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        // The default interaction rotates/removes an attached stencil. Painting,
        // scraping and placing a stencil are implemented by the items themselves,
        // so they must skip that default interaction and reach Item#useOn.
        if (stack.getItem() instanceof PaintBrushItem
                || stack.getItem() instanceof SqueegeeItem
                || stack.getItem() instanceof StencilItem) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof CanvasBlockEntity canvas)
                || canvas.getStencilPattern(hit.getDirection()) < 0) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            if (player.isShiftKeyDown()) {
                int removed = canvas.removeStencil(hit.getDirection());
                if (removed >= 0) {
                    ItemStack stencil = StencilItem.createPatternStack(removed);
                    if (!player.addItem(stencil)) Block.popResource(level, pos.relative(hit.getDirection()), stencil);
                }
            } else {
                canvas.rotateStencil(hit.getDirection());
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof CanvasBlockEntity canvas
                ? canvas.paintedPixels() * 15 / CanvasBlockEntity.TOTAL_PIXELS : 0;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof CanvasBlockEntity canvas) return List.of(canvas.toItemStack());
        return super.getDrops(state, params);
    }
}
