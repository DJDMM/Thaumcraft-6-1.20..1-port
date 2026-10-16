package thaumcraft.artifice.generator;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;
import thaumcraft.catalog.blocks.CatalogBlocks;

public final class VisGeneratorModule {
    private static final DeferredRegister<BlockEntityType<?>> TILES=DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,"thaumcraft");
    public static final RegistryObject<BlockEntityType<VisGeneratorBlockEntity>> BLOCK_ENTITY=TILES.register("vis_generator",()->BlockEntityType.Builder.of(VisGeneratorBlockEntity::new,CatalogBlocks.block("vis_generator")).build(null));
    private VisGeneratorModule(){}
    public static boolean handlesBlock(String id){return id.equals("vis_generator");}
    public static Block createBlock(String id){if(!handlesBlock(id))throw new IllegalArgumentException(id);return new VisGeneratorBlock();}
    public static BlockItem createBlockItem(Block block){return new VisGeneratorBlockItem(block);}
    public static void register(IEventBus bus){TILES.register(bus);}
    public static boolean loaded(ServerLevel level,BlockPos pos){return !level.isOutsideBuildHeight(pos)&&level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null;}
}
