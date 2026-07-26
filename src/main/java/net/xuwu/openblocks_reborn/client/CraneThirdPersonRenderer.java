package net.xuwu.openblocks_reborn.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.xuwu.openblocks_reborn.OpenBlocksReborn;
import net.xuwu.openblocks_reborn.item.CraneBackpackItem;

import java.util.List;

/**
 * Draws third-person cable parts in world coordinates. RenderPlayerEvent leaves
 * a player-model transform on the pose stack on some render paths, which used
 * to rotate the world-space cable and magnet a second time away from the boom.
 */
public final class CraneThirdPersonRenderer {
    private static final ResourceLocation CRANE_TEXTURE = new ResourceLocation(
            OpenBlocksReborn.MOD_ID, "textures/models/crane.png");
    private static ModelPart boom;

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        Object cameraEntity = event.getCamera().getEntity();
        boolean localFirstPerson = minecraft.options.getCameraType() == CameraType.FIRST_PERSON;
        List<? extends Player> players = minecraft.level.players().stream()
                .filter(player -> !CraneBackpackItem.wornBy(player).isEmpty())
                .filter(player -> !(localFirstPerson && player == cameraEntity))
                .toList();
        if (players.isEmpty()) return;

        float partialTick = event.getPartialTick();
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poses = event.getPoseStack();
        var buffers = minecraft.renderBuffers().bufferSource();

        RenderType boomType = RenderType.entityCutoutNoCull(CRANE_TEXTURE);
        for (Player player : players) {
            renderBoom(player, partialTick, camera, poses,
                    buffers.getBuffer(boomType));
        }
        buffers.endBatch(boomType);

        // Submit every line before switching the shared BufferSource builder to
        // the textured magnet render type.
        var cable = buffers.getBuffer(RenderType.lines());
        for (Player player : players) {
            var backpack = CraneBackpackItem.wornBy(player);
            Vec3 anchor = CraneBackpackItem.cableAnchorPosition(player, partialTick);
            Vec3 magnet = CraneFirstPersonRenderer.renderedMagnetPosition(
                    player, backpack, partialTick);
            CraneFirstPersonRenderer.renderStripedCable(
                    poses, cable, anchor.subtract(camera), magnet.subtract(camera));
        }
        buffers.endBatch(RenderType.lines());

        for (Player player : players) {
            var backpack = CraneBackpackItem.wornBy(player);
            Vec3 magnet = CraneFirstPersonRenderer.renderedMagnetPosition(
                    player, backpack, partialTick);
            CraneMagnetRenderer.render(poses, buffers,
                    (float)(magnet.x - camera.x),
                    (float)(magnet.y - camera.y),
                    (float)(magnet.z - camera.z),
                    LevelRenderer.getLightColor(player.level(), player.blockPosition()));
        }
        buffers.endBatch(CraneMagnetRenderer.renderType());
    }

    private static void renderBoom(Player player, float partialTick, Vec3 camera,
                                   PoseStack poses,
                                   com.mojang.blaze3d.vertex.VertexConsumer vertices) {
        Vec3 playerPosition = new Vec3(
                Mth.lerp(partialTick, player.xo, player.getX()),
                Mth.lerp(partialTick, player.yo, player.getY()),
                Mth.lerp(partialTick, player.zo, player.getZ()));
        float bodyYaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
        float headYaw = Mth.rotLerp(partialTick, player.yHeadRotO, player.getYHeadRot());
        double bodyRadians = Math.toRadians(bodyYaw);
        Vec3 bodyForward = new Vec3(
                -Math.sin(bodyRadians), 0.0D, Math.cos(bodyRadians));
        boolean crouching = player.isCrouching();
        Vec3 boomBase = playerPosition.add(
                crouching ? Vec3.ZERO : bodyForward.scale(-0.45D))
                .add(0.0D, player.getEyeHeight() + (crouching ? 0.77D : 0.72D), 0.0D);

        poses.pushPose();
        poses.translate(boomBase.x - camera.x, boomBase.y - camera.y,
                boomBase.z - camera.z);
        poses.mulPose(Axis.YP.rotationDegrees(-headYaw));
        poses.scale(1.0F, -1.0F, 1.0F);
        ModelPart part = boom();
        PartPose savedPose = part.storePose();
        part.setPos(0.0F, 0.0F, 0.0F);
        part.setRotation(0.0F, 0.0F, 0.0F);
        part.render(poses, vertices,
                LevelRenderer.getLightColor(player.level(), player.blockPosition()),
                OverlayTexture.NO_OVERLAY);
        part.loadPose(savedPose);
        poses.popPose();
    }

    private static ModelPart boom() {
        if (boom == null) {
            boom = Minecraft.getInstance().getEntityModels()
                    .bakeLayer(WearableArmorModels.CRANE).getChild("crane_arm");
        }
        return boom;
    }

    private CraneThirdPersonRenderer() {
    }
}
