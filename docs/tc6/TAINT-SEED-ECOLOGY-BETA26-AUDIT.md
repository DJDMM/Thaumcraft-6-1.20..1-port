# Taint Seed, Prime and terrain ecology: BETA26 contract

Audited on 2026-10-11 against the pinned Thaumcraft **6.1.BETA26 / 1.12.2**
source checkout `954022bb777b7546281fb36df8522f0ba6b43f81` and released JAR
SHA256 `9425f8643581b27ff8845b087c8bc6fc10425a32942f1a3f0e265ce6b38f7b5f`.
This is an original behavior contract and review checklist, not proof that a
new runtime or test passes. The released JAR takes precedence. No TC4 tainted
biome, node, Outer Lands maze or portal progression belongs to this scope.

`javap -c -p` directly confirmed `TaintHelper`, `EntityTaintSeed` and
`AuraHandler` in the pinned JAR. The bytecode transcript is kept as ignored QA
material in `validation/taint-029-original-bytecode.txt`. Source paths below
are relative to `src/main/java/thaumcraft` in the pinned reference checkout.

## Sources

- `common/blocks/world/taint/{TaintHelper,BlockTaint,BlockTaintFibre,BlockTaintLog,BlockTaintFeature,BlockFluxGoo}.java`.
- `common/entities/monster/tainted/{EntityTaintSeed,EntityTaintSeedPrime,EntityTaintacleSmall,EntityTaintacle}.java`.
- `common/entities/{EntityFluxRift,EntityFallingTaint}.java`.
- `common/world/aura/AuraHandler.java`, `common/config/{ModConfig,ConfigEntities,ConfigBlocks,ConfigItems,ConfigResearch}.java`.
- `common/lib/utils/{EntityUtils,BlockUtils,Utils}.java`, `api/potions/PotionFluxTaint.java`.
- `client/renderers/entity/mob/RenderTaintSeed.java`,
  `client/renderers/models/entity/{ModelTaintSeed,ModelRendererTaintSeed}.java`.

## Seed presence and range

The original maintains an in-memory list of seed BlockPos per dimension.
It is not a saved taint biome or a persistent terrain region. A seed refreshes
its position after the base entity tick on its first server update and every
1200 entity ticks. Refresh is remove-current-position then add-current-position;
the historical list can retain an old position if a seed falls. Death removes
the current position. A safe loaded-only lifecycle index is a modern adaptation;
it must not force-load a seed or use a dead/catalogue entity as a live source.

Near means BlockPos squared distance **<= area * area**, default area **32**.
After finding the first in-range stored position, the original validates an
`EntityTaintSeed` AABB intersection within **1** of that block's centre. If
missing, it removes that position and **returns false immediately**, even if
a later seed might be valid. Prime is a subclass and counts as a seed.
Edge means strictly **(area*.8)^2 < distanceSquared < area^2**; the edge helper
does not itself validate stale entries. The config comment says flux modifies
the area, but BETA26's actual radius is constant. Flux modifies probability.

`EntityUtils.getEntitiesInRange(BlockPos, ..., range)` uses the point at
`pos + (.5,.5,.5)` and an AABB grown by range, with **no spherical filter**.
Use this cube contract for seed spacing and nearby infection. It differs from
the spherical BlockPos distances used by near/edge terrain checks.

## Seed and Prime runtime

| Property | Seed | Prime |
| --- | --- | --- |
| Width / height | 1.5 / 1.25 | 2 / 2 |
| Max health | 75 | 150 |
| Attack damage | 4 | 7 |
| Movement speed | 0 | 0 |
| XP | 8 | 12 |
| `getArea()` | 1 | 2 |
| Infection cube range | 4 | 8 |
| Infection amplifier | 0 | 1 |
| Spread sample offsets X/Z | -3..3 inclusive | -6..6 inclusive |
| Spread sample offsets Y | -1..1 inclusive | -2..2 inclusive |

Both use ordinary stationary melee AI, retaliation and a visible-player target.
They cannot attack `ITaintedMob` classes and consider tainted mobs on the same
team. Seed cannot be pushed, is collidable, cannot drown and does not despawn.
Its movement override rejects X/Z and positive Y but **allows downward Y**.
Do not set no-gravity or cancel all movement: an airborne rift seed falls.

