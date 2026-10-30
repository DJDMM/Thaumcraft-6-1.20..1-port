package thaumcraft.world.eldritch;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.Vec3;
import thaumcraft.catalog.entities.VisualMobEntity;

/** Released boss lifecycle, including the original aggro-loop indexing quirks. */
public abstract class ThaumcraftBossEntity extends VisualMobEntity implements Enemy {
    private static final EntityDataAccessor<Integer> ANGER=SynchedEntityData.defineId(ThaumcraftBossEntity.class,EntityDataSerializers.INT);
    private static final String[] HP_IDS={"54d621c1-dd4d-4b43-8bd2-5531c8875797","f51257dc-b7fa-4f7a-92d7-75d68e8592c4","3d6b2e42-4141-4364-b76d-0e8664bbd0bb","02c97a08-801c-4131-afa2-1427a6151934","0f354f6a-33c5-40be-93be-81b1338567f1"};
    private static final String[] DMG_IDS={"534f8c57-929a-48cf-bbd6-0fd851030748","d317a76e-0e7c-4c61-acfd-9fa286053b32","ff462d63-26a2-4363-830e-143ed97e2a4f","cf1eb39e-0c67-495f-887c-0d3080828d2f","3cfab9da-2701-43d8-ac07-885f16fa4117"};
    protected final ServerBossEvent bossInfo;
    private final Map<Integer,Integer> aggro=new HashMap<>();
    protected int spawnTimer;
    protected ThaumcraftBossEntity(EntityType<? extends VisualMobEntity> type,Level level){
        super(type,level);setNoAi(false);setNoGravity(false);xpReward=50;
        bossInfo=new ServerBossEvent(getDisplayName(),BossEvent.BossBarColor.PURPLE,BossEvent.BossBarOverlay.PROGRESS);bossInfo.setDarkenScreen(true);
        for(EquipmentSlot slot:EquipmentSlot.values())setItemSlot(slot,net.minecraft.world.item.ItemStack.EMPTY);
    }
    protected static AttributeSupplier.Builder bossAttributes(double health,double damage,double speed){return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,health).add(Attributes.ATTACK_DAMAGE,damage).add(Attributes.MOVEMENT_SPEED,speed).add(Attributes.FOLLOW_RANGE,40).add(Attributes.KNOCKBACK_RESISTANCE,.95);}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(ANGER,0);}
    public int anger(){return entityData.get(ANGER);}
    public void setAnger(int value){entityData.set(ANGER,value);}
    public int spawnTimer(){return spawnTimer;}
    @Override public int presentationSpawnTimer(){return spawnTimer;}
    @Override public boolean isEffectiveAi(){return spawnTimer==0&&super.isEffectiveAi();}
    @Override public void startSeenByPlayer(ServerPlayer player){super.startSeenByPlayer(player);bossInfo.addPlayer(player);}
    @Override public void stopSeenByPlayer(ServerPlayer player){super.stopSeenByPlayer(player);bossInfo.removePlayer(player);}
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData data,CompoundTag tag){restrictTo(blockPosition(),24);generateName();bossInfo.setName(getDisplayName());return data;}
    public void generateName(){}
    @Override public void tick(){
        super.tick();if(spawnTimer>0)spawnTimer--;if(anger()>0)setAnger(anger()-1);
        if(level().isClientSide){if(random.nextInt(15)==0&&anger()>0)level().addParticle(ParticleTypes.ANGRY_VILLAGER,getX()+random.nextFloat()*getBbWidth()-getBbWidth()/2,getBoundingBox().minY+getBbHeight()+random.nextFloat()*.5,getZ()+random.nextFloat()*getBbWidth()-getBbWidth()/2,random.nextGaussian()*.02,random.nextGaussian()*.02,random.nextGaussian()*.02);}
        else{if(getTarget()!=null&&!getTarget().isAlive())setTarget(null);bossInfo.setProgress(getHealth()/getMaxHealth());if(tickCount%30==0)heal(1);if(getTarget()!=null&&tickCount%20==0)updateAggro();}
    }
    private void updateAggro(){
        int players=0,hei=getTarget().getId(),ad=aggro.getOrDefault(hei,0),ld=ad;Entity selected=null;List<Integer> stale=new ArrayList<>();
        for(var entry:aggro.entrySet())if(entry.getValue()>ad+25&&entry.getValue()>ad*1.1&&entry.getValue()>ld){
            // BETA26 reads the previous hei rather than entry.getKey(), and stores ei into ld.
            selected=level().getEntity(hei);if(selected==null||!selected.isAlive()||distanceToSqr(selected)>16384)stale.add(entry.getKey());
            else{hei=entry.getKey();ld=entry.getKey();if(selected instanceof net.minecraft.world.entity.player.Player)players++;}
        }
        stale.forEach(aggro::remove);if(selected instanceof LivingEntity living&&hei!=getTarget().getId())setTarget(living);
        float old=getMaxHealth();var health=getAttribute(Attributes.MAX_HEALTH);var damage=getAttribute(Attributes.ATTACK_DAMAGE);
        for(int i=0;i<5;i++){health.removeModifier(UUID.fromString(HP_IDS[i]));damage.removeModifier(UUID.fromString(DMG_IDS[i]));}
        for(int i=0;i<Math.min(5,players-1);i++){health.addPermanentModifier(new AttributeModifier(UUID.fromString(HP_IDS[i]),"HEALTH BUFF "+(i+1),50,AttributeModifier.Operation.ADDITION));damage.addPermanentModifier(new AttributeModifier(UUID.fromString(DMG_IDS[i]),"DAMAGE BUFF "+(i+1),.5,AttributeModifier.Operation.ADDITION));}
        setHealth(getHealth()*getMaxHealth()/old);
    }
    @Override public boolean isInvulnerableTo(DamageSource source){return spawnTimer>0||super.isInvulnerableTo(source);}
    @Override public boolean isPushable(){return super.isPushable()&&!isInvulnerableTo(damageSources().starve());}
    @Override public boolean canBreatheUnderwater(){return true;}
    @Override protected int decreaseAirSupply(int air){return air;}
    @Override public void makeStuckInBlock(net.minecraft.world.level.block.state.BlockState state,Vec3 multiplier){}
    @Override public boolean removeWhenFarAway(double distance){return false;}
    @Override protected boolean shouldDespawnInPeaceful(){return true;}
    @Override public boolean isAlliedTo(Entity entity){return entity instanceof thaumcraft.api.entities.IEldritchMob;}
    @Override public boolean hurt(DamageSource source,float amount){
        if(!level().isClientSide){if(source.getEntity() instanceof LivingEntity living)aggro.merge(living.getId(),(int)amount,Integer::sum);
            if(amount>35){if(anger()==0){addEffect(new MobEffectInstance(MobEffects.REGENERATION,200,(int)(amount/15)));addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,200,(int)(amount/10)));addEffect(new MobEffectInstance(MobEffects.DIG_SPEED,200,(int)(amount/40)));setAnger(200);if(source.getEntity() instanceof ServerPlayer player)player.displayClientMessage(getDisplayName().copy().append(" ").append(Component.translatable("tc.boss.enrage")),true);}amount=35;}}
        return super.hurt(source,amount);
    }
    protected final void drop(String id,float height){var item=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumcraft",id));if(item!=null)spawnAtLocation(new net.minecraft.world.item.ItemStack(item),height);}
    @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean hit){EldritchModule.specialDrop(this,thaumcraft.catalog.CatalogModule.stack("primordial_pearl"),getBbHeight()/2);drop("loot_bag_rare",1.5F);}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.remove("VisualOnly");if(hasRestriction()){tag.putInt("HomeD",(int)getRestrictRadius());tag.putInt("HomeX",getRestrictCenter().getX());tag.putInt("HomeY",getRestrictCenter().getY());tag.putInt("HomeZ",getRestrictCenter().getZ());}}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);setNoAi(!tag.getBoolean("VisualOnly")&&tag.getBoolean("NoAI"));setNoGravity(false);if(tag.contains("HomeD"))restrictTo(new BlockPos(tag.getInt("HomeX"),tag.getInt("HomeY"),tag.getInt("HomeZ")),Math.max(0,tag.getInt("HomeD")));spawnTimer=0;aggro.clear();setAnger(0);bossInfo.setName(getDisplayName());}
}
