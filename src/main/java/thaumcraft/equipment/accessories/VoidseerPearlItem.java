package thaumcraft.equipment.accessories;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import thaumcraft.api.items.IVisDiscountGear;
import thaumcraft.api.items.IWarpingGear;
import thaumcraft.research.KnowledgeStore;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/** Released CHARM: only permanent Warp changes its wearer-dependent discount and gear Warp. */
public final class VoidseerPearlItem extends Item implements ICurioItem,IVisDiscountGear,IWarpingGear {
    public VoidseerPearlItem(){super(new Properties().stacksTo(1).rarity(Rarity.RARE));}
    @Override public boolean isEnchantable(ItemStack stack){return false;}
    @Override public boolean isValidRepairItem(ItemStack stack,ItemStack ingredient){return false;}
    @Override public int getVisDiscount(ItemStack stack,Player player){
        int permanent=player instanceof net.minecraft.server.level.ServerPlayer server?KnowledgeStore.get(server).permanentWarp()
            :player!=null&&player.level().isClientSide?ClientValue.permanent(player):0;
        return (int)(Math.min(100,permanent)/100F*25F);
    }
    @Override public int getWarp(ItemStack stack,Player player){return getVisDiscount(stack,player)/5;}
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> tooltip,TooltipFlag flag){
        tooltip.add(Component.translatable("item.voidseer_charm.text").withStyle(ChatFormatting.DARK_BLUE,ChatFormatting.ITALIC));
        super.appendHoverText(stack,level,tooltip,flag);
    }
    private static final class ClientValue {
        private static int permanent(Player player){
            return net.minecraft.client.Minecraft.getInstance().player==player
                ?thaumcraft.client.research.ResearchClient.golemPressKnowledge().permanentWarp():0;
        }
    }
}
