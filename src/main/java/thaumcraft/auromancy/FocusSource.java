package thaumcraft.auromancy;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.equipment.accessories.AccessoriesSupport;
import thaumcraft.equipment.pouch.FocusPouchItem;

/** A physical source address; names and sorting hashes never identify inventory ownership. */
public record FocusSource(Kind kind,int outer,int inner) {
    public enum Kind { INVENTORY, INVENTORY_POUCH, EQUIPPED_POUCH }
    public static FocusSource inventory(int slot){return new FocusSource(Kind.INVENTORY,slot,-1);}
    public boolean valid(){return kind!=null&&outer>=0&&outer<(kind==Kind.EQUIPPED_POUCH?4096:36)
            &&(kind==Kind.INVENTORY?inner==-1:inner>=0&&inner<18);}
    public ItemStack pouch(Player player){
        if(!valid()||kind==Kind.INVENTORY)return ItemStack.EMPTY;
        if(kind==Kind.INVENTORY_POUCH)return player.getInventory().getItem(outer);
        var worn=AccessoriesSupport.equipped(player);return outer<worn.size()?worn.get(outer):ItemStack.EMPTY;
    }
    public ItemStack focus(Player player){
        if(!valid())return ItemStack.EMPTY;
        if(kind==Kind.INVENTORY)return player.getInventory().getItem(outer);
        var pouch=pouch(player);return FocusPouchItem.editable(pouch)?FocusPouchItem.contents(pouch).get(inner):ItemStack.EMPTY;
    }
}
