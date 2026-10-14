# Flux Condenser — released TC6 BETA26 audit

Authority: the released `Thaumcraft-1.12.2-6.1.BETA26.jar`, SHA-256
`9425f8643581b27ff8845b087c8bc6fc10425a32942f1a3f0e265ce6b38f7b5f`.
`javap -p -c` of `TileCondenser`, `BlockCondenserLattice`, `BlockTCTile` and
`BlockTCDevice` was inspected directly. The pinned decompiled source
`954022bb777b7546281fb36df8522f0ba6b43f81` supplies names and cross references.
This document records implemented contracts; it does not claim a passed build,
native server run, client run or ordinary survival playthrough.

## Two reservoirs and original operating order

The IDs remain `condenser`, `condenser_lattice`, `condenser_lattice_dirty`.
The condenser implements `IEssentiaTransport`, not a Forge fluid handler or
`IAspectSource`. Generic input power and produced Vitium each have capacity100.
Only original short NBT fields `essentia` and `flux` persist. Topology, weighted
count, interval, cost and tick count are recalculated after reload. Network
updates replace only the two reservoir values, preserving the transient visual
cadence and topology between ordinary five-tick input updates.

The global per-tile count increases even while disabled. Redstone disables
pulling and conversion, while original advisory suction and DOWN extraction
remain available. Horizontal input sides advertise untyped suction128 until
power reaches100. UP is not connectable; DOWN alone outputs Vitium. The raw
output accessor reports Vitium and its amount on every face; actual extraction
checks DOWN and accepts the Vitium aspect or null, never generic power.

Every fifth enabled tick with a working lattice pulls at most one unit from
each eligible source, in released `EnumFacing.HORIZONTALS` order SOUTH, WEST,
NORTH, EAST. It requires an open reverse connection, output permission, a
positive physical source amount, source suction strictly below128, and reached
minimum suction. A connected earlier source that cannot output causes an
immediate **return**, blocking later sides. Ordinary aspect types contribute
to one untyped power reservoir. A Vitium source instead clogs a clean lattice;
its physical Vitium is **not** extracted. This is not a global graph scan or a
one-source-only pull.

Conversion requires the exact transient modulo boundary, enough generic power,
output below100 and at least1 aura Flux. It drains1 aura Flux, spends `cost`
power and stores1 Vitium. A successful conversion independently rolls
`nextInt(50)==0` to dirty a lattice. The dirty choice uses the current clean
DFS list; a selected index0 is rolled once again, even with a one-entry list.
This source bias is retained. Filling occurs before the conversion check.

## Weighted source DFS, support and connected rods

The condenser enters the lattice only directly UP. Its traversal order is
DOWN, UP, NORTH, SOUTH, WEST, EAST. Counted nodes must lie above the base and
within squared distance74, inclusive. Both clean and dirty nodes belong to
the connected-size list used for cost; only clean nodes belong to the spark
and clog-choice list. All neighboring lattice sides contribute to the side
penalty, including a previously visited neighbor or one outside the counted
region.

A node contributes `1 - .15 * neighboringLatticeSides` only if that traversal
found an unvisited child and the path is not clogged. Leaves contribute
nothing. The condenser root itself contributes. A dirty first node clogs the
whole path; a dirty later node preserves clean ancestor weight and blocks its
descendants. A second unvisited condenser directly DOWN sets the original
negative sentinel; no shared processor ownership is invented.

Positive weight is capped40. The interval is
`max(5, round(600 - weightedCount * 15))`, and power cost is
`floor(4 + sqrt(totalConnectedLattices))`. One vertical lattice gives
weight.85 /587ticks /cost5; two give1.70 /575 /5; four give3.10 /554 /6.
The first lattice does not count its DOWN condenser as a lattice side; only
actual clean/dirty lattice neighbors contribute to the penalty.
Weight is not simply the number of lattice blocks or the number of clean rods.

The lattice support search is a separate full connected-component DFS with
the original direction order. It uses the **first** reachable condenser
directly DOWN of a lattice, then compares the clicked member's squared
distance to that base against74. Its support traversal is not the condenser's
above-base radius-limited power calculation. Unsupported or distant members
drop their own clean/dirty block item. Both support and weighted post-order
traversals are iterative while retaining the released direction/entry/return
order, avoiding Java recursion overflow in dense or long connected structures.

