package thaumcraft.world.eldritch;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.sounds.*;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import thaumcraft.catalog.entities.*;

/** Two-phase original golem with a real seeking-orb magazine, not a beam substitute. */
public final class EldritchGolemEntity extends ThaumcraftBossEntity implements RangedAttackMob,thaumcraft.api.entities.IEldritchMob {
    private static final EntityDataAccessor<Boolean> HEADLESS=SynchedEntityData.defineId(EldritchGolemEntity.class,EntityDataSerializers.BOOLEAN);
    private int beamCharge,attackTimer,arcing,ax,ay,az;private boolean chargingBeam,addedRanged;
    public EldritchGolemEntity(EntityType<? extends VisualMobEntity> type,Level level){super(type,level);}
    public static AttributeSupplier.Builder attributes(){return bossAttributes(400,10,.3).add(Attributes.ARMOR,6);}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(HEADLESS,false);}
    @Override protected void registerGoals(){goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(3,new MeleeAttackGoal(this,1.1,false));goalSelector.addGoal(6,new MoveTowardsRestrictionGoal(this,.8));goalSelector.addGoal(7,new RandomStrollGoal(this,.8));goalSelector.addGoal(8,new LookAtPlayerGoal(this,Player.class,8));goalSelector.addGoal(8,new RandomLookAroundGoal(this));targetSelector.addGoal(1,new HurtByTargetGoal(this).setAlertOthers());targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));}
    @Override public boolean headless(){return entityData.get(HEADLESS);}
    public void setHeadless(boolean value){entityData.set(HEADLESS,value);if(value&&!addedRanged){goalSelector.addGoal(2,new BossRangedGoal(this,3,5,5,24));addedRanged=true;}}
    public int attackTimer(){return attackTimer;}
    public int beamCharge(){return beamCharge;}
    public boolean chargingBeam(){return chargingBeam;}
    @Override protected float getStandingEyeHeight(Pose pose,EntityDimensions size){return headless()?3.33F:3;}
    @Override public boolean isInvulnerableTo(DamageSource source){return source.is(DamageTypeTags.IS_FIRE)||super.isInvulnerableTo(source);}
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData data,CompoundTag tag){spawnTimer=100;return super.finalizeSpawn(level,difficulty,reason,data,tag);}
    @Override public boolean hurt(DamageSource source,float amount){
        // Original checks raw incoming damage before its superclass's 35-point cap.
        if(!level().isClientSide&&amount>getHealth()&&!headless()){setHeadless(true);spawnTimer=100;double angle=getYRot()%360*Math.PI/180;level().explode(this,getX()+Math.cos(angle)*.75,getEyeY(),getZ()+Math.sin(angle)*.75,2,Level.ExplosionInteraction.NONE);return false;}return super.hurt(source,amount);
    }
    @Override public boolean doHurtTarget(Entity target){if(attackTimer>0)return false;attackTimer=10;level().broadcastEntityEvent(this,(byte)4);boolean hit=target.hurt(damageSources().mobAttack(this),(float)getAttributeValue(Attributes.ATTACK_DAMAGE)*.75F);if(hit){target.setDeltaMovement(target.getDeltaMovement().add(0,.2000000059604645,0));if(headless())target.push(-Math.sin(getYRot()*Math.PI/180)*1.5,.1,Math.cos(getYRot()*Math.PI/180)*1.5);}return hit;}
    @Override public void performRangedAttack(LivingEntity target,float distance){if(level().isClientSide||!hasLineOfSight(target)||chargingBeam||beamCharge<=0)return;beamCharge-=15+random.nextInt(5);getLookControl().setLookAt(target.getX(),target.getY()+target.getBbHeight()/2,target.getZ(),30,30);var orb=VisualEntitiesModule.GOLEM_ORB.get().create(level());if(orb==null)return;orb.setOwner(this);orb.setTarget(target);orb.setRed(false);Vec3 look=getLookAngle();orb.setPos(getX()+look.x,getEyeY()-.1,getZ()+look.z);orb.shoot(target.getX()+target.getDeltaMovement().x-getX(),target.getY()-getY()-target.getBbHeight()/2,target.getZ()+target.getDeltaMovement().z-getZ(),.66F,5);playSound(EldritchModule.sound("egattack"),1,1+random.nextFloat()*.1F);level().addFreshEntity(orb);}
    @Override public void tick(){if(spawnTimer==150&&!level().isClientSide)level().broadcastEntityEvent(this,(byte)18);if(spawnTimer>0)heal(2);super.tick();if(attackTimer>0)attackTimer--;
        if(getDeltaMovement().horizontalDistanceSqr()>2.500000277905201E-7&&random.nextInt(5)==0){BlockState stepping=level().getBlockState(blockPosition());if(level().isClientSide&&!stepping.isAir())level().addParticle(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK,stepping),getX()+(random.nextFloat()-.5)*getBbWidth(),getBoundingBox().minY+.1,getZ()+(random.nextFloat()-.5)*getBbWidth(),4*(random.nextFloat()-.5),.5,(random.nextFloat()-.5)*4);if(!level().isClientSide&&stepping.getBlock() instanceof thaumcraft.world.crimson.MoundLootBlock)level().destroyBlock(blockPosition(),true,this);}
        if(!level().isClientSide&&isAlive()){
            BlockPos p=blockPosition();BlockState state=level().getBlockState(p);float hardness=state.getDestroySpeed(level(),p);if(!state.isAir()&&hardness>=0&&hardness<=.15F)level().destroyBlock(p,true,this);
            if(headless()&&beamCharge<=0)chargingBeam=true;if(headless()&&chargingBeam){beamCharge++;level().broadcastEntityEvent(this,(byte)19);if(beamCharge==150)chargingBeam=false;}
        }else if(level().isClientSide&&headless()){
            setXRot(0);double a=Math.toRadians(yBodyRot);Vec3 look=new Vec3(-Math.sin(a),0,Math.cos(a));if(random.nextInt(20)==0)EldritchModule.spark(this,position().add(look).add((random.nextFloat()-random.nextFloat())/3,getEyeHeight()-.75,(random.nextFloat()-random.nextFloat())/3));
            thaumcraft.fx.FxDispatcher.emit(level(),"vent",position().add(look.scale(.4)).add(0,getEyeHeight()-1.25,0),new Vec3(0,.001,0),5592405,4,0);
            if(arcing>0){EldritchModule.arc(this,position().add(0,getBbHeight()/2,0),new Vec3(ax+.5,ay+1,az+.5),.65F+random.nextFloat()*.1F,1,1,1-arcing/10F);arcing--;}
        }
    }
    @Override public void handleEntityEvent(byte event){if(event==4){attackTimer=10;playSound(SoundEvents.IRON_GOLEM_ATTACK,1,1);}else if(event==18)spawnTimer=150;else if(event==19){if(arcing==0){float radius=2+random.nextFloat()*2;double angle=Math.toRadians(random.nextInt(360));int x=net.minecraft.util.Mth.floor(getX()+radius*Math.cos(angle)),y=net.minecraft.util.Mth.floor(getY()),z=net.minecraft.util.Mth.floor(getZ()+radius*Math.sin(angle));BlockPos probe=new BlockPos(x,y,z);for(int c=0;c<5&&level().isEmptyBlock(probe);c++,y--){}if(level().isEmptyBlock(probe.above())&&!level().isEmptyBlock(probe)){ax=x;ay=y;az=z;arcing=8+random.nextInt(5);playSound(EldritchModule.sound("jacobs"),.8F,1+(random.nextFloat()-random.nextFloat())*.05F);}}}else super.handleEntityEvent(event);}
    @Override protected SoundEvent getHurtSound(DamageSource source){return SoundEvents.IRON_GOLEM_HURT;}
    @Override protected SoundEvent getDeathSound(){return SoundEvents.IRON_GOLEM_DEATH;}
    @Override protected void playStepSound(BlockPos p,BlockState state){playSound(SoundEvents.IRON_GOLEM_STEP,1,1);}
    @Override public void generateName(){int type=thaumcraft.world.taint.champion.Champions.type(this);if(type>=0)setCustomName(net.minecraft.network.chat.Component.translatable("entity.Thaumcraft.EldritchGolem.name.custom",EldritchModule.championName(type)));}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putBoolean("headless",headless());}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);setHeadless(tag.contains("headless")?tag.getBoolean("headless"):tag.getBoolean("Headless"));beamCharge=attackTimer=arcing=0;chargingBeam=false;}
}
