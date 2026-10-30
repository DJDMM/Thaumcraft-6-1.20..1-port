# Remaining devices and dormant altar progression — implementation audit

Status: source implementation, pending native 0.42 execution and visual review.
This document does not establish a passed build, complete survival access or an
equivalence claim. The accepted 0.41 evidence is preserved separately.

Authority: released Thaumcraft 6.1.BETA26 JAR SHA256
`9425f8643581b27ff8845b087c8bc6fc10425a32942f1a3f0e265ce6b38f7b5f`;
the pinned recovered source is commit
`954022bb777b7546281fb36df8522f0ba6b43f81`. Relevant released classes are
`TileArcaneWorkbench`, `BlockArcaneWorkbenchCharger`, `TileSpa`, `TileWaterJug`,
`TilePotionSprayer`, `BlockPavingStone`, `TileBarrierStone`, `BlockBarrier`,
`ConfigRecipes`, `ConfigResearch`, `ConfigAspects`, `DustTriggerMultiblock`,
`FXEssentiaStream`, and `FXVent2`. The local released-bytecode transcript is
`work/late-devices-released-bytecode-042.txt` in the parent workspace.

## Actual device consumers

* Workbench Charger: the workbench sums **nine separately truncated integer
  chunk values** when its top block is a charger. Its ordered iterative debit
  uses `max(1,cost/9)` and integer credit per individual drain, preserving the
  released fractional-waste quirk. A failed modern atomic craft refunds all
  partial chunk debits. The charger is supported by the actual workbench or
  Focal Manipulator, delegates their menu and drops on loss of support.
  Focal Manipulator's existing expanded-aura implementation is retained.
* Arcane Spa: one real salt slot, no item slots exposed upward; every40 server
  ticks including its initial counter0 opportunity, disabled by redstone.
  The5000mB tank accepts fluids. Mixing needs1000mB actual water and one bath
  salt; ordinary mode needs1000mB of a placeable fluid. Sources fill above the
  device, then the original ordered5×5 supported, replaceable, source-adjacent
  cells. The original mix toggle uses the native server-owned menu button1.
* Everfull Urn:1000mB water tank; every5ticks a capped.1Vis drain generates up to
 100mB, after distributing existing water. Native UP fluid handlers receive up
  to25mB per accepted transfer, cauldrons333mB per level, bottles333mB. The
  original ordered75-position coordinate pattern and its increasing zone
  identifiers are retained. The released writer's empty `handlers` list is
  retained; cached targets are rediscovered after reload. Like the released
  capability, UP exposes the real tank and can accept water. No synthetic
  Botania apothecary interface is registered when that optional mod is absent.
* Potion Sprayer: one retained potion specimen; water/awkward/empty/mundane/
  thick potions are rejected. Original potion aspect recipes recursively use
  native brewing reagents, add Alkimia3 per reagent, remove `floor(amount*.66)`
  and cull to10 aspect types using original depth weights. Unknown custom
  brewing routes receive the released Praecantatio5/Alkimia5 fallback. The
  ordered5tick128suction loop draws one physical unit from nonfront faces on
  the device or its upper neighbour level. Completed recipes supply up to8
  charges; a powered edge spends one charge and applies the original full
  potion effects in the3×3×3 box centred two blocks in front. Holding power
  does not repeatedly dose. The original fifth-tick early return when empty
  or full and the unsaved activation latch are retained.
* Travel Paving Stone: actual living entities receive40ticks SpeedII and
  JumpI. Barrier paving creates two invisible fence cells, blocks living
  nonplayers unless a player occurs in their recursive passenger tree, and
  permits passage while powered. It retains the original airborne push,
  pathfinding rule and original rune atlas program. Removing support removes
  both fence cells through native neighbour callbacks.

All physical inventories have sided Forge capabilities, invalidation/revival,
native dropped contents, saved NBT and native tracking updates. Loaded-only
world access and snapshot guards are explicit modern integration choices.
The Spa consumes a dose after a successful block write, preventing a failed
modern placement from charging resources. No extra debug labels are added.

## Progression and paid recipes

The canonical set now contains148 unique original entries in source. This is
not a test result or a guarantee every original dormant resource has natural
survival acquisition. The eleven entity scan families include actual native
Pech, Brainy/Giant Brainy zombies and Eldritch Crab. Original `!ORBLOCK1` scans
Ancient Stone/Ancient Tile, `!ORBLOCK2` Eldritch Tile, and `!ORBLOCK3` Ancient
Glyphed Stone; their item counterparts also match as in ScanBlock.

