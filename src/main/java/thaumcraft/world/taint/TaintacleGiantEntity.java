package thaumcraft.world.taint;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.catalog.entities.VisualMobEntity;
import thaumcraft.world.taint.champion.Champions;

/** Dormant BETA26 boss remains summonable, without inventing an Outer Lands generator. */
public final class TaintacleGiantEntity extends TaintacleEntity implements thaumcraft.api.entities.IEldritchMob {
    private static final EntityDataAccessor<Integer> ANGER=SynchedEntityData.defineId(TaintacleGiantEntity.class,EntityDataSerializers.INT);
    private final ServerBossEvent boss=new ServerBossEvent(getDisplayName(),BossEvent.BossBarColor.PURPLE,BossEvent.BossBarOverlay.PROGRESS);
    public TaintacleGiantEntity(EntityType<? extends VisualMobEntity> type,Level level) { super(type,level);boss.setDarkenScreen(true);xpReward=20;setPersistenceRequired(); }
    public static AttributeSupplier.Builder attributes() { return TaintacleEntity.attributes().add(Attributes.MAX_HEALTH,175).add(Attributes.ATTACK_DAMAGE,9); }
    @Override protected void defineSynchedData() { super.defineSynchedData();entityData.define(ANGER,0); }
    public int anger() { return entityData.get(ANGER); }
    public void setAnger(int ticks) { entityData.set(ANGER,Math.max(0,ticks)); }
    @Override public void tick() {
        super.tick();if(anger()>0)setAnger(anger()-1);
        if(level().isClientSide&&random.nextInt(15)==0&&anger()>0)level().addParticle(net.minecraft.core.particles.ParticleTypes.ANGRY_VILLAGER,getX()+random.nextFloat()*getBbWidth()-getBbWidth()/2,getBoundingBox().minY+getBbHeight()+random.nextFloat()*.5,getZ()+random.nextFloat()*getBbWidth()-getBbWidth()/2,random.nextGaussian()*.02,random.nextGaussian()*.02,random.nextGaussian()*.02);
        if(!level().isClientSide&&isAlive()&&tickCount%30==0)heal(1);
        boss.setProgress(getHealth()/getMaxHealth());
    }
    @Override public boolean hurt(DamageSource source,float amount) {
        if(!level().isClientSide&&amount>35) {
            if(anger()==0) {
                addEffect(new MobEffectInstance(MobEffects.REGENERATION,200,(int)(amount/15)));
                addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,200,(int)(amount/10)));
                addEffect(new MobEffectInstance(MobEffects.DIG_SPEED,200,(int)(amount/40)));setAnger(200);
                if(source.getEntity() instanceof net.minecraft.world.entity.player.Player player)player.displayClientMessage(net.minecraft.network.chat.Component.literal(getName().getString()+" ").append(net.minecraft.network.chat.Component.translatable("tc.boss.enrage")),true);
            }
            amount=35;
        }
        return super.hurt(source,amount);
    }
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData data,CompoundTag tag) {
        Champions.makeChampion(this,true);return data;
    }
    @Override public boolean checkSpawnRules(LevelAccessor level,MobSpawnType reason) { return false; }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override public boolean canBreatheUnderwater() { return true; }
    @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean recentlyHit) {
        if(level().getEntitiesOfClass(TaintacleGiantEntity.class,getBoundingBox().inflate(48),other->other!=this&&!other.isRemoved()).isEmpty()) {
            var pearl=ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("thaumcraft","primordial_pearl"));
            if(pearl!=null)thaumcraft.world.eldritch.EldritchModule.specialDrop(this,new ItemStack(pearl),getBbHeight()/2);
        }
    }
    @Override public void startSeenByPlayer(ServerPlayer player) { super.startSeenByPlayer(player);boss.addPlayer(player); }
    @Override public void stopSeenByPlayer(ServerPlayer player) { super.stopSeenByPlayer(player);boss.removePlayer(player); }
}
