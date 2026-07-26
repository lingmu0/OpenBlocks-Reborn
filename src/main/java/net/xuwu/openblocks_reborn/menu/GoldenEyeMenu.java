package net.xuwu.openblocks_reborn.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.xuwu.openblocks_reborn.item.GoldenEyeItem;
import net.xuwu.openblocks_reborn.registry.ModMenus;

import java.util.ArrayList;
import java.util.List;

public final class GoldenEyeMenu extends AbstractContainerMenu {
    private final InteractionHand hand;
    private final List<ResourceLocation> structures;

    public GoldenEyeMenu(int containerId, Inventory inventory, FriendlyByteBuf data) {
        this(containerId, inventory, InteractionHand.values()[net.minecraft.util.Mth.clamp(data.readVarInt(), 0,
                InteractionHand.values().length - 1)], readStructures(data));
    }

    public GoldenEyeMenu(int containerId, Inventory inventory, InteractionHand hand,
                         List<ResourceLocation> structures) {
        super(ModMenus.GOLDEN_EYE.get(), containerId);
        this.hand = hand;
        this.structures = List.copyOf(structures);
    }

    private static List<ResourceLocation> readStructures(FriendlyByteBuf data) {
        int count = net.minecraft.util.Mth.clamp(data.readVarInt(), 0, 4096);
        List<ResourceLocation> result = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            ResourceLocation id = ResourceLocation.tryParse(data.readUtf(256));
            if (id != null) result.add(id);
        }
        return result;
    }

    public InteractionHand hand() {
        return hand;
    }

    public List<ResourceLocation> structures() {
        return structures;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getItemInHand(hand).getItem() instanceof GoldenEyeItem;
    }
}
