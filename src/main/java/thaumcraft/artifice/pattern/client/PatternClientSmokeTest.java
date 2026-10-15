package thaumcraft.artifice.pattern.client;

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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
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
import thaumcraft.artifice.dioptra.*;
import thaumcraft.artifice.pattern.*;
import thaumcraft.catalog.blocks.CatalogBlocks;
import thaumcraft.client.research.*;
import thaumcraft.research.*;
import thaumcraft.scanning.ScanningModule;
import thaumcraft.world.aura.AuraManager;

/** Opt-in owned Survival fixtures. Consumer state advances only through native world ticks and C2S actions. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public final class PatternClientSmokeTest {
    private static final String WORLD="thaumcraft-pattern-smoke-"+System.currentTimeMillis();
    private static final String[] IMAGES={"native-sensus-scan","native-auram-scan","native-fabrico-scan","paid-dioptra-study","paid-dioptra-bench","survival-dioptra-vis","native-dioptra-flux","paid-pattern-study","paid-pattern-bench","survival-pattern-placement","die-0-nine","die-1-one","die-2-horizontal-two","die-3-vertical-two","die-4-square-four","die-5-horizontal-three","die-6-vertical-three-unmatched","die-7-horizontal-six","die-8-vertical-six","die-9-ring-eight","native-redstone-pause","native-redstone-resume","survival-dismantling"};
    private static final BlockPos BENCH=new BlockPos(-12,112,5),DIOPTRA=new BlockPos(7,112,0),CRAFTER=new BlockPos(0,114,0);
    private static final int[] AMOUNTS={9,1,2,2,4,3,3,3,6,8};
    private static final Item[] INPUTS={Items.WHEAT,Items.OAK_LOG,Items.OAK_PLANKS,Items.OAK_PLANKS,Items.OAK_PLANKS,Items.OAK_PLANKS,Items.OAK_PLANKS,Items.OAK_PLANKS,Items.OAK_PLANKS,Items.OAK_PLANKS};
    private static final Item[] OUTPUTS={Items.HAY_BLOCK,Items.OAK_PLANKS,Items.OAK_PRESSURE_PLATE,Items.STICK,Items.CRAFTING_TABLE,Items.OAK_SLAB,Items.AIR,Items.OAK_TRAPDOOR,Items.OAK_DOOR,Items.CHEST};
    private static final int[] OUTPUT_AMOUNTS={1,4,1,4,1,6,0,2,3,1};
    private static final AtomicInteger SAVED=new AtomicInteger();
    private static boolean started,prepared,stopped,captureRequested,captured,validated,crouch;
    private static int scene,phase,wait,stable,ready,expectedStage=-1,benchCrafts,theoryPaid,observationPaid,xpEarned,currentXp,currentItems,placed,harvested,modeCycles,nativeCrafts,currentOutput,pauseInput,pauseOutput;
    private static int currentObservation;
    private static final int[] currentTheories=new int[3];
    private static volatile boolean monitorResume,resumePaymentObserved;
    private static volatile int nativePowerSpent,nativeVisRefills;
    private static float previousPower;
    private static volatile int serverMode=-1,serverInput,serverOutput,gridCenter,serverPower;
    private static long began,phaseStarted,pauseStarted;
    private static volatile long observedWorldTime;
    private static float visBefore,powerBefore;
    private static CompletableFuture<Void> work;
    private static TutorialSteps previous;
    private PatternClientSmokeTest() {}
    private static boolean enabled(){return Boolean.getBoolean("thaumcraft.patternSmokeTest");}
    private static void require(boolean b,String message){if(!b)throw new AssertionError(message);}
    private static ServerPlayer player(Minecraft mc){var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());require(p!=null,"Missing owned Pattern player");return p;}
    private static void submit(Minecraft mc,Runnable action){require(work==null,"Overlapping owned server tasks");var result=new CompletableFuture<Void>();work=result;mc.getSingleplayerServer().execute(()->{try{action.run();result.complete(null);}catch(Throwable error){result.completeExceptionally(error);}});}
    private static ItemStack item(String id){var value=ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("thaumcraft",id));require(value!=null&&value!=Items.AIR,"Missing native item "+id);return new ItemStack(value);}
    private static int count(ServerPlayer p,Item item){int n=0;for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i).is(item))n+=p.getInventory().getItem(i).getCount();return n;}
    private static int count(ServerPlayer p,String id){return count(p,item(id).getItem());}
    private static void stow(ServerPlayer p){var stack=p.getMainHandItem();if(!stack.isEmpty()){p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);for(int i=1;i<36&&!stack.isEmpty();i++){var slot=p.getInventory().getItem(i);if(!slot.isEmpty()&&ItemStack.isSameItemSameTags(slot,stack)){int n=Math.min(stack.getCount(),slot.getMaxStackSize()-slot.getCount());slot.grow(n);stack.shrink(n);}}for(int i=1;i<36&&!stack.isEmpty();i++)if(p.getInventory().getItem(i).isEmpty()){p.getInventory().setItem(i,stack);stack=ItemStack.EMPTY;}require(stack.isEmpty(),"Cannot conserve held stack");}p.inventoryMenu.broadcastChanges();}
    private static void held(ServerPlayer p,ItemStack stack){require(p.getMainHandItem().isEmpty(),"Replacing physical held item");p.getInventory().selected=0;p.setItemInHand(InteractionHand.MAIN_HAND,stack);p.connection.send(new ClientboundSetCarriedItemPacket(0));p.inventoryMenu.broadcastChanges();}
    private static ItemStack take(ServerPlayer p,String id){for(int i=0;i<36;i++){var s=p.getInventory().getItem(i);if(s.is(item(id).getItem())){var taken=s.split(1);if(s.isEmpty())p.getInventory().setItem(i,ItemStack.EMPTY);p.inventoryMenu.broadcastChanges();return taken;}}throw new AssertionError("Missing physically paid "+id);}
    private static void predecessor(ServerPlayer p,String key){try{var m=PlayerKnowledge.class.getDeclaredMethod("setResearchStage",String.class,int.class);m.setAccessible(true);require((boolean)m.invoke(KnowledgeStore.get(p),key,ResearchCatalog.get(key).stages().size()+1),"Raw predecessor "+key);}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}}
    private static void rawKnowledge(ServerPlayer p,KnowledgeType type,String category,int amount){try{var m=PlayerKnowledge.class.getDeclaredMethod("addKnowledge",KnowledgeType.class,String.class,int.class);m.setAccessible(true);require((boolean)m.invoke(KnowledgeStore.get(p),type,category,amount),"Raw knowledge fixture");}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}}
    private static void close(Minecraft mc){mc.player.closeContainer();mc.setScreen(null);}
    private static void click(Minecraft mc,BlockPos pos,Direction face){mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(pos.getCenter().add(Vec3.atLowerCornerOf(face.getNormal()).scale(.5)),face,pos,false));}
    private static void knob(Minecraft mc){mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(new Vec3(CRAFTER.getX()+.5,CRAFTER.getY()+.25,CRAFTER.getZ()+.1875),Direction.NORTH,CRAFTER,false));}
    private static boolean position(Minecraft mc,double x,double y,double z){if(mc.player.distanceToSqr(x,y,z)>.3){ready=0;return false;}return ++ready>=5;}
    private static void view(ServerPlayer p,boolean dioptra){stow(p);p.connection.teleport(dioptra?10.5:2.5,112,dioptra?4.5:-3.5,dioptra?143:35,dioptra?1:-8);}
    private static boolean viewed(Minecraft mc,boolean dioptra){return position(mc,dioptra?10.5:2.5,112,dioptra?4.5:-3.5)&&mc.screen==null;}
    private static ArcaneWorkbenchBlockEntity bench(ServerLevel l){return (ArcaneWorkbenchBlockEntity)l.getBlockEntity(BENCH);}
    private static PatternCrafterBlockEntity crafter(ServerLevel l){return (PatternCrafterBlockEntity)l.getBlockEntity(CRAFTER);}
    private static ChestBlockEntity input(ServerLevel l){return (ChestBlockEntity)l.getBlockEntity(CRAFTER.above());}
    private static ChestBlockEntity output(ServerLevel l){return (ChestBlockEntity)l.getBlockEntity(CRAFTER.below());}
    private static int contents(ChestBlockEntity chest,Item item){int n=0;for(int i=0;i<chest.getContainerSize();i++)if(chest.getItem(i).is(item))n+=chest.getItem(i).getCount();return n;}
    private static int contents(ChestBlockEntity chest){int n=0;for(int i=0;i<chest.getContainerSize();i++)n+=chest.getItem(i).getCount();return n;}
    private static void book(Minecraft mc,String key){var k=ResearchClient.golemPressKnowledge();var browser=new ThaumonomiconScreen(k,k.scanCount());mc.setScreen(new ThaumonomiconPageScreen(browser,ResearchCatalog.get(key),k,k.scanCount()));}
    private static void shift(Minecraft mc,boolean value){crouch=value;mc.player.input.shiftKeyDown=value;mc.player.setShiftKeyDown(value);mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player,value?ServerboundPlayerCommandPacket.Action.PRESS_SHIFT_KEY:ServerboundPlayerCommandPacket.Action.RELEASE_SHIFT_KEY));}
    @SubscribeEvent public static void input(MovementInputUpdateEvent event){if(!enabled()||stopped||!crouch)return;var mc=Minecraft.getInstance();if(mc.player==event.getEntity()&&mc.getSingleplayerServer()!=null&&WORLD.equals(mc.getSingleplayerServer().getWorldData().getLevelName())){var in=event.getInput();in.shiftKeyDown=true;in.forwardImpulse=in.leftImpulse=0;in.up=in.down=in.left=in.right=in.jumping=false;}}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!enabled()||stopped)return;var mc=Minecraft.getInstance();if(began==0)began=System.nanoTime();try{
            require(System.nanoTime()-began<1_800_000_000_000L,"Pattern timeout scene="+scene+" phase="+phase);if(!started){start(mc);return;}if(mc.level==null||mc.player==null||mc.getOverlay()!=null||mc.screen instanceof ReceivingLevelScreen)return;
            require(mc.getSingleplayerServer()!=null&&WORLD.equals(mc.getSingleplayerServer().getWorldData().getLevelName()),"Wrong owned Pattern world");mc.getToasts().clear();
            if(++wait%100==0)LogUtils.getLogger().info("THAUMCRAFT_PATTERN_HEARTBEAT: scene={} phase={} bench={} nativeCrafts={} mode={} input={} output={} power={} gridCenter={} renderModes={} movingFrames={}",scene,phase,benchCrafts,nativeCrafts,serverMode,serverInput,serverOutput,serverPower,gridCenter,PatternCrafterRenderer.seenModes(),PatternCrafterRenderer.movingFrames());
            if(work!=null){if(!work.isDone())return;work.join();work=null;}if(!prepared){prepared=true;submit(mc,()->prepare(mc));return;}
            if(captured){if(SAVED.get()==scene+1&&++stable>=20){scene++;phase=wait=stable=ready=0;expectedStage=-1;captured=captureRequested=false;}return;}if(scene==IMAGES.length){finish(mc);return;}
            if(!advance(mc)){stable=0;return;}if(++stable>=12)captureRequested=true;
        }catch(Throwable error){fail(mc,error);}
    }
    private static boolean advance(Minecraft mc){return switch(scene){case 0->scan(mc,Aspect.SENSES);case 1->scan(mc,Aspect.AURA);case 2->scan(mc,Aspect.CRAFT);case 3->study(mc,false);case 4->arcane(mc,false);case 5->place(mc,false);case 6->flux(mc);case 7->study(mc,true);case 8->arcane(mc,true);case 9->place(mc,true);case 20->pause(mc);case 21->resume(mc);case 22->dismantle(mc);default->scene>=10&&scene<20&&mode(mc,scene-10);};}
    private static boolean scan(Minecraft mc,Aspect aspect){
        if(phase==0){phase=1;close(mc);submit(mc,()->{var p=player(mc);stow(p);held(p,new ItemStack(ScanningModule.THAUMOMETER.get()));require(p.getOffhandItem().isEmpty(),"Raw scan offhand occupied");p.setItemInHand(InteractionHand.OFF_HAND,AspectCrystalItem.create(aspect,1));p.inventoryMenu.broadcastChanges();});return false;}
        if(phase==1){if(!mc.player.getMainHandItem().is(ScanningModule.THAUMOMETER.get())||mc.player.getOffhandItem().isEmpty()||++ready<15)return false;shift(mc,true);mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);phase=2;return false;}
        if(phase==2){if(!ResearchClient.golemPressKnowledge().knowsAspect(aspect))return false;shift(mc,false);phase=3;submit(mc,()->{var p=player(mc);require(KnowledgeStore.get(p).knowsAspect(aspect),"Native aspect scan not earned");var stack=p.getOffhandItem();p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);require(p.getInventory().add(stack),"Cannot conserve scanned raw crystal");p.inventoryMenu.broadcastChanges();LogUtils.getLogger().info("THAUMCRAFT_PATTERN_ASPECT_SCANNED: actualThaumometerC2S/rawtypedcrystal/{}/no targetaspectsetter",aspect.getTag());});return false;}return mc.screen==null;
    }
    private static boolean study(Minecraft mc,boolean pattern){
        String key=pattern?"ARCANEPATTERNCRAFTER":"DIOPTRA";
        if(phase==0){phase=1;close(mc);submit(mc,()->{var p=player(mc);stow(p);if(pattern)for(String category:List.of("GOLEMANCY","ARTIFICE","AUROMANCY"))rawKnowledge(p,KnowledgeType.THEORY,category,32);else rawKnowledge(p,KnowledgeType.OBSERVATION,"AUROMANCY",16);currentXp=p.totalExperience;currentObservation=KnowledgeStore.get(p).rawKnowledge(KnowledgeType.OBSERVATION,"AUROMANCY");for(int i=0;i<3;i++)currentTheories[i]=KnowledgeStore.get(p).rawKnowledge(KnowledgeType.THEORY,List.of("GOLEMANCY","ARTIFICE","AUROMANCY").get(i));held(p,new ItemStack(ResearchModule.THAUMONOMICON.get()));ResearchNetwork.sync(p);});return false;}
        if(phase==1){if(!mc.player.getMainHandItem().is(ResearchModule.THAUMONOMICON.get()))return false;var k=ResearchClient.golemPressKnowledge();if(!k.isResearchCompleteStrict(key)){int current=k.researchStage(key);if(expectedStage==current)return false;expectedStage=current;ResearchNetwork.requestAdvance(key,current);return false;}phase=2;submit(mc,()->{var p=player(mc);require(p.totalExperience==currentXp+10,"Original paid study XP changed");if(pattern){for(int i=0;i<3;i++){String c=List.of("GOLEMANCY","ARTIFICE","AUROMANCY").get(i);require(KnowledgeStore.get(p).rawKnowledge(KnowledgeType.THEORY,c)==currentTheories[i]-32,"Original32rawTheory "+c+" not paid");}theoryPaid+=96;}else{require(KnowledgeStore.get(p).rawKnowledge(KnowledgeType.OBSERVATION,"AUROMANCY")==currentObservation-16,"Original16rawObservation not paid");observationPaid+=16;}xpEarned+=10;LogUtils.getLogger().info("THAUMCRAFT_PATTERN_STUDY_PAID: {} actualnativeC2S/{}rawknowledge/+10earnedXP/actualdiscoveredaspects/noitemcost",key,pattern?96:16);});return false;}
        if(!(mc.screen instanceof ThaumonomiconPageScreen))book(mc,key);return true;
    }
    private static void benchInputs(ServerPlayer p,String id){var l=p.serverLevel();require(bench(l).isEmpty(),"Previous bench inputs retained");var r=l.getRecipeManager().getAllRecipesFor(ArcaneModule.RECIPE_TYPE.get()).stream().filter(x->x.getResultItem(l.registryAccess()).is(item(id).getItem())).findFirst().orElseThrow();require(r.vis()==50,"Original50vis price changed");for(int i=0;i<r.getIngredients().size();i++){var ingredient=r.getIngredients().get(i);if(!ingredient.isEmpty())bench(l).setItem(i%r.gridWidth()+i/r.gridWidth()*3,ingredient.getItems()[0].copyWithCount(1));}for(int i=0;i<6;i++)if(r.crystalCost(i)>0)bench(l).setItem(9+i,AspectCrystalItem.create(Aspect.getAspect(ArcaneModule.PRIMALS[i]),r.crystalCost(i)));currentItems=count(p,id);visBefore=AuraManager.getVis(l,BENCH);p.inventoryMenu.broadcastChanges();}
    private static boolean arcane(Minecraft mc,boolean pattern){String id=pattern?"pattern_crafter":"dioptra";
        if(phase==0){phase=1;close(mc);submit(mc,()->{var p=player(mc);stow(p);p.connection.teleport(BENCH.getX()+.5,112,BENCH.getZ()+2.5,180,20);benchInputs(p,id);});return false;}
        if(phase==1){if(!position(mc,BENCH.getX()+.5,112,BENCH.getZ()+2.5)||!mc.player.getMainHandItem().isEmpty())return false;phase=2;click(mc,BENCH,Direction.UP);return false;}
        if(!(mc.player.containerMenu instanceof ArcaneWorkbenchMenu menu))return false;
        if(phase==2){if(!menu.craftable()||!menu.getSlot(0).getItem().is(item(id).getItem()))return false;require(menu.requiredVis()==50,"Native price changed");phase=3;mc.gameMode.handleInventoryMouseClick(menu.containerId,0,0,ClickType.QUICK_MOVE,mc.player);return false;}
        if(phase==3){if(menu.hasRecipe())return false;phase=4;submit(mc,()->{var p=player(mc);require(bench(p.serverLevel()).isEmpty()&&count(p,id)==currentItems+1&&AuraManager.getVis(p.serverLevel(),BENCH)==visBefore-50,"Native bench physical/vis payment changed");benchCrafts++;LogUtils.getLogger().info("THAUMCRAFT_PATTERN_BENCH_PAID: {} actualnativeC2S/50vis/{}crystals/physicaloriginalingredients->oneactualoutput,total={}",id,pattern?3:2,benchCrafts);});return false;}return true;
    }
    private static boolean place(Minecraft mc,boolean pattern){var pos=pattern?CRAFTER:DIOPTRA;String id=pattern?"pattern_crafter":"dioptra";
        if(phase==0){phase=1;close(mc);submit(mc,()->{var p=player(mc);stow(p);if(pattern){p.serverLevel().setBlockAndUpdate(CRAFTER.below(),Blocks.CHEST.defaultBlockState());p.serverLevel().setBlockAndUpdate(CRAFTER.above(),Blocks.CHEST.defaultBlockState());}held(p,take(p,id));p.connection.teleport(pos.getX()+.5,112,pos.getZ()-2.5,0,pattern?-15:25);});return false;}
        if(phase==1){if(!position(mc,pos.getX()+.5,112,pos.getZ()-2.5)||!mc.player.getMainHandItem().is(item(id).getItem()))return false;if(pattern)shift(mc,true);phase=2;click(mc,pos.below(),Direction.UP);return false;}
        if(phase==2){if(!mc.level.getBlockState(pos).is(CatalogBlocks.block(id)))return false;shift(mc,false);phase=3;submit(mc,()->{var p=player(mc);require(count(p,id)==0,"Survival placement failed payment");if(pattern)require(crafter(p.serverLevel()).type==0&&p.serverLevel().getBlockState(CRAFTER).getValue(PatternCrafterBlock.FACING)==Direction.SOUTH,"Original horizontal placing facing/type changed");placed++;view(p,!pattern);LogUtils.getLogger().info("THAUMCRAFT_PATTERN_SURVIVAL_PLACED: {} nativeC2S/paidBlockItem/no forcedtargetstate",id);});return false;}
        return viewed(mc,!pattern)&&(pattern?PatternCrafterRenderer.renderedFrames()>0:DioptraRenderer.visFrames()>0&&DioptraRenderer.nonzeroFrames()>0&&gridCenter>0);
    }
    private static boolean flux(Minecraft mc){
        if(phase==0){phase=1;submit(mc,()->{var p=player(mc);stow(p);p.connection.teleport(DIOPTRA.getX()+.5,112,-2.5,0,10);});return false;}
        if(phase==1){if(!position(mc,DIOPTRA.getX()+.5,112,-2.5)||!mc.player.getMainHandItem().isEmpty())return false;phase=2;click(mc,DIOPTRA,Direction.NORTH);return false;}
        if(phase==2){if(mc.level.getBlockState(DIOPTRA).getValue(DioptraBlock.ENABLED)||gridCenter<=0||gridCenter>16)return false;phase=3;submit(mc,()->{view(player(mc),true);LogUtils.getLogger().info("THAUMCRAFT_PATTERN_DIOPTRA_FLUX: actualnativeC2Stoggle/169nativecachedaurasamples/centerbyte{}/no directgridsetter",gridCenter);});return false;}return viewed(mc,true)&&DioptraRenderer.fluxFrames()>0;
    }
    private static boolean mode(Minecraft mc,int mode){require(wait<3000,"Native die/recipe stalled mode="+mode);
        if(phase==0){phase=1;close(mc);submit(mc,()->{var p=player(mc);stow(p);require(crafter(p.serverLevel()).type==(mode==0?0:mode-1),"Previous original die changed");p.connection.teleport(.5,112,-2.5,0,-12);});return false;}
        if(phase==1){if(!position(mc,.5,112,-2.5)||!mc.player.getMainHandItem().isEmpty())return false;if(mode>0){phase=2;knob(mc);return false;}phase=2;}
        if(phase==2){if(!(mc.level.getBlockEntity(CRAFTER) instanceof PatternCrafterBlockEntity c)||c.type!=mode||serverMode!=mode)return false;phase=3;submit(mc,()->{var p=player(mc);if(mode>0)modeCycles++;require(contents(input(p.serverLevel()))==(mode==7?3:0),"Unexpected physical input carryover");currentOutput=contents(output(p.serverLevel()),OUTPUTS[mode]);powerBefore=crafter(p.serverLevel()).power();held(p,new ItemStack(INPUTS[mode],AMOUNTS[mode]));phaseStarted=p.serverLevel().getGameTime();});return false;}
        if(phase==3){if(!mc.player.getMainHandItem().is(INPUTS[mode])||mc.player.getMainHandItem().getCount()!=AMOUNTS[mode])return false;phase=4;click(mc,CRAFTER.above(),Direction.NORTH);return false;}
        if(!(mc.player.containerMenu instanceof ChestMenu)&&phase==4)return false;
        if(phase==4){var menu=(ChestMenu)mc.player.containerMenu;require(menu.getRowCount()==3,"Expected original single source chest");phase=5;mc.gameMode.handleInventoryMouseClick(menu.containerId,54,0,ClickType.QUICK_MOVE,mc.player);return false;}
        if(phase==5){if(!mc.player.getMainHandItem().isEmpty())return false;phase=6;close(mc);submit(mc,()->view(player(mc),false));return false;}
        if(mode==6){if(observedWorldTime-phaseStarted<80)return false;if(phase==6){phase=7;submit(mc,()->{var l=player(mc).serverLevel();require(contents(input(l))==3&&contents(output(l),Items.AIR)==currentOutput&&crafter(l).power()==powerBefore,"Unmatched native3vertical die consumed input/power");LogUtils.getLogger().info("THAUMCRAFT_PATTERN_NATIVE_UNMATCHED: mode6/threeverticalplanks/no vanilla recipe/80actualworldticks/physicalinput3conserved/power{} unchanged",powerBefore);});return false;}}
        else {if(serverInput!=0)return false;if(phase==6){phase=7;submit(mc,()->{var l=player(mc).serverLevel();require(contents(input(l))==0&&contents(output(l),OUTPUTS[mode])==currentOutput+OUTPUT_AMOUNTS[mode],"Native die physical recipe payment/output changed");nativeCrafts++;LogUtils.getLogger().info("THAUMCRAFT_PATTERN_NATIVE_CRAFT: mode{}/actualnativeworldticks/physical{}{}->{}{} /onepower/total{}",mode,mode==7?6:AMOUNTS[mode],INPUTS[mode],OUTPUT_AMOUNTS[mode],OUTPUTS[mode],nativeCrafts);});return false;}}
        return viewed(mc,false)&&(PatternCrafterRenderer.seenModes()&(1<<mode))!=0;
    }
    private static boolean pause(Minecraft mc){
        if(phase==0){phase=1;submit(mc,()->{var p=player(mc);stow(p);held(p,new ItemStack(Items.REDSTONE_BLOCK));p.connection.teleport(2.5,112,-2.5,35,-12);});return false;}
        if(phase==1){if(!position(mc,2.5,112,-2.5)||!mc.player.getMainHandItem().is(Items.REDSTONE_BLOCK))return false;shift(mc,true);phase=2;click(mc,CRAFTER,Direction.EAST);return false;}
        if(phase==2){if(!mc.level.getBlockState(CRAFTER.east()).is(Blocks.REDSTONE_BLOCK))return false;shift(mc,false);phase=3;submit(mc,()->{var p=player(mc);stow(p);require(!p.serverLevel().getBlockState(CRAFTER).getValue(PatternCrafterBlock.ENABLED),"Native redstone did not disable");held(p,new ItemStack(Items.OAK_PLANKS,8));p.connection.teleport(.5,112,-2.5,0,-12);powerBefore=crafter(p.serverLevel()).power();pauseOutput=contents(output(p.serverLevel()));});return false;}
        if(phase==3){if(!position(mc,.5,112,-2.5)||!mc.player.getMainHandItem().is(Items.OAK_PLANKS))return false;phase=4;click(mc,CRAFTER.above(),Direction.NORTH);return false;}
        if(phase==4){if(!(mc.player.containerMenu instanceof ChestMenu menu))return false;phase=5;mc.gameMode.handleInventoryMouseClick(menu.containerId,54,0,ClickType.QUICK_MOVE,mc.player);return false;}
        if(phase==5){if(!mc.player.getMainHandItem().isEmpty())return false;phase=6;close(mc);submit(mc,()->{var p=player(mc);pauseInput=contents(input(p.serverLevel()));require(pauseInput==8,"Native paused source feed absent");pauseStarted=p.serverLevel().getGameTime();p.connection.teleport(.5,112,-2.5,0,12);});return false;}
        if(phase==6){if(observedWorldTime-pauseStarted<80)return false;phase=7;submit(mc,()->{var l=player(mc).serverLevel();require(contents(input(l))==pauseInput&&contents(output(l))==pauseOutput&&crafter(l).power()==powerBefore,"Powered native crafter changed physical inventory/power");LogUtils.getLogger().info("THAUMCRAFT_PATTERN_NATIVE_PAUSE: actualC2Sredstone/{}worldticks/input8/output{} /power{} unchanged",l.getGameTime()-pauseStarted,pauseOutput,powerBefore);});return false;}
        if(phase==7){if(!position(mc,.5,112,-2.5)||!mc.player.getMainHandItem().isEmpty())return false;phase=8;click(mc,CRAFTER.below(),Direction.NORTH);return false;}return mc.player.containerMenu instanceof ChestMenu;
    }
    private static boolean resume(Minecraft mc){
        if(phase==0){phase=1;close(mc);submit(mc,()->{var p=player(mc);stow(p);held(p,new ItemStack(Items.DIAMOND_PICKAXE));monitorResume=true;p.connection.teleport(2.5,112,-2.5,35,-12);});return false;}
        if(phase==1){if(!position(mc,2.5,112,-2.5)||!mc.player.getMainHandItem().is(Items.DIAMOND_PICKAXE))return false;phase=2;mc.gameMode.startDestroyBlock(CRAFTER.east(),Direction.NORTH);return false;}
        if(phase==2){if(mc.level.getBlockState(CRAFTER.east()).is(Blocks.REDSTONE_BLOCK)){mc.gameMode.continueDestroyBlock(CRAFTER.east(),Direction.NORTH);return false;}phase=3;submit(mc,()->view(player(mc),false));return false;}
        if(serverInput!=0)return false;if(phase==3){phase=4;submit(mc,()->{var l=player(mc).serverLevel();require(contents(output(l))==pauseOutput+1&&contents(output(l),Items.CHEST)==2&&resumePaymentObserved,"Native redstone resume failed physical craft/onepower");nativeCrafts++;LogUtils.getLogger().info("THAUMCRAFT_PATTERN_NATIVE_RESUME: actualSurvivalRedstoneharvest/native20tickworldcraft/8physicalplanks->secondchest/onepower,total{}",nativeCrafts);});return false;}return viewed(mc,false);
    }
    private static boolean dismantle(Minecraft mc){
        if(harvested<2){boolean pattern=harvested==0;var pos=pattern?CRAFTER:DIOPTRA;String id=pattern?"pattern_crafter":"dioptra";
            if(phase==0){phase=1;submit(mc,()->{var p=player(mc);stow(p);held(p,new ItemStack(Items.DIAMOND_PICKAXE));p.connection.teleport(pos.getX()+2.5,112,pos.getZ()-2.5,35,pattern?-12:15);});return false;}
            if(phase==1){if(!position(mc,pos.getX()+2.5,112,pos.getZ()-2.5)||!mc.player.getMainHandItem().is(Items.DIAMOND_PICKAXE))return false;phase=2;mc.gameMode.startDestroyBlock(pos,Direction.NORTH);return false;}
            if(phase==2){if(mc.level.getBlockState(pos).is(CatalogBlocks.block(id))){mc.gameMode.continueDestroyBlock(pos,Direction.NORTH);return false;}phase=3;submit(mc,()->{var p=player(mc);var l=p.serverLevel();int loose=l.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(6),e->e.isAlive()&&e.getItem().is(item(id).getItem())).stream().mapToInt(e->e.getItem().getCount()).sum();require(l.getBlockState(pos).isAir()&&loose+count(p,id)==1,"Paid device teardown lost/duplicated item");harvested++;phase=0;LogUtils.getLogger().info("THAUMCRAFT_PATTERN_SURVIVAL_HARVESTED: {} actualnativeC2S/onephysicalpaiditem/no syntheticrefund",id);});return false;}return false;}
        if(phase==0){phase=1;submit(mc,()->view(player(mc),false));return false;}return viewed(mc,false);
    }
    @SubscribeEvent public static void serverTick(TickEvent.ServerTickEvent event){if(event.phase!=TickEvent.Phase.END||!enabled()||stopped||!prepared||!WORLD.equals(event.getServer().getWorldData().getLevelName()))return;var l=event.getServer().overworld();observedWorldTime=l.getGameTime();if(l.getBlockEntity(CRAFTER) instanceof PatternCrafterBlockEntity c){serverMode=c.type;serverPower=(int)c.power();serverInput=contents(input(l));serverOutput=contents(output(l));float now=c.power(),delta=now-previousPower;if(delta<0){require(delta==-1,"Native power debit changed");nativePowerSpent++;}else if(delta>0){nativeVisRefills++;require(delta==5||delta==4,"Native recharge size changed");if(delta==4)nativePowerSpent++;}if(monitorResume&&serverInput==0&&serverOutput==pauseOutput+1&&now==powerBefore-1)resumePaymentObserved=true;previousPower=now;}if(l.getBlockEntity(DIOPTRA) instanceof DioptraBlockEntity d&&d.grid_amt.length==169)gridCenter=d.grid_amt[84];}
    private static void prepare(Minecraft mc){
        var p=player(mc);var l=p.serverLevel();p.setInvulnerable(true);for(int x=-3;x<=2;x++)for(int z=-2;z<=2;z++)l.getChunk(x,z);for(var pos:BlockPos.betweenClosed(-20,111,-8,16,120,12))l.setBlockAndUpdate(pos,pos.getY()==111?Blocks.BEDROCK.defaultBlockState():Blocks.AIR.defaultBlockState());l.setDayTime(6000);l.setBlockAndUpdate(BENCH,ArcaneModule.WORKBENCH.get().defaultBlockState());
        // Supplied before either consumer exists; neither balances nor grids are reset afterwards.
        for(int x=-3;x<=2;x++)for(int z=-2;z<=2;z++){var pos=new ChunkPos(x,z).getMiddleBlockPosition(112);AuraManager.drainVis(l,pos,Float.MAX_VALUE,false);AuraManager.addVis(l,pos,400+x*60+z*30);AuraManager.drainFlux(l,pos,Float.MAX_VALUE,false);AuraManager.addFlux(l,pos,10+(x+3)*3+z+2);}
        p.getInventory().clearContent();for(String key:List.of("FIRSTSTEPS","KNOWLEDGETYPES","UNLOCKARTIFICE","BASEARTIFICE"))predecessor(p,key);for(String key:List.of("DIOPTRA","ARCANEPATTERNCRAFTER"))require(!KnowledgeStore.get(p).isResearchKnown(key),"Target research supplied");for(var aspect:List.of(Aspect.SENSES,Aspect.AURA,Aspect.CRAFT))require(!KnowledgeStore.get(p).knowsAspect(aspect),"Target discovered aspect supplied");p.connection.teleport(2.5,112,-3.5,35,-8);ResearchNetwork.sync(p);
        LogUtils.getLogger().info("THAUMCRAFT_PATTERN_FIXTURES: ownedSurvivalarena/rawbench/30loadedchunks variedinitialVisFlux/BASEARTIFICEprecursor/rawknowledgeandtypedcrystals/materialsdeferred/rawchestsandvanillarecipeinputs; targetstudies/paiddevices/aspects absent; no directactiveconsumer tick/count/power/type/grid/RNG/aura reset; notfullsurvival/multiplayer/reload/audio/allangles");
    }
    @SubscribeEvent public static void rendered(TickEvent.RenderTickEvent event){if(event.phase!=TickEvent.Phase.END||!enabled()||stopped||!captureRequested||captured)return;var mc=Minecraft.getInstance();try{captured=true;captureRequested=false;String name="tc6-pattern-"+IMAGES[scene]+".png";var file=new File(new File(mc.gameDirectory,"screenshots"),name);Files.deleteIfExists(file.toPath());Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),message->{if(file.isFile()&&file.length()>0){SAVED.incrementAndGet();LogUtils.getLogger().info("THAUMCRAFT_PATTERN_CLIENT_SMOKE_IMAGE: {}",file.getAbsolutePath());}else mc.execute(()->fail(mc,new AssertionError("Missing fresh Pattern capture")));});}catch(Throwable error){fail(mc,error);}}
    private static void start(Minecraft mc){if(mc.screen instanceof AccessibilityOnboardingScreen){mc.options.onboardAccessibility=false;mc.options.save();mc.setScreen(new TitleScreen());return;}if(!(mc.screen instanceof TitleScreen)||mc.getOverlay()!=null)return;started=true;previous=mc.options.tutorialStep;mc.options.tutorialStep=TutorialSteps.NONE;mc.getTutorial().stop();mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(3);mc.options.simulationDistance().set(5);mc.options.guiScale().set(2);mc.options.fov().set(80);mc.options.cloudStatus().set(CloudStatus.OFF);mc.options.setCameraType(CameraType.FIRST_PERSON);mc.options.hideGui=false;mc.resizeDisplay();var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);rules.getRule(GameRules.RULE_DAYLIGHT).set(false,null);rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false,null);rules.getRule(GameRules.RULE_RANDOMTICKING).set(0,null);mc.createWorldOpenFlows().createFreshLevel(WORLD,new LevelSettings(WORLD,GameType.SURVIVAL,false,Difficulty.PEACEFUL,true,rules,WorldDataConfiguration.DEFAULT),new WorldOptions(0x54433636L,false,false),WorldPresets::createNormalWorldDimensions);}
    private static void finish(Minecraft mc){
        if(phase==0){phase=1;close(mc);shift(mc,false);submit(mc,()->{var p=player(mc);var l=p.serverLevel();require(theoryPaid==96&&observationPaid==16&&xpEarned==20&&benchCrafts==2&&placed==2&&harvested==2&&modeCycles==9&&nativeCrafts==10&&nativePowerSpent==10&&nativeVisRefills>=2,"Final native paid census changed");require(contents(input(l))==0&&contents(output(l))==24&&contents(output(l),Items.CHEST)==2&&l.getBlockState(CRAFTER).isAir()&&l.getBlockState(DIOPTRA).isAir(),"Native physical outputs/teardown changed");LogUtils.getLogger().info("THAUMCRAFT_PATTERN_CLIENT_FACTS: scenes{} nativeAspects3/studies2/paidTheoryGOLEMANCY32ARTIFICE32AUROMANCY32/paidObservationAUROMANCY16/earnedXP20/paidBench2/vis100/Aer1Aqua2Terra1Ordo1/SurvivalPlacement2/nativeDioptraVisFlux169samples/knobCycles9/dies10/nativeVanillaCrafts10/oneUnmatchedDieConserved/physicalOutputs24/chest2/redstonePause80ticks/SurvivalTeardown2/renderFrames{}/movingFrames{}/renderModes{}/dioptraVisFrames{}/FluxFrames{}; explicitBASEARTIFICE/rawknowledge/crystals/materials/chests/arena/initialaura,notfullsurvival/multiplayer/reload/audio/allangles",SAVED.get(),PatternCrafterRenderer.renderedFrames(),PatternCrafterRenderer.movingFrames(),PatternCrafterRenderer.seenModes(),DioptraRenderer.visFrames(),DioptraRenderer.fluxFrames());validated=true;});return;}
        if(!validated)return;require(SAVED.get()==IMAGES.length&&PatternCrafterRenderer.seenModes()==1023&&PatternCrafterItemExtension.renderedItems()>0&&PatternCrafterRenderer.movingFrames()>0&&DioptraRenderer.visFrames()>0&&DioptraRenderer.fluxFrames()>0,"Native visual route incomplete");stopped=true;mc.options.tutorialStep=previous;LogUtils.getLogger().info("THAUMCRAFT_PATTERN_RENDER_AUDIT_OK: {} scenes; originaltenframeDie/nativeopposedextrudedgears/actualnativeblockevent/Dioptra144cells48edges4sides/nativeVisFluxcolors/lightmapfogadditive/noinventedGUI",SAVED.get());LogUtils.getLogger().info("THAUMCRAFT_PATTERN_CLIENT_SMOKE_OK: {} scenes; actualnative3aspects/2paidstudies/2paidbenches/2Survivalplacements/nativeDioptravisflux/nineknobcycles/all10dies/10nativecrafts/oneunmatchedconserved/80tickredstonepause/resume/physicalteardown,world={}",SAVED.get(),WORLD);mc.stop();
    }
    private static void fail(Minecraft mc,Throwable error){if(stopped)return;stopped=true;if(previous!=null)mc.options.tutorialStep=previous;LogUtils.getLogger().error("THAUMCRAFT_PATTERN_CLIENT_SMOKE_FAILED",error);mc.stop();}
}
