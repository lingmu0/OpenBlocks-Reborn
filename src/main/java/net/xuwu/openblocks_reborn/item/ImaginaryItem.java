package net.xuwu.openblocks_reborn.item;

import net.xuwu.openblocks_reborn.util.LegacyItemData;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.xuwu.openblocks_reborn.block.ImaginaryBlock;
import net.xuwu.openblocks_reborn.blockentity.ImaginaryBlockEntity;

import javax.annotation.Nullable;
import java.util.List;

public class ImaginaryItem extends BlockItem {
    public static final float DEFAULT_USES = 10.0F;
    private static final String USES = "ImaginaryUses";
    private static final String COLOR = "ImaginaryColor";
    private static final String MODE = "ImaginaryMode";

    public enum PlacementMode {
        BLOCK(1.0F, ImaginaryBlock.Shape.BLOCK, false),
        PANEL(0.5F, ImaginaryBlock.Shape.PANEL, false),
        HALF_PANEL(0.5F, ImaginaryBlock.Shape.HALF, false),
        STAIRS(0.75F, ImaginaryBlock.Shape.STAIRS, false),
        INVERTED_BLOCK(1.5F, ImaginaryBlock.Shape.BLOCK, true),
        INVERTED_PANEL(1.0F, ImaginaryBlock.Shape.PANEL, true),
        INVERTED_HALF_PANEL(1.0F, ImaginaryBlock.Shape.HALF, true),
        INVERTED_STAIRS(1.25F, ImaginaryBlock.Shape.STAIRS, true);

        private final float cost;
        private final ImaginaryBlock.Shape shape;
        private final boolean inverted;

        PlacementMode(float cost, ImaginaryBlock.Shape shape, boolean inverted) {
            this.cost = cost;
            this.shape = shape;
            this.inverted = inverted;
        }
    }

    public ImaginaryItem(Block block, Properties properties) {
        super(block, properties.stacksTo(1));
    }

    public static float getUses(ItemStack stack) {
        var tag = LegacyItemData.copyTag(stack);
        return tag.contains(USES) ? Math.max(0.0F, tag.getFloat(USES)) : DEFAULT_USES;
    }

    public static boolean isCrayon(ItemStack stack) {
        return LegacyItemData.copyTag(stack).contains(COLOR);
    }

    public static int getColor(ItemStack stack) {
        return LegacyItemData.copyTag(stack).getInt(COLOR) & 0xFFFFFF;
    }

    public static ItemStack createCrayon(int color) {
        return createConfigured(color, 0, DEFAULT_USES);
    }

    public static ItemStack createConfigured(@Nullable Integer color, int mode, float uses) {
        ItemStack result = new ItemStack(net.xuwu.openblocks_reborn.registry.ModBlocks.IMAGINARY.get().asItem());
        LegacyItemData.update(result, tag -> {
            tag.putFloat(USES, Math.max(0.0F, uses));
            if (color != null) tag.putInt(COLOR, color & 0xFFFFFF);
            tag.putInt(MODE, Math.floorMod(mode, PlacementMode.values().length));
        });
        return result;
    }

    public static int getModeIndex(ItemStack stack) {
        int mode = LegacyItemData.copyTag(stack).getInt(MODE);
        return Math.floorMod(mode, PlacementMode.values().length);
    }

    private static PlacementMode getMode(ItemStack stack) {
        return PlacementMode.values()[getModeIndex(stack)];
    }

    @Override
    protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        return state == null ? null : state.setValue(ImaginaryBlock.SHAPE, getMode(context.getItemInHand()).shape);
    }

    @Override
    protected boolean updateCustomBlockEntityTag(BlockPos pos, Level level, @Nullable Player player,
                                                 ItemStack stack, BlockState state) {
        if (level.getBlockEntity(pos) instanceof ImaginaryBlockEntity imaginary) {
            PlacementMode mode = getMode(stack);
            imaginary.configure(isCrayon(stack) ? getColor(stack) : null, mode.inverted);
        }
        return true;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        PlacementMode mode = getMode(stack);
        if (getUses(stack) < mode.cost) return InteractionResult.FAIL;
        InteractionResult result = super.useOn(context);
        Player player = context.getPlayer();
        if (result.consumesAction() && player != null && !player.getAbilities().instabuild) {
            float remaining = Math.max(0.0F, getUses(stack) - mode.cost);
            if (remaining > 0.0F) {
                stack.setCount(1);
                LegacyItemData.update(stack, tag -> tag.putFloat(USES, remaining));
            }
        }
        return result;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) return InteractionResultHolder.pass(stack);
        int next = (getMode(stack).ordinal() + 1) % PlacementMode.values().length;
        LegacyItemData.update(stack, tag -> tag.putInt(MODE, next));
        if (!level.isClientSide) {
            player.displayClientMessage(Component.translatable(
                    "message.openblocks_reborn.imaginary_mode",
                    Component.translatable("message.openblocks_reborn.imaginary_mode."
                            + PlacementMode.values()[next].name().toLowerCase())), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public String getDescriptionId(ItemStack stack) {
        return isCrayon(stack) ? "item.openblocks_reborn.magic_crayon"
                : "item.openblocks_reborn.magic_pencil";
    }

    @Override
    public void appendHoverText(ItemStack stack, @javax.annotation.Nullable net.minecraft.world.level.Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.openblocks_reborn.imaginary_uses",
                String.format(java.util.Locale.ROOT, "%.2f", getUses(stack))));
        tooltip.add(Component.translatable("tooltip.openblocks_reborn.imaginary_mode",
                Component.translatable("message.openblocks_reborn.imaginary_mode."
                        + getMode(stack).name().toLowerCase())));
        if (isCrayon(stack)) {
            tooltip.add(Component.literal(String.format(java.util.Locale.ROOT, "#%06X", getColor(stack))));
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getUses(stack) < DEFAULT_USES;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * getUses(stack) / DEFAULT_USES);
    }
}
