package thaumcraft.world.crimson;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;

/** Original media/worldgen registry; entity IDs remain owned by VisualEntitiesModule. */
public final class CrimsonModule {
    private static final net.minecraftforge.common.ForgeConfigSpec.Builder CONFIG_BUILDER = new net.minecraftforge.common.ForgeConfigSpec.Builder();
    public static final net.minecraftforge.common.ForgeConfigSpec.BooleanValue GENERATE_STRUCTURES = CONFIG_BUILDER
            .comment("Generate the original overworld mounds and Lesser Crimson Portals in new chunks.").define("generateStructure",true);
    public static final net.minecraftforge.common.ForgeConfigSpec CONFIG = CONFIG_BUILDER.build();
    private static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, "thaumcraft");
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, "thaumcraft");
    public static final RegistryObject<Feature<?>> SURFACE = FEATURES.register("crimson_surface", CrimsonSurfaceFeature::new);
    static {
        for (String name : java.util.List.of("chant", "monolith", "urnbreak"))
            SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(id(name)));
    }
    private CrimsonModule() {}
    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("thaumcraft", path); }
    public static void register(IEventBus bus) {
        FEATURES.register(bus); SOUNDS.register(bus);
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON,CONFIG,"thaumcraft-crimson.toml");
        MinecraftForge.EVENT_BUS.addListener(CrimsonDrops::onDrops);
    }
}
