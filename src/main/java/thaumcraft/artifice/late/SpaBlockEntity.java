package thaumcraft.artifice.late;
import javax.annotation.Nullable;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import thaumcraft.equipment.cleansing.*;
public final class SpaBlockEntity extends LateInventoryBlockEntity {
    private boolean mix=true;private int counter;
    public final FluidTank tank=new FluidTank(5000){@Override protected void onContentsChanged(){changed();}};
    private LazyOptional<IFluidHandler> fluid=LazyOptional.of(()->tank);
    public SpaBlockEntity(BlockPos p,BlockState s){super(LateDevicesModule.SPA.get(),p,s);}
    public boolean mix(){return mix;}
    public void toggleMix(){if(level!=null&&!level.isClientSide){mix=!mix;changed();}}
    @Override public boolean canPlaceItem(int slot,ItemStack s){return slot==0&&s.getItem() instanceof BathSaltsItem;}
    @Override public int[] getSlotsForFace(Direction face){return face==Direction.UP?new int[0]:new int[]{0};}
    @Override public boolean canPlaceItemThroughFace(int slot,ItemStack s,@Nullable Direction side){return side!=Direction.UP&&canPlaceItem(slot,s);}
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack s,Direction side){return side!=Direction.UP;}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inv,Player p){return new LateDeviceMenu(id,inv,this);}
    public static void tick(Level l,BlockPos p,BlockState s,SpaBlockEntity t){
        if(l.isClientSide||l.restoringBlockSnapshots||l.captureBlockSnapshots||t.counter++%40!=0||l.hasNeighborSignal(p)||t.tank.getFluidAmount()<1000)return;
        var fs=t.tank.getFluid();if(t.mix&&(!fs.getFluid().isSame(Fluids.WATER)||!(t.getItem(0).getItem() instanceof BathSaltsItem)))return;
        Block target=t.mix?CleansingModule.block():fs.getFluid().defaultFluidState().createLegacyBlock().getBlock();
        if(target==Blocks.AIR)return;
        BlockPos q=p.above();if(l.getBlockState(q).is(target)&&l.getFluidState(q).isSource()){
            for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){q=p.offset(x,1,z);if(t.valid(q,true,target)){t.place(q,target);return;}}
        }else if(t.valid(q,false,target))t.place(q,target);
    }
    private boolean valid(BlockPos p,boolean adjacent,Block target){
        if(level==null||!level.hasChunkAt(p)||((target==Blocks.WATER)&&level.dimensionType().ultraWarm()))return false;
        var state=level.getBlockState(p);if(!level.getBlockState(p.below()).isFaceSturdy(level,p.below(),Direction.UP)||!state.canBeReplaced()||(state.is(target)&&state.getFluidState().isSource()))return false;
        if(!adjacent)return true;
        for(var d:Direction.values()){var q=p.relative(d);if(level.hasChunkAt(q)&&level.getBlockState(q).is(target)&&level.getFluidState(q).isSource())return true;}return false;
    }
    private void place(BlockPos p,Block target){
        if(level==null)return;
        if(level.setBlock(p,target.defaultBlockState(),3)){if(mix)items.get(0).shrink(1);tank.drain(1000,IFluidHandler.FluidAction.EXECUTE);changed();}
    }
    @Override protected void saveAdditional(CompoundTag n){super.saveAdditional(n);n.putBoolean("mix",mix);tank.writeToNBT(n);}
    @Override public void load(CompoundTag n){super.load(n);mix=n.contains("mix")?n.getBoolean("mix"):true;tank.readFromNBT(n);}
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,@Nullable Direction side){return cap==ForgeCapabilities.FLUID_HANDLER?fluid.cast():super.getCapability(cap,side);}
    @Override public void invalidateCaps(){super.invalidateCaps();fluid.invalidate();}
    @Override public void reviveCaps(){super.reviveCaps();fluid=LazyOptional.of(()->tank);}
}
