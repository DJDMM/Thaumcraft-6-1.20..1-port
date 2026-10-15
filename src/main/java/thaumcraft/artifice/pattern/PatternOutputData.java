package thaumcraft.artifice.pattern;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

/** A paid operation owns its undelivered output through removal, callbacks, save and canceled item joins. */
public final class PatternOutputData extends SavedData {
    private static final class Physical extends ItemEntity {boolean accepted;Physical(ServerLevel level,BlockPos pos,ItemStack stack){super(level,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,stack);}@Override public void onAddedToWorld(){super.onAddedToWorld();accepted=true;}}
    private static final class Batch {final BlockPos pos;final UUID owner;final List<ItemStack> outputs;boolean detached;Batch(BlockPos pos,UUID owner,List<ItemStack> outputs,boolean detached){this.pos=pos.immutable();this.owner=owner;this.outputs=new ArrayList<>(outputs.stream().map(ItemStack::copy).toList());this.detached=detached;}}
    private final List<Batch> batches=new ArrayList<>();private boolean ticking;private Batch inFlight;private int index;private Physical physical;
    public static PatternOutputData get(ServerLevel level){return level.getDataStorage().computeIfAbsent(PatternOutputData::load,PatternOutputData::new,"thaumcraft_pattern_outputs");}
    public void add(BlockPos pos,UUID owner,List<ItemStack> outputs){batches.add(new Batch(pos,owner,outputs,false));setDirty();}
    public int pendingCount(UUID owner){int count=0;for(Batch batch:batches)if(batch.owner.equals(owner))for(int i=0;i<batch.outputs.size();i++)if(batch!=inFlight||i!=index||physical==null||!physical.accepted)count+=batch.outputs.get(i).getCount();return count;}
    public void detach(UUID owner){for(Batch batch:batches)if(batch.owner.equals(owner))batch.detached=true;setDirty();}
    public void tick(ServerLevel level){if(ticking||level.captureBlockSnapshots||level.restoringBlockSnapshots)return;ticking=true;try{for(Batch batch:List.copyOf(batches)){
        if(!NativePatternInventory.loaded(level,batch.pos))continue;
        if(!batch.detached){if(!(level.getBlockEntity(batch.pos) instanceof PatternCrafterBlockEntity tile)||!tile.ownerId().equals(batch.owner))batch.detached=true;else if(PatternCrafterBlockItem.uncommitted(level,batch.pos))continue;}
        if(batch.detached){for(int i=0;i<batch.outputs.size();i++){ItemStack stack=batch.outputs.get(i);if(stack.isEmpty())continue;inFlight=batch;index=i;physical=new Physical(level,batch.pos,stack.copy());physical.setDefaultPickUpDelay();try{level.addFreshEntity(physical);}catch(RuntimeException callback){/* Acceptance is authoritative even if a native join callback later throws. */}if(physical.accepted){batch.outputs.set(i,ItemStack.EMPTY);setDirty();}physical=null;inFlight=null;}}
        else {NativePatternInventory output=NativePatternInventory.capture(level,batch.pos.below(),Direction.UP);if(output==null)continue;List<NativePatternInventory.Insertion> plan=output.reserve(batch.outputs);if(plan==null||!output.stable())continue;
            for(var insertion:plan){if(batch.detached||!output.stable())break;ItemStack source=batch.outputs.get(insertion.output());ItemStack offered=source.copy();source.shrink(insertion.amount());setDirty();
                // No callback lies between ownership transfer and the exact native setter's assignment.
                // That setter's later comparator callback can remove either endpoint without replaying payment.
                try{output.insert(insertion,offered);}catch(RuntimeException callback){break;}}
            try{output.changed();}catch(RuntimeException callback){/* Native setters already own accepted stacks. */}
        }
        if(batch.outputs.stream().allMatch(ItemStack::isEmpty)){batches.remove(batch);setDirty();}
    }}finally{inFlight=null;physical=null;ticking=false;}}
    @Override public CompoundTag save(CompoundTag tag){var list=new ListTag();for(Batch batch:batches){var row=new CompoundTag();row.putLong("Pos",batch.pos.asLong());row.putUUID("Owner",batch.owner);row.putBoolean("Detached",batch.detached);var stacks=new ListTag();for(int i=0;i<batch.outputs.size();i++)if(batch!=inFlight||i!=index||physical==null||!physical.accepted){var stack=batch.outputs.get(i);if(!stack.isEmpty())stacks.add(stack.save(new CompoundTag()));}if(!stacks.isEmpty()){row.put("Outputs",stacks);list.add(row);}}tag.put("Batches",list);return tag;}
    public static PatternOutputData load(CompoundTag tag){var data=new PatternOutputData();var list=tag.getList("Batches",Tag.TAG_COMPOUND);for(int i=0;i<list.size();i++){var row=list.getCompound(i);if(!row.hasUUID("Owner"))continue;var items=row.getList("Outputs",Tag.TAG_COMPOUND);var outputs=new ArrayList<ItemStack>();for(int j=0;j<items.size();j++){var stack=ItemStack.of(items.getCompound(j));if(!stack.isEmpty())outputs.add(stack);}if(!outputs.isEmpty())data.batches.add(new Batch(BlockPos.of(row.getLong("Pos")),row.getUUID("Owner"),outputs,row.getBoolean("Detached")));}return data;}
}
