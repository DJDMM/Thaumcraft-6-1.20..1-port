package thaumcraft.fx.nitor;

import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Repairs only loaded Nitor positions from old plain/visual blocks; never loads a new chunk. */
@Mod.EventBusSubscriber(modid="thaumcraft")
public final class NitorLifecycle {
    private record Loaded(Level level,LevelChunk chunk){}
    private static final ConcurrentLinkedQueue<Loaded> LOADED=new ConcurrentLinkedQueue<>();
    private NitorLifecycle(){}
    @SubscribeEvent public static void loaded(ChunkEvent.Load event){if(event.getLevel() instanceof Level level&&event.getChunk() instanceof LevelChunk chunk)LOADED.add(new Loaded(level,chunk));}
    @SubscribeEvent public static void unloaded(ChunkEvent.Unload event){LOADED.removeIf(task->task.level==event.getLevel()&&task.chunk==event.getChunk());}
    @SubscribeEvent public static void tick(TickEvent.LevelTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;int pending=LOADED.size();
        for(int i=0;i<pending;i++){var task=LOADED.poll();if(task==null)break;if(task.level!=event.level){LOADED.add(task);continue;}var chunk=task.chunk;
            if(event.level.getChunkSource().getChunk(chunk.getPos().x,chunk.getPos().z,net.minecraft.world.level.chunk.ChunkStatus.FULL,false)!=chunk)continue;
            var sections=chunk.getSections();var mutable=new BlockPos.MutableBlockPos();
            for(int sectionIndex=0;sectionIndex<sections.length;sectionIndex++){
                var section=sections[sectionIndex];if(section.hasOnlyAir()||!section.getStates().maybeHas(s->s.getBlock() instanceof NitorBlock))continue;
                for(int x=0;x<16;x++)for(int y=0;y<16;y++)for(int z=0;z<16;z++){
                    var state=section.getBlockState(x,y,z);if(!(state.getBlock() instanceof NitorBlock))continue;
                    mutable.set(chunk.getPos().getMinBlockX()+x,event.level.getMinBuildHeight()+16*sectionIndex+y,chunk.getPos().getMinBlockZ()+z);var pos=mutable.immutable();
                    if(!(chunk.getBlockEntity(pos) instanceof NitorBlockEntity))event.level.setBlockEntity(new NitorBlockEntity(pos,state));
                }
            }
        }
    }
}
