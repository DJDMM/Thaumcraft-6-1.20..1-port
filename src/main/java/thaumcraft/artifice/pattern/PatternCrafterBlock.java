package thaumcraft.artifice.pattern;

import java.util.List;
import net.minecraft.core.*;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;

/** Horizontal, inverse-redstone device with the original small physical selector. */
public final class PatternCrafterBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ENABLED=BooleanProperty.create("enabled");
    public PatternCrafterBlock(){super(Properties.of().mapColor(net.minecraft.world.level.material.MapColor.METAL).sound(SoundType.METAL).strength(2,12).noOcclusion().isRedstoneConductor((s,l,p)->false).isSuffocating((s,l,p)->false).isViewBlocking((s,l,p)->false));registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(ENABLED,true));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING,ENABLED);}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new PatternCrafterBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return createTickerHelper(type,PatternCrafterModule.BLOCK_ENTITY.get(),PatternCrafterBlockEntity::tick);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){return defaultBlockState().setValue(FACING,context.getHorizontalDirection());}
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return Shapes.block();}
    @Override public VoxelShape getBlockSupportShape(BlockState state,BlockGetter level,BlockPos pos){return Shapes.empty();}
    @Override public BlockState rotate(BlockState state,Rotation rotation){return state.setValue(FACING,rotation.rotate(state.getValue(FACING)));}
    @Override public BlockState mirror(BlockState state,Mirror mirror){return rotate(state,mirror.getRotation(state.getValue(FACING)));}
    public static AABB selector(Direction facing){return switch(facing){case EAST->new AABB(.125,.125,.375,.25,.375,.625);case NORTH->new AABB(.375,.125,.75,.625,.375,.875);case SOUTH->new AABB(.375,.125,.125,.625,.375,.25);default->new AABB(.75,.125,.375,.875,.375,.625);};}
    /** Ray uses the player's eyes rather than trusting an arbitrary client hit coordinate. */
    public static boolean targetsSelector(Player player,BlockPos pos,BlockState state){Vec3 start=player.getEyePosition(),end=start.add(player.getLookAngle().scale(player.getBlockReach()));return selector(state.getValue(FACING)).move(pos).clip(start,end).isPresent();}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(!targetsSelector(player,pos,state)||!(level.getBlockEntity(pos) instanceof PatternCrafterBlockEntity tile))return InteractionResult.PASS;
        if(!level.isClientSide){tile.cycle();var sound=ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("thaumcraft","key"));if(sound!=null)level.playSound(null,pos,sound,SoundSource.BLOCKS,.5f,1);}
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    private static void signal(Level level,BlockPos pos,BlockState state){if(level.isClientSide)return;boolean enabled=!level.hasNeighborSignal(pos);if(enabled!=state.getValue(ENABLED))level.setBlock(pos,state.setValue(ENABLED,enabled),Block.UPDATE_ALL);}
    @Override public void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moving){super.onPlace(state,level,pos,old,moving);signal(level,pos,state);}
    @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,Block block,BlockPos from,boolean moving){signal(level,pos,state);}
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){if(!state.is(next.getBlock())&&level.getBlockEntity(pos) instanceof PatternCrafterBlockEntity tile)tile.releasePaidOutputs();super.onRemove(state,level,pos,next,moving);}
    @Override public List<ItemStack> getDrops(BlockState state,LootParams.Builder builder){return List.of(new ItemStack(this));}
}
