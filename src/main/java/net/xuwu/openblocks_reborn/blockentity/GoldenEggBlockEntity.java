package net.xuwu.openblocks_reborn.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.xuwu.openblocks_reborn.entity.MiniMeEntity;
import net.xuwu.openblocks_reborn.registry.ModBlockEntities;
import net.xuwu.openblocks_reborn.registry.ModEntities;

import java.util.UUID;

public class GoldenEggBlockEntity extends BlockEntity {
    public static final int HATCH_TIME = 600;
    private UUID owner;
    private String ownerName = "Player";
    private int progress;

    public GoldenEggBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GOLDEN_EGG.get(), pos, state);
    }

    public void setOwner(UUID owner, String ownerName) {
        this.owner = owner;
        this.ownerName = ownerName;
        setChanged();
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = Math.max(0, progress);
        setChanged();
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, GoldenEggBlockEntity egg) {
        egg.progress += level.hasNeighborSignal(pos) ? 4 : 1;
        if (egg.progress % 40 == 0) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5D, pos.getY() + 0.7D,
                    pos.getZ() + 0.5D, 2, 0.2D, 0.25D, 0.2D, 0.0D);
            egg.setChanged();
        }
        if (egg.progress < HATCH_TIME) return;
        MiniMeEntity miniMe = new MiniMeEntity(ModEntities.MINI_ME.get(), level);
        miniMe.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
        if (egg.owner != null) miniMe.setOwner(egg.owner);
        miniMe.setCustomName(Component.literal(egg.ownerName + "'s Mini Me"));
        miniMe.setCustomNameVisible(true);
        level.addFreshEntity(miniMe);
        level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 1.0F, 1.35F);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, pos.getX() + 0.5D, pos.getY() + 0.7D,
                pos.getZ() + 0.5D, 30, 0.4D, 0.5D, 0.4D, 0.1D);
        level.removeBlock(pos, false);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.hasUUID("Owner")) owner = tag.getUUID("Owner");
        ownerName = tag.getString("OwnerName");
        progress = tag.getInt("Progress");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (owner != null) tag.putUUID("Owner", owner);
        tag.putString("OwnerName", ownerName);
        tag.putInt("Progress", progress);
    }
}
