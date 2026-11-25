package net.xuwu.openblocks_reborn.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Acquisition is loaded before datapacks; effect settings belong to the authoritative world. */
public final class EnchantmentConfig {
    public static final ModConfigSpec ACQUISITION_SPEC, SERVER_SPEC;
    public static final ModConfigSpec.BooleanValue LAST_STAND_TREASURE, UNSTABLE_TREASURE, FLIM_FLAM_TREASURE;
    public static final ModConfigSpec.BooleanValue LAST_STAND_ENABLED, UNSTABLE_ENABLED, FLIM_FLAM_ENABLED, UNSTABLE_BLOCK_DAMAGE;
    public static final ModConfigSpec.DoubleValue LAST_STAND_XP_MULTIPLIER, UNSTABLE_POWER_MULTIPLIER;
    public static final ModConfigSpec.IntValue LAST_STAND_COOLDOWN_SECONDS;
    public static final ModConfigSpec.BooleanValue FLIM_SHUFFLE, FLIM_LAUNCH, FLIM_NAUSEA, FLIM_SLOWNESS, FLIM_RENAME, FLIM_TELEPORT;
    static {
        var acquisition = new ModConfigSpec.Builder();
        acquisition.comment("Treasure classification. Restart the world/server after changes. Defaults match OpenBlocks 1.12.2: all non-treasure.").push("treasure");
        LAST_STAND_TREASURE = acquisition.translation("config.openblocks_reborn.lastStandTreasure").worldRestart().define("lastStand", false);
        UNSTABLE_TREASURE = acquisition.translation("config.openblocks_reborn.unstableTreasure").worldRestart().define("unstable", false);
        FLIM_FLAM_TREASURE = acquisition.translation("config.openblocks_reborn.flimFlamTreasure").worldRestart().define("flimFlam", false);
        acquisition.pop();
        ACQUISITION_SPEC = acquisition.build();
        var server = new ModConfigSpec.Builder();
        server.comment("Effect switches do not delete enchantments or change how they are obtained.").push("lastStand");
        LAST_STAND_ENABLED = server.translation("config.openblocks_reborn.effectEnabled").define("enabled", true);
        LAST_STAND_XP_MULTIPLIER = server.comment("Multiplier on the legacy XP-point formula; rounded up, at least one point.")
                .translation("config.openblocks_reborn.xpMultiplier").defineInRange("experienceCostMultiplier", 1.0, 0.1, 10.0);
        LAST_STAND_COOLDOWN_SECONDS = server.comment("Seconds after a successful Last Stand rescue. Zero disables cooldown; not started by vanilla totems or failed attempts.")
                .translation("config.openblocks_reborn.rescueCooldown").defineInRange("cooldownSeconds", 0, 0, 600);
        server.pop().push("unstable");
        UNSTABLE_ENABLED = server.translation("config.openblocks_reborn.effectEnabled").define("enabled", true);
        UNSTABLE_POWER_MULTIPLIER = server.comment("Multiplies level I/II/III powers 2/3/4. Final power limited to 0.5..8 (TNT is 4).")
                .translation("config.openblocks_reborn.explosionMultiplier").defineInRange("explosionStrengthMultiplier", 1.0, 0.25, 2.0);
        UNSTABLE_BLOCK_DAMAGE = server.comment("Allow terrain destruction at level III. Levels I and II remain non-destructive.")
                .translation("config.openblocks_reborn.explosionBlockDamage").define("allowBlockDamage", false);
        server.pop().push("flimFlam");
        FLIM_FLAM_ENABLED = server.translation("config.openblocks_reborn.effectEnabled").define("enabled", true);
        server.comment("All prank effects are opt-in. With none enabled, no bad luck, sounds or messages are generated.").push("effects");
        FLIM_SHUFFLE = server.translation("config.openblocks_reborn.flimShuffle").define("shuffleHotbar", false);
        FLIM_LAUNCH = server.translation("config.openblocks_reborn.flimLaunch").define("launchUpwards", false);
        FLIM_NAUSEA = server.translation("config.openblocks_reborn.flimNausea").define("nausea", false);
        FLIM_SLOWNESS = server.translation("config.openblocks_reborn.flimSlowness").define("slowness", false);
        FLIM_RENAME = server.translation("config.openblocks_reborn.flimRename").define("renameHeldItem", false);
        FLIM_TELEPORT = server.translation("config.openblocks_reborn.flimTeleport").define("randomTeleport", false);
        server.pop();
        server.pop();
        SERVER_SPEC = server.build();
    }
    public static int experienceCost(float projectedHealth, int totalLevels) {
        if (totalLevels <= 0 || !Float.isFinite(projectedHealth)) return Integer.MAX_VALUE;
        double cost = Math.ceil(50.0 * Math.max(1.0, 1.0 - projectedHealth) / totalLevels * LAST_STAND_XP_MULTIPLIER.get());
        return (int)Math.max(1, Math.min(Integer.MAX_VALUE, cost));
    }
    public static float explosionPower(int level) {
        double base = 1.0 + Math.clamp(level, 1, 3);
        return (float)Math.clamp(base * UNSTABLE_POWER_MULTIPLIER.get(), 0.5, 8.0);
    }
    public static boolean anyPrankEnabled() {
        return FLIM_SHUFFLE.get() || FLIM_LAUNCH.get() || FLIM_NAUSEA.get() || FLIM_SLOWNESS.get() || FLIM_RENAME.get() || FLIM_TELEPORT.get();
    }
    private EnchantmentConfig() {}
}
