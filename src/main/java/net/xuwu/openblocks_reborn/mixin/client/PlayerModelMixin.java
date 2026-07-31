package net.xuwu.openblocks_reborn.mixin.client;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.xuwu.openblocks_reborn.item.HangGliderItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replaces walking-sized limb swings with a restrained airborne sway. */
@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void openblocksReborn$calmGliderLimbs(LivingEntity entity, float limbSwing,
                                                   float limbSwingAmount, float ageInTicks,
                                                   float netHeadYaw, float headPitch,
                                                   CallbackInfo callback) {
        if (!(entity instanceof Player player) || !HangGliderItem.isActivelyGliding(player)) return;

        PlayerModel<?> model = (PlayerModel<?>)(Object)this;
        float sway = Mth.sin(ageInTicks * 0.12F) * 0.075F;
        model.rightArm.xRot = sway;
        model.leftArm.xRot = -sway;
        model.rightLeg.xRot = -sway * 0.65F;
        model.leftLeg.xRot = sway * 0.65F;
        model.leftSleeve.copyFrom(model.leftArm);
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftPants.copyFrom(model.leftLeg);
        model.rightPants.copyFrom(model.rightLeg);
    }
}
