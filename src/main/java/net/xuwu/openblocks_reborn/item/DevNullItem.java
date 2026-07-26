package net.xuwu.openblocks_reborn.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.xuwu.openblocks_reborn.menu.MenuHelper;
import net.xuwu.openblocks_reborn.menu.MachineLayout;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;

import java.util.List;

public class DevNullItem extends Item {
    public DevNullItem(Properties properties) {
        super(properties);
    }

    public static ItemStack getStored(ItemStack container) {
        return container.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyOne();
    }

    public static void setStored(ItemStack container, ItemStack stored) {
        if (stored.isEmpty()) container.remove(DataComponents.CONTAINER);
        else container.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(stored)));
    }

    /** Absorbs matching ground items and deliberately voids overflow, matching dev/null's purpose. */
    public boolean absorb(ItemStack container, ItemStack incoming) {
        ItemStack stored = getStored(container);
        if (stored.isEmpty() || !ItemStack.isSameItemSameComponents(stored, incoming)) return false;
        stored.grow(Math.min(incoming.getCount(), stored.getMaxStackSize() - stored.getCount()));
        setStored(container, stored);
        incoming.setCount(0);
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack container = player.getItemInHand(hand);
        ItemStack stored = getStored(container);
        if (!level.isClientSide) {
            if (player.isShiftKeyDown()) {
                if (!stored.isEmpty()) {
                    setStored(container, ItemStack.EMPTY);
                    if (!player.addItem(stored)) player.drop(stored, false);
                }
            } else if (stored.isEmpty()) {
                InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
                ItemStack source = player.getItemInHand(otherHand);
                if (!source.isEmpty() && source.getItem() != this) {
                    ItemStack filter = source.copyWithCount(Math.min(source.getCount(), source.getMaxStackSize()));
                    setStored(container, filter);
                    if (!player.getAbilities().instabuild) source.shrink(filter.getCount());
                    stored = filter;
                }
            }
            if (player instanceof ServerPlayer serverPlayer) {
                MenuHelper.open(serverPlayer, Component.translatable("container.openblocks_reborn.dev_null"),
                        new DevNullItemHandler(container), MachineLayout.DEV_NULL,
                        () -> player.getItemInHand(hand) == container);
            }
        }
        return InteractionResultHolder.sidedSuccess(container, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ItemStack stored = getStored(stack);
        if (stored.isEmpty()) tooltip.add(Component.translatable("tooltip.openblocks_reborn.dev_null_empty"));
        else tooltip.add(Component.translatable("tooltip.openblocks_reborn.dev_null_stored", stored.getCount(), stored.getHoverName()));
    }
}
