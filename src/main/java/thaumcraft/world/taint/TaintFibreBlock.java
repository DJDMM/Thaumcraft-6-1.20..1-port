package thaumcraft.world.taint;

import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.*;
import thaumcraft.alchemy.AspectCrystalItem;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.world.aura.AuraManager;
import thaumcraft.world.crystal.PrimalCrystalClusterBlock;

/** Position-seeded BETA26 fibre faces, growths, support, harvest and decay. */
public final class TaintFibreBlock extends Block implements ITaintBlock {
    public static final BooleanProperty NORTH=BooleanProperty.create("north"),SOUTH=BooleanProperty.create("south"),EAST=BooleanProperty.create("east"),WEST=BooleanProperty.create("west"),UP=BooleanProperty.create("up"),DOWN=BooleanProperty.create("down");
    public static final BooleanProperty GROWTH1=BooleanProperty.create("growth1"),GROWTH2=BooleanProperty.create("growth2"),GROWTH3=BooleanProperty.create("growth3"),GROWTH4=BooleanProperty.create("growth4");
    public TaintFibreBlock() {
        super(Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(1).sound(TaintModule.SOUND).noOcclusion().dynamicShape().replaceable().randomTicks()
                .lightLevel(state->state.getValue(GROWTH3)?12:(state.getValue(GROWTH2)||state.getValue(GROWTH4)?6:0)));
        registerDefaultState(defaultBlockState().setValue(NORTH,false).setValue(SOUTH,false).setValue(EAST,false).setValue(WEST,false).setValue(UP,false).setValue(DOWN,false)
                .setValue(GROWTH1,false).setValue(GROWTH2,false).setValue(GROWTH3,false).setValue(GROWTH4,false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(NORTH,SOUTH,EAST,WEST,UP,DOWN,GROWTH1,GROWTH2,GROWTH3,GROWTH4); }
    private static BooleanProperty face(Direction dir) { return switch(dir) {case DOWN->DOWN;case UP->UP;case NORTH->NORTH;case SOUTH->SOUTH;case WEST->WEST;case EAST->EAST;}; }
    private static boolean drawAt(BlockGetter level,BlockPos neighbor,Direction face) {
        if(!TaintModule.loaded(level,neighbor)) return false;
        var state=level.getBlockState(neighbor);
        return !(state.getBlock() instanceof TaintFibreBlock)&&!(state.getBlock() instanceof TaintFeatureBlock)
                &&!state.is(TaintModule.block("flux_goo"))
                &&state.isFaceSturdy(level,neighbor,face.getOpposite());
    }
    public static BlockState actualState(BlockState state,BlockGetter level,BlockPos pos) {
        for(Direction dir:Direction.values()) state=state.setValue(face(dir),drawAt(level,pos.relative(dir),dir));
        int growth=0,q=new Random(PrimalCrystalClusterBlock.originalPositionLong(pos)).nextInt(50);
        if(state.getValue(DOWN)) { if(q<4) growth=1;else if(q==4||q==5) growth=2;else if(q==6) growth=3; }
        if(state.getValue(UP)&&q>47) growth=4;
        return state.setValue(GROWTH1,growth==1).setValue(GROWTH2,growth==2).setValue(GROWTH3,growth==3).setValue(GROWTH4,growth==4);
    }
    private static boolean growth(BlockState state) { return state.getValue(GROWTH1)||state.getValue(GROWTH2)||state.getValue(GROWTH3)||state.getValue(GROWTH4); }
    public static boolean isOnlyAdjacentToTaint(BlockGetter level,BlockPos pos) {
        for(Direction dir:Direction.values()) {
            var other=pos.relative(dir);
            // Unknown neighboring terrain must not be treated as unsupported air.
            if(!TaintModule.loaded(level,other)) return false;
            var state=level.getBlockState(other);
            if(!state.isAir()&&!TaintHelper.isTainted(state)&&state.isFaceSturdy(level,other,dir.getOpposite())) return false;
        }
        return true;
    }
    public static boolean isHemmedByTaint(BlockGetter level,BlockPos pos) {
        int count=0;
        for(Direction dir:Direction.values()) {
            var other=pos.relative(dir);if(!TaintModule.loaded(level,other)) return false;
            var state=level.getBlockState(other);
            if(TaintHelper.isTainted(state)) count++;
            else if(state.isAir()||(state.getFluidState().isEmpty()&&!state.isFaceSturdy(level,other,dir.getOpposite()))) count--;
        }
        return count>0;
    }
    @Override public void die(ServerLevel level,BlockPos pos,BlockState state) { level.removeBlock(pos,false); }
    @Override public void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moving) {
        super.onPlace(state,level,pos,old,moving);
        if(!level.isClientSide&&!old.is(this)) refresh(level,pos,state);
    }
    private void refresh(Level level,BlockPos pos,BlockState state) {
        var actual=actualState(state,level,pos);
        if(!actual.equals(level.getBlockState(pos))) level.setBlock(pos,actual,2);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return actualState(defaultBlockState(),context.getLevel(),context.getClickedPos()); }
    @Override public BlockState updateShape(BlockState state,Direction dir,BlockState other,LevelAccessor level,BlockPos pos,BlockPos otherPos) { return actualState(state,level,pos); }
    @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,Block block,BlockPos other,boolean moving) {
        if(level.isClientSide||!level.getBlockState(pos).is(this)) return;
        var actual=actualState(state,level,pos);
        if(!growth(actual)&&isOnlyAdjacentToTaint(level,pos)) level.removeBlock(pos,false);
        else refresh(level,pos,actual);
    }
    @Override public void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random) {
        if(!level.getServer().isSameThread()||!TaintModule.loaded(level,pos)||!level.getBlockState(pos).is(this)) return;
        var actual=actualState(state,level,pos);
        if((!growth(actual)&&isOnlyAdjacentToTaint(level,pos))||!TaintHelper.isNearTaintSeed(level,pos)) die(level,pos,actual);
        else { refresh(level,pos,state);TaintHelper.spreadFibres(level,pos); }
    }
    @Override public VoxelShape getBlockSupportShape(BlockState state,BlockGetter level,BlockPos pos) { return Shapes.empty(); }
    private VoxelShape faces(BlockState state,BlockGetter level,BlockPos pos) {
        state=actualState(state,level,pos);VoxelShape shape=Shapes.empty();
        if(state.getValue(UP)) shape=Shapes.or(shape,Block.box(0,15.2,0,16,16,16));
        if(state.getValue(DOWN)) shape=Shapes.or(shape,Block.box(0,0,0,16,.8,16));
        if(state.getValue(EAST)) shape=Shapes.or(shape,Block.box(15.2,0,0,16,16,16));
        if(state.getValue(WEST)) shape=Shapes.or(shape,Block.box(0,0,0,.8,16,16));
        if(state.getValue(SOUTH)) shape=Shapes.or(shape,Block.box(0,0,15.2,16,16,16));
        if(state.getValue(NORTH)) shape=Shapes.or(shape,Block.box(0,0,0,16,16,.8));
        return shape;
    }
    @Override public VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) { return faces(state,level,pos); }
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {
        VoxelShape shape=faces(state,level,pos);state=actualState(state,level,pos);
        if(state.getValue(GROWTH1)) shape=Shapes.or(shape,Block.box(1.6,0,1.6,14.4,6.4,14.4));
        else if(state.getValue(GROWTH2)) shape=Shapes.or(shape,Block.box(3.2,0,3.2,12.8,16,12.8));
        else if(state.getValue(GROWTH3)) shape=Shapes.or(shape,Block.box(4,0,4,12,5,12));
        else if(state.getValue(GROWTH4)) shape=Shapes.or(shape,Block.box(1.6,4.8,1.6,14.4,16,14.4));
        return shape;
    }
    @Override public int getLightEmission(BlockState state,BlockGetter level,BlockPos pos) {
        state=actualState(state,level,pos);return state.getValue(GROWTH3)?12:(state.getValue(GROWTH2)||state.getValue(GROWTH4)?6:0);
    }
    @Override public int getFlammability(BlockState state,BlockGetter level,BlockPos pos,Direction face) { return 3; }
    @Override public int getFireSpreadSpeed(BlockState state,BlockGetter level,BlockPos pos,Direction face) { return 3; }
    @Override public List<ItemStack> getDrops(BlockState state,LootParams.Builder builder) {
        var origin=builder.getOptionalParameter(LootContextParams.ORIGIN);
        if(origin==null||!actualState(state,builder.getLevel(),BlockPos.containing(origin)).getValue(GROWTH3)) return List.of();
        var tool=builder.getOptionalParameter(LootContextParams.TOOL);
        int fortune=tool==null?0:EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_FORTUNE,tool);
        return builder.getLevel().random.nextInt(5)<=fortune?List.of(AspectCrystalItem.create(Aspect.FLUX)):List.of();
    }
    @Override public void spawnAfterBreak(BlockState state,ServerLevel level,BlockPos pos,ItemStack tool,boolean dropExperience) {
        if(actualState(state,level,pos).getValue(GROWTH3)) AuraManager.addFlux(level,pos,1);
        super.spawnAfterBreak(state,level,pos,tool,dropExperience);
    }
    @Override public void stepOn(Level level,BlockPos pos,BlockState state,Entity entity) { TaintBlock.contact(level,entity,750);super.stepOn(level,pos,state,entity); }
    @Override public boolean triggerEvent(BlockState state,Level level,BlockPos pos,int id,int parameter) {
        if(id==1) { if(level.isClientSide) level.playLocalSound(pos,SoundEvents.CHORUS_FLOWER_DEATH,SoundSource.BLOCKS,.1F,.9F+level.random.nextFloat()*.2F,false);return true; }
        return super.triggerEvent(state,level,pos,id,parameter);
    }
}
