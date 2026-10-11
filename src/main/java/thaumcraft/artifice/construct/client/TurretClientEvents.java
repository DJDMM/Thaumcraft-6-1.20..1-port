package thaumcraft.artifice.construct.client;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import thaumcraft.artifice.construct.TurretModule;

@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class TurretClientEvents {
    private TurretClientEvents() {}
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(()->MenuScreens.register(TurretModule.MENU.get(),TurretScreen::new));
    }
}
