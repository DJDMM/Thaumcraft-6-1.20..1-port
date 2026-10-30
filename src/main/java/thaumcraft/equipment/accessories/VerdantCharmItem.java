package thaumcraft.equipment.accessories;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import thaumcraft.api.items.IRechargable;
import thaumcraft.equipment.RechargeSupport;
import thaumcraft.infusion.InfusionEffects;

/** One original NBT-typed charm, one successful operation per 20 wearer ticks. */
public final class VerdantCharmItem extends Item implements ICurioItem,IRechargable {
    public VerdantCharmItem(){super(new Properties().stacksTo(1).rarity(Rarity.RARE));}
    public static int type(ItemStack stack){return stack.hasTag()?stack.getTag().getByte("type"):0;}
    @Override public void curioTick(SlotContext context,ItemStack stack){
        var wearer=context.entity();
        if(!(wearer instanceof Player player)||wearer.level().isClientSide||wearer.tickCount%20!=0
                ||!wearer.isAlive()||!AccessoriesSupport.isEquipped(wearer,stack))return;
        if(wearer.hasEffect(MobEffects.WITHER)&&RechargeSupport.consumeCharge(stack,wearer,20)){wearer.removeEffect(MobEffects.WITHER);return;}
        if(wearer.hasEffect(MobEffects.POISON)&&RechargeSupport.consumeCharge(stack,wearer,10)){wearer.removeEffect(MobEffects.POISON);return;}
        if(wearer.hasEffect(InfusionEffects.FLUX_TAINT.get())&&RechargeSupport.consumeCharge(stack,wearer,5)){wearer.removeEffect(InfusionEffects.FLUX_TAINT.get());return;}
        if(type(stack)==1&&wearer.getHealth()<wearer.getMaxHealth()&&RechargeSupport.consumeCharge(stack,wearer,5)){wearer.heal(1);return;}
        if(type(stack)==2){
            if(wearer.getAirSupply()<100&&RechargeSupport.consumeCharge(stack,wearer,1)){wearer.setAirSupply(300);return;}
            if(player.canEat(false)&&RechargeSupport.consumeCharge(stack,wearer,1))player.getFoodData().eat(1,.3F);
        }
    }
    @Override public int getMaxCharge(ItemStack stack,LivingEntity wearer){return 200;}
    @Override public EnumChargeDisplay showInHud(ItemStack stack,LivingEntity wearer){return EnumChargeDisplay.NORMAL;}
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag){
        if(type(stack)==1)lines.add(Component.translatable("item.verdant_charm.life.text").withStyle(ChatFormatting.GOLD));
        if(type(stack)==2)lines.add(Component.translatable("item.verdant_charm.sustain.text").withStyle(ChatFormatting.GOLD));
    }
}
