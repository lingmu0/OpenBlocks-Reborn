package net.xuwu.openblocks_reborn.registry;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public final class ModCapabilities {
    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.TANK.get(), (tank, side) -> tank.getTank());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.VACUUM_HOPPER.get(), (hopper, side) -> hopper.getInventory());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.VACUUM_HOPPER.get(), (hopper, side) -> hopper.getExperienceTank());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.ITEM_MACHINE.get(), (machine, side) -> machine.getInventory());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.UTILITY_MACHINE.get(),
                (machine, side) -> machine.getItemHandler(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.UTILITY_MACHINE.get(),
                (machine, side) -> machine.exposesFluidTank() && machine.allowsExperienceInput(side)
                        ? machine.getFluidTank() : null);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.EXPERIENCE_MACHINE.get(),
                (machine, side) -> machine.getItemHandler(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.EXPERIENCE_MACHINE.get(),
                (machine, side) -> machine.allowsFluidInput(side) ? machine.getTank() : null);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.BIG_BUTTON.get(), (button, side) -> button.getInventory());
    }

    private ModCapabilities() {
    }
}
