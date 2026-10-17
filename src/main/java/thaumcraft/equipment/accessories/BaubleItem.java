package thaumcraft.equipment.accessories;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import thaumcraft.api.items.IVisDiscountGear;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/** Released seven mundane/fancy forms; only the apprentice ring discounts Vis. */
public final class BaubleItem extends Item implements ICurioItem,IVisDiscountGear {
    private final int variant;
    public BaubleItem(int variant) {
        super(new Properties().stacksTo(1).rarity(variant>=3?Rarity.UNCOMMON:Rarity.COMMON));
        if(variant<0||variant>6)throw new IllegalArgumentException("Unknown original bauble form");
        this.variant=variant;
    }
    public int variant(){return variant;}
    @Override public int getVisDiscount(ItemStack stack,Player wearer){return variant==3?5:0;}
}
