# Owned crossbow turrets: BETA26 adapter contract

Authority is the released `work/Thaumcraft-1.12.2-6.1.BETA26.jar`,
SHA256 `9425f8643581b27ff8845b087c8bc6fc10425a32942f1a3f0e265ce6b38f7b5f`,
and reference source commit `954022bb777b7546281fb36df8522f0ba6b43f81`.
The JAR hash was rechecked during the 0.30 audit. Local `javap -p -c`
extracts for the four construct classes, two target AIs and three containers
are in `work/turret-030-audit`. This document establishes behavioral contracts
and test definitions; actual integrated pass counts belong to VALIDATION.md.

Relevant original source under `src/main/java/thaumcraft`:

- `common/entities/construct/EntityOwnedConstruct.java`.
- `common/entities/construct/EntityTurretCrossbow.java` and
  `EntityTurretCrossbowAdvanced.java`, including their nested target/watch AIs.
- `common/entities/construct/ItemTurretPlacer.java`.
- `common/container/ContainerTurretBasic.java`,
  `ContainerTurretAdvanced.java` and `slot/SlotTurretBasic.java`.
- `client/gui/GuiTurretBasic.java`, `GuiTurretAdvanced.java`,
  `client/renderers/models/entity/ModelCrossbow.java` and the two construct renderers.
- `common/config/ConfigRecipes.java` and released `research/artifice.json`
  for research/arcane manufacturing. Shared research and recipes have their
  own progression tests; the constructs do not silently unlock them.

## Identity, ownership and lifecycle

The existing `thaumcraft:turret_basic` and `thaumcraft:turret_advanced`
EntityTypes and placer items now instantiate native constructs. The renderer
base remains compatible with existing catalogue models, but the two native
classes clear the old decorative NoAI setting. Other visual catalogue mobs
are unaffected.

The original `v` validity marker and `OwnerUUID` string are saved. Actual
main-hand ammo is ordinary mob equipment NBT; advanced targets remain a byte
under `targets`. A placed construct is persistent, breathes underwater,
is pushable/collidable and does not despawn with distance or Peaceful.
An invalid/unplaced construct is discarded on its first server callback.
The port rejects an old `VisualOnly` save as proof of valid deployment.

Owner UUID and the original owned flag are represented by a single nullable
UUID in the port. Owner/team lookup uses the loaded online owner; physical
management compares the player's UUID. A malformed UUID clears ownership
instead of throwing. Original pre-UUID name conversion is not a modern
account-resolution service. Restoring a native construct retains the actual
saved NoAI flag, including a disabled rail state; the decorative superclass
must not overwrite it.

Original sounds are Clack for idle/hurt and Tool for death, with ambient
interval240. A custom-named dying construct sends the combat tracker's actual
translated death message to its online ServerPlayer owner when
`showDeathMessages` is enabled. Unnamed constructs and the disabled rule
remain silent. The death guard prevents a repeated callback from notifying
twice.

## Native stats, movement and healing

| Contract | Basic | Advanced |
|---|---:|---:|
| Maximum health |30|40|
| Armor points |2|8|
| Width × height |0.95 ×1.25|0.95 ×1.5|
| Eye height |height ×0.66|1.0|
| Follow range |24|24|
| Ranged attack interval |20..60|20..40|
| Step height |0|0|

Both retain gravity and ordinary vertical movement. Basic divides horizontal
movement by20. Advanced first divides by15, then inherits the basic /20,
giving /300 overall. These are intentionally compounded, as in the release.
Basic passes knockback direction components divided by10 to the superclass
and caps upward velocity at0.1; advanced adds another /10 direction pass.
Direction normalization in the vanilla superclass is not replaced by an
invented /10 knockback-strength reduction. Hurt rotates yaw by
Gaussian45 and pitch by Gaussian20. Every80 server ticks, a living construct
heals one point even with AI disabled.

The original thaumcraft activator rail reads POWERED and assigns NoAI on
each server update. A rail immediately below takes precedence over the
current block. The vanilla activator rail does not toggle this state.
Leaving the thaumcraft rail intentionally leaves the last NoAI value
until a later thaumcraft rail changes it; this source quirk is preserved.
The native block is appended to modern `minecraft:rails`: both the tag
and BaseRailBlock class are required by native rail/minecart recognition.
The rail test uses an actual neighboring redstone block, whose removal
physically unpowers the rail. A forced POWERED=true palette without a power
source is not a valid fixture because native onPlace immediately recomputes it.

