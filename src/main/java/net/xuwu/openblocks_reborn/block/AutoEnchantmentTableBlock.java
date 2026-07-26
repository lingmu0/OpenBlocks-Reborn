package net.xuwu.openblocks_reborn.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Opens the vanilla enchanting UI while keeping the OpenBlocks registry ID. */
public class AutoEnchantmentTableBlock extends Block {
    public AutoEnchantmentTableBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    private void open(Level level, BlockPos pos, Player player) {
        if (!level.isClientSide) {
            player.openMenu(new SimpleMenuProvider(
                    (containerId, inventory, ignored) -> new EnchantmentMenu(
                            containerId, inventory, ContainerLevelAccess.create(level, pos)),
                    Component.translatable("container.openblocks_reborn.auto_enchantment_table")));
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        InteractionResult held = useHeldItem(player.getItemInHand(hand), state, level, pos, player, hand, hit);
        if (held != InteractionResult.PASS) return held;
        open(level, pos, player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public InteractionResult useHeldItem(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
        open(level, pos, player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