Spawn eligibility checks non-Peaceful difficulty, no liquid in its AABB, no
entity collision, and **no Seed/Prime** intersecting the BlockPos-centred cube
grown `areaConfig*.8` (25.6 at default). **There is no block collision or
solid-floor requirement, no light check and no biome requirement.** BETA26
`isNotColliding` is only containsAnyLiquid + checkNoEntityCollision. Modern
`Level.noCollision` would add an incorrect block-shape rejection.

Persistent integer key is **`boost`**. Positive boost selects saturation 1,
decrements once per update, and spreads every update. Without boost, saturation
is checked every 20 ticks. **Saturation is flux/base, not flux/(vis+flux) and
not a clamped percentage.** Missing aura returns 0. Original base=0 retains
float infinity/NaN behavior; any invalid-data guard must be disclosed.
At saturation <=0 the seed takes **0.5 starvation** and adds **0.1 flux** at
its own position. Thus zero flux does not imply repeated starvation forever:
its own pollution can make the next sample positive. Otherwise it calls
spreadFibres at its random offset **with ignore=true**, bypassing the random
spread gate and wussMode, but retaining seed-range/target conversion gates.

Every 20 ticks it applies FluxTaint **100 ticks**, amplifier area-1, ambient
false/visible true to **every LivingEntity except itself** in the point cube.
There is no line-of-sight, enemy, player-only, undead or tainted filter in this
infection loop. FluxTaint itself heals tainted classes before checking undead
and leaves ordinary undead unharmed. Keep these distinct responsibilities.

Every 20 ticks, a visible attack target with distanceSquared **< area*256**
can receive a Tiny tentacle if its current or below block has taint material,
or its biome is ELDRITCH. This is <256 normal / <512 Prime, not <256*area².
Tiny spawns at target X/Z plus independent `nextFloat()-nextFloat`, target Y,
without Tiny's spawn checks. In ELDRITCH, target air plus any sturdy neighbour
also becomes fibre. ELDRITCH is dormant BETA26 Outer Lands; this branch does
not establish a working dimension or portal chain.

Seed drops one exact **Vitium crystal** (Aspect.FLUX=vitium). Prime drops that
one plus two independent nextBoolean crystals, yielding **1 / 2 / 3** with
probabilities 1/4, 1/2, 1/4. No fortune/looting multiplication belongs to this
method. Melee emits entity status **16**, tentacle sound, and attack animation
0.5 on client; animation multiplies .75 per tick and zeros below .001.

Tiny inherits stationary Taintacle behavior, has size .22 x1, HP5, attack2,
XP0, no drops and never qualifies for ordinary spawn. It inherits 20-tick
**direct damageEntity(STARVE,1)** when neither its block nor below is taint;
it does not recursively spawn more tiny tentacles. Transient lifetime starts
200, is not original saved NBT, and `if (lifetime-- <= 0)` deals direct magic10
on the **201st** update. Do not confuse direct damage with hurt invulnerability.

## Rift event 1

The original weighted slot is retained; do not reroll a rejected Prime.
Construct Prime at independently sampled coordinates
`(int)(riftPos + nextGaussian()*5)` for each axis, then **+.5 only on X/Z**;
this is Java truncation toward zero, not floor. Y has no +.5. Choose yaw0..359.
Only if Prime eligibility and world spawn both succeed:

1. Set Prime `boost = riftSize`.
2. Pollute the **rift's** aura chunk by `(float)(riftSize / 2)` with integer
   division first, visible pollution true.
3. Mark event successful and remove the rift.

Failure must leave boost/payment/removal untouched. The event's shared outcome
handling remains governed by the existing rift contract. Size5 pollution is2,
not2.5. Candidate may intersect solid blocks, because Seed eligibility does
not inspect block collision. It may begin in air and must fall normally.

The legacy event success regression's Gaussian candidate lay outside its3x3
template and did not establish candidate chunk visibility or spawn eligibility.
Native0.42 server2 exposed that under-specified fixture as a rejected event;
the combined outcome/removal assertion did not identify which gate rejected it.
The regression now owns an upper vertical band, loads and tickets every chunk
in the original exclusion cube, waits for real entity visibility/ticking, and
checks the unchanged liquid/entity/seed gates before creating the event rift.
The same initial detached sample92173, exact native placement/boost/health,
integer10pollution and zero stability cost remain required. Outcome and removal
now have separate assertions. This changes fixture preparation only; original
probability, protection and payment code remain untouched, pending fresh QA.

## Fibre spread and conversion

