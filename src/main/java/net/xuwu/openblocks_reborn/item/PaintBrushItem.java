package net.xuwu.openblocks_reborn.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.xuwu.openblocks_reborn.block.ColorableBlock;
import net.xuwu.openblocks_reborn.registry.ModBlocks;
import net.xuwu.openblocks_reborn.blockentity.CanvasBlockEntity;

public class PaintBrushItem extends Item {
    private static final String COLOR = "PaintColor";
    private static final String RGB_COLOR = "PaintRgb";
    private final boolean pixelMode;

    public PaintBrushItem(Properties properties) {
        this(properties, false);
    }

    public PaintBrushItem(Properties properties, boolean pixelMode) {
        super(properties);
        this.pixelMode = pixelMode;
    }

    public static DyeColor getColor(ItemStack stack) {
        int id = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt(COLOR);
        return DyeColor.byId(id);
    }

    public static void setColor(ItemStack stack, DyeColor color) {
        setRgbColor(stack, color.getTextureDiffuseColor());
    }

    public static int getRgbColor(ItemStack stack) {
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.contains(RGB_COLOR)
                ? tag.getInt(RGB_COLOR) & 0xFFFFFF
                : getColor(stack).getTextureDiffuseColor() & 0xFFFFFF;
    }

    public static void setRgbColor(ItemStack stack, int rgb) {
        int normalized = rgb & 0xFFFFFF;
        DyeColor nearest = nearestDyeColor(normalized);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putInt(COLOR, nearest.getId());
            tag.putInt(RGB_COLOR, normalized);
        });
    }

    public static DyeColor nearestDyeColor(int rgb) {
        int red = rgb >> 16 & 0xFF;
        int green = rgb >> 8 & 0xFF;
        int blue = rgb & 0xFF;
        DyeColor result = DyeColor.WHITE;
        int bestDistance = Integer.MAX_VALUE;
        for (DyeColor candidate : DyeColor.values()) {
            int candidateRgb = candidate.getTextureDiffuseColor();
            int dr = red - (candidateRgb >> 16 & 0xFF);
            int dg = green - (candidateRgb >> 8 & 0xFF);
            int db = blue - (candidateRgb & 0xFF);
            int distance = dr * dr + dg * dg + db * db;
            if (distance < bestDistance) {
                bestDistance = distance;
                result = candidate;
            }
        }
        return result;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack brush = player.getItemInHand(hand);
        InteractionHand other = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack dyeStack = player.getItemInHand(other);
        if (dyeStack.getItem() instanceof DyeItem dye) {
            if (!level.isClientSide) {
                setColor(brush, dye.getDyeColor());
                if (!player.getAbilities().instabuild) dyeStack.shrink(1);
                player.displayClientMessage(Component.translatable("message.openblocks_reborn.paint_loaded", dye.getDyeColor().getName()), true);
            }
            return InteractionResultHolder.sidedSuccess(brush, level.isClientSide);
        }
        return InteractionResultHolder.pass(brush);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockPosAccess access = new BlockPosAccess(context);
        if (context.getLevel().getBlockEntity(context.getClickedPos()) instanceof CanvasBlockEntity canvas) {
            if (!context.getLevel().isClientSide) {
                int color = 0xFF000000 | getRgbColor(context.getItemInHand());
                boolean painted;
                if (pixelMode) {
                    int[] pixel = canvasPixel(context);
                    painted = canvas.applyPaintPixel(context.getClickedFace(), pixel[0], pixel[1], color);
                } else if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
                    canvas.applyPaintAll(color);
                    painted = true;
                } else {
                    canvas.applyPaint(context.getClickedFace(), color);
                    painted = true;
                }
                if (painted && context.getPlayer() != null) context.getItemInHand().hurtAndBreak(1, context.getPlayer(),
                        context.getPlayer().getEquipmentSlotForItem(context.getItemInHand()));
            }
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }
        BlockState state = access.state;
        if (!state.hasProperty(ColorableBlock.COLOR)) return InteractionResult.PASS;
        if (!context.getLevel().isClientSide) {
            DyeColor color = state.getBlock() == ModBlocks.PAINT_CAN.get()
                    ? state.getValue(ColorableBlock.COLOR) : getColor(context.getItemInHand());
            if (state.getBlock() == ModBlocks.PAINT_CAN.get()) {
                setColor(context.getItemInHand(), color);
            } else {
                context.getLevel().setBlock(context.getClickedPos(), state.setValue(ColorableBlock.COLOR, color), Block.UPDATE_ALL);
                if (context.getPlayer() != null) context.getItemInHand().hurtAndBreak(1, context.getPlayer(),
                        context.getPlayer().getEquipmentSlotForItem(context.getItemInHand()));
            }
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    private record BlockPosAccess(BlockState state) {
        private BlockPosAccess(UseOnContext context) {
            this(context.getLevel().getBlockState(context.getClickedPos()));
        }
    }

    public static int[] canvasPixel(UseOnContext context) {
        net.minecraft.world.phys.Vec3 hit = context.getClickLocation();
        net.minecraft.core.BlockPos pos = context.getClickedPos();
        double x = hit.x - pos.getX();
        double y = hit.y - pos.getY();
        double z = hit.z - pos.getZ();
        double u;
        double v;
        switch (context.getClickedFace()) {
            case NORTH -> { u = 1.0D - x; v = 1.0D - y; }
            case SOUTH -> { u = x; v = 1.0D - y; }
            case WEST -> { u = z; v = 1.0D - y; }
            case EAST -> { u = 1.0D - z; v = 1.0D - y; }
            // CanvasRenderer draws the UP face's first row at its south edge
            // and the DOWN face's first row at its north edge.
            case UP -> { u = x; v = 1.0D - z; }
            case DOWN -> { u = x; v = z; }
            default -> { u = 0.5D; v = 0.5D; }
        }
        return new int[] {
                Math.clamp((int)Math.floor(u * CanvasBlockEntity.SIDE_SIZE), 0, CanvasBlockEntity.SIDE_SIZE - 1),
                Math.clamp((int)Math.floor(v * CanvasBlockEntity.SIDE_SIZE), 0, CanvasBlockEntity.SIDE_SIZE - 1)
        };
    }
}
