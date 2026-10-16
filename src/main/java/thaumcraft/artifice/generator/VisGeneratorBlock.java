package thaumcraft.artifice.generator;

import java.util.List;
import net.minecraft.core.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/** The released six-facing, inverse-redstone push generator has no inventory or menu. */
public final class VisGeneratorBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.FACING;
    public static final BooleanProperty ENABLED=BooleanProperty.create("enabled");
    public VisGeneratorBlock(){super(Properties.of().mapColor(net.minecraft.world.level.material.MapColor.WOOD).sound(SoundType.WOOD).strength(2,12).ignitedByLava().noOcclusion().isRedstoneConductor((s,l,p)->false).isSuffocating((s,l,p)->false).isViewBlocking((s,l,p)->false));registerDefaultState(stateDefinition.any().setValue(FACING,Direction.DOWN).setValue(ENABLED,true));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING,ENABLED);}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new VisGeneratorBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide?null:createTickerHelper(type,VisGeneratorModule.BLOCK_ENTITY.get(),VisGeneratorBlockEntity::tick);}
    private static boolean loaded(Level level,BlockPos pos){return !level.isOutsideBuildHeight(pos)&&(level instanceof net.minecraft.server.level.ServerLevel s?VisGeneratorModule.loaded(s,pos):level.hasChunkAt(pos));}
    public static Direction legacyFacing(BlockPos pos,LivingEntity placer){
        if(Math.abs(placer.getX()-(pos.getX()+.5))<2&&Math.abs(placer.getZ()-(pos.getZ()+.5))<2){double eye=placer.getEyeY();if(eye-pos.getY()>2)return Direction.UP;if(pos.getY()-eye>0)return Direction.DOWN;}
        return placer.getDirection().getOpposite();
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){Level level=context.getLevel();BlockPos pos=context.getClickedPos();
        for(Direction direction:Direction.values()){BlockPos at=pos.relative(direction);if(!loaded(level,at))continue;BlockEntity tile=level.getBlockEntity(at);if(tile==null)continue;
            try{var capability=tile.getCapability(ForgeCapabilities.ENERGY,direction.getOpposite());var receiver=capability.resolve().orElse(null);
                if(receiver!=null&&receiver.canReceive()&&capability.isPresent()&&loaded(level,at)&&level.getBlockEntity(at)==tile&&!tile.isRemoved())return defaultBlockState().setValue(FACING,direction);
            }catch(RuntimeException brokenEndpoint){/* A foreign capability cannot make physical placement crash. */}
        }
        var placer=context.getPlayer();Direction face=placer==null?context.getClickedFace():legacyFacing(pos,placer);if(placer!=null&&placer.isShiftKeyDown())face=face.getOpposite();return defaultBlockState().setValue(FACING,face);
    }
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return switch(state.getValue(FACING)){
        case DOWN->box(4,0,4,12,14,12);case UP->box(4,2,4,12,16,12);case NORTH->box(4,4,0,12,12,14);case SOUTH->box(4,4,2,12,12,16);case WEST->box(0,4,4,14,12,12);case EAST->box(2,4,4,16,12,12);};}
    @Override public VoxelShape getBlockSupportShape(BlockState state,BlockGetter level,BlockPos pos){return Shapes.empty();}
    @Override public BlockState rotate(BlockState state,Rotation rotation){return state.setValue(FACING,rotation.rotate(state.getValue(FACING)));}
    @Override public BlockState mirror(BlockState state,Mirror mirror){return rotate(state,mirror.getRotation(state.getValue(FACING)));}
    private static void signal(Level level,BlockPos pos,BlockState state){if(level.isClientSide)return;boolean enabled=!level.hasNeighborSignal(pos);if(enabled!=state.getValue(ENABLED))level.setBlock(pos,state.setValue(ENABLED,enabled),Block.UPDATE_ALL);}
    @Override public void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moving){super.onPlace(state,level,pos,old,moving);signal(level,pos,state);}
    @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,Block block,BlockPos from,boolean moving){signal(level,pos,state);}
    @Override public List<ItemStack> getDrops(BlockState state,LootParams.Builder builder){return List.of(new ItemStack(this));}
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random){DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->thaumcraft.artifice.generator.client.GeneratorClientEffects.animate(level,pos,state,random));}
}
