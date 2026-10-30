package thaumcraft.world.warp.client;
import java.util.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thaumcraft.fx.FxDispatcher;
import thaumcraft.catalog.entities.client.ObjMesh;

@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public final class WarpSonic {
    private static final List<Pulse> PULSES=new ArrayList<>();
    private static Object lastLevel;
    private static final class Pulse{
        final Entity target;final float yaw,pitch;final int life;int age;
        Pulse(Entity target){this.target=target;yaw=target.getYHeadRot();pitch=target.getXRot();life=10+target.level().random.nextInt(5);}
    }
    public static void receive(FxDispatcher.Message message){
        var mc=Minecraft.getInstance();if(mc.level==null)return;var target=mc.level.getEntity(message.target());if(target!=null)PULSES.add(new Pulse(target));
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();
        if(mc.level!=lastLevel){PULSES.clear();lastLevel=mc.level;}
        if(!mc.isPaused())PULSES.removeIf(p->p.age++>=p.life);
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;
        var buffers=Minecraft.getInstance().renderBuffers().bufferSource();
        for(var pulse:PULSES){
            float partial=event.getPartialTick(),fade=(pulse.age+partial)/pulse.life;var target=pulse.target;
            int frame=Math.min(15,(int)(14*fade)+1);
            var pose=event.getPoseStack();var eye=event.getCamera().getPosition();
            pose.pushPose();pose.translate(Mth.lerp(partial,target.xo,target.getX())-eye.x,
                Mth.lerp(partial,target.yo,target.getY())+target.getEyeHeight()-eye.y,Mth.lerp(partial,target.zo,target.getZ())-eye.z);
            pose.mulPose(Axis.YP.rotationDegrees(-pulse.yaw));pose.mulPose(Axis.XP.rotationDegrees(pulse.pitch));
            pose.translate(0,0,2*target.getBbHeight()+target.getBbWidth()/2);
            pose.scale(.25F*target.getBbHeight(),.25F*target.getBbHeight(),-target.getBbHeight());
            ObjMesh.get("hemis").renderStyled(pose,buffers,ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/models/ripple"+frame+".png"),
                null,220,0xFF808080,t->thaumcraft.fx.client.FxRenderTypes.entity(t,0));pose.popPose();
        }
        buffers.endBatch();
    }
}
