package thaumcraft.artifice.late;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import thaumcraft.catalog.blocks.CatalogBlocks;
import thaumcraft.world.aura.AuraManager;
/** TileArcaneWorkbench original nine-chunk integer totals and ordered integer-credit spend. */
public final class WorkbenchChargerSupport {
    private WorkbenchChargerSupport(){}
    private static boolean charger(ServerLevel l,BlockPos p){return l.getBlockState(p.above()).is(CatalogBlocks.block("arcane_workbench_charger"));}
    private static boolean loaded(ServerLevel l,BlockPos p){return l.getChunkSource().getChunkNow(p.getX()>>4,p.getZ()>>4)!=null;}
    public static int available(ServerLevel l,BlockPos p){
        if(!loaded(l,p))return 0;if(!charger(l,p))return (int)AuraManager.getVis(l,p);
        int total=0;for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){var at=p.offset(x*16,0,z*16);if(loaded(l,at))total+=(int)AuraManager.getVis(l,at);}return total;
    }
    public static float spend(ServerLevel l,BlockPos p,int vis){
        if(vis<=0||!l.getServer().isSameThread()||!loaded(l,p))return 0;
        if(!charger(l,p))return AuraManager.drainVis(l,p,vis,false);
        if(available(l,p)<vis)return 0;
        int remaining=vis,share=Math.max(1,vis/9),attempts=0;
        Map<BlockPos,Float> debits=new LinkedHashMap<>();
        outer:while(remaining>0){
            attempts++;for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){
                if(share>remaining)share=remaining;var at=p.offset(x*16,0,z*16);
                float drained=loaded(l,at)?AuraManager.drainVis(l,at,share,false):0;
                debits.merge(at,drained,Float::sum);remaining-=(int)drained;
                if(remaining<=0)break outer;if(attempts>1000)break outer;
            }
        }
        if(remaining==0)return vis;
        // A failed modern atomic craft must not retain any of its partial multi-chunk debit.
        debits.forEach((at,amount)->AuraManager.addVis(l,at,amount));return 0;
    }
}
