package thaumcraft.equipment.tools;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

/** BETA26 elemental pick: fire on left click; SOUNDING is the original infusion-enchantment action. */
public final class ElementalPickaxeItem extends ToolItems.Pick {
    public ElementalPickaxeItem() { super(ToolMaterials.ELEMENTAL, 0); }
    @Override public Rarity getRarity(ItemStack stack) { return Rarity.RARE; }
    @Override public boolean onLeftClickEntity(ItemStack stack, Player player, Entity target) {
        if (!player.level().isClientSide && ToolSupport.canHarm(player, target)) target.setSecondsOnFire(2);
        return false;
    }
}
