package thaumcraft.artifice.late.client;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import thaumcraft.artifice.late.LateDevicesModule;
@Mod.EventBusSubscriber(modid="thaumcraft",bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class LateDeviceClient {
    private LateDeviceClient(){}
    @SubscribeEvent public static void setup(FMLClientSetupEvent e){e.enqueueWork(()->{MenuScreens.register(LateDevicesModule.SPA_MENU.get(),LateDeviceScreen::new);MenuScreens.register(LateDevicesModule.SPRAYER_MENU.get(),LateDeviceScreen::new);});}
}
