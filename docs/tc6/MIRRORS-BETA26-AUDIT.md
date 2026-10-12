# Mirrors: released TC6.1.BETA26 contract and implementation preparation

This historical preparation audit was written at0.31. Native0.32 implementation and final evidence are linked in MIRRORS-CORE-PORT.md, MIRROR-ESSENTIA-INTEGRATION.md, MIRROR-PROGRESSION-BETA26-AUDIT.md, MIRROR-CLIENT-BETA26-AUDIT.md and ../../VALIDATION.md. The following paragraphs describe the original preparation state.
It does not enable a research key, register a native mirror, change a recipe,
increase the canonical census, or establish passed server/client QA. The current
mirror catalogue models and reconstructed infusion JSON are not working mirrors.
Only this document was added by this audit.

## Authority and verification

The primary authority is `work/Thaumcraft-1.12.2-6.1.BETA26.jar`, SHA-256
`9425f8643581b27ff8845b087c8bc6fc10425a32942f1a3f0e265ce6b38f7b5f`.
Readable reference source is pinned to commit
`954022bb777b7546281fb36df8522f0ba6b43f81` under
`work/thaumcraft6-reference/src/main/java/thaumcraft`. Paths below are relative
to that Java package. No TC4/TC5 guide or addon is a behavioral authority.

The original ARTIFICE entries below were read directly from the JAR's
`assets/thaumcraft/research/artifice.json`. Java 17 `javap -p -c` against the
released JAR independently checked item link validation/restoration/invalidation,
item transport/ejection/spawn/tick/instability, essentia source operations and
instability, block geometry/drop/collision, bound placement, hand transport and
container input/close callbacks. It also checked all three infusion registration
bodies, mirror glass, and the `canAcceptEssentia` cache-miss branch. The client
renderer and lamp prerequisite descriptions below additionally use the pinned
readable source; this is not an original-client visual replay or a native port test.

| Released class | SHA-256 of its JAR class bytes |
| --- | --- |
| `TileMirror` | `a880e7f93a42f52a9ac206338c5d3d65ae4688c1d8d0657a7288b2c72d1e711a` |
| `TileMirrorEssentia` | `659b5230bb1f5b4a8d0ad16100e27e14cfd405ca55ac336eda20b2c7184ec649` |
| `BlockMirror` | `e1596c1026e443803e6422d1246f1314d8c22d594a298b250176445512abdb7c` |
| `BlockMirrorItem` | `9148d8672b85658731091ff2a7d910339ac893fe8a23fe16346bd084dcfdd393` |
| `ItemHandMirror` | `2a28375d80f4969e755006d9fe754aae8fb34f7a4c78ec26c1ec8dceef668f77` |
| `ContainerHandMirror` | `005452ecb7a4b95f6f1571e89c77cdc9bf0870e41af96dab61254c5c32ee1dd4` |
| `InventoryHandMirror` | `1bd77a4fe5b55d4b29e4ae0047c09bf840b5ffe1577b3da6f9d467b00ad93b99` |
| `EssentiaHandler` | `6eeec4d53fae4fe18b8d16306e61e942f1f3b1e27f6ce4b5de8cdd02ba362614` |
| `TileMirrorRenderer` | `a230085bff3f91ce9cca69d5f5707536c918e8097b1f561c39222e0987a58434` |
| `GuiHandMirror` | `8db3f6f52d578ee8dcc35b2eaa0a15b942ff2b08f4306c33ac8d2ddc230335cd` |

## Required ordinary progression and prices

All entries are ARTIFICE, have two original stages, and no declared warp.
Parents below are the exact ordered original arrays. Study eligibility must
retain completed-parent gates; bare crafting research gates retain the original
known/started behavior, independently of completing the final recipe page.

| Entry | Parents | First-stage requirements | Final-stage recipe |
| --- | --- | --- | --- |
| `ARCANELAMP` | `BASEARTIFICE` | OBSERVATION ARTIFICE 1; original `!lux` aspect condition | `thaumcraft:ArcaneLamp` |
| `MIRROR` | `ARCANELAMP`, `INFUSION` | THEORY ARTIFICE 1; OBSERVATION ARTIFICE 1; one ender pearl; one mirrored glass; `f_TELEPORT` | `thaumcraft:Mirror` |
| `MIRRORHAND` | `MIRROR` | THEORY ARTIFICE 1 | `thaumcraft:MirrorHand` |
| `MIRRORESSENTIA` | `MIRROR` | THEORY ARTIFICE 1 | `thaumcraft:MirrorEssentia` |

