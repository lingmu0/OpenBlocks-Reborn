package net.xuwu.openblocks_reborn.item;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.xuwu.openblocks_reborn.client.CannonItemRenderer;
import net.xuwu.openblocks_reborn.client.TrophyItemRenderer;

import java.util.function.Consumer;

/** Restores Forge 1.20.1's item-side custom renderer registration path. */
public final class SpecialRenderedBlockItem extends BlockItem {
    public enum Renderer {
        CANNON, TROPHY
    }

    private final Renderer renderer;

    public SpecialRenderedBlockItem(Block block, Properties properties, Renderer renderer) {
        super(block, properties);
        this.renderer = renderer;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer instance;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (instance == null) {
                    instance = renderer == Renderer.CANNON
                            ? new CannonItemRenderer() : new TrophyItemRenderer();
                }
                return instance;
            }
        });
    }
}
