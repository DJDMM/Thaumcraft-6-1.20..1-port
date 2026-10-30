package thaumcraft.world.warp;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.sounds.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.entity.ai.navigation.*;
import thaumcraft.catalog.entities.VisualMobEntity;

/** Original private hallucinations and real one-health spiders, sharing the original mesh. */
public final class MindSpiderEntity extends VisualMobEntity implements Enemy,thaumcraft.api.entities.IEldritchMob {
    private static final EntityDataAccessor<Boolean> HARMLESS=SynchedEntityData.defineId(MindSpiderEntity.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> VIEWER=SynchedEntityData.defineId(MindSpiderEntity.class,EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Byte> CLIMB=SynchedEntityData.defineId(MindSpiderEntity.class,EntityDataSerializers.BYTE);
    private int lifeSpan=Integer.MAX_VALUE;
    private boolean nativePersistence;
    public MindSpiderEntity(EntityType<? extends VisualMobEntity> type,Level level) {
        super(type,level);setNoAi(false);nativePersistence=false;xpReward=1;
        setPathfindingMalus(BlockPathTypes.DANGER_FIRE,-1);
    }
    public static AttributeSupplier.Builder attributes() {
        return net.minecraft.world.entity.monster.Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,1)
            .add(Attributes.ATTACK_DAMAGE,1).add(Attributes.MOVEMENT_SPEED,.3).add(Attributes.FOLLOW_RANGE,16);
    }
    @Override protected void defineSynchedData() {super.defineSynchedData();entityData.define(HARMLESS,false);entityData.define(VIEWER,"");entityData.define(CLIMB,(byte)0);}
    @Override protected PathNavigation createNavigation(Level level) {return new WallClimberNavigation(this,level);}
    @Override protected void registerGoals() {
        goalSelector.addGoal(1,new FloatGoal(this));goalSelector.addGoal(3,new LeapAtTargetGoal(this,.4F));
        goalSelector.addGoal(4,new MeleeAttackGoal(this,1,false));goalSelector.addGoal(5,new WaterAvoidingRandomStrollGoal(this,.8));
        goalSelector.addGoal(6,new LookAtPlayerGoal(this,Player.class,8));goalSelector.addGoal(6,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this));targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));
    }
    public boolean harmless(){return entityData.get(HARMLESS);}
    @Override public boolean isPersistenceRequired(){return nativePersistence;}
    @Override public void setPersistenceRequired(){nativePersistence=true;}
    public String viewer(){return entityData.get(VIEWER);}
    public void setViewer(String name){entityData.set(VIEWER,name);}
    public void setHarmless(boolean value){entityData.set(HARMLESS,value);if(value)lifeSpan=1200;xpReward=value?0:1;}
    @Override public boolean doHurtTarget(Entity entity){return !harmless()&&super.doHurtTarget(entity);}
    @Override public boolean onClimbable(){return (entityData.get(CLIMB)&1)!=0;}
    @Override public MobType getMobType(){return MobType.ARTHROPOD;}
    @Override public boolean isIgnoringBlockTriggers(){return true;}
    @Override protected boolean shouldDespawnInPeaceful(){return true;}
    @Override public boolean removeWhenFarAway(double distance){return true;}
    @Override protected MovementEmission getMovementEmission(){return MovementEmission.NONE;}
    @Override protected SoundEvent getAmbientSound(){return SoundEvents.SPIDER_AMBIENT;}
    @Override protected SoundEvent getHurtSound(DamageSource source){return SoundEvents.SPIDER_HURT;}
    @Override protected SoundEvent getDeathSound(){return SoundEvents.SPIDER_DEATH;}
    @Override public float getVoicePitch(){return .7F;}
    @Override public void tick(){super.tick();if(!level().isClientSide){entityData.set(CLIMB,(byte)(horizontalCollision?1:0));if(tickCount>lifeSpan)discard();}}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.remove("VisualOnly");tag.putBoolean("PersistenceRequired",nativePersistence);tag.putBoolean("harmless",harmless());tag.putString("viewer",viewer());}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);setNoAi(!tag.getBoolean("VisualOnly")&&tag.getBoolean("NoAI"));nativePersistence=!tag.getBoolean("VisualOnly")&&tag.getBoolean("PersistenceRequired");setHarmless(tag.getBoolean("harmless"));setViewer(tag.getString("viewer"));}
}
