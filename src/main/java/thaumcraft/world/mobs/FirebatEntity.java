package thaumcraft.world.mobs;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thaumcraft.catalog.entities.VisualMobEntity;

/** Original hanging/flight/chase and explosive melee, using the already established modern bat flight lifecycle. */
public final class FirebatEntity extends VisualMobEntity implements Enemy {
    private static final EntityDataAccessor<Boolean> HANGING=SynchedEntityData.defineId(FirebatEntity.class,EntityDataSerializers.BOOLEAN);
    private BlockPos flightTarget;
    private int attackTime;
    private boolean nativePersistence;
    public int damBonus;
    public FirebatEntity(EntityType<? extends VisualMobEntity> type,Level level){
        super(type,level);setNoAi(false);setNoGravity(true);nativePersistence=false;xpReward=5;setHanging(true);
    }
    public static AttributeSupplier.Builder attributes(){return createMobAttributes().add(Attributes.MAX_HEALTH,5).add(Attributes.ATTACK_DAMAGE,1).add(Attributes.MOVEMENT_SPEED,.25).add(Attributes.FOLLOW_RANGE,16);}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(HANGING,false);}
    public boolean hanging(){return entityData.get(HANGING);}
    public void setHanging(boolean value){entityData.set(HANGING,value);}
    public int attackTime(){return attackTime;}
    @Override public void tick(){
        super.tick();
        if(!level().isClientSide&&isInWaterOrRain())hurt(damageSources().drown(),1);
        if(hanging()){setDeltaMovement(Vec3.ZERO);setPos(getX(),Mth.floor(getY())+1-getBbHeight(),getZ());}
        else setDeltaMovement(getDeltaMovement().multiply(1,.6000000238418579,1));
    }
    @Override protected void customServerAiStep(){
        if(attackTime>0)attackTime--;
        BlockPos pos=blockPosition(),above=pos.above();
        if(hanging()){
            if(!level().getBlockState(above).isRedstoneConductor(level(),above)){setHanging(false);level().levelEvent(null,1025,pos,0);}
            else{
                if(random.nextInt(200)==0)yHeadRot=random.nextInt(360);
                if(level().getNearestPlayer(this,4)!=null){setHanging(false);level().levelEvent(null,1025,pos,0);}
            }
        }else{
            LivingEntity target=getTarget();Vec3 destination;
            if(target==null){
                if(flightTarget!=null&&(!level().isEmptyBlock(flightTarget)||flightTarget.getY()<level().getMinBuildHeight()+1))flightTarget=null;
                if(flightTarget==null||random.nextInt(30)==0||flightTarget.distToCenterSqr(position())<4)
                    flightTarget=new BlockPos((int)getX()+random.nextInt(7)-random.nextInt(7),(int)getY()+random.nextInt(6)-2,(int)getZ()+random.nextInt(7)-random.nextInt(7));
                destination=new Vec3(flightTarget.getX()+.5,flightTarget.getY()+.1,flightTarget.getZ()+.5);
                if(random.nextInt(100)==0&&level().getBlockState(above).isRedstoneConductor(level(),above))setHanging(true);
            }else destination=target.position().add(0,target.getEyeHeight()*.66F,0);
            if(level().hasChunkAt(BlockPos.containing(destination))){
                Vec3 delta=destination.subtract(position()),motion=getDeltaMovement();
                setDeltaMovement(motion.add((Math.signum(delta.x)*.5-motion.x)*.10000000149011612,
                    (Math.signum(delta.y)*.699999988079071-motion.y)*.10000000149011612,
                    (Math.signum(delta.z)*.5-motion.z)*.10000000149011612));
                float yaw=(float)(Math.atan2(getDeltaMovement().z,getDeltaMovement().x)*180/Math.PI)-90;
                setYRot(getYRot()+Mth.wrapDegrees(yaw-getYRot()));yBodyRot=getYRot();
            }
        }
        LivingEntity target=getTarget();
        if(target==null)setTarget(level().getNearestPlayer(this,12));
        else if(target.isAlive()&&!target.isRemoved()){
            if(isAlive()&&hasLineOfSight(target))attack(target);
        }else setTarget(null);
        if(getTarget() instanceof Player player&&player.getAbilities().invulnerable)setTarget(null);
    }
    public void attack(LivingEntity target){
        if(attackTime>0||distanceTo(target)>=Math.max(2.5F,target.getBbWidth()*1.1F)
                ||target.getBoundingBox().maxY<=getBoundingBox().minY||target.getBoundingBox().minY>=getBoundingBox().maxY)return;
        attackTime=20+random.nextInt(20);
        if(random.nextInt(10)==0&&!level().isClientSide){target.invulnerableTime=0;level().explode(this,getX(),getY(),getZ(),1.5F,Level.ExplosionInteraction.NONE);discard();}
        playSound(SoundEvents.BAT_HURT,.5F,.9F+random.nextFloat()*.2F);
        doHurtTarget(target);
    }
    @Override public void travel(Vec3 input){
        if(level() instanceof ServerLevel server&&!server.hasChunkAt(BlockPos.containing(position().add(getDeltaMovement()))))setDeltaMovement(Vec3.ZERO);
        move(MoverType.SELF,getDeltaMovement());
    }
    @Override public boolean hurt(DamageSource source,float amount){
        if(isInvulnerableTo(source)||source.is(DamageTypeTags.IS_FIRE)||source.is(DamageTypeTags.IS_EXPLOSION))return false;
        if(!level().isClientSide&&hanging())setHanging(false);
        return super.hurt(source,amount);
    }
    @Override public boolean isPushable(){return false;}
    @Override public boolean isIgnoringBlockTriggers(){return true;}
    @Override public boolean causeFallDamage(float d,float m,DamageSource source){return false;}
    @Override public boolean removeWhenFarAway(double distance){return true;}
    @Override protected boolean shouldDespawnInPeaceful(){return true;}
    @Override public boolean isPersistenceRequired(){return nativePersistence;}
    @Override public void setPersistenceRequired(){super.setPersistenceRequired();nativePersistence=true;}
    @Override protected SoundEvent getAmbientSound(){return hanging()&&random.nextInt(4)!=0?null:SoundEvents.BAT_AMBIENT;}
    @Override protected SoundEvent getHurtSound(DamageSource source){return SoundEvents.BAT_HURT;}
    @Override protected SoundEvent getDeathSound(){return SoundEvents.BAT_DEATH;}
    @Override protected float getSoundVolume(){return .1F;}
    @Override public float getVoicePitch(){return super.getVoicePitch()*.95F;}
    @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean hit){
        int amount=random.nextInt(3);if(looting>0)amount+=random.nextInt(looting+1);
        for(int i=0;i<amount;i++)spawnAtLocation(Items.GUNPOWDER);
    }
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.remove("VisualOnly");tag.putBoolean("hang",hanging());tag.putByte("damBonus",(byte)damBonus);tag.putBoolean("PersistenceRequired",nativePersistence);}
    @Override public void readAdditionalSaveData(CompoundTag tag){
        super.readAdditionalSaveData(tag);setNoAi(tag.getBoolean("NoAI")&&!tag.getBoolean("VisualOnly"));setNoGravity(true);nativePersistence=tag.getBoolean("PersistenceRequired")&&!tag.getBoolean("VisualOnly");
        setHanging(tag.getBoolean("hang"));damBonus=tag.getByte("damBonus");attackTime=0;flightTarget=null;
    }
}
