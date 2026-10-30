package thaumcraft.equipment.accessories.client;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.sounds.*;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import thaumcraft.equipment.accessories.*;
import thaumcraft.fx.FxDispatcher;
public final class CloudRingClient {
    private CloudRingClient(){}
    public static void tick(LivingEntity wearer){
        var mc=Minecraft.getInstance();
        if(wearer!=mc.player||mc.player==null||mc.screen!=null)return;
        boolean pressed=mc.options.keyJump.consumeClick();
        if(!pressed||CloudRingSupport.jumpDelay(wearer)!=0||!CloudRingSupport.tryJump(mc.player))return;
        // Released white bamf; the physical velocity is predicted here and validated once on the server.
        FxDispatcher.emit(mc.level,"bamf",wearer.position().add(0,.5,0),new Vec3(0,1,0),0xFFFFFF,1,0);
        mc.level.playLocalSound(wearer.getX(),wearer.getY(),wearer.getZ(),SoundEvents.PLAYER_ATTACK_SWEEP,SoundSource.PLAYERS,.1F,
                1+(float)mc.level.random.nextGaussian()*.05F,false);
        CloudRingNetwork.request();
    }
}
