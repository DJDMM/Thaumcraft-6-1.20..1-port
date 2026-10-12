package thaumcraft.artifice.mirror;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

/** Original append-only random-entry output queue; no player storage or extraction. */
public final class ItemMirrorBlockEntity extends AbstractMirrorBlockEntity {
    private final List<ItemStack> output=new ArrayList<>();
    private boolean ejecting, transferring;
    private ItemStack reserved=ItemStack.EMPTY;
    private ItemEntity reservedEntity;
    private ItemStack reservedEntry=ItemStack.EMPTY;
    private int reservedIndex;
    private boolean migrateAfterEjection;
    private final IItemHandler inventory=new IItemHandler() {
        @Override public int getSlots(){return 1;}
        @Override public ItemStack getStackInSlot(int slot){return ItemStack.EMPTY;}
        @Override public ItemStack extractItem(int slot,int amount,boolean simulate){return ItemStack.EMPTY;}
        @Override public int getSlotLimit(int slot){return 64;}
        @Override public boolean isItemValid(int slot,ItemStack stack){return slot==0&&!stack.isEmpty()&&!removing()&&resolvePeer(true).status()==Status.VALID;}
        @Override public ItemStack insertItem(int slot,ItemStack stack,boolean simulate) {
            if(slot!=0||stack.isEmpty()||transferring||removing()||!physicallyPresent())return stack;
            Resolution destination=resolvePeer(true);if(destination.status()!=Status.VALID||!(destination.peer() instanceof ItemMirrorBlockEntity peer))return stack;
            int accepted=Math.min(stack.getCount(),Math.min(64,stack.getMaxStackSize()));
            ItemStack remainder=stack.copy();remainder.shrink(accepted);
            if(simulate)return remainder;
            transferring=true;
            try {ItemStack paid=stack.copy();paid.setCount(accepted);if(!peer.transportDirect(paid))return stack;addInstability(accepted);transferFx();peer.transferFx();return remainder;}
            finally {transferring=false;}
        }
    };
    private LazyOptional<IItemHandler> handler=LazyOptional.of(()->inventory);
    public ItemMirrorBlockEntity(BlockPos pos,BlockState state){super(MirrorModule.ITEM_MIRROR.get(),pos,state);}
    private boolean ownsReservation(){return reservedEntity==null||!MirrorRefundSavedData.accepted(reservedEntity);}
    public int queuedCount(){return output.stream().mapToInt(ItemStack::getCount).sum()+(ownsReservation()?reserved.getCount():0);}
    public List<ItemStack> queuedStacks(){
        List<ItemStack> copies=new ArrayList<>();
        for(ItemStack stack:output){ItemStack copy=stack.copy();if(ownsReservation()&&stack==reservedEntry&&!reserved.isEmpty())copy.grow(reserved.getCount());copies.add(copy);}
        if(ownsReservation()&&!reserved.isEmpty()&&!output.contains(reservedEntry))copies.add(Math.min(reservedIndex,copies.size()),reserved.copy());return List.copyOf(copies);
    }
    public boolean transportDirect(ItemStack stack){
        requireThread();if(stack==null||stack.isEmpty()||stack.getCount()<=0||removing()||!physicallyPresent())return false;
        output.add(stack.copy());setChanged();return true;
    }
    public boolean transport(ItemEntity entity){
        requireThread();if(transferring||removing()||!physicallyPresent()||entity==null||!entity.isAlive()||entity.level()!=level||entity.isOnPortalCooldown()||entity.getItem().isEmpty())return false;
        Resolution destination=resolvePeer(true);if(destination.status()!=Status.VALID||!(destination.peer() instanceof ItemMirrorBlockEntity peer))return false;
        transferring=true;
        try {
            ItemStack stack=entity.getItem();if(!peer.transportDirect(stack))return false;
            int paid=stack.getCount();entity.setItem(ItemStack.EMPTY);addInstability(paid);entity.discard();transferFx();peer.transferFx();return true;
        }finally{transferring=false;}
    }
    @Override protected void tickOutput(){
        if(ejecting||count()<=20||output.isEmpty()||!(level instanceof ServerLevel server))return;
        int index=server.random.nextInt(output.size());ItemStack entry=output.get(index);
        if(entry.isEmpty()){output.remove(index);setChanged();return;}
        // Detach ownership before a Forge join listener can remove or reenter this endpoint.
        reservedEntry=entry;reservedIndex=index;reserved=entry.split(1);if(entry.isEmpty())output.remove(index);setChanged();ejecting=true;
        reservedEntity=MirrorRefundSavedData.output(server,worldPosition,facing(),reserved.copy());
        try {
            if(MirrorRefundSavedData.spawn(server,reservedEntity)) {
                reserved=ItemStack.EMPTY;addInstability(1);if(physicallyPresent())transferFx();
            }else {if(!reserved.isEmpty()){if(output.contains(entry))entry.grow(reserved.getCount());else output.add(Math.min(index,output.size()),reserved);reserved=ItemStack.EMPTY;}}
        }finally {
            reservedEntity=null;reservedEntry=ItemStack.EMPTY;ejecting=false;setChanged();
            if(migrateAfterEjection){migrateAfterEjection=false;migrateOutput(server);}
        }
    }
    @Override protected void onPhysicalRemoval(){
        if(!(level instanceof ServerLevel server))return;
        if(ejecting){migrateAfterEjection=true;return;}migrateOutput(server);
    }
    private void migrateOutput(ServerLevel server){
        for(ItemStack stack:output)MirrorRefundSavedData.get(server).add(worldPosition,facing(),stack);
        output.clear();if(!reserved.isEmpty()){MirrorRefundSavedData.get(server).add(worldPosition,facing(),reserved);reserved=ItemStack.EMPTY;}setChanged();
    }
    @Override protected void saveAdditional(CompoundTag tag){
        super.saveAdditional(tag);ListTag rows=new ListTag();int slot=0;
        // Native acceptance already transferred ownership; include its pending released
        // one-point output cost even when a synchronous save precedes outer spawn return.
        if(!reserved.isEmpty()&&!ownsReservation())tag.putInt("instability",(int)Math.min(Integer.MAX_VALUE,(long)tag.getInt("instability")+1));
        for(ItemStack stack:queuedStacks()){CompoundTag row=stack.save(new CompoundTag());row.putByte("Slot",(byte)slot++);rows.add(row);}tag.put("Items",rows);
    }
    @Override public void load(CompoundTag tag){
        if(ejecting||transferring)throw new IllegalStateException("Cannot reload a mirror during a paid transaction");
        super.load(tag);output.clear();reserved=ItemStack.EMPTY;ListTag rows=tag.getList("Items",Tag.TAG_COMPOUND);
        for(int i=0;i<rows.size();i++){ItemStack stack=ItemStack.of(rows.getCompound(i));if(!stack.isEmpty())output.add(stack);}
    }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> capability,@Nullable Direction side){return capability==ForgeCapabilities.ITEM_HANDLER&&!isRemoved()?handler.cast():super.getCapability(capability,side);}
    @Override public void invalidateCaps(){super.invalidateCaps();handler.invalidate();}
    @Override public void reviveCaps(){super.reviveCaps();handler=LazyOptional.of(()->inventory);}
}
