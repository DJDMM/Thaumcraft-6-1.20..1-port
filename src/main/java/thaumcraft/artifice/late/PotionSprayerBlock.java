package thaumcraft.artifice.late;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
public final class PotionSprayerBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.FACING;public static final BooleanProperty ENABLED=BooleanProperty.create("enabled");
    public PotionSprayerBlock(){super(Properties.copy(Blocks.IRON_BLOCK).strength(3));registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(ENABLED,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,ENABLED);}
    @Override public RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new PotionSprayerBlockEntity(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){return createTickerHelper(type,LateDevicesModule.SPRAYER.get(),PotionSprayerBlockEntity::tick);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){var d=c.getNearestLookingDirection().getOpposite();if(c.getPlayer()!=null&&c.getPlayer().isShiftKeyDown())d=d.getOpposite();return defaultBlockState().setValue(FACING,d).setValue(ENABLED,!c.getLevel().hasNeighborSignal(c.getClickedPos()));}
    private void power(Level l,BlockPos p,BlockState s){if(!l.isClientSide&&!l.restoringBlockSnapshots&&!l.captureBlockSnapshots&&s.getValue(ENABLED)==l.hasNeighborSignal(p))l.setBlock(p,s.setValue(ENABLED,!l.hasNeighborSignal(p)),3);}
    @Override public void onPlace(BlockState s,Level l,BlockPos p,BlockState old,boolean moved){super.onPlace(s,l,p,old,moved);power(l,p,s);}
    @Override public void neighborChanged(BlockState s,Level l,BlockPos p,Block b,BlockPos from,boolean moved){power(l,p,s);}
    @Override public InteractionResult use(BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){if(player instanceof ServerPlayer server&&l.getBlockEntity(p) instanceof PotionSprayerBlockEntity tile)NetworkHooks.openScreen(server,tile,p);return InteractionResult.sidedSuccess(l.isClientSide);}
    @Override public void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moved){if(!s.is(next.getBlock())&&!l.isClientSide&&l.getBlockEntity(p) instanceof net.minecraft.world.Container c)Containers.dropContents(l,p,c);super.onRemove(s,l,p,next,moved);}
}
