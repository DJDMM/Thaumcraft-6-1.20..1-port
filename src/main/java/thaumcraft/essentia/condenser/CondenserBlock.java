package thaumcraft.essentia.condenser;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.*;

public final class CondenserBlock extends BaseEntityBlock {
    public static final BooleanProperty ENABLED=BooleanProperty.create("enabled");
    // BlockTCTile.canHarvestBlock is unconditionally true; copying IRON_BLOCK would add an invented tool gate.
    public CondenserBlock(){super(Properties.of().mapColor(net.minecraft.world.level.material.MapColor.METAL).strength(2,12).noOcclusion().sound(SoundType.METAL)
            .isValidSpawn((s,l,p,t)->false).isRedstoneConductor((s,l,p)->false).isSuffocating((s,l,p)->false).isViewBlocking((s,l,p)->false));registerDefaultState(stateDefinition.any().setValue(ENABLED,true));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(ENABLED);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){return defaultBlockState().setValue(ENABLED,!Boolean.TRUE.equals(loadedPower(context.getLevel(),context.getClickedPos())));}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public VoxelShape getBlockSupportShape(BlockState state,BlockGetter level,BlockPos pos){return Shapes.empty();}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new CondenserBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return createTickerHelper(type,CondenserModule.CONDENSER.get(),CondenserBlockEntity::tick);}
    @Override public void onPlace(BlockState state,Level level,BlockPos pos,BlockState previous,boolean moving){super.onPlace(state,level,pos,previous,moving);updatePower(state,level,pos);}
    @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,Block neighbor,BlockPos from,boolean moving){if(CondenserBlockItem.uncommitted(level,pos))return;updatePower(state,level,pos);if(level.getBlockEntity(pos) instanceof CondenserBlockEntity tile)tile.invalidateLattice();}
    static Boolean loadedPower(Level level,BlockPos pos){
        // A conductor queries direct signal on its neighbors; a native wire there can inspect one more ring.
        // Preflight their bounded Manhattan3 read region before entering unchanged vanilla signal semantics.
        for(int x=-3;x<=3;x++)for(int y=-3;y<=3;y++)for(int z=-3;z<=3;z++)if(Math.abs(x)+Math.abs(y)+Math.abs(z)<=3){BlockPos p=pos.offset(x,y,z);if(!level.isOutsideBuildHeight(p)&&!CondenserBlockEntity.loaded(level,p))return null;}
        return level.hasNeighborSignal(pos);
    }
    void updatePower(BlockState state,Level level,BlockPos pos){if(!level.isClientSide&&!level.captureBlockSnapshots&&!level.restoringBlockSnapshots&&!CondenserBlockItem.uncommitted(level,pos)&&state.is(this)){BlockEntity owner=level.getBlockEntity(pos);Boolean powered=loadedPower(level,pos);if(powered==null||!level.getBlockState(pos).equals(state)||level.getBlockEntity(pos)!=owner)return;boolean enabled=!powered;if(state.getValue(ENABLED)!=enabled)level.setBlock(pos,state.setValue(ENABLED,enabled),3);}}
    @Override public List<ItemStack> getDrops(BlockState state,LootParams.Builder params){return List.of(new ItemStack(this));}
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){if(state.getBlock()!=next.getBlock()&&!level.restoringBlockSnapshots&&level instanceof ServerLevel server&&level.getBlockEntity(pos) instanceof CondenserBlockEntity tile)tile.spillOnRemoval(server);super.onRemove(state,level,pos,next,moving);}
}
