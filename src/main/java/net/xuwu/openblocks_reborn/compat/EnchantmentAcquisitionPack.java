package net.xuwu.openblocks_reborn.compat;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.xuwu.openblocks_reborn.config.EnchantmentConfig;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Additive vanilla tag resources; never replaces another mod's enchantment lists. */
public final class EnchantmentAcquisitionPack extends AbstractPackResources {
    private final Map<ResourceLocation, String> tags;
    private EnchantmentAcquisitionPack(PackLocationInfo info) {
        super(info);
        tags = Map.of(ResourceLocation.withDefaultNamespace("tags/enchantment/treasure.json"), tag(true),
                ResourceLocation.withDefaultNamespace("tags/enchantment/non_treasure.json"), tag(false));
    }
    public static String tag(boolean treasure) {
        var values = new ArrayList<String>();
        if (EnchantmentConfig.LAST_STAND_TREASURE.get() == treasure) values.add("\"openblocks_reborn:last_stand\"");
        if (EnchantmentConfig.UNSTABLE_TREASURE.get() == treasure) values.add("\"openblocks_reborn:unstable\"");
        if (EnchantmentConfig.FLIM_FLAM_TREASURE.get() == treasure) values.add("\"openblocks_reborn:flim_flam\"");
        return "{\"replace\":false,\"values\":[" + String.join(",", values) + "]}";
    }
    public static void register(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) return;
        event.addRepositorySource(output -> {
            var info = new PackLocationInfo("openblocks_reborn/enchantment_acquisition",
                    Component.literal("OpenBlocks enchantment acquisition"), PackSource.BUILT_IN, Optional.empty());
            var supplier = new Pack.ResourcesSupplier() {
                public PackResources openPrimary(PackLocationInfo location) { return new EnchantmentAcquisitionPack(location); }
                public PackResources openFull(PackLocationInfo location, Pack.Metadata metadata) { return new EnchantmentAcquisitionPack(location); }
            };
            Pack pack = Pack.readMetaAndCreate(info, supplier, PackType.SERVER_DATA,
                    new PackSelectionConfig(true, Pack.Position.TOP, true));
            if (pack != null) output.accept(pack.hidden());
        });
    }
    private static IoSupplier<InputStream> stream(String value) {
        return () -> new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }
    @Override public IoSupplier<InputStream> getRootResource(String... path) {
        return path.length == 1 && path[0].equals("pack.mcmeta")
                ? stream("{\"pack\":{\"pack_format\":48,\"description\":\"OpenBlocks enchantment acquisition\"}}") : null;
    }
    @Override public IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
        return type == PackType.SERVER_DATA && tags.containsKey(location) ? stream(tags.get(location)) : null;
    }
    @Override public void listResources(PackType type, String namespace, String prefix, ResourceOutput output) {
        if (type == PackType.SERVER_DATA) tags.forEach((key, value) -> {
            if (key.getNamespace().equals(namespace) && key.getPath().startsWith(prefix)) output.accept(key, stream(value));
        });
    }
    @Override public Set<String> getNamespaces(PackType type) { return type == PackType.SERVER_DATA ? Set.of("minecraft") : Set.of(); }
    @Override public void close() {}
}
