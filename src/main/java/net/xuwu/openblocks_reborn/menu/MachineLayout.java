package net.xuwu.openblocks_reborn.menu;

import net.xuwu.openblocks_reborn.block.UtilityMachineBlock;

/**
 * Slot geometry from the classic OpenBlocks 1.8.1 GUIs. Coordinates are relative
 * to the top-left corner of the machine window.
 */
public enum MachineLayout {
    AUTO_ANVIL(176, 175, 8, 93, slots(new int[][] {{14, 40}, {56, 40}, {110, 40}})),
    AUTO_ENCHANTMENT_TABLE(176, 175, 8, 93, slots(new int[][] {{18, 20}, {18, 40}, {100, 40}})),
    BIG_BUTTON(176, 182, 8, 100, grid(53, 30, 4, 2)),
    PAINT_MIXER(176, 200, 8, 120, slots(new int[][] {
            {133, 22}, {-100, -100}, {123, 76}, {143, 76}, {123, 96}, {143, 96}
    })),
    DRAWING_TABLE(176, 204, 8, 122, slots(new int[][] {{63, 34}, {97, 34}})),
    PROJECTOR(176, 234, 8, 152, slots(new int[][] {{79, 130}})),
    SPRINKLER(176, 166, 8, 84, grid(62, 17, 3, 3)),
    VACUUM_HOPPER(176, 151, 8, 69, grid(44, 20, 5, 2)),
    XP_BOTTLER(176, 151, 8, 69, slots(new int[][] {{48, 30}, {110, 30}})),
    BLOCK_PLACER(176, 166, 8, 84, grid(62, 17, 3, 3)),
    ITEM_DROPPER(176, 167, 8, 85, grid(8, 18, 3, 3)),
    CANNON(176, 167, 8, 85, grid(8, 18, 3, 3)),
    LUGGAGE(176, 167, 8, 85, grid(8, 18, 9, 3)),
    DONATION_STATION(176, 172, 8, 90, slots(new int[][] {{30, 30}})),
    DEV_NULL(176, 137, 8, 55, slots(new int[][] {{80, 22}}));

    private final int width;
    private final int height;
    private final int playerX;
    private final int playerY;
    private final int[][] machineSlots;

    MachineLayout(int width, int height, int playerX, int playerY, int[][] machineSlots) {
        this.width = width;
        this.height = height;
        this.playerX = playerX;
        this.playerY = playerY;
        this.machineSlots = machineSlots;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int playerX() {
        return playerX;
    }

    public int playerY() {
        return playerY;
    }

    public int machineSlotCount() {
        return machineSlots.length;
    }

    public int machineSlotX(int slot) {
        return machineSlots[slot][0];
    }

    public int machineSlotY(int slot) {
        return machineSlots[slot][1];
    }

    public static MachineLayout fromNetwork(int id) {
        MachineLayout[] values = values();
        return id >= 0 && id < values.length ? values[id] : BLOCK_PLACER;
    }

    public static MachineLayout forUtility(UtilityMachineBlock.Kind kind) {
        return switch (kind) {
            case AUTO_ANVIL -> AUTO_ANVIL;
            case AUTO_ENCHANTMENT_TABLE -> AUTO_ENCHANTMENT_TABLE;
            case DONATION_STATION -> DONATION_STATION;
            case PAINT_MIXER -> PAINT_MIXER;
            case DRAWING_TABLE -> DRAWING_TABLE;
            case PROJECTOR -> PROJECTOR;
            case SPRINKLER -> SPRINKLER;
        };
    }

    private static int[][] slots(int[][] positions) {
        return positions;
    }

    private static int[][] grid(int x, int y, int columns, int rows) {
        return grid(x, y, columns, rows, columns * rows);
    }

    private static int[][] grid(int x, int y, int columns, int rows, int count) {
        int[][] result = new int[count][2];
        for (int slot = 0; slot < count; slot++) {
            result[slot][0] = x + slot % columns * 18;
            result[slot][1] = y + slot / columns * 18;
        }
        return result;
    }
}
