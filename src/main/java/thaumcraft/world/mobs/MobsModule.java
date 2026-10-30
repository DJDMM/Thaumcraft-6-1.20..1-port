package thaumcraft.world.mobs;
import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraft.world.inventory.MenuType;

/** Typed Zombie subclasses retain original IDs; Firebat/Pech factories remain in the visual-compatible registry. */
public final class MobsModule {
    private static final net.minecraftforge.common.ForgeConfigSpec.Builder CONFIG_BUILDER=new net.minecraftforge.common.ForgeConfigSpec.Builder();
    public static final net.minecraftforge.common.ForgeConfigSpec.BooleanValue SPAWN_BRAINY=CONFIG_BUILDER.define("allowSpawnAngryZombie",true);
    public static final net.minecraftforge.common.ForgeConfigSpec.BooleanValue SPAWN_PECH=CONFIG_BUILDER.define("allowSpawnPech",true);
    public static final net.minecraftforge.common.ForgeConfigSpec.BooleanValue SPAWN_FIREBAT=CONFIG_BUILDER.define("allowSpawnFireBat",true);
    public static final net.minecraftforge.common.ForgeConfigSpec CONFIG=CONFIG_BUILDER.build();
    public static final DeferredRegister<EntityType<?>> TYPES=DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,"thaumcraft");
    public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,"thaumcraft");
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(ForgeRegistries.MENU_TYPES,"thaumcraft");
    private static final DeferredRegister<net.minecraft.sounds.SoundEvent> SOUNDS=DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,"thaumcraft");
    public static final RegistryObject<MenuType<PechMenu>> PECH_MENU=MENUS.register("pech",()->net.minecraftforge.common.extensions.IForgeMenuType.create(PechMenu::new));
    private static final DeferredRegister<Codec<? extends BiomeModifier>> BIOMES=DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS,"thaumcraft");
    public static final RegistryObject<EntityType<BrainyZombieEntity>> BRAINY_ZOMBIE=TYPES.register("brainy_zombie",()->EntityType.Builder
            .<BrainyZombieEntity>of(BrainyZombieEntity::new,MobCategory.MONSTER).sized(.6F,1.95F).clientTrackingRange(4).updateInterval(3).build("thaumcraft:brainy_zombie"));
    public static final RegistryObject<EntityType<GiantBrainyZombieEntity>> GIANT_BRAINY_ZOMBIE=TYPES.register("giant_brainy_zombie",()->EntityType.Builder
            .<GiantBrainyZombieEntity>of(GiantBrainyZombieEntity::new,MobCategory.MONSTER).sized(.6F,1.95F).clientTrackingRange(4).updateInterval(3).build("thaumcraft:giant_brainy_zombie"));
    public static final RegistryObject<Codec<NativeMobSpawns>> SPAWN_CODEC=BIOMES.register("native_mobs",()->Codec.unit(NativeMobSpawns::new));
    static{
        ITEMS.register("brainy_zombie_spawn_egg",()->new ForgeSpawnEggItem(BRAINY_ZOMBIE,16761087,32768,new Item.Properties()));
        ITEMS.register("giant_brainy_zombie_spawn_egg",()->new ForgeSpawnEggItem(GIANT_BRAINY_ZOMBIE,16761087,16384,new Item.Properties()));
        for(String name:java.util.List.of("pech_idle","pech_trade","pech_dice","pech_hit","pech_death","pech_charge"))SOUNDS.register(name,()->net.minecraft.sounds.SoundEvent.createVariableRangeEvent(id(name)));
    }
    private MobsModule(){}
    public static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath("thaumcraft",path);}
    public static Item createItem(thaumcraft.catalog.CatalogModule.Spec spec){return spec.id().equals("pech_wand")?new PechWandItem():null;}
    public static void register(IEventBus bus){
        TYPES.register(bus);ITEMS.register(bus);BIOMES.register(bus);MENUS.register(bus);SOUNDS.register(bus);
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON,CONFIG,"thaumcraft-mobs.toml");
        bus.addListener((EntityAttributeCreationEvent e)->{e.put(BRAINY_ZOMBIE.get(),BrainyZombieEntity.attributes().build());e.put(GIANT_BRAINY_ZOMBIE.get(),GiantBrainyZombieEntity.attributes().build());});
        bus.addListener((SpawnPlacementRegisterEvent e)->{
            e.register(BRAINY_ZOMBIE.get(),SpawnPlacements.Type.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,NativeMobSpawns::brainyRules,SpawnPlacementRegisterEvent.Operation.REPLACE);
            e.register(GIANT_BRAINY_ZOMBIE.get(),SpawnPlacements.Type.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,NativeMobSpawns::brainyRules,SpawnPlacementRegisterEvent.Operation.REPLACE);
            e.register(thaumcraft.catalog.entities.VisualEntitiesModule.LIVING.get("pech").get(),SpawnPlacements.Type.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,NativeMobSpawns::pechRules,SpawnPlacementRegisterEvent.Operation.REPLACE);
            e.register(thaumcraft.catalog.entities.VisualEntitiesModule.LIVING.get("fire_bat").get(),SpawnPlacements.Type.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,NativeMobSpawns::firebatRules,SpawnPlacementRegisterEvent.Operation.REPLACE);
        });
    }
}
