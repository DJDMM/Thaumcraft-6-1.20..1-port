package thaumcraft.artifice.redstone;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.TickPriority;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.NoteBlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import thaumcraft.catalog.blocks.CatalogBlocks;

/** Actual native placement, note events, redstone neighbors, saved tuning and normal scheduler. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class RedstoneDeviceGameTests {
    private static final String TEMPLATE = "essentia_network";
    private static final BlockPos CENTER = new BlockPos(4, 2, 4);
    private RedstoneDeviceGameTests() {}
    private static RedstoneRelayBlock relayBlock() { return (RedstoneRelayBlock) CatalogBlocks.block("redstone_relay"); }
    private static ArcaneEarBlock earBlock(boolean toggle) { return (ArcaneEarBlock) CatalogBlocks.block(toggle ? "arcane_ear_toggle" : "arcane_ear"); }
    private static FakePlayer player(GameTestHelper h, BlockPos pos) {
        var player = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "redstone_qa"));
        player.setGameMode(GameType.SURVIVAL); player.setPos(Vec3.atCenterOf(pos).add(0, 0, 3)); return player;
    }
    private static void clear(GameTestHelper h, BlockPos pos) {
        for (Direction direction : Direction.values()) h.getLevel().setBlock(pos.relative(direction), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        h.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
    }
    private static RedstoneRelayBlockEntity relay(GameTestHelper h, BlockPos pos, Direction input, boolean powered) {
        clear(h, pos); h.getLevel().setBlock(pos.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        h.getLevel().setBlock(pos, relayBlock().defaultBlockState().setValue(RedstoneRelayBlock.FACING, input).setValue(RedstoneRelayBlock.ENABLED, powered), Block.UPDATE_ALL);
        return (RedstoneRelayBlockEntity) h.getLevel().getBlockEntity(pos);
    }
    private static ArcaneEarBlockEntity ear(GameTestHelper h, BlockPos pos, Direction face, boolean toggle, Block support, int note) {
        clear(h, pos); h.getLevel().setBlock(pos.relative(face.getOpposite()), support.defaultBlockState(), Block.UPDATE_ALL);
        h.getLevel().setBlock(pos, earBlock(toggle).defaultBlockState().setValue(ArcaneEarBlock.FACING, face), Block.UPDATE_ALL);
        var ear = (ArcaneEarBlockEntity) h.getLevel().getBlockEntity(pos); ear.note = (byte) note;
        return ear;
    }
    private static boolean enabled(GameTestHelper h, BlockPos pos) {
        BlockState state = h.getLevel().getBlockState(pos);
        return state.getBlock() instanceof RedstoneRelayBlock ? state.getValue(RedstoneRelayBlock.ENABLED) : state.getValue(ArcaneEarBlock.ENABLED);
    }
    private static void clickKnob(GameTestHelper h, BlockPos pos, int knob, FakePlayer player) {
        var state = h.getLevel().getBlockState(pos);
        var point = RedstoneRelayBlock.knobBounds(state.getValue(RedstoneRelayBlock.FACING), knob).getCenter().add(Vec3.atLowerCornerOf(pos));
        var hit = new BlockHitResult(point, Direction.UP, pos, false);
        state.getBlock().use(state, h.getLevel(), pos, player, InteractionHand.MAIN_HAND, hit);
    }
    private static void nativeNote(GameTestHelper h, BlockPos pos, Block support, int note) {
        h.getLevel().setBlock(pos.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        h.getLevel().setBlock(pos.below(), support.defaultBlockState(), Block.UPDATE_ALL);
        h.getLevel().setBlock(pos, Blocks.NOTE_BLOCK.defaultBlockState().setValue(NoteBlock.INSTRUMENT, support.defaultBlockState().instrument()).setValue(NoteBlock.NOTE, note), Block.UPDATE_ALL);
        Blocks.NOTE_BLOCK.attack(h.getLevel().getBlockState(pos), h.getLevel(), pos, player(h, pos));
    }
    private static void immediateNativeNote(GameTestHelper h, BlockPos pos, NoteBlockInstrument instrument, int note) {
        BlockState state = Blocks.NOTE_BLOCK.defaultBlockState().setValue(NoteBlock.INSTRUMENT, instrument).setValue(NoteBlock.NOTE, note);
        Blocks.NOTE_BLOCK.triggerEvent(state, h.getLevel(), pos, 0, 0);
    }
    private static void tickEar(GameTestHelper h, ArcaneEarBlockEntity ear) {
        ArcaneEarBlockEntity.serverTick(h.getLevel(), ear.getBlockPos(), ear.getBlockState(), ear);
    }
    private static List<net.minecraft.world.ticks.ScheduledTick<Block>> scheduled(GameTestHelper h, BlockPos pos) {
        return ((LevelChunkTicks<Block>) h.getLevel().getChunkAt(pos).getBlockTicks()).getAll().filter(tick -> tick.type() == relayBlock() && tick.pos().equals(pos)).toList();
    }

    @GameTest(template = TEMPLATE)
    public static void normalAndSneakingBlockItemPlacementPaysAndOrientsInput(GameTestHelper h) {
        BlockPos pos = h.absolutePos(CENTER); clear(h, pos); h.getLevel().setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
        var player = player(h, pos); player.setYRot(0);
        for (boolean sneak : new boolean[]{false, true}) {
            h.getLevel().removeBlock(pos, false); player.setShiftKeyDown(sneak);
            var stack = new ItemStack(relayBlock(), 2); player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            var hit = new BlockHitResult(Vec3.atCenterOf(pos.below()).add(0, .5, 0), Direction.UP, pos.below(), false);
            h.assertTrue(stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction() && stack.getCount() == 1,
                    "Native relay BlockItem placement did not pay one item");
            var state = h.getLevel().getBlockState(pos); var tile = (RedstoneRelayBlockEntity) h.getLevel().getBlockEntity(pos);
            h.assertTrue(state.getValue(RedstoneRelayBlock.FACING) == (sneak ? Direction.SOUTH : Direction.NORTH)
                    && !state.getValue(RedstoneRelayBlock.ENABLED) && tile.getIn() == 1 && tile.getOut() == 15,
                    "Placement changed original input direction, fresh settings or unpowered state");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void poweredBlockItemPlacementSchedulesOriginalFirstTickAtOne(GameTestHelper h) {
        BlockPos pos = h.absolutePos(CENTER); clear(h, pos); h.getLevel().setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
        h.getLevel().setBlock(pos.north(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        var player = player(h, pos); player.setYRot(0); var stack = new ItemStack(relayBlock()); player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos.below()).add(0, .5, 0), Direction.UP, pos.below(), false)));
        h.assertTrue(scheduled(h, pos).size() == 1 && scheduled(h, pos).get(0).triggerTick() - h.getLevel().getGameTime() == 1,
                "Initially powered real placement missed the original one-tick first update"); h.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void rotatedPhysicalDialsIndependentlyWrapOneThroughFifteen(GameTestHelper h) {
        var pos = h.absolutePos(CENTER); var player = player(h, pos);
        for (Direction input : Direction.Plane.HORIZONTAL) {
            var relay = relay(h, pos, input, false);
            clickKnob(h, pos, 0, player); h.assertTrue(relay.getOut() == 1 && relay.getIn() == 1, "Output dial did not wrap15->1 for " + input);
            for (int n = 0; n < 15; n++) clickKnob(h, pos, 1, player);
            h.assertTrue(relay.getIn() == 1 && relay.getOut() == 1, "Input dial affected output or failed full wrap for " + input);
            clickKnob(h, pos, 1, player); clickKnob(h, pos, 0, player);
            h.assertTrue(relay.getIn() == 2 && relay.getOut() == 2, "Rotated physical dials did not address their own setting");
        }
        // Exact SOUTH source positions distinguish mirrored rotations and output/input swaps.
        h.assertTrue(RedstoneRelayBlock.knobBounds(Direction.SOUTH, 0).equals(new AABB(.125, .0625, .125, .375, .25, .375))
                && RedstoneRelayBlock.knobBounds(Direction.WEST, 0).equals(new AABB(.625, .0625, .125, .875, .25, .375))
                && RedstoneRelayBlock.knobBounds(Direction.NORTH, 1).equals(new AABB(.375, .0625, .125, .625, .25, .375)), "Original CodeChicken dial rotation changed");
        h.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void baseClickAndAdventurePlayerCannotAlterRelayTuning(GameTestHelper h) {
        var pos = h.absolutePos(CENTER); var relay = relay(h, pos, Direction.SOUTH, false); var player = player(h, pos);
        var state = h.getLevel().getBlockState(pos);
        relayBlock().use(state, h.getLevel(), pos, player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atLowerCornerOf(pos).add(.9, .125, .9), Direction.UP, pos, false));
        player.setGameMode(GameType.ADVENTURE); clickKnob(h, pos, 0, player); clickKnob(h, pos, 1, player);
        h.assertTrue(relay.getIn() == 1 && relay.getOut() == 15, "Non-dial click or original allowEdit=false changed settings"); h.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void configuredSignalUsesOriginalQuerySideInAllFourOrientations(GameTestHelper h) {
        var pos = h.absolutePos(CENTER);
        for (Direction input : Direction.Plane.HORIZONTAL) {
            var relay = relay(h, pos, input, true); relay.setOut(7);
            for (Direction side : Direction.values()) {
                var state = h.getLevel().getBlockState(pos); int wanted = side == input ? 7 : 0;
                h.assertTrue(state.getSignal(h.getLevel(), pos, side) == wanted && state.getDirectSignal(h.getLevel(), pos, side) == wanted,
                        "Weak/strong output leaked or reversed for input " + input + "/query " + side);
            }
            h.getLevel().setBlock(pos.relative(input), Blocks.REDSTONE_BLOCK.defaultBlockState(), 2);
            h.assertTrue(relayBlock().shouldBePowered(h.getLevel(), pos, h.getLevel().getBlockState(pos)), "Directional physical input was ignored");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void wireThresholdIsInclusiveAndLateralSignalDoesNotLockOrActivate(GameTestHelper h) {
        var pos = h.absolutePos(CENTER); var relay = relay(h, pos, Direction.NORTH, false); relay.setIn(15);
        h.getLevel().setBlock(pos.north().below(), Blocks.STONE.defaultBlockState(), 3);
        h.getLevel().setBlock(pos.north(), Blocks.REDSTONE_WIRE.defaultBlockState(), 3);
        h.getLevel().setBlock(pos.west(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 2);
        // New wire placement derives its power natively. Supply the boundary fixture only
        // after placement, using a same-block state change that does not rerun onPlace.
        h.getLevel().setBlock(pos.north(), h.getLevel().getBlockState(pos.north()).setValue(RedStoneWireBlock.POWER, 14), 2);
        h.assertTrue(relayBlock().calculateInputStrength(h.getLevel(), pos, relay.getBlockState()) == 14
                && !relayBlock().shouldBePowered(h.getLevel(), pos, relay.getBlockState()), "Lateral input bypassed threshold or dust power was lost");
        relay.setIn(14); h.assertTrue(relayBlock().shouldBePowered(h.getLevel(), pos, relay.getBlockState()), "Equality at input threshold failed");
        h.getLevel().setBlock(pos.north(), Blocks.AIR.defaultBlockState(), 2);
        h.assertTrue(!relayBlock().shouldBePowered(h.getLevel(), pos, relay.getBlockState()), "Side power invented repeater locking or input"); h.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void savedByteSettingsAndClientPacketRetainIndependentValues(GameTestHelper h) {
        var pos = h.absolutePos(CENTER); var relay = relay(h, pos, Direction.EAST, false); var player = player(h, pos);
        for (int n = 0; n < 6; n++) clickKnob(h, pos, 1, player);
        for (int n = 0; n < 4; n++) clickKnob(h, pos, 0, player);
        var saved = relay.saveWithoutMetadata(); var loaded = new RedstoneRelayBlockEntity(pos, relay.getBlockState()); loaded.load(saved);
        h.assertTrue(saved.contains("in", 1) && saved.contains("out", 1) && loaded.getIn() == 7 && loaded.getOut() == 4
                && relay.getUpdateTag().getByte("in") == 7 && relay.getUpdatePacket().getTag().getByte("out") == 4,
                "Original byte tuning failed save/load or native client synchronization"); h.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void schedulerRetainsTwoTicksAndOriginalPowerAndRepeaterPriorities(GameTestHelper h) {
        var positions = List.of(h.absolutePos(new BlockPos(2, 2, 2)), h.absolutePos(new BlockPos(5, 2, 2)), h.absolutePos(new BlockPos(8, 2, 2)));
        var on = relay(h, positions.get(0), Direction.NORTH, false); h.getLevel().setBlock(positions.get(0).north(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 2);
        var off = relay(h, positions.get(1), Direction.NORTH, true);
        var chained = relay(h, positions.get(2), Direction.NORTH, false);
        h.getLevel().setBlock(positions.get(2).south(), Blocks.REPEATER.defaultBlockState().setValue(RepeaterBlock.FACING, Direction.NORTH), 2);
        h.getLevel().setBlock(positions.get(2).north(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 2);
        var tiles = List.of(on, off, chained); var expected = List.of(TickPriority.HIGH, TickPriority.VERY_HIGH, TickPriority.EXTREMELY_HIGH);
        for (int n = 0; n < positions.size(); n++) {
            var pos = positions.get(n); relayBlock().updateState(h.getLevel(), pos, tiles.get(n).getBlockState());
            h.assertTrue(scheduled(h, pos).size() == 1 && scheduled(h, pos).get(0).triggerTick() - h.getLevel().getGameTime() == 2
                    && scheduled(h, pos).get(0).priority() == expected.get(n), "Relay delay/priority differs from -1/-2/-3 at " + n);
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "redstone_native", timeoutTicks = 30)
    public static void vanishedShortInputStillProducesOriginalTwoTickNativeOutput(GameTestHelper h) {
        var pos = h.absolutePos(CENTER); relay(h, pos, Direction.NORTH, false);
        h.getLevel().setBlock(pos.north(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        h.getLevel().removeBlock(pos.north(), false); long armed = h.getLevel().getGameTime();
        var observations = new java.util.HashMap<Long, Boolean>();
        h.onEachTick(() -> observations.put(h.getLevel().getGameTime() - armed, enabled(h, pos)));
        h.runAfterDelay(7, () -> {
            h.assertTrue(Boolean.FALSE.equals(observations.get(1L)) && Boolean.TRUE.equals(observations.get(2L))
                    && Boolean.TRUE.equals(observations.get(3L)) && Boolean.FALSE.equals(observations.get(4L)),
                    "Ordinary server scheduling lost the disappeared-input two-tick pulse: " + observations); h.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = "redstone_native", timeoutTicks = 40)
    public static void originalOutputSideActuallyPowersVanillaLampAndTurnsItOff(GameTestHelper h) {
        var pos = h.absolutePos(CENTER); var tile = relay(h, pos, Direction.NORTH, false); tile.setOut(7);
        h.getLevel().setBlock(pos.south(), Blocks.REDSTONE_LAMP.defaultBlockState(), 3);
        h.getLevel().setBlock(pos.north(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        h.runAfterDelay(4, () -> {
            h.assertTrue(enabled(h, pos) && h.getLevel().getBlockState(pos.south()).getValue(RedstoneLampBlock.LIT), "Configured original opposite-facing output did not power a real lamp");
            h.getLevel().removeBlock(pos.north(), false);
        });
        h.runAfterDelay(12, () -> { h.assertTrue(!enabled(h, pos) && !h.getLevel().getBlockState(pos.south()).getValue(RedstoneLampBlock.LIT), "Relay/input removal left a vanilla lamp powered"); h.succeed(); });
    }

    @GameTest(template = TEMPLATE)
    public static void lostRelaySupportDropsExactlyOnePlainItemWithoutStoredSettings(GameTestHelper h) {
        var pos = h.absolutePos(CENTER); var relay = relay(h, pos, Direction.WEST, false); relay.setIn(9); relay.setOut(3);
        h.getLevel().removeBlock(pos.below(), false);
        var drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1), entity -> entity.getItem().is(relayBlock().asItem()));
        h.assertTrue(h.getLevel().isEmptyBlock(pos) && h.getLevel().getBlockEntity(pos) == null && drops.stream().mapToInt(e -> e.getItem().getCount()).sum() == 1
                && drops.stream().noneMatch(e -> e.getItem().hasTag()), "Unsupported relay duplicated/lost its original plain item or kept tuning"); h.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void realEarBlockItemSupportsAndPaysForAllSixClickedFaces(GameTestHelper h) {
        var pos = h.absolutePos(CENTER); var player = player(h, pos);
        for (boolean toggle : new boolean[]{false, true}) for (Direction face : Direction.values()) {
            clear(h, pos); var support = pos.relative(face.getOpposite()); h.getLevel().setBlock(support, Blocks.CLAY.defaultBlockState(), 3);
            var stack = new ItemStack(earBlock(toggle), 2); player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            var hit = new BlockHitResult(Vec3.atCenterOf(support).add(face.getStepX() * .5, face.getStepY() * .5, face.getStepZ() * .5), face, support, false);
            h.assertTrue(stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction() && stack.getCount() == 1
                    && h.getLevel().getBlockState(pos).getValue(ArcaneEarBlock.FACING) == face && !enabled(h, pos)
                    && h.getLevel().getBlockEntity(pos) instanceof ArcaneEarBlockEntity, "Paid six-face original ear placement failed " + toggle + "/" + face);
        }
        h.assertTrue(earBlock(false).defaultBlockState().getValue(ArcaneEarBlock.FACING) == Direction.DOWN, "Discarded original withProperty(UP) default was replaced with neutral preview NORTH"); h.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void pitchClickWrapsTwentyFiveAndSavesNativeNoteToneBytes(GameTestHelper h) {
        var pos = h.absolutePos(CENTER); var ear = ear(h, pos, Direction.UP, false, Blocks.GOLD_BLOCK, 0); var player = player(h, pos);
        for (int n = 0; n < 25; n++) earBlock(false).use(ear.getBlockState(), h.getLevel(), pos, player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        h.assertTrue(ear.note == 0 && ear.tone == 6 && !enabled(h, pos), "Ear pitch failed25->0 or tuning alone activated it");
        ear.changePitch(); var saved = ear.saveWithoutMetadata(); var loaded = new ArcaneEarBlockEntity(pos, ear.getBlockState()); loaded.load(saved);
        h.assertTrue(saved.contains("note", 1) && saved.contains("tone", 1) && loaded.note == 1 && loaded.tone == 6
                && ear.getUpdatePacket().getTag().getByte("note") == 1, "Pitch/instrument byte keys or native client packet were lost");
        saved.putByte("note", (byte) -1); loaded.load(saved); h.assertTrue(loaded.note == 0, "Negative note did not clamp to source0");
        saved.putByte("note", (byte) 127); loaded.load(saved); h.assertTrue(loaded.note == 24, "Large note did not clamp to source24"); h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "redstone_ear_direct")
    public static void tenOriginalSupportInstrumentsMatchRealForgeNoteEvents(GameTestHelper h) {
        Block[] supports = {Blocks.DIRT, Blocks.STONE, Blocks.SAND, Blocks.GLASS, Blocks.OAK_PLANKS, Blocks.CLAY, Blocks.GOLD_BLOCK, Blocks.WHITE_WOOL, Blocks.PACKED_ICE, Blocks.BONE_BLOCK};
        var pos = h.absolutePos(CENTER); var source = h.absolutePos(new BlockPos(8, 2, 4));
        h.getLevel().setBlock(pos.below(2), Blocks.STONE.defaultBlockState(), 3);
        for (int instrument = 0; instrument < 10; instrument++) {
            var ear = ear(h, pos, Direction.UP, false, supports[instrument], 10 + instrument);
            ear.updateTone(); h.assertTrue(ear.tone == instrument, "Original support material tone changed at " + instrument);
            immediateNativeNote(h, source, NoteBlockInstrument.values()[instrument], 10 + instrument); tickEar(h, ear);
            h.assertTrue(enabled(h, pos) && ear.redstoneSignal == 10 && ear.getBlockState().getSignal(h.getLevel(), pos, Direction.UP) == 15,
                    "Native Forge note event did not reach an exact original instrument/note receiver " + instrument);
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "redstone_native", timeoutTicks = 40)
    public static void oneRealNoteActivatesAllListenersAndNormalPulseExpiresAtTen(GameTestHelper h) {
        var pos = h.absolutePos(CENTER); var a = ear(h, pos, Direction.UP, false, Blocks.DIRT, 11);
        var b = ear(h, h.absolutePos(new BlockPos(6, 2, 4)), Direction.UP, false, Blocks.DIRT, 11);
        record Sample(long time, int signal, boolean powered, boolean second) {}
        var samples = new ArrayList<Sample>();
        Consumer<TickEvent.LevelTickEvent> witness = event -> {
            if (event.level == h.getLevel() && event.phase == TickEvent.Phase.END && h.getLevel().getBlockState(pos).getBlock() instanceof ArcaneEarBlock)
                samples.add(new Sample(h.getLevel().getGameTime(), a.redstoneSignal, enabled(h, pos), enabled(h, b.getBlockPos())));
        };
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGH, witness);
        nativeNote(h, h.absolutePos(new BlockPos(8, 2, 4)), Blocks.DIRT, 11);
        h.runAfterDelay(16, () -> {
            try {
                int first = -1; for (int n = 0; n < samples.size(); n++) if (samples.get(n).powered()) { first = n; break; }
                h.assertTrue(first >= 0 && samples.size() > first + 10, "Actual NoteBlock.attack/native ticker never produced a complete pulse");
                long start = samples.get(first).time();
                for (int n = 0; n < 10; n++) {
                    var sample = samples.get(first + n);
                    h.assertTrue(sample.time() == start + n && sample.powered() && sample.second() && sample.signal() == 10 - n,
                            "Same native note was consumed globally, repeated, or changed10-tick duration at " + n + ": " + sample);
                }
                h.assertTrue(!samples.get(first + 10).powered() && !samples.get(first + 10).second() && samples.get(first + 10).signal() == 0,
                        "Normal ear did not release both native outputs on tenth continuation");
            } finally { MinecraftForge.EVENT_BUS.unregister(witness); }
            h.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = "redstone_native", timeoutTicks = 35)
    public static void toggleFlipsOnceForSameTickBatchAndAgainForLaterNoteWithoutFeedback(GameTestHelper h) {
        var pos = h.absolutePos(CENTER); var ear = ear(h, pos, Direction.UP, true, Blocks.DIRT, 12);
        var source = h.absolutePos(new BlockPos(8, 2, 4)); var second = h.absolutePos(new BlockPos(8, 2, 6));
        nativeNote(h, source, Blocks.DIRT, 12); nativeNote(h, second, Blocks.DIRT, 12);
        h.runAfterDelay(3, () -> { h.assertTrue(enabled(h, pos) && ear.redstoneSignal == 0, "Multiple notes in one original batch flipped toggle twice or started a pulse timer"); });
        h.runAfterDelay(6, () -> { h.assertTrue(enabled(h, pos), "Own ear sound re-entered the native note listener"); Blocks.NOTE_BLOCK.attack(h.getLevel().getBlockState(source), h.getLevel(), source, player(h, source)); });
        h.runAfterDelay(10, () -> { h.assertTrue(!enabled(h, pos), "Later matching actual note failed the second latch flip"); });
        h.runAfterDelay(18, () -> { h.assertTrue(!enabled(h, pos), "A cleared note batch replayed or self-triggered toggle"); h.succeed(); });
    }

    @GameTest(template = TEMPLATE, batch = "redstone_ear_direct")
    public static void wrongPitchWrongInstrumentAndNewInstrumentDoNotTrigger(GameTestHelper h) {
        var pos = h.absolutePos(CENTER); var ear = ear(h, pos, Direction.UP, false, Blocks.DIRT, 21); ear.updateTone();
        var source = h.absolutePos(new BlockPos(8, 2, 4));
        immediateNativeNote(h, source, NoteBlockInstrument.HARP, 20);
        immediateNativeNote(h, source, NoteBlockInstrument.BASS, 21);
        immediateNativeNote(h, source, NoteBlockInstrument.IRON_XYLOPHONE, 21); tickEar(h, ear);
        h.assertTrue(!enabled(h, pos) && ear.redstoneSignal == 0, "Pitch/instrument mismatch or post-TC6 instrument triggered an original receiver"); h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "redstone_ear_direct")
    public static void cancelledNativeNoteIsSilentAndFinalInstrumentModificationIsObserved(GameTestHelper h) {
        var pos = h.absolutePos(CENTER); var ear = ear(h, pos, Direction.UP, false, Blocks.DIRT, 22); ear.updateTone();
        var source = h.absolutePos(new BlockPos(8, 2, 4));
        Consumer<NoteBlockEvent.Play> cancel = event -> { if (event.getLevel() == h.getLevel() && event.getPos().equals(source)) event.setCanceled(true); };
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGH, cancel);
        try { immediateNativeNote(h, source, NoteBlockInstrument.HARP, 22); tickEar(h, ear); h.assertTrue(!enabled(h, pos), "Cancelled native note activated ear"); }
        finally { MinecraftForge.EVENT_BUS.unregister(cancel); }
        Consumer<NoteBlockEvent.Play> modify = event -> { if (event.getLevel() == h.getLevel() && event.getPos().equals(source)) event.setInstrument(NoteBlockInstrument.HARP); };
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGH, modify);
        try { immediateNativeNote(h, source, NoteBlockInstrument.BASS, 22); tickEar(h, ear); h.assertTrue(enabled(h, pos), "Listener captured pre-event instrument instead of final native played instrument"); }
        finally { MinecraftForge.EVENT_BUS.unregister(modify); }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "redstone_ear_direct")
    public static void sixtyFourBlockSphereIncludesBoundaryAndRejectsBeyondIt(GameTestHelper h) {
        var pos = h.absolutePos(CENTER); var ear = ear(h, pos, Direction.UP, false, Blocks.DIRT, 23); ear.updateTone();
        // An explicit loaded remote coordinate is a range fixture, never a production chunk request.
        var far = pos.east(65); h.getLevel().getChunkAt(far);
        immediateNativeNote(h, far, NoteBlockInstrument.HARP, 23); tickEar(h, ear);
        h.assertTrue(!enabled(h, pos), "Beyond64 note crossed original spherical range");
        immediateNativeNote(h, pos.east(64), NoteBlockInstrument.HARP, 23); tickEar(h, ear);
        h.assertTrue(enabled(h, pos), "Exact64 boundary was excluded"); h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "redstone_ear_direct")
    public static void notesStayInTheirServerDimension(GameTestHelper h) {
        var pos = h.absolutePos(CENTER); var ear = ear(h, pos, Direction.UP, false, Blocks.DIRT, 24); ear.updateTone();
        var nether = h.getLevel().getServer().getLevel(Level.NETHER); nether.getChunkAt(pos);
        var state = Blocks.NOTE_BLOCK.defaultBlockState().setValue(NoteBlock.NOTE, 24);
        Blocks.NOTE_BLOCK.triggerEvent(state, nether, pos, 0, 0); tickEar(h, ear);
        h.assertTrue(!enabled(h, pos), "Other dimension's same coordinate note leaked into this ear");
        immediateNativeNote(h, pos, NoteBlockInstrument.HARP, 24); tickEar(h, ear);
        h.assertTrue(enabled(h, pos), "Same dimension native event failed"); h.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void supportChangesRetuneEarAndExternalPowerDoesNotActivateIt(GameTestHelper h) {
        var pos = h.absolutePos(CENTER); var ear = ear(h, pos, Direction.UP, false, Blocks.OAK_PLANKS, 0);
        h.getLevel().setBlock(pos.below(), Blocks.GOLD_BLOCK.defaultBlockState(), 3);
        h.getLevel().setBlock(pos.west(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        h.assertTrue(ear.tone == 6 && !enabled(h, pos), "Physical support did not retune tone or external power invented activation");
        h.getLevel().removeBlock(pos.below(), false);
        var drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1), e -> e.getItem().is(earBlock(false).asItem()));
        h.assertTrue(h.getLevel().isEmptyBlock(pos) && drops.stream().mapToInt(e -> e.getItem().getCount()).sum() == 1 && drops.stream().noneMatch(e -> e.getItem().hasTag()),
                "Lost support did not remove ear/drop a single original untuned item"); h.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void latchStateAndTuningReloadWithOriginalTransientPulseCounter(GameTestHelper h) {
        var pos = h.absolutePos(CENTER); var ear = ear(h, pos, Direction.WEST, true, Blocks.GOLD_BLOCK, 13);
        ear.updateTone(); earBlock(true).setSignal(h.getLevel(), pos, true); ear.redstoneSignal = 4;
        var tag = ear.saveWithoutMetadata(); var loaded = new ArcaneEarBlockEntity(pos, ear.getBlockState()); loaded.load(tag);
        h.assertTrue(enabled(h, pos) && loaded.note == 13 && loaded.tone == 6 && loaded.redstoneSignal == 0 && !tag.contains("redstoneSignal"),
                "Original latch state/tuning or transient redstoneSignal save contract changed");
        for (Direction side : Direction.values()) h.assertTrue(ear.getBlockState().getSignal(h.getLevel(), pos, side) == 15
                && ear.getBlockState().getDirectSignal(h.getLevel(), pos, side) == 15, "Enabled ear lost original all-face weak and strong15"); h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "redstone_native", timeoutTicks = 40)
    public static void ordinaryNoteEarRelayLampCircuitWorksWithoutDirectTickerCalls(GameTestHelper h) {
        var pos = h.absolutePos(CENTER); var relay = relay(h, pos, Direction.NORTH, false); relay.setIn(15); relay.setOut(7);
        var ear = ear(h, pos.north(), Direction.UP, false, Blocks.DIRT, 14);
        // ear fixture clears its six neighbors, so restore the real relay at the adjacent downstream position.
        h.getLevel().setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
        h.getLevel().setBlock(pos, relayBlock().defaultBlockState().setValue(RedstoneRelayBlock.FACING, Direction.NORTH), 3);
        relay = (RedstoneRelayBlockEntity) h.getLevel().getBlockEntity(pos); relay.setIn(15); relay.setOut(7);
        h.getLevel().setBlock(pos.south(), Blocks.REDSTONE_LAMP.defaultBlockState(), 3);
        nativeNote(h, h.absolutePos(new BlockPos(8, 2, 4)), Blocks.DIRT, 14);
        h.runAfterDelay(6, () -> { h.assertTrue(enabled(h, ear.getBlockPos()) && enabled(h, pos) && h.getLevel().getBlockState(pos.south()).getValue(RedstoneLampBlock.LIT),
                "Actual vanilla note -> ear15 -> configured relay7 -> vanilla lamp chain failed"); });
        h.runAfterDelay(20, () -> { h.assertTrue(!enabled(h, ear.getBlockPos()) && !enabled(h, pos) && !h.getLevel().getBlockState(pos.south()).getValue(RedstoneLampBlock.LIT),
                "Ordinary circuit remained powered after original pulse/relay/native lamp delays"); h.succeed(); });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 240)
    public static void loadedLegacyStatesAcquireMissingTilesAndExistingTuningIsUntouched(GameTestHelper h) {
        var pos = h.absolutePos(CENTER); var earPos = h.absolutePos(new BlockPos(8, 2, 4));
        var relayChunk = h.getLevel().getChunkAt(pos); var earChunk = h.getLevel().getChunkAt(earPos);
        var fixtureRelay = new RedstoneRelayBlockEntity[1];
        h.startSequence().thenWaitUntil(() -> {
            h.assertTrue(h.getLevel().getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) == relayChunk,
                    "Native relay fixture chunk has not become accessible");
            h.assertTrue(h.getLevel().getChunkSource().getChunkNow(earPos.getX() >> 4, earPos.getZ() >> 4) == earChunk,
                    "Native ear fixture chunk has not become accessible");
        }).thenExecute(() -> {
            var relay = relay(h, pos, Direction.EAST, true); relay.setIn(9); relay.setOut(4); fixtureRelay[0] = relay;
            var ear = ear(h, earPos, Direction.UP, true, Blocks.GOLD_BLOCK, 17); ear.updateTone();
            // Keep the saved enabled relay legitimately powered during real server ticks.
            h.getLevel().setBlock(pos.east(), Blocks.REDSTONE_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
            h.getLevel().removeBlockEntity(earPos);
            // The ear and relay can straddle a chunk boundary in the full test layout.
            LegacyRedstoneDeviceMigration.load(new ChunkEvent.Load(earChunk, false));
        }).thenWaitUntil(() -> h.assertTrue(earChunk.getBlockEntity(earPos,
                net.minecraft.world.level.chunk.LevelChunk.EntityCreationType.CHECK) instanceof ArcaneEarBlockEntity,
                "Native bounded END migration has not repaired the missing ear"))
                .thenExecute(() -> {
                    var relay = fixtureRelay[0];
                    h.assertTrue(h.getLevel().getBlockEntity(pos) == relay, "Migration replaced an existing native relay");
                    h.assertTrue(relay.getIn() == 9 && relay.getOut() == 4, "Migration overwrote existing relay tuning");
                    h.assertTrue(h.getLevel().getBlockState(pos).getValue(RedstoneRelayBlock.FACING) == Direction.EAST,
                            "Migration changed saved relay input facing");
                    h.assertTrue(enabled(h, pos), "Migration changed the physically powered relay state");
                    var nativeEar = (ArcaneEarBlockEntity) earChunk.getBlockEntity(earPos,
                            net.minecraft.world.level.chunk.LevelChunk.EntityCreationType.CHECK);
                    nativeEar.note = 17; nativeEar.updateTone();
                    LegacyRedstoneDeviceMigration.repair(h.getLevel(), earChunk);
                    h.assertTrue(h.getLevel().getBlockEntity(earPos) == nativeEar && nativeEar.note == 17 && nativeEar.tone == 6,
                            "Repeated loaded migration reset a real saved ear");
                }).thenSucceed();
    }
}
