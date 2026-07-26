package net.xuwu.openblocks_reborn.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.xuwu.openblocks_reborn.registry.ModBlockEntities;
import net.xuwu.openblocks_reborn.registry.ModFluids;
import net.minecraft.core.Direction;
import net.xuwu.openblocks_reborn.block.VacuumHopperBlock;

public class VacuumHopperBlockEntity extends BlockEntity {
    public static final int FLUID_CAPACITY = 4_000;
    private final FluidTank experienceTank = new FluidTank(FLUID_CAPACITY,
            stack -> stack.is(ModFluids.XP_JUICE.get())) {
        @Override
        protected void onContentsChanged() {
            VacuumHopperBlockEntity.this.setChanged();
            if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    };
    private final ItemStackHandler inventory = new ItemStackHandler(10) {
        @Override
        protected void onContentsChanged(int slot) {
            VacuumHopperBlockEntity.this.setChanged();
        }
    };
    private int itemOutputMask;
    private int experienceOutputMask;

    public VacuumHopperBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VACUUM_HOPPER.get(), pos, state);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public FluidTank getExperienceTank() {
        return experienceTank;
    }

    public int getItemOutputMask() {
        return itemOutputMask;
    }

    public int getExperienceOutputMask() {
        return experienceOutputMask;
    }

    public void toggleItemOutput(Direction direction) {
        itemOutputMask ^= 1 << direction.get3DDataValue();
        syncOutputState();
    }

    public void toggleExperienceOutput(Direction direction) {
        experienceOutputMask ^= 1 << direction.get3DDataValue();
        syncOutputState();
    }

    private void syncOutputState() {
        setChanged();
        if (level == null || level.isClientSide || !(getBlockState().getBlock() instanceof VacuumHopperBlock)) return;
        BlockState updated = VacuumHopperBlock.applyOutputState(getBlockState(), itemOutputMask, experienceOutputMask);
        if (updated != getBlockState()) level.setBlock(worldPosition, updated, 3);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        syncOutputState();
    }

    public int takeExperience() {
        int result = ModFluids.fluidToXp(experienceTank.getFluidAmount());
        experienceTank.drain(ModFluids.xpToFluid(result), IFluidHandler.FluidAction.EXECUTE);
        return result;
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, VacuumHopperBlockEntity hopper) {
        if (level.getGameTime() % 4L != 0L || level.hasNeighborSignal(pos)) return;
        AABB area = new AABB(pos).inflate(4.0D);
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, area, item -> item.isAlive() && !item.getItem().isEmpty())) {
            ItemStack remainder = entity.getItem().copy();
            for (int slot = 0; slot < hopper.inventory.getSlots() && !remainder.isEmpty(); slot++) {
                remainder = hopper.inventory.insertItem(slot, remainder, false);
            }
            if (remainder.isEmpty()) entity.discard();
            else entity.setItem(remainder);
        }
        for (ExperienceOrb orb : level.getEntitiesOfClass(ExperienceOrb.class, area, ExperienceOrb::isAlive)) {
            FluidStack fluid = new FluidStack(ModFluids.XP_JUICE.get(), ModFluids.xpToFluid(orb.getValue()));
            if (hopper.experienceTank.fill(fluid, IFluidHandler.FluidAction.SIMULATE) == fluid.getAmount()) {
                hopper.experienceTank.fill(fluid, IFluidHandler.FluidAction.EXECUTE);
                orb.discard();
            }
        }
        if (level.getGameTime() % 20L == 0L) hopper.outputToNeighbors(level, pos);
    }

    private void outputToNeighbors(ServerLevel level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            boolean outputExperience = (experienceOutputMask & 1 << direction.get3DDataValue()) != 0;
            var fluidHandler = outputExperience ? level.getCapability(Capabilities.FluidHandler.BLOCK,
                    pos.relative(direction), direction.getOpposite()) : null;
            if (fluidHandler != null && !experienceTank.isEmpty()) {
                FluidStack offered = experienceTank.drain(100, IFluidHandler.FluidAction.SIMULATE);
                int accepted = fluidHandler.fill(offered, IFluidHandler.FluidAction.SIMULATE);
                if (accepted > 0) fluidHandler.fill(experienceTank.drain(accepted, IFluidHandler.FluidAction.EXECUTE),
                        IFluidHandler.FluidAction.EXECUTE);
            }
            if ((itemOutputMask & 1 << direction.get3DDataValue()) == 0) continue;
            var itemHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos.relative(direction), direction.getOpposite());
            if (itemHandler == null) continue;
            for (int slot = 0; slot < inventory.getSlots(); slot++) {
                ItemStack available = inventory.extractItem(slot, 1, true);
                if (available.isEmpty()) continue;
                ItemStack remainder = ItemHandlerHelper.insertItem(itemHandler, available, false);
                if (remainder.isEmpty()) inventory.extractItem(slot, 1, false);
                break;
            }
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        if (tag.contains("ExperienceTank")) {
            experienceTank.readFromNBT(registries, tag.getCompound("ExperienceTank"));
        } else if (tag.contains("Experience")) {
            experienceTank.fill(new FluidStack(ModFluids.XP_JUICE.get(), ModFluids.xpToFluid(tag.getInt("Experience"))),
                    IFluidHandler.FluidAction.EXECUTE);
        }
        itemOutputMask = tag.getInt("ItemOutputMask") & 0x3F;
        experienceOutputMask = tag.getInt("ExperienceOutputMask") & 0x3F;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", inventory.serializeNBT(registries));
        tag.put("ExperienceTank", experienceTank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("ItemOutputMask", itemOutputMask);
        tag.putInt("ExperienceOutputMask", experienceOutputMask);
    }
}
