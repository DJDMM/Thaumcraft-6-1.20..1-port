package thaumcraft.equipment.accessories;
import net.minecraft.world.item.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public final class LateAccessoryItem extends Item implements ICurioItem {
    public enum Kind { CLOUD, CURIOSITY, UNDYING }
    private final Kind kind;
    public LateAccessoryItem(Kind kind){super(new Properties().stacksTo(1).rarity(Rarity.RARE));this.kind=kind;}
    public Kind kind(){return kind;}
    @Override public void curioTick(SlotContext context,ItemStack stack){
        if(kind==Kind.CLOUD&&context.entity().level().isClientSide&&AccessoriesSupport.isEquipped(context.entity(),stack))
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->thaumcraft.equipment.accessories.client.CloudRingClient.tick(context.entity()));
    }
}
