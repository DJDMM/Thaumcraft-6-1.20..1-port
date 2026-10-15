package thaumcraft.artifice.lamp;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.registries.ForgeRegistries;

/** Persistent one-tick bridge for the updateTick -> randomTick split of modern plant blocks. */
public final class LampPlantTickData extends SavedData {
    // A native job accepted after the prior job ran must coexist with its still-pending END-phase bridge.
    private record Key(BlockPos pos,Block block,long at){Key{pos=pos.immutable();}}
    private final Map<Key,Long> pending=new LinkedHashMap<>();private boolean ticking;
    public static LampPlantTickData get(ServerLevel level){return level.getDataStorage().computeIfAbsent(LampPlantTickData::load,LampPlantTickData::new,"thaumcraft_lamp_plant_ticks");}
    void schedule(ServerLevel level,BlockPos pos,Block block){
        // 1.12's scheduled set rejected a repeated position/block, although the lamp still spent its charge.
        if(!EssentiaLampBlockEntity.loaded(level,pos)||level.getBlockTicks().hasScheduledTick(pos,block))return;
        level.scheduleTick(pos,block,1);
        if(!level.getBlockTicks().hasScheduledTick(pos,block))return;
        long at=level.getGameTime()+1;Key key=new Key(pos,block,at);if(pending.putIfAbsent(key,at)==null)setDirty();
    }
    public int pendingCount(){return pending.size();}
    static boolean nativeTicking(ServerLevel level,BlockPos pos){long chunk=ChunkPos.asLong(pos.getX()>>4,pos.getZ()>>4);return level.areEntitiesLoaded(chunk)&&level.getChunkSource().isPositionTicking(chunk);}
    public void tick(ServerLevel level){if(ticking||level.captureBlockSnapshots||level.restoringBlockSnapshots)return;ticking=true;try{for(var row:List.copyOf(pending.entrySet())){
        if(row.getValue()>level.getGameTime()||!EssentiaLampBlockEntity.loaded(level,row.getKey().pos)||!nativeTicking(level,row.getKey().pos))continue;
        var state=level.getBlockState(row.getKey().pos);
        // Retain the paid job without entering native RNG/feature callbacks until their reads are loaded.
        if(state.is(row.getKey().block)&&state.isRandomlyTicking()&&!LampPlantRules.callbackFootprintLoaded(level,row.getKey().pos,state))continue;
        // Retire before native callbacks. Reentrant saves cannot replay an already owned tick.
        pending.remove(row.getKey());setDirty();
        if(state.is(row.getKey().block)&&state.isRandomlyTicking())state.randomTick(level,row.getKey().pos,level.random);
    }}finally{ticking=false;}}
    @Override public CompoundTag save(CompoundTag tag){ListTag rows=new ListTag();for(var row:pending.entrySet()){var id=ForgeRegistries.BLOCKS.getKey(row.getKey().block);if(id==null)continue;CompoundTag entry=new CompoundTag();entry.putLong("Pos",row.getKey().pos.asLong());entry.putString("Block",id.toString());entry.putLong("At",row.getValue());rows.add(entry);}tag.put("Ticks",rows);return tag;}
    public static LampPlantTickData load(CompoundTag tag){LampPlantTickData data=new LampPlantTickData();ListTag rows=tag.getList("Ticks",Tag.TAG_COMPOUND);for(int i=0;i<rows.size();i++){var row=rows.getCompound(i);var id=ResourceLocation.tryParse(row.getString("Block"));if(id==null||!ForgeRegistries.BLOCKS.containsKey(id))continue;long at=row.getLong("At");data.pending.putIfAbsent(new Key(BlockPos.of(row.getLong("Pos")),ForgeRegistries.BLOCKS.getValue(id),at),at);}return data;}
}
