package thaumcraft.world.taint.client;

import com.mojang.logging.LogUtils;
import java.io.File;
import java.nio.file.Files;
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
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thaumcraft.catalog.blocks.CatalogBlocks;
import thaumcraft.catalog.entities.VisualEntitiesModule;
import thaumcraft.scanning.ScanningModule;
import thaumcraft.world.aura.AuraManager;
import thaumcraft.world.rift.FluxRiftEntity;
import thaumcraft.world.taint.TaintHelper;
import thaumcraft.world.taint.TaintSeedEntity;

/** Opt-in native visuals and tick lifecycle in a new owned world on the hidden desktop. */
@Mod.EventBusSubscriber(modid="thaumcraft", value=Dist.CLIENT)
public final class TaintClientSmokeTest {
    private static final String WORLD="thaumcraft-taint-smoke-"+System.currentTimeMillis();
    private static final String[] IMAGES={"prime-seed","native-fibres","tainted-materials","tiny-tentacles","seed-loot","decayed-fibres"};
    private static final BlockPos ARENA=new BlockPos(8,160,8);
    private static final AtomicInteger SAVED=new AtomicInteger();
    private static MinecraftServer ownedServer;
    private static CompletableFuture<Void> work;
    private static TutorialSteps previous;
    private static boolean started,prepared,stopped,captured,captureRequested;
    private static int scene,phase,stable,wait,seedId,initialFibres,remainingFibres,tentacles,loot;
    private static long began;
    private static volatile BlockPos seedPos;
    private TaintClientSmokeTest() {}
    private static void require(boolean ok,String message) {if(!ok)throw new AssertionError(message);}
    private static ServerPlayer player(Minecraft mc) {
        var p=ownedServer.getPlayerList().getPlayer(mc.player.getUUID());require(p!=null,"Missing owned taint QA player");return p;
    }
    private static TaintSeedEntity seed() {
        var entity=ownedServer.overworld().getEntity(seedId);require(entity instanceof TaintSeedEntity,"Native seed missing");
        return (TaintSeedEntity)entity;
    }
    private static void submit(Minecraft mc,Runnable action) {
        require(work==null,"Overlapping server work");var result=new CompletableFuture<Void>();work=result;
        ownedServer.execute(()->{try{action.run();result.complete(null);}catch(Throwable error){result.completeExceptionally(error);}});
    }
    private static int fibres() {
        int count=0;var level=ownedServer.overworld();
        for(BlockPos pos:BlockPos.betweenClosed(-16,160,-16,32,164,32))
            if(level.getBlockState(pos).is(CatalogBlocks.block("taint_fibre")))count++;
        return count;
    }
    private static void camera(ServerPlayer p,BlockPos pos,boolean above) {
        if(above)p.connection.teleport(pos.getX()+.5,pos.getY()+7,pos.getZ()-8,0,38);
        else p.connection.teleport(pos.getX()+.5,pos.getY(),pos.getZ()-6,0,6);
    }
    private static void prepare(Minecraft mc) throws Exception {
        var p=player(mc);var level=p.serverLevel();level.setDayTime(6000);
        for(int x=-2;x<=3;x++)for(int z=-2;z<=3;z++)level.getChunk(x,z);
        for(int x=-16;x<=32;x++)for(int z=-16;z<=32;z++) {
            level.setBlockAndUpdate(new BlockPos(x,159,z),Blocks.BEDROCK.defaultBlockState());
            level.setBlockAndUpdate(new BlockPos(x,160,z),Blocks.DIRT.defaultBlockState());
            for(int y=161;y<=179;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
        }
        p.getInventory().clearContent();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK));
        p.getAbilities().invulnerable=true;p.getAbilities().mayfly=true;p.getAbilities().flying=true;p.onUpdateAbilities();
        AuraManager.drainFlux(level,ARENA,Float.MAX_VALUE,false);AuraManager.addFlux(level,ARENA,30);
        var rift=(FluxRiftEntity)VisualEntitiesModule.EFFECTS.get("flux_rift").get().create(level);
        // This supplied source/randomness lands the Prime on the platform, avoiding a falling-source refresh fixture.
        rift.setPos(8.5,153,8.5);rift.setRiftSeed(723);rift.setRiftSize(25);rift.setRiftStability(-20);rift.getRandom().setSeed(92173);
        level.addFreshEntity(rift);
        var method=thaumcraft.world.rift.RiftEvents.class.getDeclaredMethod("execute",FluxRiftEntity.class,int.class);method.setAccessible(true);
        Object outcome=method.invoke(null,rift,1);
        require(outcome.toString().equals("APPLIED") && rift.isRemoved(),"Native supplied Prime event failed");
        var seeds=level.getEntitiesOfClass(TaintSeedEntity.class,new AABB(-16,160,-16,32,180,32));
        require(seeds.size()==1 && seeds.get(0).getBoost()==25 && !seeds.get(0).isNoAi(),"Prime event produced wrong seed/boost");
        var seed=seeds.get(0);seedId=seed.getId();seedPos=seed.blockPosition();
        require(AuraManager.getFlux(level,ARENA)==42,"Prime event did not pollute exact integer25/2");
        camera(p,seedPos,false);
        LogUtils.getLogger().info("THAUMCRAFT_TAINT_PRIME_EVENT_OK: supplied event1; real native Prime, boost25, flux30->42, rift removed");
        LogUtils.getLogger().info("THAUMCRAFT_TAINT_FIXTURES: owned new world, loaded dirt/bedrock platform, survival invulnerability/flying, selected original event1; later boost400, native material display, selected target, randomTickSpeed100; not full survival or random event probability proof");
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END || !Boolean.getBoolean("thaumcraft.taintSmokeTest") || stopped)return;
        var mc=Minecraft.getInstance();if(began==0)began=System.nanoTime();
        try {
            require(System.nanoTime()-began<600_000_000_000L,"Taint client timeout scene="+scene+" phase="+phase);
            if(!started){start(mc);return;}
            if(mc.level==null || mc.player==null || mc.getOverlay()!=null || mc.screen instanceof ReceivingLevelScreen)return;
            require(mc.getSingleplayerServer()!=null && WORLD.equals(mc.getSingleplayerServer().getWorldData().getLevelName()),"Wrong owned world");
            if(ownedServer==null)ownedServer=mc.getSingleplayerServer();mc.getToasts().clear();
            if(work!=null){if(!work.isDone())return;work.join();work=null;}
            if(!prepared){prepared=true;submit(mc,()->{try{prepare(mc);}catch(Exception e){throw new IllegalStateException(e);}});return;}
            if(captured){if(SAVED.get()==scene+1 && ++stable>=12){scene++;phase=stable=wait=0;captured=captureRequested=false;}return;}
            if(scene==IMAGES.length){finish(mc);return;}
            if(!advance(mc))return;
            if(++stable>=12)captureRequested=true;
        }catch(Throwable error){fail(mc,error);}
    }
    private static boolean advance(Minecraft mc) {
        if(scene==0) {
            if(phase==0){phase=1;submit(mc,()->{seedPos=seed().blockPosition();camera(player(mc),seedPos,false);});return false;}
            return ++wait>=35;
        }
        if(scene==1) {
            if(phase==0){phase=1;submit(mc,()->seed().setBoost(400));return false;}
            if(phase==1){if(++wait<160)return false;phase=2;submit(mc,()->{
                initialFibres=fibres();require(initialFibres>0,"Native seed ticks did not spread fibres");
                seedPos=seed().blockPosition();camera(player(mc),seedPos,true);
                LogUtils.getLogger().info("THAUMCRAFT_TAINT_NATIVE_SPREAD_OK: {} real fibres after native entity ticks; supplied boost400",initialFibres);
            });return false;}return true;
        }
        if(scene==2) {
            if(phase==0){phase=1;submit(mc,()->{
                var level=ownedServer.overworld();String[] ids={"taint_soil","taint_rock","taint_crust","taint_log","taint_feature"};
                for(int i=0;i<ids.length;i++) {
                    var state=CatalogBlocks.block(ids[i]).defaultBlockState();
                    if(ids[i].equals("taint_feature"))state=state.setValue(thaumcraft.world.taint.TaintFeatureBlock.FACING,net.minecraft.core.Direction.UP);
                    level.setBlockAndUpdate(seedPos.offset(i-2,0,-3),state);
                }
                player(mc).setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ScanningModule.THAUMOMETER.get()));
                player(mc).inventoryMenu.broadcastChanges();camera(player(mc),seedPos,false);
            });return false;}return true;
        }
        if(scene==3) {
            if(phase==0){phase=1;submit(mc,()->{
                var p=player(mc);var pos=seedPos.offset(3,0,0);ownedServer.overworld().setBlockAndUpdate(pos.below(),CatalogBlocks.block("taint_soil").defaultBlockState());
                p.connection.teleport(pos.getX()+.5,pos.getY(),pos.getZ()+.5,90,12);seed().setTarget(p);
            });return false;}
            if(phase==1){if(++wait<45)return false;phase=2;submit(mc,()->{
                tentacles=ownedServer.overworld().getEntitiesOfClass(thaumcraft.catalog.entities.VisualMobEntity.class,
                        new AABB(seedPos).inflate(12),e->e.spec().id().equals("taintacle_tiny") && !e.isNoAi()).size();
                require(tentacles>0,"Seed tick did not spawn operational tiny tentacle on taint");
                camera(player(mc),seedPos,true);
                LogUtils.getLogger().info("THAUMCRAFT_TAINT_TENTACLES_OK: {} native small tentacles from20tick seed callback; supplied survival target on tainted soil",tentacles);
            });return false;}return true;
        }
        if(scene==4) {
            if(phase==0){phase=1;submit(mc,()->{
                var p=player(mc);seed().hurt(p.damageSources().playerAttack(p),10000);
                require(!TaintHelper.isNearTaintSeed(p.serverLevel(),seedPos),"Dead seed area remains active");
                camera(p,seedPos,true);
            });return false;}
            if(phase==1){if(++wait<25)return false;phase=2;submit(mc,()->{
                loot=ownedServer.overworld().getEntitiesOfClass(ItemEntity.class,new AABB(seedPos).inflate(6),
                        e->e.getItem().getItem() instanceof thaumcraft.alchemy.AspectCrystalItem).size();
                require(loot>0,"Prime native death did not drop Flux crystals");
                ownedServer.overworld().getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(100,ownedServer);
                LogUtils.getLogger().info("THAUMCRAFT_TAINT_DEATH_OK: seed area removed; {} physical crystal drops",loot);
            });return false;}return true;
        }
        if(scene==5) {
            if(phase==0){if(++wait<120)return false;phase=1;submit(mc,()->{
                remainingFibres=fibres();require(remainingFibres<initialFibres,"Orphan fibres did not decay from native random ticks");
                camera(player(mc),seedPos,true);
                LogUtils.getLogger().info("THAUMCRAFT_TAINT_DECAY_OK: real fibres{}->{} after seed death/native random ticks; supplied randomTickSpeed100",initialFibres,remainingFibres);
            });return false;}return true;
        }
        return false;
    }
    @SubscribeEvent public static void rendered(TickEvent.RenderTickEvent event) {
        if(event.phase!=TickEvent.Phase.END || !Boolean.getBoolean("thaumcraft.taintSmokeTest") || stopped || !captureRequested || captured)return;
        var mc=Minecraft.getInstance();
        try {
            captured=true;captureRequested=false;String name="tc6-taint-"+IMAGES[scene]+".png";
            File file=new File(new File(mc.gameDirectory,"screenshots"),name);Files.deleteIfExists(file.toPath());
            Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),message->{
                if(file.isFile() && file.length()>0){SAVED.incrementAndGet();LogUtils.getLogger().info("THAUMCRAFT_TAINT_SMOKE_IMAGE: {}",file.getAbsolutePath());}
                else mc.execute(()->fail(mc,new AssertionError("Missing current capture")));
            });
        }catch(Throwable e){fail(mc,e);}
    }
    private static void start(Minecraft mc) {
        if(mc.screen instanceof AccessibilityOnboardingScreen){mc.options.onboardAccessibility=false;mc.options.save();mc.setScreen(new TitleScreen());return;}
        if(!(mc.screen instanceof TitleScreen) || mc.getOverlay()!=null)return;
        started=true;previous=mc.options.tutorialStep;mc.options.tutorialStep=TutorialSteps.NONE;mc.getTutorial().stop();mc.options.pauseOnLostFocus=false;
        mc.options.renderDistance().set(3);mc.options.simulationDistance().set(5);mc.options.guiScale().set(2);mc.options.cloudStatus().set(CloudStatus.OFF);mc.options.setCameraType(CameraType.FIRST_PERSON);mc.options.hideGui=false;mc.resizeDisplay();
        GameRules rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);rules.getRule(GameRules.RULE_DAYLIGHT).set(false,null);rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false,null);
        mc.createWorldOpenFlows().createFreshLevel(WORLD,new LevelSettings(WORLD,GameType.SURVIVAL,false,Difficulty.NORMAL,true,rules,WorldDataConfiguration.DEFAULT),new WorldOptions(0x54433629L,false,false),WorldPresets::createNormalWorldDimensions);
    }
    private static void finish(Minecraft mc) {
        require(SAVED.get()==IMAGES.length,"Missing six current captures");stopped=true;mc.options.tutorialStep=previous;
        LogUtils.getLogger().info("THAUMCRAFT_TAINT_RENDER_AUDIT_OK: 6 native entity/terrain/scanner captures; existing original media/models; supplied material display separate from tick-grown fibres");
        LogUtils.getLogger().info("THAUMCRAFT_TAINT_CLIENT_SMOKE_OK: 6 scenes; native Prime event/tick-grown fibres/tiny tentacles/death drops/orphan decay; explicit fixtures; world={}",WORLD);
        mc.getConnection().getConnection().disconnect(Component.literal("Taint audit complete"));mc.clearLevel(new TitleScreen());mc.stop();
    }
    private static void fail(Minecraft mc,Throwable e) {if(stopped)return;stopped=true;if(previous!=null)mc.options.tutorialStep=previous;LogUtils.getLogger().error("THAUMCRAFT_TAINT_CLIENT_SMOKE_FAILED",e);mc.stop();}
}

