package thaumcraft.artifice.furnace;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.*;
import net.minecraftforge.items.wrapper.*;

/** Persistent physical ownership after cooking/removal; canceled native joins retain the exact output. */
public final class InfernalFurnaceOutputData extends SavedData {
    private static final class OutputItem extends ItemEntity {
        boolean accepted;
        OutputItem(ServerLevel level,double x,double y,double z,ItemStack stack){super(level,x,y,z,stack);}
        @Override public void onAddedToWorld(){super.onAddedToWorld();accepted=true;}
    }
    private static final class OutputXp extends ExperienceOrb {
        boolean accepted;
        OutputXp(ServerLevel level,double x,double y,double z,int value){super(level,x,y,z,value);}
        @Override public void onAddedToWorld(){super.onAddedToWorld();accepted=true;}
    }
    private static final class Output {
        final BlockPos pos;final Direction face;ItemStack stack;final int xp;final boolean insert;
        Output(BlockPos pos,Direction face,ItemStack stack,int xp,boolean insert){this.pos=pos.immutable();this.face=face;this.stack=stack.copy();this.xp=xp;this.insert=insert;}
    }
    private final List<Output> outputs=new ArrayList<>();
    private boolean ticking;
    private Output inFlight;
    private net.minecraft.world.entity.Entity entity;
    private IItemHandler insertion;
    private int beforeInsertion;
    private boolean observingInsertion;
    private int observedPaid;
    public static InfernalFurnaceOutputData get(ServerLevel level){return level.getDataStorage().computeIfAbsent(InfernalFurnaceOutputData::load,InfernalFurnaceOutputData::new,"thaumcraft_infernal_outputs");}
    public void add(BlockPos pos,Direction face,ItemStack stack,boolean insert){if(!stack.isEmpty()){outputs.add(new Output(pos,face,stack,0,insert));setDirty();}}
    public void addExperience(BlockPos pos,Direction face,int total){while(total>0){int split=ExperienceOrb.getExperienceValue(total);outputs.add(new Output(pos,face,ItemStack.EMPTY,split,false));total-=split;}setDirty();}
    private boolean accepted(){return entity instanceof OutputItem item?item.accepted:entity instanceof OutputXp orb&&orb.accepted;}
    private int physicalCount(IItemHandler handler,ItemStack stack){int total=0;for(int slot=0;slot<handler.getSlots();slot++){ItemStack actual=handler.getStackInSlot(slot);if(ItemStack.isSameItemSameTags(stack,actual))total+=actual.getCount();}return total;}
    private ItemStack owned(Output output){if(output!=inFlight)return output.stack;if(accepted())return ItemStack.EMPTY;ItemStack stack=output.stack.copy();if(insertion!=null){if(!observingInsertion){observingInsertion=true;try{observedPaid=Math.max(0,Math.min(stack.getCount(),physicalCount(insertion,stack)-beforeInsertion));}finally{observingInsertion=false;}}stack.shrink(observedPaid);}return stack;}
    public int pendingItemCount(BlockPos pos){return outputs.stream().filter(o->o.pos.equals(pos)).mapToInt(o->owned(o).getCount()).sum();}
    public int pendingExperience(BlockPos pos){return outputs.stream().filter(o->o.pos.equals(pos)&&(o!=inFlight||!accepted())).mapToInt(o->o.xp).sum();}
    private static boolean loaded(ServerLevel level,BlockPos pos){return level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null;}
    private static IItemHandler handler(ServerLevel level,BlockPos pos,Direction side){
        if(!loaded(level,pos))return null;
        var hook=VanillaInventoryCodeHooks.getItemHandler(level,pos.getX(),pos.getY(),pos.getZ(),side);
        if(hook.isPresent())return hook.get().getLeft();
        var tile=level.getBlockEntity(pos);if(tile==null)return null;
        var handler=tile.getCapability(ForgeCapabilities.ITEM_HANDLER,side).resolve().orElse(null);
        if(handler!=null)return handler;
        return tile instanceof WorldlyContainer sided?new SidedInvWrapper(sided,side):tile instanceof Container container?new InvWrapper(container):null;
    }
    public void tick(ServerLevel level){if(ticking)return;ticking=true;try{for(Output output:List.copyOf(outputs)){
        if(!loaded(level,output.pos)||!loaded(level,output.pos.relative(output.face)))continue;
        inFlight=output;entity=null;insertion=null;
        try{
            if(!output.stack.isEmpty()){
                if(output.insert){IItemHandler recipient=handler(level,output.pos.relative(output.face),output.face.getOpposite());if(recipient!=null){beforeInsertion=physicalCount(recipient,output.stack);observedPaid=0;insertion=recipient;ItemStack offered=output.stack.copy();try{ItemStack remainder=ItemHandlerHelper.insertItem(insertion,offered,false);output.stack=remainder.copy();}catch(RuntimeException callback){output.stack=owned(output);}finally{insertion=null;}setDirty();}}
                if(output.stack.isEmpty()){outputs.remove(output);continue;}
                BlockPos origin=level.getBlockState(output.pos.relative(output.face)).isCollisionShapeFullBlock(level,output.pos.relative(output.face))?output.pos.relative(output.face.getOpposite()):output.pos;
                OutputItem item=new OutputItem(level,origin.getX()+.5+output.face.getStepX(),origin.getY()+output.face.getStepY(),origin.getZ()+.5+output.face.getStepZ(),output.stack.copy());
                item.setDeltaMovement(.3*output.face.getStepX(),.3*output.face.getStepY(),.3*output.face.getStepZ());entity=item;
            }else if(output.xp>0){
                OutputXp orb=new OutputXp(level,output.pos.getX()+.5+output.face.getStepX()*1.2,output.pos.getY()+.4,output.pos.getZ()+.5+output.face.getStepZ()*1.2,output.xp);
                double x=output.face.getStepX()==0?(level.random.nextFloat()-level.random.nextFloat())*.025:output.face.getStepX()*.13;
                double z=output.face.getStepZ()==0?(level.random.nextFloat()-level.random.nextFloat())*.025:output.face.getStepZ()*.13;
                orb.setDeltaMovement(x,0,z);entity=orb;
            }else{outputs.remove(output);continue;}
            try{level.addFreshEntity(entity);}catch(RuntimeException callback){/* Native acceptance below is authoritative even after an external callback throws. */}
            if(accepted()){outputs.remove(output);setDirty();}
        }finally{inFlight=null;entity=null;insertion=null;}
    }}finally{ticking=false;}}
    @Override public CompoundTag save(CompoundTag tag){ListTag list=new ListTag();for(Output output:outputs){if(output==inFlight&&accepted())continue;ItemStack stack=owned(output);if(stack.isEmpty()&&output.xp<=0)continue;CompoundTag row=new CompoundTag();row.putLong("Pos",output.pos.asLong());row.putByte("Face",(byte)output.face.get3DDataValue());row.put("Stack",stack.save(new CompoundTag()));row.putInt("XP",output.xp);row.putBoolean("Insert",output.insert);list.add(row);}tag.put("Outputs",list);return tag;}
    public static InfernalFurnaceOutputData load(CompoundTag tag){InfernalFurnaceOutputData data=new InfernalFurnaceOutputData();ListTag list=tag.getList("Outputs",Tag.TAG_COMPOUND);for(int i=0;i<list.size();i++){var row=list.getCompound(i);ItemStack stack=ItemStack.of(row.getCompound("Stack"));int xp=Math.max(0,row.getInt("XP"));if(!stack.isEmpty()||xp>0)data.outputs.add(new Output(BlockPos.of(row.getLong("Pos")),Direction.from3DDataValue(row.getByte("Face")),stack,xp,row.getBoolean("Insert")));}return data;}
}
