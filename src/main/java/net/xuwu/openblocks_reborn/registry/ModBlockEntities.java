package net.xuwu.openblocks_reborn.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.xuwu.openblocks_reborn.OpenBlocksReborn;
import net.xuwu.openblocks_reborn.blockentity.ExperienceBlockEntity;
import net.xuwu.openblocks_reborn.blockentity.GraveBlockEntity;
import net.xuwu.openblocks_reborn.blockentity.GuideBlockEntity;
import net.xuwu.openblocks_reborn.blockentity.ItemMachineBlockEntity;
import net.xuwu.openblocks_reborn.blockentity.TankBlockEntity;
import net.xuwu.openblocks_reborn.blockentity.VacuumHopperBlockEntity;
import net.xuwu.openblocks_reborn.blockentity.UtilityMachineBlockEntity;
import net.xuwu.openblocks_reborn.blockentity.BigButtonBlockEntity;
import net.xuwu.openblocks_reborn.blockentity.GoldenEggBlockEntity;
import net.xuwu.openblocks_reborn.blockentity.TrophyBlockEntity;
import net.xuwu.openblocks_reborn.blockentity.CanvasBlockEntity;
import net.xuwu.openblocks_reborn.blockentity.SkyBlockEntity;
import net.xuwu.openblocks_reborn.blockentity.ImaginaryBlockEntity;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, OpenBlocksReborn.MOD_ID);

    public static final RegistryObject<BlockEntityType<GraveBlockEntity>> GRAVE =
            BLOCK_ENTITIES.register("grave", () -> BlockEntityType.Builder.of(GraveBlockEntity::new, ModBlocks.GRAVE.get()).build(null));
    public static final RegistryObject<BlockEntityType<TankBlockEntity>> TANK =
            BLOCK_ENTITIES.register("tank", () -> BlockEntityType.Builder.of(TankBlockEntity::new, ModBlocks.TANK.get()).build(null));
    public static final RegistryObject<BlockEntityType<VacuumHopperBlockEntity>> VACUUM_HOPPER =
            BLOCK_ENTITIES.register("vacuum_hopper", () -> BlockEntityType.Builder.of(VacuumHopperBlockEntity::new, ModBlocks.VACUUM_HOPPER.get()).build(null));
    public static final RegistryObject<BlockEntityType<ItemMachineBlockEntity>> ITEM_MACHINE =
            BLOCK_ENTITIES.register("item_machine", () -> BlockEntityType.Builder.of(ItemMachineBlockEntity::new,
                    ModBlocks.BLOCK_PLACER.get(), ModBlocks.ITEM_DROPPER.get(), ModBlocks.CANNON.get()).build(null));
    public static final RegistryObject<BlockEntityType<ExperienceBlockEntity>> EXPERIENCE_MACHINE =
            BLOCK_ENTITIES.register("experience_machine", () -> BlockEntityType.Builder.of(ExperienceBlockEntity::new,
                    ModBlocks.XP_DRAIN.get(), ModBlocks.XP_BOTTLER.get(), ModBlocks.XP_SHOWER.get()).build(null));
    public static final RegistryObject<BlockEntityType<GuideBlockEntity>> GUIDE =
            BLOCK_ENTITIES.register("guide", () -> BlockEntityType.Builder.of(GuideBlockEntity::new,
                    ModBlocks.GUIDE.get(), ModBlocks.BUILDER_GUIDE.get()).build(null));
    public static final RegistryObject<BlockEntityType<UtilityMachineBlockEntity>> UTILITY_MACHINE =
            BLOCK_ENTITIES.register("utility_machine", () -> BlockEntityType.Builder.of(UtilityMachineBlockEntity::new,
                    ModBlocks.AUTO_ANVIL.get(), ModBlocks.AUTO_ENCHANTMENT_TABLE.get(), ModBlocks.DONATION_STATION.get(),
                    ModBlocks.PAINT_MIXER.get(), ModBlocks.DRAWING_TABLE.get(), ModBlocks.PROJECTOR.get(),
                    ModBlocks.SPRINKLER.get()).build(null));
    public static final RegistryObject<BlockEntityType<BigButtonBlockEntity>> BIG_BUTTON =
            BLOCK_ENTITIES.register("big_button", () -> BlockEntityType.Builder.of(BigButtonBlockEntity::new,
                    ModBlocks.BIG_BUTTON.get(), ModBlocks.BIG_BUTTON_WOOD.get()).build(null));
    public static final RegistryObject<BlockEntityType<GoldenEggBlockEntity>> GOLDEN_EGG =
            BLOCK_ENTITIES.register("golden_egg", () -> BlockEntityType.Builder.of(GoldenEggBlockEntity::new,
                    ModBlocks.GOLDEN_EGG.get()).build(null));
    public static final RegistryObject<BlockEntityType<TrophyBlockEntity>> TROPHY =
            BLOCK_ENTITIES.register("trophy", () -> BlockEntityType.Builder.of(TrophyBlockEntity::new,
                    ModBlocks.TROPHY.get()).build(null));
    public static final RegistryObject<BlockEntityType<CanvasBlockEntity>> CANVAS =
            BLOCK_ENTITIES.register("canvas", () -> BlockEntityType.Builder.of(CanvasBlockEntity::new,
                    ModBlocks.CANVAS.get(), ModBlocks.CANVAS_GLASS.get()).build(null));
    public static final RegistryObject<BlockEntityType<SkyBlockEntity>> SKY =
            BLOCK_ENTITIES.register("sky", () -> BlockEntityType.Builder.of(
                    SkyBlockEntity::new, ModBlocks.SKY.get()).build(null));
    public static final RegistryObject<BlockEntityType<ImaginaryBlockEntity>> IMAGINARY =
            BLOCK_ENTITIES.register("imaginary", () -> BlockEntityType.Builder.of(
                    ImaginaryBlockEntity::new, ModBlocks.IMAGINARY.get()).build(null));

    private ModBlockEntities() {
    }
}
