package net.xuwu.openblocks_reborn.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class DonationStationBlock extends Block {
    public DonationStationBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
            player.displayClientMessage(Component.translatable("message.openblocks_reborn.donation", stack.getHoverName(), key.getNamespace()), false);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
}
