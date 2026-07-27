package net.xuwu.openblocks_reborn.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
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
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        if (horizontal.lengthSqr() < 1.0E-4D) {
            horizontal = Vec3.directionFromRotation(0.0F, player.getYRot());
        }
        Vec3 spawn = player.position().add(horizontal.normalize().scale(1.5D)).add(0.0D, 0.2D, 0.0D);
        boolean placed = level.isClientSide || place(level, player, stack, spawn);
        return placed
                ? InteractionResultHolder.sidedSuccess(stack, level.isClientSide)
                : InteractionResultHolder.fail(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        BlockPos target = context.getClickedPos().relative(context.getClickedFace());
        Vec3 spawn = Vec3.atBottomCenterOf(target);
        if (context.getLevel().isClientSide) return InteractionResult.SUCCESS;
        return place(context.getLevel(), player, context.getItemInHand(), spawn)
                ? InteractionResult.CONSUME : InteractionResult.FAIL;
    }

    private static boolean place(Level level, Player player, ItemStack stack, Vec3 spawn) {
        LuggageEntity luggage = new LuggageEntity(ModEntities.LUGGAGE.get(), level);
        luggage.moveTo(spawn.x, spawn.y, spawn.z, player.getYRot(), 0.0F);
        if (!level.noCollision(luggage, luggage.getBoundingBox())) return false;
        luggage.setOwner(player.getUUID());
        luggage.restoreFromItem(stack);
        if (!level.addFreshEntity(luggage)) return false;
        if (!player.getAbilities().instabuild) stack.shrink(1);
        return true;
    }
}
