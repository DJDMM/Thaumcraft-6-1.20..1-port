package thaumcraft.world.mobs.client;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import thaumcraft.world.mobs.*;

/** Native Zombie animation/armor and original texture with BETA26 Giant scale and per-vertex tint. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class MobsClient{
    private MobsClient(){}
    @SubscribeEvent public static void setup(FMLClientSetupEvent e){e.enqueueWork(()->MenuScreens.register(MobsModule.PECH_MENU.get(),PechScreen::new));}
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e){
        e.registerEntityRenderer(MobsModule.BRAINY_ZOMBIE.get(),c->new BrainyRenderer(c));
        e.registerEntityRenderer(MobsModule.GIANT_BRAINY_ZOMBIE.get(),c->new BrainyRenderer(c));
    }
    private static final class BrainyRenderer extends ZombieRenderer{
        BrainyRenderer(net.minecraft.client.renderer.entity.EntityRendererProvider.Context context){super(context);}
        @Override public ResourceLocation getTextureLocation(Zombie zombie){return MobsModule.id("textures/entity/bzombie.png");}
        @Override protected void scale(Zombie zombie,PoseStack pose,float partial){
            if(zombie instanceof GiantBrainyZombieEntity giant){float scale=1+giant.anger();pose.scale(scale,scale,scale);}
        }
        @Override public void render(Zombie zombie,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
            float factor=zombie instanceof GiantBrainyZombieEntity giant?1-Math.min(1,giant.anger())/2:1;
            MultiBufferSource tinted=factor==1?buffers:type->new Tint(buffers.getBuffer(type),factor);
            super.render(zombie,yaw,partial,pose,tinted,light);
        }
    }
    private static final class Tint implements VertexConsumer{
        private final VertexConsumer next;private final float color;
        Tint(VertexConsumer next,float color){this.next=next;this.color=color;}
        @Override public VertexConsumer vertex(double x,double y,double z){next.vertex(x,y,z);return this;}
        @Override public VertexConsumer color(int r,int g,int b,int a){next.color(r,(int)(g*color),(int)(b*color),a);return this;}
        @Override public VertexConsumer uv(float u,float v){next.uv(u,v);return this;}
        @Override public VertexConsumer overlayCoords(int u,int v){next.overlayCoords(u,v);return this;}
        @Override public VertexConsumer uv2(int u,int v){next.uv2(u,v);return this;}
        @Override public VertexConsumer normal(float x,float y,float z){next.normal(x,y,z);return this;}
        @Override public void endVertex(){next.endVertex();}
        @Override public void defaultColor(int r,int g,int b,int a){next.defaultColor(r,(int)(g*color),(int)(b*color),a);}
        @Override public void unsetDefaultColor(){next.unsetDefaultColor();}
    }
}
