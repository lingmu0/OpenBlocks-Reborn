package net.xuwu.openblocks_reborn.blockentity;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** The eleven shape generators exposed by the original OpenBlocks Guide. */
public enum GuideShape {
    SPHERE("sphere"),
    CYLINDER("cylinder"),
    CUBOID("cuboid"),
    FULL_CUBOID("full_cuboid"),
    DOME("dome"),
    TRIANGLE("triangle"),
    PENTAGON("pentagon"),
    HEXAGON("hexagon"),
    OCTAGON("octagon"),
    AXES("axes"),
    PLANES("planes");

    private final String id;

    GuideShape(String id) {
        this.id = id;
    }

    public String translationKey() {
        return "message.openblocks_reborn.guide.shape." + id;
    }

    public List<BlockPos> generate(int negX, int negY, int negZ,
                                   int posX, int posY, int posZ, int rotation) {
        Set<BlockPos> coordinates = new LinkedHashSet<>();
        switch (this) {
            case SPHERE -> ellipsoid(coordinates, negX, negY, negZ, posX, posY, posZ, false);
            case DOME -> ellipsoid(coordinates, negX, negY, negZ, posX, posY, posZ, true);
            case CYLINDER -> cylinder(coordinates, negX, negY, negZ, posX, posY, posZ);
            case CUBOID -> cuboid(coordinates, negX, negY, negZ, posX, posY, posZ, false);
            case FULL_CUBOID -> cuboid(coordinates, negX, negY, negZ, posX, posY, posZ, true);
            case TRIANGLE -> polygon(coordinates, 3, negX, negY, negZ, posX, posY, posZ);
            case PENTAGON -> polygon(coordinates, 5, negX, negY, negZ, posX, posY, posZ);
            case HEXAGON -> polygon(coordinates, 6, negX, negY, negZ, posX, posY, posZ);
            case OCTAGON -> polygon(coordinates, 8, negX, negY, negZ, posX, posY, posZ);
            case AXES -> axes(coordinates, negX, negY, negZ, posX, posY, posZ);
            case PLANES -> planes(coordinates, negX, negY, negZ, posX, posY, posZ);
        }
        coordinates.remove(BlockPos.ZERO);
        List<BlockPos> result = new ArrayList<>(coordinates.size());
        for (BlockPos coordinate : coordinates) result.add(rotateY(coordinate, rotation));
        result.sort((first, second) -> {
            int byX = Integer.compare(first.getX(), second.getX());
            if (byX != 0) return byX;
            int byAngle = Double.compare(Math.atan2(first.getZ(), first.getX()),
                    Math.atan2(second.getZ(), second.getX()));
            if (byAngle != 0) return byAngle;
            int byDistance = Double.compare(
                    second.getX() * second.getX() + second.getZ() * second.getZ(),
                    first.getX() * first.getX() + first.getZ() * first.getZ());
            return byDistance != 0 ? byDistance : Integer.compare(first.getZ(), second.getZ());
        });
        return List.copyOf(result);
    }

    private static BlockPos rotateY(BlockPos pos, int rotation) {
        return switch (Math.floorMod(rotation, 4)) {
            case 1 -> new BlockPos(-pos.getZ(), pos.getY(), pos.getX());
            case 2 -> new BlockPos(-pos.getX(), pos.getY(), -pos.getZ());
            case 3 -> new BlockPos(pos.getZ(), pos.getY(), -pos.getX());
            default -> pos;
        };
    }

