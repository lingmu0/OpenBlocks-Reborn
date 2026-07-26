package net.xuwu.openblocks_reborn.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.xuwu.openblocks_reborn.OpenBlocksReborn;
import net.xuwu.openblocks_reborn.item.ImaginaryItem;
import net.xuwu.openblocks_reborn.item.SkyBlockItem;
import net.xuwu.openblocks_reborn.item.GlassesItem;
import net.minecraft.world.item.DyeColor;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, OpenBlocksReborn.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.openblocks_reborn"))
            .icon(() -> ModItems.HANG_GLIDER.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                ModItems.BLOCK_ITEMS.values().forEach(item -> output.accept(item.get()));
                output.accept(SkyBlockItem.create(true));
                for (DyeColor color : DyeColor.values()) {
                    output.accept(ImaginaryItem.createCrayon(color.getTextureDiffuseColor()));
                    output.accept(GlassesItem.createCrayonGlasses(color.getTextureDiffuseColor()));
                }
                ModItems.ALL_ITEMS.values().forEach(item -> output.accept(item.get()));
                output.accept(ModFluids.XP_BUCKET.get());
            })
            .build());

    private ModCreativeTabs() {
    }
}
