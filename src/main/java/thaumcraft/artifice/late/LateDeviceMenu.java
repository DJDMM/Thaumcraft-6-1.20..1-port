package thaumcraft.artifice.late;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
/** Exact released slot coordinates and vanilla server-owned mix button. */
public final class LateDeviceMenu extends AbstractContainerMenu {
    private final LateInventoryBlockEntity tile;
    public LateDeviceMenu(int id,Inventory inv,FriendlyByteBuf data){this(id,inv,(LateInventoryBlockEntity)inv.player.level().getBlockEntity(data.readBlockPos()));}
    public LateDeviceMenu(int id,Inventory inv,LateInventoryBlockEntity tile){super(tile instanceof SpaBlockEntity?LateDevicesModule.SPA_MENU.get():LateDevicesModule.SPRAYER_MENU.get(),id);this.tile=tile;boolean spa=tile instanceof SpaBlockEntity;
        addSlot(new Slot(tile,0,spa?65:56,spa?31:64){@Override public boolean mayPlace(ItemStack s){return tile.canPlaceItem(0,s);}});
        for(int y=0;y<3;y++)for(int x=0;x<9;x++)addSlot(new Slot(inv,9+y*9+x,(spa?8:16)+18*x,(spa?84:151)+18*y));for(int x=0;x<9;x++)addSlot(new Slot(inv,x,(spa?8:16)+18*x,spa?142:209));
    }
    public LateInventoryBlockEntity tile(){return tile;}
    @Override public boolean stillValid(Player p){return tile.stillValid(p);}
    @Override public boolean clickMenuButton(Player p,int id){if(id==1&&tile instanceof SpaBlockEntity spa&&stillValid(p)){spa.toggleMix();return false;}return false;}
    @Override public ItemStack quickMoveStack(Player p,int index){if(!stillValid(p)||index<0||index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);var live=slot.getItem();if(live.isEmpty()||!tile.canPlaceItem(0,live))return ItemStack.EMPTY;var before=live.copy();if(index==0?!moveItemStackTo(live,1,slots.size(),true):!moveItemStackTo(live,0,1,false))return ItemStack.EMPTY;if(live.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();slot.onTake(p,live);return before;}
}
