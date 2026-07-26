package net.xuwu.openblocks_reborn.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.xuwu.openblocks_reborn.blockentity.TankBlockEntity;
import net.xuwu.openblocks_reborn.registry.ModBlockEntities;
import net.xuwu.openblocks_reborn.registry.ModFluids;
import net.xuwu.openblocks_reborn.item.XpBucketItem;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import java.util.List;

public class TankBlock extends Block implements EntityBlock {
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty EAST = BooleanProperty.create("east");

    public TankBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(DOWN, false).setValue(UP, false)
                .setValue(NORTH, false).setValue(SOUTH, false)
                .setValue(WEST, false).setValue(EAST, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DOWN, UP, NORTH, SOUTH, WEST, EAST);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
            state = state.setValue(property(direction),
                    context.getLevel().getBlockState(context.getClickedPos().relative(direction)).is(this));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, net.minecraft.core.Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return state.setValue(property(direction), neighborState.is(this));
    }

    private static BooleanProperty property(net.minecraft.core.Direction direction) {
        return switch (direction) {
            case DOWN -> DOWN;
            case UP -> UP;
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
        };
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TankBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!level.isClientSide && type == ModBlockEntities.TANK.get()) {
            return (tickerLevel, pos, tickerState, blockEntity) ->
                    TankBlockEntity.serverTick((net.minecraft.server.level.ServerLevel) tickerLevel, pos, tickerState,
                            (TankBlockEntity) blockEntity);
        }
        return null;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof TankBlockEntity tank) {
            // Give a filled XP bucket the same priority as a vanilla water bucket:
            // empty it into the tank before any block GUI/default interaction.
            if (XpBucketItem.emptyInto(stack, level, player, hand, tank.getTank())) {
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
            if (FluidUtil.interactWithFluidHandler(player, hand, tank.getTank())) {
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof TankBlockEntity tank) {
            var fluid = tank.getTank().getFluid();
            player.displayClientMessage(fluid.isEmpty()
                    ? Component.translatable("message.openblocks_reborn.tank.empty")
                    : Component.translatable("message.openblocks_reborn.tank.contents", fluid.getHoverName(), fluid.getAmount(), tank.getTank().getCapacity()), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof TankBlockEntity tank) return List.of(tank.toItemStack());
        return super.getDrops(state, params);
    }
}
