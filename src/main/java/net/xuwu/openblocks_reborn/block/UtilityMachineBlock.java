package net.xuwu.openblocks_reborn.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.BlockHitResult;
import net.xuwu.openblocks_reborn.blockentity.UtilityMachineBlockEntity;
import net.xuwu.openblocks_reborn.menu.MenuHelper;
import net.xuwu.openblocks_reborn.menu.MachineLayout;
import net.xuwu.openblocks_reborn.registry.ModBlockEntities;
import net.xuwu.openblocks_reborn.registry.ModFluids;
import net.xuwu.openblocks_reborn.item.XpBucketItem;
import net.minecraftforge.fluids.FluidUtil;

public class UtilityMachineBlock extends Block implements EntityBlock {
    private static final VoxelShape SPRINKLER_NORTH_SOUTH = Block.box(4.8D, 0.0D, 0.0D, 11.2D, 4.8D, 16.0D);
    private static final VoxelShape SPRINKLER_EAST_WEST = Block.box(0.0D, 0.0D, 4.8D, 16.0D, 4.8D, 11.2D);
    private static final VoxelShape PROJECTOR_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D);

    public enum Kind {
        AUTO_ANVIL(3), AUTO_ENCHANTMENT_TABLE(3), DONATION_STATION(1),
        PAINT_MIXER(6), DRAWING_TABLE(2), PROJECTOR(1), SPRINKLER(9);

        private final int slots;

        Kind(int slots) {
            this.slots = slots;
        }

        public int slots() {
            return slots;
        }

        public String id() {
            return name().toLowerCase();
        }
    }

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");
    private final Kind kind;

    public UtilityMachineBlock(BlockBehaviour.Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false));
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(POWERED, kind != Kind.SPRINKLER && kind != Kind.PROJECTOR
                        && context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        // This marker uses vanilla water semantics without being a placeable or
        // flowing water block. Its legacy replacement is air, so breaking the
        // sprinkler cannot leave a water source behind.
        return kind == Kind.SPRINKLER
                ? ModFluids.SPRINKLER_WATER.get().defaultFluidState()
                : super.getFluidState(state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (kind == Kind.PROJECTOR) return PROJECTOR_SHAPE;
        if (kind != Kind.SPRINKLER) return super.getShape(state, level, pos, context);
        return state.getValue(FACING).getAxis() == Direction.Axis.Z
                ? SPRINKLER_NORTH_SOUTH : SPRINKLER_EAST_WEST;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new UtilityMachineBlockEntity(pos, state);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!level.isClientSide && type == ModBlockEntities.UTILITY_MACHINE.get()) {
            return (tickerLevel, pos, tickerState, blockEntity) -> UtilityMachineBlockEntity.serverTick(
                    (ServerLevel)tickerLevel, pos, tickerState, (UtilityMachineBlockEntity)blockEntity);
        }
        return null;
    }

    private InteractionResult open(Level level, BlockPos pos, Player player) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof UtilityMachineBlockEntity machine) {
            if (kind == Kind.DONATION_STATION && !machine.getInventory().getStackInSlot(0).isEmpty()) {
                ItemStack donation = machine.getInventory().getStackInSlot(0);
                var key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(donation.getItem());
                player.displayClientMessage(Component.translatable("message.openblocks_reborn.donation",
                        donation.getHoverName(), key.getNamespace()), false);
            }
            MenuHelper.open(serverPlayer, Component.translatable("container.openblocks_reborn." + kind.id()),
                    machine.getInventory(), MachineLayout.forUtility(kind),
                    () -> level.getBlockEntity(pos) == machine && player.distanceToSqr(pos.getCenter()) <= 64.0D,
                    MenuHelper.data(
                            () -> kind == Kind.PAINT_MIXER ? machine.getSelectedPaintColor()
                                    : (kind == Kind.DRAWING_TABLE ? machine.getDrawingMode()
                                    : (kind == Kind.PROJECTOR ? machine.getProjectorRotation()
                                    : (machine.exposesFluidTank() ? machine.getFluidTank().getFluidAmount() : 0))),
                            () -> kind == Kind.PAINT_MIXER ? machine.getPaintProgress()
                                    : (kind == Kind.DRAWING_TABLE ? machine.getDrawingSelection()
                                    : (kind == Kind.PROJECTOR ? (machine.isProjectorActive() ? 1 : 0)
                                    : (machine.exposesFluidTank() ? machine.getFluidTank().getCapacity() : 0))),
                            () -> kind == Kind.AUTO_ENCHANTMENT_TABLE ? machine.getEnchantPowerLimit()
                                    : (machine.getBlockState().getValue(POWERED) ? 1 : 0),
                            machine::getEnchantLevel,
                            () -> machine.getSideConfiguration(0),
                            () -> machine.getSideConfiguration(1),
                            () -> machine.getSideConfiguration(2),
                            () -> machine.getSideConfiguration(3),
                            machine::getAvailableEnchantPower,
                            machine::getSelectedMachineCost),
                    id -> handleMenuButton(machine, kind, id),
                    kind == Kind.AUTO_ANVIL ? machine::setAnvilItemName : ignored -> { });
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public static boolean handleMenuButton(UtilityMachineBlockEntity machine, Kind kind, int id) {
        if (id >= 6_000 && id < 6_100 && machine.supportsSideConfiguration()) {
            int encoded = id - 6_000;
            int channel = encoded / 10;
            int direction = encoded % 10;
            if (direction >= 0 && direction < Direction.values().length) {
                return machine.toggleSide(channel, Direction.from3DDataValue(direction));
            }
        }
        if (id >= 6_100 && id < 6_104 && machine.supportsSideConfiguration()) {
            return machine.toggleAutomatic(id - 6_100);
        }
        if (kind == Kind.AUTO_ENCHANTMENT_TABLE) {
            if ((id & 0xFF000000) == 0x03000000) {
                machine.setEnchantPowerLimit(id & 0x00FFFFFF);
                return true;
            }
        } else if (kind == Kind.PAINT_MIXER) {
            if ((id & 0xFF000000) == 0x01000000) {
                machine.setSelectedPaintColor(id & 0xFFFFFF);
                return true;
            }
            if (id == 0x02000000) return machine.startPaintMix();
        } else if (kind == Kind.DRAWING_TABLE) {
            if (id == 4_000) {
                machine.cycleDrawingMode();
                return true;
            }
            if (id == 4_001 || id == 4_002) {
                machine.changeDrawingSelection(id == 4_001 ? 1 : -1);
                return true;
            }
            if ((id & 0xFF000000) == 0x04000000) {
                return machine.printGlyph(id & 0xFFFFFF);
            }
        } else if (kind == Kind.PROJECTOR) {
            if (id == 4_500 || id == 4_501) {
                machine.rotateProjector(id == 4_501 ? 1 : -1);
                return true;
            }
        }
        return false;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        InteractionResult held = useHeldItem(player.getItemInHand(hand), state, level, pos, player, hand, hit);
        if (held != InteractionResult.PASS) return held;
        return open(level, pos, player);
    }

    public InteractionResult useHeldItem(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof UtilityMachineBlockEntity machine
                && machine.exposesFluidTank()) {
            if (XpBucketItem.emptyInto(stack, level, player, hand, machine.getFluidTank())
                    || FluidUtil.interactWithFluidHandler(player, hand, machine.getFluidTank())) {
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return InteractionResult.sidedSuccess(open(level, pos, player) != InteractionResult.PASS);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor,
                                   BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide) {
            if (kind == Kind.SPRINKLER || kind == Kind.PROJECTOR) return;
            boolean powered = level.hasNeighborSignal(pos);
            if (powered != state.getValue(POWERED)) {
                level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof UtilityMachineBlockEntity machine) {
            for (int slot = 0; slot < machine.getInventory().getSlots(); slot++) {
                Block.popResource(level, pos, machine.getInventory().extractItem(slot, Integer.MAX_VALUE, false));
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
