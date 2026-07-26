package net.xuwu.openblocks_reborn.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.xuwu.openblocks_reborn.OpenBlocksReborn;
import net.xuwu.openblocks_reborn.item.StencilItem;

/** Renders the prepared stencil's real cut-out pattern in inventories and hands. */
public final class StencilItemRenderer extends BlockEntityWithoutLevelRenderer {
    public StencilItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                             MultiBufferSource buffers, int packedLight, int packedOverlay) {
        int pattern = StencilItem.getPattern(stack);
        if (pattern < 0) return;
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(ResourceLocation.fromNamespaceAndPath(OpenBlocksReborn.MOD_ID, "item/stencil"));
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS));
        PoseStack.Pose pose = poseStack.last();
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                if (StencilItem.isHole(pattern, 0, x, y)) continue;
                float left = x / 16.0F;
                float right = (x + 1) / 16.0F;
                float top = 1.0F - y / 16.0F;
                float bottom = 1.0F - (y + 1) / 16.0F;
                float u0 = Mth.lerp(x / 16.0F, sprite.getU0(), sprite.getU1());
                float u1 = Mth.lerp((x + 1) / 16.0F, sprite.getU0(), sprite.getU1());
                float v0 = Mth.lerp(y / 16.0F, sprite.getV0(), sprite.getV1());
                float v1 = Mth.lerp((y + 1) / 16.0F, sprite.getV0(), sprite.getV1());
                vertex(consumer, pose, left, bottom, 0.505F, u0, v1, packedLight, packedOverlay, 1.0F);
                vertex(consumer, pose, right, bottom, 0.505F, u1, v1, packedLight, packedOverlay, 1.0F);
                vertex(consumer, pose, right, top, 0.505F, u1, v0, packedLight, packedOverlay, 1.0F);
                vertex(consumer, pose, left, top, 0.505F, u0, v0, packedLight, packedOverlay, 1.0F);
                vertex(consumer, pose, right, bottom, 0.495F, u0, v1, packedLight, packedOverlay, -1.0F);
                vertex(consumer, pose, left, bottom, 0.495F, u1, v1, packedLight, packedOverlay, -1.0F);
                vertex(consumer, pose, left, top, 0.495F, u1, v0, packedLight, packedOverlay, -1.0F);
                vertex(consumer, pose, right, top, 0.495F, u0, v0, packedLight, packedOverlay, -1.0F);
            }
        }
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z,
                               float u, float v, int light, int overlay, float normalZ) {
        consumer.addVertex(pose, x, y, z).setColor(0xFFFFFFFF).setUv(u, v)
                .setOverlay(overlay).setLight(light).setNormal(pose, 0.0F, 0.0F, normalZ);
    }
}
