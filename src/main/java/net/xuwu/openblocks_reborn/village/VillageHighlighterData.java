package net.xuwu.openblocks_reborn.village;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.WeakHashMap;

/** A bounded view of loaded POIs and actual Brain memories, never nearest-villager guesses. */
public final class VillageHighlighterData {
    public static final int HOME = 0, JOB = 1, MEETING = 2;
    public static final int MAX_POIS = 128, MAX_LINKS = 256, MAX_SECTIONS = 125, MAX_VILLAGERS = 128;
    public static final int MAX_SNAPSHOT_OWNERS = 16;
    public record Poi(BlockPos pos, int kind, boolean occupied) {}
    public record Link(int villagerId, BlockPos villagerPos, BlockPos poi, int kind) {}
    public record Snapshot(List<Poi> pois, List<Link> links, List<Long> villageSections, boolean truncated) {}
    private record Key(BlockPos anchor, int radius) {}
    private static final Map<ServerLevel, LinkedHashMap<Key, Cache>> CACHES = new WeakHashMap<>();
    private static final class Cache {
        long poiTick = Long.MIN_VALUE, populationTick = Long.MIN_VALUE, sectionTick = Long.MIN_VALUE;
        List<Poi> pois = List.of();
        List<Link> memories = List.of();
        List<Long> sections = List.of();
        boolean poiLimit, populationLimit;
    }
    private static Cache cache(ServerLevel level, BlockPos anchor, int radius) {
        var entries = CACHES.computeIfAbsent(level, key -> new LinkedHashMap<>(16, .75F, true));
        var key = new Key(anchor.immutable(), radius);
        Cache cache = entries.computeIfAbsent(key, ignored -> new Cache());
        while (entries.size() > 32) entries.remove(entries.keySet().iterator().next());
        return cache;
    }
    private static boolean stale(long now, long previous, int interval) {
        return previous == Long.MIN_VALUE || now < previous || now - previous >= interval;
    }
    /** Used by headless correctness/performance fixtures after deliberately changing test data. */
    public static void clearCache(ServerLevel level) { CACHES.remove(level); }

    public static int kind(Holder<PoiType> type) {
        if (type.is(PoiTypes.HOME)) return HOME;
        if (type.is(PoiTypeTags.ACQUIRABLE_JOB_SITE)) return JOB;
        if (type.is(PoiTypes.MEETING)) return MEETING;
        return -1;
    }

