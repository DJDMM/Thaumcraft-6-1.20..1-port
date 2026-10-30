package thaumcraft.artifice.late;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.network.NetworkHooks;
public final class SpaBlock extends BaseEntityBlock {
    public SpaBlock(){super(Properties.copy(Blocks.STONE).strength(3));}
    @Override public RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new SpaBlockEntity(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){return createTickerHelper(type,LateDevicesModule.SPA.get(),SpaBlockEntity::tick);}
    @Override public InteractionResult use(BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
        if(player instanceof ServerPlayer server&&!player.isShiftKeyDown()&&l.getBlockEntity(p) instanceof SpaBlockEntity tile){
            var held=FluidUtil.getFluidContained(player.getItemInHand(hand));if(held.isPresent()){
                if(tile.tank.getFluidAmount()<5000&&(tile.tank.isEmpty()||tile.tank.getFluid().isFluidEqual(held.get()))&&FluidUtil.interactWithFluidHandler(player,hand,tile.tank)){tile.changed();l.playSound(null,p,SoundEvents.BOTTLE_FILL,SoundSource.BLOCKS,.33F,1+(l.random.nextFloat()-l.random.nextFloat())*.3F);}
            }else NetworkHooks.openScreen(server,tile,p);
        }return InteractionResult.sidedSuccess(l.isClientSide);
    }
    @Override public void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moved){if(!s.is(next.getBlock())&&!l.isClientSide&&l.getBlockEntity(p) instanceof net.minecraft.world.Container c)Containers.dropContents(l,p,c);super.onRemove(s,l,p,next,moved);}
}
