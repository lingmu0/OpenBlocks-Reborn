package net.xuwu.openblocks_reborn.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.xuwu.openblocks_reborn.OpenBlocksReborn;
import net.xuwu.openblocks_reborn.config.EnchantmentConfig;
import net.xuwu.openblocks_reborn.event.CommonEvents;
import net.xuwu.openblocks_reborn.event.LastStandHandler;
import net.xuwu.openblocks_reborn.registry.ModEnchantments;

@GameTestHolder(OpenBlocksReborn.MOD_ID)
@PrefixGameTestTemplate(true)
public final class EnchantmentGameTests {
    private static ServerPlayer player(GameTestHelper helper, int xp, boolean enchanted) {
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "enchant-test"), false);
        var server = helper.getLevel().getServer();
        var player = new ServerPlayer(server, helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        server.getPlayerList().placeNewPlayer(connection, player, cookie);
        try {
            var grace = ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
            grace.setAccessible(true); grace.setInt(player, 0);
        } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
        player.setGameMode(GameType.SURVIVAL);
        player.getAbilities().invulnerable = false;
        player.getAbilities().instabuild = false;
        player.teleportTo(helper.absolutePos(new BlockPos(2, 2, 2)).getCenter().x,
                helper.absolutePos(new BlockPos(2, 2, 2)).getCenter().y,
                helper.absolutePos(new BlockPos(2, 2, 2)).getCenter().z);
        player.giveExperiencePoints(xp);
        if (enchanted) {
            var helmet = new ItemStack(Items.LEATHER_HELMET);
            helmet.enchant(ModEnchantments.holder(player, ModEnchantments.LAST_STAND), 2);
            player.setItemSlot(EquipmentSlot.HEAD, helmet);
        }
        return player;
    }
    private static void hit(ServerPlayer player, float damage) {
        player.invulnerableTime = 0;
        player.hurt(player.damageSources().generic(), damage);
    }
    private static void pass(GameTestHelper helper, String name) {
        helper.succeed(); OpenBlocksReborn.LOGGER.info("REDUX_TEST_PASS {}", name);
    }
    @GameTest(template = "empty", timeoutTicks = 30)
    public static void numericBoundsAndIndependentStatusEffects(GameTestHelper helper) throws ReflectiveOperationException {
        double xp = EnchantmentConfig.LAST_STAND_XP_MULTIPLIER.get();
        double power = EnchantmentConfig.UNSTABLE_POWER_MULTIPLIER.get();
        try {
            EnchantmentConfig.LAST_STAND_XP_MULTIPLIER.set(1.0);
            helper.assertTrue(EnchantmentConfig.experienceCost(-2, 2) == 75, "Legacy XP-point formula changed");
            EnchantmentConfig.LAST_STAND_XP_MULTIPLIER.set(2.0);
            helper.assertTrue(EnchantmentConfig.experienceCost(-2, 2) == 150, "XP multiplier not applied");
            EnchantmentConfig.UNSTABLE_POWER_MULTIPLIER.set(.25);
            helper.assertTrue(EnchantmentConfig.explosionPower(1) == .5F, "Minimum explosion strength incorrect");
            EnchantmentConfig.UNSTABLE_POWER_MULTIPLIER.set(2.0);
            helper.assertTrue(EnchantmentConfig.explosionPower(1000) == 8, "Overleveled enchantments must respect power cap");
        } finally {
            EnchantmentConfig.LAST_STAND_XP_MULTIPLIER.set(xp);
            EnchantmentConfig.UNSTABLE_POWER_MULTIPLIER.set(power);
        }
        var target = player(helper, 0, false);
        var apply = CommonEvents.class.getDeclaredMethod("applyFlimFlam", ServerPlayer.class);
        apply.setAccessible(true);
        try {
            EnchantmentConfig.FLIM_NAUSEA.set(true);
            apply.invoke(null, target);
            helper.assertTrue(target.hasEffect(net.minecraft.world.effect.MobEffects.CONFUSION)
                    && !target.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN),
                    "Nausea alone must not also apply disabled slowness");
            target.removeAllEffects();
            EnchantmentConfig.FLIM_NAUSEA.set(false);
            EnchantmentConfig.FLIM_SLOWNESS.set(true);
            apply.invoke(null, target);
            helper.assertTrue(!target.hasEffect(net.minecraft.world.effect.MobEffects.CONFUSION)
                    && target.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN),
                    "Slowness alone must not also apply disabled nausea");
        } finally { EnchantmentConfig.FLIM_NAUSEA.set(false); EnchantmentConfig.FLIM_SLOWNESS.set(false); }
        pass(helper, "enchantmentBoundsAndIndependentEffects");
    }
    @GameTest(template = "empty", timeoutTicks = 30)
    public static void totemFirstThenXpAndNoCooldownByDefault(GameTestHelper helper) {
        var player = player(helper, 10000, true);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
        player.setHealth(2);
        hit(player, 30);
        helper.assertTrue(player.isAlive() && player.getOffhandItem().isEmpty() && player.totalExperience == 10000
                && player.getPersistentData().getInt(LastStandHandler.COOLDOWN) == 0,
                "Vanilla totem must save first without charging XP or starting Last Stand cooldown");
        player.removeAllEffects();
        player.setAbsorptionAmount(0);
        hit(player, 30);
        helper.assertTrue(player.isAlive() && player.getHealth() == 1 && player.totalExperience < 10000,
                "Without a totem, actual death must fall back to XP rescue");
        int before = player.totalExperience;
        hit(player, 30);
        helper.assertTrue(player.isAlive() && player.totalExperience < before,
                "Default zero cooldown must allow a second funded rescue");
        pass(helper, "enchantmentTotemPriority");
    }
    @GameTest(template = "empty", timeoutTicks = 30)
    public static void insufficientXpDisabledAndCooldownNeverRescue(GameTestHelper helper) {
        var poor = player(helper, 1, true);
        hit(poor, 30);
        helper.assertTrue(!poor.isAlive() && poor.totalExperience == 1,
                "Insufficient XP must not partially charge or rescue");
        boolean enabled = EnchantmentConfig.LAST_STAND_ENABLED.get();
        int seconds = EnchantmentConfig.LAST_STAND_COOLDOWN_SECONDS.get();
        try {
            EnchantmentConfig.LAST_STAND_ENABLED.set(false);
            var disabled = player(helper, 10000, true);
            hit(disabled, 30);
            helper.assertTrue(!disabled.isAlive() && disabled.totalExperience == 10000, "Disabled Last Stand must do nothing");
            EnchantmentConfig.LAST_STAND_ENABLED.set(true);
            EnchantmentConfig.LAST_STAND_COOLDOWN_SECONDS.set(2);
            var cooling = player(helper, 10000, true);
            hit(cooling, 30);
            int before = cooling.totalExperience;
            helper.assertTrue(cooling.isAlive() && cooling.getPersistentData().getInt(LastStandHandler.COOLDOWN) == 40,
                    "Successful rescue must start configured cooldown");
            for (int i = 0; i < 40; i++) LastStandHandler.tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(cooling));
            hit(cooling, 30);
            helper.assertTrue(cooling.isAlive() && cooling.totalExperience < before, "Expired cooldown must permit rescue");
            before = cooling.totalExperience;
            hit(cooling, 30);
            helper.assertTrue(!cooling.isAlive() && cooling.totalExperience == before, "Active cooldown must prevent rescue and charging");
        } finally {
            EnchantmentConfig.LAST_STAND_ENABLED.set(enabled);
            EnchantmentConfig.LAST_STAND_COOLDOWN_SECONDS.set(seconds);
        }
        pass(helper, "enchantmentFundsSwitchAndCooldown");
    }
    @GameTest(template = "empty", timeoutTicks = 30)
    public static void absorptionNonlethalVoidAndKill(GameTestHelper helper) {
        var safe = player(helper, 10000, true);
        safe.setHealth(2);
        safe.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.ABSORPTION, 200, 4));
        hit(safe, 10);
        helper.assertTrue(safe.isAlive() && safe.getHealth() == 2 && safe.totalExperience == 10000,
                "Absorption must be processed before deciding whether rescue is necessary");
        safe.removeAllEffects(); safe.setAbsorptionAmount(0); safe.setHealth(2);
        hit(safe, 1.5F);
        helper.assertTrue(safe.getHealth() > 0 && safe.getHealth() < 1 && safe.totalExperience == 10000,
                "A surviving player below half a heart must not trigger Last Stand");
        var voidPlayer = player(helper, 10000, true);
        voidPlayer.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
        voidPlayer.hurt(voidPlayer.damageSources().fellOutOfWorld(), 30);
        helper.assertTrue(voidPlayer.isAlive() && voidPlayer.totalExperience < 10000
                && voidPlayer.getOffhandItem().is(Items.TOTEM_OF_UNDYING), "Void rescue must be allowed even when vanilla totem refuses");
        var killPlayer = player(helper, Integer.MAX_VALUE, true);
        killPlayer.kill();
        helper.assertTrue(killPlayer.isAlive() && killPlayer.getHealth() == 1
                && killPlayer.totalExperience < Integer.MAX_VALUE, "Actual kill() must be eligible when fully funded");
        pass(helper, "enchantmentAbsorptionVoidAndKill");
    }
    @GameTest(template = "empty", timeoutTicks = 30)
    public static void treasureClassificationFromLoadedConfig(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        var keys = java.util.List.of(ModEnchantments.LAST_STAND, ModEnchantments.UNSTABLE, ModEnchantments.FLIM_FLAM);
        var flags = java.util.List.of(EnchantmentConfig.LAST_STAND_TREASURE, EnchantmentConfig.UNSTABLE_TREASURE, EnchantmentConfig.FLIM_FLAM_TREASURE);
        for (int i = 0; i < keys.size(); i++) {
            var holder = registry.getHolderOrThrow(keys.get(i));
            boolean treasure = flags.get(i).get();
            helper.assertTrue(holder.is(EnchantmentTags.TREASURE) == treasure
                    && holder.is(EnchantmentTags.NON_TREASURE) != treasure
                    && holder.is(EnchantmentTags.IN_ENCHANTING_TABLE) != treasure,
                    "Loaded treasure/non-treasure/table tags must match config for " + keys.get(i));
            helper.assertTrue(holder.is(EnchantmentTags.TRADEABLE) && holder.is(EnchantmentTags.ON_RANDOM_LOOT),
                    "Treasure setting must preserve trades and random loot");
        }
        pass(helper, "enchantmentAcquisitionTags");
    }
    @GameTest(template = "empty", timeoutTicks = 30)
    public static void unstableTerrainAndDefaultPranksOff(GameTestHelper helper) {
        var owner = player(helper, 0, false);
        var boots = new ItemStack(Items.LEATHER_BOOTS);
        boots.enchant(ModEnchantments.holder(owner, ModEnchantments.UNSTABLE), 3);
        owner.setItemSlot(EquipmentSlot.FEET, boots);
        owner.getInventory().setItem(0, new ItemStack(Items.GUNPOWDER, 20));
        var events = new CommonEvents();
        boolean enabled = EnchantmentConfig.UNSTABLE_ENABLED.get();
        try {
            EnchantmentConfig.UNSTABLE_ENABLED.set(false);
            var fall = new LivingFallEvent(owner, 10, 1);
            events.onUnstableFall(fall);
            helper.assertTrue(!fall.isCanceled() && owner.getInventory().getItem(0).getCount() == 20,
                    "Disabled Unstable must neither consume fuel nor cancel falling");
            EnchantmentConfig.UNSTABLE_ENABLED.set(true);
            BlockPos glass = owner.blockPosition().below();
            helper.getLevel().setBlockAndUpdate(glass, Blocks.GLASS.defaultBlockState());
            fall = new LivingFallEvent(owner, 10, 1);
            events.onUnstableFall(fall);
            helper.assertTrue(fall.isCanceled() && helper.getLevel().getBlockState(glass).is(Blocks.GLASS)
                    && owner.getInventory().getItem(0).getCount() == 16, "Default Unstable must explode without terrain destruction");
        } finally { EnchantmentConfig.UNSTABLE_ENABLED.set(enabled); }
        var target = player(helper, 0, false);
        var sword = new ItemStack(Items.IRON_SWORD);
        sword.enchant(ModEnchantments.holder(owner, ModEnchantments.FLIM_FLAM), 4);
        owner.setItemInHand(InteractionHand.MAIN_HAND, sword);
        var attack = new LivingDamageEvent.Post(target, new DamageContainer(target.damageSources().playerAttack(owner), 1));
        events.onFlimFlamAttack(attack);
        helper.assertTrue(EnchantmentConfig.FLIM_FLAM_ENABLED.get() && !EnchantmentConfig.anyPrankEnabled()
                && target.getPersistentData().getInt("openblocks_reborn.flim_flam_luck") == 0,
                "Master switch on with all pranks off must not accumulate bad luck");
        EnchantmentConfig.FLIM_NAUSEA.set(true);
        try {
            events.onFlimFlamAttack(attack);
            helper.assertTrue(target.getPersistentData().getInt("openblocks_reborn.flim_flam_luck") < 0,
                    "Enabling one prank must allow bad luck accumulation");
            EnchantmentConfig.FLIM_FLAM_ENABLED.set(false);
            int before = target.getPersistentData().getInt("openblocks_reborn.flim_flam_luck");
            events.onFlimFlamAttack(attack);
            helper.assertTrue(target.getPersistentData().getInt("openblocks_reborn.flim_flam_luck") == before,
                    "Disabling master switch must override enabled individual pranks");
        } finally { EnchantmentConfig.FLIM_NAUSEA.set(false); EnchantmentConfig.FLIM_FLAM_ENABLED.set(true); }
        pass(helper, "enchantmentExplosionAndPrankSwitches");
    }
}
