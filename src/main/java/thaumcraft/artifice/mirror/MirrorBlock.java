package thaumcraft.artifice.mirror;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

/** Six-face original rear one-eighth slab, with one explicit removal/drop owner. */
public final class MirrorBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.FACING;
    private final boolean essentia;
    public MirrorBlock(boolean essentia){
        super(Properties.of().mapColor(net.minecraft.world.level.material.MapColor.METAL).strength(.1F).noOcclusion().sound(SoundType.GLASS)
                .isRedstoneConductor((s,l,p)->false).isSuffocating((s,l,p)->false).isViewBlocking((s,l,p)->false));
        this.essentia=essentia;registerDefaultState(stateDefinition.any().setValue(FACING,Direction.DOWN));
    }
    public boolean isEssentia(){return essentia;}
    @Override public SoundType getSoundType(BlockState state,net.minecraft.world.level.LevelReader level,BlockPos pos,@javax.annotation.Nullable Entity entity){
        var jar=MirrorModule.JAR.get();return new SoundType(.5F,1,jar,jar,jar,jar,jar);
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING);}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.INVISIBLE;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return essentia?new EssentiaMirrorBlockEntity(pos,state):new ItemMirrorBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){
        if(level.isClientSide)return null;
        return essentia?createTickerHelper(type,MirrorModule.ESSENTIA_MIRROR.get(),(l,p,s,t)->t.serverTick()):createTickerHelper(type,MirrorModule.ITEM_MIRROR.get(),(l,p,s,t)->t.serverTick());
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){BlockState state=defaultBlockState().setValue(FACING,context.getClickedFace());return state.canSurvive(context.getLevel(),context.getClickedPos())?state:null;}
    @Override public boolean canSurvive(BlockState state,LevelReader level,BlockPos pos){Direction face=state.getValue(FACING);BlockPos support=pos.relative(face.getOpposite());return level.hasChunkAt(support)&&level.getBlockState(support).isFaceSturdy(level,support,face);}
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){
        return switch(state.getValue(FACING)){case DOWN->box(0,14,0,16,16,16);case UP->box(0,0,0,16,2,16);case NORTH->box(0,0,14,16,16,16);case SOUTH->box(0,0,0,16,16,2);case WEST->box(14,0,0,16,16,16);case EAST->box(0,0,0,2,16,16);};
    }
    @Override public VoxelShape getBlockSupportShape(BlockState state,BlockGetter level,BlockPos pos){return Shapes.empty();}
    @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,Block block,BlockPos from,boolean moving){if(!level.isClientSide&&level.getBlockState(pos).is(this)&&!canSurvive(state,level,pos))level.destroyBlock(pos,true);}
    @Override public void entityInside(BlockState state,Level level,BlockPos pos,Entity entity){if(!level.isClientSide&&!essentia&&entity instanceof ItemEntity item&&level.getBlockEntity(pos) instanceof ItemMirrorBlockEntity mirror)mirror.transport(item);}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){return InteractionResult.sidedSuccess(level.isClientSide);}
    @Override public void playerWillDestroy(Level level,BlockPos pos,BlockState state,Player player){if(!level.isClientSide&&level.getBlockEntity(pos) instanceof AbstractMirrorBlockEntity mirror)mirror.setCreativeRemoval(player.isCreative());super.playerWillDestroy(level,pos,state,player);}
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState replacement,boolean moving){
        if(!replacement.is(this)&&!level.isClientSide&&level.getBlockEntity(pos) instanceof AbstractMirrorBlockEntity mirror){
            // Forge restores the held item after a canceled native placement; do not also refund a new block item.
            if(level.restoringBlockSnapshots){mirror.setCreativeRemoval(true);mirror.cancelPlacement();}
            mirror.removeMirror();
        }
        super.onRemove(state,level,pos,replacement,moving);
    }
    @Override public List<ItemStack> getDrops(BlockState state,LootParams.Builder builder){return List.of();}
    @Override public BlockState rotate(BlockState state,Rotation rotation){return state.setValue(FACING,rotation.rotate(state.getValue(FACING)));}
    @Override public BlockState mirror(BlockState state,Mirror mirror){return state.rotate(mirror.getRotation(state.getValue(FACING)));}
}
