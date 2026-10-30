package thaumcraft.artifice.late;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import thaumcraft.catalog.blocks.CatalogBlocks;
/** Original fence excludes living nonplayers; a player carried by any passenger chain is exempt. */
public final class BarrierBlock extends Block {
    public BarrierBlock(){super(Properties.of().replaceable().noOcclusion().noLootTable().isSuffocating((s,l,p)->false).isViewBlocking((s,l,p)->false).isRedstoneConductor((s,l,p)->false));}
    @Override public RenderShape getRenderShape(BlockState s){return RenderShape.INVISIBLE;}
    @Override public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Shapes.empty();}
    @Override public VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){
        if(!(c instanceof EntityCollisionContext context)||!(context.getEntity() instanceof LivingEntity e)||e instanceof Player||playerPassenger(e))return Shapes.empty();
        int down=l.getBlockState(p.below()).is(CatalogBlocks.block("paving_stone_barrier"))?1:2;
        return l instanceof Level level&&level.hasNeighborSignal(p.below(down))?Shapes.empty():Shapes.block();
    }
    private static boolean playerPassenger(Entity e){for(var child:e.getPassengers())if(child instanceof Player||playerPassenger(child))return true;return false;}
    public static boolean passable(BlockGetter l,BlockPos p){
        if(!l.getBlockState(p).is(CatalogBlocks.block("barrier")))return false;
        for(int n=1;n<=2;n++)if(l.getBlockEntity(p.below(n)) instanceof BarrierStoneBlockEntity)return l instanceof Level level&&level.hasNeighborSignal(p.below(n));
        return true;
    }
    @Override public boolean isPathfindable(BlockState s,BlockGetter l,BlockPos p,PathComputationType type){return passable(l,p);}
    @Override public void neighborChanged(BlockState s,Level l,BlockPos p,Block changed,BlockPos from,boolean moving){if(!l.isClientSide&&!l.getBlockState(p.below()).is(CatalogBlocks.block("paving_stone_barrier"))&&!l.getBlockState(p.below()).is(this))l.removeBlock(p,false);}
    @Override public ItemStack getCloneItemStack(BlockState s,net.minecraft.world.phys.HitResult hit,BlockGetter l,BlockPos p,Player player){return ItemStack.EMPTY;}
}
