package thaumcraft.artifice.construct.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import thaumcraft.artifice.construct.BoreEntity;
import thaumcraft.catalog.entities.VisualMobEntity;
import thaumcraft.catalog.entities.client.LegacyArcaneBoreModel;

/** Released TC6 tripod, translucent aiming head and fixed five-block additive beam. */
public final class BoreRenderers extends MobRenderer<VisualMobEntity,LegacyArcaneBoreModel> {
    private static final ResourceLocation TEXTURE=texture("textures/entity/arcanebore.png");
    private static long beamDraws;
    public static long actualBeamDraws(){return beamDraws;}
    private static ResourceLocation texture(String path){return ResourceLocation.fromNamespaceAndPath("thaumcraft",path);}
    public BoreRenderers(EntityRendererProvider.Context context) {
        super(context,new LegacyArcaneBoreModel(),.5F);
        addLayer(new RenderLayer<VisualMobEntity,LegacyArcaneBoreModel>(this) {
            @Override public void render(PoseStack pose,MultiBufferSource buffers,int light,VisualMobEntity entity,
                    float swing,float amount,float partial,float age,float yaw,float pitch) {
                if(entity.isInvisible())return;
                getParentModel().renderHead(pose,buffers.getBuffer(RenderType.entityTranslucent(TEXTURE)),light,
                        LivingEntityRenderer.getOverlayCoords(entity,0),1,1,1,1);
            }
        });
    }
    @Override public ResourceLocation getTextureLocation(VisualMobEntity entity){return TEXTURE;}
    @Override public void render(VisualMobEntity entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light) {
        // RenderArcaneBore fixes the tripod in world axes. Only the original base part aims.
        entity.yBodyRot=entity.yBodyRotO=0;
        super.render(entity,yaw,partial,pose,buffers,light);
        if(!(entity instanceof BoreEntity bore)||!bore.clientDigging||!bore.isActive()||!bore.validInventory())return;
        beamDraws++;
        pose.pushPose();
        float pitch=bore.getXRot(),headYaw=bore.yHeadRot;
        double angle=pitch*Mth.DEG_TO_RAD;
        Vec3 muzzle=new Vec3(.5*Math.cos(angle)+.075*Math.sin(angle),.075*Math.cos(angle)-.5*Math.sin(angle),0)
                .yRot(-(headYaw+90)*Mth.DEG_TO_RAD);
        pose.translate(muzzle.x,bore.getEyeHeight()+muzzle.y,muzzle.z);
        pose.pushPose();pose.mulPose(entityRenderDispatcher.cameraOrientation());
        int frame=96+bore.tickCount%32;
        float u=(frame%32)/32F,v=(frame/32)/32F;
        VertexConsumer node=buffers.getBuffer(BeamTypes.NODE);
        vertex(node,pose,-.5F,-.5F,0,u+1/32F,v+1/32F,.8F,210);
        vertex(node,pose,-.5F,.5F,0,u+1/32F,v,.8F,210);
        vertex(node,pose,.5F,.5F,0,u,v,.8F,210);
        vertex(node,pose,.5F,-.5F,0,u,v+1/32F,.8F,210);
        pose.popPose();
        float rot=(bore.level().getDayTime()%72)*5+5*partial;
        float time=-(bore.tickCount+partial),scroll=-time*.2F-Mth.floor(-time*.1F);
        float bodyYaw=Mth.lerp(partial,bore.yRotO,bore.getYRot());
        float bodyPitch=Mth.lerp(partial,bore.xRotO,bore.getXRot());
        pose.mulPose(Axis.XN.rotationDegrees(90));
        pose.mulPose(Axis.ZN.rotationDegrees(180+bodyYaw));
        pose.mulPose(Axis.XN.rotationDegrees(bodyPitch));
        pose.mulPose(Axis.YP.rotationDegrees(rot));
        VertexConsumer beam=buffers.getBuffer(BeamTypes.BEAM);
        // Source keeps beamLength=5 independently of the selected block distance.
        for(int panel=0;panel<3;panel++) {
            pose.mulPose(Axis.YP.rotationDegrees(60));float start=-1+scroll+panel/3F;
            vertex(beam,pose,0,5,0,1,5+start,.4F,200);
            vertex(beam,pose,-.15F,0,0,1,start,.4F,200);
            vertex(beam,pose,.15F,0,0,0,start,.4F,200);
            vertex(beam,pose,0,5,0,0,5+start,.4F,200);
        }
        pose.popPose();
    }
    private static void vertex(VertexConsumer out,PoseStack pose,float x,float y,float z,float u,float v,float alpha,int light) {
        out.vertex(pose.last().pose(),x,y,z).color(0F,1F,.4F,alpha).uv(u,v).uv2(light).endVertex();
    }
    /** Native state objects restore GL state and retain source SRC_ALPHA/ONE/no-depth-write. */
    private static final class BeamTypes extends RenderType {
        private BeamTypes(){super("thaumcraft_bore",DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,VertexFormat.Mode.QUADS,1024,false,true,()->{},()->{});}
        private static RenderType beam(String name,String file) {
            return create(name,DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,VertexFormat.Mode.QUADS,1024,false,true,
                    CompositeState.builder().setShaderState(RENDERTYPE_TEXT_SEE_THROUGH_SHADER)
                            .setTextureState(new TextureStateShard(texture(file),false,false)).setTransparencyState(LIGHTNING_TRANSPARENCY)
                            .setDepthTestState(LEQUAL_DEPTH_TEST).setCullState(NO_CULL).setLightmapState(LIGHTMAP)
                            .setWriteMaskState(COLOR_WRITE).createCompositeState(false));
        }
        private static final RenderType NODE=beam("thaumcraft_bore_node","textures/misc/auranodes.png");
        private static final RenderType BEAM=beam("thaumcraft_bore_beam","textures/misc/beam1.png");
    }
}
