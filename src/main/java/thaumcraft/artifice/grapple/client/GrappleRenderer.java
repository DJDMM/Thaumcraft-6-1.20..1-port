package thaumcraft.artifice.grapple.client;

import java.util.*;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import thaumcraft.artifice.grapple.GrappleEntity;
import thaumcraft.catalog.entities.client.LegacyGrapplerModel;
import thaumcraft.essentia.condenser.client.CondenserSparkShader;

/** Original hook cuboids, animated glow atlas and source calcPoints rope, using native render buffers. */
public final class GrappleRenderer extends EntityRenderer<GrappleEntity> {
    private static final ResourceLocation MODEL=ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/entity/grappler.png");
    private static final ResourceLocation ROPE=ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/misc/rope.png");
    private final LegacyGrapplerModel model=new LegacyGrapplerModel();
    private static long hooksDrawn,ropeSegmentsDrawn,glowsDrawn;
    public GrappleRenderer(EntityRendererProvider.Context context){super(context);shadowRadius=0;}
    public static long hooksDrawn(){return hooksDrawn;}
    public static long ropeSegmentsDrawn(){return ropeSegmentsDrawn;}
    public static long glowsDrawn(){return glowsDrawn;}
    @Override public ResourceLocation getTextureLocation(GrappleEntity entity){return MODEL;}
    @Override public void render(GrappleEntity entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
        pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partial,entity.yRotO,entity.getYRot())-90));pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partial,entity.xRotO,entity.getXRot())));
        model.renderToBuffer(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(MODEL)),light,OverlayTexture.NO_OVERLAY,1,1,1,1);pose.popPose();hooksDrawn++;
        pose.pushPose();pose.mulPose(entityRenderDispatcher.cameraOrientation());float radius=.5F*(1+Mth.sin(entity.tickCount/5F)*.2F+.2F);
        var glow=buffers.getBuffer(Glow.TYPE);var matrix=pose.last().pose();float u=(1+entity.tickCount%6)/32F,v=7/32F,cell=1/32F;
        glow.vertex(matrix,-radius,-radius,0).color(1F,1F,1F,.21F).uv(u,v+cell).uv2(220).endVertex();
        glow.vertex(matrix,radius,-radius,0).color(1F,1F,1F,.21F).uv(u+cell,v+cell).uv2(220).endVertex();
        glow.vertex(matrix,radius,radius,0).color(1F,1F,1F,.21F).uv(u+cell,v).uv2(220).endVertex();
        glow.vertex(matrix,-radius,radius,0).color(1F,1F,1F,.21F).uv(u,v).uv2(220).endVertex();pose.popPose();glowsDrawn++;
        if(entity.getOwner() instanceof LivingEntity owner){
            var points=points(owner,entity,partial,Minecraft.getInstance().options.getCameraType().isFirstPerson());
            drawRope(points,pose,buffers.getBuffer(RenderType.entityTranslucent(ROPE)),light);
        }
        super.render(entity,yaw,partial,pose,buffers,light);
    }
    /** Exact source centerline. Its first and repeated final points are GLE support points. */
    public static List<Vec3> points(LivingEntity owner,GrappleEntity hook,float partial,boolean firstPerson){
        double tx=Mth.lerp(partial,owner.xo,owner.getX()),ty=Mth.lerp(partial,owner.yo,owner.getY()),tz=Mth.lerp(partial,owner.zo,owner.getZ());
        if(firstPerson){double yaw=Mth.lerp(partial,owner.yRotO,owner.getYRot());float angle=(float)((yaw-.5)/180*3.1415929794311523);int sign=hook.hand()==net.minecraft.world.InteractionHand.MAIN_HAND?1:-1;var look=owner.getLookAngle();
            tx+=-Mth.cos(angle)*.1F*sign+look.x/5;ty+=owner.getEyeHeight()/2.6+look.y/5;tz+=-Mth.sin(angle)*.1F*sign+look.z/5;}
        Vec3 origin=new Vec3(Mth.lerp(partial,hook.xo,hook.getX()),Mth.lerp(partial,hook.yo,hook.getY()),Mth.lerp(partial,hook.zo,hook.getZ()));
        Vec3 end=new Vec3(tx-origin.x,ty-origin.y+owner.getBbHeight()/2F,tz-origin.z);float length=(float)(end.length()*5);int steps=(int)length;
        // A normal shot has at most30 flying ticks. Bound malformed long ropes without changing ordinary source samples.
        if(!Float.isFinite(length)||steps>4096)return List.of();
        List<Vec3> points=new ArrayList<>();points.add(Vec3.ZERO);
        for(int i=1;i<steps-1;i++){float dist=i*(length/steps),s=1-i/(steps*1.25F);points.add(end.scale(i/(double)steps).add(Mth.sin(dist/10)*hook.amplitude()*s,Mth.sin(dist/8)*hook.amplitude()*s,Mth.sin(dist/6)*hook.amplitude()*s));}
        points.add(end);points.add(end);return points;
    }
    private static void drawRope(List<Vec3> path,PoseStack pose,VertexConsumer out,int light){
        if(path.size()<4)return;
        // Four-sided tube with angle-bisector joins. Native tessellation replaces removed GLE calls.
        Vec3[] old=null;float oldV=0;Vec3 normal=null;Vec3 finalNormal=Vec3.ZERO;
        float phase=(float)(path.get(path.size()-1).length()*5)-path.size();
        for(int i=1;i<path.size()-1;i++){
            Vec3 before=path.get(i).subtract(path.get(i-1)).normalize(),after=path.get(i+1).subtract(path.get(i)).normalize();
            Vec3 tangent=before.add(after).normalize();if(tangent.lengthSqr()<1e-9)tangent=before;
            if(tangent.lengthSqr()<1e-9)continue;
            if(normal==null){var up=Math.abs(tangent.y)<.9?new Vec3(0,1,0):new Vec3(1,0,0);normal=tangent.cross(up).normalize();}
            else{normal=normal.subtract(tangent.scale(normal.dot(tangent))).normalize();if(normal.lengthSqr()<1e-9)normal=tangent.cross(new Vec3(0,0,1)).normalize();}
            Vec3 binormal=tangent.cross(normal).normalize();double miter=Math.min(4,1/Math.max(.25,Math.abs(tangent.dot(before))));
            Vec3[] ring=new Vec3[4];for(int side=0;side<4;side++){double angle=side*Math.PI/2;ring[side]=path.get(i).add(normal.scale(Math.cos(angle)*.025*miter)).add(binormal.scale(Math.sin(angle)*.025*miter));}
            float v=phase+(i-1)*2;
            if(old==null)cap(out,pose,ring,tangent.scale(-1),light);
            if(old!=null){for(int side=0;side<4;side++){int next=(side+1)%4;var n=ring[side].subtract(path.get(i)).normalize();vertex(out,pose,old[side],side/4F,oldV,n,light);vertex(out,pose,old[next],(side+1)/4F,oldV,n,light);vertex(out,pose,ring[next],(side+1)/4F,v,n,light);vertex(out,pose,ring[side],side/4F,v,n,light);}ropeSegmentsDrawn++;}
            old=ring;oldV=v;finalNormal=tangent;
        }
        if(old!=null)cap(out,pose,old,finalNormal,light);
    }
    private static void cap(VertexConsumer out,PoseStack pose,Vec3[] ring,Vec3 normal,int light){for(int side=0;side<4;side++)vertex(out,pose,ring[side],side==0||side==3?0:1,side<2?0:1,normal,light);}
    private static void vertex(VertexConsumer out,PoseStack pose,Vec3 point,float u,float v,Vec3 normal,int light){out.vertex(pose.last().pose(),(float)point.x,(float)point.y,(float)point.z).color(255,255,255,255).uv(u,v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.last().normal(),(float)normal.x,(float)normal.y,(float)normal.z).endVertex();}
    private static final class Glow extends RenderType {
        private Glow(){super("thaumcraft_grapple_glow",DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,VertexFormat.Mode.QUADS,4096,false,true,()->{},()->{});}
        static final RenderType TYPE=create("thaumcraft_grapple_glow",DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,VertexFormat.Mode.QUADS,4096,false,true,
            CompositeState.builder().setShaderState(new ShaderStateShard(CondenserSparkShader::shader)).setTextureState(new TextureStateShard(ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/misc/particles.png"),false,false))
                .setTransparencyState(LIGHTNING_TRANSPARENCY).setCullState(NO_CULL).setDepthTestState(LEQUAL_DEPTH_TEST).setLightmapState(LIGHTMAP).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
    }
}
