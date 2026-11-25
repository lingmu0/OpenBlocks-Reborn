// Derived from ElevatorMod 1.21.1-1.11.4 by VsnGamer (MIT); see LICENSE-ElevatorMod.
package net.xuwu.openblocks_reborn.elevator.level;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import javax.annotation.Nonnull;


public class ElevatorContainer extends AbstractContainerMenu {

    private final Direction playerFacing;
    private ElevatorBlockEntityBase elevatorTile;
    private final BlockPos pos;

    public ElevatorContainer(MenuType<ElevatorContainer> menuType, int id, BlockPos pos, Player player) {
        super(menuType, id);

        // TODO: 08/06/2023 Check if this is the correct way to get the level
        BlockEntity tile = player.level().getBlockEntity(pos);
        if (tile instanceof ElevatorBlockEntityBase)
            elevatorTile = (ElevatorBlockEntityBase) tile;

        playerFacing = player.getDirection();
        this.pos = pos;
    }

    @Override
    @Nonnull
    public ItemStack quickMoveStack(@Nonnull Player player, int id) {
        return ItemStack.EMPTY; // TODO: Handle this properly if in the future we start using items
    }

    @Override
    public boolean stillValid(Player playerIn) {
        return stillValid(ContainerLevelAccess.create(playerIn.level(), elevatorTile.getBlockPos()), playerIn, elevatorTile.getBlockState().getBlock());
    }

    public BlockPos getPos() {
        return pos;
    }

    public ElevatorBlockEntityBase getTile() {
        return elevatorTile;
    }

    public Direction getPlayerFacing() {
        return playerFacing;
    }
}
