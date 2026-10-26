package thaumcraft.client.research;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.event.level.ChunkEvent;
import thaumcraft.alchemy.*;
import thaumcraft.api.aspects.*;
import thaumcraft.catalog.CatalogModule;
import thaumcraft.catalog.blocks.CatalogBlocks;
import thaumcraft.essentia.EssentiaJarBlockEntity;
import thaumcraft.infusion.*;
import thaumcraft.research.*;
import thaumcraft.scanning.ScanningNetwork;
import thaumcraft.scanning.client.ThaumometerClient;
import thaumcraft.fx.client.FxEffects;
import thaumcraft.fx.client.FxEngine;
import thaumcraft.fx.client.FxParticle;
import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/** Opt-in isolated owned world: actual Salis/caster C2S, natural infusion ticks and server payment checks. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public final class InfusionClientSmokeTest {
    private static final String WORLD="thaumcraft-infusion-smoke-"+System.currentTimeMillis();
    private static final String[] IMAGES={"unformed","formed","active","crafting","complete","materials","book-matrix","book-elemental","vis-resonator",
            "world-essentia-runes","world-ingredients","world-completion-bamf","world-instability-sparks","world-pedestal-shield","world-pedestal-bamf"};
    private static final BlockPos MATRIX=new BlockPos(0,114,0);
    private static final List<String> BOOK_TARGETS=List.of("INFUSION","CRYSTALFARMER","ELEMENTALTOOLS");
    private static final List<BlockPos> displays=new CopyOnWriteArrayList<>();
    private static final AtomicInteger saved=new AtomicInteger();
    private static volatile CompoundTag snapshot;
    private static CompletableFuture<Void> work;
    private static boolean started,setup,prepared,captureRequested,captured,stopped;
    private static int scene,phase,stableTicks,modelStates,heartbeat;
    private static long began;
    private static TutorialSteps previousTutorial;
    private static String bookBefore;
    private static final BlockPos CONTROL=new BlockPos(20,112,0);
    private static long renderFrame,lastWorldFxFrame=-1,sparkleBefore,itemBefore,sourceBefore,bamfBefore,shieldBefore,shieldRenderedBefore;
    private static boolean worldPaymentVerified,unstableFixture,bookVerified;
    private InfusionClientSmokeTest() {}
    static void snapshot(CompoundTag tag){if(Boolean.getBoolean("thaumcraft.infusionSmokeTest"))snapshot=tag.copy();}
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!Boolean.getBoolean("thaumcraft.infusionSmokeTest")||stopped)return;
        var mc=Minecraft.getInstance();if(began==0)began=System.nanoTime();
        try{
            require(System.nanoTime()-began<420_000_000_000L,"Infusion integrated audit timed out scene="+scene+" phase="+phase);
            if(!started){startWorld(mc);return;}if(mc.level==null||mc.player==null||mc.getOverlay()!=null)return;
            require(mc.getSingleplayerServer()!=null&&WORLD.equals(mc.getSingleplayerServer().getWorldData().getLevelName()),"Wrong infusion test world");mc.getToasts().clear();
            if(work!=null){if(!work.isDone())return;work.join();work=null;}
            if(!setup){setup=true;submit(mc,()->prepareWorld(mc));return;}
            if(scene==IMAGES.length){finish(mc);return;}
            // A saved one-shot frame remains valid after its short-lived native effect
            // expires. Do not require the effect to stay alive for 24 more ticks.
            if(scene>=9&&captured&&saved.get()==scene+1){advanceScene();return;}
            var matrix=mc.level.getBlockEntity(MATRIX) instanceof InfusionMatrixBlockEntity tile?tile:null;
            // The separate callback pedestal remains reviewable even if the already
            // captured unstable altar later destroys itself through a native event.
            if(matrix==null&&scene<13)return;
            if(scene>=9&&++heartbeat%40==0)logWorldReadiness(mc,matrix);
            if(scene==0){if(!(mc.level.getBlockEntity(MATRIX.below(2)) instanceof InfusionPedestalBlockEntity pedestal)||pedestal.isEmpty())return;
                if(!prepared){prepared=true;auditModels(mc);clientMutationAudit(mc);
                    // Simulate an already tracked old-save tile. Only the migration's real chunk
                    // packet can replace this client-side catalogue BE with the operational type.
                    mc.level.getChunkAt(MATRIX).addAndRegisterBlockEntity(new thaumcraft.catalog.blocks.CatalogBlockEntity(MATRIX,mc.level.getBlockState(MATRIX)));
                    submit(mc,()->{var level=player(mc).serverLevel();var chunk=level.getChunkAt(MATRIX);
                        chunk.addAndRegisterBlockEntity(new thaumcraft.catalog.blocks.CatalogBlockEntity(MATRIX,level.getBlockState(MATRIX)));
                        LegacyInfusionMigration.load(new ChunkEvent.Load(chunk,false));
                        for(int i=0;i<256&&!(level.getBlockEntity(MATRIX) instanceof InfusionMatrixBlockEntity);i++)LegacyInfusionMigration.tick(new TickEvent.LevelTickEvent(LogicalSide.SERVER,TickEvent.Phase.END,level,()->true));
                        require(level.getBlockEntity(MATRIX) instanceof InfusionMatrixBlockEntity,"Server legacy matrix did not migrate");});return;}
                if(!(mc.screen instanceof Gallery))mc.setScreen(new Gallery());}
            else if(scene==1){
                if(!prepared){prepared=true;mc.setScreen(null);use(mc);return;}
                if(!InfusionStability.valid(mc.level,MATRIX))return;
                if(phase==0){phase=1;submit(mc,()->{var player=player(mc);require(player.getMainHandItem().getCount()==1,"Salis packet did not consume exactly one dust");require(!serverMatrix(mc).active(),"Dust activated matrix before caster click");hand(mc,CatalogModule.stack("caster_basic"));});return;}
                if(!(mc.screen instanceof Gallery))mc.setScreen(new Gallery());
            }else if(scene==2){
                if(!InfusionMatrixBlock.isCaster(mc.player.getMainHandItem()))return;
                if(!prepared){prepared=true;mc.setScreen(null);use(mc);return;}if(!matrix.active()||matrix.startup()<.95F)return;
                require(!matrix.crafting(),"First caster packet skipped idle activation");if(!(mc.screen instanceof Gallery))mc.setScreen(new Gallery());
            }else if(scene==3){
                if(!prepared){prepared=true;mc.setScreen(null);use(mc);return;}if(!matrix.crafting()||matrix.getAspects().visSize()==0||matrix.getAspects().visSize()>=25)return;
                if(phase==0){phase=1;submit(mc,()->{require(serverMatrix(mc).crafting(),"Second caster packet did not start craft");require(jars(mc).stream().mapToInt(EssentiaJarBlockEntity::amount).sum()<85,"Crafting animation without real source debit");});return;}
                if(!(mc.screen instanceof Gallery))mc.setScreen(new Gallery());
            }else if(scene==4){
                var central=mc.level.getBlockEntity(MATRIX.below(2));if(matrix.crafting()||!(central instanceof InfusionPedestalBlockEntity p)||!p.getItem(0).is(CatalogBlocks.block("crystal_aer").asItem()))return;
                if(!prepared){prepared=true;submit(mc,()->verifyFinish(mc));return;}if(!(mc.screen instanceof Gallery))mc.setScreen(new Gallery());
            }else if(scene==5){if(!prepared){prepared=true;submit(mc,()->prepareMaterials(mc));return;}
                for(BlockPos pos:displays){if(mc.level.getBlockState(pos).isAir())return;if(mc.level.getBlockEntity(pos) instanceof InfusionMatrixBlockEntity material&&material.startup()<.95F)return;}if(!(mc.screen instanceof Gallery))mc.setScreen(new Gallery());
            }else if(scene==6||scene==7){if(!bookFixtureReady())return;if(!prepared){prepared=true;openBook(mc);}
                require(mc.screen instanceof ThaumonomiconPageScreen,"Missing actual book recipe page");
            }else if(scene==8){
                if(!prepared){prepared=true;mc.setScreen(null);submit(mc,()->{hand(mc,CatalogModule.stack("vis_resonator"));ScanningNetwork.sendHud(player(mc));});return;}
                if(!BuiltInRegistries.ITEM.getKey(mc.player.getMainHandItem().getItem()).getPath().equals("vis_resonator")||ThaumometerClient.currentSnapshot()==null)return;
                require(ThaumometerClient.currentTarget()==null&&ThaumometerClient.isVisibleForSmokeTest(),"Resonator performed scan / aura meter hidden");
            }else if(!advanceWorldFx(mc,matrix))return;
            if(++stableTicks>=(scene>=9?1:scene==3?60:12)&&!captured)captureRequested=true;
            if(captured&&saved.get()==scene+1&&stableTicks>=24)advanceScene();
        }catch(Throwable error){fail(mc,error);}
    }
    private static void advanceScene(){scene++;phase=stableTicks=0;prepared=captured=captureRequested=false;lastWorldFxFrame=-1;}
    @SubscribeEvent public static void rendered(TickEvent.RenderTickEvent event){
        if(!Boolean.getBoolean("thaumcraft.infusionSmokeTest")||stopped)return;
        if(event.phase==TickEvent.Phase.START){renderFrame++;return;}
        if(!captureRequested||captured)return;
        var mc=Minecraft.getInstance();try{
            if(scene<6&&!(mc.screen instanceof Gallery)||scene>=6&&scene<=7&&!(mc.screen instanceof ThaumonomiconPageScreen))return;
            if(scene>=9&&(lastWorldFxFrame!=renderFrame||!worldFxReady(mc)))return;
            if(scene>=9){var matrix=mc.level.getBlockEntity(MATRIX) instanceof InfusionMatrixBlockEntity tile?tile:null;LogUtils.getLogger().info("THAUMCRAFT_INFUSION_WORLD_NATIVE_FRAME: scene={} camera={} nativeWorldFrame={}/{} matrixPresent={} matrixCrafting={} stability={} remaining={} sourcePackets={} itemPackets={} sparklePackets={} shieldPackets={} shieldRendered={} bamfPackets={} actualQuadFacts={}",IMAGES[scene],mc.gameRenderer.getMainCamera().getPosition(),lastWorldFxFrame,renderFrame,matrix!=null,matrix==null?"missing":matrix.crafting(),matrix==null?"missing":matrix.stability(),matrix==null?"missing":matrix.getAspects().visSize(),FxEffects.receivedCount("infusionSource"),FxEffects.receivedCount("infusionItem"),FxEffects.receivedCount("infusionSparkle"),FxEffects.receivedCount("pedestalShield"),FxEffects.renderedShieldCount(),FxEffects.receivedCount("bamf"),worldParticles(mc).stream().filter(p->quadVisible(mc,p)).limit(12).map(p->p.getClass().getSimpleName()+":start"+p.start+":layer"+p.layer+":age"+p.age()+"/"+p.lifespan()+":pos"+p.position()+":quad"+inspectQuad(mc,p).facts).toList());}
            captured=true;captureRequested=false;String name="tc6-infusion-"+IMAGES[scene]+".png";var file=new File(new File(mc.gameDirectory,"screenshots"),name);Files.deleteIfExists(file.toPath());
            Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),message->{if(file.isFile()&&file.length()>0){saved.incrementAndGet();LogUtils.getLogger().info("THAUMCRAFT_INFUSION_SMOKE_IMAGE: {}",file.getAbsolutePath());}
                else mc.execute(()->fail(mc,new AssertionError("Missing screenshot "+name)));});
        }catch(Throwable error){fail(mc,error);}
    }
    /** Appended live world views preserve the original nine model/book/payment scenes. */
    private static boolean advanceWorldFx(Minecraft mc,InfusionMatrixBlockEntity matrix){
        if(scene==9||scene==12){
            if(phase==0){phase=1;worldPaymentVerified=false;unstableFixture=scene==12;mc.setScreen(null);mc.options.hideGui=true;mc.options.setCameraType(CameraType.FIRST_PERSON);mc.options.fov().set(55);mc.options.bobView().set(false);mc.options.graphicsMode().set(GraphicsStatus.FANCY);mc.options.particles().set(ParticleStatus.ALL);submit(mc,()->{
                    if(scene==9){require(bookBefore.equals(ThaumonomiconCompleteClientSmokeTest.gameplayState(KnowledgeStore.get(player(mc))).toString()),"Book changed authoritative gameplay knowledge");bookVerified=true;LogUtils.getLogger().info("THAUMCRAFT_INFUSION_BOOK_READ_ONLY: actualKnowledgeUnchangedBeforeSupplementalWorldGameplay=true");}
                    prepareWorldFx(mc,unstableFixture);
                });return false;}
            if(phase==1){if(matrix.active()||!mc.player.getMainHandItem().is(AlchemyModule.SALIS_MUNDUS.get())||mc.player.getMainHandItem().getCount()!=2)return false;phase=2;use(mc);return false;}
            if(phase==2){if(!InfusionStability.valid(mc.level,MATRIX))return false;phase=3;submit(mc,()->{require(player(mc).getMainHandItem().getCount()==1,"Supplemental Salis did not consume one dust");require(!serverMatrix(mc).active(),"Supplemental Salis activated altar");hand(mc,CatalogModule.stack("caster_basic"));});return false;}
            if(phase==3){if(!InfusionMatrixBlock.isCaster(mc.player.getMainHandItem()))return false;phase=4;use(mc);return false;}
            if(phase==4){if(!matrix.active()||matrix.startup()<.95F)return false;phase=5;sparkleBefore=FxEffects.receivedCount("infusionSparkle");itemBefore=FxEffects.receivedCount("infusionItem");sourceBefore=FxEffects.receivedCount("infusionSource");bamfBefore=FxEffects.receivedCount("bamf");use(mc);return false;}
            if(phase==5){if(!matrix.crafting())return false;phase=6;submit(mc,()->{require(serverMatrix(mc).crafting(),"Supplemental native caster did not start recipe");player(mc).connection.teleport(.5,112,5.5,180,0);});return false;}
            if(scene==9&&(!matrix.crafting()||matrix.getAspects().visSize()==0)){logWorldReadiness(mc,matrix);throw new AssertionError("Native paid essentia phase ended before its world capture; inspect read-only readiness diagnostics");}
            if(!worldPaymentVerified&&matrix.getAspects().visSize()<25){worldPaymentVerified=true;submit(mc,()->{int amount=jars(mc).stream().mapToInt(EssentiaJarBlockEntity::amount).sum();require(unstableFixture?amount<2:amount<85,"Supplemental world FX lacks actual essentia debit");LogUtils.getLogger().info("THAUMCRAFT_INFUSION_WORLD_PAYMENT: unstableInitialFixture={} sourceRemaining={} nativeRemaining={} nativeCasterStart=true noActiveClockRngOrPlanReset",unstableFixture,amount,serverMatrix(mc).getAspects().visSize());});return false;}
            return worldPaymentVerified&&matrix.crafting()&&matrix.getAspects().visSize()>0&&worldFxReady(mc);
        }
        if(scene==10){if(!matrix.crafting()){logWorldReadiness(mc,matrix);throw new AssertionError("Native paid reagent phase ended before its world capture; inspect read-only readiness diagnostics");}return matrix.getAspects().visSize()==0&&FxEffects.receivedCount("infusionSource")>sourceBefore&&FxEffects.receivedCount("infusionItem")>itemBefore&&worldFxReady(mc);}
        if(scene==11){
            if(matrix.crafting()||!(mc.level.getBlockEntity(MATRIX.below(2)) instanceof InfusionPedestalBlockEntity tile)||!tile.getItem(0).is(CatalogBlocks.block("crystal_aer").asItem()))return false;
            if(phase==0){phase=1;submit(mc,()->verifyFinish(mc));return false;}
            if(FxEffects.receivedCount("bamf")>bamfBefore&&!liveBamf(mc)){logWorldReadiness(mc,matrix);throw new AssertionError("Native completion callback expired before its world capture");}return FxEffects.receivedCount("bamf")>bamfBefore&&worldFxReady(mc);
        }
        if(phase==0){phase=1;mc.setScreen(null);submit(mc,()->{var p=player(mc);p.connection.teleport(20.5,112,4.5,180,10);require(p.serverLevel().getBlockEntity(CONTROL) instanceof InfusionPedestalBlockEntity tile&&tile.isEmpty(),"Controlled callback pedestal is not empty");});return false;}
        if(!cameraReady(mc,true))return false;
        if(phase==1){if(scene==14&&FxEffects.activeShieldCount()>0)return false;phase=2;shieldBefore=FxEffects.receivedCount("pedestalShield");shieldRenderedBefore=FxEffects.renderedShieldCount();bamfBefore=FxEffects.receivedCount("bamf");submit(mc,()->{var level=player(mc).serverLevel();var tile=(InfusionPedestalBlockEntity)level.getBlockEntity(CONTROL);require(tile.isEmpty(),"Controlled callback must not own a reagent");level.blockEvent(CONTROL,tile.getBlockState().getBlock(),scene==13?5:11,0);LogUtils.getLogger().info("THAUMCRAFT_INFUSION_CONTROLLED_PEDESTAL_EVENT: event={} emptyPedestal={} visualNativeServerBlockEventOnly/notNaturalMitigationOrEjection/noEnergyDebitOrDropClaim",scene==13?5:11,tile.isEmpty());});return false;}
        if(scene==13&&FxEffects.receivedCount("pedestalShield")>shieldBefore&&FxEffects.activeShieldCount()==0)throw new AssertionError("Native controlled shield expired before its world capture");
        if(scene==14&&FxEffects.receivedCount("bamf")>bamfBefore&&!liveBamf(mc))throw new AssertionError("Native controlled bamf expired before its world capture");
        return worldFxReady(mc);
    }
    private static void prepareWorldFx(Minecraft mc,boolean unstable){
        // The prior paid operation is finished. Build a fresh inactive fixture;
        // never rewrite the state, counters or RNG of an active operation.
        require(!serverMatrix(mc).crafting(),"Cannot replace a live paid altar");
        prepareWorld(mc);
        if(unstable){
            for(var jar:jars(mc))jar.takeFromContainer(jar.aspect(),jar.amount());
            require(jars(mc).get(0).addExact(Aspect.AIR,2),"Initial partial essentia fixture failed");
            for(int x=2;x<=7;x++)for(int z=-7;z<=-5;z++)levelPlace(player(mc),new BlockPos(x,112,z),"candle_white");
            var fresh=serverMatrix(mc);var initial=fresh.saveWithoutMetadata();initial.putFloat("stability",-70);fresh.load(initial);fresh.setChanged();player(mc).serverLevel().sendBlockUpdated(MATRIX,fresh.getBlockState(),fresh.getBlockState(),3);
            require(!fresh.active()&&!fresh.crafting(),"Negative stability fixture must precede activation/start");
        }
        var level=player(mc).serverLevel();for(var pos:BlockPos.betweenClosed(16,111,-3,24,117,7))level.setBlockAndUpdate(pos,pos.getY()==111?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState());
        levelPlace(player(mc),CONTROL,"pedestal_arcane");
        LogUtils.getLogger().info("THAUMCRAFT_INFUSION_WORLD_FIXTURES: freshInactiveAltar/rawResearchAndInputs/nativeSalisCasterStart/{} /fixedWorldCamera/noFxDispatchNoInjectedParticleAgeClockRngOrPaidPlan/emptySeparateControlledCallbackPedestal",unstable?"initialMinus70Stability18PhysicalUnpairedCandles2EssentiaThenNaturalStall":"initial85EssentiaNative25DebitTwoReagentsAndOutput");
    }
    private static void levelPlace(ServerPlayer player,BlockPos pos,String id){player.serverLevel().setBlockAndUpdate(pos,CatalogBlocks.block(id).defaultBlockState());}
    private static boolean cameraReady(Minecraft mc,boolean control){return mc.screen==null&&mc.getOverlay()==null&&mc.options.getCameraType().isFirstPerson()&&mc.player.distanceToSqr(control?20.5:.5,112,control?4.5:5.5)<.01&&Math.abs(mc.player.getYRot()-180)<.1&&Math.abs(mc.player.getXRot()-(control?10:0))<.1;}
    private static boolean liveBamf(Minecraft mc){var origin=(scene>=13?CONTROL:MATRIX).getCenter();return FxEngine.snapshot().stream().anyMatch(p->p.isAlive()&&p.grid==16&&p.start==123&&p.layer==1&&p.age()<=10&&p.position().distanceToSqr(origin)<64);}
    private static void logWorldReadiness(Minecraft mc,InfusionMatrixBlockEntity matrix){
        var particles=worldParticles(mc);LogUtils.getLogger().info("THAUMCRAFT_INFUSION_WORLD_READINESS: scene={} phase={} player={} yaw={} pitch={} camera={} cameraReady={} matrixPresent={} crafting={} remaining={} stability={} sourcePackets={} itemPackets={} sparklePackets={} runeCount={} visibleRuneCount={} visibleEssentiaCount={} visibleItemCount={} actualQuadAndClipFacts={}",scene,phase,mc.player.position(),mc.player.getYRot(),mc.player.getXRot(),mc.gameRenderer.getMainCamera().getPosition(),cameraReady(mc,scene>=13),matrix!=null,matrix==null?"missing":matrix.crafting(),matrix==null?"missing":matrix.getAspects().visSize(),matrix==null?"missing":matrix.stability(),FxEffects.receivedCount("infusionSource"),FxEffects.receivedCount("infusionItem"),FxEffects.receivedCount("infusionSparkle"),particles.stream().filter(p->p.getClass().getSimpleName().equals("Rune")).count(),particles.stream().filter(p->p.getClass().getSimpleName().equals("Rune")&&quadVisible(mc,p)).count(),particles.stream().filter(p->p.getClass().getSimpleName().equals("Bore")&&p.layer==0&&quadVisible(mc,p)).count(),particles.stream().filter(p->p.getClass().getSimpleName().equals("Bore")&&p.layer==1&&quadVisible(mc,p)).count(),particles.stream().limit(10).map(p->{var quad=inspectQuad(mc,p);return p.getClass().getSimpleName()+":age"+p.age()+"/"+p.lifespan()+":pos"+p.position()+":visible"+quadVisible(mc,p)+":quad"+quad.facts+":clips"+quad.points.stream().map(point->{var hit=mc.level.clip(new ClipContext(mc.gameRenderer.getMainCamera().getPosition(),point,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,mc.player));return hit.getType()+":"+hit.getBlockPos()+":"+hit.getLocation();}).toList();}).toList());
    }
    private static List<FxParticle> worldParticles(Minecraft mc){
        Vec3 origin=(scene>=13?CONTROL:MATRIX).getCenter();
        return FxEngine.snapshot().stream().filter(p->p.isAlive()&&p.age()>0&&p.age()<p.lifespan()&&p.position().distanceToSqr(origin)<64).toList();
    }
    private static boolean worldFxReady(Minecraft mc){
        if(!cameraReady(mc,scene>=13))return false;
        var particles=worldParticles(mc);long runes=particles.stream().filter(p->p.getClass().getSimpleName().equals("Rune")&&p.age()>=3&&quadVisible(mc,p)).count();
        // The original rune fades in naturally; age3/alpha17 was geometrically
        // present but too faint to review in the saved world frame.
        if(scene==9)return FxEffects.receivedCount("infusionSparkle")>sparkleBefore&&runes>0
                &&particles.stream().anyMatch(p->p.getClass().getSimpleName().equals("Rune")&&p.age()>=10&&inspectQuad(mc,p).alpha>=48&&quadVisible(mc,p))
                &&particles.stream().anyMatch(p->p.getClass().getSimpleName().equals("Bore")&&p.layer==0&&p.age()>=6&&quadVisible(mc,p));
        if(scene==10)return runes>0&&particles.stream().filter(p->p.getClass().getSimpleName().equals("Bore")&&p.layer==1&&!Float.isNaN(p.customU0)&&p.age()>=2&&quadVisible(mc,p)).count()>=2;
        if(scene==12)return ((InfusionMatrixBlockEntity)mc.level.getBlockEntity(MATRIX)).stability()<0&&particles.stream().anyMatch(p->p.grid==16&&p.count==8&&(p.start==8||p.start==24||p.start==40)&&p.position().distanceToSqr(MATRIX.getCenter())<3&&quadVisible(mc,p));
        if(scene==13)return FxEffects.receivedCount("pedestalShield")>shieldBefore&&FxEffects.activeShieldCount()>0&&FxEffects.renderedShieldCount()>shieldRenderedBefore;
        return FxEffects.receivedCount("bamf")>bamfBefore&&particles.stream().anyMatch(p->p.grid==16&&p.start==123&&p.layer==1&&p.age()<=10&&quadVisible(mc,p));
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void worldFxRendered(RenderLevelStageEvent event){
        if(!Boolean.getBoolean("thaumcraft.infusionSmokeTest")||stopped||scene<9||event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;
        var mc=Minecraft.getInstance();if(worldFxReady(mc))lastWorldFxFrame=renderFrame;
    }
    /** Read the actual production draw vertices into memory after its native world pass. */
    private static QuadFacts inspectQuad(Minecraft mc,FxParticle particle){var sink=new QuadFacts(mc.gameRenderer.getMainCamera().getPosition());particle.draw(sink,mc.gameRenderer.getMainCamera(),mc.getFrameTime(),new org.joml.Matrix4f());return sink;}
    private static boolean quadVisible(Minecraft mc,FxParticle particle){
        var quad=inspectQuad(mc,particle);if(quad.points.size()!=4||quad.alpha<=1)return false;int clear=0;
        var camera=mc.gameRenderer.getMainCamera();double tangent=Math.tan(Math.toRadians(mc.options.fov().get()/2D)),aspect=mc.getWindow().getScreenWidth()/(double)mc.getWindow().getScreenHeight();
        // Camera.setRotation rotates its forward basis from +Z, not OpenGL's -Z.
        // Project onto the actual native basis rather than negating inverse-local Z.
        var forward=camera.getLookVector();var up=camera.getUpVector();var left=camera.getLeftVector();
        for(var point:quad.points){var delta=point.subtract(camera.getPosition());double depth=delta.x*forward.x()+delta.y*forward.y()+delta.z*forward.z();
            double horizontal=delta.x*left.x()+delta.y*left.y()+delta.z*left.z(),vertical=delta.x*up.x()+delta.y*up.y()+delta.z*up.z();
            if(depth<=.05||Math.abs(horizontal/depth/tangent/aspect)>.95||Math.abs(vertical/depth/tangent)>.95)continue;
            if(mc.level.clip(new ClipContext(camera.getPosition(),point,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,mc.player)).getType()==HitResult.Type.MISS)clear++;
        }return clear>=2;
    }
    private static final class QuadFacts implements VertexConsumer {
        final Vec3 camera;final List<Vec3> points=new ArrayList<>(4);final List<String> facts=new ArrayList<>(4);double x,y,z;int alpha,r,g,b,block,sky;float u,v;
        QuadFacts(Vec3 camera){this.camera=camera;}
        @Override public VertexConsumer vertex(double x,double y,double z){this.x=x;this.y=y;this.z=z;return this;}
        @Override public VertexConsumer color(int r,int g,int b,int a){this.r=r;this.g=g;this.b=b;alpha=a;return this;}
        @Override public VertexConsumer uv(float u,float v){this.u=u;this.v=v;return this;}
        @Override public VertexConsumer overlayCoords(int u,int v){return this;}
        @Override public VertexConsumer uv2(int u,int v){block=u;sky=v;return this;}
        @Override public VertexConsumer normal(float x,float y,float z){return this;}
        @Override public void endVertex(){var point=camera.add(x,y,z);points.add(point);facts.add(point+":rgba"+r+","+g+","+b+","+alpha+":uv"+u+","+v+":uv2"+block+","+sky);}
        @Override public void defaultColor(int r,int g,int b,int a){color(r,g,b,a);}
        @Override public void unsetDefaultColor(){}
    }
    private static void startWorld(Minecraft mc){
        if(mc.screen instanceof AccessibilityOnboardingScreen){mc.options.onboardAccessibility=false;mc.options.save();mc.setScreen(new TitleScreen());return;}
        if(!(mc.screen instanceof TitleScreen)||mc.getOverlay()!=null)return;started=true;previousTutorial=mc.options.tutorialStep;mc.options.tutorialStep=TutorialSteps.NONE;mc.getTutorial().stop();
        mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(3);mc.options.simulationDistance().set(5);mc.options.guiScale().set(2);mc.resizeDisplay();
        var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);rules.getRule(GameRules.RULE_DAYLIGHT).set(false,null);rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false,null);
        mc.createWorldOpenFlows().createFreshLevel(WORLD,new LevelSettings(WORLD,GameType.SURVIVAL,false,Difficulty.PEACEFUL,true,rules,WorldDataConfiguration.DEFAULT),new WorldOptions(0x54433615L,false,false),WorldPresets::createNormalWorldDimensions);
    }
    private static ServerPlayer player(Minecraft mc){var player=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());require(player!=null,"Missing integrated player");return player;}
    private static InfusionMatrixBlockEntity serverMatrix(Minecraft mc){return (InfusionMatrixBlockEntity)player(mc).serverLevel().getBlockEntity(MATRIX);}
    private static void submit(Minecraft mc,Runnable action){require(work==null,"Overlapping server QA");var result=new CompletableFuture<Void>();work=result;mc.getSingleplayerServer().execute(()->{try{action.run();result.complete(null);}catch(Throwable error){result.completeExceptionally(error);}});}
    private static void hand(Minecraft mc,ItemStack item){var player=player(mc);player.getInventory().selected=0;player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(0));player.setItemInHand(InteractionHand.MAIN_HAND,item);player.inventoryMenu.broadcastChanges();}
    private static void use(Minecraft mc){mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(MATRIX.getCenter(),Direction.NORTH,MATRIX,false));}
    private static void research(ServerPlayer player,String key){try{var method=PlayerKnowledge.class.getDeclaredMethod("setResearchStage",String.class,int.class);method.setAccessible(true);method.invoke(KnowledgeStore.get(player),key,ResearchCatalog.get(key).stages().size()+1);}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}}
    private static void researchFixture(ServerPlayer player,String raw,Set<String> seeded){
        String key=ResearchCatalog.graphParentKey(raw);
        if(!seeded.add(key))return;
        var entry=ResearchCatalog.get(key);
        if(entry==null){require(key.startsWith("!"),"Unexpected fixture prerequisite "+key);KnowledgeStore.of(player.serverLevel()).recordFact(player.getUUID(),key);}
        else{for(String parent:entry.parents())researchFixture(player,parent,seeded);research(player,key);}
        var knowledge=KnowledgeStore.get(player);require(knowledge.isResearchCompleteStrict(raw),"Incomplete raw fixture prerequisite "+raw);
        if(entry!=null)for(String parent:entry.parents())require(knowledge.isResearchCompleteStrict(parent),"Unmet strict fixture parent "+key+":"+parent);
    }
    private static boolean bookFixtureReady(){
        if(snapshot==null)return false;
        var knowledge=PlayerKnowledge.load(snapshot);
        return ResearchCategories.categoryUnlocked(knowledge,"INFUSION")&&BOOK_TARGETS.stream().allMatch(key->{var entry=ResearchCatalog.get(key);return knowledge.isResearchCompleteStrict(key)&&entry.parents().stream().allMatch(knowledge::isResearchCompleteStrict);});
    }
    private static void place(Minecraft mc,BlockPos pos,String id){player(mc).serverLevel().setBlockAndUpdate(pos,CatalogBlocks.block(id).defaultBlockState());displays.add(pos);}
    private static void prepareWorld(Minecraft mc){
        var player=player(mc);var level=player.serverLevel();player.setInvulnerable(true);
        for(BlockPos pos:BlockPos.betweenClosed(-7,111,-7,7,117,7))level.setBlockAndUpdate(pos,pos.getY()==111?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState());
        place(mc,MATRIX,"infusion_matrix");place(mc,MATRIX.below(2),"pedestal_arcane");
        ((InfusionPedestalBlockEntity)level.getBlockEntity(MATRIX.below(2))).setItem(0,AspectCrystalItem.create(Aspect.AIR));
        for(int x:new int[]{-1,1})for(int z:new int[]{-1,1})for(int y:new int[]{-1,-2})place(mc,MATRIX.offset(x,y,z),"stone_arcane");
        place(mc,MATRIX.offset(3,-2,0),"pedestal_arcane");place(mc,MATRIX.offset(-3,-2,0),"pedestal_arcane");
        ((InfusionPedestalBlockEntity)level.getBlockEntity(MATRIX.offset(3,-2,0))).setItem(0,new ItemStack(Items.WHEAT_SEEDS));
        ((InfusionPedestalBlockEntity)level.getBlockEntity(MATRIX.offset(-3,-2,0))).setItem(0,new ItemStack(AlchemyModule.SALIS_MUNDUS.get()));
        Aspect[] aspects={Aspect.AIR,Aspect.CRYSTAL,Aspect.TRAP};for(int i=0;i<3;i++){var pos=MATRIX.offset(i-1,-2,-4);place(mc,pos,"jar_normal");require(((EssentiaJarBlockEntity)level.getBlockEntity(pos)).addExact(aspects[i],i==2?25:30),"Jar fixture fill failed");}
        // Explicit raw target and strict-prerequisite closure. The ordinary book
        // still enforces its category and parent gates on the synchronized state.
        Set<String> seeded=new LinkedHashSet<>();for(String key:BOOK_TARGETS)researchFixture(player,key,seeded);
        require(ResearchCategories.categoryUnlocked(KnowledgeStore.get(player),"INFUSION"),"Raw fixture did not unlock the original Infusion category");KnowledgeStore.of(level).setDirty();
        LogUtils.getLogger().info("THAUMCRAFT_INFUSION_RAW_RESEARCH_FIXTURE: targets={} strictParentClosure={} rawCompletedStagesAndDiscoveryFacts/notPaidResearch/noBookGateBypass",BOOK_TARGETS,seeded);
        player.getInventory().clearContent();hand(mc,new ItemStack(AlchemyModule.SALIS_MUNDUS.get(),2));
        player.teleportTo(.5,112,3.5);player.setYRot(180);player.setXRot(-8);ResearchNetwork.sync(player);
    }
    private static List<EssentiaJarBlockEntity> jars(Minecraft mc){List<EssentiaJarBlockEntity> out=new ArrayList<>();for(int i=0;i<3;i++)out.add((EssentiaJarBlockEntity)player(mc).serverLevel().getBlockEntity(MATRIX.offset(i-1,-2,-4)));return out;}
    private static void verifyFinish(Minecraft mc){
        require(!serverMatrix(mc).crafting()&&jars(mc).stream().allMatch(j->j.amount()==20),"Wrong exact25-unit recipe debit");
        for(int x:new int[]{-3,3})require(((InfusionPedestalBlockEntity)player(mc).serverLevel().getBlockEntity(MATRIX.offset(x,-2,0))).isEmpty(),"Reagent pedestal was not debited");
        LogUtils.getLogger().info("THAUMCRAFT_INFUSION_REAL_FLOW: actual dust/caster packets ->25 essentia ->2 reagents ->crystal_aer; jars20/20/20");
    }
    private static void prepareMaterials(Minecraft mc){
        displays.clear();var level=player(mc).serverLevel();String[] types={"arcane","ancient","eldritch"};
        for(int i=0;i<3;i++){BlockPos pos=MATRIX.offset((i-1)*5,0,0);place(mc,pos,"infusion_matrix");place(mc,pos.below(2),"pedestal_"+types[i]);
            for(int x:new int[]{-1,1})for(int z:new int[]{-1,1})place(mc,pos.offset(x,-2,z),"pillar_"+types[i]);
            var matrix=(InfusionMatrixBlockEntity)level.getBlockEntity(pos);var tag=matrix.saveWithoutMetadata();tag.putBoolean("active",true);tag.putFloat("stability",i==2?-30:25);matrix.load(tag);matrix.setChanged();level.sendBlockUpdated(pos,matrix.getBlockState(),matrix.getBlockState(),3);
            ((InfusionPedestalBlockEntity)level.getBlockEntity(pos.below(2))).setItem(0,CatalogModule.stack(i==0?"elemental_axe":i==1?"fortress_helm":"void_pickaxe"));}
    }
    private static void clientMutationAudit(Minecraft mc){
        var matrix=(InfusionMatrixBlockEntity)mc.level.getBlockEntity(MATRIX);var before=matrix.saveWithoutMetadata();matrix.setAspects(new AspectList().add(Aspect.AIR,999));require(matrix.addToContainer(Aspect.AIR,1)==1&&!matrix.takeFromContainer(Aspect.AIR,1)&&before.equals(matrix.saveWithoutMetadata()),"Client matrix mutated payment");
        var pedestal=(InfusionPedestalBlockEntity)mc.level.getBlockEntity(MATRIX.below(2));before=pedestal.saveWithoutMetadata();pedestal.setItem(0,new ItemStack(Items.DIAMOND));require(pedestal.removeItem(0,1).isEmpty()&&before.equals(pedestal.saveWithoutMetadata()),"Client pedestal manufactured item");
    }
    private static void auditModels(Minecraft mc){
        for(String id:List.of("pedestal_arcane","pedestal_ancient","pedestal_eldritch","stabilizer","inlay","matrix_cost","matrix_speed")){
            for(var state:CatalogBlocks.block(id).getStateDefinition().getPossibleStates()){
                var model=mc.getBlockRenderer().getBlockModel(state);require(model!=mc.getModelManager().getMissingModel(),"Missing altar model "+state);List<Direction> faces=new ArrayList<>(List.of(Direction.values()));faces.add(null);
                for(var face:faces)for(var quad:model.getQuads(state,face,RandomSource.create(0)))require(!quad.getSprite().contents().name().equals(MissingTextureAtlasSprite.getLocation()),"Missing altar sprite "+state);modelStates++;}
        }
        for(String type:List.of("normal","ancient","eldritch"))require(mc.getResourceManager().getResource(InfusionModule.id("textures/blocks/infuser_"+type+".png")).isPresent(),"Missing original infuser texture");
        require(mc.level.getRecipeManager().getAllRecipesFor(InfusionModule.RECIPE_TYPE.get()).size()==56,"RecipeManager did not sync56 real recipes");
    }
    private static void openBook(Minecraft mc){
        require(bookFixtureReady(),"Book fixture must arrive through its actual S2C snapshot");var knowledge=PlayerKnowledge.load(snapshot);bookBefore=ThaumonomiconCompleteClientSmokeTest.gameplayState(knowledge).toString();var browser=new ThaumonomiconScreen(knowledge,knowledge.scanCount());mc.setScreen(browser);browser.selectForSmokeTest(scene==6?"INFUSION":"ELEMENTALTOOLS");
        require(mc.screen instanceof ThaumonomiconPageScreen,"Missing infusion book entry");var page=(ThaumonomiconPageScreen)mc.screen;String output=scene==6?"infusion_matrix":"elemental_axe";
        var selected=BookRecipeViews.resolve(scene==6?"thaumcraft:InfusionMatrix":"thaumcraft:ElementalAxe").stream().filter(v->BuiltInRegistries.ITEM.getKey(v.output().getItem()).getPath().equals(output)).findFirst().orElseThrow();
        require(page.focusRecipe(selected.id()),"Could not navigate to actual recipe chapter");page.showRecipeForSmokeTest(output);
        var view=page.recipesForSmokeTest().stream().filter(v->BuiltInRegistries.ITEM.getKey(v.output().getItem()).getPath().equals(output)).findFirst().orElseThrow();require(!view.reference()&&!view.ingredients().isEmpty(),"Book still shows archive recipe for operational altar");
    }
    private static void finish(Minecraft mc){
        require(bookVerified,"Book nonmutation check did not run before supplemental native gameplay");
        require(saved.get()==IMAGES.length,"Missing fresh infusion images");stopped=true;mc.options.tutorialStep=previousTutorial;
        LogUtils.getLogger().info("THAUMCRAFT_INFUSION_WORLD_RENDER_AUDIT_OK: sixActualWorldFrames/AFTER_PARTICLESAndActualDrawQuadVisibility/secondNativePaid25EssentiaTwoReagentOutput/initialNegativeStabilityAsymmetricStalledAltar/explicitEmptyPedestalServerBlockEvents5and11NotNaturalMitigationOrEjection/noFxDispatchOrParticleAgeClockRngReset");
        LogUtils.getLogger().info("THAUMCRAFT_INFUSION_RENDER_AUDIT_OK: {} baked states; actual matrix/pedestal BER; three original textures, startup/glow/halo; synchronized recipes and aura",modelStates);
        LogUtils.getLogger().info("THAUMCRAFT_INFUSION_CLIENT_SMOKE_OK: {} scenes; real ritual/caster packets, naturally consumed25 essentia/2 reagents, S2C/client mutation guards; isolated world={}",saved.get(),WORLD);mc.stop();
    }
    private static void fail(Minecraft mc,Throwable failure){if(stopped)return;stopped=true;if(previousTutorial!=null)mc.options.tutorialStep=previousTutorial;LogUtils.getLogger().error("THAUMCRAFT_INFUSION_CLIENT_SMOKE_FAILED",failure);mc.stop();}
    private static final class Gallery extends Screen {
        Gallery(){super(Component.literal("TC6 BETA26 / Infusion: "+IMAGES[scene]));}
        @Override public boolean isPauseScreen(){return false;}
        @Override public void render(GuiGraphics gui,int mouseX,int mouseY,float partial){
            var mc=Minecraft.getInstance();try{
                gui.fill(0,0,width,height,0xFF17202B);gui.drawCenteredString(font,"TC6 BETA26 / Infusion: "+IMAGES[scene],width/2,15,0xFFEAD5A8);gui.flush();gui.pose().pushPose();
                try{gui.pose().translate(width/2.,height/2.+55,200);gui.pose().scale(43,-43,43);gui.pose().mulPose(Axis.XP.rotationDegrees(25));gui.pose().mulPose(Axis.YP.rotationDegrees(135));Lighting.setupFor3DItems();
                    for(BlockPos pos:displays){var state=mc.level.getBlockState(pos);if(state.isAir())continue;gui.pose().pushPose();gui.pose().translate(pos.getX(),pos.getY()-112,pos.getZ());
                        RenderSystem.runAsFancy(()->{mc.getBlockRenderer().renderSingleBlock(state,gui.pose(),gui.bufferSource(),15728880,OverlayTexture.NO_OVERLAY);var tile=mc.level.getBlockEntity(pos);
                            if(tile instanceof InfusionMatrixBlockEntity||tile instanceof InfusionPedestalBlockEntity||tile instanceof EssentiaJarBlockEntity)require(!mc.getBlockEntityRenderDispatcher().renderItem(tile,gui.pose(),gui.bufferSource(),15728880,OverlayTexture.NO_OVERLAY),"Missing real altar renderer");});gui.pose().popPose();}
                }finally{gui.flush();gui.pose().popPose();Lighting.setupFor3DItems();}
                var matrix=(InfusionMatrixBlockEntity)mc.level.getBlockEntity(MATRIX);gui.drawCenteredString(font,"active="+matrix.active()+" crafting="+matrix.crafting()+" stability="+matrix.stabilityName()+" remaining="+matrix.getAspects().visSize(),width/2,height-46,0xFFCDCEDF);
                gui.drawCenteredString(font,"Actual integrated-server state / original TC6 models",width/2,height-25,0xFF939CAF);
            }catch(Throwable error){fail(mc,error);}
        }
    }
}
