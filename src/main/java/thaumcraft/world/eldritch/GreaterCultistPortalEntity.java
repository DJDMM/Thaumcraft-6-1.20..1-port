package thaumcraft.world.eldritch;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import thaumcraft.catalog.blocks.CatalogBlocks;
import thaumcraft.catalog.entities.*;
import thaumcraft.world.crimson.CultistEntity;

/** Original thirteen-stage encounter. BETA26 registers it without a natural entry route. */
public final class GreaterCultistPortalEntity extends VisualMobEntity implements Enemy {
    private final ServerBossEvent bossInfo;
    private int stage,stageCounter=200,pulse;
    private boolean exploded;
    public GreaterCultistPortalEntity(EntityType<? extends VisualMobEntity> type,Level level){super(type,level);setNoAi(false);setNoGravity(true);xpReward=30;bossInfo=new ServerBossEvent(getDisplayName(),BossEvent.BossBarColor.RED,BossEvent.BossBarOverlay.NOTCHED_6);bossInfo.setDarkenScreen(true);}
    public static AttributeSupplier.Builder attributes(){return createMobAttributes().add(Attributes.MAX_HEALTH,500).add(Attributes.ATTACK_DAMAGE,0).add(Attributes.KNOCKBACK_RESISTANCE,1).add(Attributes.MOVEMENT_SPEED,0).add(Attributes.ARMOR,5);}
    public int stage(){return stage;}
    public int stageCounter(){return stageCounter;}
    @Override public int presentationPulse(){return pulse;}
    @Override public void move(MoverType type,Vec3 motion){}
    @Override public void aiStep(){}
    @Override public boolean isPushable(){return false;}
    @Override public boolean canBeCollidedWith(){return true;}
    @Override public boolean isAffectedByPotions(){return false;}
    @Override public boolean addEffect(MobEffectInstance effect,Entity source){return false;}
    @Override public boolean causeFallDamage(float distance,float factor,DamageSource source){return false;}
    @Override public boolean hurt(DamageSource source,float amount){return !source.is(DamageTypeTags.IS_FIRE)&&super.hurt(source,amount);}
    @Override public boolean removeWhenFarAway(double distance){return false;}
    @Override protected boolean shouldDespawnInPeaceful(){return true;}
    @Override public void startSeenByPlayer(ServerPlayer player){super.startSeenByPlayer(player);bossInfo.addPlayer(player);}
    @Override public void stopSeenByPlayer(ServerPlayer player){super.stopSeenByPlayer(player);bossInfo.removePlayer(player);}
    @Override public boolean shouldRenderAtSqrDistance(double distance){return distance<4096;}
    @Override public void tick(){
        super.tick();if(!level().isClientSide&&isAlive()){
            bossInfo.setProgress(getHealth()/getMaxHealth());
            if(stageCounter>0){stageCounter--;
                if(stageCounter==160&&stage==0)raiseBanners();
                if(stageCounter>20&&stageCounter<150&&stage==0&&stageCounter%13==0)raiseCrate();
            }else if(level().getNearestPlayer(this,48)!=null){level().broadcastEntityEvent(this,(byte)16);switch(stage){case 0,1,2,3,4->{stageCounter=15+random.nextInt(10-stage)-stage;spawnMinion();}case 12->{stageCounter=50+timing()*2+random.nextInt(50);spawnBoss();}default->{int t=timing();stageCounter=t+random.nextInt(5+t/3);spawnMinion();}}stage++;}
            else stageCounter=30+random.nextInt(30);
            if(stage<12)heal(1);
        }if(pulse>0)pulse--;
    }
    private void raiseBanners(){level().broadcastEntityEvent(this,(byte)16);for(Direction direction:Direction.Plane.HORIZONTAL){BlockPos p=new BlockPos((int)getX()-direction.getStepX()*6,(int)getY(),(int)getZ()+direction.getStepZ()*6);if(!level().hasChunkAt(p))continue;BlockState state=CatalogBlocks.block("banner_crimson_cult").defaultBlockState();var rotation=state.getBlock().getStateDefinition().getProperty("rotation");int facing=switch(direction){case NORTH->8;case WEST->12;case EAST->4;default->0;};if(rotation!=null)state=set(state,rotation,Integer.toString(facing));level().setBlock(p,state,3);EldritchModule.blockArc(this,p,.5F+random.nextFloat()*.2F,0,0);sound("wandfail",1,1);}}
    private static <T extends Comparable<T>> BlockState set(BlockState state,net.minecraft.world.level.block.state.properties.Property<T> property,String value){return property.getValue(value).map(v->state.setValue(property,v)).orElse(state);}
    private void raiseCrate(){int x=(int)getX()+random.nextInt(5)-random.nextInt(5),z=(int)getZ()+random.nextInt(5)-random.nextInt(5);BlockPos p=new BlockPos(x,(int)getY(),z);if(x==(int)getX()||z==(int)getZ()||!level().hasChunkAt(p)||!level().isEmptyBlock(p))return;level().broadcastEntityEvent(this,(byte)16);float roll=level().random.nextFloat();level().setBlock(p,CatalogBlocks.block(roll<.05F?"loot_crate_rare":roll<.2F?"loot_crate_uncommon":"loot_crate_common").defaultBlockState(),3);EldritchModule.blockArc(this,p,.5F+random.nextFloat()*.2F,0,0);sound("wandfail",1,1);}
    public int timing(){return level().getEntitiesOfClass(CultistEntity.class,new net.minecraft.world.phys.AABB(position(),position()).inflate(32)).size()*20;}
    public CultistEntity spawnMinion(){if(!(level() instanceof ServerLevel server)||!isAlive())return null;String id=random.nextFloat()>.33F?"cultist_knight":"cultist_cleric";var mob=VisualEntitiesModule.LIVING.get(id).get().create(level());if(!(mob instanceof CultistEntity cultist))return null;cultist.setPos(getX()+random.nextFloat()-random.nextFloat(),getY()+.25,getZ()+random.nextFloat()-random.nextFloat());cultist.finalizeSpawn(server,server.getCurrentDifficultyAt(cultist.blockPosition()),MobSpawnType.MOB_SUMMONED,null,null);cultist.setCultHome(blockPosition(),32);if(!server.addFreshEntity(cultist)||cultist.isRemoved()){cultist.discard();return null;}cultist.spawnCultistParticles();cultist.sound("wandfail",1,1);if(stage>12)hurt(damageSources().fellOutOfWorld(),5+random.nextInt(5));return cultist;}
    public CultistLeaderEntity spawnBoss(){if(!(level() instanceof ServerLevel server)||!isAlive())return null;var mob=VisualEntitiesModule.LIVING.get("cultist_leader").get().create(level());if(!(mob instanceof CultistLeaderEntity leader))return null;leader.setPos(getX()+random.nextFloat()-random.nextFloat(),getY()+.25,getZ()+random.nextFloat()-random.nextFloat());leader.finalizeSpawn(server,server.getCurrentDifficultyAt(leader.blockPosition()),MobSpawnType.MOB_SUMMONED,null,null);leader.restrictTo(blockPosition(),32);if(!server.addFreshEntity(leader)||leader.isRemoved()){leader.discard();return null;}leader.spawnCultistParticles();sound("wandfail",1,1);return leader;}
    @Override public void playerTouch(Player player){if(!level().isClientSide&&isAlive()&&distanceToSqr(player)<3&&player.hurt(damageSources().indirectMagic(this,this),8))sound("zap",1,(random.nextFloat()-random.nextFloat())*.1F+1);}
    @Override public void die(DamageSource source){if(!level().isClientSide&&!dead&&!exploded){exploded=true;level().explode(this,getX(),getY(),getZ(),2,Level.ExplosionInteraction.NONE);}super.die(source);}
    @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean hit){EldritchModule.specialDrop(this,thaumcraft.catalog.CatalogModule.stack("primordial_pearl"),getBbHeight()/2);}
    @Override protected float getSoundVolume(){return .75F;}
    @Override public int getAmbientSoundInterval(){return 540;}
    @Override protected SoundEvent getAmbientSound(){return EldritchModule.sound("monolith");}
    @Override protected SoundEvent getHurtSound(DamageSource source){return EldritchModule.sound("zap");}
    @Override protected SoundEvent getDeathSound(){return EldritchModule.sound("shock");}
    private void sound(String id,float volume,float pitch){playSound(EldritchModule.sound(id),volume,pitch);}
    @Override public void handleEntityEvent(byte event){if(event==16)pulse=10;else super.handleEntityEvent(event);}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.remove("VisualOnly");tag.putInt("stage",stage);}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);setNoAi(false);setNoGravity(true);stage=Math.max(0,tag.getInt("stage"));stageCounter=200;pulse=0;exploded=false;}
}
