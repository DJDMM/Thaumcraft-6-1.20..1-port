package thaumcraft.world.warp.client;

import java.util.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Matrix4f;
import thaumcraft.world.rift.RiftModule;
import thaumcraft.world.warp.*;

@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public final class WarpClient {
    private static int fogDuration,width,height;
    private static float fogTarget,fogRed,fogGreen,fogBlue;
    private static PostChain mist;
    private static PostPass mistPass;
    private static final Map<Integer,PostChain> CHAINS=new TreeMap<>();
    private static final Set<Integer> FAILED=new HashSet<>();
    private static Object lastLevel;
    private WarpClient(){}
    public static int fogDuration(){return fogDuration;}
    public static float fogDensity(){return fogTarget;}
    public static int activeShaders(){return CHAINS.size();}
    public static boolean hasFailedShader(){return !FAILED.isEmpty();}
    public static void receive(int type){
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;
        if(type==0&&!RiftModule.NO_STRESS.get()){
            var sound=ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.fromNamespaceAndPath("thaumcraft","heartbeat"));
            if(sound!=null)mc.level.playLocalSound(mc.player.getX(),mc.player.getY(),mc.player.getZ(),sound,SoundSource.AMBIENT,1,1,false);
        }else if(type==1)fogDuration=2400;
        else if(type==2)fogDuration=Math.max(fogDuration,200);
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        if(mc.level!=lastLevel){close();fogDuration=0;fogTarget=0;FAILED.clear();lastLevel=mc.level;}
        if(mc.player==null||mc.isPaused())return;
        if(fogDuration>0){
            if(fogDuration<100)fogTarget=.1F*(fogDuration/100F);
            else if(fogTarget<.1F)fogTarget+=.001F;
            --fogDuration;
        }else fogTarget=0;
        var effects=List.of(WarpModule.DEATH_GAZE.get(),WarpModule.BLURRED_VISION.get(),WarpModule.UNNATURAL_HUNGER.get(),WarpModule.SUN_SCORNED.get());
        for(int i=0;i<4;i++)if(!mc.player.hasEffect(effects.get(i))){var chain=CHAINS.remove(i);if(chain!=null)chain.close();FAILED.remove(i);}
    }
    @SubscribeEvent public static void color(ViewportEvent.ComputeFogColor event){
        fogRed=event.getRed();fogGreen=event.getGreen();fogBlue=event.getBlue();
    }
    @SubscribeEvent public static void fog(ViewportEvent.RenderFog event){
        if(fogTarget<=0||FAILED.contains(-1))return;
        // The core-profile replacement applies the released EXP equation to native depth
        // before hand/GUI rendering; prevent vanilla linear terrain fog applying twice.
        event.setNearPlaneDistance(10_000_000);event.setFarPlaneDistance(11_000_000);event.setCanceled(true);
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL)return;
        var mc=Minecraft.getInstance();if(mc.player==null)return;
        int w=mc.getMainRenderTarget().width,h=mc.getMainRenderTarget().height;
        if(w!=width||h!=height){width=w;height=h;if(mist!=null)mist.resize(w,h);for(var chain:CHAINS.values())chain.resize(w,h);}
        if(fogTarget>0&&!FAILED.contains(-1)){
            var state=new PostState();
            try{
                if(mist==null){
                    mist=new PostChain(mc.getTextureManager(),mc.getResourceManager(),mc.getMainRenderTarget(),id("shaders/post/warp_mist.json"));
                    mistPass=mist.addPass("thaumcraft:warp_mist",mc.getMainRenderTarget(),mist.getTempTarget("swap"));
                    mistPass.addAuxAsset("DepthSampler",mc.getMainRenderTarget()::getDepthTextureId,w,h);
                    mist.addPass("thaumcraft:warp_copy",mist.getTempTarget("swap"),mc.getMainRenderTarget());mist.resize(w,h);
                }
                var effect=mistPass.getEffect();
                effect.safeGetUniform("InverseProjection").set(new Matrix4f(event.getProjectionMatrix()).invert());
                effect.safeGetUniform("Density").set(fogTarget);
                effect.safeGetUniform("FogColor").set(fogRed,fogGreen,fogBlue);
                RenderSystem.disableBlend();RenderSystem.disableDepthTest();RenderSystem.resetTextureMatrix();
                mist.process(event.getPartialTick());
            }catch(Exception failure){fail(-1,failure);if(mist!=null)mist.close();mist=null;}
            finally{state.close();}
        }
        RenderSystem.enableDepthTest();RenderSystem.depthMask(true);
    }
    /** Original overlay-ALL callback runs after the first-person hand and before HUD. */
    @SubscribeEvent public static void postEffects(RenderGuiEvent.Pre event){
        var mc=Minecraft.getInstance();if(mc.player==null)return;
        if(!mc.player.hasEffect(WarpModule.DEATH_GAZE.get())&&!mc.player.hasEffect(WarpModule.BLURRED_VISION.get())
                &&!mc.player.hasEffect(WarpModule.UNNATURAL_HUNGER.get())&&!mc.player.hasEffect(WarpModule.SUN_SCORNED.get()))return;
        event.getGuiGraphics().flush();
        // Match GameRenderer's native PostChain preconditions; the GUI enables blending.
        try(var state=new PostState()){RenderSystem.disableBlend();RenderSystem.disableDepthTest();RenderSystem.resetTextureMatrix();renderPostEffects(mc,event.getPartialTick());}
    }
    private static final class PostState implements AutoCloseable {
        private final Matrix4f projection=new Matrix4f(RenderSystem.getProjectionMatrix()),texture=new Matrix4f(RenderSystem.getTextureMatrix());
        private final com.mojang.blaze3d.vertex.VertexSorting sorting=RenderSystem.getVertexSorting();
        private final ShaderInstance shader=RenderSystem.getShader();
        private final boolean depth=org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_DEPTH_TEST),mask=org.lwjgl.opengl.GL11.glGetBoolean(org.lwjgl.opengl.GL11.GL_DEPTH_WRITEMASK),blend=org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_BLEND);
        private final int src=org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL14.GL_BLEND_SRC_RGB),dst=org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL14.GL_BLEND_DST_RGB),srcAlpha=org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL14.GL_BLEND_SRC_ALPHA),dstAlpha=org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL14.GL_BLEND_DST_ALPHA);
        @Override public void close(){
            Minecraft.getInstance().getMainRenderTarget().bindWrite(true);RenderSystem.setProjectionMatrix(projection,sorting);RenderSystem.setTextureMatrix(texture);
            // EffectInstance and ShaderInstance have separate last-program caches.
            if(shader!=null){com.mojang.blaze3d.shaders.ProgramManager.glUseProgram(shader.getId());shader.apply();}
            if(depth)RenderSystem.enableDepthTest();else RenderSystem.disableDepthTest();RenderSystem.depthMask(mask);
            RenderSystem.blendFuncSeparate(src,dst,srcAlpha,dstAlpha);if(blend)RenderSystem.enableBlend();else RenderSystem.disableBlend();
        }
    }
    private static void renderPostEffects(Minecraft mc,float partial){
        int w=mc.getMainRenderTarget().width,h=mc.getMainRenderTarget().height;
        var effects=List.of(WarpModule.DEATH_GAZE.get(),WarpModule.BLURRED_VISION.get(),WarpModule.UNNATURAL_HUNGER.get(),WarpModule.SUN_SCORNED.get());
        String[] names={"desaturatetc","blurtc","hunger","sunscorned"};
        for(int i=0;i<4;i++)if(mc.player.hasEffect(effects.get(i))&&!FAILED.contains(i)){
            try{
                var chain=CHAINS.get(i);
                if(chain==null){chain=new PostChain(mc.getTextureManager(),mc.getResourceManager(),mc.getMainRenderTarget(),id("shaders/post/"+names[i]+".json"));chain.resize(w,h);CHAINS.put(i,chain);}
                chain.process(partial);
            }catch(Exception failure){fail(i,failure);var chain=CHAINS.remove(i);if(chain!=null)chain.close();}
            finally{mc.getMainRenderTarget().bindWrite(false);}
        }
    }
    @SubscribeEvent public static void privateSpiders(RenderLivingEvent.Pre<?,?> event){
        if(event.getEntity() instanceof MindSpiderEntity spider&&spider.harmless()){
            var player=Minecraft.getInstance().player;
            if(player==null||!spider.viewer().equals(player.getGameProfile().getName()))event.setCanceled(true);
        }
    }
    private static void fail(int which,Exception failure){FAILED.add(which);com.mojang.logging.LogUtils.getLogger().error("Original Warp post-process {} failed",which,failure);}
    private static ResourceLocation id(String name){return ResourceLocation.fromNamespaceAndPath("thaumcraft",name);}
    public static void close(){if(mist!=null)mist.close();mist=null;mistPass=null;for(var chain:CHAINS.values())chain.close();CHAINS.clear();}
    @Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Reload {
        @SubscribeEvent public static void register(RegisterClientReloadListenersEvent event){
            event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener)resources->
                RenderSystem.recordRenderCall(()->{close();FAILED.clear();}));
        }
    }
}
