package net.xuwu.openblocks_reborn.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class RepairBlock extends Block {
    public RepairBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        return useHeldItem(player.getItemInHand(hand), state, level, pos, player, hand, hit);
    }

    public InteractionResult useHeldItem(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.isDamaged() || player.totalExperience <= 0) return InteractionResult.PASS;
        if (!level.isClientSide) {
            int repaired = Math.min(stack.getDamageValue(), Math.min(50, player.totalExperience * 2));
            int cost = (repaired + 1) / 2;
            stack.setDamageValue(stack.getDamageValue() - repaired);
            if (!player.getAbilities().instabuild) player.giveExperiencePoints(-cost);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
