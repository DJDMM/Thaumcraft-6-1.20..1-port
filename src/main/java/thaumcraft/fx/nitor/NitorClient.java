package thaumcraft.fx.nitor;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class NitorClient {
    private NitorClient(){}
    @SubscribeEvent public static void colors(RegisterColorHandlersEvent.Item event){for(var block:ForgeRegistries.BLOCKS.getValues())if(block instanceof NitorBlock nitor)event.register((stack,layer)->layer==0?nitor.color():-1,block);}
}
