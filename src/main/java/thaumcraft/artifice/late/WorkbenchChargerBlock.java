package thaumcraft.artifice.late;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.network.NetworkHooks;
import thaumcraft.arcane.ArcaneModule;
import thaumcraft.catalog.blocks.CatalogBlocks;
/** Original support/collision/menu semantics; the charger does not store Aura. */
public final class WorkbenchChargerBlock extends Block {
    public WorkbenchChargerBlock(){super(Properties.of().mapColor(net.minecraft.world.level.material.MapColor.WOOD).strength(1.25F,10).sound(SoundType.WOOD).noOcclusion()
        .isRedstoneConductor((s,l,p)->false).isSuffocating((s,l,p)->false).isViewBlocking((s,l,p)->false));}
    @Override public boolean canSurvive(BlockState state,LevelReader l,BlockPos p){var s=l.getBlockState(p.below());return s.is(ArcaneModule.WORKBENCH.get())||s.is(CatalogBlocks.block("focal_manipulator"));}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return canSurvive(defaultBlockState(),c.getLevel(),c.getClickedPos())?defaultBlockState():null;}
    @Override public VoxelShape getBlockSupportShape(BlockState s,BlockGetter l,BlockPos p){return Shapes.empty();}
    @Override public void onPlace(BlockState s,Level l,BlockPos p,BlockState old,boolean moving){super.onPlace(s,l,p,old,moving);changed(l,p);}
    @Override public void neighborChanged(BlockState s,Level l,BlockPos p,Block b,BlockPos from,boolean moving){
        if(!l.isClientSide&&!l.restoringBlockSnapshots&&!l.captureBlockSnapshots&&l.getBlockState(p).is(this)&&!canSurvive(s,l,p)){
            popResource(l,p,new net.minecraft.world.item.ItemStack(this));l.setBlock(p,Blocks.AIR.defaultBlockState(),3);
        }else changed(l,p);
    }
    private static void changed(Level l,BlockPos p){if(l.isClientSide||l.restoringBlockSnapshots||l.captureBlockSnapshots)return;var tile=l.getBlockEntity(p.below());if(tile!=null){tile.setChanged();var s=l.getBlockState(p.below());l.sendBlockUpdated(p.below(),s,s,3);}}
    @Override public InteractionResult use(BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
        if(player instanceof ServerPlayer server&&canSurvive(s,l,p)&&l.getBlockEntity(p.below()) instanceof MenuProvider menu)NetworkHooks.openScreen(server,menu,p.below());
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
}
