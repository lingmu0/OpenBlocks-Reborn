package net.xuwu.openblocks_acceptance;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.xuwu.openblocks_reborn.OpenBlocksReborn;
import net.xuwu.openblocks_reborn.compat.PatchouliCompat;
import net.xuwu.openblocks_reborn.elevator.ElevatorBlockBase;
import net.xuwu.openblocks_reborn.elevator.neoforge.init.Registry;
import net.xuwu.openblocks_reborn.registry.ModItems;
import vazkii.patchouli.api.PatchouliAPI;

/** Separate opt-in development mod; never packaged in the release JAR. */
@Mod(value = ReduxClientAcceptance.ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = ReduxClientAcceptance.ID, value = Dist.CLIENT)
public final class ReduxClientAcceptance {
    public static final String ID = "openblocks_redux_acceptance";
    private static int ticks;
    private static boolean failed;
    private static final BlockPos ELEVATOR = new BlockPos(0, 100, 0);

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) return;
        if (mc.isPaused()) return;
        ticks++;
        try {
            if (ticks == 10) mc.getSingleplayerServer().execute(() -> {
                var level = mc.getSingleplayerServer().overworld();
                for (int x = -5; x <= 5; x++) for (int z = -3; z <= 8; z++)
                    level.setBlock(new BlockPos(x, 99, z), Blocks.STONE.defaultBlockState(), 3);
                var block = Registry.ELEVATOR_BLOCKS.get(DyeColor.WHITE).get();
                level.setBlock(ELEVATOR, block.defaultBlockState()
                        .setValue(ElevatorBlockBase.DIRECTIONAL, true)
                        .setValue(ElevatorBlockBase.FACING, Direction.NORTH), 3);
                level.setBlock(ELEVATOR.above(6), Registry.ELEVATOR_BLOCKS.get(DyeColor.RED).get().defaultBlockState(), 3);
                level.setBlock(ELEVATOR.east(2), Registry.ELEVATOR_BLOCKS.get(DyeColor.BLUE).get().defaultBlockState(), 3);
                var camo = (net.xuwu.openblocks_reborn.elevator.neoforge.tile.ElevatorBlockEntity)level.getBlockEntity(ELEVATOR.east(2));
                camo.setCamoAndUpdate(Blocks.GLOWSTONE.defaultBlockState());
                var player = mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                player.setGameMode(GameType.CREATIVE);
                player.teleportTo(.5, 101, 5.5);
                player.setYRot(180);
                player.setXRot(5);
                player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.SONIC_GLASSES.get()));
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(ModItems.INFO_BOOK.get()));
                level.setDayTime(6000);
                level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false, mc.getSingleplayerServer());
            });
            if (ticks == 45) {
                mc.player.setYRot(180);
                mc.player.setXRot(5);
                mc.getSoundManager().play(new SimpleSoundInstance(SoundEvents.WARDEN_AMBIENT,
                        SoundSource.HOSTILE, 1F, 1F, RandomSource.create(), new BlockPos(0, 101, 0)));
                mc.getSoundManager().play(new SimpleSoundInstance(SoundEvents.NOTE_BLOCK_HARP.value(),
                        SoundSource.BLOCKS, 1F, 1F, RandomSource.create(), new BlockPos(2, 101, 0)));
            }
            if (ticks == 50) {
                var type = Class.forName("net.xuwu.openblocks_reborn.client.SonicGlassesClient");
                var field = type.getDeclaredField("ECHOES");
                field.setAccessible(true);
                int count = ((java.util.List<?>)field.get(null)).size();
                require(count >= 2, "Sonic Glasses did not capture positional sounds: " + count);
                OpenBlocksReborn.LOGGER.info("REDUX_CLIENT_PASS positional sound markers={}", count);
                capture(mc, "redux-sonic.png");
            }
            if (ticks == 65) PatchouliAPI.get().openBookGUI(PatchouliCompat.OPENBLOCKS_GUIDE);
            if (ticks == 80) {
                require(PatchouliCompat.OPENBLOCKS_GUIDE.equals(PatchouliAPI.get().getOpenBookGui()), "Guide did not open");
                require(mc.screen != null && mc.screen.getClass().getName().contains("patchouli"), "Wrong guide screen");
                capture(mc, "redux-guide-sections.png");
                OpenBlocksReborn.LOGGER.info("REDUX_CLIENT_PASS legacy guide sections");
            }
            if (ticks == 90) PatchouliAPI.get().openBookEntry(PatchouliCompat.OPENBLOCKS_GUIDE,
                    net.minecraft.resources.ResourceLocation.parse("openblocks_reborn:items/sonic_glasses"), 0);
            if (ticks == 110) {
                capture(mc, "redux-guide-sonic.png");
                mc.setScreen(null);
            }
            if (ticks == 125) mc.getSingleplayerServer().execute(() -> {
                var player = mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                var tile = (net.xuwu.openblocks_reborn.elevator.neoforge.tile.ElevatorBlockEntity)
                        player.serverLevel().getBlockEntity(ELEVATOR);
                player.openMenu(tile, ELEVATOR);
            });
            if (ticks == 150) {
                require(mc.screen instanceof net.xuwu.openblocks_reborn.elevator.client.gui.ElevatorScreen,
                        "Integrated elevator screen did not open");
                capture(mc, "redux-elevator-options.png");
                OpenBlocksReborn.LOGGER.info("REDUX_CLIENT_PASS elevator settings and checkbox accessor");
            }
            if (ticks == 160) {
                mc.setScreen(null);
                mc.getSingleplayerServer().execute(() -> {
                    var player = mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                    player.closeContainer();
                    player.setGameMode(GameType.SURVIVAL);
                    player.experienceLevel = 0;
                    player.experienceProgress = 0;
                    player.totalExperience = 0;
                    player.giveExperiencePoints(10);
                    net.xuwu.openblocks_reborn.elevator.Config.GENERAL.useXP.set(true);
                    net.xuwu.openblocks_reborn.elevator.Config.GENERAL.XPPointsAmount.set(1);
                    player.teleportTo(.5, 101, .5);
                });
            }
            if (ticks == 175) net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                    new net.xuwu.openblocks_reborn.elevator.network.TeleportPacket(ELEVATOR, ELEVATOR.above(6)));
            if (ticks == 200) {
                require(Math.abs(mc.player.getY() - 107) < .1, "Real player elevator transport failed: y=" + mc.player.getY());
                require(mc.player.totalExperience == 9, "Survival elevator XP cost failed: " + mc.player.totalExperience);
                OpenBlocksReborn.LOGGER.info("REDUX_CLIENT_PASS real player network elevator transport y={}", mc.player.getY());
                OpenBlocksReborn.LOGGER.info("REDUX_CLIENT_PASS survival elevator XP cost remaining={}", mc.player.totalExperience);
            }
            if (ticks == 215) {
                OpenBlocksReborn.LOGGER.info("REDUX_CLIENT_COMPLETE failed={}", failed);
                mc.stop();
            }
        } catch (Throwable exception) {
            failed = true;
            OpenBlocksReborn.LOGGER.error("REDUX_CLIENT_FAILURE", exception);
            mc.stop();
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static void capture(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(),
                component -> OpenBlocksReborn.LOGGER.info("REDUX_CAPTURE {}", name));
    }
}
