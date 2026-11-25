// Derived from ElevatorMod 1.21.1-1.11.4 by VsnGamer (MIT); see LICENSE-ElevatorMod.
package net.xuwu.openblocks_reborn.elevator.neoforge.init;

import net.xuwu.openblocks_reborn.elevator.ElevatorMod;
import net.xuwu.openblocks_reborn.elevator.neoforge.ElevatorBlock;
import net.xuwu.openblocks_reborn.elevator.neoforge.tile.ElevatorBlockEntity;
import net.xuwu.openblocks_reborn.elevator.level.ElevatorContainer;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.function.Supplier;

public class Registry {
    public static final EnumMap<DyeColor, DeferredBlock<ElevatorBlock>> ELEVATOR_BLOCKS = new EnumMap<>(DyeColor.class);
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ElevatorMod.ID);

    static {
        Arrays.stream(DyeColor.values()).forEach(color ->
                ELEVATOR_BLOCKS.put(color, BLOCKS.register("elevator_" + color.getName(), () -> new ElevatorBlock(color)))
        );
    }

    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ElevatorMod.ID);
    public static final Supplier<BlockEntityType<ElevatorBlockEntity>> ELEVATOR_TILE_ENTITY = BLOCK_ENTITIES.register(
            "elevator_tile",
            () -> BlockEntityType.Builder.of(
                    ElevatorBlockEntity::new,
                    ELEVATOR_BLOCKS.values().stream().map(DeferredBlock::get).toArray(Block[]::new)
            ).build(null)
    );

    public static final EnumMap<DyeColor, DeferredItem<BlockItem>> ELEVATOR_ITEMS = new EnumMap<>(DyeColor.class);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ElevatorMod.ID);

    static {
        ELEVATOR_BLOCKS.forEach((color, o) ->
                ELEVATOR_ITEMS.put(color, ITEMS.registerSimpleBlockItem(o))
        );
    }

    private static final DeferredRegister<MenuType<?>> CONTAINERS = DeferredRegister.create(Registries.MENU, ElevatorMod.ID);
    public static Supplier<MenuType<ElevatorContainer>> ELEVATOR_CONTAINER = null;
    static {
        ELEVATOR_CONTAINER = CONTAINERS.register(
                "elevator_container", () ->
                        IMenuTypeExtension.create((windowId, inv, data) ->
                                new ElevatorContainer(ELEVATOR_CONTAINER.get(), windowId, data.readBlockPos(), inv.player)
                        )
        );
    }

    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, ElevatorMod.ID);
    public static final Supplier<SoundEvent> TELEPORT_SOUND = SOUNDS.register(
            "teleport", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ElevatorMod.ID, "teleport"))
    );
    public static final Supplier<SoundEvent> CAMOUFLAGE_SOUND = SOUNDS.register(
            "camouflage", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ElevatorMod.ID, "camouflage"))
    );

    public static void init(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        CONTAINERS.register(modEventBus);
        SOUNDS.register(modEventBus);
    }

    // TODO: Config GUI
    /*@SubscribeEvent
    public static void configChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals(ElevatorMod.ID)) {
            ConfigManager.sync(ElevatorMod.ID, Config.Type.INSTANCE);
        }
    }*/
}
