package net.xuwu.openblocks_reborn.village;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/** Exterior edges only: shared faces and coplanar subdivision lines are omitted. */
public final class VillageBoundaryEdges {
    public record Edge(BlockPos from, BlockPos to) {
        public static Edge ordered(BlockPos a, BlockPos b) {
            return a.asLong() <= b.asLong() ? new Edge(a, b) : new Edge(b, a);
        }
    }
    private static final class Count {
        int count;
        final EnumSet<Direction> normals = EnumSet.noneOf(Direction.class);
    }
    public static List<Edge> of(List<Long> sections) {
        var occupied = new HashSet<>(sections);
        var edges = new HashMap<Edge, Count>();
        for (long packed : occupied) {
            SectionPos section = SectionPos.of(packed);
            for (Direction face : Direction.values()) {
                if (occupied.contains(SectionPos.asLong(section.x() + face.getStepX(),
                        section.y() + face.getStepY(), section.z() + face.getStepZ()))) continue;
                var corners = face(section.origin(), face);
                for (int i = 0; i < 4; i++) {
                    Count count = edges.computeIfAbsent(Edge.ordered(corners[i], corners[(i + 1) % 4]), key -> new Count());
                    count.count++; count.normals.add(face);
                }
            }
        }
        var result = new ArrayList<Edge>();
        edges.forEach((edge, count) -> { if (count.count == 1 || count.normals.size() > 1) result.add(edge); });
        return List.copyOf(result);
    }
    private static BlockPos[] face(BlockPos p, Direction face) {
        int x = p.getX(), y = p.getY(), z = p.getZ(), s = 16;
        return switch (face) {
            case DOWN -> new BlockPos[]{new BlockPos(x,y,z),new BlockPos(x+s,y,z),new BlockPos(x+s,y,z+s),new BlockPos(x,y,z+s)};
            case UP -> new BlockPos[]{new BlockPos(x,y+s,z),new BlockPos(x+s,y+s,z),new BlockPos(x+s,y+s,z+s),new BlockPos(x,y+s,z+s)};
            case NORTH -> new BlockPos[]{new BlockPos(x,y,z),new BlockPos(x+s,y,z),new BlockPos(x+s,y+s,z),new BlockPos(x,y+s,z)};
            case SOUTH -> new BlockPos[]{new BlockPos(x,y,z+s),new BlockPos(x+s,y,z+s),new BlockPos(x+s,y+s,z+s),new BlockPos(x,y+s,z+s)};
            case WEST -> new BlockPos[]{new BlockPos(x,y,z),new BlockPos(x,y+s,z),new BlockPos(x,y+s,z+s),new BlockPos(x,y,z+s)};
            case EAST -> new BlockPos[]{new BlockPos(x+s,y,z),new BlockPos(x+s,y+s,z),new BlockPos(x+s,y+s,z+s),new BlockPos(x+s,y,z+s)};
        };
    }
    private VillageBoundaryEdges() {}
}
