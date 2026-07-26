package net.xuwu.openblocks_reborn.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.xuwu.openblocks_reborn.OpenBlocksReborn;
import net.xuwu.openblocks_reborn.fluid.SprinklerWaterFluid;
import net.xuwu.openblocks_reborn.item.XpBucketItem;

/** Registration and conversion constants for OpenBlocks' liquid experience. */
public final class ModFluids {
    public static final int MILLIBUCKETS_PER_XP = 20;
    public static final int XP_PER_BOTTLE = 8;

    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, OpenBlocksReborn.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(ForgeRegistries.FLUIDS, OpenBlocksReborn.MOD_ID);
    public static final DeferredRegister<net.minecraft.world.level.block.Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, OpenBlocksReborn.MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, OpenBlocksReborn.MOD_ID);

    public static final RegistryObject<FluidType> XP_JUICE_TYPE = FLUID_TYPES.register("xp_juice",
            () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid_type.openblocks_reborn.xp_juice")
                    .density(1050).viscosity(1200).lightLevel(4)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)) {
                @Override
                public void initializeClient(
                        java.util.function.Consumer<IClientFluidTypeExtensions> consumer) {
                    consumer.accept(new IClientFluidTypeExtensions() {
                        private final ResourceLocation still = new ResourceLocation(
                                OpenBlocksReborn.MOD_ID, "block/xp_juice_still");
                        private final ResourceLocation flowing = new ResourceLocation(
                                OpenBlocksReborn.MOD_ID, "block/xp_juice_flowing");

                        @Override
                        public ResourceLocation getStillTexture() {
                            return still;
                        }

                        @Override
                        public ResourceLocation getFlowingTexture() {
                            return flowing;
                        }

                        @Override
                        public int getTintColor() {
                            return 0xFFE6FF3C;
                        }
                    });
                }
            });

    public static final RegistryObject<FlowingFluid> XP_JUICE = FLUIDS.register("xp_juice",
            () -> new ForgeFlowingFluid.Source(properties()));
    public static final RegistryObject<FlowingFluid> FLOWING_XP_JUICE = FLUIDS.register("flowing_xp_juice",
            () -> new ForgeFlowingFluid.Flowing(properties()));
    public static final RegistryObject<SprinklerWaterFluid> SPRINKLER_WATER =
            FLUIDS.register("sprinkler_water", SprinklerWaterFluid::new);

    public static final RegistryObject<LiquidBlock> XP_JUICE_BLOCK = BLOCKS.register("xp_juice",
            () -> new LiquidBlock(XP_JUICE.get(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GREEN).replaceable().noCollission()
                    .strength(100.0F).noLootTable().lightLevel(state -> 4)));

    public static final RegistryObject<XpBucketItem> XP_BUCKET = ITEMS.register("xp_bucket",
            () -> new XpBucketItem(XP_JUICE.get(), new Item.Properties().stacksTo(1).craftRemainder(net.minecraft.world.item.Items.BUCKET)));

    private static ForgeFlowingFluid.Properties properties() {
        return new ForgeFlowingFluid.Properties(XP_JUICE_TYPE, XP_JUICE, FLOWING_XP_JUICE)
                .bucket(XP_BUCKET).block(XP_JUICE_BLOCK)
                .tickRate(8).slopeFindDistance(4).levelDecreasePerBlock(1).explosionResistance(100.0F);
    }

    public static int xpToFluid(int experience) {
        return (int)Math.min(Integer.MAX_VALUE,
                Math.max(0L, (long)experience) * MILLIBUCKETS_PER_XP);
    }

    public static int fluidToXp(int amount) {
        return Math.max(0, amount) / MILLIBUCKETS_PER_XP;
    }

    /** Vanilla's total experience required to start the supplied level. */
    public static int experienceForLevel(int level) {
        int clamped = Math.max(0, level);
        if (clamped <= 16) {
            return clamped * clamped + 6 * clamped;
        }
        if (clamped <= 31) {
            return (int)Math.floor(2.5D * clamped * clamped - 40.5D * clamped + 360.0D);
        }
        return (int)Math.floor(4.5D * clamped * clamped - 162.5D * clamped + 2220.0D);
    }

    /** The highest complete vanilla experience level represented by an XP total. */
    public static int levelForExperience(int experience) {
        int target = Math.max(0, experience);
        int low = 0;
        int high = 1;
        while (high < 21863 && experienceForLevel(high) <= target) high *= 2;
        while (low + 1 < high) {
            int middle = low + (high - low) / 2;
            if (experienceForLevel(middle) <= target) low = middle;
            else high = middle;
        }
        return low;
    }

    private ModFluids() {
    }
}
