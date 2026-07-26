package net.xuwu.openblocks_reborn.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class CannonItemRenderer extends BlockEntityWithoutLevelRenderer {
    private final CannonModel model;

    public CannonItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        model = new CannonModel(Minecraft.getInstance().getEntityModels().bakeLayer(CannonRenderer.MODEL_LAYER));
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                             MultiBufferSource buffers, int packedLight, int packedOverlay) {
        CannonRenderer.renderModel(model, Direction.NORTH, (float)Math.toRadians(45.0D),
                poseStack, buffers, packedLight, packedOverlay);
    }
}
