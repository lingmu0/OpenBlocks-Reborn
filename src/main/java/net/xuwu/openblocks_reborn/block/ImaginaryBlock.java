package net.xuwu.openblocks_reborn.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.xuwu.openblocks_reborn.blockentity.ImaginaryBlockEntity;
import net.xuwu.openblocks_reborn.item.GlassesItem;
import net.xuwu.openblocks_reborn.item.ImaginaryItem;

public class ImaginaryBlock extends Block implements EntityBlock {
    public enum Shape implements StringRepresentable {
        BLOCK("block"), HALF("half"), PANEL("panel"), STAIRS("stairs");
        private final String name;
        Shape(String name) { this.name = name; }
        @Override public String getSerializedName() { return name; }
    }

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<Shape> SHAPE = EnumProperty.create("shape", Shape.class);

    public ImaginaryBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SHAPE, Shape.BLOCK));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SHAPE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ImaginaryBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                              BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (!(stack.getItem() instanceof ImaginaryItem)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) level.setBlock(pos, state.cycle(SHAPE), Block.UPDATE_ALL);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        return InteractionResult.PASS;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        // Visibility is player/glasses dependent, so the ordinary chunk model
        // cannot decide whether this block should be drawn.
        return RenderShape.INVISIBLE;
    }

    public static VoxelShape geometry(BlockState state) {
        return switch (state.getValue(SHAPE)) {
            case BLOCK -> Shapes.block();
            // Legacy panels are thin horizontal imaginary surfaces, not slabs.
            case PANEL -> Block.box(0, 14.4D, 0, 16, 16, 16);
            case HALF -> Block.box(0, 6.4D, 0, 16, 8, 16);
            case STAIRS -> switch (state.getValue(FACING)) {
                case SOUTH -> Shapes.or(
                        Block.box(0, 6.4D, 8, 16, 8, 16),
                        Block.box(0, 14.4D, 0, 16, 16, 8));
                case EAST -> Shapes.or(
                        Block.box(8, 6.4D, 0, 16, 8, 16),
                        Block.box(0, 14.4D, 0, 8, 16, 16));
                case WEST -> Shapes.or(
                        Block.box(0, 6.4D, 0, 8, 8, 16),
                        Block.box(8, 14.4D, 0, 16, 16, 16));
                default -> Shapes.or(
                        Block.box(0, 6.4D, 0, 16, 8, 8),
                        Block.box(0, 14.4D, 8, 16, 16, 16));
            };
        };
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (!(level.getBlockEntity(pos) instanceof ImaginaryBlockEntity imaginary)
                || !(context instanceof net.minecraft.world.phys.shapes.EntityCollisionContext entityContext)
                || !(entityContext.getEntity() instanceof Player player)
                || !GlassesItem.testImaginary(player, imaginary,
                GlassesItem.ImaginaryProperty.SELECTABLE)) {
            return Shapes.empty();
        }
        return geometry(state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                           CollisionContext context) {
        if (!(level.getBlockEntity(pos) instanceof ImaginaryBlockEntity imaginary)) return Shapes.empty();
        if (!(context instanceof net.minecraft.world.phys.shapes.EntityCollisionContext entityContext)
                || !(entityContext.getEntity() instanceof Player player)) {
            return Shapes.empty();
        }
        return GlassesItem.testImaginary(player, imaginary, GlassesItem.ImaginaryProperty.SOLID)
                ? geometry(state) : Shapes.empty();
    }
}
