package thaumcraft.artifice.mirror;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.network.NetworkHooks;

/** Original hand mirror sends to the explicitly selected item mirror, including an unpaired one. */
public final class HandMirrorItem extends Item {
    public record Target(AbstractMirrorBlockEntity.Status status,@Nullable ItemMirrorBlockEntity mirror){}
    public HandMirrorItem(){super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));}
    @Override public boolean isFoil(ItemStack stack){return stack.hasTag();}
    public static Target target(ServerLevel level,@Nullable MirrorLink link){
        if(link==null)return new Target(AbstractMirrorBlockEntity.Status.INVALID,null);
        ServerLevel remote=level.getServer().getLevel(link.dimension());if(remote==null)return new Target(AbstractMirrorBlockEntity.Status.UNAVAILABLE,null);
        LevelChunk chunk=remote.getChunkSource().getChunkNow(link.pos().getX()>>4,link.pos().getZ()>>4);
        if(chunk==null)return new Target(AbstractMirrorBlockEntity.Status.UNAVAILABLE,null);
        var tile=chunk.getBlockEntity(link.pos(),LevelChunk.EntityCreationType.CHECK);
        return tile instanceof ItemMirrorBlockEntity mirror&&mirror.physicallyPresent()?new Target(AbstractMirrorBlockEntity.Status.VALID,mirror):new Target(AbstractMirrorBlockEntity.Status.INVALID,null);
    }
    @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext context){
        if(!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof ItemMirrorBlockEntity mirror))return InteractionResult.FAIL;
        if(context.getLevel().isClientSide)return InteractionResult.SUCCESS;
        Player player=context.getPlayer();if(player==null||player.getItemInHand(context.getHand())!=stack||!mirror.physicallyPresent())return InteractionResult.FAIL;
        mirror.endpoint().writeTo(stack);MirrorModule.playJar(context.getLevel(),context.getClickedPos(),1,2);
        player.displayClientMessage(Component.translatable("tc.handmirrorlinked"),false);player.inventoryMenu.broadcastChanges();return InteractionResult.SUCCESS;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        ItemStack stack=player.getItemInHand(hand);MirrorLink link=MirrorLink.read(stack);
        if(link==null)return InteractionResultHolder.pass(stack);
        if(level instanceof ServerLevel server&&player instanceof ServerPlayer owner){
            Target target=target(server,link);
            if(target.status()==AbstractMirrorBlockEntity.Status.INVALID){invalidate(stack,player);return InteractionResultHolder.fail(stack);}
            if(target.status()==AbstractMirrorBlockEntity.Status.UNAVAILABLE)return InteractionResultHolder.pass(stack);
            NetworkHooks.openScreen(owner,new SimpleMenuProvider((id,inventory,p)->new HandMirrorMenu(id,inventory,hand),Component.translatable("item.thaumcraft.hand_mirror")),data->data.writeEnum(hand));
        }
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    static void invalidate(ItemStack stack,Player player){stack.setTag(null);MirrorModule.playZap(player);player.displayClientMessage(Component.translatable("tc.handmirrorerror"),false);player.inventoryMenu.broadcastChanges();}
    @Override public void appendHoverText(ItemStack stack,@Nullable Level level,List<Component> lines,TooltipFlag flag){super.appendHoverText(stack,level,lines,flag);MirrorModule.linkTooltip(stack,lines);}
}
