package net.xuwu.openblocks_reborn.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xuwu.openblocks_reborn.OpenBlocksReborn;
import net.xuwu.openblocks_reborn.config.VillageHighlighterConfig;
import net.xuwu.openblocks_reborn.network.VillageHighlighterRequestPayload;
import net.xuwu.openblocks_reborn.network.VillageHighlighterSnapshotPayload;
import net.xuwu.openblocks_reborn.registry.ModBlocks;
import net.xuwu.openblocks_reborn.village.VillageBoundaryEdges;
import net.xuwu.openblocks_reborn.village.VillageHighlighterData;
import net.xuwu.openblocks_reborn.village.HighlighterQueryLimiter;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

@EventBusSubscriber(modid = OpenBlocksReborn.MOD_ID, value = Dist.CLIENT)
public final class VillageHighlighterClient {
    static boolean enabled;
    private static BlockPos anchor, selected;
    private static ResourceKey<Level> dimension;
    private static VillageHighlighterSnapshotPayload data;
    private static List<VillageBoundaryEdges.Edge> boundary = List.of();
    private static List<Long> boundarySections = List.of();
    private static List<VillageHighlighterData.Link> orderedLinks = List.of(), orderedOwners = List.of();
    private static int tick, nextQuery, receivedTick, lastSentTick = -10;
    private static int nextSnapshot;
    private static boolean awaitingSnapshot;
    private static final int MAX_DRAWN_GOLEM_BOXES = VillageHighlighterData.MAX_SNAPSHOT_OWNERS;

