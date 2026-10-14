package thaumcraft.essentia.condenser;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thaumcraft.catalog.blocks.CatalogBlockEntity;
import thaumcraft.world.LoadedChunkMigrationQueue;

/** Only empty catalogue anchors are replaced, on the server END phase of already loaded chunks. */
@Mod.EventBusSubscriber(modid="thaumcraft")
public final class LegacyCondenserMigration {
    private static final LoadedChunkMigrationQueue PENDING=new LoadedChunkMigrationQueue();
    private LegacyCondenserMigration(){}
    @SubscribeEvent public static void load(ChunkEvent.Load event){if(event.getLevel() instanceof ServerLevel level&&event.getChunk() instanceof LevelChunk chunk)PENDING.load(level,chunk);}
    @SubscribeEvent public static void unload(ChunkEvent.Unload event){if(event.getLevel() instanceof ServerLevel level)PENDING.unload(level,event.getChunk().getPos());}
    @SubscribeEvent public static void stop(ServerStoppedEvent event){PENDING.stop(event.getServer());}
    @SubscribeEvent public static void tick(TickEvent.LevelTickEvent event){if(event.phase==TickEvent.Phase.END&&event.level instanceof ServerLevel level)PENDING.process(level,LegacyCondenserMigration::repair);}
    private static boolean condenser(BlockState state){return state.getBlock() instanceof CondenserBlock;}
    public static void repair(ServerLevel level,LevelChunk chunk){if(level.getChunkSource().getChunkNow(chunk.getPos().x,chunk.getPos().z)!=chunk)return;boolean changed=false;
        // The original catalogue condenser had no BE at all; scanning BE coordinates misses its stored blockstate.
        for(int sectionIndex=0;sectionIndex<chunk.getSectionsCount();sectionIndex++){var section=chunk.getSection(sectionIndex);if(section.hasOnlyAir()||!section.maybeHas(LegacyCondenserMigration::condenser))continue;int baseY=chunk.getSectionYFromSectionIndex(sectionIndex)*16;
            for(int y=0;y<16;y++)for(int x=0;x<16;x++)for(int z=0;z<16;z++){BlockState state=section.getBlockState(x,y,z);if(!condenser(state))continue;BlockPos pos=new BlockPos(chunk.getPos().getMinBlockX()+x,baseY+y,chunk.getPos().getMinBlockZ()+z);
                BlockEntity old=chunk.getBlockEntity(pos,LevelChunk.EntityCreationType.CHECK);if(old!=null&&old.getClass()!=CatalogBlockEntity.class)continue;var tile=new CondenserBlockEntity(pos,state);chunk.addAndRegisterBlockEntity(tile);tile.setChanged();chunk.setUnsaved(true);level.sendBlockUpdated(pos,state,state,3);changed=true;
            }
        }
        if(changed){var packet=new ClientboundLevelChunkWithLightPacket(chunk,level.getLightEngine(),null,null);for(var player:level.getChunkSource().chunkMap.getPlayers(chunk.getPos(),false))player.connection.send(packet);}}
}
