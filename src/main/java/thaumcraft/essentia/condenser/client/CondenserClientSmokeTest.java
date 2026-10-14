package thaumcraft.essentia.condenser.client;

import com.mojang.logging.LogUtils;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import thaumcraft.alchemy.*;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.arcane.*;
import thaumcraft.essentia.*;
import thaumcraft.essentia.condenser.*;
import thaumcraft.essentia.transport.*;
import thaumcraft.catalog.CatalogModule;
import thaumcraft.catalog.entities.VisualEntitiesModule;
import thaumcraft.world.rift.FluxRiftEntity;
import thaumcraft.scanning.ThaumometerItem;
import thaumcraft.scanning.client.ThaumometerClient;
import thaumcraft.catalog.blocks.CatalogBlocks;
import thaumcraft.client.research.*;
import thaumcraft.research.*;
import thaumcraft.scanning.ScanningModule;
import thaumcraft.world.aura.AuraManager;


/** Owned isolated Survival fixtures; target payments/placements/flow are native world and C2S callbacks. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public final class CondenserClientSmokeTest {
    private static final String WORLD="thaumcraft-condenser-smoke-"+System.currentTimeMillis();
    private static final String[] IMAGES={"native-rift-scan","paid-flux-cleanup-study","paid-filter-bench","paid-lattice-bench","paid-condenser-bench","survival-condenser","connected-paid-lattice","native-essentia-input-sparks","aura-flux-to-vitium","native-vitium-clog","paid-filter-cleaning","native-redstone-pause","native-resumed-cleanup","survival-dismantling"};
    private static final BlockPos BENCH=new BlockPos(-8,112,-4),CORE=new BlockPos(0,114,0),AIR_JAR=CORE.east(2).below(),AIR_TUBE=CORE.east(),OUT_JAR=CORE.below(2),OUT_TUBE=CORE.below(),BAD_JAR=CORE.west().below(),BAD_TUBE=CORE.west();
    private static final AtomicInteger SAVED=new AtomicInteger();
    private static boolean started,prepared,stopped,captured,captureRequested,crouch,finalValidated;
    private static int scene,phase,stable,wait,ready,expectedStage=-1,crafts,craftIndex,placed,filtersCleaned,dirtyBefore,filterBefore,riftId,studyXp,baseXp,studyTheory,studyObservation,cleanupCrystals,firstOutput,pauseEssentia,pauseFlux,pauseOutput;
    private static int[] knowledgeBefore=new int[4];
    private static volatile int produced,airRemaining,coreEssentia,coreFlux,badRemaining,badHeld;
    private static volatile float observedAura;
    private static float benchBefore,budgetBefore,productionAura,pauseAura;
    private static int totalVis,totalCrystals;
    private static long began,phaseStarted;
    private static long dismantleEmissions;
    private static int dismantleQuietTicks;
    private static String observation="unprepared";
    private static CompletableFuture<Void> work;
    private static TutorialSteps previous;
    private CondenserClientSmokeTest() {}
    private static boolean enabled() { return Boolean.getBoolean("thaumcraft.condenserSmokeTest"); }
    private static void require(boolean condition,String message) { if(!condition)throw new AssertionError(message); }
    private static ServerPlayer player(Minecraft mc) { var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());require(p!=null,"Missing owned condenser player");return p; }
    private static void submit(Minecraft mc,Runnable action) { require(work==null,"Overlapping owned server actions");var result=new CompletableFuture<Void>();work=result;mc.getSingleplayerServer().execute(()->{try{action.run();result.complete(null);}catch(Throwable e){result.completeExceptionally(e);}}); }
    private static ItemStack item(String id) { var i=ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("thaumcraft",id));require(i!=null&&i!=Items.AIR,"Missing condenser item "+id);return new ItemStack(i); }
    private static int count(ServerPlayer p,Item item) { int result=0;for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i).is(item))result+=p.getInventory().getItem(i).getCount();return result; }
    private static int count(ServerPlayer p,String id) { return count(p,item(id).getItem()); }
    private static void stow(ServerPlayer p) { var stack=p.getMainHandItem();if(!stack.isEmpty()){p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);for(int i=1;i<36&&!stack.isEmpty();i++){var slot=p.getInventory().getItem(i);if(!slot.isEmpty()&&ItemStack.isSameItemSameTags(slot,stack)){int move=Math.min(stack.getCount(),slot.getMaxStackSize()-slot.getCount());slot.grow(move);stack.shrink(move);}}for(int i=1;i<36&&!stack.isEmpty();i++)if(p.getInventory().getItem(i).isEmpty()){p.getInventory().setItem(i,stack);stack=ItemStack.EMPTY;}require(stack.isEmpty(),"Cannot conserve physically held stack");}p.inventoryMenu.broadcastChanges(); }
    private static void held(ServerPlayer p,ItemStack stack) { require(p.getMainHandItem().isEmpty(),"Replacing physically owned held item");p.getInventory().selected=0;p.setItemInHand(InteractionHand.MAIN_HAND,stack);p.connection.send(new ClientboundSetCarriedItemPacket(0));p.inventoryMenu.broadcastChanges(); }
    private static ItemStack take(ServerPlayer p,Item item) { for(int i=0;i<36;i++){var stack=p.getInventory().getItem(i);if(stack.is(item)){var paid=stack.split(1);if(stack.isEmpty())p.getInventory().setItem(i,ItemStack.EMPTY);p.inventoryMenu.broadcastChanges();return paid;}}throw new AssertionError("Missing paid physical item "+item); }
    private static ItemStack take(ServerPlayer p,String id) { return take(p,item(id).getItem()); }
    private static void stage(ServerPlayer p,String key) { try{var m=PlayerKnowledge.class.getDeclaredMethod("setResearchStage",String.class,int.class);m.setAccessible(true);require((boolean)m.invoke(KnowledgeStore.get(p),key,ResearchCatalog.get(key).stages().size()+1),"Raw predecessor "+key);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);} }
    private static void close(Minecraft mc) { mc.player.closeContainer();mc.setScreen(null); }
    private static void click(Minecraft mc,BlockPos pos,Direction face) { mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(pos.getCenter().add(Vec3.atLowerCornerOf(face.getNormal()).scale(.5)),face,pos,false)); }
    private static boolean position(Minecraft mc,double x,double y,double z) { if(mc.player.distanceToSqr(x,y,z)>.3){ready=0;return false;}return ++ready>=5; }
    private static void raw(ServerPlayer p,KnowledgeType type,String category,int amount) { try{var m=PlayerKnowledge.class.getDeclaredMethod("addKnowledge",KnowledgeType.class,String.class,int.class);m.setAccessible(true);require((boolean)m.invoke(KnowledgeStore.get(p),type,category,amount),"Raw knowledge fixture");}catch(ReflectiveOperationException e){throw new IllegalStateException(e);} }
    private static CondenserBlockEntity core(ServerLevel level) { require(level.getBlockEntity(CORE) instanceof CondenserBlockEntity,"Paid condenser missing");return (CondenserBlockEntity)level.getBlockEntity(CORE); }
    private static EssentiaJarBlockEntity jar(ServerLevel level,BlockPos at) { require(level.getBlockEntity(at) instanceof EssentiaJarBlockEntity,"Raw native jar missing "+at);return (EssentiaJarBlockEntity)level.getBlockEntity(at); }
    private static ArcaneWorkbenchBlockEntity bench(ServerLevel l) { return (ArcaneWorkbenchBlockEntity)l.getBlockEntity(BENCH); }
    private static void clearFooting(ServerPlayer p) {
        var level=p.serverLevel();
        for(int y=112;y<=115;y++){var at=new BlockPos(3,y,3);if(level.getBlockState(at).is(Blocks.BEDROCK))level.removeBlock(at,false);}
    }
    private static void view(ServerPlayer p) { clearFooting(p);stow(p);p.connection.teleport(4.5,112,5.5,140,-20); }
    private static boolean viewed(Minecraft mc) { return mc.screen==null&&position(mc,4.5,112,5.5); }
    private static void book(Minecraft mc) { var k=ResearchClient.golemPressKnowledge();var browser=new ThaumonomiconScreen(k,k.scanCount());mc.setScreen(new ThaumonomiconPageScreen(browser,ResearchCatalog.get("FLUXCLEANUP"),k,k.scanCount())); }
    private static void shift(Minecraft mc,boolean active) { crouch=active;mc.player.input.shiftKeyDown=active;mc.player.setShiftKeyDown(active);mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player,active?ServerboundPlayerCommandPacket.Action.PRESS_SHIFT_KEY:ServerboundPlayerCommandPacket.Action.RELEASE_SHIFT_KEY)); }
    @SubscribeEvent public static void input(MovementInputUpdateEvent e) { if(!enabled()||stopped||!crouch)return;var mc=Minecraft.getInstance();if(mc.player!=null&&e.getEntity()==mc.player&&mc.getSingleplayerServer()!=null&&WORLD.equals(mc.getSingleplayerServer().getWorldData().getLevelName())){e.getInput().shiftKeyDown=true;e.getInput().forwardImpulse=e.getInput().leftImpulse=0;e.getInput().up=e.getInput().down=e.getInput().left=e.getInput().right=e.getInput().jumping=false;} }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||!enabled()||stopped)return;var mc=Minecraft.getInstance();if(began==0)began=System.nanoTime();
        try { require(System.nanoTime()-began<1_200_000_000_000L,"Condenser global timeout scene="+scene+" phase="+phase+" "+observation);if(!started){start(mc);return;}if(mc.level==null||mc.player==null||mc.getOverlay()!=null||mc.screen instanceof ReceivingLevelScreen)return;
            require(mc.getSingleplayerServer()!=null&&WORLD.equals(mc.getSingleplayerServer().getWorldData().getLevelName()),"Wrong owned condenser world");mc.getToasts().clear();
            if(++wait%100==0){
                LogUtils.getLogger().info("THAUMCRAFT_CONDENSER_HEARTBEAT: scene={} phase={} crafts={} placed={} output={} coreEss={} coreVitium={} aura={} badSource={} badTube={} detail={}",scene,phase,crafts,placed,produced,coreEssentia,coreFlux,observedAura,badRemaining,badHeld,observation);
                var clientTile=mc.level.getBlockEntity(CORE);
                String clientState=clientTile instanceof CondenserBlockEntity tile?"ticks="+tile.ticks()+",weighted="+tile.latticeCount()+",interval="+tile.interval()+",cost="+tile.cost()+",essentia="+tile.storedEssentia()+",vitium="+tile.storedFlux()+",clean="+tile.cleanLatticePositions():String.valueOf(clientTile);
                LogUtils.getLogger().info("THAUMCRAFT_CONDENSER_CLIENT_STATE: scene={} phase={} enabled={} physical={} emitted={} drawn={} active={} detail={}",scene,phase,mc.level.getBlockState(CORE),List.of(mc.level.getBlockState(CORE.above()),mc.level.getBlockState(CORE.above(2)),mc.level.getBlockState(CORE.above(3)),mc.level.getBlockState(CORE.above(4))),CondenserClientEffects.emittedSparks(),CondenserClientEffects.drawnSparks(),CondenserClientEffects.activeSparks(),clientState);
            }
            require(wait<4000,"Condenser bounded scene timeout scene="+scene+" phase="+phase+" "+observation);
            if(work!=null){if(!work.isDone())return;work.join();work=null;}if(!prepared){prepared=true;submit(mc,()->prepare(mc));return;}
            if(captured){if(SAVED.get()==scene+1&&++stable>=20){scene++;phase=stable=wait=ready=craftIndex=0;expectedStage=-1;captured=captureRequested=false;}return;}
            if(scene==IMAGES.length){finish(mc);return;}if(!advance(mc)){stable=0;return;}if(++stable>=12&&(scene!=7||CondenserClientEffects.activeSparks()>0))captureRequested=true;
        }catch(Throwable e){fail(mc,e);}
    }
    private static boolean advance(Minecraft mc) { return switch(scene) {
        case 0->scan(mc);case 1->study(mc);case 2->craft(mc,"filter",4,15,2);case 3->craft(mc,"condenser_lattice",4,100,1);case 4->craft(mc,"condenser",1,500,1);
        case 5->placeCore(mc);case 6->placeLattice(mc);case 7->flow(mc);case 8->output(mc);case 9->clog(mc);case 10->clean(mc);case 11->pause(mc);case 12->resume(mc);case 13->dismantle(mc);default->false;}; }
    private static boolean scan(Minecraft mc) {
        if(phase==0){phase=1;submit(mc,()->{var p=player(mc);stow(p);held(p,new ItemStack(ScanningModule.THAUMOMETER.get()));p.connection.teleport(-4.5,112,-13.5,0,-20);});return false;}
        if(phase==3)return mc.screen==null;
        var snapshot=ThaumometerClient.currentSnapshot();boolean aimed=snapshot!=null&&snapshot.target()!=null&&snapshot.target().location().kind()==ThaumometerItem.TargetKind.ENTITY&&snapshot.target().location().entityId()==riftId;
        if(!aimed||!mc.player.getMainHandItem().is(ScanningModule.THAUMOMETER.get()))return false;
        if(phase==1){phase=2;mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);return false;}
        if(!ResearchClient.golemPressKnowledge().isResearchKnown("!FluxRift"))return false;
        phase=3;submit(mc,()->{var p=player(mc);require(KnowledgeStore.get(p).isResearchKnown("f_toomuchflux")&&KnowledgeStore.get(p).isResearchKnown("!FluxRift"),"Physical Rift C2S scan did not earn originalfacts");LogUtils.getLogger().info("THAUMCRAFT_CONDENSER_FACT_PAID: actualThaumometerC2S/nativeFluxRift target -> f_toomuchflux+!FluxRift; sourceEntity/seed0x54433634/size10/stability100 explicitfixture,no naturalgenerationclaim/no factsetter");});return false;
    }
    private static boolean study(Minecraft mc) {
        if(phase==0){phase=1;close(mc);submit(mc,()->{var p=player(mc);require(p.serverLevel().getEntity(riftId) instanceof FluxRiftEntity,"Owned scanfixture lost");p.serverLevel().getEntity(riftId).discard();stow(p);
            raw(p,KnowledgeType.OBSERVATION,"AUROMANCY",32);raw(p,KnowledgeType.OBSERVATION,"ARTIFICE",16);raw(p,KnowledgeType.OBSERVATION,"BASICS",16);raw(p,KnowledgeType.THEORY,"AUROMANCY",64);
            var k=KnowledgeStore.get(p);knowledgeBefore=new int[]{k.rawKnowledge(KnowledgeType.OBSERVATION,"AUROMANCY"),k.rawKnowledge(KnowledgeType.OBSERVATION,"ARTIFICE"),k.rawKnowledge(KnowledgeType.OBSERVATION,"BASICS"),k.rawKnowledge(KnowledgeType.THEORY,"AUROMANCY")};baseXp=p.totalExperience;
            require(p.getInventory().add(CatalogModule.aspectStack("phial_filled",Aspect.FLUX,10)),"Raw studyphial missing");stow(p);held(p,new ItemStack(ResearchModule.THAUMONOMICON.get()));ResearchNetwork.sync(p);});return false;}
        var k=ResearchClient.golemPressKnowledge();if(phase==1){if(!mc.player.getMainHandItem().is(ResearchModule.THAUMONOMICON.get()))return false;if(!k.isResearchCompleteStrict("FLUXCLEANUP")){int stage=k.researchStage("FLUXCLEANUP");if(expectedStage==stage)return false;expectedStage=stage;ResearchNetwork.requestAdvance("FLUXCLEANUP",stage);return false;}
            phase=2;submit(mc,()->{var p=player(mc);var actual=KnowledgeStore.get(p);require(actual.isResearchCompleteStrict("FLUXCLEANUP")&&actual.rawKnowledge(KnowledgeType.OBSERVATION,"AUROMANCY")==knowledgeBefore[0]-32&&actual.rawKnowledge(KnowledgeType.OBSERVATION,"ARTIFICE")==knowledgeBefore[1]-16&&actual.rawKnowledge(KnowledgeType.OBSERVATION,"BASICS")==knowledgeBefore[2]-16&&actual.rawKnowledge(KnowledgeType.THEORY,"AUROMANCY")==knowledgeBefore[3]-64&&p.totalExperience==baseXp+10,"Original study payment changed");require(count(p,"phial_filled")==0&&count(p,"phial_empty")==0,"Original physical10Vitiumphial notconsumed/noemptyreturn");studyXp=10;studyTheory=64;studyObservation=64;LogUtils.getLogger().info("THAUMCRAFT_CONDENSER_RESEARCH_PAID: nativeC2S FLUXCLEANUP ObservationAURO32/ART16/BASICS16 TheoryAURO64 onephysical10Vitiumphial/noemptyreturned +10XP");});return false;}
        if(!(mc.screen instanceof ThaumonomiconPageScreen))book(mc);return true;
    }
    private static void prepareCraft(ServerPlayer p,String id,int price,int yield) {
        var l=p.serverLevel();require(bench(l).isEmpty(),"Unresolved preceding bench payment");var recipe=l.getRecipeManager().getAllRecipesFor(ArcaneModule.RECIPE_TYPE.get()).stream().filter(r->r.getResultItem(l.registryAccess()).is(item(id).getItem())).findFirst().orElseThrow();require(recipe.vis()==price&&recipe.getResultItem(l.registryAccess()).getCount()==yield,"Original recipeprice/count changed");
        for(int i=0;i<recipe.getIngredients().size();i++){var ingredient=recipe.getIngredients().get(i);if(!ingredient.isEmpty()){var raw=ingredient.getItems()[0].copyWithCount(1);if(raw.is(item("filter").getItem()))raw=take(p,"filter");bench(l).setItem(i%recipe.gridWidth()+i/recipe.gridWidth()*3,raw);}}
        for(int i=0;i<6;i++)if(recipe.crystalCost(i)>0)bench(l).setItem(9+i,AspectCrystalItem.create(Aspect.getAspect(ArcaneModule.PRIMALS[i]),recipe.crystalCost(i)));
        benchBefore=AuraManager.getVis(l,BENCH);filterBefore=count(p,id);p.inventoryMenu.broadcastChanges();
    }
    private static boolean craft(Minecraft mc,String id,int repetitions,int price,int yield) {
        if(phase==0){phase=1;close(mc);submit(mc,()->{var p=player(mc);stow(p);p.connection.teleport(BENCH.getX()+.5,112,BENCH.getZ()+2.5,180,20);prepareCraft(p,id,price,yield);});return false;}
        if(phase==1){if(!position(mc,BENCH.getX()+.5,112,BENCH.getZ()+2.5)||!mc.player.getMainHandItem().isEmpty())return false;phase=2;click(mc,BENCH,Direction.UP);return false;}
        if(!(mc.player.containerMenu instanceof ArcaneWorkbenchMenu menu))return false;
        if(phase==2){if(!menu.craftable()||!menu.getSlot(0).getItem().is(item(id).getItem()))return false;require(menu.requiredVis()==price,"Native recipevis changed");phase=3;mc.gameMode.handleInventoryMouseClick(menu.containerId,0,0,ClickType.QUICK_MOVE,mc.player);return false;}
        if(phase==3){if(menu.hasRecipe())return false;phase=4;submit(mc,()->{var p=player(mc);require(bench(p.serverLevel()).isEmpty()&&count(p,id)==filterBefore+yield&&AuraManager.getVis(p.serverLevel(),BENCH)==benchBefore-price,"Native physical benchpayment failed "+id);crafts++;craftIndex++;totalVis+=price;totalCrystals+=id.equals("filter")?1:id.equals("condenser_lattice")?6:15;LogUtils.getLogger().info("THAUMCRAFT_CONDENSER_BENCH_PAID: nativeC2S {} price={} yield={} repetition={}/{} totalCrafts={} totalVis={} totalCrystals={}",id,price,yield,craftIndex,repetitions,crafts,totalVis,totalCrystals);if(craftIndex<repetitions){prepareCraft(p,id,price,yield);phase=2;}});return false;}return craftIndex==repetitions&&mc.screen!=null;
    }
    private static boolean placeCore(Minecraft mc) {
        if(phase==0){phase=1;close(mc);submit(mc,()->{var p=player(mc);var l=p.serverLevel();require(crafts==9&&totalVis==960&&totalCrystals==43&&count(p,"filter")==4&&count(p,"condenser_lattice")==4&&count(p,"condenser")==1&&AuraManager.getVis(l,BENCH)==budgetBefore-960,"All paid crafts notconserved");
            require(l.getBlockEntity(CORE)==null&&l.getEntity(riftId)==null,"Production aura fixture must precede nativeconsumer and followfixtureRiftremoval");for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){var at=new ChunkPos(x,z).getMiddleBlockPosition(112);AuraManager.drainFlux(l,at,Float.MAX_VALUE,false);AuraManager.addFlux(l,at,10);}LogUtils.getLogger().info("THAUMCRAFT_CONDENSER_PRODUCTION_AURA_FIXTURE: initialnativeRift scancomplete/ownedRiftremoved; uniform25loadedchunksFlux10 restored BEFOREpaidCondenserplacement/consumerexistence,benchvis2500minus960 retained; no postconsumer aura reset");
            stow(p);held(p,take(p,"condenser"));p.connection.teleport(.5,112,3.5,180,-15);});return false;}
        if(phase==1){if(!position(mc,.5,112,3.5)||!mc.player.getMainHandItem().is(item("condenser").getItem()))return false;phase=2;click(mc,CORE.below(),Direction.UP);return false;}
        if(!(mc.level.getBlockEntity(CORE) instanceof CondenserBlockEntity)||!mc.player.getMainHandItem().isEmpty())return false;
        if(phase==2){phase=3;submit(mc,()->{var p=player(mc);require(core(p.serverLevel()).storedEssentia()==0&&core(p.serverLevel()).storedFlux()==0,"Target buffers supplied onplacement");placed=1;view(p);LogUtils.getLogger().info("THAUMCRAFT_CONDENSER_PLACED: actualsurvivalBlockItemC2S/paidcondenser1/zeroreservoirs/nobypass");});return false;}return viewed(mc);
    }
    private static void highView(ServerPlayer p,int index) { // Explicit raw footing does not mutate any target block or player-owned stack.
        clearFooting(p);var l=p.serverLevel();int y=113+index;for(int dy=112;dy<y;dy++)l.setBlockAndUpdate(new BlockPos(3,dy,3),Blocks.BEDROCK.defaultBlockState());p.connection.teleport(3.5,y,3.5,135,-15);
    }
    private static boolean placeLattice(Minecraft mc) {
        if(placed<5){int i=placed-1;var at=CORE.above(i+1);
            if(phase==0){phase=1;close(mc);submit(mc,()->{var p=player(mc);stow(p);held(p,take(p,"condenser_lattice"));highView(p,i);});return false;}
            if(phase==1){if(!position(mc,3.5,113+i,3.5)||!mc.player.getMainHandItem().is(item("condenser_lattice").getItem()))return false;phase=2;click(mc,at.below(),Direction.UP);return false;}
            if(!mc.level.getBlockState(at).is(CatalogBlocks.block("condenser_lattice"))||!mc.player.getMainHandItem().isEmpty())return false;
            if(phase==2){phase=3;submit(mc,()->{var p=player(mc);require(p.serverLevel().getBlockState(at).getValue(CondenserLatticeBlock.DOWN),"Native connected downarm missing");placed++;phase=0;});return false;}
        }
        if(phase==0){phase=4;submit(mc,()->{var p=player(mc);var tile=core(p.serverLevel());require(tile.connectedLatticeCount()==4&&Math.abs(tile.latticeCount()-3.10F)<.001&&tile.interval()==554&&tile.cost()==6,"Original fourvertical lattice DFS/count/cost/interval changed: "+tile.latticeCount()+"/"+tile.interval()+"/"+tile.cost());require(count(p,"condenser_lattice")==0,"Paid lattice leftovers");view(p);LogUtils.getLogger().info("THAUMCRAFT_CONDENSER_LATTICE: actualsurvivalpaid4/verticalconnectedgeometry/weighted3.10/interval554/cost6");});return false;}return viewed(mc);
    }
    private static boolean clientTopologyMatches(Minecraft mc) {
        if(!(mc.level.getBlockEntity(CORE) instanceof CondenserBlockEntity tile)||tile.connectedLatticeCount()!=4)return false;
        var clean=new ArrayList<BlockPos>();
        for(int i=1;i<=4;i++){
            var at=CORE.above(i);var physical=mc.level.getBlockState(at);
            if(physical.is(CatalogBlocks.block("condenser_lattice")))clean.add(at);
            else if(!physical.is(CatalogBlocks.block("condenser_lattice_dirty")))return false;
        }
        return tile.cleanLatticePositions().equals(clean);
    }
    private static void logClientTopology(Minecraft mc,String phaseName) {
        var tile=(CondenserBlockEntity)mc.level.getBlockEntity(CORE);
        LogUtils.getLogger().info("THAUMCRAFT_CONDENSER_CLIENT_TOPOLOGY: {} actualticks={} weighted={} interval={} cost={} clean={} matchesphysicalnative4lattices; readonlyobservation/no check-or-invalidation-call",phaseName,tile.ticks(),tile.latticeCount(),tile.interval(),tile.cost(),tile.cleanLatticePositions());
    }
    private static boolean flow(Minecraft mc) {
        if(phase==0){phase=1;close(mc);submit(mc,()->{var p=player(mc);var l=p.serverLevel();view(p);l.setBlockAndUpdate(OUT_TUBE,CatalogBlocks.block("tube").defaultBlockState());l.setBlockAndUpdate(AIR_TUBE,CatalogBlocks.block("tube").defaultBlockState());l.setBlockAndUpdate(AIR_JAR.above(),CatalogBlocks.block("tube").defaultBlockState());productionAura=AuraManager.getFlux(l,CORE);phaseStarted=l.getGameTime();LogUtils.getLogger().info("THAUMCRAFT_CONDENSER_TRANSPORT_FIXTURE: rawnativeAer80jar/3physicaltubes/emptybottomoutputjar;uniformFlux10andphase0vis2500 BEFOREtargetplacement, no postproductionaura/buffer/counter reset; realserverworldticks only");});return false;}
        if(!clientTopologyMatches(mc))return false;
        var client=(CondenserBlockEntity)mc.level.getBlockEntity(CORE);
        if(Math.abs(client.latticeCount()-3.10F)>=.001||client.interval()!=554||client.cost()!=6||client.cleanLatticePositions().size()!=4||client.storedEssentia()<=0||coreEssentia<=0||CondenserClientEffects.emittedSparks()==0||CondenserClientEffects.drawnSparks()==0||(phase==1&&CondenserClientEffects.activeSparks()==0))return false;
        if(phase==1){phase=2;logClientTopology(mc,"fourclean-input");LogUtils.getLogger().info("THAUMCRAFT_CONDENSER_LIVE_SPARKS: actualclienttileessentia={} emitted={} drawn={} live={} source={},originalgrid16frames8/24/40/layer0,no syntheticemit",coreEssentia,CondenserClientEffects.emittedSparks(),CondenserClientEffects.drawnSparks(),CondenserClientEffects.activeSparks(),CondenserClientEffects.lastSource());}return viewed(mc);
    }
    private static boolean output(Minecraft mc) {
        if(produced<1||!(mc.level.getBlockEntity(OUT_JAR) instanceof EssentiaJarBlockEntity j)||j.aspect()!=Aspect.FLUX||j.amount()<1)return false;
        if(phase==0){phase=1;submit(mc,()->{var p=player(mc);var l=p.serverLevel();observe(l);require(jar(l,OUT_JAR).aspect()==Aspect.FLUX&&jar(l,OUT_JAR).amount()>0,"Actual bottom jar lacks nativeVitium");require(80-airRemaining-core(l).storedEssentia()==produced*6&&productionAura-AuraManager.getFlux(l,CORE)==produced,"Native original6ess/1Flux->1Vitium payment changed");firstOutput=produced;
            require(crafts==9&&totalVis==960&&p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).isEmpty(),"Goggles fixture must follow allpaidcrafts and preservephysicalheadslot");p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,item("goggles"));view(p);held(p,take(p,ScanningModule.THAUMOMETER.get()));p.connection.teleport(.5,112,3.5,180,23);p.inventoryMenu.broadcastChanges();
            LogUtils.getLogger().info("THAUMCRAFT_CONDENSER_OUTPUT_PAID: realticks={} consumedAer={} drainedAuraFlux={} actualoutputVitium={} coreEssentia={} bottomJar={},no queue/ticker/direct output call; explicitRAWGoggles equipped ONLYafter9paidcrafts960vis/no visdiscount,noGogglescraftclaim,reusedphysicalScanner read-onlyhover",l.getGameTime()-phaseStarted,produced*6,productionAura-AuraManager.getFlux(l,CORE),produced,core(l).storedEssentia(),jar(l,OUT_JAR).amount());});return false;}
        var snapshot=ThaumometerClient.currentSnapshot();
        if(mc.screen!=null||!position(mc,.5,112,3.5)||!mc.player.getMainHandItem().is(ScanningModule.THAUMOMETER.get())||!(mc.hitResult instanceof BlockHitResult hit)||!hit.getBlockPos().equals(OUT_JAR)||!thaumcraft.equipment.armor.GogglesArmorClient.hasContainerPopup(OUT_JAR)||snapshot==null)return false;
        if(phase==1){phase=2;LogUtils.getLogger().info("THAUMCRAFT_CONDENSER_CONTAINER_HOVER: actualnativeGoggles containerpopup Vitium{}; read-onlyThaumometer S2CAuraFlux={} nativeoutputjar={},no overlaypaint/no objectscan/factgift",j.amount(),snapshot.flux(),OUT_JAR);}return true;
    }
    private static int dirty(ServerLevel l) { int count=0;for(int i=1;i<=4;i++)if(l.getBlockState(CORE.above(i)).is(CatalogBlocks.block("condenser_lattice_dirty")))count++;return count; }
    private static boolean clog(Minecraft mc) {
        if(phase==0){phase=1;close(mc);submit(mc,()->{var p=player(mc);stow(p);held(p,item("tube"));p.connection.teleport(-2.5,112,2.5,45,-25);});return false;}
        if(phase==1){if(!position(mc,-2.5,112,2.5)||!mc.player.getMainHandItem().is(item("tube").getItem()))return false;phase=2;click(mc,CORE,Direction.WEST);return false;}
        if(phase==2){if(!(mc.level.getBlockEntity(BAD_TUBE) instanceof TubeBlockEntity t)||t.getEssentiaType(Direction.EAST)!=Aspect.FLUX||t.getEssentiaAmount(Direction.EAST)!=1)return false;boolean dirty=false;for(int i=1;i<=4;i++)dirty|=mc.level.getBlockState(CORE.above(i)).is(CatalogBlocks.block("condenser_lattice_dirty"));if(!dirty)return false;phase=3;submit(mc,()->{var p=player(mc);var l=p.serverLevel();var source=(TubeBlockEntity)l.getBlockEntity(BAD_TUBE);require(jar(l,BAD_JAR).aspect()==Aspect.FLUX&&jar(l,BAD_JAR).amount()==9&&source.getEssentiaType(null)==Aspect.FLUX&&source.getEssentiaAmount(null)==1&&source.sideOpen(Direction.EAST)&&dirty(l)>0,"Actual pre-close Vitium intake/clog ownership changed");LogUtils.getLogger().info("THAUMCRAFT_CONDENSER_CLOG_PRE_CLOSE: actualsourceJarVitium9+nativeTubeVitium1/dirty{} beforephysicalcasterarmclose, no condensertake",dirty(l));stow(p);held(p,item("caster_basic"));});return false;}
        if(phase==3){if(!mc.player.getMainHandItem().is(item("caster_basic").getItem()))return false;phase=4;click(mc,BAD_TUBE,Direction.EAST);return false;}
        if(!(mc.level.getBlockEntity(BAD_TUBE) instanceof TubeBlockEntity t)||t.sideOpen(Direction.EAST))return false;
        if(phase==4){phase=5;submit(mc,()->{var p=player(mc);stow(p);held(p,new ItemStack(Items.REDSTONE_BLOCK));p.connection.teleport(.5,112,3.5,180,-15);});return false;}
        if(phase==5){if(!position(mc,.5,112,3.5)||!mc.player.getMainHandItem().is(Items.REDSTONE_BLOCK))return false;phase=6;click(mc,CORE,Direction.SOUTH);return false;}
        if(phase==6){if(mc.level.getBlockState(CORE).getValue(CondenserBlock.ENABLED))return false;phase=7;submit(mc,()->{var p=player(mc);var l=p.serverLevel();var source=(TubeBlockEntity)l.getBlockEntity(BAD_TUBE);int sourceJar=jar(l,BAD_JAR).amount(),sourceTube=source.getEssentiaAmount(null);dirtyBefore=dirty(l);require(dirtyBefore>0&&jar(l,BAD_JAR).aspect()==Aspect.FLUX&&((sourceJar==9&&sourceTube==1&&source.getEssentiaType(null)==Aspect.FLUX)||(sourceJar==10&&sourceTube==0&&source.getEssentiaType(null)==null))&&sourceJar+sourceTube==10&&!source.sideOpen(Direction.EAST)&&!l.getBlockState(CORE).getValue(CondenserBlock.ENABLED),"Original Vitium clog conserved10 after nativeclosedarm/backflow: jar="+sourceJar+" tube="+sourceTube+" aspect="+source.getEssentiaType(null)+" dirty="+dirtyBefore);view(p);LogUtils.getLogger().info("THAUMCRAFT_CONDENSER_CLOGGED: actualsourceJarVitium{}+nativeTubeVitium{}=10/no intake debit; dirty={} actualcasterC2SclosedEASTarm permits originalJar32/Tube31backflow; actualoneRedstoneBlockC2S freezesproductionbeforefiltercleaning,no RNG/dirty/counter reset",sourceJar,sourceTube,dirtyBefore);});return false;}
        if(phase==7){if(!viewed(mc)||!clientTopologyMatches(mc))return false;phase=8;logClientTopology(mc,"physical-clogged");}return phase==8&&viewed(mc);
    }
    private static boolean clean(Minecraft mc) {
        if(phase==0){phase=1;close(mc);submit(mc,()->{var p=player(mc);var l=p.serverLevel();int index=0;while(index<4&&!l.getBlockState(CORE.above(index+1)).is(CatalogBlocks.block("condenser_lattice_dirty")))index++;if(index==4){view(p);phase=4;return;}craftIndex=index;stow(p);held(p,take(p,"filter"));highView(p,index);filterBefore=count(p,"filter");});return false;}
        if(phase==1){if(!position(mc,3.5,113+craftIndex,3.5)||!mc.player.getMainHandItem().is(item("filter").getItem()))return false;phase=2;click(mc,CORE.above(craftIndex+1),Direction.SOUTH);return false;}
        if(phase==2){if(!mc.level.getBlockState(CORE.above(craftIndex+1)).is(CatalogBlocks.block("condenser_lattice"))||!mc.player.getMainHandItem().isEmpty())return false;phase=3;submit(mc,()->{var p=player(mc);require(count(p,"filter")==filterBefore-1,"Exactlyonephysicalpaidfilter notconsumed");filtersCleaned++;phase=0;LogUtils.getLogger().info("THAUMCRAFT_CONDENSER_FILTER_PAID: nativeC2Sfilter1 consumed/cleanlattice{} native50percentcrystalbranch unforced",craftIndex+1);});return false;}
        if(phase==4){phase=5;submit(mc,()->{var p=player(mc);require(dirty(p.serverLevel())==0&&filtersCleaned==dirtyBefore&&count(p,"filter")==4-filtersCleaned,"Paid filtercleanup census changed");view(p);});return false;}
        if(phase==5){if(!viewed(mc)||!clientTopologyMatches(mc))return false;phase=6;logClientTopology(mc,"physical-cleaned");}return phase==6&&viewed(mc);
    }
    private static boolean pause(Minecraft mc) {
        if(phase==0){phase=1;close(mc);submit(mc,()->{var p=player(mc);var l=p.serverLevel();require(l.getBlockState(CORE.south()).is(Blocks.REDSTONE_BLOCK)&&!l.getBlockState(CORE).getValue(CondenserBlock.ENABLED),"Physical precleaningredstone missing");observe(l);pauseEssentia=core(l).storedEssentia();pauseFlux=core(l).storedFlux();pauseOutput=produced;pauseAura=AuraManager.getFlux(l,CORE);phaseStarted=l.getGameTime();view(p);});return false;}
        if(phase==1){phase=2;submit(mc,()->{var l=player(mc).serverLevel();if(l.getGameTime()-phaseStarted<620){phase=1;return;}observe(l);require(core(l).storedEssentia()==pauseEssentia&&produced==pauseOutput&&AuraManager.getFlux(l,CORE)==pauseAura&&!l.getBlockState(CORE).getValue(CondenserBlock.ENABLED),"Actual620tickpowerpause changed reservoirs/aura/output");LogUtils.getLogger().info("THAUMCRAFT_CONDENSER_PAUSED: actualoneprecleaningRedstoneBlockC2S/620worldticks noessentiapay/noAuraFluxdebit/noVitiumproduction, existingoutputextraction maycomplete");});return false;}return viewed(mc);
    }
    private static boolean resume(Minecraft mc) {
        if(phase==0){phase=1;close(mc);submit(mc,()->{var p=player(mc);stow(p);held(p,new ItemStack(Items.DIAMOND_PICKAXE));p.connection.teleport(.5,112,3.5,180,-15);});return false;}
        if(phase==1){if(!position(mc,.5,112,3.5)||!mc.player.getMainHandItem().is(Items.DIAMOND_PICKAXE))return false;phase=2;mc.gameMode.startDestroyBlock(CORE.south(),Direction.SOUTH);return false;}
        if(phase==2){if(mc.level.getBlockState(CORE.south()).is(Blocks.REDSTONE_BLOCK)){mc.gameMode.continueDestroyBlock(CORE.south(),Direction.SOUTH);return false;}phase=3;return false;}
        if(!mc.level.getBlockState(CORE).getValue(CondenserBlock.ENABLED)||produced<=pauseOutput)return false;
        if(phase==3){phase=4;submit(mc,()->{var p=player(mc);var l=p.serverLevel();observe(l);require(80-airRemaining-core(l).storedEssentia()==produced*6&&productionAura-AuraManager.getFlux(l,CORE)==produced,"Resumed6essentiapay/nativeauradrain conservation changed");view(p);LogUtils.getLogger().info("THAUMCRAFT_CONDENSER_RESUMED: nativeC2Sredstoneharvest/realworldticks/output{}->{} freshpaidpulse6ess/1Flux->1Vitium, nocounter/reservoir/aura reset",pauseOutput,produced);});return false;}return viewed(mc);
    }
    private static boolean dismantle(Minecraft mc) {
        if(phase==0){phase=1;close(mc);submit(mc,()->{var p=player(mc);stow(p);held(p,new ItemStack(Items.DIAMOND_PICKAXE));p.connection.teleport(.5,112,3.5,180,-15);});return false;}
        if(phase==1){if(!position(mc,.5,112,3.5)||!mc.player.getMainHandItem().is(Items.DIAMOND_PICKAXE))return false;phase=2;mc.gameMode.startDestroyBlock(CORE,Direction.SOUTH);return false;}
        if(phase==2){if(mc.level.getBlockEntity(CORE) instanceof CondenserBlockEntity){mc.gameMode.continueDestroyBlock(CORE,Direction.SOUTH);return false;}for(int i=1;i<=4;i++)if(!mc.level.getBlockState(CORE.above(i)).isAir())return false;phase=3;dismantleEmissions=CondenserClientEffects.emittedSparks();dismantleQuietTicks=0;submit(mc,()->{var p=player(mc);var l=p.serverLevel();require(l.getBlockEntity(CORE)==null,"Nativeharvest core persisted");for(int i=1;i<=4;i++)require(l.getBlockState(CORE.above(i)).isAir(),"Detached paidlattice didnotnativefall/drop");var drops=l.getEntitiesOfClass(ItemEntity.class,new AABB(CORE).inflate(12),e->e.isAlive());int cores=count(p,"condenser"),lattices=count(p,"condenser_lattice");for(var e:drops){if(e.getItem().is(item("condenser").getItem()))cores+=e.getItem().getCount();if(e.getItem().is(item("condenser_lattice").getItem()))lattices+=e.getItem().getCount();}require(cores==1&&lattices==4,"Physical survival targetloot duplicated/lost: core="+cores+" lattice="+lattices);view(p);LogUtils.getLogger().info("THAUMCRAFT_CONDENSER_DISMANTLED: actualsurvivalC2Sharvest/onecore+fourpaidlattice physicallyowned/no orphan topology; nonstoredfuel sourcequirk retained");});return false;}
        if(phase==3){require(!(mc.level.getBlockEntity(CORE) instanceof CondenserBlockEntity)&&CondenserClientEffects.emittedSparks()==dismantleEmissions,"Removed physical condenser emitted new sparks");if(++dismantleQuietTicks<9||CondenserClientEffects.activeSparks()!=0)return false;phase=4;LogUtils.getLogger().info("THAUMCRAFT_CONDENSER_CLIENT_DETACHED: actualquietTicks={} emissions={} unchanged/new0/nativeoldsparksExpired; no invalidation-or-emission-call",dismantleQuietTicks,dismantleEmissions);}return phase==4&&viewed(mc);
    }
    @SubscribeEvent public static void serverTick(TickEvent.ServerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||!enabled()||stopped||!prepared||scene<7||!WORLD.equals(event.getServer().getWorldData().getLevelName()))return;observe(event.getServer().overworld());
    }
    /** Read all physically owned reservoirs in one server callback; never mix snapshots across a pulse. */
    private static void observe(ServerLevel l) {
        if(l.getBlockEntity(CORE) instanceof CondenserBlockEntity c){coreEssentia=c.storedEssentia();coreFlux=c.storedFlux();produced=coreFlux+jar(l,OUT_JAR).amount();if(l.getBlockEntity(OUT_TUBE) instanceof TubeBlockEntity t&&t.getEssentiaType(Direction.DOWN)==Aspect.FLUX)produced+=t.getEssentiaAmount(Direction.DOWN);
            airRemaining=jar(l,AIR_JAR).amount();for(var at:List.of(AIR_TUBE,AIR_JAR.above()))if(l.getBlockEntity(at) instanceof TubeBlockEntity t&&t.getEssentiaType(null)==Aspect.AIR)airRemaining+=t.getEssentiaAmount(null);
            observedAura=AuraManager.getFlux(l,CORE);badRemaining=jar(l,BAD_JAR).amount();badHeld=l.getBlockEntity(BAD_TUBE) instanceof TubeBlockEntity t?t.getEssentiaAmount(null):0;
            observation="ticks="+c.ticks()+",interval="+c.interval()+",cost="+c.cost()+",weighted="+c.latticeCount()+",dirty="+dirty(l)+",outJar="+jar(l,OUT_JAR).amount()+",clean="+c.cleanLatticePositions();
        }
    }
    private static void prepare(Minecraft mc) {
        var p=player(mc);var l=p.serverLevel();p.setInvulnerable(true);for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)l.getChunk(x,z);
        for(var pos:BlockPos.betweenClosed(-14,111,-16,15,121,12))l.setBlockAndUpdate(pos,pos.getY()==111?Blocks.BEDROCK.defaultBlockState():Blocks.AIR.defaultBlockState());l.setDayTime(6000);
        l.setBlockAndUpdate(BENCH,ArcaneModule.WORKBENCH.get().defaultBlockState());l.setBlockAndUpdate(CORE.below(),Blocks.OBSIDIAN.defaultBlockState());for(var at:List.of(AIR_JAR,BAD_JAR))l.setBlockAndUpdate(at.below(),Blocks.OBSIDIAN.defaultBlockState());for(var at:List.of(AIR_JAR,OUT_JAR,BAD_JAR))l.setBlockAndUpdate(at,CatalogBlocks.block("jar_normal").defaultBlockState());jar(l,AIR_JAR).setAspects(new thaumcraft.api.aspects.AspectList().add(Aspect.AIR,80));jar(l,BAD_JAR).setAspects(new thaumcraft.api.aspects.AspectList().add(Aspect.FLUX,10));
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){var at=new ChunkPos(x,z).getMiddleBlockPosition(112);AuraManager.drainVis(l,at,Float.MAX_VALUE,false);AuraManager.addVis(l,at,2500);AuraManager.drainFlux(l,at,Float.MAX_VALUE,false);AuraManager.addFlux(l,at,10);}budgetBefore=AuraManager.getVis(l,BENCH);
        p.getInventory().clearContent();for(String key:List.of("FIRSTSTEPS","KNOWLEDGETYPES","UNLOCKALCHEMY","BASEALCHEMY","BASEAUROMANCY","FLUX","VISBATTERY"))stage(p,key);for(String key:List.of("FLUXCLEANUP","f_toomuchflux","!FluxRift"))require(!KnowledgeStore.get(p).isResearchKnown(key),"Targetstudy/fact supplied "+key);
        var rift=(FluxRiftEntity)VisualEntitiesModule.EFFECTS.get("flux_rift").get().create(l);require(rift!=null,"NativeRift fixture missing");rift.setRiftSeed(0x54433634);rift.setRiftSize(10);rift.setRiftStability(100);rift.setPos(-4.5,115.5,-8.5);require(l.addFreshEntity(rift),"ActualRift targetfixture rejected");riftId=rift.getId();p.connection.teleport(-8.5,112,2.5,0,5);ResearchNetwork.sync(p);
        LogUtils.getLogger().info("THAUMCRAFT_CONDENSER_FIXTURES: isolatedownedSurvivalworld/rawbedrockarena/nativebench/sourceAer80+Vitium10jars/rawtubes/emptyoutputjar/predecessorstudies/nativeRiftentityseed0x54433634size10stability100;25loadedauraChunksvis2500Flux10phase0 BEFOREtargetexistence;no targetstudy/fact/finaldevice/lattice/filter/output supplied; rawknowledge/materialsdeferred,no runtime ticker/pull/clean/aura resetafterstart");
    }
    private static Object readField(Class<?> owner,Object instance,String name) throws ReflectiveOperationException {var field=owner.getDeclaredField(name);field.setAccessible(true);return field.get(instance);}
    /** Opt-in observation of an existing natural particle, never an emission or state mutation. */
    private static String nativeSparkSample(Minecraft mc) throws ReflectiveOperationException {
        var particles=(List<?>)readField(CondenserClientEffects.class,null,"SPARKS");
        var particleType=net.minecraft.client.particle.Particle.class;
        for(var spark:particles){
            int age=(int)readField(particleType,spark,"age"),lifetime=(int)readField(particleType,spark,"lifetime"),start=(int)readField(spark.getClass(),spark,"startFrame");
            int frame=start+(int)Math.min(8*age/(float)lifetime,7),offset=frame-start;
            // The primary atlas's30/31 frames are transparent. Middle frames have verified alpha pixels.
            if(age<=0||age>=lifetime||offset<1||offset>4)continue;
            double x=(double)readField(particleType,spark,"x"),y=(double)readField(particleType,spark,"y"),z=(double)readField(particleType,spark,"z");
            if(!mc.level.getBlockState(BlockPos.containing(x,y,z)).is(CatalogBlocks.block("condenser_lattice")))continue;
            return "frame="+frame+",start="+start+",age="+age+",lifetime="+lifetime+",source="+x+"/"+y+"/"+z;
        }return null;
    }
    @SubscribeEvent public static void rendered(TickEvent.RenderTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||!enabled()||stopped||!captureRequested||captured)return;var mc=Minecraft.getInstance();try{
            String sparkSample=scene==7?nativeSparkSample(mc):null;if(scene==7&&sparkSample==null)return;
            captured=true;captureRequested=false;if(scene==7)LogUtils.getLogger().info("THAUMCRAFT_CONDENSER_CAPTURE_SPARK: actualnaturallyspawned {} alpha-bearingprimarymiddleframe/no emitter/RNG/particle mutation",sparkSample);
            String name="tc6-condenser-"+IMAGES[scene]+".png";var file=new File(new File(mc.gameDirectory,"screenshots"),name);Files.deleteIfExists(file.toPath());Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),message->{if(file.isFile()&&file.length()>0){SAVED.incrementAndGet();LogUtils.getLogger().info("THAUMCRAFT_CONDENSER_CLIENT_SMOKE_IMAGE: {}",file.getAbsolutePath());}else mc.execute(()->fail(mc,new AssertionError("Missing freshnativeowned capture")));});}catch(Throwable e){fail(mc,e);} }
    private static void start(Minecraft mc) {
        if(mc.screen instanceof AccessibilityOnboardingScreen){mc.options.onboardAccessibility=false;mc.options.save();mc.setScreen(new TitleScreen());return;}if(!(mc.screen instanceof TitleScreen)||mc.getOverlay()!=null)return;started=true;previous=mc.options.tutorialStep;mc.options.tutorialStep=TutorialSteps.NONE;mc.getTutorial().stop();mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(3);mc.options.simulationDistance().set(5);mc.options.guiScale().set(2);mc.options.fov().set(80);mc.options.cloudStatus().set(CloudStatus.OFF);mc.options.setCameraType(CameraType.FIRST_PERSON);mc.options.hideGui=false;mc.resizeDisplay();var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);rules.getRule(GameRules.RULE_DAYLIGHT).set(false,null);rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false,null);mc.createWorldOpenFlows().createFreshLevel(WORLD,new LevelSettings(WORLD,GameType.SURVIVAL,false,Difficulty.PEACEFUL,true,rules,WorldDataConfiguration.DEFAULT),new WorldOptions(0x54433634L,false,false),WorldPresets::createNormalWorldDimensions);
    }
    private static void finish(Minecraft mc) {
        if(phase==0){phase=1;close(mc);shift(mc,false);submit(mc,()->{var p=player(mc);var l=p.serverLevel();require(studyTheory==64&&studyObservation==64&&studyXp==10&&crafts==9&&totalVis==960&&totalCrystals==43&&placed==5&&filtersCleaned==dirtyBefore&&count(p,"filter")==4-filtersCleaned,"Finalpaidprogression/bench/filtercensus changed");require(KnowledgeStore.get(p).isResearchCompleteStrict("FLUXCLEANUP")&&KnowledgeStore.get(p).isResearchKnown("f_toomuchflux")&&l.getBlockEntity(CORE)==null,"Finalnativefact/study/teardown lost");int crystals=0;for(int i=0;i<p.getInventory().getContainerSize();i++){var stack=p.getInventory().getItem(i);if(stack.getItem() instanceof AspectCrystalItem){var list=((AspectCrystalItem)stack.getItem()).getAspects(stack);if(list!=null&&list.size()==1&&list.getAmount(Aspect.FLUX)==1)crystals+=stack.getCount();}}for(var e:l.getEntitiesOfClass(ItemEntity.class,new AABB(CORE).inflate(12),e->e.isAlive()))if(e.getItem().getItem() instanceof AspectCrystalItem){var list=((AspectCrystalItem)e.getItem().getItem()).getAspects(e.getItem());if(list!=null&&list.size()==1&&list.getAmount(Aspect.FLUX)==1)crystals+=e.getItem().getCount();}cleanupCrystals=crystals;require(crystals<=filtersCleaned,"Unforced50percentcleanupcrystal ownership duplicated");int sourceJar=jar(l,BAD_JAR).amount();var source=(TubeBlockEntity)l.getBlockEntity(BAD_TUBE);int sourceTube=source.getEssentiaAmount(null);require(sourceJar+sourceTube==10&&jar(l,BAD_JAR).aspect()==Aspect.FLUX&&!source.sideOpen(Direction.EAST)&&((sourceJar==9&&sourceTube==1&&source.getEssentiaType(null)==Aspect.FLUX)||(sourceJar==10&&sourceTube==0&&source.getEssentiaType(null)==null)),"Final nativeVitium10source ownership changed");LogUtils.getLogger().info("THAUMCRAFT_CONDENSER_CLIENT_FACTS: scenes={} nativeRiftScan/paidFLUXCLEANUP Observation64Theory64phialVitium10/+10XP/9benchcrafts960vis43crystals/1core4lattice8filters/filtersUsedInRecipes4 cleanupFilters={} unforced50percentCrystals={} observedNativeVitium={} rawAerConsumed={} nativeAuraFluxDrained={} actualSourceVitiumJar={}+Tube={} conserved10/casterarmclose/nativebackflow/redstone620ticks/resume/survivalteardown; explicitfixtures/no fullsurvival/multiplayer/reload/audio/allanglesclaim",SAVED.get(),filtersCleaned,cleanupCrystals,produced,produced*6,productionAura-observedAura,sourceJar,sourceTube);finalValidated=true;});return;}
        if(!finalValidated)return;require(SAVED.get()==IMAGES.length&&CondenserClientEffects.emittedSparks()>0&&CondenserClientEffects.drawnSparks()>0,"Nativevisualcoverage incomplete");for(String path:List.of("textures/blocks/condenser.png","textures/blocks/condenser_off.png","textures/blocks/condenser_lattice.png","textures/misc/particles.png"))require(mc.getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath("thaumcraft",path)).isPresent(),"Missing originalresource "+path);stopped=true;mc.options.tutorialStep=previous;LogUtils.getLogger().info("THAUMCRAFT_CONDENSER_RENDER_AUDIT_OK: {} actualRiftscan/book/paidbench/nativeconnectedclean-dirtylattice/sparks/output/power/teardown PNG,originalstaticmodels/byteidenticalmedia/spark8framesgrid16layer0 emissions={} draws={}; no gallery/no TESRinvented",SAVED.get(),CondenserClientEffects.emittedSparks(),CondenserClientEffects.drawnSparks());LogUtils.getLogger().info("THAUMCRAFT_CONDENSER_CLIENT_SMOKE_OK: {} scenes; actualpaidstudy/9crafts960vis43crystals/5survivalplacements/native6ess1Flux->1Vitium/pull-tubes/clog/filter/redstone620ticks/resume/teardown; world={}",SAVED.get(),WORLD);mc.stop();
    }
    private static void fail(Minecraft mc,Throwable e) { if(stopped)return;stopped=true;if(previous!=null)mc.options.tutorialStep=previous;LogUtils.getLogger().error("THAUMCRAFT_CONDENSER_CLIENT_SMOKE_FAILED",e);mc.stop(); }
}
