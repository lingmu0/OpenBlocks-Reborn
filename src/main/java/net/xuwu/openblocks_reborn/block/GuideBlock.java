package net.xuwu.openblocks_reborn.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.xuwu.openblocks_reborn.blockentity.GuideBlockEntity;
import net.xuwu.openblocks_reborn.registry.ModItems;
import net.xuwu.openblocks_reborn.item.WrenchItem;

import java.util.List;

public class GuideBlock extends Block implements EntityBlock {
    public static final EnumProperty<FrontAndTop> ORIENTATION = BlockStateProperties.ORIENTATION;
    private final boolean builder;

    public GuideBlock(BlockBehaviour.Properties properties, boolean builder) {
        super(properties);
        this.builder = builder;
        registerDefaultState(stateDefinition.any().setValue(ORIENTATION, FrontAndTop.NORTH_UP));
    }

    @Override
    public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ORIENTATION);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        /*
         * FrontAndTop's second direction is the baked model's local up axis. The old
         * implementation accidentally put the clicked UP face into the first slot,
         * producing UP_SOUTH and rotating the two no-minus end textures onto the
         * north/south sides. A newly placed Guide is upright: its end textures are
         * always on world UP/DOWN and the player's horizontal direction selects yaw.
         * The wrench can still move it through the other supported orientations.
         */
        Direction front = context.getHorizontalDirection().getOpposite();
        return defaultBlockState().setValue(ORIENTATION,
                FrontAndTop.fromFrontAndTop(front, Direction.UP));
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(ORIENTATION, rotation.rotation().rotate(state.getValue(ORIENTATION)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(ORIENTATION, mirror.rotation().rotate(state.getValue(ORIENTATION)));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GuideBlockEntity(pos, state);
    }

    public boolean isBuilder() {
        return builder;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof GuideBlockEntity guide) {
            guide.updatePowered();
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor,
                                   BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof GuideBlockEntity guide) {
            guide.updatePowered();
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        InteractionResult held = useHeldItem(player.getItemInHand(hand), state, level, pos, player, hand, hit);
        if (held != InteractionResult.PASS) return held;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof GuideBlockEntity guide) {
            guide.handleControl(player, hit);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public InteractionResult useHeldItem(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof GuideBlockEntity guide)) {
            return InteractionResult.PASS;
        }
        if (stack.getItem() instanceof DyeItem dye) {
            if (!level.isClientSide) {
                guide.setColor(player, dye.getDyeColor().getTextColor());
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.is(ModItems.WRENCH.get())) {
            if (!level.isClientSide) {
                BlockState rotated = WrenchItem.rotateAroundFace(state, hit.getDirection());
                if (!rotated.equals(state)) {
                    level.setBlock(pos, rotated, Block.UPDATE_ALL);
                    level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_OPEN,
                            SoundSource.BLOCKS, 0.5F, 1.2F);
                    stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(player.getEquipmentSlotForItem(stack)));
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (builder && stack.getItem() instanceof BlockItem blockItem) {
            if (!level.isClientSide && level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                guide.placeBlocks(serverLevel, player, hand, stack, blockItem, hit.getDirection());
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide) guide.handleControl(player, hit);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity blockEntity = params.getOptionalParameter(
                net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY);
        return blockEntity instanceof GuideBlockEntity guide
                ? List.of(guide.toItemStack())
                : List.of(new ItemStack(this));
    }
}
