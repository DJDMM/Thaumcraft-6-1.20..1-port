package thaumcraft.world.taint;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;
import thaumcraft.world.taint.champion.Champions;
import thaumcraft.infusion.InfusionEffects;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class TaintEcologyGameTests {
 private TaintEcologyGameTests(){}
 private static final class Arena implements AutoCloseable {
  final GameTestHelper h;final BlockPos p;final Map<BlockPos,BlockState> saved=new LinkedHashMap<>();final List<Entity> entities=new ArrayList<>();
  Arena(GameTestHelper h){this.h=h;var relative=h.absolutePos(new BlockPos(3,0,3));p=new BlockPos(relative.getX(),250,relative.getZ());for(int x=(p.getX()-3)>>4;x<=(p.getX()+3)>>4;x++)for(int z=(p.getZ()-3)>>4;z<=(p.getZ()+3)>>4;z++)h.getLevel().getChunk(x,z);for(BlockPos at:BlockPos.betweenClosed(p.offset(-3,-2,-3),p.offset(3,3,3))){at=at.immutable();saved.put(at,h.getLevel().getBlockState(at));h.getLevel().setBlock(at,at.getY()==p.getY()-1?Blocks.BEDROCK.defaultBlockState():Blocks.AIR.defaultBlockState(),3);}}
  <T extends Entity>T keep(T e){entities.add(e);return e;}
  @Override public void close(){for(Entity e:entities){if(e instanceof LivingEntity l&&l.getHealth()<=0)l.setHealth(1);e.discard();}boolean restoring=h.getLevel().restoringBlockSnapshots;h.getLevel().restoringBlockSnapshots=true;try{var rows=new ArrayList<>(saved.entrySet());Collections.reverse(rows);for(var e:rows)h.getLevel().setBlock(e.getKey(),e.getValue(),3);}finally{h.getLevel().restoringBlockSnapshots=restoring;}}
 }
 private static FluxGooBlock goo(){return (FluxGooBlock)TaintModule.block("flux_goo");}
 private static void quanta(GameTestHelper h,BlockPos p,int q){h.getLevel().setBlockAndUpdate(p,goo().defaultBlockState().setValue(FluxGooBlock.LEVEL,q-1));}
 private static void assertGooEventTick(GameTestHelper h,BlockPos p,String event){var ticks=((net.minecraft.world.ticks.LevelChunkTicks<Block>)h.getLevel().getChunkAt(p).getBlockTicks()).getAll().filter(t->t.type()==goo()&&t.pos().equals(p)).toList();h.assertTrue(ticks.size()==1&&ticks.get(0).triggerTick()==h.getLevel().getGameTime()+30,event+" did not schedule exactly one Goo update after30ticks: "+ticks);}
 @GameTest(template="empty") public static void finiteGooConservesEightQuantaOnVerticalMerge(GameTestHelper h){try(var f=new Arena(h)){
  h.getLevel().setBlockAndUpdate(f.p.below(),Blocks.AIR.defaultBlockState());h.getLevel().setBlockAndUpdate(f.p.below(2),Blocks.BEDROCK.defaultBlockState());quanta(h,f.p.below(),3);quanta(h,f.p,7);
  for(net.minecraft.core.Direction direction:net.minecraft.core.Direction.Plane.HORIZONTAL)h.getLevel().setBlockAndUpdate(f.p.relative(direction),Blocks.BEDROCK.defaultBlockState());
  goo().flowFinite(h.getLevel(),f.p,RandomSource.create(7));h.assertTrue(h.getLevel().getBlockState(f.p.below()).getValue(FluxGooBlock.LEVEL)==7&&h.getLevel().getBlockState(f.p).getValue(FluxGooBlock.LEVEL)==1,"Finite vertical merge duplicated or discarded quanta");
  quanta(h,f.p.below(),6);goo().flowFinite(h.getLevel(),f.p,RandomSource.create(7));h.assertTrue(h.getLevel().isEmptyBlock(f.p)&&h.getLevel().getBlockState(f.p.below()).getValue(FluxGooBlock.LEVEL)==7,"Complete finite merge recreated the emptied source fluid");
  h.getLevel().setBlockAndUpdate(f.p.below(),Blocks.AIR.defaultBlockState());quanta(h,f.p,8);goo().flowFinite(h.getLevel(),f.p,RandomSource.create(7));h.assertTrue(h.getLevel().isEmptyBlock(f.p)&&h.getLevel().getBlockState(f.p.below()).getValue(FluxGooBlock.LEVEL)==7,"Full finite fall duplicated the source instead of moving eight quanta");
 }h.succeed();}
 @GameTest(template="empty") public static void finiteGooHorizontalBalanceCannotCreateSources(GameTestHelper h){try(var f=new Arena(h)){
  quanta(h,f.p,8);goo().flowFinite(h.getLevel(),f.p,RandomSource.create(23));int total=0,count=0;
  for(BlockPos p:BlockPos.betweenClosed(f.p.offset(-1,0,-1),f.p.offset(1,0,1)))if(h.getLevel().getBlockState(p).is(goo())){total+=h.getLevel().getBlockState(p).getValue(FluxGooBlock.LEVEL)+1;count++;}
  h.assertTrue(total==8&&count==5,"Finite lateral balance minted fluid or missed four sides");
 }h.succeed();}
 @GameTest(template="empty") public static void fullGooCollisionStopsHorizontalMotionAndUsesOriginalUncurableTier(GameTestHelper h){try(var f=new Arena(h)){
  Cow cow=f.keep(EntityType.COW.create(h.getLevel()));cow.setDeltaMovement(1,.4,1);cow.setPos(Vec3.atBottomCenterOf(f.p));goo().entityInside(goo().defaultBlockState(),h.getLevel(),f.p,cow);
  var effect=cow.getEffect(InfusionEffects.VIS_EXHAUST.get());h.assertTrue(cow.getDeltaMovement().x==0&&cow.getDeltaMovement().z==0&&cow.getDeltaMovement().y==.4&&effect!=null&&effect.getAmplifier()==2&&effect.getDuration()==600&&effect.getCurativeItems().isEmpty(),"Original Goo collision motion/VisExhaust/curatives changed");
 }h.succeed();}
 @GameTest(template="empty") public static void gooEvaporationDoesNotInventPeriodicStableUpdates(GameTestHelper h){try(var f=new Arena(h)){
  for(int q=1;q<=8;q++){var state=goo().defaultBlockState().setValue(FluxGooBlock.LEVEL,q-1);h.assertTrue(state.isRandomlyTicking()&&!state.getFluidState().isRandomlyTicking(),"Goo lost original block random ticks or acquired duplicate fluid random ticks at "+q+"quanta");}
  quanta(h,f.p,6);assertGooEventTick(h,f.p,"Placement");quanta(h,f.p.east(),8);h.getLevel().getBlockTicks().clearArea(net.minecraft.world.level.levelgen.structure.BoundingBox.fromCorners(f.p,f.p.east()));h.assertTrue(!h.getLevel().getBlockTicks().hasScheduledTick(f.p,goo())&&!h.getLevel().getBlockTicks().hasScheduledTick(f.p.east(),goo()),"Fixture did not clear the initial Goo event ticks");long seed=0;
  for(;seed<10000;seed++){var random=RandomSource.create(seed);if(random.nextInt(50)!=0&&random.nextInt(4)==0)break;}
  h.assertTrue(seed<10000,"Could not establish detached original evaporation branch");goo().tick(h.getLevel().getBlockState(f.p),h.getLevel(),f.p,RandomSource.create(seed));
  var evaporated=h.getLevel().getBlockState(f.p);h.assertTrue(evaporated.is(goo())&&evaporated.getValue(FluxGooBlock.LEVEL)==4,"Evaporation did not remove exactly one quantum: "+evaporated);
  h.assertTrue(!h.getLevel().getBlockTicks().hasScheduledTick(f.p,goo()),"Evaporation's same-block LEVEL change rescheduled stable Goo");
  h.assertTrue(!h.getLevel().getBlockTicks().hasScheduledTick(f.p.east(),goo()),"Flag2 evaporation shape update incorrectly woke adjacent stable Goo");
  h.getLevel().neighborChanged(f.p,Blocks.STONE,f.p.east());assertGooEventTick(h,f.p,"Direct neighbor notification");h.getLevel().getBlockTicks().clearArea(net.minecraft.world.level.levelgen.structure.BoundingBox.fromCorners(f.p,f.p));
  h.getLevel().setBlockAndUpdate(f.p.east(),Blocks.BEDROCK.defaultBlockState());assertGooEventTick(h,f.p,"Neighbor block change");
 }h.succeed();}
 @GameTest(template="empty") public static void finalGooQuantumEvaporatesIntoAirInsteadOfRestoringFluid(GameTestHelper h){try(var f=new Arena(h)){
  quanta(h,f.p,1);long seed=0;for(;seed<10000;seed++){var random=RandomSource.create(seed);if(random.nextInt(4)==0&&random.nextBoolean())break;}
  h.assertTrue(seed<10000,"Could not establish detached final-quantum Flux/air branch");goo().randomTick(h.getLevel().getBlockState(f.p),h.getLevel(),f.p,RandomSource.create(seed));h.assertTrue(h.getLevel().isEmptyBlock(f.p),"Final Goo quantum evaporation recreated the fluid instead of air");
 }h.succeed();}
 @GameTest(template="empty") public static void gooConversionRemovesFluidAfterAcceptedSlimeSpawn(GameTestHelper h){try(var f=new Arena(h)){
  long seed=0;for(;seed<10000;seed++)if(RandomSource.create(seed).nextInt(50)==0)break;h.assertTrue(seed<10000,"Could not establish detached original slime-conversion branch");
  for(int q:new int[]{6,8}){quanta(h,f.p,q);goo().tick(h.getLevel().getBlockState(f.p),h.getLevel(),f.p,RandomSource.create(seed));var spawned=h.getLevel().getEntitiesOfClass(ThaumicSlimeEntity.class,new AABB(f.p).inflate(2),slime->!f.entities.contains(slime));spawned.forEach(f::keep);h.assertTrue(spawned.size()==1&&spawned.get(0).slimeSize()==(q<7?1:2)&&h.getLevel().isEmptyBlock(f.p),"Accepted original size"+(q<7?1:2)+" slime conversion left Goo or created incorrect slimes");}
 }h.succeed();}
 @GameTest(template="empty") public static void summonedSwarmKeepsNativeGravityKillCreditAndNoKnockback(GameTestHelper h){try(var f=new Arena(h)){
  var swarm=f.keep((TaintSwarmEntity)TaintModule.activeMob("taint_swarm",h.getLevel()));Cow cow=f.keep(EntityType.COW.create(h.getLevel()));swarm.setPos(Vec3.atBottomCenterOf(f.p));cow.setPos(Vec3.atBottomCenterOf(f.p));swarm.setSummoned(true);swarm.setTarget(cow);swarm.setNoAi(true);h.getLevel().addFreshEntity(cow);h.getLevel().addFreshEntity(swarm);Vec3 motion=new Vec3(.12,.34,-.56);cow.setDeltaMovement(motion);swarm.aiStep();
  int recent=net.minecraftforge.fml.util.ObfuscationReflectionHelper.getPrivateValue(LivingEntity.class,cow,"f_20889_");
  h.assertTrue(!swarm.isNoGravity()&&swarm.attackCooldown()>=15&&swarm.attackCooldown()<=24&&cow.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS)&&cow.getDeltaMovement().equals(motion)&&recent==100,"Swarm invented zero gravity or lost original attack cadence/Weakness/motion/kill credit");
 }h.succeed();}
 @GameTest(template="empty") public static void bucketCannotPromoteOneQuantumIntoFullBucket(GameTestHelper h){try(var f=new Arena(h)){
  quanta(h,f.p,1);h.assertTrue(goo().pickupBlock(h.getLevel(),f.p,h.getLevel().getBlockState(f.p)).isEmpty()&&h.getLevel().getBlockState(f.p).is(goo()),"One quantum became a full bucket");
  quanta(h,f.p,8);var stack=goo().pickupBlock(h.getLevel(),f.p,h.getLevel().getBlockState(f.p));h.assertTrue(stack.is(TaintEcologyModule.GOO_BUCKET.get())&&h.getLevel().isEmptyBlock(f.p),"Full finite bucket failed its debit");
 }h.succeed();}
 @GameTest(template="empty") public static void slimeUsesIntegerSizesSpitStatsAndOriginalSavedSize(GameTestHelper h){try(var f=new Arena(h)){
  var slime=f.keep((ThaumicSlimeEntity)TaintModule.activeMob("thaumic_slime",h.getLevel()));slime.setSlimeSize(5,true);slime.setHealth(12);var tag=new CompoundTag();slime.addAdditionalSaveData(tag);
  var restored=f.keep((ThaumicSlimeEntity)TaintModule.activeMob("thaumic_slime",h.getLevel()));restored.readAdditionalSaveData(tag);
  h.assertTrue(tag.getInt("Size")==4&&restored.slimeSize()==5&&restored.getMaxHealth()==25&&restored.getAttributeValue(Attributes.ATTACK_DAMAGE)==6&&restored.getHealth()==12,"Integer slime stats/reload changed");
  h.assertTrue(!slime.checkSpawnRules(h.getLevel(),MobSpawnType.NATURAL),"Dormant original slime gained arbitrary natural spawns");
 }h.succeed();}
 @GameTest(template="empty") public static void realChampionAttributesSurviveReloadAndTaintCannotBeForgedWithNbt(GameTestHelper h){try(var f=new Arena(h)){
  Cow cow=f.keep(EntityType.COW.create(h.getLevel()));h.assertTrue(Champions.type(cow)==-2,"Fresh passive already champion");cow.getPersistentData().putInt("Champion",13);h.assertTrue(Champions.type(cow)==-2,"Arbitrary NBT impersonates champion");
  h.assertTrue(Champions.makeTainted(cow)&&Champions.type(cow)==13&&cow.getMaxHealth()==35&&cow.getAttributeValue(Attributes.ATTACK_DAMAGE)>0,"Real tainted attribute/buffs missing");
  var tag=new CompoundTag();cow.addAdditionalSaveData(tag);Cow restored=f.keep(EntityType.COW.create(h.getLevel()));restored.readAdditionalSaveData(tag);
  h.assertTrue(Champions.type(restored)==13&&restored.getMaxHealth()==35&&!Champions.makeTainted(restored),"Champion modifiers lost or duplicated on reload");
 }h.succeed();}
 @GameTest(template="empty") public static void giantKeepsOriginalChampionBuffsAndDamageCap(GameTestHelper h){try(var f=new Arena(h)){
  var giant=f.keep((TaintacleGiantEntity)TaintModule.activeMob("taintacle_giant",h.getLevel()));h.assertTrue(Champions.makeChampion(giant,true,7),"Giant forced champion failed");h.assertTrue(giant.getMaxHealth()==275&&giant.getAttributeValue(Attributes.ATTACK_DAMAGE)==27,"Giant was incorrectly treated as EntityThaumcraftBoss for champion buffs");
  giant.setHealth(275);giant.hurt(giant.damageSources().generic(),80);h.assertTrue(giant.getHealth()>=240&&giant.anger()==200&&giant.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION),"Original giant cap/enrage changed");
 }h.succeed();}
 @GameTest(template="empty") public static void bottleImpactsUseCubeAndDoNotInfectUndeadOrNativeTaint(GameTestHelper h){try(var f=new Arena(h)){
  var bottle=f.keep(TaintEcologyModule.BOTTLE_TAINT.get().create(h.getLevel()));bottle.setPos(Vec3.atCenterOf(f.p));
  Cow cow=f.keep(EntityType.COW.create(h.getLevel()));cow.setPos(f.p.getX()+4.9,f.p.getY(),f.p.getZ()+4.9);h.getLevel().addFreshEntity(cow);
  var crawler=f.keep(TaintModule.activeMob("taint_crawler",h.getLevel()));crawler.setPos(Vec3.atCenterOf(f.p));h.getLevel().addFreshEntity(crawler);
  bottle.onHit(new BlockHitResult(bottle.position(),net.minecraft.core.Direction.UP,f.p,false));h.assertTrue(cow.hasEffect(InfusionEffects.FLUX_TAINT.get())&&!crawler.hasEffect(InfusionEffects.FLUX_TAINT.get())&&bottle.isRemoved(),"Bottle cube/native exclusions/impact removal changed");
 }h.succeed();}
}
