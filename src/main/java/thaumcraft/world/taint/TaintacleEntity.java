package thaumcraft.world.taint;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import thaumcraft.alchemy.AspectCrystalItem;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.catalog.entities.VisualMobEntity;
import thaumcraft.world.biome.BiomeModule;

/** BETA26 ordinary stationary tentacle. Tiny remains its separately ported transient variant. */
public class TaintacleEntity extends TaintedStationaryMob {
    public float flailIntensity=1;
    private boolean nativePersistence;
    public TaintacleEntity(EntityType<? extends VisualMobEntity> type,Level level) { super(type,level);nativePersistence=false;xpReward=8; }
    public static AttributeSupplier.Builder attributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH,50).add(Attributes.ATTACK_DAMAGE,7).add(Attributes.MOVEMENT_SPEED,0).add(Attributes.FOLLOW_RANGE,16);
    }
    @Override protected void registerGoals() {
        super.registerGoals();goalSelector.addGoal(2,new LookAtPlayerGoal(this,Player.class,6));goalSelector.addGoal(3,new RandomLookAroundGoal(this));
    }
    @Override public void tick() {
        super.tick();
        if(level().isClientSide) { if(flailIntensity>1)flailIntensity-=.01F;if(tickCount<getBbHeight()*10&&onGround())net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->thaumcraft.world.taint.client.TaintEcologyClient.tentacle(this));return; }
        if(!(level() instanceof ServerLevel server)||!isAlive()||isRemoved()||!loaded(server,getBoundingBox()))return;
        if(tickCount%20==0) {
            if(!onTaint())actuallyHurt(damageSources().starve(),1);
            LivingEntity target=getTarget();
            if(tickCount%40==0&&target!=null&&target.isAlive()&&distanceToSqr(target)>16&&distanceToSqr(target)<256&&getSensing().hasLineOfSight(target))spawnTentacles(server,target);
        }
    }
    private boolean onTaint() { return TaintHelper.isTainted(level().getBlockState(blockPosition()))||TaintHelper.isTainted(level().getBlockState(blockPosition().below())); }
    protected void spawnTentacles(ServerLevel level,LivingEntity target) {
        BlockPos pos=target.blockPosition();if(!TaintModule.loaded(level,pos)||!TaintModule.loaded(level,pos.below()))return;
        boolean eldritch=level.getBiome(pos).is(BiomeModule.ELDRITCH);
        if(!eldritch&&!TaintHelper.isTainted(level.getBlockState(pos))&&!TaintHelper.isTainted(level.getBlockState(pos.below())))return;
        var tiny=TaintModule.activeMob("taintacle_tiny",level);if(tiny==null)return;
        tiny.moveTo(target.getX()+random.nextFloat()-random.nextFloat(),target.getY(),target.getZ()+random.nextFloat()-random.nextFloat(),0,0);
        if(level.addFreshEntity(tiny))playSound(tentacleSound(),getSoundVolume(),getVoicePitch());
        if(eldritch&&level.isEmptyBlock(pos)&&TaintEcologySupport.adjacentToSolid(level,pos))level.setBlockAndUpdate(pos,TaintModule.block("taint_fibre").defaultBlockState());
    }
    @Override public boolean doHurtTarget(Entity target) {
        level().broadcastEntityEvent(this,(byte)16);playSound(tentacleSound(),getSoundVolume(),getVoicePitch());return super.doHurtTarget(target);
    }
    @Override public void handleEntityEvent(byte event) { if(event==16)flailIntensity=3;else super.handleEntityEvent(event); }
    @Override public boolean checkSpawnRules(LevelAccessor level,MobSpawnType reason) { return level.getDifficulty()!=Difficulty.PEACEFUL&&onTaint(); }
    @Override protected void dropCustomDeathLoot(net.minecraft.world.damagesource.DamageSource source,int looting,boolean recentlyHit) { spawnAtLocation(AspectCrystalItem.create(Aspect.FLUX,1),getBbHeight()/2); }
    @Override public boolean removeWhenFarAway(double distance) { return true; }
    @Override public boolean isPersistenceRequired() { return nativePersistence; }
    @Override public void setPersistenceRequired() { super.setPersistenceRequired();nativePersistence=true; }
    @Override public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) { super.addAdditionalSaveData(tag);tag.putBoolean("PersistenceRequired",nativePersistence); }
    @Override public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);setNoAi(!tag.getBoolean("VisualOnly")&&tag.getBoolean("NoAI"));nativePersistence=!tag.getBoolean("VisualOnly")&&tag.getBoolean("PersistenceRequired");flailIntensity=1;
    }
}
