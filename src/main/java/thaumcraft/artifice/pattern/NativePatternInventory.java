package thaumcraft.artifice.pattern;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import thaumcraft.artifice.hungrychest.HungryChestBlockEntity;

/** A bounded observable native inventory. Opaque paid third-party handlers are not transactional. */
final class NativePatternInventory {
    private static final Set<Class<?>> NATIVE=Set.of(ChestBlockEntity.class,TrappedChestBlockEntity.class,BarrelBlockEntity.class,HopperBlockEntity.class,DispenserBlockEntity.class,DropperBlockEntity.class,ShulkerBoxBlockEntity.class,FurnaceBlockEntity.class,BlastFurnaceBlockEntity.class,SmokerBlockEntity.class,BrewingStandBlockEntity.class,HungryChestBlockEntity.class);
    private record Owner(BlockPos pos,BlockEntity tile,BlockState state){}
    private final ServerLevel level;final Container container;final Direction face;
    private final List<Owner> owners;private final ItemStack[] physical,values;
    private NativePatternInventory(ServerLevel level,Container container,Direction face,List<Owner> owners){this.level=level;this.container=container;this.face=face;this.owners=List.copyOf(owners);physical=new ItemStack[container.getContainerSize()];values=new ItemStack[physical.length];for(int i=0;i<physical.length;i++){physical[i]=container.getItem(i);values[i]=physical[i].copy();}}
    static boolean loaded(ServerLevel level,BlockPos pos){return !level.isOutsideBuildHeight(pos)&&level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null;}
    static NativePatternInventory capture(ServerLevel level,BlockPos pos,Direction face){if(!loaded(level,pos))return null;BlockEntity tile=level.getBlockEntity(pos);if(tile==null||!NATIVE.contains(tile.getClass())||!(tile instanceof Container container))return null;var owners=new ArrayList<Owner>();BlockState state=level.getBlockState(pos);owners.add(new Owner(pos.immutable(),tile,state));
        if((tile.getClass()==ChestBlockEntity.class||tile.getClass()==TrappedChestBlockEntity.class)&&state.getBlock() instanceof ChestBlock chest&&state.hasProperty(ChestBlock.TYPE)&&state.getValue(ChestBlock.TYPE)!=ChestType.SINGLE){BlockPos neighbor=pos.relative(ChestBlock.getConnectedDirection(state));if(!loaded(level,neighbor))return null;BlockEntity other=level.getBlockEntity(neighbor);if(other==null||other.getClass()!=tile.getClass())return null;owners.add(new Owner(neighbor.immutable(),other,level.getBlockState(neighbor)));container=ChestBlock.getContainer(chest,state,level,pos,true);if(container==null)return null;}
        NativePatternInventory result=new NativePatternInventory(level,container,face,owners);Set<ItemStack> physical=Collections.newSetFromMap(new IdentityHashMap<>());for(ItemStack stack:result.physical)if(!stack.isEmpty()&&!physical.add(stack))return null;return result.stable()?result:null;
    }
    boolean attached(){for(Owner owner:owners)if(!loaded(level,owner.pos)||owner.tile.isRemoved()||level.getBlockEntity(owner.pos)!=owner.tile||!level.getBlockState(owner.pos).equals(owner.state))return false;return true;}
    boolean stable(){if(!attached())return false;for(int i=0;i<physical.length;i++)if(container.getItem(i)!=physical[i]||!ItemStack.matches(physical[i],values[i]))return false;return true;}
    int size(){return values.length;}ItemStack exemplar(int slot){return values[slot].copy();}
    private int[] accessible(){return container instanceof WorldlyContainer sided?sided.getSlotsForFace(face):java.util.stream.IntStream.range(0,size()).toArray();}
    private boolean exposed(int slot){for(int value:accessible())if(value==slot)return true;return false;}
    private boolean canTake(int slot){return exposed(slot)&&(!(container instanceof WorldlyContainer sided)||sided.canTakeItemThroughFace(slot,physical[slot],face));}
    private boolean canPut(int slot,ItemStack stack){return exposed(slot)&&container.canPlaceItem(slot,stack)&&(!(container instanceof WorldlyContainer sided)||sided.canPlaceItemThroughFace(slot,stack,face));}
    record Debit(int slot,int amount){}
    List<Debit> debitPlan(ItemStack exemplar,int amount){var plan=new ArrayList<Debit>();int remaining=amount;for(int slot=0;slot<size()&&remaining>0;slot++)if(!physical[slot].isEmpty()&&sameBaseOre(exemplar,physical[slot])&&canTake(slot)){int take=Math.min(remaining,physical[slot].getCount());plan.add(new Debit(slot,take));remaining-=take;}return remaining==0?plan:null;}
    /** No callbacks: shrink the already-observed original physical stacks, never manufacture extraction receipts. */
    void debit(List<Debit> plan){for(Debit debit:plan){physical[debit.slot].shrink(debit.amount);values[debit.slot]=physical[debit.slot].copy();}}
    record Insertion(int output,int slot,int amount){}
    List<Insertion> reserve(List<ItemStack> outputs){var shadow=Arrays.stream(values).map(ItemStack::copy).toArray(ItemStack[]::new);var plan=new ArrayList<Insertion>();for(int output=0;output<outputs.size();output++){ItemStack stack=outputs.get(output);int remaining=stack.getCount();for(int slot=0;slot<size()&&remaining>0;slot++)if(canPut(slot,stack)){ItemStack existing=shadow[slot];if(!existing.isEmpty()&&!ItemStack.isSameItemSameTags(existing,stack))continue;int capacity=Math.min(container.getMaxStackSize(),stack.getMaxStackSize())-existing.getCount();int moved=Math.min(remaining,Math.max(0,capacity));if(moved>0){shadow[slot]=stack.copyWithCount(existing.getCount()+moved);plan.add(new Insertion(output,slot,moved));remaining-=moved;}}if(remaining!=0)return null;}return plan;}
    /** Exact native setter classes assign before their comparator callback. Caller retires the paid amount first. */
    void insert(Insertion insertion,ItemStack offered){int slot=insertion.slot;ItemStack old=physical[slot];if(!old.isEmpty()){old.grow(insertion.amount);values[slot]=old.copy();return;}ItemStack installed=offered.copyWithCount(insertion.amount);physical[slot]=installed;values[slot]=installed.copy();container.setItem(slot,installed);}
    void changed(){for(Owner owner:owners)owner.tile.setChanged();}
    static boolean sameBaseOre(ItemStack exemplar,ItemStack candidate){if(ItemStack.isSameItemSameTags(exemplar,candidate))return true;if(exemplar.isEmpty()||candidate.isEmpty()||candidate.hasTag())return false;
        // Original BASEORE compares the candidate with ore-entry exemplars, not with the prime's NBT.
        // A broad forge:ingots tag would incorrectly mix metals; mapped ore entries have plain NBT.
        Set<ResourceLocation> names=new HashSet<>();exemplar.getTags().map(net.minecraft.tags.TagKey::location).filter(NativePatternInventory::oreTag).forEach(names::add);return candidate.getTags().map(net.minecraft.tags.TagKey::location).anyMatch(names::contains);
    }
    private static boolean oreTag(ResourceLocation id){String path=id.getPath();return id.getNamespace().equals("minecraft")&&(path.equals("planks")||path.equals("logs"))||id.getNamespace().equals("forge")&&(path.startsWith("ingots/")||path.startsWith("nuggets/")||path.startsWith("storage_blocks/")||path.startsWith("ores/")||path.startsWith("gems/")||path.startsWith("dusts/")||path.startsWith("rods/"));}
}
