package thaumcraft.artifice.furnace.client;

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
import thaumcraft.artifice.furnace.InfernalPollutionPacket;

/** PacketFXPollute / FXGeneric layer1: original purple smoke, with native particle collision/light. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public final class InfernalPollutionEffects {
    private static final List<Puff> PUFFS=new ArrayList<>();
    private static final MultiBufferSource.BufferSource BUFFERS=MultiBufferSource.immediate(new BufferBuilder(4096));
    private static ClientLevel world;
    private static BlockPos lastSource;
    private static long received,emitted,drawn;
    private InfernalPollutionEffects() {}
    private static void world(ClientLevel level) { if(world!=level){world=level;PUFFS.clear();lastSource=null;received=emitted=drawn=0;} }
    public static void receive(InfernalPollutionPacket packet) {
        var level=Minecraft.getInstance().level;
        if(level==null||!level.dimension().location().equals(packet.dimension()))return;
        world(level);lastSource=packet.pos().immutable();received++;
        for(int i=0;i<packet.particleCount()&&PUFFS.size()<4096;i++){PUFFS.add(new Puff(level,packet.pos()));emitted++;}
    }
    public static long receivedPackets() { return received; }
    public static long emittedPuffs() { return emitted; }
    public static long drawnPuffs() { return drawn; }
    public static int activePuffs() { return PUFFS.size(); }
    public static BlockPos lastSource() { return lastSource; }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();world(mc.level);
        if(world==null||mc.isPaused())return;PUFFS.removeIf(p->{p.tick();return !p.isAlive();});
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES||world!=Minecraft.getInstance().level||PUFFS.isEmpty())return;
        var vertices=BUFFERS.getBuffer(Type.SMOKE);var matrix=event.getPoseStack().last().pose();
        for(var puff:PUFFS){puff.draw(vertices,matrix,event.getCamera(),event.getPartialTick());drawn++;}BUFFERS.endBatch();
    }
    private static final class Puff extends Particle {
        final double windX,windZ;
        final float rotationStep;
        Puff(ClientLevel level,BlockPos pos) {
            super(level,pos.getX()+.2F+level.random.nextFloat()*.6F,pos.getY()+.2F+level.random.nextFloat()*.6F,pos.getZ()+.2F+level.random.nextFloat()*.6F);
            var random=level.random;setSize(.1F,.1F);hasPhysics=true;
            xd=(random.nextFloat()-random.nextFloat())*.005;yd=.02;zd=(random.nextFloat()-random.nextFloat())*.005;
            lifetime=100+random.nextInt(60);friction=1;gravity=0;rCol=1;gCol=.3F;bCol=.9F;
            float windAngle=level.getMoonPhase()*(40+random.nextInt(10))/180F*Mth.PI;
            windX=.1*Mth.cos(windAngle)*.001;windZ=-.1*Mth.sin(windAngle)*.001;
            roll=(float)(random.nextFloat()*Math.PI*2);
            // Original setRotationSpeed converts +/-1 degree, then onUpdate multiplies by2PI again.
            rotationStep=Mth.PI*(float)((random.nextBoolean()?-1:1)*.017453292519943)*2;
        }
        @Override public void tick() {
            xo=x;yo=y;zo=z;if(age++>=lifetime)remove();oRoll=roll;roll+=rotationStep;
            move(xd,yd,zd);xd+=windX;zd+=windZ;
        }
        @Override public ParticleRenderType getRenderType() { return ParticleRenderType.CUSTOM; }
        @Override public void render(VertexConsumer out,Camera camera,float partial) { draw(out,new org.joml.Matrix4f(),camera,partial); }
        void draw(VertexConsumer out,org.joml.Matrix4f matrix,Camera camera,float partial) {
            float progress=Math.min(1,age/(float)lifetime),size=.1F*(2+3*progress),opacity=.5F*(1-progress);
            var eye=camera.getPosition();float px=(float)(Mth.lerp(partial,xo,x)-eye.x),py=(float)(Mth.lerp(partial,yo,y)-eye.y),pz=(float)(Mth.lerp(partial,zo,z)-eye.z);
            float rotation=roll+(roll-oRoll)*partial;
            var orientation=new Quaternionf(camera.rotation()).rotateZ(-rotation);
            int light=getLightColor(partial);float u=8/16F,v=3/16F,cell=1/16F;
            corner(out,matrix,orientation,-1,-1,px,py,pz,size,u+cell,v+cell,opacity,light);
            corner(out,matrix,orientation,-1,1,px,py,pz,size,u+cell,v,opacity,light);
            corner(out,matrix,orientation,1,1,px,py,pz,size,u,v,opacity,light);
            corner(out,matrix,orientation,1,-1,px,py,pz,size,u,v+cell,opacity,light);
        }
        private void corner(VertexConsumer out,org.joml.Matrix4f matrix,Quaternionf rotation,int sx,int sy,float px,float py,float pz,float size,float u,float v,float alpha,int light) {
            var q=new Vector3f(sx,sy,0).rotate(rotation).mul(size).add(px,py,pz);
            out.vertex(matrix,q.x,q.y,q.z).color(rCol,gCol,bCol,alpha).uv(u,v).uv2(light).endVertex();
        }
    }
    private static final class Type extends RenderType {
        private Type() { super("thaumcraft_infernal_pollution",DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,VertexFormat.Mode.QUADS,4096,false,true,()->{},()->{}); }
        static final RenderType SMOKE=create("thaumcraft_infernal_pollution",DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,VertexFormat.Mode.QUADS,4096,false,true,
                CompositeState.builder().setShaderState(RENDERTYPE_TEXT_SEE_THROUGH_SHADER)
                        .setTextureState(new TextureStateShard(ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/misc/particles.png"),false,false))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL).setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setLightmapState(LIGHTMAP).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
    }
}
