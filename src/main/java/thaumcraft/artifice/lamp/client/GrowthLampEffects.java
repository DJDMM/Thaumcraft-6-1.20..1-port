package thaumcraft.artifice.lamp.client;

import com.mojang.blaze3d.vertex.*;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import thaumcraft.essentia.condenser.client.CondenserSparkShader;

/** Released drawBlockMistParticles: eight green frame56 puffs after an actual plant state change. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public final class GrowthLampEffects {
    private static final List<Mist> MISTS=new ArrayList<>();
    private static final MultiBufferSource.BufferSource BUFFERS=MultiBufferSource.immediate(new BufferBuilder(4096));
    private static ClientLevel world;
    private static long received,emitted,drawn;
    private static BlockPos lastSource;
    private GrowthLampEffects() {}
    private static void world(ClientLevel level) { if(world!=level){world=level;MISTS.clear();lastSource=null;received=emitted=drawn=0;} }
    public static void receive(GrowthMistPacket packet) {
        var level=Minecraft.getInstance().level;if(level==null||!level.dimension().location().equals(packet.dimension())||!level.hasChunkAt(packet.pos()))return;
        world(level);received++;lastSource=packet.pos();
        var shape=level.getBlockState(packet.pos()).getShape(level,packet.pos());
        // Legacy Block's default bounding box is a cell even for air; modern empty outlines need that fallback.
        var box=shape.isEmpty()?new net.minecraft.world.phys.AABB(0,0,0,1,1,1):shape.bounds();
        for(int i=0;i<8&&MISTS.size()<4096;i++){MISTS.add(new Mist(level,packet.pos(),box,packet.color()));emitted++;}
    }
    public static long receivedPackets() { return received; }
    public static long emittedMists() { return emitted; }
    public static long drawnMists() { return drawn; }
    public static int activeMists() { return MISTS.size(); }
    public static BlockPos lastSource() { return lastSource; }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) { if(event.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();world(mc.level);if(world==null||mc.isPaused())return;MISTS.removeIf(p->{p.tick();return !p.isAlive();}); }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES||world!=Minecraft.getInstance().level||MISTS.isEmpty())return;
        var vertices=BUFFERS.getBuffer(Type.MIST);var matrix=event.getPoseStack().last().pose();
        for(var mist:MISTS){mist.draw(vertices,matrix,event.getCamera(),event.getPartialTick());drawn++;}BUFFERS.endBatch();
    }
    private static final class Mist extends Particle {
        private static final float[] ALPHA={0,.5F,.4F,.3F,.2F,.1F,0};
        final double windX,windZ;
        final float rotationStep;
        Mist(ClientLevel level,BlockPos pos,net.minecraft.world.phys.AABB box,int color) {
            super(level,pos.getX()+box.minX+level.random.nextFloat()*box.getXsize(),pos.getY()+box.minY+level.random.nextFloat()*box.getYsize(),pos.getZ()+box.minZ+level.random.nextFloat()*box.getZsize());
            var random=level.random;setSize(.1F,.1F);hasPhysics=true;xd=random.nextGaussian()*.01;yd=random.nextFloat()*.075;zd=random.nextGaussian()*.01;
            lifetime=50+random.nextInt(25);friction=1;gravity=.1F;rCol=((color>>16)&255)/255F;gCol=((color>>8)&255)/255F;bCol=(color&255)/255F;
            float angle=level.getMoonPhase()*(40+random.nextInt(10))/180F*Mth.PI;windX=.1*Mth.cos(angle)*.001;windZ=-.1*Mth.sin(angle)*.001;
            roll=(float)(random.nextFloat()*Math.PI*2);rotationStep=Mth.PI*(float)((random.nextBoolean()?-1:1)*.017453292519943)*2;
        }
        @Override public void tick() { xo=x;yo=y;zo=z;if(age++>=lifetime)remove();oRoll=roll;roll+=rotationStep;yd-=.04*gravity;move(xd,yd,zd);xd+=windX;zd+=windZ; }
        @Override public ParticleRenderType getRenderType() { return ParticleRenderType.CUSTOM; }
        @Override public void render(VertexConsumer out,Camera camera,float partial) { draw(out,new org.joml.Matrix4f(),camera,partial); }
        void draw(VertexConsumer out,org.joml.Matrix4f matrix,Camera camera,float partial) {
            float progress=Mth.clamp(age/(float)lifetime,0,1),size=.1F*(5-4*progress),key=progress*6;int index=Math.min(5,(int)key);float opacity=Mth.lerp(key-index,ALPHA[index],ALPHA[index+1]);
            var eye=camera.getPosition();float px=(float)(Mth.lerp(partial,xo,x)-eye.x),py=(float)(Mth.lerp(partial,yo,y)-eye.y),pz=(float)(Mth.lerp(partial,zo,z)-eye.z);
            var orientation=new Quaternionf(camera.rotation()).rotateZ(-(roll+(roll-oRoll)*partial));int light=getLightColor(partial);float u=8/16F,v=3/16F,cell=1/16F;
            corner(out,matrix,orientation,-1,-1,px,py,pz,size,u+cell,v+cell,opacity,light);corner(out,matrix,orientation,-1,1,px,py,pz,size,u+cell,v,opacity,light);
            corner(out,matrix,orientation,1,1,px,py,pz,size,u,v,opacity,light);corner(out,matrix,orientation,1,-1,px,py,pz,size,u,v+cell,opacity,light);
        }
        private void corner(VertexConsumer out,org.joml.Matrix4f matrix,Quaternionf rotation,int sx,int sy,float px,float py,float pz,float size,float u,float v,float alpha,int light) { var q=new Vector3f(sx,sy,0).rotate(rotation).mul(size).add(px,py,pz);out.vertex(matrix,q.x,q.y,q.z).color(rCol,gCol,bCol,alpha).uv(u,v).uv2(light).endVertex(); }
    }
    private static final class Type extends RenderType {
        private Type() { super("thaumcraft_growth_mist",DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,VertexFormat.Mode.QUADS,4096,false,true,()->{},()->{}); }
        static final RenderType MIST=create("thaumcraft_growth_mist",DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,VertexFormat.Mode.QUADS,4096,false,true,CompositeState.builder().setShaderState(new ShaderStateShard(CondenserSparkShader::shader)).setTextureState(new TextureStateShard(ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/misc/particles.png"),false,false)).setTransparencyState(LIGHTNING_TRANSPARENCY).setCullState(NO_CULL).setDepthTestState(LEQUAL_DEPTH_TEST).setLightmapState(LIGHTMAP).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
    }
}
