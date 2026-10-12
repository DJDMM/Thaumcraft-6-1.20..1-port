package thaumcraft.research;

import javax.annotation.Nullable;
import java.util.List;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Original f_TELEPORT specimen roster; no travel/dimension achievement substitute. */
public final class MirrorProgressionScans {
    private MirrorProgressionScans(){}
    public static List<String> scanFacts(@Nullable Object scanned){
        if(scanned instanceof ItemEntity item)scanned=item.getItem();
        boolean matches=scanned instanceof EnderMan
                ||scanned instanceof ItemStack stack&&!stack.isEmpty()&&stack.is(Items.ENDER_PEARL)
                ||scanned instanceof BlockState state&&(state.is(Blocks.NETHER_PORTAL)||state.is(Blocks.END_PORTAL)||state.is(Blocks.END_PORTAL_FRAME));
        return matches?List.of("f_TELEPORT"):List.of();
    }
}
