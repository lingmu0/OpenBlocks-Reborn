package net.xuwu.openblocks_reborn.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.xuwu.openblocks_reborn.blockentity.VacuumHopperBlockEntity;
import net.xuwu.openblocks_reborn.registry.ModBlockEntities;
import net.xuwu.openblocks_reborn.menu.MenuHelper;
import net.xuwu.openblocks_reborn.menu.MachineLayout;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.util.StringRepresentable;

public class VacuumHopperBlock extends Block implements EntityBlock {
    public enum OutputType implements StringRepresentable {
        NONE("none"), ITEMS("items"), FLUIDS("fluids"), BOTH("both");

        private final String serializedName;

        OutputType(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }

    public static final EnumProperty<OutputType> DOWN = EnumProperty.create("down", OutputType.class);
    public static final EnumProperty<OutputType> UP = EnumProperty.create("up", OutputType.class);
    public static final EnumProperty<OutputType> NORTH = EnumProperty.create("north", OutputType.class);
    public static final EnumProperty<OutputType> SOUTH = EnumProperty.create("south", OutputType.class);
    public static final EnumProperty<OutputType> WEST = EnumProperty.create("west", OutputType.class);
    public static final EnumProperty<OutputType> EAST = EnumProperty.create("east", OutputType.class);

    public VacuumHopperBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(DOWN, OutputType.NONE)
                .setValue(UP, OutputType.NONE)
                .setValue(NORTH, OutputType.NONE)
                .setValue(SOUTH, OutputType.NONE)
                .setValue(WEST, OutputType.NONE)
                .setValue(EAST, OutputType.NONE));
    }

    @Override
    public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DOWN, UP, NORTH, SOUTH, WEST, EAST);
    }

    public static BlockState applyOutputState(BlockState state, int itemMask, int experienceMask) {
        for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
            int bit = 1 << direction.get3DDataValue();
            boolean items = (itemMask & bit) != 0;
            boolean fluids = (experienceMask & bit) != 0;
            OutputType type = items ? (fluids ? OutputType.BOTH : OutputType.ITEMS)
                    : (fluids ? OutputType.FLUIDS : OutputType.NONE);
            state = state.setValue(property(direction), type);
        }
        return state;
    }

    public static EnumProperty<OutputType> property(net.minecraft.core.Direction direction) {
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
        return new VacuumHopperBlockEntity(pos, state);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!level.isClientSide && type == ModBlockEntities.VACUUM_HOPPER.get()) {
            return (tickerLevel, pos, tickerState, blockEntity) -> VacuumHopperBlockEntity.serverTick(
                    (ServerLevel)tickerLevel, pos, tickerState, (VacuumHopperBlockEntity)blockEntity);
        }
        return null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        InteractionResult held = useHeldItem(player.getItemInHand(hand), state, level, pos, player, hand, hit);
        if (held != InteractionResult.PASS) return held;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof VacuumHopperBlockEntity hopper) {
            if (player.isShiftKeyDown()) {
                int xp = hopper.takeExperience();
                if (xp > 0) player.giveExperiencePoints(xp);
                for (int slot = 0; slot < hopper.getInventory().getSlots(); slot++) {
                    ItemStack stack = hopper.getInventory().extractItem(slot, Integer.MAX_VALUE, false);
                    if (!stack.isEmpty() && !player.addItem(stack)) Block.popResource(level, pos.above(), stack);
                }
                player.displayClientMessage(Component.translatable("message.openblocks_reborn.vacuum_hopper", xp), true);
            } else if (player instanceof ServerPlayer serverPlayer) {
                MenuHelper.open(serverPlayer, Component.translatable("container.openblocks_reborn.vacuum_hopper"),
                        hopper.getInventory(), MachineLayout.VACUUM_HOPPER,
                        () -> level.getBlockEntity(pos) == hopper && player.distanceToSqr(pos.getCenter()) <= 64.0,
                        MenuHelper.data(() -> hopper.getExperienceTank().getFluidAmount(),
                                () -> hopper.getExperienceTank().getCapacity(), hopper::getItemOutputMask,
                                hopper::getExperienceOutputMask),
                        id -> handleMenuButton(hopper, id));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public static boolean handleMenuButton(VacuumHopperBlockEntity hopper, int id) {
        if (id >= 5_000 && id < 5_006) {
            hopper.toggleItemOutput(net.minecraft.core.Direction.from3DDataValue(id - 5_000));
            return true;
        }
        if (id >= 5_100 && id < 5_106) {
            hopper.toggleExperienceOutput(net.minecraft.core.Direction.from3DDataValue(id - 5_100));
            return true;
        }
        return false;
    }

    public InteractionResult useHeldItem(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof VacuumHopperBlockEntity hopper
                && FluidUtil.interactWithFluidHandler(player, hand, hopper.getExperienceTank())) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof VacuumHopperBlockEntity hopper) {
            for (int slot = 0; slot < hopper.getInventory().getSlots(); slot++) {
                Block.popResource(level, pos, hopper.getInventory().extractItem(slot, Integer.MAX_VALUE, false));
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
