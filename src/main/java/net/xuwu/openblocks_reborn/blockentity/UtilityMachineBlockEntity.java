package net.xuwu.openblocks_reborn.blockentity;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.particles.ParticleTypes;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.EventHooks;
import net.minecraft.world.level.material.Fluids;
import net.xuwu.openblocks_reborn.block.UtilityMachineBlock;
import net.xuwu.openblocks_reborn.item.PaintBrushItem;
import net.xuwu.openblocks_reborn.item.HeightMapItem;
import net.xuwu.openblocks_reborn.item.GlyphItem;
import net.xuwu.openblocks_reborn.item.StencilItem;
import net.xuwu.openblocks_reborn.registry.ModBlockEntities;
import net.xuwu.openblocks_reborn.registry.ModBlocks;
import net.xuwu.openblocks_reborn.registry.ModItems;
import net.xuwu.openblocks_reborn.registry.ModFluids;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.neoforged.neoforge.items.IItemHandler;

public class UtilityMachineBlockEntity extends BlockEntity {
    public static final int PAINT_MIX_TICKS = 300;
    public static final int AUTO_ANVIL_MAX_LEVEL = 45;
    /**
     * Ask the vanilla enchanting algorithm for its third-option ceiling instead
     * of duplicating the level-30 constant. Runtime scans additionally pass this
     * result through NeoForge's level event so enchanting-overhaul mods can raise it.
     */
    public static final int DEFAULT_AUTO_ENCHANT_MAX_LEVEL = vanillaEnchantCeiling();
    private static final int MAX_SAFE_EXPERIENCE_LEVEL = ModFluids.levelForExperience(
            Integer.MAX_VALUE / ModFluids.MILLIBUCKETS_PER_XP);
    private final ItemStackHandler inventory;
    private final FluidTank experienceTank;
    private final FluidTank waterTank = new FluidTank(1_000, stack ->
            exposesWaterTank() && stack.is(Fluids.WATER)) {
        @Override
        protected void onContentsChanged() {
            UtilityMachineBlockEntity.this.setChanged();
            if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    };
    private int ticks;
    private int selectedEnchantLevel = Math.min(20, DEFAULT_AUTO_ENCHANT_MAX_LEVEL);
    private int maximumEnchantLevel = DEFAULT_AUTO_ENCHANT_MAX_LEVEL;
    private int availableEnchantPower;
    private int selectedEnchantCost;
    private long enchantmentSeed = RandomSource.create().nextLong();
    @Nullable
    private String anvilItemName;
    private int selectedPaintColor = 0xB0417E;
    private int paintResultColor = selectedPaintColor;
    private int paintProgress;
    private int drawingMode;
    private int selectedStencil;
    private int selectedGlyph = GlyphItem.selectableIndex('A');
    private boolean updatingDrawingOutput;
    private boolean updatingProjectorMap;
    private int projectorRotation;
    private final int[] sideMasks = {0x3F, 0x3F, 0x3F, 0x3F};
    private int automaticChannels;

    public UtilityMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.UTILITY_MACHINE.get(), pos, state);
        experienceTank = new FluidTank(experienceCapacity(state), stack ->
                exposesExperienceTank() && stack.is(ModFluids.XP_JUICE.get())) {
            @Override
            protected void onContentsChanged() {
                UtilityMachineBlockEntity.this.setChanged();
                if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        };
        int slots = state.getBlock() instanceof UtilityMachineBlock machine ? machine.kind().slots() : 1;
        inventory = new ItemStackHandler(slots) {
            @Override
            protected void onContentsChanged(int slot) {
                UtilityMachineBlockEntity.this.setChanged();
                if (slot == 0 && isAutoAnvil()) {
                    anvilItemName = null;
                }
                if (slot == 0 && !updatingDrawingOutput && isDrawingTable()) {
                    UtilityMachineBlockEntity.this.refreshDrawingOutput();
                }
                if (slot == 0 && !updatingProjectorMap && isProjector()) {
                    UtilityMachineBlockEntity.this.refreshProjector();
                }
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                if (!(UtilityMachineBlockEntity.this.getBlockState().getBlock() instanceof UtilityMachineBlock block)) return false;
                return switch (block.kind()) {
                    case AUTO_ANVIL -> slot == 0 ? EnchantmentHelper.canStoreEnchantments(stack) : slot == 1;
                    case AUTO_ENCHANTMENT_TABLE -> slot == 0 ? stack.isEnchantable()
                            : slot == 1 && stack.is(Tags.Items.ENCHANTING_FUELS);
                    case PAINT_MIXER -> slot == 0
                            ? stack.is(ModItems.PAINTBRUSH.get()) || stack.is(ModItems.SMALL_PAINTBRUSH.get())
                                    || stack.is(ModBlocks.PAINT_CAN.asItem())
                            : switch (slot) {
                                case 2 -> stack.getItem() instanceof DyeItem dye
                                        && dye.getDyeColor() == net.minecraft.world.item.DyeColor.CYAN;
                                case 3 -> stack.getItem() instanceof DyeItem dye
                                        && dye.getDyeColor() == net.minecraft.world.item.DyeColor.MAGENTA;
                                case 4 -> stack.getItem() instanceof DyeItem dye
                                        && dye.getDyeColor() == net.minecraft.world.item.DyeColor.YELLOW;
                                case 5 -> stack.getItem() instanceof DyeItem dye
                                        && dye.getDyeColor() == net.minecraft.world.item.DyeColor.BLACK;
                                default -> false;
                            };
                    case DRAWING_TABLE -> slot == 0 && isDrawingInput(stack);
                    case PROJECTOR -> slot == 0
                            && (stack.is(ModItems.HEIGHT_MAP.get()) || stack.is(ModItems.EMPTY_MAP.get()));
                    case SPRINKLER -> stack.is(Items.BONE_MEAL);
                    case DONATION_STATION -> true;
                };
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (!isDrawingTable() || slot != 1) return super.extractItem(slot, amount, simulate);
                UtilityMachineBlockEntity.this.refreshDrawingOutput();
                ItemStack extracted = super.extractItem(slot, amount, simulate);
                if (!simulate && !extracted.isEmpty()) {
                    super.extractItem(0, extracted.getCount(), false);
                    UtilityMachineBlockEntity.this.refreshDrawingOutput();
                }
                return extracted;
            }
        };
    }

