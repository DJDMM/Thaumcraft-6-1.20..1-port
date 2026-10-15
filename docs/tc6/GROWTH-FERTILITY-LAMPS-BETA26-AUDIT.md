# Growth and fertility lamps: released TC6 BETA26

This records source verification and implementation. Test annotations are not a
passed execution claim; native server/client results belong to VALIDATION and
the finished artifact report.

Primary authority: `Thaumcraft-1.12.2-6.1.BETA26.jar`, SHA256
`9425f8643581b27ff8845b087c8bc6fc10425a32942f1a3f0e265ce6b38f7b5f`.
Readable source is pinned to `954022bb777b7546281fb36df8522f0ba6b43f81`.
Both tiles, BlockLamp, BlockTCDevice and CropUtils were read and checked against
released `javap -p -c`; ModConfig's five original crop registrations were read.

| Released class | SHA256 |
| --- | --- |
| TileLampGrowth | `97ef1e9c7a1eabcd57c75e771e55efa07a43c0a91d4c144b30a3b77f911d9cb6` |
| TileLampFertility | `c63dbf94648b7b6b399b7f2ec4ff59515ba4814a2eaff1a189c1e847fd64e331` |
| BlockLamp | `5946bfebb9f6956225cc469f0729362d0a6a724fc381eef98a552a479d61e538` |
| CropUtils | `9a2427f9ec1ab3fde635c1b77564d62a9582018e5ce041fba19a0fc1f5e5501d` |
| ModConfig | `3998e471055d26d5f9ae9d18cb47fcb0ce02f99e4904ed6957f3788b00158c5a` |

## Mounted block and fuel boundary

Existing `lamp_growth` and `lamp_fertility` IDs retain their original models.
Both are metal, hardness1, fixed box(.25,.125,.25)..(.75,.875,.75), face their
support, emit15 when enabled and0 when disabled, and are not full support faces.
The original permits any non-air support; modern persistent glimmer is explicitly
excluded because its non-air palette storage is only a compatibility adaptation.
Support removal produces one plain lamp. BlockTCTile's unconditional harvest is
retained: copying iron's correct-tool requirement would invent a restriction.
No inventory, GUI, bone meal, vis feed or additional feed recipe is introduced.

Placement starts facing the clicked side's opposite and disabled. Native onPlace
applies inverse neighbouring redstone; a later tile tick disables an empty lamp.
Neighbour changes skip the inherited redstone update when charges<=0, as in the
release. Both variants continue refilling while powered. Redstone stops plant or
animal work; it does not reset their paid reservoirs or transient search/counter.

Only the support-facing port is connectable/input. Output is always false;
getEssentiaType is null, amount0, raw push/take0, minimum suction0. A pull tries
only that adjacent port. `++drawDelay%5` counts attempts, not global ticks. Source
must connect, output and have strictly smaller suction. The original has no
minimum-suction/amount pre-gate: it requests exactly one Herba or Desiderium and
accepts only an actual takeEssentia return1.

Modern callbacks use exact native source classes and saved reservoir verification;
opaque subclasses are refused before their paid callbacks. A mixed buffer's
neighbours are also preflighted against exact native classes, including the two
final lamp types, before its nested stronger-consumer queries. The actual source
must remain installed and lose exactly one unit before a lamp gains its payment.
The shared rollback API restores charges/reserve only; it never rewinds drawDelay,
animal count or shuffled columns. True load resets those original unsaved fields.
This is a bounded native-source guarantee, not universal addon callback rollback.

## Growth

Starts charges=-1, maximum20, reserve=false. At charges<=0 it converts a saved
reserve to20, otherwise attempts a Herba pull for20. It then independently tries
to buy a reserve when no reserve exists. An empty tick can therefore advance the
draw-attempt counter twice. A full paid charge plus reserve advertises suction0;
otherwise its facing port advertises128. A reserve represents one paid Herba,
not one extra plant charge. Literal charges0 becomes-1 and syncs before work.

Every unpowered tick with positive charges examines one column. Empty checklist
is filled by X=-6..6 then Z=-6..6, starting at Y+6, and Fisher-Yates shuffled with
the world RNG (same nextInt sequence as Collections.shuffle). It scans down to
Y-6, picks the first eligible plant strictly inside squared distance36, spends
one charge, records block/state, schedules a native block tick one tick later and
returns. Full maturity is checked; no direct bone meal or forced age advance is
used. A selected native callback can legitimately fail to grow because of its
own light, soil, geometry, Forge growth veto or random roll.

The original isPlant accepts IGrowable or CACTUS/PLANTS material, excluding GRASS.
Modern equivalents are BonemealableBlock/BushBlock/Cactus/SugarCane, with
GrassBlock excluded. `isValidBonemealTarget` is used only as the canGrow predicate,
never to perform bone meal. Original CropUtils keeps mature stems eligible,
rejects standard Nether Wart age3, and regards an upper reed/cactus over the same
block as a grown stacked crop. These defaults are explicit. Ordinary mature crops
are rejected. `LampPlantRules.blacklist(BlockState)` exposes exact-state lamp
exclusion; there are no invented built-in blacklist entries. This does not claim
binary compatibility with 1.12 IMC payloads or arbitrary modern plant addons.

