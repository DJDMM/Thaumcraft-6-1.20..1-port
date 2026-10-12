package thaumcraft.essentia.production;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.*;
import thaumcraft.catalog.blocks.CatalogBlocks;

/** Native furnace progress and six-way tile/redstone regressions against BETA26. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class BellowsGameTests {
    private BellowsGameTests() {}
    private static FurnaceBlockEntity furnace(GameTestHelper h,BlockPos relative) {
        var pos=h.absolutePos(relative);h.getLevel().setBlock(pos,Blocks.FURNACE.defaultBlockState(),3);
        return (FurnaceBlockEntity)h.getLevel().getBlockEntity(pos);
    }
    private static BellowsBlockEntity bellows(GameTestHelper h,BlockPos relative,Direction face) {
        var pos=h.absolutePos(relative);h.getLevel().setBlock(pos,CatalogBlocks.block("bellows").defaultBlockState().setValue(SmelterBellowsBlock.FACING,face),3);
        return (BellowsBlockEntity)h.getLevel().getBlockEntity(pos);
    }
    private static void tick(GameTestHelper h,BellowsBlockEntity b,int count) { for(int i=0;i<count;i++)BellowsBlockEntity.tick(h.getLevel(),b.getBlockPos(),b.getBlockState(),b); }
    @GameTest(template="essentia_production")
    public static void originalNativeTileSixDirectionsAndInvisibleWorldModel(GameTestHelper h) {
        var center=h.absolutePos(new BlockPos(6,3,6));
        for(var face:Direction.values()) {
            var b=bellows(h,new BlockPos(6,3,6).relative(face),face.getOpposite());
            h.assertTrue(b.getType()==EssentiaProductionModule.BELLOWS.get()&&BellowsBlockEntity.active(h.getLevel(),b.getBlockPos(),face.getOpposite())&&!BellowsBlockEntity.active(h.getLevel(),b.getBlockPos(),face),"Native six-way orientation missing"+face);
            h.assertTrue(b.getBlockState().getRenderShape()==net.minecraft.world.level.block.RenderShape.INVISIBLE&&b.inflation()==1&&b.delay()==0,"Bellows original tile/render state changed");
            h.assertTrue(b.getBlockState().getDestroySpeed(h.getLevel(),b.getBlockPos())==1&&b.getBlockState().getBlock().getExplosionResistance()==12,"Original hardness1/effective resistance12 changed");
        }
        h.assertTrue(h.getLevel().getBlockEntity(center)==null,"Bellows created a phantom consumer");h.succeed();
    }
    @GameTest(template="essentia_production")
    public static void exactTwoEnabledTickCadenceChangesOnlyCookingProgress(GameTestHelper h) {
        var f=furnace(h,new BlockPos(6,2,6));var b=bellows(h,new BlockPos(5,2,6),Direction.EAST);
        f.cookingProgress=80;var before=f.saveWithFullMetadata();tick(h,b,1);
        h.assertTrue(f.cookingProgress==80&&b.delay()==1,"Boost fired beforesecondtick");tick(h,b,1);
        var after=f.saveWithFullMetadata();h.assertTrue(f.cookingProgress==81&&b.delay()==0,"Original2tick+1 notpreserved");
        before.remove("CookTime");after.remove("CookTime");h.assertTrue(before.equals(after),"Bellows mutatednativefuel/input/output/recipe time");h.succeed();
    }
    @GameTest(template="essentia_production")
    public static void literalZeroAnd199CookBoundsAndShortRecipeProgressArePreserved(GameTestHelper h) {
        var f=furnace(h,new BlockPos(6,2,6));var b=bellows(h,new BlockPos(5,2,6),Direction.EAST);
        for(int value:new int[]{-1,0,1,198,199,200}) {
            f.cookingProgress=value;tick(h,b,2);int expected=value>0&&value<199?value+1:value;
            h.assertTrue(f.cookingProgress==expected,"Literal BETA26 >0/<199 bound changed:"+value);
        }
        var saved=f.saveWithFullMetadata();saved.putShort("CookTimeTotal",(short)80);saved.putShort("CookTime",(short)79);f.load(saved);tick(h,b,2);
        h.assertTrue(f.cookingProgress==80&&f.getItem(2).isEmpty(),"Bellows invented recipe finish or substitutedmoderncooktimebound");h.succeed();
    }
    @GameTest(template="essentia_production")
    public static void disabledPumpFreezesDelayAndRedstoneResumesSamePhase(GameTestHelper h) {
        var f=furnace(h,new BlockPos(6,2,6));var b=bellows(h,new BlockPos(5,2,6),Direction.EAST);f.cookingProgress=50;tick(h,b,1);
        h.getLevel().setBlock(b.getBlockPos().north(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);tick(h,b,10);
        h.assertTrue(!b.getBlockState().getValue(SmelterBellowsBlock.ENABLED)&&b.delay()==1&&f.cookingProgress==50,"Redstone resetphaseorboosteddisabledpump");
        h.getLevel().setBlock(b.getBlockPos().north(),Blocks.AIR.defaultBlockState(),3);tick(h,b,1);
        h.assertTrue(b.getBlockState().getValue(SmelterBellowsBlock.ENABLED)&&b.delay()==0&&f.cookingProgress==51,"Unpoweredpumpdidnotresumeoriginalphase");h.succeed();
    }
    @GameTest(template="essentia_production")
    public static void wrongFacingAndModernSmokerBlastFurnaceNeverGainOriginalFurnaceBoost(GameTestHelper h) {
        var f=furnace(h,new BlockPos(6,2,6));var b=bellows(h,new BlockPos(5,2,6),Direction.WEST);f.cookingProgress=60;tick(h,b,4);h.assertTrue(f.cookingProgress==60,"Outwardpumpboostedneighbor");
        h.getLevel().setBlock(b.getBlockPos(),b.getBlockState().setValue(SmelterBellowsBlock.FACING,Direction.EAST),3);
        for(var block:new net.minecraft.world.level.block.Block[]{Blocks.SMOKER,Blocks.BLAST_FURNACE}) {
            h.getLevel().setBlock(f.getBlockPos(),block.defaultBlockState(),3);var modern=(AbstractFurnaceBlockEntity)h.getLevel().getBlockEntity(f.getBlockPos());modern.cookingProgress=60;tick(h,b,4);h.assertTrue(modern.cookingProgress==60,"Bellows invented moderncookingconsumer"+block);
        }h.succeed();
    }
    @GameTest(template="essentia_production")
    public static void delayAndAnimationAreTransientOnReloadAndStaleTileCannotBoost(GameTestHelper h) {
        var f=furnace(h,new BlockPos(6,2,6));var b=bellows(h,new BlockPos(5,2,6),Direction.EAST);f.cookingProgress=20;tick(h,b,1);
        var save=b.saveWithFullMetadata();var restored=new BellowsBlockEntity(b.getBlockPos(),b.getBlockState());restored.load(save);
        h.assertTrue(restored.delay()==0&&restored.inflation()==1&&!save.contains("Delay")&&!save.contains("Inflation"),"Originaltransientpumpstatesserialized");
        h.getLevel().setBlock(b.getBlockPos(),Blocks.AIR.defaultBlockState(),3);tick(h,b,4);h.assertTrue(f.cookingProgress==20,"RemovedBellowsboostedconsumer");h.succeed();
    }
    @GameTest(template="essentia_production",timeoutTicks=200)
    public static void actualWorldFurnaceCompletesBeforeUnboostedControlWithoutExtraFuel(GameTestHelper h) {
        var boosted=furnace(h,new BlockPos(4,2,6));var control=furnace(h,new BlockPos(8,2,6));bellows(h,new BlockPos(3,2,6),Direction.EAST);
        for(var furnace:new FurnaceBlockEntity[]{boosted,control}) { furnace.setItem(0,new ItemStack(Items.COBBLESTONE));furnace.setItem(1,new ItemStack(Items.COAL)); }
        h.runAtTickTime(100,()->h.assertTrue(boosted.cookingProgress>control.cookingProgress+40&&boosted.getItem(0).getCount()==1&&control.getItem(0).getCount()==1,"Realnativeworldticksdidnotapplypumpboost"));
        h.runAtTickTime(150,()-> {h.assertTrue(boosted.getItem(0).isEmpty()&&boosted.getItem(2).is(Items.STONE)&&boosted.getItem(2).getCount()==1&&control.getItem(0).is(Items.COBBLESTONE)&&control.getItem(2).isEmpty(),"Nativepaidfurnacefinishdidnotoccurbeforecontrol");
            h.assertTrue(boosted.getItem(1).isEmpty()&&control.getItem(1).isEmpty(),"Bellows consumed extra fuel");h.succeed();});
    }
}
