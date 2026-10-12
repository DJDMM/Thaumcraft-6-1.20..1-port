package thaumcraft.artifice.mirror;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;

/** Ephemeral native Forge placement proof; ordinary block/item interaction order remains vanilla. */
final class MirrorPlacementReceipt {
    private static final ThreadLocal<MirrorPlacementReceipt> CURRENT=new ThreadLocal<>();
    private static final Map<ServerLevel,List<MirrorPlacementReceipt>> RECEIPTS=new IdentityHashMap<>();
    private final ServerLevel level;
    private final Player player;
    private final net.minecraft.world.InteractionHand hand;
    private final ItemStack held,before;
    private final BlockPos pos;
    private final BlockState previous;
    private final long gameTime;
    private final boolean creative;
    private AbstractMirrorBlockEntity placed;
    @Nullable private BlockEvent.EntityPlaceEvent placementEvent;
    private boolean canceled,eventSeen,identityAtEvent,settled,paid;
    @Nullable static MirrorPlacementReceipt begin(BlockPlaceContext context){
        if(!(context.getLevel() instanceof ServerLevel level)||!level.captureBlockSnapshots||CURRENT.get()!=null||context.getPlayer()==null)return null;
        MirrorPlacementReceipt receipt=new MirrorPlacementReceipt(level,context);CURRENT.set(receipt);RECEIPTS.computeIfAbsent(level,l->new ArrayList<>()).add(receipt);return receipt;
    }
    private MirrorPlacementReceipt(ServerLevel level,BlockPlaceContext context){this.level=level;player=context.getPlayer();hand=context.getHand();held=context.getItemInHand();before=held.copy();pos=context.getClickedPos().immutable();previous=level.getBlockState(pos);gameTime=level.getGameTime();creative=player.isCreative();}
    static void end(@Nullable MirrorPlacementReceipt receipt){if(receipt!=null&&CURRENT.get()==receipt)CURRENT.remove();}
    @Nullable static MirrorPlacementReceipt capture(AbstractMirrorBlockEntity mirror){MirrorPlacementReceipt receipt=CURRENT.get();if(receipt!=null&&mirror.getBlockPos().equals(receipt.pos)){receipt.placed=mirror;return receipt;}return null;}
    void canceled(){canceled=true;}
    static void event(BlockEvent.EntityPlaceEvent event){
        if(!(event.getLevel() instanceof ServerLevel level))return;
        for(MirrorPlacementReceipt receipt:List.copyOf(RECEIPTS.getOrDefault(level,List.of())))if(!receipt.settled&&receipt.gameTime==level.getGameTime()&&receipt.pos.equals(event.getPos())&&receipt.player==event.getEntity()){
            receipt.placementEvent=event;receipt.eventSeen=true;receipt.identityAtEvent=receipt.player.getItemInHand(receipt.hand)==receipt.held;
        }
    }
    boolean ready(){return settled||level.getGameTime()>gameTime;}
    /** Save/retry only a paid native final stack, never the temporarily restored pre-event stack. */
    boolean paid(){
        if(settled)return paid;
        if(canceled||(placementEvent!=null&&placementEvent.isCanceled())||(eventSeen?!identityAtEvent:player.getItemInHand(hand)!=held))return false;
        if(creative)return eventSeen;
        if(held.getCount()!=before.getCount()-1)return false;
        ItemStack expected=before.copy();expected.shrink(1);return ItemStack.matches(held,expected);
    }
    private void settle(){
        if(settled||!ready())return;paid=paid();settled=true;
        if(!paid&&placed!=null&&level.getBlockEntity(pos)==placed){
            canceled=true;
            // Never erase a callback's replacement block. Paid queue removal remains unconditional.
            level.setBlock(pos,previous,3);
        }
    }
    static void tick(ServerLevel level){
        List<MirrorPlacementReceipt> receipts=RECEIPTS.get(level);if(receipts==null)return;
        for(MirrorPlacementReceipt receipt:List.copyOf(receipts))receipt.settle();receipts.removeIf(r->r.settled);
        if(receipts.isEmpty())RECEIPTS.remove(level);
    }
    static void forget(ServerLevel level){RECEIPTS.remove(level);}
}
