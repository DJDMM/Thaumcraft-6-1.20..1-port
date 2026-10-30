package thaumcraft.world.eldritch;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.sounds.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
/** Original immediate death-to-crab replacement, with full native Zombie inheritance. */
public final class InhabitedZombieEntity extends Zombie implements thaumcraft.api.entities.IEldritchMob {
 private boolean emerged;
 private static final java.lang.reflect.Field RECENTLY_HIT=net.minecraftforge.fml.util.ObfuscationReflectionHelper.findField(LivingEntity.class,"f_20889_");
 public InhabitedZombieEntity(EntityType<? extends Zombie> type,Level level){super(type,level);}
 public static AttributeSupplier.Builder attributes(){return Zombie.createAttributes().add(Attributes.MAX_HEALTH,30).add(Attributes.ATTACK_DAMAGE,5).add(Attributes.SPAWN_REINFORCEMENTS_CHANCE,0);}
 @Override protected void registerGoals(){super.registerGoals();targetSelector.addGoal(1,new HurtByTargetGoal(this).setAlertOthers());targetSelector.addGoal(3,new NearestAttackableTargetGoal<>(this,thaumcraft.world.crimson.CultistEntity.class,true));}
 @Override protected void randomizeReinforcementsChance(){getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE).setBaseValue(0);}
 @Override public boolean killedEntity(ServerLevel level,LivingEntity victim){return false;}
 @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData data,CompoundTag tag){float chance=level.getDifficulty()==Difficulty.HARD?.9F:.6F;equip(EquipmentSlot.HEAD,"crimson_plate_helm");if(random.nextFloat()<=chance)equip(EquipmentSlot.CHEST,"crimson_plate_chest");if(random.nextFloat()<=chance)equip(EquipmentSlot.LEGS,"crimson_plate_legs");return super.finalizeSpawn(level,difficulty,reason,data,tag);}
 private void equip(EquipmentSlot slot,String name){var item=ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("thaumcraft",name));if(item!=null)setItemSlot(slot,new ItemStack(item));}
 @Override public void die(DamageSource source){}
 @Override protected void tickDeath(){
  if(emerged||isRemoved())return;emerged=true;
  if(level() instanceof ServerLevel server){
   var crab=thaumcraft.world.taint.TaintModule.activeMob("eldritch_crab",server);if(crab instanceof EldritchCrabEntity child){child.moveTo(getX(),getEyeY(),getZ(),getYRot(),getXRot());child.setHelm(true);server.addFreshEntity(child);}
   try{if(RECENTLY_HIT.getInt(this)>0&&shouldDropLoot()&&server.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT))ExperienceOrb.award(server,position(),getExperienceReward());}catch(IllegalAccessException error){throw new IllegalStateException("Cannot preserve inhabited Zombie XP credit",error);}
   level().broadcastEntityEvent(this,(byte)62);
  }else thaumcraft.world.eldritch.client.EldritchClient.inhabitedDeath(this);
  remove(RemovalReason.KILLED);
 }
 @Override public void handleEntityEvent(byte event){if(event==62)net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->thaumcraft.world.eldritch.client.EldritchClient.inhabitedDeath(this));else super.handleEntityEvent(event);}
 @Override protected SoundEvent getAmbientSound(){return EldritchModule.sound("crabtalk");}
 @Override protected SoundEvent getHurtSound(DamageSource source){return SoundEvents.GENERIC_HURT;}
 @Override public boolean checkSpawnRules(LevelAccessor level,MobSpawnType reason){return level instanceof Level world&&world.getEntitiesOfClass(InhabitedZombieEntity.class,new net.minecraft.world.phys.AABB(getX(),getY(),getZ(),getX()+1,getY()+1,getZ()+1).inflate(32,16,32),other->other!=this).isEmpty()&&super.checkSpawnRules(level,reason);}
 @Override public void readAdditionalSaveData(CompoundTag tag){CompoundTag nativeTag=tag.copy();if(tag.getBoolean("VisualOnly")){nativeTag.putBoolean("NoAI",false);nativeTag.putBoolean("PersistenceRequired",false);}super.readAdditionalSaveData(nativeTag);emerged=false;}
 @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.remove("VisualOnly");}
}
