package net.xuwu.openblocks_reborn.gametest;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.xuwu.openblocks_reborn.OpenBlocksReborn;
import net.xuwu.openblocks_reborn.network.VillageHighlighterSnapshotPayload;
import net.xuwu.openblocks_reborn.village.HighlighterQueryLimiter;
import net.xuwu.openblocks_reborn.village.VillageBoundaryEdges;
import net.xuwu.openblocks_reborn.village.VillageHighlighterData;

import java.util.Arrays;
import java.util.List;

@GameTestHolder(OpenBlocksReborn.MOD_ID)
@PrefixGameTestTemplate(true)
public final class VillageHighlighterGameTests {
    @GameTest(template = "empty", batch = "highlighter", timeoutTicks = 100)
    public static void actualOwnershipSingleSelectionAndDenseQueryBudget(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos anchor = helper.absolutePos(new BlockPos(2, 1, 2));
        // This fixture reuses the isolated acceptance world across runs. Remove prior
        // fixture villagers so their old Brain memories cannot become competing owners.
        level.getEntitiesOfClass(net.minecraft.world.entity.npc.Villager.class,
                new net.minecraft.world.phys.AABB(anchor).inflate(64)).forEach(net.minecraft.world.entity.Entity::discard);
        BlockPos job = helper.absolutePos(new BlockPos(6, 1, 3));
        BlockPos foot = helper.absolutePos(new BlockPos(3, 1, 3)), head = foot.east();
        level.setBlockAndUpdate(job, Blocks.COMPOSTER.defaultBlockState());
        level.setBlockAndUpdate(foot, Blocks.RED_BED.defaultBlockState()
                .setValue(BedBlock.FACING, Direction.EAST).setValue(BedBlock.PART, BedPart.FOOT));
        level.setBlockAndUpdate(head, Blocks.RED_BED.defaultBlockState()
                .setValue(BedBlock.FACING, Direction.EAST).setValue(BedBlock.PART, BedPart.HEAD));
        var registry = level.registryAccess().registryOrThrow(Registries.POINT_OF_INTEREST_TYPE);
        level.getPoiManager().add(job, registry.getHolderOrThrow(PoiTypes.FARMER));
        level.getPoiManager().add(head, registry.getHolderOrThrow(PoiTypes.HOME));
        var nearby = helper.spawn(EntityType.VILLAGER, 5, 1, 3);
        nearby.setNoAi(true); nearby.setNoGravity(true);
        var owner = helper.spawn(EntityType.VILLAGER, 1, 1, 1);
        owner.setNoAi(true); owner.setNoGravity(true);
        owner.getBrain().setMemory(MemoryModuleType.JOB_SITE, GlobalPos.of(level.dimension(), job));
        owner.getBrain().setMemory(MemoryModuleType.HOME, GlobalPos.of(level.dimension(), head));
        owner.getBrain().setMemory(MemoryModuleType.MEETING_POINT, GlobalPos.of(Level.NETHER, job));
        VillageHighlighterData.clearCache(level);
        var single = VillageHighlighterData.collect(level, anchor, job, false, 32, false);
        helper.assertTrue(single.links().size() == 2 && single.links().stream().allMatch(link -> link.villagerId() == owner.getId()),
                "Single must use actual owner memories, ignore nearest villager and other-dimension memory");
        var bed = VillageHighlighterData.collect(level, anchor, foot, false, 32, false);
        helper.assertTrue(bed.pois().size() == 1 && bed.pois().getFirst().pos().equals(head)
                && bed.links().size() == 2, "Bed foot must resolve to HOME at its head");
        nearby.getBrain().setMemory(MemoryModuleType.JOB_SITE, GlobalPos.of(level.dimension(), job));
        VillageHighlighterData.clearCache(level);
        single = VillageHighlighterData.collect(level, anchor, job, false, 32, false);
        helper.assertTrue(single.links().stream().map(VillageHighlighterData.Link::villagerId).distinct().count() == 1,
                "Single must never show multiple owners of a POI");

        for (int i = 0; i < 140; i++) {
            var villager = helper.spawn(EntityType.VILLAGER, 2 + i % 3, 1, 2 + i % 5);
            villager.setNoAi(true); villager.setNoGravity(true);
            villager.getBrain().setMemory(MemoryModuleType.JOB_SITE, GlobalPos.of(level.dimension(), job));
        }
        int chunks = level.getChunkSource().getLoadedChunksCount();
        VillageHighlighterData.clearCache(level);
        var dense = VillageHighlighterData.collect(level, anchor, anchor, true, 32, true);
        helper.assertTrue(dense.truncated() && dense.pois().size() <= VillageHighlighterData.MAX_POIS
                && dense.links().stream().map(VillageHighlighterData.Link::villagerId).distinct().count() <= 16,
                "Dense snapshots must report truncation and respect rendering budget");
        var unloaded = VillageHighlighterData.collect(level, anchor, anchor.offset(100000, 0, 100000), false, 32, false);
        helper.assertTrue(unloaded.pois().isEmpty() && level.getChunkSource().getLoadedChunksCount() == chunks,
                "Query must not load distant chunks");
        for (int i = 0; i < 100; i++) VillageHighlighterData.collect(level, anchor, anchor, true, 32, true);
        long[] cold = new long[50], hot = new long[300];
        for (int i = 0; i < cold.length; i++) {
            VillageHighlighterData.clearCache(level);
            long start = System.nanoTime();
            VillageHighlighterData.collect(level, anchor, anchor, true, 32, true);
            cold[i] = System.nanoTime() - start;
        }
        for (int i = 0; i < hot.length; i++) {
            long start = System.nanoTime();
            VillageHighlighterData.collect(level, anchor, anchor, true, 32, true);
            hot[i] = System.nanoTime() - start;
        }
        timing("cold", cold); timing("cached", hot);
        OpenBlocksReborn.LOGGER.info("HIGHLIGHTER_BENCH villagers=142 scannedCap=128 owners={} chunksBefore={} chunksAfter={}",
                dense.links().stream().map(VillageHighlighterData.Link::villagerId).distinct().count(), chunks,
                level.getChunkSource().getLoadedChunksCount());
        VillageHighlighterData.clearCache(level);
        helper.succeed();
        OpenBlocksReborn.LOGGER.info("REDUX_TEST_PASS highlighterActualOwnershipAndBudget");
    }

