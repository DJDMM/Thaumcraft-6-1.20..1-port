package thaumcraft.alchemy.liquiddeath;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.*;
import net.minecraftforge.registries.*;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import java.util.function.Consumer;
import thaumcraft.api.aspects.*;
import thaumcraft.alchemy.AspectCrystalItem;
/** Released dissolve damage and four-quanta fluid; the bucket replaces the old universal bucket. */
public final class LiquidDeathModule {
 public static final DeferredRegister<FluidType> FLUID_TYPES=DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES,"thaumcraft");
 public static final DeferredRegister<net.minecraft.world.level.material.Fluid> FLUIDS=DeferredRegister.create(ForgeRegistries.FLUIDS,"thaumcraft");
 public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,"thaumcraft");
 public static final ResourceKey<DamageType> DISSOLVE=ResourceKey.create(Registries.DAMAGE_TYPE,ResourceLocation.fromNamespaceAndPath("thaumcraft","dissolve"));
 public static final RegistryObject<FluidType> TYPE=FLUID_TYPES.register("liquid_death",()->new FluidType(FluidType.Properties.create().descriptionId("block.thaumcraft.liquid_death").viscosity(1500).density(1000).rarity(Rarity.RARE).canConvertToSource(false).canHydrate(false).canSwim(false).canDrown(false).canExtinguish(false)) {
  @Override public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer){consumer.accept(new IClientFluidTypeExtensions(){
   @Override public int getTintColor(){return -263978855;}
   @Override public ResourceLocation getStillTexture(){return ResourceLocation.fromNamespaceAndPath("thaumcraft","blocks/animatedglow");}
   @Override public ResourceLocation getFlowingTexture(){return getStillTexture();}
  });}
 });
 public static final RegistryObject<net.minecraft.world.level.material.FlowingFluid> SOURCE=FLUIDS.register("liquid_death",()->new LiquidDeathFluid.Source(properties()));
 public static final RegistryObject<net.minecraft.world.level.material.FlowingFluid> FLOWING=FLUIDS.register("flowing_liquid_death",()->new LiquidDeathFluid.Flowing(properties()));
 public static final RegistryObject<Item> BUCKET=ITEMS.register("liquid_death_bucket",()->new BucketItem(SOURCE,new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET).rarity(Rarity.RARE)));
 private LiquidDeathModule(){}
 private static ForgeFlowingFluid.Properties properties(){return new ForgeFlowingFluid.Properties(TYPE,SOURCE,FLOWING).block(LiquidDeathModule::block).bucket(BUCKET).tickRate(7).levelDecreasePerBlock(2).slopeFindDistance(2);}
 public static LiquidDeathBlock block(){return (LiquidDeathBlock)thaumcraft.catalog.blocks.CatalogBlocks.block("liquid_death");}
 public static void register(IEventBus bus){FLUID_TYPES.register(bus);FLUIDS.register(bus);ITEMS.register(bus);MinecraftForge.EVENT_BUS.addListener(LiquidDeathModule::drops);}
 public static DamageSource damage(LivingEntity target){return new DamageSource(target.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DISSOLVE));}
 public static AspectList entityAspects(Entity entity){
  if(!(entity instanceof Player))return thaumcraft.scanning.AspectRegistry.getAspects(entity);
  AspectList result=new AspectList().add(Aspect.MAN,4);java.util.Random random=new java.util.Random(entity.getName().getString().hashCode());Aspect[] choices=Aspect.aspects.values().toArray(new Aspect[0]);if(choices.length>0)for(int i=0;i<3;i++)result.add(choices[random.nextInt(choices.length)],15);return result;
 }
 private static void drops(LivingDropsEvent event){
  var victim=event.getEntity();if(victim.level().isClientSide||!event.getSource().is(DISSOLVE))return;AspectList aspects=entityAspects(victim);Aspect[] choices=aspects.getAspects();if(choices.length==0)return;
  int count=1+victim.getRandom().nextInt(1+aspects.visSize()/10);
  for(int i=0;i<count;i++){var crystal=AspectCrystalItem.create(choices[victim.getRandom().nextInt(choices.length)],1);event.getDrops().add(new net.minecraft.world.entity.item.ItemEntity(victim.level(),victim.getX(),victim.getEyeY(),victim.getZ(),crystal));}
 }
}
