package net.xuwu.openblocks_reborn.client;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.xuwu.openblocks_reborn.entity.MiniMeEntity;

import java.util.UUID;

public class MiniMeRenderer extends MobRenderer<MiniMeEntity, PlayerModel<MiniMeEntity>> {
    public MiniMeRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.3F);
    }

    @Override
    public ResourceLocation getTextureLocation(MiniMeEntity entity) {
        UUID owner = entity.getOwnerId().orElse(entity.getUUID());
        return Minecraft.getInstance().getSkinManager()
                .getInsecureSkinLocation(new GameProfile(owner, "MiniMe"));
    }

    @Override
    protected void scale(MiniMeEntity entity, PoseStack poseStack, float partialTickTime) {
        poseStack.scale(0.5F, 0.5F, 0.5F);
    }
}
