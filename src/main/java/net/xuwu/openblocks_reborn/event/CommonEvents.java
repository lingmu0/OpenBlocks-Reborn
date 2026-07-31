package net.xuwu.openblocks_reborn.event;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.TickEvent;
import net.xuwu.openblocks_reborn.block.ElevatorBlock;
import net.xuwu.openblocks_reborn.blockentity.GraveBlockEntity;
import net.xuwu.openblocks_reborn.config.OpenBlocksConfig;
import net.xuwu.openblocks_reborn.item.DevNullItem;
import net.xuwu.openblocks_reborn.item.PaintBrushItem;
import net.xuwu.openblocks_reborn.item.DevNullItemHandler;
import net.xuwu.openblocks_reborn.registry.ModBlocks;
import net.xuwu.openblocks_reborn.registry.ModItems;
import net.xuwu.openblocks_reborn.block.UtilityMachineBlock;
import net.xuwu.openblocks_reborn.block.ItemMachineBlock;
import net.xuwu.openblocks_reborn.blockentity.UtilityMachineBlockEntity;
import net.xuwu.openblocks_reborn.blockentity.ExperienceBlockEntity;
import net.xuwu.openblocks_reborn.blockentity.BigButtonBlockEntity;
import net.xuwu.openblocks_reborn.blockentity.VacuumHopperBlockEntity;
import net.xuwu.openblocks_reborn.blockentity.TankBlockEntity;
import net.xuwu.openblocks_reborn.blockentity.CanvasBlockEntity;
import net.xuwu.openblocks_reborn.blockentity.TrophyBlockEntity;
import net.xuwu.openblocks_reborn.menu.MenuHelper;
import net.xuwu.openblocks_reborn.menu.MachineLayout;
import net.xuwu.openblocks_reborn.entity.LuggageEntity;
import net.xuwu.openblocks_reborn.registry.ModEntities;
import net.xuwu.openblocks_reborn.registry.ModEnchantments;
import net.xuwu.openblocks_reborn.registry.ModFluids;
import net.xuwu.openblocks_reborn.item.SleepingBagItem;
import net.xuwu.openblocks_reborn.item.HangGliderItem;
import net.xuwu.openblocks_reborn.item.CraneBackpackItem;
import net.xuwu.openblocks_reborn.item.CraneControlItem;
import net.xuwu.openblocks_reborn.item.CursorItem;
import net.xuwu.openblocks_reborn.entity.GlyphEntity;
import net.xuwu.openblocks_reborn.entity.MiniMeEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.core.Direction;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.List;

public final class CommonEvents {
    private static final String ELEVATOR_COOLDOWN = "openblocks_reborn.elevator_cooldown";
    private static final String FLIM_FLAM_LUCK = "openblocks_reborn.flim_flam_luck";
    private static final String FLIM_FLAM_COOLDOWN = "openblocks_reborn.flim_flam_cooldown";
    private static final String EXPLOSION_GUARD = "openblocks_reborn.explosion_guard";
    private static final String SLEEPING_BAG_ACTIVE = "openblocks_reborn.sleeping_bag_active";
    private static boolean smokeInitialized;
    private static int smokeTicks;
    private static BlockPos smokeBase;
    private static LuggageEntity smokeLuggage;

    @SubscribeEvent
    public void onEquipmentChanged(LivingEquipmentChangeEvent event) {
        if (event.getSlot() == EquipmentSlot.CHEST
                && event.getEntity() instanceof net.minecraft.world.entity.player.Player player
                && event.getFrom().getItem() instanceof CraneBackpackItem) {
            CraneBackpackItem.releaseCarried(player, event.getFrom());
        }
    }

    @SubscribeEvent
    public void onContainerClosed(PlayerContainerEvent.Close event) {
        CursorItem.clearRemoteMenu(event.getEntity());
    }

