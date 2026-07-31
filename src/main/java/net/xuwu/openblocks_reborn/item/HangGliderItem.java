package net.xuwu.openblocks_reborn.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.xuwu.openblocks_reborn.entity.HangGliderEntity;
import net.xuwu.openblocks_reborn.registry.ModEntities;

public class HangGliderItem extends Item {
    private static final String DEPLOYED = "Deployed";

    public HangGliderItem(Properties properties) {
        super(properties);
    }

    public static boolean isDeployed(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean(DEPLOYED);
    }

    public static void setDeployed(ItemStack stack, boolean deployed) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(DEPLOYED, deployed));
    }

    public static boolean isActivelyGliding(Player player) {
        return !player.onGround() && !player.isInWater() && !player.isSleeping()
                && !player.isFallFlying()
                && ((player.getMainHandItem().getItem() instanceof HangGliderItem
                        && isDeployed(player.getMainHandItem()))
                    || (player.getOffhandItem().getItem() instanceof HangGliderItem
                        && isDeployed(player.getOffhandItem())));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean deployed = !isDeployed(stack);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            removeGliders(serverPlayer);
            setDeployed(stack, deployed);
            if (deployed) {
                HangGliderEntity glider = new HangGliderEntity(ModEntities.HANG_GLIDER.get(), level);
                glider.attachTo(serverPlayer, hand);
                level.addFreshEntity(glider);
            }
            level.playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_ELYTRA.value(),
                    SoundSource.PLAYERS, 0.6F, deployed ? 1.15F : 0.85F);
            player.displayClientMessage(Component.translatable(deployed
                    ? "message.openblocks_reborn.glider.deployed"
                    : "message.openblocks_reborn.glider.retracted"), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    public static void ensureGlider(ServerPlayer player) {
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        InteractionHand hand = isDeployed(main) && main.getItem() instanceof HangGliderItem
                ? InteractionHand.MAIN_HAND
                : (isDeployed(off) && off.getItem() instanceof HangGliderItem ? InteractionHand.OFF_HAND : null);
        if (hand == null) return;
        AABB search = player.getBoundingBox().inflate(4.0D);
        boolean present = player.level().getEntitiesOfClass(HangGliderEntity.class, search,
                glider -> glider.isAttachedTo(player)).stream().findAny().isPresent();
        if (!present) {
            HangGliderEntity glider = new HangGliderEntity(ModEntities.HANG_GLIDER.get(), player.level());
            glider.attachTo(player, hand);
            player.level().addFreshEntity(glider);
        }
    }

    private static void removeGliders(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        level.getEntitiesOfClass(HangGliderEntity.class, player.getBoundingBox().inflate(8.0D),
                glider -> glider.isAttachedTo(player)).forEach(HangGliderEntity::discard);
    }
}
