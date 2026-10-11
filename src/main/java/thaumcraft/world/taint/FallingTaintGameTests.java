package thaumcraft.world.taint;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import thaumcraft.catalog.entities.VisualEntitiesModule;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class FallingTaintGameTests {
    private static FallingTaintEntity create(GameTestHelper h,BlockPos source,BlockPos target) {
        var e=(FallingTaintEntity)VisualEntitiesModule.EFFECTS.get("falling_taint").get().create(h.getLevel());
        e.setPos(target.getX()+.5,target.getY()+.5,target.getZ()+.5);e.initializeFallingTaint(h.getLevel().getBlockState(source),source);return e;
    }
    @GameTest(template="empty") public static void changedSourceCannotBeStolenByFallingTaint(GameTestHelper h) {
        var source=h.absolutePos(new BlockPos(2,70,2));h.getLevel().setBlockAndUpdate(source,TaintModule.block("taint_crust").defaultBlockState());
        var entity=create(h,source,source);h.getLevel().setBlockAndUpdate(source,Blocks.DIAMOND_BLOCK.defaultBlockState());
        entity.tick();h.assertTrue(entity.isRemoved()&&h.getLevel().getBlockState(source).is(Blocks.DIAMOND_BLOCK),"Falling stale owner erased replaced source");h.succeed();
    }
    @GameTest(template="empty") public static void nativeFallingCrustTransfersOneSourceToLandingWithoutItemDrop(GameTestHelper h) {
        var source=h.absolutePos(new BlockPos(2,70,2));var landing=source.below(3);
        h.getLevel().setBlockAndUpdate(source,TaintModule.block("taint_crust").defaultBlockState());
        for(int y=1;y<=3;y++)h.getLevel().setBlockAndUpdate(source.below(y),Blocks.AIR.defaultBlockState());
        h.getLevel().setBlockAndUpdate(landing.below(),Blocks.BEDROCK.defaultBlockState());
        var entity=create(h,source,source);h.getLevel().addFreshEntity(entity);
        h.runAtTickTime(35,()->{
            h.assertTrue(entity.isRemoved()&&h.getLevel().getBlockState(source).isAir()&&h.getLevel().getBlockState(landing).is(TaintModule.block("taint_crust")),
                    "Native falling block did not transfer one source to its landing");
            h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(landing).inflate(1)).isEmpty(),"Falling block invented item fallback");
            h.getLevel().setBlockAndUpdate(landing,Blocks.AIR.defaultBlockState());h.succeed();
        });
    }
    @GameTest(template="empty") public static void fallingTaintSaveRestoresStateOriginAndUnsignedByteTimer(GameTestHelper h) {
        var source=h.absolutePos(new BlockPos(2,70,2));h.getLevel().setBlockAndUpdate(source,TaintModule.block("taint_rock").defaultBlockState());
        var entity=create(h,source,source);var tag=entity.saveWithoutId(new CompoundTag());tag.putByte("Time",(byte)200);
        var restored=(FallingTaintEntity)VisualEntitiesModule.EFFECTS.get("falling_taint").get().create(h.getLevel());restored.load(tag);
        var saved=restored.saveWithoutId(new CompoundTag());
        h.assertTrue(restored.getFallingState().is(TaintModule.block("taint_rock"))&&(saved.getByte("Time")&255)==200
                &&saved.getLong("Old")==source.asLong()&&!saved.contains("VisualOnly"),"Falling state/time/origin lost or visual placeholder saved");
        h.getLevel().setBlockAndUpdate(source,Blocks.AIR.defaultBlockState());h.succeed();
    }
    @GameTest(template="empty") public static void fallingTaintBelowBuildHeightStillAgesAndExpires(GameTestHelper h) {
        var source=h.absolutePos(new BlockPos(2,70,2));h.getLevel().setBlockAndUpdate(source,TaintModule.block("taint_crust").defaultBlockState());
        var entity=create(h,source,source);var tag=entity.saveWithoutId(new CompoundTag());tag.putByte("Time",(byte)100);
        entity.load(tag);entity.setPos(source.getX()+.5,h.getLevel().getMinBuildHeight()-2,source.getZ()+.5);entity.tick();
        h.assertTrue(entity.isRemoved(),"Loaded-XZ guard froze falling block outside build height instead of timeout");
        h.assertTrue(h.getLevel().getBlockState(source).is(TaintModule.block("taint_crust")),"Expired restored fall stole its source");
        h.getLevel().setBlockAndUpdate(source,Blocks.AIR.defaultBlockState());h.succeed();
    }
}

