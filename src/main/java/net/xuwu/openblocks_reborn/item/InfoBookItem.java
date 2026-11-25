package net.xuwu.openblocks_reborn.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.xuwu.openblocks_reborn.compat.PatchouliCompat;

/** Retains the existing guide item ID while opening the Patchouli manual. */
public class InfoBookItem extends Item {
    public InfoBookItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) PatchouliCompat.openGuide(serverPlayer);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
