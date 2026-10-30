package thaumcraft.world.mobs;
import java.util.*;
import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** Original five-slot trade; native button0, server rolls, four output-only slots and ground return on close. */
public final class PechMenu extends AbstractContainerMenu{
    private final Player player;
    @Nullable private final PechEntity pech;
    private final SimpleContainer inventory=new SimpleContainer(5);
    private final ContainerData data=new SimpleContainerData(1);
    private boolean accepted,removed;
    public PechMenu(int id,Inventory player,FriendlyByteBuf extra){this(id,player,player.player.level().getEntity(extra.readInt()) instanceof PechEntity p?p:null);}
    public PechMenu(int id,Inventory player,@Nullable PechEntity pech){
        super(MobsModule.PECH_MENU.get(),id);this.player=player.player;this.pech=pech;
        accepted=this.player.level().isClientSide||pech!=null&&pech.beginTrade(this.player);
        addSlot(new Slot(inventory,0,36,29));
        for(int row=0;row<2;row++)for(int col=0;col<2;col++)addSlot(new Slot(inventory,1+col+row*2,106+col*18,20+row*18){@Override public boolean mayPlace(ItemStack stack){return false;}});
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(player,col+row*9+9,8+col*18,84+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(player,col,8+col*18,142));
        addDataSlots(data);
    }
    public int offerValue(){return data.get(0);}
    public SimpleContainer tradeInventory(){return inventory;}
    public boolean canTrade(){
        if(inventory.getItem(0).isEmpty()||offerValue()<=0)return false;
        for(int slot=1;slot<5;slot++)if(!inventory.getItem(slot).isEmpty())return false;return true;
    }
    @Override public void broadcastChanges(){if(!player.level().isClientSide)data.set(0,PechEntity.isValued(inventory.getItem(0))?PechEntity.value(inventory.getItem(0)):0);super.broadcastChanges();}
    @Override public boolean stillValid(Player user){return accepted&&!removed&&pech!=null&&pech.isAlive()&&!pech.isRemoved()&&pech.tamed()&&pech.level()==user.level()&&pech.distanceToSqr(user)<=64&&pech.isTrader(user);}
    @Override public boolean clickMenuButton(Player user,int button){
        if(button!=0||user!=player||user.level().isClientSide||!stillValid(user))return false;
        if(inventory.getItem(0).isEmpty()||!PechEntity.isValued(inventory.getItem(0)))return false;
        for(int slot=1;slot<5;slot++)if(!inventory.getItem(slot).isEmpty())return false;
        int value=PechEntity.value(inventory.getItem(0));var random=user.level().random;
        if(random.nextInt(100)<=value/2){pech.setTamed(false);pech.sound("pech_trade");}
        if(random.nextInt(5)==0)value+=random.nextInt(3);else if(random.nextBoolean())value-=random.nextInt(3);
        while(value>0){
            int amount=Math.min(5,Math.max((value+1)/2,random.nextInt(value)+1));value-=amount;
            var pack=new ArrayList<Integer>();for(int i=0;i<pech.loot.size();i++)if(!pech.loot.get(i).isEmpty())pack.add(i);
            if(amount==1&&random.nextBoolean()&&!pack.isEmpty()){
                int slot=pack.get(random.nextInt(pack.size()));ItemStack chosen=pech.loot.get(slot).copyWithCount(1);addStack(chosen);pech.loot.get(slot).shrink(1);
                if(pech.loot.get(slot).isEmpty())pech.loot.set(slot,ItemStack.EMPTY);
            }else{
                if(amount>=4&&random.nextBoolean())continue;
                var choices=PechTrades.choices(pech.variant(),amount);if(choices.isEmpty())throw new IllegalStateException("Original Pech trade tier has no entries");
                ItemStack chosen=choices.get(random.nextInt(choices.size()));chosen.onCraftedBy(user.level(),user,0);addStack(chosen);
            }
        }
        inventory.removeItem(0,1);broadcastChanges();return true;
    }
    private void addStack(ItemStack stack){
        for(int slot=1;slot<5;slot++){
            if(inventory.getItem(slot).isEmpty()){inventory.setItem(slot,stack);break;}
            ItemStack current=inventory.getItem(slot);
            // Original compares item/metadata, ignores NBT, uses strict<max and continues after a merge.
            if(current.is(stack.getItem())&&current.getDamageValue()==stack.getDamageValue()&&current.getCount()+stack.getCount()<current.getMaxStackSize())
                current.grow(stack.getCount());
        }
    }
    @Override public ItemStack quickMoveStack(Player user,int index){
        if(index<0||index>=slots.size())return ItemStack.EMPTY;Slot slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;
        ItemStack stack=slot.getItem(),before=stack.copy();
        if(index<5){if(!moveItemStackTo(stack,5,41,true))return ItemStack.EMPTY;}
        else if(!moveItemStackTo(stack,0,1,true))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();
        if(stack.getCount()==before.getCount())return ItemStack.EMPTY;slot.onTake(user,stack);return before;
    }
    @Override public void removed(Player user){
        if(removed)return;removed=true;super.removed(user);if(pech!=null)pech.endTrade(user);
        if(!user.level().isClientSide)for(int slot=0;slot<5;slot++){
            ItemStack stack=inventory.removeItemNoUpdate(slot);if(stack.isEmpty())continue;
            ItemEntity drop=user.drop(stack,false);if(drop!=null)drop.getPersistentData().putBoolean("PechDrop",true);
        }
    }
}
