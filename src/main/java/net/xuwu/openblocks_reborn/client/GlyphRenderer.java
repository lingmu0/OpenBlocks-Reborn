package net.xuwu.openblocks_reborn.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.xuwu.openblocks_reborn.entity.GlyphEntity;

public class GlyphRenderer extends EntityRenderer<GlyphEntity> {
    private static final ResourceLocation FONT_TEXTURE = ResourceLocation.withDefaultNamespace("textures/font/ascii.png");

    public GlyphRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(GlyphEntity glyph, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        String text = new String(Character.toChars(glyph.getCharacter()));
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));
        poseStack.translate(0.0F, 0.0F, -0.002F);
        poseStack.scale(-0.0625F, -0.0625F, 0.0625F);
        float x = -getFont().width(text) / 2.0F;
        getFont().drawInBatch(text, x, -4.0F, 0xFF111111, false, poseStack.last().pose(), buffer,
                Font.DisplayMode.POLYGON_OFFSET, 0, packedLight);
        poseStack.popPose();
        super.render(glyph, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(GlyphEntity glyph) {
        return FONT_TEXTURE;
    }
}
