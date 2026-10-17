package thaumcraft.equipment.armor;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.items.IGoggles;
import thaumcraft.api.items.IRevealer;

/** Original hand/armor/accessory search; Goggles and Revealer keep distinct offhand rules. */
public final class GogglesArmorSupport {
    private GogglesArmorSupport() {}

    public static boolean hasGoggles(Player player) {
        if (player == null) return false;
        if (popups(player.getMainHandItem(), player)) return true;
        for (ItemStack stack : player.getArmorSlots()) if (popups(stack, player)) return true;
        for (ItemStack stack : thaumcraft.equipment.accessories.AccessoriesSupport.equipped(player)) if (popups(stack,player)) return true;
        // EntityUtils.hasGoggles deliberately does not inspect the offhand.
        return false;
    }

    private static boolean popups(ItemStack stack, Player player) {
        return !stack.isEmpty() && stack.getItem() instanceof IGoggles goggles && goggles.showIngamePopups(stack, player);
    }

    public static boolean hasRevealer(Player player) {
        if (player == null) return false;
        if (reveals(player.getMainHandItem(), player) || reveals(player.getOffhandItem(), player)) return true;
        for (ItemStack stack : player.getArmorSlots()) if (reveals(stack, player)) return true;
        for (ItemStack stack : thaumcraft.equipment.accessories.AccessoriesSupport.equipped(player)) if (reveals(stack,player)) return true;
        return false;
    }

    private static boolean reveals(ItemStack stack, Player player) {
        return !stack.isEmpty() && stack.getItem() instanceof IRevealer revealer && revealer.showNodes(stack, player);
    }
}
