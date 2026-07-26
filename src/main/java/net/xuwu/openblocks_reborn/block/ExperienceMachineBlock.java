package net.xuwu.openblocks_reborn.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.xuwu.openblocks_reborn.blockentity.ExperienceBlockEntity;
import net.xuwu.openblocks_reborn.entity.ShowerExperienceOrb;
import net.xuwu.openblocks_reborn.registry.ModFluids;
import net.xuwu.openblocks_reborn.registry.ModBlockEntities;
import net.xuwu.openblocks_reborn.item.XpBucketItem;
import net.xuwu.openblocks_reborn.menu.MenuHelper;
import net.xuwu.openblocks_reborn.menu.MachineLayout;

public class ExperienceMachineBlock extends Block implements EntityBlock {
    public enum Mode { DRAIN, BOTTLER, SHOWER }
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private final Mode mode;

    public ExperienceMachineBlock(BlockBehaviour.Properties properties, Mode mode) {
        super(properties);
        this.mode = mode;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public Mode mode() {
        return mode;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (mode == Mode.SHOWER && context.getClickedFace().getAxis().isHorizontal()) {
            return defaultBlockState().setValue(FACING, context.getClickedFace());
        }
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ExperienceBlockEntity(pos, state);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlockEntities.EXPERIENCE_MACHINE.get()) return null;
        return (tickerLevel, pos, tickerState, blockEntity) -> {
            long gameTime = tickerLevel.getGameTime();
            if (mode == Mode.DRAIN || mode == Mode.SHOWER && gameTime % 3L == 0L
                    || mode == Mode.BOTTLER) {
                runServerOperation(tickerState, (ServerLevel)tickerLevel, pos, tickerLevel.random);
            }
        };
    }

