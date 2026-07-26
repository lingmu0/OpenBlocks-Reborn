package net.xuwu.openblocks_reborn.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.EnchantedBookItem;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.xuwu.openblocks_reborn.OpenBlocksReborn;
import net.xuwu.openblocks_reborn.blockentity.TankBlockEntity;
import net.xuwu.openblocks_reborn.blockentity.VacuumHopperBlockEntity;
import net.xuwu.openblocks_reborn.blockentity.GuideBlockEntity;
import net.xuwu.openblocks_reborn.item.DevNullItem;
import net.xuwu.openblocks_reborn.item.PaintBrushItem;
import net.xuwu.openblocks_reborn.item.StencilItem;
import net.xuwu.openblocks_reborn.registry.ModBlocks;
import net.xuwu.openblocks_reborn.registry.ModItems;
import net.xuwu.openblocks_reborn.registry.ModEntities;
import net.xuwu.openblocks_reborn.registry.ModFluids;
import net.xuwu.openblocks_reborn.registry.ModEnchantments;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.xuwu.openblocks_reborn.entity.LuggageEntity;
import net.xuwu.openblocks_reborn.entity.CraneCarriedBlockEntity;
import net.minecraft.world.item.DyeColor;
import net.xuwu.openblocks_reborn.blockentity.UtilityMachineBlockEntity;
import net.xuwu.openblocks_reborn.blockentity.ExperienceBlockEntity;
import net.xuwu.openblocks_reborn.blockentity.CanvasBlockEntity;
import net.xuwu.openblocks_reborn.blockentity.BigButtonBlockEntity;
import net.xuwu.openblocks_reborn.blockentity.GoldenEggBlockEntity;
import net.xuwu.openblocks_reborn.blockentity.TrophyBlockEntity;
import net.xuwu.openblocks_reborn.block.FlagBlock;
import net.xuwu.openblocks_reborn.block.BearTrapBlock;
import net.xuwu.openblocks_reborn.block.ItemMachineBlock;
import net.xuwu.openblocks_reborn.block.GuideBlock;
import net.xuwu.openblocks_reborn.block.VacuumHopperBlock;
import net.xuwu.openblocks_reborn.item.CursorItem;
import net.xuwu.openblocks_reborn.entity.MiniMeEntity;
import net.xuwu.openblocks_reborn.entity.GlyphEntity;
import net.xuwu.openblocks_reborn.item.GlyphItem;
import net.xuwu.openblocks_reborn.item.HeightMapItem;
import net.minecraft.world.entity.EntityType;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.GameType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import net.xuwu.openblocks_reborn.block.ElevatorBlock;
import net.xuwu.openblocks_reborn.block.TargetBlock;
import net.xuwu.openblocks_reborn.block.UtilityMachineBlock;
import net.xuwu.openblocks_reborn.item.CraneBackpackItem;
import net.xuwu.openblocks_reborn.item.GlassesItem;
import net.xuwu.openblocks_reborn.item.ImaginaryItem;
import net.xuwu.openblocks_reborn.menu.MachineMenu;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.shapes.CollisionContext;

