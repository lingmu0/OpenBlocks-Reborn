package net.xuwu.openblocks_reborn.client;

import com.mojang.math.Axis;
import net.minecraft.world.item.DyeColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.xuwu.openblocks_reborn.OpenBlocksReborn;
import net.xuwu.openblocks_reborn.block.ColorableBlock;
import net.xuwu.openblocks_reborn.block.ElevatorBlock;
import net.xuwu.openblocks_reborn.item.PaintBrushItem;
import net.xuwu.openblocks_reborn.item.DevNullItem;
import net.xuwu.openblocks_reborn.item.SlimalyzerItem;
import net.xuwu.openblocks_reborn.item.ImaginaryItem;
import net.xuwu.openblocks_reborn.item.GlassesItem;
import net.xuwu.openblocks_reborn.registry.ModBlocks;
import net.xuwu.openblocks_reborn.registry.ModItems;
import net.xuwu.openblocks_reborn.registry.ModEntities;
import net.xuwu.openblocks_reborn.registry.ModFluids;
import net.xuwu.openblocks_reborn.registry.ModMenus;
import net.xuwu.openblocks_reborn.menu.MachineLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.xuwu.openblocks_reborn.client.MiniMeRenderer;
import net.xuwu.openblocks_reborn.client.TankRenderer;
import net.xuwu.openblocks_reborn.client.TrophyRenderer;
import net.xuwu.openblocks_reborn.client.CanvasRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.FallingBlockRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.xuwu.openblocks_reborn.item.CraneBackpackItem;
import net.xuwu.openblocks_reborn.item.HangGliderItem;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

