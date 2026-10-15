package thaumcraft.artifice.lamp;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;

/** Released20 plant ticks per Herba, plus one separately paid reserve. */
public final class GrowthLampBlockEntity extends EssentiaLampBlockEntity {
    private boolean reserve;
    private final ArrayList<BlockPos> checklist=new ArrayList<>();
    private BlockPos lastPlant=BlockPos.ZERO;
    private BlockState lastState=Blocks.AIR.defaultBlockState();
    public GrowthLampBlockEntity(BlockPos pos,BlockState state){super(LampModule.LAMP_GROWTH.get(),pos,state);charges=-1;}
    public boolean reserve(){return reserve;} public boolean hasReserve(){return reserve;}
    public int pendingColumnCount(){return checklist.size();} public BlockPos lastPlant(){return lastPlant;}
    @Override protected Aspect fuel(){return Aspect.PLANT;}
    @Override public int getSuctionAmount(Direction face){return face==face()&&(!reserve||charges<=0)?128:0;}
    public static void tick(Level level,BlockPos pos,BlockState state,GrowthLampBlockEntity lamp){lamp.serverTick();}
    public void serverTick(){
        if(!ready())return;operation=true;
        try{
            if(charges<=0){
                if(reserve){charges=20;reserve=false;changed();}
                else if(drawEssentia()){charges=20;changed();}
                if(!live())return;
                if(charges<=0)enabled(false);
                else if(Boolean.FALSE.equals(powered()))enabled(true);
            }
            if(!live())return;
            if(!reserve&&drawEssentia()){reserve=true;setChanged();}
            if(!live())return;
            if(charges==0){charges=-1;changed();}
            if(charges>0&&Boolean.FALSE.equals(powered()))updatePlant();
        }finally{operation=false;}
    }
    private void updatePlant(){
        ServerLevel server=(ServerLevel)level;
        if(loaded(server,lastPlant)){
            BlockState previous=server.getBlockState(lastPlant);
            if(!previous.equals(lastState)){
                if(server.getNearestPlayer(lastPlant.getX(),lastPlant.getY(),lastPlant.getZ(),32,false)!=null)
                    thaumcraft.artifice.lamp.client.GrowthMistPacket.send(server,lastPlant,4259648);
                lastState=previous;
            }
        }
        if(checklist.isEmpty()){
            for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++)checklist.add(worldPosition.offset(x,6,z));
            // Fisher-Yates is Collections.shuffle's exact sequence of nextInt(size) calls.
            for(int i=checklist.size();i>1;i--)Collections.swap(checklist,i-1,server.random.nextInt(i));
        }
        BlockPos column=checklist.remove(0);
        if(!loaded(server,new BlockPos(column.getX(),worldPosition.getY(),column.getZ())))return;
        for(int y=column.getY();y>=worldPosition.getY()-6;y--){
            BlockPos target=new BlockPos(column.getX(),y,column.getZ());
            if(!loaded(server,target)||worldPosition.distSqr(target)>=36)continue;
            BlockState state=server.getBlockState(target);
            if(state.isAir()||!LampPlantRules.isPlant(state)||LampPlantRules.isGrown(server,target,state)||!LampPlantRules.allowed(state))continue;
            if(!live()||charges<=0)return;
            --charges;setChanged();lastPlant=target;lastState=state;
            // 1.12 Block.updateTick was the growth callback. Modern crops split it into randomTick.
            // Preserve the original native scheduled tick and bridge that growth callback one tick later.
            LampPlantTickData.get(server).schedule(server,target,state.getBlock());
            return;
        }
    }
    @Override protected void readReservoir(CompoundTag tag){charges=tag.getInt("charges");reserve=tag.getBoolean("reserve");}
    @Override protected void writeReservoir(CompoundTag tag){tag.putInt("charges",charges);tag.putBoolean("reserve",reserve);}
    @Override protected void resetTransient(){checklist.clear();lastPlant=BlockPos.ZERO;lastState=Blocks.AIR.defaultBlockState();}
}
