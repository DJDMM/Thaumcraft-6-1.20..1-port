package thaumcraft.artifice.lamp;

import java.util.List;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.*;
import thaumcraft.catalog.blocks.CatalogBlocks;

/** The two fuelled variants of released BlockLamp; support-facing and light15 when enabled. */
public final class EssentiaLampBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.FACING;
    public static final BooleanProperty ENABLED=BooleanProperty.create("enabled");
    private static final VoxelShape BODY=Block.box(4,2,4,12,14,12);
    private final boolean fertility;
    public EssentiaLampBlock(boolean fertility){
        super(Properties.of().mapColor(net.minecraft.world.level.material.MapColor.METAL).strength(1).sound(SoundType.METAL).noOcclusion()
                .isRedstoneConductor((s,l,p)->false).isSuffocating((s,l,p)->false).isViewBlocking((s,l,p)->false).lightLevel(s->s.getValue(ENABLED)?15:0));
        this.fertility=fertility;registerDefaultState(stateDefinition.any().setValue(FACING,Direction.DOWN).setValue(ENABLED,false));
    }
    public boolean fertility(){return fertility;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING,ENABLED);}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return fertility?new FertilityLampBlockEntity(pos,state):new GrowthLampBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){if(level.isClientSide)return null;return fertility?createTickerHelper(type,LampModule.LAMP_FERTILITY.get(),FertilityLampBlockEntity::tick):createTickerHelper(type,LampModule.LAMP_GROWTH.get(),GrowthLampBlockEntity::tick);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){var state=defaultBlockState().setValue(FACING,context.getClickedFace().getOpposite());return state.canSurvive(context.getLevel(),context.getClickedPos())?state:null;}
    @Override public boolean canSurvive(BlockState state,LevelReader level,BlockPos pos){BlockPos support=pos.relative(state.getValue(FACING));return !level.hasChunkAt(support)||(!level.isEmptyBlock(support)&&!level.getBlockState(support).is(CatalogBlocks.block("effect_glimmer")));}
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return BODY;}
    @Override public VoxelShape getBlockSupportShape(BlockState state,BlockGetter level,BlockPos pos){return Shapes.empty();}
    @Override public BlockState rotate(BlockState state,Rotation rotation){return state.setValue(FACING,rotation.rotate(state.getValue(FACING)));}
    @Override public BlockState mirror(BlockState state,Mirror mirror){return rotate(state,mirror.getRotation(state.getValue(FACING)));}
    static Boolean loadedPower(Level level,BlockPos pos){
        for(int x=-3;x<=3;x++)for(int y=-3;y<=3;y++)for(int z=-3;z<=3;z++)if(Math.abs(x)+Math.abs(y)+Math.abs(z)<=3){BlockPos p=pos.offset(x,y,z);if(!level.isOutsideBuildHeight(p)&&!EssentiaLampBlockEntity.loaded(level,p))return null;}
        return level.hasNeighborSignal(pos);
    }
    void refreshPower(Level level,BlockPos pos,BlockState state,boolean neighbor){
        if(level.isClientSide||level.captureBlockSnapshots||level.restoringBlockSnapshots||LampBlockItem.uncommitted(level,pos)||!state.is(this))return;
        BlockEntity owner=level.getBlockEntity(pos);if(neighbor&&owner instanceof EssentiaLampBlockEntity tile&&tile.charges()<=0)return;
        Boolean powered=loadedPower(level,pos);if(powered==null||level.getBlockEntity(pos)!=owner||!level.getBlockState(pos).equals(state))return;
        if(state.getValue(ENABLED)==powered)level.setBlock(pos,state.setValue(ENABLED,!powered),3);
    }
    @Override public void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moving){super.onPlace(state,level,pos,old,moving);refreshPower(level,pos,state,false);}
    @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,Block block,BlockPos neighbor,boolean moving){
        if(level.isClientSide||level.captureBlockSnapshots||level.restoringBlockSnapshots||LampBlockItem.uncommitted(level,pos))return;
        BlockPos support=pos.relative(state.getValue(FACING));if(!EssentiaLampBlockEntity.loaded(level,support))return;
        if(!state.canSurvive(level,pos)){
            BlockEntity owner=level.getBlockEntity(pos);if(level.removeBlock(pos,false)&&level instanceof ServerLevel server){
                // Preserve a paid support drop even when a native entity-join callback vetoes it.
                LampDropData.get(server).addVanilla(server,pos,new ItemStack(this));
            }return;
        }refreshPower(level,pos,state,true);
    }
    @Override public List<ItemStack> getDrops(BlockState state,LootParams.Builder builder){return List.of(new ItemStack(this));}
}
