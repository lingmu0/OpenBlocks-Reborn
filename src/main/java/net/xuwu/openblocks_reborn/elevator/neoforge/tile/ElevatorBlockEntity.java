// Derived from ElevatorMod 1.21.1-1.11.4 by VsnGamer (MIT); see LICENSE-ElevatorMod.
package net.xuwu.openblocks_reborn.elevator.neoforge.tile;

import net.xuwu.openblocks_reborn.elevator.level.ElevatorBlockEntityBase;
import net.xuwu.openblocks_reborn.elevator.level.ElevatorContainer;
import net.xuwu.openblocks_reborn.elevator.neoforge.client.render.ElevatorBakedModel;
import net.xuwu.openblocks_reborn.elevator.neoforge.init.Registry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import javax.annotation.Nullable;

public class ElevatorBlockEntity extends ElevatorBlockEntityBase {
    public ElevatorBlockEntity(BlockPos pos, BlockState state) {
        super(Registry.ELEVATOR_TILE_ENTITY.get(), pos, state);
    }

    @Override
    protected SoundEvent camouflageSound() {
        return Registry.CAMOUFLAGE_SOUND.get();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ElevatorContainer(Registry.ELEVATOR_CONTAINER.get(), id, worldPosition, player);
    }

    @Override
    public ModelData getModelData() {
        return ModelData.builder().with(ElevatorBakedModel.HELD_STATE, heldState).build();
    }

    @Override
    public void setChanged() {
        super.setChanged();

        requestModelDataUpdate();

        var auxLightManager = level != null ? level.getAuxLightManager(worldPosition) : null;
        if (auxLightManager == null) {
            return;
        }

        auxLightManager.setLightAt(
                worldPosition,
                heldState != null ? heldState.getLightEmission(level, worldPosition) : 0
        );
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        super.handleUpdateTag(tag, lookupProvider);
        setChanged();
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider provider) {
        handleUpdateTag(pkt.getTag(), provider);
    }
}
