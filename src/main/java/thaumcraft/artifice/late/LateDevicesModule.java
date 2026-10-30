package thaumcraft.artifice.late;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.*;
import thaumcraft.catalog.blocks.CatalogBlocks;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraft.world.item.crafting.RecipeSerializer;
public final class LateDevicesModule {
    private static final DeferredRegister<BlockEntityType<?>> TILES=DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,"thaumcraft");
    public static final RegistryObject<BlockEntityType<BarrierStoneBlockEntity>> BARRIER_STONE=TILES.register("barrier_stone",()->BlockEntityType.Builder.of(BarrierStoneBlockEntity::new,CatalogBlocks.block("paving_stone_barrier")).build(null));
    public static final RegistryObject<BlockEntityType<SpaBlockEntity>> SPA=TILES.register("spa",()->BlockEntityType.Builder.of(SpaBlockEntity::new,CatalogBlocks.block("spa")).build(null));
    public static final RegistryObject<BlockEntityType<EverfullUrnBlockEntity>> URN=TILES.register("everfull_urn",()->BlockEntityType.Builder.of(EverfullUrnBlockEntity::new,CatalogBlocks.block("everfull_urn")).build(null));
    public static final RegistryObject<BlockEntityType<PotionSprayerBlockEntity>> SPRAYER=TILES.register("potion_sprayer",()->BlockEntityType.Builder.of(PotionSprayerBlockEntity::new,CatalogBlocks.block("potion_sprayer")).build(null));
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(ForgeRegistries.MENU_TYPES,"thaumcraft");
    public static final RegistryObject<MenuType<LateDeviceMenu>> SPA_MENU=MENUS.register("spa",()->IForgeMenuType.create(LateDeviceMenu::new)),SPRAYER_MENU=MENUS.register("potion_sprayer",()->IForgeMenuType.create(LateDeviceMenu::new));
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS=DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS,"thaumcraft");
    public static final RegistryObject<RecipeSerializer<OptionalClusterSmeltingRecipe>> CLUSTER_SMELTING=SERIALIZERS.register("optional_cluster_smelting",OptionalClusterSmeltingRecipe.Serializer::new);
    private LateDevicesModule(){}
    public static boolean handlesBlock(String id){return java.util.Set.of("arcane_workbench_charger","paving_stone_travel","paving_stone_barrier","barrier","spa","everfull_urn","potion_sprayer").contains(id);}
    public static Block createBlock(String id){return switch(id){case "arcane_workbench_charger"->new WorkbenchChargerBlock();case "paving_stone_travel"->new PavingStoneBlock(false);case "paving_stone_barrier"->new PavingStoneBlock(true);case "barrier"->new BarrierBlock();case "spa"->new SpaBlock();case "everfull_urn"->new EverfullUrnBlock();case "potion_sprayer"->new PotionSprayerBlock();default->throw new IllegalArgumentException(id);};}
    public static void register(IEventBus bus){TILES.register(bus);MENUS.register(bus);SERIALIZERS.register(bus);}
}
