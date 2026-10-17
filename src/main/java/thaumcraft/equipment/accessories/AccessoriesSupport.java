package thaumcraft.equipment.accessories;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

/** Actual non-cosmetic Curios stacks, traversed in the released Baubles slot order. */
public final class AccessoriesSupport {
    private static final String[] ORIGINAL={"necklace","ring","ring","belt","head","body","charm"};
    private static final int[] INDEX={0,0,1,0,0,0,0};
    private AccessoriesSupport() {}

    /** The seven original indices remain stable even when a slot is unavailable. */
    public static List<ItemStack> equipped(LivingEntity owner) {
        List<ItemStack> result=new ArrayList<>();
        var inventory=owner==null?null:CuriosApi.getCuriosInventory(owner).resolve().orElse(null);
        var groups=inventory==null?java.util.Map.<String,ICurioStacksHandler>of():inventory.getCurios();
        for(int i=0;i<ORIGINAL.length;i++) {
            var group=groups.get(ORIGINAL[i]);
            result.add(group!=null&&INDEX[i]<group.getStacks().getSlots()?group.getStacks().getStackInSlot(INDEX[i]):ItemStack.EMPTY);
        }
        // Other mods may expand the same slots or register more types. Never count cosmetics.
        groups.entrySet().stream().sorted(java.util.Map.Entry.comparingByKey()).forEach(entry->{
            int original=switch(entry.getKey()){case "ring"->2;case "necklace","belt","head","body","charm"->1;default->0;};
            var stacks=entry.getValue().getStacks();
            for(int i=original;i<stacks.getSlots();i++)result.add(stacks.getStackInSlot(i));
        });
        return List.copyOf(result);
    }

    public static boolean isEquipped(LivingEntity owner,ItemStack stack) {
        return stack!=null&&!stack.isEmpty()&&equipped(owner).stream().anyMatch(current->current==stack);
    }
}