    @SubscribeEvent
    public void onCraneControlEntityInteract(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!event.getItemStack().is(ModItems.CRANE_CONTROL.get())) return;
        event.setCancellationResult(CraneControlItem.beginUsing(
                event.getLevel(), event.getEntity(), event.getHand()));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onCraneControlEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!event.getItemStack().is(ModItems.CRANE_CONTROL.get())) return;
        event.setCancellationResult(CraneControlItem.beginUsing(
                event.getLevel(), event.getEntity(), event.getHand()));
        event.setCanceled(true);
    }

    /**
     * Reserve right click for the controller before the target block sees it.
     * onItemUseFirst is kept as the item-side fallback, while this high-priority
     * event also covers blocks whose Forge interaction hooks run unusually
     * early.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onCraneControlBlockInteract(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getItemStack().is(ModItems.CRANE_CONTROL.get())) return;
        event.setCancellationResult(CraneControlItem.beginUsing(
                event.getLevel(), event.getEntity(), event.getHand()));
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlayerDrops(LivingDropsEvent event) {
        if (!OpenBlocksConfig.GENERATE_GRAVES.get()
                || !(event.getEntity() instanceof ServerPlayer player)
                || event.getDrops().isEmpty()) return;
        var level = player.serverLevel();
        BlockPos origin = player.blockPosition();
        BlockPos gravePos = null;
        for (int y = 0; y <= 4; y++) {
            BlockPos candidate = origin.above(y);
            if (level.getBlockState(candidate).canBeReplaced()) {
                gravePos = candidate;
                break;
            }
        }
        if (gravePos == null) return;
        if (level.setBlock(gravePos, ModBlocks.GRAVE.get().defaultBlockState(), 3)
                && level.getBlockEntity(gravePos) instanceof GraveBlockEntity grave) {
            grave.setOwner(player.getUUID(), player.getGameProfile().getName());
            grave.store(event.getDrops().stream().map(drop -> drop.getItem().copy()).toList());
            event.getDrops().clear();
        }
    }

    @SubscribeEvent
    public void onTrophyDrop(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)
                || event.getEntity() instanceof net.minecraft.world.entity.player.Player
                || event.getEntity().getType() == ModEntities.MINI_ME.get()) return;
        int looting = net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                net.minecraft.world.item.enchantment.Enchantments.MOB_LOOTING,
                killer.getMainHandItem());
        float chance = 0.0025F + looting * 0.0025F;
        if (killer.getRandom().nextFloat() >= chance) return;
        ItemStack trophy = TrophyBlockEntity.createItem(
                BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType()));
        event.getDrops().add(new ItemEntity(event.getEntity().level(), event.getEntity().getX(),
                event.getEntity().getY(), event.getEntity().getZ(), trophy));
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onItemPickup(EntityItemPickupEvent event) {
        ItemStack incoming = event.getItem().getItem();
        if (incoming.isEmpty()) return;
        var inventory = event.getEntity().getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack container = inventory.getItem(slot);
            if (container.getItem() instanceof DevNullItem devNull && devNull.absorb(container, incoming)) {
                event.getItem().discard();
                event.setCanceled(true);
                return;
            }
        }
    }

    @SubscribeEvent
    public void onLivingJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BlockPos below = player.blockPosition().below();
            if (player.level().getBlockState(below).getBlock() instanceof ElevatorBlock elevator && elevator.teleport(player, true)) {
                player.getPersistentData().putInt(ELEVATOR_COOLDOWN, 10);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (Boolean.getBoolean("openblocks_reborn.smokeTest")) runSmokeTest(player);
        CompoundTag data = player.getPersistentData();
        int cooldown = data.getInt(ELEVATOR_COOLDOWN);
        if (cooldown > 0) data.putInt(ELEVATOR_COOLDOWN, cooldown - 1);

        HangGliderItem.ensureGlider(player);

        if (cooldown == 0 && player.isShiftKeyDown() && player.onGround()) {
            BlockPos below = player.blockPosition().below();
            if (player.level().getBlockState(below).getBlock() instanceof ElevatorBlock elevator && elevator.teleport(player, false)) {
                data.putInt(ELEVATOR_COOLDOWN, 10);
            }
        }
        tickFlimFlam(player);
        if (!player.isSleeping() && data.getBoolean(SLEEPING_BAG_ACTIVE)) data.remove(SLEEPING_BAG_ACTIVE);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLastStand(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        int levels = armorEnchantLevels(player, ModEnchantments.LAST_STAND);
        if (levels <= 0 || player.getHealth() - event.getAmount() >= 1.0F) return;
        int required = Math.max(1, (int)Math.ceil((1.0F - (player.getHealth() - event.getAmount())) * 50.0F / levels));
        if (player.totalExperience < required) return;
        player.giveExperiencePoints(-required);
        player.setHealth(1.0F);
        event.setAmount(0.0F);
        player.level().playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.7F, 1.25F);
    }

    @SubscribeEvent
    public void onUnstableFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getDistance() <= 4.0F || player.isShiftKeyDown()) return;
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        int level = ModEnchantments.level(player, boots, ModEnchantments.UNSTABLE);
        if (level <= 0 || !consumeGunpowder(player, gunpowderFor(level))) return;
        damageArmor(boots);
        event.setCanceled(true);
        player.fallDistance = 0.0F;
        createArmorExplosion(player, level);
        Vec3 movement = player.getDeltaMovement();
        player.setDeltaMovement(movement.x * (1.0D + level * 0.35D), 0.55D + level * 0.18D,
                movement.z * (1.0D + level * 0.35D));
        player.hurtMarked = true;
    }

    @SubscribeEvent
    public void onUnstableHit(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getSource().getEntity() == null
                || player.getPersistentData().getBoolean(EXPLOSION_GUARD)) return;
        int level = 0;
        ItemStack selected = ItemStack.EMPTY;
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS}) {
            ItemStack armor = player.getItemBySlot(slot);
            int candidate = ModEnchantments.level(player, armor, ModEnchantments.UNSTABLE);
            if (candidate > level) {
                level = candidate;
                selected = armor;
            }
        }
        if (level <= 0 || !consumeGunpowder(player, gunpowderFor(level))) return;
        damageArmor(selected);
        createArmorExplosion(player, level);
    }

    @SubscribeEvent
    public void onFlimFlamAttack(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer target)
                || !(event.getSource().getEntity() instanceof ServerPlayer attacker) || attacker == target) return;
        int weapon = ModEnchantments.level(attacker, attacker.getMainHandItem(), ModEnchantments.FLIM_FLAM);
        int armor = armorEnchantLevels(target, ModEnchantments.FLIM_FLAM) / 3;
        int difference = armor - weapon;
        if (difference == 0) return;
        ServerPlayer victim = difference > 0 ? attacker : target;
        int penalty = 0;
        for (int i = 0; i < Math.abs(difference); i++) penalty += victim.getRandom().nextInt(20) + 1;
        victim.getPersistentData().putInt(FLIM_FLAM_LUCK,
                victim.getPersistentData().getInt(FLIM_FLAM_LUCK) - penalty);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void shareMiniMeDamage(LivingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        LivingEntity partner = miniMePartner(entity);
        float damage = event.getAmount();
        if (partner == null || !partner.isAlive() || damage <= 0.0F) return;

        float shared = damage * 0.5F;
        // LivingDamageEvent is Forge 1.20.1's final, still-mutable damage
        // stage. Keep half on the recipient and transfer the same final amount
        // directly so the companion does not run a second armor pipeline.
        event.setAmount(shared);
        applyLinkedFinalDamage(partner, event.getSource(), shared);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void shareMiniMeHealing(LivingHealEvent event) {
        LivingEntity entity = event.getEntity();
        LivingEntity partner = miniMePartner(entity);
        if (partner == null || !partner.isAlive() || event.getAmount() <= 0.0F) return;

        float finalHealing = Math.min(event.getAmount(), entity.getMaxHealth() - entity.getHealth());
        float shared = finalHealing * 0.5F;
        event.setAmount(shared);
        partner.setHealth(Math.min(partner.getMaxHealth(), partner.getHealth() + shared));
    }

    private static LivingEntity miniMePartner(LivingEntity entity) {
        if (entity instanceof MiniMeEntity miniMe) return miniMe.getOwnerPlayer();
        if (entity instanceof ServerPlayer player) return MiniMeEntity.findOwned(player);
        return null;
    }

    private static void applyLinkedFinalDamage(LivingEntity entity,
                                               net.minecraft.world.damagesource.DamageSource source,
                                               float amount) {
        entity.setHealth(Math.max(0.0F, entity.getHealth() - amount));
        if (entity.getHealth() <= 0.0F) {
            entity.die(source);
        } else {
            entity.hurtDuration = 10;
            entity.hurtTime = 10;
            entity.level().broadcastEntityEvent(entity, (byte)2);
        }
    }

    private static int armorEnchantLevels(ServerPlayer player,
            net.minecraftforge.registries.RegistryObject<net.minecraft.world.item.enchantment.Enchantment> key) {
        int result = 0;
        for (ItemStack stack : player.getArmorSlots()) result += ModEnchantments.level(player, stack, key);
        return result;
    }

    private static int gunpowderFor(int level) {
        return level >= 3 ? 4 : level;
    }

    private static boolean consumeGunpowder(ServerPlayer player, int amount) {
        if (player.getAbilities().instabuild) return true;
        int found = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(Items.GUNPOWDER)) found += stack.getCount();
        }
        if (found < amount) return false;
        for (int slot = 0; slot < player.getInventory().getContainerSize() && amount > 0; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.is(Items.GUNPOWDER)) continue;
            int removed = Math.min(amount, stack.getCount());
            stack.shrink(removed);
            amount -= removed;
        }
        return true;
    }

    private static void damageArmor(ItemStack armor) {
        if (!armor.isDamageableItem()) return;
        int damage = armor.getDamageValue() + 1;
        if (damage >= armor.getMaxDamage()) armor.shrink(1);
        else armor.setDamageValue(damage);
    }

    private static void createArmorExplosion(ServerPlayer player, int level) {
        CompoundTag data = player.getPersistentData();
        data.putBoolean(EXPLOSION_GUARD, true);
        int oldInvulnerable = player.invulnerableTime;
        player.invulnerableTime = Math.max(oldInvulnerable, 10);
        try {
            player.serverLevel().explode(player, player.getX(), player.getY(), player.getZ(),
                    1.0F + level, false, level >= 3 ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE);
        } finally {
            data.remove(EXPLOSION_GUARD);
        }
    }

    private static void tickFlimFlam(ServerPlayer player) {
        if (player.tickCount % 20 != 0) return;
        CompoundTag data = player.getPersistentData();
        int cooldown = data.getInt(FLIM_FLAM_COOLDOWN);
        if (cooldown > 0) {
            data.putInt(FLIM_FLAM_COOLDOWN, Math.max(0, cooldown - 20));
            return;
        }
        int luck = data.getInt(FLIM_FLAM_LUCK);
        if (luck > -30 || player.getRandom().nextInt(4) != 0) return;
        applyFlimFlam(player);
        data.putInt(FLIM_FLAM_LUCK, Math.min(0, luck + 20));
        data.putInt(FLIM_FLAM_COOLDOWN, 300);
    }

    private static void applyFlimFlam(ServerPlayer player) {
        switch (player.getRandom().nextInt(5)) {
            case 0 -> {
                int first = player.getRandom().nextInt(9);
                int second = player.getRandom().nextInt(9);
                ItemStack held = player.getInventory().getItem(first);
                player.getInventory().setItem(first, player.getInventory().getItem(second));
                player.getInventory().setItem(second, held);
            }
            case 1 -> {
                player.setDeltaMovement(player.getDeltaMovement().add(0.0D, 0.9D, 0.0D));
                player.hurtMarked = true;
            }
            case 2 -> {
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 0));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
            }
            case 3 -> {
                ItemStack stack = player.getMainHandItem();
                if (!stack.isEmpty()) stack.setHoverName(Component.literal("Definitely Not " + stack.getHoverName().getString()));
            }
            case 4 -> player.randomTeleport(player.getX() + player.getRandom().nextInt(13) - 6,
                    player.getY(), player.getZ() + player.getRandom().nextInt(13) - 6, true);
            default -> { }
        }
        player.displayClientMessage(Component.translatable("message.openblocks_reborn.flim_flammed"), false);
        player.level().playSound(null, player.blockPosition(), SoundEvents.CHICKEN_EGG, SoundSource.PLAYERS, 1.0F, 0.6F);
    }

    private static void runSmokeTest(ServerPlayer player) {
        if (!smokeInitialized) setupSmokeGallery(player);
        smokeTicks++;
        if (smokeBase == null) return;
        if (Boolean.getBoolean("openblocks_reborn.cursorSmokeTest")) {
            if (smokeTicks == 1) {
                BlockPos target = smokeBase.offset(6, 15, 4);
                player.serverLevel().setBlock(target, ModBlocks.CANNON.get().defaultBlockState()
                        .setValue(ItemMachineBlock.FACING, Direction.NORTH), Block.UPDATE_ALL);
                player.teleportTo(player.serverLevel(), target.getX() + 20.5D,
                        target.getY() + 1.0D, target.getZ() + 0.5D,
                        90.0F, 0.0F);
                ItemStack cursor = new ItemStack(ModItems.CURSOR.get());
                CursorItem.bind(cursor, player.serverLevel(), target, Direction.UP);
                player.getInventory().setItem(0, cursor);
                player.getInventory().selected = 0;
                ModItems.CURSOR.get().use(player.serverLevel(), player, InteractionHand.MAIN_HAND);
            }
            return;
        }
        if (Boolean.getBoolean("openblocks_reborn.guideSmokeTest")) {
            if (smokeTicks == 1) {
                BlockPos guidePos = smokeBase.offset(6, 15, 4);
                player.serverLevel().setBlock(guidePos,
                        ModBlocks.GUIDE.get().defaultBlockState(), Block.UPDATE_ALL);
                player.serverLevel().setBlock(guidePos.below(),
                        Blocks.REDSTONE_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
                moveSmokeCamera(player, smokeBase.offset(6, 20, 13), guidePos);
            }
            return;
        }
        if (Boolean.getBoolean("openblocks_reborn.modelSmokeTest")) {
            switch (smokeTicks) {
                case 1 -> {
                    setupModelCloseup(player);
                    player.getInventory().setItem(0, new ItemStack(ModBlocks.CANNON.get().asItem()));
                    player.getAbilities().flying = true;
                    player.onUpdateAbilities();
                    moveSmokeCamera(player, smokeBase.offset(6, 19, 11), smokeBase.offset(6, 16, 4));
                }
                case 50 -> {
                    player.getInventory().setItem(0, new ItemStack(ModItems.CRANE_CONTROL.get()));
                    moveSmokeCamera(player, smokeBase.offset(13, 17, 4),
                            smokeBase.offset(6, 16, 4));
                }
                default -> { }
            }
            return;
        }
        switch (smokeTicks) {
            // Give the client enough time to finish JEI/Jade startup before opening the
            // first menu, otherwise the auto-anvil screen can be replaced before its
            // first stable rendered frame on slower development launches.
            case 80 -> openUtility(player, "auto_anvil");
            case 110 -> openUtility(player, "auto_enchantment_table");
            case 140 -> openUtility(player, "paint_mixer");
            case 170 -> openUtility(player, "drawing_table");
            case 200 -> openUtility(player, "sprinkler");
            case 230 -> openUtility(player, "projector");
            case 260 -> openButton(player);
            case 290 -> openItemMachine(player, "block_placer", MachineLayout.BLOCK_PLACER,
                    "container.openblocks_reborn.placer");
            case 320 -> openVacuum(player);
            case 350 -> openBottler(player);
            case 380 -> openLuggage(player);
            case 410 -> openUtility(player, "donation_station");
            case 440 -> openItemMachine(player, "item_dropper", MachineLayout.ITEM_DROPPER,
                    "container.openblocks_reborn.dropper");
            case 470 -> openItemMachine(player, "cannon", MachineLayout.CANNON,
                    "container.openblocks_reborn.cannon");
            case 500 -> openDevNull(player);
            case 530 -> {
                player.closeContainer();
                player.getInventory().setItem(0, new ItemStack(ModItems.GOLDEN_EYE.get()));
                player.getInventory().selected = 0;
                player.setShiftKeyDown(true);
                ModItems.GOLDEN_EYE.get().use(player.serverLevel(), player, InteractionHand.MAIN_HAND);
                player.setShiftKeyDown(false);
            }
            case 560 -> {
                player.closeContainer();
                player.getInventory().setItem(0, TrophyBlockEntity.createItem(
                        net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                                .getKey(net.minecraft.world.entity.EntityType.CREEPER)));
                player.teleportTo(player.serverLevel(), smokeBase.getX() + 6.5D, smokeBase.getY() + 10.0D,
                        smokeBase.getZ() + 22.5D, 180.0F, 24.0F);
                player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,
                        smokeBase.offset(6, 1, 5).getCenter());
            }
            case 590 -> moveSmokeCamera(player, smokeBase.offset(6, 18, 12), smokeBase.offset(6, 0, 5));
            case 620 -> moveSmokeCamera(player, smokeBase.offset(-7, 5, 5), smokeBase.offset(6, 1, 5));
            case 650 -> moveSmokeCamera(player, smokeBase.offset(20, 5, 5), smokeBase.offset(6, 1, 5));
            case 680 -> {
                player.getInventory().setItem(0, new ItemStack(ModBlocks.CANNON.get().asItem()));
                moveSmokeCamera(player, smokeBase.offset(6, 6, 12), smokeBase.offset(6, 6, -3));
            }
            case 710 -> moveSmokeCamera(player, findGalleryBlock("vacuum_hopper").above(6),
                    findGalleryBlock("vacuum_hopper"));
            default -> { }
        }
    }

    private static void setupModelCloseup(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        for (int x = 2; x <= 10; x++) {
            for (int z = 2; z <= 6; z++) {
                level.setBlock(smokeBase.offset(x, 14, z), Blocks.SMOOTH_STONE.defaultBlockState(),
                        Block.UPDATE_CLIENTS);
            }
        }

        BlockPos cannonPos = smokeBase.offset(4, 15, 4);
        level.setBlock(cannonPos, ModBlocks.CANNON.get().defaultBlockState()
                .setValue(ItemMachineBlock.FACING, Direction.NORTH), Block.UPDATE_ALL);

        BlockPos ropeTop = smokeBase.offset(7, 18, 4);
        BlockState ropeState = ModBlocks.ROPE_LADDER.get().defaultBlockState()
                .setValue(net.xuwu.openblocks_reborn.block.RopeLadderBlock.FACING, Direction.NORTH);
        level.setBlock(ropeTop.south(), Blocks.SMOOTH_STONE.defaultBlockState(), Block.UPDATE_ALL);
        for (int distance = 0; distance < 4; distance++) {
            level.setBlock(ropeTop.below(distance), ropeState, Block.UPDATE_ALL);
        }

        level.setBlock(smokeBase.offset(8, 15, 4), ModBlocks.LADDER.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.TrapDoorBlock.FACING, Direction.NORTH)
                .setValue(net.minecraft.world.level.block.TrapDoorBlock.OPEN, false), Block.UPDATE_ALL);
        level.setBlock(smokeBase.offset(9, 15, 4), ModBlocks.LADDER.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.TrapDoorBlock.FACING, Direction.NORTH)
                .setValue(net.minecraft.world.level.block.TrapDoorBlock.OPEN, true), Block.UPDATE_ALL);

        net.minecraft.world.entity.decoration.ArmorStand wearableStand =
                new net.minecraft.world.entity.decoration.ArmorStand(level,
                        smokeBase.getX() + 6.5D, smokeBase.getY() + 15.0D, smokeBase.getZ() + 4.5D);
        wearableStand.setYRot(180.0F);
        wearableStand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,
                new ItemStack(ModItems.PENCIL_GLASSES.get()));
        wearableStand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,
                new ItemStack(ModItems.CRANE_BACKPACK.get()));
        level.addFreshEntity(wearableStand);

        ItemStack playerCrane = new ItemStack(ModItems.CRANE_BACKPACK.get());
        // Keep all three magnet tiers visible in the automated first/third-person
        // screenshots instead of placing the head below the gallery platform.
        CraneBackpackItem.changeLength(playerCrane, 1.30D);
        player.setItemSlot(EquipmentSlot.CHEST, playerCrane);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.PENCIL_GLASSES.get()));
    }

    private static void moveSmokeCamera(ServerPlayer player, BlockPos camera, BlockPos target) {
        player.teleportTo(player.serverLevel(), camera.getX() + 0.5D, camera.getY() + 0.5D,
                camera.getZ() + 0.5D, player.getYRot(), player.getXRot());
        player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, target.getCenter());
    }

    private static void setupSmokeGallery(ServerPlayer player) {
        smokeInitialized = true;
        player.setGameMode(GameType.CREATIVE);
        ServerLevel level = player.serverLevel();
        level.setDayTime(6000L);
        level.setWeatherParameters(0, 0, false, false);
        int originX = 0;
        int originZ = 0;
        int surface = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
                originX + 6, originZ + 6);
        // Repeated smoke runs reuse the quick-play world. Cap the platform below the cloud
        // layer and clear its test volume so an older gallery cannot raise WORLD_SURFACE
        // by another thirty blocks on every run.
        int y = net.minecraft.util.Mth.clamp(surface + 30, 100, 160);
        smokeBase = new BlockPos(originX, y, originZ);
        AABB smokeVolume = new AABB(Vec3.atLowerCornerOf(smokeBase.offset(-5, 0, -8)),
                Vec3.atLowerCornerOf(smokeBase.offset(20, 20, 15)));
        level.getEntitiesOfClass(ItemFrame.class, smokeVolume).forEach(ItemFrame::discard);
        level.getEntitiesOfClass(net.minecraft.world.entity.decoration.ArmorStand.class, smokeVolume)
                .forEach(net.minecraft.world.entity.decoration.ArmorStand::discard);
        level.getEntitiesOfClass(MiniMeEntity.class, smokeVolume).forEach(MiniMeEntity::discard);
        level.getEntitiesOfClass(LuggageEntity.class, smokeVolume).forEach(LuggageEntity::discard);
        level.getEntitiesOfClass(GlyphEntity.class, smokeVolume).forEach(GlyphEntity::discard);
        for (int clearX = -2; clearX <= 15; clearX++) {
            for (int clearZ = -2; clearZ <= 13; clearZ++) {
                for (int clearY = 0; clearY <= 36; clearY++) {
                    level.setBlock(smokeBase.offset(clearX, clearY, clearZ), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
        for (int floorX = -1; floorX <= 13; floorX++) {
            for (int floorZ = -1; floorZ <= 11; floorZ++) {
                level.setBlock(smokeBase.offset(floorX, -1, floorZ), Blocks.SMOOTH_STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        List<BlockPos> positions = new ArrayList<>();
        int index = 0;
        for (var entry : ModBlocks.ALL.entrySet()) {
            int x = index % 7;
            int z = index / 7;
            BlockPos pos = smokeBase.offset(x * 2, 0, z * 2);
            positions.add(pos);
            level.setBlock(pos.below(), Blocks.SMOOTH_STONE.defaultBlockState(), Block.UPDATE_ALL);
            BlockState state = entry.getValue().get().defaultBlockState();
            if (state.hasProperty(HorizontalDirectionalBlock.FACING)) {
                Direction facing = new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}[index % 4];
                state = state.setValue(HorizontalDirectionalBlock.FACING, facing);
            }
            if (state.hasProperty(ButtonBlock.FACE)) state = state.setValue(ButtonBlock.FACE, AttachFace.FLOOR);
            if (entry.getKey().equals("beartrap")) {
                state = state.setValue(net.xuwu.openblocks_reborn.block.BearTrapBlock.TRIGGERED, true);
            }
            if (entry.getKey().equals("projector")) {
                state = state.setValue(UtilityMachineBlock.POWERED, true);
            }
            level.setBlock(pos, state, Block.UPDATE_CLIENTS);
            index++;
        }
        for (BlockPos pos : positions) {
            if (level.getBlockEntity(pos) instanceof TankBlockEntity tank) {
                tank.getTank().fill(new FluidStack(Fluids.WATER, 12_000), net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            } else if (level.getBlockEntity(pos) instanceof CanvasBlockEntity canvas) {
                canvas.setColor(Direction.UP, 0xFFFFCC33);
                canvas.setColor(Direction.NORTH, 0xFF3366FF);
                canvas.setColor(Direction.SOUTH, 0xFFFF3366);
                for (int pixel = 0; pixel < CanvasBlockEntity.SIDE_SIZE; pixel++) {
                    canvas.setPixel(Direction.UP, pixel, pixel, 0xFFFF3366);
                    canvas.setPixel(Direction.UP, pixel, CanvasBlockEntity.SIDE_SIZE - 1 - pixel, 0xFF3366FF);
                }
            } else if (level.getBlockEntity(pos) instanceof TrophyBlockEntity trophy) {
                trophy.setEntityType(net.minecraft.world.entity.EntityType.CREEPER);
            } else if (level.getBlockEntity(pos) instanceof VacuumHopperBlockEntity hopper) {
                hopper.toggleItemOutput(Direction.NORTH);
                hopper.toggleItemOutput(Direction.EAST);
                hopper.toggleExperienceOutput(Direction.EAST);
                hopper.toggleExperienceOutput(Direction.WEST);
            }
        }
        BlockPos vacuumDisplay = findGalleryBlock("vacuum_hopper");
        level.setBlock(vacuumDisplay.north(), Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(vacuumDisplay.east(), Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(vacuumDisplay.west(), ModBlocks.TANK.get().defaultBlockState(), Block.UPDATE_ALL);
        BlockPos fluidDisplay = smokeBase.offset(13, 0, 9);
        level.setBlock(fluidDisplay.below(), Blocks.SMOOTH_STONE.defaultBlockState(), Block.UPDATE_ALL);
        for (Direction direction : new Direction[] {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            level.setBlock(fluidDisplay.relative(direction), Blocks.GLASS.defaultBlockState(), Block.UPDATE_ALL);
        }
        level.setBlock(fluidDisplay, ModFluids.XP_JUICE_BLOCK.get().defaultBlockState(), Block.UPDATE_ALL);
        BlockPos openTrap = smokeBase.offset(13, 0, 5);
        level.setBlock(openTrap.below(), Blocks.SMOOTH_STONE.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(openTrap, ModBlocks.BEARTRAP.get().defaultBlockState(), Block.UPDATE_ALL);
        BlockPos openJadedTrapdoor = smokeBase.offset(13, 0, 3);
        level.setBlock(openJadedTrapdoor.below(), Blocks.SMOOTH_STONE.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(openJadedTrapdoor, ModBlocks.LADDER.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.TrapDoorBlock.OPEN, true)
                .setValue(net.minecraft.world.level.block.TrapDoorBlock.FACING, Direction.NORTH),
                Block.UPDATE_ALL);
        BlockPos ropeTop = smokeBase.offset(13, 3, 1);
        BlockState ropeState = ModBlocks.ROPE_LADDER.get().defaultBlockState()
                .setValue(net.xuwu.openblocks_reborn.block.RopeLadderBlock.FACING, Direction.NORTH);
        for (int ropeY = 0; ropeY < 4; ropeY++) {
            BlockPos segment = ropeTop.below(ropeY);
            level.setBlock(segment.south(), Blocks.SMOOTH_STONE.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(segment, ropeState, Block.UPDATE_ALL);
        }
        BlockPos wallFlag = smokeBase.offset(13, 0, 7);
        level.setBlock(wallFlag.south(), Blocks.SMOOTH_STONE.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(wallFlag, ModBlocks.FLAG.get().defaultBlockState()
                .setValue(net.xuwu.openblocks_reborn.block.FlagBlock.FACE, AttachFace.WALL)
                .setValue(net.xuwu.openblocks_reborn.block.FlagBlock.FACING, Direction.NORTH)
                .setValue(net.xuwu.openblocks_reborn.block.FlagBlock.COLOR, net.minecraft.world.item.DyeColor.RED),
                Block.UPDATE_ALL);
        for (int glyphIndex = 0; glyphIndex < 2; glyphIndex++) {
            BlockPos support = smokeBase.offset(10 + glyphIndex, 2, 0);
            level.setBlock(support, Blocks.SMOOTH_STONE.defaultBlockState(), Block.UPDATE_ALL);
            GlyphEntity glyph = new GlyphEntity(level, support.south(), Direction.SOUTH,
                    glyphIndex == 0 ? 'O' : 'B', 8, 8);
            if (glyph.survives()) level.addFreshEntity(glyph);
        }
        List<ItemStack> displayedItems = new ArrayList<>();
        ModItems.BLOCK_ITEMS.values().forEach(item -> displayedItems.add(new ItemStack(item.get())));
        ModItems.ALL_ITEMS.values().forEach(item -> displayedItems.add(new ItemStack(item.get())));
        for (int itemIndex = 0; itemIndex < displayedItems.size(); itemIndex++) {
            ItemStack displayed = displayedItems.get(itemIndex);
            if (displayed.is(ModItems.PAINTBRUSH.get())) PaintBrushItem.setRgbColor(displayed, 0xE040A0);
            if (displayed.is(ModItems.GLYPH.get())) net.xuwu.openblocks_reborn.item.GlyphItem.setCharacter(displayed, 'G');
            if (displayed.is(ModItems.SLIMALYZER.get())) net.xuwu.openblocks_reborn.item.SlimalyzerItem.setActive(displayed, true);
            if (displayed.is(ModItems.DEV_NULL.get())) DevNullItem.setStored(displayed, new ItemStack(Items.COBBLESTONE, 64));
            int column = itemIndex % 10;
            int row = itemIndex / 10;
            BlockPos support = smokeBase.offset(2 + column, 10 - row, -4);
            level.setBlock(support, Blocks.SMOOTH_STONE.defaultBlockState(), Block.UPDATE_ALL);
            ItemFrame frame = new ItemFrame(level, support.south(), Direction.SOUTH);
            frame.setItem(displayed, false);
            if (frame.survives()) level.addFreshEntity(frame);
        }
        smokeLuggage = new LuggageEntity(ModEntities.LUGGAGE.get(), level);
        smokeLuggage.setOwner(player.getUUID());
        // Keep the smoke-test subject on its gallery plinth. A real luggage is
        // expected to follow the player, but the scripted camera flies beyond
        // the platform before its inventory menu is captured.
        smokeLuggage.setNoAi(true);
        smokeLuggage.setPos(smokeBase.getX() + 14.5D, smokeBase.getY(), smokeBase.getZ() + 4.5D);
        level.addFreshEntity(smokeLuggage);
        player.teleportTo(level, smokeBase.getX() + 6.5D, smokeBase.getY() + 10.0D,
                smokeBase.getZ() + 22.5D, 180.0F, 24.0F);
        player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,
                smokeBase.offset(6, 1, 5).getCenter());
        player.getInventory().add(new ItemStack(ModItems.INFO_BOOK.get()));
        player.getInventory().add(new ItemStack(ModItems.PAINTBRUSH.get()));
        player.getInventory().add(new ItemStack(ModItems.STENCIL.get()));
        player.getInventory().add(new ItemStack(ModItems.CRANE_CONTROL.get()));
        player.getInventory().add(new ItemStack(ModItems.SONIC_GLASSES.get()));
        player.getInventory().add(new ItemStack(ModFluids.XP_BUCKET.get()));
        player.getInventory().selected = 0;
        player.getInventory().setItem(0, net.xuwu.openblocks_reborn.item.GlyphItem.createStack('G'));
        net.xuwu.openblocks_reborn.OpenBlocksReborn.LOGGER.info("OpenBlocks smoke gallery created at {}", smokeBase);
    }

    private static BlockPos findGalleryBlock(String id) {
        int index = 0;
        for (String key : ModBlocks.ALL.keySet()) {
            if (key.equals(id)) return smokeBase.offset((index % 7) * 2, 0, (index / 7) * 2);
            index++;
        }
        return smokeBase;
    }

    private static void openUtility(ServerPlayer player, String id) {
        BlockPos pos = findGalleryBlock(id);
        if (player.level().getBlockEntity(pos) instanceof UtilityMachineBlockEntity machine
                && player.level().getBlockState(pos).getBlock() instanceof UtilityMachineBlock block) {
            switch (id) {
                case "auto_anvil" -> {
                    ItemStack tool = new ItemStack(Items.IRON_PICKAXE);
                    tool.setDamageValue(200);
                    machine.getInventory().setStackInSlot(0, tool);
                    machine.getInventory().setStackInSlot(1, new ItemStack(Items.IRON_PICKAXE));
                    machine.getExperienceTank().fill(new FluidStack(ModFluids.XP_JUICE.get(), ModFluids.xpToFluid(10)),
                            net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                }
                case "auto_enchantment_table" -> {
                    machine.getInventory().setStackInSlot(0, new ItemStack(Items.DIAMOND_SWORD));
                    machine.getInventory().setStackInSlot(1, new ItemStack(Items.LAPIS_LAZULI, 3));
                    machine.getExperienceTank().fill(new FluidStack(ModFluids.XP_JUICE.get(), ModFluids.xpToFluid(30)),
                            net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                }
                case "paint_mixer" -> {
                    machine.getInventory().setStackInSlot(0, new ItemStack(ModBlocks.PAINT_CAN.get().asItem()));
                    machine.getInventory().setStackInSlot(2, new ItemStack(Items.CYAN_DYE));
                    machine.getInventory().setStackInSlot(3, new ItemStack(Items.MAGENTA_DYE));
                    machine.getInventory().setStackInSlot(4, new ItemStack(Items.YELLOW_DYE));
                    machine.getInventory().setStackInSlot(5, new ItemStack(Items.BLACK_DYE));
                    machine.setSelectedPaintColor(0xB0417E);
                }
                case "drawing_table" -> {
                    machine.getInventory().setStackInSlot(0, new ItemStack(ModItems.UNPREPARED_STENCIL.get(), 8));
                }
                case "sprinkler" -> {
                    machine.getInventory().setStackInSlot(0, new ItemStack(Items.BONE_MEAL, 16));
                    machine.getFluidTank().fill(new FluidStack(Fluids.WATER, 1000),
                            net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                }
                case "projector" -> machine.getInventory().setStackInSlot(0,
                        net.xuwu.openblocks_reborn.item.HeightMapItem.create(player.level(), player.blockPosition()));
                case "donation_station" -> machine.getInventory().setStackInSlot(0, new ItemStack(Items.DIAMOND));
                default -> { }
            }
            MenuHelper.open(player, Component.translatable("container.openblocks_reborn." + block.kind().id()),
                    machine.getInventory(), MachineLayout.forUtility(block.kind()), () -> true,
                    MenuHelper.data(
                            () -> block.kind() == UtilityMachineBlock.Kind.PAINT_MIXER
                                    ? machine.getSelectedPaintColor()
                                    : (block.kind() == UtilityMachineBlock.Kind.DRAWING_TABLE
                                    ? machine.getDrawingMode()
                                    : (machine.exposesFluidTank() ? machine.getFluidTank().getFluidAmount() : 0)),
                            () -> block.kind() == UtilityMachineBlock.Kind.PAINT_MIXER
                                    ? machine.getPaintProgress()
                                    : (block.kind() == UtilityMachineBlock.Kind.DRAWING_TABLE
                                    ? machine.getDrawingSelection()
                                    : (machine.exposesFluidTank() ? machine.getFluidTank().getCapacity() : 0)),
                             () -> block.kind() == UtilityMachineBlock.Kind.AUTO_ENCHANTMENT_TABLE
                                     ? machine.getEnchantPowerLimit() : 0,
                             machine::getEnchantLevel,
                             () -> machine.getSideMask(0),
                             () -> machine.getSideMask(1),
                             () -> machine.getSideMask(2),
                             () -> machine.getSideMask(3)),
                     buttonId -> UtilityMachineBlock.handleMenuButton(machine, block.kind(), buttonId));
        }
    }

    private static void openButton(ServerPlayer player) {
        BlockPos pos = findGalleryBlock("big_button");
        if (player.level().getBlockEntity(pos) instanceof BigButtonBlockEntity button) {
            button.getInventory().setStackInSlot(0, new ItemStack(Items.REDSTONE, 20));
            MenuHelper.open(player, Component.translatable("container.openblocks_reborn.big_button",
                    button.getPressTicks(), 1.0F), button.getInventory(), MachineLayout.BIG_BUTTON, () -> true,
                    MenuHelper.data(() -> 0, () -> 0, button::getPressTicks, () -> 0));
        }
    }

    private static void openItemMachine(ServerPlayer player, String id, MachineLayout layout, String titleKey) {
        BlockPos pos = findGalleryBlock(id);
        if (player.level().getBlockEntity(pos) instanceof net.xuwu.openblocks_reborn.blockentity.ItemMachineBlockEntity machine) {
            machine.getInventory().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 16));
            if (layout == MachineLayout.ITEM_DROPPER) {
                machine.setItemSpeed(175);
                MenuHelper.open(player, Component.translatable(titleKey), machine.getInventory(), layout, () -> true,
                        MenuHelper.data(() -> 0, () -> 0, machine::getItemSpeed,
                                () -> machine.usesRedstoneStrength() ? 1 : 0),
                        button -> net.xuwu.openblocks_reborn.block.ItemMachineBlock.handleDropperButton(machine, button));
            } else {
                MenuHelper.open(player, Component.translatable(titleKey), machine.getInventory(), layout, () -> true);
            }
        }
    }

    private static void openDevNull(ServerPlayer player) {
        ItemStack stack = new ItemStack(ModItems.DEV_NULL.get());
        DevNullItem.setStored(stack, new ItemStack(Items.COBBLESTONE, 64));
        MenuHelper.open(player, Component.translatable("container.openblocks_reborn.dev_null"),
                new DevNullItemHandler(stack), MachineLayout.DEV_NULL, () -> true);
    }

    private static void openVacuum(ServerPlayer player) {
        BlockPos pos = findGalleryBlock("vacuum_hopper");
        if (player.level().getBlockEntity(pos) instanceof VacuumHopperBlockEntity hopper) {
            MenuHelper.open(player, Component.translatable("container.openblocks_reborn.vacuum_hopper"),
                    hopper.getInventory(), MachineLayout.VACUUM_HOPPER, () -> true,
                    MenuHelper.data(() -> hopper.getExperienceTank().getFluidAmount(),
                            () -> hopper.getExperienceTank().getCapacity(), hopper::getItemOutputMask,
                            hopper::getExperienceOutputMask),
                    id -> net.xuwu.openblocks_reborn.block.VacuumHopperBlock.handleMenuButton(hopper, id));
        }
    }

    private static void openBottler(ServerPlayer player) {
        BlockPos pos = findGalleryBlock("xp_bottler");
        if (player.level().getBlockEntity(pos) instanceof ExperienceBlockEntity bottler) {
            bottler.addExperience(24);
            bottler.getInventory().setStackInSlot(0, new ItemStack(Items.GLASS_BOTTLE, 3));
            MenuHelper.open(player, Component.translatable("container.openblocks_reborn.xp_bottler"),
                    bottler.getInventory(), MachineLayout.XP_BOTTLER, () -> true,
                    MenuHelper.data(() -> bottler.getTank().getFluidAmount(),
                            () -> bottler.getTank().getCapacity(), () -> 0, () -> 0,
                            () -> bottler.getSideMask(0), () -> bottler.getSideMask(1),
                            () -> bottler.getSideMask(2), () -> 0),
                    buttonId -> net.xuwu.openblocks_reborn.block.ExperienceMachineBlock
                            .handleMenuButton(bottler, buttonId));
        }
    }

    private static void openLuggage(ServerPlayer player) {
        if (smokeLuggage != null && smokeLuggage.isAlive()) {
            MenuHelper.open(player, Component.translatable("container.openblocks_reborn.luggage"),
                    smokeLuggage.getInventory(), MachineLayout.LUGGAGE, () -> true);
        }
    }
}
