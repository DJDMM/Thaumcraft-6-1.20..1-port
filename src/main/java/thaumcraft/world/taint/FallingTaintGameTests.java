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
        // The original relative Y70 arena can intersect a native underground aquifer.
        // Server1's saved source/landing were both water, independently of the completed fall.
        var base=h.absolutePos(new BlockPos(2,0,2));var source=new BlockPos(base.getX(),250,base.getZ());var landing=source.below(3);
        var level=h.getLevel();var chunk=new net.minecraft.world.level.ChunkPos(source);
        var ticket=net.minecraft.server.level.TicketType.<net.minecraft.world.level.ChunkPos>create("thaumcraft_falling_taint_qa",java.util.Comparator.comparingLong(net.minecraft.world.level.ChunkPos::toLong));
        var saved=new java.util.LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        FallingTaintEntity[] entity={null};boolean[] closed={false};
        for(int x=(source.getX()-2)>>4;x<=(source.getX()+2)>>4;x++)for(int z=(source.getZ()-2)>>4;z<=(source.getZ()+2)>>4;z++)level.getChunk(x,z);
        level.getChunkSource().addRegionTicket(ticket,chunk,2,chunk);
        Runnable cleanup=()->{if(closed[0])return;closed[0]=true;if(entity[0]!=null)entity[0].discard();
            boolean restoring=level.restoringBlockSnapshots;level.restoringBlockSnapshots=true;
            try{var rows=new java.util.ArrayList<>(saved.entrySet());java.util.Collections.reverse(rows);for(var row:rows)level.setBlock(row.getKey(),row.getValue(),3);}
            finally{level.restoringBlockSnapshots=restoring;level.getChunkSource().removeRegionTicket(ticket,chunk,2,chunk);}};
        // Witness completion when the native entity lands. An orphan crust has its own
        // random-tick decay, so its continued existence at tick35 is a different mechanic.
        h.onEachTick(()->{
            if(closed[0])return;
            try{
                if(entity[0]==null){
                    for(int x=(source.getX()-2)>>4;x<=(source.getX()+2)>>4;x++)for(int z=(source.getZ()-2)>>4;z<=(source.getZ()+2)>>4;z++){
                        long key=net.minecraft.world.level.ChunkPos.asLong(x,z);
                        if(level.getChunkSource().getChunkNow(x,z)==null||!level.areEntitiesLoaded(key)||!level.getChunkSource().isPositionTicking(key))return;
                    }
                    for(var pos:BlockPos.betweenClosed(source.offset(-1,-4,-1),source.offset(1,1,1)))
                        h.assertTrue(level.getBlockEntity(pos)==null&&level.getFluidState(pos).isEmpty(),"Dry high falling fixture contains a foreign tile/fluid at "+pos);
                    for(var pos:BlockPos.betweenClosed(source.offset(-1,-4,-1),source.offset(1,1,1))){var at=pos.immutable();saved.put(at,level.getBlockState(at));level.setBlockAndUpdate(at,Blocks.AIR.defaultBlockState());}
                    level.setBlockAndUpdate(landing.below(),Blocks.BEDROCK.defaultBlockState());level.setBlockAndUpdate(source,TaintModule.block("taint_crust").defaultBlockState());
                    h.assertTrue(level.getBlockState(source).is(TaintModule.block("taint_crust"))&&level.getBlockState(landing.below()).is(Blocks.BEDROCK),"Dry native source/bedrock setup failed");
                    for(int y=1;y<=3;y++)h.assertTrue(level.getBlockState(source.below(y)).isAir(),"Native falling three-block path was not dry air");
                    entity[0]=create(h,source,source);h.assertTrue(level.addFreshEntity(entity[0]),"Native falling fixture entity join rejected");
                    System.out.println("THAUMCRAFT_FALLING_CRUST_DRY_FIXTURE source="+source+", sourceState="+level.getBlockState(source)+", landing="+landing+", landingState="+level.getBlockState(landing)+", nativeTicking=true, entityTick="+entity[0].tickCount);
                    return;
                }
                if(!entity[0].isRemoved()||!level.getBlockState(landing).is(TaintModule.block("taint_crust")))return;
                h.assertTrue(level.getBlockState(source).isAir(),"Completed native landing retained its original source: "+level.getBlockState(source));
                h.assertTrue(level.getEntitiesOfClass(ItemEntity.class,new AABB(landing).inflate(1)).isEmpty(),"Falling block invented item fallback");
                System.out.println("THAUMCRAFT_FALLING_CRUST_DRY_COMPLETE source="+level.getBlockState(source)+", landing="+level.getBlockState(landing)+", entityTick="+entity[0].tickCount);
                cleanup.run();h.succeed();
            }catch(RuntimeException|Error failure){cleanup.run();throw failure;}
        });
        h.runAtTickTime(35,()->{String state="removed="+(entity[0]!=null&&entity[0].isRemoved())+", entity="+(entity[0]==null?"not started":entity[0].tickCount)+", source="+level.getBlockState(source)+", landing="+level.getBlockState(landing);cleanup.run();h.fail("Native falling block did not transfer one source to its landing: "+state);});
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

