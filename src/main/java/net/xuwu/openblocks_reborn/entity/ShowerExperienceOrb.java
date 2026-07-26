package net.xuwu.openblocks_reborn.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Experience emitted by an XP Shower. It falls like a normal orb but deliberately
 * skips the vanilla player-seeking scan so the shower produces a straight stream.
 */
public final class ShowerExperienceOrb extends ExperienceOrb {
    private int showerAge;

    public ShowerExperienceOrb(Level level, double x, double y, double z, int value) {
        super(EntityType.EXPERIENCE_ORB, level);
        setPos(x, y, z);
        setYRot((float)(random.nextDouble() * 360.0D));
        setDeltaMovement(0.0D, -0.1D * random.nextDouble(), 0.0D);
        this.value = value;
    }

    @Override
    public void tick() {
        baseTick();
        xo = getX();
        yo = getY();
        zo = getZ();

        Vec3 motion = getDeltaMovement().add(0.0D, -0.03D, 0.0D);
        setDeltaMovement(motion);
        move(MoverType.SELF, motion);

        float friction = 0.98F;
        if (onGround()) {
            BlockPos below = getBlockPosBelowThatAffectsMyMovement();
            friction = level().getBlockState(below).getFriction(level(), below, this) * 0.98F;
        }
        setDeltaMovement(getDeltaMovement().multiply(friction, 0.98D, friction));
        if (onGround()) {
            setDeltaMovement(getDeltaMovement().multiply(1.0D, -0.9D, 1.0D));
        }
        if (++showerAge >= 6000) discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("ShowerAge", showerAge);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        showerAge = tag.getInt("ShowerAge");
    }
}