One original THEORY point is 32 raw knowledge; one OBSERVATION point is 16.
Thus MIRROR pays 32/16 plus both physical items; each child pays 32 theory.
Reuse the existing progression transaction/XP rules, rather than add a special
mirror study button or manufacture extra requirements. The original JSON above
does not itself specify an XP price. Keep `!lux`'s existing typed aspect semantics;
it is not a negation to reinterpret as an invented absence-of-Lux condition.

`ConfigResearch.java:204-206` registers `f_TELEPORT` for scanning a Nether portal,
End portal, End portal frame, ender pearl item, or Enderman class including
subclasses. No travel achievement, chorus fruit, focus teleport, or dimension
visit substitutes for these registered facts without an explicit adaptation.

| Original recipe | Research / method | Exact payment and output |
| --- | --- | --- |
| `thaumcraft:mirrorglass` | `BASEARTIFICE`, shapeless arcane | 50 vis, Aqua 1, Ordo 1; quicksilver + `paneGlass`; one mirrored glass |
| `thaumcraft:ArcaneLamp` | `ARCANELAMP`, shaped arcane | 50 vis, Aer 1, Ignis 1; rows ` I ` / `IAI` / ` I `; A = amber block, I = iron plate; one arcane lamp |
| `thaumcraft:Mirror` | `MIRROR`, infusion instability 1 | central mirrored glass; three gold ingots + ender pearl; Motus 25, Tenebrae 25, Permutatio 25; one item mirror |
| `thaumcraft:MirrorHand` | `MIRRORHAND`, infusion instability 5 | central plain item mirror, metadata 0; wooden stick + compass + **empty map**; Instrumentum 50, Motus 50; one hand mirror |
| `thaumcraft:MirrorEssentia` | `MIRRORESSENTIA`, infusion instability 2 | central mirrored glass; three iron ingots + ender pearl; Motus 25, Aqua 25, Permutatio 25; one essentia mirror |

Sources: `ConfigRecipes.java:202`, `:222`, `:291-293`. MirrorHand map identity
is particularly important: released `ConfigRecipes` bytecode offsets 1926-1929
loads `Items.field_151148_bJ` with JVM type **`ItemEmptyMap`**. Pinned Java calls
this `Items.MAP`; it means an empty map here, irrespective of earlier generic
legacy registry-ID assumptions. The current port's
`recipes/infusion/mirrorhand.json` contains `minecraft:filled_map` and must be
corrected to `minecraft:map` when the real route is implemented and tested.

MIRROR cannot honestly become ordinarily reachable by merely adding three keys:
ARCANELAMP, native lamp behavior, mirror glass acquisition and the teleport scan
fact must also work. At audit time ARCANELAMP and the three mirror research keys
are unsupported, and no native lamp implementation was found.

The lamp prerequisite needs its own native implementation review. Pinned
`TileLampArcane` tries one triangular-random offset per axis in -15..15 every
world-time multiple of 5 when unpowered; it clamps above precipitation height +4
and below original Y5, requires air/block light below11/LOS, and places glimmer.
`removeLights` clears glimmer in its -15..15 cube. `BlockLamp` calls cleanup on
break and an enabled-to-powered neighbor transition, places facing the support,
and checks air at that support. Glimmer is invisible, air-like, replaceable,
noncolliding light15 and does not randomly expire (`BlockEffect`). These are
TC6 lamp contracts, not permission to force unloaded chunks or convert all
catalogue effect blocks; modern height/light readiness must be documented.

## Reciprocal links, persistence and dimensions

`TileMirror.java:46-148` and `TileMirrorEssentia.java:76-180` have matching link
machines. Both persist/sync `linked`, `linkX`, `linkY`, `linkZ`, integer
`linkDim`, and `instability`. New instances start unlinked with coordinates/dim
zero, count0 and retry modulus40. `count` and `inc` are not saved.

