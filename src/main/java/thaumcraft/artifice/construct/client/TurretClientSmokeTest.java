package thaumcraft.artifice.construct.client;

import com.mojang.logging.LogUtils;
import java.io.File;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
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
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
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
import thaumcraft.artifice.construct.AdvancedTurretEntity;
import thaumcraft.artifice.construct.TurretEntity;
import thaumcraft.artifice.construct.TurretMenu;
import thaumcraft.catalog.entities.client.LegacyCrossbowModel;
import thaumcraft.catalog.entities.client.ObjMesh;
import thaumcraft.client.research.ResearchClient;
import thaumcraft.client.research.ThaumonomiconPageScreen;
import thaumcraft.client.research.ThaumonomiconScreen;
import thaumcraft.research.KnowledgeStore;
import thaumcraft.research.KnowledgeType;
import thaumcraft.research.PlayerKnowledge;
import thaumcraft.research.ResearchCatalog;
import thaumcraft.research.ResearchModule;
import thaumcraft.research.ResearchNetwork;
import thaumcraft.world.aura.AuraManager;

/** Opt-in owned world: paid C2S book/bench, actual placed outputs and ordinary AI/menu packets. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public final class TurretClientSmokeTest {
    private static final String WORLD="thaumcraft-turret-smoke-"+System.currentTimeMillis();
    private static final String[] IMAGES={"basic-paid-research","basic-paid-recipe","basic-original-ammo-gui","basic-native-ai",
            "advanced-paid-research","advanced-paid-recipe","advanced-original-ammo-gui","advanced-target-settings",
            "advanced-native-ai","basic-native-dispenser-reload"};
    private static final BlockPos BENCH=new BlockPos(-6,112,0), BASIC=new BlockPos(0,112,0), ADVANCED=new BlockPos(36,112,0);
    private static final AtomicInteger SAVED=new AtomicInteger();
    private static final Set<UUID> OBSERVED_ARROWS=new HashSet<>();
    private static boolean started,prepared,stopped,captureRequested,captured,basicFireSeen,advancedFireSeen,loadingSeen;
    private static volatile boolean basicArrowSeen,advancedArrowSeen,basicHitSeen,advancedHitSeen,reloadArmed,reloadPaid;
    private static volatile int basicId,advancedId,basicTargetId,advancedTargetId;
    private static volatile int basicShots,advancedShots,reloadShotsBefore;
    private static int scene,phase,stable,wait,expectedStage=-1,placementReady,obsBefore,secondBefore,xpBefore,crafts;
    private static float auraBefore;
    private static long began;
    private static CompletableFuture<Void> work;
    private static TutorialSteps previous;
    private TurretClientSmokeTest() {}

    private static void require(boolean ok,String message) {if(!ok)throw new AssertionError(message);}
    private static boolean enabled() {return Boolean.getBoolean("thaumcraft.turretSmokeTest");}
    private static ServerPlayer player(Minecraft mc) {
        var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
        require(p!=null,"Missing owned turret player");return p;
    }
    private static void submit(Minecraft mc,Runnable task) {
        require(work==null,"Overlapping turret server tasks");var future=new CompletableFuture<Void>();work=future;
        mc.getSingleplayerServer().execute(()->{try{task.run();future.complete(null);}catch(Throwable e){future.completeExceptionally(e);}});
    }
    private static void stage(ServerPlayer p,String key) {
        try {var method=PlayerKnowledge.class.getDeclaredMethod("setResearchStage",String.class,int.class);method.setAccessible(true);
            require((boolean)method.invoke(KnowledgeStore.get(p),key,ResearchCatalog.get(key).stages().size()+1),"Predecessor stage fixture "+key);
        }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
    }
    private static void fact(ServerPlayer p,String key) {
        try {var method=PlayerKnowledge.class.getDeclaredMethod("discover",String.class);method.setAccessible(true);
            require((boolean)method.invoke(KnowledgeStore.get(p),key),"Preceding scan fact fixture "+key);
        }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
    }
    private static void knowledge(ServerPlayer p,KnowledgeType type,String category,int raw) {
        try {var method=PlayerKnowledge.class.getDeclaredMethod("addKnowledge",KnowledgeType.class,String.class,int.class);method.setAccessible(true);
            require((boolean)method.invoke(KnowledgeStore.get(p),type,category,raw),"Raw knowledge fixture");
        }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
    }
    private static ItemStack item(String id) {
        var item=ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("thaumcraft",id));
        require(item!=null&&item!=Items.AIR,"Missing native turret item "+id);return new ItemStack(item);
    }
    private static void held(ServerPlayer p,ItemStack stack) {
        p.getInventory().selected=0;p.setItemInHand(InteractionHand.MAIN_HAND,stack);
        p.connection.send(new ClientboundSetCarriedItemPacket(0));p.inventoryMenu.broadcastChanges();
    }
    private static void close(Minecraft mc) {mc.player.closeContainer();mc.setScreen(null);}
    private static void click(Minecraft mc,BlockPos pos) {
        mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(pos.getCenter(),Direction.UP,pos,false));
    }
    private static ArcaneWorkbenchBlockEntity bench(Minecraft mc) {return (ArcaneWorkbenchBlockEntity)player(mc).serverLevel().getBlockEntity(BENCH);}
    private static TurretEntity serverTurret(Minecraft mc,boolean advanced) {
        var entity=player(mc).serverLevel().getEntity(advanced?advancedId:basicId);
        require(entity instanceof TurretEntity&&entity.isAlive(),"Missing paid native turret");return (TurretEntity)entity;
    }
    private static TurretEntity clientTurret(Minecraft mc,boolean advanced) {
        var entity=mc.level.getEntity(advanced?advancedId:basicId);return entity instanceof TurretEntity turret?turret:null;
    }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||!enabled()||stopped)return;
        var mc=Minecraft.getInstance();if(began==0)began=System.nanoTime();
        try {
            require(System.nanoTime()-began<900_000_000_000L,"Turret timeout scene="+scene+" phase="+phase);
            if(!started){start(mc);return;}
            if(mc.level==null||mc.player==null||mc.getOverlay()!=null||mc.screen instanceof ReceivingLevelScreen)return;
            require(mc.getSingleplayerServer()!=null&&WORLD.equals(mc.getSingleplayerServer().getWorldData().getLevelName()),"Wrong turret world");
            mc.getToasts().clear();
            var basic=clientTurret(mc,false);var advanced=clientTurret(mc,true);
            if(basic!=null){basicFireSeen|=basic.getFireProgress(.5F)>0;loadingSeen|=basic.getLoadProgress(.5F)>0;}
            if(advanced!=null)advancedFireSeen|=advanced.getFireProgress(.5F)>0;
            if(++wait%100==0)LogUtils.getLogger().info("THAUMCRAFT_TURRET_CLIENT_SMOKE_HEARTBEAT: scene={} phase={} screen={} basicAmmo={} advancedAmmo={} basicArrow={} advancedArrow={} hit={}/{}",
                    scene,phase,mc.screen==null?"none":mc.screen.getClass().getSimpleName(),basic==null?-1:basic.ammo().getCount(),
                    advanced==null?-1:advanced.ammo().getCount(),basicArrowSeen,advancedArrowSeen,basicHitSeen,advancedHitSeen);
            if(work!=null){if(!work.isDone())return;work.join();work=null;}
            if(!prepared){prepared=true;submit(mc,()->prepare(mc));return;}
            if(captured){
                if(SAVED.get()==scene+1&&++stable>=20){scene++;phase=stable=wait=placementReady=0;expectedStage=-1;captured=captureRequested=false;}return;
            }
            if(scene==IMAGES.length){finish(mc);return;}
            if(!advance(mc)){stable=0;return;}
            if(++stable>=(scene==3||scene==8||scene==9?1:12))captureRequested=true;
        }catch(Throwable e){fail(mc,e);}
    }

    /** Read-only observation after actual native ticks; never invokes firing, AI or transfers. */
    @SubscribeEvent public static void serverTick(TickEvent.ServerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||!enabled()||stopped||!WORLD.equals(event.getServer().getWorldData().getLevelName()))return;
        ServerLevel level=event.getServer().overworld();
        for(var entity:level.getAllEntities())if(entity instanceof Arrow arrow&&arrow.getOwner()!=null) {
            if(arrow.getOwner().getId()==basicId){basicArrowSeen=true;if(OBSERVED_ARROWS.add(arrow.getUUID()))basicShots++;}
            if(arrow.getOwner().getId()==advancedId){advancedArrowSeen=true;if(OBSERVED_ARROWS.add(arrow.getUUID()))advancedShots++;}
        }
        if(level.getEntity(basicTargetId) instanceof LivingEntity target&&target.getHealth()<1000)basicHitSeen=true;
        if(level.getEntity(advancedTargetId) instanceof LivingEntity target&&target.getHealth()<1000)advancedHitSeen=true;
        if(reloadArmed&&level.getEntity(basicId) instanceof TurretEntity turret&&turret.ammo().getCount()+basicShots-reloadShotsBefore==64
                &&level.getBlockEntity(BASIC.below()) instanceof DispenserBlockEntity dispenser&&dispenser.isEmpty())reloadPaid=true;
    }

    private static boolean advance(Minecraft mc) {
        return switch(scene) {
            case 0->study(mc,false);
            case 1->firstRecipe(mc);
            case 2->placedMenu(mc,false);
            case 3->shooting(mc,false);
            case 4->study(mc,true);
            case 5->advancedRecipe(mc);
            case 6->placedMenu(mc,true);
            case 7->settings(mc);
            case 8->shooting(mc,true);
            case 9->reload(mc);
            default->false;
        };
    }
    private static boolean study(Minecraft mc,boolean advanced) {
        String key=advanced?"ADVANCEDTURRET":"BASICTURRET";
        KnowledgeType second=advanced?KnowledgeType.OBSERVATION:KnowledgeType.THEORY;
        int secondCost=advanced?16:32;
        if(phase==0){phase=1;close(mc);submit(mc,()->{
            var p=player(mc);knowledge(p,KnowledgeType.OBSERVATION,"ARTIFICE",16);knowledge(p,second,"GOLEMANCY",secondCost);
            var k=KnowledgeStore.get(p);obsBefore=k.rawKnowledge(KnowledgeType.OBSERVATION,"ARTIFICE");
            secondBefore=k.rawKnowledge(second,"GOLEMANCY");xpBefore=p.totalExperience;
            held(p,new ItemStack(ResearchModule.THAUMONOMICON.get()));ResearchNetwork.sync(p);
        });return false;}
        var k=ResearchClient.golemPressKnowledge();
        if(phase==1) {
            if(!mc.player.getMainHandItem().is(ResearchModule.THAUMONOMICON.get()))return false;
            if(!k.isResearchCompleteStrict(key)){
                int current=k.researchStage(key);if(current==expectedStage)return false;
                expectedStage=current;ResearchNetwork.requestAdvance(key,current);return false;
            }
            phase=2;submit(mc,()->{
                var p=player(mc);var state=KnowledgeStore.get(p);
                require(state.isResearchCompleteStrict(key)&&state.rawKnowledge(KnowledgeType.OBSERVATION,"ARTIFICE")==obsBefore-16
                        &&state.rawKnowledge(second,"GOLEMANCY")==secondBefore-secondCost&&p.totalExperience==xpBefore+10,"Actual turret C2S study payment/XP mismatch "+key);
                LogUtils.getLogger().info("THAUMCRAFT_TURRET_CLIENT_SMOKE_RESEARCH_PAID: {} real C2S,16ArtificeObs+{}Golemancy{},10XP(start5+completion5); only predecessor/fact/raw balances are fixtures",key,secondCost,second);
            });return false;
        }
        if(!(mc.screen instanceof ThaumonomiconPageScreen)) {
            var book=new ThaumonomiconScreen(k,k.scanCount());mc.setScreen(new ThaumonomiconPageScreen(book,ResearchCatalog.get(key),k,k.scanCount()));
        }
        return true;
    }
    private static boolean benchReady(Minecraft mc) {
        boolean ready=mc.player.getMainHandItem().isEmpty()&&mc.player.distanceToSqr(-5.5,112,2.5)<.25;
        if(!ready){placementReady=0;return false;}return ++placementReady>=3;
    }
    private static void benchPosition(Minecraft mc) {
        var p=player(mc);held(p,ItemStack.EMPTY);p.connection.teleport(-5.5,112,2.5,180,20);
    }
    private static boolean payableMenu(Minecraft mc,String output,int vis,int aer) {
        if(!(mc.player.containerMenu instanceof ArcaneWorkbenchMenu menu)||!menu.getSlot(0).getItem().is(item(output).getItem())||!menu.craftable())return false;
        require(menu.requiredVis()==vis&&menu.crystalCost(0)==aer,"Native original arcane cost "+output);
        for(int primal=1;primal<6;primal++)require(menu.crystalCost(primal)==0,"Unexpected extra crystal cost "+output);
        return true;
    }
    private static boolean firstRecipe(Minecraft mc) {
        if(phase==0){phase=1;close(mc);submit(mc,()->{benchPosition(mc);prepareArcane(mc,false);});return false;}
        if(phase==1){if(!benchReady(mc))return false;phase=2;click(mc,BENCH);return false;}
        return payableMenu(mc,"turret_basic",100,1);
    }
    private static boolean advancedRecipe(Minecraft mc) {
        // The advanced recipe consumes a second genuinely crafted basic turret.
        // The earlier basic stays alive; neither basic input nor advanced output is supplied.
        if(phase==0){phase=1;close(mc);submit(mc,()->{benchPosition(mc);prepareArcane(mc,false);});return false;}
        if(phase==1){if(!benchReady(mc))return false;phase=2;click(mc,BENCH);return false;}
        if(phase==2){
            if(!payableMenu(mc,"turret_basic",100,1))return false;phase=3;
            mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId,0,3,ClickType.SWAP,mc.player);return false;
        }
        if(phase==3){
            if(!mc.player.getInventory().getItem(3).is(item("turret_basic").getItem()))return false;
            phase=4;placementReady=0;close(mc);submit(mc,()->{assertCraft(mc,false,3);benchPosition(mc);prepareArcane(mc,true);});return false;
        }
        if(phase==4){if(!benchReady(mc))return false;phase=5;click(mc,BENCH);return false;}
        return payableMenu(mc,"turret_advanced",150,2);
    }
    private static void assertCraft(Minecraft mc,boolean advanced,int slot) {
        var p=player(mc);String id=advanced?"turret_advanced":"turret_basic";int vis=advanced?150:100;
        require(bench(mc).isEmpty()&&AuraManager.getVis(p.serverLevel(),BENCH)==auraBefore-vis,"Native bench exact component/crystal/vis payment "+id);
        require(p.getInventory().getItem(slot).is(item(id).getItem())&&p.getInventory().getItem(slot).getCount()==1,"Actual paid single output "+id);
        crafts++;LogUtils.getLogger().info("THAUMCRAFT_TURRET_CLIENT_SMOKE_CRAFT_PAID: {} actual result C2S,{}vis/Aer{},one output,all original grid inputs spent; craft={}",id,vis,advanced?2:1,crafts);
    }
    private static boolean placedMenu(Minecraft mc,boolean advanced) {
        int slot=advanced?4:2;String id=advanced?"turret_advanced":"turret_basic";BlockPos pos=advanced?ADVANCED:BASIC;
        if(phase==0){phase=1;mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId,0,slot,ClickType.SWAP,mc.player);return false;}
        if(phase==1){
            if(!mc.player.getInventory().getItem(slot).is(item(id).getItem()))return false;
            phase=2;close(mc);submit(mc,()->{
                assertCraft(mc,advanced,slot);var p=player(mc);p.getInventory().selected=slot;
                p.connection.teleport(pos.getX()+.5,112,2.5,180,80);p.inventoryMenu.broadcastChanges();
            });return false;
        }
        if(phase==2){
            mc.player.getInventory().selected=slot;
            boolean ready=mc.player.getMainHandItem().is(item(id).getItem())&&mc.player.distanceToSqr(pos.getX()+.5,112,2.5)<.25;
            if(!ready){placementReady=0;return false;}if(++placementReady<3)return false;
            phase=3;mc.player.connection.send(new ServerboundSetCarriedItemPacket(slot));click(mc,pos.below());return false;
        }
        if(phase==3){
            if(!mc.player.getInventory().getItem(slot).isEmpty())return false;
            phase=4;submit(mc,()->{
                var p=player(mc);var list=p.serverLevel().getEntitiesOfClass(TurretEntity.class,new AABB(pos).inflate(2),
                        t->t.isAdvanced()==advanced&&t.isOwner(p)&&t.hasValidSpawn());
                require(list.size()==1,"Actual placer did not create one owned native turret");
                var turret=list.get(0);require(!turret.isNoAi()&&turret.getHealth()==turret.getMaxHealth(),"Placed turret inactive/wrong health");
                if(advanced)advancedId=turret.getId();else basicId=turret.getId();
                held(p,ItemStack.EMPTY);p.getInventory().setItem(9,new ItemStack(Items.ARROW,64));
                p.connection.teleport(pos.getX()+2.5,112,2.5,135,15);p.inventoryMenu.broadcastChanges();
                LogUtils.getLogger().info("THAUMCRAFT_TURRET_CLIENT_SMOKE_PLACED: {} actual paid placer C2S consumed,single owned valid native entity id={},NoAI=false; raw64arrow ingredient deferred to real GUI",id,turret.getId());
            });return false;
        }
        var turret=clientTurret(mc,advanced);if(turret==null)return false;
        if(phase==4){
            if(!mc.player.getMainHandItem().isEmpty()||mc.player.distanceToSqr(pos.getX()+2.5,112,2.5)>.25)return false;
            phase=5;mc.gameMode.interact(mc.player,turret,InteractionHand.MAIN_HAND);return false;
        }
        if(!(mc.screen instanceof TurretScreen)||!(mc.player.containerMenu instanceof TurretMenu menu)||menu.turret()!=turret)return false;
        require(menu.advanced()==advanced,"Wrong native turret menu");
        if(phase==5){
            if(!menu.getSlot(1).getItem().is(Items.ARROW)||menu.getSlot(1).getItem().getCount()!=64)return false;
            phase=6;mc.gameMode.handleInventoryMouseClick(menu.containerId,1,0,ClickType.QUICK_MOVE,mc.player);return false;
        }
        if(menu.getSlot(0).getItem().getCount()!=64||!menu.getSlot(1).getItem().isEmpty())return false;
        if(phase==6){phase=7;submit(mc,()->{
            var server=serverTurret(mc,advanced);var p=player(mc);
            require(p.containerMenu instanceof TurretMenu&&server.ammo().is(Items.ARROW)&&server.ammo().getCount()==64&&p.getInventory().getItem(9).isEmpty(),"Real menu did not conserve arrows64->64");
            if(advanced)require(((AdvancedTurretEntity)server).getTargetFlags()==2,"Original advanced mob-only defaults");
            LogUtils.getLogger().info("THAUMCRAFT_TURRET_CLIENT_SMOKE_AMMO_PAID: {} actual owner interaction/native menu/source slot1 C2S quickMove,64arrows removed from player and inserted in real mainhand slot",id);
        });return false;}
        return true;
    }
    private static boolean settings(Minecraft mc) {
        if(!(mc.screen instanceof TurretScreen screen)||!(mc.player.containerMenu instanceof TurretMenu menu))return false;
        // Exercise all four native C2S buttons, including the friendly/player round trips.
        int[] actions={1,2,3,4,4,3};int[] expected={3,1,5,13,5,1};
        if(phase<actions.length) {
            int current=phase==0?2:expected[phase-1];
            if(menu.targetFlags()!=current)return false;
            screen.toggleTarget(actions[phase]);phase++;return false;
        }
        if(menu.targetFlags()!=1)return false;
        if(phase==actions.length){phase++;submit(mc,()->{
            var advanced=(AdvancedTurretEntity)serverTurret(mc,true);
            require(advanced.getTargetAnimal()&&!advanced.getTargetMob()&&!advanced.getTargetPlayer()&&!advanced.getTargetFriendly(),"Native window-bound target toggles did not synchronize");
            LogUtils.getLogger().info("THAUMCRAFT_TURRET_CLIENT_SMOKE_FLAGS_PAID: actual original menu C2S buttons1/2/3/4/4/3,states2->3->1->5->13->5->1; server final animals=true,mobs/players/friendly=false");
        });return false;}
        return true;
    }
    private static void target(Minecraft mc,boolean advanced) {
        var p=player(mc);var level=p.serverLevel();BlockPos pos=advanced?ADVANCED:BASIC;
        Mob target=(advanced?EntityType.COW:EntityType.HUSK).create(level);
        require(target!=null,"Missing native target fixture");
        target.moveTo(pos.getX()+.5,112,-8.5,0,0);target.setNoAi(true);target.setPersistenceRequired();
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);target.setHealth(1000);
        require(level.addFreshEntity(target),"Target fixture rejected");
        if(advanced)advancedTargetId=target.getId();else basicTargetId=target.getId();
        held(p,ItemStack.EMPTY);p.connection.teleport(pos.getX()+4.5,112,3.5,125,8);
        LogUtils.getLogger().info("THAUMCRAFT_TURRET_CLIENT_SMOKE_TARGET_FIXTURE: {} actual{} at8blocks,NoAI/high1000HP only on target; paid turret native AI remains enabled, no setTarget/fire/tick shortcuts",advanced?"advanced":"basic",advanced?"Cow":"Husk");
    }
    private static boolean shooting(Minecraft mc,boolean advanced) {
        if(phase==0){phase=1;close(mc);submit(mc,()->target(mc,advanced));return false;}
        var turret=clientTurret(mc,advanced);BlockPos pos=advanced?ADVANCED:BASIC;
        if(turret==null||mc.screen!=null||mc.player.distanceToSqr(pos.getX()+4.5,112,3.5)>.25)return false;
        if(!(advanced?advancedArrowSeen&&advancedHitSeen:basicArrowSeen&&basicHitSeen)||turret.ammo().getCount()>=64)return false;
        if(phase==1){
            Object renderer=mc.getEntityRenderDispatcher().getRenderer(turret);
            require(advanced?renderer instanceof TurretRenderers.Advanced:renderer instanceof TurretRenderers.Basic,"Native turret renderer dispatch absent");
            if(advanced) {
                var mesh=ObjMesh.get("crossbow_advanced");
                require(mesh.faceCount()>0&&mesh.groups().equals(Set.of("legs","mech","box","shield","brain","loader","bow1","bow2")),"Original eight-group advanced OBJ unavailable");
                LogUtils.getLogger().info("THAUMCRAFT_TURRET_RENDER_AUDIT_MESH: native advanced renderer,original OBJ groups={},faces={}",mesh.groups(),mesh.faceCount());
            }else require(new LegacyCrossbowModel().cuboidCount()==20,"Original ModelCrossbow20cuboids");
            phase=2;submit(mc,()->{
            var server=serverTurret(mc,advanced);var level=player(mc).serverLevel();
            var victim=level.getEntity(advanced?advancedTargetId:basicTargetId);
            require(victim instanceof LivingEntity&&((LivingEntity)victim).getHealth()<1000&&server.ammo().getCount()<64&&server.getTarget()==victim,"Ordinary native turret AI failed acquire/fire/pay/hit");
            require(advanced?advancedArrowSeen:basicArrowSeen,"No actual owned Arrow observed");
            LogUtils.getLogger().info("THAUMCRAFT_TURRET_CLIENT_SMOKE_AI_PAID: {} native acquire/ranged goal,physical Arrow owner confirmed,ammo64->{},victim1000->{},targetMatches=true",advanced?"advanced":"basic",server.ammo().getCount(),((LivingEntity)victim).getHealth());
        });return false;}
        if(turret.getFireProgress(.5F)<=0)return false;
        for(var entity:mc.level.entitiesForRendering())if(entity instanceof Arrow arrow&&arrow.getOwner()==turret&&arrow.getDeltaMovement().lengthSqr()>.05)return true;
        return false;
    }
    private static boolean reload(Minecraft mc) {
        if(phase==0){phase=1;close(mc);submit(mc,()->{
            var p=player(mc);held(p,ItemStack.EMPTY);p.connection.teleport(2.5,112,2.5,135,15);
        });return false;}
        var basic=clientTurret(mc,false);if(basic==null)return false;
        if(phase==1) {
            if(mc.player.distanceToSqr(2.5,112,2.5)>.25||!mc.player.getMainHandItem().isEmpty())return false;
            phase=2;mc.gameMode.interact(mc.player,basic,InteractionHand.MAIN_HAND);return false;
        }
        if(phase==2){
            if(!(mc.player.containerMenu instanceof TurretMenu menu)||menu.turret()!=basic)return false;
            phase=3;if(!menu.getSlot(0).getItem().isEmpty())mc.gameMode.handleInventoryMouseClick(menu.containerId,0,0,ClickType.QUICK_MOVE,mc.player);return false;
        }
        if(phase==3) {
            if(!(mc.player.containerMenu instanceof TurretMenu menu)||!menu.getSlot(0).getItem().isEmpty())return false;
            phase=4;close(mc);submit(mc,()->{
                require(serverTurret(mc,false).ammo().isEmpty(),"Actual GUI withdrawal left ammunition");
                var p=player(mc);var level=p.serverLevel();
                level.setBlockAndUpdate(BASIC.below(),Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING,Direction.UP));
                var dispenser=(DispenserBlockEntity)level.getBlockEntity(BASIC.below());
                dispenser.setItem(0,new ItemStack(Items.ARROW,64));reloadShotsBefore=basicShots;reloadArmed=true;
                p.connection.teleport(4.5,112,3.5,125,8);
                LogUtils.getLogger().info("THAUMCRAFT_TURRET_CLIENT_SMOKE_RELOAD_FIXTURE: existing paid basic emptied through real GUI,explicit vanilla UP dispenser/raw64arrows below; native baseTick alone must consume source and broadcast10tick loading event");
            });return false;
        }
        if(!reloadPaid||basic.getLoadProgress(.5F)<=0||basic.getLoadProgress(.5F)>=1||mc.screen!=null)return false;
        require(mc.player.distanceToSqr(4.5,112,3.5)<.25,"Loading frame camera not synchronized");
        return true;
    }
    private static void prepareArcane(Minecraft mc,boolean advanced) {
        var p=player(mc);var level=p.serverLevel();String output=advanced?"turret_advanced":"turret_basic";
        require(bench(mc).isEmpty(),"Previous paid bench retained inputs");
        ArcaneRecipe recipe=level.getRecipeManager().getAllRecipesFor(ArcaneModule.RECIPE_TYPE.get()).stream()
                .filter(r->r.getResultItem(level.registryAccess()).is(item(output).getItem())).findFirst().orElseThrow();
        require(recipe.gridWidth()==3&&recipe.gridHeight()==(advanced?2:3),"Original turret grid shape");
        boolean usedPaidBasic=false;
        for(int index=0;index<recipe.getIngredients().size();index++) {
            var ingredient=recipe.getIngredients().get(index);if(ingredient.isEmpty())continue;
            require(ingredient.getItems().length>0,"Empty native input tag");
            ItemStack input;
            if(advanced&&ingredient.test(item("turret_basic"))) {
                var paid=p.getInventory().getItem(3);require(paid.is(item("turret_basic").getItem())&&paid.getCount()==1&&!usedPaidBasic,"Advanced input not the second actual paid basic");
                input=paid.split(1);p.getInventory().setItem(3,ItemStack.EMPTY);usedPaidBasic=true;
            }else input=ingredient.getItems()[0].copyWithCount(1);
            bench(mc).setItem(index%recipe.gridWidth()+index/recipe.gridWidth()*3,input);
        }
        require(!advanced||usedPaidBasic,"Advanced recipe did not include paid basic");
        for(int primal=0;primal<6;primal++)if(recipe.crystalCost(primal)>0)
            bench(mc).setItem(9+primal,AspectCrystalItem.create(Aspect.getAspect(ArcaneModule.PRIMALS[primal]),recipe.crystalCost(primal)));
        level.setDayTime(6000);var chunk=new ChunkPos(BENCH);
        for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++) {
            var anchor=new ChunkPos(chunk.x+dx,chunk.z+dz).getMiddleBlockPosition(112);
            AuraManager.drainVis(level,anchor,Float.MAX_VALUE,false);AuraManager.addVis(level,anchor,250);
            AuraManager.drainFlux(level,anchor,Float.MAX_VALUE,false);AuraManager.addFlux(level,anchor,1000);
        }
        auraBefore=AuraManager.getVis(level,BENCH);p.inventoryMenu.broadcastChanges();
        LogUtils.getLogger().info("THAUMCRAFT_TURRET_CLIENT_SMOKE_ARCANE_FIXTURE: {} raw original grid ingredients/crystals,aura250/fullmoon/highflux1000 neighbours isolate debit; finished outputs absent; paidBasicInput={}",output,usedPaidBasic);
    }
    private static void prepare(Minecraft mc) {
        var p=player(mc);var level=p.serverLevel();p.setInvulnerable(true);p.connection.teleport(-5.5,112,2.5,180,20);
        for(int x=-2;x<=4;x++)for(int z=-2;z<=2;z++)level.getChunk(x,z);
        for(var pos:BlockPos.betweenClosed(-10,111,-14,50,118,12))
            level.setBlockAndUpdate(pos,pos.getY()==111?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(BENCH,ArcaneModule.WORKBENCH.get().defaultBlockState());
        p.getInventory().clearContent();
        for(String key:List.of("FIRSTSTEPS","KNOWLEDGETYPES","UNLOCKARTIFICE","BASEARTIFICE","UNLOCKGOLEMANCY","BASEGOLEMANCY","MINDCLOCKWORK","ARCANEEAR","MINDBIOTHAUMIC"))stage(p,key);
        fact(p,"f_DISPENSER");p.inventoryMenu.broadcastChanges();ResearchNetwork.sync(p);
        require(!KnowledgeStore.get(p).isResearchKnown("BASICTURRET")&&!KnowledgeStore.get(p).isResearchKnown("ADVANCEDTURRET"),"Fixture completed target research");
        LogUtils.getLogger().info("THAUMCRAFT_TURRET_CLIENT_SMOKE_FIXTURE: owned isolated survival world,arena/loadedchunks/bench/preceding research/f_DISPENSER are explicit fixtures; rawknowledge/ingredients/aura/targets deferred; no target research or finished turrets supplied");
    }
    @SubscribeEvent public static void rendered(TickEvent.RenderTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||!enabled()||stopped||!captureRequested||captured)return;
        var mc=Minecraft.getInstance();
        try {
            captured=true;captureRequested=false;String name="tc6-turret-"+IMAGES[scene]+".png";
            var file=new File(new File(mc.gameDirectory,"screenshots"),name);Files.deleteIfExists(file.toPath());
            Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),message->{
                if(file.isFile()&&file.length()>0){SAVED.incrementAndGet();LogUtils.getLogger().info("THAUMCRAFT_TURRET_CLIENT_SMOKE_IMAGE: {}",file.getAbsolutePath());}
                else mc.execute(()->fail(mc,new AssertionError("Missing fresh turret capture")));
            });
        }catch(Throwable e){fail(mc,e);}
    }
    private static void start(Minecraft mc) {
        if(mc.screen instanceof AccessibilityOnboardingScreen){mc.options.onboardAccessibility=false;mc.options.save();mc.setScreen(new TitleScreen());return;}
        if(!(mc.screen instanceof TitleScreen)||mc.getOverlay()!=null)return;
        started=true;previous=mc.options.tutorialStep;mc.options.tutorialStep=TutorialSteps.NONE;mc.getTutorial().stop();mc.options.pauseOnLostFocus=false;
        mc.options.renderDistance().set(3);mc.options.simulationDistance().set(5);mc.options.guiScale().set(2);mc.options.cloudStatus().set(CloudStatus.OFF);
        mc.options.setCameraType(CameraType.FIRST_PERSON);mc.options.hideGui=false;mc.resizeDisplay();
        var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);rules.getRule(GameRules.RULE_DAYLIGHT).set(false,null);rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false,null);
        mc.createWorldOpenFlows().createFreshLevel(WORLD,new LevelSettings(WORLD,GameType.SURVIVAL,false,Difficulty.NORMAL,true,rules,WorldDataConfiguration.DEFAULT),
                new WorldOptions(0x54433630L,false,false),WorldPresets::createNormalWorldDimensions);
    }
    private static void finish(Minecraft mc) {
        require(SAVED.get()==IMAGES.length&&crafts==3&&basicArrowSeen&&advancedArrowSeen&&basicHitSeen&&advancedHitSeen&&reloadPaid,"Incomplete actual turret audit");
        require(basicFireSeen&&advancedFireSeen&&loadingSeen,"Native six-tick firing/ten-tick loading events were not observed on client");
        stopped=true;mc.options.tutorialStep=previous;
        LogUtils.getLogger().info("THAUMCRAFT_TURRET_RENDER_AUDIT_OK: 10 actual book/bench/original GUI/native world PNG; native firing and dispenser loading events observed; ModelCrossbow/original8groupOBJ/media; no gallery/dev overlay");
        LogUtils.getLogger().info("THAUMCRAFT_TURRET_CLIENT_SMOKE_OK: 10 captures; paid BASICTURRET/ADVANCEDTURRET C2S,3 native crafts100+100+150vis/Aer1+1+2,paid second basic consumed by advanced,two actual placers/menu64arrows/nativeAIownedArrows/hits,all4advancedbuttonC2S,UPdispenser64->mainhand64; predecessor/raw inputs/aura/targets explicitfixtures; world={}",WORLD);mc.stop();
    }
    private static void fail(Minecraft mc,Throwable error) {
        if(stopped)return;stopped=true;if(previous!=null)mc.options.tutorialStep=previous;
        LogUtils.getLogger().error("THAUMCRAFT_TURRET_CLIENT_SMOKE_FAILED",error);mc.stop();
    }
}
