package net.xuwu.openblocks_reborn.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.xuwu.openblocks_reborn.registry.ModBlockEntities;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.xuwu.openblocks_reborn.item.StencilItem;

import java.util.Arrays;

public class CanvasBlockEntity extends BlockEntity {
    public static final int SIDE_SIZE = 16;
    public static final int PIXELS_PER_FACE = SIDE_SIZE * SIDE_SIZE;
    public static final int TOTAL_PIXELS = 6 * PIXELS_PER_FACE;
    private final int[] pixels = new int[TOTAL_PIXELS];
    private final int[] stencilPatterns = new int[6];
    private final int[] stencilRotations = new int[6];

    public CanvasBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CANVAS.get(), pos, state);
        Arrays.fill(pixels, -1);
        Arrays.fill(stencilPatterns, -1);
    }

    public int getColor(Direction face) {
        return pixels[face.get3DDataValue() * PIXELS_PER_FACE];
    }

    public int[] copyColors() {
        int[] colors = new int[6];
        for (Direction face : Direction.values()) colors[face.get3DDataValue()] = getColor(face);
        return colors;
    }

    public int getPixel(Direction face, int x, int y) {
        return pixels[pixelIndex(face, x, y)];
    }

    public int[] copyPixels() {
        return pixels.clone();
    }

    public void setPixel(Direction face, int x, int y, int color) {
        pixels[pixelIndex(face, x, y)] = color;
        changedAndSync();
    }

    public void setColor(Direction face, int color) {
        int start = face.get3DDataValue() * PIXELS_PER_FACE;
        Arrays.fill(pixels, start, start + PIXELS_PER_FACE, color);
        changedAndSync();
    }

    public void setAll(int color) {
        Arrays.fill(pixels, color);
        changedAndSync();
    }

    public int getStencilPattern(Direction face) {
        return stencilPatterns[face.get3DDataValue()];
    }

    public int getStencilRotation(Direction face) {
        return stencilRotations[face.get3DDataValue()];
    }

    public boolean placeStencil(Direction face, int pattern) {
        int index = face.get3DDataValue();
        if (stencilPatterns[index] >= 0) return false;
        stencilPatterns[index] = Math.floorMod(pattern, StencilItem.PATTERN_COUNT);
        stencilRotations[index] = 0;
        changedAndSync();
        return true;
    }

    public boolean rotateStencil(Direction face) {
        int index = face.get3DDataValue();
        if (stencilPatterns[index] < 0) return false;
        stencilRotations[index] = (stencilRotations[index] + 1) & 3;
        changedAndSync();
        return true;
    }

    /** Removes and returns the prepared stencil pattern, or -1 when absent. */
    public int removeStencil(Direction face) {
        int index = face.get3DDataValue();
        int result = stencilPatterns[index];
        if (result < 0) return -1;
        stencilPatterns[index] = -1;
        stencilRotations[index] = 0;
        changedAndSync();
        return result;
    }

    /**
     * Paints a complete face when no stencil is attached, or only the current
     * stencil cut-outs when a cover is present.
     */
    public void applyPaint(Direction face, int color) {
        int faceIndex = face.get3DDataValue();
        int pattern = stencilPatterns[faceIndex];
        int start = faceIndex * PIXELS_PER_FACE;
        if (pattern < 0) {
            Arrays.fill(pixels, start, start + PIXELS_PER_FACE, color);
        } else {
            int rotation = stencilRotations[faceIndex];
            for (int y = 0; y < SIDE_SIZE; y++) {
                for (int x = 0; x < SIDE_SIZE; x++) {
                    if (StencilItem.isHole(pattern, rotation, x, y)) {
                        pixels[start + y * SIDE_SIZE + x] = color;
                    }
                }
            }
        }
        changedAndSync();
    }

    /**
     * Paints one pixel for the small brush. An attached stencil still masks
     * solid pixels, so the item preview and the actual painted pattern agree.
     */
    public boolean applyPaintPixel(Direction face, int x, int y, int color) {
        int faceIndex = face.get3DDataValue();
        int pattern = stencilPatterns[faceIndex];
        if (pattern >= 0 && !StencilItem.isHole(pattern, stencilRotations[faceIndex], x, y)) {
            return false;
        }
        pixels[pixelIndex(face, x, y)] = color;
        changedAndSync();
        return true;
    }

    public void applyPaintAll(int color) {
        for (Direction face : Direction.values()) {
            int faceIndex = face.get3DDataValue();
            int pattern = stencilPatterns[faceIndex];
            int start = faceIndex * PIXELS_PER_FACE;
            if (pattern < 0) {
                Arrays.fill(pixels, start, start + PIXELS_PER_FACE, color);
            } else {
                int rotation = stencilRotations[faceIndex];
                for (int y = 0; y < SIDE_SIZE; y++) {
                    for (int x = 0; x < SIDE_SIZE; x++) {
                        if (StencilItem.isHole(pattern, rotation, x, y)) {
                            pixels[start + y * SIDE_SIZE + x] = color;
                        }
                    }
                }
            }
        }
        changedAndSync();
    }

    public void setColors(int[] incoming) {
        if (incoming.length != 6) return;
        for (Direction face : Direction.values()) {
            int start = face.get3DDataValue() * PIXELS_PER_FACE;
            Arrays.fill(pixels, start, start + PIXELS_PER_FACE, incoming[face.get3DDataValue()]);
        }
        changedAndSync();
    }

    public void setPixels(int[] incoming) {
        if (incoming.length == 6) {
            setColors(incoming);
            return;
        }
        if (incoming.length != TOTAL_PIXELS) return;
        System.arraycopy(incoming, 0, pixels, 0, TOTAL_PIXELS);
        changedAndSync();
    }

    public int paintedFaces() {
        int result = 0;
        for (Direction face : Direction.values()) {
            int start = face.get3DDataValue() * PIXELS_PER_FACE;
            for (int i = start; i < start + PIXELS_PER_FACE; i++) {
                if (pixels[i] != -1) {
                    result++;
                    break;
                }
            }
        }
        return result;
    }

    public int paintedPixels() {
        int result = 0;
        for (int color : pixels) if (color != -1) result++;
        return result;
    }

    public ItemStack toItemStack() {
        ItemStack result = new ItemStack(getBlockState().getBlock().asItem());
        CompoundTag data = new CompoundTag();
        data.putIntArray("Colors", copyColors());
        data.putIntArray("Pixels", pixels);
        data.putIntArray("StencilPatterns", stencilPatterns);
        data.putIntArray("StencilRotations", stencilRotations);
        BlockItem.setBlockEntityData(result, ModBlockEntities.CANVAS.get(), data);
        return result;
    }

    private void changedAndSync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        Arrays.fill(pixels, -1);
        Arrays.fill(stencilPatterns, -1);
        Arrays.fill(stencilRotations, 0);
        int[] loadedPixels = tag.getIntArray("Pixels");
        if (loadedPixels.length == TOTAL_PIXELS) {
            System.arraycopy(loadedPixels, 0, pixels, 0, TOTAL_PIXELS);
        } else {
            int[] loadedColors = tag.getIntArray("Colors");
            if (loadedColors.length == 6) {
                for (Direction face : Direction.values()) {
                    int start = face.get3DDataValue() * PIXELS_PER_FACE;
                    Arrays.fill(pixels, start, start + PIXELS_PER_FACE, loadedColors[face.get3DDataValue()]);
                }
            }
        }
        int[] loadedPatterns = tag.getIntArray("StencilPatterns");
        if (loadedPatterns.length == stencilPatterns.length) {
            for (int index = 0; index < stencilPatterns.length; index++) {
                stencilPatterns[index] = loadedPatterns[index] < 0 ? -1
                        : Math.floorMod(loadedPatterns[index], StencilItem.PATTERN_COUNT);
            }
        }
        int[] loadedRotations = tag.getIntArray("StencilRotations");
        if (loadedRotations.length == stencilRotations.length) {
            for (int index = 0; index < stencilRotations.length; index++) {
                stencilRotations[index] = Math.floorMod(loadedRotations[index], 4);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putIntArray("Colors", copyColors());
        tag.putIntArray("Pixels", pixels);
        tag.putIntArray("StencilPatterns", stencilPatterns);
        tag.putIntArray("StencilRotations", stencilRotations);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private static int pixelIndex(Direction face, int x, int y) {
        int clampedX = net.minecraft.util.Mth.clamp(x, 0, SIDE_SIZE - 1);
        int clampedY = net.minecraft.util.Mth.clamp(y, 0, SIDE_SIZE - 1);
        return face.get3DDataValue() * PIXELS_PER_FACE + clampedY * SIDE_SIZE + clampedX;
    }
}
