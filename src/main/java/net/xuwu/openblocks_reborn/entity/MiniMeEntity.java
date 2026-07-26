package net.xuwu.openblocks_reborn.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.UUID;

public class MiniMeEntity extends PathfinderMob {
    private static final EntityDataAccessor<Optional<UUID>> OWNER =
            SynchedEntityData.defineId(MiniMeEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    public MiniMeEntity(EntityType<? extends MiniMeEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public void setOwner(UUID owner) {
        entityData.set(OWNER, Optional.of(owner));
    }

    public Optional<UUID> getOwnerId() {
        return entityData.get(OWNER);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER, Optional.empty());
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(5, new RandomStrollGoal(this, 0.8D));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel serverLevel) || tickCount % 10 != 0) return;
        getOwnerId().map(serverLevel::getPlayerByUUID).ifPresent(player -> {
            double distance = distanceToSqr(player);
            if (distance > 32.0D * 32.0D) {
                teleportTo(player.getX(), player.getY(), player.getZ());
            } else if (distance > 4.0D * 4.0D) {
                getNavigation().moveTo(player, 1.1D);
            }
        });
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        getOwnerId().ifPresent(uuid -> tag.putUUID("Owner", uuid));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) setOwner(tag.getUUID("Owner"));
    }
}
