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
import net.minecraft.world.phys.Vec3;
import net.xuwu.openblocks_reborn.block.SkyBlock;
import net.xuwu.openblocks_reborn.blockentity.SkyBlockEntity;

public class SkyBlockRenderer implements BlockEntityRenderer<SkyBlockEntity> {
    public SkyBlockRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(SkyBlockEntity sky, float partialTick, PoseStack poses, MultiBufferSource buffers,
                       int packedLight, int packedOverlay) {
        if (!SkyBlock.isActive(sky.getBlockState()) || sky.getLevel() == null) return;
        var clientLevel = Minecraft.getInstance().level;
        if (clientLevel == null) return;
        Vec3 color = clientLevel.getSkyColor(Vec3.atCenterOf(sky.getBlockPos()), partialTick);
        int argb = 0xE0000000
                | (int)(Math.clamp(color.x, 0.0D, 1.0D) * 255.0D) << 16
                | (int)(Math.clamp(color.y, 0.0D, 1.0D) * 255.0D) << 8
                | (int)(Math.clamp(color.z, 0.0D, 1.0D) * 255.0D);
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(ResourceLocation.withDefaultNamespace("block/white_concrete"));
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
        PoseStack.Pose pose = poses.last();
        float u0 = sprite.getU0(), u1 = sprite.getU1(), v0 = sprite.getV0(), v1 = sprite.getV1();
        quad(consumer, pose, 0,0,0, 0,1,0, 1,1,0, 1,0,0, u0,v1,u1,v0,argb,packedLight,0,0,-1);
        quad(consumer, pose, 1,0,1, 1,1,1, 0,1,1, 0,0,1, u0,v1,u1,v0,argb,packedLight,0,0,1);
        quad(consumer, pose, 0,0,1, 0,1,1, 0,1,0, 0,0,0, u0,v1,u1,v0,argb,packedLight,-1,0,0);
        quad(consumer, pose, 1,0,0, 1,1,0, 1,1,1, 1,0,1, u0,v1,u1,v0,argb,packedLight,1,0,0);
        quad(consumer, pose, 0,1,0, 0,1,1, 1,1,1, 1,1,0, u0,v1,u1,v0,argb,packedLight,0,1,0);
        quad(consumer, pose, 0,0,1, 0,0,0, 1,0,0, 1,0,1, u0,v1,u1,v0,argb,packedLight,0,-1,0);
    }

    private static void quad(VertexConsumer consumer, PoseStack.Pose pose,
                             float x1,float y1,float z1,float x2,float y2,float z2,
                             float x3,float y3,float z3,float x4,float y4,float z4,
                             float u0,float v0,float u1,float v1,int color,int light,
                             float nx,float ny,float nz) {
        vertex(consumer,pose,x1,y1,z1,u0,v0,color,light,nx,ny,nz);
        vertex(consumer,pose,x2,y2,z2,u0,v1,color,light,nx,ny,nz);
        vertex(consumer,pose,x3,y3,z3,u1,v1,color,light,nx,ny,nz);
        vertex(consumer,pose,x4,y4,z4,u1,v0,color,light,nx,ny,nz);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose,
                               float x,float y,float z,float u,float v,int color,int light,
                               float nx,float ny,float nz) {
        consumer.addVertex(pose,x,y,z).setColor(color).setUv(u,v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose,nx,ny,nz);
    }
}