* WORKBENCHCHARGER:200Vis, Aer2/Ordo2.
* ARCANESPA:50Vis, Aqua1.
* POTIONSPRAYER:75Vis, Aqua1/Ignis1.
* PAVINGSTONES:4barrier stones50Vis Ignis1/Ordo1, or4travel stones50Vis
  Aer1/Terra1, using the original identical2×2 Arcane Brick input.
* EVERFULLURN: flowerpot +Aqua30/Fabrico10/Terra10.
* LIQUIDDEATH: bucket +Mortuus100/Alkimia20/Perditio50.
* BOTTLETAINT: exact10Vitium filled phial +Vitium30/Aqua30. Extra unrelated
  root NBT is allowed; mismatched nested `Aspects` is rejected.
* METALPURIFICATION: corresponding ore +Metallum5/Ordo5. Original optional
  copper/tin/silver/lead paths additionally require a populated ingot tag;
  the guard is evaluated after tag binding rather than depending on reload
  listener order. The four registered original furnace cluster recipes yield
  two iron/gold/quicksilver/quartz outputs,1XP and200ticks through Forge's
  actual object-result SimpleCookingSerializer support.

Study costs, required craft/items and stage Warp are read from the pinned
original research JSON. Existing bare-key Crucible availability remains
available at known/started research, consistent with the original recipe
predicate; this does not conclude book study automatically.

The shipped PAVINGSTONES `required_craft: thaumcraft:arcane_stone` is an
invalid **item** descriptor, even though that name also identifies a recipe.
Released `ResearchManager.parseJSONtoItemStack` performs a direct item-registry
lookup, `parseJsonOreList` discards its empty result, and `ConfigBlocks` registers
only `stone_arcane`. The port therefore ignores that declared craft row in both
payment and book display, preserving the actual16raw Observation/Infusion cost
and both5XP stage grants. It does not invent an alias or a craft fact. The native
study assertion retains the original declared row while checking its ignored
behavior, exact knowledge payment and replay guard. Released bytecode evidence
is retained in parent `work/late-devices-native-fix-released-bytecode-042.txt`.

## Ancient/Eldritch altars are deliberately dormant in normal world generation

BETA26 registers INFUSIONANCIENT/INFUSIONELDRITCH, both150Vis pedestal recipes
without crystals, their Salis blueprints and stone scans. These source paths
are now connected to the existing native matrix/pedestal/stability machinery.
The latent `stone_eldritch` typo is corrected at both formation entry points to
the actually registered `stone_eldritch_tile`. The original blueprint still
requires Ancient pedestal charge2 or Eldritch pedestal charge1. The ritual
replaces only the eight stones, preserves central contents and uses the
existing atomic Forge protection callback.

No TC4 dungeon, portal or Outer Lands generation is invented to make the
original dormant materials naturally available. The two ritual tests declare
raw initial charged pedestals/materials as fixtures, and cannot establish
survival accessibility.

## Presentation and pending verification

### Sprayer server fixture correction after server2

The first readiness repair was necessary but not sufficient: server2's
physical target was query-visible, yet the combined charge/heal assertion
still failed. The fixture passed `sprayer.getBlockState()` to manual ticks.
Mapped Forge47.4.10 `LevelChunk.setBlockState` calls `onPlace` at bytecode411,
then creates/updates the block entity at421–467. The initial default
`ENABLED=false` therefore invokes a nested unpowered update to `true`;
the outer call subsequently restores the block entity's cached initial
`false`, while the actual chunk state remains `true`. Manual fixture ticks
using that cache falsely set `activated=true` before the real power edge.

Native `LevelChunk$BoundTickingBlockEntity.tick` instead reads the chunk state
at77 and passes that live state to the ticker at118. The fixture now follows
the same callback argument for every manual Sprayer tick and checks power
against the actual level state. Physical aspects, exact one-charge payment,
health4→8, retained potion, held-power rejection and reload assertions remain.
Separate power/charge/heal diagnostics are retained. Production Sprayer code
is unchanged: only the passed state controls `ENABLED`; its cached `FACING`
is identical across this power-only transition, and menu/capability validity
does not read `ENABLED`. No active flag, clock, RNG or potion value is reset.
This source correction still requires a fresh native run before acceptance.

`gui_spa.png` and `gui_potion_sprayer.png` are copied byte-for-byte from the
released JAR. Screen coordinates, fluid strip/mix icons, potion charge scroll
and ten aspect bars follow the original GUI. Urn water uses the original
Essentia texture,8-sided CoreGLE geometry, stream timing/merging/movement and
droplet atlas; Sprayer uses a local original FXVent2 implementation, including
its different growth/velocity/opacity from the existing FXVent program.
These are source-level ports; actual visibility/compositing is pending native
screenshots and independent full-frame review.

