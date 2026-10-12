package thaumcraft.artifice.furnace;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.*;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import thaumcraft.world.aura.AuraManager;

/** Original32-slot countdown furnace; a persistent emission owner protects paid modern callbacks. */
public final class InfernalFurnaceBlockEntity extends BlockEntity implements WorldlyContainer {
    private final NonNullList<ItemStack> input=NonNullList.withSize(32,ItemStack.EMPTY);
    private static final int[] UP_SLOTS=java.util.stream.IntStream.range(0,32).toArray();
    private LazyOptional<IItemHandler> up=LazyOptional.of(()->new SidedInvWrapper(this,Direction.UP));
    private int cookTime,maxCookTime,speedyTime;
    private boolean ticking,drained;
    public InfernalFurnaceBlockEntity(BlockPos pos,BlockState state){super(InfernalFurnaceModule.FURNACE.get(),pos,state);}
    public int cookTime(){return cookTime;}public int maxCookTime(){return maxCookTime;}public int speedyTime(){return speedyTime;}
    public int bellows(){return level==null?0:InfernalFurnaceRules.bellows(level,worldPosition);}
    public Direction outputFace(){return getBlockState().getValue(InfernalFurnaceBlock.FACING).getOpposite();}
    public int queuedInputCount(){return input.stream().mapToInt(ItemStack::getCount).sum();}
    public int pendingOutputCount(){return level instanceof ServerLevel server?InfernalFurnaceOutputData.get(server).pendingItemCount(worldPosition):0;}
    private boolean attached(){return level!=null&&!isRemoved()&&!drained&&level.getBlockEntity(worldPosition)==this;}
    private boolean sourceStable(int slot,ItemStack source,int count,CompoundTag tag){return attached()&&input.get(slot)==source&&source.getCount()==count&&java.util.Objects.equals(tag,source.getTag());}
    public static void tick(Level level,BlockPos pos,BlockState state,InfernalFurnaceBlockEntity furnace){furnace.tick();}
    private void tick(){if(!(level instanceof ServerLevel server)||ticking||!attached())return;ticking=true;try{
        boolean cooked=false;if(cookTime>0){cookTime--;cooked=true;}
        if(maxCookTime<=0)maxCookTime=InfernalFurnaceRules.cookTime(speedyTime,bellows());
        if(cookTime>maxCookTime)cookTime=maxCookTime;
        if(cookTime<=0&&cooked){for(int slot=0;slot<32;slot++){ItemStack source=input.get(slot);if(source.isEmpty())continue;int physicalCount=source.getCount();CompoundTag physicalTag=source.getTag()==null?null:source.getTag().copy();var recipe=InfernalFurnaceRules.recipe(server,source);if(!sourceStable(slot,source,physicalCount,physicalTag))return;if(recipe.isEmpty()){input.set(slot,ItemStack.EMPTY);continue;}
            ItemStack result=recipe.get().assemble(new SimpleContainer(source),server.registryAccess());if(!sourceStable(slot,source,physicalCount,physicalTag))return;if(result.isEmpty()){input.set(slot,ItemStack.EMPTY);continue;}
            var outputs=InfernalFurnaceOutputData.get(server);Direction face=outputFace();java.util.List<ItemStack> bonuses=InfernalFurnaceRules.bonuses(source,bellows(),server.random);
            int experience=InfernalFurnaceRules.experience(result.getCount(),recipe.get().getExperience(),Math.random());
            if(!sourceStable(slot,source,physicalCount,physicalTag))return;
            // Reserve the whole paid result before any spawn/inventory callback. No external callback occurs here.
            source.shrink(1);if(source.isEmpty())input.set(slot,ItemStack.EMPTY);if(speedyTime>0)speedyTime--;
            outputs.add(worldPosition,face,result,true);for(ItemStack bonus:bonuses)outputs.add(worldPosition,face,bonus,true);outputs.addExperience(worldPosition,face,experience);
            server.blockEvent(worldPosition,getBlockState().getBlock(),3,0);
            if(server.random.nextInt(20)==0)InfernalPollutionPacket.pollute(server,worldPosition.relative(face),1);
            setChanged();outputs.tick(server);break;
        }}
        if(!attached())return;
        if(speedyTime<=0)speedyTime=(int)AuraManager.drainVis(server,worldPosition,20,false);
        if(cookTime==0&&!cooked)for(int slot=0;slot<32;slot++){ItemStack stack=input.get(slot);int count=stack.getCount();CompoundTag tag=stack.getTag()==null?null:stack.getTag().copy();var recipe=InfernalFurnaceRules.recipe(server,stack);if(!sourceStable(slot,stack,count,tag))return;if(recipe.isPresent()){maxCookTime=InfernalFurnaceRules.cookTime(speedyTime,bellows());cookTime=maxCookTime;break;}}
        setChanged();
    }finally{ticking=false;}}
    /** Actual collision source identity is retained until the synchronous input transfer completes. */
    public void collide(ItemEntity entity){if(!attached()||!(level instanceof ServerLevel server)||entity.level()!=level||entity.isRemoved()||!entity.onGround()||entity.tickCount%10!=0)return;ItemStack physical=entity.getItem();if(physical.isEmpty())return;int count=physical.getCount();CompoundTag tag=physical.getTag()==null?null:physical.getTag().copy();var recipe=InfernalFurnaceRules.recipe(server,physical);
        if(!attached()||entity.isRemoved()||entity.getItem()!=physical||physical.getCount()!=count||!java.util.Objects.equals(tag,physical.getTag()))return;
        if(recipe.isEmpty()){entity.setItem(ItemStack.EMPTY);entity.discard();server.playSound(null,worldPosition,net.minecraft.sounds.SoundEvents.LAVA_EXTINGUISH,net.minecraft.sounds.SoundSource.BLOCKS,.3f,2.6f+(server.random.nextFloat()-server.random.nextFloat())*.8f);server.sendParticles(net.minecraft.core.particles.ParticleTypes.LAVA,worldPosition.getX()+server.random.nextFloat(),worldPosition.getY()+1,worldPosition.getZ()+server.random.nextFloat(),1,0,0,0,0);return;}
        ItemStack remainder=ItemHandlerHelper.insertItem(up.orElseThrow(()->new IllegalStateException("Removed input")),physical.copy(),false);if(entity.getItem()!=physical)return;entity.setItem(remainder);if(remainder.isEmpty())entity.discard();
    }
    /** Transfer original unprocessed contents exactly once on physical dismantling; cooked outputs already have their own owner. */
    public void releaseInputs(){if(drained||!(level instanceof ServerLevel server))return;drained=true;var outputs=InfernalFurnaceOutputData.get(server);for(int slot=0;slot<32;slot++){ItemStack stack=input.get(slot);input.set(slot,ItemStack.EMPTY);outputs.add(worldPosition,Direction.UP,stack,false);}setChanged();outputs.tick(server);}
    @Override public int getContainerSize(){return 32;}@Override public boolean isEmpty(){return input.stream().allMatch(ItemStack::isEmpty);}
    @Override public ItemStack getItem(int slot){return input.get(slot);}
    @Override public ItemStack removeItem(int slot,int amount){ItemStack removed=ContainerHelper.removeItem(input,slot,amount);setChanged();return removed;}
    @Override public ItemStack removeItemNoUpdate(int slot){return ContainerHelper.takeItem(input,slot);}
    @Override public void setItem(int slot,ItemStack stack){if(drained)return;input.set(slot,stack);if(stack.getCount()>getMaxStackSize())stack.setCount(getMaxStackSize());setChanged();}
    @Override public boolean stillValid(Player player){return attached()&&player.distanceToSqr(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5)<=64;}
    @Override public void clearContent(){input.clear();setChanged();}
    @Override public int[] getSlotsForFace(Direction face){return face==Direction.UP?UP_SLOTS.clone():new int[0];}
    @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction face){return attached()&&face==Direction.UP;}
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction face){return false;}
    @Override public <T> LazyOptional<T> getCapability(Capability<T> capability,Direction side){if(capability==ForgeCapabilities.ITEM_HANDLER&&(side==null||side==Direction.UP)&&attached())return up.cast();return super.getCapability(capability,side);}
    @Override public void invalidateCaps(){super.invalidateCaps();up.invalidate();}
    @Override public void reviveCaps(){super.reviveCaps();up=LazyOptional.of(()->new SidedInvWrapper(this,Direction.UP));}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);ContainerHelper.saveAllItems(tag,input);tag.putShort("CookTime",(short)cookTime);tag.putShort("SpeedyTime",(short)speedyTime);}
    @Override public void load(CompoundTag tag){super.load(tag);for(int i=0;i<32;i++)input.set(i,ItemStack.EMPTY);ContainerHelper.loadAllItems(tag,input);cookTime=tag.getShort("CookTime");speedyTime=tag.getShort("SpeedyTime");maxCookTime=0;drained=false;}
    @Override public boolean triggerEvent(int event,int value){if(event==3){if(level!=null&&level.isClientSide)net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->thaumcraft.artifice.furnace.client.InfernalFurnaceClientEffects.handle(level,worldPosition,getBlockState(),event));return true;}return super.triggerEvent(event,value);}
    @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(){return new net.minecraft.world.phys.AABB(worldPosition.getX()-1.3,worldPosition.getY()-1.3,worldPosition.getZ()-1.3,worldPosition.getX()+2.3,worldPosition.getY()+2.3,worldPosition.getZ()+2.3);}
}
