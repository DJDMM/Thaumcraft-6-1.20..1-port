package thaumcraft.artifice.grapple;

import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import thaumcraft.api.items.IRechargable;
import thaumcraft.catalog.entities.VisualEntitiesModule;
import thaumcraft.equipment.RechargeSupport;

/** Released rechargeable tool: one charge per accepted shot, no durability, ammunition or loaded gate. */
public final class GrappleGunItem extends Item implements IRechargable {
    private static final Set<Player> FIRING=Collections.newSetFromMap(new IdentityHashMap<>());
    public GrappleGunItem(){super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON));}
    @Override public int getMaxCharge(ItemStack stack,LivingEntity wearer){return 100;}
    @Override public EnumChargeDisplay showInHud(ItemStack stack,LivingEntity wearer){return EnumChargeDisplay.NORMAL;}
    @Override public void inventoryTick(ItemStack stack,Level level,Entity holder,int slot,boolean selected){
        if(holder instanceof LivingEntity owner&&GrappleModule.active(level,owner)==null&&stack.hasTag()&&stack.getTag().getByte("loaded")==1)stack.getTag().putByte("loaded",(byte)0);
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        player.playSound(GrappleModule.ICE.get(),3,.8F+level.random.nextFloat()*.1F);
        ItemStack stack=player.getItemInHand(hand);
        if(!(level instanceof ServerLevel server)||player.isRemoved()||!player.isAlive()||player.isSpectator()||RechargeSupport.getCharge(stack)<=0||!FIRING.add(player))return InteractionResultHolder.success(stack);
        CompoundTag before=stack.getTag()==null?null:stack.getTag().copy();GrappleEntity hook=null;boolean paid=false;ItemStack reserved=null;
        try{
            if(!RechargeSupport.consumeCharge(stack,player,1))return InteractionResultHolder.success(stack);
            reserved=stack.copy();hook=new GrappleEntity(VisualEntitiesModule.GRAPPLE.get(),server);hook.prepareShot(player,hand);
            if(!server.hasChunkAt(hook.blockPosition())||!server.addFreshEntity(hook))return InteractionResultHolder.success(stack);
            if(hook.isRemoved()||player.level()!=server||player.isRemoved()||!player.isAlive()||player.getItemInHand(hand)!=stack||!ItemStack.matches(stack,reserved))return InteractionResultHolder.success(stack);
            stack.getOrCreateTag().putByte("loaded",(byte)1);hook.commitShot();paid=true;
            return InteractionResultHolder.success(stack);
        }finally{
            if(!paid){if(hook!=null)hook.discard();if(reserved!=null&&ItemStack.matches(stack,reserved))stack.setTag(before);}
            FIRING.remove(player);
        }
    }
    @Override public boolean shouldCauseReequipAnimation(ItemStack oldStack,ItemStack newStack,boolean slotChanged){
        if(oldStack.getItem()==this&&newStack.getItem()==this&&!ItemStack.isSameItemSameTags(oldStack,newStack)){
            boolean oldEmpty=!oldStack.hasTag()||oldStack.getTag().getByte("loaded")==0;
            boolean newEmpty=!newStack.hasTag()||newStack.getTag().getByte("loaded")==0;
            return oldEmpty!=newEmpty;
        }
        return oldStack.getItem()!=newStack.getItem();
    }
}
