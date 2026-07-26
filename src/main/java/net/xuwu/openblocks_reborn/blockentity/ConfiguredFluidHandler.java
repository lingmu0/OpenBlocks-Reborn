package net.xuwu.openblocks_reborn.blockentity;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

/** Side-aware view over a tank without duplicating its storage. */
final class ConfiguredFluidHandler implements IFluidHandler {
    private final IFluidHandler delegate;
    private final boolean canFill;
    private final boolean canDrain;

    ConfiguredFluidHandler(IFluidHandler delegate, boolean canFill, boolean canDrain) {
        this.delegate = delegate;
        this.canFill = canFill;
        this.canDrain = canDrain;
    }

    @Override
    public int getTanks() {
        return delegate.getTanks();
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return delegate.getFluidInTank(tank);
    }

    @Override
    public int getTankCapacity(int tank) {
        return delegate.getTankCapacity(tank);
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return canFill && delegate.isFluidValid(tank, stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return canFill ? delegate.fill(resource, action) : 0;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        return canDrain ? delegate.drain(resource, action) : FluidStack.EMPTY;
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return canDrain ? delegate.drain(maxDrain, action) : FluidStack.EMPTY;
    }
}
