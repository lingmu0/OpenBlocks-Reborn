package net.xuwu.openblocks_reborn.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.xuwu.openblocks_reborn.OpenBlocksReborn;
import net.xuwu.openblocks_reborn.block.ItemMachineBlock;
import net.xuwu.openblocks_reborn.blockentity.ItemMachineBlockEntity;

public class CannonRenderer implements BlockEntityRenderer<ItemMachineBlockEntity> {
    public static final ModelLayerLocation MODEL_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(OpenBlocksReborn.MOD_ID, "cannon"), "main");
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            OpenBlocksReborn.MOD_ID, "textures/models/cannon.png");

    private final CannonModel model;

    public CannonRenderer(BlockEntityRendererProvider.Context context) {
        model = new CannonModel(context.bakeLayer(MODEL_LAYER));
    }

    @Override
    public void render(ItemMachineBlockEntity cannon, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        if (!(cannon.getBlockState().getBlock() instanceof ItemMachineBlock block)
                || block.mode() != ItemMachineBlock.Mode.CANNON) return;

        Direction facing = cannon.getBlockState().getValue(ItemMachineBlock.FACING);
        float pitch = getPitch(cannon, facing);
        renderModel(model, facing, pitch, poseStack, buffers, packedLight, packedOverlay);
    }

    static float getPitch(ItemMachineBlockEntity cannon, Direction facing) {
        if (cannon.getCannonTarget() == null) return (float)Math.toRadians(45.0D);
        Vec3 origin = cannon.getBlockPos().relative(facing).getCenter();
        Vec3 velocity = ItemMachineBlock.calculateCannonVelocity(origin, cannon.getCannonTarget().getCenter());
        return (float)Math.asin(velocity.normalize().y);
    }

    static void renderModel(CannonModel model, Direction facing, float pitch, PoseStack poseStack,
                            MultiBufferSource buffers, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 1.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutout(TEXTURE));
        model.render(pitch, poseStack, consumer, packedLight,
                packedOverlay == 0 ? OverlayTexture.NO_OVERLAY : packedOverlay);
        poseStack.popPose();
    }
}
