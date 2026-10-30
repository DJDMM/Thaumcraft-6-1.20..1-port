package thaumcraft.world.taint;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.material.*;
import thaumcraft.infusion.InfusionEffects;
import thaumcraft.world.aura.AuraManager;
/** Eight conserved quanta. Finite redistribution follows Forge1.12 BlockFluidFinite (LGPL2.1). */
public final class FluxGooBlock extends LiquidBlock {
 public FluxGooBlock(){super(TaintEcologyModule.GOO,Properties.copy(Blocks.WATER).randomTicks().sound(TaintModule.SOUND).noLootTable());registerDefaultState(stateDefinition.any().setValue(LEVEL,7));}
 @Override public FluidState getFluidState(BlockState state){int q=state.getValue(LEVEL)+1;return q>=8?TaintEcologyModule.GOO.get().defaultFluidState():TaintEcologyModule.FLOWING_GOO.get().defaultFluidState().setValue(FlowingFluid.LEVEL,q).setValue(FlowingFluid.FALLING,false);}
 // Modern onPlace also runs for LEVEL changes; original block-added did not.
 @Override public void onPlace(BlockState s,Level l,BlockPos p,BlockState old,boolean moving){if(!old.is(this))l.scheduleTick(p,this,30);}
 @Override public void neighborChanged(BlockState s,Level l,BlockPos p,Block neighbor,BlockPos from,boolean moving){l.scheduleTick(p,this,30);}
 @Override public BlockState updateShape(BlockState s,Direction dir,BlockState neighbor,LevelAccessor level,BlockPos p,BlockPos n){return s;}
 // LiquidBlock otherwise delegates this flag to the deliberately inert modern fluid.
 @Override public boolean isRandomlyTicking(BlockState state){return true;}
 @Override public boolean canBeReplaced(BlockState state,net.minecraft.world.item.context.BlockPlaceContext context){return state.getValue(LEVEL)<4;}
 @Override public net.minecraft.world.item.ItemStack pickupBlock(LevelAccessor level,BlockPos pos,BlockState state){if(state.getValue(LEVEL)!=7)return net.minecraft.world.item.ItemStack.EMPTY;level.setBlock(pos,Blocks.AIR.defaultBlockState(),11);return new net.minecraft.world.item.ItemStack(TaintEcologyModule.GOO_BUCKET.get());}
 @Override public void entityInside(BlockState state,Level level,BlockPos pos,Entity entity){
  int meta=state.getValue(LEVEL);
  if(entity instanceof ThaumicSlimeEntity slime){if(!level.isClientSide&&slime.slimeSize()<meta&&level.random.nextBoolean()){slime.setSlimeSize(slime.slimeSize()+1,true);write(level,pos,meta>1?meta:0,2,false);}return;}
  double factor=1-(meta+1)/8F;entity.setDeltaMovement(entity.getDeltaMovement().multiply(factor,1,factor));
  if(entity instanceof LivingEntity living){var effect=new MobEffectInstance(InfusionEffects.VIS_EXHAUST.get(),600,meta/3,true,true);effect.setCurativeItems(new java.util.ArrayList<>());living.addEffect(effect);}
 }
 @Override public void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){
  state=level.getBlockState(pos);if(!state.is(this))return;int meta=state.getValue(LEVEL);
  if(meta>=2&&TaintModule.loaded(level,pos.above())&&level.isEmptyBlock(pos.above())&&random.nextInt(50)==0){
   var mob=TaintModule.activeMob("thaumic_slime",level);
   if(mob instanceof ThaumicSlimeEntity slime){slime.setSlimeSize(meta<6?1:2,true);slime.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);if(level.addFreshEntity(slime)){removeGoo(level,pos);slime.playSound(TaintModule.GORE.get(),1,1);return;}}
  }
  if(random.nextInt(4)==0){if(meta==0){if(random.nextBoolean()){AuraManager.addFlux(level,pos,1);removeGoo(level,pos);}else level.setBlockAndUpdate(pos,TaintModule.block("taint_fibre").defaultBlockState());}else{write(level,pos,meta,2,false);AuraManager.addFlux(level,pos,1);}return;}
  flowFinite(level,pos,random);
 }
 @Override public void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){tick(state,level,pos,random);}
 public void flowFinite(ServerLevel level,BlockPos pos,RandomSource random){
  BlockState initial=level.getBlockState(pos);if(!initial.is(this))return;
  int remaining=initial.getValue(LEVEL)+1,before=remaining;BlockPos below=pos.below();
  if(level.isOutsideBuildHeight(below)){removeGoo(level,pos);return;}
  if(!TaintModule.loaded(level,below))return;
  int down=quantaBelow(level,below,8);
  if(down>=0){int amount=Math.min(8-down,remaining);if(amount>0){write(level,below,down+amount,3,true);if(level.getBlockState(pos)!=initial)return;remaining-=amount;if(remaining==0){removeGoo(level,pos);return;}}}
  else if(displaceable(level,below)){if(!displace(level,below)||level.getBlockState(pos)!=initial)return;level.setBlockAndUpdate(below,defaultBlockState().setValue(LEVEL,remaining-1));if(level.getBlockState(pos)==initial)removeGoo(level,pos);level.scheduleTick(below,this,30);return;}
  else {FluidState other=level.getFluidState(below);if(!other.isEmpty()&&other.getFluidType().getDensity()<8){BlockState displaced=level.getBlockState(below);level.setBlockAndUpdate(below,defaultBlockState().setValue(LEVEL,remaining-1));level.setBlockAndUpdate(pos,displaced);level.scheduleTick(below,this,30);return;}}
  if(remaining==1){if(before!=remaining)write(level,pos,remaining,2,false);return;}
  int limit=remaining-1,total=remaining,count=1;
  for(Direction direction:Direction.Plane.HORIZONTAL){BlockPos other=pos.relative(direction);if(!TaintModule.loaded(level,other))continue;if(displaceable(level,other))displace(level,other);int q=quantaBelow(level,other,limit);if(q>=0){count++;total+=q;}}
  if(count==1){if(before!=remaining)write(level,pos,remaining,2,false);return;}
  int each=total/count,remainder=total%count;
  for(Direction direction:Direction.Plane.HORIZONTAL){if(level.getBlockState(pos)!=initial)return;BlockPos other=pos.relative(direction);if(!TaintModule.loaded(level,other))continue;int q=quantaBelow(level,other,limit);if(q<0)continue;int next=each;if(remainder==count||remainder>1&&random.nextInt(count-remainder)!=0){next++;remainder--;}if(next!=q)write(level,other,next,2,true);count--;}
  if(level.getBlockState(pos)==initial)write(level,pos,each+(remainder>0?1:0),2,false);
 }
 private boolean displaceable(Level level,BlockPos pos){BlockState s=level.getBlockState(pos);return !s.is(this)&&s.getFluidState().isEmpty()&&(s.isAir()||s.canBeReplaced()||s.is(TaintModule.block("taint_fibre")))&&!s.hasBlockEntity();}
 private boolean displace(Level level,BlockPos pos){BlockState old=level.getBlockState(pos);if(!old.isAir()&&!old.is(Blocks.SNOW)&&level instanceof ServerLevel server)Block.dropResources(old,server,pos);if(level.getBlockState(pos)!=old)return false;level.removeBlock(pos,false);return true;}
 private int quantaBelow(Level level,BlockPos pos,int limit){BlockState s=level.getBlockState(pos);if(s.isAir())return 0;return s.is(this)&&s.getValue(LEVEL)+1<limit?s.getValue(LEVEL)+1:-1;}
 // Level.removeBlock preserves a fluid's legacy block in1.20, which would recreate Goo.
 private void removeGoo(Level level,BlockPos pos){level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());}
 private void write(Level level,BlockPos pos,int amount,int flags,boolean schedule){if(amount<=0)removeGoo(level,pos);else{level.setBlock(pos,defaultBlockState().setValue(LEVEL,Math.min(8,amount)-1),flags);if(schedule)level.scheduleTick(pos,this,30);}}
 @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random){net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->thaumcraft.world.taint.client.TaintEcologyClient.goo(level,pos,state.getValue(LEVEL)));}
}
