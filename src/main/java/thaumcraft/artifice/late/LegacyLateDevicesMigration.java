package thaumcraft.artifice.late;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thaumcraft.catalog.blocks.CatalogBlockEntity;
import thaumcraft.world.LoadedChunkMigrationQueue;
/** Existing catalogue-only device blocks need ticking native BEs without rewriting paid inventories. */
@Mod.EventBusSubscriber(modid="thaumcraft")
public final class LegacyLateDevicesMigration {
    private static final LoadedChunkMigrationQueue PENDING=new LoadedChunkMigrationQueue();
    private LegacyLateDevicesMigration(){}
    @SubscribeEvent public static void load(ChunkEvent.Load e){if(e.getLevel() instanceof ServerLevel l&&e.getChunk() instanceof LevelChunk c)PENDING.load(l,c);}
    @SubscribeEvent public static void unload(ChunkEvent.Unload e){if(e.getLevel() instanceof ServerLevel l)PENDING.unload(l,e.getChunk().getPos());}
    @SubscribeEvent public static void stop(ServerStoppedEvent e){PENDING.stop(e.getServer());}
    @SubscribeEvent public static void tick(TickEvent.LevelTickEvent e){if(e.phase==TickEvent.Phase.END&&e.level instanceof ServerLevel l)PENDING.process(l,LegacyLateDevicesMigration::repair);}
    private static boolean device(BlockState s){return s.getBlock() instanceof SpaBlock||s.getBlock() instanceof EverfullUrnBlock||s.getBlock() instanceof PotionSprayerBlock||s.getBlock() instanceof PavingStoneBlock p&&p.barrier();}
    public static void repair(ServerLevel l,LevelChunk c){
        if(l.getChunkSource().getChunkNow(c.getPos().x,c.getPos().z)!=c)return;boolean changed=false;
        for(int sectionIndex=0;sectionIndex<c.getSectionsCount();sectionIndex++){var section=c.getSection(sectionIndex);if(section.hasOnlyAir()||!section.maybeHas(LegacyLateDevicesMigration::device))continue;int baseY=c.getSectionYFromSectionIndex(sectionIndex)*16;
            for(int y=0;y<16;y++)for(int x=0;x<16;x++)for(int z=0;z<16;z++){var s=section.getBlockState(x,y,z);if(!device(s))continue;var p=new BlockPos(c.getPos().getMinBlockX()+x,baseY+y,c.getPos().getMinBlockZ()+z);var old=c.getBlockEntity(p,LevelChunk.EntityCreationType.CHECK);if(old!=null&&(old.getClass()!=CatalogBlockEntity.class||!old.saveWithoutMetadata().isEmpty()))continue;var tile=((EntityBlock)s.getBlock()).newBlockEntity(p,s);if(tile==null)continue;c.addAndRegisterBlockEntity(tile);tile.setChanged();c.setUnsaved(true);l.sendBlockUpdated(p,s,s,3);changed=true;}
        }
        if(changed){var packet=new ClientboundLevelChunkWithLightPacket(c,l.getLightEngine(),null,null);for(var player:l.getChunkSource().chunkMap.getPlayers(c.getPos(),false))player.connection.send(packet);}
    }
}
