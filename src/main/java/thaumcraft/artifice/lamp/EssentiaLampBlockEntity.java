package thaumcraft.artifice.lamp;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.*;

/** Terminal, typed one-sided pull. Original lamps never expose stored essence or accept raw pushes. */
public abstract class EssentiaLampBlockEntity extends BlockEntity implements IEssentiaTransport {
    protected int charges,drawDelay;
    protected boolean operation;
    protected EssentiaLampBlockEntity(BlockEntityType<?> type,BlockPos pos,BlockState state){super(type,pos,state);}
    public int charges(){return charges;} public int drawAttempts(){return drawDelay;}
    protected abstract Aspect fuel();
    protected abstract void readReservoir(CompoundTag tag);
    protected abstract void writeReservoir(CompoundTag tag);
    public static boolean loaded(Level level,BlockPos pos){if(level.isOutsideBuildHeight(pos)||!level.getWorldBorder().isWithinBounds(pos))return false;return level instanceof ServerLevel s?s.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null:level.hasChunkAt(pos);}
    protected boolean live(){return level!=null&&!isRemoved()&&loaded(level,worldPosition)&&level.getBlockEntity(worldPosition)==this;}
    protected boolean ready(){return level instanceof ServerLevel&&live()&&!operation&&!level.captureBlockSnapshots&&!level.restoringBlockSnapshots&&!LampBlockItem.uncommitted(level,worldPosition);}
    protected Direction face(){return getBlockState().getValue(EssentiaLampBlock.FACING);}
    protected Boolean powered(){return EssentiaLampBlock.loadedPower(level,worldPosition);}
    protected void enabled(boolean enabled){if(live()&&getBlockState().getValue(EssentiaLampBlock.ENABLED)!=enabled)level.setBlock(worldPosition,getBlockState().setValue(EssentiaLampBlock.ENABLED,enabled),3);}
    protected void changed(){setChanged();if(live()&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    protected boolean drawEssentia(){
        if(++drawDelay%5!=0||!live())return false;Direction side=face(),other=side.getOpposite();BlockPos pos=worldPosition.relative(side);
        if(!loaded(level,pos))return false;BlockEntity tile=level.getBlockEntity(pos);
        if(!(tile instanceof IEssentiaTransport source)||!knownSource(tile)||!safeBufferNeighbors(tile)||!source.isConnectable(other)||!source.canOutputTo(other)
                ||source.getSuctionAmount(other)>=getSuctionAmount(side))return false;
        // No minimum-suction/type/amount pre-gate: the release directly asks for its exact aspect.
        if(!live()||tile.isRemoved()||level.getBlockEntity(pos)!=tile)return false;
        CompoundTag before=tile.saveWithoutMetadata();int amount=source.getEssentiaAmount(other);
        int paid=source.takeEssentia(fuel(),1,other);
        if(paid==1&&live()&&level.getBlockEntity(pos)==tile&&!tile.isRemoved()&&source.getEssentiaAmount(other)==amount-1)return true;
        if(level.getBlockEntity(pos)==tile&&!tile.isRemoved()&&source.getEssentiaAmount(other)!=amount){tile.load(before);tile.setChanged();level.sendBlockUpdated(pos,tile.getBlockState(),tile.getBlockState(),3);}return false;
    }
    private boolean safeBufferNeighbors(BlockEntity source){if(!(source instanceof thaumcraft.essentia.transport.TubeBufferBlockEntity))return true;
        for(Direction side:Direction.values()){BlockPos pos=source.getBlockPos().relative(side);if(!loaded(level,pos))continue;BlockEntity other=level.getBlockEntity(pos);if(other instanceof IEssentiaTransport&&!knownSource(other))return false;}return true;
    }
    private static boolean knownSource(BlockEntity tile){Class<?> t=tile.getClass();return t==GrowthLampBlockEntity.class||t==FertilityLampBlockEntity.class||t==thaumcraft.essentia.EssentiaJarBlockEntity.class||t==thaumcraft.essentia.production.AlembicBlockEntity.class||t==thaumcraft.essentia.transport.TubeBlockEntity.class||t==thaumcraft.essentia.transport.TubeBufferBlockEntity.class||t==thaumcraft.essentia.transport.TubeFilterBlockEntity.class||t==thaumcraft.essentia.centrifuge.CentrifugeBlockEntity.class||t==thaumcraft.essentia.thaumatorium.ThaumatoriumBlockEntity.class||t==thaumcraft.essentia.thaumatorium.ThaumatoriumTopBlockEntity.class||t==thaumcraft.golemancy.press.GolemPressBlockEntity.class||t==thaumcraft.essentia.condenser.CondenserBlockEntity.class||t==thaumcraft.essentia.transfuser.EssentiaTransfuserBlockEntity.class;}
    @Override public boolean isConnectable(Direction side){return side==face();}
    @Override public boolean canInputFrom(Direction side){return side==face();}
    @Override public boolean canOutputTo(Direction side){return false;}
    @Override public void setSuction(Aspect type,int amount){}
    @Override public Aspect getSuctionType(Direction side){return fuel();}
    @Override public int getMinimumSuction(){return 0;}
    @Override public int takeEssentia(Aspect type,int amount,Direction side){return 0;}
    @Override public int addEssentia(Aspect type,int amount,Direction side){return 0;}
    @Override public Aspect getEssentiaType(Direction side){return null;}
    @Override public int getEssentiaAmount(Direction side){return 0;}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);writeReservoir(tag);}
    @Override public void load(CompoundTag tag){super.load(tag);readReservoir(tag);drawDelay=0;resetTransient();}
    protected void resetTransient(){}
    @Override public CompoundTag getUpdateTag(){CompoundTag tag=new CompoundTag();writeReservoir(tag);return tag;}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public void handleUpdateTag(CompoundTag tag){readReservoir(tag);if(level!=null)level.getLightEngine().checkBlock(worldPosition);}
    @Override public void onDataPacket(Connection connection,ClientboundBlockEntityDataPacket packet){if(packet.getTag()!=null)handleUpdateTag(packet.getTag());}
    /** Shared transfer rollback restores paid reservoirs, never rewinds a live search/draw/animal clock. */
    public void restoreTransportSnapshot(CompoundTag tag){if(level instanceof ServerLevel&&live()&&!operation){readReservoir(tag);changed();}}
}
