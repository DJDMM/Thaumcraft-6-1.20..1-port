package thaumcraft.artifice.redstone;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import thaumcraft.catalog.blocks.CatalogBlocks;
import thaumcraft.essentia.transport.EssentiaTransportModule;

/** Physical devices from BlockRedstoneRelay/BlockArcaneEar in TC6 BETA26. */
public final class ArtificeRedstoneModule {
    private static final DeferredRegister<BlockEntityType<?>> TILES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "thaumcraft");
    public static final RegistryObject<BlockEntityType<RedstoneRelayBlockEntity>> REDSTONE_RELAY = TILES.register("redstone_relay",
            () -> BlockEntityType.Builder.of(RedstoneRelayBlockEntity::new, CatalogBlocks.block("redstone_relay")).build(null));
    public static final RegistryObject<BlockEntityType<ArcaneEarBlockEntity>> ARCANE_EAR = TILES.register("arcane_ear",
            () -> BlockEntityType.Builder.of(ArcaneEarBlockEntity::new, CatalogBlocks.block("arcane_ear"), CatalogBlocks.block("arcane_ear_toggle")).build(null));
    // Tubes and relay dials share the original key sound; register its ID only once.
    public static final RegistryObject<SoundEvent> KEY = EssentiaTransportModule.KEY;

    private ArtificeRedstoneModule() {}
    public static boolean handlesBlock(String id) {
        return id.equals("redstone_relay") || id.equals("arcane_ear") || id.equals("arcane_ear_toggle");
    }
    public static Block createBlock(String id) {
        return switch (id) {
            case "redstone_relay" -> new RedstoneRelayBlock();
            case "arcane_ear" -> new ArcaneEarBlock(false);
            case "arcane_ear_toggle" -> new ArcaneEarBlock(true);
            default -> throw new IllegalArgumentException("Unknown redstone device " + id);
        };
    }
    public static void register(IEventBus bus) { TILES.register(bus); }
    static boolean loaded(BlockGetter level, BlockPos pos) {
        return !(level instanceof LevelReader reader) || (!reader.isOutsideBuildHeight(pos) && reader.hasChunkAt(pos));
    }
}
