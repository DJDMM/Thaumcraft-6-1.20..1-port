package thaumcraft.world.mobs.client;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import thaumcraft.catalog.CatalogModule;
import thaumcraft.catalog.entities.*;
import thaumcraft.world.crimson.*;
import thaumcraft.world.mobs.*;

/** Opt-in actual native interactions in a new owned world. Raw actors/materials are explicit fixtures. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public final class MobsClientSmokeTest{
    private static final String WORLD="thaumcraft-mobs-"+System.currentTimeMillis();
    private static final String[] IMAGES={"pech-wild","pech-native-tamed-pack","pech-trade-offer","pech-native-trade-outcome",
        "pech-native-arrow","pech-native-focus","firebat-hanging","firebat-native-flight","brainy-native","giant-brainy-neutral",
        "giant-brainy-native-anger","lesser-portal-inactive","lesser-portal-native-activation","lesser-portal-native-minions",
        "cultist-knight-native-melee","cultist-cleric-native-projectiles","thaumic-slime","taint-crawler","taint-swarm",
        "taintacle","taintacle-giant","contained-finite-goo-heights","bottle-native-flight","bottle-native-impact"};
    private static final AtomicInteger SAVED=new AtomicInteger();
    private static CompletableFuture<Void> work;
    private static TutorialSteps previous;
    private static thaumcraft.test.fixtures.ChampionFixture championFixture;
    private static boolean started,prepared,stopped,captureRequested,captured;
    private static int scene,phase,stable,heartbeat;
    private static volatile int actor=-1,target=-1,projectile=-1,outcome;
    private static volatile boolean verified;
    private static long began,actionTick;
    private static org.joml.Matrix4f worldProjection;
    private static org.joml.Quaternionf inverseCamera;
    private static Vec3 frameCamera;
    private static int flightScene=-1;
    private static long flightStarted,lastFlightDiagnostic,worldFrameTick;
    private MobsClientSmokeTest(){}
    private static boolean enabled(){return Boolean.getBoolean("thaumcraft.mobsSmokeTest");}
    private static void require(boolean ok,String text){if(!ok)throw new AssertionError(text);}
    private static net.minecraft.resources.ResourceLocation id(String key){return MobsModule.id(key);}
    private static ServerPlayer player(Minecraft mc){var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());require(p!=null,"Owned player absent");return p;}
    private static void submit(Minecraft mc,Runnable action){
        require(work==null,"Overlapping server callback");var future=new CompletableFuture<Void>();work=future;
        mc.getSingleplayerServer().execute(()->{try{action.run();future.complete(null);}catch(Throwable e){future.completeExceptionally(e);}});
    }
    private static <T extends Entity>T spawn(ServerPlayer p,T entity,double x,double y,double z){
        require(entity!=null,"Missing native entity fixture");entity.setPos(x,y,z);require(p.serverLevel().addFreshEntity(entity),"Native fixture insertion rejected");return entity;
    }
    private static VisualMobEntity mob(ServerPlayer p,String key,double x,double y,double z){return spawn(p,(VisualMobEntity)ForgeRegistries.ENTITY_TYPES.getValue(id(key)).create(p.serverLevel()),x,y,z);}
    private static Cow cow(ServerPlayer p,double x,double z){
        Cow cow=spawn(p,EntityType.COW.create(p.serverLevel()),x,112,z);cow.setNoAi(true);cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);cow.setHealth(100);return cow;
    }
    private static void view(ServerPlayer p,double z,float pitch){p.connection.teleport(.5,112,z,180,pitch);p.getAbilities().flying=true;p.onUpdateAbilities();}
    private static void viewAt(ServerPlayer p,double x,double z,Vec3 at){
        Vec3 delta=at.subtract(x,112+p.getEyeHeight(),z);
        float yaw=(float)(Math.atan2(delta.z,delta.x)*180/Math.PI)-90;
        float pitch=(float)(-Math.atan2(delta.y,Math.sqrt(delta.x*delta.x+delta.z*delta.z))*180/Math.PI);
        p.connection.teleport(x,112,z,yaw,pitch);
    }
    private static void held(ServerPlayer p,ItemStack stack){
        p.getInventory().selected=0;p.getInventory().setItem(0,stack);p.inventoryMenu.broadcastChanges();p.connection.send(new ClientboundSetCarriedItemPacket(0));
    }
    private static void clean(ServerPlayer p){
        p.closeContainer();
        for(Entity e:p.serverLevel().getEntities((Entity)null,new AABB(-20,111,-20,20,129,37),
                e->e instanceof Mob||e instanceof Projectile||e instanceof ItemEntity||e instanceof ExperienceOrb))e.discard();
        for(BlockPos q:BlockPos.betweenClosed(-5,112,-8,5,124,2))
            if(!p.serverLevel().getBlockState(q).isAir())p.serverLevel().setBlock(q,Blocks.AIR.defaultBlockState(),2);
        actor=target=projectile=-1;verified=false;outcome=0;
        p.removeAllEffects();p.setInvulnerable(false);p.getInventory().clearContent();held(p,ItemStack.EMPTY);
    }
    private static Entity actor(Minecraft mc){return mc.level.getEntity(actor);}
    /** Read only the projection/camera actually used by the completed native world frame. */
    @SubscribeEvent public static void worldFrame(RenderLevelStageEvent event){
        if(!enabled()||stopped||event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL)return;
        var camera=Minecraft.getInstance().gameRenderer.getMainCamera();
        worldProjection=new org.joml.Matrix4f(event.getProjectionMatrix());
        inverseCamera=new org.joml.Quaternionf(camera.rotation()).conjugate();frameCamera=camera.getPosition();worldFrameTick=Minecraft.getInstance().level.getGameTime();
    }
    private static org.joml.Vector3f projected(Vec3 point){
        if(worldProjection==null||inverseCamera==null||frameCamera==null)return null;
        Vec3 relative=point.subtract(frameCamera);
        var view=new org.joml.Vector3f((float)relative.x,(float)relative.y,(float)relative.z).rotate(inverseCamera);
        // Camera.rotation uses +Z forward/+X left; GameRenderer's OpenGL view
        // uses -Z forward/+X right (native rotateX(pitch)*rotateY(yaw+180)).
        view.x=-view.x;view.z=-view.z;
        var clip=new org.joml.Vector4f(view,1).mul(worldProjection);
        if(clip.w<=0)return null;
        return new org.joml.Vector3f(clip.x/clip.w,clip.y/clip.w,clip.z/clip.w);
    }
    private static String flightRejection(Minecraft mc,Projectile shot){
        if(shot.tickCount<3)return "age-below3";
        if(shot.getDeltaMovement().lengthSqr()<.0001)return "motion-below-threshold";
        if(shot.getOwner()==null)return "owner-absent";
        if(shot.getBoundingBox().inflate(.1).intersects(shot.getOwner().getBoundingBox()))return "physical-caster-overlap";
        Vec3 point=shot.getBoundingBox().getCenter();var pixel=projected(point);
        if(pixel==null)return "projection-unavailable-or-behind-camera";
        if(Math.abs(pixel.x)>.85||Math.abs(pixel.y)>.8||pixel.z< -1||pixel.z>1)return "outside-viewport-or-clip-depth";
        if(mc.level.clip(new ClipContext(frameCamera,point,ClipContext.Block.VISUAL,ClipContext.Fluid.NONE,mc.player)).getType()!=HitResult.Type.MISS)return "native-block-ray-obstructed";
        if(shot.getOwner()!=mc.player){
            AABB box=shot.getOwner().getBoundingBox().inflate(.15);float minX=Float.POSITIVE_INFINITY,minY=minX,maxX=Float.NEGATIVE_INFINITY,maxY=maxX;
            for(double x:new double[]{box.minX,box.maxX})for(double y:new double[]{box.minY,box.maxY})for(double z:new double[]{box.minZ,box.maxZ}){
                var corner=projected(new Vec3(x,y,z));if(corner==null)return "caster-corner-behind-camera";
                minX=Math.min(minX,corner.x);maxX=Math.max(maxX,corner.x);minY=Math.min(minY,corner.y);maxY=Math.max(maxY,corner.y);
            }
            if(pixel.x>=minX-.025&&pixel.x<=maxX+.025&&pixel.y>=minY-.025&&pixel.y<=maxY+.025)return "projected-caster-overlap";
        }
        return "visible";
    }
    private static boolean visibleFlight(Minecraft mc,Projectile shot){return flightRejection(mc,shot).equals("visible");}
    private static void diagnoseFlight(Minecraft mc){
        if(flightScene!=scene){flightScene=scene;flightStarted=mc.level.getGameTime();lastFlightDiagnostic=Long.MIN_VALUE;}
        long age=mc.level.getGameTime()-flightStarted;
        if(lastFlightDiagnostic==Long.MIN_VALUE||mc.level.getGameTime()-lastFlightDiagnostic>=20||age>=400){
            lastFlightDiagnostic=mc.level.getGameTime();var shots=new ArrayList<String>();
            for(Entity e:mc.level.entitiesForRendering())if(e instanceof Projectile shot){
                Entity owner=shot.getOwner();
                if(owner==null||owner.getId()==actor||scene==22&&owner==mc.player)shots.add("id="+shot.getId()+" type="+ForgeRegistries.ENTITY_TYPES.getKey(shot.getType())+" age="+shot.tickCount+" position="+shot.position()+" owner="+(owner==null?null:owner.getId())+" motion="+shot.getDeltaMovement()+" ndc="+projected(shot.getBoundingBox().getCenter())+" rejection="+flightRejection(mc,shot));
            }
            Entity actualActor=mc.level.getEntity(actor),actualTarget=mc.level.getEntity(target);
            LogUtils.getLogger().info("THAUMCRAFT_MOBS_FLIGHT_DIAGNOSTIC: scene={} nativeElapsed={} frameTick={} worldTick={} actor={} actorPosition={} target={} targetHealth={} camera={} inverseQuaternion={} projection={} shots={}",scene,age,worldFrameTick,mc.level.getGameTime(),actor,actualActor==null?null:actualActor.position(),target,actualTarget instanceof LivingEntity living?living.getHealth():null,frameCamera,inverseCamera,worldProjection,shots);
            final int observedScene=scene,observedActor=actor,observedTarget=target;
            mc.getSingleplayerServer().execute(()->{var p=player(mc);var mob=p.serverLevel().getEntity(observedActor);var victim=p.serverLevel().getEntity(observedTarget);
                var serverShots=p.serverLevel().getEntities((Entity)null,new AABB(-100,100,-100,100,160,100),e->e instanceof Projectile shot&&shot.getOwner()!=null&&(shot.getOwner().getId()==observedActor||observedScene==22&&shot.getOwner()==p)).stream().map(e->"id="+e.getId()+" type="+ForgeRegistries.ENTITY_TYPES.getKey(e.getType())+" position="+e.position()+" nativeAge="+e.tickCount).toList();
                LogUtils.getLogger().info("THAUMCRAFT_MOBS_FLIGHT_SERVER_DIAGNOSTIC: scene={} actor={} actorAlive={} noAi={} mobTarget={} actorPosition={} targetHealth={} ownedShots={}",observedScene,observedActor,mob!=null&&mob.isAlive(),mob instanceof Mob m?m.isNoAi():null,mob instanceof Mob m&&m.getTarget()!=null?m.getTarget().getId():null,mob==null?null:mob.position(),victim instanceof LivingEntity living?living.getHealth():null,serverShots);
            });
        }
        require(age<400,"Native flight capture exceeded400worldticks; see bounded actual projection/actor/shot diagnostics for scene="+scene);
    }
    private static boolean captureOwnedProjectile(Minecraft mc,String marker){
        diagnoseFlight(mc);
        if(projectile<0){
            for(Entity candidate:mc.level.entitiesForRendering())if(candidate instanceof Projectile shot
                    &&shot.getOwner()!=null&&shot.getOwner().getId()==actor&&visibleFlight(mc,shot)){
                projectile=shot.getId();verified=false;int clientTick=shot.tickCount;
                submit(mc,()->{var p=player(mc);Entity actual=p.serverLevel().getEntity(projectile);
                    if(actual instanceof Projectile serverShot&&serverShot.getOwner()!=null&&serverShot.getOwner().getId()==actor){
                        verified=true;LogUtils.getLogger().info("{}: scene={} actualProjectileType={} caster={} target={} nativeClientTick={}",marker,scene,
                                ForgeRegistries.ENTITY_TYPES.getKey(actual.getType()),actor,target,clientTick);
                    }
                });return false;
            }return false;
        }
        if(!(mc.level.getEntity(projectile) instanceof Projectile shot)){projectile=-1;verified=false;return false;}
        return verified&&visibleFlight(mc,shot);
    }
    private static boolean elapsed(Minecraft mc,int ticks){return mc.level.getGameTime()-actionTick>=ticks;}
    private static boolean settled(Minecraft mc,int ticks){if(phase==1){phase=2;actionTick=mc.level.getGameTime();}return elapsed(mc,ticks);}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!enabled()||stopped)return;var mc=Minecraft.getInstance();if(began==0)began=System.nanoTime();
        try{
            require(System.nanoTime()-began<1_200_000_000_000L,"Mobs smoke timeout scene="+scene+" phase="+phase);
            if(!started){start(mc);return;}
            if(mc.level==null||mc.player==null||mc.getOverlay()!=null||mc.screen instanceof ReceivingLevelScreen)return;
            require(mc.getSingleplayerServer()!=null&&WORLD.equals(mc.getSingleplayerServer().getWorldData().getLevelName()),"Wrong owned world");mc.getToasts().clear();
            if(++heartbeat%200==0)LogUtils.getLogger().info("THAUMCRAFT_MOBS_HEARTBEAT: scene={} phase={} actor={} target={} projectile={}",scene,phase,actor,target,projectile);
            if(work!=null){if(!work.isDone())return;work.join();work=null;}
            if(!prepared){prepared=true;submit(mc,()->prepare(player(mc)));return;}
            if(captured){if(SAVED.get()==scene+1&&++stable>=3){scene++;phase=0;stable=0;captureRequested=captured=false;}return;}
            if(scene==IMAGES.length){finish(mc);return;}
            if(advance(mc))captureRequested=true;
        }catch(Throwable e){fail(mc,e);}
    }
    private static boolean advance(Minecraft mc){
        if(scene<=3)return pechTrade(mc);
        if(scene<=5)return pechCombat(mc);
        if(scene<=7)return firebat(mc);
        if(scene<=10)return zombie(mc);
        if(scene<=13)return portal(mc);
        if(scene<=15)return cultist(mc);
        if(scene<=20)return taint(mc);
        if(scene==21)return goo(mc);
        return bottle(mc);
    }
    private static boolean pechTrade(Minecraft mc){
        if(scene==0){
            if(phase==0){phase=1;submit(mc,()->{var p=player(mc);clean(p);var pech=(PechEntity)mob(p,"pech",.5,112,-1.5);pech.setNoAi(true);pech.setPechType(0);pech.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.FISHING_ROD));actor=pech.getId();view(p,4.5,6);});return false;}
            return actor(mc) instanceof PechEntity&&settled(mc,18);
        }
        if(scene==1){
            if(phase==0){phase=1;submit(mc,()->{var p=player(mc);((PechEntity)p.serverLevel().getEntity(actor)).setNoAi(false);held(p,new ItemStack(Items.ENDER_PEARL,8));view(p,2,28);});return false;}
            if(phase==1){
                if(!mc.player.getMainHandItem().is(Items.ENDER_PEARL)||mc.player.getMainHandItem().getCount()!=8)return false;
                mc.player.drop(false);mc.player.drop(false);phase=2;actionTick=mc.level.getGameTime();return false;
            }
            require(!elapsed(mc,240),"Native Pech did not collect/tame from real dropped items");
            if(phase==2){submit(mc,()->{var p=player(mc);var pech=(PechEntity)p.serverLevel().getEntity(actor);
                verified=pech.tamed()&&pech.loot.stream().filter(s->s.is(Items.ENDER_PEARL)).mapToInt(ItemStack::getCount).sum()==1&&p.getInventory().getItem(0).getCount()==6;
                if(verified){phase=3;view(p,pech.getZ()+4,6);LogUtils.getLogger().info("THAUMCRAFT_MOBS_PECH_TAMED_OK: twoNativeDropPackets/8to6/oneConsumedForTame/onePhysicalPackItem/noTameSetter");}});return false;}
            return verified&&actor(mc) instanceof PechEntity pech&&pech.tamed()&&elapsed(mc,8);
        }
        if(scene==2){
            if(phase==0){phase=1;submit(mc,()->{var p=player(mc);var pech=(PechEntity)p.serverLevel().getEntity(actor);p.connection.teleport(pech.getX(),112,pech.getZ()+3,180,8);
                ItemStack offer=new ItemStack(Items.ENDER_PEARL,3);offer.getOrCreateTag().putBoolean("MobsSmokeOffer",true);held(p,offer);});return false;}
            if(phase==1){if(!(actor(mc) instanceof PechEntity pech)||mc.player.getMainHandItem().getCount()!=3||mc.player.distanceToSqr(pech)>16)return false;
                mc.gameMode.interact(mc.player,pech,InteractionHand.MAIN_HAND);phase=2;return false;}
            if(!(mc.player.containerMenu instanceof PechMenu menu)||!(mc.screen instanceof PechScreen))return false;
            if(phase==2){mc.gameMode.handleInventoryMouseClick(menu.containerId,32,0,ClickType.PICKUP,mc.player);phase=3;return false;}
            if(phase==3){if(menu.getCarried().getCount()!=3)return false;mc.gameMode.handleInventoryMouseClick(menu.containerId,0,0,ClickType.PICKUP,mc.player);phase=4;return false;}
            return menu.getSlot(0).getItem().getCount()==3&&menu.offerValue()==15&&menu.canTrade();
        }
        if(phase==0){require(mc.player.containerMenu instanceof PechMenu&&mc.screen instanceof PechScreen,"Actual Pech menu absent");
            var screen=(PechScreen)mc.screen;require(screen.mouseClicked((screen.width-175)/2.+79,(screen.height-232)/2.+36,0),"Native Pech dice button rejected click");
            phase=1;verified=false;actionTick=mc.level.getGameTime();return false;}
        require(!elapsed(mc,180),"Native trade outcome did not settle");
        if(!verified){submit(mc,()->{var p=player(mc);
            if(p.containerMenu instanceof PechMenu menu&&menu.tradeInventory().getItem(0).getCount()==2){outcome=1;verified=true;}
            else{int count=p.serverLevel().getEntitiesOfClass(ItemEntity.class,p.getBoundingBox().inflate(8),e->e.getPersistentData().getBoolean("PechDrop")&&e.getItem().is(Items.ENDER_PEARL)
                    &&e.getItem().hasTag()&&e.getItem().getTag().getBoolean("MobsSmokeOffer")).stream().mapToInt(e->e.getItem().getCount()).sum();if(count==2){outcome=2;verified=true;}}
            if(verified){
                if(outcome==2){var pech=p.serverLevel().getEntity(actor);viewAt(p,pech.getX()+5,pech.getZ()+5,pech.getBoundingBox().getCenter());phase=2;actionTick=p.serverLevel().getGameTime();}
                LogUtils.getLogger().info("THAUMCRAFT_MOBS_PECH_TRADE_OK: originalButton0/oneOfThreeConsumed/outcome={} (1=liveGUI,2=originalUntamedCloseAndGroundReturn)/noRngReroll/groundReturnCameraOnlyNoDropMutation",outcome);
            }
        });return false;}
        return outcome==1&&mc.screen instanceof PechScreen&&mc.player.containerMenu.getSlot(0).getItem().getCount()==2||outcome==2&&mc.screen==null&&elapsed(mc,8);
    }
    private static boolean pechCombat(Minecraft mc){
        if(phase==0){phase=1;submit(mc,()->{var p=player(mc);clean(p);PechEntity pech=(PechEntity)mob(p,"pech",-1.5,112,-1.5);pech.setPechType(scene==4?2:1);
            pech.setItemSlot(EquipmentSlot.MAINHAND,scene==4?new ItemStack(Items.BOW):CatalogModule.stack("pech_wand"));Cow cow=cow(p,1.5,-9);pech.setTarget(cow);actor=pech.getId();target=cow.getId();viewAt(p,6,3,new Vec3(-.5,113,-4.5));});return false;}
        if(!(actor(mc) instanceof PechEntity))return false;
        return captureOwnedProjectile(mc,"THAUMCRAFT_MOBS_PECH_COMBAT_OK");
    }
    private static boolean firebat(Minecraft mc){
        if(scene==6){
            if(phase==0){phase=1;submit(mc,()->{var p=player(mc);clean(p);p.serverLevel().setBlockAndUpdate(new BlockPos(0,115,-2),Blocks.STONE.defaultBlockState());actor=mob(p,"fire_bat",.5,114.1,-1.5).getId();view(p,6.5,-6);});return false;}
            return actor(mc) instanceof FirebatEntity bat&&bat.hanging()&&settled(mc,18);
        }
        if(phase==0){phase=1;submit(mc,()->{var p=player(mc);p.serverLevel().setBlockAndUpdate(new BlockPos(0,115,-2),Blocks.AIR.defaultBlockState());});return false;}
        return actor(mc) instanceof FirebatEntity bat&&!bat.hanging()&&bat.position().distanceToSqr(new Vec3(.5,114.1,-1.5))>.02&&settled(mc,10);
    }
    private static boolean zombie(Minecraft mc){
        if(scene==8||scene==9){
            if(phase==0){phase=1;submit(mc,()->{var p=player(mc);clean(p);
                for(var q:BlockPos.betweenClosed(-2,119,-4,2,119,1))p.serverLevel().setBlockAndUpdate(q,Blocks.STONE.defaultBlockState());
                var z=scene==8?MobsModule.BRAINY_ZOMBIE.get().create(p.serverLevel()):MobsModule.GIANT_BRAINY_ZOMBIE.get().create(p.serverLevel());z.setNoAi(true);spawn(p,z,.5,112,-1.5);actor=z.getId();view(p,5.5,0);LogUtils.getLogger().info("THAUMCRAFT_MOBS_ZOMBIE_ROOF_FIXTURE: initialOpaqueLocalRoofBeforeFirstActorTick/noActiveFireClear/noSunlightImmunityChange");});return false;}
            return actor(mc) instanceof BrainyZombieEntity&&settled(mc,18);
        }
        if(phase==0){phase=1;submit(mc,()->{var p=player(mc);var giant=(GiantBrainyZombieEntity)p.serverLevel().getEntity(actor);
            for(int i=0;i<15;i++)giant.hurt(p.damageSources().playerAttack(p),.01F);view(p,9.5,-5);
            require(giant.anger()>1,"Real native hit callbacks did not raise Giant anger");LogUtils.getLogger().info("THAUMCRAFT_MOBS_GIANT_HITS_OK: fifteenActualHurtCallbacks/health={} anger={} noAngerSetter",giant.getHealth(),giant.anger());
        });return false;}
        return actor(mc) instanceof GiantBrainyZombieEntity giant&&giant.anger()>1&&giant.getBbWidth()>1.1&&settled(mc,15);
    }
    private static boolean portal(Minecraft mc){
        if(scene==11){
            if(phase==0){phase=1;mc.options.fov().set(30);submit(mc,()->{var p=player(mc);clean(p);view(p,35.5,-1);actor=mob(p,"cultist_portal_lesser",.5,112,-1.5).getId();});return false;}
            return actor(mc) instanceof LesserCultistPortalEntity portal&&!portal.isActive()&&settled(mc,18);
        }
        if(scene==12){
            if(phase==0){phase=1;mc.options.fov().set(70);submit(mc,()->view(player(mc),6.5,0));return false;}
            return actor(mc) instanceof LesserCultistPortalEntity portal&&portal.isActive()&&portal.portalTicks()>=18&&portal.portalTicks()<=30;
        }
        if(!(actor(mc) instanceof LesserCultistPortalEntity portal)||portal.getHealth()>=100)return false;
        return mc.level.getEntitiesOfClass(CultistEntity.class,portal.getBoundingBox().inflate(5)).stream().anyMatch(e->e.tickCount>=3);
    }
    private static boolean cultist(Minecraft mc){
        if(phase==0){phase=1;submit(mc,()->{var p=player(mc);clean(p);var cultist=(CultistEntity)mob(p,scene==14?"cultist_knight":"cultist_cleric",-1.5,112,-1.5);
            cultist.finalizeSpawn(p.serverLevel(),p.serverLevel().getCurrentDifficultyAt(cultist.blockPosition()),MobSpawnType.MOB_SUMMONED,null,null);
            var cow=cow(p,scene==14?-.5:1.5,scene==14?-2.5:-9);cultist.setTarget(cow);actor=cultist.getId();target=cow.getId();if(scene==14)view(p,6.5,4);else viewAt(p,6,3,new Vec3(-.5,113,-4.5));});return false;}
        if(scene==14){
            Entity cow=mc.level.getEntity(target);if(cow instanceof Cow c&&c.hurtTime>0){
                if(!verified){submit(mc,()->{var p=player(mc);require(((Cow)p.serverLevel().getEntity(target)).getHealth()<100,"Native knight target was not damaged");verified=true;LogUtils.getLogger().info("THAUMCRAFT_MOBS_KNIGHT_MELEE_OK: actualMeleeGoal/worldTicks/noDirectAttackCall");});return false;}return true;
            }return false;
        }
        return captureOwnedProjectile(mc,"THAUMCRAFT_MOBS_CLERIC_PROJECTILE_OK");
    }
    private static boolean taint(Minecraft mc){
        if(phase==0){phase=1;submit(mc,()->{var p=player(mc);clean(p);String[] names={"thaumic_slime","taint_crawler","taint_swarm","taintacle","taintacle_giant"};
            var seed=mob(p,"taint_seed",12.5,112,-7);seed.setNoAi(true);
            var m=mob(p,names[scene-16],.5,112+(scene==18?1.8:0),-1.5);m.setNoAi(true);actor=m.getId();view(p,scene==20?11.5:6.5,scene==20?-7:scene==18?-2:6);
            LogUtils.getLogger().info("THAUMCRAFT_MOBS_TAINT_FIXTURE: {} nativeFactory/rawNoAIActor/nearbyRawSeed/notNaturalAcquisition/notGiantChampionFinalize",names[scene-16]);
        });return false;}
        if(!(actor(mc) instanceof VisualMobEntity visible))return false;
        if(scene==20&&visible.tickCount<Math.ceil(visible.getBbHeight()*10)+5)return false;
        return settled(mc,scene==18?45:20);
    }
    private static boolean goo(Minecraft mc){
        if(phase==0){phase=1;submit(mc,()->{var p=player(mc);clean(p);
            for(int x:new int[]{-2,2}){
                for(var q:BlockPos.betweenClosed(x-1,112,-4,x+1,113,-2))p.serverLevel().setBlockAndUpdate(q,Blocks.GLASS.defaultBlockState());
                p.serverLevel().setBlockAndUpdate(new BlockPos(x,112,-3),thaumcraft.world.taint.TaintModule.block("flux_goo").defaultBlockState().setValue(LiquidBlock.LEVEL,x<0?7:3));
            }view(p,4.5,14);LogUtils.getLogger().info("THAUMCRAFT_MOBS_GOO_FIXTURE: sealedGlassContainers/initial8vs4quanta/nativeFiniteFluidHeight/notNaturalSpreadClaim");
        });return false;}return settled(mc,45);
    }
    private static boolean bottle(Minecraft mc){
        if(scene==22){
            if(phase==0){phase=1;submit(mc,()->{var p=player(mc);clean(p);p.setGameMode(GameType.SURVIVAL);held(p,new ItemStack(ForgeRegistries.ITEMS.getValue(id("bottle_taint")),3));target=cow(p,.5,-7.5).getId();view(p,4.5,-5);});return false;}
            if(phase==1){if(!mc.player.getMainHandItem().is(ForgeRegistries.ITEMS.getValue(id("bottle_taint")))||mc.player.getMainHandItem().getCount()!=3)return false;
                mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);phase=2;actionTick=mc.level.getGameTime();return false;}
            require(!elapsed(mc,120),"Native Bottle did not enter flight");
            diagnoseFlight(mc);
            if(phase==2){for(Entity e:mc.level.entitiesForRendering())if(ForgeRegistries.ENTITY_TYPES.getKey(e.getType()).equals(id("bottle_taint"))){
                projectile=e.getId();phase=3;submit(mc,()->{var p=player(mc);require(p.serverLevel().getEntity(projectile) instanceof Projectile shot&&shot.getOwner()==p,"Native Bottle ownership absent before side-camera move");viewAt(p,5.5,.5,new Vec3(.5,113,-4.5));LogUtils.getLogger().info("THAUMCRAFT_MOBS_BOTTLE_CAMERA_FIXTURE: sideCameraOnlyAfterActualC2SSpawn/noMotionAgeOrThrowParameterChange");});return false;
            }return false;}
            return mc.level.getEntity(projectile) instanceof Projectile shot&&visibleFlight(mc,shot);
        }
        if(phase==0){phase=1;verified=false;actionTick=mc.level.getGameTime();}
        require(!elapsed(mc,120),"Native Bottle impact/effect/debit not observed");
        if(!verified){submit(mc,()->{var p=player(mc);var cow=(Cow)p.serverLevel().getEntity(target);
            boolean effect=cow.getActiveEffects().stream().anyMatch(e->ForgeRegistries.MOB_EFFECTS.getKey(e.getEffect()).getPath().equals("flux_taint"));
            if(effect&&p.getInventory().getItem(0).getCount()==2){verified=true;LogUtils.getLogger().info("THAUMCRAFT_MOBS_BOTTLE_IMPACT_OK: actualNativeC2SUse/3to2/physicalProjectile/cowFluxTaint/noDirectImpactOrEffectCall");}
        });return false;}return mc.level.getEntity(projectile)==null&&elapsed(mc,1);
    }
    private static void prepare(ServerPlayer p){
        var level=p.serverLevel();for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)level.getChunk(x,z);
        for(var q:BlockPos.betweenClosed(-20,112,-20,20,127,37))level.setBlock(q,Blocks.AIR.defaultBlockState(),2);
        for(var q:BlockPos.betweenClosed(-20,111,-20,20,111,37))level.setBlock(q,Blocks.BEDROCK.defaultBlockState(),2);
        for(var q:BlockPos.betweenClosed(-20,112,-19,20,124,-19))level.setBlock(q,Blocks.STONE.defaultBlockState(),2);
        for(var q:BlockPos.betweenClosed(-20,127,-20,20,127,37))level.setBlock(q,Blocks.GLASS.defaultBlockState(),2);
        level.setDayTime(6000);p.getInventory().clearContent();view(p,4.5,6);held(p,ItemStack.EMPTY);
        LogUtils.getLogger().info("THAUMCRAFT_MOBS_FIXTURES: ownedNewWorld/rawActorsGearTargetsMaterials/realPechDropPacketsMenuAndDice/realNativeCombatGoals/initialNoAIComparativeActors/glassSunRoof/containedRawGoo/realSurvivalBottleUse/notPaidResearch/notNaturalWorldDiscovery/noInjectedEffectsAgesOrRng");
    }
    @SubscribeEvent public static void rendered(TickEvent.RenderTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!enabled()||stopped||!captureRequested||captured)return;var mc=Minecraft.getInstance();
        try{
            require(mc.getWindow().getScreenWidth()==1600&&mc.getWindow().getScreenHeight()==1000,"Expected1600x1000 native capture");
            if(scene==4||scene==5||scene==15||scene==22){
                if(!(mc.level.getEntity(projectile) instanceof Projectile shot)||!visibleFlight(mc,shot)){captureRequested=false;return;}
                LogUtils.getLogger().info("THAUMCRAFT_MOBS_PROJECTED_FLIGHT: scene={} id={} type={} nativeAge={} position={} actualFrameNdc={} camera={} nativeMotion={} ownerBounds={}",scene,projectile,ForgeRegistries.ENTITY_TYPES.getKey(shot.getType()),shot.tickCount,shot.position(),projected(shot.getBoundingBox().getCenter()),frameCamera,shot.getDeltaMovement(),shot.getOwner().getBoundingBox());
            }
            captured=true;captureRequested=false;stable=0;String name="tc6-mobs-"+IMAGES[scene]+".png";
            File file=new File(new File(mc.gameDirectory,"screenshots"),name);require(!file.exists(),"Preserve existing capture "+name);
            LogUtils.getLogger().info("THAUMCRAFT_MOBS_NATIVE_FRAME: name={} camera={} yaw={} pitch={} actor={} target={} projectile={} screen={}",name,
                    mc.gameRenderer.getMainCamera().getPosition(),mc.player.getYRot(),mc.player.getXRot(),actor,target,projectile,mc.screen==null?"world":mc.screen.getClass().getSimpleName());
            Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),m->{if(file.isFile()&&file.length()>0){SAVED.incrementAndGet();LogUtils.getLogger().info("THAUMCRAFT_MOBS_CLIENT_SMOKE_IMAGE: {}",file.getAbsolutePath());}else mc.execute(()->fail(mc,new AssertionError("Screenshot absent")));});
        }catch(Throwable e){fail(mc,e);}
    }
    private static void start(Minecraft mc){
        if(mc.screen instanceof AccessibilityOnboardingScreen){mc.options.onboardAccessibility=false;mc.options.save();mc.setScreen(new TitleScreen());return;}
        if(!(mc.screen instanceof TitleScreen)||mc.getOverlay()!=null)return;championFixture=new thaumcraft.test.fixtures.ChampionFixture(mc.gameDirectory.toPath());started=true;previous=mc.options.tutorialStep;mc.options.tutorialStep=TutorialSteps.NONE;mc.getTutorial().stop();
        mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(4);mc.options.simulationDistance().set(5);mc.options.guiScale().set(2);mc.options.fov().set(70);mc.options.bobView().set(false);mc.options.cloudStatus().set(CloudStatus.OFF);mc.options.setCameraType(CameraType.FIRST_PERSON);mc.options.hideGui=true;mc.resizeDisplay();
        var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);rules.getRule(GameRules.RULE_DAYLIGHT).set(false,null);rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false,null);rules.getRule(GameRules.RULE_RANDOMTICKING).set(0,null);
        mc.createWorldOpenFlows().createFreshLevel(WORLD,new LevelSettings(WORLD,GameType.CREATIVE,false,net.minecraft.world.Difficulty.NORMAL,true,rules,WorldDataConfiguration.DEFAULT),new WorldOptions(0x544336042L,false,false),WorldPresets::createNormalWorldDimensions);
    }
    private static void finish(Minecraft mc){
        if(phase==0){phase=1;submit(mc,()->{var p=player(mc);require(p.getInventory().getItem(0).getCount()==2,"Final Bottle debit drifted");clean(p);verified=true;});return;}
        if(!verified)return;require(SAVED.get()==IMAGES.length,"Incomplete native images");if(championFixture!=null){championFixture.close();championFixture=null;}stopped=true;mc.options.tutorialStep=previous;
        LogUtils.getLogger().info("THAUMCRAFT_MOBS_RENDER_AUDIT_OK: 24captures/actualRawActors/PechNativeTamingPackMenuDice/FirebatHangingFlight/BrainyGiantAnger/PortalNativeClockMinions/CultistNativeCombat/TaintNativePresentation/ContainedFiniteGoo/BottleC2SFlightImpact/actualPixelsRequireReview");
        LogUtils.getLogger().info("THAUMCRAFT_MOBS_CLIENT_SMOKE_OK: 24 captures/rawPrerequisitesNotFullSurvival,world={}",WORLD);mc.stop();
    }
    private static void fail(Minecraft mc,Throwable e){if(stopped)return;stopped=true;if(championFixture!=null){try{championFixture.close();}catch(Throwable restore){e.addSuppressed(restore);}championFixture=null;}if(previous!=null)mc.options.tutorialStep=previous;LogUtils.getLogger().error("THAUMCRAFT_MOBS_CLIENT_SMOKE_FAILED",e);mc.stop();}
}
