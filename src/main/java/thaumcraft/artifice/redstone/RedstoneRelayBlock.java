package thaumcraft.artifice.redstone;

import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.*;
import net.minecraft.world.ticks.TickPriority;
import net.minecraftforge.event.ForgeEventFactory;

/** FACING names the input side, exactly as the BETA26 repeater-derived relay. */
public final class RedstoneRelayBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ENABLED = BooleanProperty.create("enabled");
    private static final VoxelShape BASE = box(0, 0, 0, 16, 2, 16);
    public RedstoneRelayBlock() {
        super(Properties.copy(Blocks.REPEATER).strength(0, 0).sound(SoundType.WOOD).noOcclusion()
                .isRedstoneConductor((s, l, p) -> false).isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ENABLED, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, ENABLED); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new RedstoneRelayBlockEntity(pos, state); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction direction = context.getPlayer() != null && context.getPlayer().isShiftKeyDown()
                ? context.getHorizontalDirection() : context.getHorizontalDirection().getOpposite();
        BlockState state = defaultBlockState().setValue(FACING, direction);
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return ArtificeRedstoneModule.loaded(level, pos.below()) && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }
    @Override public VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) { return Shapes.empty(); }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return BASE; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.or(BASE, Shapes.create(knobBounds(state.getValue(FACING), 0)), Shapes.create(knobBounds(state.getValue(FACING), 1)));
    }
    /** Source Cuboid0 is the output dial; Cuboid1 is the input dial. Coordinates are block-local. */
    public static AABB knobBounds(Direction direction, int knob) {
        AABB source = knob == 0 ? new AABB(.125, .0625, .125, .375, .25, .375)
                : new AABB(.375, .0625, .625, .625, .25, .875);
        int quarters = switch (direction) { case WEST -> 1; case NORTH -> 2; case EAST -> 3; default -> 0; };
        for (int n = 0; n < quarters; n++) source = new AABB(1 - source.maxZ, source.minY, source.minX, 1 - source.minZ, source.maxY, source.maxX);
        return source;
    }
    public static int selectedKnob(BlockState state, Vec3 localHit) {
        for (int knob = 0; knob < 2; knob++) if (knobBounds(state.getValue(FACING), knob).inflate(1E-6).contains(localHit)) return knob;
        return -1;
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!player.mayBuild()) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof RedstoneRelayBlockEntity relay)) return InteractionResult.PASS;
        int knob = selectedKnob(state, hit.getLocation().subtract(Vec3.atLowerCornerOf(pos)));
        if (!level.isClientSide && knob >= 0) {
            if (knob == 0) relay.increaseOut(); else relay.increaseIn();
            level.playSound(null, pos, ArtificeRedstoneModule.KEY.get(), SoundSource.BLOCKS, .5F, 1);
            updateState(level, pos, state); notifyOutput(level, pos, state);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public boolean isSignalSource(BlockState state) { return true; }
    @Override public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return state.getValue(ENABLED) && side == state.getValue(FACING) && level.getBlockEntity(pos) instanceof RedstoneRelayBlockEntity relay ? relay.getOut() : 0;
    }
    @Override public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) { return getSignal(state, level, pos, side); }
    public int calculateInputStrength(Level level, BlockPos pos, BlockState state) {
        Direction input = state.getValue(FACING); BlockPos neighbor = pos.relative(input);
        if (!ArtificeRedstoneModule.loaded(level, neighbor)) return 0;
        int signal = level.getSignal(neighbor, input);
        if (signal >= 15) return signal;
        BlockState other = level.getBlockState(neighbor);
        return Math.max(signal, other.is(Blocks.REDSTONE_WIRE) ? other.getValue(RedStoneWireBlock.POWER) : 0);
    }
    public boolean shouldBePowered(Level level, BlockPos pos, BlockState state) {
        int threshold = level.getBlockEntity(pos) instanceof RedstoneRelayBlockEntity relay ? relay.getIn() : 1;
        return calculateInputStrength(level, pos, state) >= threshold;
    }
    public boolean isFacingTowardsRepeater(Level level, BlockPos pos, BlockState state) {
        Direction output = state.getValue(FACING).getOpposite(); BlockPos neighbor = pos.relative(output);
        if (!ArtificeRedstoneModule.loaded(level, neighbor)) return false;
        BlockState other = level.getBlockState(neighbor);
        return (other.is(Blocks.REPEATER) || other.is(Blocks.COMPARATOR)) && other.getValue(FACING) != output;
    }
    public void updateState(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide || !level.getBlockState(pos).is(this)) return;
        boolean powered = state.getValue(ENABLED);
        if (powered != shouldBePowered(level, pos, state) && !level.getBlockTicks().hasScheduledTick(pos, this)) {
            TickPriority priority = isFacingTowardsRepeater(level, pos, state) ? TickPriority.EXTREMELY_HIGH
                    : powered ? TickPriority.VERY_HIGH : TickPriority.HIGH;
            level.scheduleTick(pos, this, 2, priority);
        }
    }
    @Override public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!ArtificeRedstoneModule.loaded(level, pos) || !level.getBlockState(pos).is(this)) return;
        boolean wanted = shouldBePowered(level, pos, state);
        if (state.getValue(ENABLED) && !wanted) {
            level.setBlock(pos, state.setValue(ENABLED, false), Block.UPDATE_CLIENTS); notifyOutput(level, pos, state);
        } else if (!state.getValue(ENABLED)) {
            level.setBlock(pos, state.setValue(ENABLED, true), Block.UPDATE_CLIENTS); notifyOutput(level, pos, state);
            // BETA26 keeps a vanished short input as a two-tick output pulse.
            if (!wanted) level.scheduleTick(pos, this, 2, TickPriority.HIGH);
        }
    }
    @Override public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        if (level.isClientSide || !level.getBlockState(pos).is(this)) return;
        if (canSurvive(state, level, pos)) updateState(level, pos, state);
        else { level.destroyBlock(pos, true); for (Direction side : Direction.values()) if (ArtificeRedstoneModule.loaded(level, pos.relative(side))) level.updateNeighborsAt(pos.relative(side), this); }
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide && shouldBePowered(level, pos, state)) level.scheduleTick(pos, this, 1);
    }
    @Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState previous, boolean moving) {
        super.onPlace(state, level, pos, previous, moving);
        if (!previous.is(this)) notifyOutput(level, pos, state);
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        super.onRemove(state, level, pos, replacement, moving);
        if (!replacement.is(this)) notifyOutput(level, pos, state);
    }
    public void notifyOutput(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;
        Direction input = state.getValue(FACING); Direction output = input.getOpposite(); BlockPos neighbor = pos.relative(output);
        if (!ArtificeRedstoneModule.loaded(level, neighbor) || ForgeEventFactory.onNeighborNotify(level, pos, level.getBlockState(pos), EnumSet.of(output), false).isCanceled()) return;
        level.neighborChanged(neighbor, this, pos); level.updateNeighborsAtExceptFromFacing(neighbor, this, input);
    }
    @Override public BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    @Override public BlockState mirror(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }
    @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) { return List.of(new ItemStack(this)); }
}
