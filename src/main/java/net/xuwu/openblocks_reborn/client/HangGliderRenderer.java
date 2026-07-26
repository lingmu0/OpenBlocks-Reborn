package net.xuwu.openblocks_reborn.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.xuwu.openblocks_reborn.OpenBlocksReborn;
import net.xuwu.openblocks_reborn.entity.HangGliderEntity;

public class HangGliderRenderer extends EntityRenderer<HangGliderEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            OpenBlocksReborn.MOD_ID, "textures/models/hang_glider.png");

    public HangGliderRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(HangGliderEntity glider, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight) {
        poseStack.pushPose();
        Player owner = glider.getOwner();
        float renderedYaw = entityYaw;
        if (owner != null) {
            // The attached entity and the network player are interpolated independently.
            // Rendering from the glider's packet position made their small timing
            // differences visible as shaking. Anchor to the owner's interpolated pose.
            double ownerX = Mth.lerp(partialTick, owner.xo, owner.getX());
            double ownerY = Mth.lerp(partialTick, owner.yo, owner.getY())
                    + (glider.isFlying() ? 2.1D : 1.25D);
            double ownerZ = Mth.lerp(partialTick, owner.zo, owner.getZ());
            double gliderX = Mth.lerp(partialTick, glider.xo, glider.getX());
            double gliderY = Mth.lerp(partialTick, glider.yo, glider.getY());
            double gliderZ = Mth.lerp(partialTick, glider.zo, glider.getZ());
            poseStack.translate(ownerX - gliderX, ownerY - gliderY, ownerZ - gliderZ);
            renderedYaw = Mth.rotLerp(partialTick, owner.yBodyRotO, owner.yBodyRot);
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - renderedYaw));
        if (!glider.isFlying()) {
            poseStack.translate(0.0F, 0.15F, 0.3F);
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            poseStack.scale(0.4F, 0.4F, 0.4F);
        } else {
            poseStack.translate(0.0F, -0.35F, -0.75F);
        }
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer vertices = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        float size = 2.4F;
        vertex(vertices, pose, size, 0.0F, size, 1.0F, 1.0F, packedLight);
        vertex(vertices, pose, -size, 0.0F, size, 0.0F, 1.0F, packedLight);
        vertex(vertices, pose, -size, 0.0F, -size, 0.0F, 0.0F, packedLight);
        vertex(vertices, pose, size, 0.0F, -size, 1.0F, 0.0F, packedLight);
        poseStack.popPose();
        super.render(glider, entityYaw, partialTick, poseStack, buffers, packedLight);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z,
                               float u, float v, int light) {
        consumer.addVertex(pose, x, y, z).setColor(0xFFFFFFFF).setUv(u, v)
                .setOverlay(0).setLight(light).setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(HangGliderEntity entity) {
        return TEXTURE;
    }
}
