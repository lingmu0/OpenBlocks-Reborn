package net.xuwu.openblocks_reborn.item;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class SleepingBagItem extends Item {
    public SleepingBagItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        if (!(user instanceof ServerPlayer player)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        BlockPos sleepPos = player.blockPosition();
        Player.BedSleepingProblem problem = findProblem(player, sleepPos);
        if (problem != null) {
            player.displayClientMessage(problem.getMessage() != null ? problem.getMessage()
                    : Component.translatable("message.openblocks_reborn.sleep_failed", problem.name()), true);
            return InteractionResultHolder.fail(stack);
        }
        var oldDimension = player.getRespawnDimension();
        var oldPosition = player.getRespawnPosition();
        float oldAngle = player.getRespawnAngle();
        boolean oldForced = player.isRespawnForced();
        player.getPersistentData().putBoolean("openblocks_reborn.sleeping_bag_active", true);
        var result = player.startSleepInBed(sleepPos);
        player.setRespawnPosition(oldDimension, oldPosition, oldAngle, oldForced, false);
        result.ifLeft(failure -> {
            player.getPersistentData().remove("openblocks_reborn.sleeping_bag_active");
            player.displayClientMessage(failure.getMessage() != null ? failure.getMessage()
                    : Component.translatable("message.openblocks_reborn.sleep_failed", failure.name()), true);
        });
        if (result.right().isPresent()) {
            stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(player.getEquipmentSlotForItem(stack)));
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.fail(stack);
    }

    private static Player.BedSleepingProblem findProblem(ServerPlayer player, BlockPos pos) {
        if (player.isSleeping() || !player.isAlive()) return Player.BedSleepingProblem.OTHER_PROBLEM;
        if (!player.level().dimensionType().natural()) return Player.BedSleepingProblem.NOT_POSSIBLE_HERE;
        if (player.level().isDay()) return Player.BedSleepingProblem.NOT_POSSIBLE_NOW;
        BlockPos floor = pos.below();
        if (!player.level().getBlockState(floor).isFaceSturdy(
                player.level(), floor, net.minecraft.core.Direction.UP)) {
            return Player.BedSleepingProblem.OBSTRUCTED;
        }
        if (!player.level().getBlockState(pos).getCollisionShape(player.level(), pos).isEmpty()) {
            return Player.BedSleepingProblem.OBSTRUCTED;
        }
        if (!player.isCreative()) {
            Vec3 center = Vec3.atBottomCenterOf(pos);
            AABB search = new AABB(center.x - 8.0D, center.y - 5.0D, center.z - 8.0D,
                    center.x + 8.0D, center.y + 5.0D, center.z + 8.0D);
            if (!player.level().getEntitiesOfClass(Monster.class, search,
                    monster -> monster.isPreventingPlayerRest(player)).isEmpty()) {
                return Player.BedSleepingProblem.NOT_SAFE;
            }
        }
        return null;
    }
}
