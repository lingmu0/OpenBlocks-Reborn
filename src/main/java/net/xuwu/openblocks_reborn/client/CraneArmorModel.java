package net.xuwu.openblocks_reborn.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * The backpack and its vertical support remain normal armor geometry. The long
 * boom is rendered by CraneThirdPersonRenderer in world coordinates so custom
 * root-part transforms cannot diverge from the cable and magnet.
 */
public class CraneArmorModel<T extends LivingEntity> extends HumanoidModel<T> {
    private final ModelPart craneArm;
    private boolean renderArmorStandArm;

    public CraneArmorModel(ModelPart root) {
        super(root);
        craneArm = root.getChild("crane_arm");
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        renderArmorStandArm = !(entity instanceof Player);
        if (!renderArmorStandArm) return;
        craneArm.yRot = (float)Math.PI + head.yRot;
        if (entity.isCrouching()) {
            craneArm.y = -18.0F;
            craneArm.z = -0.15F;
        } else {
            craneArm.y = -16.0F;
            craneArm.z = 7.0F;
        }
    }

    @Override
    public void renderToBuffer(PoseStack poses, VertexConsumer vertices, int packedLight,
                               int packedOverlay, int color) {
        super.renderToBuffer(poses, vertices, packedLight, packedOverlay, color);
        // Functional player cranes use the shared world-space renderer. Keep the
        // legacy child only for armor stands and other display entities.
        if (renderArmorStandArm) {
            craneArm.render(poses, vertices, packedLight, packedOverlay, color);
        }
    }
}
