package thaumcraft.auromancy.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import thaumcraft.auromancy.FocusBoltNetwork;
import java.util.*;

/** FXBolt's original three-tick seeded path and nested five-sided tubes, using the modern vertex API. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public final class FocusBoltClient {
    private static final List<Bolt> BOLTS=new ArrayList<>();
    private static final MultiBufferSource.BufferSource BUFFERS=MultiBufferSource.immediate(new BufferBuilder(4096));
    private static ClientLevel world;
    private static long received,rendered;
    private static String lastSmokeGeometry;
    private FocusBoltClient() {}
    private static boolean select(ClientLevel level) {
        if(world!=level){BOLTS.clear();world=level;received=rendered=0;lastSmokeGeometry=null;}return level!=null;
    }
    public static void receive(FocusBoltNetwork.Zap p) {
        var level=Minecraft.getInstance().level;
        if(!select(level)||!level.dimension().location().equals(p.dimension())||!FocusBoltNetwork.valid(p.source(),p.target(),p.width()))return;
        if(BOLTS.size()>=512)BOLTS.remove(0);
        BOLTS.add(new Bolt(p,level.random.nextInt(50)*(float)Math.PI,level.random.nextInt(1000)));received++;
    }
    public static long receivedCount(){return received;}
    public static long renderedCount(){return rendered;}
    public static String lastRenderedGeometryForSmoke(){return lastSmokeGeometry;}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e) {
        var mc=Minecraft.getInstance();if(e.phase!=TickEvent.Phase.END||!select(mc.level)||mc.isPaused())return;
        BOLTS.removeIf(b->++b.age>3);
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent e) {
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES||BOLTS.isEmpty()||world!=Minecraft.getInstance().level)return;
        var vertices=BUFFERS.getBuffer(FocusBoltRenderTypes.BOLT);var eye=e.getCamera().getPosition();
        for(var bolt:BOLTS){bolt.draw(vertices,e.getPoseStack().last().pose(),eye,e.getPartialTick());rendered++;}
        BUFFERS.endBatch();
    }
    private static final class Bolt {
        final FocusBoltNetwork.Zap p;final float phase;final int seed;int age;
        Bolt(FocusBoltNetwork.Zap p,float phase,int seed){this.p=p;this.phase=phase;this.seed=seed;}
        void draw(VertexConsumer vertices,Matrix4f pose,Vec3 eye,float partial) {
            Vec3 delta=p.target().subtract(p.source());float length=(float)(delta.length()*Math.PI);int steps=(int)length;
            // Original GLE rendering requires more than two path points.
            if(steps<3)return;
            Random rng=new Random(seed);List<Vec3> points=new ArrayList<>();List<Float> widths=new ArrayList<>();
            points.add(Vec3.ZERO);widths.add(p.width());float amplitude=(age+partial)/10F;
            for(int a=1;a<steps-1;a++) {
                float dist=a*(length/steps)+phase;
                points.add(delta.scale(a/(double)steps).add(Mth.sin(dist/4)*amplitude+(rng.nextFloat()-rng.nextFloat())*.1F,
                        Mth.sin(dist/3)*amplitude+(rng.nextFloat()-rng.nextFloat())*.1F,
                        Mth.sin(dist/2)*amplitude+(rng.nextFloat()-rng.nextFloat())*.1F));
                widths.add((rng.nextInt(4)==0?1-age*.25F:1)*p.width());
            }
            points.add(delta);widths.add(p.width());float alpha=Mth.clamp(1-age/3F,.1F,1);
            double[][] path=new double[points.size()][3];double[] radii=new double[points.size()];
            for(int i=0;i<points.size();i++) {
                Vec3 point=points.get(i);path[i]=new double[]{point.x,point.y,point.z};radii[i]=widths.get(i)/10F;
            }
            boolean smoke=Boolean.getBoolean("thaumcraft.auromancySmokeTest");String geometry=null;
            if(smoke)geometry=
                    "{\"source\":"+vector(p.source())+",\"target\":"+vector(p.target())+",\"eye\":"+vector(eye)
                    +",\"phase\":"+phase+",\"seed\":"+seed+",\"age\":"+age+",\"partial\":"+partial
                    +",\"color\":"+p.color()+",\"alpha\":"+alpha+",\"width\":"+p.width()
                    +",\"points\":"+Arrays.deepToString(path)+",\"baseRadii\":"+Arrays.toString(radii)
                    +",\"pose\":"+Arrays.toString(pose.get(new float[16]))
                    +",\"modelView\":"+Arrays.toString(com.mojang.blaze3d.systems.RenderSystem.getModelViewMatrix().get(new float[16]))
                    +",\"projection\":"+Arrays.toString(com.mojang.blaze3d.systems.RenderSystem.getProjectionMatrix().get(new float[16]));
            int[] vertexCount=smoke?new int[1]:null;
            OriginalBoltGeometry.Sink sink=(x,y,z,u,v)->{
                if(vertexCount!=null)vertexCount[0]++;
                vertex(vertices,pose,p.source().add(x,y,z).subtract(eye),(float)u,(float)v,alpha);
            };
            OriginalBoltGeometry.emit(path,radii,sink);
            for(int i=0;i<radii.length;i++)radii[i]/=3.0;
            OriginalBoltGeometry.emit(path,radii,sink);
            if(smoke)lastSmokeGeometry=geometry+",\"vertexCount\":"+vertexCount[0]+",\"renderSequence\":"+(rendered+1)+"}";
        }
        private static String vector(Vec3 vector){return "["+vector.x+","+vector.y+","+vector.z+"]";}
        void vertex(VertexConsumer v,Matrix4f pose,Vec3 point,float u,float t,float alpha) {
            v.vertex(pose,(float)point.x,(float)point.y,(float)point.z)
                    .color((p.color()>>16&255)/255F,(p.color()>>8&255)/255F,(p.color()&255)/255F,alpha).uv(u,t).uv2(15728880).endVertex();
        }
    }
}
