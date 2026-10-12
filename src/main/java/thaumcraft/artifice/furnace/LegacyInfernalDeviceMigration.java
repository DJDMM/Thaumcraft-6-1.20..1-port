package thaumcraft.artifice.furnace;

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
import thaumcraft.catalog.blocks.CatalogBlockEntity;
import thaumcraft.essentia.production.*;
import thaumcraft.world.LoadedChunkMigrationQueue;

/** Repair only storage-free catalogue/missing legacy tiles in already loaded chunks. */
@Mod.EventBusSubscriber(modid="thaumcraft")
public final class LegacyInfernalDeviceMigration {
    private static final LoadedChunkMigrationQueue PENDING=new LoadedChunkMigrationQueue();
    private LegacyInfernalDeviceMigration(){}
    @SubscribeEvent public static void load(ChunkEvent.Load event){if(event.getLevel() instanceof ServerLevel level&&event.getChunk() instanceof LevelChunk chunk)PENDING.load(level,chunk);}
    @SubscribeEvent public static void unload(ChunkEvent.Unload event){if(event.getLevel() instanceof ServerLevel level)PENDING.unload(level,event.getChunk().getPos());}
    @SubscribeEvent public static void stop(ServerStoppedEvent event){PENDING.stop(event.getServer());}
    @SubscribeEvent public static void tick(TickEvent.LevelTickEvent event){if(event.phase==TickEvent.Phase.END&&event.level instanceof ServerLevel level)PENDING.process(level,LegacyInfernalDeviceMigration::repair);}
    private static boolean device(BlockState state){return state.getBlock() instanceof InfernalFurnaceBlock||state.getBlock() instanceof SmelterBellowsBlock;}
    public static void repair(ServerLevel level,LevelChunk chunk){
        if(level.getChunkSource().getChunkNow(chunk.getPos().x,chunk.getPos().z)!=chunk)return;
        boolean changed=false;
        for(int sectionIndex=0;sectionIndex<chunk.getSectionsCount();sectionIndex++){
            var section=chunk.getSection(sectionIndex);if(section.hasOnlyAir()||!section.maybeHas(LegacyInfernalDeviceMigration::device))continue;int baseY=chunk.getSectionYFromSectionIndex(sectionIndex)*16;
            for(int y=0;y<16;y++)for(int x=0;x<16;x++)for(int z=0;z<16;z++){
                BlockState state=section.getBlockState(x,y,z);if(!device(state))continue;BlockPos pos=new BlockPos(chunk.getPos().getMinBlockX()+x,baseY+y,chunk.getPos().getMinBlockZ()+z);
                BlockEntity old=chunk.getBlockEntity(pos,LevelChunk.EntityCreationType.CHECK);if(old!=null&&!(old instanceof CatalogBlockEntity))continue;
                BlockEntity tile=state.getBlock() instanceof InfernalFurnaceBlock?new InfernalFurnaceBlockEntity(pos,state):new BellowsBlockEntity(pos,state);
                if(old!=null)tile.load(old.saveWithoutMetadata());chunk.addAndRegisterBlockEntity(tile);tile.setChanged();chunk.setUnsaved(true);level.sendBlockUpdated(pos,state,state,3);changed=true;
            }
        }
        if(changed){var packet=new ClientboundLevelChunkWithLightPacket(chunk,level.getLightEngine(),null,null);for(var player:level.getChunkSource().chunkMap.getPlayers(chunk.getPos(),false))player.connection.send(packet);}
    }
}
