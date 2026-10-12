package thaumcraft.artifice.furnace;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.*;
import thaumcraft.catalog.blocks.CatalogBlocks;

/** Native released furnace and its two paid, invisible shell members. */
@Mod.EventBusSubscriber(modid="thaumcraft")
public final class InfernalFurnaceModule {
    private static final DeferredRegister<BlockEntityType<?>> TILES=DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,"thaumcraft");
    public static final RegistryObject<BlockEntityType<InfernalFurnaceBlockEntity>> FURNACE=TILES.register("infernal_furnace",()->BlockEntityType.Builder.of(InfernalFurnaceBlockEntity::new,CatalogBlocks.block("infernal_furnace")).build(null));
    private InfernalFurnaceModule(){}
    public static boolean handlesBlock(String id){return id.equals("infernal_furnace")||id.equals("placeholder_brick")||id.equals("placeholder_obsidian");}
    public static Block createBlock(String id){return id.equals("infernal_furnace")?new InfernalFurnaceBlock():new InfernalFurnacePlaceholderBlock(id);}
    public static void register(IEventBus bus){TILES.register(bus);}
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void tick(TickEvent.LevelTickEvent event){if(event.phase==TickEvent.Phase.END&&event.level instanceof net.minecraft.server.level.ServerLevel level)InfernalFurnaceOutputData.get(level).tick(level);}
}
