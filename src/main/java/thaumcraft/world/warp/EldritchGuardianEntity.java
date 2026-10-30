package thaumcraft.world.warp;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import thaumcraft.catalog.entities.VisualMobEntity;
import thaumcraft.research.KnowledgeStore;

public final class EldritchGuardianEntity extends VisualMobEntity implements Enemy,RangedAttackMob,thaumcraft.api.entities.IEldritchMob {
    public float armLiftL,armLiftR;
    private boolean lastBlast;
    private boolean nativePersistence;
    public EldritchGuardianEntity(EntityType<? extends VisualMobEntity> type,Level level) {
        super(type,level);setNoAi(false);nativePersistence=false;xpReward=20;
        if(getNavigation() instanceof GroundPathNavigation navigation)navigation.setCanOpenDoors(true);
    }
    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,50).add(Attributes.FOLLOW_RANGE,40)
            .add(Attributes.MOVEMENT_SPEED,.28).add(Attributes.ATTACK_DAMAGE,7).add(Attributes.ARMOR,4);
    }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(2,new LongRangeGoal(this));
        goalSelector.addGoal(3,new MeleeAttackGoal(this,1,false));goalSelector.addGoal(5,new MoveTowardsRestrictionGoal(this,.8));
        goalSelector.addGoal(7,new WaterAvoidingRandomStrollGoal(this,1));goalSelector.addGoal(8,new LookAtPlayerGoal(this,Player.class,8));
        goalSelector.addGoal(8,new RandomLookAroundGoal(this));targetSelector.addGoal(1,new HurtByTargetGoal(this));
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));
        targetSelector.addGoal(3,new NearestAttackableTargetGoal<>(this,VisualMobEntity.class,true,
            e->ForgeRegistries.ENTITY_TYPES.getKey(e.getType()).getPath().startsWith("cultist_")));
    }
    @Override public MobType getMobType(){return MobType.UNDEAD;}
    @Override public boolean isPersistenceRequired(){return nativePersistence;}
    @Override public void setPersistenceRequired(){nativePersistence=true;}
    @Override public boolean hurt(DamageSource source,float amount){return super.hurt(source,source.is(DamageTypeTags.WITCH_RESISTANT_TO)?amount/2:amount);}
    @Override public boolean isAlliedTo(Entity entity){return entity instanceof thaumcraft.api.entities.IEldritchMob;}
    @Override protected boolean shouldDespawnInPeaceful(){return true;}
    @Override protected SoundEvent getAmbientSound(){return sound("egidle");}
    @Override protected SoundEvent getDeathSound(){return sound("egdeath");}
    @Override public int getAmbientSoundInterval(){return 500;}
    @Override protected float getSoundVolume(){return 1.5F;}
    private static SoundEvent sound(String name){return ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.fromNamespaceAndPath("thaumcraft",name));}
    @Override public void tick(){
        super.tick();
        if(level().isClientSide){armLiftL=Math.max(0,armLiftL-.05F);armLiftR=Math.max(0,armLiftR-.05F);}
        else if(tickCount%100==0&&level().getDifficulty()!=Difficulty.EASY){
            double range=level().getDifficulty()==Difficulty.HARD?576:256;
            for(var player:((ServerLevel)level()).players())if(player.isAlive()&&distanceToSqr(player)<range)WarpNetwork.send(player,2);
        }
    }
    @Override public void handleEntityEvent(byte event){
        if(event==15)armLiftL=.5F;else if(event==16)armLiftR=.5F;
        else if(event==17){armLiftL=.9F;armLiftR=.9F;}else super.handleEntityEvent(event);
    }
    @Override public void performRangedAttack(LivingEntity target,float distance){
        if(level().isClientSide||!target.isAlive())return;
        if(random.nextFloat()>.15F){
            var orb=WarpModule.ELDRITCH_ORB.get().create(level());if(orb==null)return;
            orb.setOwner(this);lastBlast=!lastBlast;level().broadcastEntityEvent(this,(byte)(lastBlast?16:15));
            float angle=(getYRot()+(lastBlast?90:180))%360*(float)Math.PI/180;
            orb.setPos(getX()-Math.cos(angle)*.5,getEyeY()-.1,getZ()-Math.sin(angle)*.5);
            Vec3 direction=target.position().add(target.getDeltaMovement().scale(10)).subtract(position()).normalize();
            orb.shoot(direction.x,direction.y,direction.z,1.1F,2);
            playSound(sound("egattack"),2,1+random.nextFloat()*.1F);level().addFreshEntity(orb);
        }else if(hasLineOfSight(target)){
            thaumcraft.fx.FxDispatcher.emitWithin(level(),32,"eldritchSonic",position(),Vec3.ZERO,0xFFFFFF,1,10,getId(),net.minecraft.world.item.ItemStack.EMPTY);
            target.addEffect(new MobEffectInstance(MobEffects.WITHER,400));
            if(target instanceof net.minecraft.server.level.ServerPlayer player)KnowledgeStore.addTemporaryWarp(player,1+random.nextInt(3));
            playSound(sound("egscreech"),3,1+random.nextFloat()*.1F);
        }
    }
    @Override public boolean removeWhenFarAway(double distance){return !hasRestriction();}
    @Override public void addAdditionalSaveData(CompoundTag tag){
        super.addAdditionalSaveData(tag);tag.remove("VisualOnly");tag.putBoolean("PersistenceRequired",nativePersistence);
        if(hasRestriction()){tag.putInt("HomeD",(int)getRestrictRadius());tag.putInt("HomeX",getRestrictCenter().getX());tag.putInt("HomeY",getRestrictCenter().getY());tag.putInt("HomeZ",getRestrictCenter().getZ());}
    }
    @Override public void readAdditionalSaveData(CompoundTag tag){
        super.readAdditionalSaveData(tag);setNoAi(!tag.getBoolean("VisualOnly")&&tag.getBoolean("NoAI"));nativePersistence=!tag.getBoolean("VisualOnly")&&tag.getBoolean("PersistenceRequired");
        if(tag.contains("HomeD"))restrictTo(new BlockPos(tag.getInt("HomeX"),tag.getInt("HomeY"),tag.getInt("HomeZ")),Math.max(0,tag.getInt("HomeD")));
    }
    /** Released long-range AI uses a minimum8 and20..40 ticks rather than a close-range turret. */
    private static final class LongRangeGoal extends RangedAttackGoal {
        private final EldritchGuardianEntity guardian;
        LongRangeGoal(EldritchGuardianEntity guardian){super(guardian,1,20,40,24);this.guardian=guardian;}
        @Override public boolean canUse(){return guardian.getTarget()!=null&&guardian.distanceToSqr(guardian.getTarget())>=64&&super.canUse();}
        @Override public boolean canContinueToUse(){return canUse();}
    }
}
