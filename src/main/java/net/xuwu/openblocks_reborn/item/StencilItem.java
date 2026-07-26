package net.xuwu.openblocks_reborn.item;

import net.xuwu.openblocks_reborn.util.LegacyItemData;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.xuwu.openblocks_reborn.blockentity.CanvasBlockEntity;
import net.xuwu.openblocks_reborn.registry.ModItems;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.xuwu.openblocks_reborn.client.StencilItemRenderer;

import java.util.function.Consumer;

/**
 * A prepared stencil is a reusable 16x16 cut-out pattern. It is placed on one
 * canvas face; subsequent paint-brush uses only colour the cut-out pixels.
 */
public class StencilItem extends Item {
    private static final String PATTERN = "StencilPattern";

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private StencilItemRenderer renderer;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new StencilItemRenderer();
                return renderer;
            }
        });
    }

    /*
     * Exact pattern order and masks from OpenBlocks 1.8.1. An X is a cut-out
     * through which paint reaches the canvas.
     */
    private static final String[] PATTERNS = {
            pattern(
                    "                ",
                    "                ",
                    "  XXXX    XXXX  ",
                    "  XXXX    XXXX  ",
                    "  XXXX    XXXX  ",
                    "  XXXX    XXXX  ",
                    "      XXXX      ",
                    "      XXXX      ",
                    "    XXXXXXXX    ",
                    "    XXXXXXXX    ",
                    "    XXXXXXXX    ",
                    "    XXXXXXXX    ",
                    "    XX    XX    ",
                    "    XX    XX    ",
                    "                ",
                    "                "),
            pattern(
                    "XX              ",
                    "XX              ",
                    "XX              ",
                    "XX              ",
                    "XX              ",
                    "XX              ",
                    "XX              ",
                    "XX              ",
                    "XX              ",
                    "XX              ",
                    "XX              ",
                    "XX              ",
                    "XX              ",
                    "XX              ",
                    "XX              ",
                    "XX              "),
            pattern(
                    "X X X X X X X X ",
                    "X X X X X X X X ",
                    "X X X X X X X X ",
                    "X X X X X X X X ",
                    "X X X X X X X X ",
                    "X X X X X X X X ",
                    "X X X X X X X X ",
                    "X X X X X X X X ",
                    "X X X X X X X X ",
                    "X X X X X X X X ",
                    "X X X X X X X X ",
                    "X X X X X X X X ",
                    "X X X X X X X X ",
                    "X X X X X X X X ",
                    "X X X X X X X X ",
                    "X X X X X X X X "),
            pattern(
                    "                ",
                    "                ",
                    "  XXXXXX        ",
                    "  XXXXXX        ",
                    "  XX            ",
                    "  XX            ",
                    "  XX            ",
                    "  XX            ",
                    "                ",
                    "                ",
                    "                ",
                    "                ",
                    "                ",
                    "                ",
                    "                ",
                    "                "),
            pattern(
                    "XXXXXXXX        ",
                    "XXXXXXX         ",
                    "XXXXXX          ",
                    "XXXXX           ",
                    "XXXX            ",
                    "XXX             ",
                    "XX              ",
                    "X               ",
                    "                ",
                    "                ",
                    "                ",
                    "                ",
                    "                ",
                    "                ",
                    "                ",
                    "                "),
            pattern(
                    "                ",
                    " XXXXXXX        ",
                    " XXXXXXX        ",
                    " XX             ",
                    " XX XXXX        ",
                    " XX XXXX        ",
                    " XX XX          ",
                    " XX XX          ",
                    "                ",
                    "                ",
                    "                ",
                    "                ",
                    "                ",
                    "                ",
                    "                ",
                    "                "),
            pattern(
                    "                ",
                    "                ",
                    "                ",
                    "                ",
                    "    XXXXXXXX    ",
                    "    XXXXXXXX    ",
                    "    XXXXXXXX    ",
                    "    XXXXXXXX    ",
                    "    XXXXXXXX    ",
                    "    XXXXXXXX    ",
                    "    XXXXXXXX    ",
                    "    XXXXXXXX    ",
                    "                ",
                    "                ",
                    "                ",
                    "                "),
            pattern(
                    "                ",
                    "XXXXXXXXXXXXXXX ",
                    "              X ",
                    " XXXXXXXXXXXX X ",
                    " X          X X ",
                    " X XXXXXXXX X X ",
                    " X X      X X X ",
                    " X X XXXX X X X ",
                    " X X X  X X X X ",
                    " X X X    X X X ",
                    " X X XXXXXX X X ",
                    " X X        X X ",
                    " X XXXXXXXXXX X ",
                    " X            X ",
                    " XXXXXXXXXXXXXX ",
                    "                "),
            pattern(
                    "  XXXX    XXXX  ",
                    "  XXXX    XXXX  ",
                    "  XXXX    XXXX  ",
                    "  XXXX    XXXX  ",
                    "  XXXX    XXXX  ",
                    "  XXXX    XXXX  ",
                    "  XXXX    XXXX  ",
                    "  XXXX    XXXX  ",
                    "  XXXX    XXXX  ",
                    "  XXXX    XXXX  ",
                    "  XXXX    XXXX  ",
                    "  XXXX    XXXX  ",
                    "  XXXX    XXXX  ",
                    "  XXXX    XXXX  ",
                    "  XXXX    XXXX  ",
                    "  XXXX    XXXX  "),
            pattern(
                    " XX     X    XX ",
                    "XXX    XXX   XXX",
                    "XX     XX      X",
                    "    X      XX   ",
                    "   XXX    XXX   ",
                    "   XXX    X    X",
                    "    XX      XXXX",
                    "              XX",
                    "        X       ",
                    "       XX  XX   ",
                    " XX   XXX     X ",
                    " XXX  XXX     XX",
                    " XXX   X        ",
                    "           XX   ",
                    "    XX     XXX  ",
                    "    XXX     XX  "),
            pattern(
                    "                ",
                    "                ",
                    "                ",
                    "   XXXXXXXXXX   ",
                    "   X        X   ",
                    "   X        X   ",
                    "   X   XX   X   ",
                    "   XXXXXXXXXX   ",
                    "   X   XX   X   ",
                    "   X        X   ",
                    "   X        X   ",
                    "   X        X   ",
                    "   XXXXXXXXXX   ",
                    "                ",
                    "                ",
                    "                "),
            pattern(
                    "                ",
                    "                ",
                    "   XXX    XXX   ",
                    "  X   X  X   X  ",
                    " X     XX     X ",
                    " X            X ",
                    " X            X ",
                    " X            X ",
                    "  X          X  ",
                    "   X        X   ",
                    "    X      X    ",
                    "     X    X     ",
                    "      X  X      ",
                    "       XX       ",
                    "                ",
                    "                "),
            pattern(
                    "                ",
                    "                ",
                    "                ",
                    "   XXX    XXX   ",
                    "  XXXXX  XXXXX  ",
                    "  XXXXXXXXXXXX  ",
                    "  XXXXXXXXXXXX  ",
                    "  XXXXXXXXXXXX  ",
                    "   XXXXXXXXXX   ",
                    "    XXXXXXXX    ",
                    "     XXXXXX     ",
                    "      XXXX      ",
                    "       XX       ",
                    "                ",
                    "                ",
                    "                "),
            pattern(
                    "                ",
                    "                ",
                    "       XXXXXX   ",
                    "  XXXXXXXXXXX   ",
                    "  XXXXXX    X   ",
                    "  X         X   ",
                    "  X         X   ",
                    "  X         X   ",
                    "  X         X   ",
                    "  X         XX  ",
                    "  XX        XXX ",
                    "  XXX       XXX ",
                    "  XXX        X  ",
                    "   X            ",
                    "                ",
                    "                "),
            pattern(
                    "                ",
                    "      XXXX      ",
                    "     XXXXXX     ",
                    "    XXXXXXXX    ",
                    "   XXXXXXXXXX   ",
                    "   XXXXXXXXXX   ",
                    "   XXXXXXXXXX   ",
                    "   XXXXXXXXXX   ",
                    "   XXXXXXXXXX   ",
                    "    XXXXXXXX    ",
                    "     XXXXXX     ",
                    "      XXXX      ",
                    "       X        ",
                    "      XXX       ",
                    "         X      ",
                    "          XXX   ")
    };

    public static final int PATTERN_COUNT = PATTERNS.length;

    public StencilItem(Properties properties) {
        super(properties);
    }

    public static ItemStack createPatternStack(int pattern) {
        ItemStack result = new ItemStack(ModItems.STENCIL.get());
        setPattern(result, pattern);
        return result;
    }

    public static void setPattern(ItemStack stack, int pattern) {
        int normalized = Math.floorMod(pattern, PATTERN_COUNT);
        LegacyItemData.update(stack, tag -> tag.putInt(PATTERN, normalized));
    }

    public static int getPattern(ItemStack stack) {
        var tag = LegacyItemData.copyTag(stack);
        return tag.contains(PATTERN) ? Math.floorMod(tag.getInt(PATTERN), PATTERN_COUNT) : -1;
    }

    public static String patternTranslationKey(int pattern) {
        return "gui.openblocks_reborn.stencil_pattern." + Math.floorMod(pattern, PATTERN_COUNT);
    }

    /** Returns true for a cut-out pixel after applying the placed stencil rotation. */
    public static boolean isHole(int pattern, int rotation, int x, int y) {
        int sourceX = net.minecraft.util.Mth.clamp(x, 0, CanvasBlockEntity.SIDE_SIZE - 1);
        int sourceY = net.minecraft.util.Mth.clamp(y, 0, CanvasBlockEntity.SIDE_SIZE - 1);
        for (int turn = 0; turn < Math.floorMod(rotation, 4); turn++) {
            int previousX = sourceX;
            sourceX = sourceY;
            sourceY = CanvasBlockEntity.SIDE_SIZE - 1 - previousX;
        }
        String mask = PATTERNS[Math.floorMod(pattern, PATTERN_COUNT)];
        return mask.charAt(sourceY * CanvasBlockEntity.SIDE_SIZE + sourceX) != ' ';
    }

    @Override
    public Component getName(ItemStack stack) {
        int pattern = getPattern(stack);
        return pattern >= 0 ? Component.translatable(getDescriptionId(stack))
                .append(Component.literal(" ["))
                .append(Component.translatable(patternTranslationKey(pattern)))
                .append(Component.literal("]")) : super.getName(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof CanvasBlockEntity canvas)) {
            return InteractionResult.PASS;
        }
        int pattern = getPattern(context.getItemInHand());
        if (pattern < 0) return InteractionResult.FAIL;
        if (!context.getLevel().isClientSide) {
            if (!canvas.placeStencil(context.getClickedFace(), pattern)) return InteractionResult.FAIL;
            if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    private static String pattern(String... rows) {
        String result = String.join("", rows);
        if (result.length() != CanvasBlockEntity.PIXELS_PER_FACE) {
            throw new IllegalArgumentException("Stencil pattern must be exactly 16x16 pixels");
        }
        return result;
    }
}
