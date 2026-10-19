package thaumcraft.equipment.tools.client;

import com.mojang.logging.LogUtils;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thaumcraft.common.lib.enchantment.EnumInfusionEnchantment;
import thaumcraft.equipment.tools.ToolClientEvents;

/**
 * Opt-in visual regression fixture, not a paid progression route. Real sneak/use
 * packets create ordinary native ripples behind an opaque wall; reflection reads
 * their state only and never supplies, edits or ages an effect.
 */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public final class SoundingClientSmokeTest {
    private static final String WORLD="thaumcraft-sounding-smoke-"+System.currentTimeMillis();
    private static final String[] IMAGES={"fancy-before","fancy-active-early","fancy-active-late","fancy-after",
            "fabulous-before","fabulous-active-early","fabulous-active-late","fabulous-after"};
    private static final BlockPos TARGET=new BlockPos(0,113,0);
    private static final BlockPos[] COAL={new BlockPos(-5,114,-2),new BlockPos(-4,114,-2)};
    private static final BlockPos[] DIAMOND={new BlockPos(0,114,-2),new BlockPos(0,115,-2)};
    private static final BlockPos[] IRON={new BlockPos(4,114,-2),new BlockPos(5,114,-2)};
    private static final BlockPos EXCLUDED=new BlockPos(9,114,-2);
    private static final AtomicInteger SAVED=new AtomicInteger();
    private static CompletableFuture<Void> work;
    private static TutorialSteps previous;
    private static boolean started,prepared,stopped,captureRequested,captured,validated;
    private static int scene,phase,wait,stable,nativeUses;
    private static long began,actionTick;
    private SoundingClientSmokeTest(){}
    private static boolean enabled(){return Boolean.getBoolean("thaumcraft.soundingSmokeTest");}
    private static void require(boolean value,String text){if(!value)throw new AssertionError(text);}
    private static ServerPlayer player(Minecraft mc){var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());require(p!=null,"Owned player missing");return p;}
    private static void submit(Minecraft mc,Runnable action){require(work==null,"Overlapping owned server work");var f=new CompletableFuture<Void>();work=f;mc.getSingleplayerServer().execute(()->{try{action.run();f.complete(null);}catch(Throwable e){f.completeExceptionally(e);}});}

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!enabled()||stopped)return;
        var mc=Minecraft.getInstance();
        if(began==0)began=System.nanoTime();
        try{
            require(System.nanoTime()-began<600_000_000_000L,"Sounding visual regression timeout scene="+scene+" phase="+phase);
            if(!started){start(mc);return;}
            if(mc.level==null||mc.player==null||mc.getOverlay()!=null||mc.screen instanceof ReceivingLevelScreen)return;
            require(mc.getSingleplayerServer()!=null&&WORLD.equals(mc.getSingleplayerServer().getWorldData().getLevelName()),"Wrong owned world");
            mc.getToasts().clear();
            if(++wait%200==0)LogUtils.getLogger().info("THAUMCRAFT_SOUNDING_HEARTBEAT: scene={} phase={} uses={} ripples={}",scene,phase,nativeUses,ripples());
            if(work!=null){if(!work.isDone())return;work.join();work=null;}
            if(!prepared){prepared=true;submit(mc,()->prepare(mc));return;}
            if(captured){
                if(SAVED.get()==scene+1&&++stable>=2){scene++;phase=0;stable=0;captured=captureRequested=false;}
                return;
            }
            if(scene==IMAGES.length){finish(mc);return;}
            if(advance(mc))captureRequested=true;
        }catch(Throwable error){fail(mc,error);}
    }

    private static boolean advance(Minecraft mc){
        int local=scene%4;
        if(local==0){
            if(phase==0){
                mc.options.graphicsMode().set(scene<4?GraphicsStatus.FANCY:GraphicsStatus.FABULOUS);
                mc.levelRenderer.allChanged();
                mc.resizeDisplay();
                mc.options.keyShift.setDown(true);
                phase=1;
                actionTick=mc.level.getGameTime();
                return false;
            }
            if(mc.level.getGameTime()-actionTick<30||!mc.player.isShiftKeyDown()
                    ||mc.player.distanceToSqr(.5,112,4.5)>.001
                    ||!mc.player.getMainHandItem().is(Items.DIAMOND_PICKAXE))return false;
            require(ripples().isEmpty(),"Before image retained an earlier native ripple");
            require(mc.options.graphicsMode().get()==(scene<4?GraphicsStatus.FANCY:GraphicsStatus.FABULOUS),"Requested graphics mode absent");
            require(mc.player.getMainHandItem().getDamageValue()==(scene/4)*5,"Unexpected pre-use durability");
            LogUtils.getLogger().info("THAUMCRAFT_SOUNDING_NATIVE_BEFORE: graphics={} camera={} yaw={} pitch={} fov={} solidWall=true nativeRipples0 damage={}",
                    mc.options.graphicsMode().get(),mc.gameRenderer.getMainCamera().getPosition(),mc.player.getYRot(),mc.player.getXRot(),mc.options.fov().get(),mc.player.getMainHandItem().getDamageValue());
            return true;
        }
        if(local==1){
            if(phase==0){
                require(mc.player.isShiftKeyDown(),"Native use needs acknowledged sneaking");
                require(ripples().isEmpty(),"Previous native ripples not expired");
                mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,
                        new BlockHitResult(new Vec3(.5,113.5,1),Direction.SOUTH,TARGET,false));
                nativeUses++;phase=1;
                return false;
            }
            if(mc.player.getMainHandItem().getDamageValue()!=nativeUses*5)return false;
            var observed=ripples();
            if(observed.size()!=3)return false;
            if(observed.stream().anyMatch(r->r.delay!=0||r.age<12||r.age>26))return false;
            require(observed.stream().filter(r->r.dark).count()==1,"Native coal normal-alpha group absent");
            require(observed.stream().filter(r->!r.dark).count()==2,"Native additive diamond/iron groups absent");
            require(observed.stream().noneMatch(r->r.position.x()>8),"Outside rank1 cube ore was included");
            if(phase==1){
                phase=2;
                submit(mc,()->{
                    var p=player(mc);
                    require(p.isShiftKeyDown()&&p.getMainHandItem().getDamageValue()==nativeUses*5,"Native server durability/sneak acknowledgment absent");
                    unchanged(p);
                    LogUtils.getLogger().info("THAUMCRAFT_SOUNDING_NATIVE_USE: mode={}/rawRank1VanillaDiamondPickaxe/actualSneakUseC2S/one5Durability/nativeS2C3Groups/coalAlpha1/additive2/noTerrainMutation/noInjectedRipples",scene<4?"FANCY":"FABULOUS");
                });
                return false;
            }
            return true;
        }
        if(local==2){
            var observed=ripples();
            if(observed.size()!=3||observed.stream().anyMatch(r->r.delay!=0||r.age<18||r.age>32))return false;
            require(mc.player.getMainHandItem().getDamageValue()==nativeUses*5,"Native use was billed twice");
            return true;
        }
        if(!ripples().isEmpty())return false;
        require(mc.player.getMainHandItem().getDamageValue()==nativeUses*5,"Post-effect durability changed");
        return ++stable>=8;
    }

    private static void prepare(Minecraft mc){
        var p=player(mc);var l=p.serverLevel();
        p.setInvulnerable(true);
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)l.getChunk(x,z);
        for(var pos:BlockPos.betweenClosed(-13,110,-13,13,123,10))
            l.setBlockAndUpdate(pos,pos.getY()==111?Blocks.SMOOTH_STONE.defaultBlockState():Blocks.AIR.defaultBlockState());
        for(var pos:BlockPos.betweenClosed(-12,112,-6,12,120,0))
            l.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());
        for(var pos:COAL)l.setBlockAndUpdate(pos,Blocks.COAL_ORE.defaultBlockState());
        for(var pos:DIAMOND)l.setBlockAndUpdate(pos,Blocks.DIAMOND_ORE.defaultBlockState());
        for(var pos:IRON)l.setBlockAndUpdate(pos,Blocks.IRON_ORE.defaultBlockState());
        l.setBlockAndUpdate(EXCLUDED,Blocks.EMERALD_ORE.defaultBlockState());
        l.setDayTime(6000);
        p.getInventory().clearContent();
        var pick=new ItemStack(Items.DIAMOND_PICKAXE);
        EnumInfusionEnchantment.addInfusionEnchantment(pick,EnumInfusionEnchantment.SOUNDING,1);
        p.getInventory().selected=0;p.setItemInHand(InteractionHand.MAIN_HAND,pick);
        p.connection.send(new ClientboundSetCarriedItemPacket(0));
        p.connection.teleport(.5,112,4.5,180,0);
        p.inventoryMenu.broadcastChanges();
        unchanged(p);
        LogUtils.getLogger().info("THAUMCRAFT_SOUNDING_FIXTURES: isolatedSurvivalWorld/rawRank1DiamondPick/opaqueStoneWallX-12to12Y112to120Z-6to0/cameraPlayer0.5,112,4.5Yaw180Pitch0Fov80/click0,113,0/rank1Cube8/coalCenter-4,114.5,-1.5/diamondCenter0.5,115,-1.5/ironCenter5,114.5,-1.5/excludedEmerald9,114,-2/noActiveEffectClockOrRenderFlagInjection/notPaidProgression");
    }
    private static void unchanged(ServerPlayer p){
        var l=p.serverLevel();
        require(l.getBlockState(TARGET).is(Blocks.STONE),"Native Sounding changed clicked wall");
        for(var pos:COAL)require(l.getBlockState(pos).is(Blocks.COAL_ORE),"Native Sounding changed coal");
        for(var pos:DIAMOND)require(l.getBlockState(pos).is(Blocks.DIAMOND_ORE),"Native Sounding changed diamond");
        for(var pos:IRON)require(l.getBlockState(pos).is(Blocks.IRON_ORE),"Native Sounding changed iron");
        require(l.getBlockState(EXCLUDED).is(Blocks.EMERALD_ORE),"Excluded ore fixture changed");
        for(var pos:BlockPos.betweenClosed(-12,112,0,12,120,0))require(l.getBlockState(pos).is(Blocks.STONE),"Opaque front wall was opened");
    }
    private record ReadOnlyRipple(Vec3 position,int color,boolean dark,int delay,int age){}
    private static Object field(Object target,String name)throws ReflectiveOperationException{
        Field field=target.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(target);
    }
    private static List<ReadOnlyRipple> ripples(){
        try{
            var field=ToolClientEvents.class.getDeclaredField("RIPPLES");field.setAccessible(true);
            var result=new ArrayList<ReadOnlyRipple>();
            for(var r:(List<?>)field.get(null))
                result.add(new ReadOnlyRipple((Vec3)field(r,"position"),(int)field(r,"color"),(boolean)field(r,"dark"),(int)field(r,"delay"),(int)field(r,"age")));
            return result;
        }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
    }
    @SubscribeEvent public static void rendered(TickEvent.RenderTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!enabled()||stopped||!captureRequested||captured)return;
        var mc=Minecraft.getInstance();
        try{
            require(mc.getWindow().getScreenWidth()==1600&&mc.getWindow().getScreenHeight()==1000,"Expected full1600x1000 capture");
            require(mc.player.distanceToSqr(.5,112,4.5)<.001&&mc.player.getYRot()==180&&mc.player.getXRot()==0,"Matched capture camera moved");
            captured=true;captureRequested=false;stable=0;
            String name="tc6-sounding-"+IMAGES[scene]+".png";
            var file=new File(new File(mc.gameDirectory,"screenshots"),name);
            require(!file.exists(),"Refusing screenshot overwrite "+name);
            LogUtils.getLogger().info("THAUMCRAFT_SOUNDING_NATIVE_FRAME: name={} graphics={} camera={} yaw={} pitch={} damage={} ripples={}",
                    name,mc.options.graphicsMode().get(),mc.gameRenderer.getMainCamera().getPosition(),mc.player.getYRot(),mc.player.getXRot(),
                    mc.player.getMainHandItem().getDamageValue(),ripples());
            Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),msg->{
                if(file.isFile()&&file.length()>0){SAVED.incrementAndGet();LogUtils.getLogger().info("THAUMCRAFT_SOUNDING_CLIENT_SMOKE_IMAGE: {}",file.getAbsolutePath());}
                else mc.execute(()->fail(mc,new AssertionError("Native screenshot missing")));
            });
        }catch(Throwable error){fail(mc,error);}
    }
    private static void start(Minecraft mc){
        if(mc.screen instanceof AccessibilityOnboardingScreen){mc.options.onboardAccessibility=false;mc.options.save();mc.setScreen(new TitleScreen());return;}
        if(!(mc.screen instanceof TitleScreen)||mc.getOverlay()!=null)return;
        started=true;previous=mc.options.tutorialStep;mc.options.tutorialStep=TutorialSteps.NONE;mc.getTutorial().stop();
        mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(4);mc.options.simulationDistance().set(5);
        mc.options.guiScale().set(2);mc.options.fov().set(80);mc.options.bobView().set(false);
        mc.options.cloudStatus().set(CloudStatus.OFF);mc.options.setCameraType(CameraType.FIRST_PERSON);mc.options.hideGui=false;mc.resizeDisplay();
        var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);
        rules.getRule(GameRules.RULE_DAYLIGHT).set(false,null);rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false,null);
        rules.getRule(GameRules.RULE_RANDOMTICKING).set(0,null);
        mc.createWorldOpenFlows().createFreshLevel(WORLD,
                new LevelSettings(WORLD,GameType.SURVIVAL,false,Difficulty.NORMAL,true,rules,WorldDataConfiguration.DEFAULT),
                new WorldOptions(0x544336401L,false,false),WorldPresets::createNormalWorldDimensions);
    }
    private static void finish(Minecraft mc){
        if(phase==0){phase=1;submit(mc,()->{
            var p=player(mc);unchanged(p);
            require(nativeUses==2&&p.getMainHandItem().getDamageValue()==10,"Two native5durability uses did not conserve exact cost");
            require(EnumInfusionEnchantment.getInfusionEnchantmentLevel(p.getMainHandItem(),EnumInfusionEnchantment.SOUNDING)==1,"Raw rank1 tool changed");
            validated=true;
        });return;}
        if(!validated)return;
        require(SAVED.get()==8&&ripples().isEmpty(),"Native before/active/expired captures incomplete");
        stopped=true;mc.options.keyShift.setDown(false);mc.options.tutorialStep=previous;
        LogUtils.getLogger().info("THAUMCRAFT_SOUNDING_RENDER_AUDIT_OK: 8 captures/FancyAndFabulous/threeNativeHiddenOreGroups/oneCoalAlphaTwoAdditive/closedWallBeforeActiveEarlyActiveLateAfter/readOnlyNativeFacts/actualPixelsRequireIndependentReview");
        LogUtils.getLogger().info("THAUMCRAFT_SOUNDING_CLIENT_SMOKE_OK: 8 captures/2actualSneakUseC2S/nativeS2C3GroupsEach/exact10Durability/noTerrainMutation/noInjectedRipples/notPaidProgression,world={}",WORLD);
        mc.stop();
    }
    private static void fail(Minecraft mc,Throwable error){
        if(stopped)return;stopped=true;mc.options.keyShift.setDown(false);if(previous!=null)mc.options.tutorialStep=previous;
        LogUtils.getLogger().error("THAUMCRAFT_SOUNDING_CLIENT_SMOKE_FAILED",error);mc.stop();
    }
}

