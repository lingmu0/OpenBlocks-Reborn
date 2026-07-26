package net.xuwu.openblocks_reborn.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class EnhancedSpongeBlock extends Block {
    private static final int RADIUS = 4;

    public EnhancedSpongeBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide && !oldState.is(state.getBlock())) drain(level, pos);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) drain(level, pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public static int drain(Level level, BlockPos center) {
        int removed = 0;
        for (BlockPos target : BlockPos.betweenClosed(center.offset(-RADIUS, -RADIUS, -RADIUS), center.offset(RADIUS, RADIUS, RADIUS))) {
            if (center.distManhattan(target) > RADIUS * 2) continue;
            BlockState targetState = level.getBlockState(target);
            if (targetState.getBlock() instanceof LiquidBlock) {
                level.setBlock(target, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                removed++;
            }
        }
        return removed;
    }
}
