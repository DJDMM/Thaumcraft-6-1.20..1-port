package thaumcraft.world.mobs;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;

/** Preserves BETA26's nonpersisted anger, delayed collision growth and initially2damage tick quirk. */
public final class GiantBrainyZombieEntity extends BrainyZombieEntity {
    private static final EntityDataAccessor<Float> ANGER=SynchedEntityData.defineId(GiantBrainyZombieEntity.class,EntityDataSerializers.FLOAT);
    private float bodyScale=1;
    public GiantBrainyZombieEntity(EntityType<? extends Zombie> type,Level level){super(type,level);xpReward=15;}
    public static AttributeSupplier.Builder attributes(){return BrainyZombieEntity.attributes().add(Attributes.MAX_HEALTH,60).add(Attributes.ATTACK_DAMAGE,7);}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(ANGER,0F);}
    @Override protected void registerGoals(){super.registerGoals();goalSelector.addGoal(2,new LeapAtTargetGoal(this,.4F));}
    public float anger(){return entityData.get(ANGER);}
    public void setAnger(float anger){if(Float.isFinite(anger))entityData.set(ANGER,Math.max(0,Math.min(2,anger)));}
    @Override public boolean hurt(DamageSource source,float amount){setAnger(Math.min(2,anger()+.1F));return super.hurt(source,amount);}
    @Override public void aiStep(){
        super.aiStep();
        if(anger()>1){setAnger(anger()-.002F);bodyScale=1+anger();refreshDimensions();}
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(7+(anger()-1)*5);
    }
    @Override public EntityDimensions getDimensions(Pose pose){return super.getDimensions(pose).scale(bodyScale==0?1:bodyScale);}
    @Override protected float getStandingEyeHeight(Pose pose,EntityDimensions dimensions){return 1.74F+anger()*1.74F-(isBaby()?.81F:0);}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);setAnger(0);bodyScale=1;refreshDimensions();}
    // Original does not override Zombie's active loot table. Its legacy dropFewItems double loop is dormant.
}