Original connected model properties are six booleans. A condenser connects a
lattice only on its DOWN face. The selection/collision box starts at5/16 to
11/16 and extends to the edge of each connected side. Clean light is4;
dirty light is0. The condenser retains full collision with non-full visual
occlusion, no redstone-conductor behavior and no sturdy supporting face.
The released non-opaque/non-full block never becomes a vanilla normal cube;
modern full collision must not allow comparator notifications through it. Released hardness/resistance are
2/effective12 for the condenser, .5/effective3 for lattice; the effective
resistance reflects old setResistance×3/getExplosionResistance÷5. Native
condenser harvesting has no correct-tool requirement, matching released
`BlockTCTile.canHarvestBlock()==true`; lattice retains its separate iron/tool
policy. Both variants retain the same native connected static block models.

## Filters, destruction and actual paid resources

Only a dirty lattice and a held actual `filter` clean the block. Exactly one
physical filter is consumed **even in creative**, as in the released method.
The source `nextBoolean()` determines one real single-aspect Vitium crystal
with probability one-half. It spawns at the center plus horizontal clicked-face
offset/3; the UP/DOWN click does not change Y. The original item-pickup sound
volume.2 and random pitch formula are retained. Clean lattice and wrong items
do not spend filters or create rewards.

Unsupported-block drops retain the separate vanilla `doTileDrops` rule,
chance1 RNG roll, spawn position `.25 + nextFloat * .5` on each axis and default
pickup delay10. Filter-crystal direct spawns remain independent of that block
loot rule and retain their original immediate pickup delay0. Their serialized
drop owner keeps the chosen position and delay across retry/reload; no rule
change after already-paid ownership re-rolls the reward.

Destroying a condenser returns only its currently stored Vitium amount to
aura Flux, using the original visible pollution effect. It does **not** turn
the generic power reservoir into Flux. The generic power is discarded, as in
released `BlockTCTile.breakBlock`: that method queries only
`getEssentiaAmount(UP)`, which this tile maps to stored Vitium. Removal payment
is guarded once before callbacks; changing only the enabled blockstate keeps
the same paid tile and never spills a reservoir. Nearby lattice support is
reevaluated and actual unsupported lattice blocks produce physical items.

## Explicit modern adaptations and compatibility boundary

The released `addEssentia` bytecode reports
`min(requested, 100-essentia)` but never writes that amount to `essentia`.
Copying its return value would silently delete the caller's paid essence.
The port deliberately reports0 for raw pushes without crediting either
reservoir; the working, original feeding path remains the device's own pull.
This is an API conservation adaptation, not an invented secondary buffer or
typed remote demand. A pump cannot satisfy an untyped condenser demand by
pretending a raw push stored essence. A real adjacent extraction pump can
transfer the DOWN Vitium output through the shared guarded handoff contract.

Pull transactions accept exact native transport classes, compare actual
physical source decrease, and restore a still-owned source snapshot on a
failed receipt. Opaque addon callbacks/subclasses are not admitted to a paid
take: their reported success cannot establish durable physical ownership or
safe rollback. Source selection, source-ordered early returns and Vitium clog
queries are retained; this does not claim arbitrary addon transport support.

All world topology/shape/support/migration reads are limited to already
loaded chunks. Incomplete boundary topology defers pulling/conversion and
support drops instead of force-loading or treating an unavailable branch as
air. Reload recomputes the graph. The support check runs one scheduled server
tick after native lattice placement so modern `BlockItem.useOn` completes
the survival hand debit before unsupported paid-block ownership moves to a
drop; no item is generated during an unpaid failed placement.

The real Forge47.4.10 `ForgeHooks.onPlaceItemIntoWorld` clears capture and
temporarily restores the original hand count **before** posting its placement
event. A capture flag alone cannot establish paid ownership. The custom
`CondenserBlockItem` therefore delegates native geometry/NBT/criteria/sound to
a provisional copied stack, retaining the actual held stack until the native
Forge place callback accepts the revalidated world target and physical hand.
Its callback still uses original `capture=false` semantics. Pending positions
guard native ticks, extraction, removal pollution, filter use, redstone and
connection updates until actual payment. Provisional `BlockEntityTag` essence
cannot be extracted or spilled by a callback, and removed references cannot
release it afterward.

