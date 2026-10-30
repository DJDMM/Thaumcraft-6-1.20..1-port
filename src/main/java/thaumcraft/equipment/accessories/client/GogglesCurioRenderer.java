package thaumcraft.equipment.accessories.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/** Original goggles_bauble sprite and UtilsFX 16x26 strip extrusion in modern head coordinates. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class GogglesCurioRenderer implements ICurioRenderer {
    private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/items/goggles_bauble.png");
    private final ResourceLocation texture;
    private final boolean curiosity;
    public GogglesCurioRenderer(){this(TEXTURE,false);}
    public GogglesCurioRenderer(ResourceLocation texture,boolean curiosity){this.texture=texture;this.curiosity=curiosity;}
    private static long draws;
    public static long draws(){return draws;}
    @SubscribeEvent public static void setup(FMLClientSetupEvent event){event.enqueueWork(()->{
        var goggles=ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("thaumcraft","goggles"));
        if(goggles!=null)CuriosRendererRegistry.register(goggles,GogglesCurioRenderer::new);
        var band=ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("thaumcraft","curiosity_band"));
        if(band!=null)CuriosRendererRegistry.register(band,()->new GogglesCurioRenderer(ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/items/curiosity_band_worn.png"),true));
    });}
    @Override public <T extends LivingEntity,M extends EntityModel<T>> void render(ItemStack stack,SlotContext slot,PoseStack pose,RenderLayerParent<T,M> parent,MultiBufferSource buffers,int light,float limb,float amount,float partial,float age,float yaw,float pitch){
        if(!(parent.getModel() instanceof HumanoidModel<?> model))return;
        pose.pushPose();model.head.translateAndRotate(pose);
        // Released helpers compose to this neutral-head transform. Their armor!=null branch
        // is always true for modern nonnull EMPTY, as it was in the original 1.12 inventory.
        pose.translate(-.275,.005,curiosity&&slot.entity().getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).isEmpty()?-.27:-.336);pose.scale(.55F,-.55F,-.55F);
        var out=buffers.getBuffer(RenderType.entityCutoutNoCull(texture));
        face(out,pose,light,0,0,1,new float[][]{{0,0,0,0,1},{1,0,0,1,1},{1,1,0,1,0},{0,1,0,0,0}});
        face(out,pose,light,0,0,-1,new float[][]{{0,1,-.1F,0,0},{1,1,-.1F,1,0},{1,0,-.1F,1,1},{0,0,-.1F,0,1}});
        for(int i=0;i<16;i++){
            float x=i/16F,u=x+1/32F,next=x+1/16F;
            face(out,pose,light,-1,0,0,new float[][]{{x,0,-.1F,u,1},{x,0,0,u,1},{x,1,0,u,0},{x,1,-.1F,u,0}});
            face(out,pose,light,1,0,0,new float[][]{{next,1,-.1F,u,0},{next,1,0,u,0},{next,0,0,u,1},{next,0,-.1F,u,1}});
        }
        for(int i=0;i<26;i++){
            float y=i/26F,v=1-y-1/52F,next=y+1/26F;
            face(out,pose,light,0,1,0,new float[][]{{0,next,0,0,v},{1,next,0,1,v},{1,next,-.1F,1,v},{0,next,-.1F,0,v}});
            face(out,pose,light,0,-1,0,new float[][]{{1,y,0,1,v},{0,y,0,0,v},{0,y,-.1F,0,v},{1,y,-.1F,1,v}});
        }
        pose.popPose();draws++;
    }
    private static void face(VertexConsumer out,PoseStack pose,int light,float nx,float ny,float nz,float[][] vertices){
        for(float[] v:vertices)out.vertex(pose.last().pose(),v[0],v[1],v[2]).color(255,255,255,255).uv(v[3],v[4]).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.last().normal(),nx,ny,nz).endVertex();
    }
}
