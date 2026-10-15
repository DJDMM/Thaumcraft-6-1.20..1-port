package thaumcraft.artifice.dioptra;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;

/** Enabled selects Vis; disabled selects Flux. Redstone does not select the mode. */
public final class DioptraBlock extends BaseEntityBlock {
    public static final BooleanProperty ENABLED=BooleanProperty.create("enabled");
    public DioptraBlock(){
        super(Properties.of().mapColor(net.minecraft.world.level.material.MapColor.STONE).strength(2,12).sound(SoundType.STONE).noOcclusion()
                .isRedstoneConductor((s,l,p)->false).isSuffocating((s,l,p)->false).isViewBlocking((s,l,p)->false));
        // Legacy PropertyBool enumerates TRUE before FALSE; BlockTCDevice's discarded assignment still starts true.
        registerDefaultState(stateDefinition.any().setValue(ENABLED,true));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(ENABLED);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){return defaultBlockState().setValue(ENABLED,true);}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new DioptraBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return createTickerHelper(type,DioptraModule.BLOCK_ENTITY.get(),DioptraBlockEntity::tick);}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(player.isSpectator())return InteractionResult.PASS;
        if(level.isClientSide)return InteractionResult.SUCCESS;
        if(level.captureBlockSnapshots||level.restoringBlockSnapshots||!DioptraModule.loaded(level,pos)||!level.mayInteract(player,pos)||level.getBlockState(pos)!=state)return InteractionResult.PASS;
        return level.setBlock(pos,state.cycle(ENABLED),Block.UPDATE_ALL)?InteractionResult.CONSUME:InteractionResult.PASS;
    }
    @Override public boolean hasAnalogOutputSignal(BlockState state){return true;}
    @Override public int getAnalogOutputSignal(BlockState state,Level level,BlockPos pos){return DioptraModule.loaded(level,pos)&&level.getBlockEntity(pos) instanceof DioptraBlockEntity tile?tile.comparatorSignal():0;}
    @Override public List<ItemStack> getDrops(BlockState state,LootParams.Builder builder){return List.of(new ItemStack(this));}
}
