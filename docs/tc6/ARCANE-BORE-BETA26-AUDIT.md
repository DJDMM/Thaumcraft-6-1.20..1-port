# Arcane Bore: released BETA26 behavioral audit

Authority: `work/Thaumcraft-1.12.2-6.1.BETA26.jar`, SHA256
`9425f8643581b27ff8845b087c8bc6fc10425a32942f1a3f0e265ce6b38f7b5f`.
Decompiled source is pinned to commit
`954022bb777b7546281fb36df8522f0ba6b43f81` of TheDarkTower314/Thaumcraft-6-Source-Code.
The released class bytecode was checked with Java17 `javap -c -p`, including
the update/decrement/cast path, harvest/wear/final-air order, recharge and NBT.
This document records contracts and test definitions, not completed QA.

## Original entry points

- `common/entities/construct/EntityArcaneBore` and `EntityOwnedConstruct`.
- `common/entities/construct/ItemTurretPlacer`: metadata2, player horizontal facing.
- `common/container/ContainerArcaneBore`, `slot/SlotArcaneBorePickaxe`.
- `common/lib/utils/BlockUtils.harvestBlock`, `InventoryUtils.ejectStackAt`,
  `Utils.findSpecialMiningResult`; `api/ThaumcraftInvHelper`.
- `common/config/ModConfig` special mining map; original render/model/GUI,
  `PacketFXBoreDig` and `FXBeamBore` remain the client authority.

## Entity, ownership and operating state

Size0.9x0.9; eye height0.8125; MAX_HEALTH50, FOLLOW_RANGE32; no extra armor.
The owned construct breathes underwater, persists, inherits owner team,
accepts push/collision and uses clack/tool sounds. Bore horizontal movement
is /5; knockback receives x/z divided by10 and caps positive Y at0.1.
Regen is exactly one health at native ticks divisible by50.

On each server update: head→body yaw, heal, recharge if tick%10==0 and
charge<10, activation update, actual held tool inventory callback; then
target look/delay/dig and target search. Recharge adds the actual result of
`AuraHandler.drainVis(...,10,false)`; it does not clamp the result to10.
Original fields paused100/maxPause100 have no active fuel behavior.

Rail lookup checks the block below before the occupied block. TC activator
rail sets active=!POWERED. Other rails leave active unchanged. Outside rail,
only nonpassengers replace active with powered(blockbelow). Disabled update
clears the current target. Ordinary rail behavior is not a generic always-on
rail mechanism; powered TC rail pauses Bore, not the entire inherited AI.

Only an actual pickaxe/tool-class pickaxe is accepted. Working condition is
damage+1<maxDamage, so last durability is rejected even though slot insertion
accepts it. Radius=enchantability/3+Destructive*2, minimum2. Depth=radius*8+
Burrowing*16. Fortune=max(Fortune,Sounding) on a valid tool; speed is
int(actual destroySpeed/2)+Efficiency, not an Efficiency-square formula.
Refining reads physical infusion enchantment; physical Silk Touch wins.
Original normal material radii are wood5/stone2/iron4/diamond3/gold7.
Modern Netherite radius5 is derived from its native enchantability15.

The window contains one original slot at80,29 and36 player slots at their
original positions; no fuel slot. Original enchant action0 returns true;
shift transfers tools into/out of the entity's actual held equipment.
Modern menu adds online owner/alive/nonspectator/same-player/current-menu/
samelevel/range8 validation rather than original unrestricted canInteract.

## Search, target delay and charge quirk

Search starts at previous target-source or own block when absent/distant.
It traces from source-center to facing*depth, then again from one block in
front of the physical eye to the first target-center. Both use original
ignoreBlockWithoutBoundingBox=true and liquid=false: modern COLLIDER/NONE.
Target must have hardness>-1, nonempty collision and be neither own block nor
block below. Delay=max(10-speed,int(hardness*2)-speed*2), minimum1.
Diamond/stone gives6; the postfix delay-- allows removal on the seventh
subsequent update, after a separate target-selection update.

Spiral += int(3+max(0,(10-abs(radius))*2)); on360 wrap radius +=1, reset0 if
outside ±digRadius. Source X/Z keep original Java `(int)` truncation at
negative positions, followed by the original Z/Y/X rotations and floor.
The port bounds the no-progress spiral loop and refuses rays across missing
chunks, out-of-height/border or unreasonable range; search never force-loads.
It clamps corrupt infusion ranks and limits addon radius64, explicit hardening.

The released update only calls dig when charge>=0.25. `dig()` returns the
**final** `world.setBlockToAir(target)` boolean after BlockUtils harvesting,
not the harvesting result. Ordinary successful harvest has already removed
the block, therefore this final operation returns false. It does not spend
charge or play the gated rumble. If final residual removal does return true,
the update converts `(charge-.25)` via float→int→byte before storing float.
This released quirk is not repaired into a new per-block vis cost.

