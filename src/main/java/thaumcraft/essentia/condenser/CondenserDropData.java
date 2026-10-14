package thaumcraft.essentia.condenser;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

/** Physical lattice/crystal drops remain owned until a native entity has actually joined the world. */
public final class CondenserDropData extends SavedData {
    private static final class PhysicalItem extends ItemEntity {
        boolean accepted;
        PhysicalItem(ServerLevel level,double x,double y,double z,ItemStack stack){super(level,x,y,z,stack);}
        @Override public void onAddedToWorld(){super.onAddedToWorld();accepted=true;}
    }
    private record Drop(BlockPos pos,double x,double y,double z,ItemStack stack,int pickupDelay){Drop{pos=pos.immutable();stack=stack.copy();}}
    private final List<Drop> drops=new ArrayList<>();private boolean ticking;private Drop inFlight;private PhysicalItem physical;
    public static CondenserDropData get(ServerLevel level){return level.getDataStorage().computeIfAbsent(CondenserDropData::load,CondenserDropData::new,"thaumcraft_condenser_drops");}
    public void add(BlockPos pos,double x,double y,double z,ItemStack stack){add(pos,x,y,z,stack,0);}
    public void add(BlockPos pos,double x,double y,double z,ItemStack stack,int pickupDelay){if(!stack.isEmpty()){drops.add(new Drop(pos,x,y,z,stack,Math.max(0,Math.min(32767,pickupDelay))));setDirty();}}
    public int pendingCount(BlockPos pos){return drops.stream().filter(d->d.pos.equals(pos)&&(d!=inFlight||physical==null||!physical.accepted)).mapToInt(d->d.stack.getCount()).sum();}
    public void tick(ServerLevel level){if(ticking)return;ticking=true;try{for(Drop drop:List.copyOf(drops)){if(!CondenserBlockEntity.loaded(level,drop.pos))continue;inFlight=drop;physical=new PhysicalItem(level,drop.x,drop.y,drop.z,drop.stack.copy());physical.setPickUpDelay(drop.pickupDelay);
        try{level.addFreshEntity(physical);}catch(RuntimeException callback){/* onAddedToWorld remains authoritative after throwing callbacks. */}
        if(physical.accepted){drops.remove(drop);setDirty();}inFlight=null;physical=null;
    }}finally{inFlight=null;physical=null;ticking=false;}}
    @Override public CompoundTag save(CompoundTag tag){ListTag list=new ListTag();for(Drop drop:drops){if(drop==inFlight&&physical!=null&&physical.accepted)continue;CompoundTag row=new CompoundTag();row.putLong("Pos",drop.pos.asLong());row.putDouble("X",drop.x);row.putDouble("Y",drop.y);row.putDouble("Z",drop.z);row.putInt("PickupDelay",drop.pickupDelay);row.put("Stack",drop.stack.save(new CompoundTag()));list.add(row);}tag.put("Drops",list);return tag;}
    public static CondenserDropData load(CompoundTag tag){CondenserDropData data=new CondenserDropData();ListTag list=tag.getList("Drops",Tag.TAG_COMPOUND);for(int i=0;i<list.size();i++){CompoundTag row=list.getCompound(i);ItemStack stack=ItemStack.of(row.getCompound("Stack"));if(!stack.isEmpty()){BlockPos pos=BlockPos.of(row.getLong("Pos"));double x=row.getDouble("X"),y=row.getDouble("Y"),z=row.getDouble("Z");if(Double.isFinite(x)&&Double.isFinite(y)&&Double.isFinite(z))data.drops.add(new Drop(pos,x,y,z,stack,Math.max(0,Math.min(32767,row.getInt("PickupDelay")))));}}return data;}
}