## Placement and collection

The native placer rejects a downward clicked face. It chooses the clicked
block if replaceable, otherwise the adjacent block in the clicked direction.
The two vertical blocks must be air/replaceable and their1×2 volume must
contain no entity. The accepted replacement includes snow layers and grass,
which are cleared for the deployed construct. Both variants spawn at
block-center X/Z, base Y, initial yaw/pitch0, with owner and valid marker.

The original consumes one placer even in creative mode; this remains true.
Modern checks add loaded chunks, build height, world border, six-block
placement reach, edit/interaction permissions and server-thread ownership.
They do not create remote chunks. Accepted native entity insertion precedes
terrain clearing/payment so a canceled Forge join event cannot delete
blocks or take an item. A callback replay guard and final held-stack/state
check prevent double deployment or paying a substituted stack.

Ordinary owner right-click opens the live ammo menu; owner sneaking
right-click returns all tagged ammo, one variant-specific placer, plays Zap
and removes the entity. Original collection does not carry its damaged
health into the returned placer. Strangers receive the original translated
`tc.notowned` notice. Modern management also requires a live, nonspectator
owner, a live entity, same world and squared distance≤64. Collection is
idempotent even across callback replay.

## Ammunition and projectiles

The live mob main-hand stack is the one ammo inventory slot. Accepted ammo
is a modern ArrowItem, equivalent to the original ItemArrow predicate.
An empty construct examines the dispenser immediately below each native
base callback, including while NoAI. Only an upward-facing dispenser counts.
It scans slots in order and transfers the entire first arrow stack,
preserving potion/NBT data, leaving other slots unchanged, playing Ticks
and broadcasting original status17. A nonempty turret never overwrites its
ammo and a sideways dispenser never reloads.

Every shot creates a modern native Arrow, the tipped-arrow equivalent.
The release always creates EntityTippedArrow rather than calling the
ammo's projectile factory: even spectral ArrowItem ammunition produces
that class. Its potion/custom effects are copied from the held stack.
Base damage is exactly `2.25 + range*2 + Gaussian*0.25`; velocity2 and
inaccuracy2 are retained. The target Y expression is
`target.minY + target.eyeHeight + range*range*3 - arrow.y`.
The muzzle shifts along the current look vector by−0.8999999761581421
on the ground or+1.75 while riding. Pickup is DISALLOWED. One accepted
shot consumes one arrow and plays native Arrow Shoot plus status16.

Modern spawn/callback guards reject dead/foreign targets and canceled
projectiles without debit. Reentrant spawning cannot create extra arrows,
and a callback-swapped ammo stack causes the unpaid projectile to be
removed. These are explicit transaction adaptations; source failure paths
were not transactional. Empty/nonarrow stacks cannot be used to manufacture
an unbacked shot through a direct method call.

Status16 drives the original six-tick firing swing and status17 the
ten-tick loading cycle, including −1 initial counters and wrapped partial
interpolation. The actual normal model/advanced OBJ are animated by these
states; no renderer grants gameplay state or ammo.

## AI and advanced target flags

Basic native target acquisition considers living Mob + Enemy classes,
equivalent to original EntityLiving + IMob, every RNG1/5 acquisition check.
It chooses the nearest visible eligible target in its bounding box expanded
24 horizontally and4 vertically. Retaliation retains native HurtByTarget.
The stationary ranged goal uses zero travel speed, native sight and the
original20..60 interval; a watch goal keeps yaw/pitch aimed with10/20 limits.
Sight loss is tolerated for60 target callbacks before release.

Advanced acquisition instead considers living entities and four byte bits:
animals0, mobs1, players2, friendly3. Default flags are2 (mobs only).
Modern creature/ambient/aquatic animal categories map the old IAnimals
classification; Enemy takes the hostile branch. Owner UUIDs on native
OwnableEntity animals and the already implemented Thaumcraft golem are
read as their original ownership counterpart. No fake ownership tag is
invented for another entity.

The release's unusual Friendly filter is retained: with a team it selects
that team rather than other teams; with an owned construct it selects same
owner animals and players, rejecting unrelated animals/hostiles. Without
Friendly, own owner/team/owned animals are excluded. Nevertheless inherited
OwnedConstruct/basic onUpdate *unconditionally clears an allied owner/team
target*, including after Friendly acquisition. Enabling Friendly must not
silently relax this original inherited callback.

