package thaumcraft.world.eldritch.client;

import com.mojang.logging.LogUtils;
import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import thaumcraft.catalog.entities.*;
import thaumcraft.world.eldritch.*;

/** Opt-in raw actor fixtures followed by native clocks, hits, AI, spawned children and effects. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public final class EldritchClientSmokeTest {
    private static final String WORLD="thaumcraft-eldritch-"+System.currentTimeMillis();
    private static final String[] IMAGES={"crab-helmet","crab-native-helmet-break","inhabited-zombie","inhabited-native-crab-emergence",
        "warden-native-emergence","warden-native-field","warden-native-orb","warden-native-field-frenzy",
        "golem-first-phase","golem-native-headless-transition","golem-native-seeking-orb","praetor-native-cultist-support",
        "praetor-native-red-orb","greater-portal-intro","greater-portal-native-banner-arcs","greater-portal-native-waves","greater-portal-native-praetor"};
    private static final AtomicInteger SAVED=new AtomicInteger();
    private static CompletableFuture<Void> work;
    private static TutorialSteps previous;
    private static thaumcraft.test.fixtures.ChampionFixture championFixture;
    private static boolean started,prepared,stopped,want,captured;
    private static int scene,phase,stable,heartbeat,initialClientTicks;
    private static volatile int actor=-1,target=-1,projectile=-1,strikes;
    private static volatile boolean verified;
    private static long began,at,lastHit,arcBaseline,initialWorldTick=-1,lastOrbAudit=-20;
    private static volatile String serverOrbFacts="not yet observed";
    private EldritchClientSmokeTest(){}
    private static boolean enabled(){return Boolean.getBoolean("thaumcraft.eldritchSmokeTest");}
    private static void require(boolean ok,String text){if(!ok)throw new AssertionError(text);}
    private static ServerPlayer player(Minecraft mc){var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());require(p!=null,"Owned player absent");return p;}
    private static void submit(Minecraft mc,Runnable action){require(work==null,"Overlapping Eldritch server callback");var f=new CompletableFuture<Void>();work=f;mc.getSingleplayerServer().execute(()->{try{action.run();f.complete(null);}catch(Throwable failure){f.completeExceptionally(failure);}});}
    private static <T extends Entity>T spawn(ServerPlayer p,T entity,double x,double z){require(entity!=null,"Missing typed actor");entity.setPos(x,112,z);require(p.serverLevel().addFreshEntity(entity),"Native spawn vetoed");return entity;}
    private static VisualMobEntity mob(ServerPlayer p,String key,boolean initialSpawn){var entity=(VisualMobEntity)ForgeRegistries.ENTITY_TYPES.getValue(EldritchModule.id(key)).create(p.serverLevel());require(entity!=null,"Missing "+key);entity.setPos(.5,112,-2.5);if(initialSpawn)entity.finalizeSpawn(p.serverLevel(),p.serverLevel().getCurrentDifficultyAt(entity.blockPosition()),MobSpawnType.MOB_SUMMONED,null,null);entity.setNoAi(true);spawn(p,entity,.5,-2.5);actor=entity.getId();return entity;}
    private static Cow target(ServerPlayer p,double z){var cow=EntityType.COW.create(p.serverLevel());cow.setNoAi(true);cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(2000);cow.setHealth(2000);spawn(p,cow,.5,z);target=cow.getId();return cow;}
    private static void view(ServerPlayer p,double z,float pitch){p.connection.teleport(.5,112,z,180,pitch);p.getAbilities().flying=true;p.onUpdateAbilities();}
    private static void sideView(ServerPlayer p,double z,Vec3 focus){
        double x=8;Vec3 delta=focus.subtract(x,112+p.getEyeHeight(),z);
        float yaw=(float)Math.toDegrees(Math.atan2(delta.z,delta.x))-90;
        float pitch=(float)-Math.toDegrees(Math.atan2(delta.y,Math.sqrt(delta.x*delta.x+delta.z*delta.z)));
        p.connection.teleport(x,112,z,yaw,pitch);p.getAbilities().flying=true;p.onUpdateAbilities();
    }
    private static void clean(ServerPlayer p){p.closeContainer();for(var e:p.serverLevel().getEntities((Entity)null,new AABB(-26,110,-26,26,132,30),e->e instanceof Mob||e instanceof Projectile||e instanceof ItemEntity||e instanceof ExperienceOrb))e.discard();for(var q:BlockPos.betweenClosed(-22,112,-24,22,118,21))if(!p.serverLevel().getBlockState(q).isAir())p.serverLevel().setBlock(q,Blocks.AIR.defaultBlockState(),2);actor=target=projectile=-1;verified=false;strikes=0;p.removeAllEffects();p.getInventory().clearContent();}
    private static Entity actor(Minecraft mc){return mc.level.getEntity(actor);}
    private static boolean elapsed(Minecraft mc,int ticks){return mc.level.getGameTime()-at>=ticks;}
    private static boolean settled(Minecraft mc,int ticks){if(phase==1){phase=2;at=mc.level.getGameTime();}return elapsed(mc,ticks);}
    private static boolean initialReady(Minecraft mc){
        if(initialWorldTick<0)initialWorldTick=mc.level.getGameTime();initialClientTicks++;
        require(mc.level.getGameTime()-initialWorldTick<600,"Initial Eldritch arena meshes did not become ready");
        return initialClientTicks>=60&&mc.level.getGameTime()-initialWorldTick>=60
            &&mc.level.hasChunkAt(new BlockPos(0,111,0))&&mc.level.getBlockState(new BlockPos(0,111,0)).is(Blocks.BEDROCK)
            &&mc.levelRenderer.isChunkCompiled(new BlockPos(0,111,0))
            &&mc.levelRenderer.isChunkCompiled(new BlockPos(-16,111,-16))&&mc.levelRenderer.isChunkCompiled(new BlockPos(0,111,-16))
            &&mc.level.getBrightness(LightLayer.SKY,new BlockPos(0,114,5))>=12;
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){if(event.phase!=TickEvent.Phase.END||!enabled()||stopped)return;var mc=Minecraft.getInstance();if(began==0)began=System.nanoTime();try{
        require(System.nanoTime()-began<1_200_000_000_000L,"Eldritch client timeout scene="+scene+" phase="+phase);if(!started){start(mc);return;}if(mc.level==null||mc.player==null||mc.getOverlay()!=null||mc.screen instanceof ReceivingLevelScreen)return;require(mc.getSingleplayerServer()!=null&&WORLD.equals(mc.getSingleplayerServer().getWorldData().getLevelName()),"Wrong owned world");mc.getToasts().clear();if(++heartbeat%200==0)LogUtils.getLogger().info("THAUMCRAFT_ELDRITCH_HEARTBEAT: scene={} phase={} actor={} target={} projectile={} strikes={}",scene,phase,actor,target,projectile,strikes);
        if(work!=null){if(!work.isDone())return;work.join();work=null;}if(!prepared){prepared=true;submit(mc,()->prepare(player(mc)));return;}if(captured){if(SAVED.get()==scene+1&&++stable>=3){scene++;phase=stable=0;want=captured=false;}return;}if(scene==IMAGES.length){finish(mc);return;}if(advance(mc))want=true;
    }catch(Throwable failure){fail(mc,failure);}}
    private static boolean advance(Minecraft mc){
        if(scene==0&&!initialReady(mc))return false;
        if(scene<4)return crab(mc);if(scene<8)return warden(mc);if(scene<11)return golem(mc);if(scene<13)return leader(mc);return portal(mc);
    }
    private static boolean crab(Minecraft mc){
        if(scene==0){if(phase==0){phase=1;submit(mc,()->{var p=player(mc);clean(p);var crab=(EldritchCrabEntity)mob(p,"eldritch_crab",false);crab.setHelm(true);view(p,3.5,12);});return false;}return actor(mc) instanceof EldritchCrabEntity crab&&crab.hasHelm()&&settled(mc,15);}
        if(scene==1){if(phase==0){phase=1;submit(mc,()->{var p=player(mc);var crab=(EldritchCrabEntity)p.serverLevel().getEntity(actor);require(crab.hurt(p.damageSources().playerAttack(p),13),"Native crab hit rejected");verified=!crab.hasHelm()&&crab.isAlive();LogUtils.getLogger().info("THAUMCRAFT_ELDRITCH_CRAB_BREAK_OK: nativeDamagePipeline/raw13Hit/remainingHealth={}/armor={}/noHelmStateSetterAfterHit",crab.getHealth(),crab.getArmorValue());});return false;}return verified&&actor(mc) instanceof EldritchCrabEntity crab&&!crab.hasHelm()&&settled(mc,4);}
        if(scene==2){if(phase==0){phase=1;submit(mc,()->{var p=player(mc);clean(p);var zombie=EldritchModule.INHABITED_ZOMBIE.get().create(p.serverLevel());zombie.setPos(.5,112,-2.5);zombie.finalizeSpawn(p.serverLevel(),p.serverLevel().getCurrentDifficultyAt(zombie.blockPosition()),MobSpawnType.MOB_SUMMONED,null,null);zombie.setNoAi(true);spawn(p,zombie,.5,-2.5);actor=zombie.getId();view(p,4.5,4);});return false;}return actor(mc) instanceof InhabitedZombieEntity&&settled(mc,20);}
        if(phase==0){phase=1;submit(mc,()->{var p=player(mc);var zombie=(InhabitedZombieEntity)p.serverLevel().getEntity(actor);require(zombie.hurt(p.damageSources().playerAttack(p),200),"Inhabited native lethal hit rejected");});return false;}
        if(phase<=2){if(phase==1){phase=2;at=mc.level.getGameTime();}require(!elapsed(mc,100),"No native emerged crab");submit(mc,()->{var p=player(mc);var children=p.serverLevel().getEntitiesOfClass(EldritchCrabEntity.class,new AABB(-3,111,-6,3,118,1));if(children.size()==1){actor=children.get(0).getId();verified=children.get(0).hasHelm();phase=3;LogUtils.getLogger().info("THAUMCRAFT_ELDRITCH_INHABITED_DEATH_OK: actualLethalHit/nativeDeathTick/oneHelmetedChild={}",actor);}});return false;}
        return verified&&actor(mc) instanceof EldritchCrabEntity&&elapsed(mc,8);
    }
    private static boolean warden(Minecraft mc){
        if(scene==4){if(phase==0){phase=1;submit(mc,()->{var p=player(mc);clean(p);mob(p,"eldritch_warden",true);view(p,9,-5);});return false;}return actor(mc) instanceof EldritchWardenEntity w&&w.spawnTimer()>40&&settled(mc,15);}
        if(scene==5){if(phase==0){phase=1;at=mc.level.getGameTime();}require(!elapsed(mc,200),"Warden spawn did not finish");submit(mc,()->{var w=(EldritchWardenEntity)player(mc).serverLevel().getEntity(actor);verified=w.spawnTimer()==0&&w.getAbsorptionAmount()>=264;});return verified&&actor(mc) instanceof EldritchWardenEntity w&&w.spawnTimer()==0;}
        if(scene==6){if(phase==0){phase=1;submit(mc,()->{var p=player(mc);var w=(EldritchWardenEntity)p.serverLevel().getEntity(actor);w.setNoAi(false);w.setTarget(target(p,12));sideView(p,4,new Vec3(.5,114,4.5));});return false;}if(phase==1){phase=2;at=mc.level.getGameTime();}require(!elapsed(mc,400),"Warden native ranged capture timed out: "+serverOrbFacts);return ownedOrb(mc,false);}
        if(phase==0){phase=1;verified=false;projectile=-1;at=lastHit=mc.level.getGameTime();arcBaseline=EldritchArcClient.renderedCount();submit(mc,()->{var p=player(mc);var w=(EldritchWardenEntity)p.serverLevel().getEntity(actor);w.setTarget(null);w.setNoAi(true);view(p,18,16);});return false;}
        require(!elapsed(mc,600),"Warden native field frenzy/arc not observed");
        if(!verified&&mc.level.getGameTime()-lastHit>=22){lastHit=mc.level.getGameTime();submit(mc,()->{var p=player(mc);var w=(EldritchWardenEntity)p.serverLevel().getEntity(actor);if(w.fieldFrenzyCounter()==0){require(strikes++<16,"Field did not break after original damage");w.hurt(p.damageSources().generic(),35);}verified=w.fieldFrenzyCounter()>0;if(verified)LogUtils.getLogger().info("THAUMCRAFT_ELDRITCH_FRENZY_OK: nativeHits={}/remainingField={}/counter={}/noAbsorptionOrTimerSetter",strikes,w.getAbsorptionAmount(),w.fieldFrenzyCounter());});return false;}
        return verified&&EldritchArcClient.hasLiveArc()&&EldritchArcClient.renderedCount()>arcBaseline&&actor(mc) instanceof EldritchWardenEntity;
    }
    private static boolean golem(Minecraft mc){
        if(scene==8){if(phase==0){phase=1;submit(mc,()->{var p=player(mc);clean(p);mob(p,"eldritch_golem",true);view(p,9,-10);});return false;}return actor(mc) instanceof EldritchGolemEntity g&&!g.headless()&&settled(mc,20);}
        if(scene==9){if(phase==0){phase=1;at=mc.level.getGameTime();}if(phase==1){submit(mc,()->{var p=player(mc);var g=(EldritchGolemEntity)p.serverLevel().getEntity(actor);if(g.spawnTimer()==0){float health=g.getHealth();require(!g.hurt(p.damageSources().generic(),1000)&&g.headless()&&g.getHealth()==health,"Original raw lethal headless transition failed");phase=2;verified=true;LogUtils.getLogger().info("THAUMCRAFT_ELDRITCH_HEADLESS_OK: actualRaw1000Hit/falseReturn/intactHealth={}/nativeSpawnTimer={}/noHeadlessOrTimerSetter",health,g.spawnTimer());}});return false;}return verified&&actor(mc) instanceof EldritchGolemEntity g&&g.headless()&&elapsed(mc,130);}
        if(phase==0){phase=1;at=mc.level.getGameTime();submit(mc,()->{var p=player(mc);var g=(EldritchGolemEntity)p.serverLevel().getEntity(actor);g.setNoAi(false);g.setTarget(target(p,12));sideView(p,4,new Vec3(.5,114,4.5));});return false;}require(!elapsed(mc,400),"Headless native ranged capture timed out: "+serverOrbFacts);return ownedOrb(mc,false);
    }
    private static boolean leader(Minecraft mc){
        if(scene==11){if(phase==0){phase=1;submit(mc,()->{var p=player(mc);clean(p);var leader=(CultistLeaderEntity)mob(p,"cultist_leader",true);leader.setNoAi(false);var ally=(thaumcraft.world.crimson.CultistEntity)ForgeRegistries.ENTITY_TYPES.getValue(EldritchModule.id("cultist_knight")).create(p.serverLevel());ally.finalizeSpawn(p.serverLevel(),p.serverLevel().getCurrentDifficultyAt(leader.blockPosition()),MobSpawnType.MOB_SUMMONED,null,null);ally.setNoAi(true);spawn(p,ally,2,-2);target=ally.getId();view(p,6,-2);});return false;}if(!settled(mc,25))return false;submit(mc,()->{var p=player(mc);var ally=(Mob)p.serverLevel().getEntity(target);verified=ally.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION);});return verified&&actor(mc) instanceof CultistLeaderEntity;}
        if(phase==0){phase=1;at=mc.level.getGameTime();submit(mc,()->{var p=player(mc);var leader=(CultistLeaderEntity)p.serverLevel().getEntity(actor);leader.setTarget(target(p,18));sideView(p,5,new Vec3(.5,113.5,5.5));});return false;}require(!elapsed(mc,400),"Praetor native ranged capture timed out: "+serverOrbFacts);return ownedOrb(mc,true);
    }
    private static boolean portal(Minecraft mc){
        if(scene==13){if(phase==0){phase=1;arcBaseline=EldritchArcClient.renderedCount();submit(mc,()->{var p=player(mc);clean(p);mob(p,"cultist_portal_greater",false);view(p,14,3);});return false;}return actor(mc) instanceof GreaterCultistPortalEntity&&settled(mc,15);}
        if(scene==14){if(phase==0){phase=1;at=mc.level.getGameTime();}require(!elapsed(mc,180),"Greater intro banner arc not rendered");if(EldritchArcClient.renderedCount()<=arcBaseline||!EldritchArcClient.hasLiveArc()||actor(mc)==null)return false;int banners=0;for(var d:net.minecraft.core.Direction.Plane.HORIZONTAL){BlockPos q=new BlockPos((int)actor(mc).getX()-d.getStepX()*6,112,(int)actor(mc).getZ()+d.getStepZ()*6);if(mc.level.getBlockState(q).is(thaumcraft.catalog.blocks.CatalogBlocks.block("banner_crimson_cult")))banners++;}if(banners==4){LogUtils.getLogger().info("THAUMCRAFT_ELDRITCH_GREATER_BANNERS_OK: nativeInitialClock/fourSynchronizedBannerBlocks/realLiveFXArcDraw/noStageTimerSetter");return true;}return false;}
        if(phase==0){phase=1;verified=false;at=mc.level.getGameTime();}require(!elapsed(mc,scene==15?400:6000),"Native greater portal wave progression timed out");
        submit(mc,()->{var p=player(mc);var portal=(GreaterCultistPortalEntity)p.serverLevel().getEntity(actor);if(scene==15){int minions=p.serverLevel().getEntitiesOfClass(thaumcraft.world.crimson.CultistEntity.class,portal.getBoundingBox().inflate(35)).size();verified=portal.stage()>=1&&minions>=1;}else{var leaders=p.serverLevel().getEntitiesOfClass(CultistLeaderEntity.class,portal.getBoundingBox().inflate(35));verified=portal.stage()>=13&&leaders.size()==1;if(verified)LogUtils.getLogger().info("THAUMCRAFT_ELDRITCH_GREATER_PRAETOR_OK: realNativeStage={}/nativeElapsedTicks={}/actualSpawnedLeader={}/noStageTimerOrRngWrites",portal.stage(),p.serverLevel().getGameTime()-at,leaders.get(0).getId());}});
        return verified;
    }
    private static boolean nativeFlight(Projectile orb){
        return orb.tickCount>=3&&!orb.isRemoved()&&orb.getOwner()!=null
            &&orb.getDeltaMovement().lengthSqr()>.000001
            &&!orb.getBoundingBox().inflate(.15).intersects(orb.getOwner().getBoundingBox());
    }
    private static String entityFacts(Entity entity){
        if(entity==null)return "absent";
        String facts="id="+entity.getId()+"/type="+ForgeRegistries.ENTITY_TYPES.getKey(entity.getType())+"/alive="+entity.isAlive()
            +"/age="+entity.tickCount+"/position="+entity.position()+"/motion="+entity.getDeltaMovement();
        if(entity instanceof LivingEntity living)facts+="/health="+living.getHealth();
        if(entity instanceof Mob mob)facts+="/noAi="+mob.isNoAi()+"/target="+(mob.getTarget()==null?"none":mob.getTarget().getId());
        if(entity instanceof ThaumcraftBossEntity boss)facts+="/spawnTimer="+boss.spawnTimer();
        if(entity instanceof EldritchGolemEntity golem)facts+="/headless="+golem.headless()+"/magazine="+golem.beamCharge()+"/charging="+golem.chargingBeam();
        if(entity instanceof Projectile orb)facts+="/owner="+(orb.getOwner()==null?"none":orb.getOwner().getId())+"/flightReady="+nativeFlight(orb);
        return facts;
    }
    private static void auditOrbWait(Minecraft mc,String reason){
        if(mc.level.getGameTime()-lastOrbAudit<20)return;lastOrbAudit=mc.level.getGameTime();
        var clientShots=new java.util.ArrayList<String>();for(Entity entity:mc.level.entitiesForRendering())
            if(entity instanceof Projectile orb&&orb.getOwner()!=null&&orb.getOwner().getId()==actor)clientShots.add(entityFacts(orb));
        LogUtils.getLogger().info("THAUMCRAFT_ELDRITCH_ORB_WAIT_CLIENT: scene={} reason={} camera={} caster={} target={} ownedShots={}",scene,reason,mc.gameRenderer.getMainCamera().getPosition(),entityFacts(mc.level.getEntity(actor)),entityFacts(mc.level.getEntity(target)),clientShots);
        submit(mc,()->{var l=player(mc).serverLevel();var shots=l.getEntities((Entity)null,new AABB(-26,110,-26,26,132,30),
                entity->entity instanceof Projectile orb&&orb.getOwner()!=null&&orb.getOwner().getId()==actor).stream().map(EldritchClientSmokeTest::entityFacts).toList();
            serverOrbFacts="caster="+entityFacts(l.getEntity(actor))+", target="+entityFacts(l.getEntity(target))+", ownedShots="+shots;
            LogUtils.getLogger().info("THAUMCRAFT_ELDRITCH_ORB_WAIT_SERVER: scene={} reason={} {}",scene,reason,serverOrbFacts);
        });
    }
    private static boolean ownedOrb(Minecraft mc,boolean red){
        if(projectile<0){
            for(Entity e:mc.level.entitiesForRendering())if(e instanceof Projectile orb&&nativeFlight(orb)&&orb.getOwner().getId()==actor
                    &&(red?e instanceof thaumcraft.golemancy.entity.GolemOrbEntity g&&g.red():scene==6?e instanceof thaumcraft.world.warp.EldritchOrbEntity:e instanceof thaumcraft.golemancy.entity.GolemOrbEntity g&&!g.red())){
                projectile=e.getId();verified=false;submit(mc,()->{var p=player(mc);Entity actual=p.serverLevel().getEntity(projectile);
                    if(actual instanceof Projectile nativeOrb&&nativeOrb.getOwner()!=null&&nativeOrb.getOwner().getId()==actor){
                        verified=true;LogUtils.getLogger().info("THAUMCRAFT_ELDRITCH_NATIVE_ORB_OK: scene={}/actualType={}/owner={}/target={}/realGoalAndMagazine/noAttackOrProjectileInjection",scene,ForgeRegistries.ENTITY_TYPES.getKey(actual.getType()),actor,target);
                    }
                });return false;
            }
            auditOrbWait(mc,"no qualifying native flight");return false;
        }
        if(!(mc.level.getEntity(projectile) instanceof Projectile orb)){projectile=-1;verified=false;auditOrbWait(mc,"selected projectile already left");return false;}
        if(verified&&nativeFlight(orb))return true;
        auditOrbWait(mc,"selected projectile no longer satisfies flight gate");return false;
    }
    private static void prepare(ServerPlayer p){var level=p.serverLevel();for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)level.getChunk(x,z);for(var q:BlockPos.betweenClosed(-26,112,-26,26,131,30))level.setBlock(q,Blocks.AIR.defaultBlockState(),2);for(var q:BlockPos.betweenClosed(-26,111,-26,26,111,30))level.setBlock(q,Blocks.BEDROCK.defaultBlockState(),2);for(var q:BlockPos.betweenClosed(-26,132,-26,26,132,30))level.setBlock(q,Blocks.GLASS.defaultBlockState(),2);level.setDayTime(6000);p.getInventory().clearContent();view(p,6,0);LogUtils.getLogger().info("THAUMCRAFT_ELDRITCH_FIXTURES: ownedNewWorld/rawNativeActors/originalFinalizeSpawnGear/raw2000HPCowTargets/initialNoAIComparisons/actualNativeDamagePipelineAndClocks/AIProjectiles/FrenzyFXArc/13NativePortalStages/noActiveTimerStateRngOrParticleInjection/notPaidProgression/notNaturalDiscovery/BETA26DormantOuterLands");}
    @SubscribeEvent public static void rendered(TickEvent.RenderTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!enabled()||stopped||!want||captured)return;var mc=Minecraft.getInstance();
        try{
            if(scene==6||scene==10||scene==12){
                if(!(mc.level.getEntity(projectile) instanceof Projectile orb)||!nativeFlight(orb)){want=false;return;}
                LogUtils.getLogger().info("THAUMCRAFT_ELDRITCH_NATIVE_FLIGHT_FRAME: scene={} camera={} actualNativeOrb={} casterBounds={}",scene,mc.gameRenderer.getMainCamera().getPosition(),entityFacts(orb),orb.getOwner().getBoundingBox());
            }
            require(mc.getWindow().getScreenWidth()==1600&&mc.getWindow().getScreenHeight()==1000,"Expected1600x1000 native frame");captured=true;want=false;stable=0;
            String name="tc6-eldritch-"+IMAGES[scene]+".png";File file=new File(new File(mc.gameDirectory,"screenshots"),name);require(!file.exists(),"Preserve previous image "+name);
            LogUtils.getLogger().info("THAUMCRAFT_ELDRITCH_NATIVE_FRAME: name={} camera={} yaw={} pitch={} actor={} target={} projectile={} actualArcDraws={}",name,mc.gameRenderer.getMainCamera().getPosition(),mc.player.getYRot(),mc.player.getXRot(),actor,target,projectile,EldritchArcClient.renderedCount());
            Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),message->{if(file.isFile()&&file.length()>0){SAVED.incrementAndGet();LogUtils.getLogger().info("THAUMCRAFT_ELDRITCH_CLIENT_SMOKE_IMAGE: {}",file.getAbsolutePath());}else mc.execute(()->fail(mc,new AssertionError("Native image absent")));});
        }catch(Throwable failure){fail(mc,failure);}
    }
    private static void start(Minecraft mc){if(mc.screen instanceof AccessibilityOnboardingScreen){mc.options.onboardAccessibility=false;mc.options.save();mc.setScreen(new TitleScreen());return;}if(!(mc.screen instanceof TitleScreen)||mc.getOverlay()!=null)return;championFixture=new thaumcraft.test.fixtures.ChampionFixture(mc.gameDirectory.toPath());started=true;previous=mc.options.tutorialStep;mc.options.tutorialStep=TutorialSteps.NONE;mc.getTutorial().stop();mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(4);mc.options.simulationDistance().set(5);mc.options.guiScale().set(2);mc.options.fov().set(70);mc.options.bobView().set(false);mc.options.cloudStatus().set(CloudStatus.OFF);mc.options.setCameraType(CameraType.FIRST_PERSON);mc.options.hideGui=false;mc.resizeDisplay();var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);rules.getRule(GameRules.RULE_DAYLIGHT).set(false,null);rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false,null);rules.getRule(GameRules.RULE_RANDOMTICKING).set(0,null);mc.createWorldOpenFlows().createFreshLevel(WORLD,new LevelSettings(WORLD,GameType.CREATIVE,false,net.minecraft.world.Difficulty.NORMAL,true,rules,WorldDataConfiguration.DEFAULT),new WorldOptions(0x454C44042L,false,false),WorldPresets::createNormalWorldDimensions);}
    private static void finish(Minecraft mc){if(phase==0){phase=1;submit(mc,()->{clean(player(mc));verified=true;});return;}if(!verified)return;require(SAVED.get()==IMAGES.length,"Incomplete native Eldritch captures");if(championFixture!=null)championFixture.close();stopped=true;mc.options.tutorialStep=previous;LogUtils.getLogger().info("THAUMCRAFT_ELDRITCH_RENDER_AUDIT_OK: 17captures/actualCrabHelmetBreak/InhabitedDeathChild/WardenEmergenceFieldOrbFrenzy/GolemHeadlessMagazineOrb/PraetorSupportRedOrb/GreaterPortalBannerFXArcAnd13NativeStages/actualPixelsRequireReview");LogUtils.getLogger().info("THAUMCRAFT_ELDRITCH_CLIENT_SMOKE_OK: 17 captures/rawActorsTargetsNotFullSurvival/world={}",WORLD);mc.stop();}
    private static void fail(Minecraft mc,Throwable failure){if(stopped)return;stopped=true;if(previous!=null)mc.options.tutorialStep=previous;try{if(championFixture!=null)championFixture.close();}catch(Throwable restoreFailure){failure.addSuppressed(restoreFailure);}LogUtils.getLogger().error("THAUMCRAFT_ELDRITCH_CLIENT_SMOKE_FAILED",failure);mc.stop();}
}
