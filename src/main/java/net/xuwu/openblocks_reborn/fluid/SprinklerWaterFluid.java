package net.xuwu.openblocks_reborn.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidType;

/**
 * A non-placeable, non-flowing water marker owned by the sprinkler block.
 *
 * <p>The legacy sprinkler used {@code Material.WATER} without actually being a
 * vanilla water block. Reusing NeoForge's vanilla water fluid type preserves
 * entity, boat, hydration and extinguishing behavior, while the no-op fluid and
 * air legacy state ensure it disappears together with the sprinkler.</p>
 */
public final class SprinklerWaterFluid extends Fluid {
    @Override
    public FluidType getFluidType() {
        return NeoForgeMod.WATER_TYPE.value();
    }

    @Override
    public Item getBucket() {
        return Items.AIR;
    }

    @Override
    protected boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos,
                                        Fluid fluid, Direction direction) {
        return false;
    }

    @Override
    protected Vec3 getFlow(BlockGetter level, BlockPos pos, FluidState state) {
        return Vec3.ZERO;
    }

    @Override
    public int getTickDelay(LevelReader level) {
        return Integer.MAX_VALUE;
    }

    @Override
    protected float getExplosionResistance() {
        return 100.0F;
    }

    @Override
    public float getHeight(FluidState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    public float getOwnHeight(FluidState state) {
        return 1.0F;
    }

    @Override
    protected BlockState createLegacyBlock(FluidState state) {
        return Blocks.AIR.defaultBlockState();
    }

    @Override
    public boolean isSource(FluidState state) {
        return true;
    }

    @Override
    public int getAmount(FluidState state) {
        return 8;
    }

    @Override
    public VoxelShape getShape(FluidState state, BlockGetter level, BlockPos pos) {
        return Shapes.block();
    }
}
