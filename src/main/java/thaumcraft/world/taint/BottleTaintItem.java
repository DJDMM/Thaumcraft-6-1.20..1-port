package thaumcraft.world.taint;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.*;
public final class BottleTaintItem extends Item {
 public BottleTaintItem(Properties properties){super(properties.stacksTo(8));}
 @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
  ItemStack stack=player.getItemInHand(hand);
  if(!level.isClientSide){var bottle=TaintEcologyModule.BOTTLE_TAINT.get().create(level);if(bottle==null)return InteractionResultHolder.fail(stack);bottle.setOwner(player);bottle.setPos(player.getX(),player.getEyeY()-.1,player.getZ());bottle.shootFromRotation(player,player.getXRot(),player.getYRot(),-5,.66F,1);if(!level.addFreshEntity(bottle))return InteractionResultHolder.fail(stack);if(!player.getAbilities().instabuild)stack.shrink(1);}
  player.playSound(SoundEvents.EGG_THROW,.5F,.4F/(level.random.nextFloat()*.4F+.8F));return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
 }
}
