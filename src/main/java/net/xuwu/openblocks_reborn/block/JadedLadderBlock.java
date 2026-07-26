package net.xuwu.openblocks_reborn.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;

/**
 * The legacy "Jaded Ladder" is a trapdoor which becomes climbable while open.
 */
public class JadedLadderBlock extends TrapDoorBlock {
    public JadedLadderBlock(BlockBehaviour.Properties properties) {
        super(BlockSetType.OAK, properties);
    }

    @Override
    public boolean isLadder(BlockState state, LevelReader level, BlockPos pos, LivingEntity entity) {
        return state.getValue(OPEN);
    }
}
