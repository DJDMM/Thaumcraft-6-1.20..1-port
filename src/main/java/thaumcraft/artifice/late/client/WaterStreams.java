package thaumcraft.artifice.late.client;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thaumcraft.fx.client.FxRenderTypes;
import thaumcraft.artifice.late.WaterStreamColor;
/** Released Urn FXEssentiaStream: eight-sided original geometry, merging, movement, drops and texture. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public final class WaterStreams {
    private static final List<Stream> ACTIVE=new ArrayList<>();private static final Map<String,Stream> LOOKUP=new HashMap<>();private static ClientLevel current;
    private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/misc/essentia.png");
    private WaterStreams(){}
    public static void add(Vec3 start,Vec3 target,int seed){var l=Minecraft.getInstance().level;if(l==null)return;if(l!=current){ACTIVE.clear();LOOKUP.clear();current=l;}String key=net.minecraft.core.BlockPos.containing(start).asLong()+""+net.minecraft.core.BlockPos.containing(target).asLong()+"2650102";var old=LOOKUP.get(key);if(old!=null&&old.trail.size()<old.length){old.length+=5;old.life+=5;return;}if(ACTIVE.size()<4096){var next=new Stream(l,start,target,seed,key);ACTIVE.add(next);LOOKUP.put(key,next);}}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){if(e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();if(mc.level!=current){ACTIVE.clear();LOOKUP.clear();current=mc.level;}if(current!=null&&!mc.isPaused())ACTIVE.removeIf(s->{if(s.tick())return false;LOOKUP.remove(s.key,s);return true;});}
    @SubscribeEvent public static void render(RenderLevelStageEvent e){
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES||current!=Minecraft.getInstance().level||ACTIVE.isEmpty())return;var buffers=Minecraft.getInstance().renderBuffers().bufferSource();var type=FxRenderTypes.particle(TEXTURE,1);var v=buffers.getBuffer(type);var camera=e.getCamera().getPosition();var matrix=e.getPoseStack().last().pose();
        for(var s:ACTIVE)if(s.points!=null&&s.points.length>2){float offset=s.growing<0?0:.075F*(s.age-s.growing+e.getPartialTick());WaterStreamGeometry.emit(s.points,s.radii,(x,y,z,u,w,index)->{float light=1-Mth.sin((index+s.age)/2F)*.1F;int color=WaterStreamColor.packRgb(2650102,light);var q=s.start.add(x,y,z).subtract(camera);v.vertex(matrix,(float)q.x,(float)q.y,(float)q.z).color(color>>16&255,color>>8&255,color&255,255).uv((float)u,(float)w*.075F+offset).uv2(net.minecraft.client.renderer.LevelRenderer.getLightColor(current,net.minecraft.core.BlockPos.containing(s.position))).endVertex();});}
        buffers.endBatch(type);
    }
    private record Point(double radius,double x,double y,double z){}
    private static final class Stream {
        final ClientLevel level;final Vec3 start,target;final String key;final int seed;final RandomSource r=RandomSource.create();final List<Point> trail=new ArrayList<>();
        Vec3 position,velocity;float scale;int age,life,length=20,growing=-1;double[][] points;double[] radii;
        Stream(ClientLevel l,Vec3 start,Vec3 target,int seed,String key){this.key=key;level=l;this.start=start;position=start;this.target=target;this.seed=seed;scale=(float)(.1F*(1+r.nextGaussian()*.15000000596046448));life=Math.max(1,(int)((float)start.distanceTo(target)*21));velocity=new Vec3(Mth.sin(seed/4F)*.015F,.2+Mth.sin(seed/3F)*.015F,Mth.sin(seed/2F)*.015F);trail.add(new Point(0,0,0,.001));trail.add(new Point(0,0,0,.001));}
        boolean tick(){
            if(age++>=life||length<1)return false;velocity=velocity.add(0,.01*.2F,0);if(!level.hasChunkAt(net.minecraft.core.BlockPos.containing(position.add(velocity))))return false;
            var box=new AABB(position.x-.1,position.y,position.z-.1,position.x+.1,position.y+.2,position.z+.1);var moved=net.minecraft.world.entity.Entity.collideBoundingBox(null,velocity,box,level,List.of());position=position.add(moved);velocity=new Vec3(moved.x==velocity.x?velocity.x:0,velocity.y,moved.z==velocity.z?velocity.z:0).scale(.985);velocity=new Vec3(Mth.clamp((float)velocity.x,-.05F,.05F),Mth.clamp((float)velocity.y,-.05F,.05F),Mth.clamp((float)velocity.z,-.05F,.05F));
            var delta=target.subtract(position);double distance=(float)delta.length();if(distance>1e-12)velocity=velocity.add(delta.scale(.01/(distance*Math.min(1,distance))));float radius=scale*(.75F+Mth.sin((seed+age)/2F)*.25F);if(distance<1){float factor=Mth.sin((float)(distance*Math.PI/2));radius*=factor;scale*=factor;}
            if(scale>.001){var q=position.subtract(start);trail.add(new Point(radius,q.x,q.y,q.z));}else{if(growing<0)growing=age;length--;LateDeviceEffects.drop(level,target.add(r.nextGaussian()*.07500000298023224,r.nextGaussian()*.07500000298023224,r.nextGaussian()*.07500000298023224));}
            if(trail.size()>length)trail.remove(0);points=new double[trail.size()][3];radii=new double[trail.size()];int c=trail.size();for(var q:trail){c--;points[c][0]=q.x+Mth.sin((c+age)/6F)*.03F;points[c][1]=q.y+Mth.sin((c+age)/7F)*.03F;points[c][2]=q.z+Mth.sin((c+age)/8F)*.03F;radii[c]=q.radius*(1+Mth.sin((c+age)/3F)*.2F);if(c>trail.size()-10)radii[c]*=Mth.cos((float)((c-(trail.size()-12))/10F*Math.PI/2));if(c<2)radii[c]=0;else if(c==2)radii[c]=(scale*.5+radii[c])/2;else if(c==3)radii[c]=(scale+radii[c])/2;else if(c==4)radii[c]=(scale+2*radii[c])/3;}
            if(trail.size()>2&&r.nextBoolean()){int q=r.nextInt(3);if(r.nextBoolean())q=trail.size()-2;var t=trail.get(q);LateDeviceEffects.drop(level,start.add(t.x,t.y,t.z));}return true;
        }
    }
}