- Full validation requires `linked`, a live same-kind destination, its `linked`
  flag, and its coordinates/dimension pointing back to this mirror. Missing
  worlds return false without clearing the local flag; missing/wrong tile,
  unlinked peer, or wrong backlink in an existing world clear local `linked`,
  dirty and sync it. Simple validation performs these checks without mutation.
- Restoration requires a same-kind destination whose own full link is invalid;
  a valid pair cannot be stolen. It writes that destination's backlink, sets
  both linked flags, dirties and syncs both endpoints. Binding an item alone
  does not establish this pair; placing the bound item runs restoration.
- Invalidation resolves the world and explicitly checks the destination chunk
  is loaded before accessing its tile. It clears a same-kind peer's flag;
  the original does not recheck that peer's backlink. A stale old endpoint can
  therefore invalidate a different later pair. Reciprocal-only invalidation
  would be an explicit modern safeguard, not silently claimed release behavior.
- No explicit self-link rejection exists. A bound item replaced at its recorded
  coordinate can restore a self-pair. A modern rejection, if chosen, must be
  documented rather than inferred from an imagined original distance rule.
- Server ticks check `count++ % inc == 0`. Invalid simple links increase inc by
  20 while below600, then attempt restoration; valid simple links reset inc40.
  This is a changing modulo of total count, not a countdown waiting exactly
  40/60/80 ticks. Reload starts count0/inc40 again.
- Essentia mirror caches destination facing in `linkedFacing`, initially DOWN;
  restoration reads the remote blockstate into the local cache. The field is
  not saved/synced. DOWN is also the unresolved sentinel, so an actual DOWN
  destination is reread on subsequent source calls. Invalidation resets the
  remote cache to DOWN.

There is no distance surcharge, fuel, vis drain, dimension cooldown or extra
chunk ticket in these classes. Many original accesses can load chunks; only
invalidation has an explicit loaded-chunk guard. The existing port's loaded-only
policy requires a deliberate adaptation: resolve via `getChunkNow`, treat an
unavailable chunk/world as transient and retain its recorded link for retries,
never mistake unavailable data for proof the physical mirror was destroyed.

Use a namespaced `ResourceKey<Level>`/`GlobalPos` link in modern NBT. Legacy
integer mapping is reliable only for 0 = Overworld, -1 = Nether and 1 = End.
Unknown custom IDs require an explicit migration mapping; never guess that a
legacy number means a functioning TC4 Outer Lands dimension. Original any-NBT
placement reads zero defaults even for unrelated tags; a typed complete link
record is a separately documented modern NBT safeguard.

## Item mirror: physical queue and one-item ejection

`TileMirror` is an `IInventory`, not an essentia source. `outputStacks` is an
append-only list until ejection: entries are not merged and are not delivered
FIFO. `transport(EntityItem)` requires a full reciprocal link, appends the
entity's actual stack at the destination, adds that count to source instability,
kills the source entity, and dirties both tiles (`:150-167`). `transportDirect`
copies a positive nonempty stack into **this mirror's** queue without checking
its pair and without receiving-side instability (`:169-176`). A hand mirror
therefore sends to its explicitly selected mirror, even if that mirror is
unpaired; it does not forward to the selected mirror's partner.

Original collision/inventory forwarding calls `targetWorld.addBlockEvent` at
the **source's position/block**, a cross-dimension visual quirk. A modern FX
packet targeting the actual source and receiving endpoints in their own worlds
is an explicit correction; it must not change which queue receives the items.

Server update order is ejection, instability, then the link-retry check.
`eject` (`:178-198`) runs each tick when count >20 and queue nonempty, uniformly
chooses one **entry**, copies exactly one item, and only after its helper says
success shrinks the selected entry by1 and adds1 local instability. Empty
entries are removed. A new tile's first eligible native update is its 22nd;
subsequent ejection is one item per tick, not one stack or one event per20ticks.
Queued output does not require an active pair. Reload resets the startup delay.

`spawnItem` (`:200-231`) emits at block center X/Z and Y+.25 with velocity
facing * .15; original `timeUntilPortal=20` avoids immediate collision
retransmission. It calls native `spawnEntity` but ignores a returned false,
reporting success unless an exception occurs. For Forge1.20.1 retain the queue
and avoid instability debit when `addFreshEntity` is rejected. An accepted
physical output must retire its reserved queue unit exactly once even if a
synchronous listener removes the mirror, relinks it, replaces the queue or
reenters an operation. No callback may leave both paid queue item and output.

