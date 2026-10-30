package thaumcraft.alchemy.liquiddeath;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.material.*;
/** Released four-quanta classic automaton, adapted from Forge1.12 BlockFluidClassic (LGPL2.1). */
public final class LiquidDeathBlock extends LiquidBlock {
 private static final Direction[] SIDES={Direction.WEST,Direction.EAST,Direction.NORTH,Direction.SOUTH};
 public LiquidDeathBlock(){super(LiquidDeathModule.SOURCE,Properties.copy(Blocks.WATER).randomTicks().noLootTable());}
 @Override public FluidState getFluidState(BlockState state){int meta=Math.min(3,state.getValue(LEVEL));return meta==0?LiquidDeathModule.SOURCE.get().defaultFluidState():LiquidDeathModule.FLOWING.get().defaultFluidState().setValue(FlowingFluid.LEVEL,(4-meta)*2).setValue(FlowingFluid.FALLING,false);}
 @Override public void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moved){level.scheduleTick(pos,this,7);}
 @Override public BlockState updateShape(BlockState state,Direction direction,BlockState other,LevelAccessor level,BlockPos pos,BlockPos neighbor){level.scheduleTick(pos,this,7);return state;}
 @Override public void entityInside(BlockState state,Level level,BlockPos pos,Entity entity){int meta=Math.min(3,state.getValue(LEVEL));float fraction=(4-meta)/4F;entity.setDeltaMovement(entity.getDeltaMovement().multiply(1-fraction/2,1,1-fraction/2));if(!level.isClientSide&&entity instanceof LivingEntity living)living.hurt(LiquidDeathModule.damage(living),5-meta);}
 @Override public void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){tick(state,level,pos,random);}
 @Override public void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){
  state=level.getBlockState(pos);if(!state.is(this))return;int remaining=4-state.getValue(LEVEL);
  if(remaining<4){int expected;
   if(loaded(level,pos.above())&&level.getBlockState(pos.above()).is(this))expected=3;
   else{int maximum=-100;for(Direction side:SIDES){BlockPos n=pos.relative(side);if(!loaded(level,n))continue;int q=quanta(level,n);if(q>0&&q<4&&loaded(level,n.above())&&level.getBlockState(n.above()).is(this))q=4;if(q>0)maximum=Math.max(q,maximum);}expected=maximum-1;}
   if(expected!=remaining){remaining=expected;if(expected<=0){level.removeBlock(pos,false);return;}level.setBlock(pos,state.setValue(LEVEL,4-expected),3);level.scheduleTick(pos,this,7);state=level.getBlockState(pos);if(!state.is(this))return;}
  }
  BlockPos below=pos.below();if(canDisplace(level,below)){flowInto(level,below,1);return;}
  int meta=4-remaining+1;if(meta>=4)return;
  boolean flowingVertically=loaded(level,below)&&level.getBlockState(below).is(this);
  if(state.getValue(LEVEL)==0||!flowingVertically){
   if(loaded(level,pos.above())&&level.getBlockState(pos.above()).is(this))meta=1;
   int minimum=1000;int[] costs=new int[4];
   for(int i=0;i<4;i++){BlockPos n=pos.relative(SIDES[i]);costs[i]=1000;if(canFlowInto(level,n)&&!source(level,n))costs[i]=canFlowInto(level,n.below())?0:flowCost(level,n,1,i);minimum=Math.min(minimum,costs[i]);}
   for(int i=0;i<4;i++)if(costs[i]==minimum){if(!level.getBlockState(pos).is(this))return;flowInto(level,pos.relative(SIDES[i]),meta);}
  }
 }
 private int flowCost(ServerLevel level,BlockPos pos,int depth,int previous){int cost=1000;for(int i=0;i<4;i++){if(SIDES[i]==SIDES[previous].getOpposite())continue;BlockPos n=pos.relative(SIDES[i]);if(!canFlowInto(level,n)||source(level,n))continue;if(canFlowInto(level,n.below()))return depth;if(depth<2)cost=Math.min(cost,flowCost(level,n,depth+1,i));}return cost;}
 private int quanta(Level level,BlockPos pos){BlockState state=level.getBlockState(pos);return state.is(this)?4-state.getValue(LEVEL):state.isAir()?0:-1;}
 private boolean source(Level level,BlockPos pos){return loaded(level,pos)&&level.getBlockState(pos).is(this)&&level.getBlockState(pos).getValue(LEVEL)==0;}
 private static boolean loaded(Level level,BlockPos pos){return !level.isOutsideBuildHeight(pos)&&level.hasChunkAt(pos);}
 private boolean canDisplace(Level level,BlockPos pos){if(!loaded(level,pos))return false;BlockState state=level.getBlockState(pos);if(state.is(this)||state.hasBlockEntity())return false;if(!state.getFluidState().isEmpty())return state.getFluidState().getFluidType().getDensity()<1000;return state.isAir()||state.canBeReplaced()&&!state.isSolidRender(level,pos);}
 private boolean canFlowInto(Level level,BlockPos pos){return loaded(level,pos)&&(level.getBlockState(pos).is(this)||canDisplace(level,pos));}
 private void flowInto(ServerLevel level,BlockPos pos,int meta){if(!canDisplace(level,pos))return;BlockState old=level.getBlockState(pos);if(!old.isAir()&&old.getFluidState().isEmpty()&&!old.is(Blocks.SNOW))Block.dropResources(old,level,pos);if(level.getBlockState(pos)!=old)return;level.setBlockAndUpdate(pos,defaultBlockState().setValue(LEVEL,meta));}
 @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random){net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->LiquidDeathClient.animate(level,pos,state.getValue(LEVEL),random));}
}
