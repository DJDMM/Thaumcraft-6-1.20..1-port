package thaumcraft.artifice.mirror.client;

import com.mojang.logging.LogUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.function.Consumer;
import net.minecraft.client.CameraType;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import thaumcraft.alchemy.AspectCrystalItem;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.arcane.ArcaneModule;
import thaumcraft.arcane.ArcaneRecipe;
import thaumcraft.arcane.ArcaneWorkbenchBlockEntity;
import thaumcraft.arcane.ArcaneWorkbenchMenu;
import thaumcraft.artifice.mirror.AbstractMirrorBlockEntity;
import thaumcraft.artifice.mirror.HandMirrorMenu;
import thaumcraft.artifice.mirror.MirrorBlock;
import thaumcraft.artifice.mirror.MirrorLink;
import thaumcraft.catalog.blocks.CatalogBlocks;
import thaumcraft.client.research.ResearchClient;
import thaumcraft.client.research.ThaumonomiconPageScreen;
import thaumcraft.client.research.ThaumonomiconScreen;
import thaumcraft.essentia.EssentiaJarBlockEntity;
import thaumcraft.essentia.airborne.AirborneEssentiaManager;
import thaumcraft.essentia.transfuser.EssentiaTransfuserBlock;
import thaumcraft.essentia.transfuser.EssentiaTransfuserBlockEntity;
import thaumcraft.infusion.InfusionMatrixBlock;
import thaumcraft.infusion.InfusionMatrixBlockEntity;
import thaumcraft.infusion.InfusionModule;
import thaumcraft.infusion.InfusionPedestalBlockEntity;
import thaumcraft.infusion.InfusionRecipe;
import thaumcraft.infusion.InfusionRecipes;
import thaumcraft.infusion.InfusionStability;
import thaumcraft.research.KnowledgeStore;
import thaumcraft.research.KnowledgeType;
import thaumcraft.research.PlayerKnowledge;
import thaumcraft.research.ResearchCatalog;
import thaumcraft.research.ResearchModule;
import thaumcraft.research.ResearchNetwork;
import thaumcraft.scanning.ScanningModule;
import thaumcraft.world.aura.AuraManager;

