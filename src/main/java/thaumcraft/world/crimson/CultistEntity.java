package thaumcraft.world.crimson;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import thaumcraft.catalog.entities.VisualMobEntity;

/** Released cultist team/home/loot contracts at the existing renderer-compatible IDs. */
public abstract class CultistEntity extends VisualMobEntity implements Enemy {
    private static final net.minecraft.network.syncher.EntityDataAccessor<java.util.Optional<BlockPos>> CULT_HOME =
            net.minecraft.network.syncher.SynchedEntityData.defineId(CultistEntity.class,net.minecraft.network.syncher.EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private boolean nativePersistence;
    private BlockPos savedHome;
    private int savedHomeRadius;

    protected CultistEntity(EntityType<? extends VisualMobEntity> type, Level level) {
        super(type,level);
        setNoAi(false);
        setNoGravity(false);
        nativePersistence=false;
        xpReward=10;
        setCanPickUpLoot(false);
        for(EquipmentSlot slot:EquipmentSlot.values())setItemSlot(slot,ItemStack.EMPTY);
        for(EquipmentSlot slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET})
            setDropChance(slot,.05F);
        if(getNavigation() instanceof GroundPathNavigation ground){ground.setCanOpenDoors(true);ground.setCanPassDoors(true);}
    }
    protected static AttributeSupplier.Builder cultistAttributes(double health) {
        return createMobAttributes().add(Attributes.MAX_HEALTH,health).add(Attributes.FOLLOW_RANGE,32)
                .add(Attributes.MOVEMENT_SPEED,.3).add(Attributes.ATTACK_DAMAGE,4);
    }
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(CULT_HOME,java.util.Optional.empty());}
    protected final void ordinaryGoals() {
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(3,new MeleeAttackGoal(this,1,false));
        goalSelector.addGoal(5,new OpenDoorGoal(this,true));
        goalSelector.addGoal(6,new MoveTowardsRestrictionGoal(this,.8));
        goalSelector.addGoal(7,new RandomStrollGoal(this,.8));
        goalSelector.addGoal(8,new LookAtPlayerGoal(this,Player.class,8));
        goalSelector.addGoal(8,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this){
            @Override public void start(){
                super.start();
                LivingEntity attacker=CultistEntity.this.getLastHurtByMob();
                if(attacker==null)return;
                double range=getAttributeValue(Attributes.FOLLOW_RANGE);
                AABB bounds=new AABB(getX(),getY(),getZ(),getX()+1,getY()+1,getZ()+1).inflate(range,10,range);
                for(CultistEntity ally:level().getEntitiesOfClass(CultistEntity.class,bounds))
                    if(ally!=CultistEntity.this&&ally.getTarget()==null&&!ally.isAlliedTo(attacker))ally.setTarget(attacker);
            }
        });
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));
        targetSelector.addGoal(3,new NearestAttackableTargetGoal<>(this,LivingEntity.class,10,true,false,
                target->ForgeRegistries.ENTITY_TYPES.getKey(target.getType()).equals(id("eldritch_guardian"))));
        targetSelector.addGoal(4,new NearestAttackableTargetGoal<>(this,AbstractIllager.class,true));
    }
    protected static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath("thaumcraft",path);}
    protected final ItemStack item(String path){var item=ForgeRegistries.ITEMS.getValue(id(path));return item==null?ItemStack.EMPTY:new ItemStack(item);}
    public final void sound(String name,float volume,float pitch){var sound=ForgeRegistries.SOUND_EVENTS.getValue(id(name));if(sound!=null)playSound(sound,volume,pitch);}
    protected abstract void equip(DifficultyInstance difficulty);
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,DifficultyInstance difficulty,MobSpawnType reason,
                                                 SpawnGroupData group,CompoundTag data){
        equip(difficulty);
        enchant(difficulty);
        return super.finalizeSpawn(level,difficulty,reason,group,data);
    }
    protected void enchant(DifficultyInstance difficulty) {}
    @Override public boolean isAlliedTo(Entity other){
        return other instanceof CultistEntity||ForgeRegistries.ENTITY_TYPES.getKey(other.getType()).equals(id("cultist_leader"))||super.isAlliedTo(other);
    }
    @Override public boolean canAttackType(EntityType<?> type){
        var key=ForgeRegistries.ENTITY_TYPES.getKey(type);
        return !java.util.Set.of(id("cultist_knight"),id("cultist_cleric"),id("cultist_leader")).contains(key)&&super.canAttackType(type);
    }
    @Override protected ResourceLocation getDefaultLootTable(){return id("entities/cultist");}
    @Override protected void dropCustomDeathLoot(net.minecraft.world.damagesource.DamageSource source,int looting,boolean hit){
        // The visual base deliberately suppresses Mob's equipment loop. Restore it for native cultists.
        for(EquipmentSlot slot:EquipmentSlot.values()){
            ItemStack stack=getItemBySlot(slot);float chance=getEquipmentDropChance(slot);boolean guaranteed=chance>1;
            if(stack.isEmpty()||net.minecraft.world.item.enchantment.EnchantmentHelper.hasVanishingCurse(stack)
                    ||!hit&&!guaranteed||Math.max(random.nextFloat()-looting*.01F,0)>=chance)continue;
            if(!guaranteed&&stack.isDamageableItem())
                stack.setDamageValue(stack.getMaxDamage()-random.nextInt(1+random.nextInt(Math.max(stack.getMaxDamage()-3,1))));
            spawnAtLocation(stack);setItemSlot(slot,ItemStack.EMPTY);
        }
    }
    @Override public boolean removeWhenFarAway(double distance){return true;}
    @Override protected boolean shouldDespawnInPeaceful(){return true;}
    @Override public boolean isPersistenceRequired(){return nativePersistence;}
    @Override public void setPersistenceRequired(){super.setPersistenceRequired();nativePersistence=true;}
    public void setCultHome(BlockPos home,int radius){savedHome=home.immutable();savedHomeRadius=Math.max(0,Math.min(4096,radius));restrictTo(home,savedHomeRadius);entityData.set(CULT_HOME,java.util.Optional.of(savedHome));}
    public BlockPos cultHome(){return entityData.get(CULT_HOME).orElse(null);}
    public void spawnCultistParticles(){
        if(!level().isClientSide){level().broadcastEntityEvent(this,(byte)20);return;}
        for(int i=0;i<20;i++){
            Vec3 velocity=new Vec3(random.nextGaussian()*.05,random.nextGaussian()*.05,random.nextGaussian()*.05);
            Vec3 point=position().add(random.nextFloat()*getBbWidth()*2-getBbWidth()+velocity.x*2,
                    random.nextFloat()*getBbHeight()+velocity.y*2,random.nextFloat()*getBbWidth()*2-getBbWidth()+velocity.z*2);
            CompoundTag spec=new CompoundTag();spec.putInt("life",10+random.nextInt(10));spec.putInt("grid",16);
            spec.putInt("start",160);spec.putInt("count",6);spec.putInt("inc",1);spec.putInt("layer",1);
            keys(spec,"rgb",1,1,1);keys(spec,"rgbEnd",.6F,0,0);keys(spec,"alpha",.8F);keys(spec,"scale",3+random.nextFloat()*2);
            thaumcraft.fx.FxDispatcher.generic(level(),point,velocity,spec,0);
        }
    }
    private static void keys(CompoundTag tag,String name,float... values){var list=new net.minecraft.nbt.ListTag();for(float v:values)list.add(net.minecraft.nbt.FloatTag.valueOf(v));tag.put(name,list);}
    @Override public void handleEntityEvent(byte event){if(event==20)spawnCultistParticles();else super.handleEntityEvent(event);}
    @Override public void addAdditionalSaveData(CompoundTag tag){
        super.addAdditionalSaveData(tag);tag.remove("VisualOnly");tag.putBoolean("PersistenceRequired",nativePersistence);
        if(savedHome!=null&&savedHomeRadius>0){tag.putInt("HomeX",savedHome.getX());tag.putInt("HomeY",savedHome.getY());tag.putInt("HomeZ",savedHome.getZ());tag.putInt("HomeD",savedHomeRadius);}
    }
    @Override public void readAdditionalSaveData(CompoundTag tag){
        super.readAdditionalSaveData(tag);setNoAi(tag.getBoolean("NoAI")&&!tag.getBoolean("VisualOnly"));setNoGravity(false);
        nativePersistence=tag.getBoolean("PersistenceRequired")&&!tag.getBoolean("VisualOnly");
        savedHome=null;savedHomeRadius=0;entityData.set(CULT_HOME,java.util.Optional.empty());
        if(tag.contains("HomeD"))setCultHome(new BlockPos(tag.getInt("HomeX"),tag.getInt("HomeY"),tag.getInt("HomeZ")),tag.getInt("HomeD"));
    }
}
