package net.xuwu.openblocks_reborn.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.xuwu.openblocks_reborn.block.GuideBlock;
import net.xuwu.openblocks_reborn.item.WrenchItem;
import net.xuwu.openblocks_reborn.registry.ModBlockEntities;

import java.util.List;

public class GuideBlockEntity extends net.minecraft.world.level.block.entity.BlockEntity {
    public static final int DEFAULT_EXTENT = 8;
    public static final int MAX_EXTENT = 64;

    private int negX = DEFAULT_EXTENT;
    private int negY = DEFAULT_EXTENT;
    private int negZ = DEFAULT_EXTENT;
    private int posX = DEFAULT_EXTENT;
    private int posY = DEFAULT_EXTENT;
    private int posZ = DEFAULT_EXTENT;
    private GuideShape shape = GuideShape.SPHERE;
    private int color = 0xFFFFFF;
    private boolean active;
    private List<BlockPos> cachedShape;
    private FrontAndTop cachedOrientation;

    public GuideBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GUIDE.get(), pos, state);
    }

    public List<BlockPos> getShapePositions() {
        FrontAndTop orientation = getBlockState().hasProperty(GuideBlock.ORIENTATION)
                ? getBlockState().getValue(GuideBlock.ORIENTATION) : FrontAndTop.NORTH_UP;
        if (cachedShape == null || cachedOrientation != orientation) {
            cachedOrientation = orientation;
            cachedShape = shape.generate(negX, negY, negZ, posX, posY, posZ, 0).stream()
                    .map(position -> orient(position, orientation))
                    .filter(position -> !isBuilder()
                            || Math.abs(position.getX()) > 1
                            || Math.abs(position.getY()) > 1
                            || Math.abs(position.getZ()) > 1)
                    .toList();
        }
        return cachedShape;
    }

    private static BlockPos orient(BlockPos position, FrontAndTop orientation) {
        Direction front = orientation.front();
        Direction top = orientation.top();
        Direction right = cross(front, top);
        Direction back = front.getOpposite();
        return new BlockPos(
                position.getX() * right.getStepX() + position.getY() * top.getStepX()
                        + position.getZ() * back.getStepX(),
                position.getX() * right.getStepY() + position.getY() * top.getStepY()
                        + position.getZ() * back.getStepY(),
                position.getX() * right.getStepZ() + position.getY() * top.getStepZ()
                        + position.getZ() * back.getStepZ());
    }

    private static Direction cross(Direction first, Direction second) {
        int x = first.getStepY() * second.getStepZ() - first.getStepZ() * second.getStepY();
        int y = first.getStepZ() * second.getStepX() - first.getStepX() * second.getStepZ();
        int z = first.getStepX() * second.getStepY() - first.getStepY() * second.getStepX();
        return Direction.getNearest(x, y, z);
    }

    public int getColor() {
        return color;
    }

    public GuideShape getShapeMode() {
        return shape;
    }

    public boolean isActive() {
        return active;
    }

    public int getExtent(Direction direction) {
        return switch (direction) {
            case WEST -> negX;
            case DOWN -> negY;
            case NORTH -> negZ;
            case EAST -> posX;
            case UP -> posY;
            case SOUTH -> posZ;
        };
    }

    public void updatePowered() {
        if (level == null || level.isClientSide) return;
        boolean powered = level.hasNeighborSignal(worldPosition);
        if (active != powered) {
            active = powered;
            sync();
        }
    }

    public void setColor(Player player, int rgb) {
        color = rgb & 0xFFFFFF;
        sync();
        player.displayClientMessage(Component.translatable(
                "message.openblocks_reborn.guide_color", String.format("#%06X", color)), true);
    }

    public boolean handleControl(Player player, BlockHitResult hit) {
        FrontAndTop orientation = getBlockState().getValue(GuideBlock.ORIENTATION);
        Direction right = cross(orientation.front(), orientation.top());
        Direction back = orientation.front().getOpposite();
        Vec3 centered = hit.getLocation().subtract(worldPosition.getCenter());
        double x = 8.0D + 16.0D * dot(centered, right);
        double y = 8.0D + 16.0D * dot(centered, orientation.top());
        double z = 8.0D + 16.0D * dot(centered, back);
        Direction face = toLocalFace(hit.getDirection(), orientation, right);
        String command = findControl(face, x, y, z);
        return command != null && executeControl(player, command);
    }

    private static double dot(Vec3 vector, Direction direction) {
        return vector.x * direction.getStepX() + vector.y * direction.getStepY()
                + vector.z * direction.getStepZ();
    }

    private static Direction toLocalFace(Direction worldFace, FrontAndTop orientation,
                                         Direction right) {
        if (worldFace == orientation.top()) return Direction.UP;
        if (worldFace == orientation.top().getOpposite()) return Direction.DOWN;
        if (worldFace == orientation.front()) return Direction.NORTH;
        if (worldFace == orientation.front().getOpposite()) return Direction.SOUTH;
        return worldFace == right ? Direction.EAST : Direction.WEST;
    }

    private static boolean within(double value, double min, double max) {
        return value >= min && value <= max;
    }

    private static boolean box(double first, double second,
                               double firstMin, double firstMax,
                               double secondMin, double secondMax) {
        return within(first, firstMin, firstMax) && within(second, secondMin, secondMax);
    }

    private static String findControl(Direction face, double x, double y, double z) {
        if (face == Direction.UP) {
            if (box(x, z, 1, 5, 3, 14)) return "rotate_ccw";
            if (box(x, z, 11, 15, 3, 14)) return "rotate_cw";
            if (box(x, z, 5, 11, 2, 5)) return "inc_mode";
            if (box(x, z, 5, 11, 8, 11)) return "dec_mode";
            return null;
        }
        if (face == Direction.DOWN) {
            if (box(x, z, 11, 15, 2, 13)) return "rotate_ccw";
            if (box(x, z, 1, 5, 2, 13)) return "rotate_cw";
            if (box(x, z, 5, 11, 11, 14)) return "inc_mode";
            if (box(x, z, 5, 11, 5, 8)) return "dec_mode";
            return null;
        }
        if (face == Direction.NORTH) {
            if (box(x, y, 4, 7, 12, 15)) return "inc_pos_y";
            if (box(x, y, 9, 12, 12, 15)) return "dec_pos_y";
            if (box(x, y, 9, 12, 1, 4)) return "inc_neg_y";
            if (box(x, y, 4, 7, 1, 4)) return "dec_neg_y";
            if (box(x, y, 6, 10, 10, 12)) return "copy_pos_y";
            if (box(x, y, 6, 10, 4, 6)) return "copy_neg_y";
            if (box(x, y, 1, 4, 9, 12)) return "dec_neg_x";
            if (box(x, y, 1, 4, 4, 7)) return "inc_neg_x";
            if (box(x, y, 4, 6, 6, 10)) return "copy_neg_x";
            if (box(x, y, 12, 15, 9, 12)) return "inc_pos_x";
            if (box(x, y, 12, 15, 4, 7)) return "dec_pos_x";
            if (box(x, y, 10, 12, 6, 10)) return "copy_pos_x";
            return null;
        }
        if (face == Direction.SOUTH) {
            if (box(x, y, 9, 12, 12, 15)) return "inc_pos_y";
            if (box(x, y, 4, 7, 12, 15)) return "dec_pos_y";
            if (box(x, y, 4, 7, 1, 4)) return "inc_neg_y";
            if (box(x, y, 9, 12, 1, 4)) return "dec_neg_y";
            if (box(x, y, 6, 10, 10, 12)) return "copy_pos_y";
            if (box(x, y, 6, 10, 4, 6)) return "copy_neg_y";
            if (box(x, y, 1, 4, 9, 12)) return "inc_neg_x";
            if (box(x, y, 1, 4, 4, 7)) return "dec_neg_x";
            if (box(x, y, 4, 6, 6, 10)) return "copy_neg_x";
            if (box(x, y, 12, 15, 9, 12)) return "dec_pos_x";
            if (box(x, y, 12, 15, 4, 7)) return "inc_pos_x";
            if (box(x, y, 10, 12, 6, 10)) return "copy_pos_x";
            return null;
        }
        if (face == Direction.WEST) {
            if (box(z, y, 9, 12, 12, 15)) return "inc_pos_y";
            if (box(z, y, 4, 7, 12, 15)) return "dec_pos_y";
            if (box(z, y, 9, 12, 1, 4)) return "dec_neg_y";
            if (box(z, y, 4, 7, 1, 4)) return "inc_neg_y";
            if (box(z, y, 6, 10, 10, 12)) return "copy_pos_y";
            if (box(z, y, 6, 10, 4, 6)) return "copy_neg_y";
            if (box(z, y, 12, 15, 9, 12)) return "dec_pos_z";
            if (box(z, y, 12, 15, 4, 7)) return "inc_pos_z";
            if (box(z, y, 10, 12, 6, 10)) return "copy_pos_z";
            if (box(z, y, 1, 4, 9, 12)) return "inc_neg_z";
            if (box(z, y, 1, 4, 4, 7)) return "dec_neg_z";
            if (box(z, y, 4, 6, 6, 10)) return "copy_neg_z";
            return null;
        }
        if (box(z, y, 4, 7, 12, 15)) return "inc_pos_y";
        if (box(z, y, 9, 12, 12, 15)) return "dec_pos_y";
        if (box(z, y, 9, 12, 1, 4)) return "inc_neg_y";
        if (box(z, y, 4, 7, 1, 4)) return "dec_neg_y";
        if (box(z, y, 6, 10, 10, 12)) return "copy_pos_y";
        if (box(z, y, 6, 10, 4, 6)) return "copy_neg_y";
        if (box(z, y, 1, 4, 9, 12)) return "dec_neg_z";
        if (box(z, y, 1, 4, 4, 7)) return "inc_neg_z";
        if (box(z, y, 4, 6, 6, 10)) return "copy_neg_z";
        if (box(z, y, 12, 15, 9, 12)) return "inc_pos_z";
        if (box(z, y, 12, 15, 4, 7)) return "dec_pos_z";
        if (box(z, y, 10, 12, 6, 10)) return "copy_pos_z";
        return null;
    }

    private boolean executeControl(Player player, String command) {
        return switch (command) {
            case "rotate_ccw" -> {
                rotate(player, -1);
                yield true;
            }
            case "rotate_cw" -> {
                rotate(player, 1);
                yield true;
            }
            case "inc_mode" -> {
                cycleShape(player, 1);
                yield true;
            }
            case "dec_mode" -> {
                cycleShape(player, -1);
                yield true;
            }
            default -> executeAxisControl(player, command);
        };
    }

    private boolean executeAxisControl(Player player, String command) {
        int split = command.lastIndexOf('_');
        if (split < 0) return false;
        Direction direction = switch (command.substring(split + 1)) {
            case "x" -> command.contains("_pos_") ? Direction.EAST : Direction.WEST;
            case "y" -> command.contains("_pos_") ? Direction.UP : Direction.DOWN;
            case "z" -> command.contains("_pos_") ? Direction.SOUTH : Direction.NORTH;
            default -> null;
        };
        if (direction == null) return false;
        if (command.startsWith("inc_")) modify(player, direction, 1);
        else if (command.startsWith("dec_")) modify(player, direction, -1);
        else if (command.startsWith("copy_")) copyToOpposite(player, direction);
        else return false;
        return true;
    }

    public void rotate(Player player, int amount) {
        if (level == null) return;
        FrontAndTop orientation = getBlockState().getValue(GuideBlock.ORIENTATION);
        Direction axis = amount >= 0 ? orientation.top() : orientation.top().getOpposite();
        BlockState rotated = WrenchItem.rotateAroundFace(getBlockState(), axis);
        if (!rotated.equals(getBlockState())) level.setBlock(worldPosition, rotated, Block.UPDATE_ALL);
        cachedOrientation = null;
        cachedShape = null;
        showStatus(player);
    }

    private void cycleShape(Player player, int amount) {
        GuideShape[] values = GuideShape.values();
        shape = values[Math.floorMod(shape.ordinal() + amount, values.length)];
        changed();
        showStatus(player);
    }

    private void modify(Player player, Direction direction, int amount) {
        int value = Math.clamp(getExtent(direction) + amount, 0, MAX_EXTENT);
        setExtent(direction, value);
        changed();
        showStatus(player);
    }

    private void copyToOpposite(Player player, Direction from) {
        setExtent(from.getOpposite(), getExtent(from));
        changed();
        showStatus(player);
    }

    private void setExtent(Direction direction, int value) {
        switch (direction) {
            case WEST -> negX = value;
            case DOWN -> negY = value;
            case NORTH -> negZ = value;
            case EAST -> posX = value;
            case UP -> posY = value;
            case SOUTH -> posZ = value;
        }
    }

    private void showStatus(Player player) {
        player.displayClientMessage(Component.translatable(
                "message.openblocks_reborn.guide_status",
                -negX, -negY, -negZ, posX, posY, posZ,
                Component.translatable(shape.translationKey()), getShapePositions().size()), true);
    }

    public int placeBlocks(ServerLevel level, Player player, InteractionHand hand,
                           ItemStack stack, BlockItem blockItem, Direction side) {
        if (!active) {
            player.displayClientMessage(Component.translatable(
                    "message.openblocks_reborn.guide_needs_redstone"), true);
            return 0;
        }
        boolean fillMode = player.getAbilities().instabuild
                && level.getBlockState(worldPosition.above()).is(Blocks.OBSIDIAN);
        int placed = 0;
        for (BlockPos relative : getShapePositions()) {
            if (stack.isEmpty() && !player.getAbilities().instabuild) break;
            BlockPos target = worldPosition.offset(relative);
            if (!level.hasChunkAt(target) || target.getY() < level.getMinBuildHeight()
                    || target.getY() >= level.getMaxBuildHeight()) continue;
            BlockState existing = level.getBlockState(target);
            if (!fillMode && !existing.canBeReplaced()) continue;

            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(target), side, target, false);
            BlockPlaceContext placement = new BlockPlaceContext(level, player, hand, stack, hit);
            if (fillMode) {
                BlockState placedState = blockItem.getBlock().getStateForPlacement(placement);
                if (placedState == null || !placedState.canSurvive(level, target)) continue;
                if (level.setBlock(target, placedState, Block.UPDATE_ALL)) {
                    placed++;
                    sendPlacementTrail(level, target, placedState);
                }
            } else if (blockItem.place(placement).consumesAction()) {
                placed = 1;
                sendPlacementTrail(level, target, level.getBlockState(target));
                break;
            }
        }
        player.displayClientMessage(Component.translatable(
                "message.openblocks_reborn.guide_built", placed), true);
        return placed;
    }

    private void sendPlacementTrail(ServerLevel level, BlockPos target, BlockState state) {
        Vec3 start = worldPosition.getCenter();
        Vec3 end = target.getCenter();
        int steps = Math.max(1, (int)Math.ceil(start.distanceTo(end) * 2.0D));
        for (int step = 0; step <= steps; step++) {
            Vec3 point = start.lerp(end, step / (double)steps);
            level.sendParticles(ParticleTypes.PORTAL, point.x, point.y, point.z,
                    1, 0.03D, 0.03D, 0.03D, 0.0D);
        }
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                end.x, end.y, end.z, 5, 0.25D, 0.25D, 0.25D, 0.0D);
    }

    public ItemStack toItemStack() {
        ItemStack result = new ItemStack(getBlockState().getBlock());
        if (level != null) {
            BlockItem.setBlockEntityData(result, ModBlockEntities.GUIDE.get(),
                    saveWithoutMetadata(level.registryAccess()));
        }
        return result;
    }

    private boolean isBuilder() {
        return getBlockState().getBlock() instanceof GuideBlock guide && guide.isBuilder();
    }

    private void changed() {
        cachedShape = null;
        cachedOrientation = null;
        sync();
    }

    private void sync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("PosX")) {
            posX = Math.clamp(tag.getInt("PosX"), 0, MAX_EXTENT);
            posY = Math.clamp(tag.getInt("PosY"), 0, MAX_EXTENT);
            posZ = Math.clamp(tag.getInt("PosZ"), 0, MAX_EXTENT);
            negX = Math.clamp(tag.getInt("NegX"), 0, MAX_EXTENT);
            negY = Math.clamp(tag.getInt("NegY"), 0, MAX_EXTENT);
            negZ = Math.clamp(tag.getInt("NegZ"), 0, MAX_EXTENT);
        } else if (tag.contains("SizeX")) {
            negX = posX = Math.clamp((tag.getInt("SizeX") - 1) / 2, 0, MAX_EXTENT);
            negY = posY = Math.clamp((tag.getInt("SizeY") - 1) / 2, 0, MAX_EXTENT);
            negZ = posZ = Math.clamp((tag.getInt("SizeZ") - 1) / 2, 0, MAX_EXTENT);
        }
        shape = GuideShape.values()[Math.floorMod(tag.getInt("Shape"), GuideShape.values().length)];
        color = tag.contains("Color") ? tag.getInt("Color") & 0xFFFFFF : 0xFFFFFF;
        active = tag.getBoolean("Active");
        cachedShape = null;
        cachedOrientation = null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("PosX", posX);
        tag.putInt("PosY", posY);
        tag.putInt("PosZ", posZ);
        tag.putInt("NegX", negX);
        tag.putInt("NegY", negY);
        tag.putInt("NegZ", negZ);
        tag.putInt("Shape", shape.ordinal());
        tag.putInt("Color", color);
        tag.putBoolean("Active", active);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
