package net.xuwu.openblocks_reborn.item;

import net.xuwu.openblocks_reborn.util.LegacyItemData;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class MagnetItem extends Item {
    private static final String ENABLED = "MagnetEnabled";

    public MagnetItem(Properties properties) {
        super(properties);
    }

    public static boolean isEnabled(ItemStack stack) {
        var data = LegacyItemData.copyTag(stack);
        return !data.contains(ENABLED) || data.getBoolean(ENABLED);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean enabled = !isEnabled(stack);
        if (!level.isClientSide) {
            LegacyItemData.update(stack, tag -> tag.putBoolean(ENABLED, enabled));
            player.displayClientMessage(Component.translatable(enabled
                    ? "message.openblocks_reborn.magnet_on" : "message.openblocks_reborn.magnet_off"), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide || !(entity instanceof Player player) || !isEnabled(stack) || player.tickCount % 2 != 0) return;
        Vec3 target = player.position().add(0.0, 0.8, 0.0);
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(8.0), ItemEntity::isAlive)) {
            pull(item, target);
        }
        for (ExperienceOrb orb : level.getEntitiesOfClass(ExperienceOrb.class, player.getBoundingBox().inflate(8.0), ExperienceOrb::isAlive)) {
            pull(orb, target);
        }
    }

    private static void pull(Entity entity, Vec3 target) {
        Vec3 delta = target.subtract(entity.position());
        if (delta.lengthSqr() > 0.04) entity.setDeltaMovement(entity.getDeltaMovement().scale(0.5).add(delta.normalize().scale(0.22)));
    }
}
