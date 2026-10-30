package thaumcraft.artifice.late;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;
import thaumcraft.fx.FxDispatcher;

/** Released travel effects and the barrier stone's invisible two-block fence. */
public final class PavingStoneBlock extends BaseEntityBlock {
    private final boolean barrier;
    public PavingStoneBlock(boolean barrier){super(Properties.copy(Blocks.STONE).strength(2.5F).noOcclusion());this.barrier=barrier;}
    public boolean barrier(){return barrier;}
    @Override public RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Block.box(0,0,0,16,15,16);}
    @Override public void stepOn(Level l,BlockPos p,BlockState s,Entity e){
        if(!l.isClientSide&&!barrier&&e instanceof LivingEntity living){living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,40,1,false,false));living.addEffect(new MobEffectInstance(MobEffects.JUMP,40,0,false,false));}
        super.stepOn(l,p,s,e);
    }
    @Override public @Nullable BlockEntity newBlockEntity(BlockPos p,BlockState s){return barrier?new BarrierStoneBlockEntity(p,s):null;}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){return barrier?createTickerHelper(type,LateDevicesModule.BARRIER_STONE.get(),BarrierStoneBlockEntity::tick):null;}
    @Override public void animateTick(BlockState s,Level l,BlockPos p,RandomSource r){
        if(!barrier)return;
        boolean power=l.hasNeighborSignal(p),pass=BarrierBlock.passable(l,p.above())||BarrierBlock.passable(l,p.above(2));
        if(power||pass){for(int i=0;i<(power?4:6);i++)FxDispatcher.emit(l,"blockRunes",p.getX()+.5,p.getY()+1.2F,p.getZ()+.5,0,-.02F,0,power?rgb(.2F+r.nextFloat()*.4F,r.nextFloat()*.3F,.8F+r.nextFloat()*.2F):rgb(.9F+r.nextFloat()*.1F,r.nextFloat()*.3F,r.nextFloat()*.3F),1,power?20:24);}
        else for(var e:l.getEntitiesOfClass(LivingEntity.class,new net.minecraft.world.phys.AABB(p).inflate(1),e->!(e instanceof Player))){FxDispatcher.emit(l,"blockRunes",p.getX()+.5,p.getY()+1.1F+r.nextFloat()*Math.max(.8F,e.getEyeHeight()),p.getZ()+.5,0,0,0,rgb(.6F+r.nextFloat()*.4F,0,.3F+r.nextFloat()*.7F),1,20);break;}
    }
    private static int rgb(float r,float g,float b){return ((int)(r*255)<<16)|((int)(g*255)<<8)|(int)(b*255);}
}
