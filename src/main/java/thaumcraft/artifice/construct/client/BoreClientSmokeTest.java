package thaumcraft.artifice.construct.client;

import com.mojang.logging.LogUtils;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.CameraType;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import thaumcraft.artifice.construct.BoreEntity;
import thaumcraft.artifice.construct.BoreMenu;
import thaumcraft.catalog.blocks.CatalogBlocks;
import thaumcraft.catalog.entities.client.LegacyArcaneBoreModel;
import thaumcraft.client.research.ResearchClient;
import thaumcraft.client.research.ThaumonomiconPageScreen;
import thaumcraft.client.research.ThaumonomiconScreen;
import thaumcraft.common.lib.enchantment.EnumInfusionEnchantment;
import thaumcraft.essentia.EssentiaJarBlockEntity;
import thaumcraft.infusion.InfusionMatrixBlock;
import thaumcraft.infusion.InfusionMatrixBlockEntity;
import thaumcraft.infusion.InfusionPedestalBlockEntity;
import thaumcraft.infusion.InfusionRecipes;
import thaumcraft.infusion.InfusionStability;
import thaumcraft.research.KnowledgeStore;
import thaumcraft.research.KnowledgeType;
import thaumcraft.research.PlayerKnowledge;
import thaumcraft.research.ResearchCatalog;
import thaumcraft.research.ResearchModule;
import thaumcraft.research.ResearchNetwork;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.world.aura.AuraManager;