    public static BlockPos canonicalPoi(ServerLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);
        if (state.getBlock() instanceof BedBlock && state.getValue(BedBlock.PART) == BedPart.FOOT)
            return pos.relative(state.getValue(BedBlock.FACING));
        return pos;
    }

    public static AABB golemSearchBox(BlockPos villagerPos) {
        // Encloses the 17 x 13 x 17 block search volume, not a guarantee of valid spawning.
        return new AABB(villagerPos.getX() - 8, villagerPos.getY() - 6, villagerPos.getZ() - 8,
                villagerPos.getX() + 9, villagerPos.getY() + 7, villagerPos.getZ() + 9);
    }

    public static Snapshot collect(ServerLevel level, BlockPos anchor, BlockPos selected,
                                   boolean all, int requestedRadius, boolean villageLayer) {
        int radius = Math.max(8, Math.min(32, requestedRadius));
        Cache cache = cache(level, anchor, radius);
        long now = level.getGameTime();
        List<Poi> pois = new ArrayList<>();
        boolean truncated = false;
        if (all) {
            if (stale(now, cache.poiTick, 40)) {
                List<Poi> found = new ArrayList<>();
                scan: for (int x = (anchor.getX() - radius) >> 4; x <= (anchor.getX() + radius) >> 4; x++)
                    for (int z = (anchor.getZ() - radius) >> 4; z <= (anchor.getZ() + radius) >> 4; z++) {
                        if (!level.hasChunk(x, z)) continue;
                        var iterator = level.getPoiManager().getInChunk(type -> kind(type) >= 0,
                                new ChunkPos(x, z), PoiManager.Occupancy.ANY).iterator();
                        while (iterator.hasNext()) {
                            var record = iterator.next();
                            if (record.getPos().distSqr(anchor) > radius * radius) continue;
                            found.add(new Poi(record.getPos().immutable(), kind(record.getPoiType()), record.isOccupied()));
                            if (found.size() > MAX_POIS) break scan;
                        }
                    }
                cache.poiLimit = found.size() > MAX_POIS;
                cache.pois = List.copyOf(found.subList(0, Math.min(found.size(), MAX_POIS)));
                cache.poiTick = now;
            }
            pois.addAll(cache.pois);
            truncated = cache.poiLimit;
        } else if (level.hasChunkAt(selected)) {
            BlockPos poi = canonicalPoi(level, selected);
            if (level.hasChunkAt(poi) && poi.distSqr(anchor) <= radius * radius) {
                level.getPoiManager().getType(poi).filter(type -> kind(type) >= 0).ifPresent(type ->
                        pois.add(new Poi(poi.immutable(), kind(type),
                                level.getPoiManager().getFreeTickets(poi) < type.value().maxTickets())));
            }
        }
        Set<BlockPos> selectedPois = new HashSet<>();
        pois.forEach(poi -> selectedPois.add(poi.pos()));
        List<Link> links = new ArrayList<>();
        if (!selectedPois.isEmpty()) {
            if (stale(now, cache.populationTick, 20)) {
                var villagers = new ArrayList<Villager>();
                level.getEntities().get(net.minecraft.world.level.entity.EntityTypeTest.forClass(Villager.class),
                        new AABB(anchor).inflate(64), villager -> {
                            if (villager.isAlive()) villagers.add(villager);
                            return villagers.size() > MAX_VILLAGERS
                                    ? net.minecraft.util.AbortableIterationConsumer.Continuation.ABORT
                                    : net.minecraft.util.AbortableIterationConsumer.Continuation.CONTINUE;
                        });
                cache.populationLimit = villagers.size() > MAX_VILLAGERS;
                var memories = new ArrayList<Link>();
                villagers.stream().limit(MAX_VILLAGERS).forEach(villager -> memories.addAll(memories(villager)));
                cache.memories = List.copyOf(memories);
                cache.populationTick = now;
            }
            truncated |= cache.populationLimit;
            // Stable, actual owners only. A shared bell still selects just one in Single mode.
            var owners = new java.util.TreeSet<Integer>();
            cache.memories.stream().filter(link -> selectedPois.contains(link.poi()))
                    .forEach(link -> owners.add(link.villagerId()));
            int limit = all ? MAX_SNAPSHOT_OWNERS : 1;
            if (all && owners.size() > limit) truncated = true;
            Set<Integer> chosen = new HashSet<>(owners.stream().limit(limit).toList());
            for (Link link : cache.memories) {
                if (chosen.contains(link.villagerId())) links.add(link);
            }
        }
        List<Long> sections = new ArrayList<>();
        if (villageLayer) {
            if (stale(now, cache.sectionTick, 100)) {
            SectionPos center = SectionPos.of(anchor);
            int sectionRadius = Math.min(2, (radius + 15) / 16);
            for (int x = -sectionRadius; x <= sectionRadius; x++)
                for (int y = -sectionRadius; y <= sectionRadius; y++)
                    for (int z = -sectionRadius; z <= sectionRadius; z++) {
                        SectionPos section = SectionPos.of(center.x() + x, center.y() + y, center.z() + z);
                        BlockPos point = section.center();
                        if (!level.isOutsideBuildHeight(point) && level.hasChunkAt(point) && level.isVillage(point))
                            sections.add(section.asLong());
                    }
                cache.sections = List.copyOf(sections);
                cache.sectionTick = now;
            } else sections.addAll(cache.sections);
        }
        return new Snapshot(List.copyOf(pois), List.copyOf(links), List.copyOf(sections), truncated);
    }

    public static List<Link> memories(Villager villager) {
        List<Link> links = new ArrayList<>();
        append(villager, MemoryModuleType.HOME, HOME, links);
        append(villager, MemoryModuleType.JOB_SITE, JOB, links);
        append(villager, MemoryModuleType.MEETING_POINT, MEETING, links);
        return List.copyOf(links);
    }

    private static void append(Villager villager, MemoryModuleType<GlobalPos> memory, int kind, List<Link> links) {
        villager.getBrain().getMemory(memory)
                .filter(pos -> pos.dimension().equals(villager.level().dimension()))
                .ifPresent(pos -> links.add(new Link(villager.getId(), villager.blockPosition(), pos.pos(), kind)));
    }

    private VillageHighlighterData() {}
}