Config defaults: spreadRate100f (allowed0..500), spreadArea32 (8..256).
Unless ignore=true, wussMode returns immediately, then the call passes only
when `nextFloat() <= rate/100f * (.001f + flux/base*2f)`. Comparison is strictly
`>` for rejection, not `>=`; no independent clamp. Random target offsets are
each -1..1. Equal source/target returns. Source must be near a live seed.
Target hardness <0 or >10 returns before any conversion.

Order and payments are part of the contract:

1. Non-leaves, non-liquid air/replaceable/flower/IPlantable target, at least
   one sturdy neighbour, and at least one **non-taint** sturdy neighbour:
   fibre + block event1 + drainFlux **.01**. Zero flux does not prohibit the
   conversion; the drain is not a required upfront cost.
2. Leaves always convert (still obey initial hardness and seed bounds).
   If `nextFloat()<.6` and touching taintLog, use taintFeature facing **away
   from the first touching log** (Direction.values order). This branch drains
   **no flux and emits no event1**. Otherwise fibre + event1 + drain .01.
3. Hemmed target and hardness **<5**: non-taint wood log -> taintLog, retaining
   axis (fallbackY), with **no .01 debit/event**; mushroom blocks/gourd/cactus/
   coral/sponge/wood material -> crust; sand/ground/grass/clay -> soil;
   rock -> rock. Crust/soil/rock all event1 + drain .01 and return.
4. If the original target was **already soil or rock**, above is air, flux>=5,
   `nextFloat() < rate/100f * .33`, and target is at a strict seed edge, try
   ordinary Seed at `(x+.5,y+1,z+.5)`. Eligibility uses the shared seed cube.
   The original drains **5** after eligibility, then calls spawn without
   checking its return; modern atomic spawn-before-debit must be identified.

Hemmed is a six-neighbour score: taint material +1; air -1; non-liquid
non-sturdy-facing neighbour -1; ordinary sturdy solid or ordinary liquid 0.
Result must be **>0**. It is not simply "one taint neighbour". The conversion
target's hardness10 taint blocks bypass the <5 part and reach branch4.

In 1.20, removed Material categories need explicit audited tags/classification.
Avoid converting every block whose destroy speed<5 into taint rock: glass,
metal, crafted devices and liquid must retain their original classification.
No new biome replacement occurs anywhere in this helper.

## Terrain blocks, decay, properties and drops

Canonical IDs are `taint_fibre`, `taint_crust`, `taint_soil`, `taint_rock`,
`taint_geyser`, `taint_feature`, `taint_log`, `flux_goo`. Existing port IDs/media
already match and should be upgraded in place, not registered twice.

- Rock/soil/crust/geyser: hardness10, resistance100, purple map, random ticks,
  GORE sound, CUTOUT, **undefined sturdy faces**, but ordinary full block
  collision. When no seed, a **1/10** random tick decays rock->porous stone,
  soil->dirt, crust/geyser->FluxGoo. Only rock calls spreadFibres. Soil does
  **not** independently spread on its tick. Walking infects non-undead Living
  at **1/250**, duration200/amplifier0. Normal drops none except rock's
  `nextInt(15)+fortune>13` Vitium crystal; explicit public SilkHarvest allows
  block silk harvesting, despite protected no-silk method quirk.
- Log: hardness3/resistance100, axe0, GORE, flammability/spread4, AXIS X/Y/Z.
  Placement uses clicked face axis; generated conversion retains input axis.
  No-seed decay is **immediate on next random tick** -> FluxGoo, otherwise
  spreadFibres. Normal log item drops metadata0, leaves support/wood true.
- Feature: hardness.1, non-full/non-opaque, facing six directions, light
  `setLightLevel(.625)` (integer9) and original packed-light override200.
  The constructor discards `withProperty(UP)`; original default is property
  base **DOWN**, notUP. Placement uses clicked face; support is behind facing
  and must be sturdy toward the feature. Without support neighbour callback
  deletes it. No seed decay1/10 -> FluxGoo; otherwise spread. On verticalY
  taintLog below, **1/100** upgrades feature to geyser. Original block removal
  spawns Crawler with .333, else pollutes1, including replacement/decay, not
  just player mining. Crawler integration cannot be claimed with NoAI entity.
- Geyser: .2 chance on random tick + player within32 + no Swarm in pointcube32
  spawns Swarm above; else if flux<2, pollutes **.25**. Without real Swarm AI
  this route is explicitly unsupported; do not spawn a catalogue substitute.
