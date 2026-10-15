package thaumcraft.artifice.dioptra;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import thaumcraft.catalog.blocks.CatalogBlockEntity;
import thaumcraft.catalog.blocks.CatalogBlocks;
import thaumcraft.world.aura.AuraChunk;
import thaumcraft.world.aura.AuraManager;
import thaumcraft.world.aura.AuraSavedData;

/** Released observer behavior. Isolated terrain/raw aura/NBT/clock fixtures are not survival progression. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class DioptraGameTests {
    private static final AtomicInteger ARENAS=new AtomicInteger();
    private static final TicketType<ChunkPos> NATIVE_TICKS=TicketType.create("thaumcraft_dioptra_qa",Comparator.comparingLong(ChunkPos::toLong));
    private DioptraGameTests() {}

    @GameTest(template="empty",timeoutTicks=180)
    public static void releasedLegacyBooleanBaseAndPhysicalPlacementBothSelectVis(GameTestHelper h) {new Fixture(h).run(f->{
        var block=(DioptraBlock)CatalogBlocks.block("dioptra");var state=block.defaultBlockState();
        h.assertTrue(state.getValue(DioptraBlock.ENABLED)&&state.getRenderShape()==RenderShape.MODEL
                &&!state.isRedstoneConductor(f.level,f.pos),"Legacy PropertyBool TRUE-first default or native device properties changed");
        f.put(f.pos.below(),Blocks.STONE.defaultBlockState());var p=f.player();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(block));
        var context=new BlockPlaceContext(new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(f.pos.below()).add(0,.5,0),Direction.UP,f.pos.below(),false)));
        h.assertTrue(block.getStateForPlacement(context).getValue(DioptraBlock.ENABLED),"Original placed Dioptra did not explicitly select Vis");
        var tile=f.tile();h.assertTrue(tile.counter==0&&tile.gridSnapshot().length==169&&tile.centerAmount()==0,"Original initial clock/grid changed");
    });}

    @GameTest(template="empty")
    public static void releasedQuantizationTruncatesAtOneSixtyFourthAndCapsAtSixtyFour(GameTestHelper h) {
        float[] amounts={0,7.8124F,7.8125F,15.624F,15.625F,250,499.999F,500,900};int[] expected={0,0,1,1,2,32,63,64,64};
        for(int i=0;i<amounts.length;i++)h.assertTrue(DioptraBlockEntity.scale(amounts[i])==expected[i],"Released /500*64 truncation/cap changed: "+amounts[i]);h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=180)
    public static void nineteenServerCallbacksKeepGridAndTwentiethReadsWithoutPayingAura(GameTestHelper h) {new Fixture(h).run(f->{
        var tile=f.tile();f.aura(f.pos,250,125);Arrays.fill(tile.grid_amt,(byte)7);
        for(int i=0;i<19;i++)tile.update();h.assertTrue(tile.counter==19&&tile.centerAmount()==7,"Observer sampled before original twentieth callback");
        tile.update();h.assertTrue(tile.counter==20&&tile.centerAmount()==32&&AuraManager.getVis(f.level,f.pos)==250
                &&AuraManager.getFlux(f.level,f.pos)==125,"Twentieth observer callback changed clock/value or consumed Vis/Flux");
    });}

    @GameTest(template="empty",timeoutTicks=180)
    public static void centerEightyFourAndXPlusThirteenZKeepOriginalChunkOrientation(GameTestHelper h) {new Fixture(h).run(f->{
        var tile=f.tile();BlockPos east=f.pos.east(16),south=f.pos.south(16),northwest=f.pos.offset(-16,0,-16);
        for(BlockPos at:new BlockPos[]{east,south,northwest})f.level.getChunkAt(at);
        f.aura(f.pos,250,10);f.aura(east,125,20);f.aura(south,375,30);f.aura(northwest,500,40);
        for(int i=0;i<20;i++)tile.update();var grid=tile.gridSnapshot();
        h.assertTrue(grid[84]==32&&grid[85]==16&&grid[97]==48&&grid[70]==64,"Original13x13 chunk origin, x+z*13 order or center84 changed");
        h.assertTrue(AuraManager.getVis(f.level,east)==125&&AuraManager.getVis(f.level,south)==375&&AuraManager.getFlux(f.level,northwest)==40,
                "Read-only multi-chunk observation altered raw aura");
    });}

    @GameTest(template="empty",timeoutTicks=180)
    public static void nativeClickChangesOnlyModeUntilNextOriginalTwentyTickSample(GameTestHelper h) {new Fixture(h).run(f->{
        var tile=f.tile();f.aura(f.pos,250,125);for(int i=0;i<20;i++)tile.update();for(int i=0;i<7;i++)tile.update();
        var p=f.player();var state=f.level.getBlockState(f.pos);var hit=new BlockHitResult(Vec3.atCenterOf(f.pos),Direction.UP,f.pos,false);
        h.assertTrue(state.getBlock().use(state,f.level,f.pos,p,InteractionHand.MAIN_HAND,hit).consumesAction()
                &&!f.level.getBlockState(f.pos).getValue(DioptraBlock.ENABLED)&&f.level.getBlockEntity(f.pos)==tile
                &&tile.counter==27&&tile.centerAmount()==32,"Native mode click replaced/reset observer or eagerly resampled Flux");
        for(int i=0;i<12;i++)tile.update();h.assertTrue(tile.counter==39&&tile.centerAmount()==32,"Toggle reset original cadence");
        tile.update();h.assertTrue(tile.counter==40&&tile.centerAmount()==16&&AuraManager.getVis(f.level,f.pos)==250&&AuraManager.getFlux(f.level,f.pos)==125,
                "Original delayed Flux sample/cost changed");
    });}

    @GameTest(template="empty",timeoutTicks=180)
    public static void physicalRedstoneNeitherDisablesNorSelectsTheObserverMode(GameTestHelper h) {new Fixture(h).run(f->{
        var tile=f.tile();f.aura(f.pos,250,125);f.put(f.pos.east(),Blocks.REDSTONE_BLOCK.defaultBlockState());
        h.assertTrue(f.level.hasNeighborSignal(f.pos)&&f.level.getBlockState(f.pos).getValue(DioptraBlock.ENABLED),"Redstone selected Flux or disabled original observer");
        for(int i=0;i<20;i++)tile.update();h.assertTrue(tile.centerAmount()==32&&tile.counter==20,"Powered Dioptra stopped native samples");
        f.level.setBlockAndUpdate(f.pos,f.level.getBlockState(f.pos).setValue(DioptraBlock.ENABLED,false));
        for(int i=0;i<20;i++)tile.update();h.assertTrue(tile.centerAmount()==16&&!f.level.getBlockState(f.pos).getValue(DioptraBlock.ENABLED),"Redstone overrode selected Flux");
    });}

    @GameTest(template="empty",timeoutTicks=180)
    public static void originalComparatorHasZeroFloorAndExactPositiveBoundariesThroughFifteen(GameTestHelper h) {new Fixture(h).run(f->{
        var tile=f.tile();int[] amounts={0,1,4,5,9,10,32,63,64},signals={0,1,1,2,2,3,8,14,15};
        for(int i=0;i<amounts.length;i++){tile.grid_amt[84]=(byte)amounts[i];var state=f.level.getBlockState(f.pos);
            h.assertTrue(state.hasAnalogOutputSignal()&&state.getAnalogOutputSignal(f.level,f.pos)==signals[i],"Released center comparator boundary changed: "+amounts[i]);}
        for(int i=0;i<=64;i++){tile.grid_amt[84]=(byte)i;h.assertTrue(tile.comparatorSignal()>=0&&tile.comparatorSignal()<=15,"Comparator escaped0..15");}
    });}

    @GameTest(template="empty",timeoutTicks=180)
    public static void savePersistsOnlyDetachedGridAndFullLoadResetsCounterWhileUpdateRetainsIt(GameTestHelper h) {new Fixture(h).run(f->{
        var tile=f.tile();for(int i=0;i<169;i++)tile.grid_amt[i]=(byte)(i%65);tile.counter=17;
        var saved=tile.saveWithoutMetadata();h.assertTrue(saved.contains("grid_a",7)&&saved.getByteArray("grid_a").length==169&&!saved.contains("counter"),"Original save key/size or ephemeral counter changed");
        var copy=new DioptraBlockEntity(f.pos,tile.getBlockState());copy.counter=11;copy.load(saved);
        h.assertTrue(copy.counter==0&&Arrays.equals(copy.gridSnapshot(),tile.gridSnapshot()),"Full reload retained nonpersistent clock or lost169grid");
        var update=tile.getUpdateTag();tile.handleUpdateTag(update);tile.onDataPacket(null,tile.getUpdatePacket());
        h.assertTrue(tile.counter==17&&Arrays.equals(copy.gridSnapshot(),tile.gridSnapshot()),"Client update path reset original interpolation clock");
        var detached=tile.gridSnapshot();detached[84]=64;update.putByteArray("grid_a",new byte[]{1});
        h.assertTrue(tile.centerAmount()==84%65&&tile.gridSnapshot().length==169,"Detached grid/update tag mutation changed observer storage");
    });}

    @GameTest(template="empty",timeoutTicks=180)
    public static void malformedShortLongNegativeAndWrongTypedGridTagsRemainBounded(GameTestHelper h) {new Fixture(h).run(f->{
        var tile=f.tile();var shortTag=new CompoundTag();shortTag.putByteArray("grid_a",new byte[]{-1,127});tile.load(shortTag);
        h.assertTrue(tile.gridSnapshot().length==169&&tile.gridSnapshot()[0]==0&&tile.gridSnapshot()[1]==64&&tile.centerAmount()==0,"Modern malformed short/signed-byte normalization changed");
        var longTag=new CompoundTag();byte[] raw=new byte[220];Arrays.fill(raw,(byte)50);longTag.putByteArray("grid_a",raw);tile.load(longTag);
        h.assertTrue(tile.gridSnapshot().length==169&&tile.gridSnapshot()[168]==50,"Oversized saved grid escaped169cells");
        var wrong=new CompoundTag();wrong.putString("grid_a","not bytes");tile.counter=9;tile.handleUpdateTag(wrong);
        h.assertTrue(tile.counter==9&&tile.gridSnapshot().length==169&&tile.centerAmount()==50,"Wrong typed update erased valid grid/reset clock");
    });}

    @GameTest(template="empty",timeoutTicks=180)
    public static void originalExpandedRenderBoxAndReadOnlyNoInventoryContractRemain(GameTestHelper h) {new Fixture(h).run(f->{
        var tile=f.tile();h.assertTrue(tile.getRenderBoundingBox().equals(new AABB(f.pos.getX()-.3,f.pos.getY()-.3,f.pos.getZ()-.3,f.pos.getX()+1.3,f.pos.getY()+2.3,f.pos.getZ()+1.3)),
                "Released -.3..1.3/2.3 observer render bounds changed");
        h.assertTrue(!((Object)tile instanceof Container)&&!tile.getUpdateTag().contains("Items")&&!tile.saveWithoutMetadata().contains("Items"),"Read-only observer gained a resource inventory");
    });}

    @GameTest(template="empty",timeoutTicks=180)
    public static void persistedAuraInAnUnloadedOuterChunkStaysInvisibleWithoutForceLoading(GameTestHelper h) {new Fixture(h).run(f->{
        var tile=f.tile();var missing=new ChunkPos((f.pos.getX()>>4)+6,(f.pos.getZ()>>4)+6);var at=missing.getMiddleBlockPosition(250);
        h.assertTrue(f.level.getChunkSource().getChunkNow(missing.x,missing.z)==null,"Owned observer missing-chunk fixture is unexpectedly loaded");
        f.aura(at,500,500);h.assertTrue(AuraSavedData.get(f.level).getChunk(missing)!=null&&AuraManager.getVis(f.level,at)==500,"Persisted unloaded aura fixture absent");
        Arrays.fill(tile.grid_amt,(byte)64);for(int i=0;i<20;i++)tile.update();
        h.assertTrue(tile.grid_amt[168]==0&&f.level.getChunkSource().getChunkNow(missing.x,missing.z)==null&&AuraManager.getVis(f.level,at)==500&&AuraManager.getFlux(f.level,at)==500,
                "Observer displayed persisted-but-unloaded aura, forced its chunk or spent its amounts");
    });}

    @GameTest(template="empty",timeoutTicks=180)
    public static void loadedPaletteMigrationRepairsOnlyMissingOrEmptyLegacyAnchorAndKeepsNativeClockAndForeignData(GameTestHelper h) {new Fixture(h).run(f->{
        var nativeTile=f.tile();nativeTile.counter=13;nativeTile.grid_amt[84]=23;
        BlockPos absent=f.pos.east(2),legacy=f.pos.south(2),foreign=f.pos.offset(2,0,2);var state=nativeTile.getBlockState();
        f.put(absent,state);f.put(legacy,state);f.put(foreign,state);f.level.removeBlockEntity(absent);
        var chunk=f.level.getChunkAt(f.pos);h.assertTrue(chunk.getBlockEntity(absent,LevelChunk.EntityCreationType.CHECK)==null,"Native removal did not leave missing legacy tile fixture");
        var old=new CatalogBlockEntity(legacy,state);f.level.setBlockEntity(old);
        var outsider=new BlockEntity(DioptraModule.BLOCK_ENTITY.get(),foreign,state){};outsider.getPersistentData().putInt("paidForeignMarker",37);f.level.setBlockEntity(outsider);
        LegacyDioptraMigration.repair(f.level,chunk);
        h.assertTrue(f.level.getBlockEntity(absent) instanceof DioptraBlockEntity&&f.level.getBlockEntity(legacy) instanceof DioptraBlockEntity
                &&f.level.getBlockEntity(legacy)!=old&&f.level.getBlockEntity(f.pos)==nativeTile&&nativeTile.counter==13&&nativeTile.centerAmount()==23
                &&f.level.getBlockEntity(foreign)==outsider&&outsider.getPersistentData().getInt("paidForeignMarker")==37,
                "Migration replaced native clock/grid/foreign data or did not repair original missing/empty anchors");
        LegacyDioptraMigration.repair(f.level,chunk);h.assertTrue(f.level.getBlockEntity(f.pos)==nativeTile&&nativeTile.counter==13&&f.level.getBlockEntity(foreign)==outsider,"Repeated migration claimed foreign/native state");
    });}

    @GameTest(template="empty",timeoutTicks=180)
    public static void capturedRestoringAndDetachedCallbacksCannotAdvanceObserverOrReplaceItsGrid(GameTestHelper h) {new Fixture(h).run(f->{
        var tile=f.tile();tile.counter=19;tile.grid_amt[84]=11;
        boolean capture=f.level.captureBlockSnapshots,restoring=f.level.restoringBlockSnapshots;
        try{f.level.captureBlockSnapshots=true;tile.update();h.assertTrue(tile.counter==19&&tile.centerAmount()==11,"Observer entered captured transaction");
            f.level.captureBlockSnapshots=false;f.level.restoringBlockSnapshots=true;tile.update();h.assertTrue(tile.counter==19&&tile.centerAmount()==11,"Observer entered restoring transaction");}
        finally{f.level.captureBlockSnapshots=capture;f.level.restoringBlockSnapshots=restoring;}
        f.put(f.pos,Blocks.DIAMOND_BLOCK.defaultBlockState());tile.update();h.assertTrue(tile.counter==19&&tile.centerAmount()==11&&f.level.getBlockState(f.pos).is(Blocks.DIAMOND_BLOCK),"Detached observer mutated replacement or advanced stale callback");
    });}

    @GameTest(template="empty",timeoutTicks=240)
    public static void realNativeWorldTicksSampleAtTwentyAndFortyWithOwnedRegionTicket(GameTestHelper h) {
        var f=new Fixture(h);var chunk=new ChunkPos(f.pos);f.level.getChunkSource().addRegionTicket(NATIVE_TICKS,chunk,2,chunk);boolean[] closed={false};
        DioptraBlockEntity[] tile={null};long[] atNineteen={0};Runnable cleanup=()->{if(closed[0])return;closed[0]=true;try{f.close();}finally{f.level.getChunkSource().removeRegionTicket(NATIVE_TICKS,chunk,2,chunk);}};
        h.startSequence().thenWaitUntil(()->h.assertTrue(f.level.getChunkSource().getChunkNow(chunk.x,chunk.z)!=null
                &&f.level.areEntitiesLoaded(chunk.toLong())&&f.level.getChunkSource().isPositionTicking(chunk.toLong()),"Native observer arena block/entity ticking readiness pending"))
                .thenExecute(()->checked(()->{tile[0]=f.tile();f.aura(f.pos,330,0);},cleanup))
                .thenWaitUntil(()->h.assertTrue(tile[0].counter==19,"Waiting for nineteen actual world-owned block-entity callbacks"))
                .thenExecute(()->checked(()->{atNineteen[0]=f.level.getGameTime();h.assertTrue(tile[0].centerAmount()==0,"Native world sampled before counter20");},cleanup))
                .thenIdle(1).thenExecute(()->checked(()->{h.assertTrue(tile[0].counter==20&&f.level.getGameTime()==atNineteen[0]+1&&tile[0].centerAmount()>0&&tile[0].centerAmount()<=64,"Native twentieth world tick did not sample bounded positive center");},cleanup))
                .thenIdle(21).thenExecute(()->{try{h.assertTrue(tile[0].counter==41&&f.level.getGameTime()==atNineteen[0]+22&&tile[0].centerAmount()>0&&tile[0].centerAmount()<=64,"Native41 callbacks/second20tick sample did not persist");}finally{cleanup.run();}}).thenSucceed();
        h.runAtTickTime(235,()->{if(!closed[0]){cleanup.run();h.fail("Native observer actual-world ticking/readiness timed out");}});
    }

    private static void checked(Runnable action,Runnable cleanup){try{action.run();}catch(RuntimeException|Error failure){cleanup.run();throw failure;}}

    private static final class Fixture implements AutoCloseable {
        final GameTestHelper h;final ServerLevel level;final BlockPos pos;final Map<BlockPos,BlockState> terrain=new LinkedHashMap<>();
        final Map<ChunkPos,AuraImage> auraBefore=new LinkedHashMap<>();boolean closed;
        Fixture(GameTestHelper h){this.h=h;level=h.getLevel();var base=h.absolutePos(BlockPos.ZERO);pos=new BlockPos(1_000_008+512*ARENAS.incrementAndGet(),250,(base.getZ()>>4)*16+8);level.getChunkAt(pos);}
        void put(BlockPos at,BlockState state){terrain.putIfAbsent(at.immutable(),level.getBlockState(at));level.setBlockAndUpdate(at,state);}
        DioptraBlockEntity tile(){put(pos,CatalogBlocks.block("dioptra").defaultBlockState().setValue(DioptraBlock.ENABLED,true));return (DioptraBlockEntity)level.getBlockEntity(pos);}
        FakePlayer player(){var p=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"dioptra_qa"));p.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);return p;}
        void aura(BlockPos at,float vis,float flux){var chunk=new ChunkPos(at);var data=AuraSavedData.get(level);
            if(!auraBefore.containsKey(chunk)){var old=data.getChunk(chunk);auraBefore.put(chunk,new AuraImage(old,old==null?0:old.getVis(),old==null?0:old.getFlux()));}
            if(data.getChunk(chunk)==null)data.getOrCreate(chunk,0);
            AuraManager.drainVis(level,at,Float.MAX_VALUE,false);AuraManager.drainFlux(level,at,Float.MAX_VALUE,false);AuraManager.addVis(level,at,vis);AuraManager.addFlux(level,at,flux);
        }
        void run(Consumer<Fixture> action){h.startSequence().thenWaitUntil(()->h.assertTrue(DioptraModule.loaded(level,pos),"Owned observer arena chunk pending"))
                .thenExecute(()->{try{action.accept(this);}finally{close();}}).thenSucceed();}
        @Override public void close(){if(closed)return;closed=true;var rows=new ArrayList<>(terrain.entrySet());Collections.reverse(rows);for(var row:rows)level.setBlockAndUpdate(row.getKey(),row.getValue());
            var data=AuraSavedData.get(level);for(var row:auraBefore.entrySet()){var at=row.getKey().getMiddleBlockPosition(250);var image=row.getValue();
                AuraManager.drainVis(level,at,Float.MAX_VALUE,false);AuraManager.drainFlux(level,at,Float.MAX_VALUE,false);
                if(image.original!=null){AuraManager.addVis(level,at,image.vis);AuraManager.addFlux(level,at,image.flux);}
                else try{var field=AuraSavedData.class.getDeclaredField("chunks");field.setAccessible(true);@SuppressWarnings("unchecked") var map=(Map<Long,AuraChunk>)field.get(data);map.remove(row.getKey().toLong());}
                    catch(ReflectiveOperationException error){throw new IllegalStateException("Cannot retire only owned raw aura fixture",error);}
            }terrain.clear();auraBefore.clear();
        }
    }
    private record AuraImage(AuraChunk original,float vis,float flux){}
}
