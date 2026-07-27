package net.xuwu.openblocks_reborn.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.xuwu.openblocks_reborn.blockentity.ExperienceBlockEntity;
import net.xuwu.openblocks_reborn.menu.MachineLayout;
import net.xuwu.openblocks_reborn.menu.MachineMenu;
import net.xuwu.openblocks_reborn.item.StencilItem;
import net.xuwu.openblocks_reborn.item.HeightMapItem;
import net.xuwu.openblocks_reborn.registry.ModFluids;
import net.xuwu.openblocks_reborn.network.ModNetworking;
import net.xuwu.openblocks_reborn.network.RenameAutoAnvilPayload;

import java.awt.Color;

/** Machine controls rendered in Minecraft's vanilla container visual language. */
public final class MachineScreen extends AbstractContainerScreen<MachineMenu> {
    private static final ResourceLocation GENERIC_CONTAINER_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final ResourceLocation DISPENSER_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/dispenser.png");
    private static final ResourceLocation WIDGETS_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/widgets.png");
    private static final ResourceLocation SLIDER_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/slider.png");
    private static final ResourceLocation MACHINE_PANEL_TEXTURE =
            new ResourceLocation("openblocks_reborn", "textures/gui/sprites/machine/panel.png");
    private static final ResourceLocation MACHINE_RECESSED_TEXTURE =
            new ResourceLocation("openblocks_reborn", "textures/gui/sprites/machine/recessed.png");
    private static final ResourceLocation MACHINE_BEVEL_FRAME_TEXTURE =
            new ResourceLocation("openblocks_reborn", "textures/gui/sprites/machine/bevel_frame.png");
    private static final ResourceLocation SLOT_SPRITE =
            ResourceLocation.withDefaultNamespace("container/slot");
    private static final ResourceLocation BUTTON_SPRITE =
            ResourceLocation.withDefaultNamespace("widget/button");
    private static final ResourceLocation BUTTON_HIGHLIGHTED_SPRITE =
            ResourceLocation.withDefaultNamespace("widget/button_highlighted");
    private static final ResourceLocation BUTTON_DISABLED_SPRITE =
            ResourceLocation.withDefaultNamespace("widget/button_disabled");
    private static final ResourceLocation SLIDER_SPRITE =
            ResourceLocation.withDefaultNamespace("widget/slider");
    private static final ResourceLocation SLIDER_HANDLE_SPRITE =
            ResourceLocation.withDefaultNamespace("widget/slider_handle");
    private static final ResourceLocation SLIDER_HANDLE_HIGHLIGHTED_SPRITE =
            ResourceLocation.withDefaultNamespace("widget/slider_handle_highlighted");
    private static final ResourceLocation OPENBLOCKS_CHECKBOX_TEXTURE =
            new ResourceLocation("openblocks_reborn", "textures/gui/sprites/machine/checkbox.png");
    private static final ResourceLocation OPENBLOCKS_CHECKBOX_SELECTED_TEXTURE =
            new ResourceLocation("openblocks_reborn", "textures/gui/sprites/machine/checkbox_selected.png");
    private static final ResourceLocation OPENBLOCKS_PLUS_TEXTURE =
            new ResourceLocation("openblocks_reborn", "textures/gui/sprites/machine/plus.png");
    private static final ResourceLocation OPENBLOCKS_ARROW_TEXTURE =
            new ResourceLocation("openblocks_reborn", "textures/gui/sprites/machine/arrow.png");
    private static final int FRAME_DARK = 0xFF373737;
    private static final int FRAME_SHADOW = 0xFF555555;
    private static final int FRAME_LIGHT = 0xFFFFFFFF;
    private static final int SURFACE = 0xFFC6C6C6;
    private static final int RECESS = 0xFF8B8B8B;
    private static final int ACCENT = 0xFF4C819B;
    /** Vanilla container label colour (AbstractContainerScreen uses 0x404040). */
    private static final int TEXT = 0xFF404040;
    private static final int[] PAINT_PALETTE = {0x000000, 0x7F7F7F, 0xFFFFFF, 0xFF0000,
            0xFF7F00, 0xFFFF00, 0x00AA00, 0x00FFFF, 0x0000FF, 0x7F00FF,
            0xFF00FF, 0x8B4513, 0xB0B0B0, 0x55FF55, 0x5555FF, 0xFF77AA};

    private final MachineLayout layout;
    private String drawingText = "";
    private boolean drawingTextFocused;
    private int vacuumTab;
    private int ioChannel;
    private int uiMouseX;
    private int uiMouseY;
    private DragControl dragControl = DragControl.NONE;
    private EditBox anvilName;
    private ItemStack lastAnvilInput = ItemStack.EMPTY;
    private boolean updatingAnvilName;

    private enum DragControl {
        NONE, PAINT_COLOR, PAINT_SATURATION, ENCHANT_POWER, DROP_SPEED
    }

