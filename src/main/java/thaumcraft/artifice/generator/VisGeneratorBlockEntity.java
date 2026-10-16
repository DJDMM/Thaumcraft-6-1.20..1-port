package thaumcraft.artifice.generator;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import thaumcraft.world.aura.AuraManager;

/** Literal one-Vis/1000 FE converter; energy is pushed, while the original pull API always returns zero. */
public final class VisGeneratorBlockEntity extends BlockEntity implements IEnergyStorage {
    private int energy;private long generation;private boolean ticking;
    private LazyOptional<IEnergyStorage> output=LazyOptional.of(()->this);
    public VisGeneratorBlockEntity(BlockPos pos,BlockState state){super(VisGeneratorModule.BLOCK_ENTITY.get(),pos,state);}
    public int energy(){return energy;}
    private boolean attached(){return level instanceof ServerLevel s&&!isRemoved()&&VisGeneratorModule.loaded(s,worldPosition)&&s.getBlockEntity(worldPosition)==this&&getBlockState().getBlock() instanceof VisGeneratorBlock&&!s.captureBlockSnapshots&&!s.restoringBlockSnapshots&&!VisGeneratorBlockItem.uncommitted(s,worldPosition);}
    private boolean emitting(BlockState state,long revision,int balance){return attached()&&generation==revision&&energy==balance&&getBlockState().equals(state)&&state.getValue(VisGeneratorBlock.ENABLED);}
    public static void tick(Level level,BlockPos pos,BlockState state,VisGeneratorBlockEntity tile){tile.serverTick();}
    public void serverTick(){if(ticking||!attached()||!getBlockState().getValue(VisGeneratorBlock.ENABLED))return;ticking=true;try{
        ServerLevel server=(ServerLevel)level;
        if(energy==0){long before=generation;energy=(int)(AuraManager.drainVis(server,worldPosition,1,false)*1000.0F);setChanged();if(!attached()||generation!=before)return;sync();if(generation!=before)return;}
        if(!attached()||!getBlockState().getValue(VisGeneratorBlock.ENABLED))return;
        BlockState state=getBlockState();long revision=generation;int balance=energy;Direction face=state.getValue(VisGeneratorBlock.FACING);BlockPos at=worldPosition.relative(face);
        if(!VisGeneratorModule.loaded(server,at))return;BlockEntity neighbor=server.getBlockEntity(at);if(neighbor==null||neighbor.isRemoved())return;
        IEnergyStorage receiver;LazyOptional<IEnergyStorage> capability;
        try{capability=neighbor.getCapability(ForgeCapabilities.ENERGY,face.getOpposite());receiver=capability.resolve().orElse(null);
            if(receiver==null||!receiver.canReceive())return;
        }catch(RuntimeException invalidEndpoint){return;}
        if(!emitting(state,revision,balance)||!capability.isPresent()||!VisGeneratorModule.loaded(server,at)||server.getBlockEntity(at)!=neighbor||neighbor.isRemoved())return;
        int offered=Math.min(energy,20);energy-=offered;
        // Saving/reentry inside receive observes a paid reservation, never the old full balance.
        // Direct chunk dirtiness invokes no comparator or endpoint callback before the real offer.
        server.getChunkSource().getChunkNow(worldPosition.getX()>>4,worldPosition.getZ()>>4).setUnsaved(true);
        int accepted;
        try{accepted=Math.max(0,Math.min(offered,receiver.receiveEnergy(offered,false)));}
        catch(RuntimeException brokenEndpoint){if(attached()&&generation==revision){setChanged();if(energy==0)sync();}return;}
        // An external reload/removal owns its new storage; never refund a stale plan into it.
        if(!attached()||generation!=revision)return;
        energy+=offered-accepted;if(accepted>0){setChanged();if(attached()&&energy==0)sync();}
    }finally{ticking=false;}}
    private void sync(){if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    @Override public int receiveEnergy(int amount,boolean simulate){return 0;}
    @Override public int extractEnergy(int amount,boolean simulate){return 0;}
    @Override public int getEnergyStored(){return energy;}
    @Override public int getMaxEnergyStored(){return 1000;}
    @Override public boolean canExtract(){return true;}
    @Override public boolean canReceive(){return false;}
    @Override public <T> LazyOptional<T> getCapability(Capability<T> capability,Direction side){if(capability==ForgeCapabilities.ENERGY&&side!=null&&!isRemoved()&&getBlockState().hasProperty(VisGeneratorBlock.FACING)&&side==getBlockState().getValue(VisGeneratorBlock.FACING))return output.cast();return super.getCapability(capability,side);}
    @Override public void invalidateCaps(){super.invalidateCaps();output.invalidate();}
    @Override public void reviveCaps(){super.reviveCaps();output=LazyOptional.of(()->this);}
    @Override public void setBlockState(BlockState state){BlockState previous=getBlockState();super.setBlockState(state);if(previous.hasProperty(VisGeneratorBlock.FACING)&&state.hasProperty(VisGeneratorBlock.FACING)&&previous.getValue(VisGeneratorBlock.FACING)!=state.getValue(VisGeneratorBlock.FACING)){output.invalidate();output=LazyOptional.of(()->this);}}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.putInt("energy",energy);}
    @Override public void load(CompoundTag tag){super.load(tag);energy=Math.max(0,Math.min(1000,tag.getInt("energy")));generation++;}
    @Override public CompoundTag getUpdateTag(){var tag=new CompoundTag();tag.putInt("energy",energy);return tag;}
    @Override public void handleUpdateTag(CompoundTag tag){load(tag);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public void onDataPacket(net.minecraft.network.Connection connection,ClientboundBlockEntityDataPacket packet){if(packet.getTag()!=null)handleUpdateTag(packet.getTag());}
}
