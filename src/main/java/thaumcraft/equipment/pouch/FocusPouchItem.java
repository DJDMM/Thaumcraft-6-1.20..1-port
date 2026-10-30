package thaumcraft.equipment.pouch;

import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import thaumcraft.auromancy.focus.FocusStacks;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/** Released eighteen-focus bag; the requesting hand owns its live storage. */
public final class FocusPouchItem extends Item implements ICurioItem {
    public static final int SIZE=18;
    public FocusPouchItem(Properties properties){super(properties.stacksTo(1).rarity(Rarity.UNCOMMON));}
    @Override public boolean isFoil(ItemStack stack){return false;}
    public static NonNullList<ItemStack> contents(ItemStack stack){
        var items=NonNullList.withSize(SIZE,ItemStack.EMPTY);
        if(stack.hasTag())ContainerHelper.loadAllItems(stack.getTag(),items);
        return items;
    }
    public static boolean validFocus(ItemStack stack){return stack.isEmpty()||stack.getCount()==1&&FocusStacks.isFocus(stack);}
    public static boolean editable(ItemStack stack){
        if(!(stack.getItem() instanceof FocusPouchItem)||stack.getCount()!=1)return false;
        if(!stack.hasTag()||!stack.getTag().contains("Items"))return true;
        if(!stack.getTag().contains("Items",net.minecraft.nbt.Tag.TAG_LIST))return false;
        var raw=stack.getTag().get("Items");if(!(raw instanceof net.minecraft.nbt.ListTag list)||list.size()>SIZE||!list.isEmpty()&&list.getElementType()!=net.minecraft.nbt.Tag.TAG_COMPOUND)return false;
        boolean[] seen=new boolean[SIZE];for(int i=0;i<list.size();i++){
            var entry=list.getCompound(i);if(!entry.contains("Slot",net.minecraft.nbt.Tag.TAG_BYTE)||!entry.contains("Count",net.minecraft.nbt.Tag.TAG_BYTE))return false;
            int slot=entry.getByte("Slot")&255;if(slot>=SIZE||seen[slot])return false;seen[slot]=true;
            var focus=ItemStack.of(entry);if(focus.isEmpty()||!validFocus(focus))return false;
        }
        return true;
    }
    public static void store(ItemStack pouch,NonNullList<ItemStack> items){
        if(!(pouch.getItem() instanceof FocusPouchItem)||items.size()!=SIZE||!items.stream().allMatch(FocusPouchItem::validFocus))throw new IllegalArgumentException("Invalid focus pouch storage");
        ContainerHelper.saveAllItems(pouch.getOrCreateTag(),items);
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        ItemStack pouch=player.getItemInHand(hand);
        if(!editable(pouch))return InteractionResultHolder.fail(pouch);
        if(player instanceof ServerPlayer server)NetworkHooks.openScreen(server,
                new SimpleMenuProvider((id,inventory,p)->new FocusPouchMenu(id,inventory,hand),Component.translatable("item.thaumcraft.focus_pouch")),
                data->data.writeEnum(hand));
        return InteractionResultHolder.pass(pouch);
    }
}
