# Late Void/Eldritch — BETA26 implementation contract

Authority: released Thaumcraft6.1.BETA26 JAR,
SHA256 `9425f8643581b27ff8845b087c8bc6fc10425a32942f1a3f0e265ce6b38f7b5f`.
Copied original research JSON is byte-identical to the JAR. Direct javap
inspection confirmed `TileVoidSiphon`'s20/40tick gates,2000progress,
1/33 shrink and short-tag methods, and `ItemVoidseerCharm`'s float
division/multiplication/truncation followed by integer gear-Warp division.
Recovered source is navigation, not authority over released bytecode.

This preparation adds UNLOCKELDRITCH, BASEELDRITCH, MATSTUDVOID, VOIDSIPHON,
ESSENTIASMELTERVOID, VOIDROBEARMOR, PRIMALCRUSHER and VOIDSEERPEARL.
The stage loader now includes the previously omitted original eldritch.json.

UNLOCKELDRITCH retains completed CrimsonRites/UNLOCKAUROMANCY, actual
f_VOIDSEED scan, Observation Basics16/Eldritch16 and stageWarp5.
BASEELDRITCH retains METALLURGY/UNLOCKELDRITCH, Theory Basics32/Alchemy32/
Eldritch32 and Warp2. MATSTUDVOID retains MATSTUDTHAUMIUM/BASEELDRITCH
and actual f_MATVOID ScanOreDictionary families: ingots, blocks and plates,
including compatible Forge tags. Seeds/nuggets do not substitute.

VOIDSIPHON, VOIDROBEARMOR, PRIMALCRUSHER and VOIDSEERPEARL retain their
original parents and Observation Eldritch16/Theory Eldritch32; stageWarp
is respectively1/1/1/3. ESSENTIASMELTERVOID retains its two completed parents,
Theory Alchemy32/Eldritch32 and one main-inventory Void plate. Original
unregistered `thaumcraft:metal` craft IDs remain ignored as BETA26 did.
Starting a bare recipe gate and completing its paid canonical stage remain
different predicates.

Void Ingot is a real Crucible recipe: one physical Void Seed and
Metallum10/Vitium5 produce one fresh ingot. Existing original Void equipment,
conversions and the native Void Smelter are reused. Void robe self-repair/
discount/Warp and Primal Crusher pickaxe+shovel/self-repair/innate infusion
enchantments were compared with their original classes; they are not
duplicated in this slice.

Six existing infusion paths become available through the new studies:
Void Siphon, three Void Robe pieces, Primal Crusher and Voidseer's Pearl.
The Pearl still needs the separate CURIOSITYBAND parent, whose implementation
must be connected before claiming natural acquisition.

## Native entity scan records

The scanner now accepts native class families for !ThaumSlime,
!CrimsonCultist, !EldritchGuardian, !TaintCrawler, !Taintacle and !TaintSwarm.
The catalogue registry ID alone is insufficient. The flattened native tiny
tentacle hierarchy is explicitly included because original EntityTaintacleSmall
extends EntityTaintacle; giant inheritance is retained. Original cultist
Knight/Cleric subclasses qualify; portals/leader do not merely by visual name.
Canonical single empty stages complete once at stage2 and pay5XP, so actual
cultist scanning also reaches the original !CrimsonCultist@2 loot denominator.
Matching/HUD hover remains read-only.

## Void Siphon runtime and presentation

The original block ID/model and enabled state are retained. Native collision
boxes reproduce the three BETA26 boxes, including its asymmetric orb X/Z
maximum. A real neighbor redstone signal disables processing. The interval
counter continues while paused and is not saved. Every20 eligible ticks the
original cubic query finds living rifts, size at least2, and checks line of
sight from the one-block-advanced source point to the actual rift origin.

Each accepted rift adds integer sqrt(size), loses sqrt(size)/15 stability
and has the original1/33 chance of shrinking by1. Every2000progress makes one
physical Void Seed in a real one-slot inventory; full/wrong output stops rift
debit. The original progress<2000 entry guard and signed-short save semantics
remain. Multiple-rift overshoot/full-output behavior is not silently rewritten.

All faces expose extraction and reject automation insertion. The original
output-only GUI uses the exact released176x166 panel, output slot80/32 and
unchanged inventory positions; it adds no numeric progress/dev labels.
Original progress property is synchronized even though its GUI does not draw
it. Retained capability objects also require the loaded live tile and server
thread; this prevents extraction from an unloaded stale saved inventory.

The native BER uses the original two six-face ender-shader boxes, offsets,
scales, rotation order, blend and depth-write choices. Event5 every40 eligible
ticks starts an original FXVoidStream with .04scale and the original counter
seed. Stream lifetime, acceleration/drag/noise, growing trail, radii and two
three-sided ANGLE/CAP/CLOSED passes are retained; the audited CoreGLE helper
is adapted locally to three sides without editing the Bolt helper. The exact
original shader is reused through the modern0.41 FluxRiftShader API.

Modern differences are explicit: finite/zero-distance guards, loaded-only
query/collision paths, .2-wide native modern particle collision, a4096 local
stream budget, and proper render-state boundaries. Particle RNG is independent
modern random state rather than an assertion of the old world's random history.
Ender shader ignores old stream texture V scrolling. This is a visual port,
not a bit-identical original1.12GPU framebuffer proof.

Intermediate progress explicitly marks the chunk dirty for reliable modern
saving, in addition to original completion synchronization. A bounded shared
loaded-chunk migration replaces only absent/storage-free catalogue anchors;
native paid inventories are never replaced.

## Validation status and supplied fixtures

Eight `LateVoidProgressionGameTests` cover strict parent and actual seed-scan
gates, every mixed-knowledge shortage, seven paid conclusions/Warp/replay/
reload, physical Void Ingot output, Void material factory gates, native scan
families, real Curios Pearl storage/formulas and all six physical paid altar
outputs. The altar fixture reuses the existing native paid-altar layout and
callbacks, with physical jars/components/candles and restored owned terrain.

Six `VoidSiphonGameTests` cover the20tick boundary and stability debit,
cubic range/LOS, real redstone, full-output/capability extraction, signed-short
reload/stale-tile guard, and storage-free migration.

Raw parent knowledge, materials, hot Crucible state, initial progress, rift
actors, supported stable altar, essential jars and controlled QA arenas are
fixtures. Tests use actual production callbacks and physical inventories;
they do not prove natural world acquisition or a complete survival run.
No native server/client/Gradle execution or release pass is declared here.
Root records actual QA, screenshots and final scope separately.
