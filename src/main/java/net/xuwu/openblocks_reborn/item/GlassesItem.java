package net.xuwu.openblocks_reborn.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.xuwu.openblocks_reborn.blockentity.ImaginaryBlockEntity;

/**
 * The legacy glasses are real helmets. Right-click uses ArmorItem's vanilla
 * equipment swap and their behaviour is enabled only while worn.
 */
public class GlassesItem extends ArmorItem {
    public enum Mode { SONIC, PENCIL, CRAYON, TECHNICOLOR, SERIOUS }
    public enum ImaginaryProperty { VISIBLE, SELECTABLE, SOLID }

    private static final String COLOR = "GlassesColor";

    private final Mode mode;

    public GlassesItem(Properties properties, Mode mode) {
        super(mode == Mode.SONIC ? ArmorMaterials.IRON : ArmorMaterials.GOLD, Type.HELMET, properties);
        this.mode = mode;
    }

    public Mode mode() {
        return mode;
    }

    @Override
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot,
                                            ArmorMaterial.Layer layer, boolean innerModel) {
        String texture = switch (mode) {
            case SONIC -> "glasses";
            case PENCIL -> "glasses_pencil";
            case CRAYON -> "glasses_crayon";
            case TECHNICOLOR -> "glasses_technicolor";
            case SERIOUS -> "glasses_admin";
        };
        return ResourceLocation.fromNamespaceAndPath("openblocks_reborn",
                "textures/models/" + texture + ".png");
    }

    public static boolean isWearing(Player player, Mode mode) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        return helmet.getItem() instanceof GlassesItem glasses && glasses.mode == mode;
    }

    public static ItemStack createCrayonGlasses(int color) {
        ItemStack result = new ItemStack(net.xuwu.openblocks_reborn.registry.ModItems.CRAYON_GLASSES.get());
        CustomData.update(DataComponents.CUSTOM_DATA, result, tag -> tag.putInt(COLOR, color & 0xFFFFFF));
        return result;
    }

    public static int getGlassesColor(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getInt(COLOR) & 0xFFFFFF;
    }

    /**
     * Reproduces the legacy imaginary-block visibility table. Pencil blocks are
     * always solid; crayon blocks are solid/selectable only for matching glasses,
     * while inverted blocks reverse the ordinary glasses result.
     */
    public static boolean testImaginary(Player player, ImaginaryBlockEntity imaginary,
                                        ImaginaryProperty property) {
        if (property == ImaginaryProperty.VISIBLE && player.isSpectator()) return true;
        if (property == ImaginaryProperty.SOLID && imaginary.isPencil()) return true;

        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!(helmet.getItem() instanceof GlassesItem glasses)
                || glasses.mode == Mode.SONIC) {
            return imaginary.isInverted();
        }
        return switch (glasses.mode) {
            case PENCIL -> imaginary.isPencil() ^ imaginary.isInverted();
            case CRAYON -> (!imaginary.isPencil()
                    && getGlassesColor(helmet) == imaginary.getColor()) ^ imaginary.isInverted();
            case TECHNICOLOR -> property == ImaginaryProperty.VISIBLE || imaginary.isInverted();
            case SERIOUS -> true;
            default -> imaginary.isInverted();
        };
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!(level instanceof ServerLevel serverLevel) || !(entity instanceof Player player)
                || player.getItemBySlot(EquipmentSlot.HEAD) != stack || player.tickCount % 10 != 0) return;
        if (mode == Mode.SONIC) {
            for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(16.0D), living -> living != player && living.isAlive())) {
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, living.getX(),
                        living.getY() + living.getBbHeight() * 0.5D, living.getZ(),
                        2, living.getBbWidth() * 0.3D, living.getBbHeight() * 0.3D,
                        living.getBbWidth() * 0.3D, 0.0D);
                living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 15, 0, false, false));
            }
            return;
        }
        // Imaginary blocks are revealed directly by their client block-entity
        // renderer. Spawning proxy particles here made the glasses noisy and did
        // not reproduce the legacy per-colour visibility rules.
    }
}
