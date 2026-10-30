package thaumcraft.world.taint.champion;

import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.*;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.*;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.*;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.registries.*;
import thaumcraft.research.KnowledgeStore;
import thaumcraft.world.rift.FluxTaintEffect;
import thaumcraft.world.taint.*;

/** Real synchronized/persistent attributes, original fourteen modifiers and event dispatch. */
public final class Champions {
 private static final DeferredRegister<Attribute> ATTRIBUTES=DeferredRegister.create(ForgeRegistries.ATTRIBUTES,"thaumcraft");
 public static final RegistryObject<Attribute> CHAMPION=ATTRIBUTES.register("champion_mod",()->new RangedAttribute("attribute.thaumcraft.champion_mod",-2,-2,100).setSyncable(true));
 public static final RegistryObject<Attribute> TAINTED=ATTRIBUTES.register("tainted_mod",()->new RangedAttribute("attribute.thaumcraft.tainted_mod",0,0,1).setSyncable(true));
 private static final String[] NAMES={"bold","spine","armor","mighty","grim","warded","warp","undying","fiery","sickly","venomous","vampiric","infested","tainted"};
 private static final String[] IDS={"40289aa1-907f-4ac6-ad79-e6681efe2cbc","365eead5-3f15-42a8-9e68-36100faef945","4e23758d-348e-42a8-8de6-08ae0a59033c","6d2ffe79-f034-4a06-b288-e1916c21e385","0f23321e-f921-4246-90b8-21ef202de224","b622c4d8-abc6-4db7-b3ee-5cf71b8e5286","107da049-af7a-4409-989a-6de23c8fe036","cb9484d3-6255-4893-a4f2-3ecc375692ee","6b567fdf-9245-48f5-8314-f93fe5db1427","b5718868-9ab0-424c-af1f-8b35e836b46e","ab9a132e-c619-4c0a-a103-10cbbcfba1a2","3412251e-af81-4c3c-93ba-2e1c33b049ea","9c577fbe-ddbc-4ea2-a661-770ea775f43b","a3bb2595-8221-4140-bc73-538abcd1bbd2"};
 private static final AttributeModifier NORMAL=modifier("1e645a3d-9115-4807-a61c-705172839f87","normal",1,AttributeModifier.Operation.ADDITION);
 private static final AttributeModifier MINUS_ONE=modifier("f48eb416-8321-46e6-8c07-a07bed729a0c","minus1",-1,AttributeModifier.Operation.ADDITION);
 private static final AttributeModifier HEALTH=modifier("a62bef38-48cc-42a6-ac5e-ef913841c4fd","Champion health buff",100,AttributeModifier.Operation.ADDITION);
 private static final AttributeModifier DAMAGE=modifier("a340d2db-d881-4c25-ac62-f0ad14cd63b0","Champion damage buff",2,AttributeModifier.Operation.MULTIPLY_TOTAL);
 private static final AttributeModifier BOLD=modifier("4b1edd33-caa9-47ae-a702-d86c05701037","Bold speed boost",.3,AttributeModifier.Operation.MULTIPLY_BASE);
 private static final AttributeModifier MIGHTY=modifier("7163897f-07f5-49b3-9ce4-b74beb83d2d3","Mighty damage boost",2,AttributeModifier.Operation.MULTIPLY_TOTAL);
 private static final AttributeModifier TAINT_HEALTH=modifier("0f354f6a-33c5-40be-93be-81b1338567f1","HEALTH BUFF 6",25,AttributeModifier.Operation.ADDITION);
 private static final AttributeModifier TAINT_DAMAGE=modifier("534f8c57-929a-48cf-bbd6-0fd851030748","DAMAGE BUFF 1",.5,AttributeModifier.Operation.ADDITION);
 private static final AttributeModifier TAINT_AI=modifier("2cb22137-a9d8-4417-ae06-de0e70f11b4c","istainted",1,AttributeModifier.Operation.ADDITION);
 private static final ForgeConfigSpec.Builder CONFIG_BUILDER=new ForgeConfigSpec.Builder();
 public static final ForgeConfigSpec.BooleanValue ALLOW_CHAMPIONS=CONFIG_BUILDER.define("allowChampionMobs",true);
 private static final ForgeConfigSpec CONFIG=CONFIG_BUILDER.build();
 private static final Map<Class<? extends Mob>,Integer> WHITELIST=new LinkedHashMap<>();
 private static final java.lang.reflect.Field GOALS=ObfuscationReflectionHelper.findField(Mob.class,"f_21345_"),TARGETS=ObfuscationReflectionHelper.findField(Mob.class,"f_21346_");
 static {WHITELIST.put(Zombie.class,0);WHITELIST.put(Spider.class,0);WHITELIST.put(Blaze.class,0);WHITELIST.put(EnderMan.class,0);WHITELIST.put(AbstractSkeleton.class,0);WHITELIST.put(Witch.class,1);}
 private Champions(){}
 private static AttributeModifier modifier(String id,String name,double value,AttributeModifier.Operation operation){return new AttributeModifier(UUID.fromString(id),name,value,operation);}
 public static void register(IEventBus bus){
  ATTRIBUTES.register(bus);bus.addListener(Champions::attributes);
  MinecraftForge.EVENT_BUS.addListener(Champions::join);MinecraftForge.EVENT_BUS.addListener(Champions::tick);
  MinecraftForge.EVENT_BUS.addListener(Champions::hurt);MinecraftForge.EVENT_BUS.addListener(Champions::drops);
  ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON,CONFIG,"thaumcraft-champions.toml");
 }
 public static void registerWhitelist(Class<? extends Mob> type,int level){WHITELIST.put(type,level);}
 private static void attributes(EntityAttributeModificationEvent event){for(var type:event.getTypes())if(type!=EntityType.PLAYER){event.add(type,CHAMPION.get(),-2);event.add(type,TAINTED.get(),0);if(!event.has(type,Attributes.ATTACK_DAMAGE))event.add(type,Attributes.ATTACK_DAMAGE,0);}}
 public static int type(LivingEntity entity){var a=entity.getAttribute(CHAMPION.get());return a==null?-2:(int)a.getValue();}
 private static String id(Entity e){var id=ForgeRegistries.ENTITY_TYPES.getKey(e.getType());return id==null?"":id.getPath();}
 private static boolean boss(Entity e){return Set.of("eldritch_warden","eldritch_golem","cultist_leader").contains(id(e));}
 private static boolean hostile(Entity e){return e instanceof Monster||e instanceof thaumcraft.auromancy.media.SpellBatEntity||e instanceof thaumcraft.catalog.entities.VisualMobEntity&&e.getClass()!=thaumcraft.catalog.entities.VisualMobEntity.class&&!Set.of("wisp","thaumic_slime","turret_basic","turret_advanced","arcane_bore").contains(id(e));}
 public static boolean isOriginalMonster(Entity entity){return hostile(entity);}
 private static void apply(AttributeInstance a,AttributeModifier m){if(a!=null){a.removeModifier(m.getId());a.addPermanentModifier(m);}}
 public static boolean makeChampion(Mob mob,boolean persistent){return makeChampion(mob,persistent,mob instanceof Creeper?0:mob.getRandom().nextInt(14));}
 public static boolean makeChampion(Mob mob,boolean persistent,int selected){
  var attribute=mob.getAttribute(CHAMPION.get());if(attribute==null||attribute.getValue()>-2||selected<0||selected>=14)return false;
  apply(attribute,modifier(IDS[selected],NAMES[selected],selected+2,AttributeModifier.Operation.ADDITION));
  if(!boss(mob)){apply(mob.getAttribute(Attributes.MAX_HEALTH),HEALTH);apply(mob.getAttribute(Attributes.ATTACK_DAMAGE),DAMAGE);mob.heal(25);mob.setCustomName(Component.translatable("champion.mod."+NAMES[selected]).append(" ").append(mob.getDisplayName()));}
  if(persistent)mob.setPersistenceRequired();
  if(selected==0)apply(mob.getAttribute(Attributes.MOVEMENT_SPEED),BOLD);
  if(selected==3)apply(mob.getAttribute(Attributes.ATTACK_DAMAGE),MIGHTY);
  if(selected==5)mob.setAbsorptionAmount(mob.getAbsorptionAmount()+(int)mob.getAttributeBaseValue(Attributes.MAX_HEALTH)/2);
  return true;
 }
 public static boolean makeTainted(LivingEntity target){
  if(target instanceof thaumcraft.artifice.construct.OwnedConstructEntity||!(target instanceof PathfinderMob||target instanceof thaumcraft.auromancy.media.SpellBatEntity)||Set.of("wisp","thaumic_slime").contains(id(target)))return false;
  var attr=target.getAttribute(CHAMPION.get());if(attr==null||attr.getValue()>-1)return false;
  if(attr.getValue()==-1)apply(attr,MINUS_ONE);apply(attr,modifier(IDS[13],NAMES[13],15,AttributeModifier.Operation.ADDITION));
  if(!boss(target)){apply(target.getAttribute(Attributes.MAX_HEALTH),TAINT_HEALTH);var damage=target.getAttribute(Attributes.ATTACK_DAMAGE);if(damage!=null){if(damage.getBaseValue()==0)damage.setBaseValue(Math.max(2,(target.getBbHeight()+target.getBbWidth())*2));else apply(damage,TAINT_DAMAGE);}target.heal(25);}
  return true;
 }
 private static void join(EntityJoinLevelEvent event){
  if(event.getLevel().isClientSide||!(event.getEntity() instanceof Mob mob))return;
  if(type(mob)==13){var attr=mob.getAttribute(TAINTED.get());if(attr!=null)attr.removeModifier(TAINT_AI.getId());}
  if(!hostile(mob)||type(mob)>=-1)return;
  int chance=event.getLevel().random.nextInt(100);boolean enabled=ALLOW_CHAMPIONS.get();
  if(event.getLevel().getDifficulty()==Difficulty.EASY||!enabled)chance+=2;
  if(event.getLevel().getDifficulty()==Difficulty.HARD&&enabled)chance-=2;
  var biome=event.getLevel().getBiome(mob.blockPosition());
  if(biome.is(thaumcraft.world.biome.BiomeModule.EERIE)||event.getLevel().dimension()==net.minecraft.world.level.Level.NETHER||event.getLevel().dimension()==net.minecraft.world.level.Level.END)chance-=enabled?2:1;
  boolean white=false;int bonus=0;for(var entry:WHITELIST.entrySet())if(entry.getKey().isInstance(mob)){white=true;if(enabled||boss(mob))bonus=Math.max(bonus,entry.getValue()-1);}
  String id=id(mob);if(Set.of("brainy_zombie","giant_brainy_zombie","mind_spider","eldritch_crab").contains(id))white=true;
  if(id.equals("taintacle")||id.equals("taintacle_giant")){white=true;if(enabled||boss(mob))bonus=Math.max(bonus,1);}
  if(id.equals("inhabited_zombie")){white=true;if(enabled||boss(mob))bonus=Math.max(bonus,2);}
  chance-=bonus;
  if(white&&chance<=0&&mob.getAttributeBaseValue(Attributes.MAX_HEALTH)>=10)makeChampion(mob,false);else apply(mob.getAttribute(CHAMPION.get()),NORMAL);
 }
 private static void tick(LivingEvent.LivingTickEvent event){
  LivingEntity living=event.getEntity();if(living.level().isClientSide||!(living instanceof Mob mob)||!mob.isAlive())return;
  int type=type(mob);if(type==5&&mob.invulnerableTime<=0&&mob.tickCount%25==0&&mob.getAbsorptionAmount()<(int)mob.getAttributeBaseValue(Attributes.MAX_HEALTH)/2)mob.setAbsorptionAmount(mob.getAbsorptionAmount()+1);
  if(type==7&&mob.tickCount%20==0)mob.heal(1);
  if(type==13&&mob instanceof PathfinderMob pathfinder&&!hostile(mob))resetTaintedAI(pathfinder);
 }
 private static void resetTaintedAI(PathfinderMob mob){
  var taint=mob.getAttribute(TAINTED.get());if(taint==null||taint.getValue()!=0)return;
  try{
   GoalSelector goals=(GoalSelector)GOALS.get(mob),targets=(GoalSelector)TARGETS.get(mob);goals.removeAllGoals(g->true);targets.removeAllGoals(g->true);
   goals.addGoal(0,new FloatGoal(mob));goals.addGoal(2,new MeleeAttackGoal(mob,1.2,false));goals.addGoal(5,new MoveTowardsRestrictionGoal(mob,1));
   goals.addGoal(6,new MoveThroughVillageGoal(mob,1,false,4,()->true));goals.addGoal(7,new RandomStrollGoal(mob,1));goals.addGoal(8,new LookAtPlayerGoal(mob,Player.class,8));goals.addGoal(8,new RandomLookAroundGoal(mob));
   targets.addGoal(1,new HurtByTargetGoal(mob).setAlertOthers(ZombifiedPiglin.class));targets.addGoal(2,new NearestAttackableTargetGoal<>(mob,Player.class,true));apply(taint,TAINT_AI);
  }catch(IllegalAccessException failure){throw new IllegalStateException("Cannot restore original tainted mob goals",failure);}
 }
 private static void hurt(LivingHurtEvent event){
  LivingEntity victim=event.getEntity();if(victim.level().isClientSide)return;
  if(victim.getHealth()<2&&!victim.isInvertedHealAndHarm()&&!FluxTaintEffect.isNativeTainted(victim)&&!(victim instanceof thaumcraft.artifice.construct.OwnedConstructEntity)&&victim.hasEffect(thaumcraft.infusion.InfusionEffects.FLUX_TAINT.get())&&victim.getRandom().nextBoolean()){makeTainted(victim);return;}
  LivingEntity attacker=event.getSource().getEntity() instanceof LivingEntity l?l:null;
  if(hostile(victim)&&type(victim)>=0&&attacker!=null){
   int type=type(victim);
   if((type==5||id(victim).startsWith("eldritch"))&&victim.getAbsorptionAmount()>0){
    int direction=event.getSource().is(DamageTypes.FALL)?-2:event.getSource().is(DamageTypes.FALLING_BLOCK)?-3:attacker.getId();
    thaumcraft.fx.FxDispatcher.emitWithin(victim.level(),32,"shield",victim.position(),net.minecraft.world.phys.Vec3.ZERO,0xFFFFFF,1,direction,victim.getId(),ItemStack.EMPTY);
   }else if(type==1&&!event.getSource().is(DamageTypes.THORNS)){attacker.hurt(victim.damageSources().thorns(victim),1+victim.getRandom().nextInt(3));attacker.playSound(SoundEvents.THORNS_HIT,.5F,1);}
   else if(type==2&&!event.getSource().is(DamageTypeTags.BYPASSES_ARMOR))event.setAmount(event.getAmount()*19/25);
   else if(type==12&&victim.getRandom().nextFloat()<.4F&&victim.level() instanceof ServerLevel level){var crawler=TaintModule.activeMob("taint_crawler",level);if(crawler!=null){crawler.moveTo(victim.getX(),victim.getY()+victim.getBbHeight()/2,victim.getZ(),victim.getRandom().nextFloat()*360,0);level.addFreshEntity(crawler);victim.playSound(TaintModule.GORE.get(),.5F,1);}}
  }
  if(event.getAmount()<=0||attacker==null||!hostile(attacker))return;int type=type(attacker);
  if(type==11){attacker.heal(Math.max(2,event.getAmount()/2));return;}
  if(type!=4&&type!=6&&type!=8&&type!=9&&type!=10)return;
  float roll=attacker.getRandom().nextFloat();
  if(type==4&&roll<.4F)victim.addEffect(new MobEffectInstance(MobEffects.WITHER,200));
  else if(type==6&&roll<.33F&&victim instanceof ServerPlayer player)KnowledgeStore.addTemporaryWarp(player,1+attacker.getRandom().nextInt(3));
  else if(type==8&&roll<.4F)victim.setSecondsOnFire(4);
  else if(type==9&&roll<.4F)victim.addEffect(new MobEffectInstance(MobEffects.HUNGER,500));
  else if(type==10&&roll<.4F)victim.addEffect(new MobEffectInstance(MobEffects.POISON,100));
 }
 private static void drops(LivingDropsEvent event){
  LivingEntity mob=event.getEntity();if(mob.level().isClientSide||!event.isRecentlyHit()||!(event.getSource().getEntity() instanceof ServerPlayer player)||player instanceof net.minecraftforge.common.util.FakePlayer||!hostile(mob)||boss(mob)||type(mob)<0||type(mob)==13)return;
  if(mob.level() instanceof ServerLevel level)ExperienceOrb.award(level,mob.position(),5+mob.getRandom().nextInt(3));
  int tier=Math.min(2,(mob.getRandom().nextInt(9)+event.getLootingLevel())/5);String name="loot_bag_"+new String[]{"common","uncommon","rare"}[tier];
  var item=ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("thaumcraft",name));
  if(item!=null)event.getDrops().add(new net.minecraft.world.entity.item.ItemEntity(mob.level(),mob.getX(),mob.getEyeY(),mob.getZ(),new ItemStack(item)));
 }
}
