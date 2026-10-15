package thaumcraft.artifice.dioptra;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;
import thaumcraft.catalog.blocks.CatalogBlocks;

/** Original dioptra under its existing block and item IDs. */
public final class DioptraModule {
    private static final DeferredRegister<BlockEntityType<?>> TILES=DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,"thaumcraft");
    public static final RegistryObject<BlockEntityType<DioptraBlockEntity>> BLOCK_ENTITY=TILES.register("dioptra",()->BlockEntityType.Builder.of(DioptraBlockEntity::new,CatalogBlocks.block("dioptra")).build(null));
    private DioptraModule(){}
    public static void register(IEventBus bus){TILES.register(bus);}
    public static boolean loaded(LevelReader level,BlockPos pos){return !level.isOutsideBuildHeight(pos)&&(level instanceof ServerLevel server?server.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null:level.hasChunkAt(pos));}
}
