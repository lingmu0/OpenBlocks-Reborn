package net.xuwu.openblocks_reborn.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundSourceEvent;
import net.neoforged.neoforge.client.event.sound.PlayStreamingSourceEvent;
import net.xuwu.openblocks_reborn.OpenBlocksReborn;
import net.xuwu.openblocks_reborn.config.SonicGlassesConfig;
import net.xuwu.openblocks_reborn.item.GlassesItem;

import java.util.ArrayList;
import java.util.List;

/** Client-only sound visualization. Never changes an entity's shared glowing state. */
@EventBusSubscriber(modid = OpenBlocksReborn.MOD_ID, value = Dist.CLIENT)
public final class SonicGlassesClient {
    private static final List<Echo> ECHOES = new ArrayList<>();
    private static ClientLevel trackedLevel;
    private static long clock;

    private static boolean wearing(Minecraft mc) {
        return mc.player != null && mc.level != null
                && GlassesItem.isWearing(mc.player, GlassesItem.Mode.SONIC);
    }

    @SubscribeEvent
    public static void sound(PlaySoundSourceEvent event) {
        heard(event.getSound());
    }

    @SubscribeEvent
    public static void stream(PlayStreamingSourceEvent event) {
        heard(event.getSound());
    }

    private static void heard(SoundInstance sound) {
        // Source events run after resolution; PlaySoundEvent is too early to
        // read AbstractSoundInstance.getVolume and also includes cancelled sounds.
        if (sound == null || sound.isRelative()
                || sound.getAttenuation() == SoundInstance.Attenuation.NONE
                || sound.getSource() == SoundSource.MUSIC || sound.getSource() == SoundSource.RECORDS
                || sound.getVolume() <= 0) return;
        Minecraft mc = Minecraft.getInstance();
        // Sound callbacks may run outside the render thread. Own all mutable state there.
        mc.execute(() -> capture(mc, sound));
    }

    private static void capture(Minecraft mc, SoundInstance sound) {
        if (!wearing(mc) || !SonicGlassesConfig.ENABLED.get()) return;
        synchronizeLevel(mc);
        if (mc.options.getSoundSourceVolume(SoundSource.MASTER) <= 0
                || mc.options.getSoundSourceVolume(sound.getSource()) <= 0) return;
        Vec3 position = new Vec3(sound.getX(), sound.getY(), sound.getZ());
        double range = Math.min(SonicGlassesConfig.RANGE.get(), 16 * Math.max(1, sound.getVolume()));
        if (position.distanceToSqr(mc.gameRenderer.getMainCamera().getPosition()) > range * range) return;
        var resolved = mc.getSoundManager().getSoundEvent(sound.getLocation());
        if (resolved == null) return;
        Component subtitle = resolved.getSubtitle();
        if (subtitle == null) subtitle = Component.literal(sound.getLocation().toString());
        var category = SonicSoundKind.classify(sound.getLocation(), sound.getSource());
        // Coalesce rapid repeated sounds at one source; rain and machines must not flood the view.
        for (Echo echo : ECHOES) {
            if (echo.id.equals(sound.getLocation()) && echo.position.distanceToSqr(position) < 1
                    && clock - echo.born < 5) {
                echo.born = clock;
                echo.position = position;
                return;
            }
        }
        int limit = SonicGlassesConfig.MAX_MARKERS.get();
        while (ECHOES.size() >= limit) ECHOES.removeFirst();
        ECHOES.add(new Echo(sound, position, subtitle, category, clock));
    }

    private static void synchronizeLevel(Minecraft mc) {
        if (trackedLevel != mc.level) {
            ECHOES.clear();
            trackedLevel = mc.level;
            clock = 0;
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        synchronizeLevel(mc);
        if (!wearing(mc) || !SonicGlassesConfig.ENABLED.get()) {
            ECHOES.clear();
            return;
        }
        if (mc.isPaused()) return;
        clock++;
        int lifetime = SonicGlassesConfig.LIFETIME.get();
        ECHOES.removeIf(echo -> {
            // Moving/looping sound instances keep their actual source position.
            if (mc.getSoundManager().isActive(echo.sound)) {
                echo.position = new Vec3(echo.sound.getX(), echo.sound.getY(), echo.sound.getZ());
                if (echo.sound.isLooping()) echo.born = clock;
            }
            return clock - echo.born >= lifetime;
        });
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        Minecraft mc = Minecraft.getInstance();
        if (!wearing(mc) || mc.options.hideGui || !SonicGlassesConfig.ENABLED.get()) return;
        synchronizeLevel(mc);
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poses = event.getPoseStack();
        var buffers = mc.renderBuffers().bufferSource();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        double range = SonicGlassesConfig.RANGE.get();
        for (Echo echo : ECHOES) {
            double distance = echo.position.distanceTo(camera);
            if (distance > range) continue;
            float age = clock - echo.born + partial;
            float alpha = Mth.clamp(1 - age / SonicGlassesConfig.LIFETIME.get(), 0, 1);
            if (alpha < 0.05F) continue;
            poses.pushPose();
            poses.translate(echo.position.x - camera.x, echo.position.y - camera.y + 0.35,
                    echo.position.z - camera.z);
            poses.mulPose(event.getCamera().rotation());
            float scale = (float)(0.022 * Mth.clamp(distance / 12, 1, 3));
            poses.scale(scale, -scale, scale);
            var matrix = poses.last().pose();
            RenderType type = RenderType.textSeeThrough(echo.kind.texture());
            var vertices = buffers.getBuffer(type);
            int color = ((int)(alpha * 255) << 24) | echo.kind.color;
            // Legacy sound icons remain visible through walls, like sound itself.
            vertices.addVertex(matrix, -8, -18, 0).setColor(color).setUv(0, 0).setLight(0xF000F0);
            vertices.addVertex(matrix, -8, -2, 0).setColor(color).setUv(0, 1).setLight(0xF000F0);
            vertices.addVertex(matrix, 8, -2, 0).setColor(color).setUv(1, 1).setLight(0xF000F0);
            vertices.addVertex(matrix, 8, -18, 0).setColor(color).setUv(1, 0).setLight(0xF000F0);
            buffers.endBatch(type);
            if (SonicGlassesConfig.LABELS.get()) {
                Component label = echo.label.copy().append(" " + Math.round(distance) + "m");
                int labelColor = ((int)(alpha * 255) << 24) | 0xFFFFFF;
                mc.font.drawInBatch(label, -mc.font.width(label) / 2F, 1, labelColor, false,
                        matrix, buffers, Font.DisplayMode.SEE_THROUGH, 0xA0000000, 0xF000F0);
            }
            poses.popPose();
        }
        buffers.endBatch();
    }

    private static final class Echo {
        final SoundInstance sound;
        final ResourceLocation id;
        final Component label;
        final SonicSoundKind kind;
        Vec3 position;
        long born;

        Echo(SoundInstance sound, Vec3 position, Component label, SonicSoundKind kind, long born) {
            this.sound = sound;
            this.id = sound.getLocation();
            this.position = position;
            this.label = label;
            this.kind = kind;
            this.born = born;
        }
    }

    private SonicGlassesClient() {}
}
