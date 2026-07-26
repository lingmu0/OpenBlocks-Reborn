package net.xuwu.openblocks_reborn.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.xuwu.openblocks_reborn.registry.ModBlockEntities;

public class SkyBlockEntity extends BlockEntity {
    public SkyBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SKY.get(), pos, state);
    }
}
