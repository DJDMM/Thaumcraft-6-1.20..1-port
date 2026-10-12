package thaumcraft.artifice.lamp;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** BETA26 persistent invisible light15; non-air storage prevents modern empty-section elision. */
public final class GlimmerBlock extends Block {
    public GlimmerBlock(){super(Properties.of().replaceable().noCollission().noOcclusion().strength(0,999).lightLevel(s->15).noLootTable());}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.INVISIBLE;}
    @Override public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Shapes.empty();}
    @Override public VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Shapes.empty();}
    @Override public ItemStack getCloneItemStack(BlockState s,HitResult target,BlockGetter l,BlockPos p,Player player){return ItemStack.EMPTY;}
}
