package net.xuwu.openblocks_reborn.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.xuwu.openblocks_reborn.OpenBlocksReborn;
import net.xuwu.openblocks_reborn.menu.GoldenEyeMenu;
import net.xuwu.openblocks_reborn.menu.MachineMenu;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, OpenBlocksReborn.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> MACHINE =
            MENUS.register("machine", () -> IMenuTypeExtension.create(MachineMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<GoldenEyeMenu>> GOLDEN_EYE =
            MENUS.register("golden_eye", () -> IMenuTypeExtension.create(GoldenEyeMenu::new));

    private ModMenus() {
    }
}
