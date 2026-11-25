// Derived from ElevatorMod 1.21.1-1.11.4 by VsnGamer (MIT); see LICENSE-ElevatorMod.
package net.xuwu.openblocks_reborn.elevator.client;

import net.xuwu.openblocks_reborn.elevator.ElevatorBlockBase;
import net.xuwu.openblocks_reborn.elevator.level.ElevatorBlockEntityBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

public class ColorCamoElevator implements BlockColor {

    @Override
    public int getColor(BlockState state, BlockAndTintGetter lightReader, BlockPos pos, int tintIndex) {
        if (lightReader == null || pos == null) {
            return -1;
        }

        if (state.getBlock() instanceof ElevatorBlockBase && lightReader.getBlockEntity(pos) instanceof ElevatorBlockEntityBase tile) {
            if (tile.getHeldState() != null) {
                return Minecraft.getInstance().getBlockColors().getColor(tile.getHeldState(), lightReader, pos, tintIndex);
            }
        }
        return -1;
    }
}
