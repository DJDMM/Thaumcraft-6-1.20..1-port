package thaumcraft.world.eldritch.client;
import java.util.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.nbt.*;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thaumcraft.world.eldritch.EldritchModule;
import thaumcraft.fx.client.*;

/** Original FXArc ballistic points, crossed beamh strips and delayed atlas sparks. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public final class EldritchArcClient {
    private static final List<Arc> ARCS=new ArrayList<>();
    private static final MultiBufferSource.BufferSource BUFFERS=MultiBufferSource.immediate(new BufferBuilder(4096));
    private static ClientLevel world;
    private static long received,rendered;
    private EldritchArcClient(){}
    private static boolean world(){var current=Minecraft.getInstance().level;if(world!=current){world=current;ARCS.clear();received=rendered=0;}return current!=null;}
    public static long receivedCount(){return received;}
    public static long renderedCount(){return rendered;}
    public static boolean hasLiveArc(){return !ARCS.isEmpty();}
    public static void receive(EldritchModule.Arc packet){if(!world()||!world.dimension().location().equals(packet.dimension())||!finite(packet.source())||!finite(packet.target())||!Float.isFinite(packet.height())||packet.source().distanceToSqr(packet.target())>65536)return;if(ARCS.size()>=512)ARCS.remove(0);ARCS.add(new Arc(packet));received++;}
    private static boolean finite(Vec3 value){return Double.isFinite(value.x)&&Double.isFinite(value.y)&&Double.isFinite(value.z);}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){if(event.phase!=TickEvent.Phase.END||!world()||Minecraft.getInstance().isPaused())return;ARCS.removeIf(a->a.age++>=3);}
    @SubscribeEvent public static void render(RenderLevelStageEvent event){if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES||!world()||ARCS.isEmpty())return;var vertices=BUFFERS.getBuffer(ArcType.TYPE);for(Arc arc:ARCS){arc.render(vertices,event);rendered++;}BUFFERS.endBatch();}
    private static Vec3 velocity(Vec3 end,double height){double max=Math.max(height,end.y+height),horizontal=end.horizontalDistance(),a=-horizontal*horizontal/(4*max),slope=-horizontal/(2*a)-Math.sqrt(horizontal*horizontal-4*a*-end.y)/(2*a),vy=Math.sqrt(max*.115),vh=vy/slope;Vec3 velocity=new Vec3(vh*end.x/horizontal,vy,vh*end.z/horizontal);return finite(velocity)?velocity:end.normalize().scale(.2);}
    private static final class Arc {
        final EldritchModule.Arc packet;final List<Vec3> points=new ArrayList<>();final float length;int age;
        Arc(EldritchModule.Arc packet){this.packet=packet;Vec3 end=packet.target().subtract(packet.source()),point=Vec3.ZERO,v=velocity(end,packet.height()<=0?.1:packet.height());length=(float)end.length();double step=v.lengthSqr();points.add(Vec3.ZERO);var random=world.random;
            for(int i=0;end.distanceToSqr(point)>step&&i<50;i++){point=point.add(v);Vec3 noisy=point.add((random.nextDouble()-random.nextDouble())*.25,(random.nextDouble()-random.nextDouble())*.25,(random.nextDouble()-random.nextDouble())*.25);points.add(noisy);sparks(packet,noisy);v=v.subtract(0,.115/1.9,0);}points.add(end);
        }
        void render(VertexConsumer vertices,RenderLevelStageEvent event){if(length<=0)return;Vec3 eye=event.getCamera().getPosition(),origin=packet.source().subtract(eye);float alpha=1-(age+event.getPartialTick())/3;var matrix=event.getPoseStack().last().pose();for(int i=0;i<points.size()-1;i++){Vec3 a=points.get(i).add(origin),b=points.get(i+1).add(origin);float ua=i/length,ub=(i+1)/length;quad(vertices,matrix,a.add(0,-.125,0),a.add(0,.125,0),b.add(0,.125,0),b.add(0,-.125,0),ua,ub,alpha);quad(vertices,matrix,a.add(-.125,0,-.125),a.add(.125,0,.125),b.add(.125,0,.125),b.add(-.125,0,-.125),ua,ub,alpha);}}
        void quad(VertexConsumer v,org.joml.Matrix4f matrix,Vec3 a,Vec3 b,Vec3 c,Vec3 d,float u0,float u1,float alpha){vertex(v,matrix,a,u0,1,alpha);vertex(v,matrix,b,u0,0,alpha);vertex(v,matrix,d,u1,1,alpha);vertex(v,matrix,d,u1,1,alpha);vertex(v,matrix,b,u0,0,alpha);vertex(v,matrix,c,u1,0,alpha);}
        void vertex(VertexConsumer v,org.joml.Matrix4f matrix,Vec3 point,float u,float t,float alpha){v.vertex(matrix,(float)point.x,(float)point.y,(float)point.z).color(packet.red(),packet.green(),packet.blue(),alpha).uv(u,t).uv2(220).endVertex();}
    }
    private static void sparks(EldritchModule.Arc packet,Vec3 local){var random=world.random;var tag=new CompoundTag();int life=30+random.nextInt(20);tag.putInt("life",life);FxEffects.keys(tag,"rgb",Mth.clamp(packet.red()*3,0,1),Mth.clamp(packet.green()*3,0,1),Mth.clamp(packet.blue()*3,0,1));FxEffects.keys(tag,"rgbEnd",random.nextFloat(),random.nextFloat(),random.nextFloat());float[] alpha=new float[6+random.nextInt(life/3)];for(int i=1;i<alpha.length-1;i++)alpha[i]=random.nextFloat();alpha[0]=1;FxEffects.keys(tag,"alpha",alpha);boolean spark=random.nextFloat()<.2;tag.putInt("start",spark?320:512);tag.putInt("count",16);tag.putInt("inc",1);tag.putBoolean("loop",true);tag.putFloat("gravity",spark?0:.125F);FxEffects.keys(tag,"scale",.5F,.125F);tag.putInt("layer",0);tag.putDouble("slowdown",.995);FxEffects.keys(tag,"random",.0025F,.001F,.0025F);FxEngine.add(new FxParticle(world,packet.source().add(local),Vec3.ZERO,tag),2+random.nextInt(3));}
    private static final class ArcType extends RenderType {
        static final RenderType TYPE=create("thaumcraft_eldritch_arc",DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,VertexFormat.Mode.TRIANGLES,4096,false,true,CompositeState.builder().setShaderState(new ShaderStateShard(()->FxShaders.particle(true))).setTextureState(new TextureStateShard(EldritchModule.id("textures/misc/beamh.png"),false,false)).setTransparencyState(LIGHTNING_TRANSPARENCY).setDepthTestState(LEQUAL_DEPTH_TEST).setCullState(NO_CULL).setLightmapState(LIGHTMAP).setWriteMaskState(COLOR_WRITE).setOutputState(PARTICLES_TARGET).createCompositeState(false));
        private ArcType(){super("thaumcraft_eldritch_arc",DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,VertexFormat.Mode.TRIANGLES,4096,false,true,()->{},()->{});}
    }
}
