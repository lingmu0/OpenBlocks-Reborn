package net.xuwu.openblocks_reborn.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.xuwu.openblocks_reborn.item.CursorItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps chests, furnaces and other block-entity containers open for a valid Cursor session. */
@Mixin(BaseContainerBlockEntity.class)
public abstract class RemoteBlockEntityMenuMixin {
    @Inject(method = "stillValid", at = @At("HEAD"), cancellable = true)
    private void openblocksReborn$allowCursorMenu(Player player,
                                                  CallbackInfoReturnable<Boolean> result) {
        if (CursorItem.isRemoteMenuValid(player)) result.setReturnValue(true);
    }
}
