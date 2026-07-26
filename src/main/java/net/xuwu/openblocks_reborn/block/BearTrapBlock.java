package net.xuwu.openblocks_reborn.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BearTrapBlock extends Block {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty TRIGGERED = BooleanProperty.create("triggered");
    private static final VoxelShape OPEN_SHAPE = Block.box(1, 0, 1, 15, 7, 15);
    private static final VoxelShape CLOSED_SHAPE = Block.box(2, 0, 2, 14, 14, 14);

    public BearTrapBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TRIGGERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, TRIGGERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.Entity entity) {
        if (entity instanceof Mob living && !level.hasNeighborSignal(pos)) {
            Vec3 motion = living.getDeltaMovement();
            living.setDeltaMovement(0.0D, Math.min(0.0D, motion.y), 0.0D);
            if (!level.isClientSide && !state.getValue(TRIGGERED)) {
                level.setBlock(pos, state.setValue(TRIGGERED, true), Block.UPDATE_ALL);
                living.hurt(level.damageSources().cactus(), 4.0F);
                level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.8F, 0.75F);
                level.scheduleTick(pos, this, 2);
            }
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(TRIGGERED)) return;
        var trapped = level.getEntitiesOfClass(Mob.class,
                new net.minecraft.world.phys.AABB(pos).inflate(0.15D), LivingEntity::isAlive);
        if (trapped.isEmpty() || level.hasNeighborSignal(pos)) {
            open(level, pos, state);
            return;
        }
        Mob mob = trapped.getFirst();
        mob.setPos(pos.getX() + 0.5D, pos.getY() + 0.05D, pos.getZ() + 0.5D);
        mob.setDeltaMovement(Vec3.ZERO);
        level.scheduleTick(pos, this, 2);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hit) {
        if (!level.isClientSide && state.getValue(TRIGGERED)) open(level, pos, state);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor,
                                   BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide && level.hasNeighborSignal(pos) && state.getValue(TRIGGERED)) open(level, pos, state);
    }

    private void open(Level level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(TRIGGERED, false), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.7F, 1.2F);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return state.getValue(TRIGGERED) ? 15 : 0;
    }

    @Override
    protected VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
                                  BlockPos pos, CollisionContext context) {
        return state.getValue(TRIGGERED) ? CLOSED_SHAPE : OPEN_SHAPE;
    }
}
