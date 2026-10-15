package thaumcraft.artifice.pattern;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.*;
import thaumcraft.catalog.blocks.CatalogBlocks;

/** The original pattern crafter ID has a native tile, not a programmable inventory. */
@Mod.EventBusSubscriber(modid="thaumcraft")
public final class PatternCrafterModule {
    private static final DeferredRegister<BlockEntityType<?>> TILES=DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,"thaumcraft");
    public static final RegistryObject<BlockEntityType<PatternCrafterBlockEntity>> BLOCK_ENTITY=TILES.register("pattern_crafter",()->BlockEntityType.Builder.of(PatternCrafterBlockEntity::new,CatalogBlocks.block("pattern_crafter")).build(null));
    private PatternCrafterModule(){}
    public static boolean handlesBlock(String id){return id.equals("pattern_crafter");}
    public static Block createBlock(String id){if(!handlesBlock(id))throw new IllegalArgumentException(id);return new PatternCrafterBlock();}
    public static BlockItem createBlockItem(Block block){return new PatternCrafterBlockItem(block);}
    public static void register(IEventBus bus){TILES.register(bus);}
    @SubscribeEvent public static void tick(TickEvent.LevelTickEvent event){if(event.phase==TickEvent.Phase.END&&event.level instanceof net.minecraft.server.level.ServerLevel level)PatternOutputData.get(level).tick(level);}
}
