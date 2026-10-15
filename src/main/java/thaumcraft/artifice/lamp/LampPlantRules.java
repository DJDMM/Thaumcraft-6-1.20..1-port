package thaumcraft.artifice.lamp;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

/** Explicit modern equivalents of IGrowable / PLANTS / CACTUS, excluding old GRASS material. */
public final class LampPlantRules {
    private static final Set<BlockState> BLACKLIST=Collections.newSetFromMap(new IdentityHashMap<>());
    private LampPlantRules(){}
    public static void blacklist(BlockState state){BLACKLIST.add(state);}
    public static boolean allowed(BlockState state){return !BLACKLIST.contains(state);}
    public static int callbackRadius(BlockState state){return state.getBlock() instanceof SaplingBlock?32:state.getBlock() instanceof MushroomBlock?8:2;}
    /** Native growth may read a wider footprint than its initial isAreaLoaded(pos,1) check. */
    public static boolean callbackFootprintLoaded(ServerLevel level,BlockPos pos,BlockState state){
        int radius=callbackRadius(state);
        for(int x=(pos.getX()-radius)>>4;x<=(pos.getX()+radius)>>4;x++)for(int z=(pos.getZ()-radius)>>4;z<=(pos.getZ()+radius)>>4;z++)
            if(level.getChunkSource().getChunkNow(x,z)==null)return false;
        return level.getWorldBorder().isWithinBounds(pos.offset(-radius,0,-radius))&&level.getWorldBorder().isWithinBounds(pos.offset(radius,0,radius));
    }
    public static boolean isPlant(BlockState state){Block b=state.getBlock();return !(b instanceof GrassBlock)&&(b instanceof BonemealableBlock||b instanceof BushBlock||b instanceof CactusBlock||b instanceof SugarCaneBlock);}
    public static boolean isGrown(ServerLevel level,BlockPos pos,BlockState state){
        Block b=state.getBlock();
        if((b instanceof SugarCaneBlock||b instanceof CactusBlock)&&level.getBlockState(pos.below()).is(b))return true;
        if(b instanceof NetherWartBlock&&state.getValue(NetherWartBlock.AGE)==3)return true;
        if(b instanceof StemBlock)return false;
        return b instanceof BonemealableBlock growable&&!growable.isValidBonemealTarget(level,pos,state,false);
    }
}
