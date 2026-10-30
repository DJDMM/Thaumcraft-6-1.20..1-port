package thaumcraft.equipment.accessories;
import java.lang.reflect.Field;
import java.util.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

/** Physical worn ownership and one extra jump per flight; no client-provided motion or identity. */
public final class CloudRingSupport {
    private static final Field JUMP_DELAY=ObfuscationReflectionHelper.findField(LivingEntity.class,"f_20954_");
    // Entity equality uses the numeric entity ID. A local player and its integrated
    // server counterpart share that ID, so their flight state needs separate maps.
    private static final Map<Player,State> CLIENT_STATES=Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Player,State> SERVER_STATES=Collections.synchronizedMap(new WeakHashMap<>());
    private static final class State{Object level;boolean used;}
    private CloudRingSupport(){}
    private static Map<Player,State> states(Player player){return player.level().isClientSide?CLIENT_STATES:SERVER_STATES;}
    public static int jumpDelay(LivingEntity wearer){
        try{return JUMP_DELAY.getInt(wearer);}catch(IllegalAccessException failure){throw new IllegalStateException(failure);}
    }
    public static boolean tryJump(Player player){
        if(!player.isAlive()||player.isSpectator()||player.onGround()||player.isInWater()
                ||LateAccessoryEvents.equipped(player,LateAccessoryItem.Kind.CLOUD).isEmpty())return false;
        var states=states(player);
        synchronized(states){
            State state=states.computeIfAbsent(player,p->new State());
            if(state.level!=player.level()){state.level=player.level();state.used=false;}
            if(state.used)return false;state.used=true;
        }
        double vertical=.75;
        var boost=player.getEffect(MobEffects.JUMP);
        if(boost!=null)vertical+=(boost.getAmplifier()+1)*.1F;
        Vec3 motion=player.getDeltaMovement();player.setDeltaMovement(motion.x,vertical,motion.z);
        if(player.isSprinting()){
            float yaw=player.getYRot()*.017453292F;
            player.setDeltaMovement(player.getDeltaMovement().add(-net.minecraft.util.Mth.sin(yaw)*.2F,0,net.minecraft.util.Mth.cos(yaw)*.2F));
        }
        player.fallDistance=0;ForgeHooks.onLivingJump(player);return true;
    }
    public static void grounded(Player player){
        if(player.onGround()&&jumpDelay(player)==0){var states=states(player);synchronized(states){states.remove(player);}}
    }
}
