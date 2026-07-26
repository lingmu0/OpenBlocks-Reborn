package net.xuwu.openblocks_reborn.registry;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.xuwu.openblocks_reborn.OpenBlocksReborn;
import net.xuwu.openblocks_reborn.block.BearTrapBlock;
import net.xuwu.openblocks_reborn.block.BigButtonBlock;
import net.xuwu.openblocks_reborn.block.AutoEnchantmentTableBlock;
import net.xuwu.openblocks_reborn.block.BlockBreakerBlock;
import net.xuwu.openblocks_reborn.block.ColorableBlock;
import net.xuwu.openblocks_reborn.block.CanvasBlock;
import net.xuwu.openblocks_reborn.block.ElevatorBlock;
import net.xuwu.openblocks_reborn.block.EnhancedSpongeBlock;
import net.xuwu.openblocks_reborn.block.FanBlock;
import net.xuwu.openblocks_reborn.block.FlagBlock;
import net.xuwu.openblocks_reborn.block.ExperienceMachineBlock;
import net.xuwu.openblocks_reborn.block.GraveBlock;
import net.xuwu.openblocks_reborn.block.GoldenEggBlock;
import net.xuwu.openblocks_reborn.block.SkyBlock;
import net.xuwu.openblocks_reborn.block.ImaginaryBlock;
import net.xuwu.openblocks_reborn.block.PaintCanBlock;
import net.xuwu.openblocks_reborn.block.GuideBlock;
import net.xuwu.openblocks_reborn.block.HealBlock;
import net.xuwu.openblocks_reborn.block.ItemMachineBlock;
import net.xuwu.openblocks_reborn.block.JadedLadderBlock;
import net.xuwu.openblocks_reborn.block.PathBlock;
import net.xuwu.openblocks_reborn.block.RopeLadderBlock;
import net.xuwu.openblocks_reborn.block.UtilityMachineBlock;
import net.xuwu.openblocks_reborn.block.TankBlock;
import net.xuwu.openblocks_reborn.block.TargetBlock;
import net.xuwu.openblocks_reborn.block.TrophyBlock;
import net.xuwu.openblocks_reborn.block.VacuumHopperBlock;
import net.xuwu.openblocks_reborn.block.VillageHighlighterBlock;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(OpenBlocksReborn.MOD_ID);
    public static final Map<String, DeferredBlock<? extends Block>> ALL = new LinkedHashMap<>();

    private static BlockBehaviour.Properties stone(float hardness) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(hardness).sound(SoundType.STONE);
    }

    private static BlockBehaviour.Properties metal(float hardness) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(hardness).sound(SoundType.METAL);
    }

    private static <T extends Block> DeferredBlock<T> register(String id, Supplier<T> block) {
        DeferredBlock<T> result = BLOCKS.register(id, block);
        ALL.put(id, result);
        return result;
    }

    public static final DeferredBlock<JadedLadderBlock> LADDER = register("ladder",
            () -> new JadedLadderBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
                    .strength(3.0F).sound(SoundType.WOOD).noOcclusion()));
    public static final DeferredBlock<GuideBlock> GUIDE = register("guide", () -> new GuideBlock(metal(2.0F).lightLevel(state -> 5).noOcclusion(), false));
    public static final DeferredBlock<GuideBlock> BUILDER_GUIDE = register("builder_guide", () -> new GuideBlock(metal(2.5F).lightLevel(state -> 5).noOcclusion(), true));
    public static final DeferredBlock<ElevatorBlock> ELEVATOR = register("elevator", () -> new ElevatorBlock(stone(1.5F), false));
    public static final DeferredBlock<ElevatorBlock> ELEVATOR_ROTATING = register("elevator_rotating", () -> new ElevatorBlock(stone(1.5F), true));
    public static final DeferredBlock<HealBlock> HEAL = register("heal", () -> new HealBlock(metal(2.0F).lightLevel(state -> 7)));
    public static final DeferredBlock<TargetBlock> TARGET = register("target", () -> new TargetBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.0F).sound(SoundType.WOOD).noOcclusion()));
    public static final DeferredBlock<GraveBlock> GRAVE = register("grave", () -> new GraveBlock(stone(2.0F).noOcclusion()));
    public static final DeferredBlock<FlagBlock> FLAG = register("flag", () -> new FlagBlock(BlockBehaviour.Properties.of().mapColor(DyeColor.WHITE).strength(0.4F).sound(SoundType.WOOL).noOcclusion()));
    public static final DeferredBlock<TankBlock> TANK = register("tank", () -> new TankBlock(BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(1.0F).sound(SoundType.GLASS).noOcclusion()));
    public static final DeferredBlock<TrophyBlock> TROPHY = register("trophy", () -> new TrophyBlock(stone(1.0F).noOcclusion()));
    public static final DeferredBlock<BearTrapBlock> BEARTRAP = register("beartrap", () -> new BearTrapBlock(metal(2.0F).noOcclusion()));
    public static final DeferredBlock<UtilityMachineBlock> SPRINKLER = register("sprinkler", () -> new UtilityMachineBlock(
            metal(2.0F).noOcclusion().liquid(), UtilityMachineBlock.Kind.SPRINKLER));
    public static final DeferredBlock<ItemMachineBlock> CANNON = register("cannon", () -> new ItemMachineBlock(metal(3.0F).noOcclusion(), ItemMachineBlock.Mode.CANNON));
    public static final DeferredBlock<VacuumHopperBlock> VACUUM_HOPPER = register("vacuum_hopper", () -> new VacuumHopperBlock(metal(3.0F).noOcclusion()));
    public static final DeferredBlock<EnhancedSpongeBlock> SPONGE = register("sponge", () -> new EnhancedSpongeBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(0.6F).sound(SoundType.GRASS)));
    public static final DeferredBlock<BigButtonBlock> BIG_BUTTON = register("big_button", () -> new BigButtonBlock(BlockSetType.STONE, stone(0.5F).noCollission()));
    public static final DeferredBlock<BigButtonBlock> BIG_BUTTON_WOOD = register("big_button_wood", () -> new BigButtonBlock(BlockSetType.OAK, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(0.5F).noCollission()));
    public static final DeferredBlock<ImaginaryBlock> IMAGINARY = register("imaginary", () -> new ImaginaryBlock(BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(0.3F).noOcclusion()));
    public static final DeferredBlock<FanBlock> FAN = register("fan", () -> new FanBlock(metal(2.0F).noOcclusion()));
    public static final DeferredBlock<ExperienceMachineBlock> XP_BOTTLER = register("xp_bottler", () -> new ExperienceMachineBlock(metal(3.0F), ExperienceMachineBlock.Mode.BOTTLER));
    public static final DeferredBlock<VillageHighlighterBlock> VILLAGE_HIGHLIGHTER = register("village_highlighter", () -> new VillageHighlighterBlock(metal(2.0F).lightLevel(state -> 5).noOcclusion()));
    public static final DeferredBlock<PathBlock> PATH = register("path", () -> new PathBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(0.7F).sound(SoundType.GRAVEL).noOcclusion()));
    public static final DeferredBlock<UtilityMachineBlock> AUTO_ANVIL = register("auto_anvil", () -> new UtilityMachineBlock(metal(4.0F).noOcclusion(), UtilityMachineBlock.Kind.AUTO_ANVIL));
    public static final DeferredBlock<UtilityMachineBlock> AUTO_ENCHANTMENT_TABLE = register("auto_enchantment_table",
            () -> new UtilityMachineBlock(stone(4.0F).lightLevel(state -> 7).noOcclusion(), UtilityMachineBlock.Kind.AUTO_ENCHANTMENT_TABLE));
    public static final DeferredBlock<ExperienceMachineBlock> XP_DRAIN = register("xp_drain", () -> new ExperienceMachineBlock(metal(2.0F).noOcclusion(), ExperienceMachineBlock.Mode.DRAIN));
    public static final DeferredBlock<BlockBreakerBlock> BLOCK_BREAKER = register("block_breaker", () -> new BlockBreakerBlock(metal(3.0F)));
    public static final DeferredBlock<ItemMachineBlock> BLOCK_PLACER = register("block_placer", () -> new ItemMachineBlock(metal(3.0F), ItemMachineBlock.Mode.PLACER));
    public static final DeferredBlock<ItemMachineBlock> ITEM_DROPPER = register("item_dropper", () -> new ItemMachineBlock(metal(3.0F), ItemMachineBlock.Mode.DROPPER));
    public static final DeferredBlock<RopeLadderBlock> ROPE_LADDER = register("rope_ladder", () -> new RopeLadderBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(0.3F).sound(SoundType.WOOD).noOcclusion().noCollission()));
    public static final DeferredBlock<UtilityMachineBlock> DONATION_STATION = register("donation_station", () -> new UtilityMachineBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD), UtilityMachineBlock.Kind.DONATION_STATION));
    public static final DeferredBlock<UtilityMachineBlock> PAINT_MIXER = register("paint_mixer", () -> new UtilityMachineBlock(metal(2.0F).noOcclusion(), UtilityMachineBlock.Kind.PAINT_MIXER));
    public static final DeferredBlock<CanvasBlock> CANVAS = register("canvas", () -> new CanvasBlock(BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.8F).sound(SoundType.WOOL).noOcclusion()));
    public static final DeferredBlock<PaintCanBlock> PAINT_CAN = register("paint_can",
            () -> new PaintCanBlock(metal(1.0F).noOcclusion()));
    public static final DeferredBlock<CanvasBlock> CANVAS_GLASS = register("canvas_glass", () -> new CanvasBlock(BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(0.8F).sound(SoundType.GLASS).noOcclusion()));
    public static final DeferredBlock<UtilityMachineBlock> PROJECTOR = register("projector", () -> new UtilityMachineBlock(metal(2.0F).lightLevel(state -> state.getValue(UtilityMachineBlock.POWERED) ? 9 : 2), UtilityMachineBlock.Kind.PROJECTOR));
    public static final DeferredBlock<UtilityMachineBlock> DRAWING_TABLE = register("drawing_table", () -> new UtilityMachineBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD), UtilityMachineBlock.Kind.DRAWING_TABLE));
    public static final DeferredBlock<SkyBlock> SKY = register("sky", () -> new SkyBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(0.5F).sound(SoundType.GLASS).lightLevel(state -> SkyBlock.isActive(state) ? 15 : 3).noOcclusion()));
    public static final DeferredBlock<ExperienceMachineBlock> XP_SHOWER = register("xp_shower", () -> new ExperienceMachineBlock(metal(2.0F).noOcclusion(), ExperienceMachineBlock.Mode.SHOWER));
    public static final DeferredBlock<GoldenEggBlock> GOLDEN_EGG = register("golden_egg", () -> new GoldenEggBlock(metal(3.0F).lightLevel(state -> 5).noOcclusion()));
    private static DeferredBlock<Block> simple(String id, BlockBehaviour.Properties properties) {
        return register(id, () -> new Block(properties));
    }

    private ModBlocks() {
    }
}
