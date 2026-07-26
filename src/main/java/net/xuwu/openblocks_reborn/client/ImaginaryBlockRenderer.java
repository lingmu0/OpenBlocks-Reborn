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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.xuwu.openblocks_reborn.OpenBlocksReborn;
import net.xuwu.openblocks_reborn.block.ImaginaryBlock;
import net.xuwu.openblocks_reborn.blockentity.ImaginaryBlockEntity;
import net.xuwu.openblocks_reborn.item.GlassesItem;

/**
 * Imaginary blocks cannot use a chunk-baked model because their visibility is
 * different for every player. This renderer applies the worn-glasses rules for
 * the local player and draws the same thin panel geometry used for collision.
 */
public class ImaginaryBlockRenderer implements BlockEntityRenderer<ImaginaryBlockEntity> {
    public ImaginaryBlockRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ImaginaryBlockEntity imaginary, float partialTick, PoseStack poses,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        var player = Minecraft.getInstance().player;
        if (player == null || !GlassesItem.testImaginary(player, imaginary,
                GlassesItem.ImaginaryProperty.VISIBLE)) return;

        ImaginaryBlock.Shape shape = imaginary.getBlockState().getValue(ImaginaryBlock.SHAPE);
        ResourceLocation texture = new ResourceLocation(OpenBlocksReborn.MOD_ID,
                switch (shape) {
                    case BLOCK -> "block/crayon_block";
                    case PANEL -> "block/crayon_panel";
                    default -> "block/crayon_half_panel";
                });
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(texture);
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
        int rgb = imaginary.getColor() == null ? 0xFFFFFF : imaginary.getColor();
        int argb = 0xE0000000 | rgb;

        switch (shape) {
            case BLOCK -> box(consumer, poses.last(), sprite,
                    new AABB(0.001D, 0.001D, 0.001D, 0.999D, 0.999D, 0.999D),
                    argb, packedLight);
            case PANEL -> box(consumer, poses.last(), sprite,
                    new AABB(0.0D, 0.9D, 0.0D, 1.0D, 1.0D, 1.0D),
                    argb, packedLight);
            case HALF -> box(consumer, poses.last(), sprite,
                    new AABB(0.0D, 0.4D, 0.0D, 1.0D, 0.5D, 1.0D),
                    argb, packedLight);
            case STAIRS -> renderStairs(imaginary, consumer, poses.last(), sprite, argb, packedLight);
        }
    }

    private static void renderStairs(ImaginaryBlockEntity imaginary, VertexConsumer consumer,
                                     PoseStack.Pose pose, TextureAtlasSprite sprite,
                                     int color, int light) {
        switch (imaginary.getBlockState().getValue(ImaginaryBlock.FACING)) {
            case SOUTH -> {
                box(consumer, pose, sprite, new AABB(0, 0.4D, 0.5D, 1, 0.5D, 1), color, light);
                box(consumer, pose, sprite, new AABB(0, 0.9D, 0, 1, 1, 0.5D), color, light);
            }
            case EAST -> {
                box(consumer, pose, sprite, new AABB(0.5D, 0.4D, 0, 1, 0.5D, 1), color, light);
                box(consumer, pose, sprite, new AABB(0, 0.9D, 0, 0.5D, 1, 1), color, light);
            }
            case WEST -> {
                box(consumer, pose, sprite, new AABB(0, 0.4D, 0, 0.5D, 0.5D, 1), color, light);
                box(consumer, pose, sprite, new AABB(0.5D, 0.9D, 0, 1, 1, 1), color, light);
            }
            default -> {
                box(consumer, pose, sprite, new AABB(0, 0.4D, 0, 1, 0.5D, 0.5D), color, light);
                box(consumer, pose, sprite, new AABB(0, 0.9D, 0.5D, 1, 1, 1), color, light);
            }
        }
    }

    private static void box(VertexConsumer out, PoseStack.Pose pose, TextureAtlasSprite sprite,
                            AABB box, int color, int light) {
        float x0 = (float)box.minX, x1 = (float)box.maxX;
        float y0 = (float)box.minY, y1 = (float)box.maxY;
        float z0 = (float)box.minZ, z1 = (float)box.maxZ;
        float u0 = sprite.getU0(), u1 = sprite.getU1();
        float v0 = sprite.getV0(), v1 = sprite.getV1();
        quad(out, pose, x0,y0,z0, x0,y1,z0, x1,y1,z0, x1,y0,z0, u0,v1,u1,v0,color,light,0,0,-1);
        quad(out, pose, x1,y0,z1, x1,y1,z1, x0,y1,z1, x0,y0,z1, u0,v1,u1,v0,color,light,0,0,1);
        quad(out, pose, x0,y0,z1, x0,y1,z1, x0,y1,z0, x0,y0,z0, u0,v1,u1,v0,color,light,-1,0,0);
        quad(out, pose, x1,y0,z0, x1,y1,z0, x1,y1,z1, x1,y0,z1, u0,v1,u1,v0,color,light,1,0,0);
        quad(out, pose, x0,y1,z0, x0,y1,z1, x1,y1,z1, x1,y1,z0, u0,v1,u1,v0,color,light,0,1,0);
        quad(out, pose, x0,y0,z1, x0,y0,z0, x1,y0,z0, x1,y0,z1, u0,v1,u1,v0,color,light,0,-1,0);
    }

    private static void quad(VertexConsumer out, PoseStack.Pose pose,
                             float x1,float y1,float z1,float x2,float y2,float z2,
                             float x3,float y3,float z3,float x4,float y4,float z4,
                             float u0,float v0,float u1,float v1,int color,int light,
                             float nx,float ny,float nz) {
        vertex(out,pose,x1,y1,z1,u0,v0,color,light,nx,ny,nz);
        vertex(out,pose,x2,y2,z2,u0,v1,color,light,nx,ny,nz);
        vertex(out,pose,x3,y3,z3,u1,v1,color,light,nx,ny,nz);
        vertex(out,pose,x4,y4,z4,u1,v0,color,light,nx,ny,nz);
    }

    private static void vertex(VertexConsumer out, PoseStack.Pose pose,
                               float x,float y,float z,float u,float v,int color,int light,
                               float nx,float ny,float nz) {
        out.vertex(pose.pose(), x, y, z).color(color).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(pose.normal(), nx, ny, nz).endVertex();
    }
}
