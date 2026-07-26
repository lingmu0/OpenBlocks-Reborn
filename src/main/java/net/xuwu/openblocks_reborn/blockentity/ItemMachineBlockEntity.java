package net.xuwu.openblocks_reborn.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;
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

    @Override
    public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
            net.minecraftforge.common.capabilities.Capability<T> capability,
            @javax.annotation.Nullable net.minecraft.core.Direction side) {
        if (capability == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER) {
            return net.minecraftforge.common.util.LazyOptional
                    .<net.minecraftforge.items.IItemHandler>of(() -> inventory).cast();
        }
        return super.getCapability(capability, side);
    }

    public BlockPos getCannonTarget() {
        return cannonTarget;
    }

    /** Item Dropper velocity in hundredths of a block per tick, matching the old 0.0-4.0 slider. */
    public int getItemSpeed() {
        return itemSpeed;
    }

    public void setItemSpeed(int itemSpeed) {
        this.itemSpeed = net.minecraft.util.Mth.clamp(itemSpeed, 0, 400);
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
    public void load(CompoundTag tag) {
        super.load(tag);
        inventory.deserializeNBT(tag.getCompound("Inventory"));
        cannonTarget = tag.contains("CannonTarget") ? BlockPos.of(tag.getLong("CannonTarget")) : null;
        itemSpeed = net.minecraft.util.Mth.clamp(tag.getInt("ItemSpeed"), 0, 400);
        useRedstoneStrength = tag.getBoolean("UseRedstoneStrength");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", inventory.serializeNBT());
        if (cannonTarget != null) tag.putLong("CannonTarget", cannonTarget.asLong());
        tag.putInt("ItemSpeed", itemSpeed);
        tag.putBoolean("UseRedstoneStrength", useRedstoneStrength);
    }
}