Queue persistence (`:299-323`) writes positive entries under `Items`, with a
legacy byte `Slot`. Reading appends every entry in list order and ignores Slot;
byte wrap does not impose a128/256-entry limit. Avoid a silent modern load cap
which deletes legitimately delayed paid items. Sync NBT contains link/instability,
not the server output queue.

The original inventory facade (`:325-409`) has one slot/max64, always-empty
get/extract/remove, unusable-by-player false, `isEmpty` false, and noop clear.
The setter forwards a copied stack if any same-kind remote tile exists, without
requiring the reciprocal linked flags; otherwise it attempts local physical
spawn. `isItemValidForSlot` checks only remote tile existence. The helper in
`api/ThaumcraftInvHelper.java:49-63` uses the Forge handler when available,
otherwise `InvWrapper`/`SidedInvWrapper`; it does not supply a hidden storage GUI.
Modern insertion must have an inert simulation, return the real remainder,
accept only committed transfer, and never expose/extract the private output list.
Requiring a valid pair for external insertion is an explicit tightening of
that original setter's lax behavior.

Breaking a mirror does not put output `Items` into its dropped item. Its public
inventory getters expose no queue to ordinary inventory dropping, and
`BlockMirror.dropMirror` copies only the link. Thus queued paid contents are lost
on original removal. A modern spill/refund or preserved queue owner on removal
would be a separately specified adaptation, with canceled drops and one-owner
persistence tested. Never copy a live queue into both block drops and a retry
owner. This audit does not silently choose the adaptation.

## Instability and aura pollution

Item mirror threshold is strictly >128; essentia mirror strictly >64. Each
server update over its threshold pollutes local aura by1 with show=true, then
subtracts the threshold **once**. Afterwards positive instability decreases
by1 when count %100 ==0, including the initial count0 tick. No loop purges all
excess in one update. Original decay itself does not dirty the tile.

Item collision/inventory forwarding charges source stack count; successful
ejection charges destination1 per item. Hand direct receipt has no input charge,
only later ejection charges. Essentia successful add/take charges1 to the
invoked local mirror; the call uses `addInstability(null, amount)`, so it does
not additionally charge the remote mirror. Queries/find/acceptance do not
spend items/essentia or charge instability. No new per-distance tax is implied.

## Block mounting, binding and drops

`BlockMirror.java:33-186`: iron material, jar sound, hardness.1, any-tool harvest,
nonfull/nonopaque, invisible block model plus tile renderer. Placement uses
the clicked face; support is behind the opposite face and must be side-solid
on the clicked face. Losing support drops the mirror then removes the block.
The constructor discards the return of `withProperty(UP)`, so its base default
actually remains DOWN; do not silently use the intended but unassigned UP.

The outline is the rear one-eighth slab: DOWN Y.875..1; UP Y0...125; NORTH
Z.875..1; SOUTH Z0...125; WEST X.875..1; EAST X0...125. Use six native mounted
shapes. Block activation is consumed without opening an ordinary block GUI.
Item mirror collision is server-only, live `ItemEntity`, portal cooldown0;
essentia mirror does not teleport physical items.

`dropMirror` produces one metadata0 item when unlinked, or metadata1 with peer
`linkX/Y/Z/Dim` when linked, invalidates the peer and emits the item. It does not
copy local instability. The custom harvest/chance overrides avoid an additional
ordinary mirror drop; modern loot/removal handling must likewise have one owner.

`BlockMirrorItem.java:38-119`: using an item mirror on a same-kind unpaired/invalid
mirror makes a one-item copy carrying metadata1 and the clicked tile's coords/dim,
puts it in inventory or drops it, and consumes held1 **only in survival**.
Creative does not debit it. Valid existing pairs give the original normal
already-linked notice. Item and essentia mirrors cannot be cross-bound.
Placement writes coordinates when the item has any NBT and calls restoration
after native block placement. Original rarity is UNCOMMON and linked coordinate/
dimension tooltips are normal gameplay UI, not developer labels.

