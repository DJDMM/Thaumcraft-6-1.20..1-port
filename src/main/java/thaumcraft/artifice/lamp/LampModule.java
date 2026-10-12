package thaumcraft.artifice.lamp;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import thaumcraft.catalog.blocks.CatalogBlocks;

/** Native lamp and its original invisible air-like light, under the existing IDs. */
public final class LampModule {
    private static final DeferredRegister<BlockEntityType<?>> TILES=DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,"thaumcraft");
    public static final RegistryObject<BlockEntityType<ArcaneLampBlockEntity>> ARCANE_LAMP=TILES.register("lamp_arcane",()->BlockEntityType.Builder.of(ArcaneLampBlockEntity::new,CatalogBlocks.block("lamp_arcane")).build(null));
    private LampModule() {}
    public static boolean handlesBlock(String id) {return id.equals("lamp_arcane")||id.equals("effect_glimmer");}
    public static Block createBlock(String id) {return switch(id){case "lamp_arcane"->new ArcaneLampBlock();case "effect_glimmer"->new GlimmerBlock();default->throw new IllegalArgumentException(id);};}
    public static void register(IEventBus bus) {TILES.register(bus);}
}
