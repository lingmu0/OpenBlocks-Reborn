package net.xuwu.openblocks_reborn.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.xuwu.openblocks_reborn.entity.LuggageEntity;

public class LuggageRenderer extends EntityRenderer<LuggageEntity> {
    private final net.minecraft.client.renderer.entity.ItemRenderer itemRenderer;

    public LuggageRenderer(EntityRendererProvider.Context context) {
        super(context);
        itemRenderer = context.getItemRenderer();
        shadowRadius = 0.35F;
    }

    @Override
    public void render(LuggageEntity luggage, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight) {
        poseStack.pushPose();
        // The baked item model includes the original luggage's many short legs.
        // Lift it clear of the entity origin so those legs are not buried in the floor.
        poseStack.translate(0.0F, 0.22F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));
        poseStack.scale(0.85F, 0.85F, 0.85F);
        itemRenderer.renderStatic(luggage.getItem(), ItemDisplayContext.FIXED, packedLight,
                OverlayTexture.NO_OVERLAY, poseStack, buffers, luggage.level(), luggage.getId());
        poseStack.popPose();
        super.render(luggage, entityYaw, partialTick, poseStack, buffers, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(LuggageEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/atlas/blocks.png");
    }
}