    public static void open(BlockPos position) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (!position.equals(anchor) || !mc.level.dimension().equals(dimension)) invalidate();
        anchor = position.immutable();
        dimension = mc.level.dimension();
        enabled = true;
        mc.setScreen(new VillageHighlighterScreen());
    }
    public static void invalidate() {
        data = null; boundary = List.of(); boundarySections = List.of();
        orderedLinks = List.of(); orderedOwners = List.of(); nextQuery = 0; awaitingSnapshot = false;
    }
    public static void accept(VillageHighlighterSnapshotPayload value) {
        if (anchor == null || !anchor.equals(value.anchor()) || value.all() != VillageHighlighterConfig.ALL.get()) return;
        if (!value.all() && !value.selected().equals(selected)) return;
        if (value.all() && !awaitingSnapshot) return;
        awaitingSnapshot = false;
        boolean boundaryChanged = !boundarySections.equals(value.snapshot().villageSections());
        data = value;
        receivedTick = tick;
        if (boundaryChanged) {
            boundarySections = value.snapshot().villageSections();
            boundary = VillageBoundaryEdges.of(boundarySections);
        }
        orderLinks();
    }
    private static void orderLinks() {
        if (data == null) return;
        orderedLinks = data.snapshot().links().stream().sorted(Comparator.comparingInt(link ->
                link.poi().equals(selected) ? 0 : 1)).toList();
        var owners = new LinkedHashMap<Integer, VillageHighlighterData.Link>();
        orderedLinks.forEach(link -> owners.putIfAbsent(link.villagerId(), link));
        orderedOwners = List.copyOf(owners.values());
    }
    private static void reset() { anchor = null; selected = null; enabled = false; invalidate(); }

    static int snapshotWaitSeconds() {
        int wait = Math.max(nextSnapshot - tick, HighlighterQueryLimiter.QUERY_TICKS - (tick - lastSentTick));
        return Math.max(0, (wait + 19) / 20);
    }
    static void requestSnapshot() {
        if (anchor == null || !enabled || !VillageHighlighterConfig.ALL.get() || snapshotWaitSeconds() > 0) return;
        awaitingSnapshot = true;
        nextSnapshot = tick + HighlighterQueryLimiter.SNAPSHOT_COOLDOWN_TICKS;
        lastSentTick = tick;
        PacketDistributor.sendToServer(new VillageHighlighterRequestPayload(anchor, anchor, true,
                VillageHighlighterConfig.RADIUS.get(), VillageHighlighterConfig.VILLAGE.get()));
        Minecraft.getInstance().setScreen(null);
    }

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (anchor == null) return;
        if (mc.level == null || mc.player == null || !mc.level.dimension().equals(dimension)
                || mc.player.distanceToSqr(anchor.getCenter()) > 64 * 64
                || !mc.level.hasChunkAt(anchor) || !mc.level.getBlockState(anchor).is(ModBlocks.VILLAGE_HIGHLIGHTER.get())) {
            reset(); return;
        }
        if (mc.isPaused()) return;
        tick++;
        if (data != null && tick - receivedTick >= (data.all()
                ? HighlighterQueryLimiter.SNAPSHOT_VISIBLE_TICKS : 80)) invalidate();
        if (!enabled || mc.options.hideGui) return;
        // Only the explicit button may request All mode, including after expiry or reopening.
        if (VillageHighlighterConfig.ALL.get() || mc.screen != null) return;
        Vec3 eye = mc.player.getEyePosition();
        Vec3 end = eye.add(mc.player.getViewVector(1).scale(32));
        var hit = mc.level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));
        BlockPos target = hit.getType() == HitResult.Type.BLOCK ? hit.getBlockPos() : anchor;
        if (!target.equals(selected)) {
            selected = target.immutable();
            // The selected POI changes; the cached village boundary does not.
            data = null; orderedLinks = List.of(); orderedOwners = List.of(); nextQuery = 0;
        }
        if (tick >= nextQuery && tick - lastSentTick >= 10) {
            PacketDistributor.sendToServer(new VillageHighlighterRequestPayload(anchor, selected,
                    VillageHighlighterConfig.ALL.get(), VillageHighlighterConfig.RADIUS.get(), VillageHighlighterConfig.VILLAGE.get()));
            nextQuery = tick + 20;
            lastSentTick = tick;
        }
    }

    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || !enabled || anchor == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.options.hideGui) return;
        PoseStack poses = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        var buffers = mc.renderBuffers().bufferSource();
        RenderType type = VillageHighlighterConfig.THROUGH_WALLS.get()
                ? VillageHighlighterRenderTypes.THROUGH_WALLS : RenderType.lines();
        VertexConsumer vertices = buffers.getBuffer(type);
        if (VillageHighlighterConfig.POIS.get() && data != null) {
            for (var poi : data.snapshot().pois()) {
                int rgb = color(poi.kind());
                box(poses, vertices, new AABB(poi.pos()).deflate(.03), camera, rgb, poi.occupied() ? .85F : .4F);
            }
        }
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        if (VillageHighlighterConfig.LINKS.get()) for (var link : orderedLinks) {
            var entity = data != null && data.all() ? null : mc.level.getEntity(link.villagerId());
            Vec3 from = entity != null ? entity.getPosition(partial).add(0, entity.getBbHeight() * .7, 0)
                    : link.villagerPos().getCenter().add(0, 1, 0);
            if (from.distanceToSqr(camera) > 80 * 80) continue;
            line(poses, vertices, from.subtract(camera),
                    link.poi().getCenter().subtract(camera), color(link.kind()), .65F);
        }
        if (VillageHighlighterConfig.GOLEMS.get()) {
            int drawn = 0;
            for (var owner : orderedOwners) {
                if (drawn++ == MAX_DRAWN_GOLEM_BOXES) break;
                var entity = data != null && data.all() ? null : mc.level.getEntity(owner.villagerId());
                BlockPos origin = entity != null ? entity.blockPosition() : owner.villagerPos();
                box(poses, vertices, VillageHighlighterData.golemSearchBox(origin), camera, 0xFFD05A, .55F);
            }
        }
        if (VillageHighlighterConfig.VILLAGE.get()) {
            for (var edge : boundary) line(poses, vertices, Vec3.atLowerCornerOf(edge.from()).subtract(camera),
                    Vec3.atLowerCornerOf(edge.to()).subtract(camera), 0x75A6FF, .45F);
        }
        buffers.endBatch(type);
    }

    @SubscribeEvent public static void hud(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!enabled || anchor == null || mc.player == null || mc.options.hideGui || mc.screen != null) return;
        var graphics = event.getGuiGraphics();
        graphics.drawString(mc.font, Component.translatable("highlighter.openblocks_reborn.hud",
                Component.translatable("highlighter.openblocks_reborn." + (VillageHighlighterConfig.ALL.get() ? "all" : "single")),
                VillageHighlighterConfig.RADIUS.get()), 8, 8, 0xFFFFFF);
        if (data == null || data.snapshot().pois().isEmpty()) {
            graphics.drawString(mc.font, Component.translatable("highlighter.openblocks_reborn."
                    + (VillageHighlighterConfig.ALL.get() ? (data == null ? "request_hint" : "empty") : "aim")), 8, 20, 0xDDDDDD);
            return;
        }
        long owners = orderedOwners.size();
        graphics.drawString(mc.font, owners == 0 ? Component.translatable("highlighter.openblocks_reborn.no_owner")
                : Component.translatable("highlighter.openblocks_reborn.owners", owners), 8, 20, 0xDDDDDD);
        if (VillageHighlighterConfig.GOLEMS.get() && owners > 0)
            graphics.drawString(mc.font, Component.translatable("highlighter.openblocks_reborn.search_only"), 8, 32, 0xFFD05A);
        if (data.snapshot().truncated() || (VillageHighlighterConfig.GOLEMS.get() && owners > MAX_DRAWN_GOLEM_BOXES))
            graphics.drawString(mc.font, Component.translatable("highlighter.openblocks_reborn.limited"), 8, 44, 0xFFBB66);
        if (data.all()) graphics.drawString(mc.font, Component.translatable("highlighter.openblocks_reborn.snapshot_age",
                (tick - receivedTick) / 20, Math.max(1, (HighlighterQueryLimiter.SNAPSHOT_VISIBLE_TICKS - tick + receivedTick + 19) / 20)),
                8, 56, 0xFFFF99);
    }

    private static int color(int kind) {
        return switch (kind) { case VillageHighlighterData.HOME -> 0x65B7FF;
            case VillageHighlighterData.JOB -> 0xFFA657; default -> 0xFFE080; };
    }
    private static void box(PoseStack poses, VertexConsumer vertices, AABB box, Vec3 camera, int rgb, float alpha) {
        LevelRenderer.renderLineBox(poses, vertices, box.move(-camera.x, -camera.y, -camera.z),
                ((rgb >> 16) & 255) / 255F, ((rgb >> 8) & 255) / 255F, (rgb & 255) / 255F, alpha);
    }
    private static void line(PoseStack poses, VertexConsumer vertices, Vec3 from, Vec3 to, int rgb, float alpha) {
        Vec3 delta = to.subtract(from);
        if (delta.lengthSqr() < 1e-8) return;
        Vec3 normal = delta.normalize();
        int color = ((int)(alpha * 255) << 24) | rgb;
        vertices.addVertex(poses.last(), (float)from.x, (float)from.y, (float)from.z).setColor(color)
                .setNormal(poses.last(), (float)normal.x, (float)normal.y, (float)normal.z);
        vertices.addVertex(poses.last(), (float)to.x, (float)to.y, (float)to.z).setColor(color)
                .setNormal(poses.last(), (float)normal.x, (float)normal.y, (float)normal.z);
    }
    private VillageHighlighterClient() {}
}
