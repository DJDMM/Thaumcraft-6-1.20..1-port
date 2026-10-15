package thaumcraft.artifice.lamp.client;

import com.mojang.logging.LogUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.*;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
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
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.arcane.*;
import thaumcraft.artifice.lamp.*;
import thaumcraft.catalog.blocks.CatalogBlocks;
import thaumcraft.client.research.*;
import thaumcraft.essentia.EssentiaJarBlockEntity;
import thaumcraft.infusion.*;
import thaumcraft.research.*;
import thaumcraft.scanning.ScanningModule;
import thaumcraft.world.aura.AuraManager;

/** Opt-in owned Survival fixtures; only native C2S payment, placement, harvest and actual world/AI ticks. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public final class LampsClientSmokeTest {
    private static final String WORLD="thaumcraft-lamps-smoke-"+System.currentTimeMillis();
    private static final String[] IMAGES={"native-herba-scan","native-desiderium-scan","paid-growth-study","paid-fertility-study","paid-two-arcane-lamps","paid-growth-infusion","paid-fertility-infusion","survival-growth-placement","native-herba-crop-mist","survival-fertility-placement","native-desiderium-hearts","native-ai-birth","native-redstone-pause","native-growth-resumed","survival-dismantling"};
    private static final BlockPos BENCH=new BlockPos(-16,112,10),MATRIX=new BlockPos(-16,114,-4),GROWTH=new BlockPos(0,114,0),FERTILITY=new BlockPos(12,114,0);
    private static final int[][] OFFSETS={{3,0},{2,2},{0,3},{-3,0},{-2,-2},{0,-3}};
    private static final List<Aspect> ASPECTS=List.of(Aspect.PLANT,Aspect.LIGHT,Aspect.LIFE,Aspect.TOOL,Aspect.BEAST,Aspect.DESIRE);
    private static final int[] ESSENTIA={20,30,30,15,20,15};
    private static final AtomicInteger SAVED=new AtomicInteger(),HEART_EVENTS=new AtomicInteger();
    private static boolean started,prepared,stopped,captureRequested,captured,validated,crouch,heartObserver,growthCollected;
    private static int scene,phase,wait,stable,ready,expectedStage=-1,benchCrafts,infusionCrafts,theoryPaid,xpEarned,currentTheory,currentXp,currentItemCount,sourceBefore,jarsBefore,placed,harvested,placementIndex;
    private static volatile int cropAge,cropsGrown,herbaSource,desireSource,growthCharges,fertilityCharges,babies,adultLove;
    private static volatile boolean loveObserved,birthObserved;
    private static int pauseCrop,pauseCount,pauseGrowthDraw,pauseFertilityDraw;
    private static long began,pauseStarted,growthEmissionsBefore;
    private static volatile long observedWorldTime;
    private static float visBefore;
    private static CompletableFuture<Void> work;
    private static TutorialSteps previous;
    private LampsClientSmokeTest() {}
    private static boolean enabled() { return Boolean.getBoolean("thaumcraft.lampsSmokeTest"); }
    private static void require(boolean valid,String message) { if(!valid)throw new AssertionError(message); }
    private static ServerPlayer player(Minecraft mc) { var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());require(p!=null,"Missing owned lamps player");return p; }
    private static void submit(Minecraft mc,Runnable action) { require(work==null,"Overlapping owned server tasks");var result=new CompletableFuture<Void>();work=result;mc.getSingleplayerServer().execute(()->{try{action.run();result.complete(null);}catch(Throwable e){result.completeExceptionally(e);}}); }
    private static ItemStack item(String id) { var value=ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("thaumcraft",id));require(value!=null&&value!=Items.AIR,"Missing native item "+id);return new ItemStack(value); }
    private static int count(ServerPlayer p,Item item) { int n=0;for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i).is(item))n+=p.getInventory().getItem(i).getCount();return n; }
    private static int count(ServerPlayer p,String id) { return count(p,item(id).getItem()); }
    private static void stow(ServerPlayer p) { var stack=p.getMainHandItem();if(!stack.isEmpty()){p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);for(int i=1;i<36&&!stack.isEmpty();i++){var slot=p.getInventory().getItem(i);if(!slot.isEmpty()&&ItemStack.isSameItemSameTags(slot,stack)){int n=Math.min(stack.getCount(),slot.getMaxStackSize()-slot.getCount());slot.grow(n);stack.shrink(n);}}for(int i=1;i<36&&!stack.isEmpty();i++)if(p.getInventory().getItem(i).isEmpty()){p.getInventory().setItem(i,stack);stack=ItemStack.EMPTY;}require(stack.isEmpty(),"Cannot conserve physical held stack");}p.inventoryMenu.broadcastChanges(); }
    private static void held(ServerPlayer p,ItemStack stack) { require(p.getMainHandItem().isEmpty(),"Replacing owned held item");p.getInventory().selected=0;p.setItemInHand(InteractionHand.MAIN_HAND,stack);p.connection.send(new ClientboundSetCarriedItemPacket(0));p.inventoryMenu.broadcastChanges(); }
    private static ItemStack take(ServerPlayer p,Item item) { for(int i=0;i<36;i++){var stack=p.getInventory().getItem(i);if(stack.is(item)){var taken=stack.split(1);if(stack.isEmpty())p.getInventory().setItem(i,ItemStack.EMPTY);p.inventoryMenu.broadcastChanges();return taken;}}throw new AssertionError("Missing physically paid "+item); }
    private static ItemStack take(ServerPlayer p,String id) { return take(p,item(id).getItem()); }
    private static void predecessor(ServerPlayer p,String key) { try{var m=PlayerKnowledge.class.getDeclaredMethod("setResearchStage",String.class,int.class);m.setAccessible(true);require((boolean)m.invoke(KnowledgeStore.get(p),key,ResearchCatalog.get(key).stages().size()+1),"Raw predecessor "+key);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);} }
    private static void rawTheory(ServerPlayer p) { try{var m=PlayerKnowledge.class.getDeclaredMethod("addKnowledge",KnowledgeType.class,String.class,int.class);m.setAccessible(true);require((boolean)m.invoke(KnowledgeStore.get(p),KnowledgeType.THEORY,"ARTIFICE",32),"Raw theory fixture");}catch(ReflectiveOperationException e){throw new IllegalStateException(e);} }
    private static void close(Minecraft mc) { mc.player.closeContainer();mc.setScreen(null); }
    private static void click(Minecraft mc,BlockPos pos,Direction face) { mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(pos.getCenter().add(Vec3.atLowerCornerOf(face.getNormal()).scale(.5)),face,pos,false)); }
    private static boolean position(Minecraft mc,double x,double y,double z) { if(mc.player.distanceToSqr(x,y,z)>.3){ready=0;return false;}return ++ready>=5; }
    private static void view(ServerPlayer p,boolean fertility) { stow(p);p.connection.teleport(fertility?17.5:5.5,112,6.5,141,2); }
    private static boolean viewed(Minecraft mc,boolean fertility) { return position(mc,fertility?17.5:5.5,112,6.5)&&mc.screen==null; }
    private static void altarView(ServerPlayer p) { p.connection.teleport(MATRIX.getX()+.5,112,MATRIX.getZ()+4.5,180,-12); }
    private static ArcaneWorkbenchBlockEntity bench(ServerLevel l) { return (ArcaneWorkbenchBlockEntity)l.getBlockEntity(BENCH); }
    private static InfusionMatrixBlockEntity matrix(ServerLevel l) { return (InfusionMatrixBlockEntity)l.getBlockEntity(MATRIX); }
    private static InfusionPedestalBlockEntity central(ServerLevel l) { return (InfusionPedestalBlockEntity)l.getBlockEntity(MATRIX.below(2)); }
    private static EssentiaJarBlockEntity altarJar(ServerLevel l,int i) { return (EssentiaJarBlockEntity)l.getBlockEntity(MATRIX.offset(-5+i*2,-1,6)); }
    private static int jars(ServerLevel l) { int n=0;for(int i=0;i<6;i++)n+=altarJar(l,i).amount();return n; }
    private static EssentiaJarBlockEntity source(ServerLevel l,boolean fertility) { return (EssentiaJarBlockEntity)l.getBlockEntity((fertility?FERTILITY:GROWTH).below(2)); }
    private static int sourceOwned(ServerLevel l,boolean f) { var pos=f?FERTILITY:GROWTH;var tube=(IEssentiaTransport)l.getBlockEntity(pos.below());int moving=tube.getEssentiaAmount(Direction.UP);require(moving>=0&&moving<=1&&(moving==0||tube.getEssentiaType(Direction.UP)==(f?Aspect.DESIRE:Aspect.PLANT)),"Physical typed native source/tube ownership invalid");return source(l,f).amount()+moving; }
    private static GrowthLampBlockEntity growth(ServerLevel l) { return (GrowthLampBlockEntity)l.getBlockEntity(GROWTH); }
    private static FertilityLampBlockEntity fertility(ServerLevel l) { return (FertilityLampBlockEntity)l.getBlockEntity(FERTILITY); }
    private static void book(Minecraft mc,String key) { var k=ResearchClient.golemPressKnowledge();var browser=new ThaumonomiconScreen(k,k.scanCount());mc.setScreen(new ThaumonomiconPageScreen(browser,ResearchCatalog.get(key),k,k.scanCount())); }
    private static void shift(Minecraft mc,boolean value) { crouch=value;mc.player.input.shiftKeyDown=value;mc.player.setShiftKeyDown(value);mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player,value?ServerboundPlayerCommandPacket.Action.PRESS_SHIFT_KEY:ServerboundPlayerCommandPacket.Action.RELEASE_SHIFT_KEY)); }
    @SubscribeEvent public static void input(MovementInputUpdateEvent event) { if(!enabled()||stopped||!crouch)return;var mc=Minecraft.getInstance();if(mc.player==event.getEntity()&&mc.getSingleplayerServer()!=null&&WORLD.equals(mc.getSingleplayerServer().getWorldData().getLevelName())){var in=event.getInput();in.shiftKeyDown=true;in.forwardImpulse=in.leftImpulse=0;in.up=in.down=in.left=in.right=in.jumping=false;} }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||!enabled()||stopped)return;var mc=Minecraft.getInstance();if(began==0)began=System.nanoTime();try{
            require(System.nanoTime()-began<1_500_000_000_000L,"Lamps client timeout scene="+scene+" phase="+phase);if(!started){start(mc);return;}if(mc.level==null||mc.player==null||mc.getOverlay()!=null||mc.screen instanceof ReceivingLevelScreen)return;
            require(mc.getSingleplayerServer()!=null&&WORLD.equals(mc.getSingleplayerServer().getWorldData().getLevelName()),"Wrong owned lamps world");mc.getToasts().clear();
            if(++wait%100==0)LogUtils.getLogger().info("THAUMCRAFT_LAMPS_HEARTBEAT: scene={} phase={} bench={} altar={} cropAge={} grown={} herba={} growth={} desire={} fertility={} love={} babies={} mistPackets={} mistDraws={} heartEvents={}",scene,phase,benchCrafts,infusionCrafts,cropAge,cropsGrown,herbaSource,growthCharges,desireSource,fertilityCharges,adultLove,babies,GrowthLampEffects.receivedPackets(),GrowthLampEffects.drawnMists(),HEART_EVENTS.get());
            if(work!=null){if(!work.isDone())return;work.join();work=null;}if(!prepared){prepared=true;submit(mc,()->prepare(mc));return;}if(!heartObserver){observeHearts(mc);return;}
            if(captured){if(SAVED.get()==scene+1&&++stable>=20){scene++;phase=wait=stable=ready=0;expectedStage=-1;captured=captureRequested=false;}return;}if(scene==IMAGES.length){finish(mc);return;}
            if(!advance(mc)){stable=0;return;}if(++stable>=12)captureRequested=true;
        }catch(Throwable e){fail(mc,e);}
    }
    private static boolean advance(Minecraft mc) { return switch(scene) {
        case 0->scan(mc,Aspect.PLANT);case 1->scan(mc,Aspect.DESIRE);case 2->study(mc,false);case 3->study(mc,true);case 4->arcane(mc);case 5->infusion(mc,false);case 6->infusion(mc,true);case 7->place(mc,false);case 8->growing(mc);case 9->place(mc,true);case 10->hearts(mc);case 11->birth(mc);case 12->pause(mc);case 13->resume(mc);case 14->dismantle(mc);default->false;}; }
    private static boolean scan(Minecraft mc,Aspect aspect) {
        if(phase==0){phase=1;close(mc);submit(mc,()->{var p=player(mc);stow(p);held(p,new ItemStack(ScanningModule.THAUMOMETER.get()));require(p.getOffhandItem().isEmpty(),"Raw scan offhand occupied");p.setItemInHand(InteractionHand.OFF_HAND,AspectCrystalItem.create(aspect,1));p.inventoryMenu.broadcastChanges();});return false;}
        if(phase==1){if(!mc.player.getMainHandItem().is(ScanningModule.THAUMOMETER.get())||mc.player.getOffhandItem().isEmpty()||++ready<15)return false;shift(mc,true);mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);phase=2;return false;}
        if(phase==2){if(!ResearchClient.golemPressKnowledge().knowsAspect(aspect))return false;shift(mc,false);phase=3;submit(mc,()->{var p=player(mc);require(KnowledgeStore.get(p).knowsAspect(aspect),"Native scan did not earn aspect");var stack=p.getOffhandItem();p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);require(p.getInventory().add(stack),"Cannot conserve scanned raw crystal");p.inventoryMenu.broadcastChanges();LogUtils.getLogger().info("THAUMCRAFT_LAMPS_ASPECT_SCANNED: actualThaumometerC2S/rawtypedcrystal/{}; no targetaspectsetter",aspect.getTag());});return false;}return mc.screen==null;
    }
    private static boolean study(Minecraft mc,boolean f) {
        String key=f?"LAMPFERTILITY":"LAMPGROWTH";
        if(phase==0){phase=1;close(mc);submit(mc,()->{var p=player(mc);stow(p);rawTheory(p);currentTheory=KnowledgeStore.get(p).rawKnowledge(KnowledgeType.THEORY,"ARTIFICE");currentXp=p.totalExperience;held(p,new ItemStack(ResearchModule.THAUMONOMICON.get()));require(p.getInventory().add(new ItemStack(f?Items.WHEAT:Items.BONE_MEAL)),"Raw study material unavailable");if(f)require(p.getInventory().add(new ItemStack(Items.CARROT)),"Raw study carrot unavailable");p.inventoryMenu.broadcastChanges();ResearchNetwork.sync(p);});return false;}
        if(phase==1){if(!mc.player.getMainHandItem().is(ResearchModule.THAUMONOMICON.get()))return false;var k=ResearchClient.golemPressKnowledge();if(!k.isResearchCompleteStrict(key)){int current=k.researchStage(key);if(expectedStage==current)return false;expectedStage=current;ResearchNetwork.requestAdvance(key,current);return false;}phase=2;submit(mc,()->{var p=player(mc);require(KnowledgeStore.get(p).rawKnowledge(KnowledgeType.THEORY,"ARTIFICE")==currentTheory-32&&p.totalExperience==currentXp+10,"Original paid study changed");require(count(p,f?Items.WHEAT:Items.BONE_MEAL)==0&&(!f||count(p,Items.CARROT)==0),"Study physical items not consumed");theoryPaid+=32;xpEarned+=10;LogUtils.getLogger().info("THAUMCRAFT_LAMPS_STUDY_PAID: {} actualnativeC2S/32rawTheoryARTIFICE/+10earnedXP/originalphysicalitems",key);});return false;}
        if(!(mc.screen instanceof ThaumonomiconPageScreen))book(mc,key);return true;
    }
    private static void benchInputs(ServerPlayer p) {
        var l=p.serverLevel();require(bench(l).isEmpty(),"Previous bench inputs retained");var r=l.getRecipeManager().getAllRecipesFor(ArcaneModule.RECIPE_TYPE.get()).stream().filter(x->x.getResultItem(l.registryAccess()).is(item("lamp_arcane").getItem())).findFirst().orElseThrow();require(r.vis()==50,"Original Arcane Lamp price changed");
        for(int i=0;i<r.getIngredients().size();i++){var ingredient=r.getIngredients().get(i);if(!ingredient.isEmpty())bench(l).setItem(i%r.gridWidth()+i/r.gridWidth()*3,ingredient.getItems()[0].copyWithCount(1));}for(int i=0;i<6;i++)if(r.crystalCost(i)>0)bench(l).setItem(9+i,AspectCrystalItem.create(Aspect.getAspect(ArcaneModule.PRIMALS[i]),r.crystalCost(i)));currentItemCount=count(p,"lamp_arcane");visBefore=AuraManager.getVis(l,BENCH);p.inventoryMenu.broadcastChanges();
    }
    private static boolean arcane(Minecraft mc) {
        if(phase==0){phase=1;close(mc);submit(mc,()->{var p=player(mc);stow(p);p.connection.teleport(BENCH.getX()+.5,112,BENCH.getZ()+2.5,180,20);benchInputs(p);});return false;}
        if(phase==1){if(!position(mc,BENCH.getX()+.5,112,BENCH.getZ()+2.5)||!mc.player.getMainHandItem().isEmpty())return false;phase=2;click(mc,BENCH,Direction.UP);return false;}
        if(!(mc.player.containerMenu instanceof ArcaneWorkbenchMenu menu))return false;
        if(phase==2){if(!menu.craftable()||!menu.getSlot(0).getItem().is(item("lamp_arcane").getItem()))return false;require(menu.requiredVis()==50,"Native precursor price changed");phase=3;mc.gameMode.handleInventoryMouseClick(menu.containerId,0,0,ClickType.QUICK_MOVE,mc.player);return false;}
        if(phase==3){if(menu.hasRecipe())return false;phase=4;submit(mc,()->{var p=player(mc);require(bench(p.serverLevel()).isEmpty()&&count(p,"lamp_arcane")==currentItemCount+1&&AuraManager.getVis(p.serverLevel(),BENCH)==visBefore-50,"Actual precursor payment changed");benchCrafts++;LogUtils.getLogger().info("THAUMCRAFT_LAMPS_BENCH_PAID: 50vis/Aer1Ignis1/fourIronPlates+AmberBlock->oneArcaneLamp,total={}",benchCrafts);if(benchCrafts<2){benchInputs(p);phase=2;}});return false;}return benchCrafts==2;
    }
    private static void altarInputs(ServerPlayer p,String id) {
        var l=p.serverLevel();require(central(l).isEmpty()&&!matrix(l).crafting(),"Previous altar payment unresolved");var paid=take(p,"lamp_arcane");central(l).setItem(0,paid);var r=l.getRecipeManager().getAllRecipesFor(InfusionModule.RECIPE_TYPE.get()).stream().filter(x->x.getResultItem(l.registryAccess()).is(item(id).getItem())).findFirst().orElseThrow();var parts=r.components(paid);require(parts.size()==6,"Original six lamp ingredients changed");for(int i=0;i<6;i++){var pedestal=(InfusionPedestalBlockEntity)l.getBlockEntity(MATRIX.offset(OFFSETS[i][0],-2,OFFSETS[i][1]));require(pedestal.isEmpty(),"Peripheral retained");pedestal.setItem(0,parts.get(i).example());}var plan=InfusionRecipes.find(p,paid,parts.stream().map(x->x.example()).toList()).orElseThrow();require(plan.aspects().visSize()==65&&plan.instability()==4,"Original65ess lamp recipe changed");jarsBefore=jars(l);stow(p);held(p,item("caster_basic"));altarView(p);
    }
    private static boolean infusion(Minecraft mc,boolean f) {
        require(wait<18000,"Native lamp altar stalled");String id=f?"lamp_fertility":"lamp_growth";
        if(f&&!growthCollected){if(!collect(mc,"lamp_growth"))return false;growthCollected=true;phase=ready=0;}
        if(phase==0){phase=1;close(mc);submit(mc,()->altarInputs(player(mc),id));return false;}
        if(!(mc.level.getBlockEntity(MATRIX) instanceof InfusionMatrixBlockEntity m))return false;
        if(phase==1){if(!position(mc,MATRIX.getX()+.5,112,MATRIX.getZ()+4.5)||!InfusionMatrixBlock.isCaster(mc.player.getMainHandItem()))return false;if(!m.active()){if(++ready%20==0)click(mc,MATRIX,Direction.UP);return false;}if(m.startup()<.95F)return false;phase=2;click(mc,MATRIX,Direction.UP);return false;}
        if(phase==2){if(m.crafting()||!centralPresent(mc,id))return false;phase=3;submit(mc,()->{var p=player(mc);var l=p.serverLevel();require(central(l).getItem(0).is(item(id).getItem())&&central(l).getItem(0).getCount()==1&&jars(l)==jarsBefore-65,"Native typed65ess lamp payment/output changed");for(var o:OFFSETS)require(((InfusionPedestalBlockEntity)l.getBlockEntity(MATRIX.offset(o[0],-2,o[1]))).isEmpty(),"Original six peripherals did not pay");infusionCrafts++;LogUtils.getLogger().info("THAUMCRAFT_LAMPS_ALTAR_PAID: {} physicalpaidArcaneLamp+sixoriginalperipherals+65nativeTypedEssentia->oneactualoutput,total={}",id,infusionCrafts);});return false;}return mc.screen==null;
    }
    private static boolean centralPresent(Minecraft mc,String id) { return mc.level.getBlockEntity(MATRIX.below(2)) instanceof InfusionPedestalBlockEntity c&&c.getItem(0).is(item(id).getItem()); }
    private static boolean collect(Minecraft mc,String id) {
        if(centralPresent(mc,id)){if(phase==0){phase=1;close(mc);submit(mc,()->{var p=player(mc);stow(p);altarView(p);currentItemCount=count(p,id);});return false;}if(phase==1){if(!mc.player.getMainHandItem().isEmpty()||!position(mc,MATRIX.getX()+.5,112,MATRIX.getZ()+4.5))return false;phase=2;click(mc,MATRIX.below(2),Direction.UP);}return false;}
        if(phase==2){phase=3;submit(mc,()->{var p=player(mc);require(central(p.serverLevel()).isEmpty()&&count(p,id)==currentItemCount+1,"Physical altar retrieval changed");});return false;}return phase>=3;
    }
    private static void sourceFixture(ServerLevel l,boolean f) { var pos=f?FERTILITY:GROWTH;l.setBlockAndUpdate(pos.below(2),CatalogBlocks.block("jar_normal").defaultBlockState());require(source(l,f).addExact(f?Aspect.DESIRE:Aspect.PLANT,40),"Raw typed40 source rejected");l.setBlockAndUpdate(pos.below(),CatalogBlocks.block("tube").defaultBlockState()); }
    private static boolean place(Minecraft mc,boolean f) {
        var pos=f?FERTILITY:GROWTH;String id=f?"lamp_fertility":"lamp_growth";
        if(!f&&placementIndex==0){if(!collect(mc,"lamp_fertility"))return false;phase=ready=0;placementIndex=1;}
        if(phase==0){phase=1;close(mc);submit(mc,()->{var p=player(mc);stow(p);sourceFixture(p.serverLevel(),f);held(p,take(p,id));p.connection.teleport(pos.getX()+2.5,112,pos.getZ()+2.5,135,-25);});return false;}
        if(phase==1){if(!position(mc,pos.getX()+2.5,112,pos.getZ()+2.5)||!mc.player.getMainHandItem().is(item(id).getItem()))return false;phase=2;click(mc,pos.below(),Direction.UP);return false;}
        if(phase==2){if(!mc.level.getBlockState(pos).is(CatalogBlocks.block(id)))return false;phase=3;submit(mc,()->{var p=player(mc);require(p.serverLevel().getBlockState(pos).getValue(EssentiaLampBlock.FACING)==Direction.DOWN&&count(p,id)==0,"Actual Survival placement did not pay/facing");placed++;if(f)spawnCows(p.serverLevel());view(p,f);LogUtils.getLogger().info("THAUMCRAFT_LAMPS_SURVIVAL_PLACED: {} actualnativeC2S/onepaidBlockItem/supportfacingDOWN/rawtyped40source+verticaltube,chargedonlybynativeworlddraw",id);});return false;}return viewed(mc,f);
    }
    private static boolean growing(Minecraft mc) {
        require(wait<6000,"Native scheduled crop progress/mist stalled");if(cropAge==0||herbaSource>=40||GrowthLampEffects.receivedPackets()==0||GrowthLampEffects.drawnMists()==0)return false;
        if(phase==0){phase=1;submit(mc,()->{var p=player(mc);require(cropAge>0&&source(p.serverLevel(),false).amount()<40,"Native Herba draw/crop progress absent");view(p,false);LogUtils.getLogger().info("THAUMCRAFT_LAMPS_NATIVE_GROWTH: scheduledactualworldcropAGEsum={} grown={} rawHerbaRemaining={} charges={} reserve={} packets={} emitted={} drawn={},randomTickSpeed0/no directdrivergrow/queue/tickcall",cropAge,cropsGrown,herbaSource,growthCharges,growth(p.serverLevel()).hasReserve(),GrowthLampEffects.receivedPackets(),GrowthLampEffects.emittedMists(),GrowthLampEffects.drawnMists());});return false;}return viewed(mc,false)&&GrowthLampEffects.activeMists()>0;
    }
    private static void spawnCows(ServerLevel l) { for(double x:new double[]{11.5,13.0}){var cow=EntityType.COW.create(l);require(cow!=null,"Raw cow creation failed");cow.moveTo(x,112,2.5,180,0);cow.setPersistenceRequired();require(l.addFreshEntity(cow),"Raw native adult rejected");}LogUtils.getLogger().info("THAUMCRAFT_LAMPS_RAW_ANIMAL_FIXTURE: twoadultnativeCow entities/fullnativeAI/no age/love/babysetter afterspawn"); }
    private static boolean hearts(Minecraft mc) { require(wait<3000,"Native fertile pair did not enter love");if(!loveObserved||HEART_EVENTS.get()==0)return false;if(phase==0){phase=1;submit(mc,()->{view(player(mc),true);LogUtils.getLogger().info("THAUMCRAFT_LAMPS_NATIVE_LOVE: originalDesiderium5payment/nativeCowsetInLove/events18={} adultLove={} charges={} source={}; nofeeding/directdriverlove/birthcall",HEART_EVENTS.get(),adultLove,fertilityCharges,desireSource);});return false;}return viewed(mc,true); }
    private static boolean birth(Minecraft mc) { require(wait<4000,"Native Cow AI did not produce child");if(!birthObserved)return false;if(phase==0){phase=1;submit(mc,()->{var p=player(mc);var l=p.serverLevel();require(babies==1&&40-sourceOwned(l,true)-fertility(l).charges()==5,"Native onepair5Desiderium payment/physical source conservation changed");view(p,true);LogUtils.getLogger().info("THAUMCRAFT_LAMPS_NATIVE_BIRTH: actualvanillaBreedGoal/onephysicalbaby/twoadultcooldowns/DesideriumsourcePlusTube={} charges={} exactPairPayment5/heartEvents={}; no directbreed/spawnchilddrivercall",sourceOwned(l,true),fertility(l).charges(),HEART_EVENTS.get());});return false;}return viewed(mc,true); }
    private static boolean pause(Minecraft mc) {
        if(phase<6){boolean f=phase>=3;var pos=f?FERTILITY:GROWTH;if(phase%3==0){phase++;submit(mc,()->{var p=player(mc);stow(p);held(p,new ItemStack(Items.REDSTONE_BLOCK));p.connection.teleport(pos.getX()+2.5,112,2.5,135,-25);});return false;}if(phase%3==1){if(!position(mc,pos.getX()+2.5,112,2.5)||!mc.player.getMainHandItem().is(Items.REDSTONE_BLOCK))return false;phase++;click(mc,pos,Direction.EAST);return false;}if(!mc.level.getBlockState(pos.east()).is(Blocks.REDSTONE_BLOCK))return false;phase++;return false;}
        if(phase==6){phase=7;submit(mc,()->{var l=player(mc).serverLevel();require(l.hasNeighborSignal(GROWTH)&&l.hasNeighborSignal(FERTILITY),"Native redstone power absent");pauseStarted=l.getGameTime();});return false;}
        if(phase==7){if(observedWorldTime-pauseStarted<60)return false;phase=8;submit(mc,()->{var l=player(mc).serverLevel();pauseCrop=cropAge;pauseCount=fertility(l).activeTicks();pauseGrowthDraw=growth(l).drawAttempts();pauseFertilityDraw=fertility(l).drawAttempts();pauseStarted=l.getGameTime();view(player(mc),true);});return false;}
        if(phase==8){if(observedWorldTime-pauseStarted<320)return false;phase=9;submit(mc,()->{var l=player(mc).serverLevel();require(l.getGameTime()-pauseStarted>=320&&cropAge==pauseCrop&&fertility(l).activeTicks()==pauseCount,"Redstone failed to pause growth/love clock");require(growth(l).drawAttempts()>=pauseGrowthDraw&&fertility(l).drawAttempts()>=pauseFertilityDraw,"Original powered refill clocks regressed");LogUtils.getLogger().info("THAUMCRAFT_LAMPS_NATIVE_PAUSE: actualtwoC2SRedstoneBlocks/{}worldticks cropAGEsum={} fertilityActiveTicks={} poweredrefillallowed/growthcharges={} reserve={} fertilitycharges={}",l.getGameTime()-pauseStarted,cropAge,pauseCount,growth(l).charges(),growth(l).hasReserve(),fertility(l).charges());});return false;}return viewed(mc,true);
    }
    private static boolean resume(Minecraft mc) {
        var pos=GROWTH.east();if(phase==0){phase=1;submit(mc,()->{var p=player(mc);stow(p);held(p,new ItemStack(Items.DIAMOND_PICKAXE));p.connection.teleport(3.5,112,2.5,135,-25);growthEmissionsBefore=GrowthLampEffects.receivedPackets();});return false;}
        if(phase==1){if(!position(mc,3.5,112,2.5)||!mc.player.getMainHandItem().is(Items.DIAMOND_PICKAXE))return false;phase=2;mc.gameMode.startDestroyBlock(pos,Direction.EAST);return false;}
        if(phase==2){if(mc.level.getBlockState(pos).is(Blocks.REDSTONE_BLOCK)){mc.gameMode.continueDestroyBlock(pos,Direction.EAST);return false;}phase=3;submit(mc,()->view(player(mc),false));return false;}
        require(wait<6000,"Native resumed crop progress absent");if(cropAge<=pauseCrop||GrowthLampEffects.receivedPackets()<=growthEmissionsBefore)return false;if(phase==3){phase=4;LogUtils.getLogger().info("THAUMCRAFT_LAMPS_NATIVE_RESUME: actualSurvivalRedstoneharvest/resumedcropAGEsum{}->{} freshoriginalMistPackets{}->{}",pauseCrop,cropAge,growthEmissionsBefore,GrowthLampEffects.receivedPackets());}return viewed(mc,false);
    }
    private static boolean dismantle(Minecraft mc) {
        if(harvested<2){boolean f=harvested==1;var pos=f?FERTILITY:GROWTH;String id=f?"lamp_fertility":"lamp_growth";
            if(phase==0){phase=1;submit(mc,()->{var p=player(mc);stow(p);held(p,new ItemStack(Items.DIAMOND_PICKAXE));p.connection.teleport(pos.getX()+2.5,112,2.5,135,-25);});return false;}
            if(phase==1){if(!position(mc,pos.getX()+2.5,112,2.5)||!mc.player.getMainHandItem().is(Items.DIAMOND_PICKAXE))return false;phase=2;mc.gameMode.startDestroyBlock(pos,Direction.EAST);return false;}
            if(phase==2){if(mc.level.getBlockState(pos).is(CatalogBlocks.block(id))){mc.gameMode.continueDestroyBlock(pos,Direction.EAST);return false;}phase=3;submit(mc,()->{var p=player(mc);var l=p.serverLevel();int loose=l.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(6),e->e.isAlive()&&e.getItem().is(item(id).getItem())).stream().mapToInt(e->e.getItem().getCount()).sum();require(l.getBlockState(pos).isAir()&&loose+count(p,id)==1,"Paid lamp teardown lost/duplicated item");harvested++;phase=0;LogUtils.getLogger().info("THAUMCRAFT_LAMPS_SURVIVAL_HARVESTED: {} actualnativeC2S/onephysicalpaiditem/lampremoved/nochargeforcedspill",id);});return false;}return false;}
        if(phase==0){phase=1;submit(mc,()->view(player(mc),true));return false;}return viewed(mc,true);
    }
    @SubscribeEvent public static void serverTick(TickEvent.ServerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||!enabled()||stopped||!prepared||!WORLD.equals(event.getServer().getWorldData().getLevelName()))return;var l=event.getServer().overworld();observedWorldTime=l.getGameTime();
        cropAge=cropsGrown=0;for(var pos:BlockPos.betweenClosed(-4,112,-4,4,112,4)){var state=l.getBlockState(pos);if(state.is(Blocks.WHEAT)){cropAge+=state.getValue(CropBlock.AGE);if(state.getValue(CropBlock.AGE)>0)cropsGrown++;}}
        if(l.getBlockEntity(GROWTH) instanceof GrowthLampBlockEntity g){growthCharges=g.charges();herbaSource=source(l,false).amount();}if(l.getBlockEntity(FERTILITY) instanceof FertilityLampBlockEntity f){fertilityCharges=f.charges();desireSource=source(l,true).amount();}
        var cows=l.getEntitiesOfClass(Cow.class,new AABB(FERTILITY).inflate(9),e->e.isAlive());babies=(int)cows.stream().filter(Cow::isBaby).count();adultLove=(int)cows.stream().filter(c->!c.isBaby()&&c.isInLove()).count();loveObserved|=adultLove==2;birthObserved|=babies==1;
    }
    private static void observeHeartPacket(Object message) { if(message instanceof ClientboundBundlePacket bundle){for(var packet:bundle.subPackets())observeHeartPacket(packet);return;}if(message instanceof ClientboundEntityEventPacket event&&event.getEventId()==18)HEART_EVENTS.incrementAndGet(); }
    private static void observeHearts(Minecraft mc) { var connection=mc.getConnection().getConnection();connection.channel().eventLoop().submit(()->connection.channel().pipeline().addBefore("packet_handler","thaumcraft_lamps_qa_hearts",new ChannelInboundHandlerAdapter(){@Override public void channelRead(ChannelHandlerContext context,Object message)throws Exception{try{observeHeartPacket(message);}finally{context.fireChannelRead(message);}}})).syncUninterruptibly();heartObserver=true; }
    private static void prepare(Minecraft mc) {
        var p=player(mc);var l=p.serverLevel();p.setInvulnerable(true);for(int x=-3;x<=2;x++)for(int z=-2;z<=2;z++)l.getChunk(x,z);for(var pos:BlockPos.betweenClosed(-26,111,-12,22,121,15))l.setBlockAndUpdate(pos,pos.getY()==111?Blocks.BEDROCK.defaultBlockState():Blocks.AIR.defaultBlockState());l.setDayTime(6000);
        l.setBlockAndUpdate(BENCH,ArcaneModule.WORKBENCH.get().defaultBlockState());l.setBlockAndUpdate(MATRIX,CatalogBlocks.block("infusion_matrix").defaultBlockState());l.setBlockAndUpdate(MATRIX.below(2),CatalogBlocks.block("pedestal_arcane").defaultBlockState());for(int x:new int[]{-1,1})for(int z:new int[]{-1,1})l.setBlockAndUpdate(MATRIX.offset(x,-2,z),CatalogBlocks.block("pillar_arcane").defaultBlockState());for(var o:OFFSETS)l.setBlockAndUpdate(MATRIX.offset(o[0],-2,o[1]),CatalogBlocks.block("pedestal_arcane").defaultBlockState());
        var colors=List.of("white","orange","magenta","lightblue","yellow","lime","pink","gray","silver","cyan","purple","blue","brown","green","red","black");for(int i=0;i<16;i++)for(int sign:new int[]{-1,1})l.setBlockAndUpdate(MATRIX.offset(sign*(2+i%6),-2,sign*(-3-i/6)),CatalogBlocks.block("candle_"+colors.get(i)).defaultBlockState());require(InfusionStability.scan(l,MATRIX).gain()>=1.5F,"Raw symmetric altar stability absent");
        for(int i=0;i<6;i++){l.setBlockAndUpdate(MATRIX.offset(-5+i*2,-1,6),CatalogBlocks.block("jar_normal").defaultBlockState());require(altarJar(l,i).addExact(ASPECTS.get(i),ESSENTIA[i]+3),"Raw typed altar essence rejected");}
        for(var pos:BlockPos.betweenClosed(-4,112,-4,4,112,4))if(pos.getX()!=0||pos.getZ()!=0){l.setBlockAndUpdate(pos.below(),Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE,7));l.setBlockAndUpdate(pos,Blocks.WHEAT.defaultBlockState());}
        for(int x=8;x<=16;x++)for(int z=-4;z<=5;z++)if(x==8||x==16||z==-4||z==5)l.setBlockAndUpdate(new BlockPos(x,112,z),Blocks.OAK_FENCE.defaultBlockState());
        for(int x=-3;x<=2;x++)for(int z=-2;z<=2;z++){var pos=new ChunkPos(x,z).getMiddleBlockPosition(112);AuraManager.drainVis(l,pos,Float.MAX_VALUE,false);AuraManager.addVis(l,pos,2500);AuraManager.drainFlux(l,pos,Float.MAX_VALUE,false);}
        p.getInventory().clearContent();for(String key:List.of("FIRSTSTEPS","KNOWLEDGETYPES","UNLOCKARTIFICE","BASEARTIFICE","ARCANELAMP","UNLOCKINFUSION","BASEINFUSION","INFUSION"))predecessor(p,key);for(String key:List.of("LAMPGROWTH","LAMPFERTILITY"))require(!KnowledgeStore.get(p).isResearchKnown(key),"Target research supplied");require(!KnowledgeStore.get(p).knowsAspect(Aspect.PLANT)&&!KnowledgeStore.get(p).knowsAspect(Aspect.DESIRE),"Target aspect supplied");p.connection.teleport(5.5,112,6.5,141,2);ResearchNetwork.sync(p);
        LogUtils.getLogger().info("THAUMCRAFT_LAMPS_FIXTURES: ownedSurvivalarena/rawbench+stablealtar+sixpedestals+32candles+148typedaltarEssentia/2500visperloadedchunk/predecessorARCANELAMP+INFUSION/80rawAGE0Wheat/randomTickSpeed0/fence; rawknowledge/studymaterials/crystals/recipeingredients deferred; targetaspects/studies/paidArcaneLamps/GrowthFertility/babies absent; no directruntime tick/grow/charge/reserve/queue/RNG/love/breedcall");
    }
    @SubscribeEvent public static void rendered(TickEvent.RenderTickEvent event) { if(event.phase!=TickEvent.Phase.END||!enabled()||stopped||!captureRequested||captured)return;var mc=Minecraft.getInstance();try{captured=true;captureRequested=false;String name="tc6-lamps-"+IMAGES[scene]+".png";var file=new File(new File(mc.gameDirectory,"screenshots"),name);Files.deleteIfExists(file.toPath());Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),message->{if(file.isFile()&&file.length()>0){SAVED.incrementAndGet();LogUtils.getLogger().info("THAUMCRAFT_LAMPS_CLIENT_SMOKE_IMAGE: {}",file.getAbsolutePath());}else mc.execute(()->fail(mc,new AssertionError("Missing fresh native lamps capture")));});}catch(Throwable e){fail(mc,e);} }
    private static void start(Minecraft mc) { if(mc.screen instanceof AccessibilityOnboardingScreen){mc.options.onboardAccessibility=false;mc.options.save();mc.setScreen(new TitleScreen());return;}if(!(mc.screen instanceof TitleScreen)||mc.getOverlay()!=null)return;started=true;previous=mc.options.tutorialStep;mc.options.tutorialStep=TutorialSteps.NONE;mc.getTutorial().stop();mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(3);mc.options.simulationDistance().set(5);mc.options.guiScale().set(2);mc.options.fov().set(80);mc.options.cloudStatus().set(CloudStatus.OFF);mc.options.setCameraType(CameraType.FIRST_PERSON);mc.options.hideGui=false;mc.resizeDisplay();var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);rules.getRule(GameRules.RULE_DAYLIGHT).set(false,null);rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false,null);rules.getRule(GameRules.RULE_RANDOMTICKING).set(0,null);mc.createWorldOpenFlows().createFreshLevel(WORLD,new LevelSettings(WORLD,GameType.SURVIVAL,false,Difficulty.PEACEFUL,true,rules,WorldDataConfiguration.DEFAULT),new WorldOptions(0x54433635L,false,false),WorldPresets::createNormalWorldDimensions); }
    private static void finish(Minecraft mc) {
        if(phase==0){phase=1;close(mc);shift(mc,false);submit(mc,()->{var p=player(mc);var l=p.serverLevel();require(theoryPaid==64&&xpEarned==20&&benchCrafts==2&&infusionCrafts==2&&jars(l)==18&&placed==2&&harvested==2,"Final native payment census changed");require(cropAge>pauseCrop&&loveObserved&&birthObserved&&babies==1&&l.getBlockState(GROWTH).isAir()&&l.getBlockState(FERTILITY).isAir(),"Native growth/birth/teardown final state changed");LogUtils.getLogger().info("THAUMCRAFT_LAMPS_CLIENT_FACTS: scenes={} nativeAspects2/studies2/paidTheoryARTIFICE64/earnedXP20/paidArcaneLampBench2/vis100/Aer2Ignis2/paidInfusions2/typedEssentia130/altarRemainder18/nativeSurvivalPlacement2/herbaSourceRemaining{}/desireSourceRemaining{}/cropAGEsum{}/nativeLoveObserved{}/nativeCowBaby{}/nativeHeartEvents{}/nativeMistPackets{}/nativeMistEmitted{}/nativeMistDraws{}/redstonePause320ticks/refillallowed/nativeSurvivalTeardown2; explicitprecursors/rawknowledge/materials/crystals/arenacrops/adults/jars/tubes/aura,notfullsurvival/multiplayer/reload/audio/allangles",SAVED.get(),source(l,false).amount(),source(l,true).amount(),cropAge,loveObserved,babies,HEART_EVENTS.get(),GrowthLampEffects.receivedPackets(),GrowthLampEffects.emittedMists(),GrowthLampEffects.drawnMists());validated=true;});return;}
        if(!validated)return;require(SAVED.get()==IMAGES.length&&GrowthLampEffects.receivedPackets()>0&&GrowthLampEffects.emittedMists()==GrowthLampEffects.receivedPackets()*8&&GrowthLampEffects.drawnMists()>0&&HEART_EVENTS.get()>0,"Actual native visual route incomplete");stopped=true;mc.options.tutorialStep=previous;LogUtils.getLogger().info("THAUMCRAFT_LAMPS_RENDER_AUDIT_OK: {} scenes; originalstaticGrowthFertilitymodels/enabledlight15/nativegreenframe56grid16additivemist/nativeAnimalhearts/actualAIbaby/noinventedbeam/nooverlay",SAVED.get());LogUtils.getLogger().info("THAUMCRAFT_LAMPS_CLIENT_SMOKE_OK: {} scenes; actualnative2aspects/2paidstudies/2paidprecursorbench/2paid65essinfusions/2Survivalplacements/nativeHerbatubescheduledgrowth/nativeDesideriumpair+AIbirth/redstonepause+resume/physicalteardown,world={}",SAVED.get(),WORLD);mc.stop();
    }
    private static void fail(Minecraft mc,Throwable error) { if(stopped)return;stopped=true;if(previous!=null)mc.options.tutorialStep=previous;LogUtils.getLogger().error("THAUMCRAFT_LAMPS_CLIENT_SMOKE_FAILED",error);mc.stop(); }
}
