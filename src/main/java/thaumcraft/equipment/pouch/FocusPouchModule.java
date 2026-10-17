package thaumcraft.equipment.pouch;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class FocusPouchModule {
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(ForgeRegistries.MENU_TYPES,"thaumcraft");
    public static final RegistryObject<MenuType<FocusPouchMenu>> MENU=MENUS.register("focus_pouch",()->IForgeMenuType.create(FocusPouchMenu::new));
    private FocusPouchModule(){}
    public static void register(IEventBus bus){MENUS.register(bus);}
}
