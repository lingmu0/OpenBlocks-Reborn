package net.xuwu.openblocks_reborn.item;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Map;
import java.util.WeakHashMap;

public class CraneControlItem extends Item {
    private static final Map<LivingEntity, Long> LAST_SWING = new WeakHashMap<>();
    public CraneControlItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return new InteractionResultHolder<>(beginUsing(level, player, hand),
                player.getItemInHand(hand));
    }

    public static InteractionResult beginUsing(Level level, Player player, InteractionHand hand) {
        if (CraneBackpackItem.wornBy(player).isEmpty()) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable(
                        "message.openblocks_reborn.crane_requires_backpack"), true);
            }
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    /**
     * Runs before the target block's own use method. Consuming here keeps the
     * controller's right click assigned exclusively to cable movement instead of
     * also opening containers, pressing buttons or operating machines.
     */
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        return player == null ? InteractionResult.FAIL
                : beginUsing(context.getLevel(), player, context.getHand());
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player,
                                                   LivingEntity target, InteractionHand hand) {
        return beginUsing(player.level(), player, hand);
    }

    @Override
    public void onUseTick(Level level, LivingEntity living, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide || !(living instanceof Player player)) return;
        ItemStack backpack = CraneBackpackItem.wornBy(player);
        if (backpack.isEmpty()) {
            player.stopUsingItem();
            return;
        }
        CraneBackpackItem.changeLength(backpack, player.isShiftKeyDown() ? 0.1D : -0.1D);
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        if (entity instanceof Player player && !entity.level().isClientSide) {
            long gameTime = entity.level().getGameTime();
            Long previous = LAST_SWING.get(entity);
            if (previous != null && gameTime - previous <= 5) return true;
            LAST_SWING.put(entity, gameTime);
            boolean changed = CraneBackpackItem.toggleMagnet(player);
            player.displayClientMessage(Component.translatable(changed
                    ? "message.openblocks_reborn.crane_magnet_toggled"
                    : "message.openblocks_reborn.crane_no_target"), true);
        }
        return true;
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        return true;
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return false;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72_000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }
}
