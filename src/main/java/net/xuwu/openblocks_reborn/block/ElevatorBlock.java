package net.xuwu.openblocks_reborn.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.xuwu.openblocks_reborn.item.PaintBrushItem;
import java.util.List;

public class ElevatorBlock extends Block {
    public static final EnumProperty<DyeColor> COLOR = EnumProperty.create("color", DyeColor.class);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private final boolean rotatesPlayer;

    public ElevatorBlock(BlockBehaviour.Properties properties, boolean rotatesPlayer) {
        super(properties);
        this.rotatesPlayer = rotatesPlayer;
        registerDefaultState(stateDefinition.any().setValue(COLOR, DyeColor.WHITE).setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(COLOR, FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(COLOR, PaintBrushItem.getColor(context.getItemInHand()))
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        return useHeldItem(player.getItemInHand(hand), state, level, pos, player, hand, hit);
    }

    public InteractionResult useHeldItem(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof DyeItem dyeItem) {
            DyeColor color = dyeItem.getDyeColor();
            if (state.getValue(COLOR) != color) {
                if (!level.isClientSide) {
                    level.setBlock(pos, state.setValue(COLOR, color), Block.UPDATE_ALL);
                    if (!player.getAbilities().instabuild) stack.shrink(1);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return InteractionResult.PASS;
    }

    public boolean teleport(ServerPlayer player, boolean up) {
        if (!(player.level() instanceof ServerLevel level)) return false;
        BlockPos origin = player.blockPosition().below();
        BlockState originState = level.getBlockState(origin);
        if (originState.getBlock() != this) return false;

        DyeColor color = originState.getValue(COLOR);
        int direction = up ? 1 : -1;
        for (int distance = 2; distance <= 64; distance++) {
            BlockPos destination = origin.offset(0, direction * distance, 0);
            BlockState destinationState = level.getBlockState(destination);
            if (destinationState.getBlock() instanceof ElevatorBlock destinationElevator
                    && destinationState.getValue(COLOR) == color) {
                if (!level.getBlockState(destination.above()).isAir() || !level.getBlockState(destination.above(2)).isAir()) {
                    continue;
                }
                float yaw = destinationElevator.rotatesPlayer
                        ? Mth.wrapDegrees(destinationState.getValue(FACING).toYRot() + 180.0F)
                        : player.getYRot();
                // Send position and rotation in the same teleport packet. Calling the
                // position-only overload and then setYRot left the client facing the old way.
                player.teleportTo(level, destination.getX() + 0.5D, destination.getY() + 1.0D,
                        destination.getZ() + 0.5D, yaw, player.getXRot());
                player.setYRot(yaw);
                player.setYHeadRot(yaw);
                player.setDeltaMovement(0.0D, 0.0D, 0.0D);
                player.fallDistance = 0.0F;
                return true;
            }
        }
        return false;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        ItemStack result = new ItemStack(asItem());
        PaintBrushItem.setColor(result, state.getValue(COLOR));
        return List.of(result);
    }
}
