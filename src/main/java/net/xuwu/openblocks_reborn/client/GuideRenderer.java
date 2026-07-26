package net.xuwu.openblocks_reborn.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.xuwu.openblocks_reborn.blockentity.GuideBlockEntity;

public class GuideRenderer implements BlockEntityRenderer<GuideBlockEntity> {
    public GuideRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(GuideBlockEntity guide, float partialTick, PoseStack poses,
                       MultiBufferSource buffers, int light, int overlay) {
        if (!guide.isActive()) return;
        int color = guide.getColor();
        float red = (color >> 16 & 0xFF) / 255.0F;
        float green = (color >> 8 & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        /*
         * BufferSource usually backs both of these non-fixed render types with the same
         * BufferBuilder. Asking for the line buffer immediately after the filled buffer
         * ends the filled batch, so retaining both consumers and writing to them in one
         * loop crashes in BufferBuilder.ensureBuilding(). Submit the complete filled
         * batch before requesting the line consumer.
         */
        var filled = buffers.getBuffer(RenderType.debugFilledBox());
        for (BlockPos position : guide.getShapePositions()) {
            float minX = position.getX() + 0.06F;
            float minY = position.getY() + 0.06F;
            float minZ = position.getZ() + 0.06F;
            float maxX = position.getX() + 0.94F;
            float maxY = position.getY() + 0.94F;
            float maxZ = position.getZ() + 0.94F;
            LevelRenderer.addChainedFilledBoxVertices(poses, filled,
                    minX, minY, minZ, maxX, maxY, maxZ,
                    red, green, blue, 0.16F);
        }
        var lines = buffers.getBuffer(RenderType.lines());
        for (BlockPos position : guide.getShapePositions()) {
            float minX = position.getX() + 0.06F;
            float minY = position.getY() + 0.06F;
            float minZ = position.getZ() + 0.06F;
            float maxX = position.getX() + 0.94F;
            float maxY = position.getY() + 0.94F;
            float maxZ = position.getZ() + 0.94F;
            LevelRenderer.renderLineBox(poses, lines,
                    minX, minY, minZ, maxX, maxY, maxZ,
                    red, green, blue, 0.65F);
        }
    }

    @Override
    public boolean shouldRenderOffScreen(GuideBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public AABB getRenderBoundingBox(GuideBlockEntity guide) {
        AABB bounds = new AABB(guide.getBlockPos());
        for (BlockPos position : guide.getShapePositions()) {
            bounds = bounds.minmax(new AABB(guide.getBlockPos().offset(position)));
        }
        return bounds.inflate(1.0D);
    }
}
