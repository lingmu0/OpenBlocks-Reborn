package net.xuwu.openblocks_reborn.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/**
 * Falling block variant used by the Crane. Vanilla falling blocks only send
 * movement updates every 20 ticks, which makes a player-controlled load jump.
 */
public class CraneCarriedBlockEntity extends FallingBlockEntity {
    private int interpolationSteps;
    private double interpolationX;
    private double interpolationY;
    private double interpolationZ;
    private float interpolationYRot;
    private float interpolationXRot;

    public CraneCarriedBlockEntity(EntityType<? extends FallingBlockEntity> type, Level level) {
        super(type, level);
    }

    public static CraneCarriedBlockEntity lift(EntityType<? extends FallingBlockEntity> type,
                                                Level level, BlockPos pos, BlockState state) {
        CraneCarriedBlockEntity entity = new CraneCarriedBlockEntity(type, level);
        BlockState carriedState = state.hasProperty(BlockStateProperties.WATERLOGGED)
                ? state.setValue(BlockStateProperties.WATERLOGGED, false)
                : state;
        CompoundTag data = new CompoundTag();
        data.put("BlockState", NbtUtils.writeBlockState(carriedState));
        data.putInt("Time", 0);
        data.putBoolean("DropItem", true);
        entity.readAdditionalSaveData(data);
        entity.blocksBuilding = true;
        entity.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        entity.setDeltaMovement(Vec3.ZERO);
        entity.xo = entity.getX();
        entity.yo = entity.getY();
        entity.zo = entity.getZ();
        entity.setStartPos(pos);
        level.setBlock(pos, state.getFluidState().createLegacyBlock(), 3);
        level.addFreshEntity(entity);
        return entity;
    }

    @Override
    public void tick() {
        if (!isNoGravity()) {
            super.tick();
            return;
        }

        /*
         * Entity's default lerpTo() snaps non-living entities directly to every
         * movement packet. Keep real previous-frame coordinates and consume each
         * server target over three client ticks, matching the smooth interpolation
         * path used by living entities.
         */
        xo = getX();
        yo = getY();
        zo = getZ();
        baseTick();
        if (level().isClientSide && interpolationSteps > 0) {
            double nextX = getX() + (interpolationX - getX()) / interpolationSteps;
            double nextY = getY() + (interpolationY - getY()) / interpolationSteps;
            double nextZ = getZ() + (interpolationZ - getZ()) / interpolationSteps;
            float nextYRot = getYRot() + Mth.wrapDegrees(interpolationYRot - getYRot())
                    / interpolationSteps;
            float nextXRot = getXRot() + (interpolationXRot - getXRot())
                    / interpolationSteps;
            interpolationSteps--;
            setPos(nextX, nextY, nextZ);
            setRot(nextYRot, nextXRot);
        }
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        if (!level().isClientSide || !isNoGravity()) {
            super.lerpTo(x, y, z, yRot, xRot, steps);
            return;
        }
        interpolationX = x;
        interpolationY = y;
        interpolationZ = z;
        interpolationYRot = yRot;
        interpolationXRot = xRot;
        interpolationSteps = Math.max(3, steps);
    }

    @Override
    public double lerpTargetX() {
        return interpolationSteps > 0 ? interpolationX : getX();
    }

    @Override
    public double lerpTargetY() {
        return interpolationSteps > 0 ? interpolationY : getY();
    }

    @Override
    public double lerpTargetZ() {
        return interpolationSteps > 0 ? interpolationZ : getZ();
    }

    @Override
    public float lerpTargetXRot() {
        return interpolationSteps > 0 ? interpolationXRot : getXRot();
    }

    @Override
    public float lerpTargetYRot() {
        return interpolationSteps > 0 ? interpolationYRot : getYRot();
    }
}
