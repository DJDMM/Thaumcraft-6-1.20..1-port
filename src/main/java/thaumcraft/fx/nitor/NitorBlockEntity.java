package thaumcraft.fx.nitor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import thaumcraft.alchemy.AlchemyModule;
import thaumcraft.fx.FxDispatcher;

/** TileNitor's every-tick flame and first-then-ten-tick white core. No server gameplay work. */
public final class NitorBlockEntity extends BlockEntity {
    private int count;
    public NitorBlockEntity(BlockPos pos,BlockState state){super(AlchemyModule.NITOR_TILE.get(),pos,state);}
    public static void tick(Level level,BlockPos pos,BlockState state,NitorBlockEntity tile){
        if(!level.isClientSide||!(state.getBlock() instanceof NitorBlock nitor))return;var r=level.random;
        FxDispatcher.emit(level,"nitorFlames",new Vec3(pos.getX()+.5F+r.nextGaussian()*.025,pos.getY()+.45F+r.nextGaussian()*.025,pos.getZ()+.5F+r.nextGaussian()*.025),new Vec3(r.nextGaussian()*.0025,r.nextFloat()*.06,r.nextGaussian()*.0025),nitor.color(),1,0);
        if(tile.count++%10==0)FxDispatcher.emit(level,"nitorCore",new Vec3(pos.getX()+.5F,pos.getY()+.49F,pos.getZ()+.5F),Vec3.ZERO,0xFFFFFF,1,0);
    }
}
