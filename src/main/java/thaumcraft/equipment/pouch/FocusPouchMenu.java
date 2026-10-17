package thaumcraft.equipment.pouch;

import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Live physical bag storage: detached or replaced hands never receive a stale close write. */
public final class FocusPouchMenu extends AbstractContainerMenu {
    private final Inventory inventory;
    private final InteractionHand hand;
    private final int selected;
    private final ItemStack pouch;
    private ItemStack expected;
    private boolean loading=true,closed;
    public final SimpleContainer input=new SimpleContainer(FocusPouchItem.SIZE){
        @Override public int getMaxStackSize(){return 1;}
        @Override public boolean canPlaceItem(int slot,ItemStack stack){return !stack.isEmpty()&&FocusPouchItem.validFocus(stack);}
        @Override public void setChanged(){super.setChanged();persist();}
    };
    public FocusPouchMenu(int id,Inventory inventory,FriendlyByteBuf data){this(id,inventory,data.readEnum(InteractionHand.class));}
    public FocusPouchMenu(int id,Inventory inventory,InteractionHand hand){
        super(FocusPouchModule.MENU.get(),id);this.inventory=inventory;this.hand=hand;selected=inventory.selected;pouch=inventory.player.getItemInHand(hand);expected=pouch.copy();
        for(int i=0;i<18;i++)addSlot(new Slot(input,i,37+i%6*18,51+i/6*18){@Override public boolean mayPlace(ItemStack stack){return !stack.isEmpty()&&FocusPouchItem.validFocus(stack);}@Override public int getMaxStackSize(){return 1;}});
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,9+row*9+col,8+col*18,151+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,8+col*18,209));
        if(!inventory.player.level().isClientSide){var saved=FocusPouchItem.contents(pouch);for(int i=0;i<18;i++)input.setItem(i,saved.get(i));}
        loading=false;
    }
    public InteractionHand hand(){return hand;}
    public int lockedHotbar(){return hand==InteractionHand.MAIN_HAND?selected:-1;}
    @Override public boolean stillValid(Player player){return !closed&&player==inventory.player&&player.isAlive()
            &&(hand==InteractionHand.OFF_HAND||inventory.selected==selected)
            &&(player.level().isClientSide?player.getItemInHand(hand).getItem() instanceof FocusPouchItem:
            player.getItemInHand(hand)==pouch&&FocusPouchItem.editable(pouch)&&ItemStack.matches(pouch,expected));}
    private boolean context(Player player){return stillValid(player)&&(!(player instanceof ServerPlayer server)||server.containerMenu==this);}
    private void persist(){
        if(loading||closed||inventory==null||inventory.player.level().isClientSide||!stillValid(inventory.player))return;
        var list=NonNullList.withSize(18,ItemStack.EMPTY);for(int i=0;i<18;i++)list.set(i,input.getItem(i).copy());
        FocusPouchItem.store(pouch,list);expected=pouch.copy();inventory.setChanged();
    }
    @Override public void removed(Player player){if(closed)return;persist();closed=true;super.removed(player);}
    @Override public void clicked(int slot,int button,ClickType type,Player player){
        if(!context(player)||type==ClickType.SWAP)return;
        if(hand==InteractionHand.MAIN_HAND&&slot==45+selected||slot>=0&&slot<slots.size()&&slots.get(slot).getItem()==pouch)return;
        if(getCarried()==pouch)return;
        super.clicked(slot,button,type,player);
    }
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(!context(player)||index<0||index>=slots.size())return ItemStack.EMPTY;
        Slot slot=slots.get(index);if(hand==InteractionHand.MAIN_HAND&&index==45+selected||!slot.hasItem()||slot.getItem()==pouch)return ItemStack.EMPTY;
        ItemStack source=slot.getItem(),before=source.copy();
        if(index<18){if(!moveItemStackTo(source,18,slots.size(),true))return ItemStack.EMPTY;}
        else if(!FocusPouchItem.validFocus(source)||!moveItemStackTo(source,0,18,false))return ItemStack.EMPTY;
        if(source.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,source);persist();return before;
    }
}
