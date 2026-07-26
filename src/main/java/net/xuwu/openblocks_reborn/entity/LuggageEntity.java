package net.xuwu.openblocks_reborn.entity;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.xuwu.openblocks_reborn.registry.ModItems;
import net.xuwu.openblocks_reborn.menu.MenuHelper;
import net.xuwu.openblocks_reborn.menu.MachineLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LuggageEntity extends PathfinderMob implements ItemSupplier {
    private static final int SLOTS = 27;
    private final ItemStackHandler inventory = new ItemStackHandler(SLOTS);
    private UUID owner;
    private boolean packedDeathDrop;

    public LuggageEntity(EntityType<? extends LuggageEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        getNavigation().setCanFloat(true);
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public UUID getOwner() {
        return owner;
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public void restoreFromItem(ItemStack stack) {
        ItemContainerContents contents = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        for (int slot = 0; slot < Math.min(contents.getSlots(), inventory.getSlots()); slot++) {
            inventory.setStackInSlot(slot, contents.getStackInSlot(slot));
        }
        if (stack.has(DataComponents.CUSTOM_NAME)) setCustomName(stack.getHoverName());
    }

    public ItemStack toItem() {
        ItemStack result = new ItemStack(ModItems.LUGGAGE.get());
        List<ItemStack> stacks = new ArrayList<>(SLOTS);
        for (int slot = 0; slot < SLOTS; slot++) stacks.add(inventory.getStackInSlot(slot));
        result.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(stacks));
        if (hasCustomName()) result.set(DataComponents.CUSTOM_NAME, getCustomName());
        return result;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) owner = tag.getUUID("Owner");
        if (tag.contains("Inventory")) inventory.deserializeNBT(level().registryAccess(), tag.getCompound("Inventory"));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (owner != null) tag.putUUID("Owner", owner);
        tag.put("Inventory", inventory.serializeNBT(level().registryAccess()));
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        collectNearbyItems();
        followOwner();
    }

    private void followOwner() {
        if (!(level() instanceof ServerLevel serverLevel) || owner == null) return;
        Player player = serverLevel.getPlayerByUUID(owner);
        if (player == null || player.isSpectator()) return;
        double distance = distanceToSqr(player);
        if (distance > 64.0D * 64.0D) {
            // Only recover from genuinely lost/unloaded distances. Normal following is
            // handled by ground navigation and therefore uses ordinary mob interpolation.
            getNavigation().stop();
            teleportTo(player.getX(), player.getY(), player.getZ());
        } else if (distance > 3.0 * 3.0) {
            if (tickCount % 10 == 0 || getNavigation().isDone()) {
                getNavigation().moveTo(player, 1.15D);
            }
        } else if (!getNavigation().isDone()) {
            getNavigation().stop();
        }
    }

    private void collectNearbyItems() {
        for (ItemEntity entity : level().getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(1.75),
                candidate -> candidate.isAlive() && !candidate.getItem().is(ModItems.LUGGAGE.get()))) {
            ItemStack remainder = entity.getItem().copy();
            int before = remainder.getCount();
            for (int slot = 0; slot < inventory.getSlots() && !remainder.isEmpty(); slot++) {
                remainder = inventory.insertItem(slot, remainder, false);
            }
            if (remainder.getCount() != before) {
                if (remainder.isEmpty()) entity.discard();
                else entity.getItem().setCount(remainder.getCount());
                level().playSound(null, blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 0.25F, 1.4F);
            }
        }
    }

    public int storedItemCount() {
        int total = 0;
        for (int slot = 0; slot < inventory.getSlots(); slot++) total += inventory.getStackInSlot(slot).getCount();
        return total;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (owner != null && !owner.equals(player.getUUID())) return InteractionResult.PASS;
        if (!level().isClientSide) {
            if (player.isShiftKeyDown()) {
                ItemStack packed = toItem();
                if (player.addItem(packed)) discard();
                else player.displayClientMessage(Component.translatable("message.openblocks_reborn.luggage_inventory_full"), true);
            } else if (player instanceof ServerPlayer serverPlayer) {
                MenuHelper.open(serverPlayer, Component.translatable("container.openblocks_reborn.luggage"), inventory,
                        MachineLayout.LUGGAGE,
                        () -> isAlive() && player.distanceToSqr(this) <= 64.0 && (owner == null || owner.equals(player.getUUID())));
            }
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(ModItems.LUGGAGE.get());
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        dropPackedForm(level);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (reason == RemovalReason.KILLED && !packedDeathDrop
                && level() instanceof ServerLevel serverLevel) {
            dropPackedForm(serverLevel);
        }
        super.remove(reason);
    }

    @Override
    public void kill() {
        if (!packedDeathDrop && level() instanceof ServerLevel serverLevel) {
            dropPackedForm(serverLevel);
        }
        super.kill();
    }

    private void dropPackedForm(ServerLevel level) {
        if (packedDeathDrop) return;
        packedDeathDrop = true;
        ItemEntity packed = new ItemEntity(level, getX(), getY(), getZ(), toItem());
        packed.setDefaultPickUpDelay();
        packed.setInvulnerable(true);
        packed.setUnlimitedLifetime();
        packed.setDeltaMovement(0.0D, 0.0D, 0.0D);
        level.addFreshEntity(packed);
    }
}