- Fibre: hardness1, GORE, flammability/spread3, replaceable/passable,
  non-full/non-opaque, **six .05-thick surface collision boxes**, undefined
  sturdy faces. Selection/ray shape also includes one selected growth box,
  but growth is **not a physical collision box**. It is not globally empty
  collision, nor a full-cube collision. Faces draw only against sturdy
  neighbours excluding fibre/feature; support against taint full blocks may
  disappear because their sturdy face contract is false.

Fibre ten bools are north/east/south/west/up/down + growth1..4. Original metadata
is always0; bools are derived actual state, not persistent random maturation.
`new Random(pos.toLong()).nextInt(50)` uses the **1.12 BlockPos packed layout**;
modern1.20 `asLong` differs and needs an explicit legacy pack for parity.
If a sturdy block is below: q0..3 growth1, q4..5 growth2, q6 growth3.
If sturdy above and q48..49: growth4, overriding any previous value.
Growth3 light12; growth2/growth4 light6; other light0.
No growth + only-adjacent-to-taint means immediate removal on random/neighbor
update; any state also dies on random tick without a near seed. Otherwise
spreads. Breaking growth3 produces a Vitium crystal when `nextInt(5)<=fortune`
and always pollutes1; it is not an ordinary self block drop.

Crust fall is allowed only when isOnlyAdjacentToTaint and below is air/fire/
fibre/replaceable/water/lava (Goo level<4), **unless any wood log exists in the
3x3x3 around the below position**. Vertical fall first; a supported crust with
air above can slide to random horizontal side only when three below-side
positions are air and the three own-column positions are same crust. Full
original falling entity owns/removes source on its first native tick, does not
create drops, and places only if supported. Generic FallingBlockEntity's
landing/drop logic is not proof of full parity.

FluxGoo is a **finite fluid**, default level7; it is a decay endpoint, not just
an inert purple cube. Its separate finite-flow, thinning/pollution, Thaumic
Slime spawning/growth and milk-incurable VisExhaust contract is outside this
seed-focused slice unless separately implemented/audited. Leave these
limitations explicit; do not claim whole taint ecology from seven blocks.

## Useful meaningful QA invariants

1. Normal and Prime native health/damage/dimensions/XP, downward-only motion,
   push behavior, persisted boost and native target/alliance handling.
2. Spawn accepts a solid-block candidate yet rejects liquid/entity collision,
   Peaceful and cube-overlapping Seed/Prime; diagonal cube spacing is rejected.
3. Near radius includes exactly32; edge excludes exactly25.6 and32; dimension
   isolation, stale/removal/death/unload do not preserve a phantom source.
4. Distinct aura base/vis proves flux/base formula; zero starvation .5 and
   pollution .1; boosted zero-flux path spreads and decrements each tick.
5. Native20-tick infection cube includes diagonally distant living mobs and
   tainted allies, with exact durations/amplifiers, excludes self, respects
   FluxTaint healing/undead contract. No render/scan path pays infection.
6. Rift size5 success gives Prime boost5 and pollution2 only once, removes
   source; blocked spawn gives neither payment nor removal; negative-coordinate
   candidate truncation and no block-collision rule remain correct.
7. Native spread preserves hardness gates, hem score, branch order, log axis,
   leaf-facing and exact .01/no-debit/5-debit distinctions. wuss suppresses
   ordinary fibre ticks while seed's ignore=true spread remains enabled.
8. Native random-tick decay reclaims soil/rock, fibre/log vanish promptly,
   no seed cannot keep spreading; live seed preserves blocks. Silk/drop/fortune
   conservation verifies physical player break and actual tagged crystals.
9. Fibre stable legacy positional growth, six thin collision surfaces,
   growth-only outline, correct light and persisted/reloaded derived state;
   actual loaded native client shows original textures without missing sprites.
10. Tiny actual melee, taint-ground starvation, lifetime201, no recursive
    child/drop/XP; Seed tentacle requires LOS and original target range.

Native tests must use a fresh dedicated QA world and client screenshots only
on an owned hidden non-input desktop. Fixtures that directly supply aura,
targets, terrain or predecessor knowledge are not ordinary survival evidence.
No new player-facing dev instructions/status strings or reference catalogue
belong to this runtime. Existing current-stage book gates remain unchanged.

## Scope limits

