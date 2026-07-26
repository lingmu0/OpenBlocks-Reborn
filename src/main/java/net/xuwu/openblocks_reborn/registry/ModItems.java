package net.xuwu.openblocks_reborn.registry;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.xuwu.openblocks_reborn.OpenBlocksReborn;
import net.xuwu.openblocks_reborn.item.GoldenEyeItem;
import net.xuwu.openblocks_reborn.item.DevNullItem;
import net.xuwu.openblocks_reborn.item.HeightMapItem;
import net.xuwu.openblocks_reborn.item.InfoBookItem;
import net.xuwu.openblocks_reborn.item.MagnetItem;
import net.xuwu.openblocks_reborn.item.LuggageItem;
import net.xuwu.openblocks_reborn.item.PaintBrushItem;
import net.xuwu.openblocks_reborn.item.PedometerItem;
import net.xuwu.openblocks_reborn.item.SlimalyzerItem;
import net.xuwu.openblocks_reborn.item.SleepingBagItem;
import net.xuwu.openblocks_reborn.item.SpongeOnAStickItem;
import net.xuwu.openblocks_reborn.item.SqueegeeItem;
import net.xuwu.openblocks_reborn.item.WrenchItem;
import net.xuwu.openblocks_reborn.item.GlassesItem;
import net.xuwu.openblocks_reborn.item.CraneControlItem;
import net.xuwu.openblocks_reborn.item.CraneBackpackItem;
import net.xuwu.openblocks_reborn.item.CartographerItem;
import net.xuwu.openblocks_reborn.item.EpicEraserItem;
import net.xuwu.openblocks_reborn.item.StencilItem;
import net.xuwu.openblocks_reborn.item.CursorItem;
import net.xuwu.openblocks_reborn.item.GlyphItem;
import net.xuwu.openblocks_reborn.item.PointerItem;
import net.xuwu.openblocks_reborn.item.HangGliderItem;
import net.xuwu.openblocks_reborn.item.ImaginaryItem;
import net.xuwu.openblocks_reborn.item.SkyBlockItem;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(OpenBlocksReborn.MOD_ID);
    public static final Map<String, DeferredItem<BlockItem>> BLOCK_ITEMS = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<? extends Item>> ALL_ITEMS = new LinkedHashMap<>();

    static {
        ModBlocks.ALL.forEach((id, block) -> {
            DeferredItem<BlockItem> item = ITEMS.register(id, () -> id.equals("imaginary")
                    ? new ImaginaryItem(block.get(), new Item.Properties())
                    : (id.equals("sky")
                    ? new SkyBlockItem(block.get(), new Item.Properties())
                    : new BlockItem(block.get(), new Item.Properties())));
            BLOCK_ITEMS.put(id, item);
        });
    }

    public static final DeferredItem<HangGliderItem> HANG_GLIDER = register("hang_glider",
            () -> new HangGliderItem(new Item.Properties().stacksTo(1).durability(256)));
    /** Legacy 1.12 metadata container retained so old IDs are not silently discarded by migration tools. */
    public static final DeferredItem<Item> GENERIC = item("generic", new Item.Properties());
    /** Legacy 1.12 unstackable metadata container retained for registry compatibility. */
    public static final DeferredItem<Item> GENERIC_UNSTACKABLE = item("generic_unstackable", new Item.Properties().stacksTo(1));
    public static final DeferredItem<LuggageItem> LUGGAGE = register("luggage", () -> new LuggageItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<GlassesItem> SONIC_GLASSES = register("sonic_glasses", () -> new GlassesItem(new Item.Properties().stacksTo(1), GlassesItem.Mode.SONIC));
    public static final DeferredItem<GlassesItem> PENCIL_GLASSES = register("pencil_glasses", () -> new GlassesItem(new Item.Properties().stacksTo(1), GlassesItem.Mode.PENCIL));
    public static final DeferredItem<GlassesItem> CRAYON_GLASSES = register("crayon_glasses", () -> new GlassesItem(new Item.Properties().stacksTo(1), GlassesItem.Mode.CRAYON));
    public static final DeferredItem<GlassesItem> TECHNICOLOR_GLASSES = register("technicolor_glasses", () -> new GlassesItem(new Item.Properties().stacksTo(1), GlassesItem.Mode.TECHNICOLOR));
    public static final DeferredItem<GlassesItem> SERIOUS_GLASSES = register("serious_glasses", () -> new GlassesItem(new Item.Properties().stacksTo(1), GlassesItem.Mode.SERIOUS));
    public static final DeferredItem<CraneControlItem> CRANE_CONTROL = register("crane_control", () -> new CraneControlItem(new Item.Properties().stacksTo(1).durability(512)));
    public static final DeferredItem<CraneBackpackItem> CRANE_BACKPACK = register("crane_backpack", () -> new CraneBackpackItem(new Item.Properties().stacksTo(1).durability(512)));
    public static final DeferredItem<SlimalyzerItem> SLIMALYZER = register("slimalyzer", () -> new SlimalyzerItem(new Item.Properties().stacksTo(1).durability(256)));
    public static final DeferredItem<SleepingBagItem> SLEEPING_BAG = register("sleeping_bag", () -> new SleepingBagItem(new Item.Properties().stacksTo(1).durability(64)));
    public static final DeferredItem<PaintBrushItem> PAINTBRUSH = register("paintbrush", () -> new PaintBrushItem(new Item.Properties().stacksTo(1).durability(128)));
    public static final DeferredItem<PaintBrushItem> SMALL_PAINTBRUSH = register("small_paintbrush", () -> new PaintBrushItem(new Item.Properties().stacksTo(1).durability(128), true));
    public static final DeferredItem<StencilItem> STENCIL = register("stencil", () -> new StencilItem(new Item.Properties()));
    public static final DeferredItem<SqueegeeItem> SQUEEGEE = register("squeegee", () -> new SqueegeeItem(new Item.Properties().stacksTo(1).durability(128)));
    public static final DeferredItem<SqueegeeItem> SMALL_SQUEEGEE = register("small_squeegee", () -> new SqueegeeItem(new Item.Properties().stacksTo(1).durability(128), true));
    public static final DeferredItem<HeightMapItem> HEIGHT_MAP = register("height_map", () -> new HeightMapItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> EMPTY_MAP = item("empty_map", new Item.Properties().stacksTo(16));
    public static final DeferredItem<CartographerItem> CARTOGRAPHER = register("cartographer", () -> new CartographerItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> TASTY_CLAY = item("tasty_clay", new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.2F).build()));
    public static final DeferredItem<GoldenEyeItem> GOLDEN_EYE = register("golden_eye", () -> new GoldenEyeItem(new Item.Properties().stacksTo(1).durability(100)));
    public static final DeferredItem<CursorItem> CURSOR = register("cursor", () -> new CursorItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<InfoBookItem> INFO_BOOK = register("info_book", () -> new InfoBookItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<DevNullItem> DEV_NULL = register("dev_null", () -> new DevNullItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<SpongeOnAStickItem> SPONGE_ON_A_STICK = register("sponge_on_a_stick", () -> new SpongeOnAStickItem(new Item.Properties().stacksTo(1).durability(256)));
    public static final DeferredItem<PedometerItem> PEDOMETER = register("pedometer", () -> new PedometerItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<EpicEraserItem> EPIC_ERASER = register("epic_eraser", () -> new EpicEraserItem(new Item.Properties().stacksTo(1).durability(256)));
    public static final DeferredItem<WrenchItem> WRENCH = register("wrench", () -> new WrenchItem(new Item.Properties().stacksTo(1).durability(512)));
    public static final DeferredItem<GlyphItem> GLYPH = register("glyph", () -> new GlyphItem(new Item.Properties()));

    // 1.12 metadata items are flattened to stable modern registry IDs.
    public static final DeferredItem<Item> GLIDER_WING = item("glider_wing", new Item.Properties());
    public static final DeferredItem<Item> BEAM = item("beam", new Item.Properties());
    public static final DeferredItem<Item> CRANE_ENGINE = item("crane_engine", new Item.Properties());
    public static final DeferredItem<Item> CRANE_MAGNET = item("crane_magnet", new Item.Properties());
    public static final DeferredItem<MagnetItem> MIRACLE_MAGNET = register("miracle_magnet", () -> new MagnetItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> LINE = item("line", new Item.Properties());
    public static final DeferredItem<Item> MAP_CONTROLLER = item("map_controller", new Item.Properties());
    public static final DeferredItem<Item> MAP_MEMORY = item("map_memory", new Item.Properties());
    public static final DeferredItem<Item> ASSISTANT_BASE = item("assistant_base", new Item.Properties());
    public static final DeferredItem<Item> UNPREPARED_STENCIL = item("unprepared_stencil", new Item.Properties());
    public static final DeferredItem<Item> SKETCHING_PENCIL = item("sketching_pencil", new Item.Properties().stacksTo(1));
    public static final DeferredItem<PointerItem> POINTER = register("pointer", () -> new PointerItem(new Item.Properties().stacksTo(1)));

    private static DeferredItem<Item> item(String id, Item.Properties properties) {
        return register(id, () -> new Item(properties));
    }

    private static <T extends Item> DeferredItem<T> register(String id, java.util.function.Supplier<T> supplier) {
        DeferredItem<T> item = ITEMS.register(id, supplier);
        ALL_ITEMS.put(id, item);
        return item;
    }

    private ModItems() {
    }
}