Only the exact owned snapshot objects are removed. An unrelated captured
transaction is preserved and rejected before this action pays, preventing the
outer Forge wrapper from re-posting another action and restoring this paid
item's pre-event count. Rollback retains foreign replacement block IDs and
their contents; property changes on the still-owned block/BE cannot leave an
unpaid target alive. Ordinary survival and creative physical item-count rules
remain native, with one Forge placement event per target and no duplicate
stats callback. Native cleanup likewise rejects a pre-existing captured
scope, uses a separate pending receipt and removes only its own snapshots.

Both native condenser and lattice tick entry points defer while block snapshots
are captured/restored or a target is pending. An external Forge place callback
cannot manually tick an uncommitted target into drawing paid source essence,
draining aura, advancing its clock or creating unsupported-block loot.
Committed, live tiles still advance the original counter on every tick,
including disabled or unavailable-power ticks.

Redstone readiness preflights the bounded native neighbor/direct-signal/wire
read region before calling unchanged vanilla `hasNeighborSignal`. An
unavailable power ring defers both pulling and conversion; it is not assumed
unpowered. Readiness is transient, invalidated by native neighbor/lattice
changes and retried while unavailable. This guard covers the device's own
queries; tests use captured raw edge states to separate them from vanilla
placement's unconditional neighbor notifications. Snapshot rollback of a
native output transfer restores only the two paid reservoir values, retaining
original unsaved count, topology, interval and cost.

Filter cleanup stages a native captured block change and uses the real Forge
place event with original capture=false callback semantics. Its pending receipt
also protects the provisional clean block. It revalidates the exact physical held item, count, NBT, player
permissions and installed target after external callbacks. Cancellation
restores only the still-owned installed target; foreign replacement blocks
and the player's replacement hand survive. Reentrant cleanup cannot pay twice.
This modern transaction protects paid resources; it is not a new player GUI.

`CondenserDropData` owns unsupported-lattice and filter-crystal outputs before
external entity callbacks. Each physical item has a monotonic native
`onAddedToWorld` acceptance observer. Canceled joins remain serialized for
retry; an accepted, then consumed item is excluded from live and reentrant
save images even if an outer callback subsequently cancels. Retry is
loaded-only and guarded against reentry. Original media bytes are reused.

Old catalogue condensers originally had no block entity. A loaded section scan
finds these absent tiles, as well as exact empty `catalog_visual` anchors,
through the shared loaded-chunk END queue (64chunks/tick,200tick readiness bound).
An arbitrary foreign BE/subclass is preserved. Existing native
paid tiles retain identity and both reservoirs. No block is broken or
recreated, so migration cannot create essence, pollution or loot.

## Validation contract

Core GameTests cover native registries/faces/transport boundaries; one, chained,
dirty and dense topology; the exact squared74 edge; dual bases; original source
order, mixed physical aspects, fifth-tick draws and independent capacities;
conversion payment/fractional Flux/output bounds; redstone/count cadence;
actual Tube/Buffer/Transfuser integration; saved/reentrant resource ownership;
filter payment and canceled/foreign/detached/reentrant Forge callbacks;
native unsupported survival/creative BlockItem placement, connected models,
legacy migration and registered real world ticking.

The raw knowledge, recipe precursors, test arenas, NBT reservoir quantities,
physical source quantities, aura balances and deterministic RNG seeds used by
isolated tests are explicitly fixtures. Fresh complete native server evidence
and actual reviewed hidden-client scenes must be recorded separately by root
before any successful QA/build claim. No developer status labels or reference
catalogue are exposed in normal gameplay.

## Фактически завершённый QA0.34

condenser-034-server7 1369/1369 и condenser-034-client514/14, оба nativeexit0. Два проверяющих
просмотрели все14полных1600×1000PNG и оба листа. Финальные fingerprints,
реальные наблюдения, fixtures/ранние failures/ограничения сохранены в
[VALIDATION.md](../../VALIDATION.md) и
[artifact-report0.34](../../validation/artifact-report-0.34.json).
