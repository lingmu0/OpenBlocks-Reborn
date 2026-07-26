package net.xuwu.openblocks_reborn.item;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.xuwu.openblocks_reborn.block.ColorableBlock;
import net.xuwu.openblocks_reborn.registry.ModBlocks;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;

public class EpicEraserItem extends Item {
    public EpicEraserItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack eraser = player.getItemInHand(hand);
        InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack target = player.getItemInHand(otherHand);
        if (target.has(DataComponents.LORE)) {
            if (!level.isClientSide) {
                target.remove(DataComponents.LORE);
                eraser.hurtAndBreak(1, player, player.getEquipmentSlotForItem(eraser));
                player.displayClientMessage(Component.translatable("message.openblocks_reborn.epic_eraser_cleaned"), true);
            }
            return InteractionResultHolder.sidedSuccess(eraser, level.isClientSide);
        }
        return InteractionResultHolder.pass(eraser);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockState state = context.getLevel().getBlockState(context.getClickedPos());
        if (!state.is(ModBlocks.IMAGINARY.get()) && !state.hasProperty(ColorableBlock.COLOR)) return InteractionResult.PASS;
        if (!context.getLevel().isClientSide) {
            if (state.is(ModBlocks.IMAGINARY.get())) context.getLevel().removeBlock(context.getClickedPos(), false);
            else context.getLevel().setBlock(context.getClickedPos(), state.setValue(ColorableBlock.COLOR, DyeColor.WHITE), Block.UPDATE_ALL);
            if (context.getPlayer() != null) context.getItemInHand().hurtAndBreak(1, context.getPlayer(),
                    context.getPlayer().getEquipmentSlotForItem(context.getItemInHand()));
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }
}
