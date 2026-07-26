package net.xuwu.openblocks_reborn.block;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FlagBlock extends ColorableBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<AttachFace> FACE = BlockStateProperties.ATTACH_FACE;

    public FlagBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(FACE, AttachFace.FLOOR));
    }

    @Override
    public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, FACE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null || context.getClickedFace() == Direction.DOWN) return null;

        if (context.getClickedFace().getAxis().isHorizontal()) {
            state = state.setValue(FACE, AttachFace.WALL).setValue(FACING, context.getClickedFace());
        } else {
            state = state.setValue(FACE, AttachFace.FLOOR)
                    .setValue(FACING, context.getHorizontalDirection().getOpposite());
        }
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Block.box(0, 0, 0, 0, 0, 0);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(FACE) == AttachFace.FLOOR) return Block.box(7, 0, 7, 9, 16, 9);
        return switch (state.getValue(FACING)) {
            case NORTH, SOUTH -> Block.box(7, 0, 3, 9, 16, 13);
            case EAST, WEST -> Block.box(3, 0, 7, 13, 16, 9);
            default -> Block.box(7, 0, 7, 9, 16, 9);
        };
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction supportDirection = state.getValue(FACE) == AttachFace.WALL
                ? state.getValue(FACING).getOpposite()
                : Direction.DOWN;
        BlockPos supportPos = pos.relative(supportDirection);
        BlockState support = level.getBlockState(supportPos);
        if (state.getValue(FACE) == AttachFace.FLOOR
                && (support.getBlock() instanceof FenceBlock
                || support.is(this) && support.getValue(FACE) == AttachFace.FLOOR)) {
            return true;
        }
        return support.isFaceSturdy(level, supportPos, supportDirection.getOpposite());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        Direction supportDirection = state.getValue(FACE) == AttachFace.WALL
                ? state.getValue(FACING).getOpposite()
                : Direction.DOWN;
        if (direction == supportDirection && !state.canSurvive(level, pos)) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
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
