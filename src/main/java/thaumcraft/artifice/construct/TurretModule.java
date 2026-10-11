package thaumcraft.artifice.construct;

import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import thaumcraft.catalog.CatalogModule;

/** Entity/item IDs stay in the existing catalogue registries. */
public final class TurretModule {
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, "thaumcraft");
    public static final RegistryObject<MenuType<TurretMenu>> MENU = MENUS.register("turret", () -> IForgeMenuType.create(TurretMenu::new));
    private TurretModule() {}
    public static void register(IEventBus bus) { MENUS.register(bus); }
    public static Item createItem(String id, CatalogModule.Spec spec) { return java.util.Set.of("turret_basic", "turret_advanced", "turret_bore").contains(id) ? new TurretPlacerItem(spec) : null; }
    public static Item createItem(CatalogModule.Spec spec) { return createItem(spec.id(), spec); }
    static ItemStack stack(String id) {
        Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("thaumcraft", id));
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }
}
