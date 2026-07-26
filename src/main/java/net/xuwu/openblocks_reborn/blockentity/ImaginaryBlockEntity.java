package net.xuwu.openblocks_reborn.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.xuwu.openblocks_reborn.registry.ModBlockEntities;

import javax.annotation.Nullable;

public class ImaginaryBlockEntity extends BlockEntity {
    @Nullable
    private Integer color;
    private boolean inverted;

    public ImaginaryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.IMAGINARY.get(), pos, state);
    }

    public void configure(@Nullable Integer color, boolean inverted) {
        this.color = color;
        this.inverted = inverted;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Nullable
    public Integer getColor() {
        return color;
    }

    public boolean isPencil() {
        return color == null;
    }

    public boolean isInverted() {
        return inverted;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        color = tag.contains("Color") ? tag.getInt("Color") : null;
        inverted = tag.getBoolean("Inverted");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (color != null) tag.putInt("Color", color);
        tag.putBoolean("Inverted", inverted);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
