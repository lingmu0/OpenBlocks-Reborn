package net.xuwu.openblocks_reborn.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.Block;
import net.xuwu.openblocks_reborn.item.CursorItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla workstations use this distance check instead of their container's
 * validity method. Only the menu opened by the current Cursor interaction is
 * exempted; ordinary menus retain the vanilla four-block reach.
 */
@Mixin(AbstractContainerMenu.class)
public abstract class RemoteContainerMenuMixin {
    @Inject(method = "stillValid(Lnet/minecraft/world/inventory/ContainerLevelAccess;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/block/Block;)Z",
            at = @At("HEAD"), cancellable = true)
    private static void openblocksReborn$allowCursorMenu(ContainerLevelAccess access, Player player,
                                                         Block block,
                                                         CallbackInfoReturnable<Boolean> result) {
        if (CursorItem.isRemoteMenuValid(player)) result.setReturnValue(true);
    }
}