Before selecting a new column the release compares the previous selected block
and metadata. On change, if a player is within32, it sends BlockMist4259648 to
players within32. The modern comparison uses native blockstate (the replacement
for block+metadata). The original frame56/additive layer0/8-puff implementation
is documented in the separate client audit. Selection itself emits no success
effect and awards no knowledge.

### Modern one-tick callback adaptation

Released World.scheduleUpdate invokes Block.updateTick, which contains ordinary
1.12 plant growth. Modern CropBlock/Sapling/Stem/Cactus/SugarCane split this into
randomTick while their scheduled tick is absent or handles survival. Merely
scheduling modern wheat would silently do no growth. The port keeps the native
scheduled tick and persists an independent one-tick randomTick bridge in saved
world data. It calls native state.randomTick once, never performBonemeal.

Native LevelTicks.hasScheduledTick rejects an already pending position/block;
the lamp still pays its original charge on a duplicate selection. Accepted jobs
retain their due time, so a new native job accepted after the previous native tick
ran can coexist with that previous job's not-yet-executed END bridge. Due records
survive save/unload and are retired before invoking native callbacks; a reentrant
save cannot replay them. Identity is position+block, like the original scheduler,
so foreign block replacement receives no growth callback. State/age changes of
the same block remain valid, also as in the release. Pending work survives lamp
removal and redstone changes because the original world job is independent.

The mapped Forge1.20.1 ServerLevel private scheduled-tick readiness is exactly
areEntitiesLoaded(packedChunkPos) && chunkSource.isPositionTicking(packedChunkPos).
The bridge uses those public methods, with actual packed chunk coordinates;
getChunkNow alone is insufficient. It defers while capture/restoration is active,
never obtains a chunk ticket and retains unavailable jobs rather than running
them in merely loaded inactive chunks. Scheduling under an existing unrelated
modern native job does not infer or duplicate that other job's callback.

This is an explicit updateTick/randomTick compatibility adaptation. It does not
claim identical RNG interleaving with 1.12's global world scheduler or support
for every new modern crop whose scheduled and random callbacks both grow.

### Loaded footprint of the native plant callback

The target's loaded/ticking status alone is insufficient. Mapped SaplingBlock's
initial isAreaLoaded(pos,1) does not cover AbstractTreeGrower.hasFlowers, which
reads X/Z+/-2 before selecting its tree feature. Native tree placement reads a
wider region still. The bridge now checks all already-loaded chunks within a
conservative X/Z32 around any SaplingBlock (including MangrovePropaguleBlock and
the two TC saplings),8 around MushroomBlock, and2 around other plants. It checks
world-border corners too. A missing chunk leaves the paid intent saved and does
not enter the callback or consume RNG. The guard runs before retirement; a stale
foreign block is still retired without invoking a plant callback. It adds no
chunk tickets and never calls a chunk-creating getter.

The32-block bound covers the shipped vanilla1.20.1 sapling configurations and
this port's fixed TC features. Released/mapped TreeFeatures bootstrap and native
trunk/foliage/root/decorator callbacks were examined; ordinary vanilla trunks and
crowns are smaller than16 horizontally. The largest mangrove root search has
width8/length15 plus its initial offset, within16; its shipped decorations do not
extend that horizontal maximum. Native leaf-distance processing reaches6 further
cells and edge-shape probes one more, remaining below32. Cherry and mangrove
remain eligible; the guard does not turn them into decorative blocks.

TC Greatwood's limit is at most21. Its largest crown layer is at most5.25;
1.66*5.25*1.328<11.58, so rounded branch nodes fit+/-12. Adding the conservative
three-cell leaf loop and the mega-tree origin shift gives16. Silverwood's fixed
leaf loop is+/-5 with roots at2. Those native reads and neighbour probes fit32.

Mushroom randomTick has a+/-4 density scan, then an initial one-cell step and four
walk steps/final probe; its direct horizontal read reaches5 and the resulting
neighbour probe fits6, so8 is conservative. Giant mushroom feature placement is
a performBonemeal path and is not called by this bridge. Standard crops/stems,
reeds/cactus and the other shipped small plants use their local soil/adjacency
reads within2. Azalea has no added randomTick implementation here: no invented
bone-meal feature invocation is introduced.

These bounds do not certify arbitrary addon callbacks, Forge event listeners or
datapacks replacing built-in configurations with larger features. Such extensions
are outside this native-footprint evidence. Ordinary world random ticking is also
not intercepted globally by the lamp bridge.

