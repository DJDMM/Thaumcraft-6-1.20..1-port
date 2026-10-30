package thaumcraft.auromancy.media.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thaumcraft.auromancy.media.*;
import thaumcraft.catalog.entities.client.LegacyGrapplerModel;

/** Original geometry only; actual entity client ticks own the original particle emitters. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class FocusMediaRenderers {
    private FocusMediaRenderers(){}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event){
        event.registerEntityRenderer(FocusMediaModule.CLOUD.get(),Cloud::new);event.registerEntityRenderer(FocusMediaModule.MINE.get(),Mine::new);event.registerEntityRenderer(FocusMediaModule.SPELL_BAT.get(),Bat::new);
        event.registerEntityRenderer(FocusMediaModule.ORIGINAL_SPELL_BAT.get(),Bat::new);
    }
    private static ResourceLocation texture(String file){return ResourceLocation.fromNamespaceAndPath("thaumcraft",file);}
    private static final class Cloud extends EntityRenderer<FocusCloudEntity>{
        Cloud(EntityRendererProvider.Context context){super(context);}
        @Override public ResourceLocation getTextureLocation(FocusCloudEntity entity){return texture("textures/misc/particles.png");}
    }
    private static final class Mine extends EntityRenderer<FocusMineEntity>{
        private final LegacyGrapplerModel model=new LegacyGrapplerModel();
        Mine(EntityRendererProvider.Context context){super(context);shadowRadius=0;}
        @Override public ResourceLocation getTextureLocation(FocusMineEntity entity){return texture("textures/entity/mine.png");}
        @Override public void render(FocusMineEntity entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
            pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees(yaw-90));pose.mulPose(Axis.ZP.rotationDegrees(net.minecraft.util.Mth.lerp(partial,entity.xRotO,entity.getXRot())));
            float pulse=(entity.counter()+partial)%8/8F;
            // Original GL float light (61680,0) clamps to block15/sky0. Packing
            // 61680 into modern signed SHORT UV2 instead produces a negative texel.
            model.renderToBuffer(pose,buffers.getBuffer(RenderType.entityTranslucent(getTextureLocation(entity))),240,OverlayTexture.NO_OVERLAY,1,1-pulse,1-pulse,1);pose.popPose();
            super.render(entity,yaw,partial,pose,buffers,light);
        }
    }
    private static final class Bat extends MobRenderer<SpellBatEntity,SpellBatModel>{
        Bat(EntityRendererProvider.Context context){super(context,new SpellBatModel(),.25F);}
        @Override public ResourceLocation getTextureLocation(SpellBatEntity entity){return texture("textures/entity/spellbat.png");}
        @Override protected void scale(SpellBatEntity entity,PoseStack pose,float partial){pose.scale(.35F,.35F,.35F);}
        @Override protected void setupRotations(SpellBatEntity entity,PoseStack pose,float age,float yaw,float partial){pose.translate(0,-.1,0);super.setupRotations(entity,pose,age,yaw,partial);}
        @Override protected int getBlockLightLevel(SpellBatEntity entity,net.minecraft.core.BlockPos pos){return 15;}
        @Override public void render(SpellBatEntity entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){super.render(entity,yaw,partial,pose,buffers,15728880);}
    }
}
