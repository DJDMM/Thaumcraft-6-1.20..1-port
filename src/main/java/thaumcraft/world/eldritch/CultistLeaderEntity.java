package thaumcraft.world.eldritch;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.Vec3;
import thaumcraft.catalog.entities.*;
import thaumcraft.world.crimson.CultistEntity;

/** Original praetor, red seeking orb and regeneration support for nearby cultists. */
public final class CultistLeaderEntity extends ThaumcraftBossEntity implements RangedAttackMob {
    private static final EntityDataAccessor<Byte> TITLE=SynchedEntityData.defineId(CultistLeaderEntity.class,EntityDataSerializers.BYTE);
    private static final String[] TITLES={"Alberic","Anselm","Bastian","Beturian","Chabier","Chorache","Chuse","Dodorol","Ebardo","Ferrando","Fertus","Guillen","Larpe","Obano","Zelipe"};
    public CultistLeaderEntity(EntityType<? extends VisualMobEntity> type,Level level){super(type,level);xpReward=40;}
    public static AttributeSupplier.Builder attributes(){return bossAttributes(150,5,.32);}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(TITLE,(byte)0);}
    @Override protected void registerGoals(){goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(2,new BossRangedGoal(this,16,30,40,24));goalSelector.addGoal(3,new MeleeAttackGoal(this,1.1,false));goalSelector.addGoal(6,new MoveTowardsRestrictionGoal(this,.8));goalSelector.addGoal(7,new RandomStrollGoal(this,.8));goalSelector.addGoal(8,new LookAtPlayerGoal(this,Player.class,8));goalSelector.addGoal(8,new RandomLookAroundGoal(this));targetSelector.addGoal(1,new HurtByTargetGoal(this){@Override public void start(){super.start();LivingEntity attacker=getLastHurtByMob();if(attacker!=null)for(CultistEntity ally:level().getEntitiesOfClass(CultistEntity.class,getBoundingBox().inflate(getAttributeValue(Attributes.FOLLOW_RANGE),10,getAttributeValue(Attributes.FOLLOW_RANGE))))if(ally.getTarget()==null&&!ally.isAlliedTo(attacker))ally.setTarget(attacker);}});targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));}
    @Override public boolean isAlliedTo(Entity e){return e instanceof CultistEntity||e instanceof CultistLeaderEntity;}
    @Override public boolean canAttackType(EntityType<?> type){var id=net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(type);return id!=null&&!java.util.Set.of("cultist_cleric","cultist_leader","cultist_knight").contains(id.getPath())&&super.canAttackType(type);}
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData data,CompoundTag tag){
        equip(EquipmentSlot.HEAD,"crimson_praetor_helm");equip(EquipmentSlot.CHEST,"crimson_praetor_chest");equip(EquipmentSlot.LEGS,"crimson_praetor_legs");equip(EquipmentSlot.FEET,"crimson_boots");equip(EquipmentSlot.MAINHAND,level.getDifficulty()==Difficulty.EASY?"void_sword":"crimson_blade");
        float special=difficulty.getSpecialMultiplier();if(!getMainHandItem().isEmpty()&&random.nextFloat()<.5F*special)setItemSlot(EquipmentSlot.MAINHAND,net.minecraft.world.item.enchantment.EnchantmentHelper.enchantItem(random,getMainHandItem(),(int)(7+special*random.nextInt(22)),false));entityData.set(TITLE,(byte)random.nextInt(TITLES.length));return super.finalizeSpawn(level,difficulty,reason,data,tag);
    }
    private void equip(EquipmentSlot slot,String path){var item=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumcraft",path));if(item!=null)setItemSlot(slot,new ItemStack(item));}
    @Override public void tick(){super.tick();if(!level().isClientSide&&isAlive()&&isEffectiveAi())for(CultistEntity ally:level().getEntitiesOfClass(CultistEntity.class,new net.minecraft.world.phys.AABB(position(),position()).inflate(8)))if(!ally.hasEffect(MobEffects.REGENERATION))ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION,60,1));}
    @Override public void performRangedAttack(LivingEntity target,float distance){if(level().isClientSide||!hasLineOfSight(target))return;swing(InteractionHand.MAIN_HAND);getLookControl().setLookAt(target.getX(),target.getY()+target.getBbHeight()/2,target.getZ(),30,30);var orb=VisualEntitiesModule.GOLEM_ORB.get().create(level());if(orb==null)return;orb.setOwner(this);orb.setTarget(target);orb.setRed(true);orb.setPos(getX(),getEyeY()-.1,getZ());orb.shoot(target.getX()-getX(),target.getY()+target.getBbHeight()/2-(getY()+getBbHeight()/2)+2,target.getZ()-getZ(),.66F,3);playSound(EldritchModule.sound("egattack"),1,1+random.nextFloat()*.1F);level().addFreshEntity(orb);}
    public void spawnCultistParticles(){if(!level().isClientSide){level().broadcastEntityEvent(this,(byte)20);return;}EldritchModule.cultistParticles(this);}
    @Override public void handleEntityEvent(byte event){if(event==20)spawnCultistParticles();else super.handleEntityEvent(event);}
    @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean hit){drop("loot_bag_rare",1.5F);for(EquipmentSlot slot:EquipmentSlot.values()){ItemStack stack=getItemBySlot(slot);float chance=getEquipmentDropChance(slot);boolean guaranteed=chance>1;if(stack.isEmpty()||net.minecraft.world.item.enchantment.EnchantmentHelper.hasVanishingCurse(stack)||!hit&&!guaranteed||Math.max(random.nextFloat()-looting*.01F,0)>=chance)continue;if(!guaranteed&&stack.isDamageableItem())stack.setDamageValue(stack.getMaxDamage()-random.nextInt(1+random.nextInt(Math.max(stack.getMaxDamage()-3,1))));spawnAtLocation(stack);setItemSlot(slot,ItemStack.EMPTY);}}
    @Override public void generateName(){int type=thaumcraft.world.taint.champion.Champions.type(this);if(type>=0)setCustomName(net.minecraft.network.chat.Component.translatable("entity.Thaumcraft.CultistLeader.name.custom",TITLES[Math.floorMod(entityData.get(TITLE),TITLES.length)],EldritchModule.championName(type)));}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putByte("title",entityData.get(TITLE));}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);entityData.set(TITLE,(byte)Math.floorMod(tag.getByte("title"),TITLES.length));}
}
