package net.xuwu.openblocks_reborn.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.xuwu.openblocks_reborn.blockentity.SkyBlockEntity;
import net.xuwu.openblocks_reborn.item.SkyBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import java.util.List;

public class SkyBlock extends Block implements EntityBlock {
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");
    public static final BooleanProperty INVERTED = BooleanProperty.create("inverted");

    public SkyBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(POWERED, false).setValue(INVERTED, false));
    }

    public static boolean isActive(BlockState state) {
        return state.getValue(POWERED) != state.getValue(INVERTED);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED, INVERTED);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SkyBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return isActive(state) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // The legacy active sky block deliberately had no selection outline,
        // while retaining a normal solid collision volume.
        return isActive(state) ? Shapes.empty() : Shapes.block();
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor,
                                   BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide) level.scheduleTick(pos, this, 1);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean powered = level.hasNeighborSignal(pos);
        if (powered != state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_ALL);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            level.setBlock(pos, state.cycle(INVERTED), Block.UPDATE_ALL);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(SkyBlockItem.create(state.getValue(INVERTED)));
    }
}
