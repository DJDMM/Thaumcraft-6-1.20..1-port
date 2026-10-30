package thaumcraft.world.voidsiphon.client;
import java.util.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.level.ClipContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Released FXVoidStream lifecycle and two three-sided CoreGLE/ender passes. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public final class VoidSiphonStreams {
    private static final List<Stream> ACTIVE=new ArrayList<>();
    private static ClientLevel current;
    private VoidSiphonStreams(){}
    public static void add(Vec3 start,Vec3 target,int seed,float scale){
        var level=Minecraft.getInstance().level;if(level==null)return;
        if(current!=level){ACTIVE.clear();current=level;}
        if(ACTIVE.size()<4096)ACTIVE.add(new Stream(level,start,target,seed,scale));
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();
        if(current!=mc.level){ACTIVE.clear();current=mc.level;}
        if(current==null||mc.isPaused())return;ACTIVE.removeIf(stream->!stream.tick());
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent e){
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES||current!=Minecraft.getInstance().level||ACTIVE.isEmpty())return;
        var buffers=Minecraft.getInstance().renderBuffers().bufferSource();var pose=e.getPoseStack();Vec3 camera=e.getCamera().getPosition();
        for(var stream:ACTIVE){
            if(stream.points==null||stream.points.length<3)continue;pose.pushPose();pose.translate(stream.start.x-camera.x,stream.start.y-camera.y,stream.start.z-camera.z);
            for(int pass=0;pass<2;pass++){
                var type=pass==0?VoidSiphonRenderTypes.GLOW:VoidSiphonRenderTypes.STREAM_CORE;var v=buffers.getBuffer(type);var matrix=pose.last().pose();
                double[] radii=stream.radii.clone();for(int i=0;i<radii.length;i++)radii[i]*=1.5F-pass;
                VoidStreamGeometry.emit(stream.points,radii,(x,y,z,u,w)->v.vertex(matrix,(float)x,(float)y,(float)z).endVertex());
                buffers.endBatch(type);
            }
            pose.popPose();
        }
    }
    private record Point(double radius,double x,double y,double z){}
    private static final class Stream {
        private final ClientLevel level;
        private final Vec3 start,target;
        private final int seed,maxAge;
        private final RandomSource random=RandomSource.create();
        private Vec3 position,velocity;
        private float scale;
        private int age,length=40,growing=-1;
        private final List<Point> trail=new ArrayList<>();
        private double[][] points;
        private double[] radii;
        private Stream(ClientLevel level,Vec3 start,Vec3 target,int seed,float scale){
            this.level=level;this.start=start;this.position=start;this.target=target;this.seed=seed;
            this.scale=(float)(scale*(1+random.nextGaussian()*.15000000596046448));
            maxAge=Math.max(1,(int)((float)start.distanceTo(target)*21F))*2;
            velocity=new Vec3(Mth.sin(seed/4F)*.025F,Mth.sin(seed/3F)*.025F,Mth.sin(seed/2F)*.025F);
            trail.add(new Point(0,0,0,.001));trail.add(new Point(0,0,0,.001));
        }
        private boolean tick(){
            if(age++>=maxAge||length<1)return false;
            velocity=velocity.add(0,.01*.2F,0);
            // Particle.move uses its .2-wide collision box, not a zero-width ray.
            if(!level.hasChunkAt(net.minecraft.core.BlockPos.containing(position.add(velocity).add(.2,0,.2)))
                ||!level.hasChunkAt(net.minecraft.core.BlockPos.containing(position.add(velocity).add(-.2,0,-.2))))return false;
            var box=new AABB(position.x-.1,position.y,position.z-.1,position.x+.1,position.y+.2,position.z+.1);
            var moved=net.minecraft.world.entity.Entity.collideBoundingBox(null,velocity,box,level,List.of());
            position=position.add(moved);
            velocity=new Vec3(moved.x==velocity.x?velocity.x:0,velocity.y,moved.z==velocity.z?velocity.z:0);
            velocity=velocity.scale(.985);velocity=new Vec3(Mth.clamp((float)velocity.x,-.04F,.04F),Mth.clamp((float)velocity.y,-.04F,.04F),Mth.clamp((float)velocity.z,-.04F,.04F));
            Vec3 delta=target.subtract(position);double distance=(float)delta.length();
            if(distance>1.0E-12)velocity=velocity.add(delta.scale(.01/(distance*Math.min(1,distance)))).add(random.nextGaussian()*.014999999664723873,random.nextGaussian()*.014999999664723873,random.nextGaussian()*.014999999664723873);
            float radius=scale*(.75F+Mth.sin((seed+age)/2F)*.25F);
            if(distance<.5){float factor=Mth.sin((float)(distance*Math.PI/2));radius*=factor;scale*=factor;}
            if(scale>.001){var point=position.subtract(start);trail.add(new Point(radius,point.x,point.y,point.z));}
            else{if(growing<0)growing=age;length--;}
            if(trail.size()>length)trail.remove(0);
            points=new double[trail.size()][3];radii=new double[trail.size()];int c=trail.size();
            for(var point:trail){
                c--;float variance=1+Mth.sin((c+age)/3F)*.2F;
                points[c][0]=point.x+Mth.sin((c+age)/6F)*.01F;points[c][1]=point.y+Mth.sin((c+age)/7F)*.01F;points[c][2]=point.z+Mth.sin((c+age)/8F)*.01F;
                radii[c]=point.radius*variance;
                if(c>trail.size()-10)radii[c]*=Mth.cos((float)((c-(trail.size()-12))/10F*Math.PI/2));
                if(c<2)radii[c]=0;else if(c==2)radii[c]=(scale*.5+radii[c])/2;else if(c==3)radii[c]=(scale+radii[c])/2;else if(c==4)radii[c]=(scale+radii[c]*2)/3;
            }
            // Retain the released unused random draws; they affect this particle's next noise.
            if(trail.size()>2&&random.nextBoolean()){random.nextInt(3);random.nextBoolean();}
            return true;
        }
    }
}
