package thaumcraft.world.rift;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

/** Optional peaceful ecology controls; existing rifts and boosted seeds retain their callbacks. */
public final class RiftModule {
    private static final ForgeConfigSpec.Builder BUILDER=new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec.BooleanValue WUSS_MODE=BUILDER.comment("Disable natural Flux Rift generation and ordinary taint spread.").define("wussMode",false);
    public static final ForgeConfigSpec.IntValue TAINT_SPREAD_AREA=BUILDER.comment("The distance taint can spread from a taint seed.").defineInRange("taintSpreadArea",32,8,256);
    public static final ForgeConfigSpec.DoubleValue TAINT_SPREAD_RATE=BUILDER.comment("Rate of ordinary taint spread, modified by local Flux saturation.").defineInRange("taintSpreadRate",100.0,0.0,500.0);
    public static final ForgeConfigSpec CONFIG=BUILDER.build();
    private RiftModule() {}
    public static void register() { ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON,CONFIG,"thaumcraft-common.toml"); }
}
