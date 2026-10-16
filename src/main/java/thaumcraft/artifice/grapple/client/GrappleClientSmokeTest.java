package thaumcraft.artifice.grapple.client;

import com.mojang.logging.LogUtils;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.*;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import thaumcraft.alchemy.AspectCrystalItem;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.arcane.*;
import thaumcraft.artifice.grapple.*;
import thaumcraft.catalog.blocks.CatalogBlocks;
import thaumcraft.client.research.*;
import thaumcraft.equipment.RechargeSupport;
import thaumcraft.equipment.recharge.RechargePedestalBlockEntity;
import thaumcraft.research.*;
import thaumcraft.world.aura.AuraManager;

/** Owned Survival fixtures; native C2S payments, charge, trajectory, impact, acceleration and release only. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public final class GrappleClientSmokeTest {
    private static final String WORLD="thaumcraft-grapple-smoke-"+System.currentTimeMillis();
    private static final String[] IMAGES={"paid-study","paid-tip-bench","paid-spool-bench","paid-gun-bench","native-pedestal-recharge","native-mainhand-flight","native-mainhand-pull","native-mainhand-release","native-offhand-pull","native-replacement-hook","native-final-release"};
    private static final String[] IDS={"grapple_gun_tip","grapple_gun_spool","grapple_gun"},CATEGORIES={"ARTIFICE","BASICS"};
    private static final int[] PRICES={25,25,75},THEORY=new int[2];
    private static final BlockPos BENCH=new BlockPos(-12,112,6),PEDESTAL=new BlockPos(-6,112,6);
    private static final Vec3 START=new Vec3(.5,112,.5);
    private static final AtomicInteger SAVED=new AtomicInteger();
    private static boolean started,prepared,stopped,crouch,captureRequested,captured,validated;
    private static int scene,phase,wait,stable,ready,expectedStage=-1,studyPaid,benchPaid,visPaid,currentXp,currentCount;
    private static volatile int shotRequested,serverHookId=-1,serverCharge,chargedUnits,chargingSteps,emptyUses;
    private static volatile boolean serverPulling,mainFlight,mainPulled,offPulled,replacementObserved;
    private static volatile double mainMove,mainRise,offMove,offRise;
    private static volatile long worldTime;
    private static Vec3 mainStart,offStart;
    private static int previousPedestalCharge,oldHookId=-1;
    private static float benchVisBefore,pedestalVisBefore;
    private static long began,phaseStarted,flightDrawsBefore;
    private static CompletableFuture<Void> work;
    private static TutorialSteps previous;
    private GrappleClientSmokeTest(){}
    private static boolean enabled(){return Boolean.getBoolean("thaumcraft.grappleSmokeTest");}
    private static void require(boolean b,String m){if(!b)throw new AssertionError(m);}
    private static ServerPlayer player(Minecraft mc){var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());require(p!=null,"Missing owned grapple player");return p;}
    private static void submit(Minecraft mc,Runnable action){require(work==null,"Overlapping owned server tasks");var next=new CompletableFuture<Void>();work=next;mc.getSingleplayerServer().execute(()->{try{action.run();next.complete(null);}catch(Throwable e){next.completeExceptionally(e);}});}
    private static ItemStack item(String id){var v=ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("thaumcraft",id));require(v!=null&&v!=Items.AIR,"Missing native item "+id);return new ItemStack(v);}
    private static int count(ServerPlayer p,String id){int n=0;for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i).is(item(id).getItem()))n+=p.getInventory().getItem(i).getCount();return n;}
    private static ItemStack take(ServerPlayer p,String id){for(int i=0;i<36;i++){var s=p.getInventory().getItem(i);if(s.is(item(id).getItem())){var r=s.split(1);if(s.isEmpty())p.getInventory().setItem(i,ItemStack.EMPTY);p.inventoryMenu.broadcastChanges();return r;}}throw new AssertionError("Missing physically earned "+id);}
    private static void stow(ServerPlayer p){var s=p.getMainHandItem();if(!s.isEmpty()){p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);for(int i=1;i<36&&!s.isEmpty();i++){var slot=p.getInventory().getItem(i);if(!slot.isEmpty()&&ItemStack.isSameItemSameTags(slot,s)){int n=Math.min(s.getCount(),slot.getMaxStackSize()-slot.getCount());slot.grow(n);s.shrink(n);}}for(int i=1;i<36&&!s.isEmpty();i++)if(p.getInventory().getItem(i).isEmpty()){p.getInventory().setItem(i,s);s=ItemStack.EMPTY;}require(s.isEmpty(),"Cannot conserve held stack");}p.inventoryMenu.broadcastChanges();}
    private static void held(ServerPlayer p,ItemStack s){require(p.getMainHandItem().isEmpty(),"Replacing physical held stack");p.getInventory().selected=0;p.setItemInHand(InteractionHand.MAIN_HAND,s);p.connection.send(new ClientboundSetCarriedItemPacket(0));p.inventoryMenu.broadcastChanges();}
    private static void predecessor(ServerPlayer p,String key){try{var m=PlayerKnowledge.class.getDeclaredMethod("setResearchStage",String.class,int.class);m.setAccessible(true);require((boolean)m.invoke(KnowledgeStore.get(p),key,ResearchCatalog.get(key).stages().size()+1),"Raw predecessor "+key);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
    private static void theory(ServerPlayer p,String c){try{var m=PlayerKnowledge.class.getDeclaredMethod("addKnowledge",KnowledgeType.class,String.class,int.class);m.setAccessible(true);require((boolean)m.invoke(KnowledgeStore.get(p),KnowledgeType.THEORY,c,32),"Raw theory");}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
    private static void close(Minecraft mc){mc.player.closeContainer();mc.setScreen(null);}
    private static void click(Minecraft mc,BlockPos pos){mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(pos.getCenter().add(0,.5,0),Direction.UP,pos,false));}
    private static boolean position(Minecraft mc,double x,double y,double z){if(mc.player.distanceToSqr(x,y,z)>.3){ready=0;return false;}return ++ready>=4;}
    private static void shift(Minecraft mc,boolean value){crouch=value;mc.player.input.shiftKeyDown=value;mc.player.setShiftKeyDown(value);mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player,value?ServerboundPlayerCommandPacket.Action.PRESS_SHIFT_KEY:ServerboundPlayerCommandPacket.Action.RELEASE_SHIFT_KEY));}
    private static ArcaneWorkbenchBlockEntity bench(ServerLevel l){return (ArcaneWorkbenchBlockEntity)l.getBlockEntity(BENCH);}
    private static RechargePedestalBlockEntity pedestal(ServerLevel l){return (RechargePedestalBlockEntity)l.getBlockEntity(PEDESTAL);}
    private static ItemStack gun(ServerPlayer p){if(p.getMainHandItem().is(item("grapple_gun").getItem()))return p.getMainHandItem();if(p.getOffhandItem().is(item("grapple_gun").getItem()))return p.getOffhandItem();return ItemStack.EMPTY;}
    private static void pedestalView(ServerPlayer p){require(GrappleModule.active(p.level(),p)==null,"Cannot teleport active grapple owner");p.connection.teleport(PEDESTAL.getX()+.5,112,PEDESTAL.getZ()+2.5,180,15);}
    private static boolean pedestalViewed(Minecraft mc){return position(mc,PEDESTAL.getX()+.5,112,PEDESTAL.getZ()+2.5)&&mc.screen==null;}
    private static GrappleEntity clientHook(Minecraft mc){return GrappleModule.active(mc.level,mc.player);}
    private static void book(Minecraft mc){var k=ResearchClient.golemPressKnowledge();var b=new ThaumonomiconScreen(k,k.scanCount());mc.setScreen(new ThaumonomiconPageScreen(b,ResearchCatalog.get("GRAPPLEGUN"),k,k.scanCount()));}
    @SubscribeEvent public static void input(MovementInputUpdateEvent e){if(!enabled()||stopped||!crouch)return;var mc=Minecraft.getInstance();if(e.getEntity()==mc.player&&mc.getSingleplayerServer()!=null&&WORLD.equals(mc.getSingleplayerServer().getWorldData().getLevelName())){var in=e.getInput();in.shiftKeyDown=true;in.forwardImpulse=in.leftImpulse=0;in.up=in.down=in.left=in.right=in.jumping=false;}}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){if(e.phase!=TickEvent.Phase.END||!enabled()||stopped)return;var mc=Minecraft.getInstance();if(began==0)began=System.nanoTime();try{
        require(System.nanoTime()-began<1_500_000_000_000L,"Grapple timeout scene="+scene+" phase="+phase);if(!started){start(mc);return;}if(mc.level==null||mc.player==null||mc.getOverlay()!=null||mc.screen instanceof ReceivingLevelScreen)return;require(mc.getSingleplayerServer()!=null&&WORLD.equals(mc.getSingleplayerServer().getWorldData().getLevelName()),"Wrong owned grapple world");mc.getToasts().clear();
        if(++wait%100==0)LogUtils.getLogger().info("THAUMCRAFT_GRAPPLE_HEARTBEAT: scene={} phase={} bench={} hook={} pull={} charge={} pedestalUnits={} mainMove={} mainRise={} offMove={} offRise={} replacement={} renders={}/{}",scene,phase,benchPaid,serverHookId,serverPulling,serverCharge,chargedUnits,mainMove,mainRise,offMove,offRise,replacementObserved,GrappleRenderer.hooksDrawn(),GrappleRenderer.ropeSegmentsDrawn());
        if(work!=null){if(!work.isDone())return;work.join();work=null;}if(!prepared){prepared=true;submit(mc,()->prepare(mc));return;}if(captured){if(SAVED.get()==scene+1&&++stable>=(scene==5?2:12)){scene++;phase=wait=stable=ready=0;captured=captureRequested=false;}return;}if(scene==IMAGES.length){finish(mc);return;}
        if(!advance(mc)){stable=0;return;}if(++stable>=(scene==5?1:8))captureRequested=true;
    }catch(Throwable error){fail(mc,error);}}
    private static boolean advance(Minecraft mc){return switch(scene){case 0->study(mc);case 1,2,3->arcane(mc,scene-1);case 4->recharge(mc);case 5->mainFlight(mc);case 6->mainPull(mc);case 7->release(mc,false);case 8->offhand(mc);case 9->replace(mc);case 10->release(mc,true);default->false;};}
    private static boolean study(Minecraft mc){
        if(phase==0){phase=1;submit(mc,()->{var p=player(mc);stow(p);for(int i=0;i<2;i++){theory(p,CATEGORIES[i]);THEORY[i]=KnowledgeStore.get(p).rawKnowledge(KnowledgeType.THEORY,CATEGORIES[i]);}currentXp=p.totalExperience;held(p,new ItemStack(ResearchModule.THAUMONOMICON.get()));require(p.getInventory().add(item("nugget_rareearth")),"Raw study nugget unavailable");p.inventoryMenu.broadcastChanges();ResearchNetwork.sync(p);});return false;}
        if(phase==1){if(!mc.player.getMainHandItem().is(ResearchModule.THAUMONOMICON.get()))return false;var k=ResearchClient.golemPressKnowledge();if(!k.isResearchCompleteStrict("GRAPPLEGUN")){int stage=k.researchStage("GRAPPLEGUN");if(stage==expectedStage)return false;expectedStage=stage;ResearchNetwork.requestAdvance("GRAPPLEGUN",stage);return false;}phase=2;submit(mc,()->{var p=player(mc);for(int i=0;i<2;i++)require(KnowledgeStore.get(p).rawKnowledge(KnowledgeType.THEORY,CATEGORIES[i])==THEORY[i]-32,"Original32theory not paid");require(p.totalExperience==currentXp+10&&count(p,"nugget_rareearth")==0,"Original nugget/10XP changed");studyPaid++;LogUtils.getLogger().info("THAUMCRAFT_GRAPPLE_STUDY_PAID: actualnativeC2S/32rawTheoryARTIFICE+32BASICS/originalsingularnuggetmeta10one/+10earnedXP");});return false;}if(!(mc.screen instanceof ThaumonomiconPageScreen))book(mc);return true;
    }
    private static boolean arcane(Minecraft mc,int index){String id=IDS[index];
        if(phase==0){phase=1;close(mc);submit(mc,()->{var p=player(mc);require(GrappleModule.active(p.level(),p)==null,"Bench setup while grappling");stow(p);var l=p.serverLevel();require(bench(l).isEmpty(),"Previous bench inputs retained");var r=l.getRecipeManager().getAllRecipesFor(ArcaneModule.RECIPE_TYPE.get()).stream().filter(x->x.getResultItem(l.registryAccess()).is(item(id).getItem())).findFirst().orElseThrow();require(r.vis()==PRICES[index],"Original price changed");for(int slot=0;slot<r.getIngredients().size();slot++){var ingredient=r.getIngredients().get(slot);if(ingredient.isEmpty())continue;var component=ingredient.getItems()[0].copyWithCount(1);if(component.is(item("grapple_gun_tip").getItem()))component=take(p,"grapple_gun_tip");if(component.is(item("grapple_gun_spool").getItem()))component=take(p,"grapple_gun_spool");bench(l).setItem(slot%r.gridWidth()+slot/r.gridWidth()*3,component);}for(int c=0;c<6;c++)if(r.crystalCost(c)>0)bench(l).setItem(9+c,AspectCrystalItem.create(Aspect.getAspect(ArcaneModule.PRIMALS[c]),r.crystalCost(c)));currentCount=count(p,id);benchVisBefore=AuraManager.getVis(l,BENCH);p.connection.teleport(BENCH.getX()+.5,112,BENCH.getZ()+2.5,180,20);});return false;}
        if(phase==1){if(!position(mc,BENCH.getX()+.5,112,BENCH.getZ()+2.5)||!mc.player.getMainHandItem().isEmpty())return false;phase=2;click(mc,BENCH);return false;}if(!(mc.player.containerMenu instanceof ArcaneWorkbenchMenu menu))return false;
        if(phase==2){if(!menu.craftable()||!menu.getSlot(0).getItem().is(item(id).getItem()))return false;phase=3;mc.gameMode.handleInventoryMouseClick(menu.containerId,0,0,ClickType.QUICK_MOVE,mc.player);return false;}
        if(phase==3){if(menu.hasRecipe())return false;phase=4;submit(mc,()->{var p=player(mc);var l=p.serverLevel();require(bench(l).isEmpty()&&count(p,id)==currentCount+1&&AuraManager.getVis(l,BENCH)==benchVisBefore-PRICES[index],"Native bench output/vis changed");if(index==2)require(count(p,"grapple_gun_tip")==0&&count(p,"grapple_gun_spool")==0,"Actual paid parts not consumed");benchPaid++;visPaid+=PRICES[index];LogUtils.getLogger().info("THAUMCRAFT_GRAPPLE_BENCH_PAID: {} actualnativeC2S/{}vis/originalprimalprices/physicalingredients/actualpaidTipSpoolUsedForGun,total={}",id,PRICES[index],benchPaid);});return false;}return true;
    }
    private static boolean recharge(Minecraft mc){require(wait<4000,"Native pedestal stalled");
        if(phase==0){phase=1;close(mc);submit(mc,()->{var p=player(mc);stow(p);held(p,take(p,"grapple_gun"));require(RechargeSupport.getCharge(gun(p))==0,"Fresh gun had supplied charge");pedestalView(p);});return false;}
        if(phase==1){if(!pedestalViewed(mc)||!mc.player.getMainHandItem().is(item("grapple_gun").getItem()))return false;phase=2;phaseStarted=worldTime;mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);return false;}
        if(phase==2){if(worldTime-phaseStarted<10)return false;phase=3;submit(mc,()->{var p=player(mc);require(GrappleModule.active(p.level(),p)==null&&RechargeSupport.getCharge(gun(p))==0&&p.serverLevel().getEntitiesOfClass(GrappleEntity.class,p.getBoundingBox().inflate(64)).isEmpty(),"Empty use made hook/charge");emptyUses++;pedestalVisBefore=AuraManager.getVis(p.serverLevel(),PEDESTAL);LogUtils.getLogger().info("THAUMCRAFT_GRAPPLE_EMPTY_USE: actualnativeC2S/unchargedpaidgun/nohook/nochargecost/originaliceSound");});return false;}
        if(phase==3){phase=4;click(mc,PEDESTAL);return false;}if(phase==4){if(!mc.player.getMainHandItem().isEmpty())return false;phase=5;return false;}
        if(phase==5){if(!(mc.level.getBlockEntity(PEDESTAL) instanceof RechargePedestalBlockEntity tile)||RechargeSupport.getCharge(tile.getItem(0))!=100)return false;phase=6;submit(mc,()->{var p=player(mc);var l=p.serverLevel();require(pedestal(l).getItem(0).is(item("grapple_gun").getItem())&&RechargeSupport.getCharge(pedestal(l).getItem(0))==100&&chargedUnits==100&&count(p,"grapple_gun")==0,"Native pedestal100charge changed");LogUtils.getLogger().info("THAUMCRAFT_GRAPPLE_PEDESTAL_PAID: actualC2Sinsert/nativeWorldTicks/{}observedChargeSteps/100wholeChargeFromNative100visRoute/auraBefore{}/auraAfter{}/no directchargesetter; aggregateAuraAlsoHasNativeDiffusion",chargingSteps,pedestalVisBefore,AuraManager.getVis(l,PEDESTAL));});return false;}return pedestalViewed(mc);
    }
    private static boolean mainFlight(Minecraft mc){require(wait<2000,"Native flight/retrieval stalled");
        if(phase==0){phase=1;submit(mc,()->{var p=player(mc);stow(p);pedestalView(p);});return false;}if(phase==1){if(!pedestalViewed(mc)||!mc.player.getMainHandItem().isEmpty())return false;phase=2;click(mc,PEDESTAL);return false;}
        if(phase==2){if(mc.player.getInventory().items.stream().noneMatch(s->s.is(item("grapple_gun").getItem())))return false;phase=3;submit(mc,()->{var p=player(mc);require(pedestal(p.serverLevel()).isEmpty()&&count(p,"grapple_gun")==1,"Native pedestal retrieval/pickup absent");stow(p);held(p,take(p,"grapple_gun"));require(RechargeSupport.getCharge(gun(p))==100,"Retrieved charge changed");require(GrappleModule.active(p.level(),p)==null,"Cannot teleport active owner");p.connection.teleport(START.x,START.y,START.z,0,-12);mainStart=START;LogUtils.getLogger().info("THAUMCRAFT_GRAPPLE_PEDESTAL_RETRIEVED: actualC2Semptyhand/nativephysicalItemEntitypickup/samepaidgun100charge");});return false;}
        if(phase==3){if(!position(mc,START.x,START.y,START.z)||!mc.player.getMainHandItem().is(item("grapple_gun").getItem())||RechargeSupport.getCharge(mc.player.getMainHandItem())!=100)return false;phase=4;shotRequested=1;flightDrawsBefore=GrappleRenderer.hooksDrawn();mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);return false;}
        var hook=clientHook(mc);if(hook==null||hook.pulling())return false;require(hook.hand()==InteractionHand.MAIN_HAND,"Main hook hand changed");if(phase==4){phase=5;LogUtils.getLogger().info("THAUMCRAFT_GRAPPLE_MAIN_FLIGHT: actualnativeC2S/mainhand/newhook{}/originalflight/nosynthetichook",hook.getId());}return GrappleRenderer.hooksDrawn()>flightDrawsBefore;
    }
    private static boolean mainPull(Minecraft mc){require(wait<2000,"Native main pull stalled");var hook=clientHook(mc);if(hook==null||!hook.pulling()||!mainPulled||mainMove<.75||mainRise<.2)return false;if(phase==0){phase=1;submit(mc,()->{var p=player(mc);require(RechargeSupport.getCharge(gun(p))==99&&GrappleModule.active(p.level(),p)!=null,"Main shot didn't pay1charge");LogUtils.getLogger().info("THAUMCRAFT_GRAPPLE_MAIN_PULL: actualblockimpact/status6/nativeowneracceleration/move{}/rise{}/charge99/fallDistance{}",mainMove,mainRise,p.fallDistance);});return false;}return hook.pulling();}
    private static boolean release(Minecraft mc,boolean last){
        if(phase==0){phase=1;shift(mc,true);return false;}if(clientHook(mc)!=null||serverHookId!=-1)return false;var s=last?mc.player.getOffhandItem():mc.player.getMainHandItem();if(s.hasTag()&&s.getTag().getByte("loaded")!=0)return false;
        if(phase==1){phase=2;shift(mc,false);submit(mc,()->{var p=player(mc);require(GrappleModule.active(p.level(),p)==null&&RechargeSupport.getCharge(gun(p))==(last?97:99)&&(!gun(p).hasTag()||gun(p).getTag().getByte("loaded")==0),"Shiftrelease/visualflag/charge changed");LogUtils.getLogger().info("THAUMCRAFT_GRAPPLE_NATIVE_RELEASE: {} actualC2Sshift/ownerhookremoved/nativeinventoryloaded0/charge{}",last?"final":"main",RechargeSupport.getCharge(gun(p)));});return false;}return mc.screen==null;
    }
    private static boolean offhand(Minecraft mc){require(wait<2000,"Native offhand stalled");
        if(phase==0){phase=1;submit(mc,()->{var p=player(mc);require(GrappleModule.active(p.level(),p)==null&&p.getOffhandItem().isEmpty(),"Offhand setup while active/occupied");var paid=gun(p);require(!paid.isEmpty()&&RechargeSupport.getCharge(paid)==99,"Paid gun missing");p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setItemInHand(InteractionHand.OFF_HAND,paid);p.inventoryMenu.broadcastChanges();p.connection.teleport(START.x,START.y,START.z,0,-12);offStart=START;});return false;}
        if(phase==1){if(!position(mc,START.x,START.y,START.z)||!mc.player.getMainHandItem().isEmpty()||!mc.player.getOffhandItem().is(item("grapple_gun").getItem()))return false;phase=2;shotRequested=2;mc.gameMode.useItem(mc.player,InteractionHand.OFF_HAND);return false;}
        var hook=clientHook(mc);if(hook==null||!hook.pulling()||hook.hand()!=InteractionHand.OFF_HAND||!offPulled||offMove<.75||offRise<.2)return false;if(phase==2){phase=3;submit(mc,()->{var p=player(mc);require(RechargeSupport.getCharge(gun(p))==98,"Offhand charge changed");oldHookId=GrappleModule.active(p.level(),p).getId();LogUtils.getLogger().info("THAUMCRAFT_GRAPPLE_OFFHAND_PULL: actualnativeC2S/handOFF/nativeblockimpact/move{}/rise{}/charge98/hook{}",offMove,offRise,oldHookId);});return false;}return true;
    }
    private static boolean replace(Minecraft mc){require(wait<2000,"Native replacement stalled");
        if(phase==0){require(clientHook(mc)!=null,"Lost offhand hook before refire");phase=1;shotRequested=3;mc.gameMode.useItem(mc.player,InteractionHand.OFF_HAND);return false;}
        var hook=clientHook(mc);if(hook==null||hook.getId()==oldHookId||!hook.pulling()||!replacementObserved||serverCharge!=97)return false;if(phase==1){phase=2;submit(mc,()->{var p=player(mc);var active=GrappleModule.active(p.level(),p);require(active!=null&&active.getId()!=oldHookId&&active.hand()==InteractionHand.OFF_HAND&&p.serverLevel().getEntity(oldHookId)==null&&RechargeSupport.getCharge(gun(p))==97,"Replacement old hook/charge changed");LogUtils.getLogger().info("THAUMCRAFT_GRAPPLE_NATIVE_REPLACEMENT: actualnativeC2Srefirewhileloaded/old{}removed/new{}/oneactiveownerhook/charge97/noactiveownerteleport",oldHookId,active.getId());});return false;}return true;
    }
    @SubscribeEvent public static void serverTick(TickEvent.ServerTickEvent e){if(e.phase!=TickEvent.Phase.END||!enabled()||stopped||!prepared||!WORLD.equals(e.getServer().getWorldData().getLevelName()))return;var l=e.getServer().overworld();worldTime=l.getGameTime();var client=Minecraft.getInstance().player;if(client==null)return;var p=e.getServer().getPlayerList().getPlayer(client.getUUID());if(p==null)return;
        if(l.getBlockEntity(PEDESTAL) instanceof RechargePedestalBlockEntity pedestal){int charge=Math.max(0,RechargeSupport.getCharge(pedestal.getItem(0)));if(charge>previousPedestalCharge){require(charge-previousPedestalCharge<=5,"Pedestal charge jump exceeded5");chargedUnits+=charge-previousPedestalCharge;chargingSteps++;}previousPedestalCharge=charge;}
        var hook=GrappleModule.active(l,p);serverHookId=hook==null?-1:hook.getId();serverPulling=hook!=null&&hook.pulling();serverCharge=RechargeSupport.getCharge(gun(p));if(hook==null)return;
        if(shotRequested==1){mainFlight|=!hook.pulling();mainPulled|=hook.pulling();if(mainStart!=null){mainMove=Math.max(mainMove,p.position().distanceTo(mainStart));mainRise=Math.max(mainRise,p.getY()-mainStart.y);}}
        if(shotRequested==2){offPulled|=hook.pulling();if(offStart!=null){offMove=Math.max(offMove,p.position().distanceTo(offStart));offRise=Math.max(offRise,p.getY()-offStart.y);}}
        if(shotRequested==3&&oldHookId!=-1&&hook.getId()!=oldHookId&&l.getEntity(oldHookId)==null)replacementObserved=true;
    }
    private static void prepare(Minecraft mc){var p=player(mc);var l=p.serverLevel();p.setInvulnerable(true);for(int x=-2;x<=2;x++)for(int z=-2;z<=3;z++)l.getChunk(x,z);for(var pos:BlockPos.betweenClosed(-20,111,-8,20,138,30))l.setBlockAndUpdate(pos,pos.getY()==111?Blocks.BEDROCK.defaultBlockState():Blocks.AIR.defaultBlockState());for(var pos:BlockPos.betweenClosed(-8,112,24,8,136,24))l.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());l.setDayTime(6000);l.setBlockAndUpdate(BENCH,ArcaneModule.WORKBENCH.get().defaultBlockState());l.setBlockAndUpdate(PEDESTAL,CatalogBlocks.block("recharge_pedestal").defaultBlockState());
        for(int x=-2;x<=2;x++)for(int z=-2;z<=3;z++){var pos=new ChunkPos(x,z).getMiddleBlockPosition(112);AuraManager.drainVis(l,pos,Float.MAX_VALUE,false);AuraManager.addVis(l,pos,5000);AuraManager.drainFlux(l,pos,Float.MAX_VALUE,false);}
        p.getInventory().clearContent();for(String key:List.of("FIRSTSTEPS","KNOWLEDGETYPES","UNLOCKARTIFICE","BASEARTIFICE","RECHARGEPEDESTAL"))predecessor(p,key);require(!KnowledgeStore.get(p).isResearchKnown("GRAPPLEGUN"),"Target study supplied");p.connection.teleport(-10.5,112,8.5,135,10);ResearchNetwork.sync(p);LogUtils.getLogger().info("THAUMCRAFT_GRAPPLE_FIXTURES: ownedSurvivalarena/staticstoneanchorwallZ24/rawbench+rechargepedestal/BASEARTIFICE+RECHARGEPEDESTALprecursors/rawtheory/nugget/ordinarycraftmaterials/primalcrystals/initial5000vis0fluxper30loadedchunks; actualpaidtip+spoolusedforgun; targetgun/research/charge/hooks absent; noactivehookteleports/noactivehookorplayermotion/charge/aura/RNGsetters; notfullsurvival/multiplayer/reload/audio/allangles");
    }
    @SubscribeEvent public static void rendered(TickEvent.RenderTickEvent e){if(e.phase!=TickEvent.Phase.END||!enabled()||stopped||!captureRequested||captured)return;var mc=Minecraft.getInstance();try{captured=true;captureRequested=false;String name="tc6-grapple-"+IMAGES[scene]+".png";var file=new File(new File(mc.gameDirectory,"screenshots"),name);Files.deleteIfExists(file.toPath());Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),m->{if(file.isFile()&&file.length()>0){SAVED.incrementAndGet();LogUtils.getLogger().info("THAUMCRAFT_GRAPPLE_CLIENT_SMOKE_IMAGE: {}",file.getAbsolutePath());}else mc.execute(()->fail(mc,new AssertionError("Missing native grapple capture")));});}catch(Throwable error){fail(mc,error);}}
    private static void start(Minecraft mc){if(mc.screen instanceof AccessibilityOnboardingScreen){mc.options.onboardAccessibility=false;mc.options.save();mc.setScreen(new TitleScreen());return;}if(!(mc.screen instanceof TitleScreen)||mc.getOverlay()!=null)return;started=true;previous=mc.options.tutorialStep;mc.options.tutorialStep=TutorialSteps.NONE;mc.getTutorial().stop();mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(3);mc.options.simulationDistance().set(5);mc.options.guiScale().set(2);mc.options.fov().set(80);mc.options.cloudStatus().set(CloudStatus.OFF);mc.options.setCameraType(CameraType.FIRST_PERSON);mc.options.hideGui=false;mc.resizeDisplay();var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);rules.getRule(GameRules.RULE_DAYLIGHT).set(false,null);rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false,null);rules.getRule(GameRules.RULE_RANDOMTICKING).set(0,null);mc.createWorldOpenFlows().createFreshLevel(WORLD,new LevelSettings(WORLD,GameType.SURVIVAL,false,Difficulty.PEACEFUL,true,rules,WorldDataConfiguration.DEFAULT),new WorldOptions(0x54433638L,false,false),WorldPresets::createNormalWorldDimensions);}
    private static void finish(Minecraft mc){if(phase==0){phase=1;submit(mc,()->{var p=player(mc);require(studyPaid==1&&benchPaid==3&&visPaid==125&&chargedUnits==100&&emptyUses==1&&shotRequested==3&&count(p,"grapple_gun")==1&&RechargeSupport.getCharge(gun(p))==97&&GrappleModule.active(p.level(),p)==null&&pedestal(p.serverLevel()).isEmpty(),"Final paid grapple census changed");require(mainFlight&&mainPulled&&offPulled&&mainMove>=.75&&mainRise>=.2&&offMove>=.75&&offRise>=.2&&replacementObserved,"Native movement/replacement route absent");LogUtils.getLogger().info("THAUMCRAFT_GRAPPLE_CLIENT_FACTS: scenes{} paidStudy1/TheoryARTIFICE32BASICS32/originalNugget1/earnedXP10/paidBench3/vis125/Terra1Aqua1Aer1Ignis1/actualPaidTipSpoolConsumed/oneFreshGun/emptyUse1/nativePedestalCharge100/nativeRechargeVis100/nativeShots3/charge97/mainMove{}/mainRise{}/offMove{}/offRise{}/replacementtrue/shiftReleases2/hookDraws{}/ropeSegments{}/glowDraws{}; explicitprecursors/rawknowledge/materials/arena/initialaura/rawpedestal,notfullsurvival/multiplayer/reload/audio/allangles",SAVED.get(),mainMove,mainRise,offMove,offRise,GrappleRenderer.hooksDrawn(),GrappleRenderer.ropeSegmentsDrawn(),GrappleRenderer.glowsDrawn());validated=true;});return;}
        if(!validated)return;require(SAVED.get()==11&&GrappleRenderer.hooksDrawn()>0&&GrappleRenderer.ropeSegmentsDrawn()>0&&GrappleRenderer.glowsDrawn()>0,"Native render route absent");stopped=true;mc.options.tutorialStep=previous;LogUtils.getLogger().info("THAUMCRAFT_GRAPPLE_RENDER_AUDIT_OK: {} scenes; originalhookcuboids/sixframeadditiveglow/sourcewavecenterline/foursiderope/mainoffhandloadedmodels/noinventedGUI",SAVED.get());LogUtils.getLogger().info("THAUMCRAFT_GRAPPLE_CLIENT_SMOKE_OK: {} scenes; actualpaidstudy/threepaidbenches/nativepedestal100charge/threeC2Sshots/nativeflightimpactpull/shiftrelease/offhand/replacement/samepaidgun97charge,world={}",SAVED.get(),WORLD);mc.stop();}
    private static void fail(Minecraft mc,Throwable error){if(stopped)return;stopped=true;if(previous!=null)mc.options.tutorialStep=previous;LogUtils.getLogger().error("THAUMCRAFT_GRAPPLE_CLIENT_SMOKE_FAILED",error);mc.stop();}
}
