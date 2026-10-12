package thaumcraft.world.taint;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.common.util.FakePlayer;
import thaumcraft.alchemy.AspectCrystalItem;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.catalog.entities.VisualEntitiesModule;
import thaumcraft.catalog.entities.VisualMobEntity;
import thaumcraft.infusion.InfusionEffects;
import thaumcraft.world.aura.AuraManager;

/** Native entity callbacks. Immediate cleanup and an upper vertical band isolate the shared QA world. */
@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class TaintSeedGameTests {
    private TaintSeedGameTests() {}
    private static Vec3 centre(GameTestHelper h) { return Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(3,180,3))); }
    private static VisualMobEntity create(GameTestHelper h, String id) {
        var mob = VisualEntitiesModule.LIVING.get(id).get().create(h.getLevel());
        mob.setPos(centre(h));
        mob.setNoGravity(true);
        return mob;
    }
    private static TaintSeedEntity seed(GameTestHelper h, boolean prime) {
        var seed = (TaintSeedEntity)create(h, prime ? "taint_seed_prime" : "taint_seed");
        seed.setNoAi(true);
        h.getLevel().addFreshEntity(seed);
        return seed;
    }
    private static TaintacleSmallEntity tiny(GameTestHelper h) {
        var tiny = (TaintacleSmallEntity)create(h, "taintacle_tiny");
        tiny.setNoAi(true);
        h.getLevel().addFreshEntity(tiny);
        return tiny;
    }
    private static Cow cow(GameTestHelper h, Vec3 pos) {
        var cow = EntityType.COW.create(h.getLevel());
        cow.setNoAi(true); cow.setNoGravity(true);
        cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100); cow.setHealth(100);
        cow.setPos(pos); h.getLevel().addFreshEntity(cow);
        return cow;
    }
    // Modern ServerLevel advances tickCount before the entity callback; calling
    // Entity.tick directly leaves the original20/1200tick boundaries unvisited.
    private static void nativeTick(Entity entity) { ((ServerLevel)entity.level()).tickNonPassenger(entity); }
    private static void pulse(TaintSeedEntity seed) { seed.tickCount = 19; nativeTick(seed); }
    private static Set<BlockPos> positions(GameTestHelper h) {
        try {
            var field = TaintHelper.class.getDeclaredField("SEEDS"); field.setAccessible(true);
            var entries = (Map<?,?>)field.get(null);
            Object value = entries.get(h.getLevel());
            return value instanceof Set<?> values ? (Set<BlockPos>)values : Set.of();
        } catch (ReflectiveOperationException exception) { throw new IllegalStateException(exception); }
    }

    @GameTest(template="empty") public static void originalSeedPrimeAndSmallStatsDimensionsAndNativeAiReplaceTheGalleryState(GameTestHelper h) {
        String[] ids = {"taint_seed", "taint_seed_prime", "taintacle_tiny"};
        double[] health = {75,150,5}, damage = {4,7,2};
        float[] width = {1.5F,2,.22F}, height = {1.25F,2,1};
        for (int i=0;i<ids.length;i++) {
            var mob=create(h,ids[i]);
            try {
                h.assertTrue(mob instanceof TaintedStationaryMob && !mob.isNoAi()
                        && mob.getType().getCategory()==net.minecraft.world.entity.MobCategory.MONSTER
                        && mob.getMaxHealth()==health[i] && mob.getAttributeValue(Attributes.ATTACK_DAMAGE)==damage[i]
                        && mob.getAttributeValue(Attributes.MOVEMENT_SPEED)==0 && mob.getAttributeValue(Attributes.FOLLOW_RANGE)==16
                        && mob.getBbWidth()==width[i] && mob.getBbHeight()==height[i],"Original native creature stats/AI/dimensions differ for"+ids[i]);
            } finally { mob.discard(); }
        }
        h.succeed();
    }
    @GameTest(template="empty") public static void allThreeStationaryEntitiesAllowOnlyDownwardPhysicalMovement(GameTestHelper h) {
        for(String id:List.of("taint_seed","taint_seed_prime","taintacle_tiny")) {
            var mob=create(h,id); Vec3 start=mob.position();
            try {
                mob.move(MoverType.SELF,new Vec3(2,3,-2));
                h.assertTrue(mob.position().equals(start),"Stationary tainted creature moved sideways/upward:"+id);
                mob.move(MoverType.SELF,new Vec3(2,-.5,-2));
                h.assertTrue(mob.getX()==start.x&&mob.getZ()==start.z&&mob.getY()==start.y-.5,"Creature stopped falling or accepted horizontal movement:"+id);
            } finally { mob.discard(); }
        }
        h.succeed();
    }
    @GameTest(template="empty") public static void seedCannotBePushedOrTravelInputMovedAndKeepsOriginalUnderwaterPersistence(GameTestHelper h) {
        var seed=seed(h,false); Vec3 start=seed.position();
        try {
            seed.moveRelative(1,new Vec3(1,1,1)); seed.setAirSupply(4); nativeTick(seed);
            h.assertTrue(!seed.isPushable()&&seed.canBeCollidedWith()&&seed.canBreatheUnderwater()
                    && !seed.removeWhenFarAway(100000)&&seed.getDeltaMovement().x==0&&seed.getDeltaMovement().z==0
                    &&seed.position().equals(start),"Original push/travel/breath/distance behavior differs");
        } finally { seed.discard(); }h.succeed();
    }
    @GameTest(template="empty") public static void taintedAlliancesAndTargetTypeRejectionUseNativeClassIdentity(GameTestHelper h) {
        var seed=seed(h,false); var ordinary=cow(h,centre(h).add(6,0,0));
        try {
            for(String id:List.of("thaumic_slime","taint_crawler","taint_swarm","taintacle","taintacle_tiny","taintacle_giant","taint_seed","taint_seed_prime")) {
                var other=create(h,id);
                try { h.assertTrue(seed.isAlliedTo(other)&&!seed.canAttackType(other.getType()),"Original ITaintedMob alliance lost:"+id); }
                finally { other.discard(); }
            }
            h.assertTrue(!seed.isAlliedTo(ordinary)&&seed.canAttackType(EntityType.COW),"Ordinary target was incorrectly tainted/allied");
        } finally { seed.discard();ordinary.discard(); }h.succeed();
    }
    @GameTest(template="empty") public static void nativeMeleeAttackActuallyDealsFourSevenAndTwoDamage(GameTestHelper h) {
        for(String id:List.of("taint_seed","taint_seed_prime","taintacle_tiny")) {
            var attacker=create(h,id);var victim=cow(h,centre(h).add(1,0,0));
            try { h.assertTrue(attacker.doHurtTarget(victim)&&victim.getHealth()==100-attacker.getAttributeValue(Attributes.ATTACK_DAMAGE),"Native released attack damage differs:"+id); }
            finally { attacker.discard();victim.discard(); }
        }
        h.succeed();
    }
    @GameTest(template="empty") public static void actualNativeAiGoalsAttackASurvivalPlayerInsteadOfKeepingNoAiPreviewBehavior(GameTestHelper h) {
        var prior=h.getLevel().getServer().getWorldData().getDifficulty();
        h.getLevel().getServer().setDifficulty(Difficulty.NORMAL,true);
        try {
            for(String id:List.of("taint_seed","taint_seed_prime","taintacle_tiny")) {
                var attacker=create(h,id);
                var player=new FakePlayer(h.getLevel(),new GameProfile(java.util.UUID.randomUUID(),"taint_combat_qa")) {
                    @Override public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source) { return false; }
                };
                // Forge's FakePlayer is otherwise always invulnerable and its no-op
                // tick never consumes ServerPlayer's60tick initial spawn protection.
                // Supply only that QA precondition; the native Player hurt path remains.
                try {
                    var protection=net.minecraft.server.level.ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
                    protection.setAccessible(true);protection.setInt(player,0);
                } catch(ReflectiveOperationException exception) { throw new IllegalStateException(exception); }
                player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);player.setNoGravity(true);player.setPos(centre(h).add(.5,0,0));
                h.getLevel().addNewPlayer(player);h.getLevel().addFreshEntity(attacker);attacker.setTarget(player);
                try {
                    float before=player.getHealth();
                    for(int i=0;i<10&&player.getHealth()==before;i++)nativeTick(attacker);
                    h.assertTrue(!attacker.isNoAi()&&player.getHealth()==before-attacker.getAttributeValue(Attributes.ATTACK_DAMAGE),"Native goal selector failed to execute the released melee attack:"+id);
                } finally { attacker.discard();player.discard();h.getLevel().players().remove(player); }
            }
        } finally { h.getLevel().getServer().setDifficulty(prior,true); }
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=240) public static void positiveBoostRunsEachTickThenStopsBetweenTwentyTickPulses(GameTestHelper h) {
        Vec3 origin=centre(h);
        AABB bounds=new AABB(origin.x-.75,origin.y,origin.z-.75,origin.x+.75,origin.y+1.25,origin.z+.75);
        // A changing full-suite grid can put this entity across a chunk edge.
        // Request the fixture's chunks, then wait for real getChunkNow readiness;
        // the production loaded-only pulse must not be bypassed by direct ticks.
        for(int x=net.minecraft.util.Mth.floor(bounds.minX)>>4;x<=net.minecraft.util.Mth.floor(bounds.maxX)>>4;x++)
            for(int z=net.minecraft.util.Mth.floor(bounds.minZ)>>4;z<=net.minecraft.util.Mth.floor(bounds.maxZ)>>4;z++)
                h.getLevel().getChunk(x,z);
        h.startSequence().thenWaitUntil(()->h.assertTrue(TaintedStationaryMob.loaded(h.getLevel(),bounds),"Boost fixture entity bounds are not natively loaded"))
                .thenExecute(()->{
                    var seed=seed(h,false);seed.setBoost(2);
                    try {
                        h.assertTrue(TaintedStationaryMob.loaded(h.getLevel(),seed.getBoundingBox()),"Actual seed fixture bounds differ from ready chunks");
                        nativeTick(seed); h.assertTrue(seed.tickCount==1&&seed.getBoost()==1&&seed.getHealth()==75,"First boosted tick differs: tick="+seed.tickCount+", boost="+seed.getBoost()+", health="+seed.getHealth());
                        nativeTick(seed); h.assertTrue(seed.tickCount==2&&seed.getBoost()==0&&seed.getHealth()==75,"Second boosted tick did not consume exactly one boost");
                        nativeTick(seed); h.assertTrue(seed.tickCount==3&&seed.getBoost()==0&&seed.getHealth()==75,"Unboosted nonpulse tick changed health/boost");
                    } finally { seed.discard(); }
                }).thenSucceed();
    }
    @GameTest(template="empty") public static void boostTagRetainsOriginalNameAndValueWhileOldGalleryNoAiIsMigrated(GameTestHelper h) {
        var seed=seed(h,true);seed.setBoost(1234);var tag=seed.saveWithoutId(new CompoundTag());
        var restored=(TaintSeedEntity)create(h,"taint_seed_prime");
        try {
            tag.putBoolean("VisualOnly",true);tag.putBoolean("NoAI",true);restored.load(tag);
            var actual=restored.saveWithoutId(new CompoundTag());
            h.assertTrue(actual.getInt("boost")==1234&&restored.getBoost()==1234&&!restored.isNoAi()&&!actual.contains("VisualOnly"),"Legacy seed stayed NoAI or lost original boost storage");
            restored.setBoost(-3);h.assertTrue(restored.saveWithoutId(new CompoundTag()).getInt("boost")==-3,"Signed original boost value was silently clamped");
        } finally { seed.discard();restored.discard(); }h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=240) public static void zeroFluxTwentyTickPulseStarvesHalfAHeartPointAndPollutesPointOne(GameTestHelper h) {
        Vec3 origin=centre(h);
        AABB bounds=new AABB(origin.x-.75,origin.y,origin.z-.75,origin.x+.75,origin.y+1.25,origin.z+.75);
        // A saved positive-base aura record does not prove native chunk readiness.
        // Prepare only this fixture's actual entity bounds, before creating its fresh seed.
        for(int x=net.minecraft.util.Mth.floor(bounds.minX)>>4;x<=net.minecraft.util.Mth.floor(bounds.maxX)>>4;x++)
            for(int z=net.minecraft.util.Mth.floor(bounds.minZ)>>4;z<=net.minecraft.util.Mth.floor(bounds.maxZ)>>4;z++)
                h.getLevel().getChunk(x,z);
        h.startSequence().thenWaitUntil(()->h.assertTrue(TaintedStationaryMob.loaded(h.getLevel(),bounds),
                "Starvation fixture entity bounds are not natively loaded: "+bounds)).thenExecute(()->{
            var seed=seed(h,false);var pos=seed.blockPosition();float previous=AuraManager.getFlux(h.getLevel(),pos);
            AuraManager.drainFlux(h.getLevel(),pos,previous,false);
            try {
                String pre=starvationState(h,seed);
                h.assertTrue(seed.isAddedToWorld()&&seed.isAlive()&&!seed.isRemoved()
                        &&TaintedStationaryMob.loaded(h.getLevel(),seed.getBoundingBox())&&seed.getHealth()==75
                        &&seed.invulnerableTime==0&&seed.getActiveEffects().isEmpty()&&seed.getBoost()==0
                        &&AuraManager.getAuraBase(h.getLevel(),pos)>0&&AuraManager.getFlux(h.getLevel(),pos)==0,
                        "Zero-Flux starvation fixture requires a fresh, loaded positive-base aura seed: "+pre);
                pulse(seed);
                String post=starvationState(h,seed);
                com.mojang.logging.LogUtils.getLogger().info("THAUMCRAFT_TAINT_STARVATION_FIXTURE: before={} after={}",pre,post);
                h.assertTrue(seed.tickCount==20&&seed.getHealth()==74.5F&&Math.abs(AuraManager.getFlux(h.getLevel(),pos)-.1F)<.00001F,
                        "Released starvation/0.1Flux payment differs: before="+pre+", after="+post);
            } finally {
                seed.discard();AuraManager.drainFlux(h.getLevel(),pos,AuraManager.getFlux(h.getLevel(),pos),false);AuraManager.addFlux(h.getLevel(),pos,previous);
            }
        }).thenSucceed();
    }
    private static String starvationState(GameTestHelper h,TaintSeedEntity seed) {
        return "tick="+seed.tickCount+", health="+seed.getHealth()+", loaded="+TaintedStationaryMob.loaded(h.getLevel(),seed.getBoundingBox())
                +", added="+seed.isAddedToWorld()+", alive="+seed.isAlive()+", removed="+seed.isRemoved()
                +", base="+AuraManager.getAuraBase(h.getLevel(),seed.blockPosition())+", Flux="+AuraManager.getFlux(h.getLevel(),seed.blockPosition())
                +", hurtFrames="+seed.invulnerableTime+", hurtTime="+seed.hurtTime+", boost="+seed.getBoost()
                +", effects="+seed.getActiveEffects()+", bounds="+seed.getBoundingBox();
    }
    @GameTest(template="empty") public static void positiveFluxTwentyTickPulseDoesNotStarveTheSeed(GameTestHelper h) {
        var seed=seed(h,false);var pos=seed.blockPosition();float previous=AuraManager.getFlux(h.getLevel(),pos);
        AuraManager.addFlux(h.getLevel(),pos,10);
        try { pulse(seed);h.assertTrue(seed.getHealth()==75,"Positive original Flux/base saturation starved seed"); }
        finally { seed.discard();AuraManager.drainFlux(h.getLevel(),pos,AuraManager.getFlux(h.getLevel(),pos),false);AuraManager.addFlux(h.getLevel(),pos,previous); }
        h.succeed();
    }
    @GameTest(template="empty") public static void ordinarySeedInfectsOriginalPointCentredFourBlockCubeIncludingItsCorner(GameTestHelper h) {
        infectionCube(h,false,4,0);
    }
    @GameTest(template="empty") public static void primeSeedInfectsOriginalEightBlockCubeWithAmplifierOne(GameTestHelper h) {
        infectionCube(h,true,8,1);
    }
    private static void infectionCube(GameTestHelper h,boolean prime,int area,int amplifier) {
        var seed=seed(h,prime);seed.setBoost(1);
        Vec3 origin=Vec3.atCenterOf(seed.blockPosition());
        var corner=cow(h,origin.add(area-.1,area-.1,area-.1));
        var outside=cow(h,origin.add(area+2,0,0));
        try {
            pulse(seed);var effect=corner.getEffect(InfusionEffects.FLUX_TAINT.get());
            h.assertTrue(effect!=null&&effect.getDuration()==100&&effect.getAmplifier()==amplifier&&effect.isVisible()
                    &&!outside.hasEffect(InfusionEffects.FLUX_TAINT.get())&&!seed.hasEffect(InfusionEffects.FLUX_TAINT.get()),"Released cube/radius/amplifier/self exclusion differs");
        } finally { seed.discard();corner.discard();outside.discard(); }h.succeed();
    }
    @GameTest(template="empty") public static void actualSeedPulseSpawnsAnOperationalSmallTentacleAtATaintedTarget(GameTestHelper h) {
        var seed=seed(h,false);seed.setBoost(1);var target=cow(h,centre(h).add(6,0,0));
        BlockPos floor=target.blockPosition().below();var original=h.getLevel().getBlockState(floor);
        var box=target.getBoundingBox().inflate(2);
        Set<java.util.UUID> old=h.getLevel().getEntitiesOfClass(TaintacleSmallEntity.class,box).stream().map(Entity::getUUID).collect(java.util.stream.Collectors.toSet());
        try {
            h.getLevel().setBlockAndUpdate(floor,TaintModule.block("taint_rock").defaultBlockState());seed.setTarget(target);pulse(seed);
            var spawned=h.getLevel().getEntitiesOfClass(TaintacleSmallEntity.class,box,e->!old.contains(e.getUUID()));
            h.assertTrue(spawned.size()==1&&!spawned.get(0).isNoAi()&&spawned.get(0).getMaxHealth()==5&&spawned.get(0).remainingLifetime()==200,"Native seed did not spawn exactly one operational released small tentacle");
        } finally { seed.discard();target.discard();h.getLevel().getEntitiesOfClass(TaintacleSmallEntity.class,box,e->!old.contains(e.getUUID())).forEach(Entity::discard);h.getLevel().setBlockAndUpdate(floor,original); }
        h.succeed();
    }
    @GameTest(template="empty") public static void ordinaryTerrainTargetCannotCauseASeedToSpawnSmallTentacles(GameTestHelper h) {
        var seed=seed(h,false);seed.setBoost(1);var target=cow(h,centre(h).add(6,0,0));var pos=target.blockPosition();
        var saved=h.getLevel().getBlockState(pos);var floor=h.getLevel().getBlockState(pos.below());var box=target.getBoundingBox().inflate(2);
        try {
            h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());h.getLevel().setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());seed.setTarget(target);pulse(seed);
            h.assertTrue(h.getLevel().getEntitiesOfClass(TaintacleSmallEntity.class,box).isEmpty(),"Ordinary terrain unexpectedly spawned a taint tentacle");
        } finally { seed.discard();target.discard();h.getLevel().setBlockAndUpdate(pos,saved);h.getLevel().setBlockAndUpdate(pos.below(),floor); }h.succeed();
    }
    @GameTest(template="empty") public static void smallTentacleLivesTwoHundredTicksAndExpiresOnReleasedPostfixBoundary(GameTestHelper h) {
        var tiny=tiny(h);var pos=tiny.blockPosition().below();var saved=h.getLevel().getBlockState(pos);
        var box=tiny.getBoundingBox().inflate(2);var oldItems=h.getLevel().getEntitiesOfClass(ItemEntity.class,box).stream().map(Entity::getUUID).collect(java.util.stream.Collectors.toSet());
        try {
            h.getLevel().setBlockAndUpdate(pos,TaintModule.block("taint_rock").defaultBlockState());
            for(int i=0;i<200;i++)nativeTick(tiny);
            h.assertTrue(tiny.tickCount==200&&tiny.isAlive()&&tiny.getHealth()==5&&tiny.remainingLifetime()==0,"Tentacle expired before released lifetime--<=0 boundary");
            nativeTick(tiny);h.assertTrue(tiny.tickCount==201&&!tiny.isAlive()&&tiny.remainingLifetime()==-1&&h.getLevel().getEntitiesOfClass(ItemEntity.class,box,e->!oldItems.contains(e.getUUID())).isEmpty(),"Tentacle failed to expire or invented death loot");
        } finally { tiny.discard();h.getLevel().setBlockAndUpdate(pos,saved);h.getLevel().getEntitiesOfClass(ItemEntity.class,box,e->!oldItems.contains(e.getUUID())).forEach(Entity::discard); }h.succeed();
    }
    @GameTest(template="empty") public static void smallTentacleOffTaintStarvesOnePointEveryTwentyTicksEvenDuringHurtFrames(GameTestHelper h) {
        var tiny=tiny(h);var pos=tiny.blockPosition();var floor=h.getLevel().getBlockState(pos.below());var old=h.getLevel().getBlockState(pos);
        try {
            h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());h.getLevel().setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());
            tiny.tickCount=19;tiny.invulnerableTime=100;nativeTick(tiny);h.assertTrue(tiny.tickCount==20&&tiny.getHealth()==4,"Original direct starvation was blocked by hurt invulnerability");
            tiny.tickCount=39;nativeTick(tiny);h.assertTrue(tiny.tickCount==40&&tiny.getHealth()==3,"Released20tick starvation cadence differs");
        } finally { tiny.discard();h.getLevel().setBlockAndUpdate(pos,old);h.getLevel().setBlockAndUpdate(pos.below(),floor); }h.succeed();
    }
    @GameTest(template="empty") public static void smallTentacleSaveMigratesGalleryPersistenceAndReloadsOriginalTemporaryLifetime(GameTestHelper h) {
        var tiny=tiny(h);var saved=tiny.saveWithoutId(new CompoundTag());nativeTick(tiny);saved.putBoolean("VisualOnly",true);saved.putBoolean("PersistenceRequired",true);saved.putBoolean("NoAI",true);
        var restored=(TaintacleSmallEntity)create(h,"taintacle_tiny");
        try {
            restored.load(saved);
            h.assertTrue(!restored.isNoAi()&&!restored.isPersistenceRequired()&&restored.removeWhenFarAway(100000)
                    &&restored.remainingLifetime()==200&&!restored.saveWithoutId(new CompoundTag()).contains("VisualOnly"),"Released tiny reload/lifetime/despawn contract differs");
            h.assertTrue(!restored.checkSpawnRules(h.getLevel(),MobSpawnType.NATURAL),"Small temporary tentacle acquired a natural spawn path");
        } finally { tiny.discard();restored.discard(); }h.succeed();
    }
    @GameTest(template="empty") public static void firstNativeTickRegistersSeedAndDeathImmediatelyRemovesItsSource(GameTestHelper h) {
        var seed=seed(h,false);var pos=seed.blockPosition();
        var box=seed.getBoundingBox().inflate(3);Set<java.util.UUID> old=h.getLevel().getEntitiesOfClass(ItemEntity.class,box).stream().map(Entity::getUUID).collect(java.util.stream.Collectors.toSet());
        try {
            h.assertTrue(!positions(h).contains(pos),"Seed source registered before its original first tick");nativeTick(seed);
            h.assertTrue(positions(h).contains(pos)&&TaintHelper.isNearTaintSeed(h.getLevel(),pos),"Actual first-tick seed source registration failed");
            seed.hurt(seed.damageSources().genericKill(),1000);
            h.assertTrue(!seed.isAlive()&&!positions(h).contains(pos)&&!TaintHelper.isNearTaintSeed(h.getLevel(),pos),"Death retained a taint source");
        } finally { seed.discard();h.getLevel().getEntitiesOfClass(ItemEntity.class,box,e->!old.contains(e.getUUID())).forEach(Entity::discard); }h.succeed();
    }
    @GameTest(template="empty") public static void twelveHundredTickRefreshReplacesTheTrackedLocationAfterAdministrativeTeleport(GameTestHelper h) {
        var seed=seed(h,false);nativeTick(seed);var old=seed.blockPosition();seed.setPos(seed.position().add(0,40,0));var current=seed.blockPosition();
        try {
            seed.tickCount=1199;seed.setBoost(1);nativeTick(seed);
            h.assertTrue(seed.tickCount==1200&&!positions(h).contains(old)&&positions(h).contains(current)&&TaintHelper.isNearTaintSeed(h.getLevel(),current),"1200tick refresh retained the old source or omitted the new position");
        } finally { seed.discard(); }h.succeed();
    }
    @GameTest(template="empty") public static void actualUnloadRemovalClearsTheTrackedSourceWithoutWaitingForAnotherProbe(GameTestHelper h) {
        var seed=seed(h,false);nativeTick(seed);var pos=seed.blockPosition();
        try {
            seed.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
            h.assertTrue(seed.isRemoved()&&!positions(h).contains(pos),"Chunk unload removal retained source tracking");
        } finally { seed.discard(); }h.succeed();
    }
    @GameTest(template="empty") public static void spawnObstructionAdmitsSolidTerrainButRejectsLiquidAndActualEntityCollision(GameTestHelper h) {
        var seed=(TaintSeedEntity)create(h,"taint_seed_prime");var pos=seed.blockPosition();var saved=h.getLevel().getBlockState(pos);Cow occupant=null;
        try {
            h.getLevel().setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());
            h.assertTrue(seed.checkSpawnObstruction(h.getLevel())&&seed.canSpawnFromRift(),"Original terrain-collision tolerance was replaced by noCollision");
            h.getLevel().setBlockAndUpdate(pos,Blocks.WATER.defaultBlockState());
            h.assertTrue(!seed.checkSpawnObstruction(h.getLevel())&&!seed.canSpawnFromRift(),"Original liquid gate accepted water");
            h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());occupant=cow(h,seed.position());
            h.assertTrue(!seed.checkSpawnObstruction(h.getLevel())&&!seed.canSpawnFromRift(),"Original entity-collision gate accepted an occupied AABB");
        } finally { seed.discard();if(occupant!=null)occupant.discard();h.getLevel().setBlockAndUpdate(pos,saved); }h.succeed();
    }
    @GameTest(template="empty") public static void spawnSpacingUsesOriginalCubeAndCountsPrimeSeedsBeyondTheDistanceSphere(GameTestHelper h) {
        var candidate=(TaintSeedEntity)create(h,"taint_seed");var occupied=seed(h,true);
        double corner=TaintModule.spreadArea()*.8-.75;
        occupied.setPos(candidate.position().add(0,corner,corner));
        try {
            h.assertTrue(candidate.distanceToSqr(occupied)>Math.pow(TaintModule.spreadArea()*.8,2)&&!candidate.canSpawnFromRift(),"Seed spacing was changed from original cube to distance sphere");
            occupied.discard();h.assertTrue(candidate.canSpawnFromRift(),"Removing original nearest-seed blocker did not allow spawn");
        } finally { candidate.discard();occupied.discard(); }h.succeed();
    }
    @GameTest(template="empty") public static void originalPeacefulGateBlocksSeedSpawning(GameTestHelper h) {
        var seed=(TaintSeedEntity)create(h,"taint_seed");var previous=h.getLevel().getServer().getWorldData().getDifficulty();
        try {
            h.getLevel().getServer().setDifficulty(Difficulty.PEACEFUL,true);
            h.assertTrue(!seed.canSpawnFromRift(),"Original peaceful spawn gate was lost");
        } finally { h.getLevel().getServer().setDifficulty(previous,true);seed.discard(); }h.succeed();
    }
    @GameTest(template="empty") public static void actualNativePeacefulDespawnRemovesAllThreeOperationalCreaturesAndSeedTracking(GameTestHelper h) {
        var previous=h.getLevel().getServer().getWorldData().getDifficulty();
        try {
            for(String id:List.of("taint_seed","taint_seed_prime","taintacle_tiny")) {
                h.getLevel().getServer().setDifficulty(Difficulty.NORMAL,true);
                var mob=create(h,id);h.getLevel().addFreshEntity(mob);mob.setNoAi(true);
                try {
                    nativeTick(mob);var pos=mob.blockPosition();
                    if(mob instanceof TaintSeedEntity)h.assertTrue(positions(h).contains(pos),"Peaceful control lacks a real registered seed");
                    h.getLevel().getServer().setDifficulty(Difficulty.PEACEFUL,true);mob.checkDespawn();
                    h.assertTrue(mob.isRemoved()&&(!(mob instanceof TaintSeedEntity)||!positions(h).contains(pos)),"Native Peaceful removal/source cleanup failed:"+id);
                } finally { mob.discard(); }
            }
        } finally { h.getLevel().getServer().setDifficulty(previous,true); }h.succeed();
    }
    @GameTest(template="empty") public static void verticalBuildBoundaryDoesNotFreezeFallingCreaturesAndNativeVoidDamageCanKillThem(GameTestHelper h) {
        for(String id:List.of("taint_seed","taint_seed_prime","taintacle_tiny")) {
            var mob=create(h,id);mob.setNoAi(true);h.getLevel().addFreshEntity(mob);
            Vec3 initial=centre(h);mob.setPos(initial.x,h.getLevel().getMinBuildHeight()-1,initial.z);
            try {
                double before=mob.getY();mob.move(MoverType.SELF,new Vec3(4,-1,-4));
                h.assertTrue(mob.getY()==before-1&&mob.getX()==initial.x&&mob.getZ()==initial.z,"Vertical chunk guard trapped a stationary mob at the build boundary:"+id);
                mob.setPos(initial.x,h.getLevel().getMinBuildHeight()-66,initial.z);
                for(int i=0;i<801&&mob.isAlive()&&!mob.isRemoved();i++)nativeTick(mob);
                h.assertTrue(!mob.isAlive()||mob.isRemoved(),"Actual native below-world damage never killed the creature:"+id);
            } finally {
                var loot=mob.getBoundingBox().inflate(3);mob.discard();
                h.getLevel().getEntitiesOfClass(ItemEntity.class,loot,e->e.getItem().is(AspectCrystalItem.create(Aspect.FLUX).getItem())).forEach(Entity::discard);
            }
        }h.succeed();
    }
    @GameTest(template="empty") public static void nativeBedSleepRejectsNearbySeedPrimeAndTinyWithoutChangingNormalFailurePriority(GameTestHelper h) {
        BlockPos head=BlockPos.containing(centre(h).add(0,0,5)),foot=head.north();
        var positions=List.of(head,foot,head.above(),foot.above(),head.below(),foot.below());
        var saved=new java.util.HashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        positions.forEach(pos->saved.put(pos,h.getLevel().getBlockState(pos)));
        long time=h.getLevel().getDayTime();
        var player=new FakePlayer(h.getLevel(),new GameProfile(java.util.UUID.randomUUID(),"taint_sleep_qa"));
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);player.setNoGravity(true);player.setPos(Vec3.atBottomCenterOf(head));
        h.getLevel().addNewPlayer(player);
        try {
            h.getLevel().setBlockAndUpdate(head.below(),Blocks.STONE.defaultBlockState());h.getLevel().setBlockAndUpdate(foot.below(),Blocks.STONE.defaultBlockState());
            h.getLevel().setBlock(head,Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING,Direction.SOUTH).setValue(BedBlock.PART,BedPart.HEAD),2);
            h.getLevel().setBlock(foot,Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING,Direction.SOUTH).setValue(BedBlock.PART,BedPart.FOOT),2);
            h.getLevel().setBlockAndUpdate(head.above(),Blocks.AIR.defaultBlockState());h.getLevel().setBlockAndUpdate(foot.above(),Blocks.AIR.defaultBlockState());
            h.getLevel().setDayTime(18000);h.getLevel().updateSkyBrightness();
            for(String id:List.of("taint_seed","taint_seed_prime","taintacle_tiny")) {
                var mob=create(h,id);mob.setNoAi(true);h.getLevel().addFreshEntity(mob);
                try {
                    h.assertTrue(player.startSleepInBed(head).left().orElse(null)==Player.BedSleepingProblem.NOT_SAFE&&!player.isSleeping(),"Native bed safety did not recognize the operational hostile creature:"+id);
                    h.assertTrue(player.getRespawnPosition()!=null&&player.getRespawnPosition().equals(head),"Original unsafe bed failed to set its normal respawn point");
                    h.getLevel().setDayTime(6000);h.getLevel().updateSkyBrightness();
                    h.assertTrue(player.startSleepInBed(head).left().orElse(null)==Player.BedSleepingProblem.NOT_POSSIBLE_NOW,"Unsafe bridge replaced the normal daytime result");
                    h.getLevel().setDayTime(18000);h.getLevel().updateSkyBrightness();player.setPos(Vec3.atBottomCenterOf(head).add(20,0,0));
                    h.assertTrue(player.startSleepInBed(head).left().orElse(null)==Player.BedSleepingProblem.TOO_FAR_AWAY,"Unsafe bridge bypassed the original reach failure");
                    player.setPos(Vec3.atBottomCenterOf(head));h.getLevel().setBlockAndUpdate(head.above(),Blocks.STONE.defaultBlockState());
                    h.assertTrue(player.startSleepInBed(head).left().orElse(null)==Player.BedSleepingProblem.OBSTRUCTED,"Unsafe bridge bypassed the original obstruction failure");
                    h.getLevel().setBlockAndUpdate(head.above(),Blocks.AIR.defaultBlockState());
                    var rejected=new net.minecraftforge.event.entity.player.PlayerSleepInBedEvent(player,java.util.Optional.of(head));
                    rejected.setResult(Player.BedSleepingProblem.OTHER_PROBLEM);TaintRestEvents.sleep(rejected);
                    h.assertTrue(rejected.getResultStatus()==Player.BedSleepingProblem.OTHER_PROBLEM,"Unsafe bridge replaced another mod's existing result");
                } finally { mob.discard(); }
            }
        } finally {
            if(player.isSleeping())player.stopSleepInBed(true,false);player.discard();h.getLevel().players().remove(player);
            saved.forEach((pos,state)->h.getLevel().setBlockAndUpdate(pos,state));h.getLevel().setDayTime(time);h.getLevel().updateSkyBrightness();
        }h.succeed();
    }
    @GameTest(template="empty") public static void nativeOrdinarySeedDropsOneExactFluxCrystalAndPrimeUsesIndependentBonusRolls(GameTestHelper h) {
        var observed=new HashSet<Integer>();
        for(int trial=0;trial<40;trial++) {
            var seed=seed(h,trial>0);var box=seed.getBoundingBox().inflate(3);
            var old=h.getLevel().getEntitiesOfClass(ItemEntity.class,box).stream().map(Entity::getUUID).collect(java.util.stream.Collectors.toSet());
            try {
                seed.getRandom().setSeed(11000L+trial*99173L);seed.hurt(seed.damageSources().genericKill(),1000);
                var dropped=h.getLevel().getEntitiesOfClass(ItemEntity.class,box,e->!old.contains(e.getUUID()));
                int count=dropped.stream().mapToInt(item->item.getItem().getCount()).sum();
                h.assertTrue(!seed.isAlive()&&dropped.stream().allMatch(item->net.minecraft.world.item.ItemStack.isSameItemSameTags(item.getItem(),AspectCrystalItem.create(Aspect.FLUX)))
                        &&(trial==0?count==1:count>=1&&count<=3),"Native seed death loot differs from exact released Flux crystal rolls");
                if(trial>0)observed.add(count);
            } finally { seed.discard();h.getLevel().getEntitiesOfClass(ItemEntity.class,box,e->!old.contains(e.getUUID())).forEach(Entity::discard); }
        }
        h.assertTrue(observed.equals(Set.of(1,2,3)),"Prime's deterministic independent bonus rolls never exercised all released outcomes");h.succeed();
    }
}