@GameTestHolder(OpenBlocksReborn.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PortGameTests {
    @GameTest(template = "empty", timeoutTicks = 30)
    public static void jadedTrapdoorAndRopeLadderRestoreLegacyBehaviour(GameTestHelper helper) {
        var mockPlayer = helper.makeMockPlayer(GameType.SURVIVAL);

        BlockPos trapdoorPos = new BlockPos(0, 1, 0);
        BlockState closedTrapdoor = ModBlocks.LADDER.get().defaultBlockState()
                .setValue(TrapDoorBlock.OPEN, false);
        helper.setBlock(trapdoorPos, closedTrapdoor);
        BlockPos trapdoorAbsolute = helper.absolutePos(trapdoorPos);
        if (closedTrapdoor.isLadder(helper.getLevel(), trapdoorAbsolute, mockPlayer)) {
            helper.fail("Closed Jaded Trapdoor was incorrectly climbable");
            return;
        }
        BlockState openTrapdoor = closedTrapdoor.setValue(TrapDoorBlock.OPEN, true);
        helper.setBlock(trapdoorPos, openTrapdoor);
        if (!openTrapdoor.isLadder(helper.getLevel(), trapdoorAbsolute, mockPlayer)) {
            helper.fail("Open Jaded Trapdoor was not climbable");
            return;
        }

        BlockPos ropePos = new BlockPos(1, 2, 1);
        BlockPos supportPos = ropePos.south();
        BlockState rope = ModBlocks.ROPE_LADDER.get().defaultBlockState()
                .setValue(net.xuwu.openblocks_reborn.block.RopeLadderBlock.FACING, Direction.NORTH);
        helper.setBlock(supportPos, Blocks.STONE);
        helper.setBlock(ropePos, rope);
        ItemStack ropeStack = new ItemStack(ModItems.BLOCK_ITEMS.get("rope_ladder").get(), 3);
        ModBlocks.ROPE_LADDER.get().setPlacedBy(helper.getLevel(), helper.absolutePos(ropePos),
                rope, mockPlayer, ropeStack);

        if (ropeStack.getCount() != 1
                || !helper.getBlockState(ropePos.below()).is(ModBlocks.ROPE_LADDER.get())
                || !helper.getBlockState(ropePos.below(2)).is(ModBlocks.ROPE_LADDER.get())) {
            helper.fail("Rope Ladder did not extend downward while consuming the remaining stack: remaining="
                    + ropeStack.getCount() + ", top=" + helper.getBlockState(ropePos)
                    + ", below1=" + helper.getBlockState(ropePos.below())
                    + ", below2=" + helper.getBlockState(ropePos.below(2)));
            return;
        }

        helper.setBlock(supportPos, Blocks.AIR);
        helper.runAfterDelay(3, () -> {
            for (int distance = 0; distance < 3; distance++) {
                if (!helper.getBlockState(ropePos.below(distance)).isAir()) {
                    helper.fail("Unsupported Rope Ladder chain did not break from the top");
                    return;
                }
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void blockEntitiesAndVacuumCollection(GameTestHelper helper) {
        BlockPos tankPos = new BlockPos(1, 1, 1);
        BlockPos secondTankPos = new BlockPos(2, 1, 1);
        BlockPos hopperPos = new BlockPos(1, 1, 2);
        helper.setBlock(tankPos, ModBlocks.TANK.get());
        helper.setBlock(secondTankPos, ModBlocks.TANK.get());
        helper.setBlock(hopperPos, ModBlocks.VACUUM_HOPPER.get());

        if (!(helper.getBlockEntity(tankPos) instanceof TankBlockEntity tank) || tank.getTank().getCapacity() != 16_000) {
            helper.fail("Tank block entity or fluid capacity was not registered correctly");
            return;
        }
        tank.getTank().fill(new FluidStack(ModFluids.XP_JUICE.get(), 1000), IFluidHandler.FluidAction.EXECUTE);
        ItemStack packedTank = tank.toItemStack();
        var packedTankData = packedTank.getOrDefault(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        net.neoforged.neoforge.fluids.capability.templates.FluidTank restoredTank =
                new net.neoforged.neoforge.fluids.capability.templates.FluidTank(TankBlockEntity.CAPACITY);
        restoredTank.readFromNBT(helper.getLevel().registryAccess(), packedTankData.getCompound("Tank"));
        if (restoredTank.getFluidAmount() != 1000 || !restoredTank.getFluid().is(ModFluids.XP_JUICE.get())) {
            helper.fail("Tank item did not retain its fluid contents");
            return;
        }
        helper.spawnItem(Items.DIAMOND, 1.5F, 1.5F, 2.5F);
        BlockPos hopperAbsolute = helper.absolutePos(hopperPos);
        helper.getLevel().addFreshEntity(new ExperienceOrb(helper.getLevel(), hopperAbsolute.getX() + 0.5D,
                hopperAbsolute.getY() + 0.5D, hopperAbsolute.getZ() + 0.5D, 3));
        // Allow one complete vacuum-output interval plus the following tank-balancing pass.
        helper.runAfterDelay(30, () -> {
            if (!(helper.getBlockEntity(hopperPos) instanceof VacuumHopperBlockEntity hopper)) {
                helper.fail("Vacuum hopper block entity was not registered");
                return;
            }
            ItemStack collected = hopper.getInventory().getStackInSlot(0);
            if (!collected.is(Items.DIAMOND)) {
                helper.fail("Vacuum hopper did not collect the nearby item");
                return;
            }
            hopper.toggleItemOutput(Direction.EAST);
            BlockState itemOutputState = helper.getBlockState(hopperPos);
            if (itemOutputState.getValue(VacuumHopperBlock.EAST) != VacuumHopperBlock.OutputType.ITEMS) {
                helper.fail("Vacuum hopper item output was not reflected in its connector model state");
                return;
            }
            hopper.toggleExperienceOutput(Direction.EAST);
            BlockState combinedOutputState = helper.getBlockState(hopperPos);
            if (combinedOutputState.getValue(VacuumHopperBlock.EAST) != VacuumHopperBlock.OutputType.BOTH) {
                helper.fail("Vacuum hopper combined output was not reflected in its connector model state");
                return;
            }
            hopper.toggleItemOutput(Direction.EAST);
            if (helper.getBlockState(hopperPos).getValue(VacuumHopperBlock.EAST)
                    != VacuumHopperBlock.OutputType.FLUIDS) {
                helper.fail("Vacuum hopper fluid-only connector model state was incorrect");
                return;
            }
            hopper.toggleExperienceOutput(Direction.EAST);
            if (!(helper.getBlockEntity(secondTankPos) instanceof TankBlockEntity secondTank)) {
                helper.fail("Second connected tank was missing");
                return;
            }
            int totalFluid = tank.getTank().getFluidAmount() + secondTank.getTank().getFluidAmount()
                    + hopper.getExperienceTank().getFluidAmount();
            if (Math.abs(tank.getTank().getFluidAmount() - secondTank.getTank().getFluidAmount()) > 1
                    || totalFluid != 1060) {
                helper.fail("Connected tanks did not balance their fluid levels");
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void guideAndComponentItems(GameTestHelper helper) {
        BlockPos guidePos = new BlockPos(1, 1, 1);
        helper.setBlock(guidePos, ModBlocks.GUIDE.get());
        if (!(helper.getBlockEntity(guidePos) instanceof GuideBlockEntity guide)) {
            helper.fail("Building guide block entity was not registered");
            return;
        }
        if (guide.getExtent(Direction.EAST) != GuideBlockEntity.DEFAULT_EXTENT
                || guide.getShapePositions().isEmpty()
                || net.xuwu.openblocks_reborn.blockentity.GuideShape.values().length != 11) {
            helper.fail("Guide did not expose the original extents and eleven shape modes");
            return;
        }

        ItemStack devNullStack = new ItemStack(ModItems.DEV_NULL.get());
        ItemStack stored = new ItemStack(Items.COBBLESTONE, 1);
        DevNullItem.setStored(devNullStack, stored);
        ItemStack incoming = new ItemStack(Items.COBBLESTONE, 64);
        if (!ModItems.DEV_NULL.get().absorb(devNullStack, incoming)
                || !incoming.isEmpty()
                || DevNullItem.getStored(devNullStack).getCount() != 64) {
            helper.fail("dev/null did not absorb matching items and void overflow");
            return;
        }

        ItemStack brush = new ItemStack(ModItems.PAINTBRUSH.get());
        PaintBrushItem.setColor(brush, DyeColor.BLUE);
        if (PaintBrushItem.getColor(brush) != DyeColor.BLUE) {
            helper.fail("Paint brush color component was not saved");
            return;
        }
        ItemStack cursor = new ItemStack(ModItems.CURSOR.get());
        BlockPos cursorTarget = helper.absolutePos(guidePos);
        CursorItem.bind(cursor, helper.getLevel(), cursorTarget, Direction.UP);
        long linkedPosition = cursor.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getLong("CursorPosition");
        if (!BlockPos.of(linkedPosition).equals(cursorTarget)) {
            helper.fail("Cursor did not retain its linked block position");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void xpBottlerUsesLegacyProgressAndAutomaticSides(GameTestHelper helper) {
        BlockPos bottlerPos = new BlockPos(3, 1, 3);
        BlockPos inputChestPos = bottlerPos.north();
        BlockPos outputChestPos = bottlerPos.south();
        BlockPos tankPos = bottlerPos.west();
        helper.setBlock(bottlerPos, ModBlocks.XP_BOTTLER.get());
        helper.setBlock(inputChestPos, Blocks.CHEST);
        helper.setBlock(outputChestPos, Blocks.CHEST);
        helper.setBlock(tankPos, ModBlocks.TANK.get());
        if (!(helper.getBlockEntity(bottlerPos) instanceof ExperienceBlockEntity bottler)
                || !(helper.getBlockEntity(inputChestPos) instanceof
                net.minecraft.world.level.block.entity.ChestBlockEntity inputChest)
                || !(helper.getBlockEntity(outputChestPos) instanceof
                net.minecraft.world.level.block.entity.ChestBlockEntity outputChest)
                || !(helper.getBlockEntity(tankPos) instanceof TankBlockEntity sourceTank)) {
            helper.fail("XP Bottler automatic-transfer fixtures were missing");
            return;
        }
        inputChest.setItem(0, new ItemStack(Items.GLASS_BOTTLE));
        sourceTank.getTank().fill(new FluidStack(ModFluids.XP_JUICE.get(), 1_000),
                IFluidHandler.FluidAction.EXECUTE);
        bottler.toggleAutomatic(0);
        bottler.toggleAutomatic(1);
        bottler.toggleAutomatic(2);
        for (Direction direction : Direction.values()) {
            if (direction != Direction.NORTH) bottler.toggleSide(0, direction);
            if (direction != Direction.SOUTH) bottler.toggleSide(1, direction);
            if (direction != Direction.WEST) bottler.toggleSide(2, direction);
        }
        helper.runAfterDelay(20, () -> {
            if (bottler.getBottlingProgress() <= 0
                    || !bottler.getInventory().getStackInSlot(0).is(Items.GLASS_BOTTLE)) {
                helper.fail("XP Bottler did not enter its 40-tick processing cycle");
            }
        });
        helper.runAfterDelay(45, () -> {
            if (!outputChest.getItem(0).is(Items.EXPERIENCE_BOTTLE)
                    || !inputChest.getItem(0).isEmpty()
                    || sourceTank.getTank().getFluidAmount() >= 1_000) {
                helper.fail("XP Bottler did not automatically pull input/XP and push its output"
                        + " (input=" + inputChest.getItem(0)
                        + ", machineInput=" + bottler.getInventory().getStackInSlot(0)
                        + ", machineOutput=" + bottler.getInventory().getStackInSlot(1)
                        + ", output=" + outputChest.getItem(0)
                        + ", sourceFluid=" + sourceTank.getTank().getFluidAmount()
                        + ", buffer=" + bottler.getTank().getFluidAmount()
                        + ", progress=" + bottler.getBottlingProgress() + ")");
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 70)
    public static void luggageCollectsAndPersistsItems(GameTestHelper helper) {
        for (int x = 0; x <= 10; x++) helper.setBlock(new BlockPos(x, 0, 1), Blocks.STONE);
        LuggageEntity luggage = new LuggageEntity(ModEntities.LUGGAGE.get(), helper.getLevel());
        BlockPos absolute = helper.absolutePos(new BlockPos(1, 1, 1));
        luggage.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
        if (!helper.getLevel().addFreshEntity(luggage)) {
            helper.fail("Luggage entity was not registered");
            return;
        }
        helper.spawnItem(Items.IRON_INGOT, 1.5F, 1.2F, 1.5F);
        helper.runAfterDelay(12, () -> {
            if (luggage.storedItemCount() != 1) {
                helper.fail("Luggage did not collect the nearby item");
                return;
            }
            ItemStack packed = luggage.toItem();
            LuggageEntity restored = new LuggageEntity(ModEntities.LUGGAGE.get(), helper.getLevel());
            restored.restoreFromItem(packed);
            if (restored.storedItemCount() != 1) {
                helper.fail("Luggage inventory did not survive item conversion");
                return;
            }
            double startX = luggage.getX();
            BlockPos destination = helper.absolutePos(new BlockPos(8, 1, 1));
            if (!luggage.getNavigation().moveTo(destination.getX() + 0.5D, destination.getY(),
                    destination.getZ() + 0.5D, 1.15D)) {
                helper.fail("Luggage could not create a normal ground-navigation path");
                return;
            }
            helper.runAfterDelay(1, () -> {
                if (Math.abs(luggage.getX() - startX) > 1.0D) {
                    helper.fail("Luggage teleported instead of starting a smooth path");
                    return;
                }
                helper.runAfterDelay(30, () -> {
                    if (luggage.getX() <= startX + 0.5D) {
                        helper.fail("Luggage ground navigation did not move toward its target");
                        return;
                    }
                    helper.succeed();
                });
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 340)
    public static void utilityMachinesProcessTheirInventories(GameTestHelper helper) {
        BlockPos anvilPos = new BlockPos(1, 1, 1);
        BlockPos paintPos = new BlockPos(2, 1, 1);
        BlockPos enchantPos = new BlockPos(8, 1, 8);
        helper.setBlock(anvilPos, ModBlocks.AUTO_ANVIL.get());
        helper.setBlock(paintPos, ModBlocks.PAINT_MIXER.get());
        helper.setBlock(enchantPos, ModBlocks.AUTO_ENCHANTMENT_TABLE.get());
        surroundWithBookshelves(helper, enchantPos, 15);
        if (!(helper.getBlockEntity(anvilPos) instanceof UtilityMachineBlockEntity anvil)
                || !(helper.getBlockEntity(paintPos) instanceof UtilityMachineBlockEntity mixer)
                || !(helper.getBlockEntity(enchantPos) instanceof UtilityMachineBlockEntity enchanter)) {
            helper.fail("Utility machine block entities were not registered");
            return;
        }
        ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
        pickaxe.setDamageValue(200);
        anvil.getInventory().setStackInSlot(0, pickaxe);
        anvil.getInventory().setStackInSlot(1, new ItemStack(Items.IRON_PICKAXE));
        anvil.getExperienceTank().fill(new FluidStack(ModFluids.XP_JUICE.get(),
                        ModFluids.xpToFluid(ModFluids.experienceForLevel(10))),
                IFluidHandler.FluidAction.EXECUTE);
        enchanter.getInventory().setStackInSlot(0, new ItemStack(Items.DIAMOND_SWORD));
        enchanter.getInventory().setStackInSlot(1, new ItemStack(Items.LAPIS_LAZULI, 3));
        int initialEnchantFluid = ModFluids.xpToFluid(ModFluids.experienceForLevel(30));
        enchanter.getExperienceTank().fill(new FluidStack(ModFluids.XP_JUICE.get(), initialEnchantFluid),
                IFluidHandler.FluidAction.EXECUTE);
        ItemStack paintCan = new ItemStack(ModBlocks.PAINT_CAN.asItem());
        mixer.getInventory().setStackInSlot(0, paintCan);
        mixer.getInventory().setStackInSlot(2, new ItemStack(Items.CYAN_DYE));
        mixer.setSelectedPaintColor(0x00FFFF);
        if (!mixer.startPaintMix()) {
            helper.fail("Paint Mixer rejected a valid cyan recipe");
            return;
        }
        helper.runAfterDelay(310, () -> {
            ItemStack repaired = anvil.getInventory().getStackInSlot(2);
            if (!repaired.is(Items.IRON_PICKAXE) || repaired.getDamageValue() >= 200) {
                helper.fail("Auto Anvil did not repair its input");
                return;
            }
            if (PaintBrushItem.getRgbColor(mixer.getInventory().getStackInSlot(0)) != 0x00FFFF
                    || mixer.getPaintProgress() != 0) {
                helper.fail("Paint Mixer did not finish the selected RGB recipe");
                return;
            }
            if (!enchanter.getInventory().getStackInSlot(2).isEnchanted()
                    || enchanter.getExperienceTank().getFluidAmount() >= initialEnchantFluid) {
                helper.fail("Auto Enchantment Table did not consume XP Juice and enchant its input");
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 70)
    public static void automaticAnvilAndEnchantingUseVanillaRules(GameTestHelper helper) {
        BlockPos anvilPos = new BlockPos(1, 1, 1);
        BlockPos enchantPos = new BlockPos(8, 1, 8);
        helper.setBlock(anvilPos, ModBlocks.AUTO_ANVIL.get());
        helper.setBlock(enchantPos, ModBlocks.AUTO_ENCHANTMENT_TABLE.get());
        surroundWithBookshelves(helper, enchantPos, 15);
        if (!(helper.getBlockEntity(anvilPos) instanceof UtilityMachineBlockEntity anvil)
                || !(helper.getBlockEntity(enchantPos) instanceof UtilityMachineBlockEntity enchanter)) {
            helper.fail("Automatic anvil/enchantment block entities were missing");
            return;
        }
        if (anvil.getExperienceTank().getCapacity()
                != ModFluids.xpToFluid(ModFluids.experienceForLevel(UtilityMachineBlockEntity.AUTO_ANVIL_MAX_LEVEL))
                || enchanter.getExperienceTank().getCapacity()
                != ModFluids.xpToFluid(ModFluids.experienceForLevel(
                        UtilityMachineBlockEntity.DEFAULT_AUTO_ENCHANT_MAX_LEVEL))) {
            helper.fail("Automatic machine XP capacities do not match their vanilla-level limits");
            return;
        }
        enchanter.setEnchantPowerLimit(UtilityMachineBlockEntity.DEFAULT_AUTO_ENCHANT_MAX_LEVEL);

        var sharpness = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.SHARPNESS);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(sharpness, 1);
        ItemStack book = EnchantedBookItem.createForEnchantment(new EnchantmentInstance(sharpness, 1));
        anvil.getInventory().setStackInSlot(0, sword);
        anvil.getInventory().setStackInSlot(1, book);
        anvil.getExperienceTank().fill(new FluidStack(ModFluids.XP_JUICE.get(),
                        ModFluids.xpToFluid(ModFluids.experienceForLevel(10))),
                IFluidHandler.FluidAction.EXECUTE);

        enchanter.getInventory().setStackInSlot(0, new ItemStack(Items.BOOK));
        enchanter.getInventory().setStackInSlot(1, new ItemStack(Items.LAPIS_LAZULI, 3));
        enchanter.getExperienceTank().fill(new FluidStack(ModFluids.XP_JUICE.get(),
                        ModFluids.xpToFluid(ModFluids.experienceForLevel(30))),
                IFluidHandler.FluidAction.EXECUTE);

        helper.runAfterDelay(50, () -> {
            if (enchanter.getMaximumEnchantLevel()
                    != UtilityMachineBlockEntity.DEFAULT_AUTO_ENCHANT_MAX_LEVEL
                    || enchanter.getEnchantPowerLimit()
                    != UtilityMachineBlockEntity.DEFAULT_AUTO_ENCHANT_MAX_LEVEL) {
                helper.fail("Auto Enchantment Table did not derive or preserve its selectable level ceiling");
                return;
            }
            ItemStack combined = anvil.getInventory().getStackInSlot(2);
            if (!combined.is(Items.DIAMOND_SWORD)
                    || EnchantmentHelper.getItemEnchantmentLevel(sharpness, combined) != 2
                    || combined.getOrDefault(DataComponents.REPAIR_COST, 0) != 1
                    || !anvil.getInventory().getStackInSlot(0).isEmpty()
                    || !anvil.getInventory().getStackInSlot(1).isEmpty()) {
                helper.fail("Auto Anvil did not apply vanilla enchanted-book merging/prior-work cost");
                return;
            }
            ItemStack enchantedBook = enchanter.getInventory().getStackInSlot(2);
            if (!enchantedBook.is(Items.ENCHANTED_BOOK)
                    || !enchantedBook.has(DataComponents.STORED_ENCHANTMENTS)
                    || !enchanter.getInventory().getStackInSlot(0).isEmpty()
                    || !enchanter.getInventory().getStackInSlot(1).isEmpty()) {
                helper.fail("Auto Enchantment Table did not use vanilla book conversion and option costs");
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void automaticAnvilSupportsVanillaRenaming(GameTestHelper helper) {
        BlockPos anvilPos = new BlockPos(1, 1, 1);
        helper.setBlock(anvilPos, ModBlocks.AUTO_ANVIL.get());
        if (!(helper.getBlockEntity(anvilPos) instanceof UtilityMachineBlockEntity anvil)) {
            helper.fail("Auto Anvil block entity was missing");
            return;
        }
        anvil.getInventory().setStackInSlot(0, new ItemStack(Items.DIAMOND_SWORD));
        anvil.setAnvilItemName("OpenBlocks Blade");
        anvil.getExperienceTank().fill(new FluidStack(ModFluids.XP_JUICE.get(),
                        ModFluids.xpToFluid(ModFluids.experienceForLevel(1))),
                IFluidHandler.FluidAction.EXECUTE);
        helper.runAfterDelay(45, () -> {
            ItemStack renamed = anvil.getInventory().getStackInSlot(2);
            if (!renamed.is(Items.DIAMOND_SWORD)
                    || !renamed.has(DataComponents.CUSTOM_NAME)
                    || !"OpenBlocks Blade".equals(renamed.getHoverName().getString())
                    || renamed.getOrDefault(DataComponents.REPAIR_COST, 0) != 0) {
                helper.fail("Auto Anvil did not apply vanilla rename-only cost semantics");
                return;
            }
            helper.succeed();
        });
    }

    private static void surroundWithBookshelves(GameTestHelper helper, BlockPos tablePos, int count) {
        int placed = 0;
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            helper.setBlock(tablePos.offset(offset), Blocks.BOOKSHELF);
            if (++placed >= count) return;
        }
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void drawingTableSelectionAndPrintingWork(GameTestHelper helper) {
        BlockPos tablePos = new BlockPos(1, 1, 1);
        helper.setBlock(tablePos, ModBlocks.DRAWING_TABLE.get());
        if (!(helper.getBlockEntity(tablePos) instanceof UtilityMachineBlockEntity table)) {
            helper.fail("Drawing Table block entity was missing");
            return;
        }
        table.getInventory().setStackInSlot(0, new ItemStack(ModItems.UNPREPARED_STENCIL.get(), 2));
        ItemStack firstOutput = table.getInventory().getStackInSlot(1);
        if (!firstOutput.is(ModItems.STENCIL.get()) || StencilItem.getPattern(firstOutput) != 0
                || firstOutput.getCount() != 2) {
            helper.fail("Drawing Table did not preview the selected stencil");
            return;
        }
        table.changeDrawingSelection(1);
        if (StencilItem.getPattern(table.getInventory().getStackInSlot(1)) != 1) {
            helper.fail("Drawing Table stencil selector did not update its output");
            return;
        }
        ItemStack extracted = table.getInventory().extractItem(1, 1, false);
        if (!extracted.is(ModItems.STENCIL.get()) || table.getInventory().getStackInSlot(0).getCount() != 1) {
            helper.fail("Taking a stencil did not consume one blank stencil");
            return;
        }
        table.cycleDrawingMode();
        if (!table.getInventory().getStackInSlot(1).is(ModItems.GLYPH.get()) || !table.printGlyph('Z')) {
            helper.fail("Drawing Table did not enter glyph mode or print a glyph");
            return;
        }
        BlockPos absolute = helper.absolutePos(tablePos);
        boolean printed = !helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                new net.minecraft.world.phys.AABB(absolute).inflate(2.0D),
                entity -> entity.getItem().is(ModItems.GLYPH.get())
                        && GlyphItem.getCharacter(entity.getItem()) == 'Z').isEmpty();
        if (!printed || !table.getInventory().getStackInSlot(0).isEmpty()) {
            helper.fail("Drawing Table did not drop the printed glyph or consume its material");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void xpBottlerAndCanvasDataWork(GameTestHelper helper) {
        BlockPos bottlerPos = new BlockPos(1, 1, 1);
        BlockPos canvasPos = new BlockPos(2, 1, 1);
        helper.setBlock(bottlerPos, ModBlocks.XP_BOTTLER.get());
        helper.setBlock(canvasPos, ModBlocks.CANVAS.get());
        if (!(helper.getBlockEntity(bottlerPos) instanceof ExperienceBlockEntity bottler)
                || !(helper.getBlockEntity(canvasPos) instanceof CanvasBlockEntity canvas)) {
            helper.fail("XP Bottler or Canvas block entity was missing");
            return;
        }
        bottler.addExperience(16);
        bottler.getInventory().setStackInSlot(0, new ItemStack(Items.GLASS_BOTTLE, 2));
        canvas.setColor(Direction.NORTH, 0xFF3366CC);
        canvas.setPixel(Direction.NORTH, 3, 7, 0xFFFF3366);
        if (canvas.getColor(Direction.NORTH) != 0xFF3366CC
                || canvas.getPixel(Direction.NORTH, 3, 7) != 0xFFFF3366 || canvas.paintedFaces() != 1) {
            helper.fail("Canvas did not keep independent face colour data");
            return;
        }
        canvas.applyPaint(Direction.UP, 0xFF55AA33);
        if (canvas.getPixel(Direction.UP, 0, 0) != 0xFF55AA33
                || canvas.getPixel(Direction.UP, 15, 15) != 0xFF55AA33) {
            helper.fail("Paint Brush face operation did not fill the complete clicked face");
            return;
        }
        if (StencilItem.PATTERN_COUNT != 15 || !canvas.placeStencil(Direction.SOUTH, 0)) {
            helper.fail("Canvas did not accept the complete legacy stencil set");
            return;
        }
        canvas.applyPaint(Direction.SOUTH, 0xFFCC4422);
        if (canvas.getPixel(Direction.SOUTH, 2, 2) != 0xFFCC4422
                || canvas.getPixel(Direction.SOUTH, 0, 0) != -1
                || !canvas.rotateStencil(Direction.SOUTH)
                || canvas.getStencilRotation(Direction.SOUTH) != 1) {
            helper.fail("Placed stencil did not mask paint or rotate correctly");
            return;
        }
        CompoundTag canvasItemData = canvas.toItemStack().getOrDefault(DataComponents.BLOCK_ENTITY_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        int[] stencilPatterns = canvasItemData.getIntArray("StencilPatterns");
        int[] stencilRotations = canvasItemData.getIntArray("StencilRotations");
        if (stencilPatterns.length != 6 || stencilRotations.length != 6
                || stencilPatterns[Direction.SOUTH.get3DDataValue()] != 0
                || stencilRotations[Direction.SOUTH.get3DDataValue()] != 1
                || canvas.removeStencil(Direction.SOUTH) != 0) {
            helper.fail("Canvas item did not retain or return its placed stencil");
            return;
        }
        int[] packedColors = canvas.toItemStack().getOrDefault(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getIntArray("Colors");
        if (packedColors.length != 6 || packedColors[Direction.NORTH.get3DDataValue()] != 0xFF3366CC) {
            helper.fail("Canvas item did not retain its painted face colours");
            return;
        }
        int[] packedPixels = canvas.toItemStack().getOrDefault(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getIntArray("Pixels");
        int packedPixelIndex = Direction.NORTH.get3DDataValue() * CanvasBlockEntity.PIXELS_PER_FACE + 7 * 16 + 3;
        if (packedPixels.length != CanvasBlockEntity.TOTAL_PIXELS || packedPixels[packedPixelIndex] != 0xFFFF3366) {
            helper.fail("Canvas item did not retain its 16x16 pixel artwork");
            return;
        }
        helper.runAfterDelay(85, () -> {
            if (bottler.getExperience() != 0 || bottler.getInventory().getStackInSlot(1).getCount() != 2) {
                helper.fail("XP Bottler did not convert stored XP and glass bottles");
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 30)
    public static void configurableButtonAndHeightMapWork(GameTestHelper helper) {
        BlockPos buttonPos = new BlockPos(1, 1, 1);
        helper.setBlock(buttonPos, ModBlocks.BIG_BUTTON.get());
        if (!(helper.getBlockEntity(buttonPos) instanceof BigButtonBlockEntity button)) {
            helper.fail("Big Button inventory was missing");
            return;
        }
        button.getInventory().setStackInSlot(0, new ItemStack(Items.REDSTONE, 17));
        if (button.getPressTicks() != 17) {
            helper.fail("Big Button pulse duration did not follow inventory item count");
            return;
        }
        ItemStack map = HeightMapItem.create(helper.getLevel(), helper.absolutePos(buttonPos));
        if (HeightMapItem.getHeights(map).length != 81) {
            helper.fail("Cartographer height map did not store its 9x9 sample");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void goldenEggHatchesAndTrophyStoresType(GameTestHelper helper) {
        BlockPos eggPos = new BlockPos(1, 1, 1);
        BlockPos trophyPos = new BlockPos(2, 1, 1);
        helper.setBlock(eggPos, ModBlocks.GOLDEN_EGG.get());
        helper.setBlock(trophyPos, ModBlocks.TROPHY.get());
        if (!(helper.getBlockEntity(eggPos) instanceof GoldenEggBlockEntity egg)
                || !(helper.getBlockEntity(trophyPos) instanceof TrophyBlockEntity trophy)) {
            helper.fail("Golden Egg or Trophy block entity was missing");
            return;
        }
        trophy.setEntityType(EntityType.CREEPER);
        if (!trophy.getEntityTypeId().equals(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.CREEPER))) {
            helper.fail("Trophy did not store the selected entity type");
            return;
        }
        ItemStack packedTrophy = trophy.toItemStack();
        String packedType = packedTrophy.getOrDefault(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getString("EntityType");
        if (!packedType.equals("minecraft:creeper")) {
            helper.fail("Trophy item did not retain its displayed entity type");
            return;
        }
        egg.setProgress(GoldenEggBlockEntity.HATCH_TIME - 1);
        helper.runAfterDelay(3, () -> {
            if (helper.getBlockState(eggPos).is(ModBlocks.GOLDEN_EGG.get())) {
                helper.fail("Golden Egg did not hatch");
                return;
            }
            BlockPos absolute = helper.absolutePos(eggPos);
            if (helper.getLevel().getEntitiesOfClass(MiniMeEntity.class,
                    new net.minecraft.world.phys.AABB(absolute).inflate(2.0D)).isEmpty()) {
                helper.fail("Golden Egg did not create a Mini Me");
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void customFluidAndEnchantmentsLoad(GameTestHelper helper) {
        BlockPos fluidPos = new BlockPos(1, 1, 1);
        helper.setBlock(fluidPos, ModFluids.XP_JUICE_BLOCK.get());
        if (!helper.getBlockState(fluidPos).getFluidState().is(ModFluids.XP_JUICE.get())) {
            helper.fail("XP Juice source block did not expose the registered fluid");
            return;
        }
        var enchantment = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(ModEnchantments.LAST_STAND);
        ItemStack chestplate = new ItemStack(Items.DIAMOND_CHESTPLATE);
        chestplate.enchant(enchantment, 2);
        if (EnchantmentHelper.getItemEnchantmentLevel(enchantment, chestplate) != 2) {
            helper.fail("Data-driven OpenBlocks enchantments were not loaded");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void flagMountingAndSupportWork(GameTestHelper helper) {
        BlockPos floorFlag = new BlockPos(1, 1, 1);
        BlockPos wallFlag = new BlockPos(2, 1, 1);
        helper.setBlock(floorFlag.below(), Blocks.OAK_FENCE);
        helper.setBlock(floorFlag, ModBlocks.FLAG.get().defaultBlockState()
                .setValue(FlagBlock.FACE, AttachFace.FLOOR)
                .setValue(FlagBlock.FACING, Direction.EAST));
        helper.setBlock(wallFlag.south(), Blocks.STONE);
        helper.setBlock(wallFlag, ModBlocks.FLAG.get().defaultBlockState()
                .setValue(FlagBlock.FACE, AttachFace.WALL)
                .setValue(FlagBlock.FACING, Direction.NORTH));
        if (helper.getBlockState(floorFlag).getValue(FlagBlock.FACE) != AttachFace.FLOOR
                || helper.getBlockState(wallFlag).getValue(FlagBlock.FACING) != Direction.NORTH) {
            helper.fail("Flag floor/wall mounting state was not retained");
            return;
        }
        helper.setBlock(wallFlag.south(), Blocks.AIR);
        helper.runAfterDelay(2, () -> {
            if (!helper.getBlockState(wallFlag).isAir()) {
                helper.fail("Wall flag did not break when its supporting block was removed");
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 30)
    public static void bearTrapClosesAndHoldsMob(GameTestHelper helper) {
        BlockPos trapPos = new BlockPos(1, 1, 1);
        helper.setBlock(trapPos, ModBlocks.BEARTRAP.get());
        var zombie = EntityType.ZOMBIE.create(helper.getLevel());
        if (zombie == null) {
            helper.fail("Could not create bear-trap test mob");
            return;
        }
        BlockPos absolute = helper.absolutePos(trapPos);
        zombie.setPos(absolute.getX() + 0.5D, absolute.getY() + 0.1D, absolute.getZ() + 0.5D);
        helper.getLevel().addFreshEntity(zombie);
        helper.runAfterDelay(6, () -> {
            if (!helper.getBlockState(trapPos).getValue(BearTrapBlock.TRIGGERED)) {
                helper.fail("Bear Trap did not close when a mob entered it");
                return;
            }
            if (Math.abs(zombie.getX() - (absolute.getX() + 0.5D)) > 0.1D
                    || Math.abs(zombie.getZ() - (absolute.getZ() + 0.5D)) > 0.1D) {
                helper.fail("Bear Trap did not hold its trapped mob at the centre");
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 30)
    public static void blockPlacerKeepsOutputFacing(GameTestHelper helper) {
        BlockPos placerPos = new BlockPos(1, 1, 1);
        BlockState placerState = ModBlocks.BLOCK_PLACER.get().defaultBlockState()
                .setValue(ItemMachineBlock.FACING, Direction.EAST);
        helper.setBlock(placerPos, placerState);
        if (!(helper.getBlockEntity(placerPos) instanceof net.xuwu.openblocks_reborn.blockentity.ItemMachineBlockEntity placer)) {
            helper.fail("Block Placer inventory was missing");
            return;
        }
        placer.getInventory().setStackInSlot(0, new ItemStack(Items.FURNACE));
        helper.setBlock(placerPos.west(), Blocks.REDSTONE_BLOCK);
        helper.runAfterDelay(15, () -> {
            BlockState placed = helper.getBlockState(placerPos.east());
            if (!placed.is(Blocks.FURNACE)
                    || placed.getValue(net.minecraft.world.level.block.FurnaceBlock.FACING) != Direction.EAST) {
                helper.fail("Block Placer did not orient its placed block toward its output");
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 30)
    public static void itemDropperGuiSettingsControlVelocity(GameTestHelper helper) {
        BlockPos dropperPos = new BlockPos(1, 1, 1);
        helper.setBlock(dropperPos, ModBlocks.ITEM_DROPPER.get().defaultBlockState()
                .setValue(ItemMachineBlock.FACING, Direction.EAST));
        if (!(helper.getBlockEntity(dropperPos) instanceof net.xuwu.openblocks_reborn.blockentity.ItemMachineBlockEntity dropper)) {
            helper.fail("Item Dropper block entity was missing");
            return;
        }
        if (!ItemMachineBlock.handleDropperButton(dropper, 1_200)
                || dropper.getItemSpeed() != 200 || dropper.usesRedstoneStrength()
                || ItemMachineBlock.calculateDropperSpeed(200, false, 0) != 2.0D
                || ItemMachineBlock.calculateDropperSpeed(200, true, 15) != 2.0D
                || ItemMachineBlock.calculateDropperSpeed(200, true, 0) != 0.0D) {
            helper.fail("Item Dropper GUI settings were not applied");
            return;
        }
        dropper.getInventory().setStackInSlot(0, new ItemStack(Items.NETHER_STAR));
        helper.setBlock(dropperPos.west(), Blocks.REDSTONE_BLOCK);
        ItemMachineBlock.handleDropperButton(dropper, 2_000);
        CompoundTag saved = dropper.saveWithoutMetadata(helper.getLevel().registryAccess());
        if (saved.getInt("ItemSpeed") != 200 || !saved.getBoolean("UseRedstoneStrength")) {
            helper.fail("Item Dropper GUI settings were not saved");
            return;
        }
        helper.runAfterDelay(1, () -> {
            BlockPos absolute = helper.absolutePos(dropperPos);
            var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                    new net.minecraft.world.phys.AABB(absolute).inflate(6.0D),
                    entity -> entity.getItem().is(Items.NETHER_STAR));
            if (drops.isEmpty() || drops.getFirst().getDeltaMovement().x < 1.5D
                    && drops.getFirst().getX() <= absolute.getX() + 1.6D) {
                helper.fail("Configured Item Dropper speed did not affect its output; entities=" + drops.size()
                        + ", storedSpeed=" + dropper.getItemSpeed()
                        + ", powered=" + helper.getBlockState(dropperPos).getValue(ItemMachineBlock.POWERED)
                        + (drops.isEmpty() ? "" : ", position=" + drops.getFirst().position()
                                + ", velocity=" + drops.getFirst().getDeltaMovement()));
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void glyphAttachesAndKeepsCharacter(GameTestHelper helper) {
        BlockPos support = new BlockPos(2, 1, 2);
        BlockPos target = support.north();
        helper.setBlock(support, Blocks.STONE);
        GlyphEntity glyph = new GlyphEntity(helper.getLevel(), helper.absolutePos(target),
                Direction.NORTH, 'Ω', 8, 8);
        if (!glyph.survives() || !helper.getLevel().addFreshEntity(glyph)) {
            helper.fail("Glyph did not attach to a solid wall");
            return;
        }
        ItemStack picked = glyph.getPickResult();
        if (GlyphItem.getCharacter(picked) != 'Ω') {
            helper.fail("Glyph item did not retain its character");
            return;
        }
        CompoundTag saved = new CompoundTag();
        glyph.addAdditionalSaveData(saved);
        GlyphEntity restored = new GlyphEntity(ModEntities.GLYPH.get(), helper.getLevel());
        restored.setPos(glyph.getX(), glyph.getY(), glyph.getZ());
        restored.readAdditionalSaveData(saved);
        if (restored.getCharacter() != 'Ω' || restored.getDirection() != Direction.NORTH) {
            helper.fail("Glyph entity did not persist its character and facing");
            return;
        }
        glyph.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 30)
    public static void cannonTargetingLaunchesInSelectedDirection(GameTestHelper helper) {
        BlockPos cannonPos = new BlockPos(1, 1, 1);
        helper.setBlock(cannonPos, ModBlocks.CANNON.get().defaultBlockState()
                .setValue(ItemMachineBlock.FACING, Direction.NORTH));
        if (!(helper.getBlockEntity(cannonPos) instanceof net.xuwu.openblocks_reborn.blockentity.ItemMachineBlockEntity cannon)) {
            helper.fail("Item Cannon inventory was missing");
            return;
        }
        BlockPos absoluteTarget = helper.absolutePos(cannonPos.offset(8, 3, 0));
        cannon.setCannonTarget(absoluteTarget);
        if (!absoluteTarget.equals(cannon.getCannonTarget())
                || helper.getBlockState(cannonPos).getValue(ItemMachineBlock.FACING) != Direction.EAST) {
            helper.fail("Pointer target did not update the Item Cannon facing");
            return;
        }
        var plannedVelocity = ItemMachineBlock.calculateCannonVelocity(
                helper.absolutePos(cannonPos.east()).getCenter(), absoluteTarget.getCenter());
        if (plannedVelocity.x <= 0.2D || plannedVelocity.y <= 0.0D) {
            helper.fail("Item Cannon did not calculate an eastward lob toward its target");
            return;
        }
        ItemStack pointer = new ItemStack(ModItems.POINTER.get());
        net.xuwu.openblocks_reborn.item.PointerItem.bind(pointer, helper.getLevel().dimension().location(),
                helper.absolutePos(cannonPos));
        if (!BlockPos.of(pointer.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getLong("PointerCannon"))
                .equals(helper.absolutePos(cannonPos))) {
            helper.fail("Pointer did not retain its selected Item Cannon");
            return;
        }
        cannon.getInventory().setStackInSlot(0, new ItemStack(Items.NETHER_STAR));
        helper.setBlock(cannonPos.west(), Blocks.REDSTONE_BLOCK);
        helper.runAfterDelay(1, () -> {
            BlockPos absolute = helper.absolutePos(cannonPos);
            var projectiles = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                    new net.minecraft.world.phys.AABB(absolute).inflate(20.0D),
                    entity -> entity.getItem().is(Items.NETHER_STAR));
            var launched = projectiles.stream().filter(entity ->
                    entity.getX() > absolute.getX() + 1.0D
                            && entity.getDeltaMovement().y > 0.2D).findFirst();
            if (launched.isEmpty()) {
                helper.fail("Targeted Item Cannon did not launch toward its selected target; projectiles="
                        + projectiles.size() + ", inventory=" + cannon.getInventory().getStackInSlot(0).getCount()
                        + (projectiles.isEmpty() ? "" : ", velocities="
                        + projectiles.stream().map(ItemEntity::getDeltaMovement).toList()));
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 30)
    public static void xpBucketAndConfiguredMachineSidesWork(GameTestHelper helper) {
        BlockPos tankPos = new BlockPos(1, 1, 1);
        BlockPos anvilPos = new BlockPos(3, 1, 1);
        helper.setBlock(tankPos, ModBlocks.TANK.get());
        helper.setBlock(anvilPos, ModBlocks.AUTO_ANVIL.get());
        if (!(helper.getBlockEntity(tankPos) instanceof TankBlockEntity tank)
                || !(helper.getBlockEntity(anvilPos) instanceof UtilityMachineBlockEntity anvil)) {
            helper.fail("Tank or Auto Anvil block entity was missing");
            return;
        }

        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack xpBucket = new ItemStack(ModFluids.XP_BUCKET.get());
        if (!xpBucket.is(Tags.Items.BUCKETS)
                || !new FluidStack(ModFluids.XP_JUICE.get(), 1_000).is(Tags.Fluids.EXPERIENCE)) {
            helper.fail("XP bucket/fluid common tags were not loaded");
            return;
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, xpBucket);
        BlockPos absoluteTank = helper.absolutePos(tankPos);
        BlockHitResult hit = new BlockHitResult(absoluteTank.getCenter(), Direction.UP, absoluteTank, false);
        helper.getBlockState(tankPos).useItemOn(player.getMainHandItem(), helper.getLevel(),
                player, InteractionHand.MAIN_HAND, hit);
        if (tank.getTank().getFluidAmount() != 1_000
                || !tank.getTank().getFluid().is(ModFluids.XP_JUICE.get())
                || !player.getMainHandItem().is(Items.BUCKET)) {
            helper.fail("XP Bucket did not empty exactly one bucket into the Tank");
            return;
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModFluids.XP_BUCKET.get()));
        BlockPos absoluteAnvil = helper.absolutePos(anvilPos);
        BlockHitResult anvilHit = new BlockHitResult(absoluteAnvil.getCenter(), Direction.UP,
                absoluteAnvil, false);
        helper.getBlockState(anvilPos).useItemOn(player.getMainHandItem(), helper.getLevel(),
                player, InteractionHand.MAIN_HAND, anvilHit);
        if (anvil.getExperienceTank().getFluidAmount() != 1_000
                || !player.getMainHandItem().is(Items.BUCKET)) {
            helper.fail("Normal XP Bucket use opened/bypassed the Auto Anvil instead of filling it");
            return;
        }

        anvil.toggleSide(0, Direction.NORTH);
        var blocked = helper.getLevel().getCapability(
                net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                helper.absolutePos(anvilPos), Direction.NORTH);
        ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
        if (blocked == null || blocked.insertItem(0, pickaxe.copy(), false).isEmpty()) {
            helper.fail("Disabled Auto Anvil input face still accepted the tool");
            return;
        }
        var allowed = helper.getLevel().getCapability(
                net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                helper.absolutePos(anvilPos), Direction.SOUTH);
        if (allowed == null || !allowed.insertItem(0, pickaxe.copy(), false).isEmpty()
                || !anvil.getInventory().getStackInSlot(0).is(Items.IRON_PICKAXE)) {
            helper.fail("Enabled Auto Anvil input face rejected the tool");
            return;
        }
        anvil.getInventory().setStackInSlot(0, ItemStack.EMPTY);
        BlockPos inputChestPos = anvilPos.south();
        helper.setBlock(inputChestPos, Blocks.CHEST);
        if (!(helper.getBlockEntity(inputChestPos) instanceof
                net.minecraft.world.level.block.entity.ChestBlockEntity inputChest)) {
            helper.fail("Automatic-side test chest was missing");
            return;
        }
        inputChest.setItem(0, pickaxe.copy());
        anvil.toggleAutomatic(0);
        helper.runAfterDelay(2, () -> {
            if (!anvil.getInventory().getStackInSlot(0).is(Items.IRON_PICKAXE)
                    || !inputChest.getItem(0).isEmpty()) {
                helper.fail("Auto Anvil did not actively pull its configured input face");
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 45)
    public static void xpDrainShowerTargetAndRotatingElevatorWork(GameTestHelper helper) {
        BlockPos drainPos = new BlockPos(1, 1, 1);
        BlockPos sourceTankPos = new BlockPos(7, 3, 1);
        BlockPos showerPos = sourceTankPos.east();
        BlockPos targetPos = new BlockPos(7, 1, 6);
        BlockPos lowerElevatorPos = new BlockPos(12, 1, 1);
        BlockPos upperElevatorPos = new BlockPos(12, 4, 1);

        helper.setBlock(drainPos, ModBlocks.XP_DRAIN.get());
        helper.setBlock(sourceTankPos, ModBlocks.TANK.get());
        helper.setBlock(showerPos, ModBlocks.XP_SHOWER.get().defaultBlockState()
                .setValue(net.xuwu.openblocks_reborn.block.ExperienceMachineBlock.FACING, Direction.EAST));
        helper.setBlock(showerPos.south(), Blocks.REDSTONE_BLOCK);
        helper.setBlock(targetPos, ModBlocks.TARGET.get().defaultBlockState()
                .setValue(TargetBlock.FACING, Direction.NORTH));
        helper.setBlock(targetPos.east(), Blocks.REDSTONE_BLOCK);
        helper.setBlock(lowerElevatorPos, ModBlocks.ELEVATOR.get().defaultBlockState());
        helper.setBlock(upperElevatorPos, ModBlocks.ELEVATOR_ROTATING.get().defaultBlockState()
                .setValue(ElevatorBlock.FACING, Direction.EAST));

        if (!(helper.getBlockEntity(drainPos) instanceof ExperienceBlockEntity drain)
                || !(helper.getBlockEntity(showerPos) instanceof ExperienceBlockEntity shower)
                || !(helper.getBlockEntity(sourceTankPos) instanceof TankBlockEntity sourceTank)) {
            helper.fail("XP Drain, XP Shower or source Tank block entity was missing");
            return;
        }
        if (drain.getTank().getCapacity() != 1_000 || shower.getTank().getCapacity() != 1_000) {
            helper.fail("XP Drain and XP Shower must each expose exactly a 1B buffer");
            return;
        }
        sourceTank.getTank().fill(new FluidStack(ModFluids.XP_JUICE.get(), 1_000),
                IFluidHandler.FluidAction.EXECUTE);
        BlockPos absoluteSourceTank = helper.absolutePos(sourceTankPos);
        BlockPos absoluteShower = helper.absolutePos(showerPos);
        var showerSource = helper.getLevel().getCapability(
                net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
                absoluteSourceTank, Direction.EAST);
        if (showerSource == null
                || showerSource.drain(100, IFluidHandler.FluidAction.SIMULATE).getAmount() != 100) {
            helper.fail("XP Shower could not see the XP-fluid capability on the block behind it");
            return;
        }
        if (!helper.getLevel().hasNeighborSignal(absoluteShower)) {
            helper.fail("XP Shower did not detect the adjacent redstone signal");
            return;
        }

        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        // Reproduce /experience add <player> levels: visible XP exists while the
        // legacy totalExperience field can still be zero.
        player.experienceLevel = 5;
        player.experienceProgress = 0.0F;
        player.totalExperience = 0;
        BlockPos absoluteDrain = helper.absolutePos(drainPos);
        player.setPos(absoluteDrain.getX() + 0.5D, absoluteDrain.getY() + 0.1D,
                absoluteDrain.getZ() + 0.5D);
        helper.getLevel().addFreshEntity(player);

        helper.runAfterDelay(4, () -> {
            if (sourceTank.getTank().getFluidAmount() >= 1_000) {
                helper.fail("XP Shower did not pull XP fluid from the tank attached behind it");
                return;
            }
            if (shower.getTank().getFluidAmount() >= 100) {
                helper.fail("XP Shower pulled fluid but did not consume it into emitted experience"
                        + " (buffer=" + shower.getTank().getFluidAmount() + " mB)");
                return;
            }
            helper.runAfterDelay(1, () -> {
            if (player.experienceLevel >= 5 || drain.getExperience() <= 0) {
                helper.fail("XP Drain did not remove XP directly from a player standing above it");
                return;
            }
            if (!helper.getBlockState(targetPos).getValue(TargetBlock.DEPLOYED)) {
                helper.fail("Redstone-powered Target did not deploy upright");
                return;
            }

            BlockPos absoluteLower = helper.absolutePos(lowerElevatorPos);
            var elevatorPlayer = net.neoforged.neoforge.common.util.FakePlayerFactory
                    .getMinecraft(helper.getLevel());
            elevatorPlayer.setPos(absoluteLower.getX() + 0.5D, absoluteLower.getY() + 1.0D,
                    absoluteLower.getZ() + 0.5D);
            if (!ModBlocks.ELEVATOR.get().teleport(elevatorPlayer, true)) {
                helper.fail("Elevator did not find the rotating destination");
                return;
            }
            helper.runAfterDelay(2, () -> {
                if (Math.abs(net.minecraft.util.Mth.wrapDegrees(elevatorPlayer.getYRot() - 90.0F)) > 0.01F) {
                    helper.fail("Rotating Elevator did not send the destination yaw to the player: "
                            + elevatorPlayer.getYRot());
                    return;
                }
                helper.succeed();
            });
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void restoredProjectorPathWearablesAndLuggageSemantics(GameTestHelper helper) {
        BlockPos pathSupport = new BlockPos(1, 1, 1);
        BlockPos pathPos = pathSupport.above();
        helper.setBlock(pathSupport, Blocks.STONE);
        helper.setBlock(pathPos, ModBlocks.PATH.get());
        BlockState path = helper.getBlockState(pathPos);
        if (!path.canSurvive(helper.getLevel(), helper.absolutePos(pathPos))
                || !path.getCollisionShape(helper.getLevel(), helper.absolutePos(pathPos),
                CollisionContext.empty()).isEmpty()) {
            helper.fail("Path did not retain its supported, pass-through legacy shape");
            return;
        }
        helper.setBlock(pathSupport, Blocks.AIR);

        BlockPos projectorPos = new BlockPos(3, 1, 3);
        helper.setBlock(projectorPos, ModBlocks.PROJECTOR.get());
        if (!(helper.getBlockEntity(projectorPos) instanceof UtilityMachineBlockEntity projector)) {
            helper.fail("Projector block entity was missing");
            return;
        }
        projector.getInventory().setStackInSlot(0, new ItemStack(ModItems.EMPTY_MAP.get()));

        ItemStack pencil = new ItemStack(ModBlocks.IMAGINARY.asItem());
        ItemStack crayon = ImaginaryItem.createCrayon(0x3366CC);
        if (ImaginaryItem.isCrayon(pencil) || !ImaginaryItem.isCrayon(crayon)
                || ImaginaryItem.getColor(crayon) != 0x3366CC
                || ImaginaryItem.getUses(crayon) != ImaginaryItem.DEFAULT_USES) {
            helper.fail("Magic pencil/crayon components were not restored");
            return;
        }
        ItemStack redCrayon = ImaginaryItem.createCrayon(0xFF0000);
        ItemStack blueCrayon = ImaginaryItem.createCrayon(0x0000FF);
        var mixingInput = net.minecraft.world.item.crafting.CraftingInput.of(2, 1,
                java.util.List.of(redCrayon, blueCrayon));
        var mixingRecipe = new net.xuwu.openblocks_reborn.recipe.CrayonMixingRecipe(
                net.minecraft.world.item.crafting.CraftingBookCategory.MISC);
        ItemStack mixedCrayon = mixingRecipe.assemble(mixingInput, helper.getLevel().registryAccess());
        if (!mixingRecipe.matches(mixingInput, helper.getLevel())
                || ImaginaryItem.getColor(mixedCrayon) != 0x7F007F
                || Math.abs(ImaginaryItem.getUses(mixedCrayon) - 1.8F) > 0.001F
                || Math.abs(ImaginaryItem.getUses(mixingRecipe.getRemainingItems(mixingInput).get(0))
                - 9.0F) > 0.001F) {
            helper.fail("Magic crayon mixing and remaining-use behaviour was not restored");
            return;
        }
        var mergeInput = net.minecraft.world.item.crafting.CraftingInput.of(2, 1,
                java.util.List.of(ImaginaryItem.createConfigured(0xFF0000, 0, 3.0F),
                        ImaginaryItem.createConfigured(0xFF0000, 4, 4.5F)));
        var mergeRecipe = new net.xuwu.openblocks_reborn.recipe.CrayonMergeRecipe(
                net.minecraft.world.item.crafting.CraftingBookCategory.MISC);
        ItemStack mergedCrayon = mergeRecipe.assemble(mergeInput, helper.getLevel().registryAccess());
        if (!mergeRecipe.matches(mergeInput, helper.getLevel())
                || Math.abs(ImaginaryItem.getUses(mergedCrayon) - 7.5F) > 0.001F) {
            helper.fail("Matching magic crayons did not merge their remaining uses");
            return;
        }
        var glassesInput = net.minecraft.world.item.crafting.CraftingInput.of(2, 1,
                java.util.List.of(ImaginaryItem.createConfigured(0xCC3311, 0, 4.0F),
                        new ItemStack(Items.PAPER)));
        var glassesRecipe = new net.xuwu.openblocks_reborn.recipe.CrayonGlassesRecipe(
                net.minecraft.world.item.crafting.CraftingBookCategory.MISC);
        ItemStack colouredGlasses = glassesRecipe.assemble(glassesInput,
                helper.getLevel().registryAccess());
        if (!glassesRecipe.matches(glassesInput, helper.getLevel())
                || !colouredGlasses.is(ModItems.CRAYON_GLASSES.get())
                || GlassesItem.getGlassesColor(colouredGlasses) != 0xCC3311
                || Math.abs(ImaginaryItem.getUses(glassesRecipe.getRemainingItems(
                glassesInput).get(0)) - 3.0F) > 0.001F) {
            helper.fail("Crayon glasses did not inherit colour and consume exactly one use");
            return;
        }
        BlockPos imaginaryPos = new BlockPos(5, 1, 3);
        helper.setBlock(imaginaryPos, ModBlocks.IMAGINARY.get().defaultBlockState()
                .setValue(net.xuwu.openblocks_reborn.block.ImaginaryBlock.SHAPE,
                        net.xuwu.openblocks_reborn.block.ImaginaryBlock.Shape.PANEL));
        var imaginaryPlayer = net.neoforged.neoforge.common.util.FakePlayerFactory.get(
                helper.getLevel(), new com.mojang.authlib.GameProfile(
                        java.util.UUID.randomUUID(), "openblocks-imaginary-test"));
        if (!(helper.getBlockEntity(imaginaryPos) instanceof
                net.xuwu.openblocks_reborn.blockentity.ImaginaryBlockEntity imaginaryBlock)) {
            helper.fail("Imaginary block entity was missing");
            return;
        }
        imaginaryBlock.configure(0xCC3311, false);
        BlockState imaginaryState = helper.getBlockState(imaginaryPos);
        var legacyPanelBounds = net.xuwu.openblocks_reborn.block.ImaginaryBlock
                .geometry(imaginaryState).bounds();
        if (Math.abs(legacyPanelBounds.minY - 0.9D) > 0.0001D
                || Math.abs(legacyPanelBounds.maxY - 1.0D) > 0.0001D
                || !imaginaryState.getCollisionShape(helper.getLevel(),
                helper.absolutePos(imaginaryPos), CollisionContext.of(imaginaryPlayer)).isEmpty()) {
            helper.fail("Magic crayon panel did not retain its thin, glasses-gated shape");
            return;
        }
        imaginaryPlayer.setItemSlot(EquipmentSlot.HEAD, colouredGlasses);
        if (imaginaryState.getCollisionShape(helper.getLevel(), helper.absolutePos(imaginaryPos),
                CollisionContext.of(imaginaryPlayer)).isEmpty()) {
            helper.fail("Matching coloured glasses did not make the crayon panel solid");
            return;
        }
        BlockPos absoluteImaginary = helper.absolutePos(imaginaryPos);
        BlockHitResult imaginaryHit = new BlockHitResult(absoluteImaginary.getCenter(),
                Direction.UP, absoluteImaginary, false);
        if (imaginaryState.useWithoutItem(helper.getLevel(), imaginaryPlayer, imaginaryHit)
                != net.minecraft.world.InteractionResult.PASS
                || helper.getBlockState(imaginaryPos).getValue(
                net.xuwu.openblocks_reborn.block.ImaginaryBlock.SHAPE)
                != net.xuwu.openblocks_reborn.block.ImaginaryBlock.Shape.PANEL) {
            helper.fail("An empty hand was still able to change an Imaginary Block shape");
            return;
        }
        ItemStack unrelatedTool = new ItemStack(Items.STICK);
        var unrelatedResult = imaginaryState.useItemOn(unrelatedTool, helper.getLevel(),
                imaginaryPlayer, InteractionHand.MAIN_HAND, imaginaryHit);
        if (unrelatedResult != net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
                || helper.getBlockState(imaginaryPos).getValue(
                net.xuwu.openblocks_reborn.block.ImaginaryBlock.SHAPE)
                != net.xuwu.openblocks_reborn.block.ImaginaryBlock.Shape.PANEL) {
            helper.fail("An unrelated item was able to change an Imaginary Block shape");
            return;
        }
        imaginaryState.useItemOn(pencil, helper.getLevel(), imaginaryPlayer,
                InteractionHand.MAIN_HAND, imaginaryHit);
        if (helper.getBlockState(imaginaryPos).getValue(
                net.xuwu.openblocks_reborn.block.ImaginaryBlock.SHAPE)
                != net.xuwu.openblocks_reborn.block.ImaginaryBlock.Shape.STAIRS) {
            helper.fail("A Magic Pencil could not change an Imaginary Block shape");
            return;
        }
        int expectedGuideEntries = ModItems.BLOCK_ITEMS.size() + ModItems.ALL_ITEMS.size() + 1;
        int expectedGuidePages = expectedGuideEntries + 1 + 6;
        if (net.xuwu.openblocks_reborn.item.InfoBookItem.buildPages().size()
                != expectedGuidePages) {
            helper.fail("OpenBlocks Guide did not create one categorized entry for every item");
            return;
        }
        var skyInput = net.minecraft.world.item.crafting.CraftingInput.of(2, 1,
                java.util.List.of(new ItemStack(ModBlocks.SKY.asItem()),
                        new ItemStack(Items.REDSTONE_TORCH)));
        var skyRecipe = new net.xuwu.openblocks_reborn.recipe.SkyBlockInversionRecipe(
                net.minecraft.world.item.crafting.CraftingBookCategory.MISC);
        ItemStack invertedSky = skyRecipe.assemble(skyInput, helper.getLevel().registryAccess());
        if (!skyRecipe.matches(skyInput, helper.getLevel())
                || !net.xuwu.openblocks_reborn.item.SkyBlockItem.isInverted(invertedSky)) {
            helper.fail("Sky Block redstone-torch inversion recipe was not restored");
            return;
        }
        ItemStack damagedEye = new ItemStack(ModItems.GOLDEN_EYE.get());
        damagedEye.setDamageValue(30);
        CustomData.update(DataComponents.CUSTOM_DATA, damagedEye, tag -> tag.putLong(
                "GoldenEyeTarget", new BlockPos(120, 64, -48).asLong()));
        var rechargeInput = net.minecraft.world.item.crafting.CraftingInput.of(2, 1,
                java.util.List.of(damagedEye, new ItemStack(Items.ENDER_PEARL)));
        var rechargeRecipe = new net.xuwu.openblocks_reborn.recipe.GoldenEyeRechargeRecipe(
                net.minecraft.world.item.crafting.CraftingBookCategory.MISC);
        ItemStack rechargedEye = rechargeRecipe.assemble(rechargeInput, helper.getLevel().registryAccess());
        if (!rechargeRecipe.matches(rechargeInput, helper.getLevel())
                || rechargedEye.getDamageValue() != 20
                || !rechargedEye.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()
                .contains("GoldenEyeTarget")) {
            helper.fail("Golden Eye recharge did not preserve its target and repair ten uses");
            return;
        }
        ItemStack heldEye = new ItemStack(ModItems.GOLDEN_EYE.get());
        heldEye.setDamageValue(30);
        CustomData.update(DataComponents.CUSTOM_DATA, heldEye, tag -> {
            tag.putString("GoldenEyeStructure", "minecraft:village_plains");
            tag.putLong("GoldenEyeTarget", new BlockPos(120, 64, -48).asLong());
        });
        imaginaryPlayer.setItemInHand(InteractionHand.MAIN_HAND, heldEye);
        ModItems.GOLDEN_EYE.get().use(helper.getLevel(), imaginaryPlayer,
                InteractionHand.MAIN_HAND);
        boolean nonReturningGuide = helper.getLevel().getEntitiesOfClass(
                        net.xuwu.openblocks_reborn.entity.GoldenEyeEntity.class,
                        imaginaryPlayer.getBoundingBox().inflate(4.0D)).stream()
                .anyMatch(eye -> !eye.returnsItem());
        if (imaginaryPlayer.getMainHandItem().getCount() != 1
                || imaginaryPlayer.getMainHandItem().getDamageValue() != 31
                || !nonReturningGuide) {
            helper.fail("Golden Eye left the hand, duplicated a returning item, or did not consume durability");
            return;
        }
        GlassesItem glasses = ModItems.PENCIL_GLASSES.get();
        if (glasses.getEquipmentSlot() != EquipmentSlot.HEAD
                || ModItems.CRANE_BACKPACK.get().getEquipmentSlot() != EquipmentSlot.CHEST) {
            helper.fail("Glasses or Crane Backpack were not native wearable equipment");
            return;
        }
        ItemStack craneBackpack = new ItemStack(ModItems.CRANE_BACKPACK.get());
        imaginaryPlayer.setItemSlot(EquipmentSlot.CHEST, craneBackpack);
        ItemStack craneControl = new ItemStack(ModItems.CRANE_CONTROL.get());
        double initialCraneLength = CraneBackpackItem.length(craneBackpack);
        imaginaryPlayer.setShiftKeyDown(true);
        ModItems.CRANE_CONTROL.get().onUseTick(helper.getLevel(), imaginaryPlayer,
                craneControl, 72000);
        double loweredCraneLength = CraneBackpackItem.length(craneBackpack);
        imaginaryPlayer.setShiftKeyDown(false);
        ModItems.CRANE_CONTROL.get().onUseTick(helper.getLevel(), imaginaryPlayer,
                craneControl, 71999);
        if (loweredCraneLength <= initialCraneLength
                || Math.abs(CraneBackpackItem.length(craneBackpack)
                - initialCraneLength) > 0.001F) {
            helper.fail("Crane controller did not lower with sneak-use and retract with normal use");
            return;
        }
        var cursorPlayer = net.neoforged.neoforge.common.util.FakePlayerFactory.get(
                helper.getLevel(), new com.mojang.authlib.GameProfile(
                        java.util.UUID.randomUUID(), "openblocks-cursor-test"));
        cursorPlayer.setPos(helper.absolutePos(new BlockPos(0, 1, 0)).getCenter());
        cursorPlayer.giveExperiencePoints(100);
        int experienceBeforeCursor = cursorPlayer.totalExperience;
        var nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        if (nether == null) {
            helper.fail("Nether was unavailable for cross-dimensional Cursor validation");
            return;
        }
        BlockPos remoteLever = new BlockPos(cursorPlayer.blockPosition().getX() + 64, 64,
                cursorPlayer.blockPosition().getZ());
        nether.setBlock(remoteLever.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        nether.setBlock(remoteLever, Blocks.LEVER.defaultBlockState()
                .setValue(net.minecraft.world.level.block.LeverBlock.FACE, AttachFace.FLOOR),
                Block.UPDATE_ALL);
        ItemStack remoteCursor = new ItemStack(ModItems.CURSOR.get());
        CursorItem.bind(remoteCursor, nether, remoteLever, Direction.UP);
        cursorPlayer.setItemInHand(InteractionHand.MAIN_HAND, remoteCursor);
        ModItems.CURSOR.get().use(helper.getLevel(), cursorPlayer, InteractionHand.MAIN_HAND);
        if (!nether.getBlockState(remoteLever).getValue(
                net.minecraft.world.level.block.LeverBlock.POWERED)
                || experienceBeforeCursor - cursorPlayer.totalExperience < 38) {
            helper.fail("Cursor did not activate a 64-block cross-dimensional target with XP cost");
            return;
        }

        LuggageEntity luggage = new LuggageEntity(ModEntities.LUGGAGE.get(), helper.getLevel());
        BlockPos luggagePos = helper.absolutePos(new BlockPos(6, 1, 6));
        luggage.setPos(luggagePos.getCenter());
        luggage.getInventory().setStackInSlot(0, new ItemStack(Items.DIAMOND));
        helper.getLevel().addFreshEntity(luggage);
        if (luggage.hurt(helper.getLevel().damageSources().generic(), 100.0F)
                || luggage.getHealth() < luggage.getMaxHealth()) {
            helper.fail("Luggage accepted damage");
            return;
        }
        luggage.kill();

        helper.runAfterDelay(3, () -> {
            if (!helper.getBlockState(pathPos).isAir()) {
                helper.fail("Unsupported Path did not destroy itself");
                return;
            }
            if (!projector.getInventory().getStackInSlot(0).is(ModItems.HEIGHT_MAP.get())
                    || !projector.getBlockState().getValue(UtilityMachineBlock.POWERED)) {
                helper.fail("Projector did not upgrade an empty map and activate");
                return;
            }
            boolean foundPackedLuggage = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                            new net.minecraft.world.phys.AABB(luggagePos).inflate(2.0D))
                    .stream().anyMatch(drop -> drop.getItem().is(ModItems.LUGGAGE.get())
                            && !drop.getItem().getOrDefault(DataComponents.CONTAINER,
                            net.minecraft.world.item.component.ItemContainerContents.EMPTY).getStackInSlot(0).isEmpty());
            if (!foundPackedLuggage) {
                String nearbyDrops = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                                new net.minecraft.world.phys.AABB(luggagePos).inflate(2.0D)).stream()
                        .map(drop -> drop.getItem().getItem() + " x" + drop.getItem().getCount()
                                + " slots=" + drop.getItem().getOrDefault(DataComponents.CONTAINER,
                                net.minecraft.world.item.component.ItemContainerContents.EMPTY).getSlots())
                        .collect(java.util.stream.Collectors.joining(", "));
                String allLuggageDrops = java.util.stream.StreamSupport.stream(
                                helper.getLevel().getAllEntities().spliterator(), false)
                        .filter(ItemEntity.class::isInstance).map(ItemEntity.class::cast)
                        .filter(drop -> drop.getItem().is(ModItems.LUGGAGE.get()))
                        .map(drop -> drop.position() + " removed=" + drop.isRemoved())
                        .collect(java.util.stream.Collectors.joining(", "));
                helper.fail("Killed luggage did not leave its packed item form with contents; nearby=" + nearbyDrops
                        + "; all luggage drops=" + allLuggageDrops);
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void cursorCraneWrenchAndGuideParity(GameTestHelper helper) {
        var player = new net.neoforged.neoforge.common.util.FakePlayer(
                helper.getLevel(), new com.mojang.authlib.GameProfile(
                java.util.UUID.randomUUID(), "openblocks-control-test")) {
            private int testContainerId;

            @Override
            public java.util.OptionalInt openMenu(
                    net.minecraft.world.MenuProvider provider,
                    java.util.function.Consumer<net.minecraft.network.RegistryFriendlyByteBuf> extraDataWriter) {
                if (provider == null) return java.util.OptionalInt.empty();
                if (containerMenu != inventoryMenu) doCloseContainer();
                var menu = provider.createMenu(++testContainerId, getInventory(), this);
                if (menu == null) return java.util.OptionalInt.empty();
                containerMenu = menu;
                return java.util.OptionalInt.of(testContainerId);
            }
        };
        player.setPos(helper.absolutePos(new BlockPos(1, 3, 1)).getCenter());
        player.giveExperiencePoints(500);

        BlockPos cannonPos = helper.absolutePos(new BlockPos(15, 3, 1));
        helper.getLevel().setBlock(cannonPos, ModBlocks.CANNON.get().defaultBlockState(),
                Block.UPDATE_ALL);
        BlockPos localClickPos = helper.absolutePos(new BlockPos(2, 3, 1));
        helper.getLevel().setBlock(localClickPos, Blocks.STONE.defaultBlockState(),
                Block.UPDATE_ALL);
        ItemStack cursor = new ItemStack(ModItems.CURSOR.get());
        CursorItem.bind(cursor, helper.getLevel(), cannonPos, Direction.UP);
        player.setItemInHand(InteractionHand.MAIN_HAND, cursor);
        player.setShiftKeyDown(false);
        BlockHitResult localClick = new BlockHitResult(localClickPos.getCenter(), Direction.UP,
                localClickPos, false);
        ModItems.CURSOR.get().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, localClick));
        if (!(player.containerMenu instanceof MachineMenu)
                || !player.containerMenu.stillValid(player)) {
            helper.fail("Cursor's remote machine menu closed at the vanilla eight-block limit");
            return;
        }
        var cursorData = cursor.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!BlockPos.of(cursorData.getLong("CursorPosition")).equals(cannonPos)) {
            helper.fail("Normal Cursor use rebound it instead of activating its saved target");
            return;
        }
        player.closeContainer();

        BlockPos bindPos = helper.absolutePos(new BlockPos(3, 3, 1));
        helper.getLevel().setBlock(bindPos, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        player.setShiftKeyDown(true);
        ModItems.CURSOR.get().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(bindPos.getCenter(), Direction.NORTH, bindPos, false)));
        player.setShiftKeyDown(false);
        cursorData = cursor.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!BlockPos.of(cursorData.getLong("CursorPosition")).equals(bindPos)
                || !"north".equals(cursorData.getString("CursorSide"))) {
            helper.fail("Sneak-use did not bind the Cursor to the clicked block face");
            return;
        }

        BlockPos unloadedPos = player.blockPosition().offset(512, 0, 0);
        if (helper.getLevel().hasChunkAt(unloadedPos)) {
            helper.fail("Cursor unloaded-target test unexpectedly started with a loaded chunk");
            return;
        }
        CursorItem.bind(cursor, helper.getLevel(), unloadedPos, Direction.UP);
        int xpBeforeUnloadedUse = player.totalExperience;
        ModItems.CURSOR.get().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        if (helper.getLevel().hasChunkAt(unloadedPos)
                || player.containerMenu != player.inventoryMenu
                || player.totalExperience != xpBeforeUnloadedUse) {
            helper.fail("Cursor force-loaded or charged XP for an unloaded target");
            return;
        }

        BlockPos machinePos = helper.absolutePos(new BlockPos(4, 3, 1));
        helper.getLevel().setBlock(machinePos, ModBlocks.BLOCK_PLACER.get().defaultBlockState()
                .setValue(ItemMachineBlock.FACING, Direction.NORTH), Block.UPDATE_ALL);
        ItemStack wrench = new ItemStack(ModItems.WRENCH.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, wrench);
        ModItems.WRENCH.get().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(machinePos.getCenter(), Direction.UP, machinePos, false)));
        if (helper.getLevel().getBlockState(machinePos).getValue(ItemMachineBlock.FACING)
                != Direction.NORTH.getClockWise(Direction.Axis.Y)) {
            helper.fail("Wrench did not rotate a directional machine around the clicked face axis");
            return;
        }

        BlockPos guidePos = helper.absolutePos(new BlockPos(7, 12, 7));
        BlockState placedGuideState = ModBlocks.GUIDE.get().getStateForPlacement(
                new BlockPlaceContext(new UseOnContext(player, InteractionHand.MAIN_HAND,
                        new BlockHitResult(guidePos.getCenter(), Direction.UP,
                                guidePos.below(), false))));
        if (placedGuideState == null
                || placedGuideState.getValue(GuideBlock.ORIENTATION).top() != Direction.UP) {
            helper.fail("Placed Guide did not keep its no-minus faces on world UP/DOWN");
            return;
        }
        helper.getLevel().setBlock(guidePos, ModBlocks.BUILDER_GUIDE.get().defaultBlockState(),
                Block.UPDATE_ALL);
        helper.getLevel().setBlock(guidePos.below(), Blocks.REDSTONE_BLOCK.defaultBlockState(),
                Block.UPDATE_ALL);
        if (!(helper.getLevel().getBlockEntity(guidePos) instanceof GuideBlockEntity guide)) {
            helper.fail("Builder Guide block entity was missing");
            return;
        }
        guide.updatePowered();
        ItemStack buildingBlocks = new ItemStack(Items.COBBLESTONE, 4);
        player.setItemInHand(InteractionHand.MAIN_HAND, buildingBlocks);
        int placed = guide.placeBlocks(helper.getLevel(), player, InteractionHand.MAIN_HAND,
                buildingBlocks, (BlockItem)buildingBlocks.getItem(), Direction.UP);
        if (!guide.isActive() || placed != 1 || buildingBlocks.getCount() != 3
                || net.xuwu.openblocks_reborn.blockentity.GuideShape.values().length != 11) {
            helper.fail("Builder Guide did not restore powered one-block placement and eleven shapes");
            return;
        }

        ItemStack backpack = new ItemStack(ModItems.CRANE_BACKPACK.get());
        player.setItemSlot(EquipmentSlot.CHEST, backpack);
        player.setPos(helper.absolutePos(new BlockPos(5, 5, 5)).getCenter());
        player.setYRot(0.0F);
        Vec3 magnet = CraneBackpackItem.magnetPosition(player, backpack);
        BlockPos pickupPos = BlockPos.containing(magnet.add(0.0D, -0.6D, 0.0D));
        helper.getLevel().setBlock(pickupPos, Blocks.COBBLESTONE.defaultBlockState(),
                Block.UPDATE_ALL);
        if (!CraneBackpackItem.toggleMagnet(player)) {
            helper.fail("Crane failed to pick up a block under its magnet");
            return;
        }
        java.util.UUID carriedId = CraneBackpackItem.carriedId(backpack);
        var carried = carriedId == null ? null : helper.getLevel().getEntity(carriedId);
        if (!(carried instanceof CraneCarriedBlockEntity) || !helper.getLevel().getBlockState(pickupPos).isAir()) {
            helper.fail("Crane did not replace a lifted block with its smooth tracked entity");
            return;
        }
        double beforeMove = carried.getX();
        player.setPos(player.getX() + 1.0D, player.getY(), player.getZ());
        ModItems.CRANE_BACKPACK.get().inventoryTick(backpack, helper.getLevel(), player, 0, false);
        double targetX = CraneBackpackItem.magnetPosition(player, backpack).x;
        if (carried.getX() <= beforeMove || carried.getX() >= targetX
                || carried.getType() != ModEntities.CRANE_CARRIED_BLOCK.get()) {
            helper.fail("Crane's carried block snapped instead of moving through an interpolated tracked step");
            return;
        }
        ItemStack craneControl = new ItemStack(ModItems.CRANE_CONTROL.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, craneControl);
        if (ModItems.CRANE_CONTROL.get().canAttackBlock(
                Blocks.STONE.defaultBlockState(), helper.getLevel(), machinePos, player)
                || !ModItems.CRANE_CONTROL.get().onLeftClickEntity(
                        craneControl, player, carried)
                || !ModItems.CRANE_CONTROL.get().onItemUseFirst(
                        craneControl, new UseOnContext(player, InteractionHand.MAIN_HAND,
                                new BlockHitResult(machinePos.getCenter(), Direction.UP,
                                        machinePos, false))).consumesAction()
                || !player.isUsingItem()) {
            helper.fail("Crane Control did not suppress mining, attacks and target right-click use");
            return;
        }
        helper.succeed();
    }

    private PortGameTests() {
    }
}
