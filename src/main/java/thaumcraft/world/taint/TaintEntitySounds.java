package thaumcraft.world.taint;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/** Original BETA26 creature sound; media remains owned by the resource integration. */
public final class TaintEntitySounds {
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, "thaumcraft");
    static {
        for(String id:java.util.List.of("tentacle","swarm","swarmattack","fly"))
            SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", id)));
    }
    private TaintEntitySounds() {}
    public static void register(IEventBus bus) { SOUNDS.register(bus); }
    public static SoundEvent sound(String id) { return ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.fromNamespaceAndPath("thaumcraft",id)); }
}
