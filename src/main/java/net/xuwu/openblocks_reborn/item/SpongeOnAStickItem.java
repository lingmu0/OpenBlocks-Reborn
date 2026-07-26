package net.xuwu.openblocks_reborn.item;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.xuwu.openblocks_reborn.block.EnhancedSpongeBlock;

public class SpongeOnAStickItem extends Item {
    public SpongeOnAStickItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!context.getLevel().isClientSide) {
            int removed = EnhancedSpongeBlock.drain(context.getLevel(), context.getClickedPos());
            if (removed > 0 && context.getPlayer() != null) {
                context.getItemInHand().hurtAndBreak(1, context.getPlayer(), context.getPlayer().getEquipmentSlotForItem(context.getItemInHand()));
            }
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }
}
