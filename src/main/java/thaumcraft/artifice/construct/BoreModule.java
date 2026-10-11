package thaumcraft.artifice.construct;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class BoreModule {
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, "thaumcraft");
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, "thaumcraft");
    public static final RegistryObject<MenuType<BoreMenu>> MENU = MENUS.register("arcane_bore", () -> IForgeMenuType.create(BoreMenu::new));
    public static final RegistryObject<SoundEvent> RUMBLE = SOUNDS.register("rumble", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", "rumble")));
    private BoreModule() {}
    public static void register(IEventBus bus) { MENUS.register(bus); SOUNDS.register(bus); }
}
