package thaumcraft.artifice.mirror;

import java.util.Objects;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** One input with a detached reservation; failed input and close each have exactly one owner. */
public final class HandMirrorMenu extends AbstractContainerMenu {
    private final Inventory inventory;
    private final InteractionHand hand;
    private final int selected;
    private final ItemStack held;
    private final MirrorLink bound;
    private boolean submitting,closed;
    private ItemStack reservation=ItemStack.EMPTY;
    public final SimpleContainer input=new SimpleContainer(1){@Override public void setChanged(){super.setChanged();submit();}};
    public HandMirrorMenu(int id,Inventory inventory,FriendlyByteBuf data){this(id,inventory,data.readEnum(InteractionHand.class));}
    public HandMirrorMenu(int id,Inventory inventory,InteractionHand hand){
        super(MirrorModule.HAND_MIRROR_MENU.get(),id);this.inventory=inventory;this.hand=hand;selected=inventory.selected;held=inventory.player.getItemInHand(hand);bound=MirrorLink.read(held);
        addSlot(new Slot(input,0,80,24){@Override public boolean mayPlace(ItemStack stack){return !(stack.getItem() instanceof HandMirrorItem);}});
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,9+row*9+col,8+col*18,84+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,8+col*18,142));
    }
    public InteractionHand hand(){return hand;}
    @Override public boolean stillValid(Player player){
        return !closed&&player==inventory.player&&player.isAlive()&&held.getItem() instanceof HandMirrorItem&&player.getItemInHand(hand)==held
                &&(hand==InteractionHand.OFF_HAND||inventory.selected==selected)&&bound!=null&&Objects.equals(bound,MirrorLink.read(held));
    }
    private boolean context(Player player){return stillValid(player)&&(!(player instanceof ServerPlayer server)||server.containerMenu==this);}
    private void submit(){
        if(submitting||closed||inventory==null||inventory.player.level().isClientSide||input.getItem(0).isEmpty())return;
        Player player=inventory.player;if(!context(player)||input.getItem(0).getItem() instanceof HandMirrorItem)return;
        submitting=true;reservation=input.removeItemNoUpdate(0);
        try{
            HandMirrorItem.Target target=HandMirrorItem.target((ServerLevel)player.level(),bound);
            if(target.status()==AbstractMirrorBlockEntity.Status.VALID&&stillValid(player)&&target.mirror().transportDirect(reservation)){
                reservation=ItemStack.EMPTY;target.mirror().transferFx();MirrorModule.playTeleport(player);broadcastChanges();
            }else if(target.status()==AbstractMirrorBlockEntity.Status.INVALID&&stillValid(player))HandMirrorItem.invalidate(held,player);
        }finally{
            if(!reservation.isEmpty()){
                ItemStack unpaid=reservation;reservation=ItemStack.EMPTY;
                if(closed)refund(unpaid);else input.setItem(0,unpaid);
            }
            submitting=false;
        }
    }
    private void refund(ItemStack stack){
        Player player=inventory.player;if(player.level().isClientSide||stack.isEmpty())return;
        player.getInventory().add(stack);
        if(!stack.isEmpty()&&player.level() instanceof ServerLevel server)MirrorRefundSavedData.get(server).add(player.blockPosition(),Direction.UP,stack);
    }
    @Override public void removed(Player player){
        if(closed)return;closed=true;
        ItemStack unpaid=input.removeItemNoUpdate(0);refund(unpaid);
        super.removed(player);
    }
    @Override public void clicked(int slot,int button,ClickType type,Player player){
        if(!context(player)||type==ClickType.SWAP)return;
        if(slot>=0&&slot<slots.size()&&slots.get(slot).getItem().getItem() instanceof HandMirrorItem)return;
        if(getCarried().getItem() instanceof HandMirrorItem)return;
        super.clicked(slot,button,type,player);
    }
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(!context(player)||index<0||index>=slots.size())return ItemStack.EMPTY;
        Slot slot=slots.get(index);if(!slot.hasItem()||slot.getItem().getItem() instanceof HandMirrorItem)return ItemStack.EMPTY;
        ItemStack source=slot.getItem(),before=source.copy();
        if(index==0){if(!moveItemStackTo(source,1,slots.size(),true))return ItemStack.EMPTY;}
        else if(!moveItemStackTo(source,0,1,false))return ItemStack.EMPTY;
        if(source.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,source);return before;
    }
}
