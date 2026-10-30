package thaumcraft.world.eldritch;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.catalog.entities.VisualMobEntity;
/** Original crab helmet, leap/ride attack and break threshold. */
public final class EldritchCrabEntity extends VisualMobEntity implements Enemy,thaumcraft.api.entities.IEldritchMob {
 private static final EntityDataAccessor<Boolean> HELMET=SynchedEntityData.defineId(EldritchCrabEntity.class,EntityDataSerializers.BOOLEAN);
 private static final EntityDataAccessor<Integer> RIDING=SynchedEntityData.defineId(EldritchCrabEntity.class,EntityDataSerializers.INT);
 private int attackTime;private boolean nativePersistence;
 public EldritchCrabEntity(EntityType<? extends VisualMobEntity> type,Level level){super(type,level);setNoAi(false);nativePersistence=false;xpReward=6;if(getNavigation() instanceof GroundPathNavigation ground)ground.setCanOpenDoors(true);}
 public static AttributeSupplier.Builder attributes(){return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,20).add(Attributes.ATTACK_DAMAGE,4).add(Attributes.MOVEMENT_SPEED,.3);}
 @Override protected void registerGoals(){goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(2,new LeapAtTargetGoal(this,.63F));goalSelector.addGoal(3,new MeleeAttackGoal(this,1,false));goalSelector.addGoal(7,new RandomStrollGoal(this,.8));goalSelector.addGoal(8,new RandomLookAroundGoal(this));targetSelector.addGoal(1,new HurtByTargetGoal(this).setAlertOthers());targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));targetSelector.addGoal(3,new NearestAttackableTargetGoal<>(this,thaumcraft.world.crimson.CultistEntity.class,true));}
 @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(HELMET,false);entityData.define(RIDING,-1);}
 @Override public boolean hasHelm(){return entityData.get(HELMET);}
 public void setHelm(boolean helmet){entityData.set(HELMET,helmet);}
 public int ridingTargetId(){return entityData.get(RIDING);}
 public int attackCooldown(){return attackTime;}
 @Override public int getArmorValue(){return hasHelm()?5:0;}
 @Override public double getMyRidingOffset(){return isPassenger()?.5:0;}
 @Override public void tick(){super.tick();attackTime--;if(tickCount<20)fallDistance=0;
  if(!level().isClientSide){LivingEntity target=getTarget();if(!isPassenger()&&target!=null&&!target.isVehicle()&&!onGround()&&!hasHelm()&&target.isAlive()&&getY()-target.getY()>=target.getBbHeight()/2&&distanceToSqr(target)<4&&startRiding(target))entityData.set(RIDING,target.getId());
   if(isPassenger()&&isAlive()&&attackTime<=0){attackTime=10+random.nextInt(10);doHurtTarget(getVehicle());if(random.nextFloat()<.2F){stopRiding();entityData.set(RIDING,-1);}}
   if(!isPassenger()&&ridingTargetId()!=-1)entityData.set(RIDING,-1);
  }else if(!isPassenger()&&ridingTargetId()!=-1){var vehicle=level().getEntity(ridingTargetId());if(vehicle!=null)startRiding(vehicle);}else if(isPassenger()&&ridingTargetId()==-1)stopRiding();
 }
 @Override public boolean doHurtTarget(Entity target){if(super.doHurtTarget(target)){playSound(EldritchModule.sound("crabclaw"),1,.9F+random.nextFloat()*.2F);return true;}return false;}
 @Override public boolean hurt(DamageSource source,float amount){boolean result=super.hurt(source,amount);if(hasHelm()&&getHealth()/getMaxHealth()<=.5F){setHelm(false);getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(.3);if(!level().isClientSide)level().broadcastEntityEvent(this,(byte)63);}return result;}
 @Override public boolean canBeAffected(MobEffectInstance effect){return effect.getEffect()!=MobEffects.POISON&&super.canBeAffected(effect);}
 @Override public MobType getMobType(){return MobType.ARTHROPOD;}
 @Override public boolean isAlliedTo(Entity entity){return entity instanceof EldritchCrabEntity;}
 @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData data,CompoundTag tag){
  setHelm(level.getDifficulty()==Difficulty.HARD||random.nextFloat()<.33F);
  if(data==null){var group=new Spider.SpiderEffectsGroupData();if(level.getDifficulty()==Difficulty.HARD&&level.getRandom().nextFloat()<.1F*difficulty.getSpecialMultiplier())group.setRandomEffect(level.getRandom());data=group;}
  if(data instanceof Spider.SpiderEffectsGroupData group&&group.effect!=null)addEffect(new MobEffectInstance(group.effect,Integer.MAX_VALUE));
  return super.finalizeSpawn(level,difficulty,reason,data,tag);
 }
 @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean recentlyHit){if(recentlyHit&&(random.nextInt(3)==0||random.nextInt(1+looting)>0))spawnAtLocation(Items.ENDER_PEARL);}
 @Override public void handleEntityEvent(byte event){if(event==63)net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->thaumcraft.world.eldritch.client.EldritchClient.crabHelmet(this));else super.handleEntityEvent(event);}
 @Override protected SoundEvent getAmbientSound(){return EldritchModule.sound("crabtalk");}
 @Override protected SoundEvent getHurtSound(DamageSource source){return SoundEvents.GENERIC_HURT;}
 @Override protected SoundEvent getDeathSound(){return EldritchModule.sound("crabdeath");}
 @Override public int getAmbientSoundInterval(){return 160;}
 @Override protected void playStepSound(BlockPos pos,BlockState state){playSound(SoundEvents.SPIDER_STEP,.15F,1);}
 @Override public boolean removeWhenFarAway(double distance){return true;}
 @Override protected boolean shouldDespawnInPeaceful(){return true;}
 @Override public boolean isPersistenceRequired(){return nativePersistence;}
 @Override public void setPersistenceRequired(){super.setPersistenceRequired();nativePersistence=true;}
 @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.remove("VisualOnly");tag.putBoolean("helm",hasHelm());tag.putBoolean("PersistenceRequired",nativePersistence);}
 @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);setNoAi(!tag.getBoolean("VisualOnly")&&tag.getBoolean("NoAI"));setHelm(tag.contains("helm")?tag.getBoolean("helm"):tag.getBoolean("Helm"));if(!hasHelm())getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(.3);nativePersistence=!tag.getBoolean("VisualOnly")&&tag.getBoolean("PersistenceRequired");attackTime=0;entityData.set(RIDING,-1);}
}
