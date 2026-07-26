package net.xuwu.openblocks_reborn.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.List;

public class BlockBreakerBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");

    public BlockBreakerBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean movedByPiston) {
        if (level.isClientSide) return;
        boolean powered = level.hasNeighborSignal(pos);
        if (powered && !state.getValue(POWERED)) {
            Direction facing = state.getValue(FACING);
            BlockPos target = pos.relative(facing);
            BlockState targetState = level.getBlockState(target);
            if (!targetState.isAir() && targetState.getDestroySpeed(level, target) >= 0.0F
                    && level instanceof ServerLevel serverLevel) {
                breakAndOutput(serverLevel, pos, facing, target, targetState);
            }
        }
        if (powered != state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
        }
    }

    private static void breakAndOutput(ServerLevel level, BlockPos breakerPos, Direction facing,
                                       BlockPos target, BlockState targetState) {
        BlockEntity targetBlockEntity = targetState.hasBlockEntity() ? level.getBlockEntity(target) : null;
        List<ItemStack> drops = Block.getDrops(targetState, level, target, targetBlockEntity);
        if (!level.setBlock(target, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL)) return;
        level.levelEvent(2001, target, Block.getId(targetState));

        BlockPos outputPos = breakerPos.relative(facing.getOpposite());
        var output = level.getCapability(Capabilities.ItemHandler.BLOCK, outputPos, facing);
        for (ItemStack drop : drops) {
            ItemStack remainder = output == null ? drop : ItemHandlerHelper.insertItem(output, drop, false);
            if (!remainder.isEmpty()) Block.popResource(level, outputPos, remainder);
        }
    }
}