The plain metadata0 central MirrorHand ingredient excludes bound metadata1 in
the original. Current port `InfusionIngredient.test` checks `getDamageValue`,
while a modern nondamageable BlockItem whose link is only NBT stays damage0.
Choose a real legacy variant mapping or typed central predicate before enabling
the recipe, and test rejection of a linked mirror; a JSON damage0 check alone
does not establish this distinction.

## Hand mirror: original GUI and transaction boundaries

`ItemHandMirror.java:36-134`: maxstack1, UNCOMMON, share tag, glint when any NBT
exists. Using it on an item mirror records that mirror's coords/dim, gives jar
sound volume1/pitch2 and normal `tc.handmirrorlinked`, consumes nothing, and
does not require the target to be paired. Other blocks return FAIL.

Right-click with NBT opens GUI4 if the target is an item mirror. An unavailable
world returns without clearing the tag. A missing/wrong tile in an existing
world clears the **whole tag**, zaps volume1/pitch.8 and gives normal
`tc.handmirrorerror`. Static transport has similar invalidation; valid target
calls direct receipt and teleport sound volume.1/pitch1. Released bytecode
returns true when that target exists even when `transportDirect` returns false;
the normal menu excludes empty input, but direct invalid-input calls need a
documented modern guard. Keep original useful link/error messages and sounds;
do not add server-check/status/debug text.

`ContainerHandMirror.java:25-118` and `GuiHandMirror.java:19-58` use one input
at (80,24), inventory rows starting (8,84), hotbar (8,142), default176x166 and
`textures/gui/gui_handmirror.png`. There is no buffer, button or fuel slot.
The container captures **current main-hand selected stack** even when the item
right-click came from offhand. Supporting offhand correctly is a modern adapter
requiring bound physical hand/slot identity; never authorize a stale menu by
merely matching item values or link coordinates.

Input change copies its nonempty stack and clears the input before synchronous
transport. True keeps it empty and syncs listeners; false restores the input.
`InventoryHandMirror` invokes the callback on nonempty set, so restoring can
recursively retry failure in the original. Use a bounded/reentry-safe modern
transaction, retaining every unpaid item on failure. Original click/shift
guards prohibit hand mirror items; hotbar keys are disabled and GUI protects
the selected mirror hotbar slot. Original `canInteractWith` always returns true;
modern hand identity/liveness checks should close safely and refund remaining
input once. Close drops remaining input; canceled physical refund must keep a
real inventory/retry owner rather than erase it on close/disconnect.

## Essentia mirror: remote source, not tube or buffer

`TileMirrorEssentia` implements `IAspectSource`, **not `IEssentiaTransport`**.
It stores no aspect amount, has null getAspects, noop setAspects, false bulk
take/contain, containerContains0, and isBlocked false. Do not add suction,
capacity, local invented essentia tank, tube ports or a new screen.

Single-aspect add/take/contains (`:186-255`) require a full pair and amount <=1;
larger requests fail/return the supplied remainder. They use the remote mirror
as the consumer for `EssentiaHandler`, destination facing, **range8**, remote
`ignoreMirror=true`, and FX extension5 for transfers. Add success returns0,
failure1; take success returns true, each success adds local instability1;
contains delegates read-only find. Original zero/negative amounts can still
request one unit while bookkeeping uses invalid amount: explicitly reject
nonpositive amounts/null or unregistered aspects in a modern typed API.

`doesContainerAccept` does not full-validate the link, can dereference an absent
world, and returns true when its destination tile is missing/wrong; otherwise
it calls remote canAccept. Loaded-only safe false for unavailable endpoints is
an explicit correction. Do not debit a source merely because that lax original
acceptance predicate says true.

`EssentiaHandler.java:31-287` resolves sources by consumer dimension+position,
not by aspect/direction/range/ignore flag. No positive cache TTL exists. Failed
drain/add or empty scan uses the shared 10-second wall-clock delay; refresh
removes candidates while retaining that delay. Preserve these existing handler
contracts rather than silently adding a second mirror-only cache policy.

Range8 is the original directional box: two transverse axes -8..8 inclusive,
forward axis0..<8, origin excluded; null direction uses -8..<8 along UP. Stable
distance-squared sorting retains loop-order ties. A mirror consumer excludes a
candidate mirror whose recorded backlink points at it; remote ignoreMirror
also skips all essentia mirrors, preventing further mirror hops on that leg.
Missing/invalid first candidate ends the original loop; blocked sources are
skipped. Add prefers compatible nonempty sources before accepted empty sources;
mirror null aspects classifies it as empty. One successful operation moves1.

