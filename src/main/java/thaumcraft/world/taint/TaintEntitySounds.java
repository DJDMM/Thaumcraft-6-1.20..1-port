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
        SOUNDS.register("tentacle", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", "tentacle")));
    }
    private TaintEntitySounds() {}
    public static void register(IEventBus bus) { SOUNDS.register(bus); }
}
