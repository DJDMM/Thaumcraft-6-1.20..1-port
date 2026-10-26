package thaumcraft.fx.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.*;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import thaumcraft.alchemy.*;
import thaumcraft.auromancy.*;
import thaumcraft.auromancy.focus.*;
import thaumcraft.auromancy.media.*;
import thaumcraft.auromancy.projectile.FocusProjectileEntity;
import thaumcraft.auromancy.remaining.RemainingFocusEffects;
import thaumcraft.catalog.CatalogModule;
import thaumcraft.catalog.entities.VisualEntitiesModule;
import thaumcraft.common.lib.enchantment.EnumInfusionEnchantment;
import thaumcraft.equipment.cleansing.CleansingClient;
import thaumcraft.equipment.cleansing.CleansingModule;
import thaumcraft.golemancy.client.SealClientState;
import thaumcraft.golemancy.entity.ThaumcraftGolemEntity;
import thaumcraft.golemancy.seals.core.*;

/**
 * Owned-world presentation fixtures. These are normal production item/effect callbacks and
 * normally ticking entities; this driver never dispatches FX or injects particles, ages or RNG.
 * Materials, infusion tags and compiler-authorized plans are explicitly raw QA fixtures, not
 * evidence that their research, recipes, focus crafting or paid casting routes were exercised.
 * Counters identify the live native frame; the resulting pixels still require human review.
 */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public final class VisualFxClientSmokeTest {
    private static final String WORLD="TC6-Visual-FX-Native-"+System.currentTimeMillis();
    private static final String[] IMAGES={
        "00-nitor-before","01-nitor-early","02-nitor-late",
        "03-runic-before","04-runic-hit-early","05-runic-hit-late","06-runic-expired",
        "07-elemental-sword-early","08-elemental-sword-late","09-shovel-builder-bamf",
        "10-arcing-slash","11-collector-follow",
        "12-fire-early","13-fire-late","14-air-early","15-air-late",
        "16-frost-early","17-frost-late","18-earth-early","19-earth-late",
        "20-flux-early","21-flux-late","22-heal-early","23-heal-late",
        "24-break-early","25-break-late","26-curse-early","27-curse-late",
        "28-cloud-fire","29-cloud-exchange","30-cloud-rift","31-mine-armed",
        "32-spellbat","33-projectile-flight","34-crucible-froth","35-crucible-boil",
        "36-alumentum-flight","37-collapser-flight","38-fancy-seal-hidden",
        "39-fancy-seal-shift","40-fancy-seal-restored","41-fabulous-seal-hidden",
        "42-fabulous-seal-shift","43-fabulous-seal-restored","44-fabulous-ghost-golem",
        "45-fabulous-fire","46-fabulous-earth","47-final-expired",
        "48-soap-native-use","49-soap-native-finished","50-bath-water-before",
        "51-bath-purified-early","52-bath-purified-late"
    };
    private static final String[] IMPACTS={FocusNodeRegistry.FIRE,FocusNodeRegistry.AIR,
        FocusNodeRegistry.FROST,FocusNodeRegistry.EARTH,FocusNodeRegistry.FLUX,
        FocusNodeRegistry.HEAL,FocusNodeRegistry.BREAK,FocusNodeRegistry.CURSE};
    private static final BlockPos DISPLAY=new BlockPos(0,112,1),NITOR=DISPLAY.above();
    private static final Vec3 IMPACT=new Vec3(.5,113.4,1.5),FORWARD=new Vec3(0,0,-1);
    private static final AtomicInteger SAVED=new AtomicInteger();
    private static final List<BlockPos> BATH=List.of(new BlockPos(-2,112,-1),new BlockPos(0,112,-1),new BlockPos(2,112,-1),
        new BlockPos(-2,112,1),new BlockPos(0,112,1),new BlockPos(2,112,1),
        new BlockPos(-2,112,3),new BlockPos(0,112,3),new BlockPos(2,112,3));
    private static final List<Integer> SALTS=new ArrayList<>();
    private static CompletableFuture<Void> work;
    private static TutorialSteps tutorial;
    private static boolean started,prepared,stopped,captureRequested,captured,verified,settlingStarted,settled;
    private static int scene,phase,stable,actor=-1,secondary=-1,arcingObserver=-1;
    private static long actionTick,sceneTick,startedAt,fxBefore,emittedBefore,shieldBefore,saltSpawnTick,bathEarlyTick,renderFrame,lastSoapPlayerRenderFrame=-1,lastNitorWorldFrame=-1,lastArcingObserverWorldFrame=-1;
    private static String expected="",fixtureFacts="initial";
    private VisualFxClientSmokeTest(){}
    private static boolean enabled(){return Boolean.getBoolean("thaumcraft.visualFxSmokeTest");}
    private static void require(boolean ok,String text){if(!ok)throw new AssertionError(text);}
    private static ServerPlayer player(Minecraft mc){var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());require(p!=null,"Owned integrated player absent");return p;}
    private static void submit(Minecraft mc,Runnable job){require(work==null,"Overlapping native server action");var next=new CompletableFuture<Void>();work=next;mc.getSingleplayerServer().execute(()->{try{job.run();next.complete(null);}catch(Throwable error){next.completeExceptionally(error);}});}
    private static boolean elapsed(Minecraft mc,int ticks){return mc.level.getGameTime()-actionTick>=ticks;}
    private static Entity actor(Minecraft mc){return mc.level.getEntity(actor);}
    private static ItemStack item(String id){var i=ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("thaumcraft",id));require(i!=null&&i!=Items.AIR,"Missing fixture item "+id);return new ItemStack(i);}
    private static void held(ServerPlayer p,ItemStack stack){p.getInventory().selected=0;p.getInventory().setItem(0,stack);p.connection.send(new ClientboundSetCarriedItemPacket(0));p.getInventory().setChanged();p.inventoryMenu.broadcastChanges();}
    private static void view(ServerPlayer p){p.teleportTo(p.serverLevel(),.5,112,6.5,180,0);p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;}
    private static void clean(ServerPlayer p){
        p.stopUsingItem();p.setShiftKeyDown(false);p.setHealth(p.getMaxHealth());p.clearFire();p.removeAllEffects();p.getAbilities().flying=false;p.onUpdateAbilities();
        for(var e:p.serverLevel().getEntities(p,new AABB(-18,111,-22,18,130,20)))if(!(e instanceof net.minecraft.world.entity.player.Player))e.discard();
        for(var q:BlockPos.betweenClosed(-3,112,-2,3,119,3))p.serverLevel().setBlock(q,Blocks.AIR.defaultBlockState(),3);
        p.serverLevel().setBlock(DISPLAY.below(),Blocks.BEDROCK.defaultBlockState(),3);
        p.setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY);p.setAbsorptionAmount(0);held(p,ItemStack.EMPTY);actor=secondary=-1;view(p);
    }
    private static Cow cow(ServerPlayer p,double x,double y,double z,boolean invisible){var c=EntityType.COW.create(p.serverLevel());require(c!=null,"Cow fixture missing");c.setNoAi(true);c.setNoGravity(true);c.setInvisible(invisible);c.moveTo(x,y,z,0,0);require(p.serverLevel().addFreshEntity(c),"Cow admission failed");return c;}
    private static void begin(Minecraft mc,String kind,Runnable action){
        expected=kind;fxBefore=FxEffects.receivedCount(kind);emittedBefore=FxEngine.emitted();actionTick=mc.level.getGameTime();phase=1;submit(mc,action);
    }
    private static boolean received(){return expected.isEmpty()||FxEffects.receivedCount(expected)>fxBefore;}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!enabled()||stopped)return;var mc=Minecraft.getInstance();
        try{
            if(!started){start(mc);return;}
            if(mc.level==null||mc.player==null||mc.getSingleplayerServer()==null||mc.getOverlay()!=null||mc.screen instanceof ReceivingLevelScreen)return;
            require(WORLD.equals(mc.getSingleplayerServer().getWorldData().getLevelName()),"Refusing a non-owned world");
            require(System.currentTimeMillis()-startedAt<15*60*1000,"Native visual fixture timeout");
            if(work!=null){if(!work.isDone())return;work.join();work=null;}
            if(!prepared){prepared=true;submit(mc,()->prepare(player(mc)));return;}
            if(captured){if(SAVED.get()!=scene+1)return;if(++stable<2)return;scene++;phase=stable=0;captured=false;settlingStarted=settled=false;sceneTick=mc.level.getGameTime();}
            if(scene>=IMAGES.length){finish(mc);return;}
            if(sceneTick==0)sceneTick=mc.level.getGameTime();
            require(mc.level.getGameTime()-sceneTick<900,"Scene did not become ready: "+IMAGES[scene]+" expected="+expected+" actor="+actor(mc));
            if(captureRequested)return;
            if(advance(mc))captureRequested=true;
        }catch(Throwable error){fail(mc,error);}
    }
    private static boolean advance(Minecraft mc){
        if(scene!=10&&mc.getCameraEntity()!=mc.player)mc.setCameraEntity(mc.player);
        // Native camera magnification only; never change production FX size or ages.
        mc.options.fov().set(scene==11||scene==31?40:scene==10||scene==34||scene==35?50:70);
        // Remove only fixture sources, then let their existing particles expire through normal ticks.
        // Paired phases, the live runic-hit sequence and concealment controls retain their actor state.
        boolean fresh=Set.of(3,7,9,10,11,12,14,16,18,20,22,24,26,28,29,30,31,32,33,34,36,37,38,45,46,47,48,50).contains(scene);
        if(fresh&&!settled){
            if(!settlingStarted){settlingStarted=true;mc.options.keyShift.setDown(false);mc.options.keyUse.setDown(false);mc.gameMode.releaseUsingItem(mc.player);submit(mc,()->clean(player(mc)));actionTick=mc.level.getGameTime();return false;}
            if(!elapsed(mc,80))return false;settled=true;
        }
        if(scene<=2)return nitor(mc);
        if(scene<=6)return shield(mc);
        if(scene<=8)return sword(mc);
        if(scene<=11)return tools(mc);
        if(scene<=27)return impact(mc,IMPACTS[(scene-12)/2],(scene&1)==0);
        if(scene<=33)return media(mc);
        if(scene<=35)return crucible(mc);
        if(scene<=37)return throwable(mc);
        if(scene<=44)return concealment(mc);
        if(scene<=46)return impact(mc,scene==45?FocusNodeRegistry.FIRE:FocusNodeRegistry.EARTH,true);
        if(scene>=48)return cleansing(mc);
        if(phase==0){phase=1;mc.options.keyShift.setDown(false);submit(mc,()->clean(player(mc)));actionTick=mc.level.getGameTime();return false;}
        return elapsed(mc,100)&&FxEffects.activeShieldCount()==0&&FxEngine.activeCount()==0;
    }
    /** Native soap and salt fixtures; reading the private batch never emits or advances particles. */
    private static List<?> cleansingBubbles(){
        try{var field=CleansingClient.class.getDeclaredField("BUBBLES");field.setAccessible(true);return List.copyOf((List<?>)field.get(null));}
        catch(ReflectiveOperationException error){throw new AssertionError("Native cleansing batch unavailable",error);}
    }
    private static boolean bathBubble(Object bubble){
        try{var field=bubble.getClass().getDeclaredField("randomMotion");field.setAccessible(true);return field.getFloat(bubble)==.001F;}
        catch(ReflectiveOperationException error){throw new AssertionError("Native cleansing bubble identity unavailable",error);}
    }
    private static long cleansingCount(boolean bath){return cleansingBubbles().stream().filter(b->bathBubble(b)==bath).count();}
    private static boolean cleansing(Minecraft mc){
        if(scene==48){
            if(phase==0){phase=1;expected="";mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);submit(mc,()->{
                var p=player(mc);clean(p);held(p,item("sanity_soap"));
                fixtureFacts="oneRawSanitySoap/nativeClientUseAndKeyUse/normalOnUseTicks/thirdPersonBodyBubbles/notPaidResearchOrCraft/noCleansingClientDispatch";
            });return false;}
            if(phase==1){if(!mc.player.getMainHandItem().is(item("sanity_soap").getItem()))return false;phase=2;actionTick=mc.level.getGameTime();mc.options.keyUse.setDown(true);require(mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND).consumesAction(),"Native soap use refused");return false;}
            return mc.player.isUsingItem()&&elapsed(mc,20)&&cleansingCount(false)>0;
        }
        if(scene==49){
            if(!elapsed(mc,96)||mc.player.isUsingItem()||!mc.player.getMainHandItem().isEmpty())return false;
            mc.options.keyUse.setDown(false);
            if(phase==0){phase=1;submit(mc,()->{
                var p=player(mc);require(!p.isUsingItem()&&p.getMainHandItem().isEmpty(),"Natural96-plus-tick soap release did not debit its physical stack");
                fixtureFacts="sameRawSoap/native96PlusTickAutoRelease/actualServerAndClientStackEmpty/normalSoapFinishedPacket/bodyBubbles/noForcedFinishOrRelease";
            });return false;}
            return cleansingCount(false)>0;
        }
        if(scene==50){
            if(phase==0){phase=1;mc.options.keyUse.setDown(false);mc.options.setCameraType(CameraType.FIRST_PERSON);submit(mc,()->{
                var p=player(mc);clean(p);var level=p.serverLevel();SALTS.clear();
                // Each source is physically isolated. Default dropped-item motion
                // can neither leak the water nor merge neighboring salt stacks.
                for(var pos:BlockPos.betweenClosed(-3,112,-2,3,112,4))level.setBlockAndUpdate(pos,Blocks.SMOOTH_STONE.defaultBlockState());
                for(var pos:BATH){
                    level.setBlockAndUpdate(pos,Blocks.WATER.defaultBlockState());
                    // Transparent physical lids keep normally buoyant items
                    // inside the source block at their natural expiry tick.
                    level.setBlockAndUpdate(pos.above(),Blocks.GLASS.defaultBlockState());
                    var stack=item("bath_salts");stack.setCount(3);
                    var salt=new ItemEntity(level,pos.getX()+.5,pos.getY()+.2,pos.getZ()+.5,stack);
                    require(salt.lifespan==200,"Raw salt entity did not retain original200tick lifespan");
                    require(level.addFreshEntity(salt),"Native bath salt admission failed");SALTS.add(salt.getId());
                }
                saltSpawnTick=level.getGameTime();p.getAbilities().flying=true;p.onUpdateAbilities();p.teleportTo(level,.5,114,7.5,180,32);
                fixtureFacts="nineRawThreeSaltStacks/ninePhysicallyIsolatedVanillaSourceWaterCells/transparentPhysicalGlassLidsKeepBuoyantItemsSubmerged/defaultItemEntityPhysicsAndLifespan200/initialArenaNotPaidCraft/noAgeMotionOrClockResetAfterAdmission";
            });actionTick=mc.level.getGameTime();return false;}
            return elapsed(mc,25)&&mc.player.distanceToSqr(.5,114,7.5)<.01
                &&BATH.stream().allMatch(pos->mc.level.getBlockState(pos).is(Blocks.WATER)&&mc.level.getFluidState(pos).isSource())
                &&SALTS.stream().allMatch(id->mc.level.getEntity(id) instanceof ItemEntity salt&&salt.tickCount>=20&&salt.tickCount<180)
                &&cleansingCount(true)==0;
        }
        if(!BATH.stream().allMatch(pos->mc.level.getBlockState(pos).is(CleansingModule.block())&&mc.level.getFluidState(pos).isSource()))return false;
        if(scene==51){
            if(phase==0){phase=1;submit(mc,()->{
                var p=player(mc);var level=p.serverLevel();require(level.getGameTime()-saltSpawnTick>=200,"Bath converted before natural200ticks");
                require(SALTS.stream().allMatch(id->level.getEntity(id)==null),"Expired salt stacks still exist");
                require(BATH.stream().allMatch(pos->level.getBlockState(pos).is(CleansingModule.block())&&level.getFluidState(pos).isSource()),"Original expiry did not convert all actual water sources");
                fixtureFacts="sameNineSourceCells/nineSaltEntitiesNaturallyExpired200Ticks/27SaltItemsConsumedByExpiry/nativeItemExpireEventPurifyingSourceConversion/nativeFluidAnimateTickBubbles/noDirectConversionOrAnimateTickCall";
            });return false;}
            if(cleansingCount(true)==0)return false;bathEarlyTick=mc.level.getGameTime();return true;
        }
        // Every first-frame bath bubble has a10..19tick life. A later live
        // generation after30ticks proves repeated natural animateTick emission.
        return mc.level.getGameTime()-bathEarlyTick>=30&&cleansingCount(true)>0&&cleansingCount(false)==0;
    }
    private static boolean nitor(Minecraft mc){
        if(scene==0){if(phase==0){phase=1;submit(mc,()->{var p=player(mc);clean(p);p.serverLevel().setBlock(DISPLAY,Blocks.STONE.defaultBlockState(),3);held(p,new ItemStack(AlchemyModule.NITOR_ITEM.get()));fixtureFacts="physicalStoneSupport/nativeSurvivalBlockItemPlacementNotPaidCraft";});return false;}return ++stable>20;}
        if(scene==1){if(phase==0){begin(mc,"nitorFlames",()->{var p=player(mc);var stack=p.getMainHandItem();require(stack.is(AlchemyModule.NITOR_ITEM.get()),"Nitor not held");var result=stack.useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(DISPLAY).add(0,.5,0),Direction.UP,DISPLAY,false)));require(result.consumesAction(),"Native nitor placement refused");require(p.serverLevel().getBlockState(NITOR).is(AlchemyModule.NITOR.get())&&stack.isEmpty(),"Nitor placement/debit mismatch");p.inventoryMenu.broadcastChanges();});return false;}return elapsed(mc,6)&&mc.level.getBlockEntity(NITOR) instanceof thaumcraft.fx.nitor.NitorBlockEntity&&received()&&liveNitorGeometry(mc);}
        return elapsed(mc,40)&&mc.level.getBlockState(NITOR).is(AlchemyModule.NITOR.get())&&FxEngine.activeCount()>0;
    }
    private static boolean liveNitorGeometry(Minecraft mc){
        // Dispatch may precede START-tick admission, and a brand-new FXGeneric
        // has no calculated keyframes. Require naturally aged actual geometry.
        var live=FxEngine.snapshot().stream().filter(p->p.position().distanceToSqr(Vec3.atCenterOf(NITOR))<2
            &&p.age()>=2&&p.age()<p.lifespan()&&p.halfSize(mc.getFrameTime())>.025F&&p.opacity(mc.getFrameTime())>1/255F).toList();
        return live.stream().filter(p->p.start==264&&p.layer==0).count()>=3
            &&live.stream().anyMatch(p->p.start==457&&p.layer==1);
    }
    private static boolean shield(Minecraft mc){
        if(scene==3){
            if(phase==0){phase=1;mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);submit(mc,()->{var p=player(mc);clean(p);var chest=new ItemStack(Items.LEATHER_CHESTPLATE);chest.getOrCreateTag().putByte("TC.RUNIC",(byte)6);p.setItemSlot(EquipmentSlot.CHEST,chest);p.setAbsorptionAmount(6);actor=cow(p,2.6,112,5.5,false).getId();fixtureFacts="rawTC.RUNIC6ChestAndAbsorption/nativePlayerTickRegistration/cowMobAttackDamage2/nativeDamageCameraRollAndKnockbackNotMatchedPoseControl/noPaidInfusion";});return false;}
            return mc.player.getItemBySlot(EquipmentSlot.CHEST).hasTag()&&mc.player.getAbsorptionAmount()>0&&++stable>=30;
        }
        if(scene==4){if(phase==0){shieldBefore=FxEffects.renderedShieldCount();begin(mc,"shield",()->{var p=player(mc);var attacker=p.serverLevel().getEntity(actor);require(attacker instanceof Cow&&p.getAbsorptionAmount()>0,"Runic actor/absorption missing");require(p.hurt(p.damageSources().mobAttack((Cow)attacker),2),"Actual cow damage refused");require(p.getAbsorptionAmount()<6,"Actual damage did not consume absorption");});return false;}return received()&&FxEffects.activeShieldCount()>0&&FxEffects.renderedShieldCount()>shieldBefore;}
        if(scene==5)return FxEffects.activeShieldCount()>0&&FxEffects.renderedShieldCount()>shieldBefore;
        return elapsed(mc,25)&&FxEffects.activeShieldCount()==0;
    }
    private static boolean sword(Minecraft mc){
        if(scene==7){
            if(phase==0){phase=1;submit(mc,()->{var p=player(mc);clean(p);held(p,item("elemental_sword"));fixtureFacts="nativeHeldElementalSwordUse/normalClientServerOnUseTicks/noSpiralInjection";});return false;}
            if(phase==1){if(!mc.player.getMainHandItem().is(item("elemental_sword").getItem()))return false;phase=2;expected="smokeSpiral";fxBefore=FxEffects.receivedCount(expected);actionTick=mc.level.getGameTime();mc.options.keyUse.setDown(true);mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);return false;}
            return mc.player.isUsingItem()&&received()&&elapsed(mc,12)&&toolGeometryReady(mc);
        }
        return mc.player.isUsingItem()&&elapsed(mc,25)&&FxEffects.receivedCount("smokeSpiral")>=fxBefore+10&&toolGeometryReady(mc);
    }
    private static boolean tools(Minecraft mc){
        if(scene==10)return arcing(mc);
        if(phase==0){
            mc.options.keyUse.setDown(false);mc.gameMode.releaseUsingItem(mc.player);mc.options.setCameraType(CameraType.FIRST_PERSON);
            if(scene==9)begin(mc,"bamf",()->{var p=player(mc);clean(p);p.serverLevel().setBlock(NITOR,Blocks.STONE.defaultBlockState(),3);held(p,item("elemental_shovel"));p.getInventory().setItem(1,new ItemStack(Items.STONE,9));var s=p.getMainHandItem();var ctx=new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(NITOR).add(0,0,.5),Direction.SOUTH,NITOR,false));require(s.getItem().onItemUseFirst(s,ctx).consumesAction(),"Native shovel builder refused");require(p.getInventory().getItem(1).isEmpty(),"Builder did not debit nine stone");p.inventoryMenu.broadcastChanges();fixtureFacts="nativeElementalShovelOnItemUseFirst/ninePhysicalBlocks/nineStoneDebit";});
            else begin(mc,"itemFollowing",()->{var p=player(mc);clean(p);p.connection.teleport(.5,112,6.5,180,15);var s=new ItemStack(Items.DIAMOND_SWORD);EnumInfusionEnchantment.addInfusionEnchantment(s,EnumInfusionEnchantment.COLLECTOR,1);held(p,s);var target=cow(p,.5,112,1.5,false);target.setHealth(1);actor=target.getId();p.attack(target);require(!target.isAlive(),"COLLECTOR cow kill failed");fixtureFacts="rawCOLLECTOR1/nativePlayerAttackKill/LivingDrops/actualItemHomingTicks/naturalCorpseRemoval/nativeFov40DownwardView";});
            return false;
        }
        return received()&&elapsed(mc,scene==9?4:scene==10?3:22)&&(scene!=11||actor(mc)==null)&&toolGeometryReady(mc);
    }
    private static boolean arcing(Minecraft mc){
        if(phase==0){
            mc.options.keyUse.setDown(false);mc.gameMode.releaseUsingItem(mc.player);mc.options.setCameraType(CameraType.FIRST_PERSON);
            begin(mc,"slash",()->{
                var p=player(mc);clean(p);var s=new ItemStack(Items.DIAMOND_SWORD);
                EnumInfusionEnchantment.addInfusionEnchantment(s,EnumInfusionEnchantment.ARCING,1);held(p,s);
                actor=cow(p,.5,112,3.5,false).getId();secondary=cow(p,1.8,112,2.6,false).getId();
                // The original plane is oriented along its flight, so a side observer
                // can see it without the caster's body or native sword sweep masking it.
                var observer=new ArmorStand(p.serverLevel(),4.5,113.4,5);
                observer.setInvisible(true);observer.setNoGravity(true);observer.setSilent(true);
                var data=observer.saveWithoutId(new net.minecraft.nbt.CompoundTag());data.putBoolean("Marker",true);observer.load(data);
                // 11.25 degrees is exactly representable by the native spawn rotation byte.
                observer.moveTo(4.5,113.4,5,90,11.25F);observer.yRotO=90;observer.xRotO=11.25F;
                observer.yHeadRot=observer.yHeadRotO=90;observer.yBodyRot=observer.yBodyRotO=90;
                require(observer.isMarker()&&p.serverLevel().addFreshEntity(observer),"Native raw Arcing observer admission failed");arcingObserver=observer.getId();
                fixtureFacts="rawARCING1/nativePlayerAttackEntityEvent/secondaryCowActuallyDamaged/rawInvisibleMarkerArmorStandSideObserver/nativeFirstPersonCameraOnly/casterAndTargetsKeepNativeTrajectory/noSweepRemovalOrFxInjection";
            });return false;
        }
        if(phase==1){
            var observer=mc.level.getEntity(arcingObserver);
            if(!(observer instanceof ArmorStand stand)||!stand.isMarker()||!stand.isInvisible())return false;
            mc.setCameraEntity(observer);lastArcingObserverWorldFrame=-1;phase=2;return false;
        }
        if(phase==2){
            if(lastArcingObserverWorldFrame<0||!arcingObserverReady(mc))return false;
            begin(mc,"slash",()->{var p=player(mc);var target=p.serverLevel().getEntity(actor);var extra=p.serverLevel().getEntity(secondary);
                require(target instanceof Cow&&extra instanceof Cow,"Native Arcing actors disappeared");
                p.attack(target);require(((Cow)extra).getHealth()<((Cow)extra).getMaxHealth(),"Native ARCING secondary damage missing");
            });phase=3;return false;
        }
        return arcingObserverReady(mc)&&received()&&elapsed(mc,3)&&toolGeometryReady(mc);
    }
    private static boolean arcingObserverReady(Minecraft mc){
        var observer=mc.level.getEntity(arcingObserver);var camera=mc.gameRenderer.getMainCamera();
        return observer instanceof ArmorStand stand&&stand.isMarker()&&stand.isInvisible()
            &&mc.getCameraEntity()==observer&&camera.getEntity()==observer&&mc.options.getCameraType()==CameraType.FIRST_PERSON
            &&camera.getPosition().distanceToSqr(observer.getEyePosition(mc.getFrameTime()))<.0001
            &&Math.abs(camera.getYRot()-90)<.01F&&Math.abs(camera.getXRot()-11.25F)<.01F;
    }
    private static boolean toolGeometryReady(Minecraft mc){
        return FxEngine.snapshot().stream().anyMatch(p->{
            if(!p.isAlive()||p.age()<1||p.age()>=p.lifespan())return false;
            if(scene==7||scene==8)return p instanceof FxCustomParticles.Spiral&&p.age()>=8;
            // BETA26 deliberately starts bamf at the block centre. Wait for its
            // natural outward motion beyond the actual newly built front face.
            if(scene==9)return p.grid==16&&p.start==123&&p.age()>=4&&p.position().z>3.02;
            if(scene==10)return p instanceof FxCustomParticles.Slash&&p.age()>=3&&p.age()<=6;
            return scene==11&&p.start==64&&p.layer==0&&p.age()>=2;
        });
    }
    private static boolean mineGeometryReady(Minecraft mc){
        var mine=actor(mc);if(!(mine instanceof FocusMineEntity))return false;
        return FxEngine.snapshot().stream().anyMatch(p->p.start==640&&p.age()>=1&&p.age()<=4
            &&p.isAlive()&&p.position().distanceToSqr(mine.position())<1&&p.opacity(mc.getFrameTime())>1/255F);
    }
    private static boolean frothGeometryReady(Minecraft mc){
        var camera=mc.gameRenderer.getMainCamera();
        if(mc.getCameraEntity()!=mc.player||camera.getEntity()!=mc.player||mc.options.getCameraType()!=CameraType.FIRST_PERSON
                ||camera.getPosition().distanceToSqr(mc.player.getEyePosition(mc.getFrameTime()))>=.0001
                ||Math.abs(Math.abs(camera.getYRot())-180)>=.01F||Math.abs(camera.getXRot()-62.5F)>=.01F)return false;
        if(!(mc.level.getBlockEntity(DISPLAY) instanceof CrucibleBlockEntity crucible))return false;
        double fluidY=DISPLAY.getY()+thaumcraft.fx.emitters.MachineFxPrograms.fluidHeight(crucible.water(),crucible.aspects());
        return FxEngine.snapshot().stream().anyMatch(p->p.start==64&&p.age()==1&&p.isAlive()
            &&p.position().y>112.6&&p.position().y<113&&p.opacity(mc.getFrameTime())>1/255F&&p.halfSize(mc.getFrameTime())>.0025F
            &&frothSight(mc,p.position()).getType()==HitResult.Type.MISS
            &&frothQuad(mc,p).stream().filter(point->point.y>fluidY+.001&&frothSight(mc,point).getType()==HitResult.Type.MISS).count()>=2);
    }
    private static BlockHitResult frothSight(Minecraft mc,Vec3 point){
        return mc.level.clip(new ClipContext(mc.gameRenderer.getMainCamera().getPosition(),point,ClipContext.Block.VISUAL,ClipContext.Fluid.NONE,mc.player));
    }
    private static List<Vec3> frothQuad(Minecraft mc,FxParticle particle){
        return inspectFrothVertices(mc,particle).points;
    }
    private static FrothVertices inspectFrothVertices(Minecraft mc,FxParticle particle){
        // Read the real production draw's interpolated vertices into a CPU-only
        // sink. It submits no GPU buffer and changes no particle or render state.
        var vertices=new FrothVertices(mc.gameRenderer.getMainCamera().getPosition());
        particle.draw(vertices,mc.gameRenderer.getMainCamera(),mc.getFrameTime(),new org.joml.Matrix4f());
        require(vertices.points.size()==4,"Original froth did not supply four billboard vertices");
        return vertices;
    }
    private static final class FrothVertices implements VertexConsumer {
        private final Vec3 camera;
        private final List<Vec3> points=new ArrayList<>(4);
        private final List<String> inputs=new ArrayList<>(4);
        private double x,y,z;
        private float u,v;
        private int r,g,b,a,blockLight,skyLight;
        private FrothVertices(Vec3 camera){this.camera=camera;}
        @Override public VertexConsumer vertex(double x,double y,double z){this.x=x;this.y=y;this.z=z;return this;}
        @Override public VertexConsumer color(int r,int g,int b,int a){this.r=r;this.g=g;this.b=b;this.a=a;return this;}
        @Override public VertexConsumer uv(float u,float v){this.u=u;this.v=v;return this;}
        @Override public VertexConsumer overlayCoords(int u,int v){return this;}
        @Override public VertexConsumer uv2(int u,int v){blockLight=u;skyLight=v;return this;}
        @Override public VertexConsumer normal(float x,float y,float z){return this;}
        @Override public void endVertex(){
            var point=camera.add(x,y,z);points.add(point);
            inputs.add("pos="+point+",rgba="+r+","+g+","+b+","+a+",uv="+u+","+v+",uv2="+blockLight+","+skyLight);
        }
        @Override public void defaultColor(int r,int g,int b,int a){color(r,g,b,a);}
        @Override public void unsetDefaultColor(){}
    }
    private static String frothSightFact(Minecraft mc,Vec3 point){
        var hit=frothSight(mc,point);
        return hit.getType()+":"+hit.getBlockPos()+":"+hit.getDirection()+":"+mc.level.getBlockState(hit.getBlockPos());
    }
    private static void logFrothGeometry(Minecraft mc){
        var facts=FxEngine.snapshot().stream().filter(p->p.start==64&&p.position().y>112.6&&p.position().y<113).map(p->{
            var pos=BlockPos.containing(p.position());var neighbours=new ArrayList<String>(6);
            for(var direction:Direction.values()){
                var next=pos.relative(direction);
                neighbours.add(direction+":block="+mc.level.getBrightness(LightLayer.BLOCK,next)+",sky="+mc.level.getBrightness(LightLayer.SKY,next));
            }
            int light=p.fixedLight>=0?p.fixedLight:p.fullbright?0xF000F0:net.minecraft.client.renderer.LevelRenderer.getLightColor(mc.level,pos);
            var vertices=inspectFrothVertices(mc,p);
            return "age="+p.age()+"/"+p.lifespan()+",pos="+p.position()+",half="+p.halfSize(mc.getFrameTime())+",alpha="+p.opacity(mc.getFrameTime())
                    +",packedLight="+light+",block="+mc.level.getBrightness(LightLayer.BLOCK,pos)+",sky="+mc.level.getBrightness(LightLayer.SKY,pos)
                    +",centreSight="+frothSightFact(mc,p.position())+",actualRenderQuad="+vertices.inputs
                    +",actualRenderQuadSight="+vertices.points.stream().map(point->point+":"+frothSightFact(mc,point)).toList()+",neighbours="+neighbours;
        }).toList();
        LogUtils.getLogger().info("THAUMCRAFT_VISUAL_FX_FROTH_NATIVE_GEOMETRY: partial={} camera={} rotation={} facts={}",mc.getFrameTime(),mc.gameRenderer.getMainCamera().getPosition(),mc.gameRenderer.getMainCamera().rotation(),facts);
    }
    private static FocusGraph.Node effect(String key){return new FocusGraph.Node(2,1,List.of(),0,2,key,FocusNodeRegistry.get(key).defaultSettings());}
    private static void applyImpact(ServerPlayer p,String key){
        clean(p);fixtureFacts="rawInvisibleCowTarget/normalProductionFocusApplyCallback/noPaidCast/"+key;
        var c=cow(p,.5,112.5,1.5,true);c.setHealth(8);actor=c.getId();HitResult hit=new EntityHitResult(c,IMPACT);var node=effect(key);boolean result;
        if(key.equals(FocusNodeRegistry.FIRE))result=FocusCasting.applyFire(p.serverLevel(),p,hit,1,1,1);
        else if(key.equals(FocusNodeRegistry.AIR)||key.equals(FocusNodeRegistry.FROST)||key.equals(FocusNodeRegistry.EARTH))result=FocusEffects.apply(p.serverLevel(),p,node,hit,FORWARD,1);
        else if(key.equals(FocusNodeRegistry.FLUX)||key.equals(FocusNodeRegistry.HEAL))result=AdvancedFocusEffects.apply(p.serverLevel(),p,node,hit,FORWARD,1);
        else if(key.equals(FocusNodeRegistry.BREAK)){p.serverLevel().setBlock(DISPLAY.above(),Blocks.BEDROCK.defaultBlockState(),3);hit=new BlockHitResult(IMPACT,Direction.SOUTH,DISPLAY.above(),false);result=FocusBreakEffect.apply(p.serverLevel(),p,node,hit,FORWARD,1,0);fixtureFacts="rawUnbreakableBedrock/nativeFocusBreakApply/presentationOnlyNotHarvestQueue";}
        else result=RemainingFocusEffects.apply(p.serverLevel(),p,node,hit,FORWARD,1,0);
        // BETA26 deliberately returns false for accepted Frost, Flux, Heal and other effects.
        // Native target changes and the actual arriving packet establish that the callback ran.
        if(key.equals(FocusNodeRegistry.HEAL))require(c.getHealth()>8,"Native Heal did not heal its living target");
        else if(!key.equals(FocusNodeRegistry.BREAK))require(c.getHealth()<8,"Native focus did not damage its living target: "+key);
        else require(result&&p.serverLevel().getBlockState(DISPLAY.above()).is(Blocks.BEDROCK),"Unbreakable native Break fixture changed");
        LogUtils.getLogger().info("THAUMCRAFT_VISUAL_FX_FOCUS_CALLBACK: key={} originalReturn={} actualTargetHealth={} nativeActor={}",key,result,c.getHealth(),actor);
    }
    private static boolean impact(Minecraft mc,String key,boolean first){
        if(first){if(phase==0){mc.options.setCameraType(CameraType.FIRST_PERSON);begin(mc,"focusImpact/"+key,()->applyImpact(player(mc),key));return false;}return received()&&FxEngine.emitted()>emittedBefore;}
        // The early screenshot and its save acknowledgment consume real native ticks. Never rewind ages.
        return received()&&elapsed(mc,3)&&FxEngine.snapshot().stream().anyMatch(p->p.age()>0&&p.age()<p.lifespan());
    }
    private static FocusPlan plan(String medium,String key,Map<String,Integer> overrides){
        var settings=new LinkedHashMap<>(FocusNodeRegistry.get(medium).defaultSettings());settings.putAll(overrides);
        var graph=new FocusGraph(List.of(new FocusGraph.Node(0,-1,List.of(1),0,0,FocusNodeRegistry.ROOT,Map.of()),new FocusGraph.Node(1,0,List.of(2),0,1,medium,settings),effect(key)));
        var result=FocusCompiler.compile(graph,CatalogModule.stack("focus_3"),research->true);require(result.success(),"Raw QA plan compiler rejected "+medium+"/"+key+": "+result.error());return result.plan();
    }
    private static void findActor(ServerPlayer p,Class<? extends Entity> type){var list=p.serverLevel().getEntitiesOfClass(type,new AABB(-10,111,-20,10,125,10));require(list.size()==1,"Expected one native "+type.getSimpleName()+", found "+list.size());actor=list.get(0).getId();}
    private static boolean media(Minecraft mc){
        if(phase==0){begin(mc,"",()->{var p=player(mc);clean(p);fixtureFacts="compilerValidatedRawFocusPlan/productionSpawnFactory/nativeEntityTicks/notPaidCast/notSavedPaidFocus";boolean ok;
            if(scene<=30){String key=scene==28?FocusNodeRegistry.FIRE:scene==29?FocusNodeRegistry.EXCHANGE:FocusNodeRegistry.RIFT;var plan=plan(FocusNodeRegistry.CLOUD,key,Map.of("radius",1,"duration",5));ok=FocusCloudEntity.spawn(p,plan,2,new Vec3(.5,114,1.5),1,5,1,0);findActor(p,FocusCloudEntity.class);}
            else if(scene==31){p.getAbilities().flying=true;p.onUpdateAbilities();p.connection.teleport(.5,113,3.5,180,51);var plan=plan(FocusNodeRegistry.MINE,FocusNodeRegistry.FIRE,Map.of("target",0));ok=FocusMineEntity.spawn(p,plan,2,new Vec3(.5,112.6,1.5),new Vec3(0,-1,0),false,1,0);findActor(p,FocusMineEntity.class);fixtureFacts+="/nativeCloserDownwardMineViewFov40";}
            else if(scene==32){var plan=plan(FocusNodeRegistry.SPELLBAT,FocusNodeRegistry.FIRE,Map.of("target",0));ok=SpellBatEntity.spawn(p,plan,2,new Vec3(.5,113.5,1.5),false,1,0);findActor(p,SpellBatEntity.class);}
            else{var plan=plan(FocusNodeRegistry.PROJECTILE,FocusNodeRegistry.FIRE,Map.of("speed",1,"option",0));ok=FocusProjectileEntity.spawn(p,plan,2,new Vec3(.5,113.4,5),FORWARD,1,0,1,0);findActor(p,FocusProjectileEntity.class);}
            require(ok,"Native focus medium factory rejected scene "+scene);
        });return false;}
        var a=actor(mc);if(a==null)return false;
        if(scene==31)return a instanceof FocusMineEntity mine&&mine.armed()&&mine.counter()==0&&mineGeometryReady(mc);
        return a.tickCount>=5&&FxEngine.emitted()>emittedBefore;
    }
    private static boolean crucible(Minecraft mc){
        if(scene==34){if(phase==0){begin(mc,"",()->{var p=player(mc);clean(p);p.serverLevel().setBlock(DISPLAY,AlchemyModule.CRUCIBLE.get().defaultBlockState(),3);p.serverLevel().setBlock(DISPLAY.below(),AlchemyModule.NITOR.get().defaultBlockState(),3);var tile=(CrucibleBlockEntity)p.serverLevel().getBlockEntity(DISPLAY);require(tile!=null&&tile.fillWater(),"Native crucible water callback refused");p.getAbilities().flying=true;p.onUpdateAbilities();p.teleportTo(p.serverLevel(),.5,112.8,2.35,180,62.5F);fixtureFacts="rawPhysicalCrucibleAndNitor/nativeFillWater/151RealServerHeatTicks/noHeatNBTOrClockSetter/closerSteeperNativeCamera/nativeAgedQuadRayVisibilityAboveFluid/nativeFov50";});return false;}var be=mc.level.getBlockEntity(DISPLAY);return be instanceof CrucibleBlockEntity c&&c.water()==1000&&c.heat()>150&&frothGeometryReady(mc);}
        if(phase==0){begin(mc,"",()->{var p=player(mc);var c=(CrucibleBlockEntity)p.serverLevel().getBlockEntity(DISPLAY);require(c!=null&&c.heat()>150&&c.water()==1000,"Crucible not naturally hot");var sugar=new ItemStack(Items.SUGAR);require(c.consume(sugar,p)&&sugar.isEmpty()&&c.aspects().visSize()>0,"Native sugar consumption/aspect debit missing");fixtureFacts="naturallyHotCrucible/normalConsumeSugar/aspectsAdded/blockEvent2NativeBoil";});return false;}
        return FxEngine.emitted()>emittedBefore&&FxEngine.snapshot().stream().anyMatch(p->p.start==64&&p.lifespan()>10&&p.position().y>112.8);
    }
    private static boolean throwable(Minecraft mc){
        if(phase==0){begin(mc,"alumentum",()->{var p=player(mc);clean(p);held(p,item(scene==36?"alumentum":"causality_collapser"));var held=p.getMainHandItem();var used=held.getItem().use(p.serverLevel(),p,InteractionHand.MAIN_HAND);require(used.getResult().consumesAction()&&held.isEmpty(),"Native throwable use/debit failed");p.inventoryMenu.broadcastChanges();var list=p.serverLevel().getEntities(p,new AABB(-4,112,-8,4,120,9));require(list.size()==1,"One native throwable expected");actor=list.get(0).getId();fixtureFacts="nativeThrowableItemUseCallback/survivalDebit/realEntityFlight/noTrailInjection";});return false;}
        return actor(mc)!=null&&actor(mc).tickCount>=3&&received()&&FxEngine.emitted()>emittedBefore;
    }
    private static boolean concealment(Minecraft mc){
        if(scene==38){if(phase==0){mc.options.graphicsMode().set(GraphicsStatus.FANCY);mc.levelRenderer.allChanged();phase=1;submit(mc,()->{var p=player(mc);clean(p);var pos=new SealPos(new BlockPos(0,113,-1),Direction.SOUTH);p.serverLevel().setBlock(pos.pos(),Blocks.STONE.defaultBlockState(),3);held(p,SealRegistry.stack("thaumcraft:pickup"));require(SealRegistry.place(p,pos,"thaumcraft:pickup",p.getMainHandItem())&&p.getMainHandItem().isEmpty(),"Physical pickup seal placement/debit failed");held(p,item("golem_bell"));var g=VisualEntitiesModule.GOLEM.get().create(p.serverLevel());require(g!=null&&g.setProps(0L),"Native wood golem fixture invalid");g.setOwnerId(p.getUUID());g.setValidSpawn();g.setNoAi(true);g.moveTo(2.5,112,-1.5,0,0);require(p.serverLevel().addFreshEntity(g),"Golem admission failed");actor=g.getId();for(var q:BlockPos.betweenClosed(-8,112,0,8,120,0))p.serverLevel().setBlock(q,Blocks.BEDROCK.defaultBlockState(),3);fixtureFacts="nativeSealPlacementAndDebit/actualS2CSealState/rawOwnedWoodGolem/noAIPlacementFixture/opaqueBedrockWall/normalShiftDepthControlsForSealAndGolem/notPaidPress";});actionTick=mc.level.getGameTime();return false;}return !SealClientState.seals().isEmpty()&&actor(mc) instanceof ThaumcraftGolemEntity&&!mc.player.isShiftKeyDown()&&elapsed(mc,100);}
        if(scene==41&&phase==0){phase=1;mc.options.graphicsMode().set(GraphicsStatus.FABULOUS);mc.levelRenderer.allChanged();actionTick=mc.level.getGameTime();return false;}
        if(scene==44){if(phase==0){phase=1;mc.options.keyShift.setDown(true);submit(mc,()->{var p=player(mc);require(p.serverLevel().getEntity(actor) instanceof ThaumcraftGolemEntity,"Matched ghost golem fixture disappeared");p.teleportTo(p.serverLevel(),.5,112,4.5,180,5);fixtureFacts="sameOwnedWoodGolemAsSixOcclusionControls/closerNativeModelShiftView/notPaidPress";});actionTick=mc.level.getGameTime();return false;}return actor(mc) instanceof ThaumcraftGolemEntity&&mc.player.isShiftKeyDown()&&elapsed(mc,20);}
        boolean shift=scene==39||scene==42;
        if(phase==0){phase=1;mc.options.keyShift.setDown(shift);actionTick=mc.level.getGameTime();return false;}
        return mc.player.isShiftKeyDown()==shift&&!SealClientState.seals().isEmpty()&&mc.level.getBlockState(new BlockPos(0,113,0)).is(Blocks.BEDROCK)&&elapsed(mc,25);
    }
    private static void prepare(ServerPlayer p){
        var l=p.serverLevel();for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)l.getChunk(x,z);
        for(var q:BlockPos.betweenClosed(-18,112,-22,18,130,20))l.setBlock(q,Blocks.AIR.defaultBlockState(),2);
        for(var q:BlockPos.betweenClosed(-18,111,-22,18,111,20))l.setBlock(q,Blocks.BEDROCK.defaultBlockState(),2);
        for(var q:BlockPos.betweenClosed(-18,112,-8,18,123,-8))l.setBlock(q,Blocks.STONE.defaultBlockState(),2);
        l.setDayTime(6000);p.getInventory().clearContent();p.getAbilities().mayfly=true;p.getAbilities().flying=false;p.getAbilities().invulnerable=false;p.onUpdateAbilities();view(p);p.inventoryMenu.broadcastChanges();
        LogUtils.getLogger().info("THAUMCRAFT_VISUAL_FX_FIXTURES: ownedSurvivalWorld/rawQAItemsAndInfusionTags/rawCompilerAuthorizedFocusPlans/rawActorsAndArena/notPaidResearchCraftsOrCasts/normalProductionCallbacksAndRealTicks/noFxDispatch/noInjectedParticles/noAgeOrClockOrRngChanges");
    }
    @SubscribeEvent public static void soapPlayerRendered(RenderPlayerEvent.Post event){
        if(!enabled()||stopped||!((scene>=48&&scene<=49)||(scene>=7&&scene<=8)))return;var mc=Minecraft.getInstance();
        if(event.getEntity()==mc.player&&mc.screen==null&&mc.getOverlay()==null&&mc.gameRenderer.getMainCamera().isDetached())lastSoapPlayerRenderFrame=renderFrame;
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void nitorWorldRendered(RenderLevelStageEvent event){
        if(!enabled()||stopped||!(scene==1||(scene>=7&&scene<=11)||scene==31||scene==34)||event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;
        var mc=Minecraft.getInstance();
        // FxEngine renders at NORMAL priority and flushes each original layer.
        // This is pipeline readiness; actual resulting pixels still need review.
        if(mc.screen!=null||mc.getOverlay()!=null)return;
        if(scene==10&&arcingObserverReady(mc))lastArcingObserverWorldFrame=renderFrame;
        boolean ready=scene==1?liveNitorGeometry(mc):scene==31?mineGeometryReady(mc):scene==34?frothGeometryReady(mc):toolGeometryReady(mc);
        if(ready&&(scene!=10||arcingObserverReady(mc)))lastNitorWorldFrame=renderFrame;
    }
    @SubscribeEvent public static void rendered(TickEvent.RenderTickEvent event){
        if(!enabled()||stopped)return;
        if(event.phase==TickEvent.Phase.START){renderFrame++;return;}
        if(!captureRequested||captured)return;var mc=Minecraft.getInstance();
        try{
            if(scene==1&&(lastNitorWorldFrame!=renderFrame||!liveNitorGeometry(mc)))return;
            if(scene>=7&&scene<=11&&(lastNitorWorldFrame!=renderFrame||!toolGeometryReady(mc)))return;
            if(scene==10&&(lastArcingObserverWorldFrame!=renderFrame||!arcingObserverReady(mc)))return;
            if(scene==31&&(lastNitorWorldFrame!=renderFrame||!mineGeometryReady(mc)))return;
            if(scene==34&&(lastNitorWorldFrame!=renderFrame||!frothGeometryReady(mc)))return;
            if((scene==7||scene==8)&&lastSoapPlayerRenderFrame!=renderFrame)return;
            if(scene>=48&&scene<=49&&(lastSoapPlayerRenderFrame!=renderFrame||cleansingCount(false)==0))return;
            if(scene>=51&&cleansingCount(true)==0)return;
            require(mc.getWindow().getScreenWidth()==1600&&mc.getWindow().getScreenHeight()==1000,"Expected native1600x1000 framebuffer");
            captured=true;captureRequested=false;stable=0;String name="tc6-visual-fx-"+IMAGES[scene]+".png";var file=new File(new File(mc.gameDirectory,"screenshots"),name);require(!file.exists(),"Preserve native frame "+file);
            var facts=FxEngine.snapshot().stream().limit(35).map(p->p.getClass().getSimpleName()+":grid"+p.grid+":start"+p.start+":layer"+p.layer+":age"+p.age()+"/"+p.lifespan()+":scale"+Arrays.toString(p.scales)+":pos"+p.position()).toList();
            LogUtils.getLogger().info("THAUMCRAFT_VISUAL_FX_NATIVE_FRAME: image={} graphics={} camera={} cameraType={} yaw={} pitch={} shift={} tick={} actionTick={} actor={} shieldActive={} shieldRendered={} expected={} received={} before={} engineActive={} emitted={} fixtures={} particleFacts={} cleansingFacts={}",name,mc.options.graphicsMode().get(),mc.gameRenderer.getMainCamera().getPosition(),mc.options.getCameraType(),mc.player.getYRot(),mc.player.getXRot(),mc.player.isShiftKeyDown(),mc.level.getGameTime(),actionTick,actor(mc),FxEffects.activeShieldCount(),FxEffects.renderedShieldCount(),expected,FxEffects.receivedCount(expected),fxBefore,FxEngine.activeCount(),FxEngine.emitted(),fixtureFacts,facts,scene>=48?cleansingBubbles().stream().limit(35).map(Object::toString).toList():List.of());
            if(scene==34)logFrothGeometry(mc);
            if(scene==10)LogUtils.getLogger().info("THAUMCRAFT_VISUAL_FX_NATIVE_OBSERVER: cameraEntity={} cameraYaw={} cameraPitch={} nativeFov={} frame={} renderedWorldFrame={} casterPosition={} nativeSlashStillAge3To6",mc.getCameraEntity(),mc.gameRenderer.getMainCamera().getYRot(),mc.gameRenderer.getMainCamera().getXRot(),mc.options.fov().get(),renderFrame,lastArcingObserverWorldFrame,mc.player.position());
            Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),message->{if(file.isFile()&&file.length()>0){SAVED.incrementAndGet();LogUtils.getLogger().info("THAUMCRAFT_VISUAL_FX_CLIENT_SMOKE_IMAGE: {}",file.getAbsolutePath());}else mc.execute(()->fail(mc,new AssertionError("Screenshot absent: "+name)));});
        }catch(Throwable error){fail(mc,error);}
    }
    private static void start(Minecraft mc){
        if(mc.screen instanceof AccessibilityOnboardingScreen){mc.options.onboardAccessibility=false;mc.options.save();mc.setScreen(new TitleScreen());return;}
        if(!(mc.screen instanceof TitleScreen)||mc.getOverlay()!=null)return;started=true;startedAt=System.currentTimeMillis();tutorial=mc.options.tutorialStep;mc.options.tutorialStep=TutorialSteps.NONE;mc.getTutorial().stop();
        mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(4);mc.options.simulationDistance().set(5);mc.options.guiScale().set(2);mc.options.fov().set(70);mc.options.bobView().set(false);mc.options.cloudStatus().set(CloudStatus.OFF);mc.options.graphicsMode().set(GraphicsStatus.FANCY);mc.options.particles().set(ParticleStatus.ALL);mc.options.setCameraType(CameraType.FIRST_PERSON);mc.options.hideGui=true;mc.resizeDisplay();
        var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);rules.getRule(GameRules.RULE_DAYLIGHT).set(false,null);rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false,null);rules.getRule(GameRules.RULE_RANDOMTICKING).set(0,null);
        mc.createWorldOpenFlows().createFreshLevel(WORLD,new LevelSettings(WORLD,GameType.SURVIVAL,false,Difficulty.NORMAL,true,rules,WorldDataConfiguration.DEFAULT),new WorldOptions(0x544336041F1L,false,false),WorldPresets::createNormalWorldDimensions);
    }
    private static void finish(Minecraft mc){
        if(phase==0){phase=1;submit(mc,()->{var p=player(mc);require(p.isAlive()&&p.serverLevel().getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)==false,"Owned fixture player/world invalid");verified=true;});return;}if(!verified)return;
        require(SAVED.get()==IMAGES.length,"Native frame set incomplete");stopped=true;restore(mc);
        LogUtils.getLogger().info("THAUMCRAFT_VISUAL_FX_RENDER_AUDIT_OK: 53 captures/nativeNitor/runicCowDamageAndExpiry/swordUse/shovelPlacement/arcingAttack/collectorDrops/eightTwoPhaseFocusApplyImpacts/rawCompiledNativeMedia/naturalCrucibleHeatAndConsume/twoThrowableFlights/FancyFabulousConcealedSealControls/ghostGolem/nativeFullUseSoapDebit/natural200TickSaltExpiryAndTwoBathBubbleGenerations/noInjectedEffectsAgesClocksRng/actualPixelsRequireReview");
        LogUtils.getLogger().info("THAUMCRAFT_VISUAL_FX_CLIENT_SMOKE_OK: 53 captures/rawQAFixturesNotPaidProgression,world={}",WORLD);mc.stop();
    }
    private static void restore(Minecraft mc){mc.options.keyShift.setDown(false);mc.options.keyUse.setDown(false);if(mc.player!=null&&mc.getCameraEntity()!=mc.player)mc.setCameraEntity(mc.player);if(tutorial!=null)mc.options.tutorialStep=tutorial;}
    private static void fail(Minecraft mc,Throwable error){if(stopped)return;stopped=true;restore(mc);LogUtils.getLogger().error("THAUMCRAFT_VISUAL_FX_CLIENT_SMOKE_FAILED",error);mc.stop();}
}
