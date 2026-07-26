package net.xuwu.openblocks_reborn.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.AABB;

public class FanBlock extends Block {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public FanBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide) level.scheduleTick(pos, this, 1);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.hasNeighborSignal(pos)) {
            Direction facing = state.getValue(FACING);
            AABB area = new AABB(pos).expandTowards(facing.getStepX() * 6.0D, 2.0D, facing.getStepZ() * 6.0D).inflate(0.5D);
            for (Entity entity : level.getEntitiesOfClass(Entity.class, area, entity -> !entity.isSpectator())) {
                entity.push(facing.getStepX() * 0.12D, 0.03D, facing.getStepZ() * 0.12D);
                entity.hurtMarked = true;
            }
        }
        level.scheduleTick(pos, this, 1);
    }
}
