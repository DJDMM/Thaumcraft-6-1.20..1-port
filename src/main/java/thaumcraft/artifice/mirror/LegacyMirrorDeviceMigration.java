package thaumcraft.artifice.mirror;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thaumcraft.artifice.lamp.ArcaneLampBlock;
import thaumcraft.artifice.lamp.ArcaneLampBlockEntity;
import thaumcraft.catalog.blocks.CatalogBlockEntity;
import thaumcraft.world.LoadedChunkMigrationQueue;

/** Storage-free old catalogue states acquire native devices on already loaded chunks. */
@Mod.EventBusSubscriber(modid="thaumcraft")
public final class LegacyMirrorDeviceMigration {
    private static final LoadedChunkMigrationQueue PENDING=new LoadedChunkMigrationQueue();
    private LegacyMirrorDeviceMigration(){}
    @SubscribeEvent public static void load(ChunkEvent.Load event){if(event.getLevel() instanceof ServerLevel level&&event.getChunk() instanceof LevelChunk chunk)PENDING.load(level,chunk);}
    @SubscribeEvent public static void unload(ChunkEvent.Unload event){if(event.getLevel() instanceof ServerLevel level)PENDING.unload(level,event.getChunk().getPos());}
    @SubscribeEvent public static void stop(ServerStoppedEvent event){PENDING.stop(event.getServer());}
    @SubscribeEvent public static void tick(TickEvent.LevelTickEvent event){if(event.phase==TickEvent.Phase.END&&event.level instanceof ServerLevel level)PENDING.process(level,LegacyMirrorDeviceMigration::repair);}
    private static boolean device(BlockState state){return state.getBlock() instanceof MirrorBlock||state.getBlock() instanceof ArcaneLampBlock;}
    public static void repair(ServerLevel level,LevelChunk chunk){
        boolean changed=false;
        for(int index=0;index<chunk.getSectionsCount();index++){
            var section=chunk.getSection(index);if(section.hasOnlyAir()||!section.maybeHas(LegacyMirrorDeviceMigration::device))continue;
            int baseY=chunk.getSectionYFromSectionIndex(index)*16;
            for(int y=0;y<16;y++)for(int x=0;x<16;x++)for(int z=0;z<16;z++){
                var state=section.getBlockState(x,y,z);if(!device(state))continue;
                var pos=new BlockPos(chunk.getPos().getMinBlockX()+x,baseY+y,chunk.getPos().getMinBlockZ()+z);
                var old=chunk.getBlockEntity(pos,LevelChunk.EntityCreationType.CHECK);
                if(old!=null&&!(old instanceof CatalogBlockEntity))continue;
                BlockEntity nativeTile=state.getBlock() instanceof ArcaneLampBlock?new ArcaneLampBlockEntity(pos,state):((MirrorBlock)state.getBlock()).newBlockEntity(pos,state);
                if(old!=null)nativeTile.load(old.saveWithoutMetadata());
                chunk.addAndRegisterBlockEntity(nativeTile);nativeTile.setChanged();chunk.setUnsaved(true);
                level.sendBlockUpdated(pos,state,state,3);changed=true;
            }
        }
        if(changed){var packet=new ClientboundLevelChunkWithLightPacket(chunk,level.getLightEngine(),null,null);for(var player:level.getChunkSource().chunkMap.getPlayers(chunk.getPos(),false))player.connection.send(packet);}
    }
}
