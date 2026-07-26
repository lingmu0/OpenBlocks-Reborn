package net.xuwu.openblocks_reborn.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

public class SlimalyzerItem extends Item {
    private static final String TAG_ACTIVE = "Active";

    public SlimalyzerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel) {
            int x = player.chunkPosition().x;
            int z = player.chunkPosition().z;
            long seed = serverLevel.getSeed() + (long)(x * x * 4_987_142) + (long)(x * 5_947_611)
                    + (long)(z * z) * 4_392_871L + (long)(z * 389_711) ^ 987_234_911L;
            boolean slimeChunk = RandomSource.create(seed).nextInt(10) == 0;
            setActive(stack, slimeChunk);
            player.displayClientMessage(Component.translatable(slimeChunk
                    ? "message.openblocks_reborn.slime_chunk"
                    : "message.openblocks_reborn.not_slime_chunk"), true);
            stack.hurtAndBreak(1, player, player.getEquipmentSlotForItem(stack));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    public static boolean isActive(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.getBoolean(TAG_ACTIVE);
    }

    public static void setActive(ItemStack stack, boolean active) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(TAG_ACTIVE, active));
    }
}
