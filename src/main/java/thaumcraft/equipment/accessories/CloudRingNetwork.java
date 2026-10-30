package thaumcraft.equipment.accessories;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="thaumcraft")
public final class CloudRingNetwork {
    private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath("thaumcraft","cloud_ring"),()->"1","1"::equals,"1"::equals);
    private static boolean registered;
    private CloudRingNetwork(){}
    public static void register(){
        if(registered)return;registered=true;
        CHANNEL.registerMessage(0,Jump.class,(m,b)->{},b->new Jump(),(m,c)->{
            var context=c.get();context.enqueueWork(()->{var p=context.getSender();if(p!=null)CloudRingSupport.tryJump(p);});context.setPacketHandled(true);
        },Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }
    public static void request(){CHANNEL.sendToServer(new Jump());}
    private record Jump(){}
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event){
        if(event.phase==TickEvent.Phase.END)CloudRingSupport.grounded(event.player);
    }
}
