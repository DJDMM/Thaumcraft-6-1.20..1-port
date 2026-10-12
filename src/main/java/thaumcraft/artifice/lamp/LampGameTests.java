package thaumcraft.artifice.lamp;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import thaumcraft.artifice.mirror.*;
import thaumcraft.catalog.blocks.CatalogBlocks;

/** Physical lamp/light geometry, signals and loaded migration; no manufactured research. */
@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class LampGameTests {
    private LampGameTests(){}
    private static final class Fixture implements AutoCloseable {
        final GameTestHelper h;final ServerLevel level;final BlockPos origin;
        final Map<BlockPos,BlockState> saved=new LinkedHashMap<>();
        Fixture(GameTestHelper h){this.h=h;level=h.getLevel();var p=h.absolutePos(new BlockPos(4,0,4));origin=new BlockPos(p.getX(),244,p.getZ());for(int x=(origin.getX()-16)>>4;x<=(origin.getX()+16)>>4;x++)for(int z=(origin.getZ()-16)>>4;z<=(origin.getZ()+16)>>4;z++)level.getChunk(x,z);}
        void block(BlockPos pos,BlockState state){saved.putIfAbsent(pos.immutable(),level.getBlockState(pos));level.setBlockAndUpdate(pos,state);}
        void track(BlockPos pos){saved.putIfAbsent(pos.immutable(),level.getBlockState(pos));}
        ArcaneLampBlockEntity lamp(){block(origin.below(),Blocks.STONE.defaultBlockState());block(origin,CatalogBlocks.block("lamp_arcane").defaultBlockState());return (ArcaneLampBlockEntity)level.getBlockEntity(origin);}
        void run(Consumer<Fixture> action){h.startSequence().thenWaitUntil(()->{for(int x=(origin.getX()-16)>>4;x<=(origin.getX()+16)>>4;x++)for(int z=(origin.getZ()-16)>>4;z<=(origin.getZ()+16)>>4;z++)h.assertTrue(level.getChunkSource().getChunkNow(x,z)!=null,"Native lamp fixture chunks unavailable");}).thenExecute(()->{try{action.accept(this);}finally{close();}}).thenSucceed();}
        @Override public void close(){var rows=new java.util.ArrayList<>(saved.entrySet());java.util.Collections.reverse(rows);for(var row:rows)level.setBlockAndUpdate(row.getKey(),row.getValue());saved.clear();}
    }
    @GameTest(template="empty",timeoutTicks=240) public static void glimmerHasNativePersistentAirLight15AndNoGeometryOrPick(GameTestHelper h){new Fixture(h).run(f->{var block=CatalogBlocks.block("effect_glimmer");var state=block.defaultBlockState();f.block(f.origin,state);h.assertTrue(block instanceof GlimmerBlock&&!state.isAir()&&state.canBeReplaced()&&state.getLightEmission(f.level,f.origin)==15&&!state.isRandomlyTicking()&&state.getRenderShape()==RenderShape.INVISIBLE&&state.getShape(f.level,f.origin).isEmpty()&&state.getCollisionShape(f.level,f.origin).isEmpty(),"Glimmer lost persistent native light/replaceability or modern section storage");h.assertTrue(block.getCloneItemStack(state,new BlockHitResult(Vec3.atCenterOf(f.origin),Direction.UP,f.origin,false),f.level,f.origin,null).isEmpty(),"Glimmer became a player obtainable block");});}
    @GameTest(template="empty",timeoutTicks=240)
    public static void glimmerPlacesSerializesAndRemovesInNativeEmptyChunkSection(GameTestHelper h){
        new Fixture(h).run(f->{
            BlockPos pos=new BlockPos(f.origin.getX(),f.level.getMaxBuildHeight()-1,f.origin.getZ());
            var chunk=f.level.getChunkAt(pos);var section=chunk.getSection(chunk.getSectionIndex(pos.getY()));
            h.assertTrue(section.hasOnlyAir(),"Native glimmer storage regression requires a real empty top section");
            f.block(pos,CatalogBlocks.block("effect_glimmer").defaultBlockState());
            h.assertTrue(!section.hasOnlyAir()&&chunk.getBlockState(pos).is(CatalogBlocks.block("effect_glimmer"))
                    &&f.level.getBlockState(pos).getLightEmission(f.level,pos)==15,"Native empty section elided placed glimmer/light15");
            var saved=net.minecraft.world.level.chunk.storage.ChunkSerializer.write(f.level,chunk);
            boolean stored=false;var sections=saved.getList("sections",Tag.TAG_COMPOUND);
            for(int i=0;i<sections.size();i++){
                var row=sections.getCompound(i);if(row.getByte("Y")!=net.minecraft.core.SectionPos.blockToSectionCoord(pos.getY()))continue;
                var palette=row.getCompound("block_states").getList("palette",Tag.TAG_COMPOUND);
                for(int j=0;j<palette.size();j++)stored|=palette.getCompound(j).getString("Name").equals("thaumcraft:effect_glimmer");
            }
            h.assertTrue(stored,"Native chunk serializer omitted persistent glimmer palette");
            h.assertTrue(!CatalogBlocks.block("lamp_arcane").defaultBlockState().setValue(ArcaneLampBlock.FACING,Direction.UP).canSurvive(f.level,pos.below()),"Stored glimmer became physical lamp support");
            h.assertTrue(f.level.removeBlock(pos,false)&&chunk.getBlockState(pos).is(Blocks.AIR)&&section.hasOnlyAir(),"Native empty-section glimmer removal failed");
        });
    }
    @GameTest(template="empty",timeoutTicks=240) public static void allSixPlacementsFaceSupportAndRetainOriginalBounds(GameTestHelper h){new Fixture(h).run(f->{var lamp=(ArcaneLampBlock)CatalogBlocks.block("lamp_arcane");var p=new FakePlayer(f.level,new GameProfile(java.util.UUID.randomUUID(),"lamp_place"));for(Direction face:Direction.values()){BlockPos support=f.origin.offset(0,face.ordinal()*2,0);f.block(support,Blocks.STONE.defaultBlockState());p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(lamp));var context=new BlockPlaceContext(new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(support).add(Vec3.atLowerCornerOf(face.getNormal()).scale(.5)),face,support,false)));var state=lamp.getStateForPlacement(context);h.assertTrue(state!=null&&state.getValue(ArcaneLampBlock.FACING)==face.getOpposite()&&!state.getValue(ArcaneLampBlock.ENABLED)&&state.getShape(f.level,context.getClickedPos()).bounds().equals(new net.minecraft.world.phys.AABB(.25,.125,.25,.75,.875,.75)),"Original lamp support-facing/default state/bounds differ:"+face);}});}
    @GameTest(template="empty",timeoutTicks=240)
    public static void nativeSignalStopsLampLightAndClearsOnlyOriginalFifteenCube(GameTestHelper h){
        new Fixture(h).run(f->{
            var lamp=f.lamp();BlockPos near=f.origin.north(14),edge=f.origin.north(15),outside=f.origin.north(16);
            var glimmer=CatalogBlocks.block("effect_glimmer").defaultBlockState();
            f.block(near,glimmer);f.block(edge,glimmer);f.block(outside,glimmer);
            h.assertTrue(f.level.getBlockState(f.origin).getValue(ArcaneLampBlock.ENABLED)
                    &&f.level.getBlockState(f.origin).getLightEmission(f.level,f.origin)==15,"Unpowered placed lamp is not enabled/light15");
            f.block(f.origin.east(),Blocks.REDSTONE_BLOCK.defaultBlockState());
            h.assertTrue(f.level.hasNeighborSignal(f.origin),"Physical redstone fixture did not power native lamp");
            h.assertTrue(!f.level.getBlockState(f.origin).getValue(ArcaneLampBlock.ENABLED)
                    &&f.level.getBlockState(f.origin).getLightEmission(f.level,f.origin)==0,"Powered lamp retained enabled state/light15");
            h.assertTrue(f.level.getBlockState(near).is(Blocks.AIR),"Powered lamp retained glimmer at distance14: "+f.level.getBlockState(near));
            h.assertTrue(f.level.getBlockState(edge).is(Blocks.AIR),"Powered lamp retained glimmer at distance15: "+f.level.getBlockState(edge));
            h.assertTrue(f.level.getBlockState(outside).is(CatalogBlocks.block("effect_glimmer")),"Powered lamp changed glimmer outside distance15: "+f.level.getBlockState(outside));
            h.assertTrue(!lamp.placeLight(f.level,f.origin.south(7)),"Powered lamp created new light");
        });
    }
    @GameTest(template="empty",timeoutTicks=240) public static void nativeRemovalClearsGlimmersButPreservesOtherBlocks(GameTestHelper h){new Fixture(h).run(f->{f.lamp();var light=f.origin.north(10);var stone=f.origin.west(10);f.block(light,CatalogBlocks.block("effect_glimmer").defaultBlockState());f.block(stone,Blocks.DIAMOND_BLOCK.defaultBlockState());f.level.removeBlock(f.origin,false);h.assertTrue(f.level.getBlockEntity(f.origin)==null&&f.level.getBlockState(light).is(Blocks.AIR)&&f.level.getBlockState(stone).is(Blocks.DIAMOND_BLOCK),"Lamp cleanup lost native removal lifecycle or deleted ordinary terrain");});}
    @GameTest(template="empty",timeoutTicks=240) public static void lightCandidateRequiresRealAirAndSourceIgnoredColliderSight(GameTestHelper h){new Fixture(h).run(f->{var lamp=f.lamp();BlockPos target=f.origin.north(8),barrier=f.origin.north(4);f.block(target,Blocks.AIR.defaultBlockState());f.block(barrier,Blocks.STONE.defaultBlockState());h.assertTrue(!lamp.placeLight(f.level,target)&&f.level.getBlockState(target).is(Blocks.AIR),"Lamp light crossed solid obstruction");f.block(barrier,Blocks.AIR.defaultBlockState());h.assertTrue(lamp.hasLineOfSight(f.level,target)&&lamp.placeLight(f.level,target)&&f.level.getBlockState(target).is(CatalogBlocks.block("effect_glimmer")),"Source lamp shape blocked its own original light ray");h.assertTrue(!lamp.placeLight(f.level,target),"Existing glimmer accepted another mutation");f.block(target,Blocks.WATER.defaultBlockState());h.assertTrue(!lamp.placeLight(f.level,target),"Lamp overwrote a fluid with glimmer");});}
    @GameTest(template="empty",timeoutTicks=240) public static void lowerMarginAdaptsToNegativeWorldAndLoadedOnlyClampUsesPrecipitationPlusFour(GameTestHelper h){new Fixture(h).run(f->{BlockPos low=new BlockPos(f.origin.getX(),f.level.getMinBuildHeight(),f.origin.getZ());var clamped=ArcaneLampBlockEntity.clampCandidate(f.level,low);h.assertTrue(clamped!=null&&clamped.getY()==f.level.getMinBuildHeight()+5,"Modern five-block bottom margin changed");var roof=f.origin.north(8);f.block(roof,Blocks.STONE.defaultBlockState());var high=ArcaneLampBlockEntity.clampCandidate(f.level,roof.above(30));h.assertTrue(high!=null&&high.getY()==roof.getY()+5,"Native precipitation-height+4 clamp differs");var absent=new BlockPos(20000000,244,20000000);h.assertTrue(f.level.getChunkSource().getChunkNow(absent.getX()>>4,absent.getZ()>>4)==null&&ArcaneLampBlockEntity.clampCandidate(f.level,absent)==null&&f.level.getChunkSource().getChunkNow(absent.getX()>>4,absent.getZ()>>4)==null,"Lamp candidate force-loaded remote chunk");});}
    @GameTest(template="empty",timeoutTicks=240)
    public static void loadedMigrationRepairsMissingTilesWithoutReplacingPaidNativeMirrors(GameTestHelper h){
        new Fixture(h).run(f->{
            f.lamp();var pos=f.origin.east(5);
            f.block(pos.below(),Blocks.STONE.defaultBlockState());
            f.block(pos,CatalogBlocks.block("mirror").defaultBlockState().setValue(MirrorBlock.FACING,Direction.UP));
            var mirror=(ItemMirrorBlockEntity)f.level.getBlockEntity(pos);
            var marker=java.util.UUID.randomUUID();var fixtureItems=new ItemStack(net.minecraft.world.item.Items.DIAMOND,3);
            fixtureItems.getOrCreateTag().putUUID("MirrorMigrationFixture",marker);
            try{
                h.assertTrue(mirror.transportDirect(fixtureItems),"Native queue fixture not accepted");
                var link=new MirrorLink(f.level.dimension(),f.origin.west(8));mirror.setLink(link);
                int count=mirror.queuedCount();f.level.removeBlockEntity(f.origin);
                LegacyMirrorDeviceMigration.repair(f.level,f.level.getChunkAt(f.origin));
                h.assertTrue(f.level.getBlockEntity(f.origin) instanceof ArcaneLampBlockEntity&&f.level.getBlockEntity(pos)==mirror
                        &&mirror.queuedCount()==count&&link.equals(mirror.link()),"Migration replaced paid native mirror or failed lamp repair");
                LegacyMirrorDeviceMigration.repair(f.level,f.level.getChunkAt(pos));
                h.assertTrue(f.level.getBlockEntity(pos)==mirror&&mirror.queuedCount()==3,"Repeated migration mutated native paid queue");
            }finally{
                // Only this test's marked raw input is retired. Every other paid entry and
                // current link survives; creative removal alone would refund the three diamonds.
                if(f.level.getBlockEntity(pos)==mirror){
                    CompoundTag current=mirror.saveWithoutMetadata();ListTag rows=current.getList("Items",Tag.TAG_COMPOUND),retained=new ListTag();
                    for(int i=0;i<rows.size();i++){
                        var row=rows.getCompound(i);var stack=ItemStack.of(row);var tag=stack.getTag();
                        if(tag==null||!tag.hasUUID("MirrorMigrationFixture")||!marker.equals(tag.getUUID("MirrorMigrationFixture")))retained.add(row.copy());
                    }
                    current.put("Items",retained);mirror.load(current);mirror.setCreativeRemoval(true);
                }
            }
        });
    }
}
