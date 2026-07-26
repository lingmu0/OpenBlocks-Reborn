package net.xuwu.openblocks_reborn.item;

import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundOpenBookPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;
import net.xuwu.openblocks_reborn.compat.PatchouliCompat;
import net.xuwu.openblocks_reborn.registry.ModFluids;
import net.xuwu.openblocks_reborn.registry.ModItems;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InfoBookItem extends Item {
    private static final int DIRECT_INVENTORY_CONTAINER_ID = -2;
    private static final int OFFHAND_INVENTORY_SLOT = 40;
    private static final Set<String> MACHINE_BLOCKS = Set.of(
            "tank", "sprinkler", "cannon", "vacuum_hopper", "fan", "xp_bottler",
            "auto_anvil", "auto_enchantment_table", "xp_drain", "block_breaker",
            "block_placer", "item_dropper", "donation_station", "paint_mixer",
            "drawing_table", "projector", "xp_shower");
    private static final Set<String> EQUIPMENT = Set.of(
            "hang_glider", "sonic_glasses", "pencil_glasses", "crayon_glasses",
            "technicolor_glasses", "serious_glasses", "crane_backpack", "sleeping_bag");
    private static final Set<String> COMPONENTS = Set.of(
            "generic", "generic_unstackable", "glider_wing", "beam", "crane_engine",
            "crane_magnet", "line", "map_controller", "map_memory", "assistant_base",
            "unprepared_stencil", "tasty_clay");

    public InfoBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (usesPatchouli()) {
                PatchouliCompat.openGuide(serverPlayer);
                return InteractionResultHolder.success(stack);
            }
            ListTag pages = new ListTag();
            buildPages().forEach(page -> pages.add(StringTag.valueOf(
                    Component.Serializer.toJson(page))));
            stack.getOrCreateTag().putString("title", "OpenBlocks Reborn");
            stack.getOrCreateTag().putString("author", "OpenBlocks");
            stack.getOrCreateTag().putInt("generation", 0);
            stack.getOrCreateTag().putBoolean("resolved", true);
            stack.getOrCreateTag().put("pages", pages);
            int inventorySlot = hand == InteractionHand.MAIN_HAND
                    ? serverPlayer.getInventory().selected
                    : OFFHAND_INVENTORY_SLOT;
            // ClientboundOpenBookPacket only identifies the hand. Synchronize the
            // newly written stack first so a freshly obtained guide opens on the
            // first use instead of requiring a second click.
            serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(
                    DIRECT_INVENTORY_CONTAINER_ID, 0, inventorySlot, stack.copy()));
            serverPlayer.connection.send(new ClientboundOpenBookPacket(hand));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    public static boolean usesPatchouli() {
        return ModList.get().isLoaded("patchouli");
    }

    public static List<Component> buildPages() {
        Map<ResourceLocation, ItemStack> registered = new LinkedHashMap<>();
        ModItems.BLOCK_ITEMS.values().forEach(holder -> addEntry(registered, holder.get()));
        ModItems.ALL_ITEMS.values().forEach(holder -> addEntry(registered, holder.get()));
        addEntry(registered, ModFluids.XP_BUCKET.get());

        List<GuideEntry> entries = registered.entrySet().stream()
                .map(entry -> new GuideEntry(entry.getKey(), entry.getValue(),
                        category(entry.getKey(), entry.getValue())))
                .sorted(Comparator.comparingInt((GuideEntry entry) -> entry.category().ordinal())
                        .thenComparing(entry -> entry.id().toString()))
                .toList();

        List<Component> pages = new ArrayList<>();
        pages.add(Component.translatable(
                "message.openblocks_reborn.book.catalog_welcome", entries.size()));
        for (Category category : Category.values()) {
            List<GuideEntry> categoryEntries = entries.stream()
                    .filter(entry -> entry.category() == category).toList();
            if (categoryEntries.isEmpty()) continue;
            pages.add(Component.translatable(
                    "message.openblocks_reborn.book.category",
                    Component.translatable(category.titleKey), categoryEntries.size(),
                    Component.translatable(category.descriptionKey)));
            for (GuideEntry entry : categoryEntries) {
                pages.add(Component.translatable(
                        "message.openblocks_reborn.book.entry",
                        entry.stack().getHoverName(),
                        Component.translatable(category.titleKey),
                        entry.id().toString(),
                        Component.translatable(entryDescriptionKey(entry))));
            }
        }
        if (pages.size() > 100) {
            throw new IllegalStateException("OpenBlocks guide exceeds the vanilla 100-page limit: "
                    + pages.size());
        }
        return List.copyOf(pages);
    }

    private static void addEntry(Map<ResourceLocation, ItemStack> entries, Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        entries.putIfAbsent(id, new ItemStack(item));
    }

    private static Category category(ResourceLocation id, ItemStack stack) {
        if (id.equals(BuiltInRegistries.ITEM.getKey(ModFluids.XP_BUCKET.get()))) return Category.FLUIDS;
        if (MACHINE_BLOCKS.contains(id.getPath())) return Category.MACHINES;
        if (stack.getItem() instanceof BlockItem) return Category.WORLD_BLOCKS;
        if (EQUIPMENT.contains(id.getPath())) return Category.EQUIPMENT;
        if (COMPONENTS.contains(id.getPath())) return Category.COMPONENTS;
        return Category.TOOLS;
    }

    private static String entryDescriptionKey(GuideEntry entry) {
        return "message.openblocks_reborn.book.detail." + entry.id().getPath();
    }

    private record GuideEntry(ResourceLocation id, ItemStack stack, Category category) {
    }

    private enum Category {
        MACHINES("machines"),
        WORLD_BLOCKS("world_blocks"),
        EQUIPMENT("equipment"),
        TOOLS("tools"),
        COMPONENTS("components"),
        FLUIDS("fluids");

        private final String titleKey;
        private final String descriptionKey;

        Category(String path) {
            titleKey = "message.openblocks_reborn.book.category." + path;
            descriptionKey = titleKey + ".description";
        }
    }
}