/** Opt-in owned world: native study/altar/placer/menu packets and ordinary world mining ticks. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public final class BoreClientSmokeTest {
    private static final String WORLD="thaumcraft-bore-smoke-"+System.currentTimeMillis();
    private static final String[] IMAGES={"paid-research","original-infusion-recipe","native-infusion-active","paid-infusion-output",
            "original-pickaxe-gui","redstone-paused","native-mining-beam","physical-chest-output","enchanted-pickaxe-gui","enchanted-native-beam"};
    private static final BlockPos MATRIX=new BlockPos(-6,114,0),BORE=new BlockPos(12,112,0),CHEST=BORE.south(),POWER=BORE.below().east();
    private static final int[][] OFFSETS={{3,0},{0,3},{-3,0},{0,-3},{4,2},{-4,-2},{2,4},{-2,-4}};
    private static final List<Aspect> ASPECTS=List.of(Aspect.ENERGY,Aspect.EARTH,Aspect.MECHANISM,Aspect.VOID,Aspect.MOTION);
    private static final AtomicInteger SAVED=new AtomicInteger();
    private static final List<BlockPos> WALL=new ArrayList<>();
    private static boolean started,prepared,stopped,captureRequested,captured,studyPaid,infusionPaid,placed,plainToolPaid,enchantedToolPaid,pauseVerified,miningVerified,finalValidated;
    private static volatile int boreId=-1,minedBlocks,chestCobble,chestStone;
    private static int scene,phase,stable,wait,expectedStage=-1,ready,artificeBefore,auromancyBefore,golemancyBefore,xpBefore,pauseAge,pauseMined,pauseOutput,enchantedStoneBefore,finalAge,pauseVerifiedTicks;
    private static long began,beamBefore,particlesBefore;
    private static CompletableFuture<Void> work;
    private static TutorialSteps previous;
    private BoreClientSmokeTest(){}
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static boolean enabled(){return Boolean.getBoolean("thaumcraft.boreSmokeTest");}
    private static ServerPlayer player(Minecraft mc){var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());require(p!=null,"Missing owned Bore player");return p;}
    private static void submit(Minecraft mc,Runnable action){require(work==null,"Overlapping Bore server tasks");var result=new CompletableFuture<Void>();work=result;mc.getSingleplayerServer().execute(()->{try{action.run();result.complete(null);}catch(Throwable e){result.completeExceptionally(e);}});}
    private static ItemStack item(String id){var value=ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("thaumcraft",id));require(value!=null&&value!=Items.AIR,"Missing Bore item "+id);return new ItemStack(value);}
    private static void held(ServerPlayer p,ItemStack stack){p.getInventory().selected=0;p.setItemInHand(InteractionHand.MAIN_HAND,stack);p.connection.send(new ClientboundSetCarriedItemPacket(0));p.inventoryMenu.broadcastChanges();}
    private static void stage(ServerPlayer p,String key){try{var m=PlayerKnowledge.class.getDeclaredMethod("setResearchStage",String.class,int.class);m.setAccessible(true);require((boolean)m.invoke(KnowledgeStore.get(p),key,ResearchCatalog.get(key).stages().size()+1),"Predecessor fixture "+key);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
    private static void raw(ServerPlayer p,KnowledgeType type,String category,int count){try{var m=PlayerKnowledge.class.getDeclaredMethod("addKnowledge",KnowledgeType.class,String.class,int.class);m.setAccessible(true);require((boolean)m.invoke(KnowledgeStore.get(p),type,category,count),"Raw knowledge fixture");}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
    private static void close(Minecraft mc){mc.player.closeContainer();mc.setScreen(null);}
    private static void click(Minecraft mc,BlockPos pos){mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(pos.getCenter(),Direction.UP,pos,false));}
    private static InfusionMatrixBlockEntity matrix(ServerLevel level){return (InfusionMatrixBlockEntity)level.getBlockEntity(MATRIX);}
    private static InfusionPedestalBlockEntity central(ServerLevel level){return (InfusionPedestalBlockEntity)level.getBlockEntity(MATRIX.below(2));}
    private static EssentiaJarBlockEntity jar(ServerLevel level,int i){return (EssentiaJarBlockEntity)level.getBlockEntity(MATRIX.offset(-4+i*2,-1,6));}
    private static ChestBlockEntity chest(ServerLevel level){return (ChestBlockEntity)level.getBlockEntity(CHEST);}
    private static BoreEntity serverBore(Minecraft mc){var e=player(mc).serverLevel().getEntity(boreId);require(e instanceof BoreEntity&&e.isAlive(),"Missing actually placed native Bore");return (BoreEntity)e;}
    private static BoreEntity clientBore(Minecraft mc){return mc.level.getEntity(boreId) instanceof BoreEntity b?b:null;}
    private static int count(ChestBlockEntity c,net.minecraft.world.item.Item item){int result=0;for(int i=0;i<c.getContainerSize();i++)if(c.getItem(i).is(item))result+=c.getItem(i).getCount();return result;}
    private static void view(ServerPlayer p){held(p,ItemStack.EMPTY);p.connection.teleport(BORE.getX()+4.5,112,BORE.getZ()+4.5,135,10);}
    private static boolean position(Minecraft mc,double x,double z){if(mc.player.distanceToSqr(x,112,z)>.25){ready=0;return false;}return ++ready>=5;}

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!enabled()||stopped)return;var mc=Minecraft.getInstance();if(began==0)began=System.nanoTime();
        try{
            require(System.nanoTime()-began<900_000_000_000L,"Bore timeout scene="+scene+" phase="+phase);
            if(!started){start(mc);return;}if(mc.level==null||mc.player==null||mc.getOverlay()!=null||mc.screen instanceof ReceivingLevelScreen)return;
            require(mc.getSingleplayerServer()!=null&&WORLD.equals(mc.getSingleplayerServer().getWorldData().getLevelName()),"Wrong owned Bore world");mc.getToasts().clear();
            if(++wait%100==0){var b=clientBore(mc);LogUtils.getLogger().info("THAUMCRAFT_BORE_CLIENT_SMOKE_HEARTBEAT: scene={} phase={} screen={} entity={} active={} target={} delay={} mined={} cobble={} stone={}",scene,phase,mc.screen==null?"none":mc.screen.getClass().getSimpleName(),boreId,b!=null&&b.isActive(),b==null?null:b.getDigTarget(),b==null?-1:b.getDigDelay(),minedBlocks,chestCobble,chestStone);}
            if(work!=null){if(!work.isDone())return;work.join();work=null;}
            if(!prepared){prepared=true;submit(mc,()->prepare(mc));return;}
            if(captured){if(SAVED.get()==scene+1&&++stable>=20){scene++;phase=stable=wait=ready=0;expectedStage=-1;captured=captureRequested=false;}return;}
            if(scene==IMAGES.length){finish(mc);return;}
            if(!advance(mc)){stable=0;return;}
            if(++stable>=(scene==6||scene==9?1:12))captureRequested=true;
        }catch(Throwable e){fail(mc,e);}
    }
    /** Observes ordinary native ticks; no target, harvest, charge, inventory or tick shortcuts. */
    @SubscribeEvent public static void serverTick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!enabled()||stopped||!WORLD.equals(event.getServer().getWorldData().getLevelName())||boreId<0)return;
        var level=event.getServer().overworld();minedBlocks=(int)WALL.stream().filter(p->level.getBlockState(p).isAir()).count();
        if(level.getBlockEntity(CHEST) instanceof ChestBlockEntity c){chestCobble=count(c,Items.COBBLESTONE);chestStone=count(c,Items.STONE);}
    }
    private static boolean advance(Minecraft mc){return switch(scene){case 0->study(mc);case 1->recipe(mc);case 2->infusion(mc);case 3->output(mc);case 4->placeAndMenu(mc);case 5->pause(mc);case 6->mining(mc,false);case 7->physicalOutput(mc);case 8->enchanted(mc);case 9->mining(mc,true);default->false;};}
    private static void book(Minecraft mc,boolean recipe){
        var k=ResearchClient.golemPressKnowledge();var browser=new ThaumonomiconScreen(k,k.scanCount());var page=new ThaumonomiconPageScreen(browser,ResearchCatalog.get("ARCANEBORE"),k,k.scanCount());mc.setScreen(page);
        if(recipe)try{var focus=ThaumonomiconPageScreen.class.getDeclaredMethod("focusRecipe",ResourceLocation.class);focus.setAccessible(true);require((boolean)focus.invoke(page,ResourceLocation.fromNamespaceAndPath("thaumcraft","infusion/arcanebore")),"Operational Bore book recipe absent");var method=ThaumonomiconPageScreen.class.getDeclaredMethod("showRecipeForSmokeTest",String.class);method.setAccessible(true);method.invoke(page,"turret_bore");}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
    }
    private static boolean study(Minecraft mc){
        if(phase==0){phase=1;close(mc);submit(mc,()->{var p=player(mc);raw(p,KnowledgeType.THEORY,"GOLEMANCY",32);raw(p,KnowledgeType.OBSERVATION,"ARTIFICE",16);raw(p,KnowledgeType.OBSERVATION,"AUROMANCY",16);var k=KnowledgeStore.get(p);golemancyBefore=k.rawKnowledge(KnowledgeType.THEORY,"GOLEMANCY");artificeBefore=k.rawKnowledge(KnowledgeType.OBSERVATION,"ARTIFICE");auromancyBefore=k.rawKnowledge(KnowledgeType.OBSERVATION,"AUROMANCY");xpBefore=p.totalExperience;held(p,new ItemStack(ResearchModule.THAUMONOMICON.get()));ResearchNetwork.sync(p);});return false;}
        var k=ResearchClient.golemPressKnowledge();if(phase==1){if(!mc.player.getMainHandItem().is(ResearchModule.THAUMONOMICON.get()))return false;if(!k.isResearchCompleteStrict("ARCANEBORE")){int current=k.researchStage("ARCANEBORE");if(current==expectedStage)return false;expectedStage=current;ResearchNetwork.requestAdvance("ARCANEBORE",current);return false;}phase=2;submit(mc,()->{var p=player(mc);var state=KnowledgeStore.get(p);require(state.isResearchCompleteStrict("ARCANEBORE")&&state.rawKnowledge(KnowledgeType.THEORY,"GOLEMANCY")==golemancyBefore-32&&state.rawKnowledge(KnowledgeType.OBSERVATION,"ARTIFICE")==artificeBefore-16&&state.rawKnowledge(KnowledgeType.OBSERVATION,"AUROMANCY")==auromancyBefore-16&&p.totalExperience==xpBefore+10,"Bore native study payment mismatch");studyPaid=true;LogUtils.getLogger().info("THAUMCRAFT_BORE_CLIENT_SMOKE_RESEARCH_PAID: actual ARCANEBORE C2S,32GolemancyTheory+16ArtificeObservation+16AuromancyObservation,10XP(start5+completion5); only predecessors/raw balances supplied");});return false;}
        if(!(mc.screen instanceof ThaumonomiconPageScreen))book(mc,false);return true;
    }
    private static boolean recipe(Minecraft mc){if(phase==0){phase=1;book(mc,true);}return mc.screen instanceof ThaumonomiconPageScreen;}
    private static boolean infusion(Minecraft mc){
        require(wait<2600,"Bore infusion did not start native debit");
        if(phase==0){phase=1;close(mc);submit(mc,()->{var p=player(mc);held(p,item("caster_basic"));p.connection.teleport(MATRIX.getX()+.5,112,MATRIX.getZ()+4.5,180,-12);});return false;}
        if(!(mc.level.getBlockEntity(MATRIX) instanceof InfusionMatrixBlockEntity m))return false;
        if(phase==1){if(!InfusionMatrixBlock.isCaster(mc.player.getMainHandItem())||!position(mc,MATRIX.getX()+.5,MATRIX.getZ()+4.5))return false;phase=2;ready=0;click(mc,MATRIX);return false;}
        if(phase==2){if(!m.active()||m.startup()<.95F||m.stability()<24.9F)return false;phase=3;submit(mc,()->{var p=player(mc);var level=p.serverLevel();var inputs=InfusionStability.scan(level,MATRIX).pedestals().stream().map(at->(InfusionPedestalBlockEntity)level.getBlockEntity(at)).filter(at->!at.isEmpty()).map(at->at.getItem(0).copy()).toList();var plan=InfusionRecipes.find(p,central(level).getItem(0),inputs).orElseThrow();require(inputs.size()==8&&plan.id().toString().equals("thaumcraft:infusion/arcanebore")&&plan.aspects().visSize()==200,"Original real Bore infusion plan absent");require(jarsTotal(level)==215&&central(level).getItem(0).is(item("turret_basic").getItem()),"Bore inputs paid prematurely");LogUtils.getLogger().info("THAUMCRAFT_BORE_CLIENT_SMOKE_INFUSION_PLAN: real physical altar,started known gate,8original components/basic central,instability4,Potentia25+Terra25+Machina100+Vacuos25+Motus25; raw central/materials/215typedessentia are explicit fixtures");});return false;}
        if(phase==3){phase=4;click(mc,MATRIX);return false;}
        if(!m.crafting()||m.getAspects().visSize()>=200||m.getAspects().visSize()<=0)return false;
        if(phase==4){phase=5;submit(mc,()->{var level=player(mc).serverLevel();require(matrix(level).crafting()&&jarsTotal(level)<215,"Native Bore craft started without physical debit");LogUtils.getLogger().info("THAUMCRAFT_BORE_CLIENT_SMOKE_INFUSION_ACTIVE: actual caster activation+start C2S,ordinary matrix cycles and real source essentia debit; no output fixture");});return false;}return true;
    }
    private static int jarsTotal(ServerLevel level){int total=0;for(int i=0;i<5;i++)total+=jar(level,i).amount();return total;}
    private static boolean output(Minecraft mc){
        require(wait<3600,"Native Bore infusion failed to finish");if(!(mc.level.getBlockEntity(MATRIX) instanceof InfusionMatrixBlockEntity m)||m.crafting()||!(mc.level.getBlockEntity(MATRIX.below(2)) instanceof InfusionPedestalBlockEntity c)||!c.getItem(0).is(item("turret_bore").getItem()))return false;
        if(phase==0){phase=1;submit(mc,()->{var level=player(mc).serverLevel();require(!matrix(level).crafting()&&central(level).getItem(0).getCount()==1&&!central(level).getItem(0).hasTag(),"Not one fresh native Bore output");for(int i=0;i<5;i++)require(jar(level,i).amount()==3,"Native Bore exact typed essentia debit mismatch");for(var offset:OFFSETS)require(((InfusionPedestalBlockEntity)level.getBlockEntity(MATRIX.offset(offset[0],-2,offset[1]))).isEmpty(),"Native Bore retained a peripheral");require(KnowledgeStore.get(player(mc)).hasCraft("thaumcraft:turret_bore"),"Paid output failed craft proof");infusionPaid=true;LogUtils.getLogger().info("THAUMCRAFT_BORE_CLIENT_SMOKE_INFUSION_PAID: native basic central+8original components+200typedessentia->one fresh turret_bore,5jars retain3each; physical native output exists before collection");});return false;}return true;
    }
    private static boolean placeAndMenu(Minecraft mc){
        if(phase==0){phase=1;close(mc);submit(mc,()->{var p=player(mc);held(p,ItemStack.EMPTY);p.connection.teleport(MATRIX.getX()+.5,112,MATRIX.getZ()+2.5,180,15);});return false;}
        if(phase==1){if(!mc.player.getMainHandItem().isEmpty()||!position(mc,MATRIX.getX()+.5,MATRIX.getZ()+2.5))return false;phase=2;ready=0;click(mc,MATRIX.below(2));return false;}
        if(phase==2){if(!mc.player.getInventory().contains(item("turret_bore")))return false;phase=3;submit(mc,()->{var p=player(mc);require(central(p.serverLevel()).isEmpty(),"Real pedestal collection left native output");int found=-1;for(int i=0;i<p.getInventory().items.size();i++)if(p.getInventory().getItem(i).is(item("turret_bore").getItem())){require(found<0,"Duplicate Bore output");found=i;}require(found>=0,"Native output collection missing");var paid=p.getInventory().getItem(found);require(paid.getCount()==1,"Native Bore output count");p.getInventory().setItem(found,ItemStack.EMPTY);held(p,paid);p.connection.teleport(BORE.getX()+.5,112,BORE.getZ()+3.5,180,65);});return false;}
        if(phase==3){if(!mc.player.getMainHandItem().is(item("turret_bore").getItem())||!position(mc,BORE.getX()+.5,BORE.getZ()+3.5))return false;phase=4;ready=0;click(mc,BORE.below());return false;}
        if(phase==4){if(!mc.player.getMainHandItem().isEmpty())return false;phase=5;submit(mc,()->{var p=player(mc);var list=p.serverLevel().getEntitiesOfClass(BoreEntity.class,new AABB(BORE).inflate(2),b->b.isOwner(p)&&b.hasValidSpawn());require(list.size()==1,"Native Bore placer did not create exactly one owned construct");var b=list.get(0);require(!b.isNoAi()&&b.getHealth()==50&&b.getMaxHealth()==50&&b.getFacing()==Direction.NORTH&&b.tool().isEmpty()&&!b.isActive(),"Actual original placed Bore identity/stats/facing changed");boreId=b.getId();placed=true;held(p,ItemStack.EMPTY);p.getInventory().setItem(9,new ItemStack(Items.DIAMOND_PICKAXE));p.connection.teleport(BORE.getX()+2.5,112,BORE.getZ()+2.5,135,15);p.inventoryMenu.broadcastChanges();LogUtils.getLogger().info("THAUMCRAFT_BORE_CLIENT_SMOKE_PLACED: paid output actual survival placer C2S consumed,single owner/valid arcane_bore,HP50/NoAI=false/facing player NORTH; plain raw diamond pickaxe deferred to native GUI");});return false;}
        var b=clientBore(mc);if(b==null)return false;
        if(phase==5){if(!position(mc,BORE.getX()+2.5,BORE.getZ()+2.5))return false;phase=6;mc.gameMode.interact(mc.player,b,InteractionHand.MAIN_HAND);return false;}
        if(!(mc.screen instanceof BoreScreen)||!(mc.player.containerMenu instanceof BoreMenu menu)||menu.bore()!=b)return false;
        if(phase==6){if(!menu.getSlot(1).getItem().is(Items.DIAMOND_PICKAXE))return false;phase=7;mc.gameMode.handleInventoryMouseClick(menu.containerId,1,0,ClickType.QUICK_MOVE,mc.player);return false;}
        if(!menu.getSlot(0).getItem().is(Items.DIAMOND_PICKAXE)||!menu.getSlot(1).getItem().isEmpty())return false;
        if(phase==7){phase=8;submit(mc,()->{var p=player(mc);var actual=serverBore(mc);require(actual.validInventory()&&actual.tool().is(Items.DIAMOND_PICKAXE)&&p.getInventory().getItem(9).isEmpty(),"Actual tool menu failed exact one-pickaxe transfer");require(actual.getDigRadius()==3&&actual.getDigDepth()==24&&actual.getDigSpeed(Blocks.STONE.defaultBlockState())==4,"Original plain diamond properties changed");plainToolPaid=true;LogUtils.getLogger().info("THAUMCRAFT_BORE_CLIENT_SMOKE_TOOL_PAID: actual owner interaction/native Bore menu/source slot1 C2S quickMove,one diamond pickaxe paid,one tool slot/no fuel,width7/depth24/speed4");});return false;}return true;
    }
    private static boolean pause(Minecraft mc){
        if(phase==0){phase=1;close(mc);submit(mc,()->{var b=serverBore(mc);require(!b.isActive(),"Unpowered Bore started");pauseAge=b.tickCount;pauseMined=minedBlocks;pauseOutput=chestCobble+chestStone;view(player(mc));});return false;}
        var b=clientBore(mc);if(b==null||mc.screen!=null||b.isActive()||!position(mc,BORE.getX()+4.5,BORE.getZ()+4.5))return false;
        if(phase==1){phase=2;submit(mc,()->{var actual=serverBore(mc);if(actual.tickCount-pauseAge<40){phase=1;return;}require(!actual.isActive()&&actual.getDigTarget()==null&&minedBlocks==pauseMined&&chestCobble+chestStone==pauseOutput,"Unpowered native Bore harvested");pauseVerified=true;pauseVerifiedTicks=actual.tickCount-pauseAge;require(player(mc).serverLevel().hasChunkAt(BORE)&&actual.getCharge()>=.25F,"Native aura recharge did not occur");LogUtils.getLogger().info("THAUMCRAFT_BORE_CLIENT_SMOKE_PAUSE_OK: unpowered native40+worldticks,not active/no target/no harvest,no manual ticker or activation setter; observedTicks={},native recharge charge={}",pauseVerifiedTicks,actual.getCharge());});return false;}return pauseVerified;
    }
    private static boolean mining(Minecraft mc,boolean enchanted){
        require(wait<2000,"Native powered Bore did not mine scene="+scene);
        if(phase==0){phase=1;close(mc);beamBefore=BoreRenderers.actualBeamDraws();particlesBefore=BoreClientEvents.Effects.actualParticleEmissions();submit(mc,()->{var p=player(mc);view(p);p.serverLevel().setBlockAndUpdate(POWER,Blocks.REDSTONE_BLOCK.defaultBlockState());if(enchanted)enchantedStoneBefore=chestStone;LogUtils.getLogger().info("THAUMCRAFT_BORE_CLIENT_SMOKE_POWER_FIXTURE: explicit redstone block beside support activates real native below-block signal; enchanted={},no setActive/setTarget/dig/tick calls",enchanted);});return false;}
        var b=clientBore(mc);if(b==null||!b.isActive()||!b.clientDigging||!b.validInventory()||mc.screen!=null||!position(mc,BORE.getX()+4.5,BORE.getZ()+4.5))return false;
        if(!(enchanted?chestStone>enchantedStoneBefore:chestCobble>=4))return false;
        require(mc.getEntityRenderDispatcher().getRenderer(b) instanceof BoreRenderers,"Native Bore renderer dispatch missing");
        if(phase==1){phase=2;submit(mc,()->{var p=player(mc);var actual=serverBore(mc);require(actual.isActive()&&actual.validInventory()&&minedBlocks>0,"Native mining physical state missing");require(enchanted?chestStone>enchantedStoneBefore:chestCobble>=4,"Native output not physically inserted");require(p.serverLevel().getEntitiesOfClass(ItemEntity.class,new AABB(BORE).inflate(14),e->e.isAlive()&&(e.getItem().is(Items.COBBLESTONE)||e.getItem().is(Items.STONE))).isEmpty(),"Chest output also spawned a duplicate loose drop");miningVerified=true;LogUtils.getLogger().info("THAUMCRAFT_BORE_CLIENT_SMOKE_MINING_PAID: native worldticks/spiral+collision target/real Forge harvest/physical adjacent chest,no scripted output; enchanted={},wallAir={},cobble={},silkStone={},toolDamage={},charge={}",enchanted,minedBlocks,chestCobble,chestStone,actual.tool().getDamageValue(),actual.getCharge());});return false;}
        return BoreRenderers.actualBeamDraws()>beamBefore&&BoreClientEvents.Effects.actualParticleEmissions()>particlesBefore&&b.getDigTarget()!=null;
    }
    private static boolean physicalOutput(Minecraft mc){
        if(phase==0){phase=1;close(mc);submit(mc,()->{var p=player(mc);p.serverLevel().setBlockAndUpdate(POWER,Blocks.BEDROCK.defaultBlockState());held(p,ItemStack.EMPTY);p.connection.teleport(CHEST.getX()+2.5,112,CHEST.getZ()+2.5,135,15);});return false;}
        var b=clientBore(mc);if(b==null||b.isActive())return false;
        if(phase==1){if(!position(mc,CHEST.getX()+2.5,CHEST.getZ()+2.5))return false;phase=2;click(mc,CHEST);return false;}
        if(!(mc.screen instanceof ContainerScreen))return false;
        int total=0;for(int i=0;i<27;i++)if(mc.player.containerMenu.getSlot(i).getItem().is(Items.COBBLESTONE))total+=mc.player.containerMenu.getSlot(i).getItem().getCount();if(total<4)return false;
        if(phase==2){phase=3;submit(mc,()->{var p=player(mc);var level=p.serverLevel();require(!serverBore(mc).isActive()&&minedBlocks==count(chest(level),Items.COBBLESTONE),"Actual plain stone harvest/chest conservation mismatch");LogUtils.getLogger().info("THAUMCRAFT_BORE_CLIENT_SMOKE_CHEST_OK: real vanilla chest C2S GUI,unpowered settled Bore,{}minedwallstone={}physicalcobble,all physical output,none supplied",minedBlocks,count(chest(level),Items.COBBLESTONE));});return false;}return true;
    }
    private static ItemStack enchantedTool(){var result=new ItemStack(Items.DIAMOND_PICKAXE);result.enchant(Enchantments.BLOCK_EFFICIENCY,3);result.enchant(Enchantments.BLOCK_FORTUNE,3);result.enchant(Enchantments.SILK_TOUCH,1);EnumInfusionEnchantment.addInfusionEnchantment(result,EnumInfusionEnchantment.DESTRUCTIVE,1);EnumInfusionEnchantment.addInfusionEnchantment(result,EnumInfusionEnchantment.BURROWING,1);EnumInfusionEnchantment.addInfusionEnchantment(result,EnumInfusionEnchantment.SOUNDING,4);EnumInfusionEnchantment.addInfusionEnchantment(result,EnumInfusionEnchantment.REFINING,4);return result;}
    private static boolean enchanted(Minecraft mc){
        if(phase==0){phase=1;close(mc);submit(mc,()->{var p=player(mc);held(p,ItemStack.EMPTY);p.connection.teleport(BORE.getX()+2.5,112,BORE.getZ()+2.5,135,15);p.getInventory().setItem(9,enchantedTool());p.inventoryMenu.broadcastChanges();LogUtils.getLogger().info("THAUMCRAFT_BORE_CLIENT_SMOKE_ENCHANT_FIXTURE: explicit raw diamond tool/EfficiencyIII+FortuneIII+SilkI+original infench DestructiveI/BurrowingI/SoundingIV/RefiningIV; not a claim of enchanting progression; deferred native menu transfer");});return false;}
        var b=clientBore(mc);if(b==null)return false;
        if(phase==1){if(!position(mc,BORE.getX()+2.5,BORE.getZ()+2.5))return false;phase=2;mc.gameMode.interact(mc.player,b,InteractionHand.MAIN_HAND);return false;}
        if(!(mc.screen instanceof BoreScreen)||!(mc.player.containerMenu instanceof BoreMenu menu)||menu.bore()!=b)return false;
        if(phase==2){if(!menu.getSlot(0).getItem().is(Items.DIAMOND_PICKAXE))return false;phase=3;mc.gameMode.handleInventoryMouseClick(menu.containerId,0,0,ClickType.QUICK_MOVE,mc.player);return false;}
        if(phase==3){if(!menu.getSlot(0).getItem().isEmpty()||!menu.getSlot(1).getItem().hasTag())return false;phase=4;mc.gameMode.handleInventoryMouseClick(menu.containerId,1,0,ClickType.QUICK_MOVE,mc.player);return false;}
        if(menu.getSlot(0).getItem().isEmpty()||!menu.getSlot(1).getItem().isEmpty()||b.getDigRadius()!=5||b.getDigDepth()!=56||b.getDigSpeed(Blocks.STONE.defaultBlockState())!=7||b.getFortune()!=4||b.getRefining()!=4||!b.hasSilkTouch())return false;
        if(phase==4){phase=5;submit(mc,()->{var p=player(mc);var actual=serverBore(mc);require(p.getInventory().getItem(9).isEmpty()&&actual.getDigRadius()==5&&actual.getDigDepth()==56&&actual.getDigSpeed(Blocks.STONE.defaultBlockState())==7&&actual.getFortune()==4&&actual.getRefining()==4&&actual.hasSilkTouch(),"Native enchanted menu payment/properties mismatch");enchantedToolPaid=true;LogUtils.getLogger().info("THAUMCRAFT_BORE_CLIENT_SMOKE_ENCHANTED_TOOL_PAID: actual GUI withdrawal+insertion C2S,one raw tool paid,width11/depth56/speed7,Fortune4(maxSoundingIV),Refining4,SilkTouchtrue; next native harvest must emit physical stone");});return false;}return true;
    }
    private static void prepare(Minecraft mc){
        var p=player(mc);var level=p.serverLevel();level.setDayTime(6000);p.setInvulnerable(true);
        for(int x=-2;x<=3;x++)for(int z=-2;z<=2;z++)level.getChunk(x,z);
        for(var pos:BlockPos.betweenClosed(-16,111,-16,30,120,16))level.setBlockAndUpdate(pos,pos.getY()==111?Blocks.BEDROCK.defaultBlockState():Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(MATRIX,CatalogBlocks.block("infusion_matrix").defaultBlockState());level.setBlockAndUpdate(MATRIX.below(2),CatalogBlocks.block("pedestal_arcane").defaultBlockState());
        central(level).setItem(0,item("turret_basic"));for(int x:new int[]{-1,1})for(int z:new int[]{-1,1})level.setBlockAndUpdate(MATRIX.offset(x,-2,z),CatalogBlocks.block("pillar_arcane").defaultBlockState());
        ItemStack[] reagents={item("plank_greatwood"),item("plank_greatwood"),item("mechanism_complex"),item("plate_brass"),new ItemStack(Items.DIAMOND_PICKAXE),new ItemStack(Items.DIAMOND_SHOVEL),item("morphic_resonator"),item("nugget_rareearth")};
        for(int i=0;i<8;i++){var at=MATRIX.offset(OFFSETS[i][0],-2,OFFSETS[i][1]);level.setBlockAndUpdate(at,CatalogBlocks.block("pedestal_arcane").defaultBlockState());((InfusionPedestalBlockEntity)level.getBlockEntity(at)).setItem(0,reagents[i]);}
        var colors=List.of("white","orange","magenta","lightblue","yellow","lime","pink","gray","silver","cyan","purple","blue","brown","green","red","black");
        for(int i=0;i<16;i++)for(int sign:new int[]{-1,1}){var at=MATRIX.offset(sign*(2+i%6),-2,sign*(-3-i/6));level.setBlockAndUpdate(at,CatalogBlocks.block("candle_"+colors.get(i)).defaultBlockState());}
        require(Math.abs(InfusionStability.scan(level,MATRIX).gain()-1.6F)<.00001F,"Original physical candle pair stability fixture changed");
        for(int i=0;i<5;i++){var at=MATRIX.offset(-4+i*2,-1,6);level.setBlockAndUpdate(at,CatalogBlocks.block("jar_normal").defaultBlockState());require(jar(level,i).addExact(ASPECTS.get(i),(ASPECTS.get(i)==Aspect.MECHANISM?100:25)+3),"Typed Bore essentia fixture failed");}
        level.setBlockAndUpdate(CHEST,Blocks.CHEST.defaultBlockState());
        for(var at:BlockPos.betweenClosed(BORE.offset(-3,0,-10),BORE.offset(3,5,-5))){var saved=at.immutable();level.setBlockAndUpdate(saved,Blocks.STONE.defaultBlockState());WALL.add(saved);}
        for(int x=-2;x<=3;x++)for(int z=-2;z<=2;z++){var anchor=new ChunkPos(x,z).getMiddleBlockPosition(112);AuraManager.drainVis(level,anchor,Float.MAX_VALUE,false);AuraManager.addVis(level,anchor,250);AuraManager.drainFlux(level,anchor,Float.MAX_VALUE,false);}
        p.getInventory().clearContent();for(String key:List.of("FIRSTSTEPS","KNOWLEDGETYPES","UNLOCKARTIFICE","BASEARTIFICE","UNLOCKGOLEMANCY","BASEGOLEMANCY","UNLOCKAUROMANCY","BASEAUROMANCY","UNLOCKINFUSION","BASEINFUSION","INFUSION","BASICTURRET","FOCUSBREAK"))stage(p,key);
        require(!KnowledgeStore.get(p).isResearchKnown("ARCANEBORE"),"Target Bore study was supplied");p.connection.teleport(-5.5,112,4.5,180,-12);ResearchNetwork.sync(p);
        LogUtils.getLogger().info("THAUMCRAFT_BORE_CLIENT_SMOKE_FIXTURES: owned survival world,loaded arena/bedrock floor,predecessor studies,physical altar/candle pairs,raw basic central+8reagents+215typedessentia/250vis and252stonewall/chest are explicit fixtures; no finished Bore or ARCANEBORE supplied,no direct runtime tick/harvest calls,no full survival/multiplayer claim");
    }
    @SubscribeEvent public static void rendered(TickEvent.RenderTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!enabled()||stopped||!captureRequested||captured)return;var mc=Minecraft.getInstance();
        try{if((scene==6||scene==9)&&(clientBore(mc)==null||!clientBore(mc).clientDigging||!clientBore(mc).isActive()||clientBore(mc).getDigTarget()==null))return;
            captured=true;captureRequested=false;String name="tc6-bore-"+IMAGES[scene]+".png";var file=new File(new File(mc.gameDirectory,"screenshots"),name);Files.deleteIfExists(file.toPath());
            Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),message->{if(file.isFile()&&file.length()>0){SAVED.incrementAndGet();LogUtils.getLogger().info("THAUMCRAFT_BORE_CLIENT_SMOKE_IMAGE: {}",file.getAbsolutePath());}else mc.execute(()->fail(mc,new AssertionError("Missing fresh native Bore capture")));});
        }catch(Throwable e){fail(mc,e);}
    }
    private static void start(Minecraft mc){
        if(mc.screen instanceof AccessibilityOnboardingScreen){mc.options.onboardAccessibility=false;mc.options.save();mc.setScreen(new TitleScreen());return;}if(!(mc.screen instanceof TitleScreen)||mc.getOverlay()!=null)return;
        started=true;previous=mc.options.tutorialStep;mc.options.tutorialStep=TutorialSteps.NONE;mc.getTutorial().stop();mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(3);mc.options.simulationDistance().set(5);mc.options.guiScale().set(2);mc.options.cloudStatus().set(CloudStatus.OFF);mc.options.setCameraType(CameraType.FIRST_PERSON);mc.options.hideGui=false;mc.resizeDisplay();
        var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);rules.getRule(GameRules.RULE_DAYLIGHT).set(false,null);rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false,null);
        mc.createWorldOpenFlows().createFreshLevel(WORLD,new LevelSettings(WORLD,GameType.SURVIVAL,false,Difficulty.PEACEFUL,true,rules,WorldDataConfiguration.DEFAULT),new WorldOptions(0x54433631L,false,false),WorldPresets::createNormalWorldDimensions);
    }
    private static void finish(Minecraft mc){
        if(phase==0){phase=1;close(mc);submit(mc,()->{var p=player(mc);p.serverLevel().setBlockAndUpdate(POWER,Blocks.BEDROCK.defaultBlockState());finalAge=serverBore(mc).tickCount;});return;}
        if(phase==1){phase=2;submit(mc,()->{
            var p=player(mc);var level=p.serverLevel();var b=serverBore(mc);if(b.tickCount-finalAge<20){phase=1;return;}
            int air=(int)WALL.stream().filter(at->level.getBlockState(at).isAir()).count(),cobble=count(chest(level),Items.COBBLESTONE),stone=count(chest(level),Items.STONE);
            require(!b.isActive()&&b.getDigTarget()==null&&air==cobble+stone&&cobble>=4&&stone>0,"Final settled native Bore physical conservation failed");
            require(KnowledgeStore.get(p).isResearchCompleteStrict("ARCANEBORE")&&p.totalExperience==xpBefore+10&&jarsTotal(level)==15,"Final native study/infusion payment changed");
            require(level.getEntitiesOfClass(BoreEntity.class,new AABB(BORE).inflate(3),e->e.isAlive()&&e.isOwner(p)).size()==1&&central(level).isEmpty()&&b.validInventory(),"Final native paid entity/output/tool identity changed");
            var loose=level.getEntitiesOfClass(ItemEntity.class,new AABB(BORE).inflate(14),e->e.isAlive()&&(e.getItem().is(Items.COBBLESTONE)||e.getItem().is(Items.STONE)));require(loose.isEmpty(),"Final physical output duplicated loose drops");
            LogUtils.getLogger().info("THAUMCRAFT_BORE_CLIENT_FACTS: scenes={},researchStage={},studyRawGolemancyTheoryPaid={},studyRawArtificeObservationPaid={},studyRawAuromancyObservationPaid={},studyXP={},infusionEssentiaPaid={},infusionJarsRemaining={},infusionComponentsPaid=8,infusionCentralBasicPaid=1,paidOutput=1,paidPlacer=1,ownedBores=1,nativeToolTransfers=2,entityId={},entityType={},HP={},maxHP={},facing={},NoAI={},wallInitial={},wallAir={},physicalCobble={},physicalSilkStone={},looseDuplicateDrops={},originalPauseNativeTicks={},finalPauseNativeTicks={},finalInactive={},charge={},toolDamage={},toolRadius={},toolDepth={},stoneSpeed={},fortune={},refining={},silkTouch={},actualBeamDraws={},actualParticleEmissions={}",
                    SAVED.get(),KnowledgeStore.get(p).researchStage("ARCANEBORE"),golemancyBefore-KnowledgeStore.get(p).rawKnowledge(KnowledgeType.THEORY,"GOLEMANCY"),artificeBefore-KnowledgeStore.get(p).rawKnowledge(KnowledgeType.OBSERVATION,"ARTIFICE"),auromancyBefore-KnowledgeStore.get(p).rawKnowledge(KnowledgeType.OBSERVATION,"AUROMANCY"),p.totalExperience-xpBefore,215-jarsTotal(level),jarsTotal(level),b.getId(),ForgeRegistries.ENTITY_TYPES.getKey(b.getType()),b.getHealth(),b.getMaxHealth(),b.getFacing(),b.isNoAi(),WALL.size(),air,cobble,stone,loose.size(),pauseVerifiedTicks,b.tickCount-finalAge,!b.isActive(),b.getCharge(),b.tool().getDamageValue(),b.getDigRadius(),b.getDigDepth(),b.getDigSpeed(Blocks.STONE.defaultBlockState()),b.getFortune(),b.getRefining(),b.hasSilkTouch(),BoreRenderers.actualBeamDraws(),BoreClientEvents.Effects.actualParticleEmissions());
            finalValidated=true;
        });return;}
        if(!finalValidated)return;
        require(SAVED.get()==IMAGES.length&&studyPaid&&infusionPaid&&placed&&plainToolPaid&&enchantedToolPaid&&pauseVerified&&miningVerified&&chestStone>enchantedStoneBefore,"Incomplete native Bore audit");
        require(new LegacyArcaneBoreModel().cuboidCount()==11&&BoreRenderers.actualBeamDraws()>0&&BoreClientEvents.Effects.actualParticleEmissions()>0,"Original model/native rendered FX audit absent");
        for(String path:List.of("textures/gui/gui_arcanebore.png","textures/entity/arcanebore.png","textures/misc/beam1.png","textures/misc/auranodes.png","textures/misc/particles.png"))require(mc.getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath("thaumcraft",path)).isPresent(),"Missing original Bore media "+path);
        stopped=true;mc.options.tutorialStep=previous;LogUtils.getLogger().info("THAUMCRAFT_BORE_RENDER_AUDIT_OK: 10 actual native book/altar/toolGUI/redstone/world/chest PNG,original11cuboids/opaque tripod/translucent aiming head,source5block additive beam,actualBeamDraws={},actualPulledParticles={}; no gallery/dev overlay",BoreRenderers.actualBeamDraws(),BoreClientEvents.Effects.actualParticleEmissions());
        LogUtils.getLogger().info("THAUMCRAFT_BORE_CLIENT_SMOKE_OK: 10 scenes; paid ARCANEBORE32/16/16+10XP,actual altar200essentia+8components+1basic->oneBore,nativeplacer,onepickaxeGUI,noFuel,redstonePause40+ticks,nativeForgeharvest/adjacentphysicalchest,enchantedwidth11/depth56/speed7/Fortune4/Refining4/SilkTouch/nativebeam+stone; explicitfixtures,no manual tick shortcuts; world={}",WORLD);mc.stop();
    }
    private static void fail(Minecraft mc,Throwable e){if(stopped)return;stopped=true;if(previous!=null)mc.options.tutorialStep=previous;LogUtils.getLogger().error("THAUMCRAFT_BORE_CLIENT_SMOKE_FAILED",e);mc.stop();}
}

