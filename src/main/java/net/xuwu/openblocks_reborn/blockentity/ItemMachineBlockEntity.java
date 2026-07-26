package net.xuwu.openblocks_reborn.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.xuwu.openblocks_reborn.registry.ModBlockEntities;

public class ItemMachineBlockEntity extends BlockEntity {
    private BlockPos cannonTarget;
    private int itemSpeed;
    private boolean useRedstoneStrength;
    private final ItemStackHandler inventory = new ItemStackHandler(9) {
        @Override
        protected void onContentsChanged(int slot) {
            ItemMachineBlockEntity.this.setChanged();
        }
    };

    public ItemMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ITEM_MACHINE.get(), pos, state);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public BlockPos getCannonTarget() {
        return cannonTarget;
    }

    /** Item Dropper velocity in hundredths of a block per tick, matching the old 0.0-4.0 slider. */
    public int getItemSpeed() {
        return itemSpeed;
    }

    public void setItemSpeed(int itemSpeed) {
        this.itemSpeed = Math.clamp(itemSpeed, 0, 400);
        setChanged();
    }

    public boolean usesRedstoneStrength() {
        return useRedstoneStrength;
    }

    public void toggleRedstoneStrength() {
        useRedstoneStrength = !useRedstoneStrength;
        setChanged();
    }

    public void setCannonTarget(BlockPos target) {
        cannonTarget = target.immutable();
        setChanged();
        if (level != null && getBlockState().hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING)) {
            int dx = target.getX() - worldPosition.getX();
            int dz = target.getZ() - worldPosition.getZ();
            net.minecraft.core.Direction facing = Math.abs(dx) > Math.abs(dz)
                    ? (dx >= 0 ? net.minecraft.core.Direction.EAST : net.minecraft.core.Direction.WEST)
                    : (dz >= 0 ? net.minecraft.core.Direction.SOUTH : net.minecraft.core.Direction.NORTH);
            level.setBlock(worldPosition, getBlockState().setValue(
                    net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, facing),
                    net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        cannonTarget = tag.contains("CannonTarget") ? BlockPos.of(tag.getLong("CannonTarget")) : null;
        itemSpeed = Math.clamp(tag.getInt("ItemSpeed"), 0, 400);
        useRedstoneStrength = tag.getBoolean("UseRedstoneStrength");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", inventory.serializeNBT(registries));
        if (cannonTarget != null) tag.putLong("CannonTarget", cannonTarget.asLong());
        tag.putInt("ItemSpeed", itemSpeed);
        tag.putBoolean("UseRedstoneStrength", useRedstoneStrength);
    }
}
