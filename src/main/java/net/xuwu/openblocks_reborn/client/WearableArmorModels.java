package net.xuwu.openblocks_reborn.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.xuwu.openblocks_reborn.OpenBlocksReborn;

public final class WearableArmorModels {
    public static final ModelLayerLocation GLASSES = new ModelLayerLocation(
            new ResourceLocation(OpenBlocksReborn.MOD_ID, "glasses_armor"), "main");
    public static final ModelLayerLocation CRANE = new ModelLayerLocation(
            new ResourceLocation(OpenBlocksReborn.MOD_ID, "crane_armor"), "main");
    public static final ModelLayerLocation CRANE_MAGNET = new ModelLayerLocation(
            new ResourceLocation(OpenBlocksReborn.MOD_ID, "crane_magnet"), "main");

    public static LayerDefinition glassesLayer() {
        MeshDefinition mesh = emptyHumanoid();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 16).addBox(-5.0F, -4.5F, -4.75F, 10.0F, 1.0F, 1.0F)
                        .texOffs(0, 28).addBox(-3.0F, -5.5F, -4.9F, 2.0F, 3.0F, 1.0F)
                        .texOffs(0, 28).addBox(1.0F, -5.5F, -4.9F, 2.0F, 3.0F, 1.0F)
                        .texOffs(0, 18).addBox(-5.0F, -4.5F, -4.0F, 1.0F, 1.0F, 3.0F)
                        .texOffs(0, 18).addBox(4.0F, -4.5F, -4.0F, 1.0F, 1.0F, 3.0F),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    public static LayerDefinition craneLayer() {
        MeshDefinition mesh = emptyHumanoid();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 8.0F)
                        .texOffs(32, 0).addBox(-1.0F, -16.0F, 6.0F, 2.0F, 24.0F, 2.0F),
                PartPose.ZERO);
        root.addOrReplaceChild("crane_arm", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-1.0F, 0.0F, 1.0F, 2.0F, 2.0F, 42.0F),
                PartPose.offset(0.0F, -16.0F, 7.0F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    public static LayerDefinition craneMagnetLayer() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("magnet", CubeListBuilder.create().mirror()
                        .texOffs(0, 0).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 1.0F, 6.0F)
                        .texOffs(0, 7).addBox(-2.0F, 1.0F, -2.0F, 4.0F, 1.0F, 4.0F)
                        .texOffs(0, 12).addBox(-1.0F, 2.0F, -1.0F, 2.0F, 1.0F, 2.0F),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 32);
    }

    private static MeshDefinition emptyHumanoid() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        for (String part : new String[] {"head", "hat", "body", "right_arm", "left_arm", "right_leg", "left_leg"}) {
            root.addOrReplaceChild(part, CubeListBuilder.create(), PartPose.ZERO);
        }
        return mesh;
    }

    public static HumanoidModel<?> bake(ModelLayerLocation layer) {
        return new HumanoidModel<>(net.minecraft.client.Minecraft.getInstance()
                .getEntityModels().bakeLayer(layer));
    }

    public static HumanoidModel<?> bakeCrane() {
        return new CraneArmorModel<>(net.minecraft.client.Minecraft.getInstance()
                .getEntityModels().bakeLayer(CRANE));
    }

    private WearableArmorModels() {
    }
}
