# Arcane Pattern Crafter — released BETA26 behavior and native ownership

Scope: `pattern_crafter`, `TilePatternCrafter`, `BlockPatternCrafter` in
Thaumcraft **6.1.BETA26**, adapted to Forge 1.20.1. TC4/TC5 mechanics are not
evidence. This document records the implemented contract; actual executed QA
belongs in the final version report and `VALIDATION.md`.

## Primary evidence

Released JAR SHA256:
`9425f8643581b27ff8845b087c8bc6fc10425a32942f1a3f0e265ce6b38f7b5f`.
Readable reference commit:
`954022bb777b7546281fb36df8522f0ba6b43f81`.
The following `javap -c -p` files were generated from that released JAR, not
compiled from the recovered source:

| Local evidence in `work` | SHA256 |
| --- | --- |
| `pattern-036-tile-javap.txt` | `09e1c67bd796fb39e867b5cddf46b328fd7853efc041c813b8c8ac38ea72b746` |
| `pattern-036-block-javap.txt` | `1c5342a760cb37dbf5b2de839c9b287fe1abf4aee384772c910b2c30fbccd37f` |
| `pattern-036-inventory-javap.txt` | `0560d37d7c3d8bd761609534fdc8bb463cf215ce59c8f59d4709d28f111b59f9` |
| `pattern-036-baseblock-javap.txt` | `57f2a2bfc12e7303d02e33055bd5689c5148a9e2197ab1f80ada1c4ebe2de02b` |

In `update`, offsets151–175 are the postincrement/mod20/enabled gate,
178–207 the five-vis refill, 213–306 the amount switch, 307–342 the
above/DOWN and below/UP endpoints, 395–458 extraction/recipe/power/result
preflight, 477–518 remainder capacity checks, and529–636 actual output,
input consumption, event1 and one-credit debit. At509 `IFNE518` accepts
an **empty** insertion leftover; it agrees with the recovered Java.
`craft` offsets18–392 establish all ten cell arrangements;392–435 use a real
ordinary crafting recipe and a second native remaining-items lookup.

The horizontal placement override reads the player's horizontal direction
directly. It does not use the usual device opposite-facing/sneak rule.
The discarded `withProperty` call in the base constructor does **not** prove a
false boolean default: the original 1.12.2 `PropertyBool` allowed-value order
is TRUE,FALSE, and `BlockStateContainer` chooses its first value. Independent
primary vanilla disassembly is retained in
`work/dioptra-legacy-state-primary.javap.txt`. Native default is NORTH/enabled;
real placement/neighbour updates then apply inverse redstone.

The mapped Forge native setter disassemblies in
`pattern-036-native-setters-javap.txt`
(`ea7c312a8cfd9db1b616801b010d65691a753c10bd9fc29155fc301b9a746b42`)
and `pattern-036-native-other-setters-javap.txt`
(`6c504c4c5cc81dff0f5b7dbea2daf209d64d0a07578e52ffe330129afe9a980a`)
establish the supported native inventory mutation boundary. After loot has
been unpacked by the observable snapshot, exact native setters install the
stack before their comparator/recipe callbacks. Furnace slot0 can consult
recipes **after** assignment; payment ownership must already have moved.

## Original crafting and power

There is no inventory GUI or programmable nine-slot recipe. The small physical
selector cycles through ten fixed dies. Every selected cell receives a separate
one-item copy of one candidate exemplar, preserving its item/damage/NBT.
The original 3×3 cells, numbered left-to-right and top-to-bottom, are:

| Type | Cells | Physical input amount |
| --- | --- | --- |
| 0 | 0,1,2,3,4,5,6,7,8 | 9 |
| 1 | 0 | 1 |
| 2 | 0,1 | 2 |
| 3 | 0,3 | 2 |
| 4 | 0,1,3,4 | 4 |
| 5 | 0,1,2 | 3 |
| 6 | 0,3,6 | 3 |
| 7 | 0,1,2,3,4,5 | 6 |
| 8 | 0,1,3,4,6,7 | 6 |
| 9 | 0,1,2,3,5,6,7,8 | 8 |

The constructor chooses a random local count0..19. Server updates increment
that count even while disabled. Only `count++ %20 ==0` and an enabled block
attempt work. Refill occurs **before** checking either inventory: if power<=0,
drain at most five real vis and add the exact returned float. A successful
operation spends one internal credit, not five. Idle positive credit is retained.
A positive fractional remainder below1 never refills and cannot craft: the
released fractional stall is retained, not rounded or topped up. Powered devices
neither refill nor craft.

Input is the inventory immediately above, accessed through DOWN; output is
immediately below through UP. Candidate slots are visited in physical inventory
order. The first candidate which can pay the full amount, matches an ordinary
native crafting recipe, has power>=1 and fits output/remainders is crafted;
then the loop breaks. Inputs can aggregate across eligible physical slots.
Arcane recipes are not ordinary crafting recipes and are not invented here.
The second native remaining-items lookup is preserved. Only remainders of
occupied grid cells are retained; these go below with the result, not above.
There is no native `onCraftedBy`, player research award, fabricated XP or
bonemeal callback for automatic ordinary crafting.

`BASEORE` uses strict item/damage/NBT when no named ore match exists.
Its ore branch compares a candidate with entries of the prime's named ore
dictionary groups, not the prime's NBT. Thus a tagged gold exemplar may consume
plain gold ore-entry candidates; differently tagged candidates cannot substitute
for a plain ore entry. This released NBT asymmetry is retained. The modern
named-entry mapping is deliberately bounded: `minecraft:planks`,
`minecraft:logs`, and Forge **leaf** tags under `ingots/`, `nuggets/`,
`storage_blocks/`, `ores/`, `gems/`, `dusts/`, `rods/`. Broad `forge:ingots`
must never make iron pay a gold recipe. There is no surviving metadata32767
wildcard on ordinary modern inventory stacks; metadata variants use their
actual registered item IDs.

