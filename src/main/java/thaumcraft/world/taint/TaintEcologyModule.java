package thaumcraft.world.taint;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.material.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.*;
import net.minecraftforge.registries.*;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import java.util.function.Consumer;
public final class TaintEcologyModule {
 public static final DeferredRegister<FluidType> FLUID_TYPES=DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES,"thaumcraft");
 public static final DeferredRegister<Fluid> FLUIDS=DeferredRegister.create(ForgeRegistries.FLUIDS,"thaumcraft");
 public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,"thaumcraft");
 public static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,"thaumcraft");
 public static final RegistryObject<FluidType> GOO_TYPE=FLUID_TYPES.register("flux_goo",()->new FluidType(FluidType.Properties.create().descriptionId("block.thaumcraft.flux_goo").density(8).viscosity(6000).canConvertToSource(false).canHydrate(false).canSwim(false).canDrown(false).canPushEntity(false).motionScale(0)) {
  @Override public void initializeClient(Consumer<IClientFluidTypeExtensions> c){c.accept(new IClientFluidTypeExtensions(){
   @Override public ResourceLocation getStillTexture(){return ResourceLocation.fromNamespaceAndPath("thaumcraft","blocks/flux_goo");}
   @Override public ResourceLocation getFlowingTexture(){return getStillTexture();}
  });}
 });
 public static final RegistryObject<FlowingFluid> GOO=FLUIDS.register("flux_goo",()->new FiniteGooFluid.Source(fluidProperties()));
 public static final RegistryObject<FlowingFluid> FLOWING_GOO=FLUIDS.register("flowing_flux_goo",()->new FiniteGooFluid.Flowing(fluidProperties()));
 public static final RegistryObject<Item> GOO_BUCKET=ITEMS.register("flux_goo_bucket",()->new BucketItem(GOO,new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)));
 public static final RegistryObject<EntityType<BottleTaintEntity>> BOTTLE_TAINT=ENTITIES.register("bottle_taint",()->EntityType.Builder.<BottleTaintEntity>of(BottleTaintEntity::new,MobCategory.MISC).sized(.25F,.25F).clientTrackingRange(4).updateInterval(20).build("thaumcraft:bottle_taint"));
 private TaintEcologyModule(){}
 private static ForgeFlowingFluid.Properties fluidProperties(){return new ForgeFlowingFluid.Properties(GOO_TYPE,GOO,FLOWING_GOO).block(()->(FluxGooBlock)TaintModule.block("flux_goo")).bucket(GOO_BUCKET).tickRate(30);}
 public static void register(IEventBus bus){FLUID_TYPES.register(bus);FLUIDS.register(bus);ITEMS.register(bus);ENTITIES.register(bus);thaumcraft.world.taint.champion.Champions.register(bus);}
}
