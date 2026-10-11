package thaumcraft.artifice.redstone;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

/** Six-face note receiver. The toggle subtype only changes the response to a matching note. */
public final class ArcaneEarBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty ENABLED = BooleanProperty.create("enabled");
    private static final NoteBlockInstrument[] INSTRUMENTS = { NoteBlockInstrument.HARP, NoteBlockInstrument.BASEDRUM,
            NoteBlockInstrument.SNARE, NoteBlockInstrument.HAT, NoteBlockInstrument.BASS, NoteBlockInstrument.FLUTE,
            NoteBlockInstrument.BELL, NoteBlockInstrument.GUITAR, NoteBlockInstrument.CHIME, NoteBlockInstrument.XYLOPHONE };
    private final boolean toggle;
    public ArcaneEarBlock(boolean toggle) {
        super(Properties.copy(Blocks.OAK_PLANKS).strength(1).noOcclusion()
                .isRedstoneConductor((s, l, p) -> false).isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false));
        this.toggle = toggle;
        // BETA26 discards the withProperty(UP) return value and retains first-direction DOWN.
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.DOWN).setValue(ENABLED, false));
    }
    public boolean isToggle() { return toggle; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, ENABLED); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new ArcaneEarBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ArtificeRedstoneModule.ARCANE_EAR.get(), ArcaneEarBlockEntity::serverTick);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getClickedFace());
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction face = state.getValue(FACING); BlockPos support = pos.relative(face.getOpposite());
        return ArtificeRedstoneModule.loaded(level, support) && level.getBlockState(support).isFaceSturdy(level, support, face);
    }
    @Override public VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) { return Shapes.empty(); }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case DOWN -> box(2, 10, 2, 14, 16, 14);
            case UP -> box(2, 0, 2, 14, 6, 14);
            case NORTH -> box(2, 2, 10, 14, 14, 16);
            case SOUTH -> box(2, 2, 0, 14, 14, 6);
            case WEST -> box(10, 2, 2, 16, 14, 14);
            case EAST -> box(0, 2, 2, 6, 14, 14);
        };
    }
    @Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState previous, boolean moving) {
        super.onPlace(state, level, pos, previous, moving);
        if (!previous.is(this) && !level.isClientSide && level.getBlockEntity(pos) instanceof ArcaneEarBlockEntity ear) ear.updateTone();
    }
    @Override public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        if (level.isClientSide || !level.getBlockState(pos).is(this)) return;
        if (level.getBlockEntity(pos) instanceof ArcaneEarBlockEntity ear) ear.updateTone();
        if (!canSurvive(state, level, pos)) level.destroyBlock(pos, true);
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        super.onRemove(state, level, pos, replacement, moving);
        if (!replacement.is(this) && !level.isClientSide) {
            BlockPos support = pos.relative(state.getValue(FACING).getOpposite());
            if (ArtificeRedstoneModule.loaded(level, support)) level.updateNeighborsAt(support, this);
        }
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ArcaneEarBlockEntity ear) { ear.changePitch(); ear.triggerNote(true); }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public boolean isSignalSource(BlockState state) { return true; }
    @Override public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) { return state.getValue(ENABLED) ? 15 : 0; }
    @Override public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) { return getSignal(state, level, pos, side); }
    public void setSignal(Level level, BlockPos pos, boolean enabled) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(this)) return;
        level.setBlock(pos, state.setValue(ENABLED, enabled), Block.UPDATE_ALL);
        level.updateNeighborsAt(pos, this);
        BlockPos support = pos.relative(state.getValue(FACING).getOpposite());
        if (ArtificeRedstoneModule.loaded(level, support)) level.updateNeighborsAt(support, this);
        BlockState current = level.getBlockState(pos); level.sendBlockUpdated(pos, current, current, Block.UPDATE_ALL);
    }
    @Override public boolean triggerEvent(BlockState state, Level level, BlockPos pos, int instrument, int note) {
        super.triggerEvent(state, level, pos, instrument, note);
        int type = instrument < 0 || instrument >= INSTRUMENTS.length ? 0 : instrument;
        level.playSound(null, pos, INSTRUMENTS[type].getSoundEvent().value(), SoundSource.BLOCKS, 3, NoteBlock.getPitchFromNote(note));
        level.addParticle(ParticleTypes.NOTE, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, note / 24.0, 0, 0);
        return true;
    }
    @Override public BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    @Override public BlockState mirror(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }
    @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) { return List.of(new ItemStack(this)); }
}
