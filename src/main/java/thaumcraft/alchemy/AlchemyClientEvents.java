package thaumcraft.alchemy;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thaumcraft.alchemy.client.CrucibleRenderer;

@Mod.EventBusSubscriber(modid = "thaumcraft", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class AlchemyClientEvents {
    @SubscribeEvent public static void renderers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(AlchemyModule.CRUCIBLE_TILE.get(), CrucibleRenderer::new);
        event.registerEntityRenderer(AlchemyModule.ALUMENTUM_ENTITY.get(),context->new net.minecraft.client.renderer.entity.EntityRenderer<AlumentumProjectile>(context){
            @Override public net.minecraft.resources.ResourceLocation getTextureLocation(AlumentumProjectile entity){return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;}
        });
    }
}
