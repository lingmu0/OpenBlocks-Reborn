package net.xuwu.openblocks_reborn.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.FastColor;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;
import net.xuwu.openblocks_reborn.blockentity.TankBlockEntity;

public class TankRenderer implements BlockEntityRenderer<TankBlockEntity> {
    public TankRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(TankBlockEntity tank, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                       int packedLight, int packedOverlay) {
        FluidStack fluid = tank.getTank().getFluid();
        if (fluid.isEmpty()) return;
        IClientFluidTypeExtensions properties = IClientFluidTypeExtensions.of(fluid.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(properties.getStillTexture());
        int tint = properties.getTintColor(fluid);
        int alpha = Math.max(160, FastColor.ARGB32.alpha(tint));
        int color = FastColor.ARGB32.color(alpha, FastColor.ARGB32.red(tint), FastColor.ARGB32.green(tint), FastColor.ARGB32.blue(tint));
        float min = 0.0F;
        float max = 1.0F;
        float top = Math.max(1.0F / 256.0F, fluid.getAmount() / (float)TankBlockEntity.CAPACITY);
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
        PoseStack.Pose pose = poseStack.last();
        float u0 = sprite.getU0(), u1 = sprite.getU1(), v0 = sprite.getV0(), v1 = sprite.getV1();

        quad(consumer, pose, min, top, min, max, top, min, max, top, max, min, top, max,
                u0, v0, u1, v1, color, packedLight, 0, 1, 0);
        if (!hasMatchingTank(tank, net.minecraft.core.Direction.NORTH, fluid)) {
            quad(consumer, pose, min, min, min, min, top, min, max, top, min, max, min, min,
                    u0, v1, u1, v0, color, packedLight, 0, 0, -1);
        }
        if (!hasMatchingTank(tank, net.minecraft.core.Direction.SOUTH, fluid)) {
            quad(consumer, pose, max, min, max, max, top, max, min, top, max, min, min, max,
                    u0, v1, u1, v0, color, packedLight, 0, 0, 1);
        }
        if (!hasMatchingTank(tank, net.minecraft.core.Direction.WEST, fluid)) {
            quad(consumer, pose, min, min, max, min, top, max, min, top, min, min, min, min,
                    u0, v1, u1, v0, color, packedLight, -1, 0, 0);
        }
        if (!hasMatchingTank(tank, net.minecraft.core.Direction.EAST, fluid)) {
            quad(consumer, pose, max, min, min, max, top, min, max, top, max, max, min, max,
                    u0, v1, u1, v0, color, packedLight, 1, 0, 0);
        }
    }

    private static boolean hasMatchingTank(TankBlockEntity tank, net.minecraft.core.Direction direction, FluidStack fluid) {
        if (tank.getLevel() == null
                || !(tank.getLevel().getBlockEntity(tank.getBlockPos().relative(direction)) instanceof TankBlockEntity other)) {
            return false;
        }
        FluidStack otherFluid = other.getTank().getFluid();
        return !otherFluid.isEmpty() && fluid.isFluidEqual(otherFluid);
    }

    private static void quad(VertexConsumer consumer, PoseStack.Pose pose,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float x3, float y3, float z3, float x4, float y4, float z4,
                             float u0, float v0, float u1, float v1, int color, int light,
                             float nx, float ny, float nz) {
        vertex(consumer, pose, x1, y1, z1, u0, v0, color, light, nx, ny, nz);
        vertex(consumer, pose, x2, y2, z2, u1, v0, color, light, nx, ny, nz);
        vertex(consumer, pose, x3, y3, z3, u1, v1, color, light, nx, ny, nz);
        vertex(consumer, pose, x4, y4, z4, u0, v1, color, light, nx, ny, nz);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z,
                               float u, float v, int color, int light, float nx, float ny, float nz) {
        consumer.vertex(pose.pose(), x, y, z).color(color).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), nx, ny, nz).endVertex();
    }
}
