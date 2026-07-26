package net.xuwu.openblocks_reborn.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.xuwu.openblocks_reborn.registry.ModBlockEntities;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

public class TankBlockEntity extends BlockEntity {
    public static final int CAPACITY = 16_000;
    private final FluidTank tank = new FluidTank(CAPACITY) {
        @Override
        protected void onContentsChanged() {
            TankBlockEntity.this.setChanged();
            if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    };

    public TankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TANK.get(), pos, state);
    }

    public FluidTank getTank() {
        return tank;
    }

    @Override
    public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
            net.minecraftforge.common.capabilities.Capability<T> capability,
            @javax.annotation.Nullable net.minecraft.core.Direction side) {
        if (capability == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER) {
            return net.minecraftforge.common.util.LazyOptional
                    .<net.minecraftforge.fluids.capability.IFluidHandler>of(() -> tank).cast();
        }
        return super.getCapability(capability, side);
    }

    public ItemStack toItemStack() {
        ItemStack result = new ItemStack(getBlockState().getBlock().asItem());
        CompoundTag data = new CompoundTag();
        data.put("Tank", tank.writeToNBT(new CompoundTag()));
        BlockItem.setBlockEntityData(result, ModBlockEntities.TANK.get(), data);
        return result;
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, TankBlockEntity tankEntity) {
        if (level.getGameTime() % 5L != 0L || tankEntity.tank.isEmpty()) return;

        // Liquid settles into tanks below first, matching the stacked-tank behaviour of the original mod.
        if (level.getBlockEntity(pos.below()) instanceof TankBlockEntity below) {
            transfer(tankEntity, below, 1000);
        }

        // Horizontally connected tanks share their contents and settle toward equal levels.
        for (Direction direction : new Direction[] { Direction.NORTH, Direction.WEST, Direction.SOUTH, Direction.EAST }) {
            if (!(level.getBlockEntity(pos.relative(direction)) instanceof TankBlockEntity other)) continue;
            if (!compatible(tankEntity.tank.getFluid(), other.tank.getFluid())) continue;
            int difference = tankEntity.tank.getFluidAmount() - other.tank.getFluidAmount();
            if (difference > 1) transfer(tankEntity, other, difference / 2);
        }
    }

    private static boolean compatible(FluidStack first, FluidStack second) {
        return first.isEmpty() || second.isEmpty() || first.isFluidEqual(second);
    }

    private static void transfer(TankBlockEntity from, TankBlockEntity to, int maximum) {
        if (maximum <= 0 || from.tank.isEmpty() || !compatible(from.tank.getFluid(), to.tank.getFluid())) return;
        FluidStack offered = from.tank.drain(maximum, IFluidHandler.FluidAction.SIMULATE);
        int accepted = to.tank.fill(offered, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) return;
        FluidStack moved = from.tank.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
        to.tank.fill(moved, IFluidHandler.FluidAction.EXECUTE);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        tank.readFromNBT(tag.getCompound("Tank"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Tank", tank.writeToNBT(new CompoundTag()));
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
