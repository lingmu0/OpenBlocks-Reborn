package net.xuwu.openblocks_reborn.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.xuwu.openblocks_reborn.entity.LuggageEntity;
import net.xuwu.openblocks_reborn.registry.ModEntities;

public class LuggageItem extends Item {
    public LuggageItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            LuggageEntity luggage = new LuggageEntity(ModEntities.LUGGAGE.get(), level);
            Vec3 spawn = player.position().add(player.getLookAngle().scale(1.5));
            luggage.setPos(spawn.x, player.getY() + 0.2, spawn.z);
            luggage.setOwner(player.getUUID());
            luggage.restoreFromItem(stack);
            if (level.addFreshEntity(luggage) && !player.getAbilities().instabuild) stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
