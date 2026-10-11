package thaumcraft.world.taint;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.*;
import thaumcraft.world.aura.AuraManager;

public final class TaintFeatureBlock extends Block implements ITaintBlock {
    public static final DirectionProperty FACING=BlockStateProperties.FACING;
    public TaintFeatureBlock() {
        super(Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(.1F).sound(SoundType.STONE).noOcclusion().randomTicks().lightLevel(state->9));
        // BETA26 discards withProperty(UP), retaining the property's first value (DOWN).
        registerDefaultState(defaultBlockState().setValue(FACING,Direction.DOWN));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(FACING,context.getClickedFace()); }
    @Override public VoxelShape getBlockSupportShape(BlockState state,BlockGetter level,BlockPos pos) { return Shapes.empty(); }
    @Override public boolean canSurvive(BlockState state,LevelReader level,BlockPos pos) {
        var facing=state.getValue(FACING);var support=pos.relative(facing.getOpposite());
        return TaintModule.loaded(level,support)&&level.getBlockState(support).isFaceSturdy(level,support,facing);
    }
    @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,Block block,BlockPos other,boolean moving) {
        if(!level.isClientSide&&level.getBlockState(pos).is(this)&&!canSurvive(state,level,pos)) level.removeBlock(pos,false);
    }
    @Override public void die(ServerLevel level,BlockPos pos,BlockState state) { level.setBlockAndUpdate(pos,TaintModule.block("flux_goo").defaultBlockState()); }
    @Override public void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random) {
        if(!level.getServer().isSameThread()||!TaintModule.loaded(level,pos)||!level.getBlockState(pos).is(this)) return;
        if(!TaintHelper.isNearTaintSeed(level,pos)&&random.nextInt(10)==0) { die(level,pos,state);return; }
        TaintHelper.spreadFibres(level,pos);
        if(TaintModule.loaded(level,pos.below())) {
            var below=level.getBlockState(pos.below());
            if(below.is(TaintModule.block("taint_log"))&&below.getValue(TaintLogBlock.AXIS)==Direction.Axis.Y&&level.random.nextInt(100)==0)
                level.setBlockAndUpdate(pos,TaintModule.block("taint_geyser").defaultBlockState());
        }
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState replacement,boolean moving) {
        if(!replacement.is(this)&&level instanceof ServerLevel server) {
            if(level.random.nextFloat()<.333F) {
                var crawler=TaintModule.activeMob("taint_crawler",server);
                if(crawler!=null) { crawler.moveTo(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,level.random.nextInt(360),0);server.addFreshEntity(crawler); }
            } else AuraManager.addFlux(server,pos,1);
        }
        super.onRemove(state,level,pos,replacement,moving);
    }
    @Override public List<ItemStack> getDrops(BlockState state,LootParams.Builder builder) {
        var tool=builder.getOptionalParameter(LootContextParams.TOOL);
        return tool!=null&&EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH,tool)>0?List.of(new ItemStack(this)):List.of();
    }
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {
        return switch(state.getValue(FACING)) {
            case DOWN -> Block.box(2,10,2,14,16,14);
            case UP -> Block.box(2,0,2,14,6,14);
            case NORTH -> Block.box(2,2,10,14,14,16);
            case SOUTH -> Block.box(2,2,0,14,14,6);
            case WEST -> Block.box(10,2,2,16,14,14);
            case EAST -> Block.box(0,2,2,6,14,14);
        };
    }
}
