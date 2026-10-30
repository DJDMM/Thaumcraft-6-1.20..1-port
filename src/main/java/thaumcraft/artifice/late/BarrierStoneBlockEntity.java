package thaumcraft.artifice.late;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import thaumcraft.catalog.blocks.CatalogBlocks;
public final class BarrierStoneBlockEntity extends BlockEntity {
    private int count;
    public BarrierStoneBlockEntity(BlockPos p,BlockState s){super(LateDevicesModule.BARRIER_STONE.get(),p,s);}
    public static void tick(Level l,BlockPos p,BlockState s,BarrierStoneBlockEntity t){
        if(l.isClientSide)return;if(t.count==0)t.count=l.random.nextInt(100);
        if(t.count%5==0&&!l.hasNeighborSignal(p))for(var e:l.getEntitiesOfClass(LivingEntity.class,new AABB(p.getX(),p.getY(),p.getZ(),p.getX()+1,p.getY()+3,p.getZ()+1).inflate(.1))){
            if(!e.onGround()&&!(e instanceof Player)){float yaw=(e.getYRot()+180)*((float)Math.PI/180);e.push(-net.minecraft.util.Mth.sin(yaw)*.2F,-.1,net.minecraft.util.Mth.cos(yaw)*.2F);}
        }
        if(++t.count%100==0)for(int y=1;y<=2;y++){var q=p.above(y);if(l.hasChunkAt(q)&&l.isEmptyBlock(q))l.setBlock(q,CatalogBlocks.block("barrier").defaultBlockState(),3);}
    }
}
