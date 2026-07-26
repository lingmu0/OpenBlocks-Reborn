package net.xuwu.openblocks_reborn.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.PartPose;

/**
 * Modern ModelPart recreation of the original OpenBlocks cannon model.
 */
public final class CannonModel {
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart shooter;

    public CannonModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.shooter = root.getChild("shooter");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).mirror()
                        .addBox(-3.0F, -5.0F, -3.0F, 6.0F, 6.0F, 6.0F),
                PartPose.offsetAndRotation(0.0F, 11.0F, 3.0F, (float)Math.toRadians(45.0D), 0.0F, 0.0F));
        root.addOrReplaceChild("shooter",
                CubeListBuilder.create().texOffs(34, 0).mirror()
                        .addBox(-2.0F, -4.0F, 2.0F, 4.0F, 4.0F, 6.0F),
                PartPose.offsetAndRotation(0.0F, 11.0F, 3.0F, (float)Math.toRadians(45.0D), 0.0F, 0.0F));
        root.addOrReplaceChild("base",
                CubeListBuilder.create().texOffs(14, 19).mirror()
                        .addBox(-6.0F, 0.0F, -6.0F, 12.0F, 1.0F, 12.0F),
                PartPose.offset(0.0F, 15.0F, 0.0F));

        addWheel(root, "wheel_left_0", 0.0F, 0.0F, 0.0F);
        addWheel(root, "wheel_left_30", -0.01F, (float)Math.toRadians(30.0D), 0.0F);
        addWheel(root, "wheel_left_60", -0.02F, (float)Math.toRadians(60.0D), 0.0F);
        addWheel(root, "wheel_right_0", 0.0F, 0.0F, (float)Math.PI);
        addWheel(root, "wheel_right_30", -0.01F, (float)Math.toRadians(30.0D), (float)Math.PI);
        addWheel(root, "wheel_right_60", -0.02F, (float)Math.toRadians(60.0D), (float)Math.PI);
        return LayerDefinition.create(mesh, 64, 32);
    }

    private static void addWheel(PartDefinition root, String name, float xOffset, float xRotation, float zRotation) {
        root.addOrReplaceChild(name,
                CubeListBuilder.create().texOffs(0, 20).mirror()
                        .addBox(3.0F, -3.0F, -3.0F, 1.0F, 6.0F, 6.0F),
                PartPose.offsetAndRotation(xOffset, 11.0F, 3.0F, xRotation, 0.0F, zRotation));
    }

    public void render(float pitchRadians, PoseStack poseStack, VertexConsumer consumer,
                       int packedLight, int packedOverlay) {
        body.xRot = pitchRadians;
        shooter.xRot = pitchRadians;
        root.render(poseStack, consumer, packedLight, packedOverlay);
    }
}
