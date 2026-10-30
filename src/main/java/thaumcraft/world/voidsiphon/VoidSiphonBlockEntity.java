package thaumcraft.world.voidsiphon;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.*;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import thaumcraft.Thaumcraft;
import thaumcraft.world.rift.FluxRiftEntity;

/** BETA26 output-only one-slot machine, including its signed-short progress/reload behavior. */
public final class VoidSiphonBlockEntity extends BlockEntity implements WorldlyContainer,MenuProvider {
    private NonNullList<ItemStack> items=NonNullList.withSize(1,ItemStack.EMPTY);
    private int counter,progress;
    private LazyOptional<IItemHandler> handler=LazyOptional.of(this::newHandler);
    public VoidSiphonBlockEntity(BlockPos p,BlockState s){super(VoidSiphonModule.BLOCK_ENTITY.get(),p,s);}
    public int progress(){return progress;}
    public int counter(){return counter;}
    public static boolean loaded(Level level,BlockPos p){return !level.isOutsideBuildHeight(p)&&(level instanceof ServerLevel server?server.getChunkSource().getChunkNow(p.getX()>>4,p.getZ()>>4)!=null:level.hasChunkAt(p));}
    private boolean live(){return level!=null&&!isRemoved()&&loaded(level,worldPosition)&&level.getBlockEntity(worldPosition)==this&&getBlockState().getBlock() instanceof VoidSiphonBlock;}
    private boolean hasSpace(){return items.get(0).isEmpty()||items.get(0).is(seed())&&items.get(0).getCount()<items.get(0).getMaxStackSize();}
    private IItemHandler newHandler(){return new IItemHandler(){
        @Override public int getSlots(){return 1;}
        @Override public ItemStack getStackInSlot(int slot){return slot==0&&live()?getItem(0).copy():ItemStack.EMPTY;}
        @Override public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){return stack;}
        @Override public ItemStack extractItem(int slot,int count,boolean simulate){
            if(slot!=0||count<=0||!live()||!(level instanceof ServerLevel server)||!server.getServer().isSameThread())return ItemStack.EMPTY;
            var stack=getItem(0);int accepted=Math.min(count,stack.getCount());
            return accepted<=0?ItemStack.EMPTY:simulate?stack.copyWithCount(accepted):removeItem(0,accepted);
        }
        @Override public int getSlotLimit(int slot){return slot==0?64:0;}
        @Override public boolean isItemValid(int slot,ItemStack stack){return false;}
    };}
    public static Item seed(){return Thaumcraft.ITEMS.getEntries().stream().filter(e->e.getId().getPath().equals("void_seed")).findFirst().orElseThrow().get();}
    public List<FluxRiftEntity> validRifts(){
        if(!live())return List.of();
        // Original cubic entity query, not an invented spherical-distance filter.
        Vec3 centre=worldPosition.getCenter();
        for(int x=(worldPosition.getX()-10)>>4;x<=(worldPosition.getX()+10)>>4;x++)for(int z=(worldPosition.getZ()-10)>>4;z<=(worldPosition.getZ()+10)>>4;z++)
            if(!loaded(level,new BlockPos(x*16,worldPosition.getY(),z*16)))return List.of();
        var result=new ArrayList<FluxRiftEntity>();
        for(var rift:level.getEntitiesOfClass(FluxRiftEntity.class,new AABB(centre,centre).inflate(8),r->r.isAlive()&&!r.isRemoved()&&r.getRiftSize()>=2)){
            Vec3 start=new Vec3(worldPosition.getX()+.5,worldPosition.getY()+1,worldPosition.getZ()+.5),end=rift.position();
            start=start.add(end.subtract(start).normalize());
            if(level.clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,rift)).getType()==HitResult.Type.MISS)result.add(rift);
        }
        return List.copyOf(result);
    }
    public static void tick(Level l,BlockPos p,BlockState s,VoidSiphonBlockEntity tile){
        if(!tile.live()||l.captureBlockSnapshots||l.restoringBlockSnapshots)return;
        tile.counter++;
        if(l.isClientSide||!(l instanceof ServerLevel server)||!server.getServer().isSameThread()||!s.getValue(VoidSiphonBlock.ENABLED)
            ||tile.counter%20!=0||tile.progress>=2000||!tile.hasSpace())return;
        boolean effect=false,dirty=false;
        for(var rift:tile.validRifts()){
            if(!tile.live()||!tile.getBlockState().getValue(VoidSiphonBlock.ENABLED)||!tile.hasSpace())return;
            double amount=Math.sqrt(rift.getRiftSize());
            tile.progress+=(int)amount;rift.setRiftStability((float)(rift.getRiftStability()-amount/15));
            if(l.random.nextInt(33)==0)rift.setRiftSize(rift.getRiftSize()-1);
            effect=amount>=1;dirty=true;
        }
        if(effect&&tile.counter%40==0)l.blockEvent(p,s.getBlock(),5,tile.counter);
        while(tile.progress>=2000&&tile.hasSpace()){
            tile.progress-=2000;
            if(tile.items.get(0).isEmpty())tile.items.set(0,new ItemStack(seed()));else tile.items.get(0).grow(1);
            dirty=true;
        }
        // Persist intermediate progress too: vanilla dirty-chunk saving cannot infer field changes.
        if(dirty){tile.setChanged();l.sendBlockUpdated(p,s,s,3);}
    }
    @Override public boolean triggerEvent(int id,int value){
        if(id!=5)return super.triggerEvent(id,value);
        if(level!=null&&level.isClientSide)for(var rift:validRifts())
            thaumcraft.world.voidsiphon.client.VoidSiphonStreams.add(rift.position(),new Vec3(worldPosition.getX()+.5,worldPosition.getY()+.5625,worldPosition.getZ()+.5),value,.04F);
        return true;
    }
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);ContainerHelper.saveAllItems(tag,items);tag.putShort("progress",(short)progress);}
    @Override public void load(CompoundTag tag){super.load(tag);items=NonNullList.withSize(1,ItemStack.EMPTY);ContainerHelper.loadAllItems(tag,items);progress=tag.getShort("progress");counter=0;}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public int getContainerSize(){return 1;}
    @Override public boolean isEmpty(){return items.get(0).isEmpty();}
    @Override public ItemStack getItem(int slot){return slot==0?items.get(0):ItemStack.EMPTY;}
    @Override public ItemStack removeItem(int slot,int count){if(slot!=0)return ItemStack.EMPTY;var out=ContainerHelper.removeItem(items,slot,count);if(!out.isEmpty())setChanged();return out;}
    @Override public ItemStack removeItemNoUpdate(int slot){return slot==0?ContainerHelper.takeItem(items,slot):ItemStack.EMPTY;}
    @Override public void setItem(int slot,ItemStack stack){if(slot!=0)return;items.set(slot,stack);if(stack.getCount()>getMaxStackSize())stack.setCount(getMaxStackSize());setChanged();}
    @Override public boolean canPlaceItem(int slot,ItemStack stack){return slot==0&&stack.is(seed());}
    @Override public void clearContent(){items.clear();setChanged();}
    @Override public boolean stillValid(Player player){return live()&&player.level()==level&&Container.stillValidBlockEntity(this,player);}
    @Override public int[] getSlotsForFace(Direction side){return new int[]{0};}
    @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction side){return false;}
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction side){return slot==0;}
    @Override public Component getDisplayName(){return Component.translatable("tile.void_siphon.name");}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inv,Player p){return new VoidSiphonMenu(id,inv,this);}
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,@org.jetbrains.annotations.Nullable Direction side){return cap==ForgeCapabilities.ITEM_HANDLER?handler.cast():super.getCapability(cap,side);}
    @Override public void invalidateCaps(){super.invalidateCaps();handler.invalidate();}
    @Override public void reviveCaps(){super.reviveCaps();handler=LazyOptional.of(this::newHandler);}
}
