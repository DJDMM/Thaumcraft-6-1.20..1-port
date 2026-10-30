package thaumcraft.world.crimson;

import java.util.EnumSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import thaumcraft.catalog.entities.VisualEntitiesModule;
import thaumcraft.catalog.entities.VisualMobEntity;

public final class CultistClericEntity extends CultistEntity implements RangedAttackMob {
    private static final EntityDataAccessor<Boolean> RITUALIST=SynchedEntityData.defineId(CultistClericEntity.class,EntityDataSerializers.BOOLEAN);
    private int rage;
    public CultistClericEntity(EntityType<? extends VisualMobEntity> type,Level level){super(type,level);}
    public static AttributeSupplier.Builder attributes(){return cultistAttributes(24);}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(RITUALIST,false);}
    public boolean isRitualist(){return entityData.get(RITUALIST);}
    public void setRitualist(boolean value){entityData.set(RITUALIST,value);}
    @Override protected void registerGoals(){
        ordinaryGoals();
        goalSelector.addGoal(1,new Goal(){
            {setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK,Flag.JUMP));}
            @Override public boolean canUse(){return isRitualist()&&cultHome()!=null;}
            @Override public boolean canContinueToUse(){return canUse();}
            @Override public void tick(){
                if(cultHome()!=null&&tickCount%40==0&&
                        (cultHome().distToCenterSqr(position())>16||!ForgeRegistries.BLOCKS.getKey(level().getBlockState(cultHome()).getBlock()).equals(id("eldritch"))))setRitualist(false);
            }
        });
        goalSelector.addGoal(2,new RangedAttackGoal(this,1,20,40,24){
            @Override public boolean canUse(){return super.canUse()&&getTarget()!=null&&distanceToSqr(getTarget().getX(),getTarget().getBoundingBox().minY,getTarget().getZ())>=4;}
        });
    }
    @Override protected void equip(DifficultyInstance difficulty){
        setItemSlot(EquipmentSlot.HEAD,item("crimson_robe_helm"));setItemSlot(EquipmentSlot.CHEST,item("crimson_robe_chest"));setItemSlot(EquipmentSlot.LEGS,item("crimson_robe_legs"));
        setItemSlot(EquipmentSlot.FEET,random.nextFloat()<(level().getDifficulty()==Difficulty.HARD?.3F:.1F)?item("crimson_boots"):net.minecraft.world.item.ItemStack.EMPTY);
    }
    @Override public void performRangedAttack(LivingEntity target,float distanceFactor){
        if(level().isClientSide||target==null||!target.isAlive()||isRemoved())return;
        swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        if(random.nextFloat()>.66F){
            var orb=VisualEntitiesModule.GOLEM_ORB.get().create(level());if(orb==null)return;
            orb.setOwner(this);orb.setTarget(target);orb.setRed(true);
            orb.setPos(getX(),getY()+getEyeHeight(),getZ());
            Vec3 direction=target.position().add(target.getDeltaMovement().scale(10)).subtract(position()).normalize();
            orb.setPos(orb.getX()+direction.x,orb.getY()+direction.y,orb.getZ()+direction.z);
            orb.shoot(direction.x,direction.y,direction.z,.66F,3);
            var sound=ForgeRegistries.SOUND_EVENTS.getValue(id("egattack"));if(sound!=null)playSound(sound,1,1+random.nextFloat()*.1F);
            level().addFreshEntity(orb);
        }else{
            double dx=target.getX()-getX(),dy=target.getBoundingBox().minY+target.getBbHeight()/2F-(getY()+getBbHeight()/2F),dz=target.getZ()-getZ();
            float spread=(float)Math.sqrt(distanceFactor)*.5F;
            level().levelEvent(null,1009,blockPosition(),0);
            for(int i=0;i<3;i++){
                var fireball=new SmallFireball(level(),this,dx+random.nextGaussian()*spread,dy,dz+random.nextGaussian()*spread);
                fireball.setPos(fireball.getX(),getY()+getBbHeight()/2F+.5,fireball.getZ());level().addFreshEntity(fireball);
            }
        }
    }
    @Override public boolean hurt(DamageSource source,float amount){
        if(isInvulnerableTo(source))return false;setRitualist(false);return super.hurt(source,amount);
    }
    @Override public boolean removeWhenFarAway(double distance){return !isRitualist();}
    @Override public void tick(){
        super.tick();
        if(!level().isClientSide&&isRitualist()&&rage>=5)setRitualist(false);
        if(level().isClientSide&&isRitualist()&&cultHome()!=null)getLookControl().setLookAt(cultHome().getX()+.5,cultHome().getY()+1.5,cultHome().getZ()+.5,10,getMaxHeadXRot());
    }
    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound(){return ForgeRegistries.SOUND_EVENTS.getValue(id("chant"));}
    @Override public int getAmbientSoundInterval(){return 500;}
    @Override public void handleEntityEvent(byte event){
        if(event==19){for(int i=0;i<3;i++)level().addParticle(net.minecraft.core.particles.ParticleTypes.ANGRY_VILLAGER,
                getX()+random.nextFloat()*getBbWidth()*2-getBbWidth(),getY()+.5+random.nextFloat()*getBbHeight(),getZ()+random.nextFloat()*getBbWidth()*2-getBbWidth(),random.nextGaussian()*.02,random.nextGaussian()*.02,random.nextGaussian()*.02);}
        else super.handleEntityEvent(event);
    }
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putBoolean("ritualist",isRitualist());}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);setRitualist(tag.getBoolean("ritualist"));rage=0;}
}
