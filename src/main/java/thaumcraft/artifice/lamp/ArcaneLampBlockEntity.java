package thaumcraft.artifice.lamp;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.catalog.blocks.CatalogBlocks;

/** Released total-world-time five-tick cadence and triangular light candidate. */
public final class ArcaneLampBlockEntity extends BlockEntity {
    public ArcaneLampBlockEntity(BlockPos pos,BlockState state){super(LampModule.ARCANE_LAMP.get(),pos,state);}
    public static void tick(Level level,BlockPos pos,BlockState state,ArcaneLampBlockEntity lamp){lamp.serverTick();}
    public void serverTick(){
        if(!(level instanceof ServerLevel server)||server.getGameTime()%5!=0||server.hasNeighborSignal(worldPosition)||!present(server))return;
        int x=server.random.nextInt(16)-server.random.nextInt(16),y=server.random.nextInt(16)-server.random.nextInt(16),z=server.random.nextInt(16)-server.random.nextInt(16);
        BlockPos candidate=clampCandidate(server,worldPosition.offset(x,y,z));
        if(candidate!=null)placeLight(server,candidate);
    }
    private boolean present(ServerLevel server){var chunk=server.getChunkSource().getChunkNow(worldPosition.getX()>>4,worldPosition.getZ()>>4);return !isRemoved()&&chunk!=null&&chunk.getBlockEntity(worldPosition,net.minecraft.world.level.chunk.LevelChunk.EntityCreationType.CHECK)==this;}
    static boolean loaded(ServerLevel server,BlockPos pos){return !server.isOutsideBuildHeight(pos)&&server.getWorldBorder().isWithinBounds(pos)&&server.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null;}
    static BlockPos clampCandidate(ServerLevel server,BlockPos candidate){
        if(!server.getWorldBorder().isWithinBounds(candidate)||server.getChunkSource().getChunkNow(candidate.getX()>>4,candidate.getZ()>>4)==null)return null;
        int top=Math.min(server.getMaxBuildHeight()-1,server.getHeight(Heightmap.Types.MOTION_BLOCKING,candidate.getX(),candidate.getZ())+4);
        // Keep the original five-block bottom margin in worlds with modern negative Y.
        int y=Math.max(server.getMinBuildHeight()+5,Math.min(candidate.getY(),top));
        return new BlockPos(candidate.getX(),y,candidate.getZ());
    }
    boolean placeLight(ServerLevel server,BlockPos target){
        if(!present(server)||server.hasNeighborSignal(worldPosition)||!loaded(server,target)||!server.isEmptyBlock(target)
                ||server.getBlockState(target).is(CatalogBlocks.block("effect_glimmer"))||server.getBrightness(LightLayer.BLOCK,target)>=11||!hasLineOfSight(server,target))return false;
        return server.setBlock(target,CatalogBlocks.block("effect_glimmer").defaultBlockState(),Block.UPDATE_ALL);
    }
    boolean hasLineOfSight(ServerLevel server,BlockPos target){
        Vec3 from=Vec3.atCenterOf(worldPosition),to=Vec3.atCenterOf(target);
        int x0=Math.min(worldPosition.getX(),target.getX())>>4,x1=Math.max(worldPosition.getX(),target.getX())>>4;
        int z0=Math.min(worldPosition.getZ(),target.getZ())>>4,z1=Math.max(worldPosition.getZ(),target.getZ())>>4;
        for(int x=x0;x<=x1;x++)for(int z=z0;z<=z1;z++)if(server.getChunkSource().getChunkNow(x,z)==null)return false;
        // Ignore only the source collision shape, as original rayTraceIgnoringSource.
        var ray=new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,null){
            @Override public net.minecraft.world.phys.shapes.VoxelShape getBlockShape(BlockState state,net.minecraft.world.level.BlockGetter world,BlockPos pos){return pos.equals(worldPosition)?net.minecraft.world.phys.shapes.Shapes.empty():super.getBlockShape(state,world,pos);}
        };
        var hit=server.clip(ray);return hit.getType()==HitResult.Type.MISS||hit.getBlockPos().equals(target);
    }
    public void removeLights(){
        if(!(level instanceof ServerLevel server))return;
        Block glimmer=CatalogBlocks.block("effect_glimmer");
        for(int x=-15;x<=15;x++)for(int y=-15;y<=15;y++)for(int z=-15;z<=15;z++){
            BlockPos pos=worldPosition.offset(x,y,z);if(loaded(server,pos)&&server.getBlockState(pos).is(glimmer))server.removeBlock(pos,false);
        }
    }
}
