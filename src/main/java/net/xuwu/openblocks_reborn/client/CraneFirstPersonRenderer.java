package net.xuwu.openblocks_reborn.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.xuwu.openblocks_reborn.OpenBlocksReborn;
import net.xuwu.openblocks_reborn.item.CraneBackpackItem;

/**
 * Restores the legacy first-person boom that was rendered independently from
 * the chest armor. The world-space anchor deliberately follows yaw but not
 * pitch, matching the horizontal overhead crane shown by the original mod.
 */
public final class CraneFirstPersonRenderer {
    private static final ResourceLocation CRANE_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            OpenBlocksReborn.MOD_ID, "textures/models/crane.png");
    private static ModelPart boom;

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.getCameraType() != CameraType.FIRST_PERSON
                || !(event.getCamera().getEntity() instanceof Player player)) {
            return;
        }
        var backpack = CraneBackpackItem.wornBy(player);
        if (backpack.isEmpty()) return;

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 playerPosition = new Vec3(
                Mth.lerp(partialTick, player.xo, player.getX()),
                Mth.lerp(partialTick, player.yo, player.getY()),
                Mth.lerp(partialTick, player.zo, player.getZ()));
        float yaw = Mth.rotLerp(partialTick, player.yRotO, player.getYRot());
        double yawRadians = Math.toRadians(yaw);
        Vec3 forward = new Vec3(-Math.sin(yawRadians), 0.0D, Math.cos(yawRadians));
        Vec3 boomBase = playerPosition.add(0.0D, player.getEyeHeight() + 0.62D, 0.0D)
                .subtract(forward.scale(0.5D));
        Vec3 cableAnchor = boomBase.add(forward.scale(43.0D / 16.0D));
        Vec3 magnet = renderedMagnetPosition(player, backpack, partialTick);
        Vec3 camera = event.getCamera().getPosition();

        var buffers = minecraft.renderBuffers().bufferSource();
        RenderType boomType = RenderType.entityCutoutNoCull(CRANE_TEXTURE);
        PoseStack poses = event.getPoseStack();
        poses.pushPose();
        poses.translate(boomBase.x - camera.x, boomBase.y - camera.y, boomBase.z - camera.z);
        poses.mulPose(Axis.YP.rotationDegrees(-yaw));
        // Humanoid models use pixel-down Y. Flip it into world-up coordinates.
        poses.scale(1.0F, -1.0F, 1.0F);
        ModelPart part = boom();
        PartPose savedPose = part.storePose();
        part.setPos(0.0F, 0.0F, 0.0F);
        part.setRotation(0.0F, 0.0F, 0.0F);
        part.render(poses, buffers.getBuffer(boomType),
                // The legacy first-person renderer disabled world lighting so the
                // warning stripes stayed legible against both sky and caves.
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY);
        part.loadPose(savedPose);
        poses.popPose();
        buffers.endBatch(boomType);

        VertexConsumer cable = buffers.getBuffer(RenderType.lines());
        renderStripedCable(poses, cable, cableAnchor.subtract(camera), magnet.subtract(camera));
        buffers.endBatch(RenderType.lines());

        CraneMagnetRenderer.render(poses, buffers,
                (float)(magnet.x - camera.x), (float)(magnet.y - camera.y),
                (float)(magnet.z - camera.z),
                LevelRenderer.getLightColor(player.level(), player.blockPosition()));
        buffers.endBatch(CraneMagnetRenderer.renderType());
    }

    public static void renderStripedCable(PoseStack poses, VertexConsumer cable,
                                          Vec3 start, Vec3 end) {
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length < 1.0E-5D) return;
        Vec3 normal = delta.scale(1.0D / length);
        int segments = Math.max(8, (int)Math.ceil(length * 8.0D));
        for (int index = 0; index < segments; index++) {
            Vec3 from = start.add(delta.scale(index / (double)segments));
            Vec3 to = start.add(delta.scale((index + 1) / (double)segments));
            int color = (index & 1) == 0 ? 0xFFFFD43B : 0xFF111111;
            cable.addVertex(poses.last(), (float)from.x, (float)from.y, (float)from.z)
                    .setColor(color)
                    .setNormal(poses.last(), (float)normal.x, (float)normal.y, (float)normal.z);
            cable.addVertex(poses.last(), (float)to.x, (float)to.y, (float)to.z)
                    .setColor(color)
                    .setNormal(poses.last(), (float)normal.x, (float)normal.y, (float)normal.z);
        }
    }

    static Vec3 renderedMagnetPosition(Player player,
                                       net.minecraft.world.item.ItemStack backpack,
                                       float partialTick) {
        Vec3 expected = CraneBackpackItem.magnetPosition(player, backpack, partialTick);
        var carriedId = CraneBackpackItem.carriedId(backpack);
        if (carriedId == null) return expected;
        var carried = player.level().getEntities(player, new AABB(expected, expected).inflate(24.0D),
                        entity -> entity.getUUID().equals(carriedId))
                .stream().findFirst().orElse(null);
        if (carried == null) return expected;
        return new Vec3(
                Mth.lerp(partialTick, carried.xo, carried.getX()),
                Mth.lerp(partialTick, carried.yo, carried.getY()) + carried.getBbHeight(),
                Mth.lerp(partialTick, carried.zo, carried.getZ()));
    }

    private static ModelPart boom() {
        if (boom == null) {
            boom = Minecraft.getInstance().getEntityModels()
                    .bakeLayer(WearableArmorModels.CRANE).getChild("crane_arm");
        }
        return boom;
    }

    private CraneFirstPersonRenderer() {
    }
}
