package thaumcraft.world.voidsiphon;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
public final class VoidSiphonMenu extends AbstractContainerMenu {
    private final VoidSiphonBlockEntity tile;
    private int displayedProgress;
    public VoidSiphonMenu(int id,Inventory inv,FriendlyByteBuf data){this(id,inv,(VoidSiphonBlockEntity)inv.player.level().getBlockEntity(data.readBlockPos()));}
    public VoidSiphonMenu(int id,Inventory inv,VoidSiphonBlockEntity tile){super(VoidSiphonModule.MENU.get(),id);this.tile=tile;
        addSlot(new Slot(tile,0,80,32){@Override public boolean mayPlace(ItemStack s){return false;}});
        for(int row=0;row<3;row++)for(int x=0;x<9;x++)addSlot(new Slot(inv,x+row*9+9,8+x*18,84+row*18));
        for(int x=0;x<9;x++)addSlot(new Slot(inv,x,8+x*18,142));
        addDataSlot(new DataSlot(){@Override public int get(){return tile.getLevel()!=null&&tile.getLevel().isClientSide?displayedProgress:tile.progress();}@Override public void set(int value){displayedProgress=value;}});
    }
    public int progress(){return tile.getLevel()!=null&&tile.getLevel().isClientSide?displayedProgress:tile.progress();}
    @Override public boolean stillValid(Player player){return tile.stillValid(player);}
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(index!=0||!stillValid(player))return ItemStack.EMPTY;var slot=slots.get(0);var live=slot.getItem();
        if(live.isEmpty()||!live.is(VoidSiphonBlockEntity.seed()))return ItemStack.EMPTY;var before=live.copy();
        if(!moveItemStackTo(live,1,slots.size(),true))return ItemStack.EMPTY;
        if(live.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();return before;
    }
}
