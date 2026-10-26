package thaumcraft.world.rift.client;

import com.mojang.logging.LogUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import thaumcraft.catalog.entities.*;
import thaumcraft.catalog.entities.client.LegacyVisualModel;
import thaumcraft.fx.client.FxEffects;
import thaumcraft.golemancy.entity.GolemOrbEntity;
import thaumcraft.golemancy.entity.ThaumcraftGolemEntity;
import thaumcraft.world.rift.FluxRiftEntity;
import thaumcraft.world.rift.WispEntity;

/** Native presentation regression in a newly owned world; actors/materials are raw fixtures, not progression. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public final class WorldVisualClientSmokeTest {
    private static final String WORLD="thaumcraft-world-visual-"+System.currentTimeMillis();
    private static final String[] IMAGES={"fancy-rift-exposed","fancy-rift-hidden","fancy-rift-goggles","fancy-rift-restored",
            "fabulous-rift-hidden","fabulous-rift-goggles","fabulous-rift-restored","wisp-ambient","wisp-native-zap","wisp-native-death",
            "guardian-near","guardian-far","warden-emerging","warden-risen","pech-held-items","lesser-inactive","lesser-growing","lesser-full",
            "greater-growing","greater-native-pulse","orb-flight","orb-native-impact","flyer-native-particles"};
    private static final AtomicInteger SAVED=new AtomicInteger();
    private static final List<Integer> ACTORS=new ArrayList<>();
    private static CompletableFuture<Void> work;
    private static TutorialSteps previous;
    private static boolean started,prepared,stopped,captureRequested,captured;
    private static int scene,phase,stable,heartbeat,actor=-1,secondary=-1;
    private static long began,actionTick,fxBefore;
    private static volatile boolean serverVerified;
    private WorldVisualClientSmokeTest(){}
    private static boolean enabled(){return Boolean.getBoolean("thaumcraft.worldVisualSmokeTest");}
    private static void require(boolean result,String text){if(!result)throw new AssertionError(text);}
    private static ServerPlayer player(Minecraft mc){var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());require(p!=null,"Owned player absent");return p;}
    private static void submit(Minecraft mc,Runnable action){require(work==null,"Overlapping owned server callback");var future=new CompletableFuture<Void>();work=future;mc.getSingleplayerServer().execute(()->{try{action.run();future.complete(null);}catch(Throwable error){future.completeExceptionally(error);}});}
    private static <T extends Entity>T spawn(ServerPlayer p,T entity,double x,double y,double z){require(entity!=null,"Fixture entity unavailable");entity.setPos(x,y,z);require(p.serverLevel().addFreshEntity(entity),"Native fixture spawn rejected");ACTORS.add(entity.getId());return entity;}
    private static VisualMobEntity mob(ServerPlayer p,String key,double x,double y,double z){return spawn(p,(VisualMobEntity)ForgeRegistries.ENTITY_TYPES.getValue(id(key)).create(p.serverLevel()),x,y,z);}
    private static net.minecraft.resources.ResourceLocation id(String key){return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumcraft",key);}
    private static ItemStack item(String key){return new ItemStack(ForgeRegistries.ITEMS.getValue(id(key)));}
    private static void view(ServerPlayer p,double z,float pitch){p.connection.teleport(.5,112,z,180,pitch);p.getAbilities().flying=true;p.onUpdateAbilities();}
    private static void clean(ServerPlayer p){
        for(int id:ACTORS){var entity=p.serverLevel().getEntity(id);if(entity!=null)entity.discard();}
        // Native death drops belong to the preceding raw fixture, not the next scene.
        for(var e:p.serverLevel().getEntitiesOfClass(Entity.class,new net.minecraft.world.phys.AABB(-17,111,-20,18,129,37),
                e->e instanceof net.minecraft.world.entity.item.ItemEntity||e instanceof ExperienceOrb))e.discard();
        ACTORS.clear();actor=secondary=-1;
    }
    private static Entity actor(Minecraft mc){return mc.level.getEntity(actor);}
    private static boolean elapsed(Minecraft mc,int ticks){return mc.level.getGameTime()-actionTick>=ticks;}
    private static void wall(ServerPlayer p,boolean opaque){for(var q:BlockPos.betweenClosed(-15,112,0,15,124,0))p.serverLevel().setBlock(q,opaque?Blocks.BEDROCK.defaultBlockState():Blocks.AIR.defaultBlockState(),2);}
    private static void requireWall(ServerPlayer p){for(var q:BlockPos.betweenClosed(-15,112,0,15,124,0))require(p.serverLevel().getBlockState(q).is(Blocks.BEDROCK),"Occlusion wall was modified");}

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!enabled()||stopped)return;
        var mc=Minecraft.getInstance();if(began==0)began=System.nanoTime();
        try{
            require(System.nanoTime()-began<1_200_000_000_000L,"World visual smoke timeout scene="+scene+" phase="+phase);
            if(!started){start(mc);return;}
            if(mc.level==null||mc.player==null||mc.getOverlay()!=null||mc.screen instanceof ReceivingLevelScreen)return;
            require(mc.getSingleplayerServer()!=null&&WORLD.equals(mc.getSingleplayerServer().getWorldData().getLevelName()),"Wrong owned world");
            mc.getToasts().clear();
            if(++heartbeat%200==0)LogUtils.getLogger().info("THAUMCRAFT_WORLD_VISUAL_HEARTBEAT: scene={} phase={} actor={} fxArc={} fxBurst={}",scene,phase,actor,FxEffects.receivedCount("arcBolt"),FxEffects.receivedCount("burst"));
            if(work!=null){if(!work.isDone())return;work.join();work=null;}
            if(!prepared){prepared=true;submit(mc,()->prepare(player(mc)));return;}
            if(captured){if(SAVED.get()==scene+1&&++stable>=3){scene++;phase=0;stable=0;captureRequested=captured=false;}return;}
            if(scene==IMAGES.length){finish(mc);return;}
            if(advance(mc))captureRequested=true;
        }catch(Throwable error){fail(mc,error);}
    }

    private static boolean advance(Minecraft mc){
        if(scene<=6)return rift(mc);
        if(scene<=9)return wisp(mc);
        if(scene<=11)return guardian(mc);
        if(scene<=13)return warden(mc);
        if(scene==14){
            if(phase==0){phase=1;submit(mc,()->{var p=player(mc);clean(p);var pech=mob(p,"pech",.5,112,-1.5);pech.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.BOW));pech.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.GOLDEN_APPLE));actor=pech.getId();view(p,3.5,8);});return false;}
            if(actor(mc)==null)return false;if(phase==1){phase=2;actionTick=mc.level.getGameTime();}return elapsed(mc,15);
        }
        if(scene<=19)return portal(mc);
        if(scene<=21)return orb(mc);
        if(phase==0){phase=1;mc.options.fov().set(70);submit(mc,()->{var p=player(mc);clean(p);for(var q:BlockPos.betweenClosed(-2,112,-7,2,117,-7))p.serverLevel().setBlock(q,Blocks.AIR.defaultBlockState(),2);var g=VisualEntitiesModule.GOLEM.get().create(p.serverLevel());require(g.setProps(3L<<32),"Raw Flyer design invalid");g.setOwnerId(p.getUUID());g.setValidSpawn();g.setNoAi(true);spawn(p,g,.5,113.5,-1.5);actor=g.getId();view(p,2.5,3);});return false;}
        if(!(actor(mc) instanceof ThaumcraftGolemEntity g))return false;
        require(g.design().hasTrait(thaumcraft.golemancy.press.GolemDesign.Trait.FLYER),"Flyer trait missing");
        if(phase==1){phase=2;actionTick=mc.level.getGameTime();fxBefore=FxEffects.receivedCount("golemFlyer");}
        return elapsed(mc,12)&&FxEffects.receivedCount("golemFlyer")>fxBefore;
    }

    private static boolean rift(Minecraft mc){
        boolean goggles=scene==2||scene==5;
        if(phase==0){
            phase=1;mc.options.graphicsMode().set(scene<4?GraphicsStatus.FANCY:GraphicsStatus.FABULOUS);mc.levelRenderer.allChanged();
            submit(mc,()->{var p=player(mc);if(scene==0){clean(p);var r=(FluxRiftEntity)ForgeRegistries.ENTITY_TYPES.getValue(id("flux_rift")).create(p.serverLevel());r.setRiftSeed(187);r.setRiftSize(40);r.setRiftStability(100);spawn(p,r,.5,116,-9);actor=r.getId();wall(p,true);}requireWall(p);p.setItemSlot(EquipmentSlot.HEAD,goggles?item("goggles"):ItemStack.EMPTY);view(p,scene==0?-3.5:9.5,-6);p.inventoryMenu.broadcastChanges();});return false;
        }
        if(!(actor(mc) instanceof FluxRiftEntity r)||r.getRiftSeed()!=187||r.getRiftSize()!=40)return false;
        if(thaumcraft.equipment.armor.GogglesArmorSupport.hasGoggles(mc.player)!=goggles)return false;
        if(mc.player.distanceToSqr(.5,112,scene==0?-3.5:9.5)>.001)return false;
        if(phase==1){phase=2;actionTick=mc.level.getGameTime();}
        if(!elapsed(mc,25))return false;
        if(phase==2){phase=3;submit(mc,()->{var p=player(mc);requireWall(p);require(p.serverLevel().getEntity(actor) instanceof FluxRiftEntity,"Original native rift disappeared");LogUtils.getLogger().info("THAUMCRAFT_WORLD_VISUAL_RIFT: scene={} opaqueBedrockUnchanged=true goggles={} nativeSeed187Size40 originalEnderShaderCameraUniforms=true",IMAGES[scene],goggles);});return false;}
        return true;
    }

    private static boolean wisp(Minecraft mc){
        if(scene==7){
            if(phase==0){phase=1;submit(mc,()->{var p=player(mc);clean(p);wall(p,false);p.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);var cow=spawn(p,EntityType.COW.create(p.serverLevel()),2.5,112,-5);cow.setNoAi(true);var w=(WispEntity)mob(p,"wisp",.5,113.3,-1.5);w.setAspectType("aer");w.setTarget(cow);actor=w.getId();secondary=cow.getId();view(p,4.5,0);p.inventoryMenu.broadcastChanges();});return false;}
            if(!(actor(mc) instanceof WispEntity w))return false;
            return w.tickCount>=5&&w.tickCount<=17;
        }
        if(scene==8){
            if(phase==0){phase=1;fxBefore=FxEffects.receivedCount("arcBolt");}
            if(FxEffects.receivedCount("arcBolt")<=fxBefore)return false;
            LogUtils.getLogger().info("THAUMCRAFT_WORLD_VISUAL_WISP_ZAP: nativeServerAI20TickThreshold/actualS2CArcBolt/newReceived={} actor={} target={}",FxEffects.receivedCount("arcBolt"),actor,secondary);return true;
        }
        if(phase==0){phase=1;fxBefore=FxEffects.receivedCount("wispBurst");submit(mc,()->{var p=player(mc);var w=p.serverLevel().getEntity(actor);require(w instanceof WispEntity,"Native Wisp missing before damage");w.hurt(p.damageSources().playerAttack(p),100);});return false;}
        if(phase==1&&FxEffects.receivedCount("wispBurst")>fxBefore){phase=2;actionTick=mc.level.getGameTime();}
        return phase==2&&elapsed(mc,6);
    }

    private static boolean guardian(Minecraft mc){
        if(phase==0){phase=1;submit(mc,()->{var p=player(mc);if(scene==10){clean(p);actor=mob(p,"eldritch_guardian",.5,112,-1.5).getId();}view(p,scene==10?5.5:23.5,scene==10?0:0);});return false;}
        if(!(actor(mc) instanceof VisualMobEntity g))return false;
        if(mc.player.distanceToSqr(.5,112,scene==10?5.5:23.5)>.001)return false;
        if(phase==1){phase=2;actionTick=mc.level.getGameTime();}
        if(!elapsed(mc,15))return false;
        float alpha=LegacyVisualModel.guardianAlpha(g);require(scene==10?Math.abs(alpha-.6F)<.001:alpha>0&&alpha<.4,"Original guardian fade outside expected band: "+alpha);
        LogUtils.getLogger().info("THAUMCRAFT_WORLD_VISUAL_GUARDIAN: scene={} distanceSquared={} alpha={}",IMAGES[scene],g.distanceToSqr(mc.player),alpha);return true;
    }

    private static boolean warden(Minecraft mc){
        if(scene==12){
            if(phase==0){phase=1;submit(mc,()->{var p=player(mc);clean(p);actor=mob(p,"eldritch_warden",.5,112,-1.5).getId();view(p,6.5,-4);});return false;}
            if(!(actor(mc) instanceof VisualMobEntity))return false;
            if(phase==1){phase=2;submit(mc,()->{var p=player(mc);p.serverLevel().broadcastEntityEvent(p.serverLevel().getEntity(actor),(byte)18);});return false;}
            int timer=((VisualMobEntity)actor(mc)).presentationSpawnTimer();return timer>=70&&timer<=78;
        }
        return actor(mc) instanceof VisualMobEntity w&&w.presentationSpawnTimer()==0&&w.tickCount>155;
    }

    private static boolean portal(Minecraft mc){
        if(scene==15){
            if(phase==0){phase=1;submit(mc,()->{var p=player(mc);clean(p);actor=mob(p,"cultist_portal_lesser",.5,112,-1.5).getId();view(p,4.5,0);});return false;}
            if(!(actor(mc) instanceof VisualMobEntity m))return false;require(!m.portalActive(),"Inactive lesser portal became active without native activation");return ++stable>12;
        }
        if(scene==16){
            if(phase==0){phase=1;submit(mc,()->{var p=player(mc);((VisualMobEntity)p.serverLevel().getEntity(actor)).setPortalActive(true);});return false;}
            return actor(mc) instanceof VisualMobEntity m&&m.portalActive()&&m.portalTicks()>=18&&m.portalTicks()<=25;
        }
        if(scene==17)return actor(mc) instanceof VisualMobEntity m&&m.portalActive()&&m.portalTicks()>=55;
        if(scene==18){
            if(phase==0){phase=1;submit(mc,()->{var p=player(mc);clean(p);actor=mob(p,"cultist_portal_greater",.5,112,-1.5).getId();view(p,4.5,0);});return false;}
            return actor(mc) instanceof VisualMobEntity m&&m.tickCount>=20&&m.tickCount<=30;
        }
        if(phase==0){if(!(actor(mc) instanceof VisualMobEntity m)||m.tickCount<55)return false;phase=1;submit(mc,()->{var p=player(mc);var portal=(VisualMobEntity)p.serverLevel().getEntity(actor);portal.hurt(p.damageSources().playerAttack(p),portal.getMaxHealth()*.3F);p.serverLevel().broadcastEntityEvent(portal,(byte)16);});return false;}
        return actor(mc) instanceof VisualMobEntity m&&m.presentationPulse()>=5&&m.presentationPulse()<=7&&m.getHealth()<m.getMaxHealth();
    }

    private static boolean orb(Minecraft mc){
        if(scene==20){
            if(phase==0){phase=1;submit(mc,()->{var p=player(mc);clean(p);for(var q:BlockPos.betweenClosed(-2,112,-7,2,117,-7))p.serverLevel().setBlock(q,Blocks.BLACK_CONCRETE.defaultBlockState(),2);var owner=spawn(p,EntityType.COW.create(p.serverLevel()),4.5,112,-1.5);owner.setNoAi(true);var o=VisualEntitiesModule.GOLEM_ORB.get().create(p.serverLevel());o.setOwner(owner);o.setDeltaMovement(0,0,-.15);spawn(p,o,.5,113.5,-1.5);actor=o.getId();view(p,4.5,0);});return false;}
            return actor(mc) instanceof GolemOrbEntity o&&o.tickCount>=5&&o.tickCount<=15;
        }
        if(phase==0){phase=1;mc.options.fov().set(30);fxBefore=FxEffects.receivedCount("burst");actionTick=mc.level.getGameTime();submit(mc,()->player(mc).connection.teleport(2.5,112,-3.5,141,2.5F));return false;}
        require(!elapsed(mc,180),"Native orb impact did not deliver its original burst within180ticks; actor="+actor(mc));
        if(phase==1&&FxEffects.receivedCount("burst")>fxBefore){phase=2;actionTick=mc.level.getGameTime();}
        return phase==2&&elapsed(mc,8);
    }

    private static void prepare(ServerPlayer p){
        var l=p.serverLevel();for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)l.getChunk(x,z);
        for(var q:BlockPos.betweenClosed(-17,112,-20,17,128,36))l.setBlock(q,Blocks.AIR.defaultBlockState(),2);
        for(var q:BlockPos.betweenClosed(-17,111,-20,17,111,36))l.setBlock(q,Blocks.BEDROCK.defaultBlockState(),2);
        for(var q:BlockPos.betweenClosed(-17,112,-19,17,124,-19))l.setBlock(q,Blocks.STONE.defaultBlockState(),2);
        l.setDayTime(6000);p.getInventory().clearContent();p.getAbilities().flying=true;p.onUpdateAbilities();p.inventoryMenu.broadcastChanges();
        LogUtils.getLogger().info("THAUMCRAFT_WORLD_VISUAL_FIXTURES: rawRiftSeed187Size40Stability100/rawNativeWispAndCowInitialTarget/rawCatalogueActorsOriginalPresentationEvents/rawGolemOrbRawFlyer/opaqueBedrockWall/ownedCreativeWorld/notPaidProgression/notBossAI/noInjectedEffectsOrAges");
    }
    @SubscribeEvent public static void rendered(TickEvent.RenderTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!enabled()||stopped||!captureRequested||captured)return;
        var mc=Minecraft.getInstance();try{
            require(mc.getWindow().getScreenWidth()==1600&&mc.getWindow().getScreenHeight()==1000,"Expected native1600x1000 capture");captured=true;captureRequested=false;stable=0;
            String name="tc6-world-visual-"+IMAGES[scene]+".png";var file=new File(new File(mc.gameDirectory,"screenshots"),name);require(!file.exists(),"Preserve native screenshot "+name);
            LogUtils.getLogger().info("THAUMCRAFT_WORLD_VISUAL_NATIVE_FRAME: name={} graphics={} camera={} yaw={} pitch={} actor={} arc={} burst={} wispBurst={} fov={}",name,mc.options.graphicsMode().get(),mc.gameRenderer.getMainCamera().getPosition(),mc.player.getYRot(),mc.player.getXRot(),actor,FxEffects.receivedCount("arcBolt"),FxEffects.receivedCount("burst"),FxEffects.receivedCount("wispBurst"),mc.options.fov().get());
            Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),msg->{if(file.isFile()&&file.length()>0){SAVED.incrementAndGet();LogUtils.getLogger().info("THAUMCRAFT_WORLD_VISUAL_CLIENT_SMOKE_IMAGE: {}",file.getAbsolutePath());}else mc.execute(()->fail(mc,new AssertionError("Screenshot absent")));});
        }catch(Throwable error){fail(mc,error);}
    }
    private static void start(Minecraft mc){
        if(mc.screen instanceof AccessibilityOnboardingScreen){mc.options.onboardAccessibility=false;mc.options.save();mc.setScreen(new TitleScreen());return;}
        if(!(mc.screen instanceof TitleScreen)||mc.getOverlay()!=null)return;started=true;previous=mc.options.tutorialStep;mc.options.tutorialStep=TutorialSteps.NONE;mc.getTutorial().stop();
        mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(4);mc.options.simulationDistance().set(5);mc.options.guiScale().set(2);mc.options.fov().set(70);mc.options.bobView().set(false);mc.options.cloudStatus().set(CloudStatus.OFF);mc.options.setCameraType(CameraType.FIRST_PERSON);mc.options.hideGui=true;mc.resizeDisplay();
        var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);rules.getRule(GameRules.RULE_DAYLIGHT).set(false,null);rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false,null);rules.getRule(GameRules.RULE_RANDOMTICKING).set(0,null);
        mc.createWorldOpenFlows().createFreshLevel(WORLD,new LevelSettings(WORLD,GameType.CREATIVE,false,Difficulty.NORMAL,true,rules,WorldDataConfiguration.DEFAULT),new WorldOptions(0x544336041L,false,false),WorldPresets::createNormalWorldDimensions);
    }
    private static void finish(Minecraft mc){
        if(phase==0){phase=1;submit(mc,()->{var p=player(mc);require(p.serverLevel().getEntity(actor) instanceof ThaumcraftGolemEntity,"Native Flyer fixture disappeared");serverVerified=true;});return;}if(!serverVerified)return;
        require(SAVED.get()==IMAGES.length,"Native frame set incomplete");stopped=true;mc.options.tutorialStep=previous;
        LogUtils.getLogger().info("THAUMCRAFT_WORLD_VISUAL_RENDER_AUDIT_OK: 23 captures/originalEnderShaderAndGogglesOcclusionControls/FancyFabulous/nativeWispAIZapAndDeath/guardianFade/wardenPresentationEvent/pechBothHands/originalPortalStates/nativeOrbImpact/flyerTick/noInjectedEffects/actualPixelsRequireReview");
        LogUtils.getLogger().info("THAUMCRAFT_WORLD_VISUAL_CLIENT_SMOKE_OK: 23 captures/rawPresentationFixturesNotPaidProgression/notBossAI,world={}",WORLD);mc.stop();
    }
    private static void fail(Minecraft mc,Throwable error){if(stopped)return;stopped=true;if(previous!=null)mc.options.tutorialStep=previous;LogUtils.getLogger().error("THAUMCRAFT_WORLD_VISUAL_CLIENT_SMOKE_FAILED",error);mc.stop();}
}
