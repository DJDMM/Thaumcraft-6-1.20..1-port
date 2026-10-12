package thaumcraft.artifice.mirror.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Actual native mirror block-event wispy mote; original 512..527 particle frames. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public final class MirrorEffects {
    private static final List<Mote> MOTES=new ArrayList<>();
    private static final MultiBufferSource.BufferSource BUFFERS=MultiBufferSource.immediate(new com.mojang.blaze3d.vertex.BufferBuilder(4096));
    private static ClientLevel world;private static long emitted,drawn;
    private MirrorEffects(){}
    public static long actualMotes(){return emitted;}
    public static long actualDraws(){return drawn;}
    public static void transfer(BlockPos pos,Direction face){
        var level=Minecraft.getInstance().level;if(level==null)return;
        if(world!=level){world=level;MOTES.clear();}
        if(MOTES.size()>=4096)return;
        MOTES.add(new Mote(level,pos,face));emitted++;
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){if(event.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();if(mc.level!=world){world=mc.level;MOTES.clear();}if(world==null||mc.isPaused())return;MOTES.removeIf(m->{m.tick(world);return m.age>=m.life;});}
    @SubscribeEvent public static void render(RenderLevelStageEvent event){if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES||world!=Minecraft.getInstance().level||MOTES.isEmpty())return;for(var m:MOTES){m.draw(BUFFERS.getBuffer(Type.MOTE),event.getPoseStack().last().pose(),event.getCamera(),event.getPartialTick());drawn++;}BUFFERS.endBatch();}
    private static final class Mote {
        double x,y,z,ox,oy,oz,vx,vy,vz;final float r,b,gravity;final int life;int age;
        Mote(ClientLevel level,BlockPos p,Direction f){
            var random=level.random;x=p.getX()+.33+random.nextFloat()*.33-f.getStepX()/2D;y=p.getY()+.33+random.nextFloat()*.33-f.getStepY()/2D;z=p.getZ()+.33+random.nextFloat()*.33-f.getStepZ()/2D;ox=x;oy=y;oz=z;
            vx=f.getStepX()/50D+random.nextGaussian()*.01;vy=f.getStepY()/50D+random.nextGaussian()*.01;vz=f.getStepZ()/50D+random.nextGaussian()*.01;
            int base=10+random.nextInt(21);life=(int)(base+base/2*random.nextFloat());r=random.nextFloat()/3F;b=random.nextFloat()/2F;gravity=(float)random.nextGaussian()*.01F;
        }
        void tick(ClientLevel level){ox=x;oy=y;oz=z;age++;vy-=.04*gravity;vx+=level.random.nextGaussian()*.0025;vz+=level.random.nextGaussian()*.0025;x+=vx;y+=vy;z+=vz;vx*=.98;vy*=.98;vz*=.98;}
        void draw(VertexConsumer out,Matrix4f pose,Camera camera,float partial){
            float progress=(age+partial)/life,a=progress<.25F?progress*2.4F:progress>.75F?(1-progress)*2.4F:.6F,size=.1F*(1-progress*.5F);
            float u=(age%16)/64F,v=8/64F;var eye=camera.getPosition();float px=(float)(Mth.lerp(partial,ox,x)-eye.x),py=(float)(Mth.lerp(partial,oy,y)-eye.y),pz=(float)(Mth.lerp(partial,oz,z)-eye.z);
            corner(out,pose,camera,-1,-1,px,py,pz,size,u+1/64F,v+1/64F,a);corner(out,pose,camera,-1,1,px,py,pz,size,u+1/64F,v,a);corner(out,pose,camera,1,1,px,py,pz,size,u,v,a);corner(out,pose,camera,1,-1,px,py,pz,size,u,v+1/64F,a);
        }
        void corner(VertexConsumer out,Matrix4f pose,Camera c,int sx,int sy,float px,float py,float pz,float size,float u,float v,float a){var q=new Vector3f(sx,sy,0).rotate(c.rotation()).mul(size).add(px,py,pz);out.vertex(pose,q.x,q.y,q.z).color(r,0,b,a).uv(u,v).uv2(240).endVertex();}
    }
    private static final class Type extends RenderType {
        private Type(){super("thaumcraft_mirror_mote",DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,VertexFormat.Mode.QUADS,4096,false,true,()->{},()->{});}
        static final RenderType MOTE=create("thaumcraft_mirror_mote",DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,VertexFormat.Mode.QUADS,4096,false,true,CompositeState.builder().setShaderState(RENDERTYPE_TEXT_SEE_THROUGH_SHADER).setTextureState(new TextureStateShard(ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/misc/particles.png"),false,false)).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL).setDepthTestState(LEQUAL_DEPTH_TEST).setLightmapState(LIGHTMAP).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
    }
}
