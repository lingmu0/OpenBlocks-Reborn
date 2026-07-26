package net.xuwu.openblocks_reborn.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.xuwu.openblocks_reborn.blockentity.GoldenEggBlockEntity;
import net.xuwu.openblocks_reborn.registry.ModBlockEntities;

public class GoldenEggBlock extends Block implements EntityBlock {
    private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 16, 12);

    public GoldenEggBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GoldenEggBlockEntity(pos, state);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!level.isClientSide && type == ModBlockEntities.GOLDEN_EGG.get()) {
            return (tickerLevel, pos, tickerState, blockEntity) -> GoldenEggBlockEntity.serverTick(
                    (ServerLevel)tickerLevel, pos, tickerState, (GoldenEggBlockEntity)blockEntity);
        }
        return null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof GoldenEggBlockEntity egg) {
            egg.setOwner(player.getUUID(), player.getGameProfile().getName());
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof GoldenEggBlockEntity egg) {
            player.displayClientMessage(Component.translatable("message.openblocks_reborn.golden_egg_progress",
                    egg.getProgress() * 100 / GoldenEggBlockEntity.HATCH_TIME), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
