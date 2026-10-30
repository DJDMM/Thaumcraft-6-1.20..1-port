package thaumcraft.artifice.late;
import javax.annotation.Nullable;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.*;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
/** One real inventory; sided capabilities never shadow or duplicate its contents. */
public abstract class LateInventoryBlockEntity extends BlockEntity implements WorldlyContainer,MenuProvider {
    protected final NonNullList<ItemStack> items=NonNullList.withSize(1,ItemStack.EMPTY);
    private LazyOptional<IItemHandlerModifiable>[] itemCaps=SidedInvWrapper.create(this,Direction.values());
    protected LateInventoryBlockEntity(BlockEntityType<?> type,BlockPos p,BlockState s){super(type,p,s);}
    public void changed(){setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    @Override public int getContainerSize(){return 1;}
    @Override public boolean isEmpty(){return items.get(0).isEmpty();}
    @Override public ItemStack getItem(int slot){return slot==0?items.get(0):ItemStack.EMPTY;}
    @Override public ItemStack removeItem(int slot,int amount){var out=ContainerHelper.removeItem(items,slot,amount);if(!out.isEmpty())inventoryChanged();return out;}
    @Override public ItemStack removeItemNoUpdate(int slot){var out=ContainerHelper.takeItem(items,slot);if(!out.isEmpty())inventoryChanged();return out;}
    @Override public void setItem(int slot,ItemStack stack){if(slot!=0)return;items.set(0,stack);if(stack.getCount()>getMaxStackSize())stack.setCount(getMaxStackSize());inventoryChanged();}
    protected void inventoryChanged(){changed();}
    @Override public void clearContent(){items.clear();inventoryChanged();}
    @Override public boolean stillValid(Player p){return !isRemoved()&&p.isAlive()&&!p.isSpectator()&&p.level()==level&&Container.stillValidBlockEntity(this,p);}
    @Override public int[] getSlotsForFace(Direction face){return new int[]{0};}
    @Override public boolean canPlaceItemThroughFace(int slot,ItemStack s,@Nullable Direction face){return canPlaceItem(slot,s);}
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack s,Direction face){return true;}
    @Override public Component getDisplayName(){return getBlockState().getBlock().getName();}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);ContainerHelper.saveAllItems(tag,items);}
    @Override public void load(CompoundTag tag){super.load(tag);items.clear();ContainerHelper.loadAllItems(tag,items);}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,@Nullable Direction side){return cap==ForgeCapabilities.ITEM_HANDLER&&side!=null?itemCaps[side.get3DDataValue()].cast():super.getCapability(cap,side);}
    @Override public void invalidateCaps(){super.invalidateCaps();for(var cap:itemCaps)cap.invalidate();}
    @Override public void reviveCaps(){super.reviveCaps();itemCaps=SidedInvWrapper.create(this,Direction.values());}
}
