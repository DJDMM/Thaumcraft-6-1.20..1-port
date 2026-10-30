package thaumcraft.world.voidsiphon;
import java.util.List;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.network.NetworkHooks;
public final class VoidSiphonBlock extends BaseEntityBlock {
    public static final BooleanProperty ENABLED=BooleanProperty.create("enabled");
    private static final VoxelShape OUTLINE=box(3,0,3,13,16,13);
    private static final VoxelShape COLLISION=Shapes.or(box(3,0,3,13,2,13),box(4,2,4,12,11,12),box(5,12,5,10,16,10));
    public VoidSiphonBlock(){super(Properties.of().mapColor(net.minecraft.world.level.material.MapColor.METAL).strength(2,10).sound(SoundType.METAL).noOcclusion()
        .isValidSpawn((s,l,p,t)->false).isRedstoneConductor((s,l,p)->false).isSuffocating((s,l,p)->false).isViewBlocking((s,l,p)->false));registerDefaultState(stateDefinition.any().setValue(ENABLED,true));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(ENABLED);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(ENABLED,!Boolean.TRUE.equals(power(c.getLevel(),c.getClickedPos())));}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return OUTLINE;}
    @Override public VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return COLLISION;}
    @Override public VoxelShape getBlockSupportShape(BlockState s,BlockGetter l,BlockPos p){return Shapes.empty();}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new VoidSiphonBlockEntity(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return createTickerHelper(t,VoidSiphonModule.BLOCK_ENTITY.get(),VoidSiphonBlockEntity::tick);}
    @Override public void onPlace(BlockState s,Level l,BlockPos p,BlockState old,boolean moving){super.onPlace(s,l,p,old,moving);updatePower(s,l,p);}
    @Override public void neighborChanged(BlockState s,Level l,BlockPos p,Block b,BlockPos from,boolean moving){updatePower(s,l,p);}
    private static Boolean power(Level l,BlockPos p){for(int x=-3;x<=3;x++)for(int y=-3;y<=3;y++)for(int z=-3;z<=3;z++)if(Math.abs(x)+Math.abs(y)+Math.abs(z)<=3){var q=p.offset(x,y,z);if(!l.isOutsideBuildHeight(q)&&!VoidSiphonBlockEntity.loaded(l,q))return null;}return l.hasNeighborSignal(p);}
    private static void updatePower(BlockState s,Level l,BlockPos p){if(l.isClientSide||l.captureBlockSnapshots||l.restoringBlockSnapshots||!s.is(l.getBlockState(p).getBlock()))return;var owner=l.getBlockEntity(p);var powered=power(l,p);if(powered!=null&&l.getBlockEntity(p)==owner&&l.getBlockState(p).equals(s)&&s.getValue(ENABLED)==powered)l.setBlock(p,s.setValue(ENABLED,!powered),3);}
    @Override public InteractionResult use(BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){if(player instanceof ServerPlayer server&&l.getBlockEntity(p) instanceof VoidSiphonBlockEntity tile)NetworkHooks.openScreen(server,tile,p);return InteractionResult.sidedSuccess(l.isClientSide);}
    @Override public List<ItemStack> getDrops(BlockState s,LootParams.Builder params){return List.of(new ItemStack(this));}
    @Override public void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving){if(s.getBlock()!=next.getBlock()&&!l.restoringBlockSnapshots&&l.getBlockEntity(p) instanceof VoidSiphonBlockEntity tile){Containers.dropContents(l,p,tile);l.updateNeighbourForOutputSignal(p,this);}super.onRemove(s,l,p,next,moving);}
}
