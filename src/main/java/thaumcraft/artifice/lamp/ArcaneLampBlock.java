package thaumcraft.artifice.lamp;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.*;
import java.util.List;

/** Original support-facing lamp; powered lamps remove glimmers and cease emission. */
public final class ArcaneLampBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.FACING;
    public static final BooleanProperty ENABLED=BooleanProperty.create("enabled");
    private static final VoxelShape BODY=Block.box(4,2,4,12,14,12);
    public ArcaneLampBlock(){super(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(1).noOcclusion().lightLevel(s->s.getValue(ENABLED)?15:0));registerDefaultState(stateDefinition.any().setValue(FACING,Direction.DOWN).setValue(ENABLED,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING,ENABLED);}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new ArcaneLampBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide?null:createTickerHelper(type,LampModule.ARCANE_LAMP.get(),ArcaneLampBlockEntity::tick);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){var state=defaultBlockState().setValue(FACING,context.getClickedFace().getOpposite());return state.canSurvive(context.getLevel(),context.getClickedPos())?state:null;}
    @Override public boolean canSurvive(BlockState state,LevelReader level,BlockPos pos){BlockPos support=pos.relative(state.getValue(FACING));return !level.hasChunkAt(support)||(!level.isEmptyBlock(support)&&!level.getBlockState(support).is(thaumcraft.catalog.blocks.CatalogBlocks.block("effect_glimmer")));}
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return BODY;}
    @Override public VoxelShape getBlockSupportShape(BlockState state,BlockGetter level,BlockPos pos){return Shapes.empty();}
    @Override public BlockState rotate(BlockState state,Rotation rotation){return state.setValue(FACING,rotation.rotate(state.getValue(FACING)));}
    @Override public BlockState mirror(BlockState state,Mirror mirror){return rotate(state,mirror.getRotation(state.getValue(FACING)));}
    private void signal(Level level,BlockPos pos,BlockState state){
        if(level.isClientSide)return;
        boolean enabled=!level.hasNeighborSignal(pos);
        if(state.getValue(ENABLED)!=enabled){
            if(!enabled&&level.getBlockEntity(pos) instanceof ArcaneLampBlockEntity lamp)lamp.removeLights();
            level.setBlock(pos,state.setValue(ENABLED,enabled),Block.UPDATE_ALL);
        }
    }
    @Override public void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moving){super.onPlace(state,level,pos,old,moving);signal(level,pos,state);}
    @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,Block block,BlockPos neighbor,boolean moving){if(!level.isClientSide&&!state.canSurvive(level,pos)){level.destroyBlock(pos,true);return;}signal(level,pos,state);}
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){if(!state.is(next.getBlock())&&level.getBlockEntity(pos) instanceof ArcaneLampBlockEntity lamp)lamp.removeLights();super.onRemove(state,level,pos,next,moving);}
    @Override public List<ItemStack> getDrops(BlockState state,LootParams.Builder builder){return List.of(new ItemStack(this));}
}