@EventBusSubscriber(modid = OpenBlocksReborn.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {
    private static int smokeTicks;
    private static int galleryTicks = -1;
    private static final EnumSet<MachineLayout> capturedSmokeMenus = EnumSet.noneOf(MachineLayout.class);
    private static MachineLayout pendingSmokeMenu;
    private static int pendingSmokeMenuTicks;
    private static boolean goldenEyeSmokeCaptured;
    private static int goldenEyeSmokeTicks;
    private static final Set<Integer> TILTED_GLIDER_PLAYERS = new HashSet<>();

    @SubscribeEvent
    public static void registerFluidClientExtensions(RegisterClientExtensionsEvent event) {
        ResourceLocation still = ResourceLocation.fromNamespaceAndPath(OpenBlocksReborn.MOD_ID, "block/xp_juice_still");
        ResourceLocation flowing = ResourceLocation.fromNamespaceAndPath(OpenBlocksReborn.MOD_ID, "block/xp_juice_flowing");
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return still;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return flowing;
            }

            @Override
            public int getTintColor() {
                return 0xFFE6FF3C;
            }
        }, ModFluids.XP_JUICE_TYPE.get());
        event.registerItem(new IClientItemExtensions() {
            private HumanoidModel<?> model;

            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity living, net.minecraft.world.item.ItemStack stack,
                                                          EquipmentSlot slot, HumanoidModel<?> original) {
                if (model == null) model = WearableArmorModels.bake(WearableArmorModels.GLASSES);
                return model;
            }
        }, ModItems.SONIC_GLASSES.get(), ModItems.PENCIL_GLASSES.get(), ModItems.CRAYON_GLASSES.get(),
                ModItems.TECHNICOLOR_GLASSES.get(), ModItems.SERIOUS_GLASSES.get());
        event.registerItem(new IClientItemExtensions() {
            private HumanoidModel<?> model;

            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity living, net.minecraft.world.item.ItemStack stack,
                                                          EquipmentSlot slot, HumanoidModel<?> original) {
                if (model == null) model = WearableArmorModels.bakeCrane();
                return model;
            }
        }, ModItems.CRANE_BACKPACK.get());
        event.registerItem(new IClientItemExtensions() {
            private CannonItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new CannonItemRenderer();
                return renderer;
            }
        }, ModItems.BLOCK_ITEMS.get("cannon").get());
        event.registerItem(new IClientItemExtensions() {
            private TrophyItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new TrophyItemRenderer();
                return renderer;
            }
        }, ModItems.BLOCK_ITEMS.get("trophy").get());
        event.registerItem(new IClientItemExtensions() {
            private StencilItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new StencilItemRenderer();
                return renderer;
            }
        }, ModItems.STENCIL.get());
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(CannonRenderer.MODEL_LAYER, CannonModel::createBodyLayer);
        event.registerLayerDefinition(WearableArmorModels.GLASSES, WearableArmorModels::glassesLayer);
        event.registerLayerDefinition(WearableArmorModels.CRANE, WearableArmorModels::craneLayer);
        event.registerLayerDefinition(WearableArmorModels.CRANE_MAGNET,
                WearableArmorModels::craneMagnetLayer);
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(ModFluids.XP_JUICE.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModFluids.FLOWING_XP_JUICE.get(), RenderType.translucent());
            ItemProperties.register(ModItems.PEDOMETER.get(),
                    ResourceLocation.fromNamespaceAndPath(OpenBlocksReborn.MOD_ID, "speed"),
                    (stack, level, entity, seed) -> entity != null
                            && entity.getDeltaMovement().horizontalDistanceSqr() > 0.0001D ? 1.0F : 0.0F);
            ItemProperties.register(ModItems.SLIMALYZER.get(),
                    ResourceLocation.fromNamespaceAndPath(OpenBlocksReborn.MOD_ID, "active"),
                    (stack, level, entity, seed) -> SlimalyzerItem.isActive(stack) ? 1.0F : 0.0F);
            ItemProperties.register(ModItems.DEV_NULL.get(),
                    ResourceLocation.fromNamespaceAndPath(OpenBlocksReborn.MOD_ID, "fill"),
                    (stack, level, entity, seed) -> {
                        var stored = DevNullItem.getStored(stack);
                        if (stored.isEmpty()) return 0.0F;
                        return stored.getCount() >= stored.getMaxStackSize() ? 1.0F : 0.5F;
                    });
            ItemProperties.register(ModItems.CRANE_CONTROL.get(),
                    ResourceLocation.fromNamespaceAndPath(OpenBlocksReborn.MOD_ID, "lift"),
                    (stack, level, entity, seed) -> {
                        if (!(entity instanceof net.minecraft.world.entity.player.Player player)
                                || !player.isUsingItem() || player.getUseItem() != stack
                                || CraneBackpackItem.wornBy(player).isEmpty()) return 0.0F;
                        return player.isShiftKeyDown() ? 0.5F : 1.0F;
                    });
            ItemProperties.register(ModItems.CRANE_CONTROL.get(),
                    ResourceLocation.fromNamespaceAndPath(OpenBlocksReborn.MOD_ID, "magnet"),
                    (stack, level, entity, seed) -> {
                        if (!(entity instanceof net.minecraft.world.entity.player.Player player)) return 0.0F;
                        var backpack = CraneBackpackItem.wornBy(player);
                        if (backpack.isEmpty()) return 0.0F;
                        if (CraneBackpackItem.isCarrying(backpack)) return 1.0F;
                        return CraneBackpackItem.hasPickupTarget(player, backpack) ? 0.5F : 0.0F;
                    });
            ItemProperties.register(ModItems.BLOCK_ITEMS.get("imaginary").get(),
                    ResourceLocation.fromNamespaceAndPath(OpenBlocksReborn.MOD_ID, "crayon"),
                    (stack, level, entity, seed) -> ImaginaryItem.isCrayon(stack) ? 1.0F : 0.0F);
            for (var block : new net.minecraft.world.level.block.Block[] {
                    ModBlocks.LADDER.get(), ModBlocks.GUIDE.get(), ModBlocks.BUILDER_GUIDE.get(),
                    ModBlocks.FLAG.get(), ModBlocks.GRAVE.get(), ModBlocks.TROPHY.get(), ModBlocks.BEARTRAP.get(),
                    ModBlocks.SPRINKLER.get(), ModBlocks.VACUUM_HOPPER.get(), ModBlocks.ROPE_LADDER.get(),
                    ModBlocks.FAN.get(), ModBlocks.XP_DRAIN.get(), ModBlocks.XP_SHOWER.get(),
                    ModBlocks.PAINT_CAN.get()
            }) ItemBlockRenderTypes.setRenderLayer(block, RenderType.cutout());
            for (var block : new net.minecraft.world.level.block.Block[] {
                    ModBlocks.TANK.get(), ModBlocks.CANVAS_GLASS.get(), ModBlocks.IMAGINARY.get(), ModBlocks.SKY.get()
            }) ItemBlockRenderTypes.setRenderLayer(block, RenderType.translucent());
        });
    }
    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> tintIndex == 0
                        ? state.getValue(ElevatorBlock.COLOR).getTextureDiffuseColor() : 0xFFFFFFFF,
                ModBlocks.ELEVATOR.get(), ModBlocks.ELEVATOR_ROTATING.get());
        event.register((state, level, pos, tintIndex) -> tintIndex == 0
                        ? state.getValue(ColorableBlock.COLOR).getTextureDiffuseColor() : 0xFFFFFFFF,
                ModBlocks.FLAG.get(), ModBlocks.PAINT_CAN.get());
        event.register((state, level, pos, tintIndex) -> {
            if (tintIndex == 0 && level != null && pos != null
                    && level.getBlockEntity(pos) instanceof
                    net.xuwu.openblocks_reborn.blockentity.ImaginaryBlockEntity imaginary
                    && imaginary.getColor() != null) {
                return 0xFF000000 | imaginary.getColor();
            }
            return 0xFFFFFFFF;
        }, ModBlocks.IMAGINARY.get());
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> tintIndex == 1
                        ? 0xFF000000 | PaintBrushItem.getRgbColor(stack) : 0xFFFFFFFF,
                ModItems.PAINTBRUSH.get(), ModItems.SMALL_PAINTBRUSH.get());
        event.register((stack, tintIndex) -> 0xFF000000 | PaintBrushItem.getRgbColor(stack),
                ModItems.BLOCK_ITEMS.get("elevator").get(),
                ModItems.BLOCK_ITEMS.get("elevator_rotating").get(),
                ModItems.BLOCK_ITEMS.get("flag").get(),
                ModItems.BLOCK_ITEMS.get("paint_can").get());
        event.register((stack, tintIndex) -> tintIndex == 1 && ImaginaryItem.isCrayon(stack)
                        ? 0xFF000000 | ImaginaryItem.getColor(stack) : 0xFFFFFFFF,
                ModItems.BLOCK_ITEMS.get("imaginary").get());
        event.register((stack, tintIndex) -> tintIndex == 0
                        ? 0xFF000000 | GlassesItem.getGlassesColor(stack) : 0xFFFFFFFF,
                ModItems.CRAYON_GLASSES.get());
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.LUGGAGE.get(),
                LuggageRenderer::new);
        event.registerEntityRenderer(ModEntities.MINI_ME.get(), MiniMeRenderer::new);
        event.registerEntityRenderer(ModEntities.GLYPH.get(), GlyphRenderer::new);
        event.registerEntityRenderer(ModEntities.HANG_GLIDER.get(), HangGliderRenderer::new);
        event.registerEntityRenderer(ModEntities.GOLDEN_EYE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.CRANE_CARRIED_BLOCK.get(), FallingBlockRenderer::new);
        event.registerBlockEntityRenderer(net.xuwu.openblocks_reborn.registry.ModBlockEntities.TANK.get(), TankRenderer::new);
        event.registerBlockEntityRenderer(net.xuwu.openblocks_reborn.registry.ModBlockEntities.TROPHY.get(), TrophyRenderer::new);
        event.registerBlockEntityRenderer(net.xuwu.openblocks_reborn.registry.ModBlockEntities.CANVAS.get(), CanvasRenderer::new);
        event.registerBlockEntityRenderer(net.xuwu.openblocks_reborn.registry.ModBlockEntities.ITEM_MACHINE.get(), CannonRenderer::new);
        event.registerBlockEntityRenderer(net.xuwu.openblocks_reborn.registry.ModBlockEntities.UTILITY_MACHINE.get(), ProjectorRenderer::new);
        event.registerBlockEntityRenderer(net.xuwu.openblocks_reborn.registry.ModBlockEntities.SKY.get(), SkyBlockRenderer::new);
        event.registerBlockEntityRenderer(net.xuwu.openblocks_reborn.registry.ModBlockEntities.IMAGINARY.get(),
                ImaginaryBlockRenderer::new);
        event.registerBlockEntityRenderer(net.xuwu.openblocks_reborn.registry.ModBlockEntities.GUIDE.get(),
                GuideRenderer::new);
    }

    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.MACHINE.get(), MachineScreen::new);
        event.register(ModMenus.GOLDEN_EYE.get(), GoldenEyeScreen::new);
    }

    @SubscribeEvent
    public static void runClientSmokeTest(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("openblocks_reborn.smokeTest")) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;
        minecraft.options.pauseOnLostFocus = false;
        minecraft.player.getInventory().selected = 0;
        if (minecraft.screen instanceof PauseScreen) minecraft.setScreen(null);
        smokeTicks++;
        if (Boolean.getBoolean("openblocks_reborn.glyphSmokeTest")) {
            if (smokeTicks == 30) screenshot(minecraft, "openblocks_glyph_model.png");
            if (smokeTicks == 40) {
                OpenBlocksReborn.LOGGER.info("OpenBlocks glyph smoke test completed; closing client");
                minecraft.stop();
            }
            return;
        }
        if (Boolean.getBoolean("openblocks_reborn.cursorSmokeTest")) {
            if (smokeTicks == 40) {
                if (!(minecraft.screen instanceof MachineScreen screen)
                        || screen.getMenu().layout() != MachineLayout.CANNON) {
                    throw new IllegalStateException(
                            "Cursor remote Cannon screen did not remain open beyond eight blocks");
                }
                screenshot(minecraft, "openblocks_cursor_remote_gui.png");
            }
            if (smokeTicks == 60) {
                OpenBlocksReborn.LOGGER.info(
                        "OpenBlocks Cursor remote-menu smoke test completed; closing client");
                minecraft.stop();
            }
            return;
        }
        if (Boolean.getBoolean("openblocks_reborn.guideSmokeTest")) {
            if (smokeTicks == 40) screenshot(minecraft, "openblocks_guide_powered.png");
            if (smokeTicks == 60) {
                OpenBlocksReborn.LOGGER.info(
                        "OpenBlocks powered Guide renderer smoke test completed; closing client");
                minecraft.stop();
            }
            return;
        }
        if (Boolean.getBoolean("openblocks_reborn.modelSmokeTest")) {
            if (smokeTicks == 1) {
                minecraft.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
            }
            if (smokeTicks == 40) screenshot(minecraft, "openblocks_models_close.png");
            if (smokeTicks == 80) screenshot(minecraft, "openblocks_crane_side.png");
            if (smokeTicks == 90) {
                minecraft.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
                // Keep the legacy boom in-frame so the screenshot validates its
                // full first-person silhouette instead of only its clipped end.
                minecraft.player.setXRot(0.0F);
                minecraft.player.xRotO = 0.0F;
            }
            if (smokeTicks == 120) screenshot(minecraft, "openblocks_crane_first_person.png");
            if (smokeTicks == 140) {
                OpenBlocksReborn.LOGGER.info("OpenBlocks model smoke test completed; closing client");
                minecraft.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
                minecraft.stop();
            }
            return;
        }
        captureCurrentSmokeMenu(minecraft);
        // Client ticks begin before the integrated server has finished loading. Anchor the
        // gallery sequence to the server closing its final menu instead of an absolute tick.
        if (capturedSmokeMenus.size() == 15 && goldenEyeSmokeCaptured
                && minecraft.screen == null) {
            galleryTicks++;
            if (galleryTicks == 15) screenshot(minecraft, "openblocks_gallery_front.png");
            if (galleryTicks == 45) screenshot(minecraft, "openblocks_gallery_overhead.png");
            if (galleryTicks == 75) screenshot(minecraft, "openblocks_gallery_left.png");
            if (galleryTicks == 105) screenshot(minecraft, "openblocks_gallery_right.png");
            if (galleryTicks == 135) screenshot(minecraft, "openblocks_item_models.png");
            if (galleryTicks == 165) screenshot(minecraft, "openblocks_vacuum_connections.png");
            if (galleryTicks == 195) {
                OpenBlocksReborn.LOGGER.info("OpenBlocks client smoke test completed; closing client");
                minecraft.stop();
            }
        }
    }

    @SubscribeEvent
    public static void renderFirstPersonCrane(RenderLevelStageEvent event) {
        CraneThirdPersonRenderer.render(event);
        CraneFirstPersonRenderer.render(event);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void tiltGlidingPlayer(RenderPlayerEvent.Pre event) {
        if (!HangGliderItem.isActivelyGliding(event.getEntity())
                || !TILTED_GLIDER_PLAYERS.add(event.getEntity().getId())) return;
        float bodyYaw = Mth.rotLerp(event.getPartialTick(),
                event.getEntity().yBodyRotO, event.getEntity().yBodyRot);
        float renderedYaw = 180.0F - bodyYaw;
        event.getPoseStack().pushPose();
        // The glider's flying plane is rendered around 1.75 blocks above the
        // owner's feet. Raising the feet-origin player model to 1.35 keeps its
        // horizontal body immediately beneath the wing instead of hanging low.
        event.getPoseStack().translate(0.0D, 1.35D, 0.0D);
        // Conjugate the pitch by the renderer's body yaw so the player leans
        // forward in their own facing direction rather than along world X.
        event.getPoseStack().mulPose(Axis.YP.rotationDegrees(renderedYaw));
        event.getPoseStack().mulPose(Axis.XP.rotationDegrees(-90.0F));
        event.getPoseStack().mulPose(Axis.YP.rotationDegrees(-renderedYaw));
    }

    @SubscribeEvent
    public static void restoreGlidingPlayerPose(RenderPlayerEvent.Post event) {
        if (TILTED_GLIDER_PLAYERS.remove(event.getEntity().getId())) {
            event.getPoseStack().popPose();
        }
    }

    private static void captureCurrentSmokeMenu(Minecraft minecraft) {
        if (minecraft.getOverlay() != null) {
            pendingSmokeMenu = null;
            pendingSmokeMenuTicks = 0;
            goldenEyeSmokeTicks = 0;
            return;
        }
        if (minecraft.screen instanceof GoldenEyeScreen) {
            pendingSmokeMenu = null;
            pendingSmokeMenuTicks = 0;
            if (!goldenEyeSmokeCaptured && ++goldenEyeSmokeTicks >= 10) {
                screenshot(minecraft, "openblocks_golden_eye_gui.png");
                goldenEyeSmokeCaptured = true;
            }
            return;
        }
        goldenEyeSmokeTicks = 0;
        if (!(minecraft.screen instanceof MachineScreen screen)) {
            pendingSmokeMenu = null;
            pendingSmokeMenuTicks = 0;
            return;
        }
        MachineLayout layout = screen.getMenu().layout();
        if (capturedSmokeMenus.contains(layout)) return;
        if (pendingSmokeMenu != layout) {
            pendingSmokeMenu = layout;
            pendingSmokeMenuTicks = 0;
            return;
        }
        // Network menu replacement happens during a client tick, before the new screen
        // has necessarily reached the render target. Wait for several complete frames so
        // the screenshot content cannot lag one menu behind its semantic filename.
        if (++pendingSmokeMenuTicks < 10) return;
        screenshot(minecraft, smokeScreenshotName(layout));
        capturedSmokeMenus.add(layout);
        pendingSmokeMenu = null;
        pendingSmokeMenuTicks = 0;
    }

    private static String smokeScreenshotName(MachineLayout layout) {
        return switch (layout) {
            case AUTO_ANVIL -> "openblocks_auto_anvil_gui.png";
            case AUTO_ENCHANTMENT_TABLE -> "openblocks_auto_enchant_gui.png";
            case PAINT_MIXER -> "openblocks_paint_gui.png";
            case DRAWING_TABLE -> "openblocks_drawing_gui.png";
            case SPRINKLER -> "openblocks_sprinkler_gui.png";
            case PROJECTOR -> "openblocks_projector_gui.png";
            case BIG_BUTTON -> "openblocks_button_gui.png";
            case BLOCK_PLACER -> "openblocks_item_machine_gui.png";
            case VACUUM_HOPPER -> "openblocks_vacuum_gui.png";
            case XP_BOTTLER -> "openblocks_xp_bottler_gui.png";
            case LUGGAGE -> "openblocks_luggage_gui.png";
            case DONATION_STATION -> "openblocks_donation_gui.png";
            case ITEM_DROPPER -> "openblocks_item_dropper_gui.png";
            case CANNON -> "openblocks_cannon_gui.png";
            case DEV_NULL -> "openblocks_dev_null_gui.png";
        };
    }

    private static void screenshot(Minecraft minecraft, String name) {
        Screenshot.grab(minecraft.gameDirectory, name, minecraft.getMainRenderTarget(),
                message -> OpenBlocksReborn.LOGGER.info("Smoke screenshot: {}", message.getString()));
    }

    private ClientEvents() {
    }
}
