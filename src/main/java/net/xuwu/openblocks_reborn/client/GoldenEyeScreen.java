package net.xuwu.openblocks_reborn.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.xuwu.openblocks_reborn.menu.GoldenEyeMenu;
import net.xuwu.openblocks_reborn.network.GoldenEyeSelectPayload;
import net.xuwu.openblocks_reborn.network.ModNetworking;

import java.util.List;
import java.util.Locale;

public final class GoldenEyeScreen extends AbstractContainerScreen<GoldenEyeMenu> {
    private static final int SURFACE = 0xFFC6C6C6;
    private static final int DARK = 0xFF373737;
    private static final int SHADOW = 0xFF555555;
    private static final int LIGHT = 0xFFFFFFFF;
    private static final int RECESS = 0xFF8B8B8B;
    private static final int TEXT = 0xFF404040;
    private static final int ROWS = 8;
    private static final int ROW_HEIGHT = 16;
    private EditBox search;
    private List<ResourceLocation> filtered;
    private int scroll;

    public GoldenEyeScreen(GoldenEyeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 260;
        imageHeight = 190;
        titleLabelX = 10;
        titleLabelY = 7;
        filtered = menu.structures();
    }

    @Override
    protected void init() {
        super.init();
        search = new EditBox(font, leftPos + 11, topPos + 21, imageWidth - 22, 18,
                Component.translatable("gui.openblocks_reborn.golden_eye.search"));
        search.setMaxLength(256);
        search.setResponder(this::filterStructures);
        search.setHint(Component.translatable("gui.openblocks_reborn.golden_eye.search"));
        addRenderableWidget(search);
        setInitialFocus(search);
    }

    private void filterStructures(String query) {
        String needle = query.strip().toLowerCase(Locale.ROOT);
        filtered = needle.isEmpty() ? menu.structures() : menu.structures().stream()
                .filter(id -> id.toString().toLowerCase(Locale.ROOT).contains(needle))
                .toList();
        scroll = 0;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        roundedPanel(graphics, leftPos, topPos, imageWidth, imageHeight);
        recessed(graphics, leftPos + 9, topPos + 43, imageWidth - 18, ROWS * ROW_HEIGHT + 4);

        int listX = leftPos + 12;
        int listY = topPos + 46;
        int listWidth = imageWidth - 29;
        graphics.enableScissor(listX, listY, listX + listWidth, listY + ROWS * ROW_HEIGHT);
        for (int row = 0; row < ROWS; row++) {
            int index = scroll + row;
            if (index >= filtered.size()) break;
            int y = listY + row * ROW_HEIGHT;
            boolean hovered = mouseX >= listX && mouseX < listX + listWidth
                    && mouseY >= y && mouseY < y + ROW_HEIGHT;
            graphics.fill(listX, y, listX + listWidth, y + ROW_HEIGHT - 1,
                    hovered ? 0xFFB5B5B5 : (row % 2 == 0 ? 0xFF929292 : 0xFF898989));
            graphics.drawString(font, filtered.get(index).toString(), listX + 4, y + 4,
                    hovered ? 0xFFFFFFFF : 0xFF202020, false);
        }
        graphics.disableScissor();
        renderScrollBar(graphics);
    }

    private void renderScrollBar(GuiGraphics graphics) {
        int x = leftPos + imageWidth - 15;
        int y = topPos + 46;
        int height = ROWS * ROW_HEIGHT;
        graphics.fill(x, y, x + 5, y + height, 0xFF6E6E6E);
        int maximum = Math.max(0, filtered.size() - ROWS);
        int thumbHeight = maximum == 0 ? height : Math.max(12, height * ROWS / filtered.size());
        int thumbY = y + (maximum == 0 ? 0 : scroll * (height - thumbHeight) / maximum);
        graphics.fill(x + 1, thumbY, x + 4, thumbY + thumbHeight, maximum == 0 ? SHADOW : LIGHT);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        graphics.drawString(font, Component.translatable("gui.openblocks_reborn.golden_eye.results",
                filtered.size()), 10, 177, TEXT, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int listX = leftPos + 12;
            int listY = topPos + 46;
            int listWidth = imageWidth - 29;
            if (mouseX >= listX && mouseX < listX + listWidth
                    && mouseY >= listY && mouseY < listY + ROWS * ROW_HEIGHT) {
                int index = scroll + (int)(mouseY - listY) / ROW_HEIGHT;
                if (index >= 0 && index < filtered.size()) {
                    ModNetworking.sendToServer(new GoldenEyeSelectPayload(
                            menu.containerId, filtered.get(index).toString()));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (mouseX >= leftPos + 9 && mouseX < leftPos + imageWidth - 9
                && mouseY >= topPos + 43 && mouseY < topPos + 175) {
            int maximum = Math.max(0, filtered.size() - ROWS);
            scroll = net.minecraft.util.Mth.clamp(scroll - (int)Math.signum(scrollY), 0, maximum);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollY);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    private static void roundedPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x + 2, y, x + width - 2, y + height, SURFACE);
        graphics.fill(x, y + 2, x + width, y + height - 2, SURFACE);
        graphics.fill(x + 2, y, x + width - 2, y + 1, LIGHT);
        graphics.fill(x, y + 2, x + 1, y + height - 2, LIGHT);
        graphics.fill(x + 2, y + height - 1, x + width - 2, y + height, DARK);
        graphics.fill(x + width - 1, y + 2, x + width, y + height - 2, DARK);
    }

    private static void recessed(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, DARK);
        graphics.fill(x + 2, y + 2, x + width, y + height, LIGHT);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, RECESS);
    }
}
