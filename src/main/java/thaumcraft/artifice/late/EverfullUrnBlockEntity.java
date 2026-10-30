package thaumcraft.artifice.late;
import java.util.*;
import javax.annotation.Nullable;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.*;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import thaumcraft.world.aura.AuraManager;
public final class EverfullUrnBlockEntity extends BlockEntity {
    private int counter,zone,visualTicks,visualTarget;private final List<Integer> handlers=new ArrayList<>();
    public final FluidTank tank=new FluidTank(1000,f->f.getFluid().isSame(Fluids.WATER)){@Override protected void onContentsChanged(){changed();}};
    private LazyOptional<IFluidHandler> cap=LazyOptional.of(()->tank);
    public EverfullUrnBlockEntity(BlockPos p,BlockState s){super(LateDevicesModule.URN.get(),p,s);}
    public void changed(){setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    private BlockPos target(int zone){return worldPosition.offset(zone/5%5-2,zone/25%3-1,zone%5-2);}
    public static void tick(Level l,BlockPos p,BlockState s,EverfullUrnBlockEntity t){
        ++t.counter;if(l.isClientSide){if(t.visualTicks>0){if(t.visualTicks%5==0)net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->thaumcraft.artifice.late.client.LateDeviceEffects.waterTrail(t,t.target(t.visualTarget),t.counter));--t.visualTicks;}return;}if(l.restoringBlockSnapshots||l.captureBlockSnapshots||t.counter%5!=0)return;
        var check=t.target(++t.zone);
        if(l.hasChunkAt(check)&&(t.handler(check)!=null||t.cauldron(check))&&!t.handlers.contains(t.zone))t.handlers.add(t.zone);
        for(int i=0;i<t.handlers.size()&&t.tank.getFluidAmount()>=25;){
            int index=t.handlers.get(i);var q=t.target(index);if(!l.hasChunkAt(q)){i++;continue;}
            var h=t.handler(q);
            if(h!=null){int accepted=h.fill(new FluidStack(Fluids.WATER,25),IFluidHandler.FluidAction.EXECUTE);if(accepted>0){t.tank.drain(Math.min(25,accepted),IFluidHandler.FluidAction.EXECUTE);l.blockEvent(p,s.getBlock(),1,index);break;}}
            else if(!t.cauldron(q)||t.tank.getFluidAmount()<333){t.handlers.remove(i);continue;}
            else {var st=l.getBlockState(q);int amount=st.is(Blocks.CAULDRON)?0:st.getValue(LayeredCauldronBlock.LEVEL);if(amount<3){l.setBlock(q,Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL,amount+1),2);l.updateNeighbourForOutputSignal(q,st.getBlock());l.blockEvent(p,s.getBlock(),1,index);t.tank.drain(333,IFluidHandler.FluidAction.EXECUTE);}}
            i++;
        }
        if(t.tank.getFluidAmount()<1000&&l instanceof ServerLevel server){float requested=Math.min(.1F,(1000-t.tank.getFluidAmount())/1000F);int made=(int)(1000*AuraManager.drainVis(server,p,requested,false));if(made>0)t.tank.fill(new FluidStack(Fluids.WATER,made),IFluidHandler.FluidAction.EXECUTE);}
    }
    private boolean cauldron(BlockPos p){var s=level.getBlockState(p);return s.is(Blocks.CAULDRON)||s.is(Blocks.WATER_CAULDRON);}
    private IFluidHandler handler(BlockPos p){if(level==null||p.equals(worldPosition)||!level.hasChunkAt(p))return null;var be=level.getBlockEntity(p);return be==null?null:be.getCapability(ForgeCapabilities.FLUID_HANDLER,Direction.UP).orElse(null);}
    @Override protected void saveAdditional(CompoundTag n){super.saveAdditional(n);tank.writeToNBT(n);n.put("handlers",new ListTag());/* Released writer forgets to append its discovered handler tags. */}
    @Override public void load(CompoundTag n){super.load(n);tank.readFromNBT(n);handlers.clear();var list=n.getList("handlers",Tag.TAG_INT);for(int i=0;i<list.size();i++)handlers.add(((IntTag)list.get(i)).getAsInt());}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public <T> LazyOptional<T> getCapability(Capability<T> type,@Nullable Direction side){return type==ForgeCapabilities.FLUID_HANDLER&&side==Direction.UP?cap.cast():super.getCapability(type,side);}
    @Override public void invalidateCaps(){super.invalidateCaps();cap.invalidate();}
    @Override public void reviveCaps(){super.reviveCaps();cap=LazyOptional.of(()->tank);}
    @Override public boolean triggerEvent(int id,int data){if(id==1){if(level!=null&&level.isClientSide){visualTarget=data;visualTicks=5;}return true;}return super.triggerEvent(id,data);}
}
