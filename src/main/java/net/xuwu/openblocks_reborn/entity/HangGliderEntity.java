package net.xuwu.openblocks_reborn.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.xuwu.openblocks_reborn.item.HangGliderItem;

public class HangGliderEntity extends Entity {
    private static final EntityDataAccessor<Integer> OWNER =
            SynchedEntityData.defineId(HangGliderEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> OFF_HAND =
            SynchedEntityData.defineId(HangGliderEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> FLYING =
            SynchedEntityData.defineId(HangGliderEntity.class, EntityDataSerializers.BOOLEAN);

    public HangGliderEntity(EntityType<? extends HangGliderEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(OWNER, 0);
        builder.define(OFF_HAND, false);
        builder.define(FLYING, false);
    }

    public void attachTo(Player player, InteractionHand hand) {
        entityData.set(OWNER, player.getId());
        entityData.set(OFF_HAND, hand == InteractionHand.OFF_HAND);
        moveWithOwner(player);
    }

    public Player getOwner() {
        Entity owner = level().getEntity(entityData.get(OWNER));
        return owner instanceof Player player ? player : null;
    }

    public boolean isAttachedTo(Player player) {
        return entityData.get(OWNER) == player.getId();
    }

    public boolean isFlying() {
        return entityData.get(FLYING);
    }

    private InteractionHand hand() {
        return entityData.get(OFF_HAND) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    @Override
    public void tick() {
        super.tick();
        Player owner = getOwner();
        if (owner == null || !owner.isAlive() || owner.isSpectator()) {
            if (!level().isClientSide) discard();
            return;
        }
        ItemStack stack = owner.getItemInHand(hand());
        if (!(stack.getItem() instanceof HangGliderItem) || !HangGliderItem.isDeployed(stack)) {
            if (!level().isClientSide) discard();
            return;
        }

        boolean flying = !owner.onGround() && !owner.isInWater() && !owner.isSleeping() && !owner.isFallFlying();
        if (!level().isClientSide) {
            entityData.set(FLYING, flying);
            if (flying) applyFlight(owner, stack);
        }
        moveWithOwner(owner);
    }

    private void applyFlight(Player owner, ItemStack stack) {
        Vec3 motion = owner.getDeltaMovement();
        double targetY = owner.isShiftKeyDown() ? -0.176D : -0.052D;
        if (motion.y < targetY) {
            Vec3 look = owner.getLookAngle();
            Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
            double acceleration = owner.isShiftKeyDown() ? 0.10D : 0.03D;
            if (horizontal.lengthSqr() > 1.0E-4D) horizontal = horizontal.normalize().scale(acceleration);
            owner.setDeltaMovement(motion.x + horizontal.x, targetY, motion.z + horizontal.z);
            owner.fallDistance = 0.0F;
            owner.hurtMarked = true;
        }
        if (tickCount % 80 == 0 && owner instanceof ServerPlayer serverPlayer) {
            stack.hurtAndBreak(1, serverPlayer, serverPlayer.getEquipmentSlotForItem(stack));
        }
    }

    private void moveWithOwner(Player owner) {
        setPos(owner.getX(), owner.getY() + (isFlying() ? 2.1D : 1.25D), owner.getZ());
        // The wing is strapped to the player's body. Following head rotation made
        // client interpolation swing it around whenever the player looked sideways.
        yRotO = owner.yBodyRotO;
        setYRot(owner.yBodyRot);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
