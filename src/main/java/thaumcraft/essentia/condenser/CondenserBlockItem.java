package thaumcraft.essentia.condenser;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;

/** Native geometry/NBT placement, with a provisional stack until the real Forge callback commits. */
public final class CondenserBlockItem extends BlockItem {
    private static final Set<Level> PLACING=Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Map<Level,Set<BlockPos>> PENDING=new IdentityHashMap<>();
    private static final ThreadLocal<Set<BlockSnapshot>> FOREIGN=new ThreadLocal<>();
    public CondenserBlockItem(Block block){super(block,new Item.Properties());}
    private static final class ShadowContext extends UseOnContext {
        ShadowContext(UseOnContext original,ItemStack shadow){super(original.getLevel(),original.getPlayer(),original.getHand(),shadow,
                new BlockHitResult(original.getClickLocation(),original.getClickedFace(),original.getClickedPos(),original.isInside()));}
    }
    static boolean uncommitted(Level level,BlockPos pos){return !level.isClientSide&&(PENDING.getOrDefault(level,Set.of()).contains(pos)||CondenserLatticeBlock.cleaningUncommitted(level,pos));}
    @Override protected BlockState getPlacementState(BlockPlaceContext context){Level level=context.getLevel();BlockPos pos=context.getClickedPos();if(!CondenserBlockEntity.loaded(level,pos))return null;BlockState before=level.getBlockState(pos);BlockEntity owner=level.getBlockEntity(pos);int start=level.capturedBlockSnapshots.size();
        try{BlockState state=super.getPlacementState(context);return level.getBlockState(pos).equals(before)&&level.getBlockEntity(pos)==owner?state:null;}
        finally{Set<BlockSnapshot> foreign=FOREIGN.get();if(foreign!=null)foreign.addAll(level.capturedBlockSnapshots.subList(start,level.capturedBlockSnapshots.size()));}
    }
    @Override public InteractionResult useOn(UseOnContext context){Level level=context.getLevel();if(level.isClientSide)return super.useOn(context);var player=context.getPlayer();ItemStack physical=context.getItemInHand();
        if(player==null||player.level()!=level||player.isRemoved()||player.isSpectator()||!player.mayBuild()||physical.isEmpty()||player.getItemInHand(context.getHand())!=physical||!level.capturedBlockSnapshots.isEmpty()||!PLACING.add(level))return InteractionResult.FAIL;
        int count=physical.getCount();CompoundTag tag=physical.getTag()==null?null:physical.getTag().copy();boolean creative=player.getAbilities().instabuild;
        boolean capture=level.captureBlockSnapshots,committed=false;int start=level.capturedBlockSnapshots.size();level.captureBlockSnapshots=true;
        List<BlockSnapshot> owned=new ArrayList<>(),before=new ArrayList<>();Map<BlockPos,BlockState> installed=new LinkedHashMap<>();Map<BlockPos,BlockEntity> owners=new HashMap<>();Set<BlockSnapshot> foreign=Collections.newSetFromMap(new IdentityHashMap<>());FOREIGN.set(foreign);BlockPos target=new BlockPlaceContext(context).getClickedPos();
        try{
            // super retains native placement checks, BlockStateTag/BlockEntityTag, criteria and sound.
            // Its temporary debit belongs only to this copy; the actual hand remains intact during callbacks.
            InteractionResult result;
            try{result=super.useOn(new ShadowContext(context,physical.copy()));}
            finally{for(BlockSnapshot snapshot:level.capturedBlockSnapshots.subList(start,level.capturedBlockSnapshots.size()))if(snapshot.getPos().equals(target)&&!foreign.contains(snapshot)){owned.add(snapshot);if(before.stream().noneMatch(first->first.getPos().equals(snapshot.getPos())))before.add(snapshot);}for(BlockSnapshot snapshot:before){installed.put(snapshot.getPos(),level.getBlockState(snapshot.getPos()));owners.put(snapshot.getPos(),level.getBlockEntity(snapshot.getPos()));}}
            if(!result.consumesAction())return result;if(before.isEmpty())return InteractionResult.FAIL;
            PENDING.put(level,Set.copyOf(installed.keySet()));
            // Forge posts placement with capture=false. Keep that native callback state; our pending receipt
            // guards its unpaid targets, and foreign callback snapshots never become this item's property.
            boolean canceled;level.captureBlockSnapshots=false;
            try{canceled=before.size()>1?ForgeEventFactory.onMultiBlockPlace(player,before,context.getClickedFace()):ForgeEventFactory.onBlockPlace(player,before.get(0),context.getClickedFace());}
            finally{level.captureBlockSnapshots=true;}
            // A callback's separate captured transaction remains its property. Do not commit an action
            // which the outer Forge wrapper could re-post/revert using that unrelated receipt.
            if(level.capturedBlockSnapshots.stream().anyMatch(snapshot->owned.stream().noneMatch(ours->ours==snapshot)))return InteractionResult.FAIL;
            if(canceled||player.level()!=level||player.isRemoved()||player.isSpectator()||!player.mayBuild()||player.getItemInHand(context.getHand())!=physical||physical.getCount()!=count||!Objects.equals(tag,physical.getTag())||player.getAbilities().instabuild!=creative)return InteractionResult.FAIL;
            for(var row:installed.entrySet())if(!level.getBlockState(row.getKey()).equals(row.getValue())||level.getBlockEntity(row.getKey())!=owners.get(row.getKey())||!level.mayInteract(player,row.getKey())||!player.mayUseItemAt(row.getKey(),context.getClickedFace(),physical))return InteractionResult.FAIL;
            if(!creative)physical.shrink(1);committed=true;level.captureBlockSnapshots=false;PENDING.remove(level);
            for(BlockSnapshot snapshot:before){BlockState state=level.getBlockState(snapshot.getPos());state.onPlace(level,snapshot.getPos(),snapshot.getReplacedBlock(),false);level.markAndNotifyBlock(snapshot.getPos(),level.getChunkAt(snapshot.getPos()),snapshot.getReplacedBlock(),state,snapshot.getFlag(),512);}
            return result;
        }finally{
            if(!committed){boolean restoring=level.restoringBlockSnapshots;level.restoringBlockSnapshots=true;level.captureBlockSnapshots=false;try{for(int i=before.size()-1;i>=0;i--){BlockSnapshot snapshot=before.get(i);BlockPos pos=snapshot.getPos();if(installed.containsKey(pos)&&level.getBlockState(pos).is(installed.get(pos).getBlock())&&level.getBlockEntity(pos)==owners.get(pos))snapshot.restore(true,false);}}finally{level.restoringBlockSnapshots=restoring;}}
            // Leave every earlier snapshot alone, including those owned by the outer Forge invocation.
            for(BlockSnapshot snapshot:owned)level.capturedBlockSnapshots.removeIf(candidate->candidate==snapshot);level.captureBlockSnapshots=capture;PENDING.remove(level);FOREIGN.remove();PLACING.remove(level);
        }
    }
}
