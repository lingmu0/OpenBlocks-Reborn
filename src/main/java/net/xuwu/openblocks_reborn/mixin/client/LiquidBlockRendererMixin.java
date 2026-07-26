package net.xuwu.openblocks_reborn.mixin.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.xuwu.openblocks_reborn.registry.ModFluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Suppresses only the virtual fluid mesh; the sprinkler block model remains. */
@Mixin(LiquidBlockRenderer.class)
public abstract class LiquidBlockRendererMixin {
    @Inject(method = "tesselate", at = @At("HEAD"), cancellable = true)
    private void openblocksReborn$hideSprinklerWater(BlockAndTintGetter level, BlockPos pos,
                                                     VertexConsumer buffer, BlockState blockState,
                                                     FluidState fluidState, CallbackInfo callback) {
        if (fluidState.getType() == ModFluids.SPRINKLER_WATER.get()) {
            callback.cancel();
        }
    }
}