    private static int experienceCapacity(BlockState state) {
        if (!(state.getBlock() instanceof UtilityMachineBlock machine)) return 1_000;
        return switch (machine.kind()) {
            case AUTO_ANVIL -> ModFluids.xpToFluid(ModFluids.experienceForLevel(AUTO_ANVIL_MAX_LEVEL));
            case AUTO_ENCHANTMENT_TABLE ->
                    ModFluids.xpToFluid(ModFluids.experienceForLevel(DEFAULT_AUTO_ENCHANT_MAX_LEVEL));
            default -> 1_000;
        };
    }

    private static int vanillaEnchantCeiling() {
        return Math.max(1, EnchantmentHelper.getEnchantmentCost(RandomSource.create(0L), 2,
                EnchantingTableBlock.BOOKSHELF_OFFSETS.size(), new ItemStack(Items.BOOK)));
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public boolean supportsSideConfiguration() {
        if (!(getBlockState().getBlock() instanceof UtilityMachineBlock block)) return false;
        return block.kind() == UtilityMachineBlock.Kind.AUTO_ANVIL
                || block.kind() == UtilityMachineBlock.Kind.AUTO_ENCHANTMENT_TABLE;
    }

    public int getSideMask(int channel) {
        return channel >= 0 && channel < sideMasks.length ? sideMasks[channel] : 0;
    }

    public int getSideConfiguration(int channel) {
        return getSideMask(channel) | (isAutomatic(channel) ? 0x40 : 0);
    }

    public boolean isAutomatic(int channel) {
        return channel >= 0 && channel < sideMasks.length && (automaticChannels & 1 << channel) != 0;
    }

    public boolean toggleSide(int channel, Direction direction) {
        if (!supportsSideConfiguration() || channel < 0 || channel >= sideMasks.length) return false;
        sideMasks[channel] ^= 1 << direction.get3DDataValue();
        setChanged();
        return true;
    }

    public boolean toggleAutomatic(int channel) {
        if (!supportsSideConfiguration() || channel < 0 || channel >= sideMasks.length) return false;
        automaticChannels ^= 1 << channel;
        setChanged();
        return true;
    }

    public IItemHandler getItemHandler(Direction side) {
        if (!supportsSideConfiguration() || side == null) return inventory;
        int sideBit = 1 << side.get3DDataValue();
        return new ConfiguredItemHandler(inventory,
                slot -> slot >= 0 && slot < 2 && (sideMasks[slot] & sideBit) != 0,
                slot -> slot == 2 && (sideMasks[2] & sideBit) != 0);
    }

    public boolean allowsExperienceInput(Direction side) {
        return !supportsSideConfiguration() || side == null
                || (sideMasks[3] & 1 << side.get3DDataValue()) != 0;
    }

    public FluidTank getExperienceTank() {
        return experienceTank;
    }

    public int getEnchantPowerLimit() {
        return selectedEnchantLevel;
    }

    public void setEnchantPowerLimit(int requestedLevel) {
        selectedEnchantLevel = Math.clamp(requestedLevel, 1, maximumEnchantLevel);
        setChanged();
    }

    public int getEnchantLevel() {
        return maximumEnchantLevel;
    }

    public int getMaximumEnchantLevel() {
        return maximumEnchantLevel;
    }

    public int getAvailableEnchantPower() {
        return availableEnchantPower;
    }

    public int getSelectedMachineCost() {
        if (getBlockState().getBlock() instanceof UtilityMachineBlock block) {
            if (block.kind() == UtilityMachineBlock.Kind.AUTO_ANVIL) {
                return calculateAnvilOperation(inventory.getStackInSlot(0),
                        inventory.getStackInSlot(1), anvilItemName).levelCost();
            }
            if (block.kind() == UtilityMachineBlock.Kind.AUTO_ENCHANTMENT_TABLE
                    && level instanceof ServerLevel) {
                return selectedEnchantCost;
            }
        }
        return 0;
    }

    public void setAnvilItemName(String requestedName) {
        if (!isAutoAnvil()) return;
        String filtered = StringUtil.filterText(requestedName);
        if (filtered.length() > net.minecraft.world.inventory.AnvilMenu.MAX_NAME_LENGTH) return;
        anvilItemName = filtered;
        setChanged();
    }

    public int getSelectedPaintColor() {
        return selectedPaintColor;
    }

    public void setSelectedPaintColor(int color) {
        selectedPaintColor = color & 0xFFFFFF;
        setChanged();
    }

    public int getPaintProgress() {
        return paintProgress;
    }

    public boolean startPaintMix() {
        if (paintProgress > 0 || !isPaintTarget(inventory.getStackInSlot(0))) return false;
        boolean[] requiredDyes = requiredPaintDyes(selectedPaintColor);
        for (int index = 0; index < requiredDyes.length; index++) {
            if (requiredDyes[index] && inventory.getStackInSlot(index + 2).isEmpty()) return false;
        }
        for (int index = 0; index < requiredDyes.length; index++) {
            if (requiredDyes[index]) inventory.extractItem(index + 2, 1, false);
        }
        paintResultColor = selectedPaintColor;
        paintProgress = PAINT_MIX_TICKS;
        setChanged();
        return true;
    }

    public int getDrawingMode() {
        return drawingMode;
    }

    public int getDrawingSelection() {
        return drawingMode == 0 ? selectedStencil : GlyphItem.selectableCharacter(selectedGlyph);
    }

    public int getProjectorRotation() {
        return projectorRotation;
    }

    public boolean isProjectorActive() {
        return isProjector() && HeightMapItem.getHeights(inventory.getStackInSlot(0)).length == 81;
    }

    public void rotateProjector(int delta) {
        if (!isProjector()) return;
        projectorRotation = Math.floorMod(projectorRotation + delta, 4);
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    public void cycleDrawingMode() {
        drawingMode = 1 - drawingMode;
        refreshDrawingOutput();
        setChanged();
    }

    public void changeDrawingSelection(int direction) {
        if (drawingMode == 0) selectedStencil = Math.floorMod(selectedStencil + direction, StencilItem.PATTERN_COUNT);
        else selectedGlyph = Math.floorMod(selectedGlyph + direction, GlyphItem.selectableCount());
        refreshDrawingOutput();
        setChanged();
    }

    public boolean printGlyph(int character) {
        if (drawingMode != 1 || !(level instanceof ServerLevel serverLevel)
                || !isDrawingInput(inventory.getStackInSlot(0))) return false;
        int sanitized = GlyphItem.sanitizeCharacter(character);
        inventory.extractItem(0, 1, false);
        ItemEntity drop = new ItemEntity(serverLevel, worldPosition.getX() + 0.5D,
                worldPosition.getY() + 1.0D, worldPosition.getZ() + 0.5D,
                GlyphItem.createStack(sanitized));
        drop.setDefaultPickUpDelay();
        serverLevel.addFreshEntity(drop);
        refreshDrawingOutput();
        setChanged();
        return true;
    }

    public FluidTank getFluidTank() {
        return exposesWaterTank() ? waterTank : experienceTank;
    }

    public boolean exposesFluidTank() {
        return exposesExperienceTank() || exposesWaterTank();
    }

    public boolean exposesWaterTank() {
        return getBlockState().getBlock() instanceof UtilityMachineBlock block
                && block.kind() == UtilityMachineBlock.Kind.SPRINKLER;
    }

    public boolean exposesExperienceTank() {
        if (!(getBlockState().getBlock() instanceof UtilityMachineBlock block)) return false;
        return block.kind() == UtilityMachineBlock.Kind.AUTO_ANVIL
                || block.kind() == UtilityMachineBlock.Kind.AUTO_ENCHANTMENT_TABLE;
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, UtilityMachineBlockEntity machine) {
        if (!(state.getBlock() instanceof UtilityMachineBlock block)) return;
        machine.ticks++;
        if (machine.supportsSideConfiguration()) {
            machine.performAutomaticTransfers(level, pos);
        }
        if (block.kind() == UtilityMachineBlock.Kind.PAINT_MIXER) {
            machine.tickPaintMixer();
            return;
        }
        if (block.kind() == UtilityMachineBlock.Kind.AUTO_ENCHANTMENT_TABLE) {
            if (machine.ticks % 20 == 1) machine.updateAvailableEnchantPower(level, pos);
            machine.enchantItem(level);
            return;
        }
        if (block.kind() == UtilityMachineBlock.Kind.AUTO_ANVIL) {
            if (machine.ticks % 40 == 0) machine.repairItem(level, pos);
            return;
        }
        if (machine.ticks % 20 != 0) return;
        switch (block.kind()) {
            case DRAWING_TABLE -> machine.refreshDrawingOutput();
            case SPRINKLER -> machine.sprinkle(level, pos, state);
            case PROJECTOR -> machine.project(level, pos, state);
            default -> { }
        }
    }

    private void performAutomaticTransfers(ServerLevel level, BlockPos pos) {
        if (isAutomatic(0)) pullItemIntoSlot(level, pos, 0, sideMasks[0]);
        if (isAutomatic(1)) pullItemIntoSlot(level, pos, 1, sideMasks[1]);
        if (isAutomatic(2)) pushItemFromSlot(level, pos, 2, sideMasks[2]);
        if (isAutomatic(3)) pullExperience(level, pos, sideMasks[3]);
    }

    private void pullItemIntoSlot(ServerLevel level, BlockPos pos, int targetSlot, int sideMask) {
        for (Direction direction : Direction.values()) {
            if ((sideMask & 1 << direction.get3DDataValue()) == 0) continue;
            IItemHandler source = level.getCapability(Capabilities.ItemHandler.BLOCK,
                    pos.relative(direction), direction.getOpposite());
            if (source == null) continue;
            for (int sourceSlot = 0; sourceSlot < source.getSlots(); sourceSlot++) {
                ItemStack offered = source.extractItem(sourceSlot, 64, true);
                if (offered.isEmpty()) continue;
                ItemStack remainder = inventory.insertItem(targetSlot, offered, true);
                int accepted = offered.getCount() - remainder.getCount();
                if (accepted <= 0) continue;
                ItemStack extracted = source.extractItem(sourceSlot, accepted, false);
                ItemStack leftover = inventory.insertItem(targetSlot, extracted, false);
                if (!leftover.isEmpty()) {
                    source.insertItem(sourceSlot, leftover, false);
                }
                return;
            }
        }
    }

    private void pushItemFromSlot(ServerLevel level, BlockPos pos, int sourceSlot, int sideMask) {
        ItemStack offered = inventory.extractItem(sourceSlot, 64, true);
        if (offered.isEmpty()) return;
        for (Direction direction : Direction.values()) {
            if ((sideMask & 1 << direction.get3DDataValue()) == 0) continue;
            IItemHandler destination = level.getCapability(Capabilities.ItemHandler.BLOCK,
                    pos.relative(direction), direction.getOpposite());
            if (destination == null) continue;
            for (int targetSlot = 0; targetSlot < destination.getSlots(); targetSlot++) {
                ItemStack remainder = destination.insertItem(targetSlot, offered, true);
                int accepted = offered.getCount() - remainder.getCount();
                if (accepted <= 0) continue;
                ItemStack extracted = inventory.extractItem(sourceSlot, accepted, false);
                ItemStack leftover = destination.insertItem(targetSlot, extracted, false);
                if (!leftover.isEmpty()) inventory.insertItem(sourceSlot, leftover, false);
                return;
            }
        }
    }

    private void pullExperience(ServerLevel level, BlockPos pos, int sideMask) {
        if (experienceTank.getSpace() <= 0) return;
        for (Direction direction : Direction.values()) {
            if ((sideMask & 1 << direction.get3DDataValue()) == 0) continue;
            IFluidHandler source = level.getCapability(Capabilities.FluidHandler.BLOCK,
                    pos.relative(direction), direction.getOpposite());
            if (source == null) continue;
            FluidStack offered = source.drain(Math.min(250, experienceTank.getSpace()),
                    IFluidHandler.FluidAction.SIMULATE);
            if (offered.isEmpty() || !offered.is(ModFluids.XP_JUICE.get())) continue;
            int accepted = experienceTank.fill(offered, IFluidHandler.FluidAction.SIMULATE);
            if (accepted <= 0) continue;
            FluidStack drained = source.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
            experienceTank.fill(drained, IFluidHandler.FluidAction.EXECUTE);
            return;
        }
    }

    private void repairItem(ServerLevel level, BlockPos pos) {
        ItemStack input = inventory.getStackInSlot(0);
        ItemStack modifier = inventory.getStackInSlot(1);
        if (input.isEmpty() || !inventory.getStackInSlot(2).isEmpty()) return;
        AnvilOperation operation = calculateAnvilOperation(input, modifier, anvilItemName);
        if (operation.output().isEmpty() || operation.levelCost() <= 0) return;
        int fluidCost = ModFluids.xpToFluid(ModFluids.experienceForLevel(operation.levelCost()));
        if (experienceTank.getFluidAmount() < fluidCost) return;

        experienceTank.drain(fluidCost, IFluidHandler.FluidAction.EXECUTE);
        inventory.extractItem(0, 1, false);
        if (operation.modifierCost() > 0) {
            inventory.extractItem(1, operation.modifierCost(), false);
        }
        inventory.setStackInSlot(2, operation.output());
        anvilItemName = null;
        level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.3F, 1.0F);
    }

    private void enchantItem(ServerLevel level) {
        ItemStack input = inventory.getStackInSlot(0);
        ItemStack lapis = inventory.getStackInSlot(1);
        int levelRequirement = calculateEnchantmentCost(level, input);
        int lapisCost = lapisCost(levelRequirement);
        if (levelRequirement != selectedEnchantCost) {
            selectedEnchantCost = levelRequirement;
            setChanged();
        }
        if (input.isEmpty() || !input.isEnchantable() || !lapis.is(Tags.Items.ENCHANTING_FUELS)
                || lapis.getCount() < lapisCost || !inventory.getStackInSlot(2).isEmpty()
                || levelRequirement <= 0) return;

        int availableExperience = ModFluids.fluidToXp(experienceTank.getFluidAmount());
        if (ModFluids.levelForExperience(availableExperience) < levelRequirement) return;
        int experienceCost = ModFluids.experienceForLevel(levelRequirement)
                - ModFluids.experienceForLevel(Math.max(0, levelRequirement - lapisCost));
        int fluidCost = ModFluids.xpToFluid(experienceCost);
        if (experienceTank.getFluidAmount() < fluidCost) return;

        List<EnchantmentInstance> enchantments = getEnchantmentList(level, input, levelRequirement);
        if (enchantments.isEmpty()) return;
        ItemStack result = input.getItem().applyEnchantments(input.copyWithCount(1), enchantments);
        inventory.extractItem(0, 1, false);
        inventory.extractItem(1, lapisCost, false);
        experienceTank.drain(fluidCost, IFluidHandler.FluidAction.EXECUTE);
        inventory.setStackInSlot(2, result);
        enchantmentSeed = level.random.nextLong();
        selectedEnchantCost = 0;
        level.playSound(null, worldPosition, SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.BLOCKS, 1.0F, level.random.nextFloat() * 0.1F + 0.9F);
    }

    private void updateAvailableEnchantPower(ServerLevel level, BlockPos pos) {
        float power = 0.0F;
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            if (EnchantingTableBlock.isValidBookShelf(level, pos, offset)) {
                BlockPos shelfPos = pos.offset(offset);
                power += level.getBlockState(shelfPos).getEnchantPowerBonus(level, shelfPos);
            }
        }
        int updated = Math.max(0, Mth.floor(power));
        int updatedMaximum = 1;
        if (updated > 0) {
            ItemStack input = inventory.getStackInSlot(0);
            ItemStack ceilingInput = input.isEmpty() ? new ItemStack(Items.BOOK) : input;
            int vanillaMaximum = EnchantmentHelper.getEnchantmentCost(
                    RandomSource.create(enchantmentSeed), 2, updated, ceilingInput);
            int eventMaximum = EventHooks.onEnchantmentLevelSet(level, pos, 2, updated,
                    ceilingInput, Math.max(1, vanillaMaximum));
            updatedMaximum = Math.clamp(eventMaximum, 1, MAX_SAFE_EXPERIENCE_LEVEL);
        }
        if (updated != availableEnchantPower || updatedMaximum != maximumEnchantLevel) {
            availableEnchantPower = updated;
            maximumEnchantLevel = updatedMaximum;
            selectedEnchantLevel = Math.clamp(selectedEnchantLevel, 1, maximumEnchantLevel);
            int requiredCapacity = ModFluids.xpToFluid(ModFluids.experienceForLevel(maximumEnchantLevel));
            if (requiredCapacity > experienceTank.getCapacity()) experienceTank.setCapacity(requiredCapacity);
            setChanged();
        }
    }

