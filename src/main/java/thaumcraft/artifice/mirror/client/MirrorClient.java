package thaumcraft.artifice.mirror.client;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import thaumcraft.artifice.mirror.MirrorModule;

@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class MirrorClient {
    private MirrorClient(){}
    /** Called only on CLIENT by the native module; subscribers are discovered by Forge. */
    public static void register(){}
    @SubscribeEvent public static void models(ModelEvent.RegisterAdditional event){event.register(MirrorRenderer.ITEM_FRAME);event.register(MirrorRenderer.ESSENTIA_FRAME);}
    @SubscribeEvent public static void setup(FMLClientSetupEvent event){event.enqueueWork(()->MenuScreens.register(MirrorModule.HAND_MIRROR_MENU.get(),HandMirrorScreen::new));}
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event){
        event.registerBlockEntityRenderer(MirrorModule.ITEM_MIRROR.get(),MirrorRenderer::new);
        event.registerBlockEntityRenderer(MirrorModule.ESSENTIA_MIRROR.get(),MirrorRenderer::new);
    }
}
