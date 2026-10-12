package thaumcraft.golemancy.entity;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;
import thaumcraft.catalog.CatalogModule;
import thaumcraft.golemancy.seals.core.*;
import thaumcraft.research.KnowledgeStore;
import thaumcraft.research.PlayerKnowledge;
import java.util.*;

/** Real server flight; paid placer/property data and arena/research are explicit fixtures. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class GolemFlyerGameTests {
    private static final String TEMPLATE = "essentia_network";
    private static final long FLYER = 3L << 32;
    private GolemFlyerGameTests() {}
    private static ThaumcraftGolemEntity flyer(GameTestHelper h, Vec3 relative) {
        var type = ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.fromNamespaceAndPath("thaumcraft", "golem"));
        var golem = (ThaumcraftGolemEntity)type.create(h.getLevel());
        golem.setPos(h.absoluteVec(relative)); golem.setProps(FLYER); golem.setHome(golem.blockPosition()); golem.setValidSpawn();
        return golem;
    }
    private static FakePlayer owner(GameTestHelper h) {
        var owner = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "flyer_qa"));
        owner.setGameMode(GameType.SURVIVAL); owner.setNoGravity(true); return owner;
    }
    private static void arena(GameTestHelper h) {
        for (int x = 0; x < 9; x++) for (int z = 0; z < 9; z++) {
            h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            for (int y = 2; y <= 10; y++) h.setBlock(new BlockPos(x, y, z), Blocks.AIR);
        }
        // Only fixture setup loads this region; operational path building rejects absent chunks.
        BlockPos center = h.absolutePos(new BlockPos(4, 2, 4));
        for (int x = (center.getX() - 64) >> 4; x <= (center.getX() + 64) >> 4; x++)
            for (int z = (center.getZ() - 64) >> 4; z <= (center.getZ() + 64) >> 4; z++) h.getLevel().getChunk(x, z);
    }
    private static void isolateHome(ThaumcraftGolemEntity golem) {
        golem.goalSelector.getAvailableGoals().stream().map(net.minecraft.world.entity.ai.goal.WrappedGoal::getGoal)
                .filter(goal -> goal instanceof SealTaskGoal).toList().forEach(golem.goalSelector::removeGoal);
    }

    @GameTest(template = TEMPLATE)
    public static void levitatingPlacerPaysOnceAndRestoresFlightOwnerHomeCargoAndXp(GameTestHelper h) {
        BlockPos floor = new BlockPos(4, 1, 4); h.setBlock(floor, Blocks.STONE);
        FakePlayer owner = owner(h); owner.setPos(h.absoluteVec(new Vec3(6.5, 2, 4.5)));
        ItemStack paid = CatalogModule.stack("golem"); paid.setCount(2); paid.getOrCreateTag().putLong("props", FLYER); paid.getOrCreateTag().putInt("xp", 213);
        owner.setItemInHand(InteractionHand.MAIN_HAND, paid);
        BlockPos source = h.absolutePos(floor);
        UseOnContext context = new UseOnContext(owner, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(source).add(0, .5, 0), Direction.UP, source, false));
        h.assertTrue(paid.onItemUseFirst(context).consumesAction() && paid.getCount() == 1, "Actual Flyer placer did not debit exactly one paid item");
        var placed = h.getLevel().getEntitiesOfClass(ThaumcraftGolemEntity.class, new AABB(source).inflate(1));
        h.assertTrue(placed.size() == 1, "Flyer placement created no worker or duplicate workers");
        var golem = placed.get(0); golem.holdItem(new ItemStack(Items.APPLE, 3));
        CompoundTag tag = new CompoundTag(); golem.addAdditionalSaveData(tag);
        var restored = flyer(h, new Vec3(4.5, 2, 4.5)); restored.readAdditionalSaveData(tag);
        h.assertTrue(restored.props() == FLYER && restored.isOwned() && restored.isOwner(owner) && restored.isValidSpawn()
                && restored.homePosition().equals(golem.homePosition()) && restored.rankXp() == 213 && restored.getMainHandItem().is(Items.APPLE)
                && restored.getMainHandItem().getCount() == 3 && restored.isNoGravity() && restored.getNavigation() instanceof GolemNavigation.Air && !restored.hasTask(),
                "Flyer reload lost saved operational properties or invented a transient task");
        h.assertTrue(restored.getMaxHealth() == 12 && restored.getArmorValue() == 1 && Math.abs(restored.moveSpeed() - .87) < 1e-6,
                "Original FRAGILE health/armor and Flyer speed changed");
        golem.discard(); restored.discard(); h.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void flightControllerPreservesOriginalAnisotropicAccelerationAndArrivalDamping(GameTestHelper h) {
        var golem = flyer(h, new Vec3(4.5, 3, 4.5));
        golem.setDeltaMovement(Vec3.ZERO); golem.getMoveControl().setWantedPosition(golem.getX() + 3, golem.getY() + 4, golem.getZ(), 2); golem.getMoveControl().tick();
        h.assertTrue(Math.abs(golem.getDeltaMovement().x - .0396) < 1e-7 && Math.abs(golem.getDeltaMovement().y - .02) < 1e-7
                && golem.getDeltaMovement().z == 0, "Lift used vanilla bird speed or a different normalization than BETA26");
        golem.setDeltaMovement(new Vec3(.2, .1, -.4)); golem.getMoveControl().setWantedPosition(golem.getX(), golem.getY(), golem.getZ(), 1); golem.getMoveControl().tick();
        h.assertTrue(golem.getDeltaMovement().distanceToSqr(new Vec3(.1, .05, -.2)) < 1e-10, "Arrival did not damp all axes by one half");
        golem.getMoveControl().tick(); h.assertTrue(golem.getDeltaMovement().distanceToSqr(new Vec3(.1, .05, -.2)) < 1e-10, "WAIT added artificial hover motion");
        h.assertTrue(!golem.causeFallDamage(60, 1, h.getLevel().damageSources().fall()) && golem.getHealth() == 10, "Flyer took fall damage"); h.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void flightPathUsesSixFaceNodesAndRejectsSolidSpaceWithoutGroundSupport(GameTestHelper h) {
        arena(h); var golem = flyer(h, new Vec3(1.5, 3, 4.5));
        for (int z = 0; z < 9; z++) for (int y = 2; y < 7; y++) h.setBlock(new BlockPos(4, y, z), Blocks.STONE);
        Path path = golem.getNavigation().createPath(h.absolutePos(new BlockPos(7, 8, 4)), 0);
        h.assertTrue(path != null && path.canReach() && path.getEndNode().y == h.absolutePos(new BlockPos(7, 8, 4)).getY(), "Air path could not reach unsupported elevated target");
        boolean aboveWall = false;
        for (int i = 0; i < path.getNodeCount(); i++) {
            Node node = path.getNode(i); h.assertTrue(h.getLevel().getBlockState(node.asBlockPos()).getCollisionShape(h.getLevel(), node.asBlockPos()).isEmpty(), "Flight route crossed solid wall");
            aboveWall |= node.y >= h.absolutePos(new BlockPos(4, 7, 4)).getY();
            if (i > 0) { Node previous = path.getNode(i - 1); h.assertTrue(Math.abs(node.x - previous.x) + Math.abs(node.y - previous.y) + Math.abs(node.z - previous.z) == 1, "Original six-neighbor flight became diagonal bird navigation"); }
        }
        h.assertTrue(aboveWall, "Flight path never rose over the wall"); h.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void stationaryFlyerActuallyRisesToItsHomeAndHoversWithNativeAI(GameTestHelper h) {
        arena(h); var golem = flyer(h, new Vec3(4.5, 2, 4.5)); isolateHome(golem);
        h.assertTrue(h.getLevel().addFreshEntity(golem), "Flyer fixture did not enter server entity ticking");
        BlockPos destination = h.absolutePos(new BlockPos(4, 8, 4)); h.runAfterDelay(2, () -> golem.setHome(destination));
        Vec3[] previous = {golem.position()}; boolean[] path = {false}; int[] moving = {0}; double[] high = {golem.getY()};
        h.onEachTick(() -> {
            if (golem.isRemoved()) return;
            try { double step = golem.position().distanceToSqr(previous[0]); h.assertTrue(step < 1, "Home flight teleported instead of navigating");
                if (step > 1e-6) moving[0]++; high[0] = Math.max(high[0], golem.getY()); path[0] |= !golem.getNavigation().isDone(); previous[0] = golem.position();
            } catch (Throwable failure) { golem.discard(); throw failure; }
        });
        h.runAfterDelay(240, () -> { try {
            h.assertTrue(golem.isAlive() && golem.isNoGravity() && path[0] && moving[0] >= 20 && high[0] > destination.getY() - 2
                    && Math.abs(golem.getY() - destination.getY()) < 2 && !golem.onGround(), "Real Flyer home lift/hover failed: pos=" + golem.position() + ",path=" + path[0] + ",moving=" + moving[0]);
            h.succeed();
        } finally { golem.discard(); } });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void bellDirectedFlyerActuallyFollowsOwnerAcrossGapAndOverWall(GameTestHelper h) {
        arena(h);
        for (int x = 3; x <= 5; x++) for (int z = 0; z < 9; z++) h.setBlock(new BlockPos(x, 1, z), Blocks.AIR);
        for (int z = 0; z < 9; z++) for (int y = 2; y <= 5; y++) h.setBlock(new BlockPos(4, y, z), Blocks.STONE);
        FakePlayer owner = owner(h); owner.setPos(h.absoluteVec(new Vec3(2.5, 2, 4.5)));
        var golem = flyer(h, new Vec3(.5, 2, 4.5)); golem.setOwnerId(owner.getUUID());
        Runnable cleanup = () -> { golem.discard(); h.getLevel().removePlayerImmediately(owner, Entity.RemovalReason.DISCARDED); };
        try {
            h.getLevel().addNewPlayer(owner); h.assertTrue(h.getLevel().addFreshEntity(golem) && golem.getOwnerEntity() == owner, "Indexed Flyer owner missing");
            try { var method = PlayerKnowledge.class.getDeclaredMethod("setResearchStage", String.class, int.class); method.setAccessible(true); method.invoke(KnowledgeStore.get(owner), "GOLEMDIRECT", 1); }
            catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
            owner.setItemInHand(InteractionHand.MAIN_HAND, CatalogModule.stack("golem_bell")); golem.mobInteract(owner, InteractionHand.MAIN_HAND);
            owner.setPos(h.absoluteVec(new Vec3(8.5, 8, 4.5)));
            h.assertTrue(golem.distanceToSqr(owner) >= 100 && golem.distanceToSqr(owner) < 144, "Flyer follow fixture allows teleport fallback");
            Vec3[] previous = {golem.position()}; boolean[] active = {false}, flewOver = {false}; int[] moving = {0}; double[] closest = {golem.distanceToSqr(owner)};
            h.onEachTick(() -> { if (golem.isRemoved()) return; try {
                double step = golem.position().distanceToSqr(previous[0]); h.assertTrue(step < 1, "Follow flight used fallback teleport");
                if (step > 1e-6) moving[0]++; active[0] |= !golem.getNavigation().isDone();
                Vec3 current = h.relativeVec(golem.position()); flewOver[0] |= current.x >= 4 && current.x <= 5 && current.y >= 6;
                closest[0] = Math.min(closest[0], golem.distanceToSqr(owner));
                previous[0] = golem.position();
            } catch (Throwable failure) { cleanup.run(); throw failure; } });
            h.runAfterDelay(320, () -> { try {
                // BETA26 arrival halves velocity once, then WAIT retains ordinary noGravity drag.
                // Measure reaching the live owner's original two-block follow threshold, rather
                // than accidentally asserting that a later drifting tick remains at that point.
                h.assertTrue(golem.isAlive() && golem.isFollowingOwner() && !golem.hasTask() && active[0] && moving[0] >= 20 && flewOver[0] && closest[0] <= 4,
                        "Native follow did not fly across gap/over wall to its live owner: pos=" + golem.position() + ",owner=" + owner.position() + ",path=" + active[0] + ",moving=" + moving[0] + ",over=" + flewOver[0] + ",closestSq=" + closest[0] + ",finalSq=" + golem.distanceToSqr(owner)); h.succeed();
            } finally { cleanup.run(); } });
        } catch (Throwable failure) { cleanup.run(); throw failure; }
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 340)
    public static void realFlyerCollectsElevatedSealTargetAndReloadClearsTransientTask(GameTestHelper h) {
        arena(h);
        // Higher physical face isolates this seal from historical suites that reused y1/y7.
        // Never remove another suite's existing owned seal to make this fixture fit.
        for (int x = 0; x < 9; x++) for (int z = 0; z < 9; z++) for (int y = 11; y <= 18; y++) h.setBlock(new BlockPos(x, y, z), Blocks.AIR);
        BlockPos support = new BlockPos(4, 15, 4);
        var golem = flyer(h, new Vec3(4.5, 2, 4.5)); UUID owner = UUID.randomUUID(); golem.setOwnerId(owner);
        SealService service = SealService.get(h.getLevel()); SealData seal = new SealData(new SealPos(h.absolutePos(support), Direction.UP), "thaumcraft:pickup", owner);
        seal.locked(true); seal.blacklist(false); seal.filter(0, new ItemStack(Items.DIAMOND));
        ItemEntity[] dropped = {null}; boolean[] initialized = {false};
        Runnable cleanup = () -> { if(service.seal(seal.position())==seal)service.remove(seal.position(), true); golem.discard(); if(dropped[0]!=null)dropped[0].discard(); };
        h.assertTrue(h.getLevel().addFreshEntity(golem), "Flight task worker fixture missing");
        boolean[] claimed = {false}, path = {false}; double[] high = {golem.getY()}; Vec3[] previous = {golem.position()};
        int[] previousNativeTick = {golem.tickCount}; long[] previousWorldTick = {h.getLevel().getGameTime()};
        boolean[] previousFirstRun = {ObfuscationReflectionHelper.getPrivateValue(ThaumcraftGolemEntity.class,golem,"firstRun")};
        boolean[] initialObserved = {false};
        h.onEachTick(() -> { if (golem.isRemoved()) return; try {
            claimed[0] |= golem.hasTask(); path[0] |= !golem.getNavigation().isDone(); high[0] = Math.max(high[0], golem.getY());
            Vec3 current = golem.position(), nativePrevious = new Vec3(golem.xo,golem.yo,golem.zo);
            boolean firstRun = ObfuscationReflectionHelper.getPrivateValue(ThaumcraftGolemEntity.class,golem,"firstRun");
            String observation = "previous="+previous[0]+",current="+current+",nativePrevious="+nativePrevious
                    +",stepSq="+current.distanceToSqr(previous[0])+",nativeStepSq="+current.distanceToSqr(nativePrevious)
                    +",entityTick="+golem.tickCount+",entityElapsed="+(golem.tickCount-previousNativeTick[0])
                    +",worldTick="+h.getLevel().getGameTime()+",worldElapsed="+(h.getLevel().getGameTime()-previousWorldTick[0])
                    +",firstRun="+previousFirstRun[0]+"->"+firstRun+",home="+golem.homePosition()
                    +",velocity="+golem.getDeltaMovement()+",noGravity="+golem.isNoGravity()+",task="+golem.hasTask();
            if(!initialObserved[0]&&golem.tickCount>0){initialObserved[0]=true;System.out.println("THAUMCRAFT_FLYER_INITIAL_NATIVE_OBSERVATION "+observation);}
            h.assertTrue(current.distanceToSqr(previous[0]) < 1, "Elevated seal flight teleported: "+observation);
            previous[0] = current; previousNativeTick[0] = golem.tickCount; previousWorldTick[0] = h.getLevel().getGameTime(); previousFirstRun[0] = firstRun;
            if(!initialized[0]&&golem.tickCount>=1&&!firstRun){
                // Recovery of the saved home is an original initialization behavior. Enable
                // this task's ceiling only after observing that real native phase complete.
                // The whole pickup route retains the same strict per-observation movement bound.
                h.assertTrue(!golem.hasTask()&&golem.blockPosition().equals(golem.homePosition()),"Idle native initialization did not remain at its physical home: "+observation);
                h.setBlock(support,Blocks.STONE);SealData existing=service.seal(seal.position());
                h.assertTrue(service.add(seal),"Elevated physical pickup seal missing: pos="+seal.position()+",existingType="+(existing==null?"none":existing.type()));
                Vec3 target=h.absoluteVec(new Vec3(4.5,16.1,4.5));dropped[0]=new ItemEntity(h.getLevel(),target.x,target.y,target.z,new ItemStack(Items.DIAMOND,3));
                // A resting physical drop settles by native gravity/collision on this support.
                dropped[0].setDeltaMovement(Vec3.ZERO);dropped[0].setNoPickUpDelay();
                h.assertTrue(h.getLevel().addFreshEntity(dropped[0]),"Flight task drop fixture missing");initialized[0]=true;
                h.runAfterDelay(10,()->{try{
                    h.assertTrue(dropped[0].isAlive()&&dropped[0].onGround()&&seal.bounds().intersects(dropped[0].getBoundingBox())&&h.getBlockState(support).is(Blocks.STONE),
                            "Elevated drop failed native landing inside Collect bounds: pos="+dropped[0].position()+",ground="+dropped[0].onGround()+",bounds="+seal.bounds()+",support="+h.getBlockState(support));
                }catch(Throwable failure){cleanup.run();throw failure;}});
            }
            if (golem.getMainHandItem().is(Items.DIAMOND) && golem.getMainHandItem().getCount() == 3) {
                h.assertTrue(initialized[0]&&dropped[0]!=null&&dropped[0].isRemoved() && claimed[0] && path[0] && high[0] > h.absolutePos(new BlockPos(4, 14, 4)).getY(), "Pickup used remote inventory mutation instead of actual flight claim");
                CompoundTag saved = new CompoundTag(); golem.addAdditionalSaveData(saved); var restored = flyer(h, new Vec3(4.5, 2, 4.5)); restored.readAdditionalSaveData(saved);
                h.assertTrue(!restored.hasTask() && restored.props() == FLYER && restored.getMainHandItem().getCount() == 3 && restored.isNoGravity(), "Reload persisted claimed task or lost actual elevated pickup cargo");
                restored.discard(); cleanup.run(); h.succeed();
            }
        } catch (Throwable failure) { cleanup.run(); throw failure; } });
        h.runAfterDelay(300, () -> { if (!golem.isRemoved()) { try { h.fail("Elevated pickup never completed by actual Flyer: pos=" + golem.position() + ",task=" + claimed[0] + ",path=" + path[0] + ",cargo=" + golem.getMainHandItem()
                + ",initialized="+initialized[0]+",dropPos=" + (dropped[0]==null?"none":dropped[0].position()) + ",dropGround=" + (dropped[0]!=null&&dropped[0].onGround()) + ",sealPresent=" + (service.seal(seal.position()) == seal)
                + ",queued=" + service.tasks().stream().filter(task -> seal.position().equals(task.sealPosition())).count()); } finally { cleanup.run(); } } });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void firstNativeFlyerTickCrossingHomeBoundaryUsesOriginalUpwardRecovery(GameTestHelper h) {
        arena(h);
        for(int x=0;x<9;x++)for(int z=0;z<9;z++)for(int y=11;y<=18;y++)h.setBlock(new BlockPos(x,y,z),Blocks.AIR);
        BlockPos support=new BlockPos(4,15,4);h.setBlock(support,Blocks.STONE);
        var golem=flyer(h,new Vec3(4.5,2,4.5));isolateHome(golem);
        Vec3 initial=golem.position();BlockPos home=golem.homePosition();long worldStart=h.getLevel().getGameTime();
        // Native horizontal travel of .6 starts from the center and crosses the integer
        // home's east face while remaining below the task's original one-block limit.
        golem.setDeltaMovement(new Vec3(.6,0,0));
        h.assertTrue(initial.y==Math.floor(initial.y)&&golem.blockPosition().equals(home)&&golem.getDeltaMovement().lengthSqr()<1
                &&!golem.isNoAi()&&golem.isNoGravity(),"Adversarial integer-home motion fixture changed");
        h.assertTrue(h.getLevel().addFreshEntity(golem),"Native initialization worker not accepted");
        h.onEachTick(()->{if(golem.isRemoved()||golem.tickCount==0)return;try{
            boolean firstRun=ObfuscationReflectionHelper.getPrivateValue(ThaumcraftGolemEntity.class,golem,"firstRun");
            Vec3 expected=h.absoluteVec(new Vec3(4.5,14,4.5));Vec3 nativePrevious=new Vec3(golem.xo,golem.yo,golem.zo);
            String observation="initial="+initial+",nativePrevious="+nativePrevious+",current="+golem.position()+",entityTick="+golem.tickCount
                    +",worldElapsed="+(h.getLevel().getGameTime()-worldStart)+",velocity="+golem.getDeltaMovement()+",firstRun="+firstRun+",home="+home;
            System.out.println("THAUMCRAFT_FLYER_INITIAL_NATIVE_RECOVERY "+observation);
            h.assertTrue(golem.tickCount==1&&h.getLevel().getGameTime()-worldStart==1&&!firstRun&&nativePrevious.distanceToSqr(initial)<1e-10
                    &&golem.position().distanceToSqr(expected)<1e-10&&golem.position().distanceToSqr(initial)>100&&golem.homePosition().equals(home)
                    &&!golem.hasTask()&&golem.getMainHandItem().isEmpty()&&golem.isNoGravity(),
                    "Actual first native tick did not retain original upward home recovery: "+observation);
            h.succeed();
        }finally{golem.discard();}});
    }
}
