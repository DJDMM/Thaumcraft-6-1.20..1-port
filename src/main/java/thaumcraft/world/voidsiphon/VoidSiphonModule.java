package thaumcraft.world.voidsiphon;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;
import thaumcraft.catalog.blocks.CatalogBlocks;
public final class VoidSiphonModule {
    private static final DeferredRegister<BlockEntityType<?>> TILES=DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,"thaumcraft");
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(ForgeRegistries.MENU_TYPES,"thaumcraft");
    public static final RegistryObject<BlockEntityType<VoidSiphonBlockEntity>> BLOCK_ENTITY=TILES.register("void_siphon",()->BlockEntityType.Builder.of(VoidSiphonBlockEntity::new,CatalogBlocks.block("void_siphon")).build(null));
    public static final RegistryObject<MenuType<VoidSiphonMenu>> MENU=MENUS.register("void_siphon",()->IForgeMenuType.create(VoidSiphonMenu::new));
    private VoidSiphonModule(){}
    public static boolean handlesBlock(String id){return id.equals("void_siphon");}
    public static Block createBlock(String id){if(!handlesBlock(id))throw new IllegalArgumentException(id);return new VoidSiphonBlock();}
    public static void register(IEventBus bus){TILES.register(bus);MENUS.register(bus);}
}
