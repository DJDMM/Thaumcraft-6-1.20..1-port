package thaumcraft.essentia.airborne;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.artifice.mirror.EssentiaMirrorBlockEntity;
import thaumcraft.artifice.mirror.MirrorBlock;
import thaumcraft.artifice.mirror.MirrorLink;
import thaumcraft.catalog.blocks.CatalogBlocks;
import thaumcraft.essentia.EssentiaJarBlockEntity;
import thaumcraft.essentia.production.AlembicBlockEntity;
import thaumcraft.essentia.transport.TubeBlockEntity;
import thaumcraft.essentia.transport.TubeBufferBlockEntity;

/** Physical native mirrors and leaves; detached clocks preserve runtime delays. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class MirrorAirborneGameTests {
    private static final String TEMPLATE = "essentia_production";
    private MirrorAirborneGameTests() {}
    private record Saved(BlockState state, CompoundTag tile) {}
    private record Key(ServerLevel level, BlockPos pos) {}

    /** The elevated arena is restored, including native tile data, after each check. */
    private static final class Arena implements AutoCloseable {
        final GameTestHelper helper;
        final ServerLevel level;
        final BlockPos origin;
        final Map<Key, Saved> saved = new LinkedHashMap<>();
        Arena(GameTestHelper helper) {
            this.helper = helper; level = helper.getLevel();
            origin = helper.absolutePos(new BlockPos(6, 4, 6)).above(70);
        }
        BlockEntity put(ServerLevel world, BlockPos pos, BlockState state) {
            Key key = new Key(world, pos.immutable());
            saved.computeIfAbsent(key, ignored -> new Saved(world.getBlockState(pos),
                    world.getBlockEntity(pos) == null ? null : world.getBlockEntity(pos).saveWithoutMetadata()));
            world.setBlockAndUpdate(pos, state);
            return world.getBlockEntity(pos);
        }
        AlembicBlockEntity consumer() {
            return (AlembicBlockEntity)put(level, origin, CatalogBlocks.block("alembic").defaultBlockState());
        }
        EssentiaMirrorBlockEntity mirror(ServerLevel world, BlockPos pos, Direction face) {
            put(world, pos.relative(face.getOpposite()), Blocks.STONE.defaultBlockState());
            return (EssentiaMirrorBlockEntity)put(world, pos,
                    CatalogBlocks.block("mirror_essentia").defaultBlockState().setValue(MirrorBlock.FACING, face));
        }
        EssentiaMirrorBlockEntity[] pair() {
            var local = mirror(level, origin.east(2), Direction.NORTH);
            var remote = mirror(level, origin.above(24), Direction.NORTH);
            bind(local, remote);
            return new EssentiaMirrorBlockEntity[]{local, remote};
        }
        void bind(EssentiaMirrorBlockEntity local, EssentiaMirrorBlockEntity remote) {
            local.setLink(remote.endpoint());
            helper.assertTrue(local.restoreLink() && local.isLinkValidSimple() && remote.isLinkValidSimple(), "Pair fixture was not reciprocal");
        }
        EssentiaJarBlockEntity jar(ServerLevel world, BlockPos pos, Aspect aspect, int amount) {
            return jar(world, pos, aspect, amount, false);
        }
        EssentiaJarBlockEntity jar(ServerLevel world, BlockPos pos, Aspect aspect, int amount, boolean isVoid) {
            var jar = (EssentiaJarBlockEntity)put(world, pos, CatalogBlocks.block(isVoid ? "jar_void" : "jar_normal").defaultBlockState());
            if (amount > 0) helper.assertTrue(jar.addExact(aspect, amount), "Could not fill physical leaf jar");
            return jar;
        }
        @Override public void close() {
            for (var entry : saved.entrySet()) {
                var key = entry.getKey();
                if (key.level.getBlockEntity(key.pos) instanceof EssentiaMirrorBlockEntity mirror) mirror.setCreativeRemoval(true);
            }
            var entries = new java.util.ArrayList<>(saved.entrySet());
            java.util.Collections.reverse(entries);
            for (var entry : entries) {
                var key = entry.getKey(); var value = entry.getValue();
                key.level.setBlockAndUpdate(key.pos, value.state);
                if (value.tile != null && key.level.getBlockEntity(key.pos) != null) key.level.getBlockEntity(key.pos).load(value.tile);
            }
        }
    }
    private static AirborneEssentiaManager.SourceCache cache() { return cache(new AtomicLong()); }
    private static AirborneEssentiaManager.SourceCache cache(AtomicLong clock) { return new AirborneEssentiaManager.SourceCache(clock::get); }

    @GameTest(template = TEMPLATE)
    public static void mirrorIsSourceWithoutInventedTankTubeOrBulkOperations(GameTestHelper helper) {
        try (var arena = new Arena(helper)) {
            var mirrors = arena.pair(); var mirror = mirrors[0];
            var jar = arena.jar(arena.level, mirrors[1].getBlockPos().north(2), Aspect.AIR, 4);
            var before = jar.saveWithoutMetadata();
            mirror.setAspects(new AspectList().add(Aspect.AIR, 99));
            helper.assertTrue(!((BlockEntity)mirror instanceof IEssentiaTransport) && mirror.getAspects() == null
                    && mirror.containerContains(Aspect.AIR) == 0 && !mirror.isBlocked()
                    && !mirror.takeFromContainer(new AspectList().add(Aspect.AIR, 1))
                    && !mirror.doesContainerContain(new AspectList().add(Aspect.AIR, 1))
                    && !mirror.takeFromContainer(Aspect.AIR, 2) && !mirror.takeFromContainer(Aspect.AIR, 0)
                    && !mirror.takeFromContainer(Aspect.AIR, -1) && !mirror.takeFromContainer(null, 1)
                    && mirror.addToContainer(Aspect.AIR, 2) == 2 && mirror.addToContainer(Aspect.AIR, -1) == -1
                    && before.equals(jar.saveWithoutMetadata()) && mirror.instability() == 0,
                    "Mirror acquired storage, tube ports, bulk transfers or malformed-amount mutation");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void mirroredNativeDrainOwnsOneRemoteUnitAndTwoLocalFxLegs(GameTestHelper helper) {
        try (var arena = new Arena(helper)) {
            var consumer = arena.consumer(); var mirrors = arena.pair();
            var jar = arena.jar(arena.level, mirrors[1].getBlockPos().north(2), Aspect.AIR, 4);
            var cache = cache();
            var transfer = cache.drain(consumer, Aspect.AIR, null, 3, 17).orElseThrow();
            helper.assertTrue(jar.amount() == 3 && mirrors[0].instability() == 1 && mirrors[1].instability() == 0
                    && transfer.source().equals(mirrors[0].getBlockPos()) && transfer.target().equals(consumer.getBlockPos().below())
                    && transfer.legs().size() == 2 && transfer.legs().get(0).source().equals(jar.getBlockPos())
                    && transfer.legs().get(0).target().equals(mirrors[1].getBlockPos()) && transfer.legs().get(0).ext() == 5
                    && transfer.legs().get(1).ext() == 17 && transfer.legs().stream().allMatch(leg -> leg.dimension() == arena.level.dimension()),
                    "Mirrored drain manufactured a buffer, charged both mirrors or lost either dimension-qualified FX leg");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void nativeMatrixConsumerPaysOnlyActualRemoteLeaf(GameTestHelper helper) {
        try (var arena = new Arena(helper)) {
            var matrix = arena.put(arena.level, arena.origin, CatalogBlocks.block("infusion_matrix").defaultBlockState());
            var mirrors = arena.pair(); var jar = arena.jar(arena.level, mirrors[1].getBlockPos().north(2), Aspect.MAGIC, 1);
            var cache = cache();
            helper.assertTrue(cache.find(matrix, Aspect.MAGIC, null, 3) && jar.amount() == 1, "Matrix read-only discovery debited leaf");
            var result = cache.drain(matrix, Aspect.MAGIC, null, 3, 8).orElseThrow();
            helper.assertTrue(jar.amount() == 0 && result.target().equals(matrix.getBlockPos().below())
                    && cache.drain(matrix, Aspect.MAGIC, null, 3, 8).isEmpty() && mirrors[0].instability() == 1,
                    "Native matrix paid absent essence, repeated one unit or used incorrect central-altar target");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void mirrorRemoteFacingUsesAllSixOriginalForwardVolumes(GameTestHelper helper) {
        try (var arena = new Arena(helper)) {
            var consumer = arena.consumer(); var mirrors = arena.pair(); var remote = mirrors[1];
            for (Direction side : Direction.values()) {
                arena.put(arena.level, remote.getBlockPos().relative(side.getOpposite()), Blocks.STONE.defaultBlockState());
                arena.level.setBlockAndUpdate(remote.getBlockPos(), remote.getBlockState().setValue(MirrorBlock.FACING, side));
                var source = arena.jar(arena.level, remote.getBlockPos().relative(side, 2), Aspect.AIR, 1);
                var result = cache().drain(consumer, Aspect.AIR, null, 3, 0);
                helper.assertTrue(result.isPresent() && source.amount() == 0 && result.orElseThrow().legs().get(0).source().equals(source.getBlockPos()),
                        "Remote mirror failed its physical " + side + " directional volume");
                arena.put(arena.level, source.getBlockPos(), Blocks.AIR.defaultBlockState());
            }
            helper.assertTrue(mirrors[0].instability() == 6, "Six transfers did not add six local instability units");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void remoteBracePreventsDebitButMismatchedLabelDoesNot(GameTestHelper helper) {
        try (var arena = new Arena(helper)) {
            var consumer = arena.consumer(); var mirrors = arena.pair(); var jar = arena.jar(arena.level, mirrors[1].getBlockPos().north(2), Aspect.AIR, 2);
            jar.installBrace(); var cache = cache();
            helper.assertTrue(!cache.find(consumer, Aspect.AIR, null, 3) && jar.amount() == 2, "Remote brace allowed read-only presence");
            var tag = jar.saveWithoutMetadata(); tag.putBoolean("blocked", false); tag.putString("AspectFilter", Aspect.FIRE.getTag()); jar.load(tag);
            helper.assertTrue(cache.drain(consumer, Aspect.AIR, null, 3, 0).isPresent() && jar.amount() == 1 && jar.filter() == Aspect.FIRE,
                    "Remote drain invented an insertion-label restriction or changed the label");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void remoteAddPreservesNonemptyPriorityAndChargesOnlyInvokedMirror(GameTestHelper helper) {
        try (var arena = new Arena(helper)) {
            var consumer = arena.consumer(); var mirrors = arena.pair();
            var near = arena.jar(arena.level, mirrors[1].getBlockPos().north(), Aspect.AIR, 0);
            var far = arena.jar(arena.level, mirrors[1].getBlockPos().north(3), Aspect.AIR, 2);
            var result = cache().add(consumer, Aspect.AIR, null, 3, 4).orElseThrow();
            helper.assertTrue(near.amount() == 0 && far.amount() == 3 && mirrors[0].instability() == 1 && mirrors[1].instability() == 0
                    && result.legs().size() == 2 && result.legs().get(0).source().equals(mirrors[1].getBlockPos())
                    && result.legs().get(0).target().equals(far.getBlockPos()) && result.target().equals(mirrors[0].getBlockPos()),
                    "Remote add chose nearest empty jar or duplicated/misqualified its paid leg");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void coldMirrorAcceptanceChecksPresenceBeforeCachedCapacityPredicate(GameTestHelper helper) {
        try (var arena = new Arena(helper)) {
            var mirrors = arena.pair(); var jar = arena.jar(arena.level, mirrors[1].getBlockPos().north(2), Aspect.AIR, 0); var cache = cache();
            helper.assertTrue(!cache.mirrorQuery(mirrors[0], Aspect.LIFE, true) && cache.mirrorQuery(mirrors[0], Aspect.LIFE, true)
                    && jar.amount() == 0 && mirrors[0].instability() == 0,
                    "First canAccept lost the released findEssentia quirk or read-only acceptance mutated essence");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void coldOuterAddRetainsReleasedSecondAcceptanceCallFirstPassQuirk(GameTestHelper helper) {
        try (var arena = new Arena(helper)) {
            var consumer = arena.consumer(); var mirrors = arena.pair();
            var remoteEmpty = arena.jar(arena.level, mirrors[1].getBlockPos().north(2), Aspect.LIFE, 0);
            var localNonempty = arena.jar(arena.level, consumer.getBlockPos().east(3), Aspect.LIFE, 2);
            var cache = cache(); var result = cache.add(consumer, Aspect.LIFE, null, 3, 0).orElseThrow();
            helper.assertTrue(remoteEmpty.amount() == 1 && localNonempty.amount() == 2 && result.target().equals(mirrors[0].getBlockPos())
                    && mirrors[0].instability() == 1, "Released cold false184->secondtrue229 add branch was normalized into invented one-query ordering");
            var warm = cache.add(consumer, Aspect.LIFE, null, 3, 0).orElseThrow();
            helper.assertTrue(remoteEmpty.amount() == 1 && localNonempty.amount() == 3 && warm.target().equals(localNonempty.getBlockPos())
                    && mirrors[0].instability() == 1, "Warm null-aspects mirror did not defer behind actual compatible nonempty local jar");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void failedRemoteDrainSharesTenSecondDelayWithRemoteAddAndFind(GameTestHelper helper) {
        try (var arena = new Arena(helper)) {
            var consumer = arena.consumer(); var mirrors = arena.pair(); var jar = arena.jar(arena.level, mirrors[1].getBlockPos().north(2), Aspect.AIR, 2);
            var clock = new AtomicLong(); var cache = cache(clock);
            helper.assertTrue(cache.drain(consumer, Aspect.LIFE, null, 3, 0).isEmpty()
                    && !cache.mirrorQuery(mirrors[0], Aspect.AIR, false) && !cache.mirrorMutation(mirrors[0], Aspect.AIR, true)
                    && jar.amount() == 2, "Remote failure did not delay all operations at remote consumer coordinates");
            cache.refreshSources(mirrors[1]); clock.set(9_999);
            helper.assertTrue(!cache.mirrorMutation(mirrors[0], Aspect.AIR, false), "Remote refresh erased shared wall-clock delay");
            clock.set(10_000);
            helper.assertTrue(cache.mirrorMutation(mirrors[0], Aspect.AIR, false) && jar.amount() == 1 && mirrors[0].instability() == 1,
                    "Remote transfer did not resume exactly at original deadline");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void remoteLegSkipsAllAdditionalMirrorsWithoutSecondHop(GameTestHelper helper) {
        try (var arena = new Arena(helper)) {
            var consumer = arena.consumer(); var mirrors = arena.pair();
            var third = arena.mirror(arena.level, mirrors[1].getBlockPos().north(2), Direction.NORTH);
            var fourth = arena.mirror(arena.level, arena.origin.above(48), Direction.NORTH); arena.bind(third, fourth);
            var jar = arena.jar(arena.level, fourth.getBlockPos().north(2), Aspect.LIFE, 2);
            helper.assertTrue(cache().drain(consumer, Aspect.LIFE, null, 3, 0).isEmpty() && jar.amount() == 2
                    && mirrors[0].instability() == 0 && third.instability() == 0,
                    "Remote ignoreMirror followed another pair or paid absent local essence");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void mirrorConfirmationCannotDebitAfterEitherReciprocalRelink(GameTestHelper helper) {
        try (var arena = new Arena(helper)) {
            var consumer = arena.consumer(); var mirrors = arena.pair(); var jar = arena.jar(arena.level, mirrors[1].getBlockPos().north(2), Aspect.AIR, 3); var cache = cache();
            var old = cache.prepareDrain(consumer, Aspect.AIR, null, 3, 7).orElseThrow();
            mirrors[1].setLink(new MirrorLink(arena.level.dimension(), arena.origin.above(55)));
            helper.assertTrue(cache.confirmDrain(consumer, old).isEmpty() && jar.amount() == 3 && mirrors[0].instability() == 0,
                    "Reciprocal peer relink did not invalidate physical preview");
            arena.bind(mirrors[0], mirrors[1]);
            var fresh = cache.prepareDrain(consumer, Aspect.AIR, null, 3, 7).orElseThrow();
            helper.assertTrue(cache.confirmDrain(consumer, fresh).isPresent() && cache.confirmDrain(consumer, fresh).isEmpty()
                    && jar.amount() == 2 && mirrors[0].instability() == 1, "Fresh confirmation failed or replayed remote unit");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void mirrorConfirmationRejectsPhysicalFacingChangeEvenWithoutRevisionChange(GameTestHelper helper) {
        try (var arena = new Arena(helper)) {
            var consumer = arena.consumer(); var mirrors = arena.pair(); var jar = arena.jar(arena.level, mirrors[1].getBlockPos().north(2), Aspect.AIR, 2); var cache = cache();
            var preview = cache.prepareDrain(consumer, Aspect.AIR, null, 3, 0).orElseThrow();
            arena.put(arena.level, mirrors[1].getBlockPos().west(), Blocks.STONE.defaultBlockState());
            arena.level.setBlockAndUpdate(mirrors[1].getBlockPos(), mirrors[1].getBlockState().setValue(MirrorBlock.FACING, Direction.EAST));
            helper.assertTrue(cache.confirmDrain(consumer, preview).isEmpty() && jar.amount() == 2 && mirrors[0].instability() == 0,
                    "Changed peer facing retained a paid route to old search region");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void remoteJarReplacementBraceAndLifecycleInvalidateDetachedPromises(GameTestHelper helper) {
        try (var arena = new Arena(helper)) {
            var consumer = arena.consumer(); var mirrors = arena.pair(); var pos = mirrors[1].getBlockPos().north(2);
            var jar = arena.jar(arena.level, pos, Aspect.AIR, 2); var cache = cache();
            var braced = cache.prepareDrain(consumer, Aspect.AIR, null, 3, 0).orElseThrow(); jar.installBrace();
            helper.assertTrue(cache.confirmDrain(consumer, braced).isEmpty() && jar.amount() == 2, "Braced remote leaf paid old preview");
            var tag = jar.saveWithoutMetadata(); tag.putBoolean("blocked", false); jar.load(tag);
            var replaced = cache.prepareDrain(consumer, Aspect.AIR, null, 3, 0).orElseThrow();
            arena.put(arena.level, pos, Blocks.AIR.defaultBlockState()); var replacement = arena.jar(arena.level, pos, Aspect.AIR, 7);
            helper.assertTrue(cache.confirmDrain(consumer, replaced).isEmpty() && replacement.amount() == 7, "Remote replacement inherited prior physical reservation");
            var forgotten = cache.prepareDrain(consumer, Aspect.AIR, null, 3, 0).orElseThrow(); cache.forgetLevel(arena.level);
            helper.assertTrue(cache.confirmDrain(consumer, forgotten).isEmpty() && replacement.amount() == 7, "World lifecycle retained a transient remote promise");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void unavailableRemoteDimensionRetainsRecordedLinkAndNeverPays(GameTestHelper helper) {
        try (var arena = new Arena(helper)) {
            var consumer = arena.consumer(); var mirrors = arena.pair();
            var missing = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                    new net.minecraft.resources.ResourceLocation("thaumcraft", "unavailable_mirror_test"));
            mirrors[0].setLink(new MirrorLink(missing, mirrors[1].getBlockPos()));
            var before = mirrors[0].link();
            helper.assertTrue(cache().drain(consumer, Aspect.LIFE, null, 3, 0).isEmpty() && before.equals(mirrors[0].link())
                    && mirrors[0].instability() == 0, "Unavailable world lost recorded link or invented payment");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void ungeneratedRemoteChunkIsNotPromotedForEssentia(GameTestHelper helper) {
        try (var arena = new Arena(helper)) {
            var mirror = arena.pair()[0]; var remotePos = new BlockPos(29_000_000, 80, 29_000_000);
            mirror.setLink(new MirrorLink(arena.level.dimension(), remotePos)); var before = mirror.link();
            helper.assertTrue(arena.level.getChunkSource().getChunkNow(remotePos.getX() >> 4, remotePos.getZ() >> 4) == null
                    && !cache().mirrorMutation(mirror, Aspect.LIFE, false)
                    && arena.level.getChunkSource().getChunkNow(remotePos.getX() >> 4, remotePos.getZ() >> 4) == null
                    && before.equals(mirror.link()), "Mirror access generated or loaded a remote chunk");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void physicalInputTransfuserConservesSourceAndRemoteJar(GameTestHelper helper) {
        try (var arena = new Arena(helper)) {
            var consumer = arena.consumer(); var mirrors = arena.pair(); var source = (AlembicBlockEntity)arena.put(arena.level,
                    arena.origin.west(), CatalogBlocks.block("alembic").defaultBlockState()); source.addExact(Aspect.AIR, 2);
            var jar = arena.jar(arena.level, mirrors[1].getBlockPos().north(2), Aspect.AIR, 1); var cache = cache();
            for (int n = 0; n < 2; n++) {
                var transfer = cache.transferToSources(consumer, source, Aspect.AIR, Direction.EAST, Direction.EAST, 3, 5).orElseThrow();
                helper.assertTrue(source.amount() == 1 - n && jar.amount() == 2 + n && transfer.legs().size() == 2,
                        "Input transfuser did not physically debit each remote insertion");
            }
            helper.assertTrue(cache.transferToSources(consumer, source, Aspect.AIR, Direction.EAST, Direction.EAST, 3, 5).isEmpty()
                    && jar.amount() == 3 && mirrors[0].instability() == 2, "Empty input manufactured remote essence");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void physicalOutputRefusalRollsBackRemoteJarAndInstabilityThenRetries(GameTestHelper helper) {
        try (var arena = new Arena(helper)) {
            var consumer = arena.consumer(); var mirrors = arena.pair(); var source = arena.jar(arena.level, mirrors[1].getBlockPos().north(2), Aspect.AIR, 3);
            var target = arena.jar(arena.level, consumer.getBlockPos().below(), Aspect.AIR, EssentiaJarBlockEntity.CAPACITY); var cache = cache();
            CompoundTag before = source.saveWithoutMetadata(), targetBefore = target.saveWithoutMetadata();
            helper.assertTrue(cache.transferFromSources(consumer, target, Aspect.AIR, Direction.UP, Direction.UP, 3, 5).isEmpty()
                    && before.equals(source.saveWithoutMetadata()) && targetBefore.equals(target.saveWithoutMetadata())
                    && mirrors[0].instability() == 0, "Rejected output consumed remote payment or charged instability");
            target.takeFromContainer(Aspect.AIR, 1);
            helper.assertTrue(cache.transferFromSources(consumer, target, Aspect.AIR, Direction.UP, Direction.UP, 3, 5).isPresent()
                    && source.amount() == 2 && target.amount() == EssentiaJarBlockEntity.CAPACITY && mirrors[0].instability() == 1,
                    "Rollback invented cooldown or lost the immediate accepted remote transfer");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void remoteVoidOverflowConsumesOnePhysicalInputWithoutStoredDuplicate(GameTestHelper helper) {
        try (var arena = new Arena(helper)) {
            var consumer = arena.consumer(); var mirrors = arena.pair(); var source = (AlembicBlockEntity)arena.put(arena.level,
                    arena.origin.west(), CatalogBlocks.block("alembic").defaultBlockState()); source.addExact(Aspect.AIR, 1);
            var target = arena.jar(arena.level, mirrors[1].getBlockPos().north(2), Aspect.AIR, EssentiaJarBlockEntity.CAPACITY, true);
            helper.assertTrue(cache().transferToSources(consumer, source, Aspect.AIR, Direction.EAST, Direction.EAST, 3, 5).isPresent()
                    && source.amount() == 0 && target.amount() == EssentiaJarBlockEntity.CAPACITY && mirrors[0].instability() == 1,
                    "Remote void overflow created stored essence or omitted actual input debit");
        }
        helper.succeed();
    }

    /** A native buffer consults neighbouring suction before its own physical debit. */
    private static final class CallbackTube extends TubeBlockEntity {
        Runnable callback;
        CallbackTube(BlockPos pos, BlockState state) { super(pos, state); }
        @Override public int getSuctionAmount(Direction face) {
            if (callback != null) { Runnable action = callback; callback = null; action.run(); }
            return 0;
        }
    }

    @GameTest(template = TEMPLATE)
    public static void relinkInsideNativeBufferDebitRefundsSourceAndDoesNotCreateRemoteUnit(GameTestHelper helper) {
        try (var arena = new Arena(helper)) {
            var consumer = arena.consumer(); var mirrors = arena.pair();
            var source = (TubeBufferBlockEntity)arena.put(arena.level, arena.origin.west(), CatalogBlocks.block("tube_buffer").defaultBlockState());
            source.addToContainer(Aspect.AIR, 1);
            var destination = arena.jar(arena.level, mirrors[1].getBlockPos().north(2), Aspect.AIR, 1);
            BlockPos callbackPos = source.getBlockPos().below();
            arena.put(arena.level, callbackPos, CatalogBlocks.block("tube").defaultBlockState());
            var callback = new CallbackTube(callbackPos, arena.level.getBlockState(callbackPos));
            arena.level.setBlockEntity(callback);
            callback.callback = () -> mirrors[0].setLink(mirrors[1].endpoint());
            CompoundTag before = source.saveWithoutMetadata(), destBefore = destination.saveWithoutMetadata();
            helper.assertTrue(cache().transferToSources(consumer, source, Aspect.AIR, Direction.EAST, Direction.EAST, 3, 5).isEmpty()
                    && before.equals(source.saveWithoutMetadata()) && destBefore.equals(destination.saveWithoutMetadata())
                    && mirrors[0].instability() == 0, "Suction callback relink lost a physical input unit or paid stale remote route");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void mirrorReentryFromNativeSuctionCallbackCannotDebitSecondUnit(GameTestHelper helper) {
        try (var arena = new Arena(helper)) {
            var consumer = arena.consumer(); var mirrors = arena.pair();
            var source = (TubeBufferBlockEntity)arena.put(arena.level, arena.origin.west(), CatalogBlocks.block("tube_buffer").defaultBlockState());
            source.addToContainer(Aspect.AIR, 1);
            var destination = arena.jar(arena.level, mirrors[1].getBlockPos().north(2), Aspect.AIR, 2);
            BlockPos callbackPos = source.getBlockPos().below();
            arena.put(arena.level, callbackPos, CatalogBlocks.block("tube").defaultBlockState());
            var callback = new CallbackTube(callbackPos, arena.level.getBlockState(callbackPos)); arena.level.setBlockEntity(callback);
            var cache = cache(); var successes = new int[1];
            callback.callback = () -> {
                if (cache.mirrorMutation(mirrors[0], Aspect.AIR, false)) successes[0]++;
                if (cache.mirrorMutation(mirrors[0], Aspect.AIR, true)) successes[0]++;
                if (cache.prepareDrain(consumer, Aspect.AIR, null, 3, 0).isPresent()) successes[0]++;
                if (cache.mirrorQuery(mirrors[0], Aspect.AIR, false)) successes[0]++;
            };
            helper.assertTrue(cache.transferToSources(consumer, source, Aspect.AIR, Direction.EAST, Direction.EAST, 3, 5).isPresent()
                    && successes[0] == 0 && source.containerContains(Aspect.AIR) == 0 && destination.amount() == 3
                    && mirrors[0].instability() == 1, "Guarded suction callback reentered mirror payment or prevented the single real transfer");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void offThreadMirrorCallsCannotDiscoverMutateOrPoisonValidServerTransfer(GameTestHelper helper) {
        try (var arena = new Arena(helper)) {
            var consumer = arena.consumer(); var mirrors = arena.pair(); var source = arena.jar(arena.level, mirrors[1].getBlockPos().north(2), Aspect.AIR, 2); var cache = cache();
            helper.assertTrue(CompletableFuture.supplyAsync(() -> !cache.mirrorQuery(mirrors[0], Aspect.AIR, false)
                    && !cache.mirrorMutation(mirrors[0], Aspect.AIR, false)
                    && cache.drain(consumer, Aspect.AIR, null, 3, 0).isEmpty()).join()
                    && source.amount() == 2 && cache.drain(consumer, Aspect.AIR, null, 3, 0).isPresent() && source.amount() == 1,
                    "Off-thread mirror call paid essence or poisoned subsequent server-thread cache");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 160)
    public static void sameCoordinatesAcrossDimensionsKeepLeafWorldAndBothFxLegs(GameTestHelper helper) {
        var arena = new Arena(helper); var consumer = arena.consumer();
        var remoteLevel = arena.level.getServer().getLevel(Level.NETHER);
        helper.assertTrue(remoteLevel != null, "Native nether world missing");
        // Loading here creates the explicit test fixture; the transport never loads a chunk.
        remoteLevel.getChunkAt(arena.origin.east(2));
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(remoteLevel.getChunkSource().getChunkNow(
                arena.origin.east(2).getX() >> 4, arena.origin.east(2).getZ() >> 4) != null, "Remote fixture FULL chunk not yet available"))
                .thenExecute(() -> {
                    try {
                        var local = arena.mirror(arena.level, arena.origin.east(2), Direction.NORTH);
                        var remote = arena.mirror(remoteLevel, arena.origin.east(2), Direction.NORTH); arena.bind(local, remote);
                        var leaf = arena.jar(remoteLevel, remote.getBlockPos().north(2), Aspect.AIR, 2);
                        var unrelated = arena.jar(arena.level, remote.getBlockPos().north(2), Aspect.FIRE, 7);
                        var result = cache().drain(consumer, Aspect.AIR, null, 3, 11).orElseThrow();
                        helper.assertTrue(leaf.amount() == 1 && unrelated.amount() == 7 && local.instability() == 1 && remote.instability() == 0
                                && result.legs().get(0).dimension() == Level.NETHER && result.legs().get(1).dimension() == arena.level.dimension(),
                                "Identical coordinates confused worlds, paid local jar or sent remote FX in local dimension");
                    } finally { arena.close(); }
                }).thenSucceed();
    }
}