    private int calculateEnchantmentCost(ServerLevel level, ItemStack input) {
        if (input.isEmpty() || !input.isEnchantable() || availableEnchantPower <= 0) return 0;
        int requested = Math.clamp(selectedEnchantLevel, 1, maximumEnchantLevel);
        int adjusted = EventHooks.onEnchantmentLevelSet(level, worldPosition, 2,
                availableEnchantPower, input, requested);
        return Math.clamp(adjusted, 1, maximumEnchantLevel);
    }

    private List<EnchantmentInstance> getEnchantmentList(ServerLevel level, ItemStack input,
                                                          int cost) {
        Optional<HolderSet.Named<Enchantment>> allowed = level.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT).getTag(EnchantmentTags.IN_ENCHANTING_TABLE);
        if (allowed.isEmpty()) return List.of();
        RandomSource random = RandomSource.create(enchantmentSeed + cost);
        List<EnchantmentInstance> selected = new ArrayList<>(
                EnchantmentHelper.selectEnchantment(random, input, cost, allowed.get().stream()));
        if (input.is(Items.BOOK) && selected.size() > 1) {
            selected.remove(random.nextInt(selected.size()));
        }
        return selected;
    }

    private int lapisCost(int selectedLevel) {
        if (selectedLevel <= 0) return 1;
        return Math.clamp(Mth.ceil(selectedLevel * 3.0F / Math.max(1, maximumEnchantLevel)), 1, 3);
    }

    /**
     * The 1.21.1 vanilla anvil result algorithm adapted for an automated machine:
     * no player-supplied rename is performed, while material repair, sacrifice repair,
     * enchanted-book merging, compatibility, anvil costs and prior-work penalties match
     * {@link net.minecraft.world.inventory.AnvilMenu#createResult()}.
     */
    private static AnvilOperation calculateAnvilOperation(ItemStack input, ItemStack modifier,
                                                           @Nullable String requestedName) {
        if (input.isEmpty() || !EnchantmentHelper.canStoreEnchantments(input)) {
            return AnvilOperation.EMPTY;
        }

        ItemStack result = input.copyWithCount(1);
        ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(
                EnchantmentHelper.getEnchantmentsForCrafting(result));
        long baseCost = input.getOrDefault(DataComponents.REPAIR_COST, 0)
                + (long)modifier.getOrDefault(DataComponents.REPAIR_COST, 0);
        int addedCost = 0;
        int renameCost = 0;
        int modifierCost = 0;
        boolean storedBook = false;

        if (!modifier.isEmpty()) {
            storedBook = modifier.has(DataComponents.STORED_ENCHANTMENTS);
            if (result.isDamageableItem() && result.getItem().isValidRepairItem(input, modifier)) {
                int repairedPerItem = Math.min(result.getDamageValue(), result.getMaxDamage() / 4);
                if (repairedPerItem <= 0) return AnvilOperation.EMPTY;
                while (repairedPerItem > 0 && modifierCost < modifier.getCount()) {
                    result.setDamageValue(result.getDamageValue() - repairedPerItem);
                    addedCost++;
                    modifierCost++;
                    repairedPerItem = Math.min(result.getDamageValue(), result.getMaxDamage() / 4);
                }
            } else {
                if (!storedBook && (!result.is(modifier.getItem()) || !result.isDamageableItem())) {
                    return AnvilOperation.EMPTY;
                }

                if (result.isDamageableItem() && !storedBook) {
                    int firstRemaining = input.getMaxDamage() - input.getDamageValue();
                    int secondRemaining = modifier.getMaxDamage() - modifier.getDamageValue();
                    int combinedRemaining = firstRemaining + secondRemaining + result.getMaxDamage() * 12 / 100;
                    int combinedDamage = Math.max(0, result.getMaxDamage() - combinedRemaining);
                    if (combinedDamage < result.getDamageValue()) {
                        result.setDamageValue(combinedDamage);
                        addedCost += 2;
                    }
                }

                ItemEnchantments modifierEnchantments = EnchantmentHelper.getEnchantmentsForCrafting(modifier);
                boolean appliedAny = false;
                boolean rejectedAny = false;
                for (Object2IntMap.Entry<Holder<Enchantment>> entry : modifierEnchantments.entrySet()) {
                    Holder<Enchantment> enchantment = entry.getKey();
                    int currentLevel = enchantments.getLevel(enchantment);
                    int incomingLevel = entry.getIntValue();
                    int mergedLevel = currentLevel == incomingLevel
                            ? incomingLevel + 1 : Math.max(incomingLevel, currentLevel);
                    boolean compatible = input.supportsEnchantment(enchantment);
                    for (Holder<Enchantment> existing : enchantments.keySet()) {
                        if (!existing.equals(enchantment)
                                && !Enchantment.areCompatible(enchantment, existing)) {
                            compatible = false;
                            addedCost++;
                        }
                    }

                    if (!compatible) {
                        rejectedAny = true;
                        continue;
                    }
                    appliedAny = true;
                    mergedLevel = Math.min(mergedLevel, enchantment.value().getMaxLevel());
                    enchantments.set(enchantment, mergedLevel);
                    int enchantmentCost = enchantment.value().getAnvilCost();
                    if (storedBook) enchantmentCost = Math.max(1, enchantmentCost / 2);
                    addedCost += enchantmentCost * mergedLevel;
                    if (input.getCount() > 1) addedCost = 40;
                }
                if (rejectedAny && !appliedAny) return AnvilOperation.EMPTY;
                modifierCost = 1;
            }
        }

        if (requestedName != null && !StringUtil.isBlank(requestedName)) {
            if (!requestedName.equals(input.getHoverName().getString())) {
                renameCost = 1;
                addedCost++;
                result.set(DataComponents.CUSTOM_NAME, Component.literal(requestedName));
            }
        } else if (requestedName != null && input.has(DataComponents.CUSTOM_NAME)) {
            renameCost = 1;
            addedCost++;
            result.remove(DataComponents.CUSTOM_NAME);
        }
        if (storedBook && !result.isBookEnchantable(modifier)) return AnvilOperation.EMPTY;
        int levelCost = (int)Mth.clamp(baseCost + addedCost, 0L, Integer.MAX_VALUE);
        if (renameCost == addedCost && levelCost >= 40) levelCost = 39;
        if (addedCost <= 0 || levelCost >= 40) return AnvilOperation.EMPTY;

        int repairCost = result.getOrDefault(DataComponents.REPAIR_COST, 0);
        repairCost = Math.max(repairCost, modifier.getOrDefault(DataComponents.REPAIR_COST, 0));
        if (renameCost != addedCost || renameCost == 0) {
            repairCost = net.minecraft.world.inventory.AnvilMenu.calculateIncreasedRepairCost(repairCost);
        }
        result.set(DataComponents.REPAIR_COST, repairCost);
        EnchantmentHelper.setEnchantments(result, enchantments.toImmutable());
        return new AnvilOperation(result, levelCost, Math.max(1, modifierCost));
    }

    private record AnvilOperation(ItemStack output, int levelCost, int modifierCost) {
        private static final AnvilOperation EMPTY = new AnvilOperation(ItemStack.EMPTY, 0, 0);
    }

    private void tickPaintMixer() {
        if (paintProgress <= 0) return;
        paintProgress--;
        if (paintProgress == 0) {
            ItemStack target = inventory.getStackInSlot(0);
            if (isPaintTarget(target)) {
                PaintBrushItem.setRgbColor(target, paintResultColor);
                inventory.setStackInSlot(0, target);
            }
            setChanged();
        } else if (paintProgress % 20 == 0) {
            setChanged();
        }
    }

    private static boolean isPaintTarget(ItemStack target) {
        return target.is(ModItems.PAINTBRUSH.get()) || target.is(ModItems.SMALL_PAINTBRUSH.get())
                || target.is(ModBlocks.PAINT_CAN.asItem());
    }

    private static boolean isDrawingInput(ItemStack stack) {
        return stack.is(ModItems.UNPREPARED_STENCIL.get()) || stack.is(ModItems.STENCIL.get());
    }

    private static boolean[] requiredPaintDyes(int rgb) {
        int red = rgb >> 16 & 0xFF;
        int green = rgb >> 8 & 0xFF;
        int blue = rgb & 0xFF;
        int black = 255 - Math.max(red, Math.max(green, blue));
        int cyan = 255 - red - black;
        int magenta = 255 - green - black;
        int yellow = 255 - blue - black;
        return new boolean[] {cyan > 12, magenta > 12, yellow > 12, black > 12};
    }

    private boolean isDrawingTable() {
        return getBlockState().getBlock() instanceof UtilityMachineBlock block
                && block.kind() == UtilityMachineBlock.Kind.DRAWING_TABLE;
    }

    private boolean isProjector() {
        return getBlockState().getBlock() instanceof UtilityMachineBlock block
                && block.kind() == UtilityMachineBlock.Kind.PROJECTOR;
    }

    private void refreshProjector() {
        if (!isProjector() || updatingProjectorMap || level == null) return;
        ItemStack stack = inventory.getStackInSlot(0);
        if (!level.isClientSide && stack.is(ModItems.EMPTY_MAP.get())) {
            updatingProjectorMap = true;
            inventory.setStackInSlot(0, HeightMapItem.create(level, worldPosition));
            updatingProjectorMap = false;
            stack = inventory.getStackInSlot(0);
        }
        boolean active = stack.is(ModItems.HEIGHT_MAP.get())
                && HeightMapItem.getHeights(stack).length == 81;
        BlockState state = getBlockState();
        if (!level.isClientSide && state.getValue(UtilityMachineBlock.POWERED) != active) {
            level.setBlock(worldPosition, state.setValue(UtilityMachineBlock.POWERED, active),
                    net.minecraft.world.level.block.Block.UPDATE_ALL);
        }
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    private boolean isAutoAnvil() {
        return getBlockState().getBlock() instanceof UtilityMachineBlock block
                && block.kind() == UtilityMachineBlock.Kind.AUTO_ANVIL;
    }

    private void refreshDrawingOutput() {
        if (!isDrawingTable() || updatingDrawingOutput) return;
        ItemStack input = inventory.getStackInSlot(0);
        ItemStack output = ItemStack.EMPTY;
        if (isDrawingInput(input)) {
            output = drawingMode == 0 ? StencilItem.createPatternStack(selectedStencil)
                    : GlyphItem.createStack(GlyphItem.selectableCharacter(selectedGlyph));
            output.setCount(input.getCount());
        }
        ItemStack current = inventory.getStackInSlot(1);
        if (ItemStack.isSameItemSameComponents(current, output) && current.getCount() == output.getCount()) return;
        updatingDrawingOutput = true;
        inventory.setStackInSlot(1, output);
        updatingDrawingOutput = false;
    }

    private void sprinkle(ServerLevel level, BlockPos pos, BlockState state) {
        boolean enabled = waterTank.getFluidAmount() > 0;
        if (state.getValue(UtilityMachineBlock.POWERED) != enabled) {
            level.setBlock(pos, state.setValue(UtilityMachineBlock.POWERED, enabled), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        }
        if (!enabled) return;
        waterTank.drain(1, IFluidHandler.FluidAction.EXECUTE);
        level.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5D, pos.getY() + 0.7D, pos.getZ() + 0.5D,
                12, 0.65D, 0.25D, 0.65D, 0.08D);
        int boneMealSlot = -1;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            if (inventory.getStackInSlot(slot).is(Items.BONE_MEAL)) {
                boneMealSlot = slot;
                break;
            }
        }
        boolean boosted = boneMealSlot >= 0;
        if (!boosted && level.random.nextInt(20) != 0) return;
        boolean grew = false;
        for (int attempt = 0; attempt < 12 && !grew; attempt++) {
            BlockPos target = pos.offset(level.random.nextInt(9) - 4, level.random.nextInt(3) - 1,
                    level.random.nextInt(9) - 4);
            grew = BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), level, target);
        }
        if (grew && boosted) inventory.extractItem(boneMealSlot, 1, false);
    }

    private void project(ServerLevel level, BlockPos pos, BlockState state) {
        refreshProjector();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        maximumEnchantLevel = tag.contains("MaximumEnchantLevel")
                ? Math.clamp(tag.getInt("MaximumEnchantLevel"), 1, MAX_SAFE_EXPERIENCE_LEVEL)
                : DEFAULT_AUTO_ENCHANT_MAX_LEVEL;
        int savedCapacity = ModFluids.xpToFluid(ModFluids.experienceForLevel(maximumEnchantLevel));
        if (savedCapacity > experienceTank.getCapacity()) experienceTank.setCapacity(savedCapacity);
        experienceTank.readFromNBT(registries, tag.getCompound("ExperienceTank"));
        waterTank.readFromNBT(registries, tag.getCompound("WaterTank"));
        ticks = tag.getInt("Ticks");
        int savedSelection = tag.contains("SelectedEnchantLevel")
                ? tag.getInt("SelectedEnchantLevel")
                : (tag.contains("EnchantPowerLimit") ? tag.getInt("EnchantPowerLimit") : 20);
        selectedEnchantLevel = Math.clamp(savedSelection, 1, maximumEnchantLevel);
        availableEnchantPower = Math.max(0, tag.getInt("AvailableEnchantPower"));
        if (tag.contains("EnchantmentSeed")) enchantmentSeed = tag.getLong("EnchantmentSeed");
        anvilItemName = tag.contains("AnvilItemName") ? tag.getString("AnvilItemName") : null;
        selectedPaintColor = tag.contains("SelectedPaintColor")
                ? tag.getInt("SelectedPaintColor") & 0xFFFFFF : 0xB0417E;
        paintResultColor = tag.contains("PaintResultColor")
                ? tag.getInt("PaintResultColor") & 0xFFFFFF : selectedPaintColor;
        paintProgress = Math.clamp(tag.getInt("PaintProgress"), 0, PAINT_MIX_TICKS);
        drawingMode = Math.clamp(tag.getInt("DrawingMode"), 0, 1);
        selectedStencil = Math.floorMod(tag.getInt("SelectedStencil"), StencilItem.PATTERN_COUNT);
        selectedGlyph = Math.floorMod(tag.contains("SelectedGlyph") ? tag.getInt("SelectedGlyph")
                : GlyphItem.selectableIndex('A'), GlyphItem.selectableCount());
        projectorRotation = Math.floorMod(tag.getInt("ProjectorRotation"), 4);
        if (tag.contains("SideMasks")) {
            int[] savedMasks = tag.getIntArray("SideMasks");
            for (int index = 0; index < Math.min(savedMasks.length, sideMasks.length); index++) {
                sideMasks[index] = savedMasks[index] & 0x3F;
            }
        }
        automaticChannels = tag.getInt("AutomaticChannels") & 0xF;
        refreshDrawingOutput();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", inventory.serializeNBT(registries));
        tag.put("ExperienceTank", experienceTank.writeToNBT(registries, new CompoundTag()));
        tag.put("WaterTank", waterTank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("Ticks", ticks);
        tag.putInt("SelectedEnchantLevel", selectedEnchantLevel);
        tag.putInt("MaximumEnchantLevel", maximumEnchantLevel);
        tag.putInt("AvailableEnchantPower", availableEnchantPower);
        tag.putLong("EnchantmentSeed", enchantmentSeed);
        if (anvilItemName != null) tag.putString("AnvilItemName", anvilItemName);
        tag.putInt("SelectedPaintColor", selectedPaintColor);
        tag.putInt("PaintResultColor", paintResultColor);
        tag.putInt("PaintProgress", paintProgress);
        tag.putInt("DrawingMode", drawingMode);
        tag.putInt("SelectedStencil", selectedStencil);
        tag.putInt("SelectedGlyph", selectedGlyph);
        tag.putInt("ProjectorRotation", projectorRotation);
        tag.putIntArray("SideMasks", sideMasks);
        tag.putInt("AutomaticChannels", automaticChannels);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!level.isClientSide && isProjector()) refreshProjector();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
