package net.xuwu.openblocks_reborn.item;

import net.xuwu.openblocks_reborn.util.LegacyItemData;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xuwu.openblocks_reborn.entity.CraneCarriedBlockEntity;
import net.xuwu.openblocks_reborn.registry.ModEntities;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.xuwu.openblocks_reborn.client.WearableArmorModels;

import java.util.Comparator;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Chest-worn crane. The backpack owns the arm length and any entity currently
 * held by the magnet so taking it off always disables the crane.
 */
public class CraneBackpackItem extends ArmorItem {
    private static final String LENGTH = "CraneLength";
    private static final String CARRIED = "CraneCarried";
    private static final String CARRIED_DIMENSION = "CraneCarriedDimension";
    public static final double MIN_LENGTH = 0.25D;
    public static final double MAX_LENGTH = 10.0D;
    public static final double ARM_RADIUS = 2.0D;

    public CraneBackpackItem(Properties properties) {
        super(ArmorMaterials.IRON, Type.CHESTPLATE, properties);
    }

    public static ItemStack wornBy(LivingEntity entity) {
        ItemStack stack = entity.getItemBySlot(EquipmentSlot.CHEST);
        return stack.getItem() instanceof CraneBackpackItem ? stack : ItemStack.EMPTY;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private net.minecraft.client.model.HumanoidModel<?> model;

            @Override
            public net.minecraft.client.model.HumanoidModel<?> getHumanoidArmorModel(
                    LivingEntity living, ItemStack stack, EquipmentSlot slot,
                    net.minecraft.client.model.HumanoidModel<?> original) {
                if (model == null) model = WearableArmorModels.bakeCrane();
                return model;
            }
        });
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return "openblocks_reborn:textures/models/crane.png";
    }

    public static double length(ItemStack stack) {
        var tag = LegacyItemData.copyTag(stack);
        return tag.contains(LENGTH) ? net.minecraft.util.Mth.clamp(tag.getDouble(LENGTH), MIN_LENGTH, MAX_LENGTH) : MIN_LENGTH;
    }

    public static boolean isCarrying(ItemStack stack) {
        return LegacyItemData.copyTag(stack).hasUUID(CARRIED);
    }

    public static UUID carriedId(ItemStack stack) {
        var tag = LegacyItemData.copyTag(stack);
        return tag.hasUUID(CARRIED) ? tag.getUUID(CARRIED) : null;
    }

    public static boolean hasPickupTarget(Player player, ItemStack backpack) {
        if (backpack.isEmpty() || isCarrying(backpack)) return false;
        Vec3 magnet = magnetPosition(player, backpack);
        AABB pickup = new AABB(magnet.x - 0.55D, magnet.y - 1.25D, magnet.z - 0.55D,
                magnet.x + 0.55D, magnet.y + 0.35D, magnet.z + 0.55D);
        if (!player.level().getEntities(player, pickup,
                entity -> entity.isAlive() && entity.isPickable() && !(entity instanceof Player)).isEmpty()) {
            return true;
        }
        BlockPos blockPos = BlockPos.containing(magnet.add(0.0D, -0.6D, 0.0D));
        var state = player.level().getBlockState(blockPos);
        return !state.isAir() && state.getDestroySpeed(player.level(), blockPos) >= 0.0F
                && player.level().getBlockEntity(blockPos) == null;
    }

    public static void changeLength(ItemStack stack, double delta) {
        LegacyItemData.update(stack, tag -> tag.putDouble(LENGTH,
                net.minecraft.util.Mth.clamp(tag.contains(LENGTH) ? tag.getDouble(LENGTH) + delta : MIN_LENGTH + delta,
                        MIN_LENGTH, MAX_LENGTH)));
    }

    public static Vec3 magnetPosition(LivingEntity owner, ItemStack backpack) {
        return magnetPosition(owner, backpack, 1.0F);
    }

    public static Vec3 magnetPosition(LivingEntity owner, ItemStack backpack, float partialTick) {
        double radians = Math.toRadians(Mth.rotLerp(partialTick, owner.yRotO, owner.getYRot()) + 90.0F);
        double x = Mth.lerp(partialTick, owner.xo, owner.getX());
        double y = Mth.lerp(partialTick, owner.yo, owner.getY());
        double z = Mth.lerp(partialTick, owner.zo, owner.getZ());
        return new Vec3(x + ARM_RADIUS * Math.cos(radians),
                y + owner.getBbHeight() - length(backpack),
                z + ARM_RADIUS * Math.sin(radians));
    }

    /** Legacy render anchor at the end of the head-tracking crane arm. */
    public static Vec3 cableAnchorPosition(LivingEntity owner) {
        return cableAnchorPosition(owner, 1.0F);
    }

    public static Vec3 cableAnchorPosition(LivingEntity owner, float partialTick) {
        double bodyRadians = Math.toRadians(Mth.rotLerp(partialTick,
                owner.yBodyRotO, owner.yBodyRot) + 90.0F);
        double headRadians = Math.toRadians(Mth.rotLerp(partialTick,
                owner.yHeadRotO, owner.getYHeadRot()) + 90.0F);
        boolean crouching = owner.isCrouching();
        double armLength = crouching ? 2.0D : 2.4D;
        double x = Mth.lerp(partialTick, owner.xo, owner.getX());
        double y = Mth.lerp(partialTick, owner.yo, owner.getY());
        double z = Mth.lerp(partialTick, owner.zo, owner.getZ());
        if (!crouching) {
            x -= 0.45D * Math.cos(bodyRadians);
            z -= 0.45D * Math.sin(bodyRadians);
        }
        return new Vec3(x + armLength * Math.cos(headRadians),
                y + owner.getEyeHeight() + (crouching ? 0.70D : 0.65D),
                z + armLength * Math.sin(headRadians));
    }

    public static boolean toggleMagnet(Player player) {
        ItemStack backpack = wornBy(player);
        if (backpack.isEmpty() || !(player.level() instanceof ServerLevel serverLevel)) return false;
        var tag = LegacyItemData.copyTag(backpack);
        if (tag.hasUUID(CARRIED)) {
            releaseCarried(player, backpack);
            return true;
        }

        Vec3 magnet = magnetPosition(player, backpack);
        AABB pickup = new AABB(magnet.x - 0.55D, magnet.y - 1.25D, magnet.z - 0.55D,
                magnet.x + 0.55D, magnet.y + 0.35D, magnet.z + 0.55D);
        Entity target = player.level().getEntities(player, pickup,
                        entity -> entity.isAlive() && entity.isPickable() && !(entity instanceof Player))
                .stream().min(Comparator.comparingDouble(entity -> entity.distanceToSqr(magnet))).orElse(null);

        if (target == null) {
            BlockPos blockPos = BlockPos.containing(magnet.add(0.0D, -0.6D, 0.0D));
            var state = serverLevel.getBlockState(blockPos);
            if (!state.isAir() && state.getDestroySpeed(serverLevel, blockPos) >= 0.0F
                    && serverLevel.getBlockEntity(blockPos) == null) {
                target = CraneCarriedBlockEntity.lift(
                        ModEntities.CRANE_CARRIED_BLOCK.get(), serverLevel, blockPos, state);
            }
        }
        if (target == null) return false;
        target.setNoGravity(true);
        UUID id = target.getUUID();
        LegacyItemData.update(backpack, data -> {
            data.putUUID(CARRIED, id);
            data.putString(CARRIED_DIMENSION, serverLevel.dimension().location().toString());
        });
        return true;
    }

    public static boolean releaseCarried(Player player, ItemStack backpack) {
        var tag = LegacyItemData.copyTag(backpack);
        if (!tag.hasUUID(CARRIED) || !(player.level() instanceof ServerLevel currentLevel)) return false;
        ServerLevel carriedLevel = currentLevel;
        ResourceLocation dimension = ResourceLocation.tryParse(tag.getString(CARRIED_DIMENSION));
        if (dimension != null) {
            ServerLevel stored = currentLevel.getServer().getLevel(
                    ResourceKey.create(Registries.DIMENSION, dimension));
            if (stored != null) carriedLevel = stored;
        }
        Entity carried = carriedLevel.getEntity(tag.getUUID(CARRIED));
        if (carried != null) {
            carried.setNoGravity(false);
            carried.hurtMarked = true;
        }
        LegacyItemData.update(backpack, data -> {
            data.remove(CARRIED);
            data.remove(CARRIED_DIMENSION);
        });
        return true;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!(level instanceof ServerLevel serverLevel) || !(entity instanceof Player player)) return;
        if (player.getItemBySlot(EquipmentSlot.CHEST) != stack) {
            releaseCarried(player, stack);
            return;
        }
        var tag = LegacyItemData.copyTag(stack);
        if (!tag.hasUUID(CARRIED)) return;
        Entity carried = serverLevel.getEntity(tag.getUUID(CARRIED));
        if (carried == null || !carried.isAlive()) {
            LegacyItemData.update(stack, data -> {
                data.remove(CARRIED);
                data.remove(CARRIED_DIMENSION);
            });
            return;
        }
        Vec3 target = magnetPosition(player, stack).add(0.0D, -carried.getBbHeight(), 0.0D);
        Vec3 offset = target.subtract(carried.position());
        carried.setNoGravity(true);
        if (offset.lengthSqr() > 64.0D) {
            carried.setPos(target);
            carried.setDeltaMovement(Vec3.ZERO);
        } else {
            Vec3 step = offset.scale(0.5D);
            if (step.lengthSqr() > 4.0D) step = step.normalize().scale(2.0D);
            carried.setPos(carried.position().add(step));
            carried.setDeltaMovement(step);
        }
        carried.hasImpulse = true;
        carried.hurtMarked = true;
    }
}
