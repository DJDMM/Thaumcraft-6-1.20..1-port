package thaumcraft.artifice.generator.client;

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
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import thaumcraft.alchemy.AspectCrystalItem;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.arcane.*;
import thaumcraft.artifice.generator.*;
import thaumcraft.catalog.blocks.CatalogBlocks;
import thaumcraft.client.research.*;
import thaumcraft.research.*;
import thaumcraft.scanning.ScanningModule;
import thaumcraft.world.aura.AuraManager;

/** Owned opt-in native Survival route. Chest FE capability below is an explicit QA fixture, not gameplay. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public final class GeneratorClientSmokeTest {
    private static final String WORLD="thaumcraft-generator-smoke-"+System.currentTimeMillis();
    private static final String[] IMAGES={"native-potentia-scan","paid-generator-study","paid-generator-bench","survival-facing-down","survival-facing-up","survival-facing-north","survival-facing-south","survival-facing-west","survival-facing-east","native-powered-pause","native-output-resumed","survival-dismantling"};
    private static final Direction[] FACES={Direction.DOWN,Direction.UP,Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST};
    private static final BlockPos[] POSITIONS={new BlockPos(-12,114,-8),new BlockPos(0,114,-8),new BlockPos(12,114,-8),new BlockPos(-12,114,6),new BlockPos(0,114,6),new BlockPos(12,114,6)};
    private static final BlockPos BENCH=new BlockPos(-12,112,14);
    private static final Receiver[] RECEIVERS=new Receiver[6];
    private static final AtomicInteger SAVED=new AtomicInteger();
    private static final int[] CURRENT_THEORY=new int[2];
    private static final String[] CATEGORIES={"BASICS","ARTIFICE"};
    private static boolean started,prepared,stopped,crouch,captureRequested,captured,validated;
    private static int scene,phase,wait,stable,ready,expectedStage=-1,currentXp,placed,harvested,pickedUp,studyPaid,benchPaid;
    private static volatile long observedWorldTime;
    private static volatile int currentEnergy,totalAccepted,observedFaces,receiverCalls;
    private static int pauseEnergy,pauseAccepted,resumeBefore;
    private static long began,pauseStarted,poweredBefore;
    private static float visBefore;
    private static CompletableFuture<Void> work;
    private static TutorialSteps previous;
    private GeneratorClientSmokeTest(){}
    private static boolean enabled(){return Boolean.getBoolean("thaumcraft.generatorSmokeTest");}
    private static void require(boolean b,String message){if(!b)throw new AssertionError(message);}
    private static ServerPlayer player(Minecraft mc){var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());require(p!=null,"Missing owned generator player");return p;}
    private static void submit(Minecraft mc,Runnable action){require(work==null,"Overlapping owned server tasks");var result=new CompletableFuture<Void>();work=result;mc.getSingleplayerServer().execute(()->{try{action.run();result.complete(null);}catch(Throwable error){result.completeExceptionally(error);}});}
    private static ItemStack item(String id){var value=ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("thaumcraft",id));require(value!=null&&value!=Items.AIR,"Missing native item "+id);return new ItemStack(value);}
    private static int count(ServerPlayer p,Item item){int n=0;for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i).is(item))n+=p.getInventory().getItem(i).getCount();return n;}
    private static int count(ServerPlayer p){return count(p,item("vis_generator").getItem());}
    private static void stow(ServerPlayer p){var stack=p.getMainHandItem();if(!stack.isEmpty()){p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);for(int i=1;i<36&&!stack.isEmpty();i++){var slot=p.getInventory().getItem(i);if(!slot.isEmpty()&&ItemStack.isSameItemSameTags(slot,stack)){int n=Math.min(stack.getCount(),slot.getMaxStackSize()-slot.getCount());slot.grow(n);stack.shrink(n);}}for(int i=1;i<36&&!stack.isEmpty();i++)if(p.getInventory().getItem(i).isEmpty()){p.getInventory().setItem(i,stack);stack=ItemStack.EMPTY;}require(stack.isEmpty(),"Cannot conserve held stack");}p.inventoryMenu.broadcastChanges();}
    private static void held(ServerPlayer p,ItemStack stack){require(p.getMainHandItem().isEmpty(),"Replacing physical held item");p.getInventory().selected=0;p.setItemInHand(InteractionHand.MAIN_HAND,stack);p.connection.send(new ClientboundSetCarriedItemPacket(0));p.inventoryMenu.broadcastChanges();}
    private static ItemStack take(ServerPlayer p){for(int i=0;i<36;i++){var s=p.getInventory().getItem(i);if(s.is(item("vis_generator").getItem())){var taken=s.split(1);if(s.isEmpty())p.getInventory().setItem(i,ItemStack.EMPTY);p.inventoryMenu.broadcastChanges();return taken;}}throw new AssertionError("Missing physically paid generator");}
    private static void predecessor(ServerPlayer p,String key){try{var m=PlayerKnowledge.class.getDeclaredMethod("setResearchStage",String.class,int.class);m.setAccessible(true);require((boolean)m.invoke(KnowledgeStore.get(p),key,ResearchCatalog.get(key).stages().size()+1),"Raw predecessor "+key);}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}}
    private static void rawTheory(ServerPlayer p,String category){try{var m=PlayerKnowledge.class.getDeclaredMethod("addKnowledge",KnowledgeType.class,String.class,int.class);m.setAccessible(true);require((boolean)m.invoke(KnowledgeStore.get(p),KnowledgeType.THEORY,category,32),"Raw theory fixture");}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}}
    private static void close(Minecraft mc){mc.player.closeContainer();mc.setScreen(null);}
    private static void click(Minecraft mc,BlockPos pos,Direction face){mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(pos.getCenter().add(Vec3.atLowerCornerOf(face.getNormal()).scale(.5)),face,pos,false));}
    private static boolean position(Minecraft mc,double x,double y,double z){if(mc.player.distanceToSqr(x,y,z)>.3){ready=0;return false;}return ++ready>=5;}
    private static double cameraX(int i){return POSITIONS[i].getX()+(FACES[i]==Direction.EAST?-2.5:3.5);}
    private static double cameraZ(int i){return POSITIONS[i].getZ()+(FACES[i]==Direction.SOUTH?-2.5:3.5);}
    private static float cameraYaw(int i){return FACES[i]==Direction.EAST?-135:FACES[i]==Direction.SOUTH?45:135;}
    private static void view(ServerPlayer p,int i){stow(p);p.connection.teleport(cameraX(i),112,cameraZ(i),cameraYaw(i),-12);}
    private static boolean viewed(Minecraft mc,int i){return position(mc,cameraX(i),112,cameraZ(i))&&mc.screen==null;}
    private static ArcaneWorkbenchBlockEntity bench(ServerLevel l){return (ArcaneWorkbenchBlockEntity)l.getBlockEntity(BENCH);}
    private static VisGeneratorBlockEntity generator(ServerLevel l,int i){return (VisGeneratorBlockEntity)l.getBlockEntity(POSITIONS[i]);}
    private static void book(Minecraft mc){var k=ResearchClient.golemPressKnowledge();var browser=new ThaumonomiconScreen(k,k.scanCount());mc.setScreen(new ThaumonomiconPageScreen(browser,ResearchCatalog.get("VISGENERATOR"),k,k.scanCount()));}
    private static void shift(Minecraft mc,boolean value){crouch=value;mc.player.input.shiftKeyDown=value;mc.player.setShiftKeyDown(value);mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player,value?ServerboundPlayerCommandPacket.Action.PRESS_SHIFT_KEY:ServerboundPlayerCommandPacket.Action.RELEASE_SHIFT_KEY));}
    @SubscribeEvent public static void input(MovementInputUpdateEvent event){if(!enabled()||stopped||!crouch)return;var mc=Minecraft.getInstance();if(mc.player==event.getEntity()&&mc.getSingleplayerServer()!=null&&WORLD.equals(mc.getSingleplayerServer().getWorldData().getLevelName())){var in=event.getInput();in.shiftKeyDown=true;in.forwardImpulse=in.leftImpulse=0;in.up=in.down=in.left=in.right=in.jumping=false;}}
    /** Optional ordinary Forge capability on six raw native chests in this one owned QA world only. */
    @SubscribeEvent public static void capabilities(AttachCapabilitiesEvent<BlockEntity> event){
        if(!enabled()||!started||event.getObject().getClass()!=ChestBlockEntity.class)return;
        var tile=event.getObject();int found=-1;for(int i=0;i<6;i++)if(tile.getBlockPos().equals(POSITIONS[i].relative(FACES[i]))){found=i;break;}if(found<0)return;final int index=found;
        var receiver=new Receiver(tile,index);var optional=LazyOptional.of(()->(IEnergyStorage)receiver);
        event.addCapability(ResourceLocation.fromNamespaceAndPath("thaumcraft","owned_generator_smoke_receiver"),new ICapabilityProvider(){
            @Override public <T> LazyOptional<T> getCapability(Capability<T> capability,Direction side){if(capability!=ForgeCapabilities.ENERGY||side!=FACES[index].getOpposite()||!(tile.getLevel() instanceof ServerLevel level)||!WORLD.equals(level.getServer().getWorldData().getLevelName()))return LazyOptional.empty();RECEIVERS[index]=receiver;return optional.cast();}
        });event.addListener(optional::invalidate);
    }
    private static final class Receiver implements IEnergyStorage {
        final BlockEntity tile;final int index;volatile int accepted,calls;long lastTick=Long.MIN_VALUE;
        Receiver(BlockEntity tile,int index){this.tile=tile;this.index=index;}
        @Override public int receiveEnergy(int maximum,boolean simulate){
            if(!(tile.getLevel() instanceof ServerLevel level)||!WORLD.equals(level.getServer().getWorldData().getLevelName())||tile.isRemoved()||level.getBlockEntity(tile.getBlockPos())!=tile)return 0;
            require(maximum>=0&&maximum<=20,"Native generator offered more than20FE");int amount=Math.min(maximum,200000-accepted);if(simulate)return amount;
            require(level.getGameTime()!=lastTick,"Native generator pushed twice in one world tick");lastTick=level.getGameTime();accepted+=amount;calls++;return amount;
        }
        @Override public int extractEnergy(int maximum,boolean simulate){return 0;}
        @Override public int getEnergyStored(){return accepted;}
        @Override public int getMaxEnergyStored(){return 200000;}
        @Override public boolean canExtract(){return false;}
        @Override public boolean canReceive(){return true;}
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!enabled()||stopped)return;var mc=Minecraft.getInstance();if(began==0)began=System.nanoTime();try{
            require(System.nanoTime()-began<1_500_000_000_000L,"Generator timeout scene="+scene+" phase="+phase);if(!started){start(mc);return;}if(mc.level==null||mc.player==null||mc.getOverlay()!=null||mc.screen instanceof ReceivingLevelScreen)return;
            require(mc.getSingleplayerServer()!=null&&WORLD.equals(mc.getSingleplayerServer().getWorldData().getLevelName()),"Wrong owned generator world");mc.getToasts().clear();
            if(++wait%100==0)LogUtils.getLogger().info("THAUMCRAFT_GENERATOR_HEARTBEAT: scene={} phase={} placed={} harvested={} pickedUp={} energy={} acceptedFE={} calls={} nativeFaces={} sparkFaces={} sparks={} poweredSparks={} drawn={}",scene,phase,placed,harvested,pickedUp,currentEnergy,totalAccepted,receiverCalls,observedFaces,GeneratorClientEffects.sparkFaces(),GeneratorClientEffects.emittedSparks(),GeneratorClientEffects.poweredSparks(),GeneratorClientEffects.drawnSparks());
            if(work!=null){if(!work.isDone())return;work.join();work=null;}if(!prepared){prepared=true;submit(mc,()->prepare(mc));return;}
            if(captured){if(SAVED.get()==scene+1&&++stable>=20){scene++;phase=wait=stable=ready=0;captured=captureRequested=false;}return;}if(scene==IMAGES.length){finish(mc);return;}
            if(!advance(mc)){stable=0;return;}if(++stable>=12)captureRequested=true;
        }catch(Throwable error){fail(mc,error);}
    }
    private static boolean advance(Minecraft mc){return switch(scene){case 0->scan(mc);case 1->study(mc);case 2->arcane(mc);case 9->pause(mc);case 10->resume(mc);case 11->dismantle(mc);default->scene>=3&&scene<9&&place(mc,scene-3);};}
    private static boolean scan(Minecraft mc){
        if(phase==0){phase=1;close(mc);submit(mc,()->{var p=player(mc);stow(p);held(p,new ItemStack(ScanningModule.THAUMOMETER.get()));require(p.getOffhandItem().isEmpty(),"Scan offhand occupied");p.setItemInHand(InteractionHand.OFF_HAND,AspectCrystalItem.create(Aspect.ENERGY,1));p.inventoryMenu.broadcastChanges();});return false;}
        if(phase==1){if(!mc.player.getMainHandItem().is(ScanningModule.THAUMOMETER.get())||mc.player.getOffhandItem().isEmpty()||++ready<15)return false;shift(mc,true);mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);phase=2;return false;}
        if(phase==2){if(!ResearchClient.golemPressKnowledge().knowsAspect(Aspect.ENERGY))return false;shift(mc,false);phase=3;submit(mc,()->{var p=player(mc);require(KnowledgeStore.get(p).knowsAspect(Aspect.ENERGY),"Native Potentia not earned");var stack=p.getOffhandItem();p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);require(p.getInventory().add(stack),"Cannot conserve scanned raw crystal");p.inventoryMenu.broadcastChanges();LogUtils.getLogger().info("THAUMCRAFT_GENERATOR_ASPECT_SCANNED: actualThaumometerC2S/rawtypedPotentiaCrystal/no targetaspectsetter");});return false;}return mc.screen==null;
    }
    private static boolean study(Minecraft mc){
        if(phase==0){phase=1;close(mc);submit(mc,()->{var p=player(mc);stow(p);for(int i=0;i<2;i++){rawTheory(p,CATEGORIES[i]);CURRENT_THEORY[i]=KnowledgeStore.get(p).rawKnowledge(KnowledgeType.THEORY,CATEGORIES[i]);}currentXp=p.totalExperience;held(p,new ItemStack(ResearchModule.THAUMONOMICON.get()));require(p.getInventory().add(new ItemStack(Items.REDSTONE_BLOCK))&&p.getInventory().add(new ItemStack(Items.PISTON)),"Raw study items unavailable");p.inventoryMenu.broadcastChanges();ResearchNetwork.sync(p);});return false;}
        if(phase==1){if(!mc.player.getMainHandItem().is(ResearchModule.THAUMONOMICON.get()))return false;var k=ResearchClient.golemPressKnowledge();if(!k.isResearchCompleteStrict("VISGENERATOR")){int current=k.researchStage("VISGENERATOR");if(expectedStage==current)return false;expectedStage=current;ResearchNetwork.requestAdvance("VISGENERATOR",current);return false;}phase=2;submit(mc,()->{var p=player(mc);for(int i=0;i<2;i++)require(KnowledgeStore.get(p).rawKnowledge(KnowledgeType.THEORY,CATEGORIES[i])==CURRENT_THEORY[i]-32,"Original raw32theory not paid");require(p.totalExperience==currentXp+10&&count(p,Items.REDSTONE_BLOCK)==0&&count(p,Items.PISTON)==0,"Original study XP/items changed");studyPaid++;LogUtils.getLogger().info("THAUMCRAFT_GENERATOR_STUDY_PAID: actualnativeC2S/32rawTheoryBASICS+32ARTIFICE/redstoneBlock1+piston1/+10earnedXP");});return false;}
        if(!(mc.screen instanceof ThaumonomiconPageScreen))book(mc);return true;
    }
    private static boolean arcane(Minecraft mc){
        if(phase==0){phase=1;close(mc);submit(mc,()->{var p=player(mc);stow(p);var l=p.serverLevel();require(bench(l).isEmpty()&&count(p)==0,"Target paid item supplied");var r=l.getRecipeManager().getAllRecipesFor(ArcaneModule.RECIPE_TYPE.get()).stream().filter(x->x.getResultItem(l.registryAccess()).is(item("vis_generator").getItem())).findFirst().orElseThrow();require(r.vis()==25,"Original25vis price changed");for(int i=0;i<r.getIngredients().size();i++){var ingredient=r.getIngredients().get(i);if(!ingredient.isEmpty())bench(l).setItem(i%r.gridWidth()+i/r.gridWidth()*3,ingredient.getItems()[0].copyWithCount(1));}for(int i=0;i<6;i++)if(r.crystalCost(i)>0)bench(l).setItem(9+i,AspectCrystalItem.create(Aspect.getAspect(ArcaneModule.PRIMALS[i]),r.crystalCost(i)));visBefore=AuraManager.getVis(l,BENCH);p.connection.teleport(BENCH.getX()+.5,112,BENCH.getZ()+2.5,180,20);});return false;}
        if(phase==1){if(!position(mc,BENCH.getX()+.5,112,BENCH.getZ()+2.5)||!mc.player.getMainHandItem().isEmpty())return false;phase=2;click(mc,BENCH,Direction.UP);return false;}
        if(!(mc.player.containerMenu instanceof ArcaneWorkbenchMenu menu))return false;
        if(phase==2){if(!menu.craftable()||!menu.getSlot(0).getItem().is(item("vis_generator").getItem()))return false;require(menu.requiredVis()==25,"Native price changed");phase=3;mc.gameMode.handleInventoryMouseClick(menu.containerId,0,0,ClickType.QUICK_MOVE,mc.player);return false;}
        if(phase==3){if(menu.hasRecipe())return false;phase=4;submit(mc,()->{var p=player(mc);var l=p.serverLevel();require(bench(l).isEmpty()&&count(p)==1&&AuraManager.getVis(l,BENCH)==visBefore-25,"Native bench physical/vis payment changed");benchPaid++;LogUtils.getLogger().info("THAUMCRAFT_GENERATOR_BENCH_PAID: actualnativeC2S/25vis/Ignis1Ordo1/fourPlanks+RedstoneDust+Piston+twoRareEarthNuggets+VisResonator->onephysicalVisGenerator");});return false;}return true;
    }
    private static int loose(ServerLevel l,BlockPos pos){return l.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(5),e->e.isAlive()&&e.getItem().is(item("vis_generator").getItem())).stream().mapToInt(e->e.getItem().getCount()).sum();}
    /** Approach the actual settled native drop. A chest can catch it above the arena floor. */
    private static void approachDrop(ServerPlayer p,BlockPos origin){
        if(count(p)==1)return;
        var drops=p.serverLevel().getEntitiesOfClass(ItemEntity.class,new AABB(origin).inflate(5),e->e.isAlive()&&e.getItem().is(item("vis_generator").getItem()));
        if(drops.isEmpty())return;
        require(count(p)==0&&drops.size()==1&&drops.get(0).getItem().getCount()==1,"Native pickup candidate lost physical uniqueness");
        var drop=drops.get(0);if(!drop.onGround())return;
        p.connection.teleport(drop.getX(),drop.getY(),drop.getZ(),0,0);
        LogUtils.getLogger().info("THAUMCRAFT_GENERATOR_NATIVE_PICKUP_APPROACH: actualSettledItemEntity={} at{}/{}/{}/movePlayerOnly/no itemmovement/directpickup",drop.getUUID(),drop.getX(),drop.getY(),drop.getZ());
    }
    private static boolean place(Minecraft mc,int i){require(wait<6000,"Native facing/receiver/spark stalled:"+FACES[i]);var pos=POSITIONS[i];
        if(phase==0){close(mc);if(i==0){phase=4;submit(mc,()->prepareHeldPlacement(player(mc),i));}else{phase=1;submit(mc,()->{var p=player(mc);stow(p);held(p,new ItemStack(Items.DIAMOND_PICKAXE));viewHolding(p,i-1);});}return false;}
        if(phase==1){if(!position(mc,cameraX(i-1),112,cameraZ(i-1))||!mc.player.getMainHandItem().is(Items.DIAMOND_PICKAXE))return false;phase=2;mc.gameMode.startDestroyBlock(POSITIONS[i-1],Direction.NORTH);return false;}
        if(phase==2){if(mc.level.getBlockState(POSITIONS[i-1]).is(CatalogBlocks.block("vis_generator"))){mc.gameMode.continueDestroyBlock(POSITIONS[i-1],Direction.NORTH);return false;}phase=3;submit(mc,()->{var p=player(mc);var l=p.serverLevel();require(l.getBlockState(POSITIONS[i-1]).isAir()&&loose(l,POSITIONS[i-1])+count(p)==1,"Native relocation harvest lost/duplicated device");harvested++;LogUtils.getLogger().info("THAUMCRAFT_GENERATOR_SURVIVAL_HARVESTED: facing{} actualnativeC2S/onephysicalpaidBlockItem; originaldropdoesnotpersiststoredFE",FACES[i-1]);});return false;}
        if(phase==3){if(!mc.player.getInventory().contains(item("vis_generator"))){if(wait%10==0)submit(mc,()->approachDrop(player(mc),POSITIONS[i-1]));return false;}phase=4;submit(mc,()->{var p=player(mc);require(count(p)==1,"Actual native item pickup absent");pickedUp++;prepareHeldPlacement(p,i);});return false;}
        if(phase==4){if(!position(mc,pos.getX()+2.5,112,pos.getZ()+2.5)||!mc.player.getMainHandItem().is(item("vis_generator").getItem()))return false;shift(mc,true);phase=5;click(mc,pos.below(),Direction.UP);return false;}
        if(phase==5){if(!mc.level.getBlockState(pos).is(CatalogBlocks.block("vis_generator")))return false;shift(mc,false);phase=6;submit(mc,()->{var p=player(mc);var l=p.serverLevel();require(count(p)==0&&l.getBlockState(pos).getValue(VisGeneratorBlock.FACING)==FACES[i],"Actual Survival placement/facing debit changed");placed++;view(p,i);LogUtils.getLogger().info("THAUMCRAFT_GENERATOR_SURVIVAL_PLACED: facing{} actualnativeC2S/samepaidBlockItem/nativeadjacentFEdiscovery/no forcedBlockState",FACES[i]);});return false;}
        return viewed(mc,i)&&RECEIVERS[i]!=null&&RECEIVERS[i].accepted>=200&&(GeneratorClientEffects.sparkFaces()&(1<<FACES[i].get3DDataValue()))!=0;
    }
    private static void viewHolding(ServerPlayer p,int i){p.connection.teleport(cameraX(i),112,cameraZ(i),cameraYaw(i),-12);}
    private static void prepareHeldPlacement(ServerPlayer p,int i){stow(p);held(p,take(p));p.connection.teleport(POSITIONS[i].getX()+2.5,112,POSITIONS[i].getZ()+2.5,135,-12);}
    private static boolean pause(Minecraft mc){var pos=POSITIONS[5];
        if(phase==0){if(currentEnergy<800)return false;phase=1;submit(mc,()->{var p=player(mc);stow(p);held(p,new ItemStack(Items.REDSTONE_BLOCK));viewHolding(p,5);});return false;}
        if(phase==1){if(!position(mc,cameraX(5),112,cameraZ(5))||!mc.player.getMainHandItem().is(Items.REDSTONE_BLOCK))return false;shift(mc,true);phase=2;click(mc,pos,Direction.NORTH);return false;}
        if(phase==2){if(!mc.level.getBlockState(pos.north()).is(Blocks.REDSTONE_BLOCK))return false;shift(mc,false);phase=3;submit(mc,()->{var p=player(mc);var l=p.serverLevel();require(!l.getBlockState(pos).getValue(VisGeneratorBlock.ENABLED)&&generator(l,5).getEnergyStored()>0,"Native powered charged source absent");pauseEnergy=generator(l,5).getEnergyStored();pauseAccepted=RECEIVERS[5].accepted;pauseStarted=l.getGameTime();poweredBefore=GeneratorClientEffects.poweredSparks();view(p,5);});return false;}
        if(phase==3){if(observedWorldTime-pauseStarted<80||GeneratorClientEffects.poweredSparks()<=poweredBefore)return false;phase=4;submit(mc,()->{var l=player(mc).serverLevel();require(generator(l,5).getEnergyStored()==pauseEnergy&&RECEIVERS[5].accepted==pauseAccepted,"Powered native source/receiverchanged");LogUtils.getLogger().info("THAUMCRAFT_GENERATOR_NATIVE_PAUSE: actualC2Sredstone/{}worldticks/energy{}unchanged/receiverFE{}unchanged/originalpoweredchargedSparks{}",l.getGameTime()-pauseStarted,pauseEnergy,pauseAccepted,GeneratorClientEffects.poweredSparks()-poweredBefore);});return false;}return viewed(mc,5);
    }
    private static boolean resume(Minecraft mc){var pos=POSITIONS[5].north();
        if(phase==0){phase=1;submit(mc,()->{var p=player(mc);stow(p);held(p,new ItemStack(Items.DIAMOND_PICKAXE));resumeBefore=RECEIVERS[5].accepted;viewHolding(p,5);});return false;}
        if(phase==1){if(!position(mc,cameraX(5),112,cameraZ(5))||!mc.player.getMainHandItem().is(Items.DIAMOND_PICKAXE))return false;phase=2;mc.gameMode.startDestroyBlock(pos,Direction.NORTH);return false;}
        if(phase==2){if(mc.level.getBlockState(pos).is(Blocks.REDSTONE_BLOCK)){mc.gameMode.continueDestroyBlock(pos,Direction.NORTH);return false;}phase=3;submit(mc,()->view(player(mc),5));return false;}
        if(RECEIVERS[5].accepted<resumeBefore+200)return false;if(phase==3){phase=4;submit(mc,()->{var l=player(mc).serverLevel();require(l.getBlockState(POSITIONS[5]).getValue(VisGeneratorBlock.ENABLED)&&RECEIVERS[5].accepted>=resumeBefore+200,"Actual native FE resume absent");LogUtils.getLogger().info("THAUMCRAFT_GENERATOR_NATIVE_RESUME: actualSurvivalRedstoneharvest/native20FEpush/receiver{}->{}FE/no directenergy/tick/aura reset",resumeBefore,RECEIVERS[5].accepted);});return false;}return viewed(mc,5);
    }
    private static boolean dismantle(Minecraft mc){var pos=POSITIONS[5];
        if(phase==0){phase=1;submit(mc,()->{var p=player(mc);stow(p);held(p,new ItemStack(Items.DIAMOND_PICKAXE));viewHolding(p,5);});return false;}
        if(phase==1){if(!position(mc,cameraX(5),112,cameraZ(5))||!mc.player.getMainHandItem().is(Items.DIAMOND_PICKAXE))return false;phase=2;mc.gameMode.startDestroyBlock(pos,Direction.NORTH);return false;}
        if(phase==2){if(mc.level.getBlockState(pos).is(CatalogBlocks.block("vis_generator"))){mc.gameMode.continueDestroyBlock(pos,Direction.NORTH);return false;}phase=3;submit(mc,()->{var p=player(mc);var l=p.serverLevel();require(l.getBlockState(pos).isAir()&&loose(l,pos)+count(p)==1,"Final native teardownlost/duplicated paiditem");harvested++;view(p,5);LogUtils.getLogger().info("THAUMCRAFT_GENERATOR_SURVIVAL_HARVESTED: facingEAST actualnativeC2S/onephysicalpaidBlockItem/finalteardown");});return false;}return viewed(mc,5);
    }
    @SubscribeEvent public static void serverTick(TickEvent.ServerTickEvent event){if(event.phase!=TickEvent.Phase.END||!enabled()||stopped||!prepared||!WORLD.equals(event.getServer().getWorldData().getLevelName()))return;var l=event.getServer().overworld();observedWorldTime=l.getGameTime();totalAccepted=receiverCalls=0;for(int i=0;i<6;i++){var receiver=RECEIVERS[i];if(receiver!=null){totalAccepted+=receiver.accepted;receiverCalls+=receiver.calls;}if(l.getBlockEntity(POSITIONS[i]) instanceof VisGeneratorBlockEntity g){currentEnergy=g.getEnergyStored();if(receiver!=null&&receiver.accepted>0)observedFaces|=1<<FACES[i].get3DDataValue();}}}
    private static void prepare(Minecraft mc){
        var p=player(mc);var l=p.serverLevel();p.setInvulnerable(true);for(int x=-2;x<=1;x++)for(int z=-2;z<=1;z++)l.getChunk(x,z);for(var pos:BlockPos.betweenClosed(-20,111,-15,20,121,20))l.setBlockAndUpdate(pos,pos.getY()==111?Blocks.BEDROCK.defaultBlockState():Blocks.AIR.defaultBlockState());l.setDayTime(6000);l.setBlockAndUpdate(BENCH,ArcaneModule.WORKBENCH.get().defaultBlockState());
        for(int i=0;i<6;i++){l.setBlockAndUpdate(POSITIONS[i].below(),Blocks.STONE.defaultBlockState());l.setBlockAndUpdate(POSITIONS[i].relative(FACES[i]),Blocks.CHEST.defaultBlockState());}
        for(int x=-2;x<=1;x++)for(int z=-2;z<=1;z++){var pos=new ChunkPos(x,z).getMiddleBlockPosition(112);AuraManager.drainVis(l,pos,Float.MAX_VALUE,false);AuraManager.addVis(l,pos,500);AuraManager.drainFlux(l,pos,Float.MAX_VALUE,false);}
        p.getInventory().clearContent();for(String key:List.of("FIRSTSTEPS","KNOWLEDGETYPES","UNLOCKARTIFICE","BASEARTIFICE","REDSTONERELAY"))predecessor(p,key);require(!KnowledgeStore.get(p).isResearchKnown("VISGENERATOR")&&!KnowledgeStore.get(p).knowsAspect(Aspect.ENERGY),"Target research/aspect supplied");p.connection.teleport(-8.5,112,16.5,135,-12);ResearchNetwork.sync(p);
        LogUtils.getLogger().info("THAUMCRAFT_GENERATOR_FIXTURES: ownedSurvivalarena/rawbench/REDSTONERELAYprecursor/rawknowledge/studymaterials/typedPotentiacrystal/originalbenchingredients/initial500visper16loadedchunks/sixnativeChestwithOPTINQAForgeFEreceiverscapacity200000; vanillaChesthasnoFEconsumer/gameplaydummyregistrationsnone; onepaidtargetdevice reused, no activeenergy/tick/aura/RNG reset");
    }
    @SubscribeEvent public static void rendered(TickEvent.RenderTickEvent event){if(event.phase!=TickEvent.Phase.END||!enabled()||stopped||!captureRequested||captured)return;var mc=Minecraft.getInstance();if(scene>=3&&scene<=10&&GeneratorClientEffects.activeSparks()==0)return;try{captured=true;captureRequested=false;String name="tc6-generator-"+IMAGES[scene]+".png";var file=new File(new File(mc.gameDirectory,"screenshots"),name);Files.deleteIfExists(file.toPath());Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),message->{if(file.isFile()&&file.length()>0){SAVED.incrementAndGet();LogUtils.getLogger().info("THAUMCRAFT_GENERATOR_CLIENT_SMOKE_IMAGE: {}",file.getAbsolutePath());}else mc.execute(()->fail(mc,new AssertionError("Missing native Generator capture")));});}catch(Throwable error){fail(mc,error);}}
    private static void start(Minecraft mc){if(mc.screen instanceof AccessibilityOnboardingScreen){mc.options.onboardAccessibility=false;mc.options.save();mc.setScreen(new TitleScreen());return;}if(!(mc.screen instanceof TitleScreen)||mc.getOverlay()!=null)return;started=true;previous=mc.options.tutorialStep;mc.options.tutorialStep=TutorialSteps.NONE;mc.getTutorial().stop();mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(3);mc.options.simulationDistance().set(5);mc.options.guiScale().set(2);mc.options.fov().set(80);mc.options.cloudStatus().set(CloudStatus.OFF);mc.options.setCameraType(CameraType.FIRST_PERSON);mc.options.hideGui=false;mc.resizeDisplay();var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);rules.getRule(GameRules.RULE_DAYLIGHT).set(false,null);rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false,null);rules.getRule(GameRules.RULE_RANDOMTICKING).set(0,null);mc.createWorldOpenFlows().createFreshLevel(WORLD,new LevelSettings(WORLD,GameType.SURVIVAL,false,Difficulty.PEACEFUL,true,rules,WorldDataConfiguration.DEFAULT),new WorldOptions(0x54433637L,false,false),WorldPresets::createNormalWorldDimensions);}
    private static void finish(Minecraft mc){
        if(phase==0){phase=1;close(mc);shift(mc,false);submit(mc,()->{var p=player(mc);var l=p.serverLevel();require(studyPaid==1&&benchPaid==1&&placed==6&&harvested==6&&pickedUp==5&&observedFaces==63,"Final native paid/placement census changed");for(int i=0;i<6;i++)require(l.getBlockState(POSITIONS[i]).isAir()&&RECEIVERS[i]!=null&&RECEIVERS[i].accepted>=200,"One original face/nativeFEroute absent");require(loose(l,POSITIONS[5])+count(p)==1&&totalAccepted>=1200,"Paid physical finaldevice/energyledgerchanged");LogUtils.getLogger().info("THAUMCRAFT_GENERATOR_CLIENT_FACTS: scenes{} nativeAspectPotentia/study1/paidTheoryBASICS32ARTIFICE32/paidStudyRedstoneBlock1Piston1/earnedXP10/paidBench1/vis25/Ignis1Ordo1/onePaidDevice/SurvivalPlacements6/nativeDropPickups5/SurvivalHarvests6/nativeFacingMask63/acceptedFE{}/receiverCalls{}/maxOffer20/redstonePause80ticks/nativePoweredChargedSparks{}/nativeSparkEmitted{}/drawn{}/sparkFaces{}/positiveEnergySamples{}/poweredPositiveSamples{}; explicitREDSTONERELAY/rawknowledge/materials/arena/initialaura/OPTINownedChestFEcapability,notvanillaChestFE/notfullsurvival/multiplayer/reload/audio",SAVED.get(),totalAccepted,receiverCalls,GeneratorClientEffects.poweredSparks(),GeneratorClientEffects.emittedSparks(),GeneratorClientEffects.drawnSparks(),GeneratorClientEffects.sparkFaces(),GeneratorClientEffects.positiveEnergySamples(),GeneratorClientEffects.poweredPositiveSamples());validated=true;});return;}
        if(!validated)return;require(SAVED.get()==12&&GeneratorClientEffects.sparkFaces()==63&&GeneratorClientEffects.drawnSparks()>0&&GeneratorClientEffects.poweredSparks()>0,"Native original visualrouteincomplete");stopped=true;mc.options.tutorialStep=previous;LogUtils.getLogger().info("THAUMCRAFT_GENERATOR_RENDER_AUDIT_OK: {} scenes; originalsixfacingbakedmodels/sameOriginalTextureForEnabledAndPoweredStates/actualstoredenergyrandomDisplayTickSpark/FXGeneric16grid8frames/originalpoweredchargedquirk/noGUI",SAVED.get());LogUtils.getLogger().info("THAUMCRAFT_GENERATOR_CLIENT_SMOKE_OK: {} scenes; actualPotentiascan/paidstudy/25visbench/onephysicalgenerator6Survivalplacements/native20FEForgecapreceivers/80tickredstonepause/chargedpoweredsparks/resume/6physicalteardowns,world={}",SAVED.get(),WORLD);mc.stop();
    }
    private static void fail(Minecraft mc,Throwable error){if(stopped)return;stopped=true;if(previous!=null)mc.options.tutorialStep=previous;LogUtils.getLogger().error("THAUMCRAFT_GENERATOR_CLIENT_SMOKE_FAILED",error);mc.stop();}
}
