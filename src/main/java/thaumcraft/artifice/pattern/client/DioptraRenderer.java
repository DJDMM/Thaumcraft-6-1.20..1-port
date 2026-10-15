package thaumcraft.artifice.pattern.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thaumcraft.artifice.dioptra.DioptraBlock;
import thaumcraft.artifice.dioptra.DioptraBlockEntity;
import thaumcraft.artifice.dioptra.DioptraModule;

/** Original thirteen-by-thirteen aura samples,144 height cells,48 fading edges and four side screens. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class DioptraRenderer implements BlockEntityRenderer<DioptraBlockEntity> {
    private static long visFrames,fluxFrames,nonzeroFrames;
    public DioptraRenderer(BlockEntityRendererProvider.Context context) {}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(DioptraModule.BLOCK_ENTITY.get(),DioptraRenderer::new);
    }
    public static long visFrames(){return visFrames;}
    public static long fluxFrames(){return fluxFrames;}
    public static long nonzeroFrames(){return nonzeroFrames;}
    @Override public void render(DioptraBlockEntity tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        if(tile.grid_amt.length!=169)return;
        boolean enabled=tile.getBlockState().getValue(DioptraBlock.ENABLED);
        if(enabled)visFrames++;else fluxFrames++;
        for(byte sample:tile.grid_amt)if(sample!=0){nonzeroFrames++;break;}
        var camera=Minecraft.getInstance().getCameraEntity();float t=camera==null?0:camera.tickCount+partial;
        float red=Mth.sin(t/12)*.05F+.85F,green=Mth.sin(t/11)*.05F+(enabled?.9F:.45F),blue=Mth.sin(t/10)*.05F+.95F;
        // java.awt.Color(float...) in the release rounds each component to the nearest byte.
        float r=Math.round(red*.8F*255)/255F,g=Math.round(Mth.clamp(green,0,1)*255)/255F,b=Math.round(blue*255)/255F;
        pose.pushPose();pose.translate(.5,.5,.5);pose.pushPose();pose.translate(-.495,.501,-.495);pose.scale(.99F,1,.99F);
        var grid=buffers.getBuffer(Types.GRID);
        for(int x=0;x<12;x++)for(int z=0;z<12;z++) {
            float x0=x/12F,x1=(x+1)/12F,z0=z/12F,z1=(z+1)/12F;
            float h00=tile.grid_amt[x+z*13]/96F,h10=tile.grid_amt[x+1+z*13]/96F,h11=tile.grid_amt[x+1+(z+1)*13]/96F,h01=tile.grid_amt[x+(z+1)*13]/96F;
            double distance=Math.sqrt((x-6)*(x-6)+(z-6)*(z-6));int brightness=(int)(200+Mth.sin((float)((tile.counter-distance*10)/8))*15);
            quad(grid,pose,new float[][]{{x0,h00,z0,0,1,.9F},{x1,h10,z0,1,1,.9F},{x1,h11,z1,1,0,.9F},{x0,h01,z1,0,0,.9F}},true,r,g,b,brightness);
            if(x==0)quad(grid,pose,new float[][]{{0,0,z0,0,1,0},{0,h00,z0,1,1,.9F},{0,h01,z1,1,0,.9F},{0,0,z1,0,0,0}},true,r,g,b,brightness);
            if(x==11)quad(grid,pose,new float[][]{{1,0,z0,0,1,0},{1,h10,z0,1,1,.9F},{1,h11,z1,1,0,.9F},{1,0,z1,0,0,0}},false,r,g,b,brightness);
            if(z==0)quad(grid,pose,new float[][]{{x0,0,0,0,1,0},{x1,0,0,1,1,0},{x1,h10,0,1,0,.9F},{x0,h00,0,0,0,.9F}},true,r,g,b,brightness);
            if(z==11)quad(grid,pose,new float[][]{{x0,0,1,0,1,0},{x1,0,1,1,1,0},{x1,h11,1,1,0,.9F},{x0,h01,1,0,0,.9F}},false,r,g,b,brightness);
        }
        pose.popPose();pose.pushPose();pose.translate(0,1,0);pose.mulPose(Axis.ZP.rotationDegrees(270));
        var sides=buffers.getBuffer(Types.SIDES);
        for(int side=0;side<4;side++) {
            pose.pushPose();pose.mulPose(Axis.XP.rotationDegrees(90*side));pose.translate(0,0,-.5);
            quad(sides,pose,new float[][]{{-.5F,.5F,0,1,1,.8F},{.5F,.5F,0,1,0,.8F},{.5F,-.5F,0,0,0,.8F},{-.5F,-.5F,0,0,1,.8F}},false,red,green,blue,220);pose.popPose();
        }
        pose.popPose();pose.popPose();
    }
    private static void quad(VertexConsumer out,PoseStack pose,float[][] points,boolean flip,float r,float g,float b,int light) {
        for(int i=0;i<4;i++){var p=points[flip?3-i:i];out.vertex(pose.last().pose(),p[0],p[1],p[2]).color(r,g,b,((int)(p[5]*255))/255F).uv(p[3],p[4]).uv2(light).endVertex();}
    }
    private static final class Types extends RenderType {
        private Types(){super("thaumcraft_dioptra",DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,VertexFormat.Mode.QUADS,32768,false,true,()->{},()->{});}
        private static RenderType type(String name,String texture,boolean cull) {
            // Native text shader has ordinary world fog/lightmap and the release's .1 alpha cutoff.
            // The see-through shader omits both and is deliberately not used here.
            return create(name,DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,VertexFormat.Mode.QUADS,32768,false,true,
                    CompositeState.builder().setShaderState(RENDERTYPE_TEXT_SHADER)
                    .setTextureState(new TextureStateShard(ResourceLocation.fromNamespaceAndPath("thaumcraft",texture),false,false))
                    .setTransparencyState(LIGHTNING_TRANSPARENCY).setCullState(cull?CULL:NO_CULL)
                    .setDepthTestState(LEQUAL_DEPTH_TEST).setLightmapState(LIGHTMAP).setWriteMaskState(COLOR_DEPTH_WRITE).createCompositeState(false));
        }
        static final RenderType GRID=type("thaumcraft_dioptra_grid","textures/misc/gridblock.png",true);
        static final RenderType SIDES=type("thaumcraft_dioptra_sides","textures/models/dioptra_side.png",false);
    }
}
