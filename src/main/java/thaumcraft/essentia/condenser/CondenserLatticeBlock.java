package thaumcraft.essentia.condenser;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;
import thaumcraft.alchemy.AspectCrystalItem;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.catalog.CatalogModule;
import thaumcraft.catalog.blocks.CatalogBlocks;

/** Original six connected rods and first reachable condenser support, with deferred paid placement. */
public final class CondenserLatticeBlock extends Block {
    public static final BooleanProperty DOWN=BooleanProperty.create("down"),UP=BooleanProperty.create("up"),NORTH=BooleanProperty.create("north"),SOUTH=BooleanProperty.create("south"),WEST=BooleanProperty.create("west"),EAST=BooleanProperty.create("east");
    private static final BooleanProperty[] CONNECTIONS={DOWN,UP,NORTH,SOUTH,WEST,EAST};
    private static final Set<Level> CLEANING=Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Map<Level,BlockPos> CLEAN_PENDING=new IdentityHashMap<>();
    private final boolean dirty;
    public CondenserLatticeBlock(boolean dirty){super(Properties.copy(Blocks.IRON_BLOCK).strength(.5F,3).noOcclusion().sound(SoundType.METAL).lightLevel(s->dirty?0:4)
            .isValidSpawn((s,l,p,t)->false).isSuffocating((s,l,p)->false).isViewBlocking((s,l,p)->false));this.dirty=dirty;BlockState state=stateDefinition.any();for(BooleanProperty property:CONNECTIONS)state=state.setValue(property,false);registerDefaultState(state);}
    public boolean dirty(){return dirty;}
    static boolean cleaningUncommitted(Level level,BlockPos pos){return !level.isClientSide&&pos.equals(CLEAN_PENDING.get(level));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(CONNECTIONS);}
    private static boolean connects(BlockGetter level,BlockPos pos,Direction face){BlockPos target=pos.relative(face);if(level instanceof Level world&&!CondenserBlockEntity.loaded(world,target))return false;Block block=level.getBlockState(target).getBlock();return block instanceof CondenserLatticeBlock||face==Direction.DOWN&&block instanceof CondenserBlock;}
    private BlockState connections(BlockState state,BlockGetter level,BlockPos pos){for(Direction direction:Direction.values())state=state.setValue(CONNECTIONS[direction.get3DDataValue()],connects(level,pos,direction));return state;}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){return connections(defaultBlockState(),context.getLevel(),context.getClickedPos());}
    @Override public BlockState updateShape(BlockState state,Direction face,BlockState neighbor,LevelAccessor level,BlockPos pos,BlockPos from){return level instanceof Level world&&CondenserBlockItem.uncommitted(world,pos)?state:connections(state,level,pos);}
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context){double minX=5,maxX=11,minY=5,maxY=11,minZ=5,maxZ=11;for(Direction side:Direction.values())if(connects(level,pos,side)){switch(side){case DOWN->minY=0;case UP->maxY=16;case NORTH->minZ=0;case SOUTH->maxZ=16;case WEST->minX=0;case EAST->maxX=16;}}return box(minX,minY,minZ,maxX,maxY,maxZ);}
    @Override public VoxelShape getBlockSupportShape(BlockState state,BlockGetter level,BlockPos pos){return Shapes.empty();}
    @Override public void onPlace(BlockState state,Level level,BlockPos pos,BlockState previous,boolean moving){super.onPlace(state,level,pos,previous,moving);if(!level.captureBlockSnapshots)triggerUpdate(level,pos);}
    @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,Block neighbor,BlockPos from,boolean moving){if(neighbor instanceof CondenserLatticeBlock||neighbor instanceof CondenserBlock)triggerUpdate(level,pos);}
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){super.onRemove(state,level,pos,next,moving);if(state.getBlock()!=next.getBlock()&&!level.captureBlockSnapshots&&!level.restoringBlockSnapshots)for(Direction side:Direction.values()){BlockPos p=pos.relative(side);if(CondenserBlockEntity.loaded(level,p)&&level.getBlockState(p).getBlock() instanceof CondenserLatticeBlock)triggerUpdate(level,p);}}
    @Override public List<ItemStack> getDrops(BlockState state,LootParams.Builder params){return List.of(new ItemStack(this));}
    public static void triggerUpdate(Level level,BlockPos pos){if(level.restoringBlockSnapshots||level.captureBlockSnapshots||CondenserBlockItem.uncommitted(level,pos)||!CondenserBlockEntity.loaded(level,pos))return;
        if(level.getBlockState(pos).getBlock() instanceof CondenserLatticeBlock lattice){BlockState physical=level.getBlockState(pos),connected=lattice.connections(physical,level,pos);if(!physical.equals(connected))level.setBlock(pos,connected,2);}
        Support support=findSupport(level,pos);if(support.pos!=null&&level.getBlockEntity(support.pos) instanceof CondenserBlockEntity tile)tile.invalidateLattice();
        if(!level.isClientSide&&level.getBlockState(pos).getBlock() instanceof CondenserLatticeBlock lattice)level.scheduleTick(pos,lattice,1);}
    private record Support(BlockPos pos,boolean unknown){}
    private static final class Frame {final BlockPos pos;int side;Frame(BlockPos pos){this.pos=pos;}}
    // Iterative DFS retains DOWN,UP,NORTH,SOUTH,WEST,EAST and avoids recursion overflow on long structures.
    private static Support findSupport(Level level,BlockPos start){Set<BlockPos> history=new HashSet<>();ArrayDeque<Frame> stack=new ArrayDeque<>();history.add(start);stack.push(new Frame(start));boolean unknown=false;
        while(!stack.isEmpty()){Frame frame=stack.peek();if(frame.side==6){stack.pop();continue;}Direction side=Direction.values()[frame.side++];BlockPos p=frame.pos.relative(side);if(history.contains(p)||level.isOutsideBuildHeight(p))continue;if(!CondenserBlockEntity.loaded(level,p)){unknown=true;continue;}Block block=level.getBlockState(p).getBlock();
            if(block instanceof CondenserLatticeBlock){history.add(p);stack.push(new Frame(p));continue;}if(side==Direction.DOWN&&block instanceof CondenserBlock)return new Support(p,unknown);
        }return new Support(null,unknown);}
    @Override public void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){if(level.captureBlockSnapshots||level.restoringBlockSnapshots||CondenserBlockItem.uncommitted(level,pos)||!level.getBlockState(pos).is(this))return;Support support=findSupport(level,pos);if(support.unknown){level.scheduleTick(pos,this,20);return;}if(support.pos!=null&&level.getBlockEntity(support.pos) instanceof CondenserBlockEntity tile)tile.invalidateLattice();
        if(support.pos==null||support.pos.distSqr(pos)>74){BlockState physical=level.getBlockState(pos);if(physical.getBlock()!=this)return;
            // Transfer ownership before notifying neighbor callbacks; canceled item joins stay in SavedData.
            if(level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)){
                // Released Block.spawnAsEntity uses .25 + nextFloat*.5 on each axis and default pickup delay10.
                level.random.nextFloat(); // Original chance1 drop still makes its chance roll.
                double x=pos.getX()+.25+level.random.nextFloat()*.5F,y=pos.getY()+.25+level.random.nextFloat()*.5F,z=pos.getZ()+.25+level.random.nextFloat()*.5F;
                CondenserDropData.get(level).add(pos,x,y,z,new ItemStack(this),10);
            }level.removeBlock(pos,false);
        }}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){if(level.isClientSide)return InteractionResult.SUCCESS;
        if(!dirty||!(level instanceof ServerLevel server)||level.captureBlockSnapshots||level.restoringBlockSnapshots||!level.capturedBlockSnapshots.isEmpty()||CondenserBlockItem.uncommitted(level,pos)||player.level()!=level||player.isRemoved()||!player.mayBuild()||player.isSpectator()||!level.mayInteract(player,pos))return InteractionResult.PASS;
        ItemStack physical=player.getItemInHand(hand);if(physical.isEmpty()||!physical.is(CatalogModule.ENTRIES.get("filter").get())||!player.mayUseItemAt(pos,hit.getDirection(),physical)||!CLEANING.add(level))return InteractionResult.PASS;
        int count=physical.getCount();CompoundTag nbt=physical.getTag()==null?null:physical.getTag().copy();BlockSnapshot snapshot=BlockSnapshot.create(level.dimension(),level,pos);BlockState target=connections(CatalogBlocks.block("condenser_lattice").defaultBlockState(),level,pos);
        boolean capture=level.captureBlockSnapshots,committed=false;int start=level.capturedBlockSnapshots.size();level.captureBlockSnapshots=true;List<BlockSnapshot> owned=new ArrayList<>();
        try{if(level.getBlockState(pos).getBlock()!=this)return InteractionResult.FAIL;boolean installed;try{installed=level.setBlock(pos,target,3);}finally{owned.addAll(level.capturedBlockSnapshots.subList(start,level.capturedBlockSnapshots.size()));}if(!installed)return InteractionResult.FAIL;
            CLEAN_PENDING.put(level,pos.immutable());boolean canceled;level.captureBlockSnapshots=false;try{canceled=ForgeEventFactory.onBlockPlace(player,snapshot,hit.getDirection());}finally{level.captureBlockSnapshots=true;}if(canceled)return InteractionResult.FAIL;
            if(player.level()!=level||player.isRemoved()||player.getItemInHand(hand)!=physical||physical.getCount()!=count||!Objects.equals(nbt,physical.getTag())||!level.getBlockState(pos).equals(target)||!player.mayBuild()||player.isSpectator()||!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,hit.getDirection(),physical))return InteractionResult.FAIL;
            // BETA26 consumes one physical filter even in creative.
            physical.shrink(1);if(level.random.nextBoolean())CondenserDropData.get(server).add(pos,pos.getX()+.5+hit.getDirection().getStepX()/3.0,pos.getY()+.5,pos.getZ()+.5+hit.getDirection().getStepZ()/3.0,AspectCrystalItem.create(Aspect.FLUX));
            committed=true;level.captureBlockSnapshots=false;CLEAN_PENDING.remove(level);
            target.onPlace(level,pos,snapshot.getReplacedBlock(),false);level.markAndNotifyBlock(pos,level.getChunkAt(pos),snapshot.getReplacedBlock(),target,3,512);
            level.playSound(null,pos,SoundEvents.ITEM_PICKUP,SoundSource.BLOCKS,.2F,((level.random.nextFloat()-level.random.nextFloat())*.7F+1)*1.6F);triggerUpdate(level,pos);return InteractionResult.CONSUME;
        }finally{if(!committed&&level.getBlockState(pos).is(target.getBlock())){boolean restoring=level.restoringBlockSnapshots;level.restoringBlockSnapshots=true;level.captureBlockSnapshots=false;try{snapshot.restore(true,false);}finally{level.restoringBlockSnapshots=restoring;}}
            for(BlockSnapshot ours:owned)level.capturedBlockSnapshots.removeIf(candidate->candidate==ours);level.captureBlockSnapshots=capture;CLEAN_PENDING.remove(level);CLEANING.remove(level);}
    }
}
