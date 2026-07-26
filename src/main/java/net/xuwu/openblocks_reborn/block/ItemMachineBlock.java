package net.xuwu.openblocks_reborn.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.xuwu.openblocks_reborn.blockentity.ItemMachineBlockEntity;
import net.xuwu.openblocks_reborn.menu.MenuHelper;
import net.xuwu.openblocks_reborn.menu.MachineLayout;

public class ItemMachineBlock extends Block implements EntityBlock {
    public enum Mode { PLACER, DROPPER, CANNON }
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");
    private final Mode mode;

    public ItemMachineBlock(BlockBehaviour.Properties properties, Mode mode) {
        super(properties);
        this.mode = mode;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false));
    }

    public Mode mode() {
        return mode;
    }

    @Override
    public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getNearestLookingDirection().getOpposite();
        if (mode == Mode.CANNON && facing.getAxis().isVertical()) {
            facing = context.getHorizontalDirection().getOpposite();
        }
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ItemMachineBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return mode == Mode.CANNON ? RenderShape.ENTITYBLOCK_ANIMATED : RenderShape.MODEL;
    }

    public InteractionResult useHeldItem(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
        // Continue into useWithoutItem so the menu opens even when the player is
        // holding something. Previously this path swallowed every right-click by
        // trying to quick-insert the held stack, making all three item machines
        // appear to have no GUI.
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        InteractionResult held = useHeldItem(player.getItemInHand(hand), state, level, pos, player, hand, hit);
        if (held != InteractionResult.PASS) return held;
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof ItemMachineBlockEntity machine) {
            if (player.isShiftKeyDown()) {
                for (int slot = 0; slot < machine.getInventory().getSlots(); slot++) {
                    ItemStack stack = machine.getInventory().extractItem(slot, Integer.MAX_VALUE, false);
                    if (!stack.isEmpty() && !player.addItem(stack)) Block.popResource(level, pos.above(), stack);
                }
            } else {
                MachineLayout layout = switch (mode) {
                            case PLACER -> MachineLayout.BLOCK_PLACER;
                            case DROPPER -> MachineLayout.ITEM_DROPPER;
                            case CANNON -> MachineLayout.CANNON;
                        };
                if (mode == Mode.DROPPER) {
                    MenuHelper.open(serverPlayer, Component.translatable("container.openblocks_reborn.dropper"),
                            machine.getInventory(), layout,
                            () -> level.getBlockEntity(pos) == machine && player.distanceToSqr(pos.getCenter()) <= 64.0,
                            MenuHelper.data(() -> 0, () -> 0, machine::getItemSpeed,
                                    () -> machine.usesRedstoneStrength() ? 1 : 0),
                            id -> handleDropperButton(machine, id));
                } else if (mode == Mode.CANNON) {
                    MenuHelper.open(serverPlayer, Component.translatable("container.openblocks_reborn.cannon"),
                            machine.getInventory(), layout,
                            () -> level.getBlockEntity(pos) == machine && player.distanceToSqr(pos.getCenter()) <= 64.0,
                            MenuHelper.data(
                                    () -> machine.getCannonTarget() == null ? 0 : 1,
                                    () -> machine.getCannonTarget() == null ? 0 : machine.getCannonTarget().getX(),
                                    () -> machine.getCannonTarget() == null ? 0 : machine.getCannonTarget().getY(),
                                    () -> machine.getCannonTarget() == null ? 0 : machine.getCannonTarget().getZ()));
                } else {
                    MenuHelper.open(serverPlayer, Component.translatable("container.openblocks_reborn." + mode.name().toLowerCase()),
                            machine.getInventory(), layout,
                            () -> level.getBlockEntity(pos) == machine && player.distanceToSqr(pos.getCenter()) <= 64.0);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean movedByPiston) {
        if (level.isClientSide) return;
        boolean powered = level.hasNeighborSignal(pos);
        int redstoneStrength = level.getBestNeighborSignal(pos);
        if (redstoneStrength == 0 && powered) redstoneStrength = 15;
        if (powered && !state.getValue(POWERED)) activate((ServerLevel)level, pos, state, redstoneStrength);
        if (powered != state.getValue(POWERED)) level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
    }

    private void activate(ServerLevel level, BlockPos pos, BlockState state, int redstoneStrength) {
        if (!(level.getBlockEntity(pos) instanceof ItemMachineBlockEntity machine)) return;
        for (int slot = 0; slot < machine.getInventory().getSlots(); slot++) {
            ItemStack stack = machine.getInventory().getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            Direction facing = state.getValue(FACING);
            BlockPos target = pos.relative(facing);
            if (mode == Mode.PLACER && stack.getItem() instanceof BlockItem blockItem && level.getBlockState(target).canBeReplaced()) {
                BlockState placed = blockItem.getBlock().defaultBlockState();
                if (placed.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                        && facing.getAxis().isHorizontal()) {
                    placed = placed.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
                } else if (placed.hasProperty(BlockStateProperties.FACING)) {
                    placed = placed.setValue(BlockStateProperties.FACING, facing);
                }
                if (placed.hasProperty(BlockStateProperties.AXIS)) {
                    placed = placed.setValue(BlockStateProperties.AXIS, facing.getAxis());
                }
                if (placed.canSurvive(level, target)) {
                    level.setBlock(target, placed, Block.UPDATE_ALL);
                    machine.getInventory().extractItem(slot, 1, false);
                }
            } else if (mode != Mode.PLACER) {
                ItemStack extracted = machine.getInventory().extractItem(slot, 1, false);
                ItemEntity entity = new ItemEntity(level, target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D, extracted);
                if (mode == Mode.CANNON && machine.getCannonTarget() != null) {
                    entity.setDeltaMovement(calculateCannonVelocity(entity.position(), machine.getCannonTarget().getCenter()));
                } else {
                    double velocity = mode == Mode.CANNON ? 1.2D
                            : calculateDropperSpeed(machine.getItemSpeed(), machine.usesRedstoneStrength(), redstoneStrength);
                    entity.setDeltaMovement(facing.getStepX() * velocity,
                            mode == Mode.CANNON ? 0.12D : facing.getStepY() * velocity,
                            facing.getStepZ() * velocity);
                }
                level.addFreshEntity(entity);
            }
            return;
        }
    }

    public static boolean handleDropperButton(ItemMachineBlockEntity machine, int id) {
        if (id >= 1_000 && id <= 1_400 && id % 10 == 0) {
            machine.setItemSpeed(id - 1_000);
            return true;
        }
        if (id == 2_000) {
            machine.toggleRedstoneStrength();
            return true;
        }
        return false;
    }

    public static double calculateDropperSpeed(int configuredSpeed, boolean useRedstoneStrength, int redstoneStrength) {
        double result = net.minecraft.util.Mth.clamp(configuredSpeed, 0, 400) / 100.0D;
        return useRedstoneStrength ? result * net.minecraft.util.Mth.clamp(redstoneStrength, 0, 15) / 15.0D : result;
    }

    /** Approximates the vanilla dropped-item gravity so the projectile reaches the selected block. */
    public static net.minecraft.world.phys.Vec3 calculateCannonVelocity(net.minecraft.world.phys.Vec3 origin,
                                                                        net.minecraft.world.phys.Vec3 destination) {
        double dx = destination.x - origin.x;
        double dz = destination.z - origin.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (horizontal < 0.001D) return new net.minecraft.world.phys.Vec3(0.0D, 0.7D, 0.0D);
        double speed = net.minecraft.util.Mth.clamp(0.45D + horizontal * 0.012D, 0.45D, 1.2D);
        double ticks = Math.max(1.0D, horizontal / speed);
        double vertical = (destination.y - origin.y + 0.02D * ticks * ticks) / ticks;
        vertical = net.minecraft.util.Mth.clamp(vertical, -0.2D, 1.5D);
        return new net.minecraft.world.phys.Vec3(dx / horizontal * speed, vertical, dz / horizontal * speed);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ItemMachineBlockEntity machine) {
            for (int slot = 0; slot < machine.getInventory().getSlots(); slot++) {
                Block.popResource(level, pos, machine.getInventory().extractItem(slot, Integer.MAX_VALUE, false));
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
