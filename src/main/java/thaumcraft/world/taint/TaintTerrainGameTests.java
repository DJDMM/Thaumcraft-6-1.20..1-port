package thaumcraft.world.taint;

import java.lang.reflect.Field;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import thaumcraft.alchemy.AspectCrystalItem;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.catalog.entities.VisualEntitiesModule;
import thaumcraft.infusion.InfusionEffects;
import thaumcraft.world.aura.*;
import thaumcraft.world.rift.RiftModule;

/** Native registered blocks and callbacks with detached seed/aura fixtures, restored after each assertion. */
@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class TaintTerrainGameTests {
    private static final String TEMPLATE="essentia_network";
    private static final BlockPos CENTER=new BlockPos(4,3,4);
    private TaintTerrainGameTests() {}
    @SuppressWarnings("unchecked") private static final class Fixture implements AutoCloseable {
        final GameTestHelper h;final ServerLevel level;final BlockPos pos;
        final Map<ServerLevel,LinkedHashSet<BlockPos>> sources;
        final LinkedHashSet<BlockPos> previousSources;
        final Map<Long,AuraChunk> chunks;
        final Map<Long,AuraChunk> previousChunks=new HashMap<>();
        final List<Entity> entities=new ArrayList<>();
        final boolean wuss=RiftModule.WUSS_MODE.get();final double rate=RiftModule.TAINT_SPREAD_RATE.get();
        Fixture(GameTestHelper h,boolean seed) {
            this.h=h;level=h.getLevel();pos=h.absolutePos(CENTER);
            try {
                Field sf=TaintHelper.class.getDeclaredField("SEEDS");sf.setAccessible(true);sources=(Map<ServerLevel,LinkedHashSet<BlockPos>>)sf.get(null);
                Field af=AuraSavedData.class.getDeclaredField("chunks");af.setAccessible(true);chunks=(Map<Long,AuraChunk>)af.get(AuraSavedData.get(level));
            } catch(ReflectiveOperationException e) { throw new IllegalStateException(e); }
            previousSources=sources.put(level,new LinkedHashSet<>());
            RiftModule.WUSS_MODE.set(false);RiftModule.TAINT_SPREAD_RATE.set(100.0);
            for(BlockPos p:BlockPos.betweenClosed(pos.offset(-2,-1,-2),pos.offset(2,2,2))) level.setBlock(p,Blocks.AIR.defaultBlockState(),2);
            for(int x=-2;x<=2;x++) for(int z=-2;z<=2;z++) level.setBlock(pos.offset(x,-1,z),Blocks.STONE.defaultBlockState(),2);
            aura(pos,500,100,100);aura(pos.east(),500,100,100);
            if(seed) addSeed(pos);
        }
        void aura(BlockPos p,int base,float vis,float flux) {
            long key=new ChunkPos(p).toLong();if(!previousChunks.containsKey(key)) previousChunks.put(key,chunks.get(key));
            chunks.put(key,new AuraChunk(base,vis,flux));
        }
        Entity addSeed(BlockPos p) {
            level.getChunk(p); // Explicit owned fixture, not the production loaded-only lookup.
            var entity=VisualEntitiesModule.LIVING.get("taint_seed").get().create(level);
            h.assertTrue(entity instanceof TaintSeedEntity,"Registered seed remains a visual catalogue entity");
            entity.moveTo(p.getX()+.5,p.getY(),p.getZ()+.5,0,0);level.addFreshEntity(entity);entities.add(entity);TaintHelper.addTaintSeed(level,p);return entity;
        }
        void surround(BlockPos target) { for(Direction d:Direction.values()) level.setBlock(target.relative(d),TaintModule.block("taint_rock").defaultBlockState(),2); }
        void spread(boolean ignore) { level.random.setSeed(spreadRoll(ignore,false));TaintHelper.spreadFibres(level,pos,ignore); }
        @Override public void close() {
            for(Entity entity:entities) entity.discard();
            if(previousSources==null) sources.remove(level);else sources.put(level,previousSources);
            previousChunks.forEach((key,old)->{if(old==null) chunks.remove(key);else chunks.put(key,old);});
            RiftModule.WUSS_MODE.set(wuss);RiftModule.TAINT_SPREAD_RATE.set(rate);
        }
    }
    private static long spreadRoll(boolean ignore,boolean leaves) {
        for(long seed=0;seed<100000;seed++) {
            var random=RandomSource.create(seed);
            if(!ignore) { float f=random.nextFloat();if(f<.05||f>.3) continue; }
            if(random.nextInt(3)==2&&random.nextInt(3)==1&&random.nextInt(3)==1&&(!leaves||random.nextFloat()<.6)) return seed;
        }
        throw new AssertionError("Missing spread fixture");
    }
    private static RandomSource roll(int bound,int result) {
        for(long seed=0;seed<100000;seed++) { if(RandomSource.create(seed).nextInt(bound)==result) return RandomSource.create(seed); }
        throw new AssertionError("Missing block tick fixture");
    }
    private static void amount(GameTestHelper h,float actual,float expected,String message) { h.assertTrue(Math.abs(actual-expected)<.0001F,message+": "+actual+" vs "+expected); }
    private static List<ItemStack> drops(Fixture f,BlockPos pos,BlockState state,ItemStack tool) {
        return state.getBlock().getDrops(state,new LootParams.Builder(f.level).withParameter(LootContextParams.ORIGIN,Vec3.atCenterOf(pos)).withParameter(LootContextParams.TOOL,tool).withParameter(LootContextParams.BLOCK_STATE,state));
    }
    @GameTest(template=TEMPLATE) public static void fibreSpreadPaysExactlyPointZeroOneFluxWithoutVis(GameTestHelper h) {
        try(var f=new Fixture(h,true)) {
            float flux=AuraManager.getFlux(f.level,f.pos.east()),vis=AuraManager.getVis(f.level,f.pos.east());
            f.spread(true);
            h.assertTrue(f.level.getBlockState(f.pos.east()).is(TaintModule.block("taint_fibre")),"Supported air did not acquire native fibres");
            amount(h,AuraManager.getFlux(f.level,f.pos.east()),flux-.01F,"Fibre payment changed");amount(h,AuraManager.getVis(f.level,f.pos.east()),vis,"Spread debited Vis");
        }h.succeed();
    }
    @GameTest(template=TEMPLATE) public static void fluxSaturationUsesBaseRatherThanTotalAura(GameTestHelper h) {
        try(var f=new Fixture(h,true)) {
            f.aura(f.pos,500,9000,100);f.aura(f.pos.east(),500,9000,100);f.spread(false);
            h.assertTrue(f.level.getBlockState(f.pos.east()).is(TaintModule.block("taint_fibre")),"High Vis incorrectly diluted Flux/base spread probability");
        }h.succeed();
    }
    @GameTest(template=TEMPLATE) public static void ordinaryLowFluxRollDoesNotSpreadOrSpend(GameTestHelper h) {
        try(var f=new Fixture(h,true)) { f.aura(f.pos,500,100,0);f.aura(f.pos.east(),500,100,0);f.spread(false);
            h.assertTrue(f.level.isEmptyBlock(f.pos.east()),"Low-Flux probability gate did not stop ordinary spread");amount(h,AuraManager.getVis(f.level,f.pos),100,"Rejected trial consumed Vis");
        }h.succeed();
    }
    @GameTest(template=TEMPLATE) public static void wussModeBlocksOrdinarySpreadButSeedPulseBypassesIt(GameTestHelper h) {
        try(var f=new Fixture(h,true)) { RiftModule.WUSS_MODE.set(true);f.spread(false);h.assertTrue(f.level.isEmptyBlock(f.pos.east()),"wussMode spread gate failed");
            f.spread(true);h.assertTrue(f.level.getBlockState(f.pos.east()).is(TaintModule.block("taint_fibre")),"Seed's original ignore callback was blocked");
        }h.succeed();
    }
    @GameTest(template=TEMPLATE) public static void zeroRateBlocksOrdinarySpreadButDoesNotBlockForcedSeedPulse(GameTestHelper h) {
        try(var f=new Fixture(h,true)) { RiftModule.TAINT_SPREAD_RATE.set(0.0);f.spread(false);h.assertTrue(f.level.isEmptyBlock(f.pos.east()),"Rate0 ordinary gate failed");
            f.spread(true);h.assertTrue(f.level.getBlockState(f.pos.east()).is(TaintModule.block("taint_fibre")),"Rate0 incorrectly blocked original seed pulse");
        }h.succeed();
    }
    @GameTest(template=TEMPLATE) public static void forcedSpreadStillRequiresLivingNativeSeed(GameTestHelper h) {
        try(var f=new Fixture(h,false)) { f.spread(true);h.assertTrue(f.level.isEmptyBlock(f.pos.east()),"ignore flag bypassed seed requirement");amount(h,AuraManager.getFlux(f.level,f.pos.east()),100,"No-seed trial consumed Flux"); }
        h.succeed();
    }
    @GameTest(template=TEMPLATE) public static void deadSeedCacheIsPrunedRatherThanKeepingTerrainAlive(GameTestHelper h) {
        try(var f=new Fixture(h,true)) { f.entities.get(0).discard();h.assertTrue(!TaintHelper.isNearTaintSeed(f.level,f.pos),"Removed seed kept source alive");f.spread(true);h.assertTrue(f.level.isEmptyBlock(f.pos.east()),"Removed seed still spread"); }
        h.succeed();
    }
    @GameTest(template=TEMPLATE) public static void seedRadiusIsClosedSphereAndExpansionFringeIsStrict(GameTestHelper h) {
        try(var f=new Fixture(h,true)) {
            int area=TaintModule.spreadArea();h.assertTrue(TaintHelper.isNearTaintSeed(f.level,f.pos.offset(area,0,0)),"Exact radius should remain affected");
            h.assertTrue(!TaintHelper.isNearTaintSeed(f.level,f.pos.offset(area,1,0)),"Distance cube leaked beyond original radius");
            h.assertTrue(!TaintHelper.isAtTaintSeedEdge(f.level,f.pos.offset(area,0,0)),"Outer exact radius incorrectly counted as fringe");
            h.assertTrue(TaintHelper.isAtTaintSeedEdge(f.level,f.pos.offset(area-1,0,0)),"Outer inner-radius fringe disappeared");
            h.assertTrue(!TaintHelper.isAtTaintSeedEdge(f.level,f.pos.offset(1,0,0)),"Seed centre became expansion fringe");
        }h.succeed();
    }
    @GameTest(template=TEMPLATE) public static void protectedHardAndLiquidTargetsDoNotConvertOrSpend(GameTestHelper h) {
        try(var f=new Fixture(h,true)) {
            for(Block block:List.of(Blocks.BEDROCK,Blocks.OBSIDIAN,Blocks.WATER,Blocks.LAVA)) {
                f.level.setBlock(f.pos.east(),block.defaultBlockState(),2);f.spread(true);
                h.assertTrue(f.level.getBlockState(f.pos.east()).is(block),"Protected/liquid block was converted: "+block);
                amount(h,AuraManager.getFlux(f.level,f.pos.east()),100,"Rejected protected/liquid trial spent Flux");
            }
        }h.succeed();
    }
    @GameTest(template=TEMPLATE) public static void floatingAirAndTaintOnlySupportDoNotGrowFaces(GameTestHelper h) {
        try(var f=new Fixture(h,true)) {
            f.level.setBlock(f.pos.east().below(),Blocks.AIR.defaultBlockState(),2);f.spread(true);
            h.assertTrue(f.level.isEmptyBlock(f.pos.east()),"Fibres grew in unsupported air");
            f.level.setBlock(f.pos.east().below(),TaintModule.block("taint_log").defaultBlockState(),2);f.spread(true);
            h.assertTrue(f.level.isEmptyBlock(f.pos.east()),"Fibres spread with only tainted solid support");
        }h.succeed();
    }
    @GameTest(template=TEMPLATE) public static void hemmedStoneSoilAndWoodUseOriginalConversionsAndPayment(GameTestHelper h) {
        try(var f=new Fixture(h,true)) {
            BlockPos target=f.pos.east();f.surround(target);
            for(var pair:Map.of(Blocks.STONE,"taint_rock",Blocks.DIRT,"taint_soil",Blocks.SAND,"taint_soil",Blocks.OAK_PLANKS,"taint_crust",Blocks.PUMPKIN,"taint_crust").entrySet()) {
                f.level.setBlock(target,pair.getKey().defaultBlockState(),2);float flux=AuraManager.getFlux(f.level,target);f.spread(true);
                h.assertTrue(f.level.getBlockState(target).is(TaintModule.block(pair.getValue())),"Wrong hemmed conversion for "+pair.getKey());amount(h,AuraManager.getFlux(f.level,target),flux-.01F,"Conversion payment changed");
            }
        }h.succeed();
    }
    @GameTest(template=TEMPLATE) public static void hemmedLogsRetainAxisAndDoNotPayFibreFluxCost(GameTestHelper h) {
        try(var f=new Fixture(h,true)) {
            BlockPos target=f.pos.east();f.surround(target);f.level.setBlock(target,Blocks.OAK_LOG.defaultBlockState().setValue(BlockStateProperties.AXIS,Direction.Axis.X),2);f.spread(true);
            var state=f.level.getBlockState(target);h.assertTrue(state.is(TaintModule.block("taint_log"))&&state.getValue(TaintLogBlock.AXIS)==Direction.Axis.X,"Log axis conversion lost");amount(h,AuraManager.getFlux(f.level,target),100,"Original log conversion charged Flux");
        }h.succeed();
    }
    @GameTest(template=TEMPLATE) public static void ordinaryLeavesBecomePaidFibres(GameTestHelper h) {
        try(var f=new Fixture(h,true)) { f.level.setBlock(f.pos.east(),Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT,true),2);f.spread(true);
            h.assertTrue(f.level.getBlockState(f.pos.east()).is(TaintModule.block("taint_fibre")),"Leaf conversion failed");amount(h,AuraManager.getFlux(f.level,f.pos.east()),99.99F,"Leaf fibre conversion did not debit Flux");
        }h.succeed();
    }
    @GameTest(template=TEMPLATE) public static void leavesTouchingTaintLogCanBecomeOrientedFeatureWithoutFluxCost(GameTestHelper h) {
        try(var f=new Fixture(h,true)) { var target=f.pos.east();f.level.setBlock(target.east(),TaintModule.block("taint_log").defaultBlockState(),2);f.level.setBlock(target,Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT,true),2);
            f.level.random.setSeed(spreadRoll(true,true));TaintHelper.spreadFibres(f.level,f.pos,true);var state=f.level.getBlockState(target);
            h.assertTrue(state.is(TaintModule.block("taint_feature"))&&state.getValue(TaintFeatureBlock.FACING)==Direction.WEST,"Leaf feature points away from supporting log incorrectly");amount(h,AuraManager.getFlux(f.level,target),100,"Feature conversion incorrectly charged fibre Flux cost");
        }h.succeed();
    }
    @GameTest(template=TEMPLATE) public static void soilRockCrustAndGeyserDecayToTheirOriginalMaterials(GameTestHelper h) {
        try(var f=new Fixture(h,false)) {
            for(var pair:Map.of("taint_soil",Blocks.DIRT,"taint_rock",TaintModule.block("stone_porous"),"taint_crust",TaintModule.block("flux_goo"),"taint_geyser",TaintModule.block("flux_goo")).entrySet()) {
                var block=(TaintBlock)TaintModule.block(pair.getKey());f.level.setBlock(f.pos,block.defaultBlockState(),2);block.randomTick(f.level.getBlockState(f.pos),f.level,f.pos,roll(10,0));
                h.assertTrue(f.level.getBlockState(f.pos).is(pair.getValue()),"Wrong seedless decay of "+pair.getKey());
            }
        }h.succeed();
    }
    @GameTest(template=TEMPLATE) public static void seedlessSoilDoesNotDecayOnNineOtherRolls(GameTestHelper h) {
        try(var f=new Fixture(h,false)) { var block=(TaintBlock)TaintModule.block("taint_soil");f.level.setBlock(f.pos,block.defaultBlockState(),2);
            for(int outcome=1;outcome<10;outcome++) block.randomTick(block.defaultBlockState(),f.level,f.pos,roll(10,outcome));
            h.assertTrue(f.level.getBlockState(f.pos).is(block),"Seedless solid terrain lost original 1-in-10 decay gate");
        }h.succeed();
    }
    @GameTest(template=TEMPLATE) public static void seedlessLogAndFibreDecayImmediately(GameTestHelper h) {
        try(var f=new Fixture(h,false)) {
            var log=(TaintLogBlock)TaintModule.block("taint_log");f.level.setBlock(f.pos,log.defaultBlockState(),2);log.randomTick(log.defaultBlockState(),f.level,f.pos,RandomSource.create(1));
            h.assertTrue(f.level.getBlockState(f.pos).is(TaintModule.block("flux_goo")),"Seedless log did not become Flux Goo");
            var fibre=(TaintFibreBlock)TaintModule.block("taint_fibre");f.level.setBlock(f.pos,fibre.defaultBlockState(),2);fibre.randomTick(f.level.getBlockState(f.pos),f.level,f.pos,RandomSource.create(1));
            h.assertTrue(f.level.isEmptyBlock(f.pos),"Seedless fibres did not immediately die");
        }h.succeed();
    }
    @GameTest(template=TEMPLATE) public static void fibreFaceSelectionHasThinPhysicalCollisionAndNoSturdySupport(GameTestHelper h) {
        try(var f=new Fixture(h,true)) { var fibre=(TaintFibreBlock)TaintModule.block("taint_fibre");f.level.setBlock(f.pos,fibre.defaultBlockState(),2);var state=f.level.getBlockState(f.pos);
            h.assertTrue(state.getValue(TaintFibreBlock.DOWN)&&!state.getValue(TaintFibreBlock.NORTH),"Fibre face state did not follow native support");
            var shape=fibre.getCollisionShape(state,f.level,f.pos,CollisionContext.empty());h.assertTrue(!shape.isEmpty()&&shape.bounds().maxY<.051,"Fibre collision became a full cube or lost original thin face");
            h.assertTrue(!state.isFaceSturdy(f.level,f.pos,Direction.UP),"Fibre became sturdy support for neighbors");
        }h.succeed();
    }
    @GameTest(template=TEMPLATE) public static void removingLastNaturalSupportRemovesNongrowingFibre(GameTestHelper h) {
        try(var f=new Fixture(h,true)) { var fibre=(TaintFibreBlock)TaintModule.block("taint_fibre");f.level.setBlock(f.pos,fibre.defaultBlockState(),2);
            f.level.setBlock(f.pos.below(),Blocks.AIR.defaultBlockState(),2);fibre.neighborChanged(f.level.getBlockState(f.pos),f.level,f.pos,Blocks.STONE,f.pos.below(),false);
            h.assertTrue(f.level.isEmptyBlock(f.pos),"Unsupported fibre survived native neighbor callback");
        }h.succeed();
    }
    @GameTest(template=TEMPLATE) public static void featureUsesFacingSupportAndBreakProducesOriginalFluxFallback(GameTestHelper h) {
        try(var f=new Fixture(h,true)) { var feature=(TaintFeatureBlock)TaintModule.block("taint_feature");var state=feature.defaultBlockState().setValue(TaintFeatureBlock.FACING,Direction.UP);
            f.level.setBlock(f.pos,state,2);h.assertTrue(feature.canSurvive(state,f.level,f.pos),"Floor-attached feature rejected sturdy support");
            for(long seed=0;;seed++) if(RandomSource.create(seed).nextFloat()>=.333F) { f.level.random.setSeed(seed);break; }
            float flux=AuraManager.getFlux(f.level,f.pos);f.level.setBlock(f.pos.below(),Blocks.AIR.defaultBlockState(),2);feature.neighborChanged(state,f.level,f.pos,Blocks.STONE,f.pos.below(),false);
            h.assertTrue(f.level.isEmptyBlock(f.pos),"Unsupported feature survived");amount(h,AuraManager.getFlux(f.level,f.pos),flux+1,"Feature break flux fallback changed");
        }h.succeed();
    }
    @GameTest(template=TEMPLATE) public static void solidTaintAndFeatureSilkDropsDifferFromOrdinaryDrops(GameTestHelper h) {
        try(var f=new Fixture(h,false)) { ItemStack silk=new ItemStack(Items.DIAMOND_PICKAXE);silk.enchant(Enchantments.SILK_TOUCH,1);
            for(String id:List.of("taint_soil","taint_crust","taint_geyser","taint_feature")) { var state=TaintModule.block(id).defaultBlockState();h.assertTrue(drops(f,f.pos,state,ItemStack.EMPTY).isEmpty(),"Normal taint harvest produced block item");
                var result=drops(f,f.pos,state,silk);h.assertTrue(result.size()==1&&result.get(0).is(state.getBlock().asItem()),"Original Silk block drop missing for "+id);
            }
        }h.succeed();
    }
    @GameTest(template=TEMPLATE) public static void taintRockHighFortuneYieldsFluxCrystalRatherThanRock(GameTestHelper h) {
        try(var f=new Fixture(h,false)) { ItemStack tool=new ItemStack(Items.DIAMOND_PICKAXE);tool.enchant(Enchantments.BLOCK_FORTUNE,14);
            var result=drops(f,f.pos,TaintModule.block("taint_rock").defaultBlockState(),tool);
            h.assertTrue(result.size()==1&&AspectCrystalItem.crystalAspect(result.get(0))==Aspect.FLUX&&result.get(0).getCount()==1,"Rock fortune branch did not yield exactly one Vitium crystal");
        }h.succeed();
    }
    @GameTest(template=TEMPLATE) public static void taintLogDropsItselfWithoutSilkAndDoesNotDropAxisMetadata(GameTestHelper h) {
        try(var f=new Fixture(h,false)) { var state=TaintModule.block("taint_log").defaultBlockState().setValue(TaintLogBlock.AXIS,Direction.Axis.Z);var result=drops(f,f.pos,state,ItemStack.EMPTY);
            h.assertTrue(result.size()==1&&result.get(0).is(state.getBlock().asItem())&&result.get(0).getTag()==null,"Ordinary taint log drop lost or retained placement data");
        }h.succeed();
    }
    @GameTest(template=TEMPLATE) public static void taintContactAddsTwoHundredTickEffectAndExemptsUndead(GameTestHelper h) {
        try(var f=new Fixture(h,false)) { Cow cow=EntityType.COW.create(f.level);Zombie zombie=EntityType.ZOMBIE.create(f.level);
            long seed=0;while(RandomSource.create(seed).nextInt(250)!=0) seed++;f.level.random.setSeed(seed);
            TaintModule.block("taint_soil").stepOn(f.level,f.pos,TaintModule.block("taint_soil").defaultBlockState(),cow);
            h.assertTrue(cow.hasEffect(InfusionEffects.FLUX_TAINT.get())&&cow.getEffect(InfusionEffects.FLUX_TAINT.get()).getDuration()==200,"Solid taint contact duration changed");
            f.level.random.setSeed(seed);TaintModule.block("taint_soil").stepOn(f.level,f.pos,TaintModule.block("taint_soil").defaultBlockState(),zombie);h.assertTrue(!zombie.hasEffect(InfusionEffects.FLUX_TAINT.get()),"Taint terrain infected undead");
        }h.succeed();
    }
    @GameTest(template=TEMPLATE) public static void spreadDoesNotLoadRemoteChunkOrReadProtectedTerrain(GameTestHelper h) {
        try(var f=new Fixture(h,false)) { var far=new BlockPos(f.pos.getX()+1000000,f.pos.getY(),f.pos.getZ()+1000000);var chunk=new ChunkPos(far);
            h.assertTrue(f.level.getChunkSource().getChunkNow(chunk.x,chunk.z)==null,"Unloaded fixture was loaded");TaintHelper.spreadFibres(f.level,far,true);
            h.assertTrue(f.level.getChunkSource().getChunkNow(chunk.x,chunk.z)==null,"Taint forced a foreign chunk to load");
        }h.succeed();
    }
}