## Native harvest and physical output

Fake profile name `FakeThaumcraftBore`, survival, physical tool/location;
xpCooldown starts1. Original BlockUtils invokes Forge break permission,
canHarvest, removedByPlayer/destroy/harvest with detached loot tool and
Fortune override; XP remains physical. Forge harvest-drop handler collects
native loot, and nearby ItemEntities in target AABB inflated1.5 are copied
and removed, including native container contents.

After successful removal, each drop independently uses Refining chance
(level+1)*0.125 unless Silk harvested. Utils makes the RNG call even for
unmapped input. Its map has iron/gold/cinnabar ores, quartz and supported
ore-dictionary metals; output count multiplies original output count by
input count. Current mapped clusters each have original count1. Modern
raw iron/gold/copper and deepslate ore forms are explicit equivalents, shared
with the existing equipment adaptation, not historical1.12 item IDs.

First neighbor with item handler is selected in EnumFacing order
DOWN/UP/NORTH/SOUTH/WEST/EAST. The discovery probe uses the outward direction;
actual insertion uses opposite face. Ejection inserts remainder into that
single neighbor, never chooses a later empty chest after the first full one.
If none, choose opposite facing. Full-cube destination moves origin one block
opposite before spawning at origin+side; velocity is0.3*side, and original
Y has no added0.5. Experience is not captured as an item or stored as charge.

Wear counter += fake.xpCooldown after successful removal; subtract50 and
damage actual tool1 when>=50. No ordinary per-block tool wear callback.
Empty tool resets counter. Original counter/search/target/delay do not save.
Recharge/death/inventory callbacks retain original order except modern
accepted-output conservation and permission guard described below.

## Management, save and modern callback protection

Owner hit rotates toward player-facing opposite (or up when physically above)
without damage; DOWN does not replace facing. Name-tag interaction falls
through. Owner ordinary click opens GUI14; crouched click returns real tool
and original metadata2 placer, then discards. Normal lethal death drops real
tool but no placer. Component chances are mind/resonator/Aer/Terra0.2 each;
mechanismSimple/brassplate/greatwoodplank0.5 each; each gains Looting*0.15.

Original keys are `charge` float, typo `faceing` byte and `active` boolean;
owner UUID string/valid-spawn `v` are inherited, tool is native equipment.
Port preserves those keys, normalizes malformed data and rejects decorative
VisualOnly promotion. Transient spiral/delay/wear restart after load.
`PendingBoreOutput` and `BoreDeathOutputPending` are new conservation state.
All valid saved queue rows are read without silently truncating after4096.

Original dig forcibly tries setAir even after Forge canceled its harvest.
Port intentionally preserves a canceled protected block, callback-substituted
target/tool and removed construct. Physical tool identity is checked as well
as stack contents; identical detached copies cannot pay an old reservation.
Canceled item joins leave harvested output pending and saved for retry.
Collection/death drain only accepted item spawns and retain substituted tools;
canceled collection leaves the living construct available. A native canceled
LivingDeathEvent commits no death drain. Accepted death during harvest/output
callbacks defers the drain until the active operation finishes; canceled
death tool/output waits before native removal: the accepted-dead entity is
retained with deathTime below the native20-removal threshold (at most19;
the tick cap restores18 at19) until physical spawns are accepted. This pending
state is persisted. Original probabilistic component drops still follow the
native death-loot pipeline, with no new durable queue guarantee. Shared placer
checks physical stack identity around join while retaining original replaceable
terrain/debit ordering. These adaptations prevent losing,
duplicating or clearing physical stacks inside external event callbacks.

## Defined verification and boundaries

`BoreGameTests` exercises native attributes, movement, placement/facing,
tool eligibility/enchants, physical power/rail,50/10 tick boundaries, target
postdecrement, released no-spend, protected break and callback swaps, collision
ray, loaded-only refusal, real container loot/chests/refining/Silk/XP,
pending save/retry, original menu shifts and owner restrictions, physical
collection/death/component table, vetoed drops and reentrant lifecycle events.
One actual world/server/entity-tick trial removes50 physical blocks and checks
50 chest items/one durability, with a reinserted stone target as an explicit
fixture. Another actual scheduled mining trial kills the entity inside native
ejection join. A single-slot capability fixture kills on real accepted insert.

Fixtures use absoluteY284,300,308 and loaded neighbor chunks; source knowledge,
craft payment and ordinary gameplay are separate progression/client suites.
Synchronous native tick calls support deterministic callback boundaries;
they are not labelled natural AI/world scheduling proof. Server/client passes,
reviewed screenshots and exact counts belong only to actual logs/artifact
reports. No claim of complete survival, addon callback compatibility, arbitrary
automation inventories or independent multiplayer follows from these tests.
