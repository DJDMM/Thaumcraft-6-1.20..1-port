package thaumcraft.auromancy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.auromancy.focus.FocusStacks;
import thaumcraft.equipment.accessories.AccessoriesSupport;
import thaumcraft.equipment.pouch.FocusPouchItem;
import net.minecraft.core.NonNullList;
import java.util.ArrayList;
import java.util.List;

/** Physical inventory slots avoid the original sorting-helper collision and never create or drop a focus. */
public final class FocusSelection {
    private FocusSelection() {}
    public static boolean isCaster(ItemStack stack) { return !stack.isEmpty() && stack.getItem() instanceof CasterItem; }
    public static InteractionHand casterHand(Player player) {
        return isCaster(player.getMainHandItem()) ? InteractionHand.MAIN_HAND : isCaster(player.getOffhandItem()) ? InteractionHand.OFF_HAND : null;
    }
    public static ItemStack installed(ItemStack caster) {
        if (!isCaster(caster) || !caster.hasTag() || !caster.getTag().contains("focus", 10)) return ItemStack.EMPTY;
        var tag=caster.getTag().getCompound("focus");
        if(!tag.contains("Count",net.minecraft.nbt.Tag.TAG_BYTE))return ItemStack.EMPTY;
        ItemStack focus = ItemStack.of(tag.copy());
        return FocusStacks.isFocus(focus) && focus.getCount() == 1 ? focus : ItemStack.EMPTY;
    }
    public static void setInstalled(ItemStack caster, ItemStack focus) {
        if (!isCaster(caster)) throw new IllegalArgumentException("Not a caster");
        if (focus.isEmpty()) {
            if (caster.hasTag()) caster.getTag().remove("focus");
        } else {
            if (!FocusStacks.isFocus(focus) || focus.getCount() != 1) throw new IllegalArgumentException("Not one focus");
            caster.getOrCreateTag().put("focus", focus.copy().save(new CompoundTag()));
        }
    }
    public static boolean change(ServerPlayer player, InteractionHand hand, int slot, ItemStack expectedCaster, ItemStack expectedFocus) {
        if(slot < -1 || slot >= 36)return false;
        return change(player,hand,slot==-1?null:FocusSource.inventory(slot),expectedCaster,ItemStack.EMPTY,expectedFocus);
    }
    public record Choice(FocusSource source,ItemStack pouch,ItemStack focus){}
    public static List<Choice> choices(Player player){
        var result=new ArrayList<Choice>();var worn=AccessoriesSupport.equipped(player);
        for(int i=0;i<worn.size();i++)addPouchChoices(result,new FocusSource(FocusSource.Kind.EQUIPPED_POUCH,i,0),worn.get(i));
        for(int i=0;i<36;i++){var stack=player.getInventory().getItem(i);
            if(FocusStacks.readPlan(stack).isPresent())result.add(new Choice(FocusSource.inventory(i),ItemStack.EMPTY,stack.copy()));
            addPouchChoices(result,new FocusSource(FocusSource.Kind.INVENTORY_POUCH,i,0),stack);
        }
        return result;
    }
    private static void addPouchChoices(List<Choice> result,FocusSource base,ItemStack pouch){
        if(!FocusPouchItem.editable(pouch))return;var list=FocusPouchItem.contents(pouch);
        for(int i=0;i<18;i++)if(FocusStacks.readPlan(list.get(i)).isPresent())result.add(new Choice(new FocusSource(base.kind(),base.outer(),i),pouch.copy(),list.get(i).copy()));
    }
    private record Bag(ItemStack live,ItemStack before,NonNullList<ItemStack> contents){}
    private static void bag(List<Bag> bags,ItemStack stack){if(FocusPouchItem.editable(stack)&&bags.stream().noneMatch(b->b.live==stack))bags.add(new Bag(stack,stack.copy(),FocusPouchItem.contents(stack)));}
    public static boolean change(ServerPlayer player,InteractionHand hand,FocusSource source,ItemStack expectedCaster,ItemStack expectedPouch,ItemStack expectedFocus){
        if(player==null||hand==null||!player.isAlive()||player.isSpectator()||!player.getServer().isSameThread()||player.containerMenu!=player.inventoryMenu)return false;
        ItemStack caster=player.getItemInHand(hand);
        if(!isCaster(caster)||!ItemStack.matches(caster,expectedCaster)||caster.hasTag()&&caster.getTag().contains("focus",10)&&installed(caster).isEmpty())return false;
        ItemStack old=installed(caster),chosen=ItemStack.EMPTY,chosenPouch=ItemStack.EMPTY;
        if(source==null){if(old.isEmpty())return false;}
        else{
            if(!source.valid())return false;chosen=source.focus(player);chosenPouch=source.pouch(player);
            if(!ItemStack.matches(chosen,expectedFocus)||chosen.getCount()!=1||FocusStacks.readPlan(chosen).isEmpty())return false;
            if(source.kind()!=FocusSource.Kind.INVENTORY&&!ItemStack.matches(chosenPouch,expectedPouch))return false;
        }
        var bags=new ArrayList<Bag>();for(var stack:AccessoriesSupport.equipped(player))bag(bags,stack);for(var stack:player.getInventory().items)bag(bags,stack);
        var touched=new ArrayList<Bag>();
        Bag selectedBag=null;if(source!=null&&source.kind()!=FocusSource.Kind.INVENTORY){for(var b:bags)if(b.live==chosenPouch)selectedBag=b;if(selectedBag==null)return false;selectedBag.contents.set(source.inner(),ItemStack.EMPTY);touched.add(selectedBag);}
        int returnInventory=-1;boolean returned=old.isEmpty();
        if(!returned)for(var b:bags){for(int i=0;i<18;i++)if(b.contents.get(i).isEmpty()){b.contents.set(i,old.copy());if(!touched.contains(b))touched.add(b);returned=true;break;}if(returned)break;}
        if(!returned){returnInventory=source!=null&&source.kind()==FocusSource.Kind.INVENTORY?source.outer():player.getInventory().getFreeSlot();if(returnInventory<0)return false;}
        // Resolve capability-backed sources before committing: detached/replaced bags may never receive a write.
        if(player.getItemInHand(hand)!=caster||!ItemStack.matches(caster,expectedCaster))return false;
        for(var b:bags)if(!ItemStack.matches(b.live,b.before)||!owned(player,b.live))return false;
        if(source!=null&&(source.kind()==FocusSource.Kind.INVENTORY?!ItemStack.matches(player.getInventory().getItem(source.outer()),expectedFocus):source.pouch(player)!=chosenPouch))return false;
        if(source!=null&&source.kind()==FocusSource.Kind.INVENTORY)player.getInventory().setItem(source.outer(),ItemStack.EMPTY);
        for(var b:touched)FocusPouchItem.store(b.live,b.contents);
        if(returnInventory>=0)player.getInventory().setItem(returnInventory,old.copy());
        setInstalled(caster,chosen);
        player.getInventory().setChanged();player.inventoryMenu.broadcastChanges();
        player.serverLevel().playSound(null,player.blockPosition(),AuromancySounds.TICKS.get(),net.minecraft.sounds.SoundSource.PLAYERS,.3F,source==null?.9F:1F);
        return true;
    }
    private static boolean owned(Player player,ItemStack bag){return player.getInventory().items.stream().anyMatch(s->s==bag)||AccessoriesSupport.isEquipped(player,bag);}
}
