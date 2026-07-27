package net.xuwu.openblocks_reborn;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.xuwu.openblocks_reborn.config.OpenBlocksConfig;
import net.xuwu.openblocks_reborn.event.CommonEvents;
import net.xuwu.openblocks_reborn.registry.ModBlocks;
import net.xuwu.openblocks_reborn.registry.ModBlockEntities;
import net.xuwu.openblocks_reborn.registry.ModCapabilities;
import net.xuwu.openblocks_reborn.registry.ModCreativeTabs;
import net.xuwu.openblocks_reborn.registry.ModItems;
import net.xuwu.openblocks_reborn.registry.ModMenus;
import net.xuwu.openblocks_reborn.registry.ModRecipes;
import net.xuwu.openblocks_reborn.registry.ModEntities;
import net.xuwu.openblocks_reborn.registry.ModFluids;
import net.xuwu.openblocks_reborn.network.ModNetworking;
import org.slf4j.Logger;

@Mod(OpenBlocksReborn.MOD_ID)
public final class OpenBlocksReborn {
    public static final String MOD_ID = "openblocks_reborn";
    public static final Logger LOGGER = LogUtils.getLogger();

    public OpenBlocksReborn(IEventBus modEventBus, ModContainer modContainer) {
        ModFluids.FLUID_TYPES.register(modEventBus);
        ModFluids.FLUIDS.register(modEventBus);
        ModFluids.BLOCKS.register(modEventBus);
        ModFluids.ITEMS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModRecipes.RECIPE_SERIALIZERS.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
        modEventBus.addListener(ModCapabilities::register);
        modEventBus.addListener(ModEntities::registerAttributes);
        modEventBus.addListener(ModNetworking::register);
        modContainer.registerConfig(ModConfig.Type.SERVER, OpenBlocksConfig.SERVER_SPEC);
        NeoForge.EVENT_BUS.register(new CommonEvents());

        LOGGER.info("Loading OpenBlocks Reborn for Minecraft 1.21.1");
    }
}
