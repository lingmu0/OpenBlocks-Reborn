package net.xuwu.openblocks_reborn.mixin.client;

import net.minecraft.client.gui.components.Checkbox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Equivalent to ElevatorMod's Checkbox access widener, without invoking the change callback. */
@Mixin(Checkbox.class)
public interface CheckboxAccessor {
    @Accessor("selected")
    void openblocks$setSelected(boolean selected);
}