Two native regression definitions supplement the original thirty core checks.
One uses an isolated far arena: the target lies at localX31 in a genuinely
block-ticking neighbouring chunk while its32-block footprint reaches an absent
outer chunk3. A saved due-intent fixture must retain payment/state and preserve
the RNG sequence without loading that chunk. After the test explicitly loads
the footprint, the restored intent runs one actual native sapling randomTick
and a second invocation cannot replay it. Another runs actual native stage0->1
callbacks for all eight vanilla saplings/propagules (including cherry/mangrove)
and both TC saplings under full loaded readiness. Supplied saved due records,
daylight/soil and a known native1-in7 RNG roll are explicit fixtures; these tests
do not claim complete tree growth or a survival source for their paid intent.

## Fertility

Starts charges0/count0/drawDelay0. While charges<10, each fifth draw attempt pays
one Desiderium and adds1 charge; facing suction is exactly128-charges*10. Charges
and suction remain signed: the original can have negative charges. Light is
disabled at<=1 and enabled above1 while unpowered within the refill branch.

Only an unpowered tick with charges>1 advances `count++`; updateAnimals runs at
old count%300==0, including the first active tick. The search is the block AABB
grown7 in all axes, not a radius7 sphere. The first eligible animal has age0 and
is not already in love. Its population list includes every returned animal of
the exact same runtime class, including babies/cooldown/already-in-love animals.
Population>9 blocks that class. The first two age0/not-in-love members are each
setInLove(null); exactly5 charges are spent and the outer search returns after
that one pair. Different runtime classes do not pair, no player gets love credit,
and the lamp does not directly create a baby or bypass ordinary breeding AI.
The >1 gate intentionally permits2->-3; it is not repaired into a five-unit gate.

Loaded-only preflight covers min=block-7/max=block+8 plus the literal2.0 X/Z
margin verified in mapped Forge EntitySectionStorage.forEachAccessibleNonEmptySection.
This includes a potential intersecting
entity whose centre lies outside the query AABB, rather than silently truncating
population at the lamp's chunk edge. Missing neighbouring chunks defer that
pulse; no search loads them. The already-qualified active clock still advances,
preserving its original conditional increment.

## Physical ownership, migration and native definitions

The two BlockItems retain actual native geometry/NBT/sound/criteria but use a
provisional stack during placement. The original physical hand remains unpaid
through the real Forge capture=false placement event. A pending receipt prevents
unpaid NBT charges from selecting plants, breeding, pulling essence or issuing
support drops. Commit checks the same physical identity/count/NBT, creative and
build permissions, exact installed tile/state and native location permissions.
Canceled rollback restores only its own installed owner and removes only its
own captured receipts; callback-owned hands, chest contents or foreign targets
remain intact. Existing ordinary Arcane Lamp placement is unchanged.

Unsupported paid lamps use saved physical drop receipts with vanilla chance1 RNG,
random .25..75 offsets, pickup10 and doTileDrops. A canceled join remains pending;
onAddedToWorld marks actual native acceptance monotonically. Accepted/consumed
entities are retired and excluded from reentrant saves. No chunk is forced for a
retry. This ledger is separate from the lamps' fuel reservoirs; breaking them
drops the ordinary plain lamp, not fabricated essence or a charged portable item.

The shared64/200 loaded migration queue scans palette states because old catalogue
lamps had no BE. It repairs absent/exact empty catalogue anchors only, preserves
foreign/nonempty native owners, refreshes client tiles and never loads chunks.
Native paid charge/reserve and running clock are left untouched on repeated repair.

Thirty-two new core GameTest definitions cover paid charge/reserve/draw cadence, wrong
fuel, redstone refill/pause, signed suction, maturity/stem/stack defaults, all169
columns, strict sphere/top-down selection, scheduled one-tick/no-bonemeal behavior,
persisted identity and multiple accepted deadlines, actual native cactus growth
after lamp removal, no-load/capture constraints, original condenser closed-output
early return, actual Cow/Pig exact-class population/baby/cooldown/love gates, first
and300th pulses, cube corners, reservoir-only updates and loaded migration/drop
retry. Six new placement definitions exercise both lamps on all six mounted faces,
survival/creative/canceled count1, real provisional charged NBT, native permission
revocation and foreign paid chest/hand/target preservation. Existing eight ordinary
Arcane Lamp tests and production behavior are retained. Execution evidence must
be appended only after root's fresh full server and reviewed hidden-client runs.

## Фактически завершённый QA0.35

`lamps-035-server3` прошёл1422/1422; `lamps-035-client2`15/15, оба nativeexit0.
Два проверяющих просмотрели все15полных1600×1000PNG и оба листа.
Реальные130эссенции/100vis/4кристалла/64rawTheory/20XP, 320тиков паузы,
native рост при randomTickSpeed0, сердца/AI-рождение и физическая разборка
связаны с финальными logs/SHA. Подготовленные условия, принятый modern1tick
bridge, source quirks, ранние попытки и ограничения сохранены в
[VALIDATION.md](../../VALIDATION.md) и
[artifact-report0.35](../../validation/artifact-report-0.35.json).
