// Derived from ElevatorMod 1.21.1-1.11.4 by VsnGamer (MIT); see LICENSE-ElevatorMod.
package net.xuwu.openblocks_reborn.elevator.client.gui;

import net.xuwu.openblocks_reborn.elevator.ElevatorBlockBase;
import net.xuwu.openblocks_reborn.elevator.ElevatorMod;
import net.xuwu.openblocks_reborn.elevator.level.ElevatorBlockEntityBase;
import net.xuwu.openblocks_reborn.elevator.network.ClientPacketSender;
import net.xuwu.openblocks_reborn.elevator.network.client.RemoveCamoPacket;
import net.xuwu.openblocks_reborn.elevator.network.client.SetArrowPacket;
import net.xuwu.openblocks_reborn.elevator.network.client.SetDirectionalPacket;
import net.xuwu.openblocks_reborn.elevator.level.ElevatorContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;


public class ElevatorScreen extends AbstractContainerScreen<ElevatorContainer> {

    private final ResourceLocation GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath(ElevatorMod.ID, "textures/gui/elevator_gui.png");
    private final ElevatorBlockEntityBase tile;
    private final Direction playerFacing;
    private final ClientPacketSender packetSender;

    private Checkbox dirButton;
    private Checkbox hideArrowButton;
    private Button resetCamoButton;
    private FacingControllerWrapper facingController;

    public ElevatorScreen(ElevatorContainer container, Inventory inv, Component titleIn, ClientPacketSender pktSender) {
        super(container, inv, titleIn);
        imageWidth = 200;
        imageHeight = 100;

        tile = container.getTile();
        playerFacing = container.getPlayerFacing();

        packetSender = pktSender;
    }

    @Override
    public void init() {
        super.init();

        // Toggle directional button
        dirButton = Checkbox.builder(Component.translatable("screen.openblocks_reborn.elevator.directional"), font)
                .pos(leftPos + 8, topPos + 25)
                .selected(tile.getBlockState().getValue(ElevatorBlockBase.DIRECTIONAL))
                .onValueChange((checkbox, selected) -> packetSender.sendToServer(new SetDirectionalPacket(selected, tile.getBlockPos())))
                .build();
        addRenderableWidget(dirButton);

        // Toggle arrow button
        hideArrowButton = Checkbox.builder(Component.translatable("screen.openblocks_reborn.elevator.hide_arrow"), font)
                .pos(leftPos + 8, topPos + 50)
                .selected(!tile.getBlockState().getValue(ElevatorBlockBase.SHOW_ARROW))
                .onValueChange((checkbox, selected) -> packetSender.sendToServer(new SetArrowPacket(!selected, tile.getBlockPos())))
                .build();
        hideArrowButton.visible = tile.getBlockState().getValue(ElevatorBlockBase.DIRECTIONAL);
        addRenderableWidget(hideArrowButton);

        // Directional controller
        facingController = new FacingControllerWrapper(leftPos + 120, topPos + 20, tile.getBlockPos(), playerFacing, packetSender);
        facingController.getButtons().forEach(this::addRenderableWidget);
        facingController.getButtons().forEach(button -> {
            button.visible = tile.getBlockState().getValue(ElevatorBlockBase.DIRECTIONAL);
            button.active = tile.getBlockState().getValue(ElevatorBlockBase.FACING) != button.direction;
        });

        // Reset camouflage button
        resetCamoButton = Button.builder(
                        Component.translatable("screen.openblocks_reborn.elevator.reset_camo"),
                        but -> packetSender.sendToServer(new RemoveCamoPacket(tile.getBlockPos()))
                )
                .pos(leftPos + 8, topPos + 75)
                .size(110, 20)
                .build();
        addRenderableWidget(resetCamoButton);
        resetCamoButton.active = tile.getHeldState() != null;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public void containerTick() {
        super.containerTick();

        ((net.xuwu.openblocks_reborn.mixin.client.CheckboxAccessor)(Object)dirButton)
                .openblocks$setSelected(tile.getBlockState().getValue(ElevatorBlockBase.DIRECTIONAL));

        facingController.getButtons().forEach(button -> {
            button.visible = tile.getBlockState().getValue(ElevatorBlockBase.DIRECTIONAL);
            button.active = tile.getBlockState().getValue(ElevatorBlockBase.FACING) != button.direction;
        });

        hideArrowButton.visible = tile.getBlockState().getValue(ElevatorBlockBase.DIRECTIONAL);
        ((net.xuwu.openblocks_reborn.mixin.client.CheckboxAccessor)(Object)hideArrowButton)
                .openblocks$setSelected(!tile.getBlockState().getValue(ElevatorBlockBase.SHOW_ARROW));

        resetCamoButton.active = tile.getHeldState() != null;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float v, int mouseX, int mouseY) {
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(GUI_TEXTURE, relX, relY, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, 8, 8, 14737632);
    }
}
