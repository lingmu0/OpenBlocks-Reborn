package net.xuwu.openblocks_reborn.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.xuwu.openblocks_reborn.registry.ModItems;

public class CartographerItem extends Item {
    public CartographerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        InteractionHand other = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack paper = player.getItemInHand(other);
        if (!paper.is(ModItems.EMPTY_MAP.get())) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable("message.openblocks_reborn.cartographer_need_map"), true);
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        }
        if (!level.isClientSide) {
            ItemStack map = HeightMapItem.create(level, player.blockPosition());
            if (!player.getAbilities().instabuild) paper.shrink(1);
            if (!player.addItem(map)) player.drop(map, false);
            player.getCooldowns().addCooldown(this, 20);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
}
