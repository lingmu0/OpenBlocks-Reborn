package net.xuwu.openblocks_reborn.item;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.xuwu.openblocks_reborn.entity.GoldenEyeEntity;
import net.xuwu.openblocks_reborn.menu.GoldenEyeMenu;
import net.xuwu.openblocks_reborn.registry.ModEntities;

import java.util.Comparator;
import java.util.List;

public class GoldenEyeItem extends Item {
    private static final String STRUCTURE = "GoldenEyeStructure";
    private static final String TARGET = "GoldenEyeTarget";

    public GoldenEyeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(stack);
        if (player.isShiftKeyDown()) {
            if (player instanceof ServerPlayer serverPlayer) openSelection(serverPlayer, hand);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        InteractionHand other = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack recharge = player.getItemInHand(other);
        if (recharge.is(Items.ENDER_PEARL) && stack.getDamageValue() > 0) {
            if (!level.isClientSide) {
                stack.setDamageValue(Math.max(0, stack.getDamageValue() - 10));
                if (!player.getAbilities().instabuild) recharge.shrink(1);
                player.displayClientMessage(Component.translatable(
                        "message.openblocks_reborn.golden_eye_recharged"), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(TARGET) || stack.getDamageValue() >= stack.getMaxDamage()) {
            player.displayClientMessage(Component.translatable(tag.contains(TARGET)
                    ? "message.openblocks_reborn.golden_eye_broken"
                    : "message.openblocks_reborn.golden_eye_unbound"), true);
            return InteractionResultHolder.fail(stack);
        }

        stack.setDamageValue(stack.getDamageValue() + 1);
        ItemStack launchedStack = stack.copyWithCount(1);
        GoldenEyeEntity eye = new GoldenEyeEntity(ModEntities.GOLDEN_EYE.get(), serverLevel);
        eye.launchFrom(player, BlockPos.of(tag.getLong(TARGET)), launchedStack, false);
        serverLevel.addFreshEntity(eye);
        serverLevel.playSound(null, player.blockPosition(), SoundEvents.ENDER_EYE_LAUNCH,
                SoundSource.NEUTRAL, 0.5F, 0.4F / (player.getRandom().nextFloat() * 0.4F + 0.8F));
        return InteractionResultHolder.success(stack);
    }

    private static void openSelection(ServerPlayer player, InteractionHand hand) {
        Registry<Structure> registry = player.serverLevel().registryAccess()
                .registryOrThrow(Registries.STRUCTURE);
        List<ResourceLocation> structures = registry.keySet().stream()
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .toList();
        player.openMenu(new SimpleMenuProvider(
                        (containerId, inventory, ignored) ->
                                new GoldenEyeMenu(containerId, inventory, hand, structures),
                        Component.translatable("gui.openblocks_reborn.golden_eye.title")),
                buffer -> {
                    buffer.writeVarInt(hand.ordinal());
                    buffer.writeVarInt(structures.size());
                    structures.forEach(id -> buffer.writeUtf(id.toString(), 256));
                });
    }

    public static boolean bindStructure(ServerPlayer player, InteractionHand hand,
                                        ResourceLocation structureId) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof GoldenEyeItem)) return false;
        ServerLevel level = player.serverLevel();
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        ResourceKey<Structure> key = ResourceKey.create(Registries.STRUCTURE, structureId);
        Holder<Structure> structure = registry.getHolder(key).orElse(null);
        if (structure == null) return false;
        Pair<BlockPos, Holder<Structure>> found = level.getChunkSource().getGenerator()
                .findNearestMapStructure(level, HolderSet.direct(structure),
                        player.blockPosition(), 100, false);
        if (found == null) {
            player.displayClientMessage(Component.translatable(
                    "message.openblocks_reborn.golden_eye_no_selected_structure",
                    structureId.toString()), false);
            return false;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString(STRUCTURE, structureId.toString());
            tag.putLong(TARGET, found.getFirst().asLong());
        });
        player.displayClientMessage(Component.translatable(
                "message.openblocks_reborn.golden_eye_locked", structureId.toString()), false);
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.contains(STRUCTURE)) {
            tooltip.add(Component.translatable("tooltip.openblocks_reborn.golden_eye_locked",
                    tag.getString(STRUCTURE)));
        } else {
            tooltip.add(Component.translatable("tooltip.openblocks_reborn.golden_eye_unbound"));
        }
    }
}