Four WorkbenchChargerGameTests and thirteen LateDevicesGameTests cover actual
paid research/menu crafts/Crucible outputs, shortage/replay guards, saved
state, existing catalogue-chunk migration without replacing paid native contents,
physical suction/fluids, redstone edges, collision and both dormant
rituals. Raw prerequisites, aura, salts, fluid, essentia, actors and initial
charged altar states are explicit fixtures. No native pass is predeclared.

The first native0.42 run exposed a2×2recipe fixture written as consecutive
3×3slots, and a Sprayer actor placed while its remote entity section was still
HIDDEN. The recipe fixture now maps the recipe's width into the actual workbench
grid. The Sprayer fixture owns a temporary region ticket, waits for actual native
entity loading/ticking, and requires the Cow to be visible in the released spray
box before the power edge. Its one-charge payment, full4health healing and held
power guard are unchanged. These fixture repairs do not alter production device
cadence, effect strength, aura or RNG; their fresh native execution is pending.

`LateDevicesClientSmokeTest` is an opt-in17capture driver selected by
`thaumcraft.lateDevicesSmokeTest`; its isolated launcher profile belongs to root
and uses `run/late-devices-smoke`. Source presence does not establish execution
or visual acceptance. It creates a new owned Survival world and declares a raw
Charger/workbench, completed ARCANESPA prerequisite, initial400Vis loaded chunks
with an empty bench centre, initial moon4, recipe ingredients/crystal, water,
salts, retained potion, typed source jars/tubes and NoAI Cow actors as fixtures.
There are no active aura, charge, source amount, clock, particle age or RNG resets.

The scenes cover the actual nine-integer-chunk availability and delegated menu,
a native50Vis/Aqua1 paid Spa result, its Survival placement, original water/salt
GUI, both native mix-button directions and a real paid bath placement after
native redstone harvest. An initially empty Urn fills through real aura debits,
then supplies a powered Spa's native UP fluid capability in physical25mB units;
its original eight-sided water stream is captured in the actual world. A native
Sprayer menu accepts the retained Healing potion, ordinary native tubes pull its
exact recipe from initially populated physical jars, and a native lever edge
pays one charge to heal a Cow4→8. A subsequent60worldtick held-power guard keeps
that health, zero charges and the potion specimen. The final scenes use real
Cow physics on Travel stones and actual client movement through the unpowered
Barrier, with native mob/player collision predicates and powered blue runes.

Transient captures require the same actual AFTER_PARTICLES world frame plus
read-only production draw vertices, nontrivial alpha, camera projection and
unobstructed rays. Water stream internals are inspected without modification;
Vent2/Rune quad world corners and actual RGBA are logged. Powered-rune readiness
requires new blue colours so lingering unpowered runes cannot satisfy it.
`THAUMCRAFT_LATE_DEVICES_RENDER_AUDIT_OK` reports those native capture gates;
the strict final marker is `THAUMCRAFT_LATE_DEVICES_CLIENT_SMOKE_OK:17 captures`
(the emitted marker includes a space after the colon). Both remain contingent
on fresh execution, and every full-frame PNG still requires actual independent
pixel review. This fixture does not prove full survival acquisition, multiplayer,
reload, audio or every viewing angle.

The Urn block event retains the original client tcount5 callback on the next
real tile tick. Stream lookup and active trails are separate, so a replaced
lookup does not prematurely remove the previous visible trail. Eight-sided
CoreGLE colour samples use the original next-index-minus-one front colour
even when tangent guides skip degenerate adjacent points.

Original ModConfig's optional Copper/Tin/Silver/Lead cluster smelting is also
registered through a native SmeltingRecipe subclass. Its output is two items
from the first actual populated ingot tag, with original1XP/200ticks. Empty
addon tags produce no output and do not match; no missing addon registry ID
is fabricated. Selection uses modern native tag order instead of1.12 ore
dictionary registration order. Native Copper therefore has a working furnace
consumer. A dedicated test uses the actual vanilla FurnaceBlockEntity,
physical clusters/coal/output, missing-tag guards and unknown-metal rejection.

The opt-in client driver now waits before its first world capture for60
natural world ticks and60 client ticks, the received arena floor/Bench chunk,
three actual compiled floor/Bench/corner meshes and native skylight readiness.
This prevents an early empty-world frame. Aura diffusion, lunar time, source
amounts and active device clocks continue normally throughout the wait; no
post-start aura or clock reset was added. The exact paid craft checks remain
unchanged. This capture gate still requires root's fresh native run and two
actual image reviews; no pass follows from source inspection alone.

