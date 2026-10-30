package thaumcraft.equipment.accessories;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.ForgeRegistries;
import thaumcraft.catalog.CatalogModule;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/** Curios owns equipped storage, its menu, persistence, synchronization and death handling. */
public final class AccessoriesModule {
    private AccessoriesModule() {}
    public static Item create(CatalogModule.Spec spec) {
        if(spec.id().equals("voidseer_charm"))return new VoidseerPearlItem();
        if(spec.id().equals("verdant_charm"))return new VerdantCharmItem();
        if(spec.id().equals("cloud_ring"))return new LateAccessoryItem(LateAccessoryItem.Kind.CLOUD);
        if(spec.id().equals("curiosity_band"))return new LateAccessoryItem(LateAccessoryItem.Kind.CURIOSITY);
        if(spec.id().equals("charm_undying"))return new LateAccessoryItem(LateAccessoryItem.Kind.UNDYING);
        if(spec.sourceClass().equals("ItemBaubles"))return new BaubleItem(spec.legacyMetadata());
        if(spec.sourceClass().equals("ItemAmuletVis"))return new VisAmuletItem(spec.legacyMetadata()==1);
        if(spec.id().equals("focus_pouch"))return new thaumcraft.equipment.pouch.FocusPouchItem(new Item.Properties().stacksTo(1));
        return null;
    }
    public static void register(IEventBus bus) {
        CloudRingNetwork.register();
        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event)->event.enqueueWork(()->{
            var goggles=ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("thaumcraft","goggles"));
            if(goggles!=null)CuriosApi.registerCurio(goggles,new ICurioItem(){});
        }));
    }
}
