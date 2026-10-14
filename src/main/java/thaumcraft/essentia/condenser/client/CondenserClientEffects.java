package thaumcraft.essentia.condenser.client;

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
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import thaumcraft.essentia.condenser.CondenserBlock;
import thaumcraft.essentia.condenser.CondenserBlockEntity;

/** BETA26 TileCondenser / FXDispatcher.spark; observes physical client tiles only. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public final class CondenserClientEffects {
    private static final List<Spark> SPARKS=new ArrayList<>();
    private static final Map<BlockPos,CondenserBlockEntity> TILES=new HashMap<>();
    private static final Map<BlockPos,Integer> LAST_TICK=new HashMap<>();
    private static final MultiBufferSource.BufferSource BUFFERS=MultiBufferSource.immediate(new BufferBuilder(4096));
    private static ClientLevel world;
    private static int discovery;
    private static long emitted,drawn;
    private static BlockPos lastSource;
    private CondenserClientEffects() {}
    public static long emittedSparks() { return emitted; }
    public static long drawnSparks() { return drawn; }
    public static int activeSparks() { return SPARKS.size(); }
    public static BlockPos lastSource() { return lastSource; }
    private static void world(ClientLevel level) { if(world!=level){world=level;SPARKS.clear();TILES.clear();LAST_TICK.clear();discovery=0;emitted=drawn=0;lastSource=null;} }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();world(mc.level);
        if(world==null||mc.player==null||mc.isPaused())return;
        SPARKS.removeIf(p->{p.tick();return !p.isAlive();});
        int cx=mc.player.chunkPosition().x,cz=mc.player.chunkPosition().z;
        boolean refresh=discovery++%20==0;
        if(refresh){
            // Inspect already loaded client chunks; never create a chunk or hold tickets.
            for(int x=cx-3;x<=cx+3;x++)for(int z=cz-3;z<=cz+3;z++){
                var chunk=world.getChunkSource().getChunk(x,z,net.minecraft.world.level.chunk.ChunkStatus.FULL,false);if(chunk==null)continue;
                for(var tile:chunk.getBlockEntities().values())if(tile instanceof CondenserBlockEntity condenser)TILES.put(tile.getBlockPos().immutable(),condenser);
            }
        }
        TILES.entrySet().removeIf(e->Math.abs((e.getKey().getX()>>4)-cx)>3||Math.abs((e.getKey().getZ()>>4)-cz)>3||e.getValue().isRemoved()||!world.hasChunkAt(e.getKey())||world.getBlockEntity(e.getKey())!=e.getValue());
        LAST_TICK.keySet().retainAll(TILES.keySet());
        // Modern client state packets bypass Block.onPlace; observe the loaded physical topology
        // before spark eligibility without resetting the tile clock or either synced reservoir.
        if(refresh)for(var tile:TILES.values())tile.triggerCheck();
        for(var entry:TILES.entrySet()){
            var tile=entry.getValue();int tick=tile.ticks();Integer previous=LAST_TICK.put(entry.getKey(),tick);
            if(previous!=null&&previous==tick)continue;
            var state=tile.getBlockState();var clean=tile.cleanLatticePositions();
            if(!state.hasProperty(CondenserBlock.ENABLED)||!state.getValue(CondenserBlock.ENABLED)||tile.latticeCount()<=0||tile.storedEssentia()<=0||clean.isEmpty()||tick%Math.max(3,tile.interval()/50)!=0||SPARKS.size()>=4096)continue;
            var pos=clean.get(world.random.nextInt(clean.size()));
            if(!world.hasChunkAt(pos)||world.getBlockState(pos).getBlock()!=thaumcraft.catalog.blocks.CatalogBlocks.block("condenser_lattice"))continue;
            var random=world.random;float size=4.5F+random.nextFloat(),red=.33F+random.nextFloat()*.66F,green=.33F+random.nextFloat()*.66F,blue=.33F+random.nextFloat()*.66F;
            SPARKS.add(new Spark(world,pos,size,red,green,blue));lastSource=pos.immutable();emitted++;
        }
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES||world!=Minecraft.getInstance().level||SPARKS.isEmpty())return;
        var vertices=BUFFERS.getBuffer(Type.SPARK);var matrix=event.getPoseStack().last().pose();
        for(var spark:SPARKS){spark.draw(vertices,matrix,event.getCamera(),event.getPartialTick());drawn++;}BUFFERS.endBatch();
    }
    private static final class Spark extends Particle {
        final float size;
        final int startFrame;
        final boolean flipped;
        Spark(ClientLevel level,BlockPos pos,float scale,float red,float green,float blue) {
            super(level,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5);
            xd=yd=zd=0;gravity=0;friction=.98F;setSize(.1F,.1F);hasPhysics=true;
            lifetime=5+level.random.nextInt(5);size=.1F*scale;rCol=red;gCol=green;bCol=blue;alpha=.8F;
            startFrame=8+level.random.nextInt(3)*16;flipped=level.random.nextBoolean();
        }
        @Override public void tick() { xo=x;yo=y;zo=z;if(age++>=lifetime)remove(); }
        @Override public ParticleRenderType getRenderType() { return ParticleRenderType.CUSTOM; }
        @Override public void render(VertexConsumer out,Camera camera,float partial) { draw(out,new org.joml.Matrix4f(),camera,partial); }
        void draw(VertexConsumer out,org.joml.Matrix4f matrix,Camera camera,float partial) {
            int frame=startFrame+(int)Math.min(8*age/(float)lifetime,7);
            float u=(frame%16)/16F,v=(frame/16)/16F,cell=1/16F;
            float left=flipped?u+cell:u,right=flipped?u:u+cell;
            // FXGeneric.setRBGColorF also sets dr/dg/db to these same values.
            float color=1;
            var eye=camera.getPosition();float px=(float)(x-eye.x),py=(float)(y-eye.y),pz=(float)(z-eye.z);
            var orientation=new Quaternionf(camera.rotation());int light=getLightColor(partial);
            corner(out,matrix,orientation,-1,-1,px,py,pz,right,v+cell,color,light);
            corner(out,matrix,orientation,-1,1,px,py,pz,right,v,color,light);
            corner(out,matrix,orientation,1,1,px,py,pz,left,v,color,light);
            corner(out,matrix,orientation,1,-1,px,py,pz,left,v+cell,color,light);
        }
        private void corner(VertexConsumer out,org.joml.Matrix4f matrix,Quaternionf rotation,int sx,int sy,float px,float py,float pz,float u,float v,float color,int light) {
            var q=new Vector3f(sx,sy,0).rotate(rotation).mul(size).add(px,py,pz);
            out.vertex(matrix,q.x,q.y,q.z).color(rCol*color,gCol*color,bCol*color,alpha).uv(u,v).uv2(light).endVertex();
        }
    }
    private static final class Type extends RenderType {
        private Type() { super("thaumcraft_condenser_spark",DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,VertexFormat.Mode.QUADS,4096,false,true,()->{},()->{}); }
        static final RenderType SPARK=create("thaumcraft_condenser_spark",DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,VertexFormat.Mode.QUADS,4096,false,true,
                CompositeState.builder().setShaderState(new ShaderStateShard(CondenserSparkShader::shader))
                        .setTextureState(new TextureStateShard(ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/misc/particles.png"),false,false))
                        .setTransparencyState(LIGHTNING_TRANSPARENCY).setCullState(NO_CULL).setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setLightmapState(LIGHTMAP).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
    }
}
