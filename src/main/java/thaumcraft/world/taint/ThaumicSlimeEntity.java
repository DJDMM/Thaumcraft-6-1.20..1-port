package thaumcraft.world.taint;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.*;
import net.minecraft.world.phys.Vec3;
import thaumcraft.catalog.entities.VisualMobEntity;
import thaumcraft.alchemy.AspectCrystalItem;
import thaumcraft.api.aspects.Aspect;
/** Finite-goo slime: arbitrary integer sizes, one-unit spit cost and exact unit splitting. */
public final class ThaumicSlimeEntity extends TaintedMobileMob {
 private static final EntityDataAccessor<Integer> SIZE=SynchedEntityData.defineId(ThaumicSlimeEntity.class,EntityDataSerializers.INT);
 private int spitCounter=100,jumpDelay=10,launched=10;private boolean split;public float squish,oSquish;
 public ThaumicSlimeEntity(EntityType<? extends VisualMobEntity> type,Level level){super(type,level);setSlimeSize(1<<(1+random.nextInt(3)),true);}
 public static AttributeSupplier.Builder attributes(){return createMobAttributes().add(Attributes.MAX_HEALTH,4).add(Attributes.MOVEMENT_SPEED,.4).add(Attributes.ATTACK_DAMAGE,3).add(Attributes.FOLLOW_RANGE,16);}
 @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(SIZE,2);}
 public int slimeSize(){return entityData.get(SIZE);}
 public void setSlimeSize(int size,boolean heal){size=Math.max(1,Math.min(64,size));entityData.set(SIZE,size);refreshDimensions();getAttribute(Attributes.MAX_HEALTH).setBaseValue(size*size);getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(.2+.1*size);getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(size+1);xpReward=size+2;if(heal)setHealth(getMaxHealth());}
 @Override public EntityDimensions getDimensions(Pose pose){float dimension=.51000005F*slimeSize();return EntityDimensions.scalable(dimension,dimension);}
 @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key){super.onSyncedDataUpdated(key);if(SIZE.equals(key))refreshDimensions();}
 @Override protected void registerGoals(){targetSelector.addGoal(1,new NearestAttackableTargetGoal<>(this,Player.class,true));}
 @Override public SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level,net.minecraft.world.DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData data,CompoundTag tag){
  for(int pass=0;pass<2;pass++){int size=random.nextInt(3);if(size<2&&random.nextFloat()<.5F*difficulty.getSpecialMultiplier())size++;setSlimeSize(1<<size,true);}
  return super.finalizeSpawn(level,difficulty,reason,data,tag);
 }
 @Override public void aiStep(){
  super.aiStep();if(level().isClientSide||!isAlive())return;
  Player closest=level().getNearestPlayer(this,16);if(closest!=null){if(spitCounter>0)spitCounter--;getLookControl().setLookAt(closest,10,20);
   if(distanceTo(closest)>4&&spitCounter<=0&&slimeSize()>2){spitCounter=101;var fresh=TaintModule.activeMob("thaumic_slime",(net.minecraft.server.level.ServerLevel)level());if(fresh instanceof ThaumicSlimeEntity child){
    child.setSlimeSize(1,true);Vec3 delta=closest.position().subtract(position());double horizontal=Math.sqrt(delta.x*delta.x+delta.z*delta.z);
    if(horizontal>=1e-7){double y=(getBoundingBox().minY+getBoundingBox().maxY)/2;child.moveTo(getX()+delta.x/horizontal,y,getZ()+delta.z/horizontal,getYRot(),getXRot());child.shoot(delta.x,closest.getY()+closest.getBbHeight()/3-y+horizontal*.2,delta.z,1.5F,1);}
    if(level().addFreshEntity(child)){playSound(TaintModule.GORE.get(),1,((random.nextFloat()-random.nextFloat())*.2F+1)*.8F);setSlimeSize(slimeSize()-1,true);}
   }}
  }
  LivingEntity target=getTarget();if(target!=null&&target.isAlive()){double dx=target.getX()-getX(),dz=target.getZ()-getZ();setYRot((float)(Math.atan2(dz,dx)*180/Math.PI)-90);yBodyRot=yHeadRot=getYRot();}
  if(onGround()){if(--jumpDelay<=0){jumpDelay=10+random.nextInt(20);if(target!=null)jumpDelay/=3;jumpFromGround();float yaw=getYRot()*(float)Math.PI/180;setDeltaMovement(getDeltaMovement().add(-Math.sin(yaw)*.2,0,Math.cos(yaw)*.2));squish=1;}}
 }
 public void shoot(double x,double y,double z,float velocity,float inaccuracy){Vec3 dir=new Vec3(x,y,z).normalize().add(random.nextGaussian()*.007499999832361937*inaccuracy,random.nextGaussian()*.007499999832361937*inaccuracy,random.nextGaussian()*.007499999832361937*inaccuracy).scale(velocity);setDeltaMovement(dir);}
 @Override public void tick(){boolean previous=onGround();oSquish=squish;super.tick();if(!previous&&onGround())squish=-.5F;squish*=.6F;if(level().isClientSide&&launched>0){launched--;net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->thaumcraft.world.taint.client.TaintEcologyClient.slime(this,slimeSize()*(launched+1)));}}
 @Override public void playerTouch(Player player){contactDamage(player);}
 @Override public void push(Entity other){super.push(other);if(other instanceof net.minecraft.world.entity.animal.IronGolem golem)contactDamage(golem);}
 private void contactDamage(LivingEntity target){int reach=slimeSize()+(launched>0?2:0);if(isAlive()&&getSensing().hasLineOfSight(target)&&distanceToSqr(target)<.6*reach*.6*reach&&target.hurt(damageSources().mobAttack(this),slimeSize()+1))playSound(SoundEvents.SLIME_ATTACK,1,(random.nextFloat()-random.nextFloat())*.2F+1);}
 @Override public void remove(RemovalReason reason){if(!split&&!level().isClientSide&&getHealth()<=0&&slimeSize()>1){split=true;int size=slimeSize();for(int k=0;k<size;k++){var mob=TaintModule.activeMob("thaumic_slime",(net.minecraft.server.level.ServerLevel)level());if(mob instanceof ThaumicSlimeEntity child){child.setSlimeSize(1,true);child.moveTo(getX()+(k%2-.5F)*size/4,getY()+.5,getZ()+(k/2-.5F)*size/4,random.nextFloat()*360,0);level().addFreshEntity(child);}}}super.remove(reason);}
 @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean hit){if(slimeSize()>1)spawnAtLocation(AspectCrystalItem.create(Aspect.FLUX,1),getBbHeight()/2);}
 @Override public boolean checkSpawnRules(LevelAccessor level,MobSpawnType reason){return false;}
 @Override protected SoundEvent getHurtSound(DamageSource source){return SoundEvents.SLIME_HURT;}
 @Override protected SoundEvent getDeathSound(){return SoundEvents.SLIME_DEATH;}
 @Override protected float getStandingEyeHeight(Pose pose,EntityDimensions size){return .625F*getBbHeight();}
 @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putInt("Size",slimeSize()-1);}
 @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);setSlimeSize(tag.getInt("Size")+1,false);spitCounter=100;launched=10;split=false;}
}
