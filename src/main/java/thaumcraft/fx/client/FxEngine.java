package thaumcraft.fx.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Tick-owned particles with original separate depth/blend layers; delayed particles do not age early. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public final class FxEngine {
    private record Delayed(FxParticle particle,int remaining){}
    private static final List<FxParticle> PARTICLES=new ArrayList<>();
    private static final List<Delayed> DELAYED=new ArrayList<>();
    private static final MultiBufferSource.BufferSource BUFFERS=MultiBufferSource.immediate(new BufferBuilder(8192));
    private static ClientLevel world;
    private static long emitted;
    private FxEngine(){}
    private static boolean world(){var next=Minecraft.getInstance().level;if(next!=world){PARTICLES.clear();DELAYED.clear();FxEffects.clear();world=next;}return world!=null;}
    public static boolean ready(){return world();}
    public static void add(FxParticle particle,int delay){
        if(!world())return;if(delay>0){DELAYED.add(new Delayed(particle,delay));return;}
        int setting=Minecraft.getInstance().options.particles().get().getId(),skip=setting+(Minecraft.getInstance().getFps()<30?1:0);
        if(world.random.nextInt(3)<skip)return;int limit=setting==2?500:setting==1?1000:2000,count=0,oldest=-1;
        for(int i=0;i<PARTICLES.size();i++)if(PARTICLES.get(i).layer==particle.layer){count++;if(oldest<0)oldest=i;}
        if(count>=limit&&oldest>=0)PARTICLES.remove(oldest);PARTICLES.add(particle);emitted++;
    }
    /** Original addEffectWithDelay always queues, including delay0, until the next START tick. */
    public static void addDelayed(FxParticle particle,int delay){if(world())DELAYED.add(new Delayed(particle,Math.max(0,delay)));}
    public static long emitted(){return emitted;}
    public static int activeCount(){return PARTICLES.size();}
    public static List<FxParticle> snapshot(){return List.copyOf(PARTICLES);}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.START||!world()||Minecraft.getInstance().isPaused())return;
        for(int i=0;i<DELAYED.size();){var delayed=DELAYED.get(i);if(delayed.remaining<=1){add(delayed.particle,0);DELAYED.remove(i);}else{DELAYED.set(i,new Delayed(delayed.particle,delayed.remaining-1));i++;}}
        for(var iterator=PARTICLES.iterator();iterator.hasNext();){var particle=iterator.next();particle.tick();if(!particle.isAlive())iterator.remove();}
        FxEffects.tick();
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent e){
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES||!world())return;
        for(int layer=3;layer>=0;layer--){
            for(var particle:PARTICLES)if(particle.layer==layer)particle.draw(BUFFERS.getBuffer(FxRenderTypes.particle(particle.texture,layer)),e.getCamera(),e.getPartialTick(),e.getPoseStack().last().pose());
            BUFFERS.endBatch();
        }
        FxEffects.render(e,BUFFERS);BUFFERS.endBatch();
    }
}
