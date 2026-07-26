package net.xuwu.openblocks_reborn.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.AABB;
import net.xuwu.openblocks_reborn.block.UtilityMachineBlock;
import net.xuwu.openblocks_reborn.blockentity.UtilityMachineBlockEntity;
import net.xuwu.openblocks_reborn.item.HeightMapItem;

public class ProjectorRenderer implements BlockEntityRenderer<UtilityMachineBlockEntity> {
    public ProjectorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(UtilityMachineBlockEntity projector, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        if (!(projector.getBlockState().getBlock() instanceof UtilityMachineBlock block)
                || block.kind() != UtilityMachineBlock.Kind.PROJECTOR
                || !projector.isProjectorActive()) return;
        int[] heights = HeightMapItem.getHeights(projector.getInventory().getStackInSlot(0));
        if (heights.length != 81) return;
        int minimum = java.util.Arrays.stream(heights).min().orElse(0);
        int maximum = java.util.Arrays.stream(heights).max().orElse(minimum + 1);
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F * projector.getProjectorRotation()));
        poseStack.translate(-0.5D, 0.0D, -0.5D);
        double cell = 1.0D / 9.0D;
        for (int z = 0; z < 9; z++) {
            for (int x = 0; x < 9; x++) {
                int value = heights[z * 9 + x];
                double height = maximum == minimum ? 1.0D
                        : 0.15D + (value - minimum) * 2.85D / (maximum - minimum);
                AABB column = new AABB(x * cell, 0.78D, z * cell,
                        (x + 1) * cell, 0.78D + height, (z + 1) * cell);
                LevelRenderer.renderLineBox(poseStack, lines, column,
                        0.20F, 0.95F, 0.55F, 0.72F);
            }
        }
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(UtilityMachineBlockEntity blockEntity) {
        return blockEntity.isProjectorActive();
    }
}
