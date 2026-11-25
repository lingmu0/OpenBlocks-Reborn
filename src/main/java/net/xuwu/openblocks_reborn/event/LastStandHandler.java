package net.xuwu.openblocks_reborn.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.xuwu.openblocks_reborn.OpenBlocksReborn;
import net.xuwu.openblocks_reborn.config.EnchantmentConfig;
import net.xuwu.openblocks_reborn.registry.ModEnchantments;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

/** Only rescues actual damage deaths after vanilla's totem check has failed. */
@EventBusSubscriber(modid = OpenBlocksReborn.MOD_ID)
public final class LastStandHandler {
    public static final String COOLDOWN = "openblocks_reborn.last_stand_cooldown";
    private record Hit(WeakReference<DamageSource> source, int tick, float health, boolean lethal) {}
    private static final Map<ServerPlayer, Hit> HITS = new WeakHashMap<>();

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void captureHealth(LivingDamageEvent.Pre event) {
        if (event.getEntity() instanceof ServerPlayer player && EnchantmentConfig.LAST_STAND_ENABLED.get())
            HITS.put(player, new Hit(new WeakReference<>(event.getSource()), player.getServer().getTickCount(), player.getHealth(), false));
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void captureFinalDamage(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Hit hit = HITS.remove(player);
        if (hit != null && hit.source.get() == event.getSource() && hit.tick == player.getServer().getTickCount()
                && player.getHealth() <= 0 && event.getNewDamage() > 0) {
            // Post damage has already accounted for armor, effects and absorption.
            HITS.put(player, new Hit(hit.source, hit.tick, hit.health - event.getNewDamage(), true));
        }
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void rescue(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Hit hit = HITS.remove(player);
        if (!EnchantmentConfig.LAST_STAND_ENABLED.get() || player.getHealth() > 0) return;
        int seconds = EnchantmentConfig.LAST_STAND_COOLDOWN_SECONDS.get();
        if (seconds > 0 && player.getPersistentData().getInt(COOLDOWN) > 0) return;
        int levels = 0;
        for (var stack : player.getArmorSlots()) levels += ModEnchantments.level(player, stack, ModEnchantments.LAST_STAND);
        if (levels <= 0) return;
        boolean measured = hit != null && hit.lethal && hit.source.get() == event.getSource()
                && hit.tick == player.getServer().getTickCount();
        // Direct death calls may have no damage event. Every damage source is eligible;
        // absent an overkill measurement, use the base cost of restoring one health point.
        int cost = EnchantmentConfig.experienceCost(measured ? hit.health : 0, levels);
        if (player.totalExperience < cost) return;
        player.giveExperiencePoints(-cost);
        player.setHealth(1.0F);
        player.fallDistance = 0;
        event.setCanceled(true);
        if (seconds > 0) player.getPersistentData().putInt(COOLDOWN, seconds * 20);
        else player.getPersistentData().remove(COOLDOWN);
        player.level().playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, .7F, 1.25F);
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Hit hit = HITS.get(player);
        if (hit != null && hit.tick != player.getServer().getTickCount()) HITS.remove(player);
        int ticks = player.getPersistentData().getInt(COOLDOWN);
        if (ticks > 1) player.getPersistentData().putInt(COOLDOWN, ticks - 1);
        else if (ticks != 0) player.getPersistentData().remove(COOLDOWN);
    }
    private LastStandHandler() {}
}
