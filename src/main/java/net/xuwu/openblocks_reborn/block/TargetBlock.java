package net.xuwu.openblocks_reborn.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TargetBlock extends Block {
    public static final IntegerProperty POWER = IntegerProperty.create("power", 0, 15);
    public static final BooleanProperty DEPLOYED = BooleanProperty.create("deployed");
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public TargetBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(POWER, 0)
                .setValue(DEPLOYED, false).setValue(FACING, Direction.NORTH));
    }

    @Override
    public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWER, DEPLOYED, FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(DEPLOYED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    public void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        if (level.isClientSide || !state.getValue(DEPLOYED)) return;
        Vec3 local = hit.getLocation().subtract(Vec3.atLowerCornerOf(hit.getBlockPos()));
        Direction face = hit.getDirection();
        double a = face.getAxis() == Direction.Axis.X ? local.z : local.x;
        double b = face.getAxis() == Direction.Axis.Y ? local.z : local.y;
        double distance = Math.sqrt((a - 0.5D) * (a - 0.5D) + (b - 0.5D) * (b - 0.5D));
        int power = Mth.clamp(15 - (int)Math.floor(distance * 20.0D), 1, 15);
        level.setBlock(hit.getBlockPos(), state.setValue(POWER, power), Block.UPDATE_ALL);
        level.scheduleTick(hit.getBlockPos(), this, 10);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(POWER) != 0) level.setBlock(pos, state.setValue(POWER, 0), Block.UPDATE_ALL);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor,
                                   BlockPos neighborPos, boolean movedByPiston) {
        if (level.isClientSide) return;
        boolean deployed = level.hasNeighborSignal(pos);
        if (deployed != state.getValue(DEPLOYED)) {
            dropEmbeddedArrows(level, pos);
            level.playSound(null, pos,
                    deployed ? SoundEvents.PISTON_EXTEND : SoundEvents.PISTON_CONTRACT,
                    SoundSource.BLOCKS, 0.5F, 1.0F);
            level.setBlock(pos, state.setValue(DEPLOYED, deployed), Block.UPDATE_ALL);
        }
    }

    private static void dropEmbeddedArrows(Level level, BlockPos pos) {
        for (AbstractArrow arrow : level.getEntitiesOfClass(AbstractArrow.class,
                new net.minecraft.world.phys.AABB(pos).inflate(0.2D))) {
            if (arrow.pickup == AbstractArrow.Pickup.ALLOWED) {
                Block.popResource(level, pos, arrow.getPickResult());
            }
            arrow.discard();
        }
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(POWER);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (!state.getValue(DEPLOYED)) return Block.box(0, 0, 0, 16, 2, 16);
        return switch (state.getValue(FACING)) {
            case NORTH -> Block.box(0, 0, 0, 16, 16, 2);
            case SOUTH -> Block.box(0, 0, 14, 16, 16, 16);
            case EAST -> Block.box(14, 0, 0, 16, 16, 16);
            case WEST -> Block.box(0, 0, 0, 2, 16, 16);
            default -> Block.box(0, 0, 0, 16, 2, 16);
        };
    }
}
