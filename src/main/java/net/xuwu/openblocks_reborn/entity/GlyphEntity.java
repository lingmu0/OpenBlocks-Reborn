package net.xuwu.openblocks_reborn.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xuwu.openblocks_reborn.item.GlyphItem;
import net.xuwu.openblocks_reborn.registry.ModEntities;

import javax.annotation.Nullable;

/** A half-block-wide character that can be positioned freely on a solid wall face. */
public class GlyphEntity extends HangingEntity {
    private static final EntityDataAccessor<Integer> CHARACTER = SynchedEntityData.defineId(
            GlyphEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> OFFSET_X = SynchedEntityData.defineId(
            GlyphEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> OFFSET_Y = SynchedEntityData.defineId(
            GlyphEntity.class, EntityDataSerializers.INT);

    public GlyphEntity(EntityType<? extends GlyphEntity> type, Level level) {
        super(type, level);
    }

    public GlyphEntity(Level level, BlockPos pos, Direction direction, int character, int offsetX, int offsetY) {
        super(ModEntities.GLYPH.get(), level, pos);
        setCharacter(character);
        entityData.set(OFFSET_X, net.minecraft.util.Mth.clamp(offsetX, 0, 16));
        entityData.set(OFFSET_Y, net.minecraft.util.Mth.clamp(offsetY, 0, 16));
        setDirection(direction);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(CHARACTER, (int)'?');
        entityData.define(OFFSET_X, 8);
        entityData.define(OFFSET_Y, 8);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        if (OFFSET_X.equals(key) || OFFSET_Y.equals(key)) recalculateBoundingBox();
        super.onSyncedDataUpdated(key);
    }

    public int getCharacter() {
        return entityData.get(CHARACTER);
    }

    public void setCharacter(int character) {
        entityData.set(CHARACTER, GlyphItem.sanitizeCharacter(character));
    }

    protected AABB calculateBoundingBox(BlockPos pos, Direction direction) {
        Vec3 center = Vec3.atCenterOf(pos).relative(direction, -0.46875D);
        Direction left = direction.getCounterClockWise();
        center = center.relative(left, (entityData.get(OFFSET_X) - 8) / 16.0D)
                .relative(Direction.UP, (entityData.get(OFFSET_Y) - 8) / 16.0D);
        double widthX = direction.getAxis() == Direction.Axis.X ? 0.0625D : 0.5D;
        double widthZ = direction.getAxis() == Direction.Axis.Z ? 0.0625D : 0.5D;
        return AABB.ofSize(center, widthX, 0.5D, widthZ);
    }

    @Override
    public int getWidth() {
        return 8;
    }

    @Override
    public int getHeight() {
        return 8;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Character", getCharacter());
        tag.putInt("OffsetX", entityData.get(OFFSET_X));
        tag.putInt("OffsetY", entityData.get(OFFSET_Y));
        tag.putByte("Facing", (byte)direction.get2DDataValue());
        super.addAdditionalSaveData(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        setCharacter(tag.getInt("Character"));
        entityData.set(OFFSET_X, net.minecraft.util.Mth.clamp(tag.getInt("OffsetX"), 0, 16));
        entityData.set(OFFSET_Y, net.minecraft.util.Mth.clamp(tag.getInt("OffsetY"), 0, 16));
        direction = Direction.from2DDataValue(tag.getByte("Facing"));
        super.readAdditionalSaveData(tag);
        setDirection(direction);
    }

    @Override
    public void dropItem(@Nullable Entity brokenEntity) {
        if (!level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) return;
        playSound(SoundEvents.ITEM_FRAME_BREAK, 1.0F, 1.0F);
        if (brokenEntity instanceof Player player && player.getAbilities().instabuild) return;
        spawnAtLocation(GlyphItem.createStack(getCharacter()));
    }

    @Override
    public void playPlacementSound() {
        playSound(SoundEvents.ITEM_FRAME_PLACE, 1.0F, 1.0F);
    }

    @Override
    public void moveTo(double x, double y, double z, float yaw, float pitch) {
        setPos(x, y, z);
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot,
                       int steps, boolean teleport) {
        setPos(x, y, z);
    }

    @Override
    public Vec3 trackingPosition() {
        return Vec3.atLowerCornerOf(pos);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this, direction.get3DDataValue(), getPos());
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        setDirection(Direction.from3DDataValue(packet.getData()));
    }

    @Override
    public ItemStack getPickResult() {
        return GlyphItem.createStack(getCharacter());
    }
}
