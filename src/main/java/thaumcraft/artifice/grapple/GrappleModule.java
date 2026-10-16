package thaumcraft.artifice.grapple;

import java.lang.reflect.Field;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.registries.*;

/** One live hook per owner, qualified by its actual world instead of legacy global numeric IDs. */
@Mod.EventBusSubscriber(modid="thaumcraft")
public final class GrappleModule {
    private static final Map<Level,Map<UUID,GrappleEntity>> ACTIVE=Collections.synchronizedMap(new WeakHashMap<>());
    private static final DeferredRegister<SoundEvent> SOUNDS=DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,"thaumcraft");
    public static final RegistryObject<SoundEvent> ICE=SOUNDS.register("ice",()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft","ice")));
    private static final Field FLOATING=ObfuscationReflectionHelper.findField(ServerGamePacketListenerImpl.class,"f_9737_");
    private GrappleModule() {}
    public static void register(IEventBus bus){SOUNDS.register(bus);}
    private static Map<UUID,GrappleEntity> hooks(Level level){return ACTIVE.computeIfAbsent(level,ignored->new HashMap<>());}
    public static GrappleEntity active(Level level,LivingEntity owner){
        var hook=hooks(level).get(owner.getUUID());
        if(hook!=null&&(hook.isRemoved()||hook.level()!=level)){hooks(level).remove(owner.getUUID(),hook);return null;}
        return hook;
    }
    static void bind(GrappleEntity hook,LivingEntity owner){var old=hooks(hook.level()).put(owner.getUUID(),hook);if(old!=null&&old!=hook)old.discard();}
    static void release(GrappleEntity hook){if(hook.getOwner() instanceof LivingEntity owner)hooks(hook.level()).remove(owner.getUUID(),hook);}
    static void permitPull(ServerPlayer player){if(player.connection!=null)try{FLOATING.setInt(player.connection,0);}catch(IllegalAccessException failure){throw new IllegalStateException("Cannot reset native grapple floating counter",failure);}}
    @SubscribeEvent public static void unload(LevelEvent.Unload event){if(event.getLevel() instanceof Level level)ACTIVE.remove(level);}
    @SubscribeEvent public static void logout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event){
        synchronized(ACTIVE){for(var hooks:ACTIVE.values()){var hook=hooks.remove(event.getEntity().getUUID());if(hook!=null)hook.discard();}}
    }
}
