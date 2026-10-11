package thaumcraft.artifice.construct.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import thaumcraft.artifice.construct.BoreModule;
import thaumcraft.artifice.construct.BoreEntity;
import org.joml.Matrix4f;
import org.joml.Vector3f;

@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class BoreClientEvents {
    private BoreClientEvents(){}
    @SubscribeEvent public static void setup(FMLClientSetupEvent event){event.enqueueWork(()->MenuScreens.register(BoreModule.MENU.get(),BoreScreen::new));}

    /** Source's delay scheduled FX follow the captured block even after native harvesting. */
    @Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
    public static final class Effects {
        private static final Map<BoreEntity,Schedule> SCHEDULES=new IdentityHashMap<>();
        private static final List<PulledParticle> PARTICLES=new ArrayList<>();
        private static final MultiBufferSource.BufferSource BUFFERS=MultiBufferSource.immediate(new com.mojang.blaze3d.vertex.BufferBuilder(4096));
        private static ClientLevel world;
        private static long emitted;
        public static long actualParticleEmissions(){return emitted;}
        @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
            if(event.phase!=TickEvent.Phase.END)return;
            var mc=Minecraft.getInstance();
            if(mc.level!=world){world=mc.level;SCHEDULES.clear();PARTICLES.clear();}
            if(world==null||mc.isPaused())return;
            PARTICLES.removeIf(p->{p.tick();return !p.isAlive();});
            SCHEDULES.keySet().removeIf(b->b.isRemoved()||b.level()!=world);
            for(var entity:world.entitiesForRendering())if(entity instanceof BoreEntity bore) {
                Schedule previous=SCHEDULES.get(bore);
                if(previous==null||previous.epoch!=bore.getDigEpoch()) {
                    BlockPos target=bore.getDigTarget();int delay=bore.getDigDelayMax();
                    if(target==null||delay<=0||!world.hasChunkAt(target))continue;
                    var state=world.getBlockState(target);if(state.isAir())continue;
                    previous=new Schedule(bore.getDigEpoch(),target.immutable(),state,delay);SCHEDULES.put(bore,previous);
                }
                if(previous.remaining--<=0)continue;
                if(mc.getCameraEntity()==null||mc.getCameraEntity().distanceToSqr(bore)>64*64)continue;
                for(int i=0;i<50F/previous.delay;i++) {
                    boolean sparkle=world.random.nextInt(4)==0;
                    // Bounds are only for a malformed/addon-heavy visual scene, never physical mining.
                    if(PARTICLES.size()>=8192)break;
                    PARTICLES.add(new PulledParticle(world,previous.pos,previous.state,bore,sparkle));emitted++;
                }
            }
        }
        @SubscribeEvent public static void render(RenderLevelStageEvent event) {
            if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES||world!=Minecraft.getInstance().level||PARTICLES.isEmpty())return;
            for(var particle:PARTICLES)particle.draw(BUFFERS.getBuffer(particle.sparkle?ParticleTypes.SPARKLE:ParticleTypes.DEBRIS),
                    event.getPoseStack().last().pose(),event.getCamera(),event.getPartialTick());
            BUFFERS.endBatch();
        }
        private static final class Schedule {
            final int epoch,delay;final BlockPos pos;final BlockState state;int remaining;
            Schedule(int epoch,BlockPos pos,BlockState state,int delay){this.epoch=epoch;this.pos=pos;this.state=state;this.delay=delay;remaining=delay;}
        }
    }
    private static final class PulledParticle extends Particle {
        final BoreEntity target;final boolean sparkle;final TextureAtlasSprite sprite;final float jitterU,jitterV;
        float scale;
        PulledParticle(ClientLevel level,BlockPos pos,BlockState state,BoreEntity target,boolean sparkle) {
            super(level,pos.getX()+level.random.nextFloat(),pos.getY()+level.random.nextFloat(),pos.getZ()+level.random.nextFloat());
            this.target=target;this.sparkle=sparkle;
            sprite=sparkle?null:Minecraft.getInstance().getBlockRenderer().getBlockModel(state).getParticleIcon();
            jitterU=random.nextFloat()*3;jitterV=random.nextFloat()*3;
            scale=sparkle?random.nextFloat()*.5F+.5F:random.nextFloat()*.3F+.4F;
            rCol=sparkle?.2F:.6F;gCol=sparkle?.6F+random.nextFloat()*.3F:.6F;bCol=sparkle?.2F:.6F;
            // Sparkle's entity constructor starts at the eye; the block-debris
            // constructor receives the feet before setTarget supplies the eye.
            double distance=target.position().add(0,sparkle?target.getEyeHeight():0,0).distanceTo(new net.minecraft.world.phys.Vec3(x,y,z));
            int base=Math.max(1,(int)(distance*10));lifetime=base/2+random.nextInt(base);
            xd=random.nextGaussian()*.01;yd=random.nextGaussian()*.01;zd=random.nextGaussian()*.01;
        }
        @Override public void tick() {
            xo=x;yo=y;zo=z;
            double tx=target.getX(),ty=target.getY()+target.getEyeHeight(),tz=target.getZ();
            if(target.isRemoved()||age++>=lifetime||Mth.floor(x)==Mth.floor(tx)&&Mth.floor(y)==Mth.floor(ty)&&Mth.floor(z)==Mth.floor(tz)){remove();return;}
            move(xd,yd,zd);xd*=.985;yd*=.95;zd*=.985;
            double dx=tx-x,dy=ty-y,dz=tz-z,d=Math.sqrt(dx*dx+dy*dy+dz*dz);
            if(d<1e-7){remove();return;}double clamp=Math.min(.25,d/15);
            if(d<2)scale*=.9F;
            xd=Mth.clamp(xd+dx/d*clamp,-clamp,clamp);yd=Mth.clamp(yd+dy/d*clamp,-clamp,clamp);zd=Mth.clamp(zd+dz/d*clamp,-clamp,clamp);
            double noise=sparkle?.01:.005;xd+=random.nextGaussian()*noise;yd+=random.nextGaussian()*noise;zd+=random.nextGaussian()*noise;
        }
        void draw(VertexConsumer out,Matrix4f pose,Camera camera,float partial) {
            float u0=sparkle?(age%4)/64F:sprite.getU(jitterU*4),u1=sparkle?u0+1/64F:sprite.getU((jitterU+1)*4);
            float v0=sparkle?4/64F:sprite.getV(jitterV*4),v1=sparkle?v0+1/64F:sprite.getV((jitterV+1)*4);
            var eye=camera.getPosition();float px=(float)(Mth.lerp(partial,xo,x)-eye.x),py=(float)(Mth.lerp(partial,yo,y)-eye.y),pz=(float)(Mth.lerp(partial,zo,z)-eye.z);
            float size=.1F*scale*(sparkle?Mth.sin(age/3F)*.5F+1:1);
            int light=sparkle?240:LevelRenderer.getLightColor(level,BlockPos.containing(x,y,z));
            corner(out,pose,camera,-1,-1,px,py,pz,size,u1,v1,light);corner(out,pose,camera,-1,1,px,py,pz,size,u1,v0,light);
            corner(out,pose,camera,1,1,px,py,pz,size,u0,v0,light);corner(out,pose,camera,1,-1,px,py,pz,size,u0,v1,light);
        }
        private void corner(VertexConsumer out,Matrix4f pose,Camera camera,int sx,int sy,float px,float py,float pz,float size,float u,float v,int light) {
            var corner=new Vector3f(sx,sy,0).rotate(camera.rotation()).mul(size).add(px,py,pz);
            out.vertex(pose,corner.x,corner.y,corner.z).color(rCol,gCol,bCol,1).uv(u,v).uv2(light).endVertex();
        }
        @Override public ParticleRenderType getRenderType(){return ParticleRenderType.CUSTOM;}
        @Override public void render(VertexConsumer out,Camera camera,float partial){draw(out,new Matrix4f(),camera,partial);}
    }
    private static final class ParticleTypes extends RenderType {
        private ParticleTypes(){super("thaumcraft_bore_particles",DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,VertexFormat.Mode.QUADS,4096,false,true,()->{},()->{});}
        private static RenderType type(String name,ResourceLocation texture,boolean sparkle) {
            return create(name,DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,VertexFormat.Mode.QUADS,4096,false,true,
                    CompositeState.builder().setShaderState(RENDERTYPE_TEXT_SEE_THROUGH_SHADER).setTextureState(new TextureStateShard(texture,false,false))
                            .setTransparencyState(sparkle?LIGHTNING_TRANSPARENCY:TRANSLUCENT_TRANSPARENCY).setDepthTestState(LEQUAL_DEPTH_TEST)
                            .setCullState(NO_CULL).setLightmapState(LIGHTMAP).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
        }
        static final RenderType SPARKLE=type("thaumcraft_bore_sparkle",ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/misc/particles.png"),true);
        static final RenderType DEBRIS=type("thaumcraft_bore_debris",TextureAtlas.LOCATION_BLOCKS,false);
    }
}
