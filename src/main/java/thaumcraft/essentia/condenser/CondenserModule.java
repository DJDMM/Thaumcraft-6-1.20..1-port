package thaumcraft.essentia.condenser;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.*;
import thaumcraft.catalog.blocks.CatalogBlocks;

/** The three original block IDs retain their original static models. */
@Mod.EventBusSubscriber(modid="thaumcraft")
public final class CondenserModule {
    private static final DeferredRegister<BlockEntityType<?>> TILES=DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,"thaumcraft");
    public static final RegistryObject<BlockEntityType<CondenserBlockEntity>> CONDENSER=TILES.register("condenser",()->BlockEntityType.Builder.of(CondenserBlockEntity::new,CatalogBlocks.block("condenser")).build(null));
    private CondenserModule(){}
    public static boolean handlesBlock(String id){return id.equals("condenser")||id.equals("condenser_lattice")||id.equals("condenser_lattice_dirty");}
    public static Block createBlock(String id){return id.equals("condenser")?new CondenserBlock():new CondenserLatticeBlock(id.equals("condenser_lattice_dirty"));}
    public static net.minecraft.world.item.BlockItem createBlockItem(Block block){return new CondenserBlockItem(block);}
    public static void register(IEventBus bus){TILES.register(bus);}
    @SubscribeEvent public static void tick(TickEvent.LevelTickEvent event){if(event.phase==TickEvent.Phase.END&&event.level instanceof net.minecraft.server.level.ServerLevel level)CondenserDropData.get(level).tick(level);}
}
