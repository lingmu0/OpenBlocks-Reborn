package net.xuwu.openblocks_reborn.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.xuwu.openblocks_reborn.registry.ModBlocks;

public class TrophyItemRenderer extends BlockEntityWithoutLevelRenderer {
    private ResourceLocation cachedType;
    private ClientLevel cachedLevel;
    private Entity cachedEntity;

    public TrophyItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                             MultiBufferSource buffers, int packedLight, int packedOverlay) {
        Minecraft minecraft = Minecraft.getInstance();
        poseStack.pushPose();
        if (displayContext.firstPerson() || displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                || displayContext == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                || displayContext == ItemDisplayContext.FIXED) {
            poseStack.translate(0.5D, 0.5D, 0.5D);
            poseStack.scale(0.65F, 0.65F, 0.65F);
            poseStack.translate(-0.5D, -0.5D, -0.5D);
        }
        minecraft.getBlockRenderer().renderSingleBlock(ModBlocks.TROPHY.get().defaultBlockState(),
                poseStack, buffers, packedLight, packedOverlay);

        Entity entity = getDisplayEntity(stack, minecraft.level);
        if (entity != null) {
            poseStack.pushPose();
            poseStack.translate(0.0D, 0.25D, 0.0D);
            TrophyRenderer.renderDisplayEntity(0.0F, poseStack, buffers, packedLight,
                    entity, minecraft.getEntityRenderDispatcher(), 20.0D);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private Entity getDisplayEntity(ItemStack stack, ClientLevel level) {
        if (level == null) return null;
        CompoundTag data = BlockItem.getBlockEntityData(stack);
        if (data == null) data = new CompoundTag();
        ResourceLocation type = ResourceLocation.tryParse(data.getString("EntityType"));
        if (type == null) type = ResourceLocation.withDefaultNamespace("pig");
        if (!type.equals(cachedType) || cachedLevel != level || cachedEntity == null) {
            cachedType = type;
            cachedLevel = level;
            cachedEntity = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                    .getOptional(type).map(entityType -> entityType.create(level)).orElse(null);
        }
        return cachedEntity;
    }
}
