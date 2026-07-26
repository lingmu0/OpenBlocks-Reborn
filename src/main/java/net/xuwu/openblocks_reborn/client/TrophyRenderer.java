package net.xuwu.openblocks_reborn.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.xuwu.openblocks_reborn.blockentity.TrophyBlockEntity;

public class TrophyRenderer implements BlockEntityRenderer<TrophyBlockEntity> {
    private final EntityRenderDispatcher dispatcher;

    public TrophyRenderer(BlockEntityRendererProvider.Context context) {
        dispatcher = context.getEntityRenderer();
    }

    @Override
    public void render(TrophyBlockEntity trophy, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                       int packedLight, int packedOverlay) {
        Entity entity = trophy.getOrCreateDisplayEntity();
        if (entity == null || trophy.getLevel() == null) return;
        double spin = trophy.getLevel().getGameTime() / 10.0D;
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.25D, 0.0D);
        renderDisplayEntity(partialTick, poseStack, buffers, packedLight, entity,
                dispatcher, spin);
        poseStack.popPose();
    }

    public static void renderDisplayEntity(float partialTick, PoseStack poseStack,
                                           MultiBufferSource buffers, int packedLight,
                                           Entity entity, EntityRenderDispatcher dispatcher,
                                           double spin) {
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.15D, 0.5D);
        float largest = Math.max(1.0F, Math.max(entity.getBbWidth(), entity.getBbHeight()));
        float scale = 0.65F / largest;
        poseStack.scale(scale, scale, scale);
        poseStack.mulPose(Axis.YP.rotationDegrees((float)(spin * 18.0D)));
        dispatcher.render(entity, 0.0D, 0.0D, 0.0D, 0.0F,
                partialTick, poseStack, buffers, packedLight);
        poseStack.popPose();
    }
}
