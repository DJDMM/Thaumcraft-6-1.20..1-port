# Item and hand mirror native implementation contract

This document describes the native implementation in the 0.32 work tree.
It is not a passed GameTest/client report. Root records final evidence after
running the complete server and the isolated hidden client.

Authority is the released 6.1.BETA26 JAR, SHA-256
`9425f8643581b27ff8845b087c8bc6fc10425a32942f1a3f0e265ce6b38f7b5f`,
and readable source commit `954022bb777b7546281fb36df8522f0ba6b43f81`.
The complete released audit is [MIRRORS-BETA26-AUDIT.md](MIRRORS-BETA26-AUDIT.md).
This implementation also independently reread Java 17 `javap -p -c` output for
TileMirror's direct receipt, random entry ejection, 20 portal cooldown, count
modulus, strictly greater128 pollution and count%100 decay. Block/hand source
was reread for all faces, bound/plain, actual vanilla Enderman teleport sound,
and original success/error/already-linked messages.

## Retained release behavior

Native block IDs `mirror`/`mirror_essentia` and item `hand_mirror` remain unchanged.
Default DOWN retains the discarded original withProperty(UP) assignment.
Six rear one-eighth slabs require the corresponding sturdy support face;
hardness .1, any tool, original jar SoundType volume.5/pitch1, invisible model
and native BE renderer. Ordinary activation is consumed without a block GUI.
The queues never enter a mirror item or client sync tag.

Links contain a dimension-qualified physical position. Item and essentia kinds
cannot pair. Bound BlockItem placement restores reciprocity only at a free
same-kind destination. Merely creating a bound item does not pair it. Survival
binding consumes1, creative consumes0 and still creates1 bound copy. Existing
pairs show the ordinary original purple italic already-linked notice;
successful item binding has jar sound and no invented success chat.

Item collision copies the whole physical stack into the peer queue, empties
and discards the source entity, charges source count and not receiver count.
Direct receipt copies a positive stack into this selected mirror's queue even
without a pair. Separate entries stay separate and retain order on persistence;
the legacy byte Slot is not a128/256-entry cap. Ejection picks a random entry,
emits1 after count>20 (first22nd update), then1 per native update. It uses native
ItemEntity at center X/Z, Y+.25, facing*.15 and portal cooldown20. Queue output
continues when unpaired. Reload resets count0/inc40; link retry is changing
`count++ % inc`, not a countdown. Inc grows20 to600 on invalid checks and
resets40 when reciprocal simple validation succeeds.

Item threshold is >128, subtract128 once and add1 local Flux; count%100 decay
includes count0. Hand direct input charges no receiver input instability.
No distance surcharge, fuel, vis payment, GUI button, or manufactured tank.
The original GUI has input80,24, inventory8,84, hotbar8,142 and176x166 texture.
Hand binding records the explicitly selected item mirror without requiring
its pair. Sending targets that block, never its partner. Missing loaded target
clears the entire hand item tag and gives original zap/error. Unavailable target
retains the link. Success uses vanilla ENDERMAN_TELEPORT volume.1/pitch1.

## Explicit modern adaptations and physical ownership

All endpoint/refund resolution uses getChunkNow, never a chunk ticket. Unavailable
world/chunk is transient, not proof of destruction. Complete typed NBT prevents
unrelated tags from fabricating coordinate0. Legacy integer dimensions migrate
only0/-1/1. Self pairs are rejected and stale endpoint invalidation clears only
its own reciprocal backlink, both deliberate safeguards beyond the release.

The one-slot IItemHandler is insertion-only. Simulation is inert, physical
insertion returns the real remainder, accepts at most min64/item maximum, and
requires a reciprocal pair rather than the original setter's lax remote-exists
check or local fallback spawn. A handler never exposes the paid output queue.
Old capability objects require a physical live BE, so they cannot insert after
native removal/replacement. The output list has no silent cap.

Before a Forge physical join the BE detaches one reserved queue unit. Reentry
cannot advance the same server tick or emit another reserved unit. Accepted
joins retire that exact reservation once; veto restores the same entry/count
without splitting the probability distribution. A callback removing the mirror
defers queue migration until the accepted/vetoed reservation resolves. New
incoming entries remain separate and owned throughout the callback.

Native onRemove establishes one per-level MirrorRefundSavedData owner for the
link-only mirror item and paid queue, before unlink/sync callbacks. Vanilla loot
is empty, so native player/support/explosion removals cannot also produce a
second mirror. Accepted output migration contains only unpaid units. Creative
suppresses only the block's own mirror item, not its paid contents. Canceled
spawns stay in SavedData, retry only in loaded chunks, and disappear from that
owner only after an accepted join. A guarded detached batch preserves newly
added callback refunds and prevents nested retry. This intentionally preserves
contents that original BETA26 discards on physical removal.

