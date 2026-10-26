package thaumcraft.fx.nitor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;
import thaumcraft.alchemy.AlchemyModule;

/** Original invisible, non-colliding circuit-material light; its flame is a ticked client effect. */
public final class NitorBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE=Block.box(5.2800002098,5.2800002098,5.2800002098,10.5600004196,10.5600004196,10.5600004196);
    private final DyeColor dye;
    public NitorBlock(DyeColor dye){super(Properties.of().mapColor(dye.getMapColor()).strength(.1F).sound(SoundType.WOOL).noCollission().noOcclusion().lightLevel(s->15));this.dye=dye;}
    public DyeColor dye(){return dye;}
    public int color(){return dye.getMapColor().col;}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.INVISIBLE;}
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return SHAPE;}
    @Override public VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return Shapes.empty();}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new NitorBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){
        if(type==thaumcraft.catalog.blocks.CatalogBlocks.VISUAL_TILE.get())return (world,pos,current,old)->{if(world.getBlockEntity(pos)==old)world.setBlockEntity(new NitorBlockEntity(pos,current));};
        return level.isClientSide?createTickerHelper(type,AlchemyModule.NITOR_TILE.get(),NitorBlockEntity::tick):null;
    }
    public static DyeColor dye(String id){String color=id.startsWith("nitor_")?id.substring(6):"yellow";if(color.equals("silver"))color="light_gray";if(color.equals("lightblue"))color="light_blue";return DyeColor.byName(color,DyeColor.YELLOW);}
}
