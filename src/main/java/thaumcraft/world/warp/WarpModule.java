package thaumcraft.world.warp;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;

public final class WarpModule {
    public static final DeferredRegister<net.minecraft.world.entity.EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "thaumcraft");
    public static final RegistryObject<net.minecraft.world.entity.EntityType<EldritchOrbEntity>> ELDRITCH_ORB = ENTITIES.register("eldritch_orb", () ->
        net.minecraft.world.entity.EntityType.Builder.<EldritchOrbEntity>of(EldritchOrbEntity::new, net.minecraft.world.entity.MobCategory.MISC)
            .sized(.25F,.25F).clientTrackingRange(4).updateInterval(20).setShouldReceiveVelocityUpdates(true).build("thaumcraft:eldritch_orb"));
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, "thaumcraft");
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, "thaumcraft");
    public static final RegistryObject<MobEffect> THAUMARHIA = EFFECTS.register("thaumarhia", () -> new WarpAffliction(WarpAffliction.Kind.THAUMARHIA));
    public static final RegistryObject<MobEffect> UNNATURAL_HUNGER = EFFECTS.register("unnatural_hunger", () -> new WarpAffliction(WarpAffliction.Kind.HUNGER));
    public static final RegistryObject<MobEffect> SUN_SCORNED = EFFECTS.register("sun_scorned", () -> new WarpAffliction(WarpAffliction.Kind.SUN));
    public static final RegistryObject<MobEffect> DEATH_GAZE = EFFECTS.register("death_gaze", () -> new WarpAffliction(WarpAffliction.Kind.GAZE));
    public static final RegistryObject<MobEffect> BLURRED_VISION = EFFECTS.register("blurred_vision", () -> new WarpAffliction(WarpAffliction.Kind.BLUR));
    static {
        for (String name : java.util.List.of("heartbeat", "egidle", "egdeath", "egattack", "egscreech"))
            SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", name)));
    }
    private WarpModule() {}
    public static void register(IEventBus bus) {
        EFFECTS.register(bus); SOUNDS.register(bus); ENTITIES.register(bus);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(WarpEvents::finishedEating);
        WarpNetwork.register();
    }
}