    private void runServerOperation(BlockState state, ServerLevel level, BlockPos pos,
                                    RandomSource random) {
        if (mode == Mode.DRAIN && level.getBlockEntity(pos) instanceof ExperienceBlockEntity storage) {
            var receiver = level.getCapability(Capabilities.FluidHandler.BLOCK, pos.below(), Direction.UP);
            if (receiver != null) {
                var offered = storage.getTank().drain(1000, IFluidHandler.FluidAction.SIMULATE);
                if (!offered.isEmpty()) {
                    int accepted = receiver.fill(offered, IFluidHandler.FluidAction.SIMULATE);
                    if (accepted > 0) receiver.fill(storage.getTank().drain(accepted, IFluidHandler.FluidAction.EXECUTE),
                            IFluidHandler.FluidAction.EXECUTE);
                }
            }
            for (ExperienceOrb orb : level.getEntitiesOfClass(ExperienceOrb.class,
                    new AABB(pos).expandTowards(0.0D, 0.35D, 0.0D))) {
                int value = orb.getValue();
                if (storage.tryAddExperience(value) == value) orb.discard();
            }
            // Player.totalExperience can be zero or stale when XP was granted as levels
            // (for example by /experience add ... levels). Read the actual level and
            // progress bar; the entity query includes both real ServerPlayers and tests.
            for (Player player : level.getEntitiesOfClass(Player.class,
                    new AABB(pos).inflate(0.01D), Player::isAlive)) {
                if (!player.isAlive() || !isStandingOnDrain(player, pos)) continue;
                int offeredXp = Math.min(4, currentExperiencePoints(player));
                int acceptedXp = storage.tryAddExperience(offeredXp);
                if (acceptedXp > 0) {
                    player.giveExperiencePoints(-acceptedXp);
                    if (level.getGameTime() % 20L == 0L) {
                        level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP,
                                SoundSource.BLOCKS, 0.1F, 0.9F + random.nextFloat() * 0.2F);
                    }
                }
            }
        }
        if (mode == Mode.SHOWER && level.hasNeighborSignal(pos)
                && level.getBlockEntity(pos) instanceof ExperienceBlockEntity storage) {
            pullShowerFluid(level, pos, state, storage);
            int available = storage.getExperience();
            if (available > 0) {
                int orbValue = ExperienceOrb.getExperienceValue(available);
                int requiredFluid = ModFluids.xpToFluid(orbValue);
                if (storage.getTank().getFluidAmount() >= requiredFluid) {
                    ExperienceOrb orb = new ShowerExperienceOrb(level, pos.getX() + 0.5D,
                            pos.getY() + 0.1D, pos.getZ() + 0.5D, orbValue);
                    if (level.addFreshEntity(orb)) {
                        storage.consumeExperience(orbValue);
                    }
                }
            }
        }
        if (mode == Mode.BOTTLER && level.getBlockEntity(pos) instanceof ExperienceBlockEntity storage) {
            storage.tickBottler(level, pos);
        }
    }

    static boolean isStandingOnDrain(Player player, BlockPos pos) {
        AABB bounds = player.getBoundingBox();
        double feet = bounds.minY;
        return feet >= pos.getY() - 0.05D && feet <= pos.getY() + 1.05D
                && bounds.maxX > pos.getX() && bounds.minX < pos.getX() + 1.0D
                && bounds.maxZ > pos.getZ() && bounds.minZ < pos.getZ() + 1.0D;
    }

    static int currentExperiencePoints(Player player) {
        int level = Math.max(0, player.experienceLevel);
        int completedLevels = ModFluids.experienceForLevel(level);
        return Math.max(0, completedLevels
                + Mth.floor(Mth.clamp(player.experienceProgress, 0.0F, 1.0F)
                * player.getXpNeededForNextLevel()));
    }

    private static void pullShowerFluid(ServerLevel level, BlockPos pos, BlockState state,
                                        ExperienceBlockEntity storage) {
        Direction facing = state.getValue(FACING);
        BlockPos sourcePos = pos.relative(facing.getOpposite());
        IFluidHandler source = level.getCapability(Capabilities.FluidHandler.BLOCK, sourcePos, facing);
        if (source == null || storage.getTank().getSpace() <= 0) return;
        int requested = Math.min(100, storage.getTank().getSpace());
        var offered = source.drain(requested, IFluidHandler.FluidAction.SIMULATE);
        if (offered.isEmpty() || !offered.is(ModFluids.XP_JUICE.get())) return;
        int accepted = storage.getTank().fill(offered, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) return;
        var drained = source.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
        storage.getTank().fill(drained, IFluidHandler.FluidAction.EXECUTE);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof ExperienceBlockEntity storage) {
            if (XpBucketItem.emptyInto(stack, level, player, hand, storage.getTank())
                    || FluidUtil.interactWithFluidHandler(player, hand, storage.getTank())) {
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        if (mode == Mode.BOTTLER && stack.is(Items.GLASS_BOTTLE)
                && level.getBlockEntity(pos) instanceof ExperienceBlockEntity storage
                && storage.getExperience() >= ModFluids.XP_PER_BOTTLE) {
            if (!level.isClientSide && storage.consumeExperience(ModFluids.XP_PER_BOTTLE)) {
                if (!player.getAbilities().instabuild) stack.shrink(1);
                ItemStack bottle = new ItemStack(Items.EXPERIENCE_BOTTLE);
                if (!player.addItem(bottle)) Block.popResource(level, pos.above(), bottle);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ExperienceBlockEntity storage) {
            if (mode == Mode.BOTTLER && !player.isShiftKeyDown() && player instanceof ServerPlayer serverPlayer) {
                MenuHelper.open(serverPlayer, Component.translatable("container.openblocks_reborn.xp_bottler"),
                        storage.getInventory(), MachineLayout.XP_BOTTLER,
                        () -> level.getBlockEntity(pos) == storage && player.distanceToSqr(pos.getCenter()) <= 64.0D,
                        MenuHelper.data(() -> storage.getTank().getFluidAmount(),
                                () -> storage.getTank().getCapacity(), storage::getBottlingProgress, () -> 0,
                                () -> storage.getSideConfiguration(0), () -> storage.getSideConfiguration(1),
                                () -> storage.getSideConfiguration(2), () -> 0),
                        id -> handleMenuButton(storage, id));
            } else {
                player.displayClientMessage(Component.translatable("message.openblocks_reborn.xp_stored", storage.getExperience()), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public static boolean handleMenuButton(ExperienceBlockEntity storage, int id) {
        if (id >= 6_100 && id < 6_103) {
            return storage.toggleAutomatic(id - 6_100);
        }
        if (id < 6_000 || id >= 6_100) return false;
        int encoded = id - 6_000;
        int channel = encoded / 10;
        int direction = encoded % 10;
        return direction >= 0 && direction < Direction.values().length
                && storage.toggleSide(channel, Direction.from3DDataValue(direction));
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ExperienceBlockEntity storage) {
            for (int slot = 0; slot < storage.getInventory().getSlots(); slot++) {
                Block.popResource(level, pos, storage.getInventory().extractItem(slot, Integer.MAX_VALUE, false));
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (mode == Mode.DRAIN) return Block.box(0, 0, 0, 16, 1, 16);
        if (mode != Mode.SHOWER) return super.getShape(state, level, pos, context);
        return switch (state.getValue(FACING)) {
            case SOUTH -> Block.box(7, 7, 0, 9, 9, 9);
            case NORTH -> Block.box(7, 7, 7, 9, 9, 16);
            case EAST -> Block.box(0, 7, 7, 9, 9, 9);
            case WEST -> Block.box(7, 7, 7, 16, 9, 9);
            default -> Block.box(7, 7, 0, 9, 9, 9);
        };
    }
}
