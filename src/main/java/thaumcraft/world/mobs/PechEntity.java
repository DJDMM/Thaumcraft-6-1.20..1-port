package thaumcraft.world.mobs;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.catalog.CatalogModule;
import thaumcraft.catalog.entities.VisualMobEntity;
import thaumcraft.scanning.AspectRegistry;

/** Original pack gathering, valued-item taming, anger and physical three-variant trading mob. */
public final class PechEntity extends VisualMobEntity implements RangedAttackMob,Enemy {
    private static final EntityDataAccessor<Integer> TYPE=SynchedEntityData.defineId(PechEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ANGER=SynchedEntityData.defineId(PechEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> TAMED=SynchedEntityData.defineId(PechEntity.class,EntityDataSerializers.BOOLEAN);
    private final MeleeAttackGoal melee=new MeleeAttackGoal(this,.6,false);
    private final RangedAttackGoal arrows=new RangedAttackGoal(this,.6,20,50,15);
    private final RangedAttackGoal blasts=new RangedAttackGoal(this,.6,20,50,15);
    private final AvoidEntityGoal<Player> avoid=new AvoidEntityGoal<>(this,Player.class,8,.5,.6);
    public final NonNullList<ItemStack> loot=NonNullList.withSize(9,ItemStack.EMPTY);
    public float mumble;
    private boolean nativePersistence,trading,tickingGoals,combatTaskDirty;
    private int chargeCount;
    private UUID trader;
    public PechEntity(EntityType<? extends VisualMobEntity> type,Level level){
        super(type,level);setNoAi(false);setNoGravity(false);nativePersistence=false;xpReward=8;
        setDropChance(EquipmentSlot.MAINHAND,.2F);setDropChance(EquipmentSlot.OFFHAND,.2F);
        if(getNavigation() instanceof GroundPathNavigation g){g.setCanFloat(false);g.setCanOpenDoors(true);g.setCanPassDoors(true);}
        setCombatTask();
    }
    public static AttributeSupplier.Builder attributes(){return createMobAttributes().add(Attributes.MAX_HEALTH,30).add(Attributes.ATTACK_DAMAGE,6)
            .add(Attributes.MOVEMENT_SPEED,.5).add(Attributes.FOLLOW_RANGE,16);}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(TYPE,0);entityData.define(ANGER,0);entityData.define(TAMED,false);}
    @Override public int variant(){return entityData.get(TYPE);}
    public int anger(){return entityData.get(ANGER);}
    public boolean tamed(){return entityData.get(TAMED);}
    public void setPechType(int type){entityData.set(TYPE,Math.floorMod(type,3));}
    public void setAnger(int anger){entityData.set(ANGER,Math.max(0,Math.min(32767,anger)));}
    public void setTamed(boolean tamed){entityData.set(TAMED,tamed);setCombatTask();}
    public boolean trading(){return trading;}
    public boolean beginTrade(Player player){if(trader!=null&&!trader.equals(player.getUUID()))return false;trader=player.getUUID();trading=true;return true;}
    public boolean isTrader(Player player){return trader!=null&&trader.equals(player.getUUID());}
    public void endTrade(Player player){if(isTrader(player)){trader=null;trading=false;}}
    @Override protected void registerGoals(){
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(1,new Goal(){
            {setFlags(EnumSet.of(Flag.MOVE,Flag.JUMP));}
            @Override public boolean canUse(){return isAlive()&&!isInWater()&&tamed()&&onGround()&&!hurtMarked&&trading;}
            @Override public void start(){getNavigation().stop();}
            @Override public void stop(){trading=false;}
        });
        goalSelector.addGoal(3,new GatherGoal());
        goalSelector.addGoal(5,new OpenDoorGoal(this,true));
        goalSelector.addGoal(6,new MoveTowardsRestrictionGoal(this,.5));
        goalSelector.addGoal(9,new RandomStrollGoal(this,.6));
        goalSelector.addGoal(9,new LookAtPlayerGoal(this,Player.class,3,1));
        goalSelector.addGoal(10,new LookAtPlayerGoal(this,Mob.class,8));
        goalSelector.addGoal(11,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this));
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true){
            @Override public boolean canUse(){return anger()!=0&&super.canUse();}
        });
    }
    public void setCombatTask(){
        if(level()==null||level().isClientSide||melee==null)return;
        // Taming can happen inside GatherGoal.tick. Modern GoalSelector iterates
        // the same ordered set it mutates; finish this native AI tick before rebuilding.
        if(tickingGoals){combatTaskDirty=true;return;}
        goalSelector.removeGoal(melee);goalSelector.removeGoal(arrows);goalSelector.removeGoal(blasts);goalSelector.removeGoal(avoid);
        goalSelector.addGoal(2,getMainHandItem().is(Items.BOW)?arrows:getMainHandItem().is(CatalogModule.stack("pech_wand").getItem())?blasts:melee);
        if(!tamed())goalSelector.addGoal(4,avoid);
    }
    @Override public void setItemSlot(EquipmentSlot slot,ItemStack stack){super.setItemSlot(slot,stack);if(slot==EquipmentSlot.MAINHAND)setCombatTask();}
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,net.minecraft.world.DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData group,CompoundTag tag){
        ItemStack held=switch(random.nextInt(20)){
            case 0,12->CatalogModule.stack("pech_wand");case 1->new ItemStack(Items.STONE_SWORD);case 3->new ItemStack(Items.STONE_AXE);
            case 5->new ItemStack(Items.IRON_SWORD);case 6->new ItemStack(Items.IRON_AXE);case 7->new ItemStack(Items.FISHING_ROD);
            case 8->new ItemStack(Items.STONE_PICKAXE);case 9->new ItemStack(Items.IRON_PICKAXE);
            case 2,4,10,11,13->new ItemStack(Items.BOW);default->ItemStack.EMPTY;};
        setItemSlot(EquipmentSlot.MAINHAND,held);
        if(held.is(CatalogModule.stack("pech_wand").getItem())){setPechType(1);setDropChance(EquipmentSlot.MAINHAND,.1F);}
        else if(!held.isEmpty()){if(held.is(Items.BOW))setPechType(2);populateDefaultEquipmentEnchantments(random,difficulty);}
        setCanPickUpLoot(random.nextFloat()<.75F*difficulty.getSpecialMultiplier());setCombatTask();
        return super.finalizeSpawn(level,difficulty,reason,group,tag);
    }
    @Override public void performRangedAttack(LivingEntity target,float factor){
        if(level().isClientSide||target==null||!target.isAlive())return;
        if(variant()==1){thaumcraft.auromancy.NpcFocusCasting.cast(this,target);swing(InteractionHand.MAIN_HAND);}
        else if(variant()==2){
            Arrow arrow=new Arrow(level(),this);
            if(random.nextFloat()<.2F)arrow.addEffect(new MobEffectInstance(MobEffects.POISON,40));
            double dx=target.getX()-getX(),dy=target.getBoundingBox().minY+target.getBbHeight()/3-arrow.getY(),dz=target.getZ()-getZ();
            arrow.shoot(dx,dy+Math.sqrt(dx*dx+dz*dz)*.20000000298023224,dz,1.6F,14-level().getDifficulty().getId()*4);
            int power=EnchantmentHelper.getEnchantmentLevel(Enchantments.POWER_ARROWS,this),punch=EnchantmentHelper.getEnchantmentLevel(Enchantments.PUNCH_ARROWS,this);
            arrow.setBaseDamage(factor*2+random.nextGaussian()*.25+level().getDifficulty().getId()*.11F+(power>0?power*.5+.5:0));
            if(punch>0)arrow.setKnockback(punch);
            if(EnchantmentHelper.getEnchantmentLevel(Enchantments.FLAMING_ARROWS,this)>0)arrow.setSecondsOnFire(100);
            playSound(SoundEvents.ARROW_SHOOT,1,1/(random.nextFloat()*.4F+.8F));level().addFreshEntity(arrow);
        }
    }
    public static boolean isValued(ItemStack stack){return !stack.isEmpty()&&(stack.is(Items.ENDER_PEARL)||AspectRegistry.getAspects(stack).getAmount(Aspect.DESIRE)>1);}
    public static int value(ItemStack stack){return stack.isEmpty()?0:stack.is(Items.ENDER_PEARL)?15:Math.min(32,AspectRegistry.getAspects(stack).getAmount(Aspect.DESIRE)/2);}
    public boolean canPickup(ItemStack stack){
        if(stack.isEmpty())return false;
        if(!tamed()&&stack.is(Items.ENDER_PEARL))return true;
        for(ItemStack slot:loot)if(slot.isEmpty()||ItemStack.isSameItemSameTags(stack,slot)&&stack.getCount()+slot.getCount()<=slot.getMaxStackSize())return true;
        return false;
    }
    /** Returns a detached remainder or EMPTY; pack storage never aliases the dropped entity's stack. */
    public ItemStack pickupItem(ItemStack input){
        if(input.isEmpty())return ItemStack.EMPTY;ItemStack rest=input.copy();
        if(!tamed()&&isValued(rest)){
            if(random.nextInt(10)<value(rest)){setTamed(true);level().broadcastEntityEvent(this,(byte)18);}
            rest.shrink(1);return rest;
        }
        for(ItemStack slot:loot)if(!slot.isEmpty()&&ItemStack.isSameItemSameTags(rest,slot)&&slot.getCount()<slot.getMaxStackSize()){
            int moved=Math.min(rest.getCount(),slot.getMaxStackSize()-slot.getCount());slot.grow(moved);rest.shrink(moved);if(rest.isEmpty())return ItemStack.EMPTY;
        }
        for(int i=0;i<loot.size();i++)if(loot.get(i).isEmpty()){loot.set(i,rest.copy());return ItemStack.EMPTY;}
        return rest;
    }
    private void becomeAngry(LivingEntity attacker){
        if(attacker instanceof Player p&&p.isCreative())return;
        if(anger()<=0){level().broadcastEntityEvent(this,(byte)19);sound("pech_charge");}
        setTarget(attacker);setAnger(400+random.nextInt(400));setTamed(false);
    }
    @Override public boolean hurt(DamageSource source,float amount){
        if(isInvulnerableTo(source))return false;
        if(source.getEntity() instanceof Player p){
            for(PechEntity nearby:level().getEntitiesOfClass(PechEntity.class,getBoundingBox().inflate(32,16,32)))if(nearby!=this)nearby.becomeAngry(p);
            becomeAngry(p);
        }return super.hurt(source,amount);
    }
    @Override public void tick(){
        if(mumble>0)mumble*=.75F;
        if(anger()>0)setAnger(anger()-1);
        if(!level().isClientSide&&anger()>0&&getTarget()!=null){
            if(chargeCount>0)chargeCount--;if(chargeCount==0){chargeCount=100;sound("pech_charge");}level().broadcastEntityEvent(this,(byte)17);
        }
        if(level().isClientSide){if(random.nextInt(15)==0&&anger()>0)particles(net.minecraft.core.particles.ParticleTypes.ANGRY_VILLAGER,1);
            if(random.nextInt(25)==0&&tamed())particles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,1);}
        tickingGoals=true;
        try{super.tick();}finally{tickingGoals=false;}
        if(combatTaskDirty){combatTaskDirty=false;setCombatTask();}
    }
    @Override protected void customServerAiStep(){super.customServerAiStep();if(tickCount%40==0)heal(1);}
    @Override protected InteractionResult mobInteract(Player player,InteractionHand hand){
        if(player.isShiftKeyDown()||player.getItemInHand(hand).is(Items.NAME_TAG))return super.mobInteract(player,hand);
        if(tamed()&&(trader==null||isTrader(player))){
            if(player instanceof ServerPlayer server)NetworkHooks.openScreen(server,new SimpleMenuProvider((id,inv,p)->new PechMenu(id,inv,this),getDisplayName()),b->b.writeInt(getId()));
            return InteractionResult.sidedSuccess(level().isClientSide);
        }return super.mobInteract(player,hand);
    }
    @Override public Component getName(){return hasCustomName()?getCustomName():Component.translatable(variant()==0?"entity.Pech.name":"entity.Pech."+variant()+".name");}
    @Override public int getArmorValue(){return Math.min(20,super.getArmorValue()+2);}
    @Override protected float getStandingEyeHeight(Pose pose,EntityDimensions dimensions){return dimensions.height*.66F;}
    @Override public boolean canBeLeashed(Player p){return false;}
    @Override public boolean removeWhenFarAway(double distance){return loot.stream().filter(s->!s.isEmpty()).count()<5;}
    @Override protected boolean shouldDespawnInPeaceful(){return true;}
    @Override public boolean isPersistenceRequired(){return nativePersistence;}
    @Override public void setPersistenceRequired(){super.setPersistenceRequired();nativePersistence=true;}
    @Override protected net.minecraft.resources.ResourceLocation getDefaultLootTable(){return MobsModule.id("entities/pech");}
    @Override protected void dropFromLootTable(DamageSource source,boolean hit){
        for(ItemStack stack:loot)if(!stack.isEmpty()&&level().random.nextFloat()<.33F)spawnAtLocation(stack.copy(),1.5F);
        super.dropFromLootTable(source,hit);
    }
    @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean hit){
        for(EquipmentSlot slot:EquipmentSlot.values()){
            ItemStack stack=getItemBySlot(slot);float chance=getEquipmentDropChance(slot);boolean guarantee=chance>1;
            if(stack.isEmpty()||EnchantmentHelper.hasVanishingCurse(stack)||!hit&&!guarantee||Math.max(random.nextFloat()-looting*.01F,0)>=chance)continue;
            if(!guarantee&&stack.isDamageableItem())stack.setDamageValue(stack.getMaxDamage()-random.nextInt(1+random.nextInt(Math.max(stack.getMaxDamage()-3,1))));
            spawnAtLocation(stack);setItemSlot(slot,ItemStack.EMPTY);
        }
    }
    private void particles(net.minecraft.core.particles.ParticleOptions type,int count){for(int i=0;i<count;i++)level().addParticle(type,getX()+random.nextFloat()*getBbWidth()*2-getBbWidth(),getY()+.5+random.nextFloat()*getBbHeight(),getZ()+random.nextFloat()*getBbWidth()*2-getBbWidth(),random.nextGaussian()*.02,random.nextGaussian()*.02,random.nextGaussian()*.02);}
    @Override public void handleEntityEvent(byte event){if(event==16)mumble=MthPi.PI;else if(event==17)mumble=MthPi.PI*2;else if(event==18)particles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,5);else if(event==19)particles(net.minecraft.core.particles.ParticleTypes.ANGRY_VILLAGER,5);else super.handleEntityEvent(event);}
    private static final class MthPi{static final float PI=(float)Math.PI;}
    public void sound(String key){var sound=ForgeRegistries.SOUND_EVENTS.getValue(MobsModule.id(key));if(sound!=null)playSound(sound,.4F,getVoicePitch());}
    @Override public void playAmbientSound(){
        if(!level().isClientSide){if(random.nextInt(3)==0&&level().getEntitiesOfClass(PechEntity.class,getBoundingBox().inflate(4,2,4),p->p!=this).size()>0){level().broadcastEntityEvent(this,(byte)17);sound("pech_trade");return;}level().broadcastEntityEvent(this,(byte)16);}super.playAmbientSound();
    }
    @Override protected SoundEvent getAmbientSound(){return ForgeRegistries.SOUND_EVENTS.getValue(MobsModule.id("pech_idle"));}
    @Override protected SoundEvent getHurtSound(DamageSource source){return ForgeRegistries.SOUND_EVENTS.getValue(MobsModule.id("pech_hit"));}
    @Override protected SoundEvent getDeathSound(){return ForgeRegistries.SOUND_EVENTS.getValue(MobsModule.id("pech_death"));}
    @Override protected float getSoundVolume(){return .4F;}
    @Override public int getAmbientSoundInterval(){return 120;}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.remove("VisualOnly");tag.putBoolean("PersistenceRequired",nativePersistence);tag.putByte("PechType",(byte)variant());tag.putShort("Anger",(short)anger());tag.putBoolean("Tamed",tamed());ContainerHelper.saveAllItems(tag,loot);}
    @Override public void readAdditionalSaveData(CompoundTag tag){
        super.readAdditionalSaveData(tag);setNoAi(tag.getBoolean("NoAI")&&!tag.getBoolean("VisualOnly"));setNoGravity(false);nativePersistence=tag.getBoolean("PersistenceRequired")&&!tag.getBoolean("VisualOnly");
        setPechType(tag.getByte("PechType"));setAnger(tag.getShort("Anger"));entityData.set(TAMED,tag.getBoolean("Tamed"));loot.clear();ContainerHelper.loadAllItems(tag,loot);
        trading=false;trader=null;chargeCount=0;mumble=0;setCombatTask();
    }
    private final class GatherGoal extends Goal{
        ItemEntity target;int count,penalty;
        GatherGoal(){setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
        @Override public boolean canUse(){
            if(--count>0)return false;target=level().getEntitiesOfClass(ItemEntity.class,getBoundingBox().inflate(16),i->i.isAlive()&&!i.getPersistentData().getBoolean("PechDrop")&&canPickup(i.getItem())&&distanceToSqr(i)<=256)
                    .stream().min(Comparator.comparingDouble(PechEntity.this::distanceToSqr)).orElse(null);return target!=null;
        }
        @Override public boolean canContinueToUse(){return target!=null&&target.isAlive()&&!getNavigation().isDone()&&distanceToSqr(target)<256;}
        @Override public void start(){getNavigation().moveTo(target,getAttributeValue(Attributes.MOVEMENT_SPEED)*1.5);count=0;}
        @Override public void stop(){target=null;}
        @Override public void tick(){
            if(target==null||!target.isAlive())return;getLookControl().setLookAt(target,30,30);
            if(getSensing().hasLineOfSight(target)&&--count<=0){
                count=penalty+4+random.nextInt(4);getNavigation().moveTo(target,getAttributeValue(Attributes.MOVEMENT_SPEED)*1.5);
                var path=getNavigation().getPath();var end=path==null?null:path.getEndNode();
                if(end!=null&&target.distanceToSqr(end.x,end.y,end.z)<1)penalty=0;else penalty+=10;
            }
            if(distanceToSqr(target.getX(),target.getBoundingBox().minY,target.getZ())<=1.5){
                count=0;int before=target.getItem().getCount();ItemStack rest=pickupItem(target.getItem());
                if(rest.isEmpty())target.discard();else target.setItem(rest);
                if(rest.isEmpty()||rest.getCount()!=before)level().playSound(null,target.getX(),target.getY(),target.getZ(),SoundEvents.ITEM_PICKUP,SoundSource.NEUTRAL,.2F,((random.nextFloat()-random.nextFloat())*.7F+1)*2);
            }
        }
    }
}
