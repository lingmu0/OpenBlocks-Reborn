package net.xuwu.openblocks_reborn.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.xuwu.openblocks_reborn.OpenBlocksReborn;
import net.xuwu.openblocks_reborn.entity.LuggageEntity;
import net.xuwu.openblocks_reborn.entity.MiniMeEntity;
import net.xuwu.openblocks_reborn.entity.GlyphEntity;
import net.xuwu.openblocks_reborn.entity.HangGliderEntity;
import net.xuwu.openblocks_reborn.entity.GoldenEyeEntity;
import net.xuwu.openblocks_reborn.entity.CraneCarriedBlockEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, OpenBlocksReborn.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<LuggageEntity>> LUGGAGE =
            ENTITY_TYPES.register("luggage", () -> EntityType.Builder
                    .of(LuggageEntity::new, MobCategory.CREATURE)
                    .sized(0.7F, 0.7F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("openblocks_reborn:luggage"));
    public static final DeferredHolder<EntityType<?>, EntityType<MiniMeEntity>> MINI_ME =
            ENTITY_TYPES.register("mini_me", () -> EntityType.Builder
                    .of(MiniMeEntity::new, MobCategory.CREATURE)
                    .sized(0.3F, 0.9F)
                    .clientTrackingRange(10)
                    .build("openblocks_reborn:mini_me"));
    public static final DeferredHolder<EntityType<?>, EntityType<GlyphEntity>> GLYPH =
            ENTITY_TYPES.register("glyph", () -> EntityType.Builder
                    .<GlyphEntity>of(GlyphEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(10)
                    .updateInterval(Integer.MAX_VALUE)
                    .build("openblocks_reborn:glyph"));
    public static final DeferredHolder<EntityType<?>, EntityType<HangGliderEntity>> HANG_GLIDER =
            ENTITY_TYPES.register("hang_glider", () -> EntityType.Builder
                    .<HangGliderEntity>of(HangGliderEntity::new, MobCategory.MISC)
                    .sized(4.8F, 0.2F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("openblocks_reborn:hang_glider"));
    public static final DeferredHolder<EntityType<?>, EntityType<GoldenEyeEntity>> GOLDEN_EYE =
            ENTITY_TYPES.register("golden_eye", () -> EntityType.Builder
                    .<GoldenEyeEntity>of(GoldenEyeEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build("openblocks_reborn:golden_eye"));
    public static final DeferredHolder<EntityType<?>, EntityType<CraneCarriedBlockEntity>> CRANE_CARRIED_BLOCK =
            ENTITY_TYPES.register("crane_carried_block", () -> EntityType.Builder
                    .<CraneCarriedBlockEntity>of(CraneCarriedBlockEntity::new, MobCategory.MISC)
                    .sized(0.98F, 0.98F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("openblocks_reborn:crane_carried_block"));

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(LUGGAGE.get(), Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 12.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .build());
        event.put(MINI_ME.get(), Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .build());
    }

    private ModEntities() {
    }
}
