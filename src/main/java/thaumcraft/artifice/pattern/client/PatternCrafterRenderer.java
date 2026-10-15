package thaumcraft.artifice.pattern.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thaumcraft.artifice.pattern.PatternCrafterBlock;
import thaumcraft.artifice.pattern.PatternCrafterBlockEntity;
import thaumcraft.artifice.pattern.PatternCrafterModule;

/** Released BETA26 die strip and two oppositely driven, sixteen-pixel extruded brass gears. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class PatternCrafterRenderer implements BlockEntityRenderer<PatternCrafterBlockEntity> {
    private static final ResourceLocation MODES=texture("textures/blocks/pattern_crafter_modes.png"),GEAR=texture("textures/misc/gear_brass.png");
    private static long renderedFrames,movingFrames;
    private static int seenModes;
    public PatternCrafterRenderer(BlockEntityRendererProvider.Context context) {}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(PatternCrafterModule.BLOCK_ENTITY.get(),PatternCrafterRenderer::new);
    }
    public static long renderedFrames(){return renderedFrames;}
    public static long movingFrames(){return movingFrames;}
    public static int seenModes(){return seenModes;}
    @Override public void render(PatternCrafterBlockEntity tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        renderedFrames++;seenModes|=1<<tile.type;
        if(tile.rp>0)movingFrames++;
        // The released renderer reads rot directly; it does not add a partial-tick interpolation.
        renderHardware(tile.getBlockState(),tile.type,tile.rot,pose,buffers,light,overlay);
    }
    /** Also used for the original neutral mode-zero item preview; world rendering uses the native tile. */
    public static void renderHardware(BlockState state,int type,float rotation,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        Direction direction=state.hasProperty(PatternCrafterBlock.FACING)?state.getValue(PatternCrafterBlock.FACING):Direction.SOUTH;
        pose.pushPose();pose.translate(.5,.75,.5);
        pose.mulPose(Axis.YP.rotationDegrees(switch(direction){case EAST->90;case WEST->270;case NORTH->180;default->0;}));
        pose.pushPose();pose.mulPose(Axis.ZP.rotationDegrees(90));pose.translate(0,0,-.5001);
        var panel=buffers.getBuffer(RenderType.entityTranslucent(MODES));
        float u=Math.floorMod(type,10)/10F,v=type;
        // UtilsFX's frame/gridY intentionally repeats V by an integer on the ten-by-one strip.
        vertex(panel,pose.last(),-.25F,.25F,0,u+.1F,v+1,light,overlay,0,0,-1);
        vertex(panel,pose.last(),.25F,.25F,0,u+.1F,v,light,overlay,0,0,-1);
        vertex(panel,pose.last(),.25F,-.25F,0,u,v,light,overlay,0,0,-1);
        vertex(panel,pose.last(),-.25F,-.25F,0,u,v+1,light,overlay,0,0,-1);
        pose.popPose();
        var gear=buffers.getBuffer(RenderType.entityCutoutNoCull(GEAR));
        for(int side:new int[]{-1,1}) {
            pose.pushPose();pose.translate(side*.2,-.40625,.05);
            pose.mulPose(Axis.ZP.rotationDegrees(side*rotation%360));
            pose.scale(.5F,.5F,1);pose.translate(-.5,-.5,0);
            extrudedSprite(gear,pose.last(),light,overlay,.1F);pose.popPose();
        }
        pose.popPose();
    }
    private static ResourceLocation texture(String name){return ResourceLocation.fromNamespaceAndPath("thaumcraft",name);}
    private static void vertex(VertexConsumer v,PoseStack.Pose p,float x,float y,float z,float u,float w,int light,int overlay,float nx,float ny,float nz) {
        v.vertex(p.pose(),x,y,z).color(255,255,255,255).uv(u,w).overlayCoords(overlay).uv2(light).normal(p.normal(),nx,ny,nz).endVertex();
    }
    /** Original UtilsFX.renderTextureIn3D(1,0,0,1,16,16,.1), including the inset half-texel side UVs. */
    private static void extrudedSprite(VertexConsumer v,PoseStack.Pose p,int light,int overlay,float thickness) {
        vertex(v,p,0,0,0,1,1,light,overlay,0,0,1);vertex(v,p,1,0,0,0,1,light,overlay,0,0,1);
        vertex(v,p,1,1,0,0,0,light,overlay,0,0,1);vertex(v,p,0,1,0,1,0,light,overlay,0,0,1);
        vertex(v,p,0,1,-thickness,1,0,light,overlay,0,0,-1);vertex(v,p,1,1,-thickness,0,0,light,overlay,0,0,-1);
        vertex(v,p,1,0,-thickness,0,1,light,overlay,0,0,-1);vertex(v,p,0,0,-thickness,1,1,light,overlay,0,0,-1);
        for(int i=0;i<16;i++) {
            float f=i/16F,next=f+1/16F,uv=1-f-1/32F;
            vertex(v,p,f,0,-thickness,uv,1,light,overlay,-1,0,0);vertex(v,p,f,0,0,uv,1,light,overlay,-1,0,0);
            vertex(v,p,f,1,0,uv,0,light,overlay,-1,0,0);vertex(v,p,f,1,-thickness,uv,0,light,overlay,-1,0,0);
            vertex(v,p,next,1,-thickness,uv,0,light,overlay,1,0,0);vertex(v,p,next,1,0,uv,0,light,overlay,1,0,0);
            vertex(v,p,next,0,0,uv,1,light,overlay,1,0,0);vertex(v,p,next,0,-thickness,uv,1,light,overlay,1,0,0);
            vertex(v,p,0,next,0,1,uv,light,overlay,0,1,0);vertex(v,p,1,next,0,0,uv,light,overlay,0,1,0);
            vertex(v,p,1,next,-thickness,0,uv,light,overlay,0,1,0);vertex(v,p,0,next,-thickness,1,uv,light,overlay,0,1,0);
            vertex(v,p,1,f,0,0,uv,light,overlay,0,-1,0);vertex(v,p,0,f,0,1,uv,light,overlay,0,-1,0);
            vertex(v,p,0,f,-thickness,1,uv,light,overlay,0,-1,0);vertex(v,p,1,f,-thickness,0,uv,light,overlay,0,-1,0);
        }
    }
}
