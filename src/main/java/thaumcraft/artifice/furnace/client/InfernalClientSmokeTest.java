package thaumcraft.artifice.furnace.client;

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
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.*;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import thaumcraft.alchemy.*;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.arcane.*;
import thaumcraft.artifice.furnace.*;
import thaumcraft.catalog.blocks.CatalogBlocks;
import thaumcraft.client.research.*;
import thaumcraft.essentia.production.*;
import thaumcraft.research.*;
import thaumcraft.scanning.ScanningModule;
import thaumcraft.world.aura.AuraManager;

/** Explicit owned raw fixtures; every target study/craft/placement uses native packets and world ticks. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public final class InfernalClientSmokeTest {
    private static final String WORLD="thaumcraft-infernal-smoke-"+System.currentTimeMillis();
    private static final long SMELT_RANDOM_SEED=0x54433633L;
    private static final String[] IMAGES={"native-nether-biome-visit","native-ignis-scan","paid-infernal-research","paid-bellows-research","original-infernal-blueprint","paid-four-bellows-bench","paid-salis-craft","raw-furnace-assembly","native-salis-formation","native-paid-bellows","native-vanilla-furnace-boost","native-infernal-smelting","physical-gold-bonus-and-xp","native-powered-bellows-pause","native-furnace-dismantling"};
    private static final BlockPos BENCH=new BlockPos(-8,112,-4),CRAFT=new BlockPos(-8,112,0),CORNER=new BlockPos(0,112,0),CORE=CORNER.offset(1,1,1),VANILLA=new BlockPos(8,112,1),CHEST=new BlockPos(6,112,5);
    private static final BlockPos[] BELLOWS={CORE.east(2),CORE.west(2),CORE.north(2),VANILLA.west()};
    private static final Direction[] TOWARDS={Direction.WEST,Direction.EAST,Direction.SOUTH,Direction.EAST};
    private static final AtomicInteger SAVED=new AtomicInteger();
    private static boolean started,prepared,stopped,captureRequested,captured,finalValidated,crouch;
    private static int scene,phase,stable,wait,ready,expectedStage=-1,crafts,placementIndex,studyObservationPaid,studyXpEarned,currentKnowledge,currentXp,currentCount,goldOutput,nuggetOutput,rareOutput,xpTotal;
    private static volatile int inputCount,pendingCount;
    private static volatile boolean cookedComplete;
    private static float beforeVis,smeltVisStart,smeltFluxStart,finalVis,finalFlux,frozenInflation;
    private static long began,smeltStarted,vanillaStarted,pickupStarted;
    private static CompletableFuture<Void> work;
    private static TutorialSteps previous;
    private InfernalClientSmokeTest() {}
    private static boolean enabled() { return Boolean.getBoolean("thaumcraft.infernalSmokeTest"); }
    private static void require(boolean condition,String message) { if(!condition)throw new AssertionError(message); }
    private static ServerPlayer player(Minecraft mc) { var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());require(p!=null,"Missing owned infernal player");return p; }
    private static void submit(Minecraft mc,Runnable action) { require(work==null,"Overlapping owned server actions");var result=new CompletableFuture<Void>();work=result;mc.getSingleplayerServer().execute(()->{try{action.run();result.complete(null);}catch(Throwable e){result.completeExceptionally(e);}}); }
    private static ItemStack item(String id) { var i=ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("thaumcraft",id));require(i!=null&&i!=Items.AIR,"Missing infernal item "+id);return new ItemStack(i); }
    private static int count(ServerPlayer p,Item item) { int result=0;for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i).is(item))result+=p.getInventory().getItem(i).getCount();return result; }
    private static int count(ServerPlayer p,String id) { return count(p,item(id).getItem()); }
    private static void stow(ServerPlayer p) { var stack=p.getMainHandItem();if(!stack.isEmpty()){p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);for(int i=1;i<36&&!stack.isEmpty();i++){var slot=p.getInventory().getItem(i);if(!slot.isEmpty()&&ItemStack.isSameItemSameTags(slot,stack)){int move=Math.min(stack.getCount(),slot.getMaxStackSize()-slot.getCount());slot.grow(move);stack.shrink(move);}}for(int i=1;i<36&&!stack.isEmpty();i++)if(p.getInventory().getItem(i).isEmpty()){p.getInventory().setItem(i,stack);stack=ItemStack.EMPTY;}require(stack.isEmpty(),"Cannot conserve physically held stack");}p.inventoryMenu.broadcastChanges(); }
    private static void held(ServerPlayer p,ItemStack stack) { require(p.getMainHandItem().isEmpty(),"Replacing physically owned held item");p.getInventory().selected=0;p.setItemInHand(InteractionHand.MAIN_HAND,stack);p.connection.send(new ClientboundSetCarriedItemPacket(0));p.inventoryMenu.broadcastChanges(); }
    private static ItemStack take(ServerPlayer p,Item item) { for(int i=0;i<36;i++){var stack=p.getInventory().getItem(i);if(stack.is(item)){var paid=stack.split(1);if(stack.isEmpty())p.getInventory().setItem(i,ItemStack.EMPTY);p.inventoryMenu.broadcastChanges();return paid;}}throw new AssertionError("Missing paid physical item "+item); }
    private static ItemStack take(ServerPlayer p,String id) { return take(p,item(id).getItem()); }
    private static void stage(ServerPlayer p,String key) { try{var m=PlayerKnowledge.class.getDeclaredMethod("setResearchStage",String.class,int.class);m.setAccessible(true);require((boolean)m.invoke(KnowledgeStore.get(p),key,ResearchCatalog.get(key).stages().size()+1),"Raw predecessor "+key);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);} }
    private static void raw(ServerPlayer p,String category,int amount) { try{var m=PlayerKnowledge.class.getDeclaredMethod("addKnowledge",KnowledgeType.class,String.class,int.class);m.setAccessible(true);require((boolean)m.invoke(KnowledgeStore.get(p),KnowledgeType.OBSERVATION,category,amount),"Raw observation fixture");}catch(ReflectiveOperationException e){throw new IllegalStateException(e);} }
    private static void close(Minecraft mc) { mc.player.closeContainer();mc.setScreen(null); }
    private static void click(Minecraft mc,BlockPos pos,Direction face) { mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(pos.getCenter().add(Vec3.atLowerCornerOf(face.getNormal()).scale(.5)),face,pos,false)); }
    private static boolean position(Minecraft mc,double x,double y,double z) { if(mc.player.distanceToSqr(x,y,z)>.3){ready=0;return false;}return ++ready>=5; }
    private static void worldView(ServerPlayer p) { stow(p);p.connection.teleport(5.5,112,6.5,141,2); }
    private static boolean worldPosition(Minecraft mc) { return position(mc,5.5,112,6.5)&&mc.screen==null; }
    private static void shift(Minecraft mc,boolean active) { crouch=active;mc.player.input.shiftKeyDown=active;mc.player.setShiftKeyDown(active);mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player,active?ServerboundPlayerCommandPacket.Action.PRESS_SHIFT_KEY:ServerboundPlayerCommandPacket.Action.RELEASE_SHIFT_KEY)); }
    @SubscribeEvent public static void input(MovementInputUpdateEvent e) { if(!enabled()||stopped||!crouch)return;var mc=Minecraft.getInstance();if(mc.player!=null&&e.getEntity()==mc.player&&mc.getSingleplayerServer()!=null&&WORLD.equals(mc.getSingleplayerServer().getWorldData().getLevelName())){e.getInput().shiftKeyDown=true;e.getInput().forwardImpulse=e.getInput().leftImpulse=0;e.getInput().up=e.getInput().down=e.getInput().left=e.getInput().right=e.getInput().jumping=false;} }
    private static ArcaneWorkbenchBlockEntity bench(ServerLevel l) { return (ArcaneWorkbenchBlockEntity)l.getBlockEntity(BENCH); }
    private static InfernalFurnaceBlockEntity furnace(ServerLevel l) { require(l.getBlockEntity(CORE) instanceof InfernalFurnaceBlockEntity,"Missing formed native furnace");return (InfernalFurnaceBlockEntity)l.getBlockEntity(CORE); }
    private static void book(Minecraft mc,String key,boolean blueprint) { var k=ResearchClient.golemPressKnowledge();var browser=new ThaumonomiconScreen(k,k.scanCount());var page=new ThaumonomiconPageScreen(browser,ResearchCatalog.get(key),k,k.scanCount());mc.setScreen(page);if(blueprint)try{var m=ThaumonomiconPageScreen.class.getDeclaredMethod("showStructureForSmokeTest",String.class);m.setAccessible(true);m.invoke(page,"infernalfurnace");}catch(ReflectiveOperationException e){throw new IllegalStateException(e);} }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||!enabled()||stopped)return;var mc=Minecraft.getInstance();if(began==0)began=System.nanoTime();
        try { require(System.nanoTime()-began<1_200_000_000_000L,"Infernal QA timeout scene="+scene+" phase="+phase);if(!started){start(mc);return;}if(mc.level==null||mc.player==null||mc.getOverlay()!=null||mc.screen instanceof ReceivingLevelScreen)return;
            require(mc.getSingleplayerServer()!=null&&WORLD.equals(mc.getSingleplayerServer().getWorldData().getLevelName()),"Wrong owned infernal world");mc.getToasts().clear();
            if(++wait%100==0)LogUtils.getLogger().info("THAUMCRAFT_INFERNAL_HEARTBEAT: scene={} phase={} crafts={} placed={} queued={} pending={} gold={} nuggets={} xp={}",scene,phase,crafts,placementIndex,inputCount,pendingCount,goldOutput,nuggetOutput,xpTotal);
            if(work!=null){if(!work.isDone())return;work.join();work=null;}if(!prepared){prepared=true;submit(mc,()->prepare(mc));return;}
            if(captured){if(SAVED.get()==scene+1&&++stable>=20){scene++;phase=stable=wait=ready=0;expectedStage=-1;captured=captureRequested=false;}return;}
            if(scene==IMAGES.length){finish(mc);return;}if(!advance(mc)){stable=0;return;}if(++stable>=12)captureRequested=true;
        } catch(Throwable e) { fail(mc,e); }
    }
    private static boolean advance(Minecraft mc) { return switch(scene) {
        case 0->visit(mc);case 1->scan(mc);case 2->study(mc,"INFERNALFURNACE","ARTIFICE");case 3->study(mc,"BELLOWS","GOLEMANCY");
        case 4->{if(phase==0){phase=1;book(mc,"INFERNALFURNACE",true);}yield mc.screen instanceof ThaumonomiconPageScreen;}
        case 5->arcane(mc);case 6->salis(mc);case 7->assembly(mc);case 8->formation(mc);case 9->placements(mc);case 10->vanilla(mc);case 11->smelting(mc);case 12->outputs(mc);case 13->pause(mc);case 14->dismantle(mc);default->false;}; }
    private static void dimension(ServerPlayer p,ServerLevel target) { p.changeDimension(target,new ITeleporter(){@Override public PortalInfo getPortalInfo(net.minecraft.world.entity.Entity entity,ServerLevel world,java.util.function.Function<ServerLevel,PortalInfo> fallback){return new PortalInfo(new Vec3(.5,112,.5),Vec3.ZERO,0,0);}}); }
    private static boolean visit(Minecraft mc) {
        if(phase==0){phase=1;close(mc);submit(mc,()->{var p=player(mc);var n=p.server.getLevel(Level.NETHER);require(n!=null,"Missing owned Nether");n.getChunk(0,0);for(var pos:BlockPos.betweenClosed(-3,111,-3,3,115,3))n.setBlockAndUpdate(pos,pos.getY()==111?Blocks.BEDROCK.defaultBlockState():Blocks.AIR.defaultBlockState());require(n.getBiome(new BlockPos(0,112,0)).is(BiomeTags.IS_NETHER),"Native Nether biome fixture not tagged");dimension(p,n);LogUtils.getLogger().info("THAUMCRAFT_INFERNAL_NETHER_FIXTURE: actual ownedNetherbiome/7x7safeplatform/nativechangeDimension transport,no fact setter; awaitoriginal200Livingticks");});return false;}
        return mc.level.dimension()==Level.NETHER&&ResearchClient.golemPressKnowledge().isResearchKnown("m_hellandback")&&mc.screen==null;
    }
    private static boolean scan(Minecraft mc) {
        if(phase==0){phase=1;submit(mc,()->{var p=player(mc);require(KnowledgeStore.get(p).isResearchKnown("m_hellandback"),"Native Nether visit did not earn fact");dimension(p,p.server.overworld());p.connection.teleport(-8.5,112,2.5,0,5);stow(p);held(p,new ItemStack(ScanningModule.THAUMOMETER.get()));p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.BLAZE_ROD));p.inventoryMenu.broadcastChanges();});return false;}
        if(phase==3)return mc.screen==null;
        if(mc.level.dimension()!=Level.OVERWORLD||!mc.player.getMainHandItem().is(ScanningModule.THAUMOMETER.get())||!mc.player.getOffhandItem().is(Items.BLAZE_ROD))return false;
        if(phase==1){if(++ready<15)return false;shift(mc,true);mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);phase=2;return false;}
        if(!ResearchClient.golemPressKnowledge().knowsAspect(Aspect.FIRE))return false;shift(mc,false);
        if(phase==2){phase=3;submit(mc,()->{var p=player(mc);require(KnowledgeStore.get(p).knowsAspect(Aspect.FIRE),"Actual BlazeRodscan failedIgnis");var off=p.getOffhandItem();p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);require(p.getInventory().add(off),"Raw physical studyrod notconserved");p.inventoryMenu.broadcastChanges();LogUtils.getLogger().info("THAUMCRAFT_INFERNAL_FACTS: native periodicNetherbiome fact + actualThaumometer offhandBlazeRodC2S Ignis,no targetfacts supplied");});return false;}return mc.screen==null;
    }
    private static boolean study(Minecraft mc,String key,String category) {
        if(phase==0){phase=1;close(mc);submit(mc,()->{var p=player(mc);stow(p);raw(p,category,16);currentKnowledge=KnowledgeStore.get(p).rawKnowledge(KnowledgeType.OBSERVATION,category);currentXp=p.totalExperience;
            if(key.equals("INFERNALFURNACE")){require(p.getInventory().add(new ItemStack(Items.OBSIDIAN))&&p.getInventory().add(new ItemStack(Items.NETHER_BRICKS)),"Raw studyinventory full");require(count(p,Items.BLAZE_ROD)==1,"Physical scannedrod missing");}
            else require(p.getInventory().add(AspectCrystalItem.create(Aspect.AIR,1)),"Raw exactAer crystal missing");stow(p);held(p,new ItemStack(ResearchModule.THAUMONOMICON.get()));ResearchNetwork.sync(p);});return false;}
        var k=ResearchClient.golemPressKnowledge();if(phase==1){if(!mc.player.getMainHandItem().is(ResearchModule.THAUMONOMICON.get()))return false;if(!k.isResearchCompleteStrict(key)){int stage=k.researchStage(key);if(expectedStage==stage)return false;expectedStage=stage;ResearchNetwork.requestAdvance(key,stage);return false;}
            phase=2;submit(mc,()->{var p=player(mc);var actual=KnowledgeStore.get(p);require(actual.isResearchCompleteStrict(key)&&actual.rawKnowledge(KnowledgeType.OBSERVATION,category)==currentKnowledge-16&&p.totalExperience==currentXp+10,"Exact nativepaid study "+key);
                if(key.equals("INFERNALFURNACE"))require(count(p,Items.OBSIDIAN)==0&&count(p,Items.NETHER_BRICKS)==0&&count(p,Items.BLAZE_ROD)==0,"Studyoriginalblocks/rod didnotpay");else require(count(p,"crystal_essence")==0,"BellowsstudyAer didnotpay");studyObservationPaid+=16;studyXpEarned+=10;LogUtils.getLogger().info("THAUMCRAFT_INFERNAL_RESEARCH_PAID: {} observationRaw16/{} actualoriginalitems consumed +10XPeearned",key,category);});return false;}
        if(!(mc.screen instanceof ThaumonomiconPageScreen))book(mc,key,false);return true;
    }
    private static void prepareArcane(ServerPlayer p) {
        var l=p.serverLevel();require(bench(l).isEmpty(),"Prior bench payment unresolved");var recipe=l.getRecipeManager().getAllRecipesFor(ArcaneModule.RECIPE_TYPE.get()).stream().filter(r->r.getResultItem(l.registryAccess()).is(item("bellows").getItem())).findFirst().orElseThrow();require(recipe.vis()==25,"OriginalBellowsvis changed");
        for(int i=0;i<recipe.getIngredients().size();i++){var ingredient=recipe.getIngredients().get(i);if(!ingredient.isEmpty())bench(l).setItem(i%recipe.gridWidth()+i/recipe.gridWidth()*3,ingredient.getItems()[0].copyWithCount(1));}
        for(int i=0;i<6;i++)if(recipe.crystalCost(i)>0)bench(l).setItem(9+i,AspectCrystalItem.create(Aspect.getAspect(ArcaneModule.PRIMALS[i]),recipe.crystalCost(i)));
        var chunk=new ChunkPos(BENCH);for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){var at=new ChunkPos(chunk.x+x,chunk.z+z).getMiddleBlockPosition(112);AuraManager.drainVis(l,at,Float.MAX_VALUE,false);AuraManager.addVis(l,at,250);AuraManager.drainFlux(l,at,Float.MAX_VALUE,false);AuraManager.addFlux(l,at,1000);}beforeVis=AuraManager.getVis(l,BENCH);currentCount=count(p,"bellows");p.inventoryMenu.broadcastChanges();
    }
    private static boolean arcane(Minecraft mc) {
        if(phase==0){phase=1;close(mc);submit(mc,()->{var p=player(mc);stow(p);p.connection.teleport(BENCH.getX()+.5,112,BENCH.getZ()+2.5,180,20);prepareArcane(p);});return false;}
        if(phase==1){if(!position(mc,BENCH.getX()+.5,112,BENCH.getZ()+2.5)||!mc.player.getMainHandItem().isEmpty())return false;phase=2;click(mc,BENCH,Direction.UP);return false;}
        if(!(mc.player.containerMenu instanceof ArcaneWorkbenchMenu menu))return false;
        if(phase==2){if(!menu.craftable()||!menu.getSlot(0).getItem().is(item("bellows").getItem()))return false;require(menu.requiredVis()==25,"Nativebellowsprice changed");phase=3;mc.gameMode.handleInventoryMouseClick(menu.containerId,0,0,ClickType.QUICK_MOVE,mc.player);return false;}
        if(phase==3){if(menu.hasRecipe())return false;phase=4;submit(mc,()->{var p=player(mc);require(bench(p.serverLevel()).isEmpty()&&count(p,"bellows")==currentCount+1&&AuraManager.getVis(p.serverLevel(),BENCH)==beforeVis-25,"Physical bench payment changed");crafts++;LogUtils.getLogger().info("THAUMCRAFT_INFERNAL_BELLOWS_CRAFT_PAID: nativeC2S25vis/Aer1/4planks2leather1iron->oneBellows,total={}",crafts);if(crafts<4){prepareArcane(p);phase=2;}else{var c=new ChunkPos(BENCH);for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)AuraManager.drainFlux(p.serverLevel(),new ChunkPos(c.x+x,c.z+z).getMiddleBlockPosition(112),1000,false);}});return false;}
        return crafts==4&&mc.screen!=null;
    }
    private static boolean salis(Minecraft mc) {
        if(phase==0){phase=1;close(mc);submit(mc,()->{var p=player(mc);stow(p);p.connection.teleport(CRAFT.getX()+.5,112,CRAFT.getZ()+2.5,180,20);currentCount=count(p,"salis_mundus");});return false;}
        if(phase==1){if(!position(mc,CRAFT.getX()+.5,112,CRAFT.getZ()+2.5))return false;phase=2;click(mc,CRAFT,Direction.UP);return false;}
        if(!(mc.player.containerMenu instanceof CraftingMenu menu))return false;
        if(phase==2){phase=3;submit(mc,()->{var p=player(mc);require(p.containerMenu instanceof CraftingMenu,"Nativevanilla craftingmenu missing");var stacks=List.of(new ItemStack(Items.FLINT),new ItemStack(Items.BOWL),new ItemStack(Items.REDSTONE),AspectCrystalItem.create(Aspect.AIR,1),AspectCrystalItem.create(Aspect.FIRE,1),AspectCrystalItem.create(Aspect.WATER,1));for(int i=0;i<stacks.size();i++)p.containerMenu.getSlot(1+i).set(stacks.get(i));p.containerMenu.broadcastChanges();LogUtils.getLogger().info("THAUMCRAFT_INFERNAL_SALIS_FIXTURE: rawflint/bowl/redstone+3distinctcrystals only; nativeCraftingMenu computesactualrecipe");});return false;}
        if(phase==3){if(!menu.getSlot(0).getItem().is(AlchemyModule.SALIS_MUNDUS.get()))return false;phase=4;mc.gameMode.handleInventoryMouseClick(menu.containerId,0,0,ClickType.QUICK_MOVE,mc.player);return false;}
        if(phase==4){if(!menu.getSlot(0).getItem().isEmpty())return false;phase=5;submit(mc,()->{var p=player(mc);require(count(p,"salis_mundus")==currentCount+1,"Salisnativepaymentmissing");for(int i=3;i<=6;i++)require(p.containerMenu.getSlot(i).getItem().isEmpty(),"Salisrawredstone/crystalnotpaid");require(p.containerMenu.getSlot(1).getItem().is(Items.FLINT)&&p.containerMenu.getSlot(2).getItem().is(Items.BOWL),"OriginalSalisflint/bowl remainderlost");LogUtils.getLogger().info("THAUMCRAFT_INFERNAL_SALIS_PAID: actualC2Srecipeconsumesredstone+3distinctcrystals,returnsflint+bowl,onephysicaldust");});return false;}return mc.screen!=null;
    }
    private static boolean assembly(Minecraft mc) {
        if(phase==0){phase=1;close(mc);submit(mc,()->{var p=player(mc);worldView(p);var l=p.serverLevel();for(var part:InfernalFurnaceFormation.parts(CORNER,Direction.SOUTH)){var state=switch(part.id()){case "infernal_furnace"->Blocks.LAVA.defaultBlockState();case "opening"->Blocks.IRON_BARS.defaultBlockState();case "placeholder_brick"->Blocks.NETHER_BRICKS.defaultBlockState();default->Blocks.OBSIDIAN.defaultBlockState();};l.setBlockAndUpdate(part.pos(),state);}require(InfernalFurnaceFormation.match(l,CORNER)!=null,"Original raw3x3x3pattern impossible");var chunk=new ChunkPos(CORE);for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){var at=new ChunkPos(chunk.x+x,chunk.z+z).getMiddleBlockPosition(113);AuraManager.drainVis(l,at,Float.MAX_VALUE,false);AuraManager.addVis(l,at,80);AuraManager.drainFlux(l,at,Float.MAX_VALUE,false);AuraManager.addFlux(l,at,1000);}smeltVisStart=AuraManager.getVis(l,CORE);smeltFluxStart=AuraManager.getFlux(l,CORE);LogUtils.getLogger().info("THAUMCRAFT_INFERNAL_STRUCTURE_FIXTURE: raw12netherbrick/12obsidian/1lava/1bars atoriginal26positions;topcenterwildcard/nativebedrockarena/no finalfurnaceBE/placeholder; raw80vis/highflux9chunks supplied BEFOREformation,originalidle20vischarge included,no speedy budget setter");});return false;}return worldPosition(mc)&&mc.level.getBlockState(CORE).is(Blocks.LAVA);
    }
    private static boolean formation(Minecraft mc) {
        var clicked=CORNER;
        if(phase==0){phase=1;close(mc);submit(mc,()->{var p=player(mc);stow(p);held(p,take(p,"salis_mundus"));p.connection.teleport(.5,112,-2.5,0,-8);});return false;}
        if(phase==1){if(!position(mc,.5,112,-2.5)||!mc.player.getMainHandItem().is(AlchemyModule.SALIS_MUNDUS.get()))return false;phase=2;click(mc,clicked,Direction.NORTH);return false;}
        if(!mc.level.getBlockState(CORE).is(CatalogBlocks.block("infernal_furnace"))||!mc.player.getMainHandItem().isEmpty())return false;
        if(phase==2){phase=3;submit(mc,()->{var p=player(mc);require(furnace(p.serverLevel()).outputFace()==Direction.SOUTH&&count(p,"salis_mundus")==0,"Native dustformation orientation/paymentchanged");require(furnace(p.serverLevel()).speedyTime()==20&&AuraManager.getVis(p.serverLevel(),CORE)==smeltVisStart-20,"Originalidleformation20vischarge notobserved");for(var part:InfernalFurnaceFormation.parts(CORNER,Direction.SOUTH))require(part.id().equals("opening")?p.serverLevel().getBlockState(part.pos()).isAir():p.serverLevel().getBlockState(part.pos()).is(CatalogBlocks.block(part.id())),"Formation missed originalpart"+part);worldView(p);LogUtils.getLogger().info("THAUMCRAFT_INFERNAL_FORMED: actualC2SpaidSalis1/26nativepatternparts/core32slots/12+12shell/airfront/outputSOUTH;originalidle20vischarge->20speedy withnoinputs");});return false;}return worldPosition(mc);
    }
    private static boolean placements(Minecraft mc) {
        if(placementIndex<4){int i=placementIndex;var at=BELLOWS[i];Direction towards=TOWARDS[i];var support=at.relative(towards);
            if(phase==0){phase=1;close(mc);submit(mc,()->{var p=player(mc);stow(p);held(p,take(p,"bellows"));p.connection.teleport(at.getX()+.5,112,at.getZ()-2.5,0,-8);});return false;}
            if(phase==1){if(!position(mc,at.getX()+.5,112,at.getZ()-2.5)||!mc.player.getMainHandItem().is(item("bellows").getItem()))return false;if(i==3)shift(mc,true);phase=2;click(mc,support,towards.getOpposite());return false;}
            if(!(mc.level.getBlockEntity(at) instanceof BellowsBlockEntity)||mc.level.getBlockState(at).getValue(SmelterBellowsBlock.FACING)!=towards||!mc.player.getMainHandItem().isEmpty())return false;
            if(phase==2){shift(mc,false);phase=3;submit(mc,()->{var p=player(mc);require(BellowsBlockEntity.active(p.serverLevel(),at,towards),"Physical paid Bellowsorientationchanged");placementIndex++;phase=0;LogUtils.getLogger().info("THAUMCRAFT_INFERNAL_BELLOWS_PLACED: paidphysical#{} at{} towards{},nativeC2SBlockItem",placementIndex,at,towards);});return false;}
        }
        if(phase==0){phase=4;submit(mc,()->{var p=player(mc);require(count(p,"bellows")==0&&furnace(p.serverLevel()).bellows()==3,"Native paidbellowscensus changed");worldView(p);});return false;}return worldPosition(mc);
    }
    private static boolean vanilla(Minecraft mc) {
        if(phase==0){phase=1;close(mc);submit(mc,()->{var p=player(mc);worldView(p);var f=(FurnaceBlockEntity)p.serverLevel().getBlockEntity(VANILLA);require(f!=null&&f.isEmpty(),"Raw vanilla consumer missing");f.setItem(0,new ItemStack(Items.COBBLESTONE));f.setItem(1,new ItemStack(Items.COAL));vanillaStarted=p.serverLevel().getGameTime();p.connection.teleport(VANILLA.getX()+.5,112,VANILLA.getZ()+2.5,180,20);LogUtils.getLogger().info("THAUMCRAFT_INFERNAL_VANILLA_FIXTURE: rawnativefurnace/1cobble/1coal,paidfourthbellows;ordinaryserverticks,no progress setter");});return false;}
        if(phase==1){if(!position(mc,VANILLA.getX()+.5,112,VANILLA.getZ()+2.5))return false;phase=2;click(mc,VANILLA,Direction.SOUTH);return false;}
        if(!(mc.player.containerMenu instanceof FurnaceMenu menu))return false;
        if(phase==2){if(menu.getBurnProgress()<6)return false;phase=3;submit(mc,()->{var p=player(mc);var f=(FurnaceBlockEntity)p.serverLevel().getBlockEntity(VANILLA);long elapsed=p.serverLevel().getGameTime()-vanillaStarted;require(f.cookingProgress>elapsed+8&&f.getItem(0).is(Items.COBBLESTONE)&&f.getItem(2).isEmpty(),"Nativefurnace2tickboostnotobserved beforefinish");LogUtils.getLogger().info("THAUMCRAFT_INFERNAL_VANILLA_NATIVE: actualelapsed={} cookingProgress={} originalpaidfuel/outputunfinished",elapsed,f.cookingProgress);});return false;}return mc.screen!=null;
    }
    private static boolean smelting(Minecraft mc) {
        if(phase==0){phase=1;close(mc);submit(mc,()->{var p=player(mc);var l=p.serverLevel();worldView(p);require(furnace(l).speedyTime()==20&&AuraManager.getVis(l,CORE)==smeltVisStart-20,"Originalidlepaidbudget changedbeforeinput");currentXp=p.totalExperience;smeltStarted=l.getGameTime();l.random.setSeed(SMELT_RANDOM_SEED);var raw=new ItemEntity(l,CORE.getX()+.5,CORE.getY()+.501,CORE.getZ()+.5,new ItemStack(Items.RAW_GOLD,32));raw.setPickUpDelay(32767);require(l.addFreshEntity(raw),"Rawphysicalore spawnrejected");LogUtils.getLogger().info("THAUMCRAFT_INFERNAL_INPUT_FIXTURE: raw32RawGoldphysicallyspawned atnativehalfheighttop,ordinaryentityground/collision/timer/recipe;preformation80vis/highflux9chunks retained/originalidlepaid20speedy; ownedserverRandomseed={} setONCEbeforephysicalinput,no postproduction reset/pollute/send/FX/output/XP/queue fixture",SMELT_RANDOM_SEED);});return false;}
        if(cookedComplete&&InfernalPollutionEffects.emittedPuffs()==0)throw new AssertionError("Native32smelts completed without purple pollution FX: seed="+SMELT_RANDOM_SEED+",received="+InfernalPollutionEffects.receivedPackets()+",emitted="+InfernalPollutionEffects.emittedPuffs()+",drawn="+InfernalPollutionEffects.drawnPuffs()+",gold="+goldOutput+",XP="+xpTotal);
        if(goldOutput==0||InfernalPollutionEffects.receivedPackets()==0||InfernalPollutionEffects.activePuffs()==0||InfernalPollutionEffects.drawnPuffs()==0)return false;
        if(phase==1){require(CORE.south().equals(InfernalPollutionEffects.lastSource()),"Nativepurplepacketdidnotoriginateatfurnacefront");phase=2;LogUtils.getLogger().info("THAUMCRAFT_INFERNAL_LIVE_POLLUTION_OBSERVED: actual32radiusS2C/source{}/received={} emitted={} live={} drawn={} goldSoFar={} queued={} pending={}; captureordinaryworld DURINGnativeproduction,sourceframe56/RGB1,.3,.9/no syntheticpollution",InfernalPollutionEffects.lastSource(),InfernalPollutionEffects.receivedPackets(),InfernalPollutionEffects.emittedPuffs(),InfernalPollutionEffects.activePuffs(),InfernalPollutionEffects.drawnPuffs(),goldOutput,inputCount,pendingCount);}
        return worldPosition(mc);
    }
    private static void recordSmeltPayment(ServerPlayer p) {
        var l=p.serverLevel();require(furnace(l).queuedInputCount()==0&&furnace(l).pendingOutputCount()==0&&goldOutput==32&&nuggetOutput>0&&xpTotal==32,"Nativeoriginalpaidoutput/bonus/XPownership changed");finalVis=AuraManager.getVis(l,CORE);finalFlux=AuraManager.getFlux(l,CORE);require(finalVis==smeltVisStart-40,"Original20smeltvispulses payment changed");require(furnace(l).speedyTime()==8,"Native40speedy budget minus32outputs changed");LogUtils.getLogger().info("THAUMCRAFT_INFERNAL_SMELTED: realworldticks={} raw32RawGold->32gold +{}bonusnuggets +{}rareearth/32XP;3bellows/cook26/speedy8/visPaid40/Fluxdelta={}/originalevent3={}/actualPurplePackets={} puffs={} draws={},preproductionSeed={}",l.getGameTime()-smeltStarted,nuggetOutput,rareOutput,finalFlux-smeltFluxStart,InfernalFurnaceClientEffects.events(),InfernalPollutionEffects.receivedPackets(),InfernalPollutionEffects.emittedPuffs(),InfernalPollutionEffects.drawnPuffs(),SMELT_RANDOM_SEED);
    }
    private static boolean outputs(Minecraft mc) {
        if(phase==0){if(!cookedComplete)return false;phase=1;close(mc);submit(mc,()->{var p=player(mc);recordSmeltPayment(p);stow(p);pickupStarted=p.serverLevel().getGameTime();p.connection.teleport(1.5,112,3.5,180,0);});return false;}
        if(phase==1){phase=2;submit(mc,()->{var p=player(mc);var l=p.serverLevel();
            if(count(p,Items.GOLD_INGOT)!=32||count(p,Items.GOLD_NUGGET)!=nuggetOutput||count(p,"nugget_rareearth")!=rareOutput||p.totalExperience-currentXp!=32){
                var region=new AABB(CORE).inflate(20);var remaining=l.getEntitiesOfClass(ItemEntity.class,region,e->e.isAlive()&&(e.getItem().is(Items.GOLD_INGOT)||e.getItem().is(Items.GOLD_NUGGET)||e.getItem().is(item("nugget_rareearth").getItem())));
                var orbs=l.getEntitiesOfClass(ExperienceOrb.class,region,e->e.isAlive());
                require(l.getGameTime()-pickupStarted<=500,"Native output pickup timed out: player="+p.position()+",inventoryGold="+count(p,Items.GOLD_INGOT)+",nuggets="+count(p,Items.GOLD_NUGGET)+",rare="+count(p,"nugget_rareearth")+",XPdelta="+(p.totalExperience-currentXp)+",items="+remaining.stream().map(e->e.getItem()+"@"+e.position()).toList()+",orbs="+orbs.stream().map(e->e.getValue()+"x"+e.saveWithoutId(new net.minecraft.nbt.CompoundTag()).getInt("Count")+"@"+e.position()).toList());
                Vec3 target=remaining.stream().min(java.util.Comparator.comparingDouble(e->e.distanceToSqr(p))).map(ItemEntity::position).orElseGet(()->orbs.stream().min(java.util.Comparator.comparingDouble(e->e.distanceToSqr(p))).map(ExperienceOrb::position).orElse(null));
                if(target!=null){double x=target.x,z=target.z;
                    // Original floor below the opening is full obsidian atY112; stand just outside the one-block doorway.
                    if(x>=CORNER.getX()&&x<CORNER.getX()+3&&z>=CORNER.getZ()&&z<CORNER.getZ()+3)z=CORE.getZ()+2.5;
                    if(p.distanceToSqr(x,112,z)>.3){p.connection.teleport(x,112,z,180,0);LogUtils.getLogger().info("THAUMCRAFT_INFERNAL_PHYSICAL_PICKUP_APPROACH: actualremainingentity={} ownedplayer->({},{},{}); onlyplayerconnectionmovement, no entity/item/XP mutation",target,x,112,z);}
                }phase=1;return;
            }
            LogUtils.getLogger().info("THAUMCRAFT_INFERNAL_PHYSICAL_PICKUP_PAID: nativeplayerinventory32gold/{}nuggets/{}rare +32XP,actualplayer={},elapsedWorldTicks={}",nuggetOutput,rareOutput,p.position(),l.getGameTime()-pickupStarted);
            p.connection.teleport(CHEST.getX()+.5,112,CHEST.getZ()+2.5,180,20);});return false;}
        if(phase==2){if(!position(mc,CHEST.getX()+.5,112,CHEST.getZ()+2.5))return false;phase=3;click(mc,CHEST,Direction.SOUTH);return false;}
        if(!(mc.player.containerMenu instanceof ChestMenu menu))return false;
        if(phase==3){for(int slot=27;slot<menu.slots.size();slot++)if(menu.getSlot(slot).getItem().is(Items.GOLD_INGOT)||menu.getSlot(slot).getItem().is(Items.GOLD_NUGGET)||menu.getSlot(slot).getItem().is(item("nugget_rareearth").getItem()))mc.gameMode.handleInventoryMouseClick(menu.containerId,slot,0,ClickType.QUICK_MOVE,mc.player);phase=4;return false;}
        int gold=0,nugget=0;for(int slot=0;slot<27;slot++){var stack=menu.getSlot(slot).getItem();if(stack.is(Items.GOLD_INGOT))gold+=stack.getCount();if(stack.is(Items.GOLD_NUGGET))nugget+=stack.getCount();}return gold==32&&nugget==nuggetOutput;
    }
    private static boolean pause(Minecraft mc) {
        var at=BELLOWS[0];
        if(phase==0){phase=1;close(mc);submit(mc,()->{var p=player(mc);stow(p);held(p,new ItemStack(Items.REDSTONE_BLOCK));p.connection.teleport(at.getX()+.5,112,at.getZ()+2.5,180,-25);});return false;}
        if(phase==1){if(!position(mc,at.getX()+.5,112,at.getZ()+2.5)||!mc.player.getMainHandItem().is(Items.REDSTONE_BLOCK))return false;phase=2;click(mc,at,Direction.UP);return false;}
        if(phase==2){if(!(mc.level.getBlockEntity(at) instanceof BellowsBlockEntity b)||b.getBlockState().getValue(SmelterBellowsBlock.ENABLED))return false;frozenInflation=b.inflation();phase=3;ready=0;return false;}
        if(phase==3){if(++ready<40)return false;require(mc.level.getBlockEntity(at) instanceof BellowsBlockEntity b&&b.inflation()==frozenInflation,"PoweredBellowsanimationdidnotfreeze");phase=4;submit(mc,()->{var p=player(mc);require(furnace(p.serverLevel()).bellows()==2,"Originalpoweredbellowsstillcounts");worldView(p);LogUtils.getLogger().info("THAUMCRAFT_INFERNAL_PAUSED: nativeC2Sredstoneblock/40actualclientticks frozeninflation={}/Infernalbellows3->2",frozenInflation);});return false;}return worldPosition(mc);
    }
    private static boolean dismantle(Minecraft mc) {
        if(phase==0){phase=1;close(mc);submit(mc,()->{var p=player(mc);stow(p);held(p,new ItemStack(Items.DIAMOND_PICKAXE));p.connection.teleport(.5,112,-2.5,0,-8);});return false;}
        if(phase==1){if(!position(mc,.5,112,-2.5)||!mc.player.getMainHandItem().is(Items.DIAMOND_PICKAXE))return false;phase=2;mc.gameMode.startDestroyBlock(CORNER,Direction.NORTH);return false;}
        if(phase==2){if(mc.level.getBlockState(CORNER).is(CatalogBlocks.block("placeholder_brick"))){mc.gameMode.continueDestroyBlock(CORNER,Direction.NORTH);return false;}phase=3;submit(mc,()->{var p=player(mc);var l=p.serverLevel();require(l.getBlockState(CORE).is(Blocks.LAVA)&&l.getBlockState(CORE.south()).is(Blocks.IRON_BARS),"Originalnative reverseblueprintcore/barsnotrestored");for(var part:InfernalFurnaceFormation.parts(CORNER,Direction.SOUTH))if(!part.pos().equals(CORNER)&&part.id().startsWith("placeholder"))require(l.getBlockState(part.pos()).is(part.id().equals("placeholder_brick")?Blocks.NETHER_BRICKS:Blocks.OBSIDIAN),"Nativepaid shellnotrestored"+part);worldView(p);LogUtils.getLogger().info("THAUMCRAFT_INFERNAL_DISMANTLED: actualsurvivalC2Sshellharvest restores23survivingoriginalshellblocks/lava/bars,paidGold/bonus/XPremainowned");});return false;}return worldPosition(mc)&&mc.level.getBlockState(CORE).is(Blocks.LAVA);
    }
    @SubscribeEvent public static void serverTick(TickEvent.ServerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||!enabled()||stopped||!prepared||!WORLD.equals(event.getServer().getWorldData().getLevelName())||scene<11)return;var l=event.getServer().overworld();var p=event.getServer().getPlayerList().getPlayers().stream().findFirst().orElse(null);if(p==null)return;
        var region=new AABB(CORE).inflate(20);goldOutput=count(p,Items.GOLD_INGOT);nuggetOutput=count(p,Items.GOLD_NUGGET);rareOutput=count(p,"nugget_rareearth");
        for(var entity:l.getEntitiesOfClass(ItemEntity.class,region,e->e.isAlive())){var stack=entity.getItem();if(stack.is(Items.GOLD_INGOT))goldOutput+=stack.getCount();if(stack.is(Items.GOLD_NUGGET))nuggetOutput+=stack.getCount();if(stack.is(item("nugget_rareearth").getItem()))rareOutput+=stack.getCount();}
        if(l.getBlockEntity(CHEST) instanceof ChestBlockEntity c)for(int i=0;i<c.getContainerSize();i++){var stack=c.getItem(i);if(stack.is(Items.GOLD_INGOT))goldOutput+=stack.getCount();if(stack.is(Items.GOLD_NUGGET))nuggetOutput+=stack.getCount();if(stack.is(item("nugget_rareearth").getItem()))rareOutput+=stack.getCount();}
        // Native merged orbs store multiple units in Count; Value alone would undercount real ownership.
        xpTotal=p.totalExperience-currentXp+l.getEntitiesOfClass(ExperienceOrb.class,region,e->e.isAlive()).stream().mapToInt(e->e.getValue()*e.saveWithoutId(new net.minecraft.nbt.CompoundTag()).getInt("Count")).sum();
        if(l.getBlockEntity(CORE) instanceof InfernalFurnaceBlockEntity f){inputCount=f.queuedInputCount();pendingCount=f.pendingOutputCount();cookedComplete|=inputCount==0&&pendingCount==0&&goldOutput==32&&xpTotal==32;}
    }
    private static void prepare(Minecraft mc) {
        var p=player(mc);var l=p.serverLevel();p.setInvulnerable(true);for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)l.getChunk(x,z);
        for(var pos:BlockPos.betweenClosed(-14,111,-8,15,119,12))l.setBlockAndUpdate(pos,pos.getY()==111?Blocks.BEDROCK.defaultBlockState():Blocks.AIR.defaultBlockState());l.setDayTime(6000);
        l.setBlockAndUpdate(BENCH,ArcaneModule.WORKBENCH.get().defaultBlockState());l.setBlockAndUpdate(CRAFT,Blocks.CRAFTING_TABLE.defaultBlockState());l.setBlockAndUpdate(VANILLA,Blocks.FURNACE.defaultBlockState());l.setBlockAndUpdate(CHEST,Blocks.CHEST.defaultBlockState());
        p.getInventory().clearContent();for(String key:List.of("FIRSTSTEPS","KNOWLEDGETYPES","UNLOCKARTIFICE","BASEARTIFICE"))stage(p,key);for(String key:List.of("INFERNALFURNACE","BELLOWS","m_hellandback"))require(!KnowledgeStore.get(p).isResearchKnown(key),"Targetstudy/facts supplied "+key);require(!KnowledgeStore.get(p).knowsAspect(Aspect.FIRE),"TargetIgnis supplied");p.connection.teleport(-8.5,112,2.5,0,5);ResearchNetwork.sync(p);
        LogUtils.getLogger().info("THAUMCRAFT_INFERNAL_FIXTURES: isolatedownedSurvivalworld/loadedbedrockarena/actualbench+vanillaCraftingTable/rawFurnace+emptyChest/predecessorstudies only; no targetknowledge/aspect/biomefact/finalBellows/Salis/Furnaceoutput supplied; rawobservations/ingredients/structure/aura deferred,no directruntime tick/queue/eject/formationcall");
    }
    @SubscribeEvent public static void rendered(TickEvent.RenderTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||!enabled()||stopped||!captureRequested||captured)return;var mc=Minecraft.getInstance();try{captured=true;captureRequested=false;String name="tc6-infernal-"+IMAGES[scene]+".png";var file=new File(new File(mc.gameDirectory,"screenshots"),name);Files.deleteIfExists(file.toPath());Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),message->{if(file.isFile()&&file.length()>0){SAVED.incrementAndGet();LogUtils.getLogger().info("THAUMCRAFT_INFERNAL_CLIENT_SMOKE_IMAGE: {}",file.getAbsolutePath());}else mc.execute(()->fail(mc,new AssertionError("Missing freshownednative capture")));});}catch(Throwable e){fail(mc,e);} }
    private static void start(Minecraft mc) {
        if(mc.screen instanceof AccessibilityOnboardingScreen){mc.options.onboardAccessibility=false;mc.options.save();mc.setScreen(new TitleScreen());return;}if(!(mc.screen instanceof TitleScreen)||mc.getOverlay()!=null)return;started=true;previous=mc.options.tutorialStep;mc.options.tutorialStep=TutorialSteps.NONE;mc.getTutorial().stop();mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(3);mc.options.simulationDistance().set(5);mc.options.guiScale().set(2);mc.options.fov().set(80);mc.options.cloudStatus().set(CloudStatus.OFF);mc.options.setCameraType(CameraType.FIRST_PERSON);mc.options.hideGui=false;mc.resizeDisplay();var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);rules.getRule(GameRules.RULE_DAYLIGHT).set(false,null);rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false,null);mc.createWorldOpenFlows().createFreshLevel(WORLD,new LevelSettings(WORLD,GameType.SURVIVAL,false,Difficulty.PEACEFUL,true,rules,WorldDataConfiguration.DEFAULT),new WorldOptions(0x54433633L,false,false),WorldPresets::createNormalWorldDimensions);
    }
    private static void finish(Minecraft mc) {
        if(phase==0){phase=1;close(mc);shift(mc,false);submit(mc,()->{var p=player(mc);var l=p.serverLevel();require(studyObservationPaid==32&&studyXpEarned==20&&crafts==4&&placementIndex==4&&count(p,"bellows")==0&&count(p,"salis_mundus")==0,"Finalnativeprogression/paymentcensus changed");require(goldOutput==32&&nuggetOutput>0&&xpTotal==32&&p.totalExperience==52&&l.getBlockState(CORE).is(Blocks.LAVA),"Finalactualitem/bonus/XP/reverseblueprintownership changed");var f=(FurnaceBlockEntity)l.getBlockEntity(VANILLA);require(f.getItem(0).isEmpty()&&f.getItem(2).is(Items.STONE)&&f.getItem(2).getCount()==1,"Nativeboostedvanilla finalcobblepayment changed");for(int i=0;i<4;i++)require(l.getBlockEntity(BELLOWS[i]) instanceof BellowsBlockEntity,"PaidplacedBellows lost");LogUtils.getLogger().info("THAUMCRAFT_INFERNAL_CLIENT_FACTS: scenes={},studies2,observationsRawPaid32/ARTIFICE16+GOLEMANCY16,studyXPEarned20,paidBenchCrafts4/vis100/Aer4,paidSalis1/redstone1+3crystals/bowlFlintReturned,survivalBellows4,formedFurnace1/dust1,physicalGold32,nativeGoldBonus={},nativeRareEarthBonus={},nativeXP32,visPaid={},FluxDelta={},nativeLavaEvents={},nativeLavaParticles={},bellowsRender={},actualSurvivalTeardown1; explicitrawfixtures,no fullsurvival/portalbuilding/multiplayer/audio/allanglesclaim",SAVED.get(),nuggetOutput,rareOutput,smeltVisStart-finalVis,finalFlux-smeltFluxStart,InfernalFurnaceClientEffects.events(),InfernalFurnaceClientEffects.particles(),BellowsRenderer.audit());finalValidated=true;});return;}
        if(!finalValidated)return;LogUtils.getLogger().info("THAUMCRAFT_INFERNAL_POLLUTION_FINAL_OBSERVED: source={} nativeReceivedPackets={} emittedOriginalPuffs={} actualPuffDraws={} preproductionSeed={} no postproductionFlux/RNG/send/FX/queue/output replacement",InfernalPollutionEffects.lastSource(),InfernalPollutionEffects.receivedPackets(),InfernalPollutionEffects.emittedPuffs(),InfernalPollutionEffects.drawnPuffs(),SMELT_RANDOM_SEED);require(SAVED.get()==IMAGES.length&&BellowsRenderer.animatedDrawn()&&InfernalFurnaceClientEffects.events()==32&&InfernalFurnaceClientEffects.particles()==160&&InfernalPollutionEffects.receivedPackets()>0&&InfernalPollutionEffects.emittedPuffs()==InfernalPollutionEffects.receivedPackets()&&InfernalPollutionEffects.drawnPuffs()>0,"Actualnativevisualcoverage incomplete");for(String path:List.of("textures/blocks/bellows.png","textures/blocks/infernal_furnace.png","textures/models/bore.png","textures/misc/particles.png"))require(mc.getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath("thaumcraft",path)).isPresent(),"Missing originalresource"+path);stopped=true;mc.options.tutorialStep=previous;LogUtils.getLogger().info("THAUMCRAFT_INFERNAL_RENDER_AUDIT_OK: {} actualstudy/blueprint/paidbench/paidSalis/nativeworld/furnaceGUI/chestPNG,originalanimatedworldBellows/staticoriginalinventory/lavaFX/native32radiuspurplePollutePacket/liveframe56/nativefurnaceblockmodel/no gallery",SAVED.get());LogUtils.getLogger().info("THAUMCRAFT_INFERNAL_CLIENT_SMOKE_OK: {} scenes; nativebiome200tickfact/Ignis scan/2paidstudies/4paidbellows/paidSalis/nativeformation/itemcollision32RawGold/output+bonus+32XP/40vis/vanillaboost/redstonepause/survivalteardown/actualPurplePacketPuffs={}; explicitownedfixtures,world={}",SAVED.get(),InfernalPollutionEffects.emittedPuffs(),WORLD);mc.stop();
    }
    private static void fail(Minecraft mc,Throwable e) { if(stopped)return;stopped=true;if(previous!=null)mc.options.tutorialStep=previous;LogUtils.getLogger().error("THAUMCRAFT_INFERNAL_CLIENT_SMOKE_FAILED",e);mc.stop(); }
}