## Physical interaction and rendering state

The block is nonopaque/nonfull for face support, with the original full-block
outline/collision body and metal sound. Placement faces the player horizontally,
including while sneaking. Redstone inversely updates the `enabled` state.
Parent BlockTCTile gives hardness2 and unconditionally allows harvesting;
copying the modern iron block's required-tool flag would incorrectly prevent
bare-hand drops. Its old setResistance20 stores20×3 and the original explosion
getter divides by5, giving modern effective resistance12. Redstone conductor,
suffocation and view blocking are explicitly false, preserving original
nonfull/opaque behavior despite the full bounding box.
Selector bounds are original local coordinates:

| Facing | Selector AABB min → max |
| --- | --- |
| WEST/default | (.75,.125,.375) → (.875,.375,.625) |
| EAST | (.125,.125,.375) → (.25,.375,.625) |
| NORTH | (.375,.125,.75) → (.625,.375,.875) |
| SOUTH | (.375,.125,.125) → (.625,.375,.25) |

The real player ray selects that knob; arbitrary client coordinates do not set
a mode. A successful click cycles0..9 and plays the original key sound,
volume.5/pitch1. No chat explanation or developer menu appears.

Event1 starts ten client rotation ticks. Each tick decrements rotTicks,
plays clack at volume.2/pitch1.7 when `rotTicks % floor(max(1,rp)) ==0`,
then increments rp; idle rp decays by.8; rot increases by rp. These are discrete
source values, not an invented server-driven angle. The type sync tag contains
only byte `type`; save adds float `power`. The original local count and visual
rot/rp/ticks are not persisted. An additional hidden owner UUID binds paid
modern output ownership. Malformed modes/nonfinite/negative loaded credit are
bounded rather than used as free active state.

## Explicit modern conservation and compatibility boundaries

The original performs independent output/remainder simulations, inserts before
extraction, and ignores real insertion leftovers. The port uses a **cumulative**
shadow reservation, and debits the exact already-observed physical input stacks
before recording a detached paid result. This prevents one free destination slot
being promised both the result and a bucket, and prevents callbacks producing
unpaid outputs. Recipe matching, assembly, remaining-items, side admission and
loot initialization run before payment; attached identities, all physical
counts/NBT, mode, state and endpoints must still match after those callbacks.
Reentrant ticking is excluded. Foreign changes are retained, not rolled back.

Supported transactional endpoints are exact native chest/trapped chest
(including both real double-chest halves), barrel, hopper, dispenser/dropper,
shulker, the three vanilla furnace classes, brewing stand and this port's
Hungry Chest. Native exposed face slots and insertion/extraction admission are
honored. Duplicate nonempty physical ItemStack identities across native slots
are rejected rather than charged twice through an aliased mutable object.
Unknown inventory subclasses, opaque addon capabilities and inventory
entities are not claimed as paid transactional support. The release's generic
handler discovery can reach those; this is an explicit modern compatibility
limit. No chunk is forced to discover an endpoint or a double-chest half.

The paid result owner is persistent SavedData, not a player-accessible buffer.
Each accepted native insertion retires that amount before the exact setter's
assignment/callback boundary. It is not reinserted after callback removal or a
save. Undelivered portions remain owned and block another operation; they retry
against available native capacity. Removing the owner changes those portions
into physical drops. A canceled native ItemEntity join remains pending; accepted
joins retire ownership exactly once, including reentrant saves/tick attempts.
Ownership ends after a native recipient accepts an item; that recipient's later
ordinary destruction/loot rules remain its own behavior.

Placement retains actual vanilla geometry/NBT/sound and actual Forge placement
callbacks. Its unpaid provisional block cannot tick or drop paid output. The
real hand remains intact through the callback and is debited once only after
the exact hand, count, NBT, target identity and permissions survive. Canceled
rollback restores only this action's still-owned snapshots, preserving foreign
hand/block/inventory changes and separately captured transactions.

Loaded-chunk migration scans real section palettes because the previous
catalogue block had no tile. It installs a missing native tile or replaces an
empty catalogue anchor; existing paid native/foreign tiles are preserved.

## Validation scope

New suites contain **30** common checks and **6** placement/ownership checks.
They include nine real vanilla recipes, all ten cell layouts, real special
recipe callbacks/remainders, native sided inventories/double chests, exact
physical input/output/payment, fractional credit, idle refill, redstone,
native actual world ticks, guarded placement and native canceled/reentrant
physical joins with SavedData roundtrips. Raw credit, temporary special recipes,
materials, terrain and fixed containers are explicit test fixtures. They do not
prove a full survival playthrough, addon compatibility or multiplayer sessions.
No QA result is asserted until the root records the actual final server/client
logs and source fingerprints.

## Фактически завершённый QA0.36

`pattern-036-server4` прошёл1488/1488; `pattern-036-client1`23/23, оба nativeexit0.
Два проверяющих просмотрели все23полных1600×1000PNG и оба листа.
Настоящие3scan/2study/2bench/100vis5crystals/96Theory16Observation20XP,
nativeDioptraVisFlux,10dies/10crafts/24outputs, 80тиков паузы,
продолжение и физическая разборка связаны с финальными logs/SHA.
Фикстуры, source quirks, современное loaded-only/владение, ранние попытки
и пределы доказательства сохранены в [VALIDATION.md](../../VALIDATION.md)
и [artifact-report0.36](../../validation/artifact-report-0.36.json).
