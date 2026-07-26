package net.xuwu.openblocks_reborn.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class HealBlock extends Block {
    public HealBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.Entity entity) {
        if (!level.isClientSide && entity instanceof LivingEntity living && level.getGameTime() % 20L == 0L) {
            living.heal(1.0F);
            if (living instanceof net.minecraft.world.entity.player.Player player) {
                player.getFoodData().eat(1, 0.2F);
            }
        }
        super.stepOn(level, pos, state, entity);
    }
}
