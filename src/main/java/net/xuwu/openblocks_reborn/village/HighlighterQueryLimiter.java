package net.xuwu.openblocks_reborn.village;

/** Server-owned cooldown; rejected attempts never extend it. Tick subtraction tolerates wraparound. */
public final class HighlighterQueryLimiter {
    public static final int QUERY_TICKS = 10, SNAPSHOT_COOLDOWN_TICKS = 200, SNAPSHOT_VISIBLE_TICKS = 200;
    private boolean queried, scanned;
    private int lastQuery, lastScan;

    public boolean acquire(int tick, boolean snapshot) {
        if (queried && tick - lastQuery < QUERY_TICKS) return false;
        if (snapshot && scanned && tick - lastScan < SNAPSHOT_COOLDOWN_TICKS) return false;
        queried = true;
        lastQuery = tick;
        if (snapshot) { scanned = true; lastScan = tick; }
        return true;
    }
}
