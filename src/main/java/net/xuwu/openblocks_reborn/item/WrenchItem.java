package net.xuwu.openblocks_reborn.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.AbstractChestBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class WrenchItem extends Item {
    public WrenchItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockState state = context.getLevel().getBlockState(context.getClickedPos());
        Player player = context.getPlayer();
        if (requiresSneaking(state.getBlock()) && (player == null || !player.isShiftKeyDown())) {
            return InteractionResult.FAIL;
        }
        BlockState rotated = rotateAroundFace(state, context.getClickedFace());
        if (!rotated.equals(state)) {
            if (!context.getLevel().isClientSide) {
                context.getLevel().setBlock(context.getClickedPos(), rotated, 3);
                context.getLevel().playSound(null, context.getClickedPos(), SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.5F, 1.2F);
                if (player != null) context.getItemInHand().hurtAndBreak(
                        1, player, player.getEquipmentSlotForItem(context.getItemInHand()));
            }
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }
        return InteractionResult.PASS;
    }

    private static boolean requiresSneaking(Block block) {
        return block instanceof LeverBlock || block instanceof ButtonBlock
                || block instanceof AbstractChestBlock<?>;
    }

    public static BlockState rotateAroundFace(BlockState state, Direction face) {
        Direction.Axis around = face.getAxis();
        if (state.hasProperty(BlockStateProperties.ORIENTATION)) {
            FrontAndTop orientation = state.getValue(BlockStateProperties.ORIENTATION);
            Direction front = rotateDirection(orientation.front(), face);
            Direction top = rotateDirection(orientation.top(), face);
            FrontAndTop rotated = FrontAndTop.fromFrontAndTop(front, top);
            if (rotated != null && rotated != orientation) {
                return state.setValue(BlockStateProperties.ORIENTATION, rotated);
            }
        }
        for (var property : state.getProperties()) {
            if (property instanceof DirectionProperty directionProperty) {
                Direction current = state.getValue(directionProperty);
                Direction rotated = rotateDirection(current, face);
                if (rotated != current && directionProperty.getPossibleValues().contains(rotated)) {
                    return state.setValue(directionProperty, rotated);
                }
            }
        }
        if (state.hasProperty(BlockStateProperties.AXIS)) {
            return rotateAxisProperty(state, BlockStateProperties.AXIS, around);
        }
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_AXIS) && around == Direction.Axis.Y) {
            return rotateAxisProperty(state, BlockStateProperties.HORIZONTAL_AXIS, around);
        }
        if (around == Direction.Axis.Y && state.hasProperty(BlockStateProperties.ROTATION_16)) {
            IntegerProperty rotation = BlockStateProperties.ROTATION_16;
            int amount = face.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 4 : -4;
            return state.setValue(rotation, (state.getValue(rotation) + amount) & 15);
        }
        if (around != Direction.Axis.Y) return state;
        return state.rotate(face.getAxisDirection() == Direction.AxisDirection.POSITIVE
                ? Rotation.CLOCKWISE_90 : Rotation.COUNTERCLOCKWISE_90);
    }

    private static Direction rotateDirection(Direction direction, Direction face) {
        if (direction.getAxis() == face.getAxis()) return direction;
        return face.getAxisDirection() == Direction.AxisDirection.POSITIVE
                ? direction.getClockWise(face.getAxis())
                : direction.getCounterClockWise(face.getAxis());
    }

    private static Direction.Axis rotateAxis(Direction.Axis current, Direction.Axis around) {
        if (current == around) return current;
        return switch (around) {
            case X -> current == Direction.Axis.Y ? Direction.Axis.Z : Direction.Axis.Y;
            case Y -> current == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
            case Z -> current == Direction.Axis.X ? Direction.Axis.Y : Direction.Axis.X;
        };
    }

    private static BlockState rotateAxisProperty(BlockState state,
                                                 EnumProperty<Direction.Axis> property,
                                                 Direction.Axis around) {
        Direction.Axis current = state.getValue(property);
        Direction.Axis rotated = rotateAxis(current, around);
        return rotated != current && property.getPossibleValues().contains(rotated)
                ? state.setValue(property, rotated)
                : state;
    }

    @Override
    public boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player) {
        return true;
    }
}