Players sneaking reduce acquisition distance by0.800000011920929.
Invisible players additionally multiply by0.7 times armor-cover fraction,
with a minimum cover0.1. At range24 a naked invisible player's bound is1.68.
Disabled server PvP switches off Players when Friendly is false; the
original Friendly exception allows acquisition, but advanced onUpdate
still clears a nonowner Player target while PvP is off. All four setters
clear the current target immediately.

Target queries reject spectators/dead/foreign entities. Sight routes check
loaded chunks before ray tracing and never force remote generation; this
loaded-only boundary is a modern adaptation. Target bits from malformed
NBT are bounded to the four supported bits.

## Live menu and physical loot

The basic arrow slot is at80,29 and advanced at42,29, followed by the
original three inventory rows at8,84 and hotbar at8,142. Both original
textures/layouts are175×232. The screen draws the actual live ammo/health.
The advanced original translated target buttons send ordinary window-bound
menu actions1..4. There is no new arbitrary entity mutation channel.
Ordinary slot clicks, shift transfer and target actions are rejected for a
stranger, stale window, removed entity or distant owner. Shift insertion
accepts only arrow stacks; extraction conserves actual tagged ammo.

Death drops the remaining ammo once and independently rolls each component.
A destroyed turret never returns a complete placer. For Looting L, bonus
`L*0.15` adds to each probability:

| Component | Basic probability | Advanced probability |
|---|---:|---:|
| Appropriate mind |0.2+bonus|0.2+bonus|
| Simple mechanism |0.5+bonus|0.5+bonus|
| Greatwood plank, two independent rolls |0.5+bonus each|0.5+bonus each|
| Brass plate |—|0.3+bonus|
| Iron plate, two independent rolls |—|0.4+bonus each|

Mind is clockwork for basic and biothaumic for advanced. Item lookup must
use the actual Forge item registry: brass/iron plate IDs are owned by the
existing primary module and are not entries in CatalogModule.stack.
Looting7 makes every listed roll deterministic. The native test asserts
exactly four basic items and seven advanced items including brass1/iron2.

## Evidence boundary

TurretGameTests defines21 native server scenarios: original stats/motion,
survival/creative placement, obstruction/canceled/reentrant deployment,
whole dispenser refill, arrow effects/formula/spectral quirk,
mounted muzzle, canceled/reentrant/substituted projectile callbacks,
actual enemy-selection AI, advanced attack rate, sight/unloaded queries,
target/friendly/team/PvP/sneak/invisibility behavior, rail/healing,
native/old decorative save handling, collection/death ammo conservation,
guaranteed physical component drops, named-owner death notices and guarded
live menu transfer/actions.

Knowledge, precursor materials, empty high arenas and durable target mobs
are explicit fixtures. Test definitions do not establish a completed
ordinary survival playthrough or independent-client multiplayer session.
Construct arenas use absolute Y250 and explicitly loaded neighboring
chunks, preserving all loaded-only production checks. Modern flat template
origins are below zero, so relative Y128 can otherwise land at absolute Y68
among unrelated fixtures. Bounded air preparation and saved block restoration
isolate sight/collision. Block restoration precedes final item cleanup because
removing native rail support can itself create a physical rail drop.
The actual acquisition test waits for the native enemy section to become
accessible to the same entity query used by the goal. FULL chunk preparation
does not itself finish entity-section visibility updates. AI remains paused
during readiness; one fresh, fixed140-real-server-tick trial then checks hostile
selection, nearer-animal exclusion, physical arrows and exact ammo debit.
It never retries a failed trial until a shot eventually appears.
Its asynchronous arena uses Y280 so it cannot occupy a synchronous Y250
fixture on the next server callback.
The actual world scheduler ticks the entity, with exact140 world-clock,
server-clock and entity-counter deltas. Modern Mob scheduling combines the
global server tick with entity ID: repeated tickNonPassenger calls inside one
callback freeze its parity and can prevent fresh acquisition after the first
RNG miss. Direct native ticks are suitable for the separate already-targeted
rate/formula callbacks, but do not establish acquisition. Physical arrows are
observed during the real trial; ordinary arrow collision can remove them before
the final frame, so a final surviving projectile is not required.
Integrated fresh server counts and reviewed hidden client captures must
be reported separately after running. Arcane Bore and its placer variant,
mirrors and other devices are outside this construct tranche.
