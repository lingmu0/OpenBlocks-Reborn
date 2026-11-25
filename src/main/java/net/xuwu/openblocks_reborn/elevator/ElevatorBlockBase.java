// Derived from ElevatorMod 1.21.1-1.11.4 by VsnGamer (MIT); see LICENSE-ElevatorMod.
package net.xuwu.openblocks_reborn.elevator;

import net.xuwu.openblocks_reborn.elevator.level.ElevatorBlockEntityBase;
import net.xuwu.openblocks_reborn.elevator.util.FakeUseContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Optional;
import java.util.function.Supplier;


public abstract class ElevatorBlockBase extends HorizontalDirectionalBlock implements EntityBlock {
    public static final BooleanProperty DIRECTIONAL = BooleanProperty.create("directional");
    public static final BooleanProperty SHOW_ARROW = BooleanProperty.create("show_arrow");


    private final DyeColor dyeColor;
    private final Supplier<BlockEntityType<? extends ElevatorBlockEntityBase>> tileEntityType;


    public ElevatorBlockBase(DyeColor color, Supplier<BlockEntityType<? extends ElevatorBlockEntityBase>> type) {

        super(Properties
                        .of()
                        .mapColor(color)
                        .sound(SoundType.WOOL)
                        .strength(0.8F)
                        .dynamicShape()
                        .noOcclusion()
                        .isValidSpawn(isValidSpawn(type))
//                .forceSolidOn()
        );

        registerDefaultState(
                defaultBlockState()
                        .setValue(FACING, Direction.NORTH)
                        .setValue(DIRECTIONAL, false)
                        .setValue(SHOW_ARROW, true)
        );

        dyeColor = color;
        tileEntityType = type;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, DIRECTIONAL, SHOW_ARROW);
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext useContext) {
        return false;
    }


    protected abstract void openMenu(Player player, ElevatorBlockEntityBase tile, BlockPos pos);

    @Override
    public ItemInteractionResult useItemOn(ItemStack itemStack, BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit) {
        if (worldIn.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }

        return getElevatorTile(worldIn, pos)
                .map(tile -> {
                    Block handBlock = Block.byItem(player.getItemInHand(handIn).getItem());
                    BlockState stateToApply = handBlock.getStateForPlacement(new FakeUseContext(player, handIn, hit));
                    if (stateToApply != null && tile.setCamoAndUpdate(stateToApply))
                        return ItemInteractionResult.SUCCESS; // If we successfully set camo, don't open the menu

                    // Remove camo
                    if (player.isCrouching() && tile.getHeldState() != null) {
                        tile.setCamoAndUpdate(null);
                        return ItemInteractionResult.SUCCESS;
                    }

                    openMenu(player, tile, pos);
                    return ItemInteractionResult.SUCCESS;
                })
                .orElse(ItemInteractionResult.FAIL);
    }

    public static StateArgumentPredicate<EntityType<?>> isValidSpawn(Supplier<BlockEntityType<? extends ElevatorBlockEntityBase>> tileType) {
        return (BlockState state, BlockGetter world, BlockPos pos, EntityType<?> entityType) ->
                Config.GENERAL.mobSpawn.get() &&
                        world.getBlockEntity(pos, tileType.get()).map(ElevatorBlockEntityBase::getHeldState)
                                .map(s -> s.isValidSpawn(world, pos, entityType))
                                .orElse(state.isFaceSturdy(world, pos, Direction.UP));
    }

    // Collision

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        return getHeldState(worldIn, pos)
                .map(s -> s.getCollisionShape(worldIn, pos, context))
                .orElse(super.getCollisionShape(state, worldIn, pos, context));
    }

    @Override
    public boolean isCollisionShapeFullBlock(BlockState state, BlockGetter world, BlockPos pos) {
        return getHeldState(world, pos)
                .map(s -> s.isCollisionShapeFullBlock(world, pos))
                .orElse(super.isCollisionShapeFullBlock(state, world, pos));
    }

    // Visual outline
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        return getHeldState(worldIn, pos)
                .map(s -> s.getShape(worldIn, pos, context))
                .orElse(super.getShape(state, worldIn, pos, context));
    }

    // TODO soulsand and honey use this
    @Override
    public float getSpeedFactor() {
        return super.getSpeedFactor();
    }

    // TODO honey uses this
    @Override
    public float getJumpFactor() {
        return super.getJumpFactor();
    }


    @Override
    public BlockState updateShape(BlockState stateIn, Direction facing, BlockState facingState, LevelAccessor worldIn, BlockPos currentPos, BlockPos facingPos) {
        getElevatorTile(worldIn, currentPos)
                .ifPresent(t -> {
                    if (t.getHeldState() == null) {
                        return;
                    }

                    BlockState appearance = getAppearance(facingState, worldIn, facingPos, facing, t.getHeldState(), currentPos);
                    BlockState updatedState = t.getHeldState().updateShape(facing, appearance, worldIn, currentPos, facingPos);
                    if (!updatedState.equals(t.getHeldState())) {
                        t.setHeldState(updatedState);
                    }
                });

        return super.updateShape(stateIn, facing, facingState, worldIn, currentPos, facingPos);
    }

    protected abstract BlockState getAppearance(BlockState facingState, LevelAccessor worldIn, BlockPos facingPos, Direction opposite, BlockState heldState, BlockPos currentPos);

    // Redstone
    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter reader, BlockPos pos, Direction direction) {
        return getHeldState(reader, pos)
                .map(s -> s.getSignal(reader, pos, direction))
                .orElse(super.getSignal(state, reader, pos, direction));
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter reader, BlockPos pos, Direction direction) {
        return getHeldState(reader, pos)
                .map(s -> s.getDirectSignal(reader, pos, direction))
                .orElse(super.getDirectSignal(state, reader, pos, direction));
    }

    // Light

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
        return true;
    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos) {
        return getHeldState(worldIn, pos)
                .map(s -> s.getLightBlock(worldIn, pos))
                .orElse(worldIn.getMaxLightLevel());
    }

    @Override
    protected float getShadeBrightness(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos) {
        return getHeldState(blockGetter, blockPos)
                .map(s -> s.getShadeBrightness(blockGetter, blockPos))
                .orElse(super.getShadeBrightness(blockState, blockGetter, blockPos));
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter world, BlockPos pos) {
        return getHeldState(world, pos)
                .map(s -> s.getOcclusionShape(world, pos))
                .orElse(super.getOcclusionShape(state, world, pos));
    }

    public DyeColor getColor() {
        return dyeColor;
    }

    private Optional<? extends ElevatorBlockEntityBase> getElevatorTile(BlockGetter world, BlockPos pos) {
        if (world == null || pos == null) {
            return Optional.empty();
        }

        return world.getBlockEntity(pos, tileEntityType.get());
    }

    protected Optional<BlockState> getHeldState(BlockGetter world, BlockPos pos) {
        return getElevatorTile(world, pos).map(ElevatorBlockEntityBase::getHeldState);
    }
}
