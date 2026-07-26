package net.xuwu.openblocks_reborn.registry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;

/**
 * Forge 1.20.1 capability lookup helpers. NeoForge's level-based capability
 * lookup API did not exist yet, so queries are routed through block entities.
 */
public final class ModCapabilities {
    public static IItemHandler item(Level level, BlockPos pos, Direction side) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity == null ? null
                : blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, side).orElse(null);
    }

    public static IFluidHandler fluid(Level level, BlockPos pos, Direction side) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity == null ? null
                : blockEntity.getCapability(ForgeCapabilities.FLUID_HANDLER, side).orElse(null);
    }

    private ModCapabilities() {
    }
}
