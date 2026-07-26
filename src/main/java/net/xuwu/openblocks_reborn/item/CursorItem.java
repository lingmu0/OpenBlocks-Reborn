package net.xuwu.openblocks_reborn.item;

import net.xuwu.openblocks_reborn.util.LegacyItemData;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class CursorItem extends Item {
    private static final String DIMENSION = "CursorDimension";
    private static final String POSITION = "CursorPosition";
    private static final String SIDE = "CursorSide";
    private static final double MAX_DISTANCE = 1_024.0D;
    private static final int CROSS_DIMENSION_COST = 30;
    private static final Map<ServerPlayer, RemoteMenu> REMOTE_MENUS =
            Collections.synchronizedMap(new WeakHashMap<>());

    public CursorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (player.isShiftKeyDown()) {
            if (!context.getLevel().isClientSide) {
                bind(context.getItemInHand(), context.getLevel(), context.getClickedPos(), context.getClickedFace());
                player.displayClientMessage(Component.translatable("message.openblocks_reborn.cursor_bound",
                        context.getClickedPos().getX(), context.getClickedPos().getY(), context.getClickedPos().getZ()), true);
            }
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }
        return activate(context.getLevel(), player, context.getHand(), context.getItemInHand()).getResult();
    }

    public static void bind(ItemStack stack, Level level, BlockPos pos, Direction side) {
        LegacyItemData.update(stack, tag -> {
            tag.putString(DIMENSION, level.dimension().location().toString());
            tag.putLong(POSITION, pos.asLong());
            tag.putString(SIDE, side.getName());
        });
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(stack);
        return activate(level, player, hand, stack);
    }

    private InteractionResultHolder<ItemStack> activate(Level level, Player player,
                                                        InteractionHand hand, ItemStack stack) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        var tag = LegacyItemData.copyTag(stack);
        ResourceLocation dimension = ResourceLocation.tryParse(tag.getString(DIMENSION));
        if (dimension == null || !tag.contains(POSITION)) {
            serverPlayer.displayClientMessage(Component.translatable("message.openblocks_reborn.cursor_unbound"), true);
            return InteractionResultHolder.fail(stack);
        }
        ServerLevel targetLevel = serverPlayer.server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
        if (targetLevel == null) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.openblocks_reborn.cursor_dimension_missing"), true);
            return InteractionResultHolder.fail(stack);
        }
        BlockPos pos = BlockPos.of(tag.getLong(POSITION));
        boolean crossDimension = targetLevel != level;
        double distance = Math.sqrt(player.distanceToSqr(pos.getCenter()));
        if (distance > MAX_DISTANCE) {
            serverPlayer.displayClientMessage(Component.translatable("message.openblocks_reborn.cursor_out_of_range"), true);
            return InteractionResultHolder.fail(stack);
        }
        if (!targetLevel.hasChunkAt(pos)) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.openblocks_reborn.cursor_target_unloaded"), true);
            return InteractionResultHolder.fail(stack);
        }
        if (targetLevel.getBlockState(pos).isAir()) {
            serverPlayer.displayClientMessage(Component.translatable("message.openblocks_reborn.cursor_out_of_range"), true);
            return InteractionResultHolder.fail(stack);
        }
        int cost = player.getAbilities().instabuild ? 0
                : Math.max(1, Mth.ceil(distance / 8.0D)) + (crossDimension ? CROSS_DIMENSION_COST : 0);
        if (currentExperiencePoints(player) < cost) {
            serverPlayer.displayClientMessage(Component.translatable("message.openblocks_reborn.cursor_need_xp", cost), true);
            return InteractionResultHolder.fail(stack);
        }
        Direction side = Direction.byName(tag.getString(SIDE));
        if (side == null) side = Direction.UP;
        BlockHitResult hit = new BlockHitResult(pos.getCenter(), side, pos, false);
        var state = targetLevel.getBlockState(pos);
        var previousMenu = serverPlayer.containerMenu;
        InteractionResult result = state.use(targetLevel, player, hand, hit);
        boolean openedMenu = serverPlayer.containerMenu != previousMenu
                && serverPlayer.containerMenu != serverPlayer.inventoryMenu;
        if (openedMenu) {
            REMOTE_MENUS.put(serverPlayer, new RemoteMenu(serverPlayer.containerMenu.containerId,
                    targetLevel.dimension(), pos, state.getBlock()));
            result = InteractionResult.SUCCESS;
        }
        if (result.consumesAction() && cost > 0) player.giveExperiencePoints(-cost);
        player.getCooldowns().addCooldown(this, 5);
        return new InteractionResultHolder<>(result, stack);
    }

    public static boolean isRemoteMenuValid(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return false;
        RemoteMenu remote = REMOTE_MENUS.get(serverPlayer);
        if (remote == null || serverPlayer.containerMenu.containerId != remote.containerId()) return false;
        ServerLevel targetLevel = serverPlayer.server.getLevel(remote.dimension());
        if (targetLevel == null) {
            REMOTE_MENUS.remove(serverPlayer);
            return false;
        }
        if (!targetLevel.hasChunkAt(remote.position())) {
            REMOTE_MENUS.remove(serverPlayer);
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.openblocks_reborn.cursor_target_unloaded"), true);
            return false;
        }
        if (!targetLevel.getBlockState(remote.position()).is(remote.block())) {
            REMOTE_MENUS.remove(serverPlayer);
            return false;
        }
        return true;
    }

    public static void clearRemoteMenu(Player player) {
        if (player instanceof ServerPlayer serverPlayer) REMOTE_MENUS.remove(serverPlayer);
    }

    private static int currentExperiencePoints(Player player) {
        int level = Math.max(0, player.experienceLevel);
        return Math.max(0, net.xuwu.openblocks_reborn.registry.ModFluids.experienceForLevel(level)
                + Mth.floor(Mth.clamp(player.experienceProgress, 0.0F, 1.0F)
                * player.getXpNeededForNextLevel()));
    }

    @Override
    public void appendHoverText(ItemStack stack, @javax.annotation.Nullable net.minecraft.world.level.Level level, List<Component> tooltip, TooltipFlag flag) {
        var tag = LegacyItemData.copyTag(stack);
        if (tag.contains(POSITION)) {
            BlockPos pos = BlockPos.of(tag.getLong(POSITION));
            tooltip.add(Component.translatable("tooltip.openblocks_reborn.cursor_bound", pos.getX(), pos.getY(), pos.getZ()));
            tooltip.add(Component.literal(tag.getString(DIMENSION)));
        } else {
            tooltip.add(Component.translatable("tooltip.openblocks_reborn.cursor_unbound"));
        }
    }

    private record RemoteMenu(int containerId, ResourceKey<Level> dimension,
                              BlockPos position, net.minecraft.world.level.block.Block block) {
    }
}