/** Owned survival QA: native packets and ordinary world ticks pay every target output. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public final class MirrorClientSmokeTest {
    private static final String WORLD="thaumcraft-mirror-smoke-"+System.currentTimeMillis();
    private static final String[] IMAGES={"teleport-and-lux-scan","paid-lamp-research","paid-lamp-bench","paid-mirror-glass-bench","paid-mirror-research","paid-hand-research","paid-essentia-research","original-mirror-recipe","paid-item-mirror-altar","paid-hand-mirror-altar","paid-essentia-mirror-altar","native-bound-north-south","physical-item-teleport-output","original-one-input-hand-gui","physical-hand-mirror-output","native-remote-essentia-and-lamp","native-east-west-mounts","native-floor-ceiling-mounts","native-floor-mirror-closeup","native-ceiling-mirror-closeup"};
    private static final double VIEW_X=20.5,VIEW_Y=112,VIEW_Z=-9.5;
    private static final BlockPos BENCH=new BlockPos(-8,112,10),MATRIX=new BlockPos(-8,114,0),LAMP=new BlockPos(29,113,0),CONSUMER=new BlockPos(18,112,0),PUMP=CONSUMER.above(),SOURCE=new BlockPos(24,112,-2);
    private static final BlockPos[] MIRRORS={new BlockPos(12,113,0),new BlockPos(16,113,0),new BlockPos(20,113,0),new BlockPos(24,113,0)};
    private static final Direction[] INITIAL={Direction.NORTH,Direction.SOUTH,Direction.SOUTH,Direction.NORTH};
    private static final int[][] OFFSETS={{3,0},{0,3},{-3,0},{0,-3}};
    private static final List<Aspect> ASPECTS=List.of(Aspect.MOTION,Aspect.DARKNESS,Aspect.EXCHANGE,Aspect.WATER,Aspect.TOOL);
    private static final int[] ESSENTIA={175,75,125,50,50};
    private static final AtomicInteger SAVED=new AtomicInteger();
    private static final AtomicInteger REMOTE_LEG_PACKETS=new AtomicInteger(),LOCAL_LEG_PACKETS=new AtomicInteger();
    private static final AtomicIntegerArray FX_ENDPOINTS=new AtomicIntegerArray(4);
    private static volatile boolean watchEssentiaPackets;
    private static volatile String fxObserverFailure;
    private static boolean started,prepared,stopped,captureRequested,captured,finalValidated,arcaneFluxFixtureCleared,fxObserverInstalled;
    private static int scene,phase,stable,wait,ready,expectedStage=-1,scanIndex,arcaneCount,infusionCount,run,placementIndex,mountIndex,mountPhase,studyTheoryPaid,studyObservationPaid,studyXpPaid,lastCollectedScene=-1,worldObserve;
    private static int currentTheory,currentObservation,currentXp,currentItemCount,beforeJars,handCountBefore,itemOutput,handOutput,remoteSource,remoteDestination,glimmerCount,maxGlimmerLight;
    private static volatile boolean entityTransferred,handTransferred,essentiaTransferred;
    private static long began,actualRelocations;private static float beforeVis;
    private static CompletableFuture<Void> work;private static TutorialSteps previous;
    private MirrorClientSmokeTest(){}
    private static boolean enabled(){return Boolean.getBoolean("thaumcraft.mirrorSmokeTest");}
    private static void require(boolean valid,String message){if(!valid)throw new AssertionError(message);}
    private static ServerPlayer player(Minecraft mc){var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());require(p!=null,"Missing owned mirror player");return p;}
    private static void submit(Minecraft mc,Runnable action){require(work==null,"Overlapping mirror server tasks");var result=new CompletableFuture<Void>();work=result;mc.getSingleplayerServer().execute(()->{try{action.run();result.complete(null);}catch(Throwable e){result.completeExceptionally(e);}});}
    private static ItemStack item(String id){var i=ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("thaumcraft",id));require(i!=null&&i!=Items.AIR,"Missing mirror item "+id);return new ItemStack(i);}
    private static void held(ServerPlayer p,ItemStack value){require(p.getMainHandItem().isEmpty()||!target(p.getMainHandItem()),"Replacing a physically paid held target");p.getInventory().selected=0;p.setItemInHand(InteractionHand.MAIN_HAND,value);p.connection.send(new ClientboundSetCarriedItemPacket(0));p.inventoryMenu.broadcastChanges();}
    private static boolean target(ItemStack value){return List.of("mirror","mirror_essentia","hand_mirror","mirrored_glass","lamp_arcane").stream().anyMatch(id->value.is(item(id).getItem()));}
    private static void stow(ServerPlayer p){var value=p.getMainHandItem();if(!value.isEmpty()){
        p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        for(int i=1;i<36&&!value.isEmpty();i++){var slot=p.getInventory().getItem(i);if(!slot.isEmpty()&&ItemStack.isSameItemSameTags(slot,value)){int move=Math.min(value.getCount(),slot.getMaxStackSize()-slot.getCount());slot.grow(move);value.shrink(move);}}
        for(int i=1;i<36&&!value.isEmpty();i++)if(p.getInventory().getItem(i).isEmpty()){p.getInventory().setItem(i,value);value=ItemStack.EMPTY;}
        require(value.isEmpty(),"No physical inventory capacity");
    }p.inventoryMenu.broadcastChanges();}
    private static ItemStack take(ServerPlayer p,String id){for(int i=0;i<p.getInventory().getContainerSize();i++){var value=p.getInventory().getItem(i);if(value.is(item(id).getItem())){var paid=value.split(1);if(value.isEmpty())p.getInventory().setItem(i,ItemStack.EMPTY);p.inventoryMenu.broadcastChanges();return paid;}}throw new AssertionError("Missing actual paid input "+id);}
    private static int count(ServerPlayer p,String id){int n=0;for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i).is(item(id).getItem()))n+=p.getInventory().getItem(i).getCount();return n;}
    private static int vanillaCount(ServerPlayer p,net.minecraft.world.item.Item item){int n=0;for(int i=0;i<36;i++)if(p.getInventory().getItem(i).is(item))n+=p.getInventory().getItem(i).getCount();return n;}
    private static void emptyRawSlot(ServerPlayer p,int slot){var prior=p.getInventory().getItem(slot);if(prior.isEmpty())return;p.getInventory().setItem(slot,ItemStack.EMPTY);for(int i=1;i<36;i++)if(i!=slot&&p.getInventory().getItem(i).isEmpty()){p.getInventory().setItem(i,prior);return;}throw new AssertionError("No physical capacity for reserved raw slot");}
    private static void stage(ServerPlayer p,String key){try{var m=PlayerKnowledge.class.getDeclaredMethod("setResearchStage",String.class,int.class);m.setAccessible(true);require((boolean)m.invoke(KnowledgeStore.get(p),key,ResearchCatalog.get(key).stages().size()+1),"Predecessor "+key);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
    private static void raw(ServerPlayer p,KnowledgeType type,int amount){try{var m=PlayerKnowledge.class.getDeclaredMethod("addKnowledge",KnowledgeType.class,String.class,int.class);m.setAccessible(true);require((boolean)m.invoke(KnowledgeStore.get(p),type,"ARTIFICE",amount),"Raw knowledge");}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
    private static void close(Minecraft mc){mc.player.closeContainer();mc.setScreen(null);}
    private static void click(Minecraft mc,BlockPos pos,Direction face){mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(pos.getCenter(),face,pos,false));}
    private static void shift(Minecraft mc,boolean value){mc.player.input.shiftKeyDown=value;mc.player.setShiftKeyDown(value);mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player,value?ServerboundPlayerCommandPacket.Action.PRESS_SHIFT_KEY:ServerboundPlayerCommandPacket.Action.RELEASE_SHIFT_KEY));}
    private static boolean position(Minecraft mc,double x,double y,double z){if(mc.player.distanceToSqr(x,y,z)>.3){ready=0;return false;}return ++ready>=5;}
    /** Read-only source geometry; neither public operation nor cache mutation is invoked. */
    private static boolean candidateContains(BlockPos origin,Direction direction,int range,BlockPos wanted){
        try{var method=AirborneEssentiaManager.class.getDeclaredMethod("forEachCandidate",BlockPos.class,Direction.class,int.class,Consumer.class);method.setAccessible(true);boolean[] found={false};Consumer<BlockPos> observer=pos->{if(pos.equals(wanted))found[0]=true;};method.invoke(null,origin,direction,range,observer);return found[0];}
        catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
    }
    /** Observe actual native particle packets, passing every packet through unchanged. */
    private static void installFxObserver(Minecraft mc){
        if(fxObserverInstalled)return;var connection=mc.getConnection().getConnection();
        connection.channel().eventLoop().submit(()->connection.channel().pipeline().addBefore("packet_handler","thaumcraft_mirror_qa_observer",new ChannelInboundHandlerAdapter(){
            @Override public void channelRead(ChannelHandlerContext context,Object message){try{observeFx(message);}catch(Throwable e){fxObserverFailure=e.toString();}finally{context.fireChannelRead(message);}}
        })).syncUninterruptibly();fxObserverInstalled=true;
    }
    private static void observeFx(Object message){
        if(!watchEssentiaPackets)return;
        if(message instanceof ClientboundBundlePacket bundle){for(var packet:bundle.subPackets())observeFx(packet);return;}
        if(!(message instanceof ClientboundLevelParticlesPacket packet)||packet.getCount()!=1||!(packet.getParticle() instanceof DustParticleOptions dust))return;
        int color=Aspect.FIRE.getColor();var actual=dust.getColor();if(Math.abs(actual.x-((color>>16)&255)/255F)>1e-6F||Math.abs(actual.y-((color>>8)&255)/255F)>1e-6F||Math.abs(actual.z-(color&255)/255F)>1e-6F)return;
        var point=new net.minecraft.world.phys.Vec3(packet.getX(),packet.getY(),packet.getZ());
        var remoteStart=SOURCE.getCenter();var remoteEnd=MIRRORS[3].getCenter();var localStart=MIRRORS[2].getCenter();var localEnd=PUMP.getCenter();
        if(onSegment(point,remoteStart,remoteEnd))REMOTE_LEG_PACKETS.incrementAndGet();if(onSegment(point,localStart,localEnd))LOCAL_LEG_PACKETS.incrementAndGet();
        var endpoints=List.of(remoteStart,remoteEnd,localStart,localEnd);for(int i=0;i<4;i++)if(point.distanceToSqr(endpoints.get(i))<1e-10)FX_ENDPOINTS.incrementAndGet(i);
    }
    private static boolean onSegment(net.minecraft.world.phys.Vec3 point,net.minecraft.world.phys.Vec3 from,net.minecraft.world.phys.Vec3 to){var line=to.subtract(from);double length=line.lengthSqr(),t=point.subtract(from).dot(line)/length;return t>=-1e-8&&t<=1+1e-8&&point.distanceToSqr(from.add(line.scale(t)))<1e-10;}
    private static boolean allMirrorFxCommitted(){if(REMOTE_LEG_PACKETS.get()!=24*13||LOCAL_LEG_PACKETS.get()!=24*13)return false;for(int i=0;i<4;i++)if(FX_ENDPOINTS.get(i)!=24)return false;return true;}
    private static void clearArcaneFluxFixture(ServerPlayer p){
        var chunk=new ChunkPos(BENCH);float removed=0;for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)removed+=AuraManager.drainFlux(p.serverLevel(),new ChunkPos(chunk.x+dx,chunk.z+dz).getMiddleBlockPosition(112),1000,false);
        arcaneFluxFixtureCleared=true;LogUtils.getLogger().info("THAUMCRAFT_MIRROR_ARCANE_FIXTURE_CLEARED: after seventh verified native payment,removed={} residual supplied Flux from exactly nine bench fixture chunks (at most1000 each); production Rift generation unchanged,other chunks/paid essence/items untouched",removed);
    }
    private static void altarView(ServerPlayer p){p.connection.teleport(MATRIX.getX()+.5,112,MATRIX.getZ()+4.5,180,-12);}
    private static void mirrorView(ServerPlayer p){stow(p);captureView(p);}
    private static void captureView(ServerPlayer p){p.connection.teleport(VIEW_X,VIEW_Y,VIEW_Z,0,6);}
    private static boolean capturePosition(Minecraft mc){return position(mc,VIEW_X,VIEW_Y,VIEW_Z);}
    private static ArcaneWorkbenchBlockEntity bench(ServerLevel l){return (ArcaneWorkbenchBlockEntity)l.getBlockEntity(BENCH);}
    private static InfusionMatrixBlockEntity matrix(ServerLevel l){return (InfusionMatrixBlockEntity)l.getBlockEntity(MATRIX);}
    private static InfusionPedestalBlockEntity central(ServerLevel l){return (InfusionPedestalBlockEntity)l.getBlockEntity(MATRIX.below(2));}
    private static EssentiaJarBlockEntity jar(ServerLevel l,int i){return (EssentiaJarBlockEntity)l.getBlockEntity(MATRIX.offset(-4+i*2,-1,6));}
    private static int jars(ServerLevel l){int n=0;for(int i=0;i<5;i++)n+=jar(l,i).amount();return n;}
    private static AbstractMirrorBlockEntity mirror(ServerLevel l,int i){require(l.getBlockEntity(MIRRORS[i]) instanceof AbstractMirrorBlockEntity,"Missing survival-placed mirror "+i);return (AbstractMirrorBlockEntity)l.getBlockEntity(MIRRORS[i]);}
    private static void book(Minecraft mc,String key,boolean recipe){var k=ResearchClient.golemPressKnowledge();var browser=new ThaumonomiconScreen(k,k.scanCount());var page=new ThaumonomiconPageScreen(browser,ResearchCatalog.get(key),k,k.scanCount());mc.setScreen(page);if(recipe)try{var method=ThaumonomiconPageScreen.class.getDeclaredMethod("focusRecipe",ResourceLocation.class);method.setAccessible(true);require((boolean)method.invoke(page,ResourceLocation.fromNamespaceAndPath("thaumcraft","infusion/mirror")),"Operational mirror recipe missing");var show=ThaumonomiconPageScreen.class.getDeclaredMethod("showRecipeForSmokeTest",String.class);show.setAccessible(true);show.invoke(page,"mirror");}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!enabled()||stopped)return;var mc=Minecraft.getInstance();if(began==0)began=System.nanoTime();
        try{require(System.nanoTime()-began<1_500_000_000_000L,"Mirror timeout scene="+scene+" phase="+phase);if(!started){start(mc);return;}if(mc.level==null||mc.player==null||mc.getOverlay()!=null||mc.screen instanceof ReceivingLevelScreen)return;
            require(mc.getSingleplayerServer()!=null&&WORLD.equals(mc.getSingleplayerServer().getWorldData().getLevelName()),"Wrong owned mirror world");mc.getToasts().clear();
            if(++wait%100==0)LogUtils.getLogger().info("THAUMCRAFT_MIRROR_HEARTBEAT: scene={} phase={} run={} screen={} arcane={} infusion={} items={} hand={} source={} destination={} glimmers={}",scene,phase,run,mc.screen==null?"world":mc.screen.getClass().getSimpleName(),arcaneCount,infusionCount,itemOutput,handOutput,remoteSource,remoteDestination,glimmerCount);
            if(work!=null){if(!work.isDone())return;work.join();work=null;}if(!prepared){prepared=true;submit(mc,()->prepare(mc));return;}
            if(captured){if(SAVED.get()==scene+1&&++stable>=20){scene++;phase=stable=wait=ready=run=0;expectedStage=-1;captured=captureRequested=false;}return;}
            if(scene==IMAGES.length){finish(mc);return;}if(!advance(mc)){stable=0;return;}if(++stable>=12)captureRequested=true;
        }catch(Throwable e){fail(mc,e);}
    }
    private static boolean advance(Minecraft mc){return switch(scene){case 0->scan(mc);case 1->study(mc,"ARCANELAMP",0,16);case 2->arcane(mc,"lamp_arcane",1);case 3->arcane(mc,"mirrored_glass",6);case 4->study(mc,"MIRROR",32,16);case 5->study(mc,"MIRRORHAND",32,0);case 6->study(mc,"MIRRORESSENTIA",32,0);case 7->{if(phase==0){phase=1;book(mc,"MIRROR",true);}yield mc.screen instanceof ThaumonomiconPageScreen;}case 8->infusions(mc,"mirror",3);case 9->infusions(mc,"hand_mirror",1);case 10->infusions(mc,"mirror_essentia",2);case 11->placePairs(mc);case 12->entityTransfer(mc);case 13->handGui(mc);case 14->handSend(mc);case 15->remoteEssentia(mc);case 16->mounts(mc,false);case 17->mounts(mc,true);case 18->closeup(mc,false);case 19->closeup(mc,true);default->false;};}
    private static boolean scan(Minecraft mc){
        if(scanIndex<2){if(phase==0){phase=1;submit(mc,()->{var p=player(mc);held(p,new ItemStack(ScanningModule.THAUMOMETER.get()));p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(scanIndex==0?Items.TORCH:Items.ENDER_PEARL));p.inventoryMenu.broadcastChanges();});return false;}
            if(phase==1){if(!mc.player.getMainHandItem().is(ScanningModule.THAUMOMETER.get())||!mc.player.getOffhandItem().is(scanIndex==0?Items.TORCH:Items.ENDER_PEARL)||++ready<15)return false;shift(mc,true);mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);phase=2;return false;}
            var k=ResearchClient.golemPressKnowledge();if(scanIndex==0?!k.knowsAspect(Aspect.LIGHT):!k.isResearchKnown("f_TELEPORT"))return false;shift(mc,false);scanIndex++;phase=ready=0;return false;}
        if(phase==0){phase=1;submit(mc,()->{var p=player(mc);require(KnowledgeStore.get(p).knowsAspect(Aspect.LIGHT)&&KnowledgeStore.get(p).isResearchKnown("f_TELEPORT"),"Actual typed torch/ender-pearl scan failed");LogUtils.getLogger().info("THAUMCRAFT_MIRROR_SCANS: actual Thaumometer shift/offhand C2S raw torch discovers Lux,raw pearl registers f_TELEPORT,no supplied target facts/aspects");});return false;}return mc.screen==null;
    }
    private static boolean study(Minecraft mc,String key,int theory,int observation){
        if(phase==0){phase=1;close(mc);submit(mc,()->{var p=player(mc);stow(p);p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);if(theory>0)raw(p,KnowledgeType.THEORY,theory);if(observation>0)raw(p,KnowledgeType.OBSERVATION,observation);var k=KnowledgeStore.get(p);currentTheory=k.rawKnowledge(KnowledgeType.THEORY,"ARTIFICE");currentObservation=k.rawKnowledge(KnowledgeType.OBSERVATION,"ARTIFICE");currentXp=p.totalExperience;currentItemCount=count(p,"mirrored_glass");held(p,new ItemStack(ResearchModule.THAUMONOMICON.get()));if(key.equals("MIRROR"))require(p.getInventory().add(new ItemStack(Items.ENDER_PEARL)),"Raw study pearl inventory unavailable");ResearchNetwork.sync(p);});return false;}
        var k=ResearchClient.golemPressKnowledge();if(phase==1){if(!mc.player.getMainHandItem().is(ResearchModule.THAUMONOMICON.get()))return false;if(!k.isResearchCompleteStrict(key)){int current=k.researchStage(key);if(expectedStage==current)return false;expectedStage=current;ResearchNetwork.requestAdvance(key,current);return false;}phase=2;submit(mc,()->{var p=player(mc);var actual=KnowledgeStore.get(p);require(actual.isResearchCompleteStrict(key)&&actual.rawKnowledge(KnowledgeType.THEORY,"ARTIFICE")==currentTheory-theory&&actual.rawKnowledge(KnowledgeType.OBSERVATION,"ARTIFICE")==currentObservation-observation&&p.totalExperience==currentXp+10,"Native study payment changed "+key);if(key.equals("MIRROR"))require(count(p,"mirrored_glass")==currentItemCount-1&&vanillaCount(p,Items.ENDER_PEARL)==0,"MIRROR study did not pay actual crafted glass+pearl");studyTheoryPaid+=theory;studyObservationPaid+=observation;studyXpPaid+=10;LogUtils.getLogger().info("THAUMCRAFT_MIRROR_RESEARCH_PAID: {} native C2S theoryRaw={} observationRaw={} XP=10 actualglass+pearl={}",key,theory,observation,key.equals("MIRROR"));});return false;}
        if(!(mc.screen instanceof ThaumonomiconPageScreen))book(mc,key,false);return true;
    }
    private static void prepareArcane(ServerPlayer p,String id){
        var l=p.serverLevel();require(bench(l).isEmpty(),"Previous bench inputs retained");var recipe=l.getRecipeManager().getAllRecipesFor(ArcaneModule.RECIPE_TYPE.get()).stream().filter(r->r.getResultItem(l.registryAccess()).is(item(id).getItem())).findFirst().orElseThrow();
        require(recipe.vis()==50,"Original mirror/lamp arcane price");for(int index=0;index<recipe.getIngredients().size();index++){var ingredient=recipe.getIngredients().get(index);if(!ingredient.isEmpty()){require(ingredient.getItems().length>0,"Empty original native tag");bench(l).setItem(index%recipe.gridWidth()+index/recipe.gridWidth()*3,ingredient.getItems()[0].copyWithCount(1));}}
        for(int primal=0;primal<6;primal++)if(recipe.crystalCost(primal)>0)bench(l).setItem(9+primal,AspectCrystalItem.create(Aspect.getAspect(ArcaneModule.PRIMALS[primal]),recipe.crystalCost(primal)));
        l.setDayTime(6000);var chunk=new ChunkPos(BENCH);for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++){var anchor=new ChunkPos(chunk.x+dx,chunk.z+dz).getMiddleBlockPosition(112);AuraManager.drainVis(l,anchor,Float.MAX_VALUE,false);AuraManager.addVis(l,anchor,250);AuraManager.drainFlux(l,anchor,Float.MAX_VALUE,false);AuraManager.addFlux(l,anchor,1000);}beforeVis=AuraManager.getVis(l,BENCH);currentItemCount=count(p,id);p.inventoryMenu.broadcastChanges();
        LogUtils.getLogger().info("THAUMCRAFT_MIRROR_ARCANE_FIXTURES: {} raworiginalingredients/primalcrystals/250vis+highfluxneighbours; finaloutput absent",id);
    }
    private static boolean arcane(Minecraft mc,String id,int total){
        if(phase==0){phase=1;close(mc);submit(mc,()->{var p=player(mc);stow(p);held(p,ItemStack.EMPTY);p.connection.teleport(BENCH.getX()+.5,112,BENCH.getZ()+2.5,180,20);prepareArcane(p,id);});return false;}
        if(phase==1){if(!position(mc,BENCH.getX()+.5,112,BENCH.getZ()+2.5)||!mc.player.getMainHandItem().isEmpty())return false;phase=2;click(mc,BENCH,Direction.UP);return false;}
        if(!(mc.player.containerMenu instanceof ArcaneWorkbenchMenu menu))return false;
        if(phase==2){if(!menu.craftable()||!menu.getSlot(0).getItem().is(item(id).getItem()))return false;require(menu.requiredVis()==50,"Live native vis price");phase=3;mc.gameMode.handleInventoryMouseClick(menu.containerId,0,0,ClickType.QUICK_MOVE,mc.player);return false;}
        if(phase==3){if(menu.hasRecipe())return false;phase=4;submit(mc,()->{var p=player(mc);require(bench(p.serverLevel()).isEmpty()&&count(p,id)==currentItemCount+1&&AuraManager.getVis(p.serverLevel(),BENCH)==beforeVis-50,"Native arcane exact payment "+id);arcaneCount++;run++;LogUtils.getLogger().info("THAUMCRAFT_MIRROR_ARCANE_PAID: {} actual resultclick50vis+2primalcrystals+rawcomponents->one physicaloutput,totalcrafts={}",id,arcaneCount);if(run<total){prepareArcane(p,id);phase=2;}if(arcaneCount==7)clearArcaneFluxFixture(p);});return false;}
        return run==total&&mc.screen!=null;
    }
    private static InfusionRecipe infusion(ServerLevel l,String id){return l.getRecipeManager().getAllRecipesFor(InfusionModule.RECIPE_TYPE.get()).stream().filter(r->r.getResultItem(l.registryAccess()).is(item(id).getItem())).findFirst().orElseThrow();}
    private static void prepareInfusion(ServerPlayer p,String id){
        require(arcaneFluxFixtureCleared,"Supplied arcane Flux fixture not settled before altar QA");
        var l=p.serverLevel();require(central(l).isEmpty()&&!matrix(l).crafting(),"Previous native altar unresolved");var recipe=infusion(l,id);String input=id.equals("hand_mirror")?"mirror":"mirrored_glass";var paid=take(p,input);require(!MirrorLink.isBound(paid),"Linked mirror admitted as plain central");central(l).setItem(0,paid);var components=recipe.components(paid);require(components.size()==(id.equals("hand_mirror")?3:4),"Original mirror components");for(int i=0;i<4;i++){var pedestal=(InfusionPedestalBlockEntity)l.getBlockEntity(MATRIX.offset(OFFSETS[i][0],-2,OFFSETS[i][1]));require(pedestal.isEmpty(),"Retained peripheral payment");if(i<components.size())pedestal.setItem(0,components.get(i).example());}
        var plan=InfusionRecipes.find(p,paid,components.stream().map(c->c.example()).toList()).orElseThrow();require(plan.output().is(item(id).getItem())&&plan.aspects().visSize()==(id.equals("hand_mirror")?100:75),"Original mirror infusion plan");beforeJars=jars(l);stow(p);held(p,item("caster_basic"));altarView(p);
        LogUtils.getLogger().info("THAUMCRAFT_MIRROR_ALTAR_INPUT: {} actualpreviouslycraftedcentral={} physicalrawperipherals={} essentia={} instability={} unbound=true,no targetoutputfixture",id,input,components.size(),plan.aspects().visSize(),plan.instability());
    }
    private static boolean infusions(Minecraft mc,String id,int total){
        require(wait<18000,"Native mirror altar stalled "+id);
        if((scene==9||scene==10)&&lastCollectedScene!=scene){if(!collectLast(mc,scene==9?"mirror":"hand_mirror"))return false;lastCollectedScene=scene;phase=ready=0;}
        if(phase==0){phase=1;close(mc);submit(mc,()->{prepareInfusion(player(mc),id);});return false;}
        if(!(mc.level.getBlockEntity(MATRIX) instanceof InfusionMatrixBlockEntity m))return false;
        if(phase==1){if(!position(mc,MATRIX.getX()+.5,112,MATRIX.getZ()+4.5)||!InfusionMatrixBlock.isCaster(mc.player.getMainHandItem()))return false;if(!m.active()){if(++ready%20==0)click(mc,MATRIX,Direction.UP);return false;}if(m.startup()<.95F)return false;phase=2;click(mc,MATRIX,Direction.UP);return false;}
        if(phase==2){if(!mc.level.getBlockState(MATRIX).is(CatalogBlocks.block("infusion_matrix")))return false;if(m.crafting())return false;if(!(mc.level.getBlockEntity(MATRIX.below(2)) instanceof InfusionPedestalBlockEntity c)||!c.getItem(0).is(item(id).getItem()))return false;phase=3;submit(mc,()->{var p=player(mc);var l=p.serverLevel();require(central(l).getItem(0).is(item(id).getItem())&&central(l).getItem(0).getCount()==1&&!central(l).getItem(0).hasTag()&&jars(l)==beforeJars-(id.equals("hand_mirror")?100:75),"Actual native altar typedpayment/output changed "+id);for(var o:OFFSETS)require(((InfusionPedestalBlockEntity)l.getBlockEntity(MATRIX.offset(o[0],-2,o[1]))).isEmpty(),"Original peripheral did not pay");infusionCount++;run++;LogUtils.getLogger().info("THAUMCRAFT_MIRROR_ALTAR_PAID: {} ordinary matrix ticks/typednativejars/actualcraftedcentral+originalperipherals->onefreshunboundoutput,craft={},jars={}",id,infusionCount,jars(l));});return false;}
        if(phase==3&&run<total){phase=4;submit(mc,()->{var p=player(mc);stow(p);held(p,ItemStack.EMPTY);currentItemCount=count(p,id);});return false;}
        if(phase==4){if(!mc.player.getMainHandItem().isEmpty())return false;phase=5;click(mc,MATRIX.below(2),Direction.UP);return false;}
        if(phase==5){phase=6;submit(mc,()->{var p=player(mc);if(count(p,id)!=currentItemCount+1){phase=5;return;}require(central(p.serverLevel()).isEmpty(),"Paid output not physically retrieved");prepareInfusion(p,id);phase=1;});return false;}
        return phase==3&&run==total&&mc.screen==null;
    }
    private static boolean collectLast(Minecraft mc,String id){
        if(mc.level.getBlockEntity(MATRIX.below(2)) instanceof InfusionPedestalBlockEntity c&&!c.isEmpty()){
            if(phase==0){phase=1;close(mc);submit(mc,()->{var p=player(mc);stow(p);held(p,ItemStack.EMPTY);currentItemCount=count(p,id);altarView(p);});return false;}
            if(phase==1){if(!mc.player.getMainHandItem().isEmpty()||!position(mc,MATRIX.getX()+.5,112,MATRIX.getZ()+4.5))return false;phase=2;click(mc,MATRIX.below(2),Direction.UP);return false;}
            return false;
        }
        if(phase==2){phase=3;submit(mc,()->{var p=player(mc);if(count(p,id)<currentItemCount+1){phase=2;return;}require(central(p.serverLevel()).isEmpty(),"Missing physically retrieved altar output");});return false;}return phase>=3;
    }
    private static boolean placePairs(Minecraft mc){
        if(placementIndex==0&&phase<3){if(!collectLast(mc,"mirror_essentia"))return false;phase=3;}
        if(placementIndex<4){int i=placementIndex;String id=i<2?"mirror":"mirror_essentia";var pos=MIRRORS[i];Direction face=INITIAL[i];
            if(phase==3){phase=4;submit(mc,()->{var p=player(mc);stow(p);held(p,take(p,id));p.connection.teleport(pos.getX()+.5,112,pos.getZ()-2.5,0,-8);if(i%2==1)require(mirror(p.serverLevel(),i-1).resolvePeer(true).status()==AbstractMirrorBlockEntity.Status.INVALID,"Unpaired first mirror unexpectedly paired");});return false;}
            if(phase==4){if(!mc.player.getMainHandItem().is(item(id).getItem())||!position(mc,pos.getX()+.5,112,pos.getZ()-2.5))return false;phase=5;if(i%2==1)click(mc,MIRRORS[i-1],INITIAL[i-1]);else click(mc,pos.relative(face.getOpposite()),face);return false;}
            if(phase==5&&i%2==1){if(!MirrorLink.isBound(mc.player.getMainHandItem()))return false;phase=6;click(mc,pos.relative(face.getOpposite()),face);return false;}
            if(!(mc.level.getBlockEntity(pos) instanceof AbstractMirrorBlockEntity tile)||!mc.player.getMainHandItem().isEmpty()||tile.facing()!=face||i%2==1&&!tile.linked())return false;
            if(phase==5||phase==6){phase=7;submit(mc,()->{var p=player(mc);var actual=mirror(p.serverLevel(),i);require(actual.facing()==face&&p.getMainHandItem().isEmpty(),"Survival placement did not pay physical mirror");if(i%2==1)require(actual.isLinkValid()&&mirror(p.serverLevel(),i-1).isLinkValid(),"Native survival binding+placement did not restore reciprocal pair");LogUtils.getLogger().info("THAUMCRAFT_MIRROR_PLACED: {} actualsurvivalBlockItem/useItemOn,binding={} facing={} actualpair={},no setLink/restore fixture",id,i%2==1,face,i%2==1);placementIndex++;phase=3;});return false;}
        }
        if(phase==3){phase=8;submit(mc,()->{var p=player(mc);mirrorView(p);p.serverLevel().setDayTime(18000);require(count(p,"mirror")==0&&count(p,"mirror_essentia")==0,"Paid pair left duplicated inventory outputs");});return false;}
        return mc.screen==null&&capturePosition(mc)&&mc.level.getBlockEntity(MIRRORS[0]) instanceof AbstractMirrorBlockEntity a&&a.linked()&&mc.level.getBlockEntity(MIRRORS[2]) instanceof AbstractMirrorBlockEntity b&&b.linked();
    }
    private static boolean entityTransfer(Minecraft mc){
        if(phase==0){phase=1;close(mc);submit(mc,()->{var p=player(mc);mirrorView(p);var at=MIRRORS[0];var input=new ItemEntity(p.serverLevel(),at.getX()+.5,at.getY()+.5,at.getZ()+.5,new ItemStack(Items.EMERALD,8));input.setPickUpDelay(32767);require(p.serverLevel().addFreshEntity(input),"Raw loose input spawn rejected");LogUtils.getLogger().info("THAUMCRAFT_MIRROR_ENTITY_FIXTURE: raw8emerald physicallyspawned insidesourceblock,ordinary collision/queue/ejection,cooldownnative,no targetoutput supplied");});return false;}
        if(!entityTransferred||itemOutput!=8)return false;return mc.screen==null&&capturePosition(mc);
    }
    private static boolean handGui(Minecraft mc){
        if(phase==0){phase=1;close(mc);submit(mc,()->{var p=player(mc);stow(p);held(p,take(p,"hand_mirror"));p.connection.teleport(MIRRORS[0].getX()+.5,112,-2.5,0,-8);emptyRawSlot(p,9);p.getInventory().setItem(9,new ItemStack(Items.DIAMOND,8));handCountBefore=8;p.inventoryMenu.broadcastChanges();});return false;}
        if(phase==1){if(!mc.player.getMainHandItem().is(item("hand_mirror").getItem())||!position(mc,MIRRORS[0].getX()+.5,112,-2.5))return false;phase=2;click(mc,MIRRORS[0],INITIAL[0]);return false;}
        if(phase==2){if(!MirrorLink.isBound(mc.player.getMainHandItem()))return false;phase=3;mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);return false;}
        if(!(mc.screen instanceof HandMirrorScreen)||!(mc.player.containerMenu instanceof HandMirrorMenu menu))return false;
        require(menu.slots.size()==37&&menu.getSlot(0).x==80&&menu.getSlot(0).y==24,"Original one-input hand GUI shape");return menu.getSlot(0).getItem().isEmpty()&&menu.getSlot(1).getItem().is(Items.DIAMOND)&&menu.getSlot(1).getItem().getCount()==8;
    }
    private static boolean handSend(Minecraft mc){
        if(phase==0){require(mc.player.containerMenu instanceof HandMirrorMenu,"Hand menu disappeared");phase=1;mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId,1,0,ClickType.QUICK_MOVE,mc.player);return false;}
        if(phase==1){if(!mc.player.containerMenu.getSlot(0).getItem().isEmpty()||!mc.player.containerMenu.getSlot(1).getItem().isEmpty())return false;phase=2;close(mc);submit(mc,()->{var p=player(mc);require(p.getInventory().getItem(9).isEmpty(),"Hand C2S input was not paid");mirrorView(p);LogUtils.getLogger().info("THAUMCRAFT_MIRROR_HAND_INPUT_PAID: actualnativeoneinputmenu/C2SquickMove8diamonds,heldphysicallinktargetsource(notpartner),ordinaryqueueejection");});return false;}
        if(!handTransferred||handOutput!=handCountBefore)return false;return mc.screen==null&&capturePosition(mc);
    }
    private static boolean remoteEssentia(Minecraft mc){
        if(phase==0){phase=1;close(mc);submit(mc,()->{var p=player(mc);stow(p);var paid=take(p,"lamp_arcane");held(p,paid);p.connection.teleport(LAMP.getX()+.5,112,-2.5,0,-8);require(p.serverLevel().getBlockEntity(CONSUMER) instanceof EssentiaJarBlockEntity destination&&destination.amount()==0,"Remote destination raw jar missing");LogUtils.getLogger().info("THAUMCRAFT_MIRROR_REMOTE_FIXTURES: priorimplementedrawoutputtransfuser+emptyfilteredreceiverjar andraw24Ignisremotesourcejar deferred; pairedmirrors+lamp targetoutputs actuallypaid");});return false;}
        if(phase==1){if(!mc.player.getMainHandItem().is(item("lamp_arcane").getItem())||!position(mc,LAMP.getX()+.5,112,-2.5))return false;phase=2;click(mc,LAMP.south(),Direction.NORTH);return false;}
        if(phase==2){if(!mc.level.getBlockState(LAMP).is(CatalogBlocks.block("lamp_arcane"))||!mc.player.getMainHandItem().isEmpty())return false;installFxObserver(mc);watchEssentiaPackets=true;phase=3;submit(mc,()->{var p=player(mc);var l=p.serverLevel();require(SOURCE.getY()<PUMP.getY()&&!candidateContains(PUMP,Direction.UP,EssentiaTransfuserBlockEntity.RANGE,SOURCE),"Remote raw source is directly reachable by ordinary output range");require(candidateContains(MIRRORS[3],mirror(l,3).facing(),8,SOURCE)&&candidateContains(PUMP,Direction.UP,EssentiaTransfuserBlockEntity.RANGE,MIRRORS[2]),"Mirror-only native directional route fixture impossible");var destination=(EssentiaJarBlockEntity)l.getBlockEntity(CONSUMER);require(destination.applyLabel(p,Direction.NORTH,Aspect.FIRE),"Receiver label fixture failed");require(((EssentiaJarBlockEntity)l.getBlockEntity(SOURCE)).addExact(Aspect.FIRE,24),"Raw remote24Ignis failed");l.setBlockAndUpdate(PUMP,CatalogBlocks.block("essentia_output").defaultBlockState().setValue(EssentiaTransfuserBlock.FACING,Direction.UP));mirrorView(p);LogUtils.getLogger().info("THAUMCRAFT_MIRROR_REMOTE_GEOMETRY: source={} belowUPpump{} and excluded by actual native range16 enumeration; included by remote facing{} range8; local mirror included by pump range16; native packets observed without changing route/publication",SOURCE,PUMP,mirror(l,3).facing());});return false;}
        if(remoteDestination<1||remoteDestination>24||remoteSource+remoteDestination!=24)return false;
        return mc.screen==null&&essentiaTransferred&&glimmerCount>0&&maxGlimmerLight==15&&capturePosition(mc);
    }
    private static boolean mounts(Minecraft mc,boolean vertical){
        require(wait<5000,"Native sixface relocation stalled index="+mountIndex+" phase="+mountPhase);
        if(phase==0){if(remoteSource!=0||remoteDestination!=24||!allMirrorFxCommitted())return false;require(fxObserverFailure==null,"Native FX observation failed: "+fxObserverFailure);phase=1;close(mc);mountIndex=mountPhase=0;submit(mc,()->{var p=player(mc);stow(p);held(p,ItemStack.EMPTY);p.serverLevel().setBlockAndUpdate(PUMP,Blocks.AIR.defaultBlockState());LogUtils.getLogger().info("THAUMCRAFT_MIRROR_REMOTE_COMMIT_OBSERVED: before physical relocation,remote source0/receiver24 Ignis; ordinary direct source impossible; actual native accepted remoteJar->remoteMirror packets={},localMirror->pump packets={},four endpoints={}/{}/{}/{}; one essence owner,no observer publication",REMOTE_LEG_PACKETS.get(),LOCAL_LEG_PACKETS.get(),FX_ENDPOINTS.get(0),FX_ENDPOINTS.get(1),FX_ENDPOINTS.get(2),FX_ENDPOINTS.get(3));});return false;}
        if(mountIndex<4){int i=mountIndex;var at=MIRRORS[i];Direction face=vertical?(i%2==0?Direction.UP:Direction.DOWN):(i%2==0?Direction.EAST:Direction.WEST);String id=i<2?"mirror":"mirror_essentia";
            if(mountPhase==0){mountPhase=1;submit(mc,()->{var p=player(mc);stow(p);held(p,ItemStack.EMPTY);p.connection.teleport(at.getX()+.5,112,at.getZ()-2.5,0,-8);currentItemCount=count(p,id);});return false;}
            if(mountPhase==1){if(!position(mc,at.getX()+.5,112,at.getZ()-2.5))return false;mountPhase=2;mc.gameMode.startDestroyBlock(at,Direction.NORTH);return false;}
            if(mountPhase==2){if(!mc.level.getBlockState(at).isAir()){mc.gameMode.continueDestroyBlock(at,Direction.NORTH);return false;}mountPhase=3;submit(mc,()->{var p=player(mc);p.connection.teleport(at.getX()+.5,112,at.getZ()+.5,0,10);});return false;}
            if(mountPhase==3){mountPhase=4;submit(mc,()->{var p=player(mc);if(count(p,id)<currentItemCount+1){mountPhase=3;return;}var l=p.serverLevel();for(Direction old:Direction.values()){var support=at.relative(old);if(support.getY()>111&&l.getBlockState(support).is(Blocks.BEDROCK))l.setBlockAndUpdate(support,Blocks.AIR.defaultBlockState());}l.setBlockAndUpdate(at.relative(face.getOpposite()),Blocks.BEDROCK.defaultBlockState());stow(p);held(p,take(p,id));p.connection.teleport(at.getX()+.5,112,at.getZ()-2.5,0,face==Direction.DOWN?-35:face==Direction.UP?35:-8);});return false;}
            if(mountPhase==4){if(!mc.player.getMainHandItem().is(item(id).getItem())||!position(mc,at.getX()+.5,112,at.getZ()-2.5))return false;mountPhase=5;click(mc,at.relative(face.getOpposite()),face);return false;}
            if(!(mc.level.getBlockEntity(at) instanceof AbstractMirrorBlockEntity tile)||tile.facing()!=face||!mc.player.getMainHandItem().isEmpty())return false;
            if(mountPhase==5){mountPhase=6;submit(mc,()->{var p=player(mc);require(mirror(p.serverLevel(),i).facing()==face,"Native physical relocation face changed");actualRelocations++;mountIndex++;mountPhase=0;});return false;}
        }
        if(phase==1){phase=2;submit(mc,()->{var p=player(mc);for(int i=0;i<4;i++)require(mirror(p.serverLevel(),i).isLinkValid(),"Physical break/replacement did not restore pair "+i);stow(p);captureView(p);});return false;}
        return mc.screen==null&&capturePosition(mc);
    }
    /** Ordinary owned first-person camera: no paid state, inventory or terrain mutation. */
    private static boolean closeup(Minecraft mc,boolean ceiling){
        int index=ceiling?3:0;var at=MIRRORS[index];double feet=ceiling?112:113.125;
        if(phase==0){phase=1;close(mc);mc.options.hideGui=true;shift(mc,ceiling);submit(mc,()->{var p=player(mc);var tile=mirror(p.serverLevel(),index);require(tile.linked()&&tile.facing()==(ceiling?Direction.DOWN:Direction.UP),"Closeup changed native paid mounted mirror");p.connection.teleport(at.getX()+.5,feet,at.getZ()+.5,0,ceiling?-90:90);LogUtils.getLogger().info("THAUMCRAFT_MIRROR_CLOSEUP_CAMERA: {} actual owned player at mirror{} feetY={} pitch={} nativecrouch={},ordinary F1 capture,no inventory/link/terrain/transport changes",ceiling?"essentia DOWN":"item UP",at,feet,ceiling?-90:90,ceiling);});return false;}
        if(!position(mc,at.getX()+.5,feet,at.getZ()+.5)||mc.player.isCrouching()!=ceiling||mc.screen!=null||!mc.options.hideGui)return false;
        if(!(mc.level.getBlockEntity(at) instanceof AbstractMirrorBlockEntity tile)||!tile.linked()||tile.facing()!=(ceiling?Direction.DOWN:Direction.UP)||mc.player.getXRot()!=(ceiling?-90:90))return false;
        if(phase==1){phase=2;submit(mc,()->{var p=player(mc);require(p.getPose()==(ceiling?net.minecraft.world.entity.Pose.CROUCHING:net.minecraft.world.entity.Pose.STANDING),"Native server closeup pose did not settle");require(ceiling?p.getEyeHeight()<1.3F:p.getEyeHeight()>1.6F,"Native closeup eye height mismatch");LogUtils.getLogger().info("THAUMCRAFT_MIRROR_CLOSEUP_NATIVE_POSE: ceiling={},serverPose={},serverEyeHeight={},serverFeetY={},serverEyeY={}; no pose or eye-height setters",ceiling,p.getPose(),p.getEyeHeight(),p.getY(),p.getEyeY());});return false;}
        require(mc.player.getPose()==(ceiling?net.minecraft.world.entity.Pose.CROUCHING:net.minecraft.world.entity.Pose.STANDING),"Native client closeup pose mismatch");
        require(ceiling?mc.player.getEyeHeight()<1.3F:mc.player.getEyeHeight()>1.6F,"Native client closeup eye height mismatch");return true;
    }
    /** After KeyboardInput resets its fields, retain only the owned QA camera's native crouch input. */
    @SubscribeEvent public static void closeupInput(MovementInputUpdateEvent event){
        if(!enabled()||stopped||scene!=19||phase<1)return;var mc=Minecraft.getInstance();
        if(mc.player==null||event.getEntity()!=mc.player||mc.getSingleplayerServer()==null||!WORLD.equals(mc.getSingleplayerServer().getWorldData().getLevelName()))return;
        var input=event.getInput();input.shiftKeyDown=true;input.forwardImpulse=input.leftImpulse=0;
        input.up=input.down=input.left=input.right=input.jumping=false;
    }
    @SubscribeEvent public static void serverTick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!enabled()||stopped||!WORLD.equals(event.getServer().getWorldData().getLevelName())||!prepared)return;
        var l=event.getServer().overworld();
        var emeralds=l.getEntitiesOfClass(ItemEntity.class,new AABB(MIRRORS[0]).inflate(10),e->e.isAlive()&&e.getItem().is(Items.EMERALD));itemOutput=emeralds.stream().mapToInt(e->e.getItem().getCount()).sum();
        entityTransferred|=emeralds.size()>0&&emeralds.stream().allMatch(e->e.distanceToSqr(MIRRORS[1].getCenter())<9);
        var diamonds=l.getEntitiesOfClass(ItemEntity.class,new AABB(MIRRORS[0]).inflate(10),e->e.isAlive()&&e.getItem().is(Items.DIAMOND));handOutput=diamonds.stream().mapToInt(e->e.getItem().getCount()).sum();handTransferred|=diamonds.size()>0&&diamonds.stream().allMatch(e->e.distanceToSqr(MIRRORS[0].getCenter())<9);
        if(scene>=16)for(var p:event.getServer().getPlayerList().getPlayers())for(int i=0;i<36;i++){var value=p.getInventory().getItem(i);if(value.is(Items.EMERALD))itemOutput+=value.getCount();if(value.is(Items.DIAMOND))handOutput+=value.getCount();}
        if(l.getBlockEntity(SOURCE) instanceof EssentiaJarBlockEntity s&&l.getBlockEntity(CONSUMER) instanceof EssentiaJarBlockEntity d){remoteSource=s.amount();remoteDestination=d.amount();essentiaTransferred=d.amount()>0&&s.amount()+d.amount()==24&&d.aspect()==Aspect.FIRE;}
        if(scene>=15&&++worldObserve%20==0){glimmerCount=maxGlimmerLight=0;for(var pos:BlockPos.betweenClosed(LAMP.offset(-15,-15,-15),LAMP.offset(15,15,15)))if(l.hasChunkAt(pos)&&l.getBlockState(pos).is(CatalogBlocks.block("effect_glimmer"))){glimmerCount++;maxGlimmerLight=Math.max(maxGlimmerLight,l.getBrightness(LightLayer.BLOCK,pos));}}
    }
    private static void prepare(Minecraft mc){
        var p=player(mc);var l=p.serverLevel();p.setInvulnerable(true);for(int x=-2;x<=3;x++)for(int z=-2;z<=2;z++)l.getChunk(x,z);
        for(var pos:BlockPos.betweenClosed(-18,111,-12,34,121,18))l.setBlockAndUpdate(pos,pos.getY()==111?Blocks.BEDROCK.defaultBlockState():Blocks.AIR.defaultBlockState());
        l.setBlockAndUpdate(BENCH,ArcaneModule.WORKBENCH.get().defaultBlockState());l.setBlockAndUpdate(MATRIX,CatalogBlocks.block("infusion_matrix").defaultBlockState());l.setBlockAndUpdate(MATRIX.below(2),CatalogBlocks.block("pedestal_arcane").defaultBlockState());for(int x:new int[]{-1,1})for(int z:new int[]{-1,1})l.setBlockAndUpdate(MATRIX.offset(x,-2,z),CatalogBlocks.block("pillar_arcane").defaultBlockState());
        for(var o:OFFSETS)l.setBlockAndUpdate(MATRIX.offset(o[0],-2,o[1]),CatalogBlocks.block("pedestal_arcane").defaultBlockState());
        var colors=List.of("white","orange","magenta","lightblue","yellow","lime","pink","gray","silver","cyan","purple","blue","brown","green","red","black");for(int i=0;i<16;i++)for(int sign:new int[]{-1,1})l.setBlockAndUpdate(MATRIX.offset(sign*(2+i%6),-2,sign*(-3-i/6)),CatalogBlocks.block("candle_"+colors.get(i)).defaultBlockState());require(Math.abs(InfusionStability.scan(l,MATRIX).gain()-1.6F)<.00001F,"Native symmetrical altar fixture changed");
        for(int i=0;i<5;i++){l.setBlockAndUpdate(MATRIX.offset(-4+i*2,-1,6),CatalogBlocks.block("jar_normal").defaultBlockState());require(jar(l,i).addExact(ASPECTS.get(i),ESSENTIA[i]+3),"Typed raw490essentia fixture failed");}
        for(int i=0;i<4;i++)l.setBlockAndUpdate(MIRRORS[i].relative(INITIAL[i].getOpposite()),Blocks.BEDROCK.defaultBlockState());l.setBlockAndUpdate(LAMP.south(),Blocks.BEDROCK.defaultBlockState());l.setBlockAndUpdate(CONSUMER,CatalogBlocks.block("jar_normal").defaultBlockState());l.setBlockAndUpdate(SOURCE,CatalogBlocks.block("jar_normal").defaultBlockState());
        p.getInventory().clearContent();for(String key:List.of("FIRSTSTEPS","KNOWLEDGETYPES","UNLOCKARTIFICE","BASEARTIFICE","UNLOCKINFUSION","BASEINFUSION","INFUSION"))stage(p,key);for(String key:List.of("ARCANELAMP","MIRROR","MIRRORHAND","MIRRORESSENTIA"))require(!KnowledgeStore.get(p).isResearchKnown(key),"Target research supplied "+key);altarView(p);ResearchNetwork.sync(p);
        LogUtils.getLogger().info("THAUMCRAFT_MIRROR_FIXTURES: owned isolated survivalworld/loadedarena/bedrock/actualbench/altar/candlepairs/raw490typedessentia/predecessorstudies; rawknowledge/crystals/vis/materials deferred,all4targetstudies/finaloutputs/targetfacts absent; no direct runtime ticker/transfer/queue/setLink calls,no fullsurvival/multiplayer/crossdimensionclientclaim");
    }
    @SubscribeEvent public static void rendered(TickEvent.RenderTickEvent event){if(event.phase!=TickEvent.Phase.END||!enabled()||stopped||!captureRequested||captured)return;var mc=Minecraft.getInstance();try{captured=true;captureRequested=false;String name="tc6-mirror-"+IMAGES[scene]+".png";var file=new File(new File(mc.gameDirectory,"screenshots"),name);Files.deleteIfExists(file.toPath());Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),message->{if(file.isFile()&&file.length()>0){SAVED.incrementAndGet();LogUtils.getLogger().info("THAUMCRAFT_MIRROR_CLIENT_SMOKE_IMAGE: {}",file.getAbsolutePath());}else mc.execute(()->fail(mc,new AssertionError("Missing fresh native mirror capture")));});}catch(Throwable e){fail(mc,e);}}
    private static void start(Minecraft mc){if(mc.screen instanceof AccessibilityOnboardingScreen){mc.options.onboardAccessibility=false;mc.options.save();mc.setScreen(new TitleScreen());return;}if(!(mc.screen instanceof TitleScreen)||mc.getOverlay()!=null)return;started=true;previous=mc.options.tutorialStep;mc.options.tutorialStep=TutorialSteps.NONE;mc.getTutorial().stop();mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(3);mc.options.simulationDistance().set(5);mc.options.guiScale().set(2);mc.options.fov().set(80);mc.options.cloudStatus().set(CloudStatus.OFF);mc.options.setCameraType(CameraType.FIRST_PERSON);mc.options.hideGui=false;mc.resizeDisplay();var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);rules.getRule(GameRules.RULE_DAYLIGHT).set(false,null);rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false,null);mc.createWorldOpenFlows().createFreshLevel(WORLD,new LevelSettings(WORLD,GameType.SURVIVAL,false,Difficulty.PEACEFUL,true,rules,WorldDataConfiguration.DEFAULT),new WorldOptions(0x54433632L,false,false),WorldPresets::createNormalWorldDimensions);}
    private static void finish(Minecraft mc){
        if(phase==0){phase=1;mc.options.hideGui=false;shift(mc,false);submit(mc,()->{
            var p=player(mc);var l=p.serverLevel();var ownership=new AABB(MIRRORS[0]).inflate(10);
            int looseEmeralds=l.getEntitiesOfClass(ItemEntity.class,ownership,e->e.isAlive()&&e.getItem().is(Items.EMERALD)).stream().mapToInt(e->e.getItem().getCount()).sum();
            int looseDiamonds=l.getEntitiesOfClass(ItemEntity.class,ownership,e->e.isAlive()&&e.getItem().is(Items.DIAMOND)).stream().mapToInt(e->e.getItem().getCount()).sum();
            int inventoryEmeralds=vanillaCount(p,Items.EMERALD),inventoryDiamonds=vanillaCount(p,Items.DIAMOND);
            LogUtils.getLogger().info("THAUMCRAFT_MIRROR_CLIENT_FINAL_OBSERVED: scenes={},theoryRawPaid={},observationRawPaid={},studyXP={},arcaneCrafts={},infusionCrafts={},jarsRemaining={},mirroredGlassInventory={},itemMirrorInventory={},essentiaMirrorInventory={},handMirrorInventory={},lampInventory={},itemCollisionObserved={},emeraldObserverTotal={},emeraldLoose={},emeraldInventory={},handTransferObserved={},diamondObserverTotal={},diamondLoose={},diamondInventory={},remoteSourceIgnis={},receiverIgnis={},glimmers={},maxNativeBlockLight={},physicalRelocations={},remoteLegPackets={},localLegPackets={},fxEndpoints={}/{}/{}/{},selectedSlot={},selectedPhysicalItem={}",SAVED.get(),studyTheoryPaid,studyObservationPaid,studyXpPaid,arcaneCount,infusionCount,jars(l),count(p,"mirrored_glass"),count(p,"mirror"),count(p,"mirror_essentia"),count(p,"hand_mirror"),count(p,"lamp_arcane"),entityTransferred,itemOutput,looseEmeralds,inventoryEmeralds,handTransferred,handOutput,looseDiamonds,inventoryDiamonds,remoteSource,remoteDestination,glimmerCount,maxGlimmerLight,actualRelocations,REMOTE_LEG_PACKETS.get(),LOCAL_LEG_PACKETS.get(),FX_ENDPOINTS.get(0),FX_ENDPOINTS.get(1),FX_ENDPOINTS.get(2),FX_ENDPOINTS.get(3),p.getInventory().selected,p.getMainHandItem());
            require(studyTheoryPaid==96&&studyObservationPaid==32&&studyXpPaid==40&&arcaneCount==7&&infusionCount==6&&jars(l)==15,"Final ordinary target payment census changed");
            for(int i=0;i<4;i++)require(mirror(l,i).isLinkValid(),"Final paid pair invalid "+i);
            require(count(p,"mirrored_glass")==0&&count(p,"mirror")==0&&count(p,"mirror_essentia")==0&&count(p,"hand_mirror")==1&&count(p,"lamp_arcane")==0,"Final paid physical output ownership changed");
            require(entityTransferred,"Native item collision was not observed");
            require(itemOutput==8&&looseEmeralds+inventoryEmeralds==8,"Final Emerald ownership: observer="+itemOutput+",loose="+looseEmeralds+",inventory="+inventoryEmeralds+",expected8");
            require(handTransferred,"Native hand input transfer was not observed");
            require(handOutput==8&&looseDiamonds+inventoryDiamonds==8,"Final Diamond ownership: observer="+handOutput+",loose="+looseDiamonds+",inventory="+inventoryDiamonds+",expected8");
            require(remoteSource==0,"Final remote source Ignis="+remoteSource+",expected0");
            require(remoteDestination==24,"Final receiver Ignis="+remoteDestination+",expected24");
            require(glimmerCount>0,"Paid lamp produced no native glimmer blocks");
            require(maxGlimmerLight==15,"Native glimmer block light="+maxGlimmerLight+",expected15");
            LogUtils.getLogger().info("THAUMCRAFT_MIRROR_CLIENT_FACTS: scenes={},studies=4,theoryRawPaid={},observationRawPaid={},studyXP={},arcaneCrafts={},arcaneVisPaid=350,arcaneCrystalsPaid=14,infusionCrafts={},infusionEssentiaPaid=475,jarsRemaining={},survivalMirrors=4,reciprocalPairs=2,physicalEmeraldOutput={},physicalHandDiamonds={},remoteSourceIgnis={},receiverIgnis={},glimmers={},maxNativeBlockLight={},physicalRelocations={},nativeTunnelLayerDraws={},nativePaneDraws={},actualMirrorMotes={},actualMoteDraws={}",SAVED.get(),studyTheoryPaid,studyObservationPaid,studyXpPaid,arcaneCount,infusionCount,jars(l),itemOutput,handOutput,remoteSource,remoteDestination,glimmerCount,maxGlimmerLight,actualRelocations,MirrorRenderer.tunnelDraws(),MirrorRenderer.paneDraws(),MirrorEffects.actualMotes(),MirrorEffects.actualDraws());finalValidated=true;
        });return;}if(!finalValidated)return;
        require(SAVED.get()==IMAGES.length&&actualRelocations==8&&MirrorRenderer.tunnelDraws()>0&&MirrorEffects.actualMotes()>0&&MirrorEffects.actualDraws()>0&&arcaneFluxFixtureCleared&&allMirrorFxCommitted()&&fxObserverFailure==null,"Incomplete actual client mirror render/FX audit");for(long count:MirrorRenderer.mountedDraws())require(count>0,"A native original mounted mirror kind/facing was not drawn");for(String path:List.of("textures/gui/gui_handmirror.png","textures/blocks/mirrorframe.png","textures/blocks/mirrorframe2.png","textures/blocks/mirrorpane.png","textures/blocks/mirrorpanetrans.png","textures/misc/tunnel.png","textures/misc/particlefield.png","textures/misc/particles.png"))require(mc.getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath("thaumcraft",path)).isPresent(),"Missing original mirror media "+path);
        stopped=true;mc.options.tutorialStep=previous;LogUtils.getLogger().info("THAUMCRAFT_MIRROR_RENDER_AUDIT_OK: {} actualbook/paidbench/paidaltar/originaloneinputGUI/nativeworldPNG,twodistinctoriginalframes/sixfaces/linked16layercamera-projectedtunnel+unlinkedpane,actualphysicalitemeventmotes,nativecrossmirroressentia/localFX/lampglimmerlight/two realplayer floor-ceiling closeups; no gallery/dev overlay",SAVED.get());LogUtils.getLogger().info("THAUMCRAFT_MIRROR_CLIENT_SMOKE_OK: {} scenes; actual4paidresearch/7arcanecrafts/6infusions+475essentia/actualsurvivalbindingand4placements/itemcollision8emerald/handC2S8diamond/remoteleafjar24Ignisconserved/paidnativearcanelamp/light15/eightnativeharvestandreplacements,sixmountedfacesbothkinds/ownedactualplayercloseups; explicitrawfixtures,no directticks/queues/links/transfercall,world={}",SAVED.get(),WORLD);mc.stop();
    }
    private static void fail(Minecraft mc,Throwable e){if(stopped)return;stopped=true;if(previous!=null)mc.options.tutorialStep=previous;LogUtils.getLogger().error("THAUMCRAFT_MIRROR_CLIENT_SMOKE_FAILED",e);mc.stop();}
}
