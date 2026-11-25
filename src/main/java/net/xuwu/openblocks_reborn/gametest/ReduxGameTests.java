package net.xuwu.openblocks_reborn.gametest;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.xuwu.openblocks_reborn.OpenBlocksReborn;
import net.xuwu.openblocks_reborn.compat.PatchouliCompat;
import net.xuwu.openblocks_reborn.registry.ModItems;
import vazkii.patchouli.api.PatchouliAPI;

import java.util.List;

@GameTestHolder(OpenBlocksReborn.MOD_ID)
@PrefixGameTestTemplate(true)
public final class ReduxGameTests {
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void patchouliGuideRegistered(GameTestHelper helper) {
        // The public API getBookStack always returns patchouli:guide_book, even for
        // custom books. Check the loaded book definition rather than that factory.
        try {
            var type = Class.forName("vazkii.patchouli.common.book.BookRegistry");
            var registry = type.getField("INSTANCE").get(null);
            var books = (java.util.Map<?, ?>)type.getField("books").get(registry);
            var definition = books.get(PatchouliCompat.OPENBLOCKS_GUIDE);
            if (definition == null || !((ItemStack)definition.getClass().getMethod("getBookItem")
                    .invoke(definition)).is(ModItems.INFO_BOOK.get())) {
                helper.fail("Patchouli did not load the custom OpenBlocks guide definition");
                return;
            }
        } catch (ReflectiveOperationException exception) {
            helper.fail("Could not inspect the loaded Patchouli guide: " + exception);
            return;
        }
        helper.succeed();
        OpenBlocksReborn.LOGGER.info("REDUX_TEST_PASS patchouliGuideRegistered");
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void integratedElevatorRegistryAndRecipes(GameTestHelper helper) {
        if (net.neoforged.fml.ModList.get().isLoaded("elevatorid")) {
            helper.fail("Standalone ElevatorMod was unexpectedly loaded in the integration test");
            return;
        }
        for (String legacy : List.of("elevator", "elevator_rotating")) {
            if (BuiltInRegistries.BLOCK.containsKey(ResourceLocation.fromNamespaceAndPath(OpenBlocksReborn.MOD_ID, legacy))) {
                helper.fail("Retired legacy elevator is still registered: " + legacy);
                return;
            }
        }
        var white = net.xuwu.openblocks_reborn.elevator.neoforge.init.Registry.ELEVATOR_ITEMS
                .get(net.minecraft.world.item.DyeColor.WHITE).get();
        var red = net.xuwu.openblocks_reborn.elevator.neoforge.init.Registry.ELEVATOR_ITEMS
                .get(net.minecraft.world.item.DyeColor.RED).get();
        if (net.xuwu.openblocks_reborn.elevator.neoforge.init.Registry.ELEVATOR_BLOCKS.size() != 16) {
            helper.fail("Expected all sixteen integrated elevator colors");
            return;
        }
        var crafting = CraftingInput.of(2, 1, List.of(new ItemStack(white), new ItemStack(net.minecraft.world.item.Items.RED_DYE)));
        var recipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, crafting, helper.getLevel());
        if (recipe.isEmpty() || !recipe.get().value().assemble(crafting, helper.getLevel().registryAccess()).is(red)) {
            helper.fail("Integrated elevator re-dye recipe failed");
            return;
        }
        helper.succeed();
        OpenBlocksReborn.LOGGER.info("REDUX_TEST_PASS integratedElevatorRegistryAndRecipes");
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void camouflageAndDirectionSurviveNbtRoundTrip(GameTestHelper helper) {
        var relative = new net.minecraft.core.BlockPos(1, 1, 1);
        var elevator = net.xuwu.openblocks_reborn.elevator.neoforge.init.Registry.ELEVATOR_BLOCKS
                .get(net.minecraft.world.item.DyeColor.BLUE).get();
        var state = elevator.defaultBlockState()
                .setValue(net.xuwu.openblocks_reborn.elevator.ElevatorBlockBase.DIRECTIONAL, true)
                .setValue(net.xuwu.openblocks_reborn.elevator.ElevatorBlockBase.FACING, net.minecraft.core.Direction.WEST)
                .setValue(net.xuwu.openblocks_reborn.elevator.ElevatorBlockBase.SHOW_ARROW, false);
        helper.setBlock(relative, state);
        var entity = (net.xuwu.openblocks_reborn.elevator.neoforge.tile.ElevatorBlockEntity)helper.getBlockEntity(relative);
        var camouflage = net.minecraft.world.level.block.Blocks.OAK_STAIRS.defaultBlockState()
                .setValue(net.minecraft.world.level.block.StairBlock.FACING, net.minecraft.core.Direction.SOUTH);
        if (!entity.setCamoAndUpdate(camouflage)) {
            helper.fail("Integrated elevator rejected ordinary stair camouflage");
            return;
        }
        var saved = entity.saveCustomOnly(helper.getLevel().registryAccess());
        entity.setCamoAndUpdate(null);
        entity.loadAdditional(saved, helper.getLevel().registryAccess());
        if (!camouflage.equals(entity.getHeldState()) || !state.equals(entity.getBlockState())) {
            helper.fail("Camouflage or direction/arrow settings were lost while reloading");
            return;
        }
        helper.succeed();
        OpenBlocksReborn.LOGGER.info("REDUX_TEST_PASS camouflageAndDirectionSurviveNbtRoundTrip");
    }

    @GameTest(template = "empty", timeoutTicks = 30)
    public static void sonicGlassesDoNotRevealSilentMobsToOtherPlayers(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack glasses = new ItemStack(ModItems.SONIC_GLASSES.get());
        player.setItemSlot(EquipmentSlot.HEAD, glasses);
        var mob = helper.spawn(EntityType.COW, 1, 1, 1);
        mob.setSilent(true);
        player.tickCount = 10;
        glasses.getItem().inventoryTick(glasses, helper.getLevel(), player, 0, false);
        if (mob.hasEffect(MobEffects.GLOWING)) {
            helper.fail("Sonic Glasses applied a shared glowing effect to a silent mob");
            return;
        }
        helper.succeed();
        OpenBlocksReborn.LOGGER.info("REDUX_TEST_PASS sonicGlassesDoNotRevealSilentMobsToOtherPlayers");
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void directionalTransportHonorsColorXpAndCamouflageHeight(GameTestHelper helper) {
        var config = net.xuwu.openblocks_reborn.elevator.Config.GENERAL;
        boolean sameColor = config.sameColor.get(), useXp = config.useXP.get();
        int xpCost = config.XPPointsAmount.get();
        boolean precision = config.precisionTarget.get(), resetPitch = config.resetPitchDirectional.get();
        try {
            config.sameColor.set(true);
            config.useXP.set(true);
            config.XPPointsAmount.set(1);
            config.precisionTarget.set(true);
            config.resetPitchDirectional.set(true);
            var from = new net.minecraft.core.BlockPos(2, 1, 2);
            var to = from.above(4);
            var white = net.xuwu.openblocks_reborn.elevator.neoforge.init.Registry.ELEVATOR_BLOCKS
                    .get(net.minecraft.world.item.DyeColor.WHITE).get();
            var blue = net.xuwu.openblocks_reborn.elevator.neoforge.init.Registry.ELEVATOR_BLOCKS
                    .get(net.minecraft.world.item.DyeColor.BLUE).get();
            helper.setBlock(from, white);
            helper.setBlock(to, blue.defaultBlockState()
                    .setValue(net.xuwu.openblocks_reborn.elevator.ElevatorBlockBase.DIRECTIONAL, true)
                    .setValue(net.xuwu.openblocks_reborn.elevator.ElevatorBlockBase.FACING, net.minecraft.core.Direction.EAST));
            var target = (net.xuwu.openblocks_reborn.elevator.neoforge.tile.ElevatorBlockEntity)helper.getBlockEntity(to);
            target.setCamoAndUpdate(net.minecraft.world.level.block.Blocks.OAK_SLAB.defaultBlockState());
            // FakePlayerFactory uses a connection whose teleport method is a no-op.
            // Minecraft's test server player exercises real position updates.
            var player = helper.makeMockServerPlayerInLevel();
            player.setGameMode(GameType.SURVIVAL);
            player.experienceLevel = 0;
            player.experienceProgress = 0;
            player.totalExperience = 0;
            player.giveExperiencePoints(10);
            var origin = helper.absolutePos(from);
            player.setPos(origin.getX() + .3, origin.getY() + 1, origin.getZ() + .3);
            double startY = player.getY();
            var packet = new net.xuwu.openblocks_reborn.elevator.network.TeleportPacket(origin, helper.absolutePos(to));
            var sound = net.xuwu.openblocks_reborn.elevator.neoforge.init.Registry.TELEPORT_SOUND.get();
            net.xuwu.openblocks_reborn.elevator.network.TeleportPacket.handle(packet, player, sound);
            if (player.getY() != startY || player.totalExperience != 10) {
                helper.fail("Same-color restriction failed or charged XP for a rejected trip");
                return;
            }
            config.sameColor.set(false);
            net.xuwu.openblocks_reborn.elevator.network.TeleportPacket.handle(packet, player, sound);
            if (Math.abs(player.getY() - (helper.absolutePos(to).getY() + .5)) > .01
                    || Math.abs(net.minecraft.util.Mth.wrapDegrees(player.getYRot() - net.minecraft.core.Direction.EAST.toYRot())) > .01
                    // Minecraft's mock server player reports creative regardless
                    // of its game-mode field. Creative trips correctly cost no XP;
                    // the real-client acceptance separately tests survival XP cost.
                    || player.totalExperience != (player.isCreative() ? 10 : 9) || player.getXRot() != 0) {
                helper.fail("Integrated transport result: y=" + player.getY()
                        + " expectedY=" + (helper.absolutePos(to).getY() + .5)
                        + " yaw=" + player.getYRot() + " pitch=" + player.getXRot()
                        + " xp=" + player.totalExperience);
                return;
            }
            helper.succeed();
            OpenBlocksReborn.LOGGER.info("REDUX_TEST_PASS directionalTransportHonorsColorXpAndCamouflageHeight");
        } finally {
            config.sameColor.set(sameColor);
            config.useXP.set(useXp);
            config.XPPointsAmount.set(xpCost);
            config.precisionTarget.set(precision);
            config.resetPitchDirectional.set(resetPitch);
        }
    }
}
