package net.xuwu.openblocks_reborn.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.xuwu.openblocks_reborn.registry.ModFluids;

public class XpBucketItem extends BucketItem {
    public XpBucketItem(Fluid fluid, Item.Properties properties) {
        super(fluid, properties);
    }

    /**
     * Empties one full XP bucket before a target block opens its menu. This mirrors
     * vanilla bucket priority while still leaving ordinary world placement to
     * {@link BucketItem#use(Level, Player, InteractionHand)}.
     */
    public static boolean emptyInto(ItemStack stack, Level level, Player player,
                                    InteractionHand hand, IFluidHandler destination) {
        if (!stack.is(ModFluids.XP_BUCKET.get())) return false;
        FluidStack bucket = new FluidStack(ModFluids.XP_JUICE.get(), 1_000);
        if (destination.fill(bucket, IFluidHandler.FluidAction.SIMULATE) != 1_000) return false;
        if (!level.isClientSide) {
            destination.fill(bucket, IFluidHandler.FluidAction.EXECUTE);
            if (!player.getAbilities().instabuild) {
                player.setItemInHand(hand, new ItemStack(Items.BUCKET));
            }
        }
        return true;
    }
}
