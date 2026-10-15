package thaumcraft.artifice.dioptra;

import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import thaumcraft.world.aura.AuraManager;

/** Released13x13 chunk aura grid, sampled each20ticks without spending aura. */
public final class DioptraBlockEntity extends BlockEntity {
    public int counter;
    public byte[] grid_amt=new byte[169];
    public DioptraBlockEntity(BlockPos pos,BlockState state){super(DioptraModule.BLOCK_ENTITY.get(),pos,state);}
    public byte[] gridSnapshot(){return grid_amt.clone();}
    public int centerAmount(){return grid_amt[84];}
    public int comparatorSignal(){float ratio=centerAmount()/64F;return (int)Math.floor(ratio*14F)+(ratio>0?1:0);}
    public static int scale(float amount){return (byte)Math.min(64F,amount/500F*64F);}
    private boolean live(){return level!=null&&!isRemoved()&&DioptraModule.loaded(level,worldPosition)&&level.getBlockEntity(worldPosition)==this;}
    public static void tick(Level level,BlockPos pos,BlockState state,DioptraBlockEntity tile){tile.update();}
    public void update(){
        if(!live()||level.captureBlockSnapshots||level.restoringBlockSnapshots)return;
        ++counter;
        if(level.isClientSide){counter=0;return;}
        if(counter%20!=0||!(level instanceof ServerLevel server))return;
        Arrays.fill(grid_amt,(byte)0);boolean vis=getBlockState().getValue(DioptraBlock.ENABLED);
        for(int x=0;x<13;x++)for(int z=0;z<13;z++){
            int cx=(worldPosition.getX()>>4)+x-6,cz=(worldPosition.getZ()>>4)+z-6;
            // The original aura cache contains loaded chunks. Persisted modern unloaded records must not become visible.
            if(server.getChunkSource().getChunkNow(cx,cz)==null)continue;
            BlockPos sample=new BlockPos((cx<<4)+8,worldPosition.getY(),(cz<<4)+8);
            grid_amt[x+z*13]=(byte)scale(vis?AuraManager.getVis(server,sample):AuraManager.getFlux(server,sample));
        }
        setChanged();server.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);
    }
    private void readGrid(CompoundTag tag){if(tag.contains("grid_a",7)){byte[] raw=tag.getByteArray("grid_a");grid_amt=new byte[169];for(int i=0;i<Math.min(169,raw.length);i++)grid_amt[i]=(byte)Math.max(0,Math.min(64,raw[i]));}}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.putByteArray("grid_a",grid_amt.clone());}
    @Override public void load(CompoundTag tag){super.load(tag);counter=0;readGrid(tag);}
    @Override public CompoundTag getUpdateTag(){CompoundTag tag=new CompoundTag();tag.putByteArray("grid_a",grid_amt.clone());return tag;}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public void handleUpdateTag(CompoundTag tag){readGrid(tag);}
    @Override public void onDataPacket(Connection connection,ClientboundBlockEntityDataPacket packet){if(packet.getTag()!=null)readGrid(packet.getTag());}
    @Override public AABB getRenderBoundingBox(){return new AABB(worldPosition.getX()-.3,worldPosition.getY()-.3,worldPosition.getZ()-.3,worldPosition.getX()+1.3,worldPosition.getY()+2.3,worldPosition.getZ()+1.3);}
}