    public MachineScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        layout = menu.layout();
        imageWidth = layout.width();
        imageHeight = layout.height();
        titleLabelX = 8;
        titleLabelY = 6;
        inventoryLabelX = layout.playerX();
        inventoryLabelY = layout.playerY() - 11;
    }

    @Override
    protected void init() {
        super.init();
        if (layout != MachineLayout.AUTO_ANVIL) return;
        anvilName = new EditBox(font, leftPos + 15, topPos + 20, 94, 12,
                Component.translatable("container.repair"));
        anvilName.setBordered(false);
        anvilName.setTextColor(0xFFFFFFFF);
        anvilName.setTextColorUneditable(0xFFA0A0A0);
        anvilName.setMaxLength(50);
        updateAnvilNameFromSlot();
        anvilName.setResponder(this::onAnvilNameChanged);
        addRenderableWidget(anvilName);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (anvilName != null && !ItemStack.matches(lastAnvilInput, menu.getSlot(0).getItem())) {
            updateAnvilNameFromSlot();
        }
    }

    private void updateAnvilNameFromSlot() {
        if (anvilName == null) return;
        ItemStack input = menu.getSlot(0).getItem();
        lastAnvilInput = input.copy();
        updatingAnvilName = true;
        anvilName.setValue(input.isEmpty() ? "" : input.getHoverName().getString());
        anvilName.setEditable(!input.isEmpty());
        updatingAnvilName = false;
    }

    private void onAnvilNameChanged(String value) {
        if (updatingAnvilName || minecraft == null || menu.getSlot(0).getItem().isEmpty()) return;
        ItemStack input = menu.getSlot(0).getItem();
        String requested = !input.hasCustomHoverName()
                && value.equals(input.getHoverName().getString()) ? "" : value;
        ModNetworking.sendToServer(new RenameAutoAnvilPayload(menu.containerId, requested));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        renderExperienceTooltip(graphics, mouseX, mouseY);
        renderWaterTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        uiMouseX = mouseX;
        uiMouseY = mouseY;
        int x = leftPos;
        int y = topPos;
        boolean vanillaContainer = renderVanillaContainer(graphics, x, y);
        if (!vanillaContainer) containerPanel(graphics, x, y, imageWidth, imageHeight);

        renderMachineDecoration(graphics, x, y);
        if (vanillaContainer) return;

        for (int slot = 0; slot < layout.machineSlotCount(); slot++) {
            if (layout.machineSlotX(slot) < 0 || layout.machineSlotY(slot) < 0) continue;
            slotFrame(graphics, x + layout.machineSlotX(slot), y + layout.machineSlotY(slot));
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                slotFrame(graphics, x + layout.playerX() + column * 18,
                        y + layout.playerY() + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            slotFrame(graphics, x + layout.playerX() + column * 18,
                    y + layout.playerY() + 58);
        }
    }

    private boolean renderVanillaContainer(GuiGraphics graphics, int x, int y) {
        if (layout == MachineLayout.BLOCK_PLACER || layout == MachineLayout.SPRINKLER) {
            graphics.blit(DISPENSER_TEXTURE, x, y, 0, 0, 176, 166);
            return true;
        }
        if (layout == MachineLayout.LUGGAGE) {
            int machineHeight = 3 * 18 + 17;
            graphics.blit(GENERIC_CONTAINER_TEXTURE, x, y, 0, 0, 176, machineHeight);
            graphics.blit(GENERIC_CONTAINER_TEXTURE, x, y + machineHeight, 0, 126, 176, 96);
            return true;
        }
        return false;
    }

    private void renderMachineDecoration(GuiGraphics graphics, int x, int y) {
        switch (layout) {
            case AUTO_ANVIL -> renderAutoAnvil(graphics, x, y);
            case AUTO_ENCHANTMENT_TABLE -> renderAutoEnchant(graphics, x, y);
            case BIG_BUTTON -> renderBigButton(graphics, x, y);
            case PAINT_MIXER -> renderPaintMixer(graphics, x, y);
            case DRAWING_TABLE -> renderDrawingTable(graphics, x, y);
            case PROJECTOR -> renderProjector(graphics, x, y);
            case SPRINKLER -> renderSprinkler(graphics, x, y);
            case VACUUM_HOPPER -> renderVacuumHopper(graphics, x, y);
            case XP_BOTTLER -> renderXpBottler(graphics, x, y);
            case BLOCK_PLACER -> renderBlockPlacer(graphics, x, y);
            case ITEM_DROPPER -> renderItemDropper(graphics, x, y);
            case CANNON -> renderCannon(graphics, x, y);
            case LUGGAGE -> { }
            case DONATION_STATION -> renderDonationStation(graphics, x, y);
            case DEV_NULL -> renderDevNull(graphics, x, y);
        }
        if (supportsIoConfiguration()) renderIoConfiguration(graphics, x, y);
    }

    private void renderAutoAnvil(GuiGraphics graphics, int x, int y) {
        recessed(graphics, x + 13, y + 17, 98, 18);
        graphics.blit(OPENBLOCKS_PLUS_TEXTURE, x + 36, y + 41,
                13, 13, 0.0F, 0.0F, 13, 13, 13, 13);
        arrow(graphics, x + 78, y + 47, 18);
        graphics.renderItem(new ItemStack(Items.ANVIL), x + 116, y + 17);
        fluidGauge(graphics, x + 140, y + 30, 17, 37, menu.data(0), menu.data(1), 0xFFE6FF3C);
        centeredText(graphics, Component.translatable("gui.openblocks_reborn.repair"), x + 88, y + 69, TEXT);
        centeredText(graphics, Component.translatable("gui.openblocks_reborn.anvil_cost",
                Math.max(0, menu.data(9))), x + 88, y + 80, TEXT);
    }

    private void renderAutoEnchant(GuiGraphics graphics, int x, int y) {
        int maximum = Math.max(1, menu.data(3));
        int selected = net.minecraft.util.Mth.clamp(menu.data(2), 1, maximum);
        centeredText(graphics, Component.translatable("gui.openblocks_reborn.enchant_level_short",
                selected, maximum), x + 66, y + 28, TEXT);
        centeredText(graphics, Component.translatable("gui.openblocks_reborn.available_power",
                Math.max(0, menu.data(8))), x + 118, y + 18, TEXT);
        recessed(graphics, x + 43, y + 42, 47, 8);
        graphics.fill(x + 45, y + 44, x + 45 + selected * 43 / maximum, y + 48, 0xFF72B146);
        int handleX = x + 43 + (maximum <= 1 ? 0
                : net.minecraft.util.Mth.clamp(Math.round((selected - 1) * 43.0F / (maximum - 1)), 0, 43));
        bevel(graphics, handleX, y + 41, 4, 11,
                isHovered(x + 43, y + 41, 47, 11) || dragControl == DragControl.ENCHANT_POWER
                        ? 0xFFE0E0E0 : 0xFFC6C6C6);
        arrow(graphics, x + 76, y + 58, 17);
        fluidGauge(graphics, x + 140, y + 30, 17, 37, menu.data(0), menu.data(1), 0xFFE6FF3C);
        centeredText(graphics, Component.translatable("gui.openblocks_reborn.enchant_cost",
                Math.max(0, menu.data(9))), x + 88, y + 66, TEXT);
    }

    private void renderBigButton(GuiGraphics graphics, int x, int y) {
        recessed(graphics, x + 48, y + 25, 80, 43);
        Component ticks = Component.translatable("gui.openblocks_reborn.button_ticks", Math.max(1, menu.data(2)));
        centeredText(graphics, ticks, x + imageWidth / 2, y + 76, TEXT);
    }

    private void renderPaintMixer(GuiGraphics graphics, int x, int y) {
        int selectedColor = menu.data(0) & 0xFFFFFF;
        float[] selectedHsb = Color.RGBtoHSB(selectedColor >> 16 & 0xFF,
                selectedColor >> 8 & 0xFF, selectedColor & 0xFF, null);
        int pickerX = x + 10;
        int pickerY = y + 20;
        for (int column = 0; column < 100; column++) {
            for (int row = 0; row < 50; row++) {
                int color = Color.HSBtoRGB(column / 99.0F, selectedHsb[1], 1.0F - row / 49.0F);
                graphics.fill(pickerX + column, pickerY + row, pickerX + column + 1, pickerY + row + 1, color);
            }
        }
        graphics.renderOutline(pickerX - 1, pickerY - 1, 102, 52, FRAME_DARK);
        int cursorX = pickerX + net.minecraft.util.Mth.clamp(Math.round(selectedHsb[0] * 99), 0, 99);
        int cursorY = pickerY + net.minecraft.util.Mth.clamp(Math.round((1.0F - selectedHsb[2]) * 49), 0, 49);
        graphics.renderOutline(cursorX - 2, cursorY - 2, 5, 5, selectedHsb[2] > 0.55F ? 0xFF151515 : 0xFFF4F4F4);

        recessed(graphics, x + 10, y + 75, 100, 12);
        for (int column = 0; column < 96; column++) {
            int color = Color.HSBtoRGB(selectedHsb[0], column / 95.0F, selectedHsb[2]);
            graphics.fill(x + 12 + column, y + 78, x + 13 + column, y + 84, color);
        }
        int saturationX = x + 9 + net.minecraft.util.Mth.clamp(Math.round(selectedHsb[1] * 96), 0, 96);
        bevel(graphics, saturationX, y + 73, 8, 16, 0xFFD8D8D8);
        bevel(graphics, x + 10, y + 90, 45, 10, 0xFF000000 | selectedColor);
        recessed(graphics, x + 65, y + 90, 44, 10);
        centeredText(graphics, Component.literal(String.format(java.util.Locale.ROOT, "%06X", selectedColor)),
                x + 87, y + 91, TEXT);

        for (int index = 0; index < PAINT_PALETTE.length; index++) {
            int swatchX = x + 112 + index % 2 * 5;
            int swatchY = y + 25 + index / 2 * 5;
            graphics.fill(swatchX, swatchY, swatchX + 5, swatchY + 5, 0xFF000000 | PAINT_PALETTE[index]);
        }

        recessed(graphics, x + 125, y + 43, 30, 9);
        int progress = net.minecraft.util.Mth.clamp(menu.data(1), 0, 300);
        if (progress > 0) {
            int completedWidth = Math.max(1, (300 - progress) * 26 / 300);
            graphics.fill(x + 127, y + 45, x + 127 + completedWidth, y + 50, ACCENT);
        }
        if (progress > 0) disabledButton(graphics, x + 125, y + 57, 30, 13,
                Component.translatable("gui.openblocks_reborn.mixing"));
        else button(graphics, x + 125, y + 57, 30, 13, Component.translatable("gui.openblocks_reborn.mix"));
        graphics.fill(x + 121, y + 74, x + 141, y + 94, 0xFF4B9FC1);
        graphics.fill(x + 141, y + 74, x + 161, y + 94, 0xFFDB7AD5);
        graphics.fill(x + 121, y + 94, x + 141, y + 114, 0xFFE7E72A);
        graphics.fill(x + 141, y + 94, x + 161, y + 114, 0xFF151515);
    }

    private void renderDrawingTable(GuiGraphics graphics, int x, int y) {
        boolean glyphMode = menu.data(0) == 1;
        button(graphics, x + 8, y + 35, 50, 14, Component.translatable(glyphMode
                ? "gui.openblocks_reborn.drawing_mode_glyphs" : "gui.openblocks_reborn.drawing_mode_stencils"));
        arrow(graphics, x + 80, y + 42, 14);
        button(graphics, x + 116, y + 34, 18, 17, Component.literal("<"));
        button(graphics, x + 136, y + 34, 18, 17, Component.literal(">"));
        Component selection = glyphMode
                ? Component.literal(new String(Character.toChars(menu.data(1))))
                : Component.translatable(StencilItem.patternTranslationKey(menu.data(1)));
        recessed(graphics, x + 7, y + 53, 36, 36);
        if (glyphMode) {
            graphics.pose().pushPose();
            graphics.pose().translate(x + 25.0F, y + 71.0F, 0.0F);
            graphics.pose().scale(2.0F, 2.0F, 1.0F);
            graphics.drawCenteredString(font, selection, 0, -4, TEXT);
            graphics.pose().popPose();
        } else {
            int pattern = menu.data(1);
            for (int pixelY = 0; pixelY < 16; pixelY++) {
                for (int pixelX = 0; pixelX < 16; pixelX++) {
                    int color = StencilItem.isHole(pattern, 0, pixelX, pixelY)
                            ? 0xFF454545 : 0xFFD8D1AD;
                    graphics.fill(x + 9 + pixelX * 2, y + 55 + pixelY * 2,
                            x + 11 + pixelX * 2, y + 57 + pixelY * 2, color);
                }
            }
        }
        graphics.drawString(font, selection, x + 47, y + 66, TEXT, false);

        recessed(graphics, x + 8, y + 90, 120, 14);
        if (drawingTextFocused) graphics.renderOutline(x + 7, y + 89, 122, 16, 0xFFE7E7E7);
        Component textbox = drawingText.isEmpty()
                ? Component.translatable("gui.openblocks_reborn.text_to_print") : Component.literal(drawingText);
        graphics.enableScissor(x + 10, y + 92, x + 126, y + 102);
        graphics.drawString(font, textbox, x + 11, y + 93,
                drawingText.isEmpty() ? 0xFF606060 : (glyphMode ? TEXT : 0xFF686868), false);
        graphics.disableScissor();
        if (glyphMode) button(graphics, x + 130, y + 90, 40, 14, Component.translatable("gui.openblocks_reborn.print"));
        else disabledButton(graphics, x + 130, y + 90, 40, 14, Component.translatable("gui.openblocks_reborn.print"));
    }

    private void renderProjector(GuiGraphics graphics, int x, int y) {
        recessed(graphics, x + 35, y + 20, 106, 103);
        button(graphics, x + 10, y + 65, 20, 17, Component.literal("<"));
        button(graphics, x + 146, y + 65, 20, 17, Component.literal(">"));
        int cell = 10;
        int[] heights = HeightMapItem.getHeights(menu.getSlot(0).getItem());
        int minimum = heights.length == 81 ? java.util.Arrays.stream(heights).min().orElse(0) : 0;
        int maximum = heights.length == 81 ? java.util.Arrays.stream(heights).max().orElse(minimum + 1) : 1;
        for (int z = 0; z < 9; z++) {
            for (int mapX = 0; mapX < 9; mapX++) {
                int rotation = Math.floorMod(menu.data(0), 4);
                int sourceX = switch (rotation) {
                    case 1 -> z;
                    case 2 -> 8 - mapX;
                    case 3 -> 8 - z;
                    default -> mapX;
                };
                int sourceZ = switch (rotation) {
                    case 1 -> 8 - mapX;
                    case 2 -> 8 - z;
                    case 3 -> mapX;
                    default -> z;
                };
                int height = heights.length == 81 ? heights[sourceZ * 9 + sourceX] : 0;
                int normalized = maximum == minimum ? 8 : (height - minimum) * 15 / (maximum - minimum);
                int green = 92 + normalized * 7;
                int blue = 58 + normalized * 2;
                int color = 0xFF000000 | 45 << 16 | Math.min(210, green) << 8 | blue;
                graphics.fill(x + 43 + mapX * cell, y + 27 + z * cell,
                        x + 52 + mapX * cell, y + 36 + z * cell, color);
            }
        }
        graphics.renderOutline(x + 42, y + 26, 92, 92, 0xFF274B34);
    }

    private void renderSprinkler(GuiGraphics graphics, int x, int y) {
        fluidGauge(graphics, x + 137, y + 20, 17, 45, menu.data(0), menu.data(1), 0xFF397ED1);
        graphics.drawString(font, Component.translatable("gui.openblocks_reborn.bonemeal"), x + 13, y + 31, TEXT, false);
        centeredText(graphics, Component.translatable("gui.openblocks_reborn.water"), x + 145, y + 7, TEXT);
    }

    private void renderVacuumHopper(GuiGraphics graphics, int x, int y) {
        fluidGauge(graphics, x + 140, y + 18, 17, 37, menu.data(0), menu.data(1), 0xFFE6FF3C);
        int panelX = x - 108;
        int panelY = y + 16;
        containerPanel(graphics, panelX, panelY, 86, 82);
        centeredText(graphics, Component.translatable(vacuumTab == 0
                ? "gui.openblocks_reborn.item_outputs" : "gui.openblocks_reborn.xp_outputs"),
                panelX + 43, panelY + 6, TEXT);

        bevel(graphics, x - 22, y + 20, 22, 22, vacuumTab == 0 ? 0xFFA0A0A0 : SURFACE);
        bevel(graphics, x - 22, y + 44, 22, 22, vacuumTab == 1 ? 0xFFA0A0A0 : SURFACE);
        graphics.renderItem(new ItemStack(Items.CHEST), x - 19, y + 23);
        graphics.renderItem(new ItemStack(Items.EXPERIENCE_BOTTLE), x - 19, y + 47);

        int mask = menu.data(vacuumTab == 0 ? 2 : 3);
        int[] sideX = {28, 28, 28, 44, 12, 60};
        int[] sideY = {56, 20, 38, 38, 38, 38};
        String[] sideNames = {"D", "U", "N", "S", "W", "E"};
        for (Direction direction : Direction.values()) {
            int index = direction.get3DDataValue();
            boolean enabled = (mask & 1 << index) != 0;
            bevel(graphics, panelX + sideX[index], panelY + sideY[index], 14, 14,
                    enabled ? 0xFF65AA65 : 0xFFA0A0A0);
            centeredText(graphics, Component.literal(sideNames[index]), panelX + sideX[index] + 7,
                    panelY + sideY[index] + 3, enabled ? 0xFF173817 : TEXT);
        }
    }

    private void renderXpBottler(GuiGraphics graphics, int x, int y) {
        arrow(graphics, x + 70, y + 36, 28);
        int progressWidth = net.minecraft.util.Mth.clamp(menu.data(2), 0, ExperienceBlockEntity.BOTTLING_TICKS) * 24
                / ExperienceBlockEntity.BOTTLING_TICKS;
        if (progressWidth > 0) {
            graphics.fill(x + 70, y + 35, x + 70 + progressWidth, y + 38, ACCENT);
        }
        graphics.renderItem(new ItemStack(Items.EXPERIENCE_BOTTLE), x + 77, y + 10);
        fluidGauge(graphics, x + 145, y + 16, 13, 37, menu.data(0), menu.data(1), 0xFFE6FF3C);
    }

    private void renderBlockPlacer(GuiGraphics graphics, int x, int y) {
    }

    private void renderItemDropper(GuiGraphics graphics, int x, int y) {
        int speed = net.minecraft.util.Mth.clamp(menu.data(2), 0, 400);
        blitSprite(graphics, SLIDER_SPRITE, x + 70, y + 16, 98, 20);
        int knobX = x + 70 + speed * 90 / 400;
        blitSprite(graphics, isHovered(x + 70, y + 16, 98, 20) || dragControl == DragControl.DROP_SPEED
                ? SLIDER_HANDLE_HIGHLIGHTED_SPRITE : SLIDER_HANDLE_SPRITE, knobX, y + 16, 8, 20);
        centeredText(graphics, Component.translatable("gui.openblocks_reborn.drop_speed",
                String.format(java.util.Locale.ROOT, "%.1f", speed / 100.0D)), x + 119, y + 39, TEXT);
        boolean redstoneStrength = menu.data(3) != 0;
        graphics.blit(redstoneStrength
                        ? OPENBLOCKS_CHECKBOX_SELECTED_TEXTURE : OPENBLOCKS_CHECKBOX_TEXTURE,
                x + 70, y + 50, 8, 8, 0.0F, 0.0F, 8, 8, 8, 8);
        graphics.drawString(font, Component.translatable("gui.openblocks_reborn.use_redstone_strength"),
                x + 85, y + 50, TEXT, false);
    }

    private void renderCannon(GuiGraphics graphics, int x, int y) {
        graphics.renderItem(new ItemStack(Items.TARGET), x + 75, y + 20);
        boolean bound = menu.data(0) != 0;
        graphics.drawString(font, Component.translatable(bound
                        ? "gui.openblocks_reborn.cannon_target_bound"
                        : "gui.openblocks_reborn.cannon_target_unbound"),
                x + 95, y + 23, bound ? 0xFF2F642F : TEXT, false);
        if (bound) {
            centeredText(graphics, Component.translatable("gui.openblocks_reborn.cannon_target_coordinates",
                    menu.data(1), menu.data(2), menu.data(3)), x + 119, y + 49, TEXT);
        } else {
            centeredText(graphics, Component.translatable("gui.openblocks_reborn.cannon_target_hint"),
                    x + 119, y + 47, 0xFF606060);
        }
    }

    private void renderDonationStation(GuiGraphics graphics, int x, int y) {
        ItemStack donation = menu.getSlot(0).getItem();
        if (!donation.isEmpty()) {
            var itemId = BuiltInRegistries.ITEM.getKey(donation.getItem());
            graphics.drawString(font, Component.literal(itemId.getNamespace()), x + 55, y + 31, TEXT, false);
            graphics.drawString(font, Component.translatable("gui.openblocks_reborn.mod_author"), x + 55, y + 42,
                    0xFF606060, false);
        }
        disabledButton(graphics, x + 31, y + 60, 115, 13, Component.translatable("gui.openblocks_reborn.donate"));
    }

    private void renderDevNull(GuiGraphics graphics, int x, int y) {
        graphics.drawString(font, Component.translatable("gui.openblocks_reborn.dev_null_filter"), x + 15, y + 26, TEXT, false);
        graphics.drawString(font, Component.translatable("gui.openblocks_reborn.dev_null_hint"), x + 105, y + 23, 0xFF606060, false);
    }

    private boolean supportsIoConfiguration() {
        return layout == MachineLayout.AUTO_ANVIL
                || layout == MachineLayout.AUTO_ENCHANTMENT_TABLE
                || layout == MachineLayout.XP_BOTTLER;
    }

    private int ioChannelCount() {
        return layout == MachineLayout.XP_BOTTLER ? 3 : 4;
    }

    private void renderIoConfiguration(GuiGraphics graphics, int x, int y) {
        int channelCount = ioChannelCount();
        ioChannel = net.minecraft.util.Mth.clamp(ioChannel, 0, channelCount - 1);
        int panelX = x - 108;
        int panelY = y + 16;
        containerPanel(graphics, panelX, panelY, 86, 106);
        centeredText(graphics, ioChannelLabel(ioChannel), panelX + 43, panelY + 6, TEXT);

        for (int channel = 0; channel < channelCount; channel++) {
            int tabY = y + 20 + channel * 24;
            boolean hovered = isHovered(x - 22, tabY, 22, 22);
            bevel(graphics, x - 22, tabY, 22, 22,
                    channel == ioChannel ? 0xFFA0A0A0 : (hovered ? 0xFFD8D8D8 : SURFACE));
            graphics.renderItem(ioChannelIcon(channel), x - 19, tabY + 3);
        }

        int configuration = menu.data(4 + ioChannel);
        int mask = configuration & 0x3F;
        int[] sideX = {28, 28, 28, 44, 12, 60};
        int[] sideY = {56, 20, 38, 38, 38, 38};
        String[] sideNames = {"D", "U", "N", "S", "W", "E"};
        for (Direction direction : Direction.values()) {
            int index = direction.get3DDataValue();
            int buttonX = panelX + sideX[index];
            int buttonY = panelY + sideY[index];
            boolean enabled = (mask & 1 << index) != 0;
            boolean hovered = isHovered(buttonX, buttonY, 14, 14);
            bevel(graphics, buttonX, buttonY, 14, 14, enabled
                    ? (hovered ? 0xFF7BC77B : 0xFF65AA65)
                    : (hovered ? 0xFFB8B8B8 : 0xFFA0A0A0));
            centeredText(graphics, Component.literal(sideNames[index]), buttonX + 7, buttonY + 3,
                    enabled ? 0xFF173817 : TEXT);
        }
        int automaticX = panelX + 10;
        int automaticY = panelY + 84;
        boolean automatic = (configuration & 0x40) != 0;
        graphics.blit(automatic
                        ? OPENBLOCKS_CHECKBOX_SELECTED_TEXTURE : OPENBLOCKS_CHECKBOX_TEXTURE,
                automaticX, automaticY, 8, 8, 0.0F, 0.0F, 8, 8, 8, 8);
        graphics.drawString(font, Component.translatable("gui.openblocks_reborn.io_automatic"),
                automaticX + 12, automaticY, TEXT, false);
    }

    private Component ioChannelLabel(int channel) {
        String key = switch (layout) {
            case AUTO_ANVIL -> switch (channel) {
                case 0 -> "gui.openblocks_reborn.io_tool";
                case 1 -> "gui.openblocks_reborn.io_modifier";
                case 2 -> "gui.openblocks_reborn.io_output";
                default -> "gui.openblocks_reborn.io_xp";
            };
            case AUTO_ENCHANTMENT_TABLE -> switch (channel) {
                case 0 -> "gui.openblocks_reborn.io_tool";
                case 1 -> "gui.openblocks_reborn.io_lapis";
                case 2 -> "gui.openblocks_reborn.io_output";
                default -> "gui.openblocks_reborn.io_xp";
            };
            default -> switch (channel) {
                case 0 -> "gui.openblocks_reborn.io_bottle";
                case 1 -> "gui.openblocks_reborn.io_output";
                default -> "gui.openblocks_reborn.io_xp";
            };
        };
        return Component.translatable(key);
    }

    private ItemStack ioChannelIcon(int channel) {
        return switch (layout) {
            case AUTO_ANVIL -> new ItemStack(switch (channel) {
                case 0 -> Items.IRON_PICKAXE;
                case 1 -> Items.IRON_INGOT;
                case 2 -> Items.CHEST;
                default -> Items.EXPERIENCE_BOTTLE;
            });
            case AUTO_ENCHANTMENT_TABLE -> new ItemStack(switch (channel) {
                case 0 -> Items.BOOK;
                case 1 -> Items.LAPIS_LAZULI;
                case 2 -> Items.CHEST;
                default -> Items.EXPERIENCE_BOTTLE;
            });
            default -> new ItemStack(switch (channel) {
                case 0 -> Items.GLASS_BOTTLE;
                case 1 -> Items.EXPERIENCE_BOTTLE;
                default -> Items.BUCKET;
            });
        };
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && supportsIoConfiguration()
                && minecraft != null && minecraft.gameMode != null
                && handleIoConfigurationClick(mouseX, mouseY)) {
            return true;
        }
        if (button == 0 && layout == MachineLayout.PAINT_MIXER
                && minecraft != null && minecraft.gameMode != null) {
            double relativeX = mouseX - leftPos;
            double relativeY = mouseY - topPos;
            if (relativeX >= 10 && relativeX < 110 && relativeY >= 20 && relativeY < 70) {
                dragControl = DragControl.PAINT_COLOR;
                updatePaintControl(mouseX, mouseY, false);
                return true;
            }
            if (relativeX >= 10 && relativeX < 110 && relativeY >= 73 && relativeY < 89) {
                dragControl = DragControl.PAINT_SATURATION;
                updatePaintControl(mouseX, mouseY, true);
                return true;
            }
            if (relativeX >= 112 && relativeX < 122 && relativeY >= 25 && relativeY < 65) {
                int column = (int)(relativeX - 112) / 5;
                int row = (int)(relativeY - 25) / 5;
                int index = row * 2 + column;
                if (index >= 0 && index < PAINT_PALETTE.length) sendPaintColor(PAINT_PALETTE[index]);
                return true;
            }
            if (relativeX >= 125 && relativeX < 155 && relativeY >= 57 && relativeY < 70) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0x02000000);
                return true;
            }
        }
        if (button == 0 && layout == MachineLayout.ITEM_DROPPER
                && minecraft != null && minecraft.gameMode != null) {
            double relativeX = mouseX - leftPos;
            double relativeY = mouseY - topPos;
            if (relativeX >= 70 && relativeX < 168 && relativeY >= 16 && relativeY < 36) {
                dragControl = DragControl.DROP_SPEED;
                updateDropSpeed(mouseX);
                return true;
            }
            if (relativeX >= 70 && relativeX < 90 && relativeY >= 49 && relativeY < 69) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 2_000);
                return true;
            }
        }
        if (button == 0 && layout == MachineLayout.DRAWING_TABLE
                && minecraft != null && minecraft.gameMode != null) {
            double relativeX = mouseX - leftPos;
            double relativeY = mouseY - topPos;
            if (relativeX >= 8 && relativeX < 58 && relativeY >= 35 && relativeY < 49) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 4_000);
                drawingTextFocused = false;
                return true;
            }
            if (relativeX >= 116 && relativeX < 134 && relativeY >= 34 && relativeY < 51) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 4_002);
                return true;
            }
            if (relativeX >= 136 && relativeX < 154 && relativeY >= 34 && relativeY < 51) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 4_001);
                return true;
            }
            if (relativeX >= 8 && relativeX < 128 && relativeY >= 90 && relativeY < 104) {
                drawingTextFocused = menu.data(0) == 1;
                return true;
            }
            if (relativeX >= 130 && relativeX < 170 && relativeY >= 90 && relativeY < 104
                    && menu.data(0) == 1) {
                printDrawingText();
                return true;
            }
            drawingTextFocused = false;
        }
        if (button == 0 && layout == MachineLayout.PROJECTOR
                && minecraft != null && minecraft.gameMode != null) {
            double relativeX = mouseX - leftPos;
            double relativeY = mouseY - topPos;
            if (relativeY >= 65 && relativeY < 82) {
                if (relativeX >= 10 && relativeX < 30) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 4_500);
                    return true;
                }
                if (relativeX >= 146 && relativeX < 166) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 4_501);
                    return true;
                }
            }
        }
        if (button == 0 && layout == MachineLayout.VACUUM_HOPPER
                && minecraft != null && minecraft.gameMode != null) {
            double relativeX = mouseX - leftPos;
            double relativeY = mouseY - topPos;
            if (relativeX >= -22 && relativeX < 0 && relativeY >= 20 && relativeY < 42) {
                vacuumTab = 0;
                return true;
            }
            if (relativeX >= -22 && relativeX < 0 && relativeY >= 44 && relativeY < 66) {
                vacuumTab = 1;
                return true;
            }
            int[] sideX = {28, 28, 28, 44, 12, 60};
            int[] sideY = {56, 20, 38, 38, 38, 38};
            for (Direction direction : Direction.values()) {
                int index = direction.get3DDataValue();
                double sideLeft = -108 + sideX[index];
                double sideTop = 16 + sideY[index];
                if (relativeX >= sideLeft && relativeX < sideLeft + 14
                        && relativeY >= sideTop && relativeY < sideTop + 14) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId,
                            (vacuumTab == 0 ? 5_000 : 5_100) + index);
                    return true;
                }
            }
        }
        if (button == 0 && layout == MachineLayout.AUTO_ENCHANTMENT_TABLE
                && minecraft != null && minecraft.gameMode != null) {
            double relativeX = mouseX - leftPos;
            double relativeY = mouseY - topPos;
            if (relativeX >= 43 && relativeX < 90 && relativeY >= 41 && relativeY < 52) {
                dragControl = DragControl.ENCHANT_POWER;
                updateEnchantPower(mouseX);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && dragControl != DragControl.NONE
                && minecraft != null && minecraft.gameMode != null) {
            switch (dragControl) {
                case PAINT_COLOR -> updatePaintControl(mouseX, mouseY, false);
                case PAINT_SATURATION -> updatePaintControl(mouseX, mouseY, true);
                case ENCHANT_POWER -> updateEnchantPower(mouseX);
                case DROP_SPEED -> updateDropSpeed(mouseX);
                default -> { }
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && dragControl != DragControl.NONE) {
            dragControl = DragControl.NONE;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean handleIoConfigurationClick(double mouseX, double mouseY) {
        double relativeX = mouseX - leftPos;
        double relativeY = mouseY - topPos;
        for (int channel = 0; channel < ioChannelCount(); channel++) {
            int tabY = 20 + channel * 24;
            if (relativeX >= -22 && relativeX < 0 && relativeY >= tabY && relativeY < tabY + 22) {
                ioChannel = channel;
                return true;
            }
        }
        int[] sideX = {28, 28, 28, 44, 12, 60};
        int[] sideY = {56, 20, 38, 38, 38, 38};
        for (Direction direction : Direction.values()) {
            int index = direction.get3DDataValue();
            double sideLeft = -108 + sideX[index];
            double sideTop = 16 + sideY[index];
            if (relativeX >= sideLeft && relativeX < sideLeft + 14
                    && relativeY >= sideTop && relativeY < sideTop + 14) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId,
                        6_000 + ioChannel * 10 + index);
                return true;
            }
        }
        if (relativeX >= -98 && relativeX < -90 && relativeY >= 100 && relativeY < 108) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 6_100 + ioChannel);
            return true;
        }
        return false;
    }

    private void updatePaintControl(double mouseX, double mouseY, boolean saturationOnly) {
        int current = menu.data(0) & 0xFFFFFF;
        float[] hsb = Color.RGBtoHSB(current >> 16 & 0xFF, current >> 8 & 0xFF, current & 0xFF, null);
        if (saturationOnly) {
            float saturation = net.minecraft.util.Mth.clamp((float)(mouseX - leftPos - 10) / 99.0F, 0.0F, 1.0F);
            sendPaintColor(Color.HSBtoRGB(hsb[0], saturation, hsb[2]));
        } else {
            float hue = net.minecraft.util.Mth.clamp((float)(mouseX - leftPos - 10) / 99.0F, 0.0F, 1.0F);
            float brightness = 1.0F - net.minecraft.util.Mth.clamp((float)(mouseY - topPos - 20) / 49.0F, 0.0F, 1.0F);
            sendPaintColor(Color.HSBtoRGB(hue, hsb[1], brightness));
        }
    }

    private void updateEnchantPower(double mouseX) {
        int maximum = Math.max(1, menu.data(3));
        int level = maximum <= 1 ? 1 : net.minecraft.util.Mth.clamp(
                (int)Math.round((mouseX - leftPos - 43) * (maximum - 1.0D) / 47.0D) + 1,
                1, maximum);
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0x03000000 | level);
    }

    private void updateDropSpeed(double mouseX) {
        int step = net.minecraft.util.Mth.clamp((int)Math.round((mouseX - leftPos - 70) * 40.0D / 98.0D), 0, 40);
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1_000 + step * 10);
    }

    private void sendPaintColor(int color) {
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0x01000000 | color & 0xFFFFFF);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (layout == MachineLayout.DRAWING_TABLE && drawingTextFocused
                && !Character.isISOControl(codePoint) && drawingText.length() < 32) {
            drawingText += codePoint;
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (layout == MachineLayout.DRAWING_TABLE && drawingTextFocused) {
            if (keyCode == 259 && !drawingText.isEmpty()) {
                drawingText = drawingText.substring(0, drawingText.offsetByCodePoints(drawingText.length(), -1));
                return true;
            }
            if (keyCode == 257 || keyCode == 335) {
                printDrawingText();
                return true;
            }
            if (keyCode == 256) {
                drawingTextFocused = false;
                return true;
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void printDrawingText() {
        if (minecraft == null || minecraft.gameMode == null || drawingText.isEmpty()) return;
        drawingText.codePoints().limit(32).forEach(character ->
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0x04000000 | character));
        drawingText = "";
    }

    private void fluidGauge(GuiGraphics graphics, int x, int y, int width, int height,
                            int amount, int capacity, int color) {
        recessed(graphics, x, y, width, height);
        if (capacity > 0 && amount > 0) {
            int innerHeight = Math.max(1, Math.min(height - 4, amount * (height - 4) / capacity));
            graphics.fill(x + 2, y + height - 2 - innerHeight, x + width - 2, y + height - 2, color);
            graphics.fill(x + 3, y + height - 2 - innerHeight, x + width - 3,
                    y + height - 1 - innerHeight, 0x88FFFFFF);
        }
    }

    private void slotFrame(GuiGraphics graphics, int x, int y) {
        blitSprite(graphics, SLOT_SPRITE, x - 1, y - 1, 18, 18);
    }

    private void button(GuiGraphics graphics, int x, int y, int width, int height, Component label) {
        blitSprite(graphics, isHovered(x, y, width, height) ? BUTTON_HIGHLIGHTED_SPRITE : BUTTON_SPRITE,
                x, y, width, height);
        int textX = x + (width - font.width(label)) / 2;
        int textY = y + (height - 8) / 2;
        graphics.enableScissor(x + 2, y + 2, x + width - 2, y + height - 2);
        graphics.drawString(font, label, textX, textY, 0xFFFFFFFF, true);
        graphics.disableScissor();
    }

    private void disabledButton(GuiGraphics graphics, int x, int y, int width, int height, Component label) {
        blitSprite(graphics, BUTTON_DISABLED_SPRITE, x, y, width, height);
        int textX = x + (width - font.width(label)) / 2;
        int textY = y + (height - 8) / 2;
        graphics.enableScissor(x + 2, y + 2, x + width - 2, y + height - 2);
        graphics.drawString(font, label, textX, textY, 0xFFA0A0A0, false);
        graphics.disableScissor();
    }

    private void renderExperienceTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        int[] gauge = switch (layout) {
            case AUTO_ANVIL, AUTO_ENCHANTMENT_TABLE -> new int[] {140, 30, 17, 37};
            case VACUUM_HOPPER -> new int[] {140, 18, 17, 37};
            case XP_BOTTLER -> new int[] {145, 16, 13, 37};
            default -> null;
        };
        if (gauge == null) return;
        int x = leftPos + gauge[0];
        int y = topPos + gauge[1];
        if (mouseX < x || mouseX >= x + gauge[2] || mouseY < y || mouseY >= y + gauge[3]) return;
        graphics.renderTooltip(font, Component.translatable("gui.openblocks_reborn.experience_amount",
                ModFluids.fluidToXp(menu.data(0)), ModFluids.fluidToXp(menu.data(1))), mouseX, mouseY);
    }

    private void renderWaterTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (layout != MachineLayout.SPRINKLER) return;
        int x = leftPos + 137;
        int y = topPos + 20;
        if (mouseX < x || mouseX >= x + 17 || mouseY < y || mouseY >= y + 45) return;
        graphics.renderTooltip(font, Component.translatable("gui.openblocks_reborn.water_amount",
                menu.data(0), menu.data(1)), mouseX, mouseY);
    }

    private void centeredText(GuiGraphics graphics, Component text, int centerX, int y, int color) {
        graphics.drawString(font, text, centerX - font.width(text) / 2, y, color, false);
    }

    private boolean isHovered(int x, int y, int width, int height) {
        return uiMouseX >= x && uiMouseX < x + width && uiMouseY >= y && uiMouseY < y + height;
    }

    /**
     * Minecraft 1.20.1 predates the GUI sprite-atlas helper used by 1.21.
     * Draw the matching vanilla source textures directly instead of recreating
     * them with hard-coded colours. Resource packs can therefore skin these
     * controls through the same files used by vanilla containers and widgets.
     */
    private void blitSprite(GuiGraphics graphics, ResourceLocation sprite,
                            int x, int y, int width, int height) {
        if (sprite.equals(SLOT_SPRITE)) {
            graphics.blit(GENERIC_CONTAINER_TEXTURE, x, y, 7, 17, width, height);
            return;
        }
        if (sprite.equals(SLIDER_SPRITE)) {
            graphics.blitNineSliced(SLIDER_TEXTURE, x, y, width, height,
                    20, 4, 200, 20, 0, 0);
            return;
        }
        if (sprite.equals(SLIDER_HANDLE_SPRITE) || sprite.equals(SLIDER_HANDLE_HIGHLIGHTED_SPRITE)) {
            int textureY = sprite.equals(SLIDER_HANDLE_HIGHLIGHTED_SPRITE) ? 60 : 40;
            graphics.blitNineSliced(SLIDER_TEXTURE, x, y, width, height,
                    20, 4, 200, 20, 0, textureY);
            return;
        }
        int textureY = sprite.equals(BUTTON_DISABLED_SPRITE) ? 46
                : sprite.equals(BUTTON_HIGHLIGHTED_SPRITE) ? 86 : 66;
        graphics.blitNineSliced(WIDGETS_TEXTURE, x, y, width, height,
                20, 4, 200, 20, 0, textureY);
    }

    private static void arrow(GuiGraphics graphics, int x, int y, int width) {
        int height = Math.min(15, Math.max(1, Math.round(width * 15.0F / 22.0F)));
        graphics.blit(OPENBLOCKS_ARROW_TEXTURE, x, y - height / 2,
                width, height, 0.0F, 0.0F, 22, 15, 22, 15);
    }

    private static void recessed(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.blitNineSliced(MACHINE_RECESSED_TEXTURE, x, y, width, height,
                2, 2, 256, 256, 0, 0);
    }

    private static void containerPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.blitNineSliced(MACHINE_PANEL_TEXTURE, x, y, width, height,
                4, 4, 256, 256, 0, 0);
    }

    private static void bevel(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + height, color);
        graphics.blitNineSliced(MACHINE_BEVEL_FRAME_TEXTURE, x, y, width, height,
                2, 2, 256, 256, 0, 0);
    }
}