    @GameTest(template = "empty", batch = "highlighter", timeoutTicks = 20)
    public static void serverCooldownAndExteriorGeometry(GameTestHelper helper) {
        var limiter = new HighlighterQueryLimiter();
        helper.assertTrue(limiter.acquire(0, true) && !limiter.acquire(0, true)
                && limiter.acquire(10, false) && !limiter.acquire(199, true)
                && limiter.acquire(200, true), "Snapshot cooldown must be enforced independently of Single requests");
        var wrap = new HighlighterQueryLimiter();
        helper.assertTrue(wrap.acquire(Integer.MAX_VALUE - 50, true)
                && !wrap.acquire(Integer.MIN_VALUE + 20, true)
                && wrap.acquire(Integer.MIN_VALUE + 149, true), "Cooldown must tolerate server tick wraparound");
        var box = VillageHighlighterData.golemSearchBox(new BlockPos(0, 0, 0));
        helper.assertTrue(box.getXsize() == 17 && box.getYsize() == 13 && box.getZsize() == 17
                && box.minX == -8 && box.minY == -6, "Golem search box must enclose the correct block volume");
        helper.assertTrue(VillageBoundaryEdges.of(List.of(SectionPos.asLong(0, 0, 0))).size() == 12,
                "Single section needs twelve edges");
        var joined = VillageBoundaryEdges.of(List.of(SectionPos.asLong(0, 0, 0), SectionPos.asLong(1, 0, 0)));
        helper.assertTrue(joined.stream().noneMatch(edge -> edge.from().getX() == 16 && edge.to().getX() == 16),
                "Adjacent sections must omit their shared internal plane");
        helper.succeed();
        OpenBlocksReborn.LOGGER.info("REDUX_TEST_PASS highlighterCooldownAndGeometry");
    }

    @GameTest(template = "empty", batch = "highlighter", timeoutTicks = 20)
    public static void boundedSnapshotCodec(GameTestHelper helper) {
        var snapshot = new VillageHighlighterData.Snapshot(
                List.of(new VillageHighlighterData.Poi(BlockPos.ZERO, 1, true)),
                List.of(new VillageHighlighterData.Link(42, new BlockPos(1, 2, 3), BlockPos.ZERO, 1)),
                List.of(SectionPos.asLong(0, 1, 0)), true);
        var expected = new VillageHighlighterSnapshotPayload(BlockPos.ZERO, BlockPos.ZERO, true, snapshot);
        var buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            VillageHighlighterSnapshotPayload.STREAM_CODEC.encode(buf, expected);
            helper.assertTrue(expected.equals(VillageHighlighterSnapshotPayload.STREAM_CODEC.decode(buf)), "Snapshot codec round trip failed");
            buf.clear();
            buf.writeBlockPos(BlockPos.ZERO); buf.writeBlockPos(BlockPos.ZERO);
            buf.writeBoolean(true); buf.writeBoolean(false); buf.writeVarInt(VillageHighlighterData.MAX_POIS + 1);
            boolean rejected = false;
            try { VillageHighlighterSnapshotPayload.STREAM_CODEC.decode(buf); }
            catch (DecoderException e) { rejected = true; }
            helper.assertTrue(rejected, "Oversized network snapshots must be rejected before allocation");
        } finally { buf.release(); }
        helper.succeed();
        OpenBlocksReborn.LOGGER.info("REDUX_TEST_PASS highlighterBoundedCodec");
    }

    private static void timing(String kind, long[] values) {
        Arrays.sort(values);
        OpenBlocksReborn.LOGGER.info("HIGHLIGHTER_BENCH {} n={} meanMs={} p95Ms={} maxMs={}", kind, values.length,
                Arrays.stream(values).average().orElse(0) / 1e6, values[(int)(values.length * .95) - 1] / 1e6,
                values[values.length - 1] / 1e6);
    }
}
