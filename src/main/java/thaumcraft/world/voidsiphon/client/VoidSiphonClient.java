package thaumcraft.world.voidsiphon.client;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.joml.Quaternionf;
import thaumcraft.world.voidsiphon.*;
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class VoidSiphonClient implements BlockEntityRenderer<VoidSiphonBlockEntity> {
    public VoidSiphonClient(BlockEntityRendererProvider.Context context){}
    @SubscribeEvent public static void setup(FMLClientSetupEvent e){e.enqueueWork(()->MenuScreens.register(VoidSiphonModule.MENU.get(),VoidSiphonScreen::new));}
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(VoidSiphonModule.BLOCK_ENTITY.get(),VoidSiphonClient::new);}
    @Override public void render(VoidSiphonBlockEntity tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(!tile.getBlockState().getValue(VoidSiphonBlock.ENABLED))return;
        cube(pose,buffers,.875F,.126F,.2F,.2F,true);
        cube(pose,buffers,.3125F,.26F,.25F,.5F,false);
    }
    private static void cube(PoseStack pose,MultiBufferSource buffers,float y,float offset,float sx,float sy,boolean top){
        var type=top?VoidSiphonRenderTypes.GLOW:VoidSiphonRenderTypes.ALPHA;
        pose.pushPose();pose.translate(.5,y,.5);
        for(Direction face:Direction.values()){
            pose.pushPose();pose.mulPose(new Quaternionf().rotationAxis((float)Math.PI/2,-face.getStepY(),face.getStepX(),-face.getStepZ()));
            pose.translate(0,0,face.getStepZ()<0?offset:-offset);
            if(face.getStepZ()<0)pose.mulPose(Axis.YP.rotationDegrees(180));
            if(top||face.getStepZ()!=0)pose.mulPose(Axis.ZP.rotationDegrees(-90));
            pose.scale(sx,sy,sx);var v=buffers.getBuffer(type);var m=pose.last().pose();
            v.vertex(m,-.5F,-.5F,0).endVertex();v.vertex(m,.5F,-.5F,0).endVertex();v.vertex(m,.5F,.5F,0).endVertex();v.vertex(m,-.5F,.5F,0).endVertex();
            pose.popPose();
        }
        pose.popPose();if(buffers instanceof MultiBufferSource.BufferSource source)source.endBatch(type);
    }
}
