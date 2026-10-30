package thaumcraft.world.warp.client;

import com.mojang.logging.LogUtils;
import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.fml.common.Mod;
import thaumcraft.world.warp.*;

/** Actual post-chain rendering in a new isolated world. Initial potion effects/private
 * message are declared fixtures; this does not simulate the random Warp scheduler. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public final class WarpClientSmokeTest {
    private static final String WORLD="thaumcraft-warp-"+System.currentTimeMillis();
    private static final String[] NAMES={"gaze-before","gaze-active","gaze-after","blur-before","blur-active","blur-after",
        "hunger-before","hunger-active","hunger-after","sun-before","sun-active","sun-after","mist-before","mist-active","mist-after"};
    private static final AtomicInteger SAVED=new AtomicInteger();
    private static boolean started,prepared,stopped,want,captured;
    private static int scene,phase,stable,heartbeat;
    private static long began,at;
    private static CompletableFuture<Void> work;
    private static TutorialSteps previous;
    private WarpClientSmokeTest(){}
    private static void require(boolean ok,String detail){if(!ok)throw new AssertionError(detail);}
    private static MobEffect effect(int index){return new MobEffect[]{WarpModule.DEATH_GAZE.get(),WarpModule.BLURRED_VISION.get(),WarpModule.UNNATURAL_HUNGER.get(),WarpModule.SUN_SCORNED.get()}[index];}
    private static ServerPlayer player(Minecraft mc){return mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());}
    private static void submit(Minecraft mc,Runnable action){
        require(work==null,"Overlapping Warp callback");var future=new CompletableFuture<Void>();work=future;
        mc.getSingleplayerServer().execute(()->{try{action.run();future.complete(null);}catch(Throwable e){future.completeExceptionally(e);}});
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!Boolean.getBoolean("thaumcraft.warpSmokeTest")||stopped)return;
        var mc=Minecraft.getInstance();if(began==0)began=System.nanoTime();
        try{
            require(System.nanoTime()-began<600_000_000_000L,"Warp smoke timeout scene="+scene+" phase="+phase);
            if(!started){start(mc);return;}
            if(mc.level==null||mc.player==null||mc.getOverlay()!=null||mc.screen instanceof ReceivingLevelScreen)return;
            require(mc.getSingleplayerServer()!=null&&WORLD.equals(mc.getSingleplayerServer().getWorldData().getLevelName()),"Wrong isolated world");
            require(!WarpClient.hasFailedShader(),"Native Warp shader compilation/render failed");mc.getToasts().clear();
            if(++heartbeat%200==0)LogUtils.getLogger().info("THAUMCRAFT_WARP_HEARTBEAT: scene={} phase={} chains={} mistDuration={} density={}",scene,phase,WarpClient.activeShaders(),WarpClient.fogDuration(),WarpClient.fogDensity());
            if(work!=null){if(!work.isDone())return;work.join();work=null;}
            if(!prepared){prepared=true;submit(mc,()->prepare(player(mc)));return;}
            if(captured){if(SAVED.get()==scene+1&&++stable>=3){scene++;phase=0;stable=0;want=captured=false;}return;}
            if(scene==NAMES.length){finish(mc);return;}
            if(advance(mc))want=true;
        }catch(Throwable e){fail(mc,e);}
    }
    private static boolean advance(Minecraft mc){
        if(scene<12){
            int index=scene/3,step=scene%3;
            if(step==0){
                if(phase==0){phase=1;at=mc.level.getGameTime();require(!mc.player.hasEffect(effect(index))&&WarpClient.activeShaders()==0,"Before control has an active shader");}
                if(scene==0)return mc.level.getGameTime()-at>=200
                    &&mc.levelRenderer.isChunkCompiled(new BlockPos(-8,111,-12))&&mc.levelRenderer.isChunkCompiled(new BlockPos(7,120,-12))
                    &&mc.levelRenderer.isChunkCompiled(new BlockPos(0,110,0))&&mc.level.getBrightness(net.minecraft.world.level.LightLayer.SKY,new BlockPos(0,113,5))>=12;
                return mc.level.getGameTime()-at>=10;
            }
            if(step==1){
                if(phase==0){phase=1;submit(mc,()->{var p=player(mc);p.addEffect(new MobEffectInstance(effect(index),80,0));LogUtils.getLogger().info("THAUMCRAFT_WARP_EFFECT_FIXTURE: {} initial80ticks/naturalExpiry/actualS2C/nativePostChain",index);});return false;}
                if(phase==1){if(!mc.player.hasEffect(effect(index))||WarpClient.activeShaders()!=1)return false;phase=2;at=mc.level.getGameTime();}
                return mc.player.hasEffect(effect(index))&&WarpClient.activeShaders()==1&&mc.level.getGameTime()-at>=8;
            }
            if(phase==0){phase=1;at=mc.level.getGameTime();}
            require(mc.level.getGameTime()-at<160,"Potion post-chain did not naturally clear");
            return !mc.player.hasEffect(effect(index))&&WarpClient.activeShaders()==0;
        }
        if(scene==12){
            if(phase==0){phase=1;at=mc.level.getGameTime();require(WarpClient.fogDuration()==0&&WarpClient.fogDensity()==0,"Mist before control is not clear");}
            return mc.level.getGameTime()-at>=8;
        }
        if(scene==13){
            if(phase==0){phase=1;submit(mc,()->WarpNetwork.send(player(mc),2));return false;}
            return WarpClient.fogDuration()>100&&WarpClient.fogDensity()>=.055F;
        }
        if(phase==0){phase=1;at=mc.level.getGameTime();}
        require(mc.level.getGameTime()-at<260,"Original private mist did not naturally fade");
        return WarpClient.fogDuration()==0&&WarpClient.fogDensity()==0;
    }
    private static void prepare(ServerPlayer p){
        var level=p.serverLevel();for(int x=-2;x<=2;x++)for(int z=-3;z<=1;z++)level.getChunk(x,z);
        for(var q:BlockPos.betweenClosed(-16,111,-35,16,125,12))level.setBlock(q,Blocks.AIR.defaultBlockState(),2);
        for(var q:BlockPos.betweenClosed(-16,110,-35,16,110,12))level.setBlock(q,Blocks.QUARTZ_BLOCK.defaultBlockState(),2);
        Block[] colors={Blocks.RED_WOOL,Blocks.ORANGE_WOOL,Blocks.YELLOW_WOOL,Blocks.LIME_WOOL,Blocks.CYAN_WOOL,Blocks.BLUE_WOOL,Blocks.PURPLE_WOOL,Blocks.MAGENTA_WOOL};
        for(int i=0;i<8;i++)for(var q:BlockPos.betweenClosed(-8+i*2,111,-12,-7+i*2,120,-12))level.setBlock(q,colors[i].defaultBlockState(),2);
        for(int z:new int[]{-5,-19,-30})for(var q:BlockPos.betweenClosed(-12,111,z,-11,119,z))level.setBlock(q,Blocks.GOLD_BLOCK.defaultBlockState(),2);
        for(var q:BlockPos.betweenClosed(-16,124,-35,16,124,12))level.setBlock(q,Blocks.GLASS.defaultBlockState(),2);
        level.setDayTime(6000);p.setInvulnerable(true);p.getInventory().clearContent();p.getInventory().selected=0;
        p.getInventory().setItem(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.MELON_SLICE));p.inventoryMenu.broadcastChanges();
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(0));p.connection.teleport(.5,111,5.5,180,0);
        LogUtils.getLogger().info("THAUMCRAFT_WARP_FIXTURES: ownedNewWorld/rawColorDepthArena/rawInitial80tickPotions/nativePrivateMessage2/original200tickMist/realNaturalExpiry/noFxInjection/noActiveClockOrDensityMutation/noRandomWarpFrequencyClaim");
    }
    @SubscribeEvent public static void rendered(TickEvent.RenderTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!Boolean.getBoolean("thaumcraft.warpSmokeTest")||stopped||!want||captured)return;
        var mc=Minecraft.getInstance();
        try{
            require(mc.getWindow().getScreenWidth()==1600&&mc.getWindow().getScreenHeight()==1000,"Expected1600x1000 capture");
            require(!WarpClient.hasFailedShader(),"Post-chain failed before actual capture");captured=true;want=false;stable=0;
            String name="tc6-warp-"+NAMES[scene]+".png";var file=new File(new File(mc.gameDirectory,"screenshots"),name);require(!file.exists(),"Preserve existing "+name);
            LogUtils.getLogger().info("THAUMCRAFT_WARP_NATIVE_FRAME: name={} camera={} yaw={} pitch={} chains={} mist={} density={}",name,mc.gameRenderer.getMainCamera().getPosition(),mc.player.getYRot(),mc.player.getXRot(),WarpClient.activeShaders(),WarpClient.fogDuration(),WarpClient.fogDensity());
            auditNativeUniforms();
            Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),m->{if(file.isFile()&&file.length()>0){SAVED.incrementAndGet();LogUtils.getLogger().info("THAUMCRAFT_WARP_CLIENT_SMOKE_IMAGE: {}",file.getAbsolutePath());}else mc.execute(()->fail(mc,new AssertionError("Screenshot absent")));});
        }catch(Throwable e){fail(mc,e);}
    }
    private static void auditNativeUniforms()throws ReflectiveOperationException{
        var field=WarpClient.class.getDeclaredField("CHAINS");field.setAccessible(true);
        var passes=net.minecraft.client.renderer.PostChain.class.getDeclaredField("passes");passes.setAccessible(true);
        for(var entry:((java.util.Map<?,?>)field.get(null)).entrySet())for(var value:(java.util.List<?>)passes.get(entry.getValue())){
            var effect=((net.minecraft.client.renderer.PostPass)value).getEffect();
            for(String key:new String[]{"InSize","OutSize","BlurDir","Radius","Saturation"}){
                var uniform=effect.getUniform(key);if(uniform==null)continue;var floats=uniform.getFloatBuffer();if(floats==null)continue;
                var gpu=new float[floats.capacity()];org.lwjgl.opengl.GL20.glGetUniformfv(effect.getId(),uniform.getLocation(),gpu);
                var cpu=new float[floats.capacity()];for(int i=0;i<cpu.length;i++)cpu[i]=floats.get(i);
                LogUtils.getLogger().info("THAUMCRAFT_WARP_NATIVE_UNIFORM: scene={} chain={} program={} uniform={} cpu={} gpu={}",scene,entry.getKey(),effect.getId(),key,java.util.Arrays.toString(cpu),java.util.Arrays.toString(gpu));
            }
        }
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void beforeGui(RenderGuiEvent.Pre event){sampleColor("before-gui-post");}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void afterGui(RenderGuiEvent.Pre event){sampleColor("after-gui-post");}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void beforeMist(RenderLevelStageEvent event){if(event.getStage()==RenderLevelStageEvent.Stage.AFTER_LEVEL)sampleColor("before-world-post");}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void afterMist(RenderLevelStageEvent event){if(event.getStage()==RenderLevelStageEvent.Stage.AFTER_LEVEL)sampleColor("after-world-post");}
    private static void sampleColor(String phase){
        if(!Boolean.getBoolean("thaumcraft.warpSmokeTest")||!want||captured||stopped||(scene!=7&&scene!=13))return;
        for(int[] point:new int[][]{{1300,500},{1080,320},{1450,850}}){
            var pixel=org.lwjgl.BufferUtils.createByteBuffer(4);
            org.lwjgl.opengl.GL11.glReadPixels(point[0],1000-point[1]-1,1,1,org.lwjgl.opengl.GL11.GL_RGBA,org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE,pixel);
            LogUtils.getLogger().info("THAUMCRAFT_WARP_NATIVE_RGBA: scene={} phase={} x={} y={} rgba={},{},{},{}",scene,phase,point[0],point[1],pixel.get(0)&255,pixel.get(1)&255,pixel.get(2)&255,pixel.get(3)&255);
        }
    }
    private static void start(Minecraft mc){
        if(mc.screen instanceof AccessibilityOnboardingScreen){mc.options.onboardAccessibility=false;mc.options.save();mc.setScreen(new TitleScreen());return;}
        if(!(mc.screen instanceof TitleScreen)||mc.getOverlay()!=null)return;started=true;previous=mc.options.tutorialStep;mc.options.tutorialStep=TutorialSteps.NONE;mc.getTutorial().stop();
        mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(4);mc.options.simulationDistance().set(5);mc.options.guiScale().set(2);mc.options.fov().set(70);mc.options.bobView().set(false);mc.options.cloudStatus().set(CloudStatus.OFF);mc.options.setCameraType(CameraType.FIRST_PERSON);mc.options.hideGui=false;mc.resizeDisplay();
        var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);rules.getRule(GameRules.RULE_DAYLIGHT).set(false,null);rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false,null);rules.getRule(GameRules.RULE_RANDOMTICKING).set(0,null);
        mc.createWorldOpenFlows().createFreshLevel(WORLD,new LevelSettings(WORLD,GameType.SURVIVAL,false,net.minecraft.world.Difficulty.NORMAL,true,rules,WorldDataConfiguration.DEFAULT),new WorldOptions(0x544342L,false,false),WorldPresets::createNormalWorldDimensions);
    }
    private static void finish(Minecraft mc){
        require(SAVED.get()==NAMES.length&&!WarpClient.hasFailedShader()&&WarpClient.activeShaders()==0&&WarpClient.fogDensity()==0,"Incomplete Warp rendering/cleanup");stopped=true;mc.options.tutorialStep=previous;
        LogUtils.getLogger().info("THAUMCRAFT_WARP_RENDER_AUDIT_OK: 15captures/fourNativePostChainsMatchedBeforeActiveNaturalAfter/private200tickMist/nativeDepth/notActualVisualReview");
        LogUtils.getLogger().info("THAUMCRAFT_WARP_CLIENT_SMOKE_OK: 15 captures/world={}",WORLD);mc.stop();
    }
    private static void fail(Minecraft mc,Throwable e){if(stopped)return;stopped=true;if(previous!=null)mc.options.tutorialStep=previous;LogUtils.getLogger().error("THAUMCRAFT_WARP_CLIENT_SMOKE_FAILED",e);mc.stop();}
}
