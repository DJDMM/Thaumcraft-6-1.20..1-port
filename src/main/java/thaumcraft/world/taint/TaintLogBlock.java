package thaumcraft.world.taint;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootParams;

public final class TaintLogBlock extends Block implements ITaintBlock {
    public static final EnumProperty<Direction.Axis> AXIS=BlockStateProperties.AXIS;
    public TaintLogBlock() { super(Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(3,100).sound(TaintModule.SOUND).randomTicks());registerDefaultState(defaultBlockState().setValue(AXIS,Direction.Axis.Y)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(AXIS); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(AXIS,context.getClickedFace().getAxis()); }
    @Override public void die(ServerLevel level,BlockPos pos,BlockState state) { level.setBlockAndUpdate(pos,TaintModule.block("flux_goo").defaultBlockState()); }
    @Override public void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random) {
        if(!level.getServer().isSameThread()||!TaintModule.loaded(level,pos)||!level.getBlockState(pos).is(this)) return;
        if(!TaintHelper.isNearTaintSeed(level,pos)) die(level,pos,state);else TaintHelper.spreadFibres(level,pos);
    }
    @Override public int getFlammability(BlockState state,BlockGetter level,BlockPos pos,Direction face) { return 4; }
    @Override public int getFireSpreadSpeed(BlockState state,BlockGetter level,BlockPos pos,Direction face) { return 4; }
    @Override public List<ItemStack> getDrops(BlockState state,LootParams.Builder builder) { return List.of(new ItemStack(this)); }
}
