package thaumcraft.world.eldritch;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.Vec3;
import thaumcraft.catalog.entities.VisualMobEntity;
import thaumcraft.catalog.blocks.CatalogBlocks;
import thaumcraft.world.warp.WarpModule;

/** BETA26 Warden: separate field shield, once-only field frenzy and native orb/sonic attacks. */
public final class EldritchWardenEntity extends ThaumcraftBossEntity implements RangedAttackMob,thaumcraft.api.entities.IEldritchMob {
    private static final EntityDataAccessor<Byte> TITLE=SynchedEntityData.defineId(EldritchWardenEntity.class,EntityDataSerializers.BYTE);
    private static final String[] TITLES={"Aphoom-Zhah","Basatan","Chaugnar Faugn","Mnomquah","Nyogtha","Oorn","Shaikorth","Rhan-Tegoth","Rhogog","Shudde M'ell","Vulthoom","Yag-Kosha","Yibb-Tstll","Zathog","Zushakon"};
    private final ServerBossEvent fieldInfo=new ServerBossEvent(net.minecraft.network.chat.Component.empty(),BossEvent.BossBarColor.BLUE,BossEvent.BossBarOverlay.NOTCHED_10);
    private boolean fieldFrenzy,lastBlast;
    private int fieldFrenzyCounter;
    public float armLiftL,armLiftR;
    public EldritchWardenEntity(EntityType<? extends VisualMobEntity> type,Level level){super(type,level);}
    public static AttributeSupplier.Builder attributes(){return bossAttributes(400,10,.33).add(Attributes.ARMOR,4);}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(TITLE,(byte)0);}
    @Override protected void registerGoals(){
        goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(2,new BossRangedGoal(this,3,20,40,24));goalSelector.addGoal(3,new MeleeAttackGoal(this,1.1,false));goalSelector.addGoal(5,new MoveTowardsRestrictionGoal(this,.8));goalSelector.addGoal(7,new RandomStrollGoal(this,1));goalSelector.addGoal(8,new LookAtPlayerGoal(this,Player.class,8));goalSelector.addGoal(8,new RandomLookAroundGoal(this));targetSelector.addGoal(1,new HurtByTargetGoal(this));targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));targetSelector.addGoal(3,new NearestAttackableTargetGoal<>(this,thaumcraft.world.crimson.CultistEntity.class,true));
    }
    @Override public void startSeenByPlayer(ServerPlayer p){super.startSeenByPlayer(p);fieldInfo.addPlayer(p);}
    @Override public void stopSeenByPlayer(ServerPlayer p){super.stopSeenByPlayer(p);fieldInfo.removePlayer(p);}
    @Override public boolean isEffectiveAi(){return fieldFrenzyCounter==0&&super.isEffectiveAi();}
    public int fieldFrenzyCounter(){return fieldFrenzyCounter;}
    public boolean hasUsedFieldFrenzy(){return fieldFrenzy;}
    @Override public boolean isInvulnerableTo(DamageSource source){return fieldFrenzyCounter>0||super.isInvulnerableTo(source);}
    @Override public boolean hurt(DamageSource source,float amount){if(isInvulnerableTo(source)||source.is(DamageTypes.DROWN)||source.is(DamageTypes.WITHER))return false;boolean hit=super.hurt(source,amount);if(!level().isClientSide&&hit&&!fieldFrenzy&&getAbsorptionAmount()<=0){fieldFrenzy=true;fieldFrenzyCounter=150;}return hit;}
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData data,CompoundTag tag){spawnTimer=150;entityData.set(TITLE,(byte)random.nextInt(TITLES.length));setAbsorptionAmount(getAbsorptionAmount()+(float)(getAttributeBaseValue(Attributes.MAX_HEALTH)*.66));return super.finalizeSpawn(level,difficulty,reason,data,tag);}
    @Override public void tick(){if(spawnTimer==150&&!level().isClientSide)level().broadcastEntityEvent(this,(byte)18);super.tick();if(level().isClientSide){armLiftL=Math.max(0,armLiftL-.05F);armLiftR=Math.max(0,armLiftR-.05F);}else if(isAlive()){
        int max=(int)(getAttributeBaseValue(Attributes.MAX_HEALTH)*.66);if(invulnerableTime<=0&&tickCount%25==0&&getAbsorptionAmount()<max)setAbsorptionAmount(getAbsorptionAmount()+1);fieldInfo.setProgress(Math.min(1,getAbsorptionAmount()/max));
        for(int l=0;l<4;l++){BlockPos p=BlockPos.containing(getX()+(l%2*2-1)*.25,getY(),getZ()+(l/2%2*2-1)*.25);if(level().isEmptyBlock(p))level().setBlock(p,CatalogBlocks.block("effect_sap").defaultBlockState(),3);}
        if(fieldFrenzyCounter>0){if(fieldFrenzyCounter==150)teleportHome();performFieldFrenzy();}
    }}
    private void performFieldFrenzy(){
        if(fieldFrenzyCounter<121&&fieldFrenzyCounter%10==0){level().broadcastEntityEvent(this,(byte)17);double radius=(150-fieldFrenzyCounter)/8.;int d=1+fieldFrenzyCounter/8;BlockPos center=blockPosition();for(int q=0;q<180/d;q++){double angle=Math.toRadians(q*2*d);BlockPos p=center.offset((int)(radius*Math.cos(angle)),0,(int)(radius*Math.sin(angle)));if(level().hasChunkAt(p)&&level().isEmptyBlock(p)&&level().getBlockState(p.below()).isRedstoneConductor(level(),p.below())){var sap=CatalogBlocks.block("effect_sap");level().setBlock(p,sap.defaultBlockState(),3);level().scheduleTick(p,sap,250+random.nextInt(150));if(random.nextFloat()<.3F)EldritchModule.blockArc(this,p,.5F+random.nextFloat()*.2F,0,.5F+random.nextFloat()*.2F);}}playSound(EldritchModule.sound("zap"),1,.9F+random.nextFloat()*.1F);}fieldFrenzyCounter--;
    }
    protected void teleportHome(){
        var home=getRestrictCenter();var event=new net.minecraftforge.event.entity.EntityTeleportEvent.EnderEntity(this,home.getX(),home.getY(),home.getZ());if(net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event))return;
        Vec3 old=position();setPos(event.getTargetX(),event.getTargetY(),event.getTargetZ());int x=net.minecraft.util.Mth.floor(getX()),y=net.minecraft.util.Mth.floor(getY()),z=net.minecraft.util.Mth.floor(getZ());BlockPos probe=new BlockPos(x,y,z);boolean success=false;
        if(level().hasChunkAt(probe)){boolean floor=false;int tries=20;while(!floor&&tries>0){if(level().getBlockState(probe.below()).blocksMotion()&&!level().getBlockState(probe).blocksMotion())floor=true;else{x=net.minecraft.util.Mth.floor(getX())+random.nextInt(8)-random.nextInt(8);z=net.minecraft.util.Mth.floor(getZ())+random.nextInt(8)-random.nextInt(8);tries--;}}if(floor){setPos(x+.5,y+.1,z+.5);success=level().noCollision(this);}}
        if(!success){setPos(old);return;}if(level() instanceof ServerLevel server)for(int i=0;i<128;i++){double t=i/127.;server.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL,old.x+(getX()-old.x)*t+(random.nextDouble()-.5)*getBbWidth()*2,old.y+(getY()-old.y)*t+random.nextDouble()*getBbHeight(),old.z+(getZ()-old.z)*t+(random.nextDouble()-.5)*getBbWidth()*2,0,(random.nextFloat()-.5)*.2,(random.nextFloat()-.5)*.2,(random.nextFloat()-.5)*.2,1);}playSound(SoundEvents.ENDERMAN_TELEPORT,1,1);
    }
    @Override protected float getStandingEyeHeight(Pose pose,EntityDimensions dimensions){return 3.1F;}
    @Override public void performRangedAttack(LivingEntity target,float distance){if(level().isClientSide||!target.isAlive())return;if(random.nextFloat()>.2F){var orb=WarpModule.ELDRITCH_ORB.get().create(level());if(orb==null)return;orb.setOwner(this);lastBlast=!lastBlast;level().broadcastEntityEvent(this,(byte)(lastBlast?16:15));float angle=(getYRot()+(lastBlast?90:180))%360*(float)Math.PI/180;orb.setPos(getX()-Math.cos(angle)*.5,getEyeY()-.23,getZ()-Math.sin(angle)*.5);orb.shoot(target.getX()+target.getDeltaMovement().x-getX(),target.getY()-getY()-target.getBbHeight()/2,target.getZ()+target.getDeltaMovement().z-getZ(),1,2);playSound(EldritchModule.sound("egattack"),2,1+random.nextFloat()*.1F);level().addFreshEntity(orb);}else if(hasLineOfSight(target)){thaumcraft.fx.FxDispatcher.emitWithin(level(),32,"eldritchSonic",position(),Vec3.ZERO,0xFFFFFF,1,10,getId(),net.minecraft.world.item.ItemStack.EMPTY);target.push(-Math.sin(getYRot()*Math.PI/180)*1.5,.1,Math.cos(getYRot()*Math.PI/180)*1.5);target.addEffect(new MobEffectInstance(MobEffects.WITHER,400));target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,400));if(target instanceof ServerPlayer player)thaumcraft.research.KnowledgeStore.addTemporaryWarp(player,3+random.nextInt(3));playSound(EldritchModule.sound("egscreech"),4,1+random.nextFloat()*.1F);}}
    @Override public boolean canAttackType(EntityType<?> type){return type!=thaumcraft.catalog.entities.VisualEntitiesModule.LIVING.get("eldritch_guardian").get()&&super.canAttackType(type);}
    @Override public void handleEntityEvent(byte event){if(event==15)armLiftL=.5F;else if(event==16)armLiftR=.5F;else if(event==17){armLiftL=.9F;armLiftR=.9F;}else if(event==18){spawnTimer=150;super.handleEntityEvent(event);}else super.handleEntityEvent(event);}
    @Override public void generateName(){int type=thaumcraft.world.taint.champion.Champions.type(this);if(type>=0)setCustomName(net.minecraft.network.chat.Component.translatable("entity.Thaumcraft.EldritchWarden.name.custom",TITLES[Math.floorMod(entityData.get(TITLE),TITLES.length)],EldritchModule.championName(type)));}
    @Override protected SoundEvent getAmbientSound(){return EldritchModule.sound("egidle");}
    @Override protected SoundEvent getDeathSound(){return EldritchModule.sound("egdeath");}
    @Override public int getAmbientSoundInterval(){return 500;}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putByte("title",entityData.get(TITLE));}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);entityData.set(TITLE,(byte)Math.floorMod(tag.getByte("title"),TITLES.length));fieldFrenzy=false;fieldFrenzyCounter=0;lastBlast=false;}
}