    private static void ellipsoid(Set<BlockPos> output, int negX, int negY, int negZ,
                                  int posX, int posY, int posZ, boolean southOnly) {
        AxisRange xRange = AxisRange.of(-negX, posX);
        AxisRange yRange = AxisRange.of(-negY, posY);
        AxisRange zRange = AxisRange.of(-negZ, posZ);
        double inverseX = 1.0D / (xRange.radius() + 0.5D);
        double inverseY = 1.0D / (yRange.radius() + 0.5D);
        double inverseZ = 1.0D / (zRange.radius() + 0.5D);
        double nextX = 0.0D;
        outerX:
        for (int x = 0; x <= xRange.radius(); x++) {
            double normalizedX = nextX;
            nextX += inverseX;
            double nextY = 0.0D;
            for (int y = 0; y <= yRange.radius(); y++) {
                double normalizedY = nextY;
                nextY += inverseY;
                double nextZ = 0.0D;
                for (int z = 0; z <= zRange.radius(); z++) {
                    double normalizedZ = nextZ;
                    nextZ += inverseZ;
                    if (lengthSquared(normalizedX, normalizedY, normalizedZ) > 1.0D) {
                        if (z == 0) {
                            if (y == 0) break outerX;
                            break;
                        }
                        break;
                    }
                    if (lengthSquared(nextX, normalizedY, normalizedZ) <= 1.0D
                            && lengthSquared(normalizedX, nextY, normalizedZ) <= 1.0D
                            && lengthSquared(normalizedX, normalizedY, nextZ) <= 1.0D) {
                        continue;
                    }
                    for (int signX : new int[] {-1, 1}) {
                        for (int signY : new int[] {-1, 1}) {
                            for (int signZ : southOnly ? new int[] {1} : new int[] {-1, 1}) {
                                addMapped(output, xRange, x * signX, yRange, y * signY,
                                        zRange, z * signZ);
                            }
                        }
                    }
                }
            }
        }
    }

    private static double lengthSquared(double x, double y, double z) {
        return x * x + y * y + z * z;
    }

    private static double lengthSquared(double x, double z) {
        return x * x + z * z;
    }

    private static void addMapped(Set<BlockPos> output,
                                  AxisRange xRange, int x,
                                  AxisRange yRange, int y,
                                  AxisRange zRange, int z) {
        Integer mappedX = xRange.map(x);
        Integer mappedY = yRange.map(y);
        Integer mappedZ = zRange.map(z);
        if (mappedX != null && mappedY != null && mappedZ != null) {
            output.add(new BlockPos(mappedX, mappedY, mappedZ));
        }
    }

    private static void cylinder(Set<BlockPos> output, int negX, int negY, int negZ,
                                 int posX, int posY, int posZ) {
        AxisRange xRange = AxisRange.of(-negX, posX);
        AxisRange zRange = AxisRange.of(-negZ, posZ);
        double inverseX = 1.0D / (xRange.radius() + 0.5D);
        double inverseZ = 1.0D / (zRange.radius() + 0.5D);
        double nextX = 0.0D;
        outerX:
        for (int x = 0; x <= xRange.radius(); x++) {
            double normalizedX = nextX;
            nextX += inverseX;
            double nextZ = 0.0D;
            for (int z = 0; z <= zRange.radius(); z++) {
                double normalizedZ = nextZ;
                nextZ += inverseZ;
                if (lengthSquared(normalizedX, normalizedZ) > 1.0D) {
                    if (z == 0) break outerX;
                    break;
                }
                if (lengthSquared(nextX, normalizedZ) <= 1.0D
                        && lengthSquared(normalizedX, nextZ) <= 1.0D) {
                    continue;
                }
                for (int signX : new int[] {-1, 1}) {
                    for (int signZ : new int[] {-1, 1}) {
                        Integer mappedX = xRange.map(x * signX);
                        Integer mappedZ = zRange.map(z * signZ);
                        if (mappedX == null || mappedZ == null) continue;
                        for (int y = -negY; y <= posY; y++) {
                            output.add(new BlockPos(mappedX, y, mappedZ));
                        }
                    }
                }
            }
        }
    }

