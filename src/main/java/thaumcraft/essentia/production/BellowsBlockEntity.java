package thaumcraft.essentia.production;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** BETA26 TileBellows: transient pump phase, literal vanilla cookTime bound199. */
public final class BellowsBlockEntity extends BlockEntity {
    private float inflation=1;
    private boolean direction,firstRun=true;
    private int delay;
    public BellowsBlockEntity(BlockPos pos,BlockState state) { super(EssentiaProductionModule.BELLOWS.get(),pos,state); }
    public float inflation() { return inflation; }
    public int delay() { return delay; }
    @Override public AABB getRenderBoundingBox() { return new AABB(worldPosition.getX()-.3,worldPosition.getY()-.3,worldPosition.getZ()-.3,worldPosition.getX()+1.3,worldPosition.getY()+1.3,worldPosition.getZ()+1.3); }
    public static void tick(Level level,BlockPos pos,BlockState state,BellowsBlockEntity bellows) {
        if(bellows.isRemoved()||!state.is(thaumcraft.catalog.blocks.CatalogBlocks.block("bellows"))||level.getBlockEntity(pos)!=bellows||!state.getValue(SmelterBellowsBlock.ENABLED))return;
        if(level.isClientSide) {
            if(bellows.firstRun)bellows.inflation=.35F+level.random.nextFloat()*.55F;
            bellows.firstRun=false;
            if(bellows.inflation>.35F&&!bellows.direction)bellows.inflation-=.075F;
            if(bellows.inflation<=.35F&&!bellows.direction)bellows.direction=true;
            if(bellows.inflation<1&&bellows.direction)bellows.inflation+=.025F;
            if(bellows.inflation>=1&&bellows.direction) {
                bellows.direction=false;
                level.playLocalSound(pos.getX(),pos.getY(),pos.getZ(),SoundEvents.GHAST_SHOOT,SoundSource.BLOCKS,.01F,.5F+(level.random.nextFloat()-level.random.nextFloat())*.2F,false);
            }
        } else if(++bellows.delay>=2) {
            bellows.delay=0;
            var target=pos.relative(state.getValue(SmelterBellowsBlock.FACING));
            if(level.hasChunkAt(target)&&level.getBlockEntity(target) instanceof FurnaceBlockEntity furnace) {
                int progress=furnace.cookingProgress;
                if(progress>0&&progress<199) { furnace.cookingProgress=progress+1;furnace.setChanged(); }
            }
        }
    }
    /** Original consumers require a real correctly oriented enabled bellows tile. */
    public static boolean active(Level level,BlockPos pos,Direction towards) {
        if(!level.hasChunkAt(pos)||!(level.getBlockEntity(pos) instanceof BellowsBlockEntity))return false;
        var state=level.getBlockState(pos);
        return state.is(thaumcraft.catalog.blocks.CatalogBlocks.block("bellows"))&&state.getValue(SmelterBellowsBlock.FACING)==towards&&state.getValue(SmelterBellowsBlock.ENABLED);
    }
}
