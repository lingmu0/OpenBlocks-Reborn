package net.xuwu.openblocks_reborn.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.SpawnerRenderer;
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
        SpawnerRenderer.renderEntityInSpawner(partialTick, poseStack, buffers, packedLight, entity, dispatcher, spin - 0.1D, spin);
        poseStack.popPose();
    }
}
