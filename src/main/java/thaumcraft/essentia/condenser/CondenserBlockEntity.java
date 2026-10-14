package thaumcraft.essentia.condenser;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.*;
import thaumcraft.world.aura.AuraManager;

/** BETA26's source-ordered local pull, weighted lattice DFS and two100-unit reservoirs. */
public final class CondenserBlockEntity extends BlockEntity implements IEssentiaTransport {
    // EnumFacing.HORIZONTALS is SOUTH,WEST,NORTH,EAST, not modern horizontal iteration order.
    private static final Direction[] HORIZONTAL={Direction.SOUTH,Direction.WEST,Direction.NORTH,Direction.EAST};
    private int essentia,flux,count,interval,cost;
    private float latticeCount=-1;
    private final Set<BlockPos> history=new HashSet<>();
    private final List<BlockPos> blocks=new ArrayList<>(),clean=new ArrayList<>();
    private boolean operation,spilled,unknownTopology,powerReady;
    public CondenserBlockEntity(BlockPos pos,BlockState state){super(CondenserModule.CONDENSER.get(),pos,state);}
    public int storedEssentia(){return essentia;} public int storedFlux(){return flux;}
    public int ticks(){return count;} public float latticeCount(){return latticeCount;}
    public int interval(){return interval;} public int cost(){return cost;}
    public List<BlockPos> cleanLatticePositions(){return List.copyOf(clean);}
    public int connectedLatticeCount(){return blocks.size();}
    public void invalidateLattice(){latticeCount=-1;powerReady=false;}
    private boolean mutable(){return level==null||!level.isClientSide;}
    private boolean live(){return level!=null&&!isRemoved()&&loaded(level,worldPosition)&&level.getBlockEntity(worldPosition)==this;}
    static boolean loaded(Level level,BlockPos pos){if(level.isOutsideBuildHeight(pos))return false;return level instanceof ServerLevel server?server.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null:level.hasChunkAt(pos);}
    private void changed(){setChanged();if(level!=null&&!level.isClientSide&&live())level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    public static void tick(Level level,BlockPos pos,BlockState state,CondenserBlockEntity tile){
        if(tile.operation||!tile.live()||level.captureBlockSnapshots||level.restoringBlockSnapshots||CondenserBlockItem.uncommitted(level,pos))return;
        if(tile.latticeCount<0)tile.triggerCheck();
        ++tile.count;
        if(level.isClientSide)return;
        if(!tile.powerReady){Boolean powered=CondenserBlock.loadedPower(level,pos);if(powered==null||!tile.live())return;tile.powerReady=true;((CondenserBlock)tile.getBlockState().getBlock()).updatePower(tile.getBlockState(),level,pos);}
        if(!tile.live()||!tile.getBlockState().getValue(CondenserBlock.ENABLED)||tile.latticeCount<=0)return;
        tile.operation=true;
        try{
            if(tile.count%5==0&&tile.essentia<100)tile.fill();
            if(!tile.live()||!tile.getBlockState().getValue(CondenserBlock.ENABLED))return;
            if(tile.interval>0&&tile.essentia>=tile.cost&&tile.flux<100&&tile.count%tile.interval==0&&AuraManager.getFlux((ServerLevel)level,pos)>=1){
                if(AuraManager.drainFlux((ServerLevel)level,pos,1,false)!=1)return;
                tile.essentia-=tile.cost;++tile.flux;tile.setChanged();
                if(level.random.nextInt(50)==0)tile.makeLatticeDirty();
                tile.changed();
            }
        }finally{tile.operation=false;}
    }
    private void fill(){
        for(Direction face:HORIZONTAL){
            BlockPos p=worldPosition.relative(face);if(!loaded(level,p))continue;
            BlockEntity endpoint=level.getBlockEntity(p);
            if(!(endpoint instanceof IEssentiaTransport source)||!nativeSource(endpoint)||!safeBufferNeighbors(endpoint)||!source.isConnectable(face.getOpposite()))continue;
            if(!live()||endpoint.isRemoved()||level.getBlockEntity(p)!=endpoint)return;
            // The original return, rather than continue, lets an earlier closed output block later faces.
            if(!source.canOutputTo(face.getOpposite()))return;
            int suction=getSuctionAmount(face);
            if(source.getEssentiaAmount(face.getOpposite())<=0||source.getSuctionAmount(face.getOpposite())>=suction||suction<source.getMinimumSuction())continue;
            Aspect type=source.getEssentiaType(face.getOpposite());
            if(!live()||endpoint.isRemoved()||level.getBlockEntity(p)!=endpoint)return;
            if(type==null)continue;
            if(type==Aspect.FLUX){makeLatticeDirty();changed();continue;}
            // Exact native classes have reversible saved reservoirs and no opaque paid callback subclasses.
            CompoundTag before=endpoint.saveWithoutMetadata();int amount=source.getEssentiaAmount(face.getOpposite());
            if(!live()||endpoint.isRemoved()||level.getBlockEntity(p)!=endpoint||essentia>=100)return;
            int paid=source.takeEssentia(type,1,face.getOpposite());
            if(paid==1&&live()&&level.getBlockEntity(p)==endpoint&&!endpoint.isRemoved()&&source.getEssentiaAmount(face.getOpposite())==amount-1){++essentia;changed();}
            else if(level.getBlockEntity(p)==endpoint&&!endpoint.isRemoved()&&source.getEssentiaAmount(face.getOpposite())!=amount){endpoint.load(before);endpoint.setChanged();level.sendBlockUpdated(p,endpoint.getBlockState(),endpoint.getBlockState(),3);}
            if(essentia>=100)break;
        }
    }
    private static boolean nativeSource(BlockEntity tile){Class<?> type=tile.getClass();return type==thaumcraft.essentia.EssentiaJarBlockEntity.class||type==thaumcraft.essentia.production.AlembicBlockEntity.class
            ||type==thaumcraft.essentia.transport.TubeBlockEntity.class||type==thaumcraft.essentia.transport.TubeBufferBlockEntity.class||type==thaumcraft.essentia.transport.TubeFilterBlockEntity.class
            ||type==thaumcraft.essentia.centrifuge.CentrifugeBlockEntity.class||type==thaumcraft.essentia.thaumatorium.ThaumatoriumBlockEntity.class||type==thaumcraft.essentia.thaumatorium.ThaumatoriumTopBlockEntity.class||type==thaumcraft.golemancy.press.GolemPressBlockEntity.class
            ||type==CondenserBlockEntity.class||type==thaumcraft.essentia.transfuser.EssentiaTransfuserBlockEntity.class;}
    private boolean safeBufferNeighbors(BlockEntity source){if(!(source instanceof thaumcraft.essentia.transport.TubeBufferBlockEntity))return true;
        // Buffer.takeEssentia queries every neighboring consumer. Do not enter an opaque nested debit callback.
        for(Direction side:Direction.values()){BlockPos pos=source.getBlockPos().relative(side);if(!loaded(level,pos))continue;BlockEntity other=level.getBlockEntity(pos);if(other instanceof IEssentiaTransport&&!(other instanceof CondenserBlockEntity)&&!nativeSource(other))return false;}return true;}
    private void makeLatticeDirty(){if(clean.isEmpty())return;int index=level.random.nextInt(clean.size());if(index==0)index=level.random.nextInt(clean.size());BlockPos pos=clean.get(index);
        if(loaded(level,pos)&&level.getBlockState(pos).getBlock() instanceof CondenserLatticeBlock lattice&&!lattice.dirty()){level.setBlock(pos,thaumcraft.catalog.blocks.CatalogBlocks.block("condenser_lattice_dirty").defaultBlockState(),3);CondenserLatticeBlock.triggerUpdate(level,pos);}}
    public void triggerCheck(){if(level==null)return;history.clear();blocks.clear();clean.clear();latticeCount=0;interval=0;unknownTopology=false;performCheck(worldPosition,true,false);history.clear();
        if(unknownTopology){latticeCount=-1;return;}if(latticeCount<=0){latticeCount=0;return;}latticeCount=Math.min(40,latticeCount);interval=Math.max(5,Math.round(600-latticeCount*15));cost=(int)(4+Math.sqrt(blocks.size()));}
    private static final class CheckFrame {final BlockPos pos;final boolean skip;boolean clogged,found;int face,sides;CheckFrame(BlockPos pos,boolean skip,boolean clogged){this.pos=pos;this.skip=skip;this.clogged=clogged;}}
    private void performCheck(BlockPos pos,boolean skip,boolean clogged){if(latticeCount<0)return;ArrayDeque<CheckFrame> stack=new ArrayDeque<>();history.add(pos);stack.push(new CheckFrame(pos,skip,clogged));
        while(!stack.isEmpty()){CheckFrame frame=stack.peek();if(frame.face==6){stack.pop();if(frame.found&&!frame.clogged)latticeCount+=1-.15F*frame.sides;continue;}
            Direction face=Direction.values()[frame.face++];if(frame.skip&&face!=Direction.UP)continue;BlockPos p=frame.pos.relative(face);
            if(level.isOutsideBuildHeight(p))continue;if(!loaded(level,p)){unknownTopology=true;continue;}BlockState state=level.getBlockState(p);boolean lattice=state.getBlock() instanceof CondenserLatticeBlock candidate&&!candidate.dirty();boolean dirty=state.getBlock() instanceof CondenserLatticeBlock candidate&&candidate.dirty();
            if(frame.skip&&dirty)frame.clogged=true;if(lattice||dirty)++frame.sides;if(history.contains(p))continue;
            if(face==Direction.DOWN&&state.getBlock() instanceof CondenserBlock){latticeCount=-99;stack.pop();continue;}
            if(worldPosition.getY()<p.getY()&&worldPosition.distSqr(p)<=74&&(lattice||dirty)){blocks.add(p);if(lattice)clean.add(p);frame.found=true;if(frame.skip)frame.face=6;
                // Match the source entry guard, including ancestors' post-order work after its negative sentinel.
                if(latticeCount>=0){history.add(p);stack.push(new CheckFrame(p,false,frame.clogged||dirty));}}
        }
    }
    void spillOnRemoval(ServerLevel server){if(spilled||isRemoved()||server.captureBlockSnapshots||server.restoringBlockSnapshots||CondenserBlockItem.uncommitted(server,worldPosition))return;spilled=true;int paid=flux;flux=0;essentia=0;setChanged();if(paid>0)thaumcraft.artifice.furnace.InfernalPollutionPacket.pollute(server,worldPosition,paid);}
    @Override public boolean isConnectable(Direction face){return face!=null&&face!=Direction.UP;}
    @Override public boolean canInputFrom(Direction face){return face!=null&&face.getAxis().isHorizontal();}
    @Override public boolean canOutputTo(Direction face){return face==Direction.DOWN;}
    @Override public void setSuction(Aspect type,int amount){}
    @Override public Aspect getSuctionType(Direction face){return null;}
    @Override public int getSuctionAmount(Direction face){return face==Direction.DOWN||essentia>=100?0:128;}
    @Override public int getMinimumSuction(){return 0;}
    @Override public Aspect getEssentiaType(Direction face){return Aspect.FLUX;}
    @Override public int getEssentiaAmount(Direction face){return flux;}
    @Override public int takeEssentia(Aspect aspect,int amount,Direction face){if(!mutable()||operation||level!=null&&(!live()||level.captureBlockSnapshots||level.restoringBlockSnapshots||CondenserBlockItem.uncommitted(level,worldPosition))||amount<=0||!canOutputTo(face)||(aspect!=null&&aspect!=Aspect.FLUX))return 0;int taken=Math.min(amount,flux);if(taken>0){flux-=taken;changed();}return taken;}
    @Override public int addEssentia(Aspect aspect,int amount,Direction face){return 0;} // Release falsely reported credit without storing it; reject push to conserve its payer.
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.putShort("essentia",(short)essentia);tag.putShort("flux",(short)flux);}
    @Override public void load(CompoundTag tag){super.load(tag);essentia=Math.max(0,Math.min(100,tag.getInt("essentia")));flux=Math.max(0,Math.min(100,tag.getInt("flux")));latticeCount=-1;interval=0;cost=0;count=0;blocks.clear();clean.clear();spilled=false;powerReady=false;}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public void handleUpdateTag(CompoundTag tag){essentia=Math.max(0,Math.min(100,tag.getInt("essentia")));flux=Math.max(0,Math.min(100,tag.getInt("flux")));}
    @Override public void onDataPacket(Connection connection,ClientboundBlockEntityDataPacket packet){if(packet.getTag()!=null)handleUpdateTag(packet.getTag());}
    /** Shared native transport rollback restores resources, never an original unsaved topology/countdown. */
    public void restoreTransportSnapshot(CompoundTag tag){if(level instanceof ServerLevel&&live()&&!operation&&!spilled){handleUpdateTag(tag);changed();}}
}
