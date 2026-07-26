package net.xuwu.openblocks_reborn.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.xuwu.openblocks_reborn.registry.ModBlockEntities;
import net.xuwu.openblocks_reborn.registry.ModBlocks;

public class TrophyBlockEntity extends BlockEntity {
    private ResourceLocation entityType = ResourceLocation.withDefaultNamespace("pig");
    private transient Entity displayEntity;

    public TrophyBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TROPHY.get(), pos, state);
    }

    public void setEntityType(EntityType<?> type) {
        entityType = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        displayEntity = null;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    public ResourceLocation getEntityTypeId() {
        return entityType;
    }

    public ItemStack toItemStack() {
        return createItem(entityType);
    }

    public static ItemStack createItem(ResourceLocation entityType) {
        ItemStack result = new ItemStack(ModBlocks.TROPHY.get().asItem());
        CompoundTag data = new CompoundTag();
        data.putString("EntityType", entityType.toString());
        BlockItem.setBlockEntityData(result, ModBlockEntities.TROPHY.get(), data);
        return result;
    }

    public Entity getOrCreateDisplayEntity() {
        if (displayEntity == null && level != null) {
            displayEntity = BuiltInRegistries.ENTITY_TYPE.getOptional(entityType).map(type -> type.create(level)).orElse(null);
        }
        return displayEntity;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        ResourceLocation parsed = ResourceLocation.tryParse(tag.getString("EntityType"));
        if (parsed != null) entityType = parsed;
        displayEntity = null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("EntityType", entityType.toString());
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
