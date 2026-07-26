package net.xuwu.openblocks_reborn.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class SprinklerBlock extends Block {
    public SprinklerBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide) level.scheduleTick(pos, this, 20);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.hasNeighborSignal(pos)) {
            for (int i = 0; i < 4; i++) {
                BlockPos target = pos.offset(random.nextInt(9) - 4, random.nextInt(3) - 1, random.nextInt(9) - 4);
                BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), level, target);
            }
        }
        level.scheduleTick(pos, this, 20);
    }
}
