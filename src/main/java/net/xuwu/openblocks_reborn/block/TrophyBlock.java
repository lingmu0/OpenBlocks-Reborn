package net.xuwu.openblocks_reborn.block;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.xuwu.openblocks_reborn.blockentity.TrophyBlockEntity;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import java.util.List;

public class TrophyBlock extends Block implements EntityBlock {
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 6, 13);
    public TrophyBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TrophyBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof SpawnEggItem egg && level.getBlockEntity(pos) instanceof TrophyBlockEntity trophy) {
            if (!level.isClientSide) {
                trophy.setEntityType(egg.getType(stack));
                if (!player.getAbilities().instabuild) stack.shrink(1);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        var sounds = new net.minecraft.sounds.SoundEvent[]{
                SoundEvents.CREEPER_PRIMED, SoundEvents.ENDERMAN_AMBIENT, SoundEvents.BLAZE_AMBIENT,
                SoundEvents.CHICKEN_AMBIENT, SoundEvents.COW_AMBIENT, SoundEvents.ZOMBIE_AMBIENT
        };
        level.playSound(player, pos, sounds[level.random.nextInt(sounds.length)], SoundSource.BLOCKS, 0.8F, 0.9F + level.random.nextFloat() * 0.2F);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
                                  BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof TrophyBlockEntity trophy) return List.of(trophy.toItemStack());
        return super.getDrops(state, params);
    }
}
