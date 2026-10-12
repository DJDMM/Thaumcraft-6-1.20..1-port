package thaumcraft.artifice.mirror;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** Bound and plain are typed link variants, never fabricated durability values. */
public final class MirrorBlockItem extends BlockItem {
    public MirrorBlockItem(Block block){super(block,new Item.Properties().rarity(Rarity.UNCOMMON));}
    @Override public InteractionResult onItemUseFirst(ItemStack held,UseOnContext context){
        if(context.getLevel().getBlockState(context.getClickedPos()).getBlock()!=getBlock())return InteractionResult.PASS;
        if(context.getLevel().isClientSide)return InteractionResult.SUCCESS;
        var player=context.getPlayer();if(player==null||held!=player.getItemInHand(context.getHand())||held.isEmpty())return InteractionResult.FAIL;
        if(!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof AbstractMirrorBlockEntity mirror)||!mirror.physicallyPresent())return InteractionResult.FAIL;
        if(mirror.isLinkValid()){player.displayClientMessage(Component.translatable("tc.mirrorlinked").withStyle(net.minecraft.ChatFormatting.DARK_PURPLE,net.minecraft.ChatFormatting.ITALIC),false);return InteractionResult.SUCCESS;}
        // An unavailable existing endpoint is not evidence that this mirror is free to steal.
        if(mirror.resolvePeer(false).status()==AbstractMirrorBlockEntity.Status.UNAVAILABLE)return InteractionResult.FAIL;
        ItemStack bound=held.copy();bound.setCount(1);mirror.endpoint().writeTo(bound);
        if(!player.isCreative())held.shrink(1);
        player.getInventory().add(bound);
        if(!bound.isEmpty()&&context.getLevel() instanceof ServerLevel server)MirrorRefundSavedData.get(server).add(player.blockPosition(),context.getClickedFace(),bound);
        MirrorModule.playJar(context.getLevel(),context.getClickedPos(),1,2);
        player.inventoryMenu.broadcastChanges();return InteractionResult.SUCCESS;
    }
    @Override public InteractionResult place(BlockPlaceContext context){
        MirrorLink link=MirrorLink.read(context.getItemInHand());var pos=context.getClickedPos();MirrorPlacementReceipt receipt=MirrorPlacementReceipt.begin(context);
        try{
            InteractionResult result=super.place(context);
            if(result.consumesAction()&&!context.getLevel().isClientSide&&link!=null&&context.getLevel().getBlockEntity(pos) instanceof AbstractMirrorBlockEntity mirror){mirror.setLink(link);mirror.restoreLink();}
            return result;
        }finally{MirrorPlacementReceipt.end(receipt);}
    }
    @Override public void appendHoverText(ItemStack stack,@Nullable Level level,List<Component> lines,TooltipFlag flag){super.appendHoverText(stack,level,lines,flag);MirrorModule.linkTooltip(stack,lines);}
}
