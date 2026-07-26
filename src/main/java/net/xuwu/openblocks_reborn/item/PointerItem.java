package net.xuwu.openblocks_reborn.item;

import net.xuwu.openblocks_reborn.util.LegacyItemData;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.xuwu.openblocks_reborn.blockentity.ItemMachineBlockEntity;
import net.xuwu.openblocks_reborn.registry.ModBlocks;

import java.util.List;

/** Links an item cannon to a target block, matching the two-click legacy pointer workflow. */
public class PointerItem extends Item {
    private static final String DIMENSION = "PointerDimension";
    private static final String CANNON = "PointerCannon";

    public PointerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        ItemStack stack = context.getItemInHand();
        BlockPos clicked = context.getClickedPos();
        if (level.getBlockState(clicked).is(ModBlocks.CANNON.get())) {
            if (!level.isClientSide) {
                bind(stack, level.dimension().location(), clicked);
                if (context.getPlayer() != null) context.getPlayer().displayClientMessage(
                        Component.translatable("message.openblocks_reborn.pointer_cannon",
                                clicked.getX(), clicked.getY(), clicked.getZ()), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        CompoundTagView binding = getBinding(stack);
        if (binding == null || !binding.dimension.equals(level.dimension().location())
                || !level.hasChunkAt(binding.position)
                || !(level.getBlockEntity(binding.position) instanceof ItemMachineBlockEntity cannon)
                || !level.getBlockState(binding.position).is(ModBlocks.CANNON.get())) {
            if (!level.isClientSide && context.getPlayer() != null) context.getPlayer().displayClientMessage(
                    Component.translatable("message.openblocks_reborn.pointer_unbound"), true);
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide) {
            cannon.setCannonTarget(clicked);
            if (context.getPlayer() != null) context.getPlayer().displayClientMessage(
                    Component.translatable("message.openblocks_reborn.pointer_target",
                            clicked.getX(), clicked.getY(), clicked.getZ()), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public static void bind(ItemStack stack, ResourceLocation dimension, BlockPos pos) {
        LegacyItemData.update(stack, tag -> {
            tag.putString(DIMENSION, dimension.toString());
            tag.putLong(CANNON, pos.asLong());
        });
    }

    private static CompoundTagView getBinding(ItemStack stack) {
        var tag = LegacyItemData.copyTag(stack);
        ResourceLocation dimension = ResourceLocation.tryParse(tag.getString(DIMENSION));
        return dimension == null || !tag.contains(CANNON) ? null
                : new CompoundTagView(dimension, BlockPos.of(tag.getLong(CANNON)));
    }

    @Override
    public void appendHoverText(ItemStack stack, @javax.annotation.Nullable net.minecraft.world.level.Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTagView binding = getBinding(stack);
        if (binding == null) {
            tooltip.add(Component.translatable("tooltip.openblocks_reborn.pointer_unbound").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.openblocks_reborn.pointer_bound",
                    binding.position.getX(), binding.position.getY(), binding.position.getZ()).withStyle(ChatFormatting.GRAY));
        }
    }

    private record CompoundTagView(ResourceLocation dimension, BlockPos position) { }
}