Native Seed/Prime, Tiny assistance, rift event1 and selected fibre/terrain
conversion are separable from full Tainted Swarm, Crawler, large/Giant
Taintacle combat, BottleTaint, finite FluxGoo/Thaumic Slime, Champion13,
Crimson and Eldritch ecology. Registered appearance/summon IDs alone prove
none of those behaviors. No new canonical research total is established by
this audit; apply the existing supported-progress gates until original paid
studies are separately implemented.

## Independent integration review and resolved corrections

Read-only review of the first 0.29 native source found two concrete lifecycle
and gameplay issues. Both were corrected before the final native evidence:

- `FallingTaintEntity.tick` previously checked build-height-loaded bounds before
  advancing `fallTime` and motion, freezing crust at the bottom boundary.
  Current `loadedXZ` checks chunk/world-border presence without restricting Y;
  the timer and physical gravity progress outside build height, and timeout is
  evaluated before any out-of-height block reads. Source ownership remains
  first-tick-only and timeout does not steal a restored source. The related
  `TaintedStationaryMob.move` now guards loaded horizontal chunks only, allowing
  native below-world damage to kill Seed/Prime/Tiny instead of trapping them.
- `TaintedStationaryMob` extends `VisualMobEntity`/`PathfinderMob`, not native
  `Monster`. Its explicit `shouldDespawnInPeaceful=true` correctly restores
  Peaceful removal, including persistent Seeds, but registering MONSTER and
  implementing `Enemy` does not restore bed safety. Mapped Forge 1.20.1
  `ServerPlayer.startSleepInBed` queries **Monster.class**, then invokes
  `Monster.isPreventingPlayerRest(Player)`. BETA26 Seed/Tiny are EntityMob and
  therefore obstruct ordinary sleep. Current `TaintRestEvents` adds the missing
  Forge sleep check. Independently compared with mapped Forge bytecode:
  it retains sleeping/alive/natural/reach/obstruction priorities, either-half
  reach bounds3X/2Y/3Z around bottom-centre, both-above suffocation predicates,
  ordinary respawn assignment and the Forge time hook before safety, creative
  exemption, and the point-centred hostile AABB grown8X/5Y/8Z. It keeps another
  mod's existing result. It invokes the time hook only once when returning an
  unsafe result and introduces no new player message.

The small renderer parity difference is also resolved: original model local
`at` stays0, so current `LegacyVisualModel` changes the amplitude modifier on
hurtTime only. AttackAnim still adds directly to Seed X bends as original.
An additional source-backed rendering defect is resolved: the extracted Seed
constructor retains two unused empty root placeholders before reassigning the
real tentacle. Rendering every baked root previously advanced the Seed pulse
counter twice before the real geometry, starting its scale sequence at q2 and
shrinking the descendant chain. Original `ModelTaintSeed.render` renders only
the real tentacle; root plus seven children visits q0..7 exactly once, after
which the original static counter wraps to0. Current `renderToBuffer` skips
only taint-model orphan roots with no cubes and no children, before calling
`renderTentacle`. Independent source review confirms that the real eight-part
Seed/Prime chain now starts q0 and follows q0..7, while preserving actual
child transforms and leaving other model render paths unchanged. The fresh
native screenshots must be reviewed after this fix; earlier client images do
not establish the corrected dimensions or segment shape.
Independent resource reads confirm all five fibre/growth source models and
eleven fibre state wrappers specify `minecraft:cutout`. FluxTaint index3,1
and VisExhaust index5,1 now each preserve the exact nonempty18x18 original
RGBA crop from the pinned potion sheet at y216 (native HUD row198 +18).
The extraction pipeline uses the same coordinates. The supplied client scene's
event1/boost400/platform/target/material display/randomTickSpeed100 are
explicit fixtures, not proof of a complete survival infection outbreak.

Scope adaptations already present in the native source include loaded-only
terrain reads, cleaned tracked old seed position during1200tick refresh and
immediate source removal on unload; rollback of failed secondary-seed spawn;
modern block-state NBT plus modern packed `Old` position for FallingTaint;
and selected modern tag/class mappings for removed Material categories.
No backward1.12 falling-entity block-state migration is promised. Current
finite FluxGoo, real Crawler/Swarm and their excluded event slots remain
unimplemented. The current fix surfaces have no remaining concrete blocker in
this read-only review; source review and exact resource reads are not native
pass counts. New sleep/void/falling tests exercise real startSleepInBed and
native damage callbacks; corrected tick fixtures use ServerLevel.tickNonPassenger
because direct entity.tick does not increment tickCount in1.20.1. Final native
run counts, fresh-source identities and screenshot review belong in VALIDATION.
