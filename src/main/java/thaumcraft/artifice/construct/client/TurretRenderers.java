package thaumcraft.artifice.construct.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import thaumcraft.artifice.construct.TurretEntity;
import thaumcraft.catalog.entities.VisualMobEntity;
import thaumcraft.catalog.entities.client.LegacyCrossbowModel;
import thaumcraft.catalog.entities.client.ObjMesh;

/** BETA26 ModelCrossbow and original eight-group advanced OBJ, with native animation state. */
public final class TurretRenderers {
    private static ResourceLocation texture(String path) { return ResourceLocation.fromNamespaceAndPath("thaumcraft",path); }
    private TurretRenderers() {}

    public static final class Basic extends MobRenderer<VisualMobEntity,LegacyCrossbowModel> {
        private static final ResourceLocation TEXTURE=texture("textures/entity/crossbow.png");
        public Basic(EntityRendererProvider.Context context) { super(context,new LegacyCrossbowModel(),.5F); }
        @Override public ResourceLocation getTextureLocation(VisualMobEntity entity) { return TEXTURE; }
        @Override public void render(VisualMobEntity entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light) {
            if (entity instanceof TurretEntity turret) {
                // The original tripod remains fixed in world axes; only its weapon aims.
                turret.yBodyRot=turret.yBodyRotO=0;
                model.setLoadProgress(turret.getLoadProgress(partial));
                model.setFireProgress(turret.getFireProgress(partial));
            }
            super.render(entity,yaw,partial,pose,buffers,light);
        }
    }

    public static final class Advanced extends EntityRenderer<VisualMobEntity> {
        private static final ResourceLocation TEXTURE=texture("textures/entity/crossbow_advanced.png");
        public Advanced(EntityRendererProvider.Context context) { super(context);shadowRadius=.5F; }
        @Override public ResourceLocation getTextureLocation(VisualMobEntity entity) { return TEXTURE; }
        private static void mesh(PoseStack pose,MultiBufferSource buffers,int light,int tint,String... groups) {
            ObjMesh.get("crossbow_advanced").renderStyled(pose,buffers,TEXTURE,null,light,tint,RenderType::entityTranslucent,groups);
        }
        @Override public void render(VisualMobEntity entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light) {
            if (!(entity instanceof TurretEntity turret)) { super.render(entity,yaw,partial,pose,buffers,light);return; }
            pose.pushPose();pose.translate(0,.75,0);
            pose.pushPose();
            if(turret.getVehicle() instanceof AbstractMinecart)pose.scale(.66F,.75F,.66F);
            mesh(pose,buffers,light,0xffffffff,"legs");pose.popPose();
            pose.pushPose();
            int tint=0xffffffff;
            if(turret.hurtTime>0) {
                tint=0xffff8080;float jiggle=turret.hurtTime/500F;
                pose.translate(turret.getRandom().nextGaussian()*jiggle,turret.getRandom().nextGaussian()*jiggle,turret.getRandom().nextGaussian()*jiggle);
            }
            // Preserve original interpolation and negative-Y rotation for OBJ coordinates.
            pose.mulPose(Axis.YN.rotationDegrees(Mth.lerp(partial,turret.yHeadRotO,turret.yHeadRot)));
            pose.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partial,turret.xRotO,turret.getXRot())));
            mesh(pose,buffers,light,tint,"mech","box","shield","brain");
            pose.pushPose();pose.translate(0,0,Mth.sin(Mth.sqrt(turret.getLoadProgress(partial))*Mth.TWO_PI)/12F);
            mesh(pose,buffers,light,tint,"loader");pose.popPose();
            float swing=Mth.sin(Mth.sqrt(turret.getFireProgress(partial))*Mth.TWO_PI)*20F;
            pose.translate(0,0,.375);
            pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees(swing));mesh(pose,buffers,light,tint,"bow1");pose.popPose();
            pose.pushPose();pose.mulPose(Axis.YN.rotationDegrees(swing));mesh(pose,buffers,light,tint,"bow2");pose.popPose();
            pose.popPose();pose.popPose();super.render(entity,yaw,partial,pose,buffers,light);
        }
    }
}
