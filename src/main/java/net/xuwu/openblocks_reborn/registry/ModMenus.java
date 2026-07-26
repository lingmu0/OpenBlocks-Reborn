package net.xuwu.openblocks_reborn.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.xuwu.openblocks_reborn.OpenBlocksReborn;
import net.xuwu.openblocks_reborn.menu.GoldenEyeMenu;
import net.xuwu.openblocks_reborn.menu.MachineMenu;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, OpenBlocksReborn.MOD_ID);

    public static final RegistryObject<MenuType<MachineMenu>> MACHINE =
            MENUS.register("machine", () -> IForgeMenuType.create(MachineMenu::new));
    public static final RegistryObject<MenuType<GoldenEyeMenu>> GOLDEN_EYE =
            MENUS.register("golden_eye", () -> IForgeMenuType.create(GoldenEyeMenu::new));

    private ModMenus() {
    }
}
