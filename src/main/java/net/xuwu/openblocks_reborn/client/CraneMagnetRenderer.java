package net.xuwu.openblocks_reborn.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.xuwu.openblocks_reborn.OpenBlocksReborn;

/** Exact legacy 6/4/2-pixel three-tier magnet with the original texture offsets. */
public final class CraneMagnetRenderer {
    private static final float MODEL_HEIGHT = 6.0F / 16.0F;
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            OpenBlocksReborn.MOD_ID, "textures/models/magnet.png");
    private static ModelPart model;

    public static void render(PoseStack poses, MultiBufferSource buffers,
                              float x, float y, float z, int light) {
        if (model == null) {
            model = Minecraft.getInstance().getEntityModels()
                    .bakeLayer(WearableArmorModels.CRANE_MAGNET).getChild("magnet");
        }
        poses.pushPose();
        // The supplied position is the cable attachment point. Place the narrow
        // 2x2 tier directly below it, followed by the 4x4 and 6x6 base tiers.
        poses.translate(x, y - MODEL_HEIGHT, z);
        // Legacy ModelRenderer.render(1/8) made each model pixel 1/8 block.
        // Modern ModelPart already uses 1/16, so the equivalent scale is 2.
        poses.scale(2.0F, 2.0F, 2.0F);
        model.render(poses, buffers.getBuffer(renderType()),
                light, OverlayTexture.NO_OVERLAY);
        poses.popPose();
    }

    public static RenderType renderType() {
        return RenderType.entityCutoutNoCull(TEXTURE);
    }

    private CraneMagnetRenderer() {
    }
}
