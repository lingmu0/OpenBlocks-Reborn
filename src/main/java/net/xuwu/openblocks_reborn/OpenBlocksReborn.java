package net.xuwu.openblocks_reborn;

import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.xuwu.openblocks_reborn.config.OpenBlocksConfig;
import net.xuwu.openblocks_reborn.event.CommonEvents;
import net.xuwu.openblocks_reborn.registry.ModBlocks;
import net.xuwu.openblocks_reborn.registry.ModBlockEntities;
import net.xuwu.openblocks_reborn.registry.ModCreativeTabs;
import net.xuwu.openblocks_reborn.registry.ModItems;
import net.xuwu.openblocks_reborn.registry.ModMenus;
import net.xuwu.openblocks_reborn.registry.ModRecipes;
import net.xuwu.openblocks_reborn.registry.ModEntities;
import net.xuwu.openblocks_reborn.registry.ModFluids;
import net.xuwu.openblocks_reborn.registry.ModEnchantments;
import net.xuwu.openblocks_reborn.network.ModNetworking;
import org.slf4j.Logger;

@Mod(OpenBlocksReborn.MOD_ID)
public final class OpenBlocksReborn {
    public static final String MOD_ID = "openblocks_reborn";
    public static final Logger LOGGER = LogUtils.getLogger();

    public OpenBlocksReborn() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
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
        ModEnchantments.ENCHANTMENTS.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
        modEventBus.addListener(ModEntities::registerAttributes);
        ModNetworking.register();
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, OpenBlocksConfig.SERVER_SPEC);
        MinecraftForge.EVENT_BUS.register(new CommonEvents());

        LOGGER.info("Loading OpenBlocks Reborn for Minecraft 1.20.1");
    }
}
