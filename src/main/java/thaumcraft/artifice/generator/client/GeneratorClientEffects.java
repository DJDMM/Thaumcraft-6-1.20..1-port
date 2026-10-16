package thaumcraft.artifice.generator.client;

import com.mojang.blaze3d.vertex.*;
import java.util.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import thaumcraft.artifice.generator.VisGeneratorBlock;
import thaumcraft.essentia.condenser.client.CondenserSparkShader;

/** Released BlockVisGenerator.randomDisplayTick → FXDispatcher.spark; no invented operational overlay. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public final class GeneratorClientEffects {
    private static final List<Spark> SPARKS=new ArrayList<>();
    private static final MultiBufferSource.BufferSource BUFFERS=MultiBufferSource.immediate(new BufferBuilder(4096));
    private static ClientLevel world;
    private static long attempted,emitted,drawn,poweredEmitted,poweredSamples;
    private static int sparkFaces;
    private GeneratorClientEffects(){}
    private static void world(ClientLevel level){if(world!=level){world=level;SPARKS.clear();attempted=emitted=drawn=poweredEmitted=poweredSamples=0;sparkFaces=0;}}
    public static long positiveEnergySamples(){return attempted;}
    public static long poweredPositiveSamples(){return poweredSamples;}
    public static long emittedSparks(){return emitted;}
    public static long drawnSparks(){return drawn;}
    public static long poweredSparks(){return poweredEmitted;}
    public static int sparkFaces(){return sparkFaces;}
    public static int activeSparks(){return SPARKS.size();}
    /** Called only by the native block's ordinary client animateTick. */
    public static void animate(Level level,BlockPos pos,BlockState state,RandomSource random){
        if(!(level instanceof ClientLevel client)||!state.hasProperty(VisGeneratorBlock.FACING))return;
        world(client);var face=state.getValue(VisGeneratorBlock.FACING);var tile=client.getBlockEntity(pos);
        if(tile==null)return;
        var capability=tile.getCapability(ForgeCapabilities.ENERGY,face).resolve().orElse(null);
        // The released block tests stored energy only: charged, redstone-disabled generators still spark.
        if(capability==null||capability.getEnergyStored()<=0)return;
        double x=face.getStepX()==0?random.nextGaussian()*.1:face.getStepX()*.1;
        double y=face.getStepY()==0?random.nextGaussian()*.1:face.getStepY()*.1;
        double z=face.getStepZ()==0?random.nextGaussian()*.1:face.getStepZ()*.1;
        var spark=new Spark(client,pos.getX()+.5+x,pos.getY()+.5+y,pos.getZ()+.5+z,.66F+random.nextFloat(),.65F+random.nextFloat()*.1F);
        attempted++;if(!state.getValue(VisGeneratorBlock.ENABLED))poweredSamples++;
        var mc=Minecraft.getInstance();int setting=mc.options.particles().get().getId();
        int budget=setting==2?500:setting==1?1000:2000;
        if(mc.getFps()<30)setting++;
        if(client.random.nextInt(3)<setting)return;
        if(SPARKS.size()>=budget)SPARKS.remove(0);
        SPARKS.add(spark);
        emitted++;sparkFaces|=1<<face.get3DDataValue();if(!state.getValue(VisGeneratorBlock.ENABLED))poweredEmitted++;
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){if(event.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();world(mc.level);if(world==null||mc.isPaused())return;SPARKS.removeIf(s->{s.tick();return !s.isAlive();});}
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES||world!=Minecraft.getInstance().level||SPARKS.isEmpty())return;
        var vertices=BUFFERS.getBuffer(Type.SPARK);var matrix=event.getPoseStack().last().pose();
        for(var spark:SPARKS){spark.draw(vertices,matrix,event.getCamera(),event.getPartialTick());drawn++;}BUFFERS.endBatch();
    }
    private static final class Spark extends Particle {
        final float size;final int startFrame;final boolean flipped;
        Spark(ClientLevel level,double x,double y,double z,float scale,float red){
            super(level,x,y,z);xd=yd=zd=0;gravity=0;friction=.98F;setSize(.1F,.1F);hasPhysics=true;
            lifetime=5+level.random.nextInt(5);size=.1F*scale;rCol=red;gCol=bCol=1;alpha=.8F;
            startFrame=8+level.random.nextInt(3)*16;flipped=level.random.nextBoolean();
        }
        @Override public void tick(){xo=x;yo=y;zo=z;if(age++>=lifetime)remove();}
        @Override public ParticleRenderType getRenderType(){return ParticleRenderType.CUSTOM;}
        @Override public void render(VertexConsumer out,Camera camera,float partial){draw(out,new org.joml.Matrix4f(),camera,partial);}
        void draw(VertexConsumer out,org.joml.Matrix4f matrix,Camera camera,float partial){
            int frame=startFrame+(int)Math.min(8*age/(float)lifetime,7);float u=(frame%16)/16F,v=(frame/16)/16F,cell=1/16F,left=flipped?u+cell:u,right=flipped?u:u+cell;
            var eye=camera.getPosition();float px=(float)(x-eye.x),py=(float)(y-eye.y),pz=(float)(z-eye.z);var rotation=new Quaternionf(camera.rotation());int light=getLightColor(partial);
            corner(out,matrix,rotation,-1,-1,px,py,pz,right,v+cell,light);corner(out,matrix,rotation,-1,1,px,py,pz,right,v,light);
            corner(out,matrix,rotation,1,1,px,py,pz,left,v,light);corner(out,matrix,rotation,1,-1,px,py,pz,left,v+cell,light);
        }
        private void corner(VertexConsumer out,org.joml.Matrix4f matrix,Quaternionf rotation,int sx,int sy,float px,float py,float pz,float u,float v,int light){var q=new Vector3f(sx,sy,0).rotate(rotation).mul(size).add(px,py,pz);out.vertex(matrix,q.x,q.y,q.z).color(rCol,gCol,bCol,alpha).uv(u,v).uv2(light).endVertex();}
    }
    private static final class Type extends RenderType {
        private Type(){super("thaumcraft_generator_spark",DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,VertexFormat.Mode.QUADS,4096,false,true,()->{},()->{});}
        static final RenderType SPARK=create("thaumcraft_generator_spark",DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,VertexFormat.Mode.QUADS,4096,false,true,
                CompositeState.builder().setShaderState(new ShaderStateShard(CondenserSparkShader::shader))
                .setTextureState(new TextureStateShard(ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/misc/particles.png"),false,false))
                .setTransparencyState(LIGHTNING_TRANSPARENCY).setCullState(NO_CULL).setDepthTestState(LEQUAL_DEPTH_TEST)
                .setLightmapState(LIGHTMAP).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
    }
}
