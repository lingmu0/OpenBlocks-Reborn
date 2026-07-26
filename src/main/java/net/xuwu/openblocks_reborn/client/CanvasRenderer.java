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
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.xuwu.openblocks_reborn.OpenBlocksReborn;
import net.xuwu.openblocks_reborn.blockentity.CanvasBlockEntity;
import net.xuwu.openblocks_reborn.item.StencilItem;

public class CanvasRenderer implements BlockEntityRenderer<CanvasBlockEntity> {
    public CanvasRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(CanvasBlockEntity canvas, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                       int packedLight, int packedOverlay) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(ResourceLocation.withDefaultNamespace("block/white_wool"));
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
        float e = 0.001F, a = -e, b = 1.0F + e;
        for (Direction face : Direction.values()) {
            for (int y = 0; y < CanvasBlockEntity.SIDE_SIZE; y++) {
                for (int x = 0; x < CanvasBlockEntity.SIDE_SIZE; x++) {
                    int color = canvas.getPixel(face, x, y);
                    if (color != -1) pixelQuad(consumer, poseStack.last(), face, x, y,
                            sprite, color, packedLight, a, b);
                }
            }
        }

        TextureAtlasSprite stencilSprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(new ResourceLocation(OpenBlocksReborn.MOD_ID, "item/stencil"));
        VertexConsumer stencilConsumer = buffers.getBuffer(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS));
        float stencilOffset = 0.002F;
        for (Direction face : Direction.values()) {
            int pattern = canvas.getStencilPattern(face);
            if (pattern < 0) continue;
            int rotation = canvas.getStencilRotation(face);
            for (int y = 0; y < CanvasBlockEntity.SIDE_SIZE; y++) {
                for (int x = 0; x < CanvasBlockEntity.SIDE_SIZE; x++) {
                    if (!StencilItem.isHole(pattern, rotation, x, y)) {
                        pixelQuad(stencilConsumer, poseStack.last(), face, x, y, stencilSprite,
                                0xFFFFFFFF, packedLight, -stencilOffset, 1.0F + stencilOffset);
                    }
                }
            }
        }
    }

    private static void pixelQuad(VertexConsumer consumer, PoseStack.Pose pose, Direction face, int pixelX, int pixelY,
                                  TextureAtlasSprite sprite, int color, int light, float outsideLow, float outsideHigh) {
        float lo = pixelX / 16.0F;
        float hi = (pixelX + 1) / 16.0F;
        float top = 1.0F - pixelY / 16.0F;
        float bottom = 1.0F - (pixelY + 1) / 16.0F;
        switch (face) {
            case NORTH -> quad(consumer, pose, 1-hi,bottom,outsideLow, 1-hi,top,outsideLow,
                    1-lo,top,outsideLow, 1-lo,bottom,outsideLow, sprite,color,light,0,0,-1);
            case SOUTH -> quad(consumer, pose, hi,bottom,outsideHigh, hi,top,outsideHigh,
                    lo,top,outsideHigh, lo,bottom,outsideHigh, sprite,color,light,0,0,1);
            case WEST -> quad(consumer, pose, outsideLow,bottom,hi, outsideLow,top,hi,
                    outsideLow,top,lo, outsideLow,bottom,lo, sprite,color,light,-1,0,0);
            case EAST -> quad(consumer, pose, outsideHigh,bottom,1-hi, outsideHigh,top,1-hi,
                    outsideHigh,top,1-lo, outsideHigh,bottom,1-lo, sprite,color,light,1,0,0);
            case UP -> quad(consumer, pose, lo,outsideHigh,bottom, lo,outsideHigh,top,
                    hi,outsideHigh,top, hi,outsideHigh,bottom, sprite,color,light,0,1,0);
            case DOWN -> quad(consumer, pose, lo,outsideLow,1-bottom, lo,outsideLow,1-top,
                    hi,outsideLow,1-top, hi,outsideLow,1-bottom, sprite,color,light,0,-1,0);
        }
    }

    private static void quad(VertexConsumer c, PoseStack.Pose pose,
                             float x1,float y1,float z1,float x2,float y2,float z2,
                             float x3,float y3,float z3,float x4,float y4,float z4,
                             TextureAtlasSprite s,int color,int light,float nx,float ny,float nz) {
        vertex(c,pose,x1,y1,z1,s.getU0(),s.getV1(),color,light,nx,ny,nz);
        vertex(c,pose,x2,y2,z2,s.getU0(),s.getV0(),color,light,nx,ny,nz);
        vertex(c,pose,x3,y3,z3,s.getU1(),s.getV0(),color,light,nx,ny,nz);
        vertex(c,pose,x4,y4,z4,s.getU1(),s.getV1(),color,light,nx,ny,nz);
    }

    private static void vertex(VertexConsumer c, PoseStack.Pose pose,float x,float y,float z,float u,float v,
                               int color,int light,float nx,float ny,float nz) {
        c.vertex(pose.pose(),x,y,z).color(color).uv(u,v).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light).normal(pose.normal(),nx,ny,nz).endVertex();
    }
}
