package net.xuwu.openblocks_reborn.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.xuwu.openblocks_reborn.registry.ModItems;

/**
 * Eye-of-ender style pointer used as a visual guide by the Golden Eye.
 */
public class GoldenEyeEntity extends Entity implements ItemSupplier {
    private static final EntityDataAccessor<ItemStack> ITEM =
            SynchedEntityData.defineId(GoldenEyeEntity.class, EntityDataSerializers.ITEM_STACK);
    private double targetX;
    private double targetY;
    private double targetZ;
    private int life;
    private boolean returnItem;

    public GoldenEyeEntity(EntityType<? extends GoldenEyeEntity> type, Level level) {
        super(type, level);
    }

    public void launchFrom(Entity owner, BlockPos target, ItemStack stack, boolean returnItem) {
        setPos(owner.getX(), owner.getY(0.5D), owner.getZ());
        setItem(stack);
        this.returnItem = returnItem;
        double dx = target.getX() - getX();
        double dz = target.getZ() - getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (horizontal > 12.0D) {
            targetX = getX() + dx / horizontal * 12.0D;
            targetZ = getZ() + dz / horizontal * 12.0D;
            targetY = getY() + 8.0D;
        } else {
            targetX = target.getX();
            targetY = target.getY();
            targetZ = target.getZ();
        }
        life = 0;
    }

    public void setItem(ItemStack stack) {
        entityData.set(ITEM, stack.copyWithCount(1));
    }

    @Override
    public ItemStack getItem() {
        return entityData.get(ITEM);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ITEM, new ItemStack(ModItems.GOLDEN_EYE.get()));
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 motion = getDeltaMovement();
        double x = getX() + motion.x;
        double y = getY() + motion.y;
        double z = getZ() + motion.z;
        double horizontalSpeed = motion.horizontalDistance();
        setXRot(Mth.rotLerp(0.2F, xRotO,
                (float)(Mth.atan2(motion.y, horizontalSpeed) * 180.0F / Math.PI)));
        setYRot(Mth.rotLerp(0.2F, yRotO,
                (float)(Mth.atan2(motion.x, motion.z) * 180.0F / Math.PI)));

        if (!level().isClientSide) {
            double dx = targetX - x;
            double dz = targetZ - z;
            float distance = (float)Math.sqrt(dx * dx + dz * dz);
            float angle = (float)Mth.atan2(dz, dx);
            double speed = Mth.lerp(0.0025D, horizontalSpeed, distance);
            double vertical = motion.y;
            if (distance < 1.0F) {
                speed *= 0.8D;
                vertical *= 0.8D;
            }
            int verticalDirection = getY() < targetY ? 1 : -1;
            setDeltaMovement(Math.cos(angle) * speed,
                    vertical + (verticalDirection - vertical) * 0.015D,
                    Math.sin(angle) * speed);
        }

        level().addParticle(ParticleTypes.PORTAL,
                x - motion.x * 0.25D + random.nextDouble() * 0.6D - 0.3D,
                y - motion.y * 0.25D - 0.5D,
                z - motion.z * 0.25D + random.nextDouble() * 0.6D - 0.3D,
                motion.x, motion.y, motion.z);

        if (!level().isClientSide) {
            setPos(x, y, z);
            if (++life > 60) {
                playSound(SoundEvents.ENDER_EYE_DEATH, 1.0F, 1.0F);
                if (returnItem) {
                    ItemEntity returned = new ItemEntity(level(), getX(), getY(), getZ(), getItem());
                    returned.setDefaultPickUpDelay();
                    level().addFreshEntity(returned);
                }
                discard();
            }
        } else {
            setPosRaw(x, y, z);
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.put("Item", getItem().save(registryAccess()));
        tag.putDouble("TargetX", targetX);
        tag.putDouble("TargetY", targetY);
        tag.putDouble("TargetZ", targetZ);
        tag.putInt("Life", life);
        tag.putBoolean("ReturnItem", returnItem);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("Item", CompoundTag.TAG_COMPOUND)) {
            setItem(ItemStack.parse(registryAccess(), tag.getCompound("Item"))
                    .orElseGet(() -> new ItemStack(ModItems.GOLDEN_EYE.get())));
        }
        targetX = tag.getDouble("TargetX");
        targetY = tag.getDouble("TargetY");
        targetZ = tag.getDouble("TargetZ");
        life = tag.getInt("Life");
        returnItem = tag.getBoolean("ReturnItem");
    }

    public boolean returnsItem() {
        return returnItem;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public float getLightLevelDependentMagicValue() {
        return 1.0F;
    }
}
