package thaumcraft.artifice.redstone;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thaumcraft.catalog.blocks.CatalogBlockEntity;
import thaumcraft.world.LoadedChunkMigrationQueue;

/** Previously storage-free catalogue states acquire their native BEs on already loaded chunks. */
@Mod.EventBusSubscriber(modid = "thaumcraft")
public final class LegacyRedstoneDeviceMigration {
    private static final LoadedChunkMigrationQueue PENDING = new LoadedChunkMigrationQueue();
    private LegacyRedstoneDeviceMigration() {}
    @SubscribeEvent public static void load(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk) PENDING.load(level, chunk);
    }
    @SubscribeEvent public static void unload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) PENDING.unload(level, event.getChunk().getPos());
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent event) { PENDING.stop(event.getServer()); }
    @SubscribeEvent public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel level) PENDING.process(level, LegacyRedstoneDeviceMigration::repair);
    }
    static void repair(ServerLevel level, LevelChunk chunk) {
        boolean changed = false;
        for (int index = 0; index < chunk.getSectionsCount(); index++) {
            var section = chunk.getSection(index);
            if (section.hasOnlyAir() || !section.maybeHas(state -> state.getBlock() instanceof RedstoneRelayBlock || state.getBlock() instanceof ArcaneEarBlock)) continue;
            int baseY = chunk.getSectionYFromSectionIndex(index) * 16;
            for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                var state = section.getBlockState(x, y, z);
                if (!(state.getBlock() instanceof RedstoneRelayBlock) && !(state.getBlock() instanceof ArcaneEarBlock)) continue;
                var pos = new BlockPos(chunk.getPos().getMinBlockX() + x, baseY + y, chunk.getPos().getMinBlockZ() + z);
                var old = chunk.getBlockEntity(pos, LevelChunk.EntityCreationType.CHECK);
                if (old != null && !(old instanceof CatalogBlockEntity)) continue;
                BlockEntity nativeTile = state.getBlock() instanceof RedstoneRelayBlock ? new RedstoneRelayBlockEntity(pos, state) : new ArcaneEarBlockEntity(pos, state);
                if (old != null) {
                    var tag = old.saveWithoutMetadata();
                    // Retain actual tuning keys if a prior native save was held by an old anchor.
                    if (nativeTile instanceof RedstoneRelayBlockEntity relay) {
                        if (tag.contains("in")) relay.setIn(tag.getByte("in"));
                        if (tag.contains("out")) relay.setOut(tag.getByte("out"));
                    } else if (tag.contains("note") && tag.contains("tone")) nativeTile.load(tag);
                }
                chunk.addAndRegisterBlockEntity(nativeTile); nativeTile.setChanged(); chunk.setUnsaved(true);
                level.sendBlockUpdated(pos, state, state, 3); changed = true;
            }
        }
        if (changed) {
            var packet = new ClientboundLevelChunkWithLightPacket(chunk, level.getLightEngine(), null, null);
            for (var player : level.getChunkSource().chunkMap.getPlayers(chunk.getPos(), false)) player.connection.send(packet);
        }
    }
}
