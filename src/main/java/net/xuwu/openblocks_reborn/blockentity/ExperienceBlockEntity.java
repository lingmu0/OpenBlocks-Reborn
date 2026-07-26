package net.xuwu.openblocks_reborn.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.IItemHandler;
import net.xuwu.openblocks_reborn.registry.ModCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.xuwu.openblocks_reborn.registry.ModBlockEntities;
import net.xuwu.openblocks_reborn.registry.ModFluids;
import net.xuwu.openblocks_reborn.registry.ModBlocks;
import net.xuwu.openblocks_reborn.block.ExperienceMachineBlock;

public class ExperienceBlockEntity extends BlockEntity {
    public static final int CAPACITY = 16_000;
    public static final int DRAIN_AND_SHOWER_CAPACITY = 1_000;
    public static final int BOTTLING_TICKS = 40;
    private final FluidTank tank;
    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            ExperienceBlockEntity.this.setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && stack.is(Items.GLASS_BOTTLE);
        }
    };
    private final int[] sideMasks = {0x3F, 0x3F, 0x3F};
    private int automaticChannels;
    private int bottlingProgress;

    public ExperienceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EXPERIENCE_MACHINE.get(), pos, state);
        int capacity = state.getBlock() instanceof ExperienceMachineBlock machine
                && machine.mode() != ExperienceMachineBlock.Mode.BOTTLER
                ? DRAIN_AND_SHOWER_CAPACITY : CAPACITY;
        tank = new FluidTank(capacity, stack -> stack.getFluid() == ModFluids.XP_JUICE.get()) {
            @Override
            protected void onContentsChanged() {
                ExperienceBlockEntity.this.setChanged();
                if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        };
    }

    public int getExperience() {
        return ModFluids.fluidToXp(tank.getFluidAmount());
    }

    public FluidTank getTank() {
        return tank;
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    @Override
    public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
            net.minecraftforge.common.capabilities.Capability<T> capability,
            @javax.annotation.Nullable Direction side) {
        if (capability == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER) {
            return net.minecraftforge.common.util.LazyOptional
                    .<IItemHandler>of(() -> getItemHandler(side)).cast();
        }
        if (capability == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER) {
            return net.minecraftforge.common.util.LazyOptional
                    .<IFluidHandler>of(() -> new ConfiguredFluidHandler(
                            tank, allowsFluidInput(side), true)).cast();
        }
        return super.getCapability(capability, side);
    }

    public boolean supportsSideConfiguration() {
        return getBlockState().is(ModBlocks.XP_BOTTLER.get());
    }

    public int getSideMask(int channel) {
        return channel >= 0 && channel < sideMasks.length ? sideMasks[channel] : 0;
    }

    public int getSideConfiguration(int channel) {
        return getSideMask(channel) | (isAutomatic(channel) ? 0x40 : 0);
    }

    public boolean isAutomatic(int channel) {
        return channel >= 0 && channel < sideMasks.length && (automaticChannels & 1 << channel) != 0;
    }

    public boolean toggleSide(int channel, Direction direction) {
        if (!supportsSideConfiguration() || channel < 0 || channel >= sideMasks.length) return false;
        sideMasks[channel] ^= 1 << direction.get3DDataValue();
        setChanged();
        return true;
    }

    public boolean toggleAutomatic(int channel) {
        if (!supportsSideConfiguration() || channel < 0 || channel >= sideMasks.length) return false;
        automaticChannels ^= 1 << channel;
        setChanged();
        return true;
    }

    public IItemHandler getItemHandler(Direction side) {
        if (!supportsSideConfiguration() || side == null) return inventory;
        int sideBit = 1 << side.get3DDataValue();
        return new ConfiguredItemHandler(inventory,
                slot -> slot == 0 && (sideMasks[0] & sideBit) != 0,
                slot -> slot == 1 && (sideMasks[1] & sideBit) != 0);
    }

    public boolean allowsFluidInput(Direction side) {
        return !supportsSideConfiguration() || side == null
                || (sideMasks[2] & 1 << side.get3DDataValue()) != 0;
    }

    public int getBottlingProgress() {
        return bottlingProgress;
    }

    public void tickBottler(ServerLevel level, BlockPos pos) {
        if (!supportsSideConfiguration()) return;
        performAutomaticTransfers(level, pos);
        if (!canBottleExperience()) {
            if (bottlingProgress != 0) {
                bottlingProgress = 0;
                setChanged();
            }
            return;
        }
        bottlingProgress++;
        if (bottlingProgress < BOTTLING_TICKS) {
            if (bottlingProgress % 5 == 0) setChanged();
            return;
        }
        bottlingProgress = 0;
        if (bottleExperience()) {
            level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.5F, 1.25F);
        }
        setChanged();
    }

    private boolean canBottleExperience() {
        ItemStack bottles = inventory.getStackInSlot(0);
        ItemStack output = inventory.getStackInSlot(1);
        return bottles.is(Items.GLASS_BOTTLE) && getExperience() >= ModFluids.XP_PER_BOTTLE
                && (output.isEmpty() || output.is(Items.EXPERIENCE_BOTTLE)
                && output.getCount() < output.getMaxStackSize());
    }

    public boolean bottleExperience() {
        if (!canBottleExperience()) return false;
        ItemStack output = inventory.getStackInSlot(1);
        inventory.extractItem(0, 1, false);
        if (output.isEmpty()) inventory.setStackInSlot(1, new ItemStack(Items.EXPERIENCE_BOTTLE));
        else {
            ItemStack grown = output.copy();
            grown.grow(1);
            inventory.setStackInSlot(1, grown);
        }
        tank.drain(ModFluids.xpToFluid(ModFluids.XP_PER_BOTTLE), IFluidHandler.FluidAction.EXECUTE);
        setChanged();
        return true;
    }

    private void performAutomaticTransfers(ServerLevel level, BlockPos pos) {
        if (isAutomatic(0)) pullItemIntoSlot(level, pos, 0, sideMasks[0]);
        if (isAutomatic(1)) pushItemFromSlot(level, pos, 1, sideMasks[1]);
        if (isAutomatic(2)) pullExperience(level, pos, sideMasks[2]);
    }

    private void pullItemIntoSlot(ServerLevel level, BlockPos pos, int targetSlot, int sideMask) {
        for (Direction direction : Direction.values()) {
            if ((sideMask & 1 << direction.get3DDataValue()) == 0) continue;
            IItemHandler source = ModCapabilities.item(level, pos.relative(direction), direction.getOpposite());
            if (source == null) continue;
            for (int sourceSlot = 0; sourceSlot < source.getSlots(); sourceSlot++) {
                ItemStack offered = source.extractItem(sourceSlot, 64, true);
                if (offered.isEmpty()) continue;
                ItemStack remainder = inventory.insertItem(targetSlot, offered, true);
                int accepted = offered.getCount() - remainder.getCount();
                if (accepted <= 0) continue;
                ItemStack extracted = source.extractItem(sourceSlot, accepted, false);
                ItemStack leftover = inventory.insertItem(targetSlot, extracted, false);
                if (!leftover.isEmpty()) source.insertItem(sourceSlot, leftover, false);
                return;
            }
        }
    }

    private void pushItemFromSlot(ServerLevel level, BlockPos pos, int sourceSlot, int sideMask) {
        ItemStack offered = inventory.extractItem(sourceSlot, 64, true);
        if (offered.isEmpty()) return;
        for (Direction direction : Direction.values()) {
            if ((sideMask & 1 << direction.get3DDataValue()) == 0) continue;
            IItemHandler destination = ModCapabilities.item(level, pos.relative(direction), direction.getOpposite());
            if (destination == null) continue;
            for (int targetSlot = 0; targetSlot < destination.getSlots(); targetSlot++) {
                ItemStack remainder = destination.insertItem(targetSlot, offered, true);
                int accepted = offered.getCount() - remainder.getCount();
                if (accepted <= 0) continue;
                ItemStack extracted = inventory.extractItem(sourceSlot, accepted, false);
                ItemStack leftover = destination.insertItem(targetSlot, extracted, false);
                if (!leftover.isEmpty()) inventory.insertItem(sourceSlot, leftover, false);
                return;
            }
        }
    }

    private void pullExperience(ServerLevel level, BlockPos pos, int sideMask) {
        if (tank.getSpace() <= 0) return;
        for (Direction direction : Direction.values()) {
            if ((sideMask & 1 << direction.get3DDataValue()) == 0) continue;
            IFluidHandler source = ModCapabilities.fluid(level, pos.relative(direction), direction.getOpposite());
            if (source == null) continue;
            FluidStack offered = source.drain(Math.min(250, tank.getSpace()),
                    IFluidHandler.FluidAction.SIMULATE);
            if (offered.isEmpty() || offered.getFluid() != ModFluids.XP_JUICE.get()) continue;
            int accepted = tank.fill(offered, IFluidHandler.FluidAction.SIMULATE);
            if (accepted <= 0) continue;
            FluidStack drained = source.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
            tank.fill(drained, IFluidHandler.FluidAction.EXECUTE);
            return;
        }
    }

    public void addExperience(int amount) {
        if (amount > 0) {
            tryAddExperience(amount);
        } else if (amount < 0) {
            tank.drain(ModFluids.xpToFluid(-amount), IFluidHandler.FluidAction.EXECUTE);
        }
    }

    /** Returns the whole number of XP points accepted without losing conversion remainders. */
    public int tryAddExperience(int amount) {
        if (amount <= 0) return 0;
        int accepted = tank.fill(new FluidStack(ModFluids.XP_JUICE.get(), ModFluids.xpToFluid(amount)), IFluidHandler.FluidAction.EXECUTE);
        return ModFluids.fluidToXp(accepted);
    }

    public boolean consumeExperience(int amount) {
        int fluid = ModFluids.xpToFluid(amount);
        if (amount < 0 || tank.getFluidAmount() < fluid) return false;
        tank.drain(fluid, IFluidHandler.FluidAction.EXECUTE);
        return true;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Tank")) {
            tank.readFromNBT(tag.getCompound("Tank"));
        } else if (tag.contains("Experience")) {
            addExperience(tag.getInt("Experience"));
        }
        inventory.deserializeNBT(tag.getCompound("Inventory"));
        if (tag.contains("SideMasks")) {
            int[] savedMasks = tag.getIntArray("SideMasks");
            for (int index = 0; index < Math.min(savedMasks.length, sideMasks.length); index++) {
                sideMasks[index] = savedMasks[index] & 0x3F;
            }
        }
        automaticChannels = tag.getInt("AutomaticChannels") & 0x7;
        bottlingProgress = net.minecraft.util.Mth.clamp(tag.getInt("BottlingProgress"), 0, BOTTLING_TICKS - 1);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Tank", tank.writeToNBT(new CompoundTag()));
        tag.put("Inventory", inventory.serializeNBT());
        tag.putIntArray("SideMasks", sideMasks);
        tag.putInt("AutomaticChannels", automaticChannels);
        tag.putInt("BottlingProgress", bottlingProgress);
    }
}