    private record AxisRange(int center, int radius, boolean skipMiddle) {
        private static AxisRange of(int minimum, int maximum) {
            int difference = maximum - minimum;
            return new AxisRange((minimum + maximum) / 2,
                    (difference & 1) == 0 ? difference / 2 : difference / 2 + 1,
                    (difference & 1) != 0);
        }

        private Integer map(int coordinate) {
            if (skipMiddle && coordinate == 0) return null;
            int shifted = skipMiddle && coordinate < 0 ? coordinate + 1 : coordinate;
            return center + shifted;
        }
    }

    private static void cuboid(Set<BlockPos> output, int negX, int negY, int negZ,
                               int posX, int posY, int posZ, boolean walls) {
        for (int x = -negX; x <= posX; x++) {
            for (int y = -negY; y <= posY; y++) {
                for (int z = -negZ; z <= posZ; z++) {
                    int boundaries = (x == -negX || x == posX ? 1 : 0)
                            + (y == -negY || y == posY ? 1 : 0)
                            + (z == -negZ || z == posZ ? 1 : 0);
                    if (walls ? boundaries >= 1 : boundaries >= 2) {
                        output.add(new BlockPos(x, y, z));
                    }
                }
            }
        }
    }

    private static void polygon(Set<BlockPos> output, int sides,
                                int negX, int negY, int negZ, int posX, int posY, int posZ) {
        double centerX = (posX - negX) / 2.0D;
        double centerZ = (posZ - negZ) / 2.0D;
        double radiusX = (posX + negX) / 2.0D;
        double radiusZ = (posZ + negZ) / 2.0D;
        List<BlockPos> points = new ArrayList<>();
        for (int index = 0; index < sides; index++) {
            double angle = Math.PI * 2.0D * index / sides;
            points.add(new BlockPos((int)Math.round(centerX + radiusX * Math.cos(angle)), 0,
                    (int)Math.round(centerZ + radiusZ * Math.sin(angle))));
        }
        Set<BlockPos> perimeter = new LinkedHashSet<>();
        for (int index = 0; index < points.size(); index++) {
            line2D(perimeter, points.get(index), points.get((index + 1) % points.size()));
        }
        for (BlockPos point : perimeter) {
            for (int y = -negY; y <= posY; y++) {
                output.add(new BlockPos(point.getX(), y, point.getZ()));
            }
        }
    }

    private static void line2D(Set<BlockPos> output, BlockPos start, BlockPos end) {
        int x = start.getX();
        int z = start.getZ();
        int dx = Math.abs(end.getX() - x);
        int sx = x < end.getX() ? 1 : -1;
        int dz = -Math.abs(end.getZ() - z);
        int sz = z < end.getZ() ? 1 : -1;
        int error = dx + dz;
        while (true) {
            output.add(new BlockPos(x, 0, z));
            if (x == end.getX() && z == end.getZ()) return;
            int doubled = error * 2;
            if (doubled >= dz) {
                error += dz;
                x += sx;
            }
            if (doubled <= dx) {
                error += dx;
                z += sz;
            }
        }
    }

    private static void axes(Set<BlockPos> output, int negX, int negY, int negZ,
                             int posX, int posY, int posZ) {
        for (int x = -negX; x <= posX; x++) output.add(new BlockPos(x, 0, 0));
        for (int y = -negY; y <= posY; y++) output.add(new BlockPos(0, y, 0));
        for (int z = -negZ; z <= posZ; z++) output.add(new BlockPos(0, 0, z));
    }

    private static void planes(Set<BlockPos> output, int negX, int negY, int negZ,
                               int posX, int posY, int posZ) {
        for (int y = -negY; y <= posY; y++) {
            for (int z = -negZ; z <= posZ; z++) output.add(new BlockPos(0, y, z));
            for (int x = -negX; x <= posX; x++) output.add(new BlockPos(x, y, 0));
        }
        for (int x = -negX; x <= posX; x++) {
            for (int z = -negZ; z <= posZ; z++) output.add(new BlockPos(x, 0, z));
        }
    }
}