Primary `canAcceptEssentia` cache-miss bytecode really calls **findEssentia**
after populating candidates, testing containment rather than acceptance on
that first call. Later cached calls test acceptance. This is a release quirk,
not a decompiler typo; preserve it or record an explicit tested adaptation.

An ordinary consumer -> local mirror -> remote mirror -> remote jar transfer
has two original local FX legs in their respective dimensions; it physically
debits the remote jar only once, has no intermediate stored unit, and charges
only the invoked local mirror. Packet endpoint dimensions cannot be inferred
from a single unqualified BlockPos.

## Integration suggestions and required verification

Keep native item/essentia block IDs, current original frame assets, and original
hand texture. The source renderer (`TileMirrorRenderer.java:525-626`) renders
item `mirrorframe` versus essentia `mirrorframe2` in all six orientations. A
linked mirror within squared distance <1024 renders its layered tunnel/particle
surface and `mirrorpanetrans`; otherwise `mirrorpane`. The tunnel is not a
camera into the actual destination chunk. Review six orientations, linked/
unlinked/distant surfaces and native item teleport FX in actual hidden-client PNG.

Suggested division of code, not already implemented APIs:

1. A shared immutable namespaced link record plus same-kind physical endpoint
   resolver; explicit unavailable/invalid/valid result, server thread and loaded
   identity checks, reciprocal link snapshots/revisions, one guarded restore/
   unlink operation, original sync fields/retry timing.
2. Native mirror block/BlockItem and item mirror BE queue, insertion-only handler,
   one-item accepted physical ejection, portal cooldown and coherent removal
   ownership; original bound-item/creative distinctions.
3. Native hand item/menu with actual held-stack identity, one-input detached
   reservation, guarded direct queue commit and exactly-once close/refund.
4. Essentia BE as a route descriptor and a manager-internal leaf resolver, then
   progression/precursors/payments and original client renderer/menu.

The current `AirborneEssentiaManager.SourceCache` guards all operations with
one `operationActive` boolean. Naively implementing mirror.take as a recursive
public manager.drain call fails this guard. Its `DrainConfirmation` captures
one ServerLevel/source identity, and `Transfer` has only unqualified source/
target positions. Paid mutation whitelists currently exclude mirrors because
no rollback route exists. Merely whitelisting a mirror as nativeSource does
not make cross-dimension payment transactional.

Resolve the one permitted remote leg **inside** the existing guarded operation.
A confirmation must snapshot both physical mirror instances/links/facing and
the native remote leaf jar, carry both worlds, and reject replacement/relink/
unload/reuse. Real native leaf mutation/refund stays the only essence owner;
the mirror never acquires a manufactured buffer. Publish both dimension-qualified
FX legs only after one accepted commit. Preserve existing local ordering/delays
and the paid rollback policy of supported leaf sources; generic foreign sources
remain explicitly outside paid guarantees. Existing AirborneEssentiaGameTests
assert that mirror_essentia is not yet an IAspectSource and must be updated only
when native implementation replaces that visual-only state.

Future meaningful tests must cover all parents and separate knowledge/item/fact
shortages; paid mirror glass/lamp and all three altar recipes; empty-map versus
filled-map and bound central rejection; survival/creative binding; six support
faces; reciprocal restoration/valid-pair refusal/stale invalidation; loaded and
unavailable same/cross-dimension links; original retry modulus and save reload;
one-item queue timing, random entry choice and unlinked direct ejection; rejected
spawn/reentry/removal persistence; exact >128/>64 pollution and100-count decay;
hand offhand/slot replacement/input close/refund; leaf jar single-unit transfer,
both FX legs, no remote-mirror recursion, failed/stale confirmation with no debit,
shared handler cooldown and first-cache acceptance quirk.

These are test definitions, not passed checks. Only a native implementation,
fresh full server QA, reviewed final hidden-client captures and explicit ordinary
acquisition routes can justify enabling the corresponding census entries and
shipping a mirror build. No canonical research/infusion count changes here.
