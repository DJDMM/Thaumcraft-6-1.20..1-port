package thaumcraft.artifice.lamp;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import thaumcraft.catalog.blocks.CatalogBlocks;

/** Native lamp and its original invisible air-like light, under the existing IDs. */
@Mod.EventBusSubscriber(modid="thaumcraft")
public final class LampModule {
    private static final DeferredRegister<BlockEntityType<?>> TILES=DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,"thaumcraft");
    public static final RegistryObject<BlockEntityType<ArcaneLampBlockEntity>> ARCANE_LAMP=TILES.register("lamp_arcane",()->BlockEntityType.Builder.of(ArcaneLampBlockEntity::new,CatalogBlocks.block("lamp_arcane")).build(null));
    public static final RegistryObject<BlockEntityType<GrowthLampBlockEntity>> LAMP_GROWTH=TILES.register("lamp_growth",()->BlockEntityType.Builder.of(GrowthLampBlockEntity::new,CatalogBlocks.block("lamp_growth")).build(null));
    public static final RegistryObject<BlockEntityType<FertilityLampBlockEntity>> LAMP_FERTILITY=TILES.register("lamp_fertility",()->BlockEntityType.Builder.of(FertilityLampBlockEntity::new,CatalogBlocks.block("lamp_fertility")).build(null));
    private LampModule() {}
    public static boolean handlesBlock(String id) {return id.equals("lamp_arcane")||id.equals("effect_glimmer")||id.equals("lamp_growth")||id.equals("lamp_fertility");}
    public static Block createBlock(String id) {return switch(id){case "lamp_arcane"->new ArcaneLampBlock();case "effect_glimmer"->new GlimmerBlock();case "lamp_growth"->new EssentiaLampBlock(false);case "lamp_fertility"->new EssentiaLampBlock(true);default->throw new IllegalArgumentException(id);};}
    public static net.minecraft.world.item.BlockItem createBlockItem(Block block){return new LampBlockItem(block);}
    public static void register(IEventBus bus) {TILES.register(bus);}
    @SubscribeEvent public static void tick(TickEvent.LevelTickEvent event){if(event.phase==TickEvent.Phase.END&&event.level instanceof net.minecraft.server.level.ServerLevel level){LampPlantTickData.get(level).tick(level);LampDropData.get(level).tick(level);}}
}
