package thaumcraft.world.taint;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import thaumcraft.alchemy.AspectCrystalItem;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.catalog.entities.VisualMobEntity;

/** Original drifting swarm with native target acquisition, attack cadence and no attack knockback. */
public final class TaintSwarmEntity extends TaintedMobileMob {
    private static final EntityDataAccessor<Boolean> SUMMONED=SynchedEntityData.defineId(TaintSwarmEntity.class,EntityDataSerializers.BOOLEAN);
    private BlockPos flightTarget;
    private int attackTime;
    public int damBonus;
    private static final java.lang.reflect.Field RECENTLY_HIT=net.minecraftforge.fml.util.ObfuscationReflectionHelper.findField(LivingEntity.class,"f_20889_");
    public TaintSwarmEntity(EntityType<? extends VisualMobEntity> type,Level level) { super(type,level);xpReward=5; }
    public static AttributeSupplier.Builder attributes() { return createMobAttributes().add(Attributes.MAX_HEALTH,30).add(Attributes.ATTACK_DAMAGE,2).add(Attributes.MOVEMENT_SPEED,.7).add(Attributes.FOLLOW_RANGE,8); }
    @Override protected void defineSynchedData() { super.defineSynchedData();entityData.define(SUMMONED,false); }
    public boolean isSummoned() { return entityData.get(SUMMONED); }
    public void setSummoned(boolean summoned) { entityData.set(SUMMONED,summoned); }
    public int attackCooldown() { return attackTime; }
    @Override public void aiStep() {
        super.aiStep();if(attackTime>0)attackTime--;
        if(level().isClientSide||!(level() instanceof ServerLevel server)||!isAlive())return;
        LivingEntity target=getTarget();
        if(target!=null&&(!target.isAlive()||target.isRemoved()||target.level()!=server||target instanceof Player p&&p.getAbilities().invulnerable)) { setTarget(null);target=null; }
        if(target==null) {
            if(isSummoned())hurt(damageSources().generic(),5);
            if(flightTarget!=null&&(!TaintModule.loaded(server,flightTarget)||!server.isEmptyBlock(flightTarget)||flightTarget.getY()<1
                    ||flightTarget.getY()>server.getHeight(Heightmap.Types.MOTION_BLOCKING,flightTarget.getX(),flightTarget.getZ())+2||!TaintHelper.isNearTaintSeed(server,flightTarget)))flightTarget=null;
            if(flightTarget==null||random.nextInt(30)==0||distanceToSqr(Vec3.atCenterOf(flightTarget))<4)
                flightTarget=new BlockPos((int)getX()+random.nextInt(7)-random.nextInt(7),(int)getY()+random.nextInt(6)-2,(int)getZ()+random.nextInt(7)-random.nextInt(7));
            steer(Vec3.atCenterOf(flightTarget).add(0,-.4,0),.015000000014901161);
            if(!isSummoned()) {
                Player closest=server.getNearestPlayer(this,8);
                if(closest!=null&&!closest.getAbilities().invulnerable&&!closest.isSpectator())setTarget(closest);
            }
        } else {
            steer(new Vec3(target.getX(),target.getEyeY(),target.getZ()),.025000000149011613);
            if(getSensing().hasLineOfSight(target)&&attackTime<=0&&distanceTo(target)<3
                    &&target.getBoundingBox().maxY>getBoundingBox().minY&&target.getBoundingBox().minY<getBoundingBox().maxY) {
                attackTime=15+random.nextInt(10);Vec3 motion=target.getDeltaMovement();
                if(isSummoned())try{RECENTLY_HIT.setInt(target,100);}catch(IllegalAccessException error){throw new IllegalStateException("Cannot retain summoned swarm kill credit",error);}
                if(doHurtTarget(target))target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,100,0));
                target.setDeltaMovement(motion);target.hasImpulse=false;
                playSound(TaintEntitySounds.sound("swarmattack"),.3F,.9F+random.nextFloat()*.2F);
            }
        }
    }
    private void steer(Vec3 target,double horizontal) {
        Vec3 motion=getDeltaMovement(),delta=target.subtract(position());
        setDeltaMovement(motion.add((Math.signum(delta.x)*.5-motion.x)*horizontal,(Math.signum(delta.y)*.699999988079071-motion.y)*.10000000149011612,(Math.signum(delta.z)*.5-motion.z)*horizontal));
        Vec3 next=getDeltaMovement();setYRot(getYRot()+Mth.wrapDegrees((float)(Math.atan2(next.z,next.x)*180/Math.PI)-90-getYRot()));yBodyRot=yHeadRot=getYRot();
        zza=.1F;
    }
    @Override public void tick() {
        super.tick();setDeltaMovement(getDeltaMovement().multiply(1,.6000000238418579,1));
        if(level().isClientSide)DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->thaumcraft.world.taint.client.TaintEcologyClient.swarm(this));
    }
    @Override public boolean causeFallDamage(float distance,float multiplier,DamageSource source) { return false; }
    @Override public boolean isIgnoringBlockTriggers() { return true; }
    @Override public boolean isPushable() { return false; }
    @Override protected SoundEvent getAmbientSound() { return TaintEntitySounds.sound("swarm"); }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return TaintEntitySounds.sound("swarmattack"); }
    @Override protected SoundEvent getDeathSound() { return TaintEntitySounds.sound("swarmattack"); }
    @Override protected float getSoundVolume() { return .1F; }
    @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean recentlyHit) { if(random.nextBoolean())spawnAtLocation(AspectCrystalItem.create(Aspect.FLUX,1),getBbHeight()/2); }
    @Override public boolean checkSpawnRules(LevelAccessor level,MobSpawnType reason) {
        return level.getDifficulty()!=net.minecraft.world.Difficulty.PEACEFUL&&level.getMaxLocalRawBrightness(blockPosition())<=random.nextInt(7)&&super.checkSpawnRules(level,reason);
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag);tag.putBoolean("summoned",isSummoned());tag.putByte("damBonus",(byte)damBonus); }
    @Override public void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag);setSummoned(tag.getBoolean("summoned"));damBonus=tag.getByte("damBonus");flightTarget=null;attackTime=0; }
}