The first actual hidden LateDevices run reached eight captures, then waited
at Sprayer scene8 phase1: its declared Survival camera feetY113 atz1.5 stood
over the original arena floorY111, beyond the raised Sprayer platform'sz-6..-1.
Native gravity correctly settled the player atfeetY112. The driver now declares
that actual ground position/pitch and checks its exact stone-brick support
without moving the player again during the wait. The later post-passage Barrier
camera likewise uses its actual groundY112; raised Sprayer/Barrier starting
positions retainY113 and have explicit read-only support assertions. Position
readiness is bounded at160 natural world ticks, with actual client/server
position, ground flag, expected support and current support diagnostics every20
ticks. No flight, gravity bypass or active position reset was introduced.

Both reviewers saw the original Urn transfer frame as only a small coloured
fragment near the mouth, despite its560 emitted vertices and eight clear
samples. That is not proof of the full water path. The released FXEssentiaStream
JAR bytecode retains initial length20, lifetime distance*21, five-tick merging,
.985 damping and .05 motion clamps; those production values remain unchanged.
The opt-in capture now waits for a native stream aged at least20 ticks with
at least20 actual path points. Its emitted original geometry must provide
unobstructed, in-view vertices in all four10%-to90% path-progress bands toward
the actual recipient, at least six distinct original path indices, a nontrivial
native radius and at least80 pixels of projected path extent. The same native
render-frame receipt remains required. Every20 ticks a read-only path summary
records age/lifetime, points, projected visible vertices, progress bands and
screen bounds; an absent qualifying path fails within600 world ticks. These
are observation gates, not injected vertices or changed particle physics.

The interrupted run's eight full PNGs and contact sheet were actually viewed
by root and an independent reviewer; Sprayer/Travel/Barrier remain unexecuted
there. Its partial evidence is rejected for full-route acceptance. All camera
and readiness corrections above still await a fresh native run and actual
pixel review; no0.42 release acceptance is asserted.

Client2 reached thirteen captures with a fully visible Urn arc and native
one-charge/full-healing Sprayer, then stalled at Travel. The generic comparison
Cow fixture had NoAI=true and started atfeet113 above the15/16-high Paver top
112.9375. Modern Entity.isControlledByLocalInstance delegates to isEffectiveAi;
Mob's NoAI flag makes that false, and LivingEntity.travel skips its physics.
The Travel fixture now creates its own ordinary Cow with default AI before
its first native tick, retaining the once-only initial .08 horizontal impulse.
No active motion, position, effects or stepOn callback is set. Travel waits at
most160 world ticks and logs actual client/server Cow health, position, ground,
NoAI, velocity, foot support and effects every20 ticks. Barrier wait stages
also have finite bounds and read-only actor/player/fence/power diagnostics.

The blue-green arc patches in client2 were a real0.42 vertex-packing defect,
not original texture colour. The16x16 essentia.png is grayscale and exactly
matches the released JAR (SHA2569cf44ffa1226a9aacaa19e3404fb1dc65e91d191024cd6d4f8d5b00d9b020396).
Original tint0x286ff6 has blue246; its unchanged brightness pulse can reach1.1,
making a float blue equivalent to270. Modern VertexConsumer.color(float)
multiplies by255 and casts to int; BufferVertexConsumer stores an i2b byte,
wrapping270 to14 and producing green. Original fixed-function GL instead
clamps colour components before interpolation or colour-buffer writes:
[Khronos glColor reference](https://registry.khronos.org/OpenGL-Refpages/gl2.1/xhtml/glColor.xml).
WaterStreamColor now clamps that float boundary before unsigned-byte packing
only in WaterStreams. Texture, UV, CoreGLE geometry, brightness equation,
motion, lifetime, source payments and other accepted0.41 renderers are unchanged.
Two boundary regressions expect0x2c7aff at1.1, white saturation, and unchanged
unit/.9/.5 in-range values. Fresh native pixels and a full server run remain
required; these corrections do not by themselves constitute acceptance.

The Travel capture's client guard also follows native1.20 synchronization:
ordinary tracking clients receive the Cow's attributes and movement, not its
full active-effect map (LivingEntity.onEffectAdded only sends that effect
packet to passenger players; ServerPlayer separately receives its own effects).
The exact server40-tick SpeedII/JumpI check remains authoritative. Client capture
now requires the actual Cow's native movement over a Travel support and the
received vanilla Speed attribute modifier, using its original UUID, operation
and amplifier-one amount. It does not inject effect packets or infer JumpI
from a client map that vanilla never broadcasts. The other hasEffect checks
in this driver are on the actual server thread and remain unchanged.
