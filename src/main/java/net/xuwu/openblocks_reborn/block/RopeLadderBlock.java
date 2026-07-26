package net.xuwu.openblocks_reborn.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class RopeLadderBlock extends Block {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public RopeLadderBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        Direction clickedFace = context.getClickedFace();
        if (clickedFace.getAxis().isHorizontal()) {
            BlockState attached = defaultBlockState().setValue(FACING, clickedFace);
            if (attached.canSurvive(level, pos)) return attached;
        }

        BlockState above = level.getBlockState(pos.above());

        if (above.is(this)) {
            BlockState hanging = defaultBlockState().setValue(FACING, above.getValue(FACING));
            if (hanging.canSurvive(level, pos)) return hanging;
        }

        for (Direction direction : context.getNearestLookingDirections()) {
            if (!direction.getAxis().isHorizontal()) continue;
            BlockState attached = defaultBlockState().setValue(FACING, direction.getOpposite());
            if (attached.canSurvive(level, pos)) return attached;
        }
        return null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (level.isClientSide) return;

        boolean creative = placer instanceof Player player && player.getAbilities().instabuild;
        int available = creative ? 63 : Math.max(0, stack.getCount() - 1);
        for (int distance = 1; distance <= available; distance++) {
            BlockPos target = pos.below(distance);
            if (!level.getBlockState(target).canBeReplaced()
                    || !state.canSurvive(level, target)) break;
            level.setBlock(target, state, Block.UPDATE_ALL);
            if (!creative) stack.shrink(1);
        }
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState above = level.getBlockState(pos.above());
        if (above.is(this) && above.getValue(FACING) == state.getValue(FACING)) return true;

        Direction facing = state.getValue(FACING);
        BlockPos supportPos = pos.relative(facing.getOpposite());
        return level.getBlockState(supportPos).isFaceSturdy(level, supportPos, facing);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor,
                                   BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide && !state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
            return;
        }
        super.neighborChanged(state, level, pos, neighbor, neighborPos, movedByPiston);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockPos below = pos.below();
            BlockState belowState = level.getBlockState(below);
            if (belowState.is(this) && belowState.getValue(FACING) == state.getValue(FACING)) {
                level.destroyBlock(below, true);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public boolean isLadder(BlockState state, LevelReader level, BlockPos pos, LivingEntity entity) {
        return true;
    }

    @Override
    public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> Block.box(0, 0, 13, 16, 16, 16);
            case SOUTH -> Block.box(0, 0, 0, 16, 16, 3);
            case WEST -> Block.box(13, 0, 0, 16, 16, 16);
            default -> Block.box(0, 0, 0, 3, 16, 16);
        };
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
