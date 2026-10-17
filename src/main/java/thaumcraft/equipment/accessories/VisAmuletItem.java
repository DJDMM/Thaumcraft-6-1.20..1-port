package thaumcraft.equipment.accessories;

import java.util.*;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import thaumcraft.equipment.RechargeSupport;
import thaumcraft.research.KnowledgeStore;
import thaumcraft.world.aura.AuraManager;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/** Original worn-only ambient recharge: first successful hotbar, accessory or armor item, one unit. */
public final class VisAmuletItem extends Item implements ICurioItem {
    private static final Set<Player> RECHARGING=Collections.newSetFromMap(new IdentityHashMap<>());
    private final boolean crafted;
    public VisAmuletItem(boolean crafted){super(new Properties().stacksTo(1).rarity(crafted?Rarity.RARE:Rarity.UNCOMMON));this.crafted=crafted;}
    public boolean crafted(){return crafted;}
    public int interval(){return crafted?5:40;}
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> tooltip,TooltipFlag flag){tooltip.add(Component.translatable("item.amulet_vis.text").withStyle(ChatFormatting.AQUA));super.appendHoverText(stack,level,tooltip,flag);}
    @Override public void curioTick(SlotContext context,ItemStack amulet){
        if(context.cosmetic()||!(context.entity() instanceof Player player)||!(player.level() instanceof ServerLevel server)||player.tickCount%interval()!=0||!RECHARGING.add(player))return;
        try{
            var source=amulet.copy();BlockPos origin=player.blockPosition();
            if(!validSource(server,player,origin,amulet,source))return;
            for(int i=0;i<9;i++){final int slot=i;if(recharge(server,player,origin,amulet,source,()->player.getInventory().getItem(slot)))return;if(!validSource(server,player,origin,amulet,source))return;}
            var equipped=AccessoriesSupport.equipped(player);
            for(int i=0;i<equipped.size();i++){final int slot=i;if(recharge(server,player,origin,amulet,source,()->{var live=AccessoriesSupport.equipped(player);return slot<live.size()?live.get(slot):ItemStack.EMPTY;}))return;if(!validSource(server,player,origin,amulet,source))return;}
            for(int i=0;i<4;i++){final int slot=i;if(recharge(server,player,origin,amulet,source,()->player.getInventory().armor.get(slot)))return;if(!validSource(server,player,origin,amulet,source))return;}
        }finally{RECHARGING.remove(player);}
    }
    private static boolean validSource(ServerLevel server,Player player,BlockPos origin,ItemStack amulet,ItemStack before){
        return player.level()==server&&!player.isRemoved()&&player.isAlive()&&!player.isSpectator()&&player.blockPosition().equals(origin)
                &&server.getChunkSource().getChunkNow(origin.getX()>>4,origin.getZ()>>4)!=null&&!server.captureBlockSnapshots&&!server.restoringBlockSnapshots
                &&AccessoriesSupport.isEquipped(player,amulet)&&ItemStack.matches(amulet,before);
    }
    private static boolean recharge(ServerLevel server,Player player,BlockPos origin,ItemStack amulet,ItemStack source,Supplier<ItemStack> slot){
        ItemStack physical=slot.get();int charge=RechargeSupport.getCharge(physical);if(charge<0)return false;
        var before=physical.copy();int capacity;
        // Foreign max-charge implementations can reenter, move a stack or replace the worn source.
        // Observe them before payment, then validate the actual physical owners again.
        try{capacity=RechargeSupport.maxCharge(physical,player);}catch(RuntimeException brokenGear){return false;}
        if(charge>=capacity||!validSource(server,player,origin,amulet,source)||slot.get()!=physical||!ItemStack.matches(physical,before))return false;
        float available=AuraManager.getVis(server,origin);
        if(player instanceof net.minecraft.server.level.ServerPlayer wearer&&KnowledgeStore.get(wearer).isResearchCompleteStrict("AURAPRESERVE")
                &&available/AuraManager.getAuraBase(server,origin)<.1F)return false;
        if(!validSource(server,player,origin,amulet,source)||slot.get()!=physical||!ItemStack.matches(physical,before))return false;
        // Original RechargeHelper drains up to one float Vis and then truncates the credit.
        // No external gear callback runs between this native payment and its direct tc.charge write.
        int paid=(int)AuraManager.drainVis(server,origin,1,false);
        if(paid<=0)return false;
        physical.getOrCreateTag().putInt(RechargeSupport.CHARGE_TAG,charge+paid);player.getInventory().setChanged();return true;
    }
}
