package net.xuwu.openblocks_reborn.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.xuwu.openblocks_reborn.blockentity.BigButtonBlockEntity;
import net.xuwu.openblocks_reborn.menu.MenuHelper;
import net.xuwu.openblocks_reborn.menu.MachineLayout;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BigButtonBlock extends ButtonBlock implements EntityBlock {
    public BigButtonBlock(BlockSetType type, BlockBehaviour.Properties properties) {
        super(type, 512, properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BigButtonBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        boolean pressed = state.getValue(POWERED);
        int depth = pressed ? 1 : 2;
        if (state.getValue(FACE) == AttachFace.FLOOR) return Block.box(1, 0, 1, 15, depth, 15);
        if (state.getValue(FACE) == AttachFace.CEILING) return Block.box(1, 16 - depth, 1, 15, 16, 15);
        return switch (state.getValue(FACING)) {
            case SOUTH -> Block.box(1, 1, 0, 15, 15, depth);
            case EAST -> Block.box(0, 1, 1, depth, 15, 15);
            case WEST -> Block.box(16 - depth, 1, 1, 16, 15, 15);
            default -> Block.box(1, 1, 16 - depth, 15, 15, 16);
        };
    }

    private InteractionResult open(Level level, BlockPos pos, Player player) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof BigButtonBlockEntity button) {
            MenuHelper.open(serverPlayer, Component.translatable("container.openblocks_reborn.big_button",
                            button.getPressTicks(), button.getPressTicks() / 20.0F), button.getInventory(),
                    MachineLayout.BIG_BUTTON,
                    () -> level.getBlockEntity(pos) == button && player.distanceToSqr(pos.getCenter()) <= 64.0D,
                    MenuHelper.data(() -> 0, () -> 0, button::getPressTicks, () -> 0));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return open(level, pos, player);
        if (!state.getValue(POWERED)) press(state, level, pos, player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isShiftKeyDown()) {
            open(level, pos, player);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public void press(BlockState state, Level level, BlockPos pos, Player player) {
        level.setBlock(pos, state.setValue(POWERED, true), Block.UPDATE_ALL);
        level.updateNeighborsAt(pos, this);
        level.updateNeighborsAt(pos.relative(getConnectedDirection(state).getOpposite()), this);
        int ticks = level.getBlockEntity(pos) instanceof BigButtonBlockEntity button ? button.getPressTicks() : 1;
        level.scheduleTick(pos, this, ticks);
        playSound(player, level, pos, true);
        level.gameEvent(player, GameEvent.BLOCK_ACTIVATE, pos);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(POWERED)) return;
        level.setBlock(pos, state.setValue(POWERED, false), Block.UPDATE_ALL);
        level.updateNeighborsAt(pos, this);
        level.updateNeighborsAt(pos.relative(getConnectedDirection(state).getOpposite()), this);
        playSound(null, level, pos, false);
        level.gameEvent(null, GameEvent.BLOCK_DEACTIVATE, pos);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof BigButtonBlockEntity button) {
            for (int slot = 0; slot < button.getInventory().getSlots(); slot++) {
                Block.popResource(level, pos, button.getInventory().extractItem(slot, Integer.MAX_VALUE, false));
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
