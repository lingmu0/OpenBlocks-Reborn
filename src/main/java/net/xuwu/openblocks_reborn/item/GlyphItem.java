package net.xuwu.openblocks_reborn.item;

import net.xuwu.openblocks_reborn.util.LegacyItemData;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.xuwu.openblocks_reborn.entity.GlyphEntity;
import net.xuwu.openblocks_reborn.registry.ModItems;

import java.util.List;

public class GlyphItem extends Item {
    private static final String TAG_CHARACTER = "GlyphCharacter";
    private static final String SELECTABLE = " 0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz!?+-=*/<>";

    public GlyphItem(Properties properties) {
        super(properties);
    }

    public static int sanitizeCharacter(int character) {
        return Character.isValidCodePoint(character) && !Character.isISOControl(character) ? character : '?';
    }

    public static int getCharacter(ItemStack stack) {
        CompoundTag tag = LegacyItemData.copyTag(stack);
        return tag.contains(TAG_CHARACTER) ? sanitizeCharacter(tag.getInt(TAG_CHARACTER)) : '?';
    }

    public static void setCharacter(ItemStack stack, int character) {
        LegacyItemData.update(stack,
                tag -> tag.putInt(TAG_CHARACTER, sanitizeCharacter(character)));
    }

    public static ItemStack createStack(int character) {
        ItemStack stack = new ItemStack(ModItems.GLYPH.get());
        setCharacter(stack, character);
        return stack;
    }

    public static int selectableCount() {
        return SELECTABLE.length();
    }

    public static int selectableCharacter(int index) {
        return SELECTABLE.charAt(Math.floorMod(index, SELECTABLE.length()));
    }

    public static int selectableIndex(int character) {
        int index = SELECTABLE.indexOf(character);
        return index >= 0 ? index : SELECTABLE.indexOf('A');
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Direction direction = context.getClickedFace();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockPos target = context.getClickedPos().relative(direction);
        if (direction.getAxis().isVertical()
                || player != null && !player.mayUseItemAt(target, direction, stack)) {
            return InteractionResult.FAIL;
        }

        Vec3 local = context.getClickLocation().subtract(Vec3.atLowerCornerOf(context.getClickedPos()));
        Direction left = direction.getCounterClockWise();
        double horizontal = local.x * left.getStepX() + local.z * left.getStepZ();
        if (left.getStepX() < 0 || left.getStepZ() < 0) horizontal = -horizontal;
        int offsetX = Mth.clamp(Mth.floor(horizontal * 16.0D), 0, 16);
        int offsetY = Mth.clamp(Mth.floor(local.y * 16.0D), 0, 16);
        Level level = context.getLevel();
        GlyphEntity glyph = new GlyphEntity(level, target, direction, getCharacter(stack), offsetX, offsetY);
        if (!glyph.survives()) return InteractionResult.CONSUME;
        if (!level.isClientSide) {
            glyph.playPlacementSound();
            level.gameEvent(player, GameEvent.ENTITY_PLACE, glyph.position());
            level.addFreshEntity(glyph);
        }
        if (!player.getAbilities().instabuild) stack.shrink(1);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        int current = SELECTABLE.indexOf(getCharacter(stack));
        int next = (current + (player.isShiftKeyDown() ? SELECTABLE.length() - 1 : 1)) % SELECTABLE.length();
        setCharacter(stack, SELECTABLE.charAt(next));
        if (!level.isClientSide) {
            player.displayClientMessage(Component.translatable("message.openblocks_reborn.glyph_selected",
                    new String(Character.toChars(getCharacter(stack)))), true);
            level.playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(),
                    SoundSource.PLAYERS, 0.4F, 1.3F);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(getDescriptionId(stack))
                .append(Component.literal(" [" + new String(Character.toChars(getCharacter(stack))) + "]"));
    }

    @Override
    public void appendHoverText(ItemStack stack, @javax.annotation.Nullable net.minecraft.world.level.Level level, List<Component> tooltip, TooltipFlag flag) {
        int character = getCharacter(stack);
        tooltip.add(Component.translatable("tooltip.openblocks_reborn.glyph_character",
                String.format("U+%04X", character)).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.openblocks_reborn.glyph_cycle").withStyle(ChatFormatting.DARK_GRAY));
    }
}
