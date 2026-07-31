package net.xuwu.openblocks_reborn.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
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
    public static final String OWNER_MINI_ME = "openblocks_reborn.mini_me";
    private static final EntityDataAccessor<Optional<UUID>> OWNER =
            SynchedEntityData.defineId(MiniMeEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> STAYING =
            SynchedEntityData.defineId(MiniMeEntity.class, EntityDataSerializers.BOOLEAN);

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

    public boolean isStaying() {
        return entityData.get(STAYING);
    }

    public void setStaying(boolean staying) {
        entityData.set(STAYING, staying);
        if (staying) getNavigation().stop();
    }

    public ServerPlayer getOwnerPlayer() {
        if (!(level() instanceof ServerLevel serverLevel)) return null;
        return getOwnerId().map(serverLevel.getServer().getPlayerList()::getPlayer).orElse(null);
    }

    public void claimOwnership(ServerLevel origin) {
        UUID ownerId = getOwnerId().orElse(null);
        if (ownerId == null) return;
        for (ServerLevel level : origin.getServer().getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof MiniMeEntity other && other != this
                        && other.getOwnerId().filter(ownerId::equals).isPresent()) {
                    other.discard();
                }
            }
        }
        ServerPlayer owner = origin.getServer().getPlayerList().getPlayer(ownerId);
        if (owner != null) owner.getPersistentData().putUUID(OWNER_MINI_ME, getUUID());
    }

    public static MiniMeEntity findOwned(ServerPlayer owner) {
        CompoundTag data = owner.getPersistentData();
        if (data.hasUUID(OWNER_MINI_ME)) {
            UUID entityId = data.getUUID(OWNER_MINI_ME);
            for (ServerLevel level : owner.getServer().getAllLevels()) {
                Entity entity = level.getEntity(entityId);
                if (entity instanceof MiniMeEntity miniMe
                        && miniMe.getOwnerId().filter(owner.getUUID()::equals).isPresent()) {
                    return miniMe;
                }
            }
            return null;
        }
        for (ServerLevel level : owner.getServer().getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof MiniMeEntity miniMe
                        && miniMe.getOwnerId().filter(owner.getUUID()::equals).isPresent()) {
                    data.putUUID(OWNER_MINI_ME, miniMe.getUUID());
                    return miniMe;
                }
            }
        }
        return null;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER, Optional.empty());
        builder.define(STAYING, false);
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
        if (!(level() instanceof ServerLevel serverLevel)) return;
        ServerPlayer owner = getOwnerPlayer();
        if (owner != null) {
            CompoundTag data = owner.getPersistentData();
            if (data.hasUUID(OWNER_MINI_ME)
                    && !data.getUUID(OWNER_MINI_ME).equals(getUUID())) {
                discard();
                return;
            }
            if (!data.hasUUID(OWNER_MINI_ME)) {
                data.putUUID(OWNER_MINI_ME, getUUID());
            }
        }
        if (isStaying()) {
            getNavigation().stop();
            setDeltaMovement(0.0D, getDeltaMovement().y, 0.0D);
            return;
        }
        if (owner == null || owner.serverLevel() != serverLevel || tickCount % 10 != 0) return;
        double distance = distanceToSqr(owner);
        if (distance > 32.0D * 32.0D) {
            teleportTo(owner.getX(), owner.getY(), owner.getZ());
        } else if (distance > 4.0D * 4.0D) {
            getNavigation().moveTo(owner, 1.1D);
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!getOwnerId().filter(player.getUUID()::equals).isPresent()) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide) {
            setStaying(!isStaying());
            player.displayClientMessage(Component.translatable(isStaying()
                    ? "message.openblocks_reborn.mini_me.staying"
                    : "message.openblocks_reborn.mini_me.following"), true);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        getOwnerId().ifPresent(uuid -> tag.putUUID("Owner", uuid));
        tag.putBoolean("Staying", isStaying());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) setOwner(tag.getUUID("Owner"));
        setStaying(tag.getBoolean("Staying"));
    }
}
