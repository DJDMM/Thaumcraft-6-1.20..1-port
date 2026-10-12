package thaumcraft.artifice.mirror;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

/** One persistent owner for paid contents after the native block/menu owner disappears. */
public final class MirrorRefundSavedData extends SavedData {
    /** Native ITEM lifecycle proof survives an accepted output being consumed in a join callback. */
    private static final class OutputItemEntity extends ItemEntity {
        private boolean acceptedOnce;
        OutputItemEntity(ServerLevel level,double x,double y,double z,ItemStack stack){super(level,x,y,z,stack);}
        @Override public void onAddedToWorld(){super.onAddedToWorld();acceptedOnce=true;}
    }
    private static final class Refund {
        final BlockPos pos; final Direction face; final ItemStack stack;
        final MirrorPlacementReceipt placement;
        Refund(BlockPos pos, Direction face, ItemStack stack) {this(pos,face,stack,null);}
        Refund(BlockPos pos,Direction face,ItemStack stack,MirrorPlacementReceipt placement){this.pos=pos.immutable();this.face=face;this.stack=stack.copy();this.placement=placement;}
    }
    private final List<Refund> pending=new ArrayList<>();
    private boolean ejecting;
    private Refund inFlight;
    private ItemEntity inFlightEntity;
    public static MirrorRefundSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(MirrorRefundSavedData::load,MirrorRefundSavedData::new,"thaumcraft_mirror_refunds");
    }
    public void add(BlockPos pos,Direction face,ItemStack stack) {
        if (!stack.isEmpty()) { pending.add(new Refund(pos,face,stack)); setDirty(); }
    }
    void addPlacement(BlockPos pos,Direction face,ItemStack stack,MirrorPlacementReceipt receipt){if(!stack.isEmpty()){pending.add(new Refund(pos,face,stack,receipt));setDirty();}}
    private boolean owns(Refund refund){return refund!=inFlight||inFlightEntity==null||!accepted(inFlightEntity);}
    public int pendingCount() { return pending.stream().filter(this::owns).mapToInt(r->r.stack.getCount()).sum(); }
    public int pendingCount(BlockPos pos) { return pending.stream().filter(r->r.pos.equals(pos)&&owns(r)).mapToInt(r->r.stack.getCount()).sum(); }
    public void tick(ServerLevel level) {
        if (ejecting) return;
        ejecting=true;
        try {
            // The detached batch prevents a listener adding or reentering the refund queue from replaying it.
            for (Refund refund:List.copyOf(pending)) {
                if(refund.placement!=null){if(!refund.placement.ready())continue;if(!refund.placement.paid()){pending.remove(refund);setDirty();continue;}}
                if (level.getChunkSource().getChunkNow(refund.pos.getX()>>4,refund.pos.getZ()>>4)==null) continue;
                ItemEntity entity=output(level,refund.pos,refund.face,refund.stack.copy());
                inFlight=refund;inFlightEntity=entity;
                try{if(spawn(level,entity)){pending.remove(refund);setDirty();}}
                finally{inFlight=null;inFlightEntity=null;}
            }
        } finally { ejecting=false; }
    }
    static ItemEntity output(ServerLevel level,BlockPos pos,Direction face,ItemStack stack) {
        ItemEntity entity=new OutputItemEntity(level,pos.getX()+.5,pos.getY()+.25,pos.getZ()+.5,stack);
        entity.setDeltaMovement(face.getStepX()*.15,face.getStepY()*.15,face.getStepZ()*.15);
        entity.setPortalCooldown(20); return entity;
    }
    static boolean spawn(ServerLevel level,ItemEntity entity) {
        try {return level.addFreshEntity(entity)||accepted(entity);}
        catch (RuntimeException failure) {return accepted(entity);}
    }
    static boolean accepted(ItemEntity entity){return entity instanceof OutputItemEntity output?output.acceptedOnce:entity.isAddedToWorld();}
    @Override public CompoundTag save(CompoundTag tag) {
        ListTag rows=new ListTag();
        for (Refund refund:pending) {
            if(!owns(refund))continue;
            if(refund.placement!=null&&!refund.placement.paid())continue;
            CompoundTag row=new CompoundTag(); row.putLong("Pos",refund.pos.asLong());row.putByte("Face",(byte)refund.face.get3DDataValue());row.put("Stack",refund.stack.save(new CompoundTag()));rows.add(row);
        }
        tag.put("Refunds",rows);return tag;
    }
    public static MirrorRefundSavedData load(CompoundTag tag) {
        MirrorRefundSavedData data=new MirrorRefundSavedData(); ListTag rows=tag.getList("Refunds",Tag.TAG_COMPOUND);
        for(int i=0;i<rows.size();i++) { CompoundTag row=rows.getCompound(i);ItemStack stack=ItemStack.of(row.getCompound("Stack"));if(!stack.isEmpty())data.pending.add(new Refund(BlockPos.of(row.getLong("Pos")),Direction.from3DDataValue(row.getByte("Face")),stack)); }
        return data;
    }
}
