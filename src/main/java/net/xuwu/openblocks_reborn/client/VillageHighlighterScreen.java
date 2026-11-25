package net.xuwu.openblocks_reborn.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.xuwu.openblocks_reborn.config.VillageHighlighterConfig;

/** Personal display preferences; opening this screen never pauses the world. */
public final class VillageHighlighterScreen extends Screen {
    private Button snapshotButton;
    public VillageHighlighterScreen() { super(Component.translatable("highlighter.openblocks_reborn.title")); }

    @Override protected void init() {
        int left = width / 2 - 144, top = height / 2 - 120;
        addRenderableWidget(Button.builder(enabledText(), button -> {
            VillageHighlighterClient.enabled = !VillageHighlighterClient.enabled;
            button.setMessage(enabledText());
            VillageHighlighterClient.invalidate();
        }).bounds(left, top + 28, 288, 20).build());
        addRenderableWidget(Button.builder(modeText(), button -> {
            VillageHighlighterConfig.ALL.set(!VillageHighlighterConfig.ALL.get());
            VillageHighlighterConfig.ALL.save();
            VillageHighlighterClient.invalidate();
            button.setMessage(modeText());
            updateSnapshotButton();
        }).bounds(left, top + 58, 140, 20).build());
        addRenderableWidget(Button.builder(radiusText(), button -> {
            VillageHighlighterConfig.RADIUS.set(VillageHighlighterConfig.RADIUS.get() == 16 ? 32 : 16);
            VillageHighlighterConfig.RADIUS.save();
            VillageHighlighterClient.invalidate();
            button.setMessage(radiusText());
        }).bounds(left + 148, top + 58, 140, 20).build());
        toggle("pois", VillageHighlighterConfig.POIS, left, top + 88, 140);
        toggle("links", VillageHighlighterConfig.LINKS, left + 148, top + 88, 140);
        toggle("golems", VillageHighlighterConfig.GOLEMS, left, top + 118, 140);
        toggle("village", VillageHighlighterConfig.VILLAGE, left + 148, top + 118, 140);
        toggle("through_walls", VillageHighlighterConfig.THROUGH_WALLS, left, top + 148, 288);
        snapshotButton = addRenderableWidget(Button.builder(Component.empty(), button -> VillageHighlighterClient.requestSnapshot())
                .bounds(left, top + 178, 288, 20).build());
        updateSnapshotButton();
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(left, top + 208, 288, 20).build());
    }

    @Override public void tick() { super.tick(); updateSnapshotButton(); }
    private void updateSnapshotButton() {
        if (snapshotButton == null) return;
        int wait = VillageHighlighterClient.snapshotWaitSeconds();
        snapshotButton.active = VillageHighlighterClient.enabled && VillageHighlighterConfig.ALL.get() && wait == 0;
        snapshotButton.setMessage(wait > 0 ? Component.translatable("highlighter.openblocks_reborn.cooldown", wait)
                : Component.translatable("highlighter.openblocks_reborn.request"));
    }

    private void toggle(String name, ModConfigSpec.BooleanValue value, int x, int y, int width) {
        addRenderableWidget(Button.builder(option(name, value.get()), button -> {
            value.set(!value.get()); value.save();
            if (name.equals("village")) VillageHighlighterClient.invalidate();
            button.setMessage(option(name, value.get()));
        }).bounds(x, y, width, 20).build());
    }
    private static Component option(String name, boolean value) {
        return Component.translatable("highlighter.openblocks_reborn.option",
                Component.translatable("highlighter.openblocks_reborn." + name),
                Component.translatable(value ? "options.on" : "options.off"));
    }
    private static Component enabledText() { return option("display", VillageHighlighterClient.enabled); }
    private static Component modeText() {
        return Component.translatable("highlighter.openblocks_reborn.mode",
                Component.translatable("highlighter.openblocks_reborn." + (VillageHighlighterConfig.ALL.get() ? "all" : "single")));
    }
    private static Component radiusText() {
        return Component.translatable("highlighter.openblocks_reborn.radius", VillageHighlighterConfig.RADIUS.get());
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        int top = height / 2 - 120;
        graphics.fill(width / 2 - 152, top, width / 2 + 152, top + 236, 0xC0202020);
        graphics.drawCenteredString(font, title, width / 2, top + 10, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