Hand menus bind the actual physical main/offhand stack, original selected slot,
and immutable link snapshot. Same-value copied/rebound stacks are unauthorized.
Hotbar swap and hand-mirror input are blocked. The one input is detached during
direct commit; failure restores it under a reentry guard, avoiding original
recursive failure callback. Close/logout refunds once: first native inventory,
then the persistent physical retry owner if inventory is full. The input remains
owned even if the menu becomes invalid. These modern physical boundaries do not
claim that arbitrary third-party item callbacks are rollback-capable.

## Native verification definitions

MirrorGameTests currently declares51 tests. They exercise typed/legacy links,
six actual BlockItem placements and support loss, reciprocal/self/cross-kind/
stale invalidation, loaded-only unavailability, original retry/load timing,
actual physical collisions/cooldowns, bounded insertion simulation/remainders,
22nd-tick/per-tick/random-entry output,300 persisted separate entries and sync
privacy, veto/reentry/native removal/refund SavedData persistence, creative
queue preservation, link-only single drop, survival/creative binding and actual
bound placement, hand binding/main/offhand/current-slot identities, invalid
target/input/close/logout/full-inventory retry, strict instability and count0
decay,30+ actual world/block entity ticks, and a loaded native Overworld-Nether
physical item route. Six-face fixtures use native sturdy blocks; high arenas,
raw inputs, loaded Nether region and seeded random-entry fixture are explicit
test preparations. No direct queue test alone proves a full survival playthrough.

Native Forge canceled placement restores the held item; its restoringBlockSnapshots
removal suppresses the new block's own mirror item while retaining any paid queue.
An ephemeral placement receipt also handles a listener physically removing the
new endpoint before cancellation. Only the own mirror item waits until a later
actual world gameTime; paid queue migration never waits for placement approval.
The native EntityPlaceEvent records the exact hand identity/cancellation; its
actual event reference also supplies the final cancellation state after later
same-priority callbacks, and
the final detached native stack supplies the survival debit proof. Accepted
placement followed by a legitimate selected-slot change remains valid. A copied
physical hand during the event cannot pay: settlement restores only the same
new physical endpoint and discards its unpaid own drop. The native interaction
order, interactive blocks, Adventure permissions and Forge placement event stay
inside the normal ItemStack.useOn path. Serialization includes only proved paid
own drops; paid queued contents always have an unconditional SavedData owner.
Nine placement veto/removal/identity regressions are included in the51 tests;
Final cancellation remains authoritative even if a later listener uncancels;
only an actual snapshot restoration/receipt rollback sets the explicit canceled flag.
there is no crash-snapshot claim inside an unfinished native Forge callback.

Core compile/full server/client pass counts belong only to the final logs,
artifact report and VALIDATION.md. No provisional count here is QA evidence.

The first actual server run after registry repair found the cross-dimension
fixture querying a loaded but player-untracked Nether chunk through the native
visibility-filtered getEntitiesOfClass. Saved native region NBT independently
contained the one cobblestone output with portal cooldown20 and, after failed
fixture cleanup, its remaining2-unit refund; production conserved all3.
The fixture now captures actual Forge joins, verifies isAddedToWorld/alive/
receiver-level identity/count1 plus queue2, and removes those exact entity
references. It adds no production chunk ticket or forced entity visibility;
removal refunds are not included as successful transfer evidence.

Independent review after server3 found an actual reentrant join case: a Forge
listener can call native addFreshEntity on the same output before the outer
join finishes. The inner add registers the physical entity; the outer add then
returns false for its duplicate UUID. A private genuine ItemEntity subclass
(still native EntityType.ITEM and ordinary saved minecraft:item) records the
native onAddedToWorld callback monotonically. The shared ejection/refund helper
retires the reservation after either native acceptance, including an accepted
output then consumed/removed before the outer callback cancels. It does not
restore an already paid unit. Two additional native tests exercise both these
phases for one-item ejection and persistent whole-stack refunds; unsupported
manual spoofing of entity lifecycle callbacks is not a native accepted join.

A second independent audit after server4 found that a synchronous native save
inside the outer join callback could still serialize an already accepted unit
while the outer call had not returned. BE reservation views/counts/save now
exclude the exact in-flight entity once its monotonic native acceptance proof
is true. Refund counts/save exclude only the exact accepted in-flight refund
identity; an equal appended position/item/count remains independently owned.
Unaccepted reservation snapshots retain every unit. Two additional native
tests take snapshots before acceptance, after inner acceptance and after the
accepted entity is consumed, then reload and eject/refund the saved remaining
units with exact physical conservation. Normal original ejection payment and
instability order are retained; the change eliminates a second serialized
item owner at the actual native acceptance boundary.
The same accepted in-flight BE snapshot includes the pending1 output instability
cost without charging the live field twice. The save/reload regression checks
unaccepted0, accepted/consumed1, normal outer-return1, loaded1, then original
count0 reload decay followed by three further accepted outputs for final3.
