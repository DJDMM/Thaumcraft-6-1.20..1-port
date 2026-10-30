package thaumcraft.world.taint;

import net.minecraft.core.*;
import net.minecraft.world.level.BlockGetter;

final class TaintEcologySupport {
    private TaintEcologySupport() {}
    static boolean adjacentToSolid(BlockGetter level,BlockPos pos) {
        for(Direction dir:Direction.values()) {
            var n=pos.relative(dir);
            if(TaintModule.loaded(level,n)&&level.getBlockState(n).isFaceSturdy(level,n,dir.getOpposite()))return true;
        }
        return false;
    }
}
