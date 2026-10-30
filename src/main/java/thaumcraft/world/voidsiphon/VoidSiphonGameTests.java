package thaumcraft.world.voidsiphon;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.*;
import thaumcraft.catalog.blocks.*;
import thaumcraft.catalog.entities.VisualEntitiesModule;
import thaumcraft.world.rift.FluxRiftEntity;

/** Direct native BE updates with declared initial progress/actors; no active RNG or clock resets. */
@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class VoidSiphonGameTests {
    private VoidSiphonGameTests(){}
    private static final class Trial implements AutoCloseable {
        final GameTestHelper h;final net.minecraft.server.level.ServerLevel level;final BlockPos pos;
        final Map<BlockPos,BlockState> old=new LinkedHashMap<>();final List<FluxRiftEntity> rifts=new ArrayList<>();
        final VoidSiphonBlockEntity tile;
        Trial(GameTestHelper h){this.h=h;level=h.getLevel();var base=h.absolutePos(new BlockPos(4,0,4));pos=new BlockPos(base.getX(),250,base.getZ());
            for(int x=(pos.getX()>>4)-1;x<=(pos.getX()>>4)+1;x++)for(int z=(pos.getZ()>>4)-1;z<=(pos.getZ()>>4)+1;z++)level.getChunk(x,z);
            put(pos,CatalogBlocks.block("void_siphon").defaultBlockState());tile=(VoidSiphonBlockEntity)level.getBlockEntity(pos);}
        void put(BlockPos p,BlockState state){old.putIfAbsent(p,level.getBlockState(p));level.setBlockAndUpdate(p,state);}
        FluxRiftEntity rift(int size,double x,double y,double z){var r=(FluxRiftEntity)VisualEntitiesModule.EFFECTS.get("flux_rift").get().create(level);r.setPos(pos.getX()+.5+x,pos.getY()+.5+y,pos.getZ()+.5+z);r.setRiftSeed(315);r.setRiftSize(size);r.setRiftStability(0);h.assertTrue(level.addFreshEntity(r),"Native Rift fixture rejected");rifts.add(r);return r;}
        void ticks(int n){for(int i=0;i<n;i++)VoidSiphonBlockEntity.tick(level,pos,tile.getBlockState(),tile);}
        void progress(int initial){var tag=tile.saveWithoutMetadata();tag.putShort("progress",(short)initial);tile.load(tag);}
        @Override public void close(){rifts.forEach(FluxRiftEntity::discard);tile.clearContent();var entries=new ArrayList<>(old.entrySet());Collections.reverse(entries);for(var e:entries)level.setBlockAndUpdate(e.getKey(),e.getValue());}
    }
    @GameTest(template="essentia_network")
    public static void twentiethEnabledTickUsesOriginalIntegerSquareRootAndStabilityDebit(GameTestHelper h){
        try(var f=new Trial(h)){var r=f.rift(16,3,0,0);f.ticks(19);h.assertTrue(f.tile.progress()==0&&r.getRiftStability()==0,"Siphon ran before twentieth tick");
            f.ticks(1);h.assertTrue(f.tile.progress()==4&&Math.abs(r.getRiftStability()+4F/15)<1E-6&&r.getRiftSize()>=15&&r.getRiftSize()<=16,"Original progress/debit/one33-shrink failed");
            h.assertTrue(f.tile.getItem(0).isEmpty(),"Four progress manufactured a finished seed");}
        h.succeed();
    }
    @GameTest(template="essentia_network")
    public static void rangeIsOriginalCubeAndLineOfSightRejectsOpaqueWallAndSmallOrRemovedRifts(GameTestHelper h){
        try(var f=new Trial(h)){var small=f.rift(1,3,0,0);var diagonal=f.rift(16,7,0,7);var far=f.rift(16,12,0,0);
            h.assertTrue(f.tile.validRifts().equals(List.of(diagonal)),"Range became a sphere or accepted small/far Rift");
            f.put(f.pos.offset(3,0,3),Blocks.BEDROCK.defaultBlockState());f.put(f.pos.offset(3,1,3),Blocks.BEDROCK.defaultBlockState());
            h.assertTrue(f.tile.validRifts().isEmpty(),"Opaque wall did not block Siphon");
            diagonal.discard();f.ticks(20);h.assertTrue(f.tile.progress()==0&&small.getRiftStability()==0&&far.getRiftStability()==0,"Removed/invalid Rift was debited");}
        h.succeed();
    }
    @GameTest(template="essentia_network")
    public static void realRedstonePausesRatherThanResettingCounterProgressOrRift(GameTestHelper h){
        try(var f=new Trial(h)){var r=f.rift(4,3,0,0);f.progress(1999);f.put(f.pos.north(),Blocks.REDSTONE_BLOCK.defaultBlockState());
            h.assertTrue(!f.tile.getBlockState().getValue(VoidSiphonBlock.ENABLED),"Native neighbor signal failed");
            f.ticks(40);h.assertTrue(f.tile.counter()==40&&f.tile.progress()==1999&&r.getRiftStability()==0,"Powered Siphon processed or reset state");
            f.put(f.pos.north(),Blocks.AIR.defaultBlockState());h.assertTrue(f.tile.getBlockState().getValue(VoidSiphonBlock.ENABLED),"Removing power did not enable");
            f.ticks(20);h.assertTrue(f.tile.getItem(0).is(VoidSiphonBlockEntity.seed())&&f.tile.getItem(0).getCount()==1&&f.tile.progress()==1,"Crossing2000 did not create exactly one physical seed");}
        h.succeed();
    }
    @GameTest(template="essentia_network")
    public static void fullOutputStopsRiftDebitAndEveryFaceExtractsWithoutAcceptingInsertions(GameTestHelper h){
        try(var f=new Trial(h)){var r=f.rift(4,3,0,0);f.progress(1999);f.tile.setItem(0,new ItemStack(VoidSiphonBlockEntity.seed(),64));f.ticks(20);
            h.assertTrue(f.tile.progress()==1999&&r.getRiftStability()==0,"Full inventory still drained a Rift");
            for(Direction face:Direction.values()){var handler=f.tile.getCapability(ForgeCapabilities.ITEM_HANDLER,face).orElseThrow(IllegalStateException::new);var seed=new ItemStack(VoidSiphonBlockEntity.seed());
                h.assertTrue(ItemStack.matches(handler.insertItem(0,seed,false),seed)&&handler.extractItem(0,1,true).getCount()==1&&f.tile.getItem(0).getCount()==64,"Face changed insertion/extraction/simulation "+face);}
            var output=f.tile.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(IllegalStateException::new).extractItem(0,64,false);
            h.assertTrue(output.getCount()==64&&f.tile.isEmpty(),"Real capability extraction failed");f.ticks(20);
            h.assertTrue(f.tile.getItem(0).getCount()==1&&f.tile.progress()==1&&r.getRiftStability()<0,"Free capacity did not resume physical production");
            var cap=f.tile.getCapability(ForgeCapabilities.ITEM_HANDLER);f.tile.invalidateCaps();h.assertTrue(!cap.isPresent(),"Removed capability survived invalidation");f.tile.reviveCaps();}
        h.succeed();
    }
    @GameTest(template="essentia_network")
    public static void progressAndPhysicalInventorySurviveSignedShortReloadWhileIntervalRestarts(GameTestHelper h){
        try(var f=new Trial(h)){f.progress(1579);f.tile.setItem(0,new ItemStack(VoidSiphonBlockEntity.seed(),7));f.ticks(13);var tag=f.tile.saveWithoutMetadata();
            h.assertTrue(tag.contains("progress",net.minecraft.nbt.Tag.TAG_SHORT),"Original progress NBT type changed");
            var copy=new VoidSiphonBlockEntity(f.pos,f.tile.getBlockState());copy.load(tag);
            h.assertTrue(copy.progress()==1579&&copy.counter()==0&&copy.getItem(0).getCount()==7,"Save retained transient clock or lost progress/output");
            tag.putShort("progress",(short)-1);copy.load(tag);h.assertTrue(copy.progress()==-1,"Released signed-short quirk was silently normalized");
            var before=copy.saveWithoutMetadata();VoidSiphonBlockEntity.tick(f.level,f.pos,f.tile.getBlockState(),copy);
            h.assertTrue(before.equals(copy.saveWithoutMetadata())&&copy.counter()==0,"Detached stale tile progressed");}
        h.succeed();
    }
    @GameTest(template="essentia_network")
    public static void storageFreeCatalogueMigrationPreservesStateAndNeverReplacesPaidInventory(GameTestHelper h){
        try(var f=new Trial(h)){f.put(f.pos.east(),Blocks.REDSTONE_BLOCK.defaultBlockState());var state=f.level.getBlockState(f.pos);
            h.assertTrue(!state.getValue(VoidSiphonBlock.ENABLED),"Initial native redstone fixture failed to disable the Siphon");
            var chunk=f.level.getChunkAt(f.pos);var anchor=new CatalogBlockEntity(f.pos,state);f.level.removeBlockEntity(f.pos);chunk.addAndRegisterBlockEntity(anchor);
            LegacyVoidSiphonMigration.repair(f.level,chunk);var migrated=(VoidSiphonBlockEntity)f.level.getBlockEntity(f.pos);
            h.assertTrue(migrated!=null&&migrated.isEmpty()&&migrated.getBlockState().equals(state),"Empty anchor migration changed enabled/paid state");
            migrated.setItem(0,new ItemStack(VoidSiphonBlockEntity.seed(),5));LegacyVoidSiphonMigration.repair(f.level,chunk);
            h.assertTrue(f.level.getBlockEntity(f.pos)==migrated&&migrated.getItem(0).getCount()==5,"Migration replaced native paid inventory");migrated.clearContent();}
        h.succeed();
    }
}
