package thaumcraft.artifice.furnace;

import java.util.List;
import net.minecraft.core.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.*;
import net.minecraft.world.level.storage.loot.LootParams;

public final class InfernalFurnaceBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE=Block.box(0,0,0,16,8,16);
    public InfernalFurnaceBlock(){super(Properties.of().strength(2,12).sound(SoundType.STONE).lightLevel(s->13).noOcclusion().pushReaction(PushReaction.BLOCK));registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING);}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return SHAPE;}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){return defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite());}
    @Override public BlockState rotate(BlockState state,Rotation rotation){return state.setValue(FACING,rotation.rotate(state.getValue(FACING)));}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new InfernalFurnaceBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide?null:createTickerHelper(type,InfernalFurnaceModule.FURNACE.get(),InfernalFurnaceBlockEntity::tick);}
    @Override public List<ItemStack> getDrops(BlockState state,LootParams.Builder context){return List.of();}
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState replacement,boolean moving){if(!state.is(replacement.getBlock())&&!level.isClientSide&&!level.restoringBlockSnapshots){if(level.getBlockEntity(pos) instanceof InfernalFurnaceBlockEntity furnace)furnace.releaseInputs();InfernalFurnaceFormation.destroy(level,pos,state,pos);}super.onRemove(state,level,pos,replacement,moving);}
    @Override public void entityInside(BlockState state,Level level,BlockPos pos,Entity entity){var movement=entity.getDeltaMovement();double x=entity.getX()<pos.getX()+.3?1e-4:entity.getX()>pos.getX()+.7?-1e-4:0;double z=entity.getZ()<pos.getZ()+.3?1e-4:entity.getZ()>pos.getZ()+.7?-1e-4:0;entity.setDeltaMovement(movement.add(x,0,z));if(!level.isClientSide&&entity.tickCount%10==0){if(entity instanceof ItemEntity item){item.setDeltaMovement(item.getDeltaMovement().x,.02500000037252903,item.getDeltaMovement().z);if(level.getBlockEntity(pos) instanceof InfernalFurnaceBlockEntity furnace)furnace.collide(item);}else if(entity instanceof LivingEntity&&!entity.fireImmune()){entity.hurt(level.damageSources().lava(),3);entity.setSecondsOnFire(10);}}}
}
